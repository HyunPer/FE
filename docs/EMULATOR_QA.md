# 에뮬레이터 세팅·시연·실측

Pleos Connect 에뮬레이터에서 쉼표 차량 앱을 띄우고, 시연 흐름을 확인하고, **문서만으로는 알 수 없는 SDK 동작을 실제로 재서 기록**하기 위한 문서입니다.

## 1. 준비 (처음 한 번)

| 단계 | 내용 |
| --- | --- |
| Android Studio | SDK Platform 34·35, Platform-Tools 설치 |
| 시스템 이미지 | Playground 문서의 **Pleos Connect Emulator**(API 34) 이미지를 SDK Manager에 추가 |
| AVD | 위 이미지로 생성. **RAM 4GB**, Boot option **Cold boot**(Quick boot는 Vehicle Service가 꼬일 수 있음) |
| 폰 | 일반 Pixel 에뮬레이터(API 34) 하나 더 |
| BE | PC에서 `cd BE && ./gradlew bootRun` → `http://localhost:8090` (에뮬레이터에서는 `http://10.0.2.2:8090`) |

설치와 로그:

```bash
./gradlew :vehicle-app:installDebug :phone-app:installDebug
```

```bash
adb logcat -s TripMonitor BoardingTrigger VehicleContainer
```

Virtual Console(차량 상태 조작):

```bash
adb forward tcp:8088 tcp:8088
```

