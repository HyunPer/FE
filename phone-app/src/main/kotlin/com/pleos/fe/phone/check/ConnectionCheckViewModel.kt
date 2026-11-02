package com.pleos.fe.phone.check

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pleos.fe.api.ApiResult
import com.pleos.fe.api.apiCall
import com.pleos.fe.api.bodyOrNull
import com.pleos.fe.api.getOrElse
import com.pleos.fe.api.model.ApplyLogResponse
import com.pleos.fe.api.model.ClimateDto
import com.pleos.fe.api.model.MyVehicleResponse
import com.pleos.fe.api.model.PairRequest
import com.pleos.fe.api.model.ProfileRequest
import com.pleos.fe.api.model.ProfileResponse
import com.pleos.fe.api.model.ReservationResponse
import com.pleos.fe.api.model.ReserveRequest
import com.pleos.fe.api.model.SeatPositionRequest
import com.pleos.fe.api.model.ThemeResponse
import com.pleos.fe.api.model.UserResponse
import com.pleos.fe.api.onSuccess
import com.pleos.fe.api.requireSuccess
import com.pleos.fe.phone.PhoneContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** 연결 확인 화면이 그리는 값 전부. 화면은 이것만 보고 그린다. */
data class ConnectionCheckState(
    val me: UserResponse? = null,
    val vehicles: List<MyVehicleResponse> = emptyList(),
    val selectedVehicleId: String? = null,
    val profiles: List<ProfileResponse> = emptyList(),
    val selectedProfileId: String? = null,
    val reservation: ReservationResponse? = null,
    val history: List<ApplyLogResponse> = emptyList(),
    val themes: List<ThemeResponse> = emptyList(),
    val busy: Boolean = false,
    val log: List<String> = emptyList(),
) {
    val selectedVehicle: MyVehicleResponse? get() = vehicles.firstOrNull { it.id == selectedVehicleId }
    val selectedProfile: ProfileResponse? get() = profiles.firstOrNull { it.id == selectedProfileId }
}

/**
 * 폰 앱 ↔ BE 연동 확인용 ViewModel. 실제 화면의 ViewModel도 같은 모양으로 만든다:
 * 1. 서버 호출은 `apiCall { container.api.xxx() }`로 감싸 ApiResult로 받는다(예외가 화면까지 오지 않음).
 * 2. 결과로 StateFlow 상태를 갱신하고, 화면은 collectAsStateWithLifecycle()로 그린다.
 * 3. 실패 문구는 서버가 준 한국어 detail(error.message)을 그대로 보여준다.
 */
class ConnectionCheckViewModel(private val container: PhoneContainer) : ViewModel() {

    private val current = MutableStateFlow(ConnectionCheckState())
    val state: StateFlow<ConnectionCheckState> = current.asStateFlow()

    val baseUrl: String get() = container.baseUrl
    val isSignedIn: Boolean get() = container.session.isSignedIn

    init {
        if (isSignedIn) refreshAll()
    }

    // ── 계정 ──

    fun signUp(nickname: String) = run("가입 \"$nickname\"") {
        container.session.signUp(nickname).onSuccess { user ->
            current.update { it.copy(me = user) }
            refreshAllNow()
        }
    }

    fun signOut() {
        container.session.signOut()
        current.value = ConnectionCheckState(log = current.value.log)
        log("로그아웃: 저장된 토큰을 지웠습니다")
    }

    fun refreshAll() = run("새로고침") { refreshAllNow() }

    // ── 차량 ──

    /** 차량 앱 화면에 뜬 연결 코드로 이 차의 멤버가 된다. 처음 연결한 사람이 OWNER. */
    fun pair(code: String, carNickname: String) = run("페어링 $code") {
        apiCall { container.api.pair(PairRequest(code.trim().uppercase(), carNickname.ifBlank { null })) }.onSuccess { car ->
            current.update { it.copy(selectedVehicleId = car.id) }
            refreshAllNow()
        }
    }

    fun selectVehicle(id: String) = run("차량 선택") {
        current.update { it.copy(selectedVehicleId = id) }
        refreshVehicleNow(id)
    }

    // ── 프로필 ──

