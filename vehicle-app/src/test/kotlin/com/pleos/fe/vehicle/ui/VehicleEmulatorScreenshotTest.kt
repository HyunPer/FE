package com.pleos.fe.vehicle.ui

import android.app.Application
import androidx.compose.runtime.Composable
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
 * Pleos Connect 에뮬레이터(v2.1.0)의 앱 영역 크기(1680×1210dp, 실측)에서 화면이 크게 채워지는지 그린다.
 * 실제 앱처럼 ScaledSurface를 거친다. ./gradlew :vehicle-app:testDebugUnitTest → vehicle-app/build/screens/emulator_*.png
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w1680dp-h1210dp-land-mdpi")
class VehicleEmulatorScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private fun shot(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent { ScaledSurface(dark) { content() } }
        compose.mainClock.advanceTimeBy(300)
        compose.onRoot().captureRoboImage("build/screens/emulator_$name.png")
    }

    private val high = MotionRiskModel.evaluate(
        MotionSignals(sharpTurnsAhead = 4, bumpsAhead = 2, brakesPerMinute = 6.0, steeringSwingDeg = 420.0, drivingMinutes = 30, sensitivity = MotionSensitivity.HIGH),
    )

    @Test
    fun pairing() = shot("0_pairing") { WaitingScreen(VehicleSelfResponse("v1", "Pleos Connect", null, "482913", 0)) {} }

    @Test
    fun waiting() = shot("1_waiting") { WaitingScreen(VehicleSelfResponse("v1", "Pleos Connect", null, "482913", 3)) {} }

    @Test
    fun careHighMoving() = shot("2_care_high_moving") {
        CareScreen(high, onArrive = {}, restStyle = RestStyle.FRESH_AIR, restAreaAheadM = 12_000.0, parked = false)
    }

    @Test
    fun careParkedWithDemo() = shot("3_care_parked_demo") {
        CareScreen(MotionRiskModel.evaluate(MotionSignals(sharpTurnsAhead = 3, brakesPerMinute = 4.0)), onArrive = {}, restStyle = RestStyle.MUSIC, onDemo = {})
    }

    @Test
    fun waitingNight() = shot("5_waiting_night", dark = true) { WaitingScreen(VehicleSelfResponse("v1", "Pleos Connect", null, "482913", 3)) {} }

    @Test
    fun careNight() = shot("6_care_night", dark = true) {
        CareScreen(high, onArrive = {}, restStyle = RestStyle.FRESH_AIR, restAreaAheadM = 12_000.0, parked = false)
    }

    @Test
    fun arrival() = shot("4_arrival") {
        ArrivalScreen(TripResponse("t1", "v1", "2026-11-03T09:02:00Z", "2026-11-03T09:46:00Z", 46, 8, 4, 100), {})
    }
}
