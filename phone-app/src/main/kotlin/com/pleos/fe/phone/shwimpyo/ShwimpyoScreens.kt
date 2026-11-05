package com.pleos.fe.phone.shwimpyo

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pleos.fe.api.model.Mood
import com.pleos.fe.api.model.MotionSensitivity
import com.pleos.fe.api.model.RestStyle
import com.pleos.fe.api.model.RiderConditionDto
import com.pleos.fe.api.care.WeeklyInsights
import com.pleos.fe.api.model.TripResponse
import com.pleos.fe.api.model.VehicleDetailResponse
import com.pleos.fe.api.model.VehicleRole
import com.pleos.fe.phone.ui.Avatar
import com.pleos.fe.phone.ui.BottomNav
import com.pleos.fe.phone.ui.FamilyColors
import com.pleos.fe.phone.ui.LocalPalette
import com.pleos.fe.phone.ui.PCard
import com.pleos.fe.phone.ui.PChip
import com.pleos.fe.phone.ui.PText
import com.pleos.fe.phone.ui.PhoneSurface
import com.pleos.fe.phone.ui.PhoneTab
import com.pleos.fe.phone.ui.PillButton
import com.pleos.fe.phone.ui.SectionTitle
import com.pleos.fe.ui.CarIllustration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// 쉼표 폰 앱 화면. 모두 상태 없는 함수라 캡처 테스트·미리보기에서 그대로 그릴 수 있다.

private val CarColor = Color(0xFF6E7A60)

/** 앱 진입점: 단계(가입 → 차량 연결 → 사용)와 탭을 고른다. */
@Composable
fun ShwimpyoApp(viewModel: ShwimpyoViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(PhoneTab.HOME) }
    PhoneSurface {
        when (state.phase) {
            Phase.LOADING -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            Phase.SIGN_UP -> SignUpScreen(state.busy, viewModel::signUp)
            Phase.PAIR -> PairScreen(state.me?.nickname.orEmpty(), state.busy, viewModel::pair)
            Phase.READY -> Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    when (tab) {
                        PhoneTab.HOME -> HomeScreen(state, viewModel::checkIn, viewModel::refresh)
                        PhoneTab.HISTORY -> HistoryScreen(state.trips)
                        PhoneTab.FAMILY -> FamilyScreen(state.family, state.isOwner, state.busy, viewModel::regeneratePairingCode)
                        PhoneTab.CONDITION -> ConditionScreen(state.condition, state.busy, viewModel::saveCondition)
                    }
                }
                BottomNav(tab) { tab = it }
            }
        }
        state.message?.let { MessageBar(it, viewModel::dismissMessage, Modifier.align(Alignment.BottomCenter)) }
    }
}

@Composable
fun SignUpScreen(busy: Boolean, onSignUp: (String) -> Unit) {
    var nickname by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        PText("쉼표", 44.sp, weight = FontWeight.Bold)
        PText("함께 타는 사람의 몸과 마음을 챙기는 차", 16.sp, Modifier.padding(top = 6.dp), sub = true)
        CarIllustration(CarColor, Modifier.fillMaxWidth().padding(vertical = 28.dp))
        OutlinedTextField(nickname, { nickname = it }, Modifier.fillMaxWidth(), label = { Text("가족이 부르는 이름 (예: 엄마)") }, singleLine = true)
        Spacer(Modifier.height(16.dp))
        PillButton("시작하기", Modifier.fillMaxWidth(), onClick = { if (nickname.isNotBlank() && !busy) onSignUp(nickname) })
    }
}

@Composable
fun PairScreen(nickname: String, busy: Boolean, onPair: (String) -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        PText("${nickname}님, 차와 연결해요", 28.sp, weight = FontWeight.SemiBold, maxLines = 2)
        PText("차 화면에 뜬 6자리 연결 코드를 입력하세요", 15.sp, Modifier.padding(top = 8.dp), sub = true)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(code, { code = it.take(6) }, Modifier.fillMaxWidth(), label = { Text("연결 코드") }, singleLine = true)
        Spacer(Modifier.height(16.dp))
        PillButton("연결하기", Modifier.fillMaxWidth(), onClick = { if (code.length == 6 && !busy) onPair(code) })
    }
}