→ 브라우저에서 `http://127.0.0.1:8088` → Emulation Controls. 쉼표가 쓰는 카드(공식 [Virtual Console 사용 가이드](https://document.pleos.ai/docs/pleos-only/app-test-guide/virtual-console-guide), 11/6 확인):

| 카드 | 범위 | 쉼표에서 |
| --- | --- | --- |
| Motion › Gear Selection | P·R·N·D | 트립 시작(P), 주행 중 버튼 숨김(D) |
| Motion › Brake Pedal | 0–100% (작동 상태 파생) | 가다 서다(밟았다 떼기를 1분에 4번 넘게) |
| Motion › Steering Angle | −540–540° | 구불구불함(1분 동안 각도 변화 합) |
| Convenience › Seat Occupancy | 좌석별 Empty·Occupied | 탑승 감지(운전석) |

Virtual Console에는 내비 카드가 없습니다. 회전 안내·주의 구간·도착은 에뮬레이터 내비 앱으로 경로 안내를 걸어야 옵니다(M7–M9).

주행 규제(운전 중 화면 제한)를 켜고 끄는 명령(공식 ADB 가이드):

```bash
adb shell cmd hmg_car_service --service drivingstate driving_regulation on
```

```bash
adb shell cmd hmg_car_service --service drivingstate driving_regulation off
```

## 2. 시연 시나리오

| # | 순서 | 기대 결과 |
| --- | --- | --- |
| S1 | 차량 앱 첫 실행 | "이 차를 가족과 연결하세요" + 연결 코드(큰 글자) |
| S2 | 폰 앱: 가입 → 연결 코드 입력 | 5초 안에 차량 화면이 "쉼표가 함께 탈 준비가 됐어요"로 바뀜 |
| S3 | 폰 앱 "내 컨디션": 멀미 "잘 타요" 저장 | 다음 트립부터 위험도가 높게 계산됨(배율 1.3) |
| S4 | 차량 앱 "지금 출발" (또는 Virtual Console에서 운전석 점유 끔→켬, 기어 P) | 주행 중 화면: "편안한 길이에요" + 멀미 위험도 막대 |
| S5 | 내비로 경로 안내 중(가능하면 굽은 길) | 급커브·주의 구간이 2km 안에 들어오면 "멀미 주의"/"조금 흔들릴 수 있어요"와 이유, 위험이 높으면 다음 휴게소 거리 |
| S5‴ | Virtual Console 기어 **D** → Brake Pedal을 0→60→0%로 1분에 5번 이상, Steering Angle을 −300↔300°로 몇 번 | "가다 서다가 잦아요"·"구불구불한 길이에요"가 이유에 뜨고 위험도가 오름(브레이크가 1분에 4번 넘는 동안은 도착 요약의 막힘 시간에 쌓임) |
| S5′ | (내비 경로 안내를 못 걸 때) Virtual Console 기어를 **P**로 두고, 디버그 빌드 주행 화면 아래 **시연** 버튼: 급커브 앞 → 구불길 → 휴게소 12km | 위험도가 주의 → 멀미 주의로 오르고, S3에서 고른 쉬는 방식(예: 바깥 바람)으로 권유가 바뀜 |
| S5″ | Virtual Console 기어를 **D**로 | "도착했어요"·시연 버튼이 사라지고 "도착하면 자동으로 요약해요"만 보임(주행 중 조작 방지) |
| S6 | 차량 앱 기어 P에서 "도착했어요" (또는 내비 목적지 도착) | 도착 화면: 이동·막힘 시간, 급커브, 최고 위험도 → "30초 숨 고르기"(4초 들숨·6초 날숨 × 3) |
| S7 | 폰 앱 홈 | "오늘 이동 어땠나요?" 카드 → 기분 하나 + 한 줄 → 남기기 → 기록 탭에 반영 |
| S8 | BE를 끄고 "지금 출발" | "트립을 시작하지 못했습니다" + 다시 시도 |

## 3. 실측 표

| # | 항목 | 확인 방법 | 기록할 것 | 결과 |
| --- | --- | --- | --- | --- |
| M1 | Vehicle SDK 연결 | `adb logcat -s VehicleContainer` | "Vehicle SDK 연결 실패"가 없는지 | |
| M2 | NaviHelper 연결 | 같은 로그 | "NaviHelper 연결 실패"가 없는지(내비 앱 필요) | |
| M3 | 착석 감지 | VC 운전석 점유 끔→켬 + 기어 P | 트립이 한 번만 시작되는지 | |
| M4 | 기어 | VC 기어 D에서 착석 → P로 | D에서는 시작 안 하고 P에서 시작하는지 | |
| M5 | 브레이크 페달 | VC Brake Pedal 0→60→0% | `onBrakePedalStatusUpdated`가 true/false로 바뀌는지(몇 %부터 true인지) | 문서상 조작 가능 |
| M6 | 핸들 각도 | VC Steering Angle 변경 | 각도 값이 오는지, 슬라이더를 끌 때 몇 번 오는지 | 문서상 조작 가능 |
| M7 | 회전 안내(TBT) | 내비 경로 안내 중. 앱이 10초마다 `getTBTInfo()`로 묻는다(NAVI_ROUTE 권한) | `onTBTInfo`로 급커브·회전교차로 타입과 거리 값이 오는지 | |
| M8 | 주의 구간 | 경로 안내 중 `onCautionPointInfo` | 과속방지턱·내리막이 오는지 | |
| M9 | 목적지 도착 | 안내 종료 | `onDestinationArrived`로 도착 화면이 뜨는지 | |
| M10 | 화면 크기 | 폼 팩터 테스트(12.9″ 세로, 14.6″ 가로) | 글자 잘림·겹침. 세로면 위아래 배치로 바뀌는지(PC 캡처 `portrait_*.png`로 미리 확인함) | |
| M11 | 재실행 | 앱 강제 종료 후 다시 열기 | 다시 등록하지 않고 같은 연결 코드인지(토큰 보관) | |

## 4. 실측 후 할 일

- 권한은 SDK의 `@RequiresPermission`으로 확인했다(11/6): 기어·브레이크·핸들 각도 = `CAR_DRIVE`, 좌석 = `CAR_SEATS`, `getTBTInfo` = `NAVI_ROUTE`. 셋 다 매니페스트에 있다.
- M7(내비 경로 안내)이 에뮬레이터에서 안 되면 S5′의 시연 버튼으로 보여 준다. 브레이크·핸들은 S5‴처럼 Virtual Console로 직접 보여 줄 수 있다. 시연 버튼은 디버그 빌드에만 있고 제출 릴리스 APK에는 없다. PT 시연은 디버그 빌드로 할지 정한다.
- M7·M8 값의 거리 단위가 미터가 아니면 `TripMonitor.AHEAD_M`·`PASSED_M`을 맞춘다.
- 세로 배치는 `ui/Screens.kt`의 `Adaptive`(화면이 세로로 길면 위아래로)로 처리했다. 실제 폼 팩터에서 잘리면 여백·글자 크기를 조정한다.
