package com.pleos.fe.phone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 쉼표 폰 앱 디자인 토큰. 차량 앱과 같은 결로, 현대 남색·샌드 계열 그라데이션 바탕에 반투명 유리 카드를 올린다.
 * 폰의 밝은/어두운 모드를 따른다. 강조색은 현대 액티브 블루 하나만 쓴다.
 */
@Immutable
data class PhonePalette(
    /** 바탕 그라데이션의 위·아래 */
    val backgroundTop: Color,
    val background: Color,
    /** 바탕에 번지는 빛 */
    val glow: Color,
    /** 유리 카드 면의 위·아래, 위쪽 모서리에 맺히는 빛 */
    val surface: Color,
    val surfaceLow: Color,
    val edge: Color,
    /** 입력 칸·코드 칸처럼 카드 안에 파인 면 */
    val well: Color,
    val text: Color,
    val subText: Color,
    val line: Color,
    val accent: Color,
    val strong: Color,
    val onStrong: Color,
    /** 아래 탭 줄의 면과 그 위 글자 */
    val nav: Color,
    val onNav: Color,
    val isDark: Boolean,
)

val LightPalette = PhonePalette(
    backgroundTop = Color(0xFFF7F4F2),
    background = Color(0xFFE1E7EF),
    glow = Color(0xFF00AAD2),
    surface = Color(0xE0FFFFFF),
    surfaceLow = Color(0x99FFFFFF),
    edge = Color(0xFFFFFFFF),
    well = Color(0x0F0A1626),
    text = Color(0xFF0A1626),
    subText = Color(0xFF566170),
    line = Color(0x240A1626),
    accent = Color(0xFF006E99),
    strong = Color(0xFF002C5F),
    onStrong = Color(0xFFFFFFFF),
    nav = Color(0xFF0A1626),
    onNav = Color(0xFFFFFFFF),
    isDark = false,
)

val DarkPalette = PhonePalette(
    backgroundTop = Color(0xFF1B2635),
    background = Color(0xFF06090D),
    glow = Color(0xFF00AAD2),
    surface = Color(0x24FFFFFF),
    surfaceLow = Color(0x0DFFFFFF),
    edge = Color(0x52FFFFFF),
    well = Color(0x14FFFFFF),
    text = Color(0xFFF4F7FA),
    subText = Color(0xFFB7C0CC),
    line = Color(0x2EFFFFFF),
    accent = Color(0xFF63D5F3),
    strong = Color(0xFFF4F7FA),
    onStrong = Color(0xFF0A1626),
    nav = Color(0xFF1C2633),
    onNav = Color(0xFFF4F7FA),
    isDark = true,
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

/** 가족 구분 색. 멤버 순서대로 돌려 쓴다(아바타·탑승 기록 점). */
val FamilyColors = listOf(Color(0xFF00AAD2), Color(0xFF4F7CFF), Color(0xFF22A06B), Color(0xFFA25CFF), Color(0xFFE5A100))

val CardShape = RoundedCornerShape(28.dp)
val WellShape = RoundedCornerShape(18.dp)

/** 색을 정하고 바탕 그라데이션과 번지는 빛을 칠한다. [palette]를 주지 않으면 폰의 밝은/어두운 모드를 따른다. */
@Composable
fun PhoneSurface(palette: PhonePalette = if (isSystemInDarkTheme()) DarkPalette else LightPalette, content: @Composable BoxScope.() -> Unit) {
    val strength = if (palette.isDark) 0.22f else 0.16f
    CompositionLocalProvider(LocalPalette provides palette) {
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(Brush.verticalGradient(listOf(palette.backgroundTop, palette.background)))
                    drawRect(
                        Brush.radialGradient(
                            0f to palette.glow.copy(alpha = strength),
                            0.5f to palette.glow.copy(alpha = strength * 0.35f),
                            1f to Color.Transparent,
                            center = Offset(size.width * 0.78f, size.height * 0.22f),
                            radius = size.width * 0.95f,
                        ),
                    )
                },
            content = content,
        )
    }
}

/** 유리 면: 위가 조금 더 밝은 반투명 면 + 위쪽 모서리에 빛이 맺힌 가는 테두리. */
@Composable
fun Modifier.glass(shape: Shape = CardShape): Modifier {
    val p = LocalPalette.current
    // 어두운 화면에서는 아래로 갈수록 테두리가 사라지고, 밝은 화면에서는 아래쪽에 옅은 그늘 선이 남아 카드가 구분된다
    val lowEdge = if (p.isDark) p.edge.copy(alpha = p.edge.alpha * 0.18f) else p.text.copy(alpha = 0.10f)
    return clip(shape)
        .background(Brush.verticalGradient(listOf(p.surface, p.surfaceLow)))
        .border(1.dp, Brush.verticalGradient(listOf(p.edge, lowEdge)), shape)
}

