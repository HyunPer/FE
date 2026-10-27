plugins {
    id("com.android.application")
}

val apiBaseUrl = providers.gradleProperty("pleos.apiBaseUrl").get()

android {
    namespace = "com.pleos.fe.phone"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pleos.fe.phone"
        minSdk = 28
        targetSdk = 35
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
}
