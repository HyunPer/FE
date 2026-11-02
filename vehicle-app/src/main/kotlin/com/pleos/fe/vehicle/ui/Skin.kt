package com.pleos.fe.vehicle.ui

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
import androidx.compose.ui.graphics.Color
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
            .background(if (outlined) Color.Transparent else skin.primary)
            .border(2.dp, if (!outlined) Color.Transparent else if (primary) skin.primary else skin.text.copy(alpha = 0.5f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 36.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        SkinText(text, 24.sp, bold = true, color = if (outlined) skin.text else skin.onPrimary, maxLines = 1)
    }
}

/** 위젯·카드 틀. FILLED는 surface로 채움, OUTLINED는 테두리만, GLASS는 반투명 surface + 옅은 테두리. */
@Composable
fun SkinWidget(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val skin = LocalSkin.current
    val shape = skin.widgetShape
    val styled = when (skin.widgetStyle) {
        WidgetStyle.FILLED -> Modifier.background(skin.surface, shape)
        WidgetStyle.OUTLINED -> Modifier.border(2.dp, skin.text.copy(alpha = 0.4f), shape)
        WidgetStyle.GLASS -> Modifier
            .background(skin.surface.copy(alpha = 0.55f), shape)
            .border(1.dp, skin.text.copy(alpha = 0.18f), shape)
    }
    Column(
        modifier = modifier
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
