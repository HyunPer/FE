package com.pleos.fe.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 박스형 소형 전기차 옆모습(오른쪽을 봄). 실제 차종 사진 대신 쓰는 자체 일러스트라 저작권 걱정 없이 쓸 수 있다.
 * [bodyColor]로 차체 색을 바꾼다(테마 색·가족별 색). [reflection]은 어두운 배경에서 바닥 반사.
 */
@Composable
fun CarIllustration(
    bodyColor: Color,
    modifier: Modifier = Modifier,
    reflection: Boolean = false,
) {
    Canvas(modifier.aspectRatio(ASPECT)) {
        drawGroundShadow()
        if (reflection) drawReflection(bodyColor)
        val body = bodyPath()
        drawBody(body, bodyColor)
        drawWindows(bodyColor)
        drawDetails()
        drawWheel(Offset(REAR_WHEEL_X * size.width, WHEEL_Y * size.height))
        drawWheel(Offset(FRONT_WHEEL_X * size.width, WHEEL_Y * size.height))
    }
}

private const val ASPECT = 2.35f
private const val REAR_WHEEL_X = 0.205f
private const val FRONT_WHEEL_X = 0.79f
private const val WHEEL_Y = 0.725f
private const val ARCH_R = 0.215f // 높이 기준. 캐스퍼처럼 짧고 높은 차는 바퀴가 커 보인다
private const val TIRE_R = 0.18f
private const val SILL_Y = 0.76f

private fun DrawScope.x(f: Float) = f * size.width
private fun DrawScope.y(f: Float) = f * size.height

private fun DrawScope.bodyPath(): Path = Path().apply {
    val archR = y(ARCH_R)
    moveTo(x(0.04f), y(SILL_Y))
    // 뒤: 거의 수직인 박스형 뒷면
    cubicTo(x(0.025f), y(0.62f), x(0.022f), y(0.38f), x(0.035f), y(0.24f))
    quadraticTo(x(0.045f), y(0.105f), x(0.11f), y(0.095f))
    // 지붕
    lineTo(x(0.60f), y(0.085f))
    quadraticTo(x(0.655f), y(0.085f), x(0.68f), y(0.13f))
    // 앞유리 → 짧은 보닛
    cubicTo(x(0.72f), y(0.22f), x(0.75f), y(0.31f), x(0.80f), y(0.37f))
    quadraticTo(x(0.90f), y(0.39f), x(0.95f), y(0.43f))
    // 둥근 앞코
    cubicTo(x(0.985f), y(0.47f), x(0.985f), y(0.66f), x(0.965f), y(SILL_Y))
    // 바닥 + 휠 아치
    lineTo(x(FRONT_WHEEL_X) + archR, y(SILL_Y))
    arcTo(Rect(Offset(x(FRONT_WHEEL_X), y(WHEEL_Y)), archR), 0f, -180f, false)
    lineTo(x(REAR_WHEEL_X) + archR, y(SILL_Y))
    arcTo(Rect(Offset(x(REAR_WHEEL_X), y(WHEEL_Y)), archR), 0f, -180f, false)
    close()
}

private fun DrawScope.drawGroundShadow() {
    val center = Offset(x(0.5f), y(0.885f))
    drawOval(
        brush = Brush.radialGradient(
            listOf(Color.Black.copy(alpha = 0.32f), Color.Black.copy(alpha = 0.12f), Color.Transparent),
            center = center,
            radius = x(0.5f),
        ),
        topLeft = Offset(x(0.02f), y(0.835f)),
        size = Size(x(0.96f), y(0.1f)),
    )
}

private fun DrawScope.drawReflection(color: Color) {
    drawRect(
        brush = Brush.verticalGradient(
            listOf(color.copy(alpha = 0.18f), Color.Transparent),
            startY = y(0.9f),
            endY = size.height,
        ),
        topLeft = Offset(x(0.08f), y(0.9f)),
        size = Size(x(0.84f), y(0.1f)),
    )
}

private fun DrawScope.drawBody(body: Path, color: Color) {
    drawPath(
        body,
        Brush.verticalGradient(
            0f to lerp(color, Color.White, 0.28f),
            0.45f to color,
            1f to lerp(color, Color.Black, 0.35f),
            startY = y(0.08f),
            endY = y(SILL_Y),
        ),
    )
    clipPath(body) {
        // 옆면 반사광 띠
        drawRect(
            Brush.verticalGradient(
                0f to Color.Transparent,
                0.5f to Color.White.copy(alpha = 0.22f),
                1f to Color.Transparent,
                startY = y(0.42f),
                endY = y(0.56f),
            ),
        )
        // 아래쪽 검은 클래딩
        drawRect(Color(0xFF1B1D21), topLeft = Offset(0f, y(0.665f)), size = Size(size.width, size.height))
        drawLine(Color.White.copy(alpha = 0.12f), Offset(0f, y(0.665f)), Offset(size.width, y(0.665f)), strokeWidth = y(0.006f))
    }
}

