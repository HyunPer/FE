package com.pleos.fe.vehicle.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.pleos.fe.api.model.VehicleSelfResponse
import com.pleos.fe.ui.PleosCar
import com.pleos.fe.vehicle.BuildConfig

/** 차량 앱 화면 진입점. 쉼표 화면은 VehicleScreen에 상태를 더하고 여기서 분기한다. */
@Composable
fun VehicleRoot(controller: VehicleController) {
    val screen by controller.screen.collectAsState()
    ScaledSurface {
        when (val s = screen) {
            VehicleScreen.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center).size(72.dp), color = TextColor)
            is VehicleScreen.Waiting -> WaitingScreen(s.self, controller::startTrip)
            is VehicleScreen.Driving -> CareScreen(
                risk = s.risk,
                onArrive = controller::finishTrip,
                restStyle = s.restStyle,
                restAreaAheadM = s.restAreaAheadM,
                parked = s.parked,
                // 시연 패널은 디버그 빌드에서만(제출 릴리스 APK에는 보이지 않는다)
                onDemo = if (BuildConfig.DEBUG) controller::demo else null,
            )
            is VehicleScreen.Arrived -> ArrivalScreen(s.trip, controller::done)
            is VehicleScreen.Failed -> FailedScreen(s.message, s.retry)
        }
    }
}

/**
 * 화면 크기에 맞춰 글자와 여백을 함께 키운다. 화면들은 가로 1280dp(와이드)를 기준으로 그렸고,
 * Pleos Connect 에뮬레이터의 앱 영역(약 1680×1210dp)처럼 덜 납작한 화면은 가로 1120dp를 기준으로 삼아
 * 남는 세로 공간만큼 더 크게(최대 1.6배) 그린다. 운전석에서 한눈에 읽히는 크기를 유지하려는 것이다.
 * [dark]를 주지 않으면 시스템의 주간/야간 모드를 따른다.
 */
@Composable
fun ScaledSurface(dark: Boolean? = null, content: @Composable BoxScope.() -> Unit) {
    val themed: @Composable (@Composable BoxScope.() -> Unit) -> Unit =
        { inner -> if (dark == null) VehicleTheme(content = inner) else VehicleTheme(dark, inner) }
    themed {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val baseWidth = if (maxWidth / maxHeight < TALL_ASPECT) BASE_WIDTH_TALL else BASE_WIDTH
            val scale = minOf(maxWidth / baseWidth, maxHeight / BASE_HEIGHT).coerceIn(1f, MAX_SCALE)
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density * scale, base.fontScale)) {
                Box(Modifier.fillMaxSize(), content = content)
            }
        }
    }
}

private val BASE_WIDTH = 1280.dp
private val BASE_WIDTH_TALL = 1120.dp

/** 가로÷세로가 이보다 작으면(세로 여유가 큰 가로 화면) 계기와 글을 위아래로 쌓는다. */
internal const val TALL_ASPECT = 1.5f
private val BASE_HEIGHT = 640.dp
private const val MAX_SCALE = 1.6f

/**
 * 대기 화면. 멤버가 없으면 연결 코드를 크게, 있으면 대기 상태와 작은 초대 코드. 정차 중 화면이다.
 * 큰 글자 두 줄이 화면을 이끌고, 차는 화면 끝에 걸쳐 크게 놓는다.
 * 차량 이미지는 Pleos Connect 기본 화면과 같은 렌더(디자인 가이드 배포 자료)를 쓴다.
 */
