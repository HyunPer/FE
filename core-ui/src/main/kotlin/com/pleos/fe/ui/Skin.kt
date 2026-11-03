package com.pleos.fe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pleos.fe.api.model.ButtonStyle
import com.pleos.fe.api.model.CharacterPlacement
import com.pleos.fe.api.model.FontStyle
import com.pleos.fe.api.model.ThemeColorsDto
import com.pleos.fe.api.model.ThemeSkinDto
import com.pleos.fe.api.model.WidgetStyle

/**
 * 서버 테마(ThemeSkinDto)를 Compose 값으로 바꾼 것. 차량 화면의 모든 글자·버튼·위젯은 이 값으로 그린다.
 * 색 대비(본문 4.5:1, 버튼 3:1)는 서버가 테마 저장 때 검사하므로 여기서는 그대로 쓴다.
 */
@Immutable
data class Skin(
    val name: String,
    val background: Color,
    val surface: Color,
    val primary: Color,
    val onPrimary: Color,
    val text: Color,
    val backgroundImageUrl: String?,
    val characterImageUrl: String?,
    val characterPlacement: CharacterPlacement,
    val buttonStyle: ButtonStyle,
    val widgetStyle: WidgetStyle,
    val fontStyle: FontStyle,
) {
    val buttonShape: Shape
        get() = when (buttonStyle) {
            ButtonStyle.ROUNDED, ButtonStyle.OUTLINED -> RoundedCornerShape(16.dp)
            ButtonStyle.PILL -> RoundedCornerShape(percent = 50)
            ButtonStyle.SQUARE -> RoundedCornerShape(4.dp)
        }

    val widgetShape: Shape get() = RoundedCornerShape(24.dp)

    val isDark: Boolean get() = background.luminance() < 0.5f

    val weight: FontWeight
        get() = when (fontStyle) {
            FontStyle.DEFAULT -> FontWeight.Normal
            FontStyle.ROUNDED -> FontWeight.Medium
            FontStyle.BOLD -> FontWeight.Bold
        }

    // 차량에 둥근 글꼴이 따로 없어, ROUNDED는 산세리프 + 굵기로 구분한다. 글꼴 파일을 넣으면 여기만 바꾼다.
    val fontFamily: FontFamily get() = if (fontStyle == FontStyle.ROUNDED) FontFamily.SansSerif else FontFamily.Default
}

/** 이미지 주소(`/files/{id}`)는 [imageUrl]로 서버 전체 주소를 만든다. */
fun ThemeSkinDto.toSkin(imageUrl: (String) -> String): Skin {
    val fallback = ThemeColorsDto()
    return Skin(
        name = name,
        background = parseColor(colors.background, fallback.background),
        surface = parseColor(colors.surface, fallback.surface),
        primary = parseColor(colors.primary, fallback.primary),
        onPrimary = parseColor(colors.onPrimary, fallback.onPrimary),
        text = parseColor(colors.text, fallback.text),
        backgroundImageUrl = backgroundImageUrl?.let(imageUrl),
        characterImageUrl = character?.imageUrl?.let(imageUrl),
        characterPlacement = character?.placement ?: CharacterPlacement.BOTTOM_RIGHT,
        buttonStyle = buttonStyle,
        widgetStyle = widgetStyle,
        fontStyle = fontStyle,
    )
}