private fun DrawScope.drawWindows(body: Color) {
    val glass = Path().apply {
        moveTo(x(0.085f), y(0.17f))
        quadraticTo(x(0.09f), y(0.135f), x(0.125f), y(0.13f))
        lineTo(x(0.59f), y(0.125f))
        quadraticTo(x(0.635f), y(0.125f), x(0.655f), y(0.16f))
        cubicTo(x(0.69f), y(0.23f), x(0.715f), y(0.29f), x(0.745f), y(0.355f))
        lineTo(x(0.085f), y(0.37f))
        close()
    }
    drawPath(
        glass,
        Brush.linearGradient(
            listOf(Color(0xFF4A5A70), Color(0xFF1A2230), Color(0xFF2B3646)),
            start = Offset(x(0.1f), y(0.12f)),
            end = Offset(x(0.6f), y(0.38f)),
        ),
    )
    // 유리 위 사선 반사
    clipPath(glass) {
        drawLine(Color.White.copy(alpha = 0.18f), Offset(x(0.30f), y(0.40f)), Offset(x(0.40f), y(0.10f)), strokeWidth = x(0.035f))
        drawLine(Color.White.copy(alpha = 0.10f), Offset(x(0.36f), y(0.40f)), Offset(x(0.44f), y(0.10f)), strokeWidth = x(0.012f))
    }
    // B·C필러(차체색)
    val pillar = lerp(body, Color.Black, 0.15f)
    drawRect(pillar, topLeft = Offset(x(0.405f), y(0.12f)), size = Size(x(0.028f), y(0.26f)))
    drawRect(pillar, topLeft = Offset(x(0.145f), y(0.12f)), size = Size(x(0.018f), y(0.26f)))
}

private fun DrawScope.drawDetails() {
    val crease = Color.Black.copy(alpha = 0.22f)
    // 문 틈
    drawLine(crease, Offset(x(0.432f), y(0.38f)), Offset(x(0.44f), y(0.665f)), strokeWidth = y(0.006f))
    drawLine(crease, Offset(x(0.69f), y(0.37f)), Offset(x(0.675f), y(0.665f)), strokeWidth = y(0.006f))
    // 손잡이
    val handle = Color.Black.copy(alpha = 0.28f)
    drawRoundRect(handle, Offset(x(0.335f), y(0.44f)), Size(x(0.045f), y(0.025f)), CornerRadius(y(0.012f)))
    drawRoundRect(handle, Offset(x(0.585f), y(0.435f)), Size(x(0.045f), y(0.025f)), CornerRadius(y(0.012f)))
    // 사이드미러
    drawRoundRect(Color(0xFF22252A), Offset(x(0.705f), y(0.33f)), Size(x(0.04f), y(0.05f)), CornerRadius(y(0.02f)))
    // 앞: 픽셀 주간주행등 + 헤드램프
    drawRoundRect(Color(0xFFF5F7FA), Offset(x(0.925f), y(0.455f)), Size(x(0.045f), y(0.03f)), CornerRadius(y(0.015f)))
    for (i in 0 until 4) {
        drawRect(Color(0xFFE9EEF5), Offset(x(0.93f + i * 0.0105f), y(0.505f)), Size(x(0.006f), y(0.014f)))
    }
    drawCircle(
        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent), Offset(x(0.955f), y(0.47f)), x(0.06f)),
        radius = x(0.06f),
        center = Offset(x(0.955f), y(0.47f)),
    )
    // 뒤: 세로 테일램프
    drawRoundRect(Color(0xFFD7263D), Offset(x(0.026f), y(0.27f)), Size(x(0.016f), y(0.2f)), CornerRadius(y(0.01f)))
    drawRoundRect(Color(0xFFFF6B6B).copy(alpha = 0.6f), Offset(x(0.029f), y(0.29f)), Size(x(0.006f), y(0.16f)), CornerRadius(y(0.005f)))
}

private fun DrawScope.drawWheel(center: Offset) {
    val tire = y(TIRE_R)
    drawCircle(Color(0xFF111215), radius = tire, center = center)
    drawCircle(Color(0xFF2A2C31), radius = tire * 0.96f, center = center, style = Stroke(tire * 0.04f))
    val rim = tire * 0.66f
    drawCircle(
        Brush.linearGradient(
            listOf(Color(0xFFE6E9EE), Color(0xFF8C939D), Color(0xFFC9CED6)),
            start = Offset(center.x - rim, center.y - rim),
            end = Offset(center.x + rim, center.y + rim),
        ),
        radius = rim,
        center = center,
    )
    // 에어로 휠 스포크
    repeat(5) { i ->
        val angle = (i * 72.0 - 90.0) * PI / 180.0
        val end = Offset(center.x + (rim * 0.92f * cos(angle)).toFloat(), center.y + (rim * 0.92f * sin(angle)).toFloat())
        drawLine(Color(0xFF5E646D), center, end, strokeWidth = rim * 0.22f, cap = StrokeCap.Round)
    }
    drawCircle(Color(0xFF3A3E45), radius = rim * 0.3f, center = center)
    drawCircle(Color(0xFFDDE1E6), radius = rim * 0.14f, center = center)
}