@Composable
fun WaitingScreen(self: VehicleSelfResponse, onStartTrip: () -> Unit) {
    val paired = self.memberCount > 0
    val c = LocalVehicleColors.current
    val glow = if (c.dark) 0.34f else 0.20f
    AdaptiveTall { portrait, tall ->
        when {
            portrait -> Column(Modifier.fillMaxSize().ambientGlow(c.accent, 0.5f, 0.86f, strength = glow).padding(48.dp)) {
                TopBar(self)
                Spacer(Modifier.height(56.dp))
                Headline(paired, 84.sp)
                Spacer(Modifier.height(20.dp))
                SubLine(paired, 26.sp)
                Spacer(Modifier.height(44.dp))
                ActionPanel(self, onStartTrip, Modifier.fillMaxWidth())
                Spacer(Modifier.weight(1f))
                PleosCar(Modifier.fillMaxWidth(), c.dark)
            }
            // 세로 여유가 큰 가로 화면(에뮬레이터 앱 영역). 연결 전에는 코드를 가운데에 두고 차를 아래에 놓는다
            tall && !paired -> Column(
                Modifier.fillMaxSize().ambientGlow(c.accent, 0.5f, 0.92f, strength = glow).padding(start = 56.dp, end = 56.dp, top = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TopBar(self)
                Spacer(Modifier.height(44.dp))
                Headline(paired, 76.sp, oneLine = true)
                Spacer(Modifier.height(14.dp))
                SubLine(paired, 26.sp)
                Spacer(Modifier.height(36.dp))
                ActionPanel(self, onStartTrip)
                Spacer(Modifier.weight(1f))
                PleosCar(Modifier.width(520.dp), c.dark)
            }
            // 연결 뒤에는 큰 글자를 왼쪽 위에, 차를 오른쪽 아래에 크게
            tall -> Box(Modifier.fillMaxSize().ambientGlow(c.accent, 0.70f, 0.74f, strength = glow)) {
                PleosCar(Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 44.dp).fillMaxWidth(0.62f), c.dark)
                Column(Modifier.fillMaxSize().padding(56.dp)) {
                    TopBar(self)
                    Spacer(Modifier.height(44.dp))
                    Headline(paired, 112.sp)
                    Spacer(Modifier.height(20.dp))
                    SubLine(paired, 26.sp)
                    Spacer(Modifier.weight(1f))
                    ActionPanel(self, onStartTrip, Modifier.width(352.dp))
                }
            }
            else -> Row(
                Modifier.fillMaxSize().ambientGlow(c.accent, 0.78f, 0.7f, strength = glow).padding(start = 56.dp, top = 36.dp, bottom = 36.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Brand()
                    Spacer(Modifier.height(18.dp))
                    Headline(paired, 52.sp, oneLine = true)
                    Spacer(Modifier.height(8.dp))
                    SubLine(paired, 22.sp)
                    Spacer(Modifier.height(22.dp))
                    ActionPanel(self, onStartTrip, wide = true)
                }
                PleosCar(Modifier.width(540.dp).padding(end = 12.dp), c.dark)
            }
        }
    }
}

/** 화면이 세로로 긴지(폼 팩터 12.9″ 세로 등) 알려준다. 가로면 좌우로, 세로면 위아래로 놓는 화면이 쓴다. */
@Composable
fun Adaptive(content: @Composable (portrait: Boolean) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) { content(maxHeight > maxWidth) }
}

/** 가로 화면이지만 세로 여유가 큰지(에뮬레이터 앱 영역 등). 세로 화면은 false. */
@Composable
fun AdaptiveTall(content: @Composable (portrait: Boolean, tall: Boolean) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val portrait = maxHeight > maxWidth
        content(portrait, !portrait && maxWidth / maxHeight < TALL_ASPECT)
    }
}

/** 맨 위 줄: 왼쪽에 앱 표식, 오른쪽에 연결 상태. */
@Composable
private fun TopBar(self: VehicleSelfResponse) {
    val c = LocalVehicleColors.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Brand()
        Spacer(Modifier.weight(1f))
        if (self.memberCount > 0) StatusChip("가족 ${self.memberCount}명 연결됨", c.riskLow) else StatusChip("연결 기다리는 중", c.accent)
    }
}

