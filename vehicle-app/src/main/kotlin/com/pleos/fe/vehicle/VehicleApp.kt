package com.pleos.fe.vehicle

import android.app.Application
import android.content.Context
import android.util.Log
import com.pleos.fe.api.PleosApi
import com.pleos.fe.api.VehicleApi
import com.pleos.fe.api.VehicleSession
import com.pleos.fe.api.assetUrl
import com.pleos.fe.api.flow.VehicleApplyFlow
import com.pleos.fe.api.flow.VehicleControl
import com.pleos.fe.api.flow.WelcomeRenderer
import com.pleos.fe.api.model.VehicleSettingsDto
import com.pleos.fe.api.model.WelcomeResponse
import com.pleos.fe.core.android.SecureTokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VehicleApp : Application() {

    lateinit var container: VehicleContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = VehicleContainer(this)
    }
}

/**
 * 차량 앱의 서버 연결 묶음.
 * - 첫 실행: session.ensureRegistered(차종) → 페어링 코드를 화면에 띄운다(정차 중).
 * - 탑승 트리거(앱 열림·착석/도어 이벤트·음성 버튼): applyFlow.onBoarding()
 *   → Welcomed / ChooseDriver(1탭 선택 화면) / Failed(단계별 재시도)
 */
class VehicleContainer(context: Context) {
    val baseUrl: String = BuildConfig.API_BASE_URL
    private val tokens = SecureTokenStore(context, "vehicle_session")
    val api: VehicleApi = PleosApi.vehicle(baseUrl, tokens)
    val session = VehicleSession(api, tokens)

    /** 화면(Compose 등)은 welcome.state를 구독해 스킨·위젯·인사말을 그린다. */
    val welcome = WelcomeState()

    /** Pleos SDK를 붙이면 LogOnlyVehicleControl을 실제 구현으로 바꾼다. */
    val applyFlow = VehicleApplyFlow(api, LogOnlyVehicleControl(), welcome)

    fun imageUrl(path: String) = assetUrl(baseUrl, path)
}

val Context.vehicleContainer: VehicleContainer get() = (applicationContext as VehicleApp).container

/**
 * Pleos Vehicle SDK 연결 전 자리. 무엇을 적용할지만 로그로 남긴다.
 * 실제 구현은 VehicleControl 주석의 매핑대로 SDK를 호출한다.
 */
class LogOnlyVehicleControl : VehicleControl {
    override suspend fun apply(settings: VehicleSettingsDto) {
        Log.i(TAG, "차량 설정 적용(SDK 미연결): $settings")
    }

    private companion object {
        const val TAG = "VehicleControl"
    }
}

/** 2단계 웰컴 묶음을 화면 상태로 내보낸다. 앰비언트 라이트·사운드는 SDK 연결 때 여기서 이어서 처리한다. */
class WelcomeState : WelcomeRenderer {
    private val current = MutableStateFlow<WelcomeResponse?>(null)
    val state: StateFlow<WelcomeResponse?> = current.asStateFlow()

    override suspend fun render(welcome: WelcomeResponse) {
        current.value = welcome
    }
}
