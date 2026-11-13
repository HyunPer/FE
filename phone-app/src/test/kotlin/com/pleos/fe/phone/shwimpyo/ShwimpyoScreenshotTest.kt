package com.pleos.fe.phone.shwimpyo

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.pleos.fe.api.model.Mood
import com.pleos.fe.api.model.MotionSensitivity
import com.pleos.fe.api.model.MyVehicleResponse
import com.pleos.fe.api.model.RestStyle
import com.pleos.fe.api.model.RiderConditionDto
import com.pleos.fe.api.model.TripResponse
import com.pleos.fe.api.model.UserResponse
import com.pleos.fe.api.model.VehicleDetailResponse
import com.pleos.fe.api.model.VehicleMemberResponse
import com.pleos.fe.api.model.VehicleRole
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.pleos.fe.phone.ui.BottomNav
import com.pleos.fe.phone.ui.DarkPalette
import com.pleos.fe.phone.ui.LightPalette
import com.pleos.fe.phone.ui.PhoneSurface
import com.pleos.fe.phone.ui.PhoneTab
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.ZoneId

/** 쉼표 폰 화면을 PNG로 그린다(412×915dp). ./gradlew :phone-app:testDebugUnitTest → phone-app/build/screens/ */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w412dp-h915dp-port-xhdpi")
class ShwimpyoScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val trips = listOf(
        TripResponse("t3", "v1", "2026-11-03T09:02:00Z", "2026-11-03T09:46:00Z", 44, 18, 6, 72),
        TripResponse("t2", "v1", "2026-11-02T23:10:00Z", "2026-11-02T23:38:00Z", 28, 4, 1, 21, Mood.TIRED, "야근 끝, 조용히 오고 싶었다"),
        TripResponse("t1", "v1", "2026-11-01T01:30:00Z", "2026-11-01T03:05:00Z", 95, 12, 14, 81, Mood.QUEASY, "아이가 뒷자리에서 멀미"),
    )

    private val family = VehicleDetailResponse(
        id = "v1",
        model = "CASPER Electric",
        nickname = "우리집 캐스퍼",
        myRole = VehicleRole.OWNER,
        members = listOf(
            VehicleMemberResponse("u1", "엄마", VehicleRole.OWNER, "2026-10-30T00:00:00Z"),
            VehicleMemberResponse("u2", "아빠", VehicleRole.MEMBER, "2026-10-30T00:05:00Z"),
            VehicleMemberResponse("u3", "지우", VehicleRole.MEMBER, "2026-10-30T00:10:00Z"),
        ),
        pairingCode = "482913",
    )

    private val ready = ShwimpyoState(
        phase = Phase.READY,
        me = UserResponse("u1", "엄마", "2026-10-30T00:00:00Z"),
        vehicle = MyVehicleResponse("v1", "CASPER Electric", "우리집 캐스퍼", VehicleRole.OWNER, "2026-10-30T00:00:00Z"),
        trips = trips,
        condition = RiderConditionDto(MotionSensitivity.HIGH, RestStyle.FRESH_AIR),
    )

    private fun shot(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent { PhoneSurface(if (dark) DarkPalette else LightPalette) { content() } }
        compose.mainClock.advanceTimeBy(300)
        compose.onRoot().captureRoboImage("build/screens/shwimpyo_$name.png")
    }

    /** 실제 앱처럼 아래에 탭 줄을 붙여 그린다. */
    @Composable
    private fun WithNav(tab: PhoneTab, content: @Composable () -> Unit) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) { content() }
            BottomNav(tab)
        }
    }

    @Test
    fun signUp() = shot("1_sign_up") { SignUpScreen(false) {} }

    @Test
    fun pair() = shot("2_pair") { PairScreen("엄마", false) {} }

    @Test
    fun home() = shot("3_home_check_in") { WithNav(PhoneTab.HOME) { HomeScreen(ready, { _, _, _ -> }, {}) } }

    @Test
    fun homeNight() = shot("7_home_night", dark = true) { WithNav(PhoneTab.HOME) { HomeScreen(ready, { _, _, _ -> }, {}) } }

    @Test
    fun signUpNight() = shot("8_sign_up_night", dark = true) { SignUpScreen(false) {} }

    @Test
    fun historyNight() = shot("9_history_night", dark = true) {
        HistoryScreen(trips, now = Instant.parse("2026-11-05T03:00:00Z"), zone = ZoneId.of("Asia/Seoul"))
    }

    @Test
    fun history() = shot("4_history") {
        // 2026-11-05(목) 기준 이번 주: 11-03 아침(피곤)·저녁 이동. 11-01(지난주 일요일)은 빠진다
        HistoryScreen(trips, now = Instant.parse("2026-11-05T03:00:00Z"), zone = ZoneId.of("Asia/Seoul"))
    }

    @Test
    fun family() = shot("6_family") { FamilyScreen(family, isOwner = true, busy = false) {} }

    @Test
    fun condition() = shot("5_condition") { ConditionScreen(ready.condition, false) {} }
}