/** 앱 이름과 쉼표 모양 표식. */
@Composable
private fun Brand() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ShwimpyoMark(44.dp)
        Spacer(Modifier.width(14.dp))
        Text("쉼표", color = TextColor, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** 네모 안의 쉼표(,). 런처 아이콘과 같은 모양이다. */
@Composable
internal fun ShwimpyoMark(size: Dp) {
    val bg = LocalVehicleColors.current.primary
    val fg = LocalVehicleColors.current.onPrimary
    Canvas(Modifier.size(size).background(bg, RoundedCornerShape(size * 0.22f))) {
        val u = this.size.minDimension / 24f
        val comma = Path().apply {
            moveTo(12.4f * u, 5.6f * u)
            cubicTo(15.2f * u, 5.6f * u, 17.4f * u, 7.8f * u, 17.4f * u, 10.6f * u)
            cubicTo(17.4f * u, 15.5f * u, 14.5f * u, 19.1f * u, 9.9f * u, 20.6f * u)
            lineTo(9f * u, 18.7f * u)
            cubicTo(11.5f * u, 17.7f * u, 13.1f * u, 16.1f * u, 13.7f * u, 14.6f * u)
            cubicTo(13.3f * u, 14.7f * u, 12.8f * u, 14.8f * u, 12.4f * u, 14.8f * u)
            cubicTo(9.6f * u, 14.8f * u, 7.4f * u, 12.5f * u, 7.4f * u, 9.8f * u)
            cubicTo(7.4f * u, 7.5f * u, 9.6f * u, 5.6f * u, 12.4f * u, 5.6f * u)
            close()
        }
        drawPath(comma, fg)
    }
}

/** 화면을 이끄는 큰 글자. 아래로 갈수록 조금 옅어져 바탕에 스민다. */
@Composable
private fun Headline(paired: Boolean, size: TextUnit, oneLine: Boolean = false) {
    val c = LocalVehicleColors.current
    val text = when {
        paired && oneLine -> "함께 탈 준비가 됐어요"
        paired -> "함께 탈\n준비가 됐어요"
        oneLine -> "이 차를 가족과 연결하세요"
        else -> "이 차를 가족과\n연결하세요"
    }
    Text(
        text,
        fontSize = size,
        lineHeight = size * 1.14f,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.02).em,
        style = TextStyle(brush = Brush.verticalGradient(listOf(c.text, c.text.copy(alpha = if (oneLine) 0.86f else 0.66f)))),
    )
}

@Composable
private fun SubLine(paired: Boolean, size: TextUnit) {
    Text(
        if (paired) "운전석에 앉고 P단이면 자동으로 시작해요" else "쉼표 폰 앱에서 이 코드를 입력하세요",
        color = SubText,
        fontSize = size,
    )
}

/**
 * 유리 카드 하나에 할 일을 모은다. 연결 전에는 큰 연결 코드, 연결 뒤에는 출발 버튼과 작은 초대 코드.
 * 탑승은 착석+P단으로 자동 감지한다. 버튼은 에뮬레이터 시연과 수동 시작용이다.
 */
@Composable
private fun ActionPanel(self: VehicleSelfResponse, onStartTrip: () -> Unit, modifier: Modifier = Modifier, wide: Boolean = false) {
    val paired = self.memberCount > 0
    when {
        !paired -> Column(modifier.glass().padding(horizontal = 28.dp, vertical = 24.dp)) { PairingCode(self.pairingCode, large = !wide) }
        // 낮은 와이드 화면: 버튼과 코드를 한 줄에
        wide -> Row(verticalAlignment = Alignment.CenterVertically) {
            PrimaryButton("지금 출발", onStartTrip)
            Spacer(Modifier.width(24.dp))
            Column(Modifier.glass(TileShape).padding(horizontal = 20.dp, vertical = 12.dp)) { PairingCode(self.pairingCode, large = false) }
        }
        else -> Column(modifier.glass().padding(24.dp)) {
            PairingCode(self.pairingCode, large = false)
            Spacer(Modifier.height(22.dp))
            PrimaryButton("지금 출발", onStartTrip, Modifier.fillMaxWidth())
        }
    }
}

/** 연결 코드 6자리. 멤버가 없을 때(정차 중 연결 화면)는 크게. */
@Composable
private fun PairingCode(code: String, large: Boolean) {
    val c = LocalVehicleColors.current
    val cell = RoundedCornerShape(if (large) 18.dp else 12.dp)
    Column {
        Text(if (large) "연결 코드" else "가족 초대 코드", color = SubText, fontSize = if (large) 20.sp else 16.sp)
        Spacer(Modifier.height(if (large) 14.dp else 10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(if (large) 12.dp else 8.dp)) {
            code.forEach { ch ->
                Box(
                    Modifier
                        .size(if (large) 84.dp else 44.dp, if (large) 108.dp else 58.dp)
                        .background(c.track, cell)
                        .border(1.dp, c.line, cell),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(ch.toString(), color = TextColor, fontSize = if (large) 54.sp else 28.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FailedScreen(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = TextColor, fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(36.dp))
        PrimaryButton("다시 시도", onRetry)
    }
}
