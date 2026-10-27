plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

// 안드로이드에 의존하지 않는 순수 Kotlin 모듈. 폰 앱·차량 앱이 같이 쓰고, PC에서 바로 테스트한다.
kotlin {
    jvmToolchain(17)
}

dependencies {
    api("com.squareup.retrofit2:retrofit:3.0.0")
    // 5.4+는 안드로이드 compileSdk 36 이상을 요구한다. 설치된 SDK(35)에 맞춰 5.3.2로 둔다.
    api("com.squareup.okhttp3:okhttp:5.3.2")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:3.0.0")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    testImplementation(kotlin("test"))
    testImplementation("com.squareup.okhttp3:mockwebserver3:5.3.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}

tasks.test {
    useJUnitPlatform()
    // 실제 서버 대상 E2E: PLEOS_E2E_BASE_URL=http://localhost:8090/ ./gradlew :core-api:test
    environment("PLEOS_E2E_BASE_URL", System.getenv("PLEOS_E2E_BASE_URL") ?: "")
}
