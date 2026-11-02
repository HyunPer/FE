# 안드로이드 스튜디오에서 BE 연동하기

폰 앱·차량 앱이 백엔드(BE)와 통신하는 코드는 이미 들어 있습니다. 화면을 만드는 사람은 **ViewModel에서 `container.api.함수()`를 부르고, 결과를 상태로 바꿔 화면에 그리기**만 하면 됩니다.

## 1. 처음 열기

1. 안드로이드 스튜디오 → **Open** → 저장소의 `FE` 폴더(루트가 아니라 FE)를 엽니다. Gradle 동기화가 끝날 때까지 기다립니다.
2. BE를 켭니다. 터미널에서:

```bash
cd BE
./gradlew bootRun
```

   `http://localhost:8090/swagger-ui.html`이 열리면 준비된 것입니다. **8090을 다른 프로그램이 쓰고 있으면** BE가 뜨지 않으니 그 프로그램을 끄세요.
3. 상단 실행 설정에서 `phone-app`을 고르고, 폰 에뮬레이터(아무 Pixel, API 34)로 ▶ 실행합니다.
4. "서버 연동 확인" 화면이 뜹니다. 가입 → 연결 코드로 페어링 → 프로필 만들기 → 예약 → 탑승 기록 순서로 눌러 보세요. 아래 로그에 ✓/✗가 쌓입니다.

연결 코드는 차량 앱 화면이나 BE QA 콘솔(`http://localhost:8090/qa/`)의 "차량 등록"에서 얻습니다. 에뮬레이터를 두 개(폰 + Pleos 차량) 띄우면 실제 시연과 같은 흐름이 됩니다.

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

| 무엇 | 어디 | 화면 만드는 사람이 할 일 |
| --- | --- | --- |
| API 목록 | `core-api/.../PhoneApi.kt`, `VehicleApi.kt` | 함수 이름만 골라 부르기 |
| 데이터 모양 | `core-api/.../model/Models.kt` | 그대로 화면에 쓰기 |
| 오류 처리 | `core-api/.../ApiResult.kt` | `ApiResult.Success`/`Failure`로 분기 |
| 토큰 | `core-android/SecureTokenStore` | 신경 쓸 것 없음(가입하면 저장, 요청마다 자동 첨부) |
| 진입점 | `phone-app/PhoneApp.kt`의 `PhoneContainer` | ViewModel 만들 때 넘겨주기 |
| 예시 | `phone-app/.../check/ConnectionCheckViewModel.kt` | 이 모양을 복사해서 시작 |

## 3. 새 화면 만드는 순서 (예: 내 프로필 목록)

**ViewModel**

```kotlin
data class ProfilesState(
    val profiles: List<ProfileResponse> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

class ProfilesViewModel(private val container: PhoneContainer) : ViewModel() {
    private val current = MutableStateFlow(ProfilesState())
    val state: StateFlow<ProfilesState> = current.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        current.update { it.copy(loading = true, error = null) }
        when (val r = apiCall { container.api.profiles() }) {
            is ApiResult.Success -> current.update { it.copy(profiles = r.value, loading = false) }
            is ApiResult.Failure -> current.update { it.copy(error = r.error.message, loading = false) }
        }
    }
}
```

**화면**

```kotlin
@Composable
fun ProfilesScreen(vm: ProfilesViewModel = viewModel { ProfilesViewModel(LocalContext.current.phoneContainer) }) {
    val state by vm.state.collectAsStateWithLifecycle()
    when {
        state.loading -> CircularProgressIndicator()
        state.error != null -> Column { Text(state.error!!); Button(vm::load) { Text("다시 시도") } }
        else -> LazyColumn { items(state.profiles) { Text(it.name) } }
    }
}
```

규칙 세 가지:

1. 서버 호출은 **항상 `apiCall { }`로 감쌉니다.** 예외가 화면까지 오지 않고 `ApiResult`로 옵니다.
2. 오류 문구는 `error.message`를 **그대로 보여줘도 됩니다.** 서버가 한국어로 이유를 줍니다(예: "글자와 배경의 대비가 부족합니다").
3. 결과가 없을 수 있는 호출(현재 예약 등)은 `.bodyOrNull()`로 꺼내고 `null`이면 "없음" 화면을 그립니다. 본문 없는 성공(삭제 등)은 `.requireSuccess()`.

