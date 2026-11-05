package com.pleos.fe.vehicle.ui

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.pleos.fe.api.care.MotionRiskModel
import com.pleos.fe.api.care.MotionSignals
import com.pleos.fe.api.model.MotionSensitivity
import com.pleos.fe.api.model.RestStyle
import com.pleos.fe.api.model.TripResponse
import com.pleos.fe.api.model.VehicleSelfResponse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 세로 화면(폼 팩터 테스트의 12.9″ 세로를 800×1280dp로 근사)에서 차량 화면이 위아래로 놓이는지 그린다.
 * ./gradlew :vehicle-app:testDebugUnitTest → vehicle-app/build/screens/portrait_*.png
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w800dp-h1280dp-port-mdpi")
class VehiclePortraitScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private fun shot(name: String, advanceMs: Long = 300, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent { Box(Modifier.fillMaxSize().background(Color(0xFF0C1016))) { content() } }
        compose.mainClock.advanceTimeBy(advanceMs)
        compose.onRoot().captureRoboImage("build/screens/portrait_$name.png")
    }

    @Test
    fun pairing() = shot("1_pairing") {
        WaitingScreen(VehicleSelfResponse("v1", "CASPER Electric", null, "482913", 0)) {}
    }

    @Test
    fun careHigh() = shot("2_care_high") {
        CareScreen(
            MotionRiskModel.evaluate(
                MotionSignals(sharpTurnsAhead = 4, bumpsAhead = 2, steeringSwingDeg = 420.0, drivingMinutes = 50, sensitivity = MotionSensitivity.HIGH),
            ),
            onArrive = {},
            restStyle = RestStyle.FRESH_AIR,
            restAreaAheadM = 12_000.0,
            onDemo = {},
        )
    }

    @Test
    fun arrival() = shot("3_arrival") {
        ArrivalScreen(TripResponse("t1", "v1", "2026-11-03T09:02:00Z", "2026-11-03T09:46:00Z", 44, 18, 6, 78), {})
    }

    @Test
    fun breathing() = shot("4_breathing", advanceMs = 2_500) { BreathingScreen {} }
}
