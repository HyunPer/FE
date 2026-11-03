package com.pleos.fe.phone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pleos.fe.api.model.ApplyLogResponse
import com.pleos.fe.api.model.ApplySource
import com.pleos.fe.api.model.ButtonStyle
import com.pleos.fe.api.model.ProfileResponse
import com.pleos.fe.api.model.ReservationResponse
import com.pleos.fe.api.model.ThemeSkinDto
import com.pleos.fe.api.model.VehicleDetailResponse
import com.pleos.fe.api.model.WidgetStyle
import com.pleos.fe.ui.CarDisplayPreview
import com.pleos.fe.ui.CarIllustration

// 폰 앱 화면(디자인 시안 겸 실제 화면 틀). 모두 상태 없는 함수라 ViewModel 상태를 그대로 넘기면 된다.

@Composable
private fun TopBar(title: String, subtitle: String, trailing: ImageVector? = null, back: Boolean = false, dropdown: Boolean = false) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 20.dp, top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        if (back) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "뒤로", tint = p.text, modifier = Modifier.size(28.dp).offset(x = (-6).dp))
            Spacer(Modifier.width(4.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PText(title, 22.sp, weight = FontWeight.SemiBold)
                if (dropdown) Icon(Icons.Outlined.KeyboardArrowDown, null, tint = p.text, modifier = Modifier.size(22.dp))
            }
            PText(subtitle, 13.sp, sub = true)
        }
        trailing?.let {
            Box(Modifier.size(44.dp).clip(CircleShape).background(p.surface).border(1.dp, p.line, CircleShape), contentAlignment = Alignment.Center) {
                Icon(it, null, tint = p.text, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun Screen(tab: PhoneTab, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { content() }
        BottomNav(tab)
    }
}

private fun memberColor(index: Int) = FamilyColors[index % FamilyColors.size]

private fun formatTemp(t: Double) = if (t % 1.0 == 0.0) "${t.toInt()}°" else "$t°"

/** 홈: 우리 차 + 다음 운전자 예약 + 가족·마지막 탑승. */
@Composable
fun PhoneHomeScreen(
    vehicle: VehicleDetailResponse,
    reservation: ReservationResponse?,
    lastRide: ApplyLogResponse?,
    carColor: Color,
    onReserve: () -> Unit = {},
) {
    val p = LocalPalette.current
    Screen(PhoneTab.HOME) {
        TopBar(vehicle.nickname ?: vehicle.model, "${vehicle.model} · 가족 ${vehicle.members.size}명 연결됨", Icons.Outlined.Notifications, dropdown = true)
        CarIllustration(carColor, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), reflection = p.isDark)

        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                PText("다음 운전자", 13.sp, sub = true)
                PText(reservation?.driverNickname ?: "예약 없음", 38.sp, weight = FontWeight.Light)
            }
            Column(horizontalAlignment = Alignment.End) {
                PText("적용할 프로필", 13.sp, sub = true)
                PText(reservation?.profileName ?: "—", 22.sp, weight = FontWeight.Light)
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 24.dp, start = 16.dp, end = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            CircleAction(Icons.Outlined.DateRange, "예약", onReserve, active = true)
            CircleAction(Icons.Outlined.Person, "내 프로필")
            CircleAction(Icons.Outlined.Edit, "테마")
            CircleAction(Icons.Outlined.Share, "공유")
        }

        PCard(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PText("다음 운전자 예약", 15.sp, Modifier.weight(1f), weight = FontWeight.SemiBold)
                StatusDot(if (reservation != null) "대기 중" else "없음", if (reservation != null) p.accent else p.subText)
            }
            Spacer(Modifier.height(10.dp))
            PText(
                reservation?.let { "${it.driverNickname} · ${it.profileName}" } ?: "탈 사람이 정해지면 예약해 두세요",
                18.sp,
                weight = FontWeight.Medium,
            )
            PText("타면 시트·공조와 화면 테마가 자동으로 바뀌어요", 13.sp, Modifier.padding(top = 4.dp), sub = true, maxLines = 2)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton(if (reservation != null) "예약 변경" else "예약하기", onClick = onReserve)
                if (reservation != null) PillButton("취소", filled = false)
            }
        }

        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PCard(Modifier.weight(1f)) {
                PText("가족", 13.sp, sub = true)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                    vehicle.members.forEachIndexed { i, m -> Avatar(m.nickname, memberColor(i)) }
                }
                PText(vehicle.members.joinToString(" · ") { it.nickname }, 13.sp, Modifier.padding(top = 10.dp), maxLines = 1)
            }
            PCard(Modifier.weight(1f)) {
                PText("마지막 탑승", 13.sp, sub = true)
                Spacer(Modifier.height(12.dp))
                PText(lastRide?.driverNickname ?: "아직 없음", 20.sp, weight = FontWeight.Medium)
                PText(lastRide?.let { "${it.profileName} · ${it.source.label}" } ?: "", 13.sp, Modifier.padding(top = 4.dp), sub = true)
            }
        }
    }
}

