plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

// 폰 앱·차량 앱이 같이 쓰는 화면 부품: 서버 테마(ThemeSkinDto)로 그리는 Skin, 서버 이미지, 차 그림.
// 폰의 테마 미리보기가 차 화면과 똑같이 보이도록 한 곳에서 그린다.
android {
    namespace = "com.pleos.fe.ui"
    compileSdk = 35

    defaultConfig {
        minSdk = 28
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":core-api"))
    api(platform("androidx.compose:compose-bom:2025.06.01"))
    api("androidx.compose.foundation:foundation")
    api("androidx.compose.material3:material3")
    api("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
}
