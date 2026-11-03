package com.pleos.fe.vehicle.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.pleos.fe.api.flow.BoardingOutcome
import com.pleos.fe.api.model.AmbientLightDto
import com.pleos.fe.api.model.ApplicableProfileDto
import com.pleos.fe.api.model.ButtonStyle
import com.pleos.fe.api.model.ClimateDto
import com.pleos.fe.api.model.DriverProfilesResponse
import com.pleos.fe.api.model.FontStyle
import com.pleos.fe.api.model.SeatDto
import com.pleos.fe.api.model.SeatPositionDto
import com.pleos.fe.api.model.ThemeColorsDto
import com.pleos.fe.api.model.ThemeSkinDto
import com.pleos.fe.api.model.VehicleSelfResponse
import com.pleos.fe.api.model.VehicleSettingsDto
import com.pleos.fe.api.model.WelcomeResponse
import com.pleos.fe.api.model.WidgetStyle
import com.pleos.fe.ui.LocalSkin
import com.pleos.fe.ui.SkinBackground
import com.pleos.fe.ui.toSkin
import com.pleos.fe.vehicle.sdk.ApplyItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 캐스퍼 일렉트릭 기준 시안: 10.25인치 내비 화면(1920×720px, 약 2.67:1)을 hdpi로 보고 1280×480dp에 그린다.
 * 캐스퍼는 현재 Pleos Connect 탑재 차종이 아니므로 "이 화면 크기에서 이렇게 보인다"는 가정용이다.
 * 결과: vehicle-app/build/screens/casper_*.png
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w1280dp-h480dp-land-hdpi")
class CasperScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val car = VehicleSelfResponse("v1", "CASPER Electric", "우리집 캐스퍼", "K7M2Q9", 3)

    // 인스퍼레이션 트림 1회 충전 315km 기준, 잔량 78%일 때
    private val ev = VehicleStatus(batteryPercent = 78, rangeKm = 245, cabinTemperature = 22.0)

    private val darkGlass = ThemeSkinDto(
        name = "기본 다크",
        colors = ThemeColorsDto(background = "#0B1018", surface = "#1C2533", primary = "#3D9BFF", onPrimary = "#06101E", text = "#EEF3FA"),
        buttonStyle = ButtonStyle.PILL, widgetStyle = WidgetStyle.GLASS, fontStyle = FontStyle.DEFAULT, ambientColor = "#3D9BFF",
    )

    private val khaki = ThemeSkinDto(
        name = "주말 캠핑",
        colors = ThemeColorsDto(background = "#1E2219", surface = "#3A4232", primary = "#C9D88A", onPrimary = "#1E2219", text = "#F1F4E6"),
        buttonStyle = ButtonStyle.PILL, widgetStyle = WidgetStyle.GLASS, fontStyle = FontStyle.ROUNDED, ambientColor = "#C9D88A",
    )

    private val lightMinimal = ThemeSkinDto(
        name = "라이트 미니멀",
        colors = ThemeColorsDto(background = "#EEF0F3", surface = "#FFFFFF", primary = "#111317", onPrimary = "#FFFFFF", text = "#111317"),
        buttonStyle = ButtonStyle.PILL, widgetStyle = WidgetStyle.FILLED, fontStyle = FontStyle.DEFAULT, ambientColor = "#FFFFFF",
    )

    private val metallic = ThemeSkinDto(
        name = "메탈릭",
        colors = ThemeColorsDto(background = "#121417", surface = "#3A4049", primary = "#D6DEE8", onPrimary = "#15181C", text = "#F2F4F7"),
        buttonStyle = ButtonStyle.ROUNDED, widgetStyle = WidgetStyle.METALLIC, fontStyle = FontStyle.DEFAULT, ambientColor = "#FFFFFF",
    )

    private fun welcome(skin: ThemeSkinDto, nickname: String) = WelcomeResponse(
        profileId = "p1",
        driverNickname = nickname,
        skin = skin,
        widgets = listOf("battery", "climate", "music"),
        ambientLight = AmbientLightDto(skin.ambientColor, 70),
        greeting = "${nickname}님, 오늘도 안전 운전하세요",
    )

    private val items = listOf(
        ApplyItem("공조 온도", true, ""), ApplyItem("시트 열선", true, ""), ApplyItem("핸들 열선", true, ""),
        ApplyItem("실내 조명", true, ""), ApplyItem("인사말 음성", true, ""),
    )

    private val settings = VehicleSettingsDto(
        SeatDto(heatingLevel = 2, steeringWheelHeating = true),
        ClimateDto(21.5, 3),
        SeatPositionDto(memorySlot = 2, updatedAt = ""),
    )

    private fun shot(name: String, skin: ThemeSkinDto = darkGlass, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalSkin provides skin.toSkin { it }) {
                SkinBackground(showCharacter = false) { content() }
            }
        }
        compose.mainClock.advanceTimeBy(500)
        compose.onRoot().captureRoboImage("build/screens/casper_$name.png")
    }

    @Test
    fun pairing() = shot("1_pairing") { WaitingScreen(car.copy(nickname = null, memberCount = 0), {}, {}) }

    @Test
    fun waiting() = shot("2_waiting") { WaitingScreen(car, {}, {}) }

    @Test
    fun applying() = shot("3_applying") { ApplyingScreen("엄마", BoardingOutcome.Step.VEHICLE_SETTINGS) }

    @Test
    fun chooseDriver() = shot("4_choose_driver") {
        ChooseDriverScreen(
            listOf(
                DriverProfilesResponse("u1", "엄마", listOf(ApplicableProfileDto("p1", "출근", settings))),
                DriverProfilesResponse("u2", "아빠", listOf(ApplicableProfileDto("p2", "주말 캠핑", settings.copy(climate = ClimateDto(19.0, 5))))),
                DriverProfilesResponse("u3", "대학생 자녀", listOf(ApplicableProfileDto("p3", "밤 드라이브", settings))),
            ),
            { _, _ -> },
            {},
        )
    }

    @Test
    fun welcomeScreen() = shot("5_welcome", khaki) { WelcomeScreen(welcome(khaki, "아빠"), {}) }

    @Test
    fun homeDark() = shot("6_home_dark_glass") { HomeScreen(welcome(darkGlass, "지우"), items, {}, {}, ev) }

    @Test
    fun homeKhaki() = shot("7_home_khaki_glass", khaki) { HomeScreen(welcome(khaki, "아빠"), items, {}, {}, ev) }

    @Test
    fun homeLight() = shot("8_home_light_minimal", lightMinimal) { HomeScreen(welcome(lightMinimal, "엄마"), items, {}, {}, ev) }

    @Test
    fun homeMetallic() = shot("9_home_metallic", metallic) { HomeScreen(welcome(metallic, "아빠"), items, {}, {}, ev) }
}