/** 홈: 우리 차 + 마지막 이동 요약 + 아직 안 남긴 도착 체크인. */
@Composable
fun HomeScreen(state: ShwimpyoState, onCheckIn: (String, Mood, String) -> Unit, onRefresh: () -> Unit) {
    val vehicle = state.vehicle
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 20.dp, top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                PText(vehicle?.nickname ?: vehicle?.model ?: "우리 차", 22.sp, weight = FontWeight.SemiBold)
                PText("${state.me?.nickname.orEmpty()}님 · 쉼표", 13.sp, sub = true)
            }
            PillButton("새로고침", filled = false, onClick = onRefresh)
        }
        CarIllustration(CarColor, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp))

        val last = state.trips.firstOrNull { it.endedAt != null }
        Column(Modifier.padding(horizontal = 20.dp)) {
            if (last == null) {
                PCard(Modifier.fillMaxWidth()) {
                    PText("아직 이동 기록이 없어요", 18.sp, weight = FontWeight.Medium)
                    PText("차에 타면 쉼표가 길을 살피고, 도착하면 여기서 한 줄을 남길 수 있어요", 13.sp, Modifier.padding(top = 6.dp), sub = true, maxLines = 2)
                }
            } else {
                PText("마지막 이동 · ${last.startedAt.toDisplayTime()}", 13.sp, Modifier.padding(start = 4.dp, bottom = 8.dp), sub = true)
                TripFacts(last)
            }
            state.pendingCheckIn?.let { CheckInCard(it, state.busy, onCheckIn) }
        }
    }
}

/** 큰 숫자로 보는 이동 요약: 이동 시간 · 막힘 · 급커브 · 최고 멀미 위험도. */
@Composable
private fun TripFacts(trip: TripResponse) {
    PCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Fact("이동", "${trip.drivingMinutes}", "분", Modifier.weight(1f))
            Fact("막힘", "${trip.congestionMinutes}", "분", Modifier.weight(1f))
            Fact("급커브", "${trip.sharpTurns}", "곳", Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        RiskLine(trip.peakRisk)
    }
}

@Composable
private fun Fact(label: String, value: String, unit: String, modifier: Modifier) {
    Column(modifier) {
        PText(label, 13.sp, sub = true)
        Row(verticalAlignment = Alignment.Bottom) {
            PText(value, 34.sp, weight = FontWeight.Light)
            PText(unit, 14.sp, Modifier.padding(start = 2.dp, bottom = 6.dp), sub = true)
        }
    }
}

@Composable
private fun RiskLine(peakRisk: Int) {
    val p = LocalPalette.current
    val color = when {
        peakRisk >= 65 -> Color(0xFFFF6B5E)
        peakRisk >= 35 -> Color(0xFFFFB020)
        else -> Color(0xFF22A06B)
    }
    Column {
        Row {
            PText("가장 흔들렸을 때 멀미 위험도", 13.sp, Modifier.weight(1f), sub = true)
            PText("$peakRisk", 13.sp, weight = FontWeight.SemiBold, color = color)
        }
        Box(Modifier.fillMaxWidth().padding(top = 8.dp).height(6.dp).clip(CircleShape).background(p.line)) {
            Box(Modifier.fillMaxWidth(peakRisk / 100f).height(6.dp).clip(CircleShape).background(color))
        }
    }
}