private fun parseColor(hex: String, fallback: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrElse { Color(android.graphics.Color.parseColor(fallback)) }

/** 프로필 적용 전(연결 코드·대기·적용 중) 화면의 기본 테마. */
val DefaultSkinDto = ThemeSkinDto(
    name = "기본",
    colors = ThemeColorsDto(),
    buttonStyle = ButtonStyle.ROUNDED,
    widgetStyle = WidgetStyle.FILLED,
    fontStyle = FontStyle.DEFAULT,
    ambientColor = "#FFFFFF",
)

val DefaultSkin = DefaultSkinDto.toSkin { it }

val LocalSkin = staticCompositionLocalOf { DefaultSkin }

// ── 테마로 그리는 기본 부품 ──

@Composable
fun SkinText(
    text: String,
    size: TextUnit,
    modifier: Modifier = Modifier,
    bold: Boolean = false,
    alpha: Float = 1f,
    color: Color? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    val skin = LocalSkin.current
    Text(
        text = text,
        modifier = modifier,
        color = (color ?: skin.text).copy(alpha = alpha),
        fontSize = size,
        lineHeight = size * 1.3f,
        fontWeight = if (bold) FontWeight.Bold else skin.weight,
        fontFamily = skin.fontFamily,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * 테마 버튼. 차량 터치 기준에 맞춰 높이 76dp 이상.
 * 주 버튼은 primary로 채우고(OUTLINED 스타일이면 테두리만), 보조 버튼은 언제나 테두리만.
 */
@Composable
fun SkinButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
) {
    val skin = LocalSkin.current
    val outlined = !primary || skin.buttonStyle == ButtonStyle.OUTLINED
    val shape = skin.buttonShape
    Box(
        modifier = modifier
            .heightIn(min = 76.dp)
            .clip(shape)
            .then(
                when {
                    outlined -> Modifier
                    // 글래스·메탈릭은 주 버튼에도 위→아래 광택을 준다
                    skin.widgetStyle == WidgetStyle.GLASS || skin.widgetStyle == WidgetStyle.METALLIC ->
                        Modifier.background(Brush.verticalGradient(listOf(lerp(skin.primary, Color.White, 0.22f), skin.primary)))
                    else -> Modifier.background(skin.primary)
                },
            )
            .border(2.dp, if (!outlined) Color.Transparent else if (primary) skin.primary else skin.text.copy(alpha = 0.5f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 36.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        SkinText(text, 24.sp, bold = true, color = if (outlined) skin.text else skin.onPrimary, maxLines = 1)
    }
}

/**
 * 위젯·카드 틀.
 * - FILLED: surface로 채움 / OUTLINED: 테두리만
 * - GLASS: 위가 밝은 반투명 막 + 위쪽에 빛이 닿는 테두리(배경 그라데이션·사진이 비쳐 보임)
 * - METALLIC: 대각선 금속 그라데이션 + 반사광 테두리
 */
@Composable
fun SkinWidget(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val skin = LocalSkin.current
    val shape = skin.widgetShape
    val styled = when (skin.widgetStyle) {
        // 밝은 테마에서는 부드러운 그림자로 카드를 띄운다(밝은 미니멀 레퍼런스)
        WidgetStyle.FILLED -> Modifier.background(skin.surface, shape)
        WidgetStyle.OUTLINED -> Modifier.border(2.dp, skin.text.copy(alpha = 0.4f), shape)
        WidgetStyle.GLASS -> Modifier
            .background(skin.surface.copy(alpha = 0.35f), shape)
            .background(Brush.verticalGradient(listOf(skin.text.copy(alpha = 0.10f), skin.text.copy(alpha = 0.02f))), shape)
            .border(1.dp, Brush.verticalGradient(listOf(skin.text.copy(alpha = 0.32f), skin.text.copy(alpha = 0.06f))), shape)
        WidgetStyle.METALLIC -> Modifier
            .background(
                Brush.linearGradient(
                    0f to lerp(skin.surface, Color.White, 0.16f),
                    0.45f to skin.surface,
                    0.7f to lerp(skin.surface, Color.White, 0.07f),
                    1f to lerp(skin.surface, Color.Black, 0.25f),
                ),
                shape,
            )
            .border(
                1.5.dp,
                Brush.linearGradient(
                    0f to Color.White.copy(alpha = 0.55f),
                    0.4f to Color.White.copy(alpha = 0.06f),
                    0.75f to Color.White.copy(alpha = 0.22f),
                    1f to Color.White.copy(alpha = 0.04f),
                ),
                shape,
            )
    }
    val lifted = skin.widgetStyle == WidgetStyle.FILLED && !skin.isDark
    Column(
        modifier = modifier
            .then(if (lifted) Modifier.shadow(10.dp, shape, ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.12f)) else Modifier)
            .clip(shape)
            .then(styled)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(28.dp),
        content = content,
    )
}

/** 테마 배경(색 → 배경 이미지 + 옅은 덮개) 위에 캐릭터를 지정 위치에 둔다. WELCOME_ONLY면 웰컴 화면에서만. */
@Composable
fun SkinBackground(
    showCharacter: Boolean,
    content: @Composable BoxScope.() -> Unit,
) {
    val skin = LocalSkin.current
    Box(Modifier.fillMaxSize().background(skin.background)) {
        // 사진이 없을 때도 단색 대신 은은한 깊이: 위는 조금 밝게, 오른쪽 위에 강조색 빛 번짐
        val top = if (skin.isDark) lerp(skin.background, skin.text, 0.07f) else lerp(skin.background, Color.White, 0.6f)
        val bottom = if (skin.isDark) lerp(skin.background, Color.Black, 0.2f) else lerp(skin.background, Color.Black, 0.04f)
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(top, skin.background, bottom))))
        if (skin.isDark) {
            Box(
                Modifier.matchParentSize().background(
                    Brush.radialGradient(
                        listOf(skin.primary.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(Float.POSITIVE_INFINITY, 0f),
                        radius = 1100f,
                    ),
                ),
            )
        }
        skin.backgroundImageUrl?.let { url ->
            RemoteImage(url, Modifier.matchParentSize(), ContentScale.Crop)
            // 사진 위에서도 테마 글자색 대비가 유지되게 배경색을 옅게 덮는다
            Box(Modifier.matchParentSize().background(skin.background.copy(alpha = 0.35f)))
        }
        val character = skin.characterImageUrl
        if (showCharacter && character != null && skin.characterPlacement != CharacterPlacement.WELCOME_ONLY) {
            RemoteImage(
                character,
                Modifier.align(skin.characterPlacement.alignment).padding(24.dp).size(200.dp),
                ContentScale.Fit,
            )
        }
        content()
    }
}

private val CharacterPlacement.alignment: Alignment
    get() = when (this) {
        CharacterPlacement.BOTTOM_RIGHT, CharacterPlacement.WELCOME_ONLY -> Alignment.BottomEnd
        CharacterPlacement.BOTTOM_LEFT -> Alignment.BottomStart
        CharacterPlacement.TOP_RIGHT -> Alignment.TopEnd
    }
