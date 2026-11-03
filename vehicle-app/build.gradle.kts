import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val apiBaseUrl = providers.gradleProperty("pleos.apiBaseUrl").get()
// 릴리스(제출 APK)는 HTTPS 서버를 가리켜야 한다. 아직 배포 전이면 개발 주소로 빌드된다(그때는 디버그만 HTTP 허용이라 접속 불가).
val releaseApiBaseUrl = providers.gradleProperty("pleos.releaseApiBaseUrl").orElse(apiBaseUrl).get()

// 제출 파일 이름: 팀명_앱이름_release.apk (공모전 안내). 앱 이름이 정해지면 gradle.properties만 바꾼다.
// gradle.properties는 ISO-8859-1로 읽혀 한글이 깨지므로 UTF-8로 다시 해석한다(\uXXXX로 적었으면 그대로 둔다).
fun utf8Property(name: String): String {
    val raw = providers.gradleProperty(name).get()
    if (raw.any { it.code > 0xFF }) return raw
    return String(raw.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
}
val teamName = utf8Property("pleos.teamName")
val appName = utf8Property("pleos.appName")

// 서명 키는 저장소에 올리지 않는다(keystore.properties·*.jks는 .gitignore). 없으면 디버그 키로 서명해 설치만 되게 한다.
val keystoreProps = rootProject.file("keystore.properties").takeIf { it.exists() }
    ?.let { file -> Properties().apply { file.inputStream().use(::load) } }

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
    }

    signingConfigs {
        if (keystoreProps != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiBaseUrl\"")
            // Retrofit·kotlinx.serialization 리플렉션 규칙을 따로 관리하지 않도록 축소는 끈다(제출 APK 크기는 문제없음).
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // 화면 캡처 테스트(Robolectric): 에뮬레이터 없이 PC에서 Compose 화면을 PNG로 그린다.
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

// ./gradlew :vehicle-app:testDebugUnitTest --tests '*ScreenSnapshot*' → vehicle-app/build/screens/*.png
tasks.withType<Test>().configureEach {
    systemProperty("roborazzi.test.record", "true")
}

dependencies {
    implementation(project(":core-android"))
    implementation(project(":core-ui"))
    implementation("ai.pleos.playground:Vehicle:2.3.2")
    implementation("ai.pleos.playground:TextToSpeech:3.3.1")

    // compileSdk 35에서 쓸 수 있는 마지막 줄(Compose 1.8.x). 1.9+는 compileSdk 36 필요.
    implementation(platform("androidx.compose:compose-bom:2025.06.01"))
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.10.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-tooling-preview")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.77.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

/** ./gradlew :vehicle-app:exportReleaseApk → 저장소 루트 apk/ 에 제출 이름으로 복사. */
tasks.register<Copy>("exportReleaseApk") {
    group = "pleos"
    description = "릴리스 APK를 apk/${teamName}_${appName}_release.apk 로 내보낸다."
    dependsOn("assembleRelease")
    from(layout.buildDirectory.dir("outputs/apk/release")) {
        include("*.apk")
        rename { "${teamName}_${appName}_release.apk" }
    }
    into(rootProject.layout.projectDirectory.dir("apk"))
    doLast {
        if (keystoreProps == null) logger.warn("keystore.properties가 없어 디버그 키로 서명했습니다. 제출 전 릴리스 키를 만드세요(docs/RELEASE.md).")
        if (releaseApiBaseUrl.startsWith("http://")) logger.warn("릴리스 서버 주소가 HTTP입니다($releaseApiBaseUrl). 릴리스는 HTTPS만 접속됩니다.")
    }
}