/** 도착 체크인. 상담이 아니라 스스로 남기는 기분 하나 + 한 줄. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CheckInCard(trip: TripResponse, busy: Boolean, onCheckIn: (String, Mood, String) -> Unit) {
    var mood by remember(trip.id) { mutableStateOf<Mood?>(null) }
    var note by remember(trip.id) { mutableStateOf("") }
    PCard(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        PText("오늘 이동 어땠나요?", 18.sp, weight = FontWeight.SemiBold)
        PText("기분 하나만 골라도 돼요", 13.sp, Modifier.padding(top = 4.dp), sub = true)
        Spacer(Modifier.height(14.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Mood.entries.forEach { m -> PChip(m.label, m == mood) { mood = m } }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(note, { note = it.take(200) }, Modifier.fillMaxWidth(), placeholder = { Text("한 줄 남기기 (선택)") }, singleLine = true)
        Spacer(Modifier.height(12.dp))
        PillButton("남기기", Modifier.fillMaxWidth(), onClick = { mood?.let { if (!busy) onCheckIn(trip.id, it, note) } })
    }
}

/** 기록: 이번 주 요약 + 최근 이동과 그때의 기분. */
@Composable
fun HistoryScreen(trips: List<TripResponse>, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()) {
    val ended = trips.filter { it.endedAt != null }
    val week = remember(trips, now) { WeeklyInsights.of(trips, now, zone) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        PText("기록", 22.sp, weight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        PCard(Modifier.fillMaxWidth()) {
            PText("이번 주 ${week.tripCount}번 이동", 13.sp, sub = true)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Fact("평균 이동", "${week.averageMinutes}", "분", Modifier.weight(1f))
                Fact("막힘 합계", "${week.congestionMinutes}", "분", Modifier.weight(1f))
                Fact("힘들었던 이동", "${week.hardTrips}", "번", Modifier.weight(1f))
            }
            week.pattern?.let {
                PText(it, 14.sp, Modifier.padding(top = 14.dp), weight = FontWeight.Medium, color = LocalPalette.current.accent, maxLines = 2)
            }
        }
        SectionTitle("최근 이동")
        if (ended.isEmpty()) PText("아직 기록이 없어요", 15.sp, sub = true)
        ended.forEach { trip ->
            PCard(Modifier.fillMaxWidth().padding(bottom = 10.dp), padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        PText(trip.startedAt.toDisplayTime(), 15.sp, weight = FontWeight.Medium)
                        PText(
                            "${trip.drivingMinutes}분 · 막힘 ${trip.congestionMinutes}분 · 급커브 ${trip.sharpTurns}곳 · 위험도 ${trip.peakRisk}",
                            13.sp,
                            sub = true,
                        )
                        trip.myNote?.let { PText("“$it”", 13.sp, Modifier.padding(top = 4.dp), maxLines = 2) }
                    }
                    PChip(trip.myMood?.label ?: "미기록", trip.myMood != null)
                }
            }
        }
    }
}

/** 가족: 초대용 연결 코드(공유·OWNER 재발급)와 멤버 목록. */
@Composable
fun FamilyScreen(family: VehicleDetailResponse?, isOwner: Boolean, busy: Boolean, onRegenerate: () -> Unit) {
    val context = LocalContext.current
    val p = LocalPalette.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        PText("가족", 22.sp, weight = FontWeight.SemiBold)
        PText(family?.nickname ?: family?.model ?: "", 13.sp, Modifier.padding(top = 2.dp), sub = true)
        if (family == null) {
            PText("가족 정보를 불러오지 못했어요", 15.sp, Modifier.padding(top = 16.dp), sub = true)
            return@Column
        }
        PCard(Modifier.fillMaxWidth().padding(top = 16.dp)) {
            PText("가족 초대 코드", 13.sp, sub = true)
            Row(Modifier.padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                family.pairingCode.forEach { c ->
                    Box(Modifier.size(42.dp, 52.dp).clip(RoundedCornerShape(12.dp)).background(p.background), contentAlignment = Alignment.Center) {
                        PText(c.toString(), 26.sp, weight = FontWeight.SemiBold)
                    }
                }
            }
            PText("가족이 쉼표 폰 앱에서 이 코드를 입력하면 이 차와 연결돼요. 차 화면에도 같은 코드가 떠요.", 13.sp, sub = true, maxLines = 3)
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton("공유하기", onClick = {
                    val text = "쉼표 앱에서 이 코드를 입력하면 우리 차와 연결돼요: ${family.pairingCode}"
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                    context.startActivity(Intent.createChooser(send, "가족 초대 코드 공유"))
                })
                if (isOwner) PillButton("새 코드 받기", filled = false, onClick = { if (!busy) onRegenerate() })
            }
            if (isOwner) PText("새 코드를 받으면 이전 코드는 바로 쓸 수 없어요", 12.sp, Modifier.padding(top = 8.dp), sub = true)
        }
        SectionTitle("함께 타는 가족 ${family.members.size}명")
        PCard(Modifier.fillMaxWidth(), padding = 8.dp) {
            family.members.forEachIndexed { i, m ->
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(m.nickname, FamilyColors[i % FamilyColors.size], 40.dp)
                    PText(m.nickname, 16.sp, Modifier.weight(1f).padding(start = 12.dp), weight = FontWeight.Medium)
                    PChip(if (m.role == VehicleRole.OWNER) "차 주인" else "가족", m.role == VehicleRole.OWNER)
                }
            }
        }
    }
}

