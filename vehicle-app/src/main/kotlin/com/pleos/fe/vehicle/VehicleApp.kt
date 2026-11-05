package com.pleos.fe.vehicle

import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.vehicle.Vehicle
import android.app.Application
import android.content.Context
import android.util.Log
import com.pleos.fe.api.PleosApi
import com.pleos.fe.api.VehicleApi
import com.pleos.fe.api.VehicleSession
import com.pleos.fe.core.android.SecureTokenStore
import com.pleos.fe.vehicle.care.TripMonitor
import com.pleos.fe.vehicle.sdk.BoardingTrigger
import com.pleos.fe.vehicle.ui.VehicleController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

class VehicleApp : Application() {

    lateinit var container: VehicleContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = VehicleContainer(this)
    }
}

/**
 * 차량 앱의 서버·SDK 연결 묶음.
 * - 첫 실행: session.ensureRegistered(차종) → 연결 코드를 화면에 띄운다(정차 중).
 * - 쉼표: 차량 상태 조회(Vehicle SDK) + 내비 정보(NaviHelper)로 트립을 지켜본다.
 */
class VehicleContainer(context: Context) {
    val baseUrl: String = BuildConfig.API_BASE_URL
    private val tokens = SecureTokenStore(context, "vehicle_session")
    val api: VehicleApi = PleosApi.vehicle(baseUrl, tokens)
    val session = VehicleSession(api, tokens)

    /** Pleos Vehicle SDK. Pleos Connect 에뮬레이터·실차에서만 연결되고, 일반 기기면 null. */
    val sdk: Vehicle? = runCatching { Vehicle(context.applicationContext).also { it.initialize() } }
        .onFailure { Log.w("VehicleContainer", "Vehicle SDK 연결 실패: ${it.message}") }
        .getOrNull()

    /** Pleos NaviHelper(내비 연동). 내비 앱이 없는 기기면 null이고, 트립은 차량 신호만으로 계산한다. */
    val navi: NaviHelper? = runCatching { NaviHelper(context.applicationContext).also { it.initialize() } }
        .onFailure { Log.w("VehicleContainer", "NaviHelper 연결 실패: ${it.message}") }
        .getOrNull()

    fun tripMonitor(scope: CoroutineScope) = TripMonitor(sdk, navi, scope)

    /** 화면 상태는 앱 단위로 둔다(화면 회전·재생성에도 이어지게). */
    private val appScope = MainScope()
    val controller: VehicleController by lazy { VehicleController(this, appScope) }

    /** 착석 + P단 감지 트리거. SDK가 없으면 null. */
    fun boardingTrigger(scope: CoroutineScope, onBoarding: suspend () -> Unit): BoardingTrigger? =
        sdk?.let { BoardingTrigger(it, scope, onBoarding) }
}

val Context.vehicleContainer: VehicleContainer get() = (applicationContext as VehicleApp).container
