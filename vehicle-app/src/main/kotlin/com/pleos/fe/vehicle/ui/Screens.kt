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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
 * 차량 이미지는 Pleos Connect 기본 화면과 같은 렌더(디자인 가이드 배포 자료)를 쓴다.
 */
@Composable
fun WaitingScreen(self: VehicleSelfResponse, onStartTrip: () -> Unit) {
    val paired = self.memberCount > 0
    val dark = LocalVehicleColors.current.dark
    AdaptiveTall { portrait, tall ->
        when {
            portrait -> Column(Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.Center) {
                Brand()
                Spacer(Modifier.height(32.dp))
                WaitingText(self, onStartTrip)
                Spacer(Modifier.height(40.dp))
                PairingCode(self.pairingCode, large = !paired)
                Spacer(Modifier.height(40.dp))
                PleosCar(Modifier.fillMaxWidth(), dark)
            }
            // 세로 여유가 큰 가로 화면: 글은 위, 차는 아래 오른쪽에 크게
            tall -> Box(Modifier.fillMaxSize().padding(56.dp)) {
                PleosCar(Modifier.align(Alignment.BottomEnd).fillMaxWidth(0.62f), dark)
                Column(Modifier.align(Alignment.TopStart)) {
                    Brand()
                    Spacer(Modifier.height(28.dp))
                    WaitingText(self, onStartTrip)
                    Spacer(Modifier.height(36.dp))
                    PairingCode(self.pairingCode, large = !paired)
                }
            }
            else -> Row(Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 40.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Brand()
                    Spacer(Modifier.height(20.dp))
                    WaitingText(self, onStartTrip)
                    Spacer(Modifier.height(24.dp))
                    PairingCode(self.pairingCode, large = !paired)
                }
                Spacer(Modifier.width(32.dp))
                PleosCar(Modifier.width(520.dp), dark)
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

/** 앱 이름과 쉼표 모양 표식. */
@Composable
private fun Brand() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ShwimpyoMark(40.dp)
        Spacer(Modifier.width(12.dp))
        Text("쉼표", color = TextColor, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
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

@Composable
private fun WaitingText(self: VehicleSelfResponse, onStartTrip: () -> Unit) {
    val paired = self.memberCount > 0
    Column {
        Text(
            if (paired) "함께 탈 준비가 됐어요" else "이 차를 가족과 연결하세요",
            color = TextColor,
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            if (paired) "가족 ${self.memberCount}명 연결됨 · 운전석에 앉고 P단이면 자동으로 시작해요" else "쉼표 폰 앱에서 이 코드를 입력하세요",
            color = SubText,
            fontSize = 24.sp,
        )
        if (paired) {
            // 탑승은 착석+P단으로 자동 감지한다. 에뮬레이터 시연용으로 버튼도 둔다.
            Spacer(Modifier.height(32.dp))
            PrimaryButton("지금 출발", { onStartTrip() })
        }
    }
}

/** 연결 코드 6자리. 멤버가 없을 때(정차 중 연결 화면)는 크게. */
@Composable
private fun PairingCode(code: String, large: Boolean) {
    Column {
        Text(if (large) "연결 코드" else "가족 초대 코드", color = SubText, fontSize = if (large) 20.sp else 16.sp)
        Spacer(Modifier.height(if (large) 12.dp else 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(if (large) 12.dp else 8.dp)) {
            code.forEach { c ->
                Box(
                    Modifier
                        .size(if (large) 84.dp else 48.dp, if (large) 108.dp else 60.dp)
                        .background(Surface, RoundedCornerShape(8.dp))
                        .border(1.dp, Line, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(c.toString(), color = TextColor, fontSize = if (large) 52.sp else 28.sp, fontWeight = FontWeight.Bold)
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
