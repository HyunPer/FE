package com.pleos.fe.vehicle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 차량 앱 색. 현대 브랜드의 남색(Hyundai Blue)·샌드 계열 바탕 위에 반투명 유리 카드를 올린다.
 * 바탕은 위아래로 흐르는 그라데이션이고, 상태 색이 은은하게 번진다([ambientGlow]).
 * 시스템이 야간 모드면 어두운 색으로 바뀐다(디자인 가이드: 주간/야간 모드 지원, 글자 대비 4.5:1 이상).
 *
 * 유리 카드는 실제 블러를 쓰지 않는다. 뒤가 매끈한 그라데이션이라 블러 결과가 같고,
 * 차량 화면에서 프레임을 떨어뜨리지 않으려는 것이다. 그래서 카드를 차 사진 위에 겹치지 않는다.
 */
data class VehicleColors(
    val dark: Boolean,
    /** 바탕 그라데이션의 위·아래 */
    val backgroundTop: Color,
    val background: Color,
    /** 유리 카드 면의 위·아래, 그리고 위쪽 모서리에 맺히는 빛 */
    val surface: Color,
    val surfaceLow: Color,
    val edge: Color,
    val text: Color,
    val subText: Color,
    val line: Color,
    val track: Color,
    val primary: Color,
    val onPrimary: Color,
    val accent: Color,
    val riskLow: Color,
    val riskCaution: Color,
    val riskHigh: Color,
)

val LightVehicleColors = VehicleColors(
    dark = false,
    backgroundTop = Color(0xFFF6F3F2),
    background = Color(0xFFE2E7EE),
    surface = Color(0xD9FFFFFF),
    surfaceLow = Color(0x8CFFFFFF),
    edge = Color(0xFFFFFFFF),
    text = Color(0xFF0A1626),
    subText = Color(0xFF4A5563),
    line = Color(0x290A1626),
    track = Color(0x1A0A1626),
    primary = Color(0xFF002C5F),
    onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFF00AAD2),
    riskLow = Color(0xFF0A7446),
    riskCaution = Color(0xFF945600),
    riskHigh = Color(0xFFA81F16),
)

val DarkVehicleColors = VehicleColors(
    dark = true,
    backgroundTop = Color(0xFF111D2E),
    background = Color(0xFF05080C),
    surface = Color(0x1FFFFFFF),
    surfaceLow = Color(0x0AFFFFFF),
    edge = Color(0x4DFFFFFF),
    text = Color(0xFFF4F7FA),
    subText = Color(0xFFA6B0BD),
    line = Color(0x29FFFFFF),
    track = Color(0x1AFFFFFF),
    primary = Color(0xFFF4F7FA),
    onPrimary = Color(0xFF0A1626),
    accent = Color(0xFF00AAD2),
    riskLow = Color(0xFF3DDC97),
    riskCaution = Color(0xFFFFC145),
    riskHigh = Color(0xFFFF6B5E),
)

val LocalVehicleColors = staticCompositionLocalOf { LightVehicleColors }

internal val Surface: Color @Composable @ReadOnlyComposable get() = LocalVehicleColors.current.surface
internal val TextColor: Color @Composable @ReadOnlyComposable get() = LocalVehicleColors.current.text
internal val SubText: Color @Composable @ReadOnlyComposable get() = LocalVehicleColors.current.subText
internal val Line: Color @Composable @ReadOnlyComposable get() = LocalVehicleColors.current.line
internal val Track: Color @Composable @ReadOnlyComposable get() = LocalVehicleColors.current.track
internal val Accent: Color @Composable @ReadOnlyComposable get() = LocalVehicleColors.current.accent

/** 카드는 크게 둥글리고, 버튼은 알약 모양. */
internal val CardShape = RoundedCornerShape(28.dp)
internal val TileShape = RoundedCornerShape(20.dp)
internal val ButtonShape: Shape = CircleShape

/** 색을 정하고 바탕 그라데이션을 칠한다. 화면 캡처 테스트도 이걸로 감싼다. */
@Composable
fun VehicleTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable BoxScope.() -> Unit) {
    val colors = if (dark) DarkVehicleColors else LightVehicleColors
    CompositionLocalProvider(LocalVehicleColors provides colors) {
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind { drawRect(Brush.verticalGradient(listOf(colors.backgroundTop, colors.background))) },
            content = content,
        )
    }
}

/**
 * 바탕에 번지는 빛. [x]·[y]는 화면 대비 중심 위치(0–1), [radius]는 긴 변 대비 반지름.
 * 주간 화면에서는 같은 색이 더 옅게 보이므로 [strength]를 조금 낮춰 쓴다.
 */
internal fun Modifier.ambientGlow(color: Color, x: Float, y: Float, radius: Float = 0.55f, strength: Float = 0.30f): Modifier =
    drawBehind {
        drawRect(
            Brush.radialGradient(
                0f to color.copy(alpha = strength),
                0.45f to color.copy(alpha = strength * 0.38f),
                1f to Color.Transparent,
                center = Offset(size.width * x, size.height * y),
                radius = size.maxDimension * radius,
            ),
        )
    }

/** 유리 면: 위가 조금 더 밝은 반투명 면 + 위쪽 모서리에 빛이 맺힌 가는 테두리. */
@Composable
internal fun Modifier.glass(shape: Shape = CardShape): Modifier {
    val c = LocalVehicleColors.current
    // 야간에는 아래로 갈수록 테두리가 사라지고, 주간에는 아래쪽에 옅은 그늘 선이 남아 밝은 바탕에서도 카드가 구분된다
    val lowEdge = if (c.dark) c.edge.copy(alpha = c.edge.alpha * 0.18f) else c.text.copy(alpha = 0.10f)
    return clip(shape)
        .background(Brush.verticalGradient(listOf(c.surface, c.surfaceLow)))
        .border(1.dp, Brush.verticalGradient(listOf(c.edge, lowEdge)), shape)
}

/** 주 버튼: 남색(야간엔 흰) 알약 버튼. */
@Composable
internal fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 76.dp, fontSize: TextUnit = 24.sp) {
    val c = LocalVehicleColors.current
    Button(
        onClick,
        modifier.height(height),
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.onPrimary),
    ) { Text(text, fontSize = fontSize, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp)) }
}

/** 보조 버튼: 옅은 면에 가는 테두리를 두른 알약 버튼. 유리 카드 위에 놓여도 구분된다. */
@Composable
internal fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 76.dp, fontSize: TextUnit = 22.sp) {
    val c = LocalVehicleColors.current
    Button(
        onClick,
        modifier.height(height).clip(ButtonShape).background(c.track).border(1.dp, c.line, ButtonShape),
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = c.text),
    ) { Text(text, fontSize = fontSize, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 12.dp)) }
}

/** 상태를 한 줄로 알리는 유리 알약. 앞의 점이 상태 색이다. */
@Composable
internal fun StatusChip(text: String, dot: Color, modifier: Modifier = Modifier) {
    Row(modifier.glass(CircleShape).padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(10.dp))
        Text(text, color = TextColor, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}
