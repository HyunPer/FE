# 쉼표 (가칭)

**함께 타는 사람의 몸과 마음을 챙기는 차량 앱.** 차가 앞길의 급커브·과속방지턱·가다 서다를 미리 읽어 동승자의 멀미를 대비하게 하고, 도착하면 오늘 이동을 한 줄로 남기며 잠깐 쉬어 가게 합니다.

현대차그룹 차량용 앱 개발 공모전(Pleos Playground SDK) 출품작 · 팀 **귤까먹는 헬창** (대표 정재윤)

> 앱 이름은 확정 전입니다. 정해지면 `gradle.properties`의 `pleos.appName`과 두 앱의 `res/values/strings.xml`을 함께 바꿉니다.

## 무엇을 하나

| 때 | 차량 앱 (Pleos Connect) | 폰 앱 |
| --- | --- | --- |
| 처음 | 연결 코드 6자리 표시 | 가입 → 코드 입력으로 가족 연결, 가족 탭에서 초대 코드 공유(차 주인은 새 코드 발급), 내 컨디션(멀미 민감도·쉬는 방식) 저장 |
| 탑승 | 운전석 착석 + P단을 감지해 트립 시작 | — |
| 주행 중 | 멀미 위험도와 이유(“2km 안 급커브 3곳”, “가다 서다가 잦아요”), 가족이 고른 쉬는 방식으로 권유, 위험이 높으면 다음 휴게소 거리 | — |
| 도착 | 이동 요약(이동·막힘 시간, 급커브, 최고 위험도), 정차 중 30초 숨 고르기 | “오늘 이동 어땠나요?” 기분 하나 + 한 줄 체크인, 기록·이번 주 요약(힘들었던 시간대·가장 막힌 요일) |

**쓰는 Pleos SDK**: 차량 상태 조회만 씁니다(차량 제어 없음).

| SDK | 쓰는 것 |
| --- | --- |
| Vehicle 2.3.2 | 좌석 점유·기어(탑승 감지), 브레이크 페달(가다 서다), 핸들 각도(구불구불함) |
| NaviHelper 3.0.5 | 회전 안내(급커브·U턴·회전교차로), 주의 구간(과속방지턱·내리막·좁은 길), 목적지 도착 |

멀미 위험도는 의학적 진단이 아니라 “미리 대비하자”는 신호입니다(0–100, 항목별 상한, 가족 중 가장 멀미를 잘 타는 사람 기준 배율).

## 저장소 구성

| 모듈 | 내용 |
| --- | --- |
| `vehicle-app` | 차량 앱. 연결·대기·주행 중 케어·도착 화면, 트립 모니터(`care/TripMonitor`) |
| `phone-app` | 폰 앱. 가입·차량 연결·홈(마지막 이동·체크인)·기록(이번 주 요약)·가족(초대 코드)·내 컨디션 |
| `core-api` | 서버 API(Retrofit)·모델, 멀미 위험도 계산(`care/MotionRisk`), 이번 주 요약(`care/WeeklyInsight`). 순수 Kotlin이라 PC에서 테스트 |
| `core-android` | 토큰 보관(Android Keystore AES-GCM + DataStore) |
| `core-ui` | 공용 화면 부품(차 그림) |

서버는 [BE 저장소](https://github.com/HyunPer/BE)에 있습니다.

## 실행 방법

1. **서버**: BE 저장소에서 실행합니다(JDK 21). `http://localhost:8090/swagger-ui.html`이 열리면 준비된 것입니다.
   ```bash
   ./gradlew bootRun
   ```
2. **차량 앱**: Pleos Connect 에뮬레이터를 띄우고 설치합니다.
   ```bash
   ./gradlew :vehicle-app:installDebug
   ```
3. **폰 앱**: 일반 폰 에뮬레이터(API 34)에 설치합니다.
   ```bash
   ./gradlew :phone-app:installDebug
   ```
4. 폰에서 가입 → 차량 화면의 연결 코드 입력 → 차량 앱 "지금 출발"(또는 Virtual Console에서 운전석 착석 + 기어 P) → "도착했어요" → 폰 홈에서 체크인.

자세한 에뮬레이터 세팅과 시연 시나리오는 [docs/EMULATOR_QA.md](docs/EMULATOR_QA.md)에 있습니다.

## 테스트 환경·필요한 설정

| 항목 | 값 |
| --- | --- |
| 차량 앱 실행 환경 | Pleos Connect Emulator(API 34) · 실차 Pleos Connect. minSdk 28, targetSdk 34 |
| 폰 앱 실행 환경 | Android 9(API 28) 이상 |
| 빌드 | Android Studio, JDK 17 이상, Gradle 9.8.2(Wrapper 포함), AGP 9.4.1(내장 Kotlin 2.3.21) |
| SDK 저장소 | `https://nexus-playground.pleos.ai/repository/maven-releases/` (settings.gradle.kts에 설정됨) |
| 서버 주소 | 기본 `http://10.0.2.2:8090/`(에뮬레이터에서 본 PC). 실기기는 `-Ppleos.apiBaseUrl=http://<PC IP>:8090/`, 릴리스는 `pleos.releaseApiBaseUrl`(HTTPS) |

- HTTP는 디버그 빌드에서만 허용합니다(Pleos 보안 가이드: TLS 1.2+). 릴리스 APK는 HTTPS 서버가 필요합니다.
- Vehicle SDK·NaviHelper가 없는 기기(일반 에뮬레이터)에서도 앱은 뜨고, 위험도는 시간 기반으로만 계산됩니다.
- Android Studio가 "Kotlin 구성"을 권해도 누르지 마세요. AGP 9 내장 Kotlin을 쓰고 있어 따로 구성할 것이 없습니다.

## 테스트

```bash
./gradlew :core-api:test
```

멀미 위험도 계산과 API 클라이언트를 PC에서 확인합니다. 서버를 띄우고 주소를 넘기면 쉼표 흐름 전체(가입 → 연결 → 컨디션 → 트립 → 체크인)를 실제 서버로 확인합니다.

```bash
PLEOS_E2E_BASE_URL=http://localhost:8090/ ./gradlew :core-api:test --tests '*EndToEnd*'
```

트립 모니터(신호 → 위험도 → 도착 요약)와 화면은 에뮬레이터 없이 PC에서 확인합니다. 화면은 PNG로 그려집니다(`vehicle-app/build/screens/`, `phone-app/build/screens/`).

```bash
./gradlew :vehicle-app:testDebugUnitTest :phone-app:testDebugUnitTest
```

## APK

제출용 릴리스 APK는 이 저장소의 **`apk/` 폴더**에 `귤까먹는헬창_<앱이름>_release.apk`로 올립니다. 만드는 법과 서명은 [docs/RELEASE.md](docs/RELEASE.md)에 있습니다.

```bash
./gradlew :vehicle-app:exportReleaseApk
```

## 문서

- [에뮬레이터 세팅·시연·실측](docs/EMULATOR_QA.md)
- [안드로이드 스튜디오에서 BE 연동하기](docs/BACKEND_GUIDE.md)
- [제출용 릴리스 APK](docs/RELEASE.md)

예전 "가족 공유차 프로필" 버전(테마·예약·차량 제어·TTS)은 `backup/family-profile` 브랜치에 있습니다.
