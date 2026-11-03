package com.pleos.fe.vehicle.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pleos.fe.api.flow.BoardingOutcome
import com.pleos.fe.api.model.AmbientLightDto
import com.pleos.fe.api.model.ApplicableProfileDto
import com.pleos.fe.api.model.ButtonStyle
import com.pleos.fe.api.model.ClimateDto
import com.pleos.fe.api.model.DriverProfilesResponse
import com.pleos.fe.api.model.FontStyle
import com.pleos.fe.api.model.SeatDto
import com.pleos.fe.api.model.ThemeColorsDto
import com.pleos.fe.api.model.ThemeSkinDto
import com.pleos.fe.api.model.VehicleSelfResponse
import com.pleos.fe.api.model.VehicleSettingsDto
import com.pleos.fe.api.model.WelcomeResponse
import com.pleos.fe.api.model.WidgetStyle
import com.pleos.fe.ui.DefaultSkin
import com.pleos.fe.ui.DefaultSkinDto
import com.pleos.fe.ui.LocalSkin
import com.pleos.fe.ui.RemoteImage
import com.pleos.fe.ui.SkinBackground
import com.pleos.fe.ui.SkinButton
import com.pleos.fe.ui.SkinText
import com.pleos.fe.ui.SkinWidget
import com.pleos.fe.ui.toSkin
import com.pleos.fe.vehicle.sdk.ApplyItem
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

