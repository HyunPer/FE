package com.pleos.fe.vehicle.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.pleos.fe.api.care.MotionRisk
import com.pleos.fe.api.care.RiskFactor
import com.pleos.fe.api.care.RiskLevel
import com.pleos.fe.api.model.RestStyle
import com.pleos.fe.api.model.TripResponse
import kotlin.math.roundToInt

// 쉼표 차량 화면. 주행 중에는 한눈에 읽히도록 큰 숫자 하나 + 큰 글자 한 줄 + 짧은 권유만 둔다.

private val Primary: Color @Composable @ReadOnlyComposable get() = TextColor
private val Secondary: Color @Composable @ReadOnlyComposable get() = SubText

private val RiskLevel.color: Color
    @Composable @ReadOnlyComposable
    get() = when (this) {
        RiskLevel.LOW -> LocalVehicleColors.current.riskLow
        RiskLevel.CAUTION -> LocalVehicleColors.current.riskCaution
        RiskLevel.HIGH -> LocalVehicleColors.current.riskHigh
    }

private val RiskLevel.headline: String
    get() = when (this) {
        RiskLevel.LOW -> "편안한 길이에요"
        RiskLevel.CAUTION -> "조금 흔들릴 수 있어요"
        RiskLevel.HIGH -> "멀미 주의"
    }

/**
 * 권유는 "~해 보세요" 수준. 진단·처방처럼 들리지 않게 쓴다.
 * 주의 이상이면 가장 멀미를 잘 타는 가족이 폰에서 고른 쉬는 방식으로 권한다.
 */
private fun tipFor(level: RiskLevel, rest: RestStyle): String = when (level) {
    RiskLevel.LOW -> "지금처럼 가면 돼요"
    else -> when (rest) {
        RestStyle.FRESH_AIR -> "창문을 조금 열어 바깥 바람을 쐬어 보세요"
        RestStyle.BREATHING -> "4초 들이쉬고 6초 내쉬며 천천히 숨 쉬어 보세요"
        RestStyle.MUSIC -> "좋아하는 음악을 작게 틀고 창밖 먼 곳을 바라보세요"
        RestStyle.QUIET -> "휴대폰은 내려 두고, 잠시 눈을 감고 기대어 보세요"
    }
}

@Composable
fun CareScreen(
    risk: MotionRisk,
    onArrive: () -> Unit,
    restStyle: RestStyle = RestStyle.QUIET,
    restAreaAheadM: Double? = null,
    parked: Boolean = true,
    onDemo: ((DemoSignal) -> Unit)? = null,
) {
    // 바탕에 번지는 빛이 위험도 색을 따라 바뀐다. 숫자를 읽지 않아도 화면 분위기로 상태가 전해진다
    val glow by animateColorAsState(risk.level.color, tween(COLOR_MS), label = "glow")
    val strength = if (LocalVehicleColors.current.dark) 0.30f else 0.20f
    AdaptiveTall { portrait, tall ->
        if (portrait) {
            Column(Modifier.fillMaxSize().ambientGlow(glow, 0.3f, 0.2f, strength = strength).padding(48.dp), verticalArrangement = Arrangement.spacedBy(32.dp)) {
                CareSummary(risk, restStyle, restAreaAheadM, parked, onDemo, stacked = true)
                ReasonsCard(risk, parked, onArrive, Modifier.fillMaxWidth().weight(1f))
            }
        } else if (tall) {
            // 세로 여유가 큰 가로 화면: 위에 큰 계기와 글, 아래에 이유 카드를 화면 너비로 놓아 빈 곳을 없앤다
            Column(
                Modifier.fillMaxSize().ambientGlow(glow, 0.2f, 0.3f, strength = strength).padding(horizontal = 56.dp, vertical = 36.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically),
            ) {
                CareSummary(risk, restStyle, restAreaAheadM, parked, onDemo = null, stacked = false, gauge = 290.dp, large = true)
                ReasonsCard(risk, parked, onArrive, Modifier.fillMaxWidth(), columns = 2)
                if (parked) onDemo?.let { DemoPanel(it) }
            }
        } else {
            Row(
                Modifier.fillMaxSize().ambientGlow(glow, 0.14f, 0.5f, strength = strength).padding(horizontal = 48.dp, vertical = 40.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) { CareSummary(risk, restStyle, restAreaAheadM, parked, onDemo, stacked = false) }
                Spacer(Modifier.width(40.dp))
                ReasonsCard(risk, parked, onArrive, Modifier.width(400.dp).fillMaxHeight())
            }
        }
    }
}

