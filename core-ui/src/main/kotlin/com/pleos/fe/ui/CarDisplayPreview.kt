package com.pleos.fe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pleos.fe.api.model.ThemeSkinDto

/**
 * 차량 화면(10.25인치 가로, 1280×480dp)을 그대로 그린 뒤 주어진 폭에 맞춰 축소한다.
 * 폰의 테마 편집·마켓에서 "차에서는 이렇게 보인다"를 보여줄 때 쓴다. 글자·버튼 비율이 차 화면과 같다.
 */
@Composable
fun CarDisplayPreview(
    skin: ThemeSkinDto,
    driverNickname: String,
    modifier: Modifier = Modifier,
    imageUrl: (String) -> String = { it },
    designWidth: Dp = 1280.dp,
    designHeight: Dp = 480.dp,
) {
    val resolved = remember(skin) { skin.toSkin(imageUrl) }
    Box(
        modifier.layout { measurable, constraints ->
            val w = designWidth.roundToPx()
            val h = designHeight.roundToPx()
            val placeable = measurable.measure(Constraints.fixed(w, h))
            val scale = constraints.maxWidth.toFloat() / w
            layout(constraints.maxWidth, (h * scale).toInt()) {
                placeable.placeWithLayer(0, 0) {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0f)
                }
            }
        },
    ) {
        CompositionLocalProvider(LocalSkin provides resolved) {
            SkinBackground(showCharacter = true) {
                Column(Modifier.fillMaxSize().padding(48.dp)) {
                    SkinText("${driverNickname}님, 오늘도 안전 운전하세요", 40.sp, bold = true, maxLines = 1)
                    SkinText("${skin.name} 테마", 22.sp, alpha = 0.7f, maxLines = 1)
                    Spacer(Modifier.padding(top = 28.dp))
                    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        listOf("배터리" to "78% · 245km", "공조" to "실내 22℃", "음악" to "재생 대기").forEach { (title, body) ->
                            SkinWidget(Modifier.weight(1f).fillMaxHeight()) {
                                SkinText(title, 22.sp, alpha = 0.75f, maxLines = 1)
                                Spacer(Modifier.weight(1f))
                                SkinText(body, 30.sp, bold = true, maxLines = 1)
                            }
                        }
                    }
                    Spacer(Modifier.padding(top = 24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SkinButton("다른 운전자", {})
                        SkinButton("처음으로", {}, primary = false)
                    }
                }
            }
        }
    }
}
