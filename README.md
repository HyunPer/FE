# Pleos FE

현대차 Pleos 차량용 앱 공모전 클라이언트입니다. 폰 앱과 차량 앱(Pleos Connect)이 [백엔드](https://github.com/HyunPer/BE)를 통해 프로필·테마를 주고받습니다.

기준 문서: [Notion 공모전 페이지](https://app.notion.com/p/Pleos-4044c75bb8a781d4aa5ecb3bfcca8074) · 화면 설계: [와이어프레임](https://claude.ai/artifact/7dDEt1LvopWgWkoAefpA2d)

> 지금은 **서버 연결 준비 단계**입니다. 화면(Activity·Compose)은 아직 없습니다.

## 모듈

| 모듈 | 종류 | 내용 |
| --- | --- | --- |
| `core-api` | 순수 Kotlin (JVM) | 서버 API(Retrofit), 데이터 모델, 토큰 첨부, 오류 처리, 가입·차량 등록 세션, 탑승 2단계 적용 흐름 |
| `core-android` | Android 라이브러리 | 토큰 보관: Android Keystore(AES-GCM) 암호화 + DataStore |
| `phone-app` | Android 앱 | `PhoneContainer`: API·세션 연결 |
| `vehicle-app` | Android Automotive 앱 | `VehicleContainer`: API·차량 세션·2단계 적용 흐름 연결 |

`core-api`는 안드로이드에 의존하지 않아 PC에서 바로 테스트하고, 실제 서버를 상대로 E2E를 돌릴 수 있습니다.

## 스택

| 항목 | 버전 |
| --- | --- |
| Kotlin | 2.3.21 |
| Android Gradle Plugin | 9.4.1 (Kotlin 내장) |
| compileSdk / minSdk | 35 / 28 · 폰 targetSdk 35, 차량 targetSdk 34 (Pleos 에뮬레이터 API 34) |
| Retrofit · OkHttp | 3.0.0 · 5.3.2 (5.4+는 compileSdk 36 이상 필요) |
| kotlinx.serialization · coroutines | 1.11.0 · 1.11.0 |
| Gradle | 9.8.2 (Wrapper 포함) |

## 빌드와 테스트

`local.properties`에 Android SDK 경로가 필요합니다(Android Studio가 열면 자동 생성).

```bash
./gradlew :core-api:test        # API 클라이언트·2단계 흐름 단위 테스트
./gradlew assembleDebug         # 폰·차량 앱 디버그 APK
```

실제 서버를 상대로 데모 시나리오 전체(가입 → 차량 등록 → 페어링 → 테마 → 프로필 → 시트 슬롯 → 예약 → 2단계 적용)를 확인하려면 BE를 띄우고:

```bash
PLEOS_E2E_BASE_URL=http://localhost:8090/ ./gradlew :core-api:test --tests '*EndToEnd*'
```

## 서버 주소

- 기본값은 `gradle.properties`의 `pleos.apiBaseUrl=http://10.0.2.2:8090/` (에뮬레이터에서 본 호스트 PC).
- 실기기: `./gradlew assembleDebug -Ppleos.apiBaseUrl=http://<PC LAN IP>:8090/` 그리고 디버그 `network_security_config.xml`에 그 IP를 추가.
- HTTP는 **디버그 빌드에서만** 허용합니다(`src/debug`). 릴리스는 HTTPS만(Pleos 보안 가이드: TLS 1.2+).

## 쓰는 법

### 폰 앱

```kotlin
val c = context.phoneContainer
if (!c.session.isSignedIn) c.session.signUp("대학생 자녀")          // 토큰 자동 저장

when (val r = apiCall { c.api.profiles() }) {
    is ApiResult.Success -> show(r.value)
    is ApiResult.Failure -> showError(r.error.message)                // 서버의 한국어 detail
}
```

- 모든 호출은 `apiCall { }`로 감싸 `ApiResult`로 받습니다. `ApiError.Http`(status·detail) / `ApiError.Network`.
- 204가 올 수 있는 호출(현재 예약 등)은 `Response<T>`라서 `bodyOrNull()`로 꺼냅니다.
- 이미지 주소(`/files/{id}`)는 `c.imageUrl(path)`로 전체 주소를 만들어 이미지 로더에 넘깁니다.
- 401을 받으면 `UserSession.me()`가 토큰을 지웁니다 → 가입 화면으로.

### 차량 앱

```kotlin
val c = context.vehicleContainer
val self = c.session.ensureRegistered("IONIQ 5").getOrThrow()        // 첫 실행: 등록, 이후 재사용
showPairingCode(self.pairingCode)                                    // 정차 중

// 탑승 트리거(앱 열림·착석/도어 이벤트·음성 버튼)에서
when (val o = c.applyFlow.onBoarding()) {
    is BoardingOutcome.Welcomed -> Unit                              // c.welcome.state 구독 화면이 스킨·인사말 표시
    is BoardingOutcome.ChooseDriver -> showPicker(o.drivers)         // 고르면 c.applyFlow.applySelected(profile)
    is BoardingOutcome.Failed -> showRetry(o.step)
}
```

`VehicleApplyFlow`가 순서를 지킵니다: **차량 설정 적용(VehicleControl) → 서버에 적용 완료 보고 → 웰컴 묶음 렌더(WelcomeRenderer)**. 차량 설정이 실패하면 보고하지 않고 멈춥니다.

## 다음 작업

- **Pleos Vehicle SDK 연결**: `LogOnlyVehicleControl`을 실제 구현으로 교체(`VehicleControl` 주석에 SDK 메서드 매핑). SDK 저장소 주소를 `settings.gradle.kts`에, `pleos.car.permission.CONTROL_CAR_*` 권한을 매니페스트에 추가.
- **WelcomeRenderer 확장**: 스킨 교체 후 앰비언트 라이트(`setAmbientLightBrightnessColor`) → 사운드·TTS 인사.
- **화면**: 와이어프레임 8화면을 동료 디자인에 맞춰 구현. 모든 버튼·위젯은 `ThemeSkinDto`(색 토큰·프리셋)로 그린다.
