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
import com.pleos.fe.ui.DefaultSkinDto
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
 * 차량 화면을 PNG로 그린다(에뮬레이터 불필요). 디자인 확인·PT 자료용.
 * 결과: vehicle-app/build/screens/. 차량 디스플레이 가로 비율(1280×640dp)로 그린다.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w1280dp-h640dp-land-mdpi")
class ScreenSnapshotTest {

    private val nightCat = ThemeSkinDto(
        name = "고양이 밤",
        colors = ThemeColorsDto(background = "#1B1530", surface = "#2A2245", primary = "#B39DFF", onPrimary = "#1B1530", text = "#F3EEFF"),
        buttonStyle = ButtonStyle.PILL,
        widgetStyle = WidgetStyle.GLASS,
        fontStyle = FontStyle.ROUNDED,
        ambientColor = "#7C4DFF",
    )

    private val morning = ThemeSkinDto(
        name = "아침 출근",
        colors = ThemeColorsDto(background = "#F4F1EA", surface = "#FFFFFF", primary = "#1E6B52", onPrimary = "#FFFFFF", text = "#1C1C1C"),
        buttonStyle = ButtonStyle.ROUNDED,
        widgetStyle = WidgetStyle.FILLED,
        fontStyle = FontStyle.BOLD,
        ambientColor = "#FFFFFF",
    )

    private fun welcome(skin: ThemeSkinDto, nickname: String) = WelcomeResponse(
        profileId = "p1",
        driverNickname = nickname,
        skin = skin,
        widgets = listOf("clock", "music", "weather", "battery", "climate"),
        ambientLight = AmbientLightDto(skin.ambientColor, 70),
        greeting = "${nickname}님, 오늘도 안전 운전하세요",
    )

    private val items = listOf(
        ApplyItem("공조 온도", true, ""), ApplyItem("풍량", true, ""), ApplyItem("시트 열선", true, ""),
        ApplyItem("시트 자세", false, ""), ApplyItem("실내 조명", true, ""), ApplyItem("인사말 음성", true, ""),
    )

    @get:Rule
    val compose = createComposeRule()

    /** 로딩 표시·시계처럼 끝없이 도는 애니메이션이 있어 시계를 멈추고 0.5초만 넘긴 뒤 찍는다. */
    private fun shot(name: String, skin: ThemeSkinDto = DefaultSkinDto, showCharacter: Boolean = false, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalSkin provides skin.toSkin { it }) {
                SkinBackground(showCharacter) { content() }
            }
        }
        compose.mainClock.advanceTimeBy(500)
        compose.onRoot().captureRoboImage("build/screens/$name.png")
    }

    private val settings = VehicleSettingsDto(
        SeatDto(heatingLevel = 2, steeringWheelHeating = true),
        ClimateDto(21.5, 3),
        SeatPositionDto(memorySlot = 2, updatedAt = ""),
    )

    @Test
    fun pairing() = shot("1_pairing") { WaitingScreen(VehicleSelfResponse("v1", "Pleos Connect", null, "K7M2Q9", 0), {}, {}) }

    @Test
    fun waiting() = shot("2_waiting") { WaitingScreen(VehicleSelfResponse("v1", "Pleos Connect", "우리집 아이오닉", "K7M2Q9", 3), {}, {}) }

    @Test
    fun applying() = shot("3_applying") { ApplyingScreen("엄마", BoardingOutcome.Step.VEHICLE_SETTINGS) }

    @Test
    fun chooseDriver() = shot("4_choose_driver") {
        ChooseDriverScreen(
            listOf(
                DriverProfilesResponse("u1", "엄마", listOf(ApplicableProfileDto("p1", "출근", settings))),
                DriverProfilesResponse("u2", "아빠", listOf(ApplicableProfileDto("p2", "주말 캠핑", settings.copy(climate = ClimateDto(19.0, 5))))),
                DriverProfilesResponse("u3", "대학생 자녀", listOf(ApplicableProfileDto("p3", "밤 드라이브", settings), ApplicableProfileDto("p4", "등교", settings))),
            ),
            { _, _ -> },
            {},
        )
    }

    @Test
    fun welcomeGlass() = shot("5_welcome_glass", nightCat) { WelcomeScreen(welcome(nightCat, "지우"), {}) }

    @Test
    fun homeGlass() = shot("6_home_glass", nightCat, showCharacter = true) { HomeScreen(welcome(nightCat, "지우"), items, {}, {}) }

    @Test
    fun homeFilled() = shot("7_home_filled", morning, showCharacter = true) { HomeScreen(welcome(morning, "엄마"), items, {}, {}) }

    // ── 스타일 비교: 같은 홈 화면을 레퍼런스 세 방향으로 ──

    private val darkGlass = ThemeSkinDto(
        name = "다크 글래스",
        colors = ThemeColorsDto(background = "#0B1018", surface = "#1C2533", primary = "#3D9BFF", onPrimary = "#06101E", text = "#EEF3FA"),
        buttonStyle = ButtonStyle.PILL, widgetStyle = WidgetStyle.GLASS, fontStyle = FontStyle.DEFAULT, ambientColor = "#3D9BFF",
    )

    private val metallic = ThemeSkinDto(
        name = "메탈릭",
        colors = ThemeColorsDto(background = "#121417", surface = "#3A4049", primary = "#D6DEE8", onPrimary = "#15181C", text = "#F2F4F7"),
        buttonStyle = ButtonStyle.ROUNDED, widgetStyle = WidgetStyle.METALLIC, fontStyle = FontStyle.DEFAULT, ambientColor = "#FFFFFF",
    )

    private val lightMinimal = ThemeSkinDto(
        name = "라이트 미니멀",
        colors = ThemeColorsDto(background = "#EEF0F3", surface = "#FFFFFF", primary = "#111317", onPrimary = "#FFFFFF", text = "#111317"),
        buttonStyle = ButtonStyle.PILL, widgetStyle = WidgetStyle.FILLED, fontStyle = FontStyle.DEFAULT, ambientColor = "#FFFFFF",
    )

    @Test
    fun styleDarkGlass() = shot("style_a_dark_glass", darkGlass) { HomeScreen(welcome(darkGlass, "지우"), items, {}, {}) }

    @Test
    fun styleMetallic() = shot("style_b_metallic", metallic) { HomeScreen(welcome(metallic, "아빠"), items, {}, {}) }

    @Test
    fun styleLightMinimal() = shot("style_c_light_minimal", lightMinimal) { HomeScreen(welcome(lightMinimal, "엄마"), items, {}, {}) }

    @Test
    fun styleDarkGlassWaiting() = shot("style_a_dark_glass_waiting", darkGlass) {
        WaitingScreen(VehicleSelfResponse("v1", "Pleos Connect", "우리집 아이오닉", "K7M2Q9", 3), {}, {})
    }

    @Test
    fun failed() = shot("8_failed") { FailedScreen("차량 설정을 적용하지 못했습니다.\n주행 중에는 시트·공조를 바꿀 수 없습니다.", {}, {}) }
}
