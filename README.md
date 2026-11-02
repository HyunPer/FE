# Pleos FE

현대차 Pleos 차량용 앱 공모전 클라이언트입니다. 폰 앱과 차량 앱(Pleos Connect)이 [백엔드](https://github.com/HyunPer/BE)를 통해 프로필·테마를 주고받습니다.

기준 문서: [Notion 공모전 페이지](https://app.notion.com/p/Pleos-4044c75bb8a781d4aa5ecb3bfcca8074) · 화면 설계: [와이어프레임](https://claude.ai/artifact/7dDEt1LvopWgWkoAefpA2d)

> 차량 앱은 **실제 화면(Compose) + Vehicle SDK + 인사말 음성(TTS)** 까지 들어갔습니다. 폰 앱은 디자인 전이라 **서버 연동 확인 화면**(가입·페어링·프로필·예약·탑승 기록)이 임시로 떠 있습니다.
>
> 문서: [안드로이드 스튜디오에서 BE 연동하기](docs/BACKEND_GUIDE.md) · [에뮬레이터 세팅·실측](docs/EMULATOR_QA.md) · [제출용 릴리스 APK](docs/RELEASE.md) · [Gleo 음성 연동 초안](docs/gleo/README.md)

## 모듈

| 모듈 | 종류 | 내용 |
| --- | --- | --- |
| `core-api` | 순수 Kotlin (JVM) | 서버 API(Retrofit), 데이터 모델, 토큰 첨부, 오류 처리, 가입·차량 등록 세션, 탑승 2단계 적용 흐름 |
| `core-android` | Android 라이브러리 | 토큰 보관: Android Keystore(AES-GCM) 암호화 + DataStore |
| `phone-app` | Android 앱 | `PhoneContainer`: API·세션 연결, 임시 서버 연동 확인 화면(`check/`, ViewModel 예시) |
| `vehicle-app` | Android Automotive 앱 | 화면(`ui/`, Compose), Pleos Vehicle·TextToSpeech SDK 연동(`sdk/`), Gleo 호출 처리(`voice/`), 디버그 전용 QA 로그 화면 |

`core-api`는 안드로이드에 의존하지 않아 PC에서 바로 테스트하고, 실제 서버를 상대로 E2E를 돌릴 수 있습니다.

## 스택

| 항목 | 버전 |
| --- | --- |
| Kotlin | 2.3.21 |
| Android Gradle Plugin | 9.4.1 (Kotlin 내장) |
| compileSdk / minSdk | 35 / 28 · 폰 targetSdk 35, 차량 targetSdk 34 (Pleos 에뮬레이터 API 34) |
| Retrofit · OkHttp | 3.0.0 · 5.3.2 (5.4+는 compileSdk 36 이상 필요) |
| kotlinx.serialization · coroutines | 1.11.0 · 1.11.0 |
| Pleos SDK | `ai.pleos.playground:Vehicle:2.3.2`, `TextToSpeech:3.3.1` (저장소 `nexus-playground.pleos.ai`, 하위 그룹 `ai.pleos.playground.security` 포함) |
| Compose | BOM 2025.06.01(1.8.x), activity-compose 1.10.1 — 1.9+는 compileSdk 36 필요 |
| Gradle | 9.8.2 (Wrapper 포함) |

## 빌드와 테스트

`local.properties`에 Android SDK 경로가 필요합니다(Android Studio가 열면 자동 생성).

```bash
./gradlew :core-api:test        # API 클라이언트·2단계 흐름 단위 테스트
./gradlew assembleDebug         # 폰·차량 앱 디버그 APK
./gradlew :vehicle-app:exportReleaseApk   # 제출용 apk/팀명_앱이름_release.apk (docs/RELEASE.md)
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
- 차량 관리: `renameVehicle`·`regeneratePairingCode`·`transferOwner`·`removeMember`는 OWNER만(MEMBER는 403), `leaveVehicle`은 누구나(다른 멤버가 있으면 OWNER는 409).
- 화면이 나오기 전에는 BE의 임시 QA 콘솔(`http://localhost:8090/qa/`)에서 같은 API 흐름을 눌러볼 수 있습니다.

### 차량 앱

화면은 `VehicleActivity` → `ui/VehicleRoot`이고, 상태는 `ui/VehicleController`(앱 단위)가 들고 있습니다.

| 화면 | 언제 | 테마 |
| --- | --- | --- |
| 연결 코드 | 멤버 0명. 5초마다 확인해 페어링되면 바로 대기 화면으로 | 기본 |
| 탑승 대기 | 멤버 있음. "지금 적용" / "운전자 고르기", 착석+P단이면 자동 시작 | 기본 |
| 적용 중 | 4단계 진행(운전자 확인 → 시트·공조 → 적용 보고 → 테마·조명·인사) | 기본 |
| 1탭 선택 | 예약 없음. 가족 × 프로필 카드 | 기본 |
| 웰컴 | 인사말 + 캐릭터, 음성으로 인사. 4초 뒤 또는 누르면 홈 | 운전자 테마 |
| 테마 홈 | 인사·시계, 프로필 위젯, 항목별 적용 결과, 다른 운전자 | 운전자 테마 |
| 실패 | 멈춘 단계에 맞는 안내 + 다시 시도 | 기본 |

모든 글자·버튼·위젯은 `ui/Skin.kt`가 `ThemeSkinDto`(색 5개, 버튼 ROUNDED/PILL/SQUARE/OUTLINED, 위젯 FILLED/OUTLINED/GLASS, 글꼴, 배경·캐릭터 이미지)를 Compose 값으로 바꿔 그립니다. `ui/Screens.kt` 아래쪽에 Android Studio 미리보기 5개가 있습니다(1280×640).

흐름을 코드로 직접 부를 때:

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

## Pleos Vehicle SDK 연동 (vehicle-app/sdk)

| 파일 | 하는 일 |
| --- | --- |
| `PleosVehicleControl` | 1단계 차량 설정을 운전석에 적용: 공조 온도·풍량(차량 범위로 자동 조정), 시트 열선·통풍, 핸들 열선, 시트 자세(메모리 슬롯 우선, 없으면 목표 위치까지 폐루프 이동). 항목별 결과를 `report`로 남기고, 하나도 적용하지 못했을 때만 실패로 본다 |
| `PleosWelcomeRenderer` | 2단계 웰컴: 화면 상태 갱신 → 실내 앰비언트 라이트 켜기·색·밝기. 임의 색이 거부되면 가장 가까운 지원 색(에뮬레이터 팔레트)으로 다시 시도 |
| `BoardingTrigger` | 운전석 착석 + P단을 감지하면 한 번만 2단계 적용 시작(시동 시 앱 자동 실행은 서드파티에 허용되지 않음). 앱이 앞에 있을 때만 켠다 |
| `WelcomeVoice` | 웰컴 인사말 음성. Pleos TextToSpeech ON_DEVICE(서버 인증 불필요) → 없으면 안드로이드 기본 TTS. 첫 음절이 잘리지 않게 200ms 여유 |
| `SdkCalls` | SDK 콜백을 코루틴으로 감쌈. set은 onComplete가 보장되지 않아, 시간 안에 실패가 없으면 "요청 보냄"으로 처리 |

- SDK 실제 API는 받은 AAR에서 확인했습니다: 진입점 `Vehicle(context).initialize()`, 도메인 `getHVAC()`·`getSeat()`·`getSteeringWheel()`·`getLight()`·`getGear()`.
- 권한은 매니페스트 선언만으로 에뮬레이터에서 호출됩니다(10/28 설명회). M마켓 배포 때는 승인된 권한만 남깁니다.
- 시트 목표 위치 이동 로직(`core-api/vehicle/SeatAxisMover`)과 조명 색 매핑(`AmbientColor`)은 PC에서 단위 테스트합니다. FORWARD가 값을 늘리는지 줄이는지 문서에 없어, 멀어지면 한 번 방향을 뒤집고 시간 초과에도 반드시 멈춥니다.

### 에뮬레이터에서 QA하기

세팅, 시연 시나리오 S1–S8, 실측 표 M1–M18은 [docs/EMULATOR_QA.md](docs/EMULATOR_QA.md)에 있습니다. 디버그 빌드에는 런처 아이콘이 두 개입니다: **Pleos 프로필**(실제 화면), **Pleos QA**(항목별 SDK 결과 로그).

## 다음 작업

- **에뮬레이터 실측**: docs/EMULATOR_QA.md 실측 표 채우기(시트 자세·앰비언트 임의 색·TTS 엔진).
- **앱 이름**: 정해지면 `gradle.properties`의 `pleos.appName`, `strings.xml`, Gleo `invokeWord`.
- **HTTPS 서버 배포**: 릴리스 APK는 HTTPS만 접속(`pleos.releaseApiBaseUrl`).
- **폰 앱 화면**: 와이어프레임 폰 쪽 화면. 테마 미리보기는 차량 앱의 `Skin`·화면 부품을 옮겨 쓰면 된다.
- **웰컴 사운드**: `welcomeSoundId`에 맞는 소리 파일(아직 없음).
