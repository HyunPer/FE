package com.pleos.fe.vehicle.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 차량 앱 색. Pleos Connect 기본 화면에 맞춰 밝은 회색 바탕 + 검은 글자·검은 사각 버튼을 기본으로 하고,
 * 시스템이 야간 모드면 어두운 색으로 바뀐다(디자인 가이드: 주간/야간 모드 지원, 대비 4.5:1 이상).
 */
data class VehicleColors(
    val dark: Boolean,
    val background: Color,
    val surface: Color,
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
    background = Color(0xFFF2F2F2),
    surface = Color(0xFFFFFFFF),
    text = Color(0xFF111111),
    subText = Color(0xFF666666),
    line = Color(0xFFD4D4D4),
    track = Color(0xFFE2E2E2),
    primary = Color(0xFF111111),
    onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFF2F6BFF),
    riskLow = Color(0xFF12925A),
    riskCaution = Color(0xFFC77700),
    riskHigh = Color(0xFFD93A2F),
)

val DarkVehicleColors = VehicleColors(
    dark = true,
    background = Color(0xFF101012),
    surface = Color(0xFF1D1D20),
    text = Color(0xFFF2F2F2),
    subText = Color(0xFF9B9BA1),
    line = Color(0xFF3A3A40),
    track = Color(0xFF2E2E33),
    primary = Color(0xFFF2F2F2),
    onPrimary = Color(0xFF111111),
    accent = Color(0xFF5AA2FF),
    riskLow = Color(0xFF3DDC97),
    riskCaution = Color(0xFFFFC145),
    riskHigh = Color(0xFFFF6B5E),
)

val LocalVehicleColors = staticCompositionLocalOf { LightVehicleColors }

internal val Background: Color @Composable @ReadOnlyComposable get() = LocalVehicleColors.current.background
internal val Surface: Color @Composable @ReadOnlyComposable get() = LocalVehicleColors.current.surface
internal val TextColor: Color @Composable @ReadOnlyComposable get() = LocalVehicleColors.current.text
internal val SubText: Color @Composable @ReadOnlyComposable get() = LocalVehicleColors.current.subText
internal val Line: Color @Composable @ReadOnlyComposable get() = LocalVehicleColors.current.line
internal val Track: Color @Composable @ReadOnlyComposable get() = LocalVehicleColors.current.track
internal val Accent: Color @Composable @ReadOnlyComposable get() = LocalVehicleColors.current.accent

/** 카드·버튼 모서리. Pleos 기본 화면처럼 거의 각지게. */
internal val CardShape = RoundedCornerShape(12.dp)
internal val ButtonShape = RoundedCornerShape(8.dp)

/** 색을 정하고 바탕을 칠한다. 화면 캡처 테스트도 이걸로 감싼다. */
@Composable
fun VehicleTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable BoxScope.() -> Unit) {
    val colors = if (dark) DarkVehicleColors else LightVehicleColors
    CompositionLocalProvider(LocalVehicleColors provides colors) {
        Box(Modifier.fillMaxSize().background(colors.background), content = content)
    }
}

/** 주 버튼: 검은(야간엔 흰) 사각 버튼. */
@Composable
internal fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 76.dp, fontSize: TextUnit = 24.sp) {
    val c = LocalVehicleColors.current
    Button(
        onClick,
        modifier.height(height),
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.onPrimary),
    ) { Text(text, fontSize = fontSize, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp)) }
}

/** 보조 버튼: 테두리만 있는 사각 버튼. */
@Composable
internal fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 76.dp, fontSize: TextUnit = 22.sp) {
    val c = LocalVehicleColors.current
    OutlinedButton(onClick, modifier.height(height), shape = ButtonShape, border = BorderStroke(1.5.dp, c.line)) {
        Text(text, color = c.text, fontSize = fontSize, modifier = Modifier.padding(horizontal = 8.dp))
    }
}
