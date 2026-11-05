# 안드로이드 스튜디오에서 BE 연동하기

폰 앱·차량 앱이 백엔드(BE)와 통신하는 코드는 이미 들어 있습니다. 화면을 만드는 사람은 **ViewModel에서 `container.api.함수()`를 부르고, 결과를 상태로 바꿔 화면에 그리기**만 하면 됩니다.

## 1. 처음 열기

1. 안드로이드 스튜디오 → **Open** → 저장소의 `FE` 폴더를 엽니다. Gradle 동기화가 끝날 때까지 기다립니다.
   - "Kotlin 구성(Configure Kotlin)" 알림이 떠도 **누르지 마세요**. AGP 9 내장 Kotlin을 쓰고 있어 따로 구성할 것이 없고, 누르면 빌드 파일이 망가집니다.
2. BE를 켭니다. `http://localhost:8090/swagger-ui.html`이 열리면 준비된 것입니다. **8090을 다른 프로그램이 쓰고 있으면** BE가 뜨지 않습니다.
   ```bash
   cd BE && ./gradlew bootRun
   ```
3. 실행 설정에서 `phone-app`을 고르고 폰 에뮬레이터(Pixel, API 34)로 ▶ 실행합니다. 가입 → 연결 코드 입력 → 홈이 나옵니다.
4. 연결 코드는 차량 앱 화면에 뜹니다. 에뮬레이터 두 개(폰 + Pleos 차량)를 띄우면 실제 시연과 같은 흐름이 됩니다.

### 서버 주소

| 어디서 실행 | 주소 | 설정 |
| --- | --- | --- |
| 에뮬레이터 | `http://10.0.2.2:8090/` (에뮬레이터에서 본 PC) | 기본값, 바꿀 것 없음 |
| 실제 폰(USB/Wi-Fi) | `http://<PC의 LAN IP>:8090/` | `gradle.properties`의 `pleos.apiBaseUrl` 수정 + `src/debug/res/xml/network_security_config.xml`에 그 IP 추가 |
| 제출용 릴리스 | `https://...` | `pleos.releaseApiBaseUrl` (docs/RELEASE.md) |

## 2. 구조 한눈에

```
화면(Composable) ──collect──▶ ViewModel(StateFlow) ──apiCall { }──▶ container.api (Retrofit) ──HTTP──▶ BE
                                                                   └ 토큰은 자동으로 붙고 Keystore에 암호화 저장
```

| 무엇 | 어디 |
| --- | --- |
| API 목록 | `core-api/.../PhoneApi.kt`, `VehicleApi.kt` |
| 데이터 모양 | `core-api/.../model/Models.kt`(사용자·차량), `TripModels.kt`(트립·체크인·컨디션) |
| 오류 처리 | `core-api/.../ApiResult.kt` (`apiCall`, `onSuccess`, `getOrElse`) |
| 토큰 | `core-android/SecureTokenStore` (가입하면 저장, 요청마다 자동 첨부) |
| 폰 진입점 | `phone-app/PhoneApp.kt`의 `PhoneContainer` |
| 폰 예시 | `phone-app/.../shwimpyo/ShwimpyoViewModel.kt`, `ShwimpyoScreens.kt` |
| 차량 예시 | `vehicle-app/.../ui/VehicleController.kt`, `care/TripMonitor.kt` |

## 3. 규칙 세 가지

1. 서버 호출은 **항상 `apiCall { }`로 감쌉니다.** 예외가 화면까지 오지 않고 `ApiResult`로 옵니다.
2. 오류 문구는 `error.message`를 **그대로 보여줘도 됩니다.** 서버가 한국어로 이유를 줍니다.
3. 여러 호출을 이어서 할 때는 `getOrElse { return ... }`로 실패하면 바로 빠져나가고, 성공한 뒤 상태만 바꿀 때는 `onSuccess { }`를 씁니다.

```kotlin
fun checkIn(tripId: String, mood: Mood, note: String) = viewModelScope.launch {
    val saved = apiCall { container.api.checkIn(tripId, CheckInRequest(mood, note)) }
        .getOrElse { current.update { s -> s.copy(message = it.error.message) }; return@launch }
    current.update { s -> s.copy(trips = s.trips.map { if (it.id == saved.id) saved else it }) }
}
```

## 4. 화면별로 쓸 API

**폰 앱**

| 화면 | 부를 함수 | 메모 |
| --- | --- | --- |
| 시작·가입 | `session.signUp(닉네임)`, `session.isSignedIn`, `session.me()` | `me()`가 401이면 토큰을 지우니 가입 화면으로 |
| 차량 연결 | `api.pair(PairRequest(코드))` | 처음 연결한 사람이 OWNER. 틀린 코드는 404 |
| 홈·기록 | `api.vehicles()`, `api.trips(vehicleId)` | 트립은 최신순, `myMood`가 null이면 아직 체크인 안 함 |
| 도착 체크인 | `api.checkIn(tripId, CheckInRequest(mood, note))` | 다시 남기면 덮어씀. note 200자 |
| 내 컨디션 | `api.myCondition()`, `api.updateMyCondition(RiderConditionDto(...))` | 저장 전이면 기본값(MEDIUM·QUIET) |
| 가족 관리 | `vehicle(id)`, `renameVehicle`, `regeneratePairingCode`, `transferOwner`, `removeMember`, `leaveVehicle` | OWNER 전용은 MEMBER면 403 |

**차량 앱**

| 때 | 부를 함수 | 메모 |
| --- | --- | --- |
| 첫 실행 | `session.ensureRegistered(차종)` | 토큰 저장, 연결 코드 표시 |
| 탑승 | `api.riders()` → `api.startTrip()` | 가족 중 가장 높은 멀미 민감도로 `TripMonitor.sensitivity` |
| 도착 | `api.finishTrip(tripId, TripMonitor.stop())` | 재시도해도 같은 응답 |

## 5. 막힐 때

| 증상 | 원인·해결 |
| --- | --- |
| "네트워크에 연결할 수 없습니다" | BE가 꺼져 있거나 주소가 틀림. 에뮬레이터는 `10.0.2.2`, 실기기는 PC IP + 같은 Wi-Fi |
| `CLEARTEXT communication not permitted` | 그 주소가 `network_security_config.xml`에 없음. 디버그 빌드에만 추가 |
| 401 | 토큰이 무효(서버 DB를 지웠을 때 등). 앱 데이터를 지우고 다시 가입 |
| 404 | 멤버가 아닌 차량이거나 그 차의 트립이 아님. BE는 남의 것을 "없음"으로 돌려줌 |
| Gradle 동기화 오류, `kotlin("jvm")`이 빌드 파일에 생김 | "Kotlin 구성"을 누른 경우. 안드로이드 모듈에서 `kotlin("jvm")`·`jvmToolchain`을 지우고 `compileOptions`를 Java 17로 |

API를 직접 눌러 보고 싶으면 Swagger(`/swagger-ui.html`, phone·vehicle 그룹)를 쓰세요.
