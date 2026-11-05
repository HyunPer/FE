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
 * 쉼표 차량 화면을 PNG로 그린다. 10.25인치 가로 화면(1920×720px)을 hdpi로 보고 1280×480dp에 그린다.
 * ./gradlew :vehicle-app:testDebugUnitTest → vehicle-app/build/screens/
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w1280dp-h480dp-land-hdpi")
class VehicleScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val car = VehicleSelfResponse("v1", "CASPER Electric", "우리집 캐스퍼", "482913", 3)

    private fun shot(name: String, advanceMs: Long = 300, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent { Box(Modifier.fillMaxSize().background(Color(0xFF0C1016))) { content() } }
        compose.mainClock.advanceTimeBy(advanceMs)
        compose.onRoot().captureRoboImage("build/screens/vehicle_$name.png")
    }

    @Test
    fun pairing() = shot("1_pairing") { WaitingScreen(car.copy(nickname = null, memberCount = 0)) {} }

    @Test
    fun waiting() = shot("2_waiting") { WaitingScreen(car) {} }

    @Test
    fun careLow() = shot("3_care_low") { CareScreen(MotionRiskModel.evaluate(MotionSignals(drivingMinutes = 12)), {}) }

    @Test
    fun careCaution() = shot("4_care_caution") {
        CareScreen(MotionRiskModel.evaluate(MotionSignals(sharpTurnsAhead = 3, brakesPerMinute = 4.0)), {}, RestStyle.MUSIC)
    }

    @Test
    fun careHigh() = shot("5_care_high_demo") {
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
    fun careHighMoving() = shot("5b_care_high_moving") {
        CareScreen(
            MotionRiskModel.evaluate(MotionSignals(sharpTurnsAhead = 4, steeringSwingDeg = 420.0, sensitivity = MotionSensitivity.HIGH)),
            onArrive = {},
            restStyle = RestStyle.BREATHING,
            parked = false, // 주행 중: 버튼·시연 패널 없이 안내 문구만
            onDemo = {},
        )
    }

    @Test
    fun arrival() = shot("6_arrival") {
        ArrivalScreen(TripResponse("t1", "v1", "2026-11-03T09:02:00Z", "2026-11-03T09:46:00Z", 44, 18, 6, 78), {})
    }

    @Test
    fun breathing() = shot("7_breathing", advanceMs = 2_500) { BreathingScreen {} }
}