    fun selectProfile(id: String) = current.update { it.copy(selectedProfileId = id) }

    fun createProfile(name: String, temperature: Double) = run("프로필 만들기 \"$name\"") {
        val themeId = current.value.themes.firstOrNull()?.id
        apiCall { container.api.createProfile(ProfileRequest(name = name, climate = ClimateDto(temperature, 3), themeId = themeId)) }
            .onSuccess { created ->
                current.update { it.copy(profiles = it.profiles + created, selectedProfileId = created.id) }
            }
    }

    /** 선택한 차량에서 쓸 시트 메모리 슬롯. 차량마다 따로 저장된다. */
    fun saveSeatSlot(slot: Int) = run("시트 슬롯 $slot 저장") {
        val (vehicleId, profileId) = selection() ?: return@run notReady()
        apiCall { container.api.saveSeatPosition(profileId, vehicleId, SeatPositionRequest(memorySlot = slot)) }
    }

    // ── 예약·기록 ──

    fun reserve() = run("다음 운전자 예약") {
        val (vehicleId, profileId) = selection() ?: return@run notReady()
        apiCall { container.api.reserve(vehicleId, ReserveRequest(profileId)) }
            .onSuccess { r -> current.update { it.copy(reservation = r) } }
    }

    fun cancelReservation() = run("예약 취소") {
        val vehicleId = current.value.selectedVehicleId ?: return@run notReady()
        apiCall { container.api.cancelReservation(vehicleId).requireSuccess() }
            .onSuccess { current.update { it.copy(reservation = null) } }
    }

    fun refreshVehicle() = run("예약·탑승 기록 새로고침") {
        current.value.selectedVehicleId?.let { refreshVehicleNow(it) } ?: notReady()
    }

    // ── 내부 ──

    private suspend fun refreshAllNow(): ApiResult<Unit> {
        val me = container.session.me()
        if (me is ApiResult.Failure) return me
        val vehicles = apiCall { container.api.vehicles() }.getOrElse { return it }
        val profiles = apiCall { container.api.profiles() }.getOrElse { return it }
        val themes = apiCall { container.api.themes() }.getOrElse { return it }
        current.update { s ->
            s.copy(
                me = (me as ApiResult.Success).value,
                vehicles = vehicles,
                selectedVehicleId = s.selectedVehicleId?.takeIf { id -> vehicles.any { it.id == id } } ?: vehicles.firstOrNull()?.id,
                profiles = profiles,
                selectedProfileId = s.selectedProfileId?.takeIf { id -> profiles.any { it.id == id } } ?: profiles.firstOrNull()?.id,
                themes = themes,
            )
        }
        return current.value.selectedVehicleId?.let { refreshVehicleNow(it) } ?: ApiResult.Success(Unit)
    }

    private suspend fun refreshVehicleNow(vehicleId: String): ApiResult<Unit> {
        val reservation = apiCall { container.api.currentReservation(vehicleId).bodyOrNull() }.getOrElse { return it }
        val history = apiCall { container.api.applyHistory(vehicleId) }.getOrElse { return it }
        current.update { it.copy(reservation = reservation, history = history) }
        return ApiResult.Success(Unit)
    }

    private fun selection(): Pair<String, String>? {
        val s = current.value
        return s.selectedVehicleId?.let { v -> s.selectedProfileId?.let { p -> v to p } }
    }

    private fun notReady(): ApiResult<Unit> {
        log("  차량과 프로필을 먼저 고르세요")
        return ApiResult.Success(Unit)
    }

    /** 버튼 하나 = 작업 하나. 진행 중 표시와 ✓/✗ 로그를 남긴다. */
    private fun run(label: String, block: suspend () -> ApiResult<*>) {
        viewModelScope.launch {
            current.update { it.copy(busy = true) }
            when (val result = block()) {
                is ApiResult.Success -> log("✓ $label")
                is ApiResult.Failure -> log("✗ $label: ${result.error.message}")
            }
            current.update { it.copy(busy = false) }
        }
    }

    private fun log(text: String) {
        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        current.update { it.copy(log = (listOf("[$time] $text") + it.log).take(MAX_LOG)) }
    }

    private companion object {
        const val MAX_LOG = 50
    }
}
