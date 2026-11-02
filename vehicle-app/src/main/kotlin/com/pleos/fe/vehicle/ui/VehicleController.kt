package com.pleos.fe.vehicle.ui

import com.pleos.fe.api.ApiResult
import com.pleos.fe.api.apiCall
import com.pleos.fe.api.flow.BoardingOutcome
import com.pleos.fe.api.model.ApplicableProfileDto
import com.pleos.fe.api.model.DriverProfilesResponse
import com.pleos.fe.api.model.VehicleSelfResponse
import com.pleos.fe.api.model.WelcomeResponse
import com.pleos.fe.vehicle.VehicleContainer
import com.pleos.fe.vehicle.sdk.ApplyItem
import com.pleos.fe.vehicle.sdk.BoardingTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** 차량 앱 화면. 와이어프레임의 차량 쪽 흐름: 연결 코드 → 대기 → 적용 중 → (1탭 선택) → 웰컴 → 테마 홈. */
sealed interface VehicleScreen {
    data object Loading : VehicleScreen

    /** 멤버가 없으면 연결 코드를 크게, 있으면 "탑승하면 자동 적용"을 보여준다. */
    data class Waiting(val self: VehicleSelfResponse) : VehicleScreen

    data class Applying(val driverNickname: String?, val step: BoardingOutcome.Step) : VehicleScreen

    data class ChooseDriver(val drivers: List<DriverProfilesResponse>) : VehicleScreen

    data class Welcome(val welcome: WelcomeResponse, val items: List<ApplyItem>) : VehicleScreen

    data class Home(val welcome: WelcomeResponse, val items: List<ApplyItem>) : VehicleScreen

    data class Failed(val message: String, val retry: () -> Unit) : VehicleScreen
}

/**
 * 화면 상태와 사용자 동작. 실제 순서(차량 설정 → 보고 → 웰컴)는 VehicleApplyFlow가 지키고,
 * 여기서는 그 결과를 화면으로 옮긴다. 착석+P단 트리거는 앱이 앞에 있을 때만 켠다.
 */
