package com.pleos.fe.vehicle.care

import ai.pleos.playground.navi.constants.CautionPointType
import ai.pleos.playground.navi.constants.TurnType
import ai.pleos.playground.navi.data.CautionPoint
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.SteeringWheelAngleListener
import android.util.Log
import com.pleos.fe.api.care.MotionRisk
import com.pleos.fe.api.care.MotionRiskModel
import com.pleos.fe.api.care.MotionSignals
import com.pleos.fe.api.care.RollingWindow
import com.pleos.fe.api.model.MotionSensitivity
import com.pleos.fe.api.model.TripSummaryRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs


class TripMonitor(
    private val vehicle: Vehicle?,
    private val navi: NaviHelper?,
    private val scope: CoroutineScope,
    /**
     * 회전 안내(TBT) 요청. onTBTInfo는 getTBTInfo()의 응답으로 오므로 주행 중 주기적으로 묻는다
     * (NAVI_ROUTE 권한). 주의 구간·도착은 경로 안내 중 내비가 먼저 보내 준다.
     */
    private val requestTbt: () -> Unit = { navi?.getTBTInfo() },
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val current = MutableStateFlow(MotionRiskModel.evaluate(MotionSignals()))
    val risk: StateFlow<MotionRisk> = current.asStateFlow()

    private val restArea = MutableStateFlow<Double?>(null)
    /** 경로상 다음 휴게소·졸음쉼터까지 남은 거리(m). 경로 안내 중이 아니면 null. */
    val restAreaAheadM: StateFlow<Double?> = restArea.asStateFlow()

    private val parkedState = MutableStateFlow(true)
    /**
     * P단(정차)인지. 주행 중에는 화면 조작을 받지 않으려고 쓴다.
     * 기어를 모르면(SDK 없음·응답 전) 정차로 본다. 트립은 착석+P단에서 시작하므로 처음엔 정차다.
     */
    val parked: StateFlow<Boolean> = parkedState.asStateFlow()

    private val arrivedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val arrivals: SharedFlow<Unit> = arrivedEvents.asSharedFlow()
    var sensitivity: MotionSensitivity = MotionSensitivity.MEDIUM
    private val brakes = RollingWindow(BRAKE_WINDOW_MS)
    private val steering = RollingWindow(STEERING_WINDOW_MS)
    private var lastAngle: Float? = null
    private var braking = false
    private var sharpTurnsAhead = 0
    private var bumpsAhead = 0
    private var nearestSharpTurnM: Double? = null
    private var startedAt = 0L
    private var lastTick = 0L
    private var congestedMs = 0L
    private var sharpTurnsPassed = 0
    private var peakRisk = 0
    private var ticker: Job? = null
    /** 시연 신호를 쓰는 중이면 실제 내비 응답(경로 안내 없음 = 빈 목록)이 시연 값을 덮지 않게 한다. */
    private var simulating = false

    private val brakeListener = object : BrakePedalStatusListener {
        override fun onBrakePedalStatusUpdated(pressed: Boolean?) {
            val now = pressed == true
            if (now && !braking) brakes.add(clock())
            braking = now
        }

        override fun onFailed(e: Exception) = warn("브레이크 상태", e)
    }

    private val steeringListener = object : SteeringWheelAngleListener {
        override fun onSteeringWheelAngleUpdated(angle: Float?) {
            angle ?: return
            lastAngle?.let { steering.add(clock(), abs(angle - it).toDouble()) }
            lastAngle = angle
        }

        override fun onFailed(e: Exception) = warn("핸들 각도", e)
    }

    private val gearListener = object : GearSelectionListener {
        override fun onSelected(gear: VehicleGear?) {
            parkedState.value = gear == null || gear == VehicleGear.GEAR_PARK
        }

        override fun onFailed(e: Exception) = warn("기어 상태", e)
    }

    private val naviListener = object : NaviHelperEventListener {
        override fun onTBTInfo(list: List<TBTInfo>) = handleTbt(list)

        override fun onCautionPointInfo(list: List<CautionPoint>) {
            if (simulating) return
            bumpsAhead = list.count { it.type in BUMPY && it.remainDistance <= AHEAD_M }
        }

        override fun onDestinationArrived(info: DestinationArrivedInfo) {
            arrivedEvents.tryEmit(Unit)
        }
    }

    /** 회전 안내 응답. 시연 중에는 무시한다. */
    internal fun handleTbt(list: List<TBTInfo>) {
        if (simulating) return
        val sharp = list.filter { it.type in SHARP_TURNS && it.distance <= AHEAD_M }
        sharpTurnsAhead = sharp.size
        // TBT에는 식별자가 없어, 가장 가까운 급커브가 100m 안으로 들어올 때 지나간 것으로 센다
        val nearest = sharp.minOfOrNull { it.distance }
        if (nearest != null && nearest < PASSED_M && (nearestSharpTurnM ?: Double.MAX_VALUE) >= PASSED_M) sharpTurnsPassed++
        nearestSharpTurnM = nearest
        restArea.value = list.filter { it.type in REST_STOPS }.minOfOrNull { it.distance }
    }

    fun start() {
        brakes.clear()
        steering.clear()
        lastAngle = null
        braking = false
        sharpTurnsAhead = 0
        bumpsAhead = 0
        nearestSharpTurnM = null
        restArea.value = null
        startedAt = clock()
        lastTick = startedAt
        congestedMs = 0
        sharpTurnsPassed = 0
        peakRisk = 0
        simulating = false
        parkedState.value = true

        vehicle?.let {
            runCatching { it.getGear().registerGearSelection(gearListener) }.onFailure { e -> warn("기어 구독", e) }
            runCatching { it.getBrake().registerBrakePedalStatus(brakeListener) }.onFailure { e -> warn("브레이크 구독", e) }
            runCatching { it.getSteeringWheel().registerSteeringWheelAngle(steeringListener) }.onFailure { e -> warn("핸들 구독", e) }
        }
        navi?.let { runCatching { it.addListener(naviListener) }.onFailure { e -> warn("내비 구독", e) } }

        ticker?.cancel()
        ticker = scope.launch {
            var ticks = 0
            while (isActive) {
                if (ticks++ % TBT_EVERY_TICKS == 0) runCatching(requestTbt).onFailure { e -> warn("회전 안내 요청", e) }
                tick()
                delay(TICK_MS)
            }
        }
    }

    fun stop(): TripSummaryRequest {
        ticker?.cancel()
        tick()
        vehicle?.let {
            runCatching { it.getGear().unregisterGearSelection(gearListener) }
            runCatching { it.getBrake().unregisterBrakePedalStatus(brakeListener) }
            runCatching { it.getSteeringWheel().unregisterSteeringWheelAngle(steeringListener) }
        }
        navi?.let { runCatching { it.removeListener(naviListener) } }
        return TripSummaryRequest(
            drivingMinutes = minutes(clock() - startedAt),
            congestionMinutes = minutes(congestedMs),
            sharpTurns = sharpTurnsPassed,
            peakRisk = peakRisk,
        )
    }

    /**
     * 시연용(디버그 화면에서만 부른다). 내비 경로 안내 없이는 오지 않는 신호(앞의 급커브·턱, 휴게소)와,
     * 시연 시간 안에 쌓기 번거로운 브레이크·핸들 변화를 흉내 낸다(브레이크·핸들은 Virtual Console로도 바꿀 수 있다).
     * 실제 신호와 같은 경로로 위험도를 다시 계산하므로 화면·요약도 똑같이 바뀐다.
     */
    fun simulate(
        turnsAhead: Int? = null,
        bumps: Int? = null,
        brakePresses: Int = 0,
        steeringDeg: Double = 0.0,
        restAreaM: Double? = null,
        passedTurns: Int = 0,
        elapsedMinutes: Int = 0,
        congestionMinutes: Int = 0,
    ) {
        val now = clock()
        simulating = true
        // 시연 시간을 줄이려고 이동·막힘 시간과 지나간 급커브를 앞당겨 쌓는다(요약에 그대로 반영)
        startedAt -= elapsedMinutes * 60_000L
        congestedMs += congestionMinutes * 60_000L
        sharpTurnsPassed += passedTurns
        turnsAhead?.let { sharpTurnsAhead = it }
        bumps?.let { bumpsAhead = it }
        repeat(brakePresses) { brakes.add(now) }
        if (steeringDeg > 0) steering.add(now, steeringDeg)
        restAreaM?.let { restArea.value = it }
        tick()
    }

    /** 시연용: 기어 신호 없이 주행(false)·정차(true) 화면을 보여 준다. 실제 기어가 바뀌면 그 값으로 덮인다. */
    fun simulateParked(parked: Boolean) {
        parkedState.value = parked
    }

    /** 시연용 신호를 모두 지운다. */
    fun resetSimulation() {
        sharpTurnsAhead = 0
        bumpsAhead = 0
        brakes.clear()
        steering.clear()
        restArea.value = null
        tick()
    }

    private fun tick() {
        val now = clock()
        val brakesPerMinute = brakes.perMinute(now)
        if (brakesPerMinute >= CONGESTED_BRAKES_PER_MIN) congestedMs += now - lastTick
        lastTick = now
        val risk = MotionRiskModel.evaluate(
            MotionSignals(
                sharpTurnsAhead = sharpTurnsAhead,
                bumpsAhead = bumpsAhead,
                brakesPerMinute = brakesPerMinute,
                steeringSwingDeg = steering.sum(now),
                drivingMinutes = minutes(now - startedAt),
                sensitivity = sensitivity,
            ),
        )
        peakRisk = maxOf(peakRisk, risk.score)
        current.value = risk
    }
    private fun minutes(ms: Long) = (ms / 60_000).toInt()
    private fun warn(what: String, e: Throwable) { Log.w(TAG, "$what 실패: ${e.message}") }

    private companion object {
        const val TAG = "TripMonitor"
        const val TICK_MS = 5_000L
        /** 회전 안내는 10초마다 묻는다(앞 2km 급커브 수는 그 사이 크게 변하지 않는다). */
        const val TBT_EVERY_TICKS = 2
        const val BRAKE_WINDOW_MS = 3 * 60_000L
        const val STEERING_WINDOW_MS = 60_000L
        const val AHEAD_M = 2_000.0
        const val PASSED_M = 100.0
        const val CONGESTED_BRAKES_PER_MIN = 4.0
        val SHARP_TURNS = setOf(
            TurnType.TURN_SHARP_R, TurnType.TURN_SHARP_L, TurnType.TURN_UTURN,
            TurnType.TURN_ENTER_ROUNDABOUT_02, TurnType.TURN_ENTER_ROUNDABOUT_03, TurnType.TURN_ENTER_ROUNDABOUT_04,
            TurnType.TURN_ENTER_ROUNDABOUT_06, TurnType.TURN_ENTER_ROUNDABOUT_08, TurnType.TURN_ENTER_ROUNDABOUT_09,
            TurnType.TURN_ENTER_ROUNDABOUT_10, TurnType.TURN_ENTER_ROUNDABOUT_12,
        )
        val REST_STOPS = setOf(TurnType.TURN_ENTER_SERVICEAREA, TurnType.TURN_RESTAREA)
        val BUMPY = setOf(
            CautionPointType.SPEED_BUMP, CautionPointType.DOWNHILL_CAUTION_AREA,
            CautionPointType.NARROW_ROAD, CautionPointType.FALLING_ROCK_AREA,
        )
    }
}