@Composable
fun PText(
    text: String,
    size: TextUnit,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Normal,
    color: Color? = null,
    sub: Boolean = false,
    maxLines: Int = 1,
) {
    val p = LocalPalette.current
    Text(
        text,
        modifier,
        color = color ?: if (sub) p.subText else p.text,
        fontSize = size,
        lineHeight = size * 1.3f,
        fontWeight = weight,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** 유리 카드. */
@Composable
fun PCard(
    modifier: Modifier = Modifier,
    padding: Dp = 22.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .glass()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** 유리로 된 둥근 아이콘 버튼. */
@Composable
fun CircleAction(icon: ImageVector, label: String, onClick: () -> Unit = {}, size: Dp = 48.dp) {
    val p = LocalPalette.current
    Box(
        Modifier.size(size).glass(CircleShape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = p.text, modifier = Modifier.size(size * 0.46f))
    }
}

@Composable
fun PillButton(text: String, modifier: Modifier = Modifier, filled: Boolean = true, onClick: () -> Unit = {}) {
    val p = LocalPalette.current
    Box(
        modifier
            .height(56.dp)
            .clip(CircleShape)
            .then(if (filled) Modifier.background(p.strong) else Modifier.background(p.well).border(1.dp, p.line, CircleShape))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 26.dp),
        contentAlignment = Alignment.Center,
    ) {
        PText(text, 16.sp, weight = FontWeight.SemiBold, color = if (filled) p.onStrong else p.text)
    }
}

@Composable
fun PChip(text: String, selected: Boolean, onClick: () -> Unit = {}) {
    val p = LocalPalette.current
    Box(
        Modifier
            .height(42.dp)
            .clip(CircleShape)
            .background(if (selected) p.strong else p.well)
            .border(1.dp, if (selected) p.strong else p.line, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        PText(text, 14.sp, weight = FontWeight.Medium, color = if (selected) p.onStrong else p.text)
    }
}

/** 한 줄 입력 칸. 카드 안에 살짝 파인 면으로 그린다. 비어 있으면 [hint]를 옅게 보여준다. */
@Composable
fun PField(value: String, onValueChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    BasicTextField(
        value,
        onValueChange,
        modifier
            .height(56.dp)
            .clip(WellShape)
            .background(p.well)
            .border(1.dp, p.line, WellShape),
        singleLine = true,
        textStyle = TextStyle(color = p.text, fontSize = 16.sp),
        cursorBrush = SolidColor(p.accent),
        decorationBox = { inner ->
            Box(Modifier.fillMaxSize().padding(horizontal = 18.dp), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) PText(hint, 16.sp, sub = true)
                inner()
            }
        },
    )
}

@Composable
fun Avatar(name: String, color: Color, size: Dp = 36.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Text(name.take(1), color = Color.White, fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    PText(text, 15.sp, modifier.padding(top = 26.dp, bottom = 12.dp, start = 4.dp), weight = FontWeight.SemiBold)
}

/** 쉼표 폰 앱 탭: 홈(마지막 이동·체크인) / 기록(트립·기분) / 가족(초대·멤버) / 내 컨디션. */
enum class PhoneTab(val label: String, val icon: ImageVector) {
    HOME("홈", Icons.Outlined.Home),
    HISTORY("기록", Icons.Outlined.DateRange),
    FAMILY("가족", Icons.Outlined.Person),
    CONDITION("내 컨디션", Icons.Outlined.FavoriteBorder),
}

/** 화면 아래에 떠 있는 알약 모양 탭 줄. 고른 탭만 이름을 함께 보여준다. */
@Composable
fun BottomNav(selected: PhoneTab, onSelect: (PhoneTab) -> Unit = {}) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp)
            .height(68.dp)
            .clip(CircleShape)
            .background(p.nav)
            .then(if (p.isDark) Modifier.border(1.dp, p.line, CircleShape) else Modifier)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PhoneTab.entries.forEach { tab ->
            val on = tab == selected
            Row(
                Modifier
                    .height(52.dp)
                    .clip(CircleShape)
                    .then(if (on) Modifier.background(p.onNav) else Modifier)
                    .clickable(role = Role.Tab) { onSelect(tab) }
                    .padding(horizontal = if (on) 18.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(tab.icon, contentDescription = tab.label, tint = if (on) p.nav else p.onNav.copy(alpha = 0.62f), modifier = Modifier.size(24.dp))
                if (on) PText(tab.label, 14.sp, Modifier.padding(start = 8.dp), weight = FontWeight.SemiBold, color = p.nav)
            }
        }
    }
}
