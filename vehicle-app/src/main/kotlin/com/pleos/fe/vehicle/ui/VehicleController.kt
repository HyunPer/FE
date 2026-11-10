package com.pleos.fe.vehicle.ui

import com.pleos.fe.api.ApiResult
import com.pleos.fe.api.apiCall
import com.pleos.fe.api.care.MotionRisk
import com.pleos.fe.api.getOrElse
import com.pleos.fe.api.model.MotionSensitivity
import com.pleos.fe.api.model.RestStyle
import com.pleos.fe.api.model.TripResponse
import com.pleos.fe.api.model.TripSummaryRequest
import com.pleos.fe.api.model.VehicleSelfResponse
import com.pleos.fe.vehicle.VehicleContainer
import com.pleos.fe.vehicle.care.TripMonitor
import com.pleos.fe.vehicle.sdk.BoardingTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** 차량 앱 화면: 연결 코드 → 대기 → (탑승) 주행 중 케어 → (도착) 요약. */
sealed interface VehicleScreen {
    data object Loading : VehicleScreen

    /** 멤버가 없으면 연결 코드를 크게, 있으면 대기 화면. */
    data class Waiting(val self: VehicleSelfResponse) : VehicleScreen

    /**
     * 주행 중. 멀미 위험도와 이유를 보여준다.
     * [restStyle]은 가장 멀미를 잘 타는 가족이 고른 쉬는 방식, [restAreaAheadM]은 경로상 다음 휴게소까지 거리,
     * [parked]는 P단 여부(주행 중에는 버튼을 숨긴다).
     */
    data class Driving(
        val tripId: String,
        val risk: MotionRisk,
        val restStyle: RestStyle = RestStyle.QUIET,
        val restAreaAheadM: Double? = null,
        val parked: Boolean = true,
    ) : VehicleScreen

    /** 도착. 주행 요약을 보여주고 폰 체크인으로 이어준다. */
    data class Arrived(val trip: TripResponse) : VehicleScreen

    data class Failed(val message: String, val retry: () -> Unit) : VehicleScreen
}

/** 시연 패널에서 누르는 도로 신호. */
enum class DemoSignal(val label: String) {
    CURVES("급커브 앞"),
    STOP_AND_GO("가다 서다"),
    WINDING("구불길"),
    REST_AREA("휴게소 12km"),
    RESET("초기화"),
    STORY("▶ 가족 나들이"),
}

/**
 * 자동 시연 "가족 나들이": 출발 → 가다 서다 → 급커브 예고 → 구불길 → 휴게소 안내 → 지나감 → 도착.
 * 약 1분. 각 단계는 실제 신호와 같은 계산 경로(TripMonitor.simulate)를 탄다.
 */
internal object DemoStory {
    data class Step(val afterMs: Long, val caption: String, val apply: (TripMonitor) -> Unit)

    val steps = listOf(
        Step(0, "출발") { it.resetSimulation(); it.simulateParked(false); it.simulate(elapsedMinutes = 12) },
        Step(6_000, "시내를 빠져나가는 중") { it.simulate(brakePresses = 18, congestionMinutes = 8, elapsedMinutes = 8) },
        Step(10_000, "산길 앞") { it.simulate(turnsAhead = 4, bumps = 2, elapsedMinutes = 10) },
        Step(12_000, "구불구불한 길") { it.simulate(steeringDeg = 420.0) },
        Step(8_000, "휴게소 안내") { it.simulate(restAreaM = 12_000.0) },
        Step(14_000, "산길을 지남") { it.resetSimulation(); it.simulate(passedTurns = 4, elapsedMinutes = 15) },
        Step(8_000, "도착") { it.simulateParked(true) },
    )
}