class VehicleController(
    private val container: VehicleContainer,
    private val scope: CoroutineScope,
) {
    private val current = MutableStateFlow<VehicleScreen>(VehicleScreen.Loading)
    val screen: StateFlow<VehicleScreen> = current.asStateFlow()

    private var self: VehicleSelfResponse? = null
    private var work: Job? = null
    private var pairingPoll: Job? = null
    private var welcomeTimer: Job? = null
    private var trigger: BoardingTrigger? = null

    fun start() {
        if (work == null) register()
    }

    fun onForeground() {
        if (trigger != null) return
        trigger = container.boardingTrigger(scope) { if (canAutoBoard()) board().join() }?.also { it.start() }
    }

    fun onBackground() {
        trigger?.stop()
        trigger = null
    }

    fun register(): Job = launchWork {
        current.value = VehicleScreen.Loading
        when (val r = container.session.ensureRegistered(MODEL)) {
            is ApiResult.Success -> showWaiting(r.value)
            is ApiResult.Failure -> current.value = VehicleScreen.Failed("서버에 연결하지 못했습니다.\n${r.error.message}") { register() }
        }
    }

    /** 예약된 다음 운전자를 적용한다. 예약이 없으면 1탭 선택 화면으로. */
    fun board(): Job = launchWork {
        current.value = VehicleScreen.Applying(null, BoardingOutcome.Step.FETCH)
        val outcome = container.applyFlow.onBoarding { current.value = VehicleScreen.Applying(null, it) }
        show(outcome) { board() }
    }

    fun pickDriver(): Job = launchWork {
        current.value = VehicleScreen.Loading
        when (val r = apiCall { container.api.driverProfiles() }) {
            is ApiResult.Success -> showDrivers(r.value)
            is ApiResult.Failure -> current.value = VehicleScreen.Failed("가족 프로필을 불러오지 못했습니다.\n${r.error.message}") { pickDriver() }
        }
    }

    fun choose(driverNickname: String, profile: ApplicableProfileDto): Job = launchWork {
        current.value = VehicleScreen.Applying(driverNickname, BoardingOutcome.Step.VEHICLE_SETTINGS)
        val outcome = container.applyFlow.applySelected(profile) { current.value = VehicleScreen.Applying(driverNickname, it) }
        show(outcome) { choose(driverNickname, profile) }
    }

    /** 웰컴 화면을 건너뛰고 테마 홈으로. 웰컴은 몇 초 뒤 저절로 넘어간다. */
    fun goHome() {
        val welcome = current.value as? VehicleScreen.Welcome ?: return
        welcomeTimer?.cancel()
        current.value = VehicleScreen.Home(welcome.welcome, welcome.items)
    }

    fun reset() {
        self?.let(::showWaiting) ?: register()
    }

    private fun show(outcome: BoardingOutcome, retry: () -> Unit) {
        current.value = when (outcome) {
            is BoardingOutcome.Welcomed -> {
                welcomeTimer = scope.launch {
                    delay(WELCOME_MS)
                    goHome()
                }
                VehicleScreen.Welcome(outcome.welcome, container.lastApplyItems())
            }
            is BoardingOutcome.ChooseDriver -> return showDrivers(outcome.drivers)
            is BoardingOutcome.Failed -> VehicleScreen.Failed(failureMessage(outcome), retry)
        }
    }

    private fun showDrivers(drivers: List<DriverProfilesResponse>) {
        current.value = if (drivers.all { it.profiles.isEmpty() }) {
            VehicleScreen.Failed("아직 프로필이 없습니다.\n폰 앱에서 내 프로필을 만들어 주세요.") { pickDriver() }
        } else {
            VehicleScreen.ChooseDriver(drivers)
        }
    }

    private fun showWaiting(vehicle: VehicleSelfResponse) {
        self = vehicle
        current.value = VehicleScreen.Waiting(vehicle)
        if (vehicle.memberCount == 0) pollUntilPaired()
    }

    /** 연결 코드를 띄운 동안 폰에서 페어링하면 대기 화면이 저절로 바뀌도록 주기적으로 확인한다. */
    private fun pollUntilPaired() {
        pairingPoll?.cancel()
        pairingPoll = scope.launch {
            while (isActive) {
                delay(PAIRING_POLL_MS)
                if (current.value !is VehicleScreen.Waiting) return@launch
                val latest = (apiCall { container.api.me() } as? ApiResult.Success)?.value ?: continue
                if (latest.memberCount > 0) {
                    self = latest
                    current.value = VehicleScreen.Waiting(latest)
                    return@launch
                }
            }
        }
    }

    /** 적용 중이거나 운전자를 고르는 중에는 착석 트리거가 끼어들지 않는다. */
    private fun canAutoBoard(): Boolean = when (val s = current.value) {
        is VehicleScreen.Waiting -> s.self.memberCount > 0
        is VehicleScreen.Home, is VehicleScreen.Failed -> true
        else -> false
    }

    private fun launchWork(block: suspend CoroutineScope.() -> Unit): Job {
        work?.cancel()
        welcomeTimer?.cancel()
        return scope.launch(block = block).also { work = it }
    }

    private fun failureMessage(failed: BoardingOutcome.Failed): String {
        val reason = failed.cause.message.orEmpty()
        return when (failed.step) {
            BoardingOutcome.Step.FETCH -> "서버에서 운전자 정보를 받지 못했습니다.\n$reason"
            BoardingOutcome.Step.VEHICLE_SETTINGS -> "차량 설정을 적용하지 못했습니다.\n주행 중에는 시트·공조를 바꿀 수 없습니다."
            BoardingOutcome.Step.REPORT -> "차량 설정은 적용됐지만 서버에 알리지 못했습니다.\n다시 시도하면 이어서 진행합니다."
            BoardingOutcome.Step.WELCOME -> "테마를 적용하지 못했습니다.\n$reason"
        }
    }

    private companion object {
        const val MODEL = "Pleos Connect"
        const val WELCOME_MS = 4_000L
        const val PAIRING_POLL_MS = 5_000L
    }
}
