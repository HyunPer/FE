plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

// 폰 앱·차량 앱이 같이 쓰는 화면 부품(차 그림 등).
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