class VehicleController(
    private val container: VehicleContainer,
    private val scope: CoroutineScope,
) {
    private val current = MutableStateFlow<VehicleScreen>(VehicleScreen.Loading)
    val screen: StateFlow<VehicleScreen> = current.asStateFlow()

    private val monitor = container.tripMonitor(scope)
    private var self: VehicleSelfResponse? = null
    private var work: Job? = null
    private var pairingPoll: Job? = null
    private var tripWatch: Job? = null
    private var trigger: BoardingTrigger? = null
    private var story: Job? = null

    fun start() {
        if (work == null) register()
    }

    /** 착석+P단 감지는 앱이 앞에 있을 때만(시동 시 자동 실행은 서드파티에 허용되지 않음). */
    fun onForeground() {
        if (trigger != null) return
        trigger = container.boardingTrigger(scope) {
            if ((current.value as? VehicleScreen.Waiting)?.self?.memberCount ?: 0 > 0) startTrip().join()
        }?.also { it.start() }
    }

    fun onBackground() {
        trigger?.stop()
        trigger = null
    }

    fun register(): Job = scope.launch {
        current.value = VehicleScreen.Loading
        when (val r = container.session.ensureRegistered(MODEL)) {
            is ApiResult.Success -> showWaiting(r.value)
            is ApiResult.Failure -> current.value = VehicleScreen.Failed("서버에 연결하지 못했습니다.\n${r.error.message}") { register() }
        }
    }.also { work = it }

    /** 탑승. 가족 중 가장 멀미를 잘 타는 사람 기준으로 위험도를 계산한다. */
    fun startTrip(): Job = scope.launch {
        val trip = apiCall { container.api.startTrip() }.getOrElse {
            current.value = VehicleScreen.Failed("트립을 시작하지 못했습니다.\n${it.error.message}") { startTrip() }
            return@launch
        }
        val riders = (apiCall { container.api.riders() } as? ApiResult.Success)?.value.orEmpty()
        val mostSensitive = riders.maxByOrNull { it.condition.motionSensitivity }?.condition
        monitor.sensitivity = mostSensitive?.motionSensitivity ?: MotionSensitivity.MEDIUM
        val restStyle = mostSensitive?.restStyle ?: RestStyle.QUIET
        monitor.start()

        tripWatch?.cancel()
        tripWatch = scope.launch {
            launch {
                combine(monitor.risk, monitor.restAreaAheadM, monitor.parked) { risk, restArea, parked ->
                    VehicleScreen.Driving(trip.id, risk, restStyle, restArea, parked)
                }.collect { current.value = it }
            }
            monitor.arrivals.first()
            finishTrip()
        }
    }

    /** 도착(내비 도착 알림 또는 버튼). 주행 요약을 서버에 남긴다. */
    fun finishTrip() {
        val driving = current.value as? VehicleScreen.Driving ?: return
        story?.cancel()
        tripWatch?.cancel()
        submitFinish(driving.tripId, monitor.stop())
    }

    /** 시연용(디버그 빌드 화면에서만): 에뮬레이터에서 바꿀 수 없는 도로 신호를 흉내 낸다. */
    fun demo(signal: DemoSignal) {
        if (current.value !is VehicleScreen.Driving) return
        when (signal) {
            DemoSignal.CURVES -> monitor.simulate(turnsAhead = 4, bumps = 2)
            DemoSignal.STOP_AND_GO -> monitor.simulate(brakePresses = 18)
            DemoSignal.WINDING -> monitor.simulate(steeringDeg = 420.0)
            DemoSignal.REST_AREA -> monitor.simulate(restAreaM = 12_000.0)
            DemoSignal.RESET -> monitor.resetSimulation()
            DemoSignal.STORY -> playStory()
        }
    }

    private fun playStory() {
        story?.cancel()
        story = scope.launch {
            DemoStory.steps.forEach { step ->
                delay(step.afterMs)
                step.apply(monitor)
            }
            finishTrip()
        }
    }

    /** 도착 요약을 닫고 대기 화면으로. */
    fun done() {
        self?.let(::showWaiting) ?: register()
    }

    private fun submitFinish(tripId: String, summary: TripSummaryRequest): Job = scope.launch {
        current.value = when (val r = apiCall { container.api.finishTrip(tripId, summary) }) {
            is ApiResult.Success -> VehicleScreen.Arrived(r.value)
            is ApiResult.Failure -> VehicleScreen.Failed("도착 기록을 저장하지 못했습니다.\n다시 시도하면 같은 기록으로 이어서 저장합니다.") {
                submitFinish(tripId, summary)
            }
        }
    }

    private fun showWaiting(vehicle: VehicleSelfResponse) {
        self = vehicle
        current.value = VehicleScreen.Waiting(vehicle)
        if (vehicle.memberCount == 0) pollUntilPaired()
    }

    /** 연결 코드를 띄운 동안 폰에서 페어링하면 화면이 저절로 바뀌도록 주기적으로 확인한다. */
    private fun pollUntilPaired() {
        pairingPoll?.cancel()
        pairingPoll = scope.launch {
            while (isActive) {
                delay(PAIRING_POLL_MS)
                if (current.value !is VehicleScreen.Waiting) return@launch
                val latest = (apiCall { container.api.me() } as? ApiResult.Success)?.value ?: continue
                if (latest.memberCount > 0) {
                    showWaiting(latest)
                    return@launch
                }
            }
        }
    }

    private companion object {
        const val MODEL = "Pleos Connect"
        const val PAIRING_POLL_MS = 5_000L
    }
}
