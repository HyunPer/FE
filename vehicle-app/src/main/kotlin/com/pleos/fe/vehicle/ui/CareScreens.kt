package com.pleos.fe.vehicle.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pleos.fe.api.care.MotionRisk
import com.pleos.fe.api.care.RiskLevel
import com.pleos.fe.api.model.RestStyle
import com.pleos.fe.api.model.TripResponse

// 쉼표 차량 화면. 주행 중에는 한눈에 읽히도록 큰 글자 한 줄 + 짧은 권유만 둔다.

private val CardColor = Color(0xFF1A2029)
private val Primary = Color(0xFFF2F5F9)
private val Secondary = Color(0xFF8D97A6)
private val Accent = Color(0xFF5AA2FF)

private val RiskLevel.color: Color
    get() = when (this) {
        RiskLevel.LOW -> Color(0xFF3DDC97)
        RiskLevel.CAUTION -> Color(0xFFFFC145)
        RiskLevel.HIGH -> Color(0xFFFF6B5E)
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
    Adaptive { portrait ->
        if (portrait) {
            Column(Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.spacedBy(32.dp)) {
                CareSummary(risk, restStyle, restAreaAheadM, parked, onDemo)
                ReasonsCard(risk, parked, onArrive, Modifier.fillMaxWidth().weight(1f))
            }
        } else {
            Row(Modifier.fillMaxSize().padding(48.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { CareSummary(risk, restStyle, restAreaAheadM, parked, onDemo) }
                Spacer(Modifier.width(40.dp))
                ReasonsCard(risk, parked, onArrive, Modifier.width(360.dp).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun CareSummary(
    risk: MotionRisk,
    restStyle: RestStyle,
    restAreaAheadM: Double?,
    parked: Boolean,
    onDemo: ((DemoSignal) -> Unit)?,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(20.dp).clip(CircleShape).background(risk.level.color))
            Spacer(Modifier.width(14.dp))
            Text("동승자 컨디션", color = Secondary, fontSize = 22.sp)
        }
        Spacer(Modifier.height(16.dp))
        Text(risk.level.headline, color = Primary, fontSize = 52.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(tipFor(risk.level, restStyle), color = Primary.copy(alpha = 0.85f), fontSize = 26.sp)
        // 위험이 높을 때만 경로상 다음 휴게소를 권한다(내비 경로 안내 중일 때)
        if (risk.level == RiskLevel.HIGH && restAreaAheadM != null) {
            Text(
                "다음 휴게소 ${formatKm(restAreaAheadM)} · 쉬어 가도 좋아요",
                color = risk.level.color,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        Spacer(Modifier.height(28.dp))
        RiskBar(risk)
        // 시연 패널도 정차 중에만(디버그 빌드)
        if (parked) onDemo?.let { DemoPanel(it) }
    }
}

@Composable
private fun ReasonsCard(risk: MotionRisk, parked: Boolean, onArrive: () -> Unit, modifier: Modifier) {
    Column(
        modifier.background(CardColor, RoundedCornerShape(24.dp)).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("왜 그런가요", color = Secondary, fontSize = 20.sp)
        if (risk.reasons.isEmpty()) Text("흔들릴 만한 구간이 없어요", color = Primary, fontSize = 24.sp)
        risk.reasons.forEach { Text("· $it", color = Primary, fontSize = 24.sp) }
        Spacer(Modifier.weight(1f))
        // 운전 중 화면 조작을 줄이려고, 버튼은 P단(정차)일 때만 보여준다. 주행 중 도착은 내비가 알려준다.
        if (parked) {
            OutlinedButton(onArrive, Modifier.fillMaxWidth().height(76.dp)) {
                Text("도착했어요", color = Primary, fontSize = 22.sp)
            }
        } else {
            Text("도착하면 자동으로 요약해요", color = Secondary, fontSize = 20.sp)
        }
    }
}

@Composable
private fun RiskBar(risk: MotionRisk) {
    Column {
        Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(CardColor)) {
            Box(Modifier.fillMaxWidth(risk.score / 100f).height(10.dp).clip(CircleShape).background(risk.level.color))
        }
        Spacer(Modifier.height(8.dp))
        Text("멀미 위험도 ${risk.score}", color = Secondary, fontSize = 18.sp)
    }
}

/** 디버그 빌드에서만 보이는 시연 패널. 에뮬레이터에서 바꿀 수 없는 도로 신호를 흉내 낸다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DemoPanel(onDemo: (DemoSignal) -> Unit) {
    FlowRow(
        Modifier.padding(top = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text("시연", color = Secondary, fontSize = 16.sp)
        DemoSignal.entries.forEach { signal ->
            OutlinedButton({ onDemo(signal) }, Modifier.height(52.dp)) { Text(signal.label, color = Secondary, fontSize = 16.sp) }
        }
    }
}

private fun formatKm(m: Double): String = if (m >= 1_000) "%.0fkm".format(m / 1_000) else "${m.toInt()}m"

/** 도착: 오늘 이동을 사실로만 요약하고, 정차 중이니 30초 숨 고르기를 권한다. 기분은 폰에서 본인이 남긴다. */
@Composable
fun ArrivalScreen(trip: TripResponse, onDone: () -> Unit) {
    var breathing by remember(trip.id) { mutableStateOf(false) }
    if (breathing) {
        BreathingScreen(onFinish = { breathing = false })
        return
    }
    Column(
        Modifier.fillMaxSize().padding(56.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("도착했어요, 잠깐 쉼표", color = Primary, fontSize = 48.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        Text(
            buildList {
                add("${trip.drivingMinutes}분 이동")
                if (trip.congestionMinutes > 0) add("막힘 ${trip.congestionMinutes}분")
                if (trip.sharpTurns > 0) add("급커브 ${trip.sharpTurns}곳")
            }.joinToString(" · "),
            color = Primary.copy(alpha = 0.85f),
            fontSize = 28.sp,
        )
        Text("가장 흔들렸을 때 멀미 위험도 ${trip.peakRisk}", color = Secondary, fontSize = 22.sp, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(28.dp))
        Text("내리기 전에 30초만 숨을 고르고, 폰에서 오늘 이동을 한 줄로 남겨 보세요", color = Secondary, fontSize = 22.sp)
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button({ breathing = true }, Modifier.height(76.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                Text("30초 숨 고르기", fontSize = 24.sp)
            }
            OutlinedButton(onDone, Modifier.height(76.dp)) { Text("확인", color = Primary, fontSize = 24.sp) }
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
            Box(Modifier.size(320.dp).scale(scale.value).clip(CircleShape).background(Accent.copy(alpha = 0.35f)))
            Box(Modifier.size(120.dp).clip(CircleShape).background(Accent.copy(alpha = 0.8f)))
        }
    }
    val guide: @Composable () -> Unit = {
        Column {
            Text(if (inhale) "천천히 들이쉬고" else "길게 내쉬고", color = Primary, fontSize = 52.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("$round / $BREATH_ROUNDS", color = Secondary, fontSize = 24.sp)
            Spacer(Modifier.height(36.dp))
            OutlinedButton(onFinish, Modifier.height(76.dp)) { Text("그만하기", color = Primary, fontSize = 22.sp) }
        }
    }
    Adaptive { portrait ->
        if (portrait) {
            Column(
                Modifier.fillMaxSize().padding(56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                circle()
                Spacer(Modifier.height(48.dp))
                guide()
            }
        } else {
            Row(Modifier.fillMaxSize().padding(56.dp), verticalAlignment = Alignment.CenterVertically) {
                circle()
                Spacer(Modifier.width(56.dp))
                guide()
            }
        }
    }
}

private const val BREATH_ROUNDS = 3
private const val INHALE_MS = 4_000
private const val EXHALE_MS = 6_000
private const val MIN_SCALE = 0.45f