여러 호출을 이어서 할 때는 `getOrElse { return it }`로 실패 시 바로 빠져나가고, 성공 후 상태만 바꿀 때는 `onSuccess { }`를 씁니다. 예시는 `ConnectionCheckViewModel`에 있습니다.

## 4. 화면별로 쓸 API (폰 앱)

| 화면(와이어프레임) | 부를 함수 | 메모 |
| --- | --- | --- |
| 시작·가입 | `container.session.signUp(닉네임)`, `session.isSignedIn`, `session.me()` | `me()`가 401이면 토큰을 지우니 가입 화면으로 |
| 차량 연결 | `api.pair(PairRequest(코드, 별명))` | 처음 연결한 사람이 OWNER. 틀린 코드는 404 |
| 내 차량·가족 | `api.vehicles()`, `api.vehicle(id)` | 상세에 멤버 목록·역할 |
| 차량 관리 | `renameVehicle`, `regeneratePairingCode`, `transferOwner`, `removeMember`, `leaveVehicle` | OWNER 전용은 MEMBER면 403. 다른 멤버가 있으면 OWNER는 나가기 409 |
| 프로필 목록·편집 | `profiles()`, `profile(id)`, `createProfile`, `updateProfile`(전체 교체), `deleteProfile` | 온도·풍량·열선·통풍·위젯·테마·밝기 |
| 시트 자세 | `seatPositions(profileId)`, `saveSeatPosition(profileId, vehicleId, SeatPositionRequest(memorySlot = n))` | 차량마다 따로 |
| 다음 운전자 예약 | `reserve`, `currentReservation(...).bodyOrNull()`, `cancelReservation(...).requireSuccess()` | 새로 예약하면 기존 예약은 취소됨 |
| 탑승 기록 | `applyHistory(vehicleId, limit)` | 최신순, 프로필을 지워도 남음 |
| 테마 목록·편집 | `themes()`, `createTheme`, `updateTheme`, `deleteTheme` | 색 대비 부족하면 400 + 이유 |
| 이미지 올리기 | `uploadAsset(imagePart(파일, "image/png"))` → 받은 `id`를 테마의 `backgroundAssetId`/`characterAssetId`에 | PNG·JPEG·WEBP, 3MB 이하 |
| 테마 공유·마켓 | `shareTheme`, `unshareTheme`, `previewSharedTheme(코드)`, `importTheme`, `themeMarket()` | 받아온 테마는 다시 공유 불가 |
| 이미지 표시 | `container.imageUrl(skin.backgroundImageUrl)` | `/files/...` 경로를 전체 주소로 |

테마 미리보기를 차량 화면과 똑같이 그리려면 차량 앱의 `vehicle-app/.../ui/Skin.kt`(`ThemeSkinDto.toSkin()`, `SkinButton`, `SkinWidget`)를 공용 모듈로 옮겨 같이 쓰면 됩니다.

## 5. 막힐 때

| 증상 | 원인·해결 |
| --- | --- |
| "네트워크에 연결할 수 없습니다" | BE가 꺼져 있거나 주소가 틀림. 에뮬레이터는 `10.0.2.2`, 실기기는 PC IP + 같은 Wi-Fi |
| `CLEARTEXT communication not permitted` | 그 주소가 `network_security_config.xml`에 없음. 디버그 빌드에만 추가 |
| 401 | 토큰이 무효(서버 DB를 지웠을 때 등). 로그아웃 후 다시 가입 |
| 404 | 남의 자원이거나 멤버가 아닌 차량. BE는 남의 것을 "없음"으로 돌려줌 |
| 응답은 오는데 화면이 안 바뀜 | 상태를 `current.update { it.copy(...) }`로 바꿨는지, 화면이 `collectAsStateWithLifecycle()`로 구독하는지 |

API를 직접 눌러 보고 싶으면 Swagger(`/swagger-ui.html`, phone 그룹)나 BE QA 콘솔(`/qa/`)을 쓰세요.