@Composable
private fun StatusDot(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        PText(text, 13.sp, Modifier.padding(start = 6.dp), color = color, weight = FontWeight.Medium)
    }
}

private val ApplySource.label get() = if (this == ApplySource.RESERVATION) "예약" else "1탭 선택"

/** 내 프로필: 큰 온도 숫자, 열선·통풍 단계, 차량별 시트 자세, 화면 테마. */
@Composable
fun ProfileScreen(profile: ProfileResponse, vehicleName: String, seatSlot: Int?, theme: ThemeSkinDto?, onReserve: () -> Unit = {}) {
    val p = LocalPalette.current
    Screen(PhoneTab.PROFILE) {
        TopBar(profile.name, "${vehicleName}에서 쓰는 내 설정", Icons.Outlined.Edit, back = true)

        Row(Modifier.fillMaxWidth().padding(top = 28.dp, start = 24.dp, end = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundStep("−")
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                PText(formatTemp(profile.climate.temperature), 72.sp, weight = FontWeight.Light)
                PText("실내 온도", 13.sp, sub = true)
            }
            RoundStep("+")
        }
        FanBar(profile.climate.fanSpeed, Modifier.padding(horizontal = 24.dp, vertical = 20.dp))

        Column(Modifier.padding(horizontal = 20.dp)) {
            SectionTitle("시트 열선")
            LevelChips(profile.seat.heatingLevel)
            SectionTitle("시트 통풍")
            LevelChips(profile.seat.ventilationLevel)
            SectionTitle("핸들 열선")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PChip("끔", !profile.seat.steeringWheelHeating)
                PChip("켬", profile.seat.steeringWheelHeating)
            }

            PCard(Modifier.fillMaxWidth().padding(top = 24.dp)) {
                PText("시트 자세 · $vehicleName", 13.sp, sub = true)
                PText(seatSlot?.let { "메모리 ${it}번으로 맞춰요" } ?: "저장된 자세 없음", 18.sp, Modifier.padding(top = 6.dp), weight = FontWeight.Medium)
                PText("자세는 차마다 따로 저장돼요. 차에서 '지금 자세 저장'으로 바꿔요", 13.sp, Modifier.padding(top = 4.dp), sub = true, maxLines = 2)
            }

            theme?.let {
                PCard(Modifier.fillMaxWidth().padding(top = 12.dp), padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PText("화면 테마", 13.sp, Modifier.weight(1f), sub = true)
                        PText(it.name, 13.sp, weight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(10.dp))
                    DisplayFrame { CarDisplayPreview(it, profile.name, Modifier.fillMaxWidth()) }
                }
            }

            PillButton("다음 운전자로 예약", Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 24.dp), onClick = onReserve)
        }
    }
}

@Composable
private fun RoundStep(symbol: String) {
    val p = LocalPalette.current
    Box(Modifier.size(52.dp).clip(CircleShape).background(p.surface).border(1.dp, p.line, CircleShape), contentAlignment = Alignment.Center) {
        PText(symbol, 24.sp, weight = FontWeight.Light)
    }
}

@Composable
private fun FanBar(level: Int, modifier: Modifier = Modifier, max: Int = 8) {
    val p = LocalPalette.current
    Column(modifier) {
        Row {
            PText("풍량", 13.sp, Modifier.weight(1f), sub = true)
            PText("$level / $max", 13.sp, weight = FontWeight.Medium)
        }
        Box(Modifier.fillMaxWidth().padding(top = 10.dp).height(6.dp).clip(CircleShape).background(p.line)) {
            Box(Modifier.fillMaxWidth(level.coerceIn(0, max) / max.toFloat()).height(6.dp).clip(CircleShape).background(p.accent))
        }
    }
}

