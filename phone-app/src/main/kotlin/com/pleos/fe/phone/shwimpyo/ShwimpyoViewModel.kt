package com.pleos.fe.phone.shwimpyo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pleos.fe.api.ApiError
import com.pleos.fe.api.ApiResult
import com.pleos.fe.api.apiCall
import com.pleos.fe.api.getOrElse
import com.pleos.fe.api.model.CheckInRequest
import com.pleos.fe.api.model.Mood
import com.pleos.fe.api.model.MyVehicleResponse
import com.pleos.fe.api.model.PairRequest
import com.pleos.fe.api.model.RiderConditionDto
import com.pleos.fe.api.model.TripResponse
import com.pleos.fe.api.model.UserResponse
import com.pleos.fe.api.model.VehicleDetailResponse
import com.pleos.fe.api.model.VehicleRole
import com.pleos.fe.api.onSuccess
import com.pleos.fe.phone.PhoneContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Phase { LOADING, SIGN_UP, PAIR, READY }

/** 쉼표 폰 앱이 그리는 값 전부. 화면은 이것만 보고 그린다. */
data class ShwimpyoState(
    val phase: Phase = Phase.LOADING,
    val me: UserResponse? = null,
    val vehicle: MyVehicleResponse? = null,
    /** 가족 멤버와 초대용 연결 코드. */
    val family: VehicleDetailResponse? = null,
    val trips: List<TripResponse> = emptyList(),
    val condition: RiderConditionDto = RiderConditionDto(),
    val busy: Boolean = false,
    val message: String? = null,
) {
    /** 끝났는데 내가 아직 체크인하지 않은 가장 최근 트립. 홈에서 "오늘 이동 어땠나요?"로 묻는다. */
    val pendingCheckIn: TripResponse? get() = trips.firstOrNull { it.endedAt != null && it.myMood == null }

    val isOwner: Boolean get() = family?.myRole == VehicleRole.OWNER
}

/**
 * 쉼표 폰 앱: 가입 → 차량 연결 → 트립 기록·도착 체크인·내 컨디션.
 * 서버 호출은 모두 apiCall로 감싸 ApiResult로 받고, 실패 문구는 서버의 한국어 detail을 그대로 보여준다.
 */
class ShwimpyoViewModel(private val container: PhoneContainer) : ViewModel() {

    private val current = MutableStateFlow(ShwimpyoState())
    val state: StateFlow<ShwimpyoState> = current.asStateFlow()

    init {
        load()
    }

    fun load() = launchBusy {
        if (!container.session.isSignedIn) {
            current.update { it.copy(phase = Phase.SIGN_UP) }
            return@launchBusy
        }
        val me = container.session.me().getOrElse { failure ->
            // 토큰이 무효(401)면 session.me()가 토큰을 지웠다 → 다시 가입
            val phase = if ((failure.error as? ApiError.Http)?.isUnauthorized == true) Phase.SIGN_UP else current.value.phase
            current.update { it.copy(phase = phase, message = failure.error.message) }
            return@launchBusy
        }
        current.update { it.copy(me = me) }
        showFirstVehicle()
    }

    fun signUp(nickname: String) = launchBusy {
        container.session.signUp(nickname.trim()).getOrElse { return@launchBusy fail(it) }
        load()
    }

    /** 차량 앱 화면에 뜬 연결 코드로 이 차의 가족이 된다. */
    fun pair(code: String) = launchBusy {
        apiCall { container.api.pair(PairRequest(code.trim().uppercase())) }.getOrElse { return@launchBusy fail(it) }
        showFirstVehicle()
    }

    fun refresh() = launchBusy {
        val vehicle = current.value.vehicle ?: return@launchBusy showFirstVehicle()
        loadVehicle(vehicle)
    }

    /** 도착 체크인. 기분 하나와 한 줄 메모. 다시 남기면 덮어쓴다. */
    fun checkIn(tripId: String, mood: Mood, note: String) = launchBusy {
        val saved = apiCall { container.api.checkIn(tripId, CheckInRequest(mood, note.trim().ifEmpty { null })) }
            .getOrElse { return@launchBusy fail(it) }
        current.update { s -> s.copy(trips = s.trips.map { if (it.id == saved.id) saved else it }, message = "남겼어요. 잠깐 쉼표.") }
    }

    fun saveCondition(condition: RiderConditionDto) = launchBusy {
        apiCall { container.api.updateMyCondition(condition) }
            .onSuccess { saved -> current.update { it.copy(condition = saved, message = "다음 이동부터 반영돼요") } }
            .getOrElse { return@launchBusy fail(it) }
    }

    /** 연결 코드 새로 받기(OWNER만). 이전 코드는 바로 막힌다. */
    fun regeneratePairingCode() = launchBusy {
        val vehicleId = current.value.vehicle?.id ?: return@launchBusy
        val code = apiCall { container.api.regeneratePairingCode(vehicleId) }.getOrElse { return@launchBusy fail(it) }
        current.update { s -> s.copy(family = s.family?.copy(pairingCode = code.pairingCode), message = "새 코드를 받았어요. 이전 코드는 이제 쓸 수 없어요") }
    }

    fun dismissMessage() = current.update { it.copy(message = null) }

    private suspend fun showFirstVehicle() {
        val vehicles = apiCall { container.api.vehicles() }.getOrElse { return fail(it) }
        val first = vehicles.firstOrNull()
        if (first == null) {
            current.update { it.copy(phase = Phase.PAIR, vehicle = null, trips = emptyList()) }
            return
        }
        loadVehicle(first)
    }

    private suspend fun loadVehicle(vehicle: MyVehicleResponse) {
        val trips = apiCall { container.api.trips(vehicle.id) }.getOrElse { return fail(it) }
        val condition = apiCall { container.api.myCondition() }.getOrElse { return fail(it) }
        val family = apiCall { container.api.vehicle(vehicle.id) }.getOrElse { return fail(it) }
        current.update { it.copy(phase = Phase.READY, vehicle = vehicle, trips = trips, condition = condition, family = family) }
    }

    private fun fail(failure: ApiResult.Failure) = current.update { it.copy(message = failure.error.message) }

    private fun launchBusy(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch {
            current.update { it.copy(busy = true) }
            try {
                block()
            } finally {
                current.update { it.copy(busy = false) }
            }
        }
    }
}
