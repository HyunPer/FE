plugins {
    id("com.android.application")
}

val apiBaseUrl = providers.gradleProperty("pleos.apiBaseUrl").get()

// Pleos Connect: Android Automotive(GMS 없음), 에뮬레이터 API 34, minSdk 28.
android {
    namespace = "com.pleos.fe.vehicle"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pleos.fe.vehicle"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core-android"))
    // Pleos SDK: implementation("ai.pleos.playground:Vehicle:2.3.2") — 저장소 주소 확인 후 settings.gradle.kts에 추가
}