/** 위험도 둥근 계기 + 한 줄 상태 + 권유. [stacked]면(세로 화면) 계기를 글 위에 놓는다. */
@Composable
private fun CareSummary(
    risk: MotionRisk,
    restStyle: RestStyle,
    restAreaAheadM: Double?,
    parked: Boolean,
    onDemo: ((DemoSignal) -> Unit)?,
    stacked: Boolean,
    gauge: Dp = if (stacked) 230.dp else 190.dp,
    large: Boolean = false,
) {
    val color by animateColorAsState(risk.level.color, tween(COLOR_MS), label = "riskColor")
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(12.dp))
            Text("동승자 컨디션", color = Secondary, fontSize = 22.sp)
        }
        Spacer(Modifier.height(20.dp))
        if (stacked) {
            RiskGauge(risk, color, gauge)
            Spacer(Modifier.height(28.dp))
            CareText(risk, color, restStyle, restAreaAheadM, large)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RiskGauge(risk, color, gauge)
                Spacer(Modifier.width(if (large) 56.dp else 36.dp))
                CareText(risk, color, restStyle, restAreaAheadM, large)
            }
        }
        // 시연 패널도 정차 중에만(디버그 빌드)
        if (parked) onDemo?.let { DemoPanel(it) }
    }
}

@Composable
private fun CareText(risk: MotionRisk, color: Color, restStyle: RestStyle, restAreaAheadM: Double?, large: Boolean = false) {
    Column {
        Text(
            risk.level.headline,
            color = Primary,
            fontSize = if (large) 68.sp else 48.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.02).em,
        )
        Spacer(Modifier.height(if (large) 14.dp else 10.dp))
        Text(
            tipFor(risk.level, restStyle),
            color = Primary.copy(alpha = 0.85f),
            fontSize = if (large) 30.sp else 26.sp,
            lineHeight = if (large) 40.sp else 34.sp,
        )
        // 위험이 높을 때만 경로상 다음 휴게소를 권한다(내비 경로 안내 중일 때)
        if (risk.level == RiskLevel.HIGH && restAreaAheadM != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                "다음 휴게소 ${formatKm(restAreaAheadM)} · 쉬어 가도 좋아요",
                color = color,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(color.copy(alpha = 0.14f), ButtonShape)
                    .border(1.dp, color.copy(alpha = 0.32f), ButtonShape)
                    .padding(horizontal = 22.dp, vertical = 12.dp),
            )
        }
    }
}

/** 멀미 위험도(0–100)를 270° 둥근 계기로. 값이 바뀌면 부드럽게 움직인다. */
@Composable
private fun RiskGauge(risk: MotionRisk, color: Color, diameter: Dp) {
    val progress by animateFloatAsState(risk.score / 100f, tween(GAUGE_MS), label = "risk")
    val track = Track
    Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.075f
            // 번지는 빛이 잘리지 않게 선 굵기만큼 안쪽에 그린다
            val topLeft = Offset(stroke * 0.85f, stroke * 0.85f)
            val arc = Size(size.width - stroke * 1.7f, size.height - stroke * 1.7f)
            drawArc(track, GAUGE_START, GAUGE_SWEEP, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
            if (progress > 0.005f) {
                // 선 아래에 같은 색을 넓고 옅게 한 번 더 그려 빛이 번지는 느낌을 낸다
                drawArc(color.copy(alpha = 0.13f), GAUGE_START, GAUGE_SWEEP * progress, false, topLeft, arc, style = Stroke(stroke * 1.7f, cap = StrokeCap.Round))
                drawArc(color, GAUGE_START, GAUGE_SWEEP * progress, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(progress * 100).roundToInt()}", color = Primary, fontSize = (diameter.value * 0.32f).sp, fontWeight = FontWeight.Bold)
            Text("멀미 위험도", color = Secondary, fontSize = (diameter.value * 0.085f).sp)
        }
    }
}

