package com.pleos.fe.vehicle

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.pleos.fe.vehicle.ui.VehicleRoot

/** 차량 앱 진입 화면. 상태는 VehicleController(앱 단위)가 들고 있어 화면이 다시 만들어져도 이어진다. */
class VehicleActivity : ComponentActivity() {

    private val container by lazy { vehicleContainer }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container.controller.start()
        setContent { VehicleRoot(container.controller, container::imageUrl) }
        if (savedInstanceState == null) container.gleo.handle(intent)
    }

    // singleTask라 앱이 떠 있을 때의 Gleo 호출은 여기로 온다.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        container.gleo.handle(intent)
    }

    // 착석+P단 감지는 앱이 앞에 있을 때만(시동 시 자동 실행은 서드파티에 허용되지 않음).
    override fun onStart() {
        super.onStart()
        container.controller.onForeground()
    }

    override fun onStop() {
        container.controller.onBackground()
        super.onStop()
    }
}
