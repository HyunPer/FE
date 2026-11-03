package com.pleos.fe.phone.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.pleos.fe.api.model.ApplyLogResponse
import com.pleos.fe.api.model.ApplySource
import com.pleos.fe.api.model.ButtonStyle
import com.pleos.fe.api.model.ClimateDto
import com.pleos.fe.api.model.DisplayDto
import com.pleos.fe.api.model.FontStyle
import com.pleos.fe.api.model.LightDto
import com.pleos.fe.api.model.ProfileResponse
import com.pleos.fe.api.model.ReservationResponse
import com.pleos.fe.api.model.ReservationStatus
import com.pleos.fe.api.model.SeatDto
import com.pleos.fe.api.model.ThemeColorsDto
import com.pleos.fe.api.model.ThemeSkinDto
import com.pleos.fe.api.model.VehicleDetailResponse
import com.pleos.fe.api.model.VehicleMemberResponse
import com.pleos.fe.api.model.VehicleRole
import com.pleos.fe.api.model.WidgetStyle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 폰 앱 화면 시안을 PNG로 그린다(412×915dp). 결과: phone-app/build/screens/ */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w412dp-h915dp-port-xhdpi")
class PhoneScreenSnapshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val khaki = Color(0xFF6E7A60)
    private val dusk = Color(0xFF55657F)

    private val vehicle = VehicleDetailResponse(
        id = "v1",
        model = "CASPER Electric",
        nickname = "우리집 캐스퍼",
        myRole = VehicleRole.OWNER,
        members = listOf(
            VehicleMemberResponse("u1", "엄마", VehicleRole.OWNER, "2026-10-30T09:00:00Z"),
            VehicleMemberResponse("u2", "아빠", VehicleRole.MEMBER, "2026-10-30T09:05:00Z"),
            VehicleMemberResponse("u3", "지우", VehicleRole.MEMBER, "2026-10-30T09:10:00Z"),
        ),
    )

    private val reservation = ReservationResponse(
        id = "r1", vehicleId = "v1", driverUserId = "u3", driverNickname = "지우", profileId = "p3",
        profileName = "밤 드라이브", status = ReservationStatus.PENDING, createdAt = "2026-11-03T18:40:00Z",
    )

    private val history = listOf(
        ApplyLogResponse("u1", "엄마", "p1", "출근", ApplySource.RESERVATION, "2026-11-03T08:12:00Z"),
        ApplyLogResponse("u2", "아빠", "p2", "주말 캠핑", ApplySource.SELECTED, "2026-11-01T10:30:00Z"),
        ApplyLogResponse("u3", "지우", "p3", "밤 드라이브", ApplySource.RESERVATION, "2026-10-31T21:05:00Z"),
        ApplyLogResponse("u1", "엄마", "p1", "출근", ApplySource.RESERVATION, "2026-10-31T08:09:00Z"),
    )

    private val nightTheme = ThemeSkinDto(
        name = "밤 드라이브",
        colors = ThemeColorsDto(background = "#0B1018", surface = "#1C2533", primary = "#3D9BFF", onPrimary = "#06101E", text = "#EEF3FA"),
        buttonStyle = ButtonStyle.PILL, widgetStyle = WidgetStyle.GLASS, fontStyle = FontStyle.DEFAULT, ambientColor = "#3D9BFF",
    )

    private val profile = ProfileResponse(
        id = "p3", name = "밤 드라이브", display = DisplayDto(listOf("battery", "climate", "music")),
        seat = SeatDto(heatingLevel = 2, ventilationLevel = 0, steeringWheelHeating = true),
        climate = ClimateDto(21.5, 3), light = LightDto(70), themeId = "t1",
        createdAt = "2026-10-30T09:00:00Z", updatedAt = "2026-11-02T22:00:00Z",
    )

    private fun shot(name: String, palette: PhonePalette = LightPalette, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent { PhoneSurface(palette) { content() } }
        compose.mainClock.advanceTimeBy(500)
        compose.onRoot().captureRoboImage("build/screens/$name.png")
    }

    @Test
    fun homeLight() = shot("phone_1_home_light") { PhoneHomeScreen(vehicle, reservation, history.first(), khaki) }

    @Test
    fun homeDark() = shot("phone_2_home_dark", DarkPalette) { PhoneHomeScreen(vehicle, reservation, history.first(), dusk) }

    @Test
    fun profile() = shot("phone_3_profile") { ProfileScreen(profile, "우리집 캐스퍼", seatSlot = 2, theme = nightTheme) }

    @Test
    fun themeStudio() = shot("phone_4_theme_studio") {
        ThemeStudioScreen(
            nightTheme,
            listOf(Color(0xFF3D9BFF), Color(0xFFB39DFF), Color(0xFFC9D88A), Color(0xFFFF8A65), Color(0xFFE6E9EE)),
            selectedColor = 0,
        )
    }

    @Test
    fun historyScreen() = shot("phone_5_history") { HistoryScreen("우리집 캐스퍼", reservation, history, vehicle.members.map { it.nickname }, khaki) }
}