/** 차량 앱 화면 진입점. 웰컴·홈은 운전자 테마로, 나머지는 기본 테마로 그린다. */
@Composable
fun VehicleRoot(controller: VehicleController, imageUrl: (String) -> String) {
    val screen by controller.screen.collectAsState()
    val skin = when (val s = screen) {
        is VehicleScreen.Welcome -> s.welcome.skin.toSkin(imageUrl)
        is VehicleScreen.Home -> s.welcome.skin.toSkin(imageUrl)
        else -> DefaultSkin
    }
    CompositionLocalProvider(LocalSkin provides skin) {
        SkinBackground(showCharacter = screen is VehicleScreen.Home) {
            AnimatedContent(
                targetState = screen,
                contentKey = { it::class },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen",
            ) { s ->
                when (s) {
                    VehicleScreen.Loading -> LoadingScreen()
                    is VehicleScreen.Waiting -> WaitingScreen(s.self, controller::board, controller::pickDriver)
                    is VehicleScreen.Applying -> ApplyingScreen(s.driverNickname, s.step)
                    is VehicleScreen.ChooseDriver -> ChooseDriverScreen(s.drivers, controller::choose, controller::reset)
                    is VehicleScreen.Welcome -> WelcomeScreen(s.welcome, controller::goHome)
                    is VehicleScreen.Home -> HomeScreen(s.welcome, s.items, controller::pickDriver, controller::reset)
                    is VehicleScreen.Failed -> FailedScreen(s.message, s.retry, controller::reset)
                }
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = LocalSkin.current.primary, strokeWidth = 6.dp, modifier = Modifier.size(72.dp))
    }
}

/** 연결 코드(멤버 없음) 또는 탑승 대기(멤버 있음). 연결 코드는 정차 중 화면이라 크게 보여준다. */
@Composable
fun WaitingScreen(self: VehicleSelfResponse, onApplyNow: () -> Unit, onPickDriver: () -> Unit) {
    val paired = self.memberCount > 0
    Row(Modifier.fillMaxSize().padding(56.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            if (paired) {
                SkinText(self.nickname ?: self.model, 24.sp, alpha = 0.7f)
                Spacer(Modifier.height(8.dp))
                SkinText("탑승하면 내 프로필이\n자동으로 적용돼요", 44.sp, bold = true)
                Spacer(Modifier.height(36.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    SkinButton("지금 적용", onApplyNow)
                    SkinButton("운전자 고르기", onPickDriver, primary = false)
                }
            } else {
                SkinText("이 차를 가족과 연결하세요", 44.sp, bold = true)
                Spacer(Modifier.height(20.dp))
                SkinText("폰 앱 → 차량 연결에서 오른쪽 코드를 입력하면\n가족 모두의 프로필을 이 차에서 쓸 수 있어요.", 24.sp, alpha = 0.8f)
            }
        }
        Spacer(Modifier.width(48.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SkinText(if (paired) "가족 ${self.memberCount}명 연결됨 · 연결 코드" else "연결 코드", 22.sp, alpha = 0.7f)
            Spacer(Modifier.height(16.dp))
            PairingCode(self.pairingCode, large = !paired)
        }
    }
}

@Composable
private fun PairingCode(code: String, large: Boolean) {
    val skin = LocalSkin.current
    val box = if (large) 88.dp else 52.dp
    Row(horizontalArrangement = Arrangement.spacedBy(if (large) 14.dp else 8.dp)) {
        code.forEach { c ->
            Box(
                Modifier.size(box, box * 1.25f).background(skin.surface, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                SkinText(c.toString(), if (large) 52.sp else 30.sp, bold = true, color = skin.primary)
            }
        }
    }
}

@Composable
fun ApplyingScreen(driverNickname: String?, step: BoardingOutcome.Step) {
    val skin = LocalSkin.current
    Column(
        Modifier.fillMaxSize().padding(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = skin.primary, strokeWidth = 6.dp, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(32.dp))
        SkinText(driverNickname?.let { "${it}님 프로필을 적용하고 있어요" } ?: "프로필을 적용하고 있어요", 36.sp, bold = true)
        Spacer(Modifier.height(32.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BoardingOutcome.Step.entries.forEach { s ->
                val (mark, alpha) = when {
                    s.ordinal < step.ordinal -> "✓" to 0.7f
                    s == step -> "●" to 1f
                    else -> "○" to 0.4f
                }
                SkinText("$mark  ${s.label}", 24.sp, alpha = alpha, bold = s == step)
            }
        }
    }
}

private val BoardingOutcome.Step.label: String
    get() = when (this) {
        BoardingOutcome.Step.FETCH -> "다음 운전자 확인"
        BoardingOutcome.Step.VEHICLE_SETTINGS -> "시트·공조·열선 맞추기"
        BoardingOutcome.Step.REPORT -> "적용 완료 알리기"
        BoardingOutcome.Step.WELCOME -> "테마·조명·인사"
    }

/** 예약이 없을 때 1탭 선택. 가족 × 프로필을 카드 한 줄로 늘어놓는다. */
@Composable
fun ChooseDriverScreen(
    drivers: List<DriverProfilesResponse>,
    onChoose: (String, ApplicableProfileDto) -> Unit,
    onCancel: () -> Unit,
) {
    val cards = remember(drivers) { drivers.flatMap { d -> d.profiles.map { d.nickname to it } } }
    Column(Modifier.fillMaxSize().padding(48.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkinText("누가 운전하나요?", 40.sp, bold = true, modifier = Modifier.weight(1f))
            SkinButton("취소", onCancel, primary = false)
        }
        Spacer(Modifier.height(32.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(cards, key = { it.second.profileId }) { (nickname, profile) ->
                SkinWidget(Modifier.width(320.dp).fillMaxHeight(), onClick = { onChoose(nickname, profile) }) {
                    SkinText(nickname, 34.sp, bold = true, maxLines = 1)
                    Spacer(Modifier.height(8.dp))
                    SkinText(profile.name, 26.sp, alpha = 0.85f, maxLines = 2)
                    Spacer(Modifier.weight(1f))
                    SkinText(profile.vehicleSettings.summary(), 20.sp, alpha = 0.7f)
                }
            }
        }
    }
}

private fun VehicleSettingsDto.summary(): String = buildList {
    add(formatTemp(climate.temperature))
    add("풍량 ${climate.fanSpeed}")
    if (seat.heatingLevel > 0) add("열선 ${seat.heatingLevel}")
    if (seat.ventilationLevel > 0) add("통풍 ${seat.ventilationLevel}")
    if (seat.steeringWheelHeating) add("핸들 열선")
    seatPosition?.memorySlot?.let { add("자세 ${it}번") }
}.joinToString(" · ")

/** 운전자 테마가 처음 나타나는 순간. 몇 초 뒤 또는 누르면 홈으로. */
@Composable
fun WelcomeScreen(welcome: WelcomeResponse, onContinue: () -> Unit) {
    val skin = LocalSkin.current
    Row(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onContinue)
            .padding(64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        skin.characterImageUrl?.let {
            RemoteImage(it, Modifier.size(320.dp), ContentScale.Fit)
            Spacer(Modifier.width(48.dp))
        }
        Column {
            SkinText(welcome.greeting, 52.sp, bold = true)
            Spacer(Modifier.height(16.dp))
            SkinText("${welcome.skin.name} 테마를 적용했어요", 26.sp, alpha = 0.75f)
        }
    }
}

/** 운전자 테마 홈: 인사·시계, 프로필 위젯, 적용 결과와 운전자 바꾸기. */
@Composable
fun HomeScreen(
    welcome: WelcomeResponse,
    items: List<ApplyItem>,
    onPickDriver: () -> Unit,
    onReset: () -> Unit,
    status: VehicleStatus = VehicleStatus(),
) {
    Column(Modifier.fillMaxSize().padding(40.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                SkinText("${welcome.driverNickname}님", 34.sp, bold = true, maxLines = 1)
                SkinText("${welcome.skin.name} 테마", 20.sp, alpha = 0.7f, maxLines = 1)
            }
            SkinText(rememberClock(), 48.sp, bold = true)
        }
        Spacer(Modifier.height(28.dp))
        Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            if (welcome.widgets.isEmpty()) {
                SkinText("폰 앱에서 위젯을 고르면 여기에 표시돼요", 24.sp, alpha = 0.7f)
            }
            welcome.widgets.take(MAX_WIDGETS).chunked(WIDGETS_PER_ROW).forEach { row ->
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    row.forEach { WidgetCard(it, status, Modifier.weight(1f).fillMaxHeight()) }
                    repeat(WIDGETS_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SkinText(
                items.joinToString("   ") { "${if (it.ok) "✓" else "✗"} ${it.name}" },
                18.sp,
                alpha = 0.7f,
                maxLines = 2,
                modifier = Modifier.weight(1f),
            )
            SkinButton("다른 운전자", onPickDriver, primary = false)
            SkinButton("처음으로", onReset, primary = false)
        }
    }
}

/** 홈 위젯에 보여줄 차량 상태. Vehicle SDK(EvBattery·HVAC)에서 읽어 채운다. 모르면 null. */
data class VehicleStatus(
    val batteryPercent: Int? = null,
    val rangeKm: Int? = null,
    val cabinTemperature: Double? = null,
)

private fun formatTemp(t: Double) = if (t % 1.0 == 0.0) "${t.toInt()}℃" else "$t℃"

/** 위젯 이름은 폰에서 고른 문자열 그대로 온다. 아는 이름만 내용을 채우고 나머지는 이름만 보여준다. */
@Composable
private fun WidgetCard(id: String, status: VehicleStatus, modifier: Modifier) {
    val (title, body) = when (id) {
        "clock" -> "시계" to rememberClock()
        "music" -> "음악" to "재생 중인 곡이 없어요"
        "weather" -> "날씨" to "날씨 정보를 준비하고 있어요"
        "battery" -> "배터리" to (
            status.batteryPercent?.let { pct -> "$pct%" + (status.rangeKm?.let { " · ${it}km" } ?: "") } ?: "배터리 정보를 준비하고 있어요"
        )
        "climate" -> "공조" to (status.cabinTemperature?.let { "실내 ${formatTemp(it)}" } ?: "프로필 온도로 맞췄어요")
        else -> id to ""
    }
    SkinWidget(modifier) {
        SkinText(title, 22.sp, alpha = 0.75f, maxLines = 1)
        Spacer(Modifier.weight(1f))
        SkinText(body, 28.sp, bold = true, maxLines = 2)
    }
}

@Composable
fun FailedScreen(message: String, onRetry: () -> Unit, onReset: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SkinText(message, 30.sp, bold = true, textAlign = TextAlign.Center)
        Spacer(Modifier.height(36.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            SkinButton("다시 시도", onRetry)
            SkinButton("처음으로", onReset, primary = false)
        }
    }
}

@Composable
private fun rememberClock(): String {
    val format = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val now by produceState(LocalTime.now().format(format)) {
        while (true) {
            delay(1_000)
            value = LocalTime.now().format(format)
        }
    }
    return now
}

private const val MAX_WIDGETS = 6
private const val WIDGETS_PER_ROW = 3

// ── 미리보기(Android Studio). 차량 디스플레이 비율에 맞춰 가로로 본다. ──

private val previewSkin = ThemeSkinDto(
    name = "고양이 밤",
    colors = ThemeColorsDto(background = "#1B1530", surface = "#2A2245", primary = "#B39DFF", onPrimary = "#1B1530", text = "#F3EEFF"),
    buttonStyle = ButtonStyle.PILL,
    widgetStyle = WidgetStyle.GLASS,
    fontStyle = FontStyle.ROUNDED,
    ambientColor = "#7C4DFF",
)

private val previewWelcome = WelcomeResponse(
    profileId = "p1",
    driverNickname = "지우",
    skin = previewSkin.copy(character = null),
    widgets = listOf("clock", "music", "weather", "battery"),
    ambientLight = AmbientLightDto("#7C4DFF", 70),
    greeting = "지우님, 오늘도 안전 운전하세요",
)

@Composable
private fun Preview(skin: ThemeSkinDto = DefaultSkinDto, showCharacter: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSkin provides skin.toSkin { it }) {
        SkinBackground(showCharacter) { content() }
    }
}

@Preview(widthDp = 1280, heightDp = 640)
@Composable
private fun PairingPreview() = Preview {
    WaitingScreen(VehicleSelfResponse("v1", "Pleos Connect", null, "K7M2Q9", 0), {}, {})
}

@Preview(widthDp = 1280, heightDp = 640)
@Composable
private fun ApplyingPreview() = Preview { ApplyingScreen("엄마", BoardingOutcome.Step.VEHICLE_SETTINGS) }

@Preview(widthDp = 1280, heightDp = 640)
@Composable
private fun ChooseDriverPreview() = Preview {
    val settings = VehicleSettingsDto(SeatDto(heatingLevel = 2, steeringWheelHeating = true), ClimateDto(21.5, 3))
    ChooseDriverScreen(
        listOf(
            DriverProfilesResponse("u1", "엄마", listOf(ApplicableProfileDto("p1", "출근", settings))),
            DriverProfilesResponse("u2", "지우", listOf(ApplicableProfileDto("p2", "주말 드라이브", settings), ApplicableProfileDto("p3", "밤", settings))),
        ),
        { _, _ -> },
        {},
    )
}

@Preview(widthDp = 1280, heightDp = 640)
@Composable
private fun HomePreview() = Preview(previewSkin, showCharacter = true) {
    HomeScreen(
        previewWelcome,
        listOf(ApplyItem("공조 온도", true, ""), ApplyItem("시트 열선", true, ""), ApplyItem("시트 자세", false, ""), ApplyItem("실내 조명", true, "")),
        {},
        {},
    )
}

@Preview(widthDp = 1280, heightDp = 640)
@Composable
private fun WelcomePreview() = Preview(previewSkin) { WelcomeScreen(previewWelcome, {}) }