@Composable
private fun LevelChips(level: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("끔", "1단", "2단", "3단").forEachIndexed { i, label -> PChip(label, i == level) }
    }
}

/** 차 화면 미리보기에 검은 베젤을 둘러 실제 디스플레이처럼 보이게 한다. */
@Composable
private fun DisplayFrame(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF0A0B0D)).padding(5.dp)) {
        Box(Modifier.clip(RoundedCornerShape(12.dp))) { content() }
    }
}

/** 테마 만들기: 컨피규레이터처럼 고르면 위의 차 화면 미리보기가 바로 바뀐다. */
@Composable
fun ThemeStudioScreen(
    theme: ThemeSkinDto,
    colorOptions: List<Color>,
    selectedColor: Int,
    onButtonStyle: (ButtonStyle) -> Unit = {},
    onWidgetStyle: (WidgetStyle) -> Unit = {},
) {
    val p = LocalPalette.current
    Screen(PhoneTab.THEME) {
        TopBar("테마 만들기", theme.name, Icons.Outlined.Share, back = true)
        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(20.dp))
            DisplayFrame { CarDisplayPreview(theme, "지우", Modifier.fillMaxWidth()) }
            PText("차 화면 미리보기 · 10.25인치 가로", 12.sp, Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally), sub = true)

            SectionTitle("색")
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                colorOptions.forEachIndexed { i, c ->
                    Box(
                        Modifier.size(44.dp).clip(CircleShape)
                            .border(2.dp, if (i == selectedColor) p.strong else Color.Transparent, CircleShape)
                            .padding(5.dp).clip(CircleShape).background(c),
                    )
                }
            }

            SectionTitle("버튼 모양")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ButtonStyle.entries.forEach { style ->
                    OptionCard(style.label, style == theme.buttonStyle, Modifier.weight(1f), { onButtonStyle(style) }) { ButtonSwatch(style) }
                }
            }

            SectionTitle("위젯 스타일")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WidgetStyle.entries.forEach { style ->
                    OptionCard(style.label, style == theme.widgetStyle, Modifier.weight(1f), { onWidgetStyle(style) }) { WidgetSwatch(style) }
                }
            }

            Row(Modifier.padding(top = 24.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton("저장", Modifier.weight(1f))
                PillButton("공유 코드 만들기", Modifier.weight(1f), filled = false)
            }
        }
    }
}

private val ButtonStyle.label get() = when (this) {
    ButtonStyle.ROUNDED -> "둥근"
    ButtonStyle.PILL -> "알약"
    ButtonStyle.SQUARE -> "각진"
    ButtonStyle.OUTLINED -> "테두리"
}

private val WidgetStyle.label get() = when (this) {
    WidgetStyle.FILLED -> "채움"
    WidgetStyle.OUTLINED -> "테두리"
    WidgetStyle.GLASS -> "글래스"
    WidgetStyle.METALLIC -> "메탈릭"
}

/** 휠 고르듯 고르는 선택 카드. 고른 것은 검은 테두리 + 체크. */
@Composable
private fun OptionCard(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit, swatch: @Composable () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(18.dp)
    Box(modifier) {
        Column(
            Modifier.fillMaxWidth().clip(shape).background(p.surface)
                .border(if (selected) 2.dp else 1.dp, if (selected) p.strong else p.line, shape)
                .clickable(onClick = onClick).padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.height(36.dp), contentAlignment = Alignment.Center) { swatch() }
            PText(label, 13.sp, Modifier.padding(top = 8.dp), weight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        }
        if (selected) {
            Box(
                Modifier.align(Alignment.TopCenter).offset(y = (-9).dp).size(20.dp).clip(CircleShape).background(p.strong),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.Check, null, tint = p.onStrong, modifier = Modifier.size(13.dp)) }
        }
    }
}