/** 내 컨디션: 차량 앱이 멀미 위험도 배율과 권하는 쉬는 방식을 정할 때 쓴다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConditionScreen(condition: RiderConditionDto, busy: Boolean, onSave: (RiderConditionDto) -> Unit) {
    var sensitivity by remember(condition) { mutableStateOf(condition.motionSensitivity) }
    var rest by remember(condition) { mutableStateOf(condition.restStyle) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        PText("내 컨디션", 22.sp, weight = FontWeight.SemiBold)
        PText("함께 탄 가족 중 가장 멀미를 잘 타는 사람 기준으로 길을 살펴요", 13.sp, Modifier.padding(top = 6.dp), sub = true, maxLines = 2)
        SectionTitle("멀미를 얼마나 타나요")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MotionSensitivity.entries.forEach { s -> PChip(s.label, s == sensitivity) { sensitivity = s } }
        }
        SectionTitle("어떻게 쉬는 게 좋아요")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RestStyle.entries.forEach { r -> PChip(r.label, r == rest) { rest = r } }
        }
        Spacer(Modifier.height(28.dp))
        PillButton("저장", Modifier.fillMaxWidth(), onClick = { if (!busy) onSave(RiderConditionDto(sensitivity, rest)) })
    }
}

@Composable
private fun MessageBar(text: String, onDismiss: () -> Unit, modifier: Modifier) {
    val p = LocalPalette.current
    Box(
        modifier
            .padding(start = 20.dp, end = 20.dp, bottom = 96.dp)
            .fillMaxWidth()
            .clip(CircleShape)
            .background(p.strong)
            .clickable(onClick = onDismiss)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) { PText(text, 14.sp, color = p.onStrong, maxLines = 2) }
}

val Mood.label: String
    get() = when (this) {
        Mood.CALM -> "편안"
        Mood.OK -> "괜찮음"
        Mood.TIRED -> "피곤"
        Mood.STRESSED -> "답답"
        Mood.QUEASY -> "속이 안 좋음"
    }

val MotionSensitivity.label: String
    get() = when (this) {
        MotionSensitivity.LOW -> "거의 안 타요"
        MotionSensitivity.MEDIUM -> "가끔 타요"
        MotionSensitivity.HIGH -> "잘 타요"
    }

val RestStyle.label: String
    get() = when (this) {
        RestStyle.QUIET -> "조용히"
        RestStyle.BREATHING -> "숨 고르기"
        RestStyle.MUSIC -> "음악"
        RestStyle.FRESH_AIR -> "바깥 바람"
    }

private val DisplayTime = DateTimeFormatter.ofPattern("M월 d일 HH:mm")

/** 서버 시각(ISO-8601)을 폰 시간대로. 형식이 다르면 그대로 보여준다. */
private fun String.toDisplayTime(): String =
    runCatching { Instant.parse(this).atZone(ZoneId.systemDefault()).format(DisplayTime) }.getOrDefault(this)
