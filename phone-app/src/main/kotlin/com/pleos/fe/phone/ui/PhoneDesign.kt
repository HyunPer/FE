package com.pleos.fe.phone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 쉼표 폰 앱 디자인 토큰. 기본은 밝은 미니멀(연회색 배경 · 흰 카드 · 검은 알약 버튼 · 강조색 하나),
 * 어두운 버전은 차 사진이 돋보이는 노을빛 그라데이션.
 */
@Immutable
data class PhonePalette(
    val background: Color,
    val glow: Color,
    val surface: Color,
    val text: Color,
    val subText: Color,
    val line: Color,
    val accent: Color,
    val strong: Color,
    val onStrong: Color,
    val isDark: Boolean,
)

val LightPalette = PhonePalette(
    background = Color(0xFFEFF1F4),
    glow = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    text = Color(0xFF111317),
    subText = Color(0xFF6B7280),
    line = Color(0xFFE3E6EB),
    accent = Color(0xFFFF6A2B),
    strong = Color(0xFF111317),
    onStrong = Color(0xFFFFFFFF),
    isDark = false,
)

val DarkPalette = PhonePalette(
    background = Color(0xFF0C1016),
    glow = Color(0xFF3B4B68),
    surface = Color(0xFF1A2029),
    text = Color(0xFFF2F5F9),
    subText = Color(0xFF8D97A6),
    line = Color(0xFF29303B),
    accent = Color(0xFF5AA2FF),
    strong = Color(0xFFF2F5F9),
    onStrong = Color(0xFF0C1016),
    isDark = true,
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

/** 가족 구분 색. 멤버 순서대로 돌려 쓴다(아바타·탑승 기록 점). */
val FamilyColors = listOf(Color(0xFFFF6A2B), Color(0xFF4F7CFF), Color(0xFF22A06B), Color(0xFFA25CFF), Color(0xFFE5A100))

@Composable
fun PhoneSurface(palette: PhonePalette = LightPalette, content: @Composable BoxScope.() -> Unit) {
    CompositionLocalProvider(LocalPalette provides palette) {
        Box(
            Modifier
                .fillMaxSize()
                .background(palette.background),
            content = content,
        )
    }
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

/** 밝은 테마는 흰 카드 + 부드러운 그림자, 어두운 테마는 짙은 면 + 옅은 테두리. */
@Composable
fun PCard(
    modifier: Modifier = Modifier,
    padding: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .then(if (p.isDark) Modifier else Modifier.shadow(14.dp, shape, ambientColor = Color.Black.copy(alpha = 0.06f), spotColor = Color.Black.copy(alpha = 0.10f)))
            .clip(shape)
            .background(p.surface)
            .then(if (p.isDark) Modifier.border(1.dp, p.line, shape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** 레퍼런스의 원형 빠른 버튼. */
@Composable
fun CircleAction(icon: ImageVector, label: String, onClick: () -> Unit = {}, active: Boolean = false) {
    val p = LocalPalette.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(64.dp)
                .then(if (p.isDark) Modifier else Modifier.shadow(10.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.06f), spotColor = Color.Black.copy(alpha = 0.10f)))
                .clip(CircleShape)
                .background(if (active) p.strong else p.surface)
                .then(if (p.isDark) Modifier.border(1.dp, p.line, CircleShape) else Modifier)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = if (active) p.onStrong else p.text, modifier = Modifier.size(26.dp))
        }
        PText(label, 13.sp, Modifier.padding(top = 8.dp), sub = true)
    }
}

@Composable
fun PillButton(text: String, modifier: Modifier = Modifier, filled: Boolean = true, onClick: () -> Unit = {}) {
    val p = LocalPalette.current
    Box(
        modifier
            .height(52.dp)
            .clip(CircleShape)
            .background(if (filled) p.strong else Color.Transparent)
            .then(if (filled) Modifier else Modifier.border(1.dp, p.line, CircleShape))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        PText(text, 15.sp, weight = FontWeight.SemiBold, color = if (filled) p.onStrong else p.text)
    }
}

@Composable
fun PChip(text: String, selected: Boolean, onClick: () -> Unit = {}) {
    val p = LocalPalette.current
    Box(
        Modifier
            .height(38.dp)
            .clip(CircleShape)
            .background(if (selected) p.strong else p.surface)
            .border(1.dp, if (selected) p.strong else p.line, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        PText(text, 14.sp, weight = FontWeight.Medium, color = if (selected) p.onStrong else p.text)
    }
}

@Composable
fun Avatar(name: String, color: Color, size: Dp = 36.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Text(name.take(1), color = Color.White, fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    PText(text, 15.sp, modifier.padding(top = 24.dp, bottom = 12.dp), weight = FontWeight.SemiBold)
}

/** 쉼표 폰 앱 탭: 홈(마지막 이동·체크인) / 기록(트립·기분) / 가족(초대·멤버) / 내 컨디션. */
enum class PhoneTab(val label: String, val icon: ImageVector) {
    HOME("홈", Icons.Outlined.Home),
    HISTORY("기록", Icons.Outlined.DateRange),
    FAMILY("가족", Icons.Outlined.Person),
    CONDITION("내 컨디션", Icons.Outlined.FavoriteBorder),
}

@Composable
fun BottomNav(selected: PhoneTab, onSelect: (PhoneTab) -> Unit = {}) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .background(p.surface)
            .border(1.dp, p.line)
            .padding(top = 10.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        PhoneTab.entries.forEach { tab ->
            val color = if (tab == selected) p.accent else p.subText
            Column(
                Modifier.clickable(role = Role.Tab) { onSelect(tab) }.padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(tab.icon, contentDescription = tab.label, tint = color, modifier = Modifier.size(24.dp))
                PText(tab.label, 11.sp, Modifier.padding(top = 4.dp), color = color)
            }
        }
    }
}
