plugins {
    id("com.android.library")
}

// 폰 앱·차량 앱이 같이 쓰는 안드로이드 코드: 토큰 보관(Keystore 암호화 + DataStore).
android {
    namespace = "com.pleos.fe.core.android"
    compileSdk = 35

    defaultConfig {
        minSdk = 28
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":core-api"))
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
}