@Composable
private fun ButtonSwatch(style: ButtonStyle) {
    val p = LocalPalette.current
    val shape = when (style) {
        ButtonStyle.ROUNDED, ButtonStyle.OUTLINED -> RoundedCornerShape(8.dp)
        ButtonStyle.PILL -> CircleShape
        ButtonStyle.SQUARE -> RoundedCornerShape(2.dp)
    }
    Box(
        Modifier.size(52.dp, 24.dp).clip(shape)
            .then(if (style == ButtonStyle.OUTLINED) Modifier.border(2.dp, p.strong, shape) else Modifier.background(p.strong)),
    )
}

@Composable
private fun WidgetSwatch(style: WidgetStyle) {
    val shape = RoundedCornerShape(8.dp)
    val base = Color(0xFF2A3342)
    val m = Modifier.size(52.dp, 34.dp).clip(shape)
    Box(Modifier.clip(shape).background(Brush.linearGradient(listOf(Color(0xFF1B2431), Color(0xFF3E5070)))).padding(4.dp)) {
        when (style) {
            WidgetStyle.FILLED -> Box(m.size(44.dp, 26.dp).background(base))
            WidgetStyle.OUTLINED -> Box(m.size(44.dp, 26.dp).border(1.5.dp, Color.White.copy(alpha = 0.55f), shape))
            WidgetStyle.GLASS -> Box(
                m.size(44.dp, 26.dp).background(Color.White.copy(alpha = 0.14f))
                    .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.08f))), shape),
            )
            WidgetStyle.METALLIC -> Box(
                m.size(44.dp, 26.dp)
                    .background(Brush.linearGradient(listOf(Color(0xFFC9D0D9), Color(0xFF59616D), Color(0xFFA9B1BC), Color(0xFF3B424C))))
                    .border(1.dp, Color.White.copy(alpha = 0.5f), shape),
            )
        }
    }
}

/** 탑승 기록: 지금 대기 중인 예약과 최근 탑승(누가·어떤 프로필·예약/1탭). */
@Composable
fun HistoryScreen(vehicleName: String, reservation: ReservationResponse?, history: List<ApplyLogResponse>, memberOrder: List<String>, carColor: Color) {
    val p = LocalPalette.current
    Screen(PhoneTab.HISTORY) {
        TopBar("탑승 기록", vehicleName, Icons.Outlined.DateRange)
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            CarIllustration(carColor, Modifier.width(150.dp))
            Column(Modifier.padding(start = 12.dp)) {
                PText("지금 대기 중", 13.sp, sub = true)
                PText(reservation?.let { "${it.driverNickname} · ${it.profileName}" } ?: "예약 없음", 18.sp, weight = FontWeight.Medium)
            }
        }
        PCard(Modifier.fillMaxWidth().padding(horizontal = 20.dp), padding = 8.dp) {
            history.forEachIndexed { i, log ->
                val color = memberColor(memberOrder.indexOf(log.driverNickname).coerceAtLeast(0))
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(log.driverNickname, color, 40.dp)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        PText("${log.driverNickname} · ${log.profileName}", 15.sp, weight = FontWeight.Medium)
                        PText(log.appliedAt.toDisplayTime(), 13.sp, sub = true)
                    }
                    SourceTag(log.source)
                }
                if (i < history.lastIndex) Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(1.dp).background(p.line))
            }
        }
        PText("같은 프로필로 1분 안에 다시 적용하면 한 번만 남아요. 프로필을 지워도 기록은 남아요.", 12.sp, Modifier.padding(20.dp), sub = true, maxLines = 2)
    }
}

@Composable
private fun SourceTag(source: ApplySource) {
    val p = LocalPalette.current
    val reserved = source == ApplySource.RESERVATION
    Box(
        Modifier.clip(CircleShape)
            .background(if (reserved) p.accent.copy(alpha = 0.12f) else p.line)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) { PText(source.label, 12.sp, color = if (reserved) p.accent else p.subText, weight = FontWeight.Medium) }
}

/** "2026-11-03T08:12:00Z" → "11월 3일 08:12" (서버 시간 그대로, 시간대 변환은 화면 연결 때). */
private fun String.toDisplayTime(): String = runCatching {
    val (date, time) = split('T')
    val (_, month, day) = date.split('-')
    "${month.toInt()}월 ${day.toInt()}일 ${time.take(5)}"
}.getOrDefault(this)
