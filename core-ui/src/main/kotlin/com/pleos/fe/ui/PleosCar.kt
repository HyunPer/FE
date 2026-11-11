package com.pleos.fe.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/**
 * Pleos Connect 기본 화면과 같은 차량 렌더 이미지.
 * 출처: Pleos Playground 디자인 가이드가 배포하는 "Pleos Connect display guide 2.1.5" Figma 파일의 차량 이미지
 * (https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro → Design Resources Download).
 * 밝은 화면용과 어두운 화면용이 따로 있다.
 */
@Composable
fun PleosCar(modifier: Modifier = Modifier, dark: Boolean = false) {
    Image(
        painter = painterResource(if (dark) R.drawable.pleos_car_dark else R.drawable.pleos_car_light),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier.aspectRatio(PLEOS_CAR_ASPECT),
    )
}

/** 이미지 가로÷세로(1400×688). */
const val PLEOS_CAR_ASPECT = 1400f / 688f