/** 왜 그런지: 위험도를 올린 항목과, 항목별 상한 대비 얼마나 찼는지. */
@Composable
private fun ReasonsCard(risk: MotionRisk, parked: Boolean, onArrive: () -> Unit, modifier: Modifier, columns: Int = 1) {
    val color by animateColorAsState(risk.level.color, tween(COLOR_MS), label = "factorColor")
    Column(modifier.glass().padding(horizontal = 32.dp, vertical = 24.dp)) {
        Text("왜 그런가요", color = Secondary, fontSize = 20.sp)
        Spacer(Modifier.height(18.dp))
        val rows = risk.factors.ifEmpty { risk.reasons.map { RiskFactor(it, 0, 0) } }
        if (rows.isEmpty()) Text("흔들릴 만한 구간이 없어요", color = Primary, fontSize = 22.sp)
        if (columns > 1) {
            // 화면 너비를 쓰는 카드: 항목을 여러 열로 나눠 크게 보여준다
            rows.chunked(columns).forEach { line ->
                Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                    line.forEach { Box(Modifier.weight(1f)) { FactorRow(it, color, large = true) } }
                    repeat(columns - line.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(8.dp))
        } else {
            rows.forEach { FactorRow(it, color) }
            Spacer(Modifier.weight(1f))
        }
        // 운전 중 화면 조작을 줄이려고, 버튼은 P단(정차)일 때만 보여준다. 주행 중 도착은 내비가 알려준다.
        if (parked) {
            SecondaryButton("도착했어요", onArrive, if (columns > 1) Modifier.width(280.dp) else Modifier.fillMaxWidth(), height = 64.dp)
        } else {
            Text("도착하면 자동으로 요약해요", color = Secondary, fontSize = 18.sp)
        }
    }
}

@Composable
private fun FactorRow(factor: RiskFactor, color: Color, large: Boolean = false) {
    val bar = if (large) 8.dp else 5.dp
    Column(Modifier.padding(bottom = if (large) 22.dp else 14.dp)) {
        Text(factor.label, color = Primary, fontSize = if (large) 26.sp else 20.sp)
        if (factor.max > 0) {
            val fill by animateFloatAsState((factor.points.toFloat() / factor.max).coerceIn(0.04f, 1f), tween(GAUGE_MS), label = "factor")
            Spacer(Modifier.height(if (large) 10.dp else 7.dp))
            Box(Modifier.fillMaxWidth().height(bar).clip(CircleShape).background(Track)) {
                Box(Modifier.fillMaxWidth(fill).height(bar).clip(CircleShape).background(color))
            }
        }
    }
}

/** 디버그 빌드에서만 보이는 시연 패널. 내비 경로 안내 없이는 오지 않는 도로 신호를 흉내 낸다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DemoPanel(onDemo: (DemoSignal) -> Unit) {
    FlowRow(
        Modifier.padding(top = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text("시연", color = Secondary, fontSize = 14.sp)
        DemoSignal.entries.forEach { signal ->
            OutlinedButton({ onDemo(signal) }, Modifier.height(44.dp), shape = ButtonShape) { Text(signal.label, color = Secondary, fontSize = 14.sp) }
        }
    }
}

private fun formatKm(m: Double): String = if (m >= 1_000) "%.0fkm".format(m / 1_000) else "${m.toInt()}m"

/** 도착: 오늘 이동을 사실로만 요약하고, 정차 중이니 30초 숨 고르기를 권한다. 기분은 폰에서 본인이 남긴다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArrivalScreen(trip: TripResponse, onDone: () -> Unit) {
    var breathing by remember(trip.id) { mutableStateOf(false) }
    if (breathing) {
        BreathingScreen(onFinish = { breathing = false })
        return
    }
    val c = LocalVehicleColors.current
    AdaptiveTall { _, tall ->
        Column(
            Modifier.fillMaxSize().ambientGlow(c.accent, 0.82f, 0.12f, strength = if (c.dark) 0.30f else 0.18f).padding(56.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "도착했어요, 잠깐 쉼표",
                color = Primary,
                fontSize = if (tall) 80.sp else 48.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.02).em,
            )
            Spacer(Modifier.height(if (tall) 14.dp else 8.dp))
            Text("내리기 전에 30초만 숨을 고르고, 폰에서 오늘 이동을 한 줄로 남겨 보세요", color = Secondary, fontSize = if (tall) 26.sp else 22.sp)
            Spacer(Modifier.height(if (tall) 44.dp else 22.dp))
            // 숫자 칸은 화면 너비를 똑같이 나눠 빈 곳 없이 채운다
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val tile = Modifier.weight(1f).fillMaxHeight()
                StatTile("이동", "${trip.drivingMinutes}", "분", tile, tall)
                if (trip.congestionMinutes > 0) StatTile("막힘", "${trip.congestionMinutes}", "분", tile, tall)
                if (trip.sharpTurns > 0) StatTile("급커브", "${trip.sharpTurns}", "곳", tile, tall)
                StatTile("가장 흔들렸을 때", "${trip.peakRisk}", "", tile, tall, peakColor(trip.peakRisk))
            }
            Spacer(Modifier.height(if (tall) 44.dp else 24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PrimaryButton("30초 숨 고르기", { breathing = true })
                SecondaryButton("확인", onDone, fontSize = 24.sp)
            }
        }
    }
}

@Composable
private fun peakColor(peakRisk: Int): Color = when {
    peakRisk >= 65 -> RiskLevel.HIGH.color
    peakRisk >= 35 -> RiskLevel.CAUTION.color
    else -> RiskLevel.LOW.color
}

/** 도착 요약의 숫자 하나. */
@Composable
private fun StatTile(label: String, value: String, unit: String, modifier: Modifier, large: Boolean, valueColor: Color = Primary) {
    Column(modifier.glass().padding(horizontal = if (large) 28.dp else 22.dp, vertical = if (large) 24.dp else 14.dp)) {
        Text(label, color = Secondary, fontSize = if (large) 22.sp else 18.sp, maxLines = 1)
        Spacer(Modifier.height(if (large) 10.dp else 2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = valueColor, fontSize = if (large) 84.sp else 44.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em)
            if (unit.isNotEmpty()) {
                Text(unit, color = Secondary, fontSize = if (large) 28.sp else 20.sp, modifier = Modifier.padding(start = 6.dp, bottom = if (large) 16.dp else 8.dp))
            }
        }
    }
}

/** 30초 숨 고르기: 4초 들이쉬고 6초 내쉬기 × 3번. 원이 커지면 들이쉬고, 작아지면 내쉰다. */
@Composable
fun BreathingScreen(onFinish: () -> Unit) {
    val scale = remember { Animatable(MIN_SCALE) }
    var inhale by remember { mutableStateOf(true) }
    var round by remember { mutableIntStateOf(1) }
    LaunchedEffect(Unit) {
        repeat(BREATH_ROUNDS) { i ->
            round = i + 1
            inhale = true
            scale.animateTo(1f, tween(INHALE_MS, easing = LinearEasing))
            inhale = false
            scale.animateTo(MIN_SCALE, tween(EXHALE_MS, easing = LinearEasing))
        }
        onFinish()
    }
    val circle: @Composable () -> Unit = {
        Box(Modifier.size(320.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(320.dp).clip(CircleShape).background(Accent.copy(alpha = 0.07f)))
            Box(Modifier.size(320.dp).scale(scale.value).clip(CircleShape).background(Accent.copy(alpha = 0.35f)))
            Box(Modifier.size(120.dp).clip(CircleShape).background(Accent.copy(alpha = 0.85f)))
        }
    }
    val guide: @Composable () -> Unit = {
        Column {
            Text("30초 숨 고르기 · $round / $BREATH_ROUNDS", color = Secondary, fontSize = 22.sp)
            Spacer(Modifier.height(12.dp))
            Text(if (inhale) "천천히 들이쉬고" else "길게 내쉬고", color = Primary, fontSize = 60.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em)
            Spacer(Modifier.height(36.dp))
            SecondaryButton("그만하기", onFinish)
        }
    }
    val c = LocalVehicleColors.current
    val glow = if (c.dark) 0.30f else 0.18f
    Adaptive { portrait ->
        if (portrait) {
            Column(
                Modifier.fillMaxSize().ambientGlow(c.accent, 0.5f, 0.36f, strength = glow).padding(56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                circle()
                Spacer(Modifier.height(48.dp))
                guide()
            }
        } else {
            Row(Modifier.fillMaxSize().ambientGlow(c.accent, 0.2f, 0.5f, strength = glow).padding(56.dp), verticalAlignment = Alignment.CenterVertically) {
                circle()
                Spacer(Modifier.width(56.dp))
                guide()
            }
        }
    }
}

private const val GAUGE_START = 135f
private const val GAUGE_SWEEP = 270f
private const val GAUGE_MS = 700
private const val COLOR_MS = 500
private const val BREATH_ROUNDS = 3
private const val INHALE_MS = 4_000
private const val EXHALE_MS = 6_000
private const val MIN_SCALE = 0.45f
