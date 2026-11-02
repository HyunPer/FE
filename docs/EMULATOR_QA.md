# 에뮬레이터 세팅과 실측 체크리스트

Pleos Connect 에뮬레이터에서 차량 앱을 띄우고, **문서만으로는 알 수 없는 SDK 동작을 실제로 재서 기록**하기 위한 문서입니다. 결과는 맨 아래 실측 표에 채우고, 운영사무국 답변과 다르면 Notion 기획서 "확인 필요" 항목도 고칩니다.

## 1. 준비 (처음 한 번)

| 단계 | 내용 |
| --- | --- |
| Android Studio | 최신 안정판. SDK Platform 34·35, Platform-Tools 설치 |
| 시스템 이미지 | Playground 문서의 **Pleos Connect Emulator**(API 34, v2.1.0) 이미지를 SDK Manager에 추가 |
| AVD | 위 이미지로 생성. **RAM 4GB**, Boot option **Cold boot**(Quick boot는 Vehicle Service가 꼬일 수 있음) |
| 오디오 | TTS 확인용. AVD 설정에서 호스트 오디오 출력 켜기(마이크는 STT 쓸 때만) |
| BE | PC에서 `cd BE && ./gradlew bootRun` → `http://localhost:8090` (에뮬레이터에서는 `http://10.0.2.2:8090`) |

설치와 로그:

```bash
./gradlew :vehicle-app:installDebug
```

```bash
adb logcat -s VehicleControl BoardingTrigger WelcomeVoice GleoActions RemoteImage VehicleContainer
```

디버그 빌드는 런처 아이콘이 두 개입니다.

- **Pleos 프로필**: 실제 화면(Compose). 시연은 이것으로 합니다.
- **Pleos QA**: 항목별 SDK 결과를 로그로 보여 주는 화면. 실측은 이것으로 합니다.

Virtual Console(차량 상태 조작):

```bash
adb forward tcp:8088 tcp:8088
```

→ 브라우저에서 `http://127.0.0.1:8088`. 공조(풍량 0–8, 16–30℃ 0.5 단위), 시트 열선·통풍 3단계, 핸들 열선 Off/Low/Med/High, 앰비언트(밝기 0–100%, Cyan/Blue/Purple/White), 기어 P/R/N/D, 도어, 좌석 점유 카드가 있습니다. **시트 위치·시동 카드는 없습니다.**

## 2. 시연 시나리오 (Pleos 프로필 앱)

BE QA 콘솔 `http://localhost:8090/qa/`를 폰 앱 대신 씁니다.

| # | 순서 | 기대 결과 |
| --- | --- | --- |
| S1 | 앱 첫 실행 | "이 차를 가족과 연결하세요" + 연결 코드(큰 글자) |
| S2 | QA 콘솔에서 가입 → 차량 코드로 **페어링** | 5초 안에 화면이 "탑승하면 내 프로필이 자동으로 적용돼요"로 바뀜 |
| S3 | QA 콘솔에서 테마·프로필 만들고 **다음 운전자 예약** → 차량 앱 "지금 적용" | 적용 중 4단계 진행 → 웰컴(인사말 + 음성) → 4초 뒤 테마 홈. Virtual Console 공조·열선·조명 값이 바뀜 |
| S4 | 다시 예약 → Virtual Console에서 운전석 점유 끔→켬, 기어 P | 버튼 없이 S3과 같은 흐름 시작(착석+P단 트리거) |
| S5 | 예약 없이 "지금 적용" 또는 "운전자 고르기" | "누가 운전하나요?" 카드 → 하나 누르면 S3과 같은 흐름 |
| S6 | BE를 끄고 "지금 적용" | "서버에서 운전자 정보를 받지 못했습니다" + 다시 시도 |
| S7 | 주행 규제 켜고 S3 (아래 명령) | 차량 설정 실패 → "주행 중에는 시트·공조를 바꿀 수 없습니다" 화면 |
| S8 | 테마에 배경·캐릭터 이미지를 넣고 S3 | 홈 배경 사진 + 캐릭터가 지정 위치에, WELCOME_ONLY면 웰컴에만 |

주행 규제 켜기·끄기:

```bash
adb shell cmd hmg_car_service --service drivingstate driving_regulation on
```

```bash
adb shell cmd hmg_car_service --service drivingstate driving_regulation off
```

Gleo 음성 호출 흉내(스키마 승인 전에도 앱 쪽 처리는 확인 가능):

```bash
adb shell am start -n com.pleos.fe.vehicle/.VehicleActivity -a APPLY_NEXT_DRIVER
```

```bash
adb shell am start -n com.pleos.fe.vehicle/.VehicleActivity -a SHOW_DRIVER_PROFILES
```

`APPLY_DRIVER_PROFILE`은 `--es ACTION_PARAM '{"driverName":"..."}'`로 이름을 넘깁니다. adb 셸에서 한글이 깨지면 QA 콘솔에서 닉네임을 영문으로 만든 뒤 시험합니다.

## 3. 실측 표

Pleos QA 앱의 "탑승 (2단계 적용)" 로그(✓/✗와 상세)와 Virtual Console 화면을 함께 봅니다. **"요청 보냄"은 onComplete가 오지 않았지만 실패도 없었다는 뜻**이라, Virtual Console 값이 실제로 바뀌었는지가 판정 기준입니다.

| # | 항목 | 확인 방법 | 기록할 것 | 결과 |
| --- | --- | --- | --- | --- |
| M1 | SDK 연결 | QA 화면 맨 위 상태 줄 | "Vehicle SDK 연결됨"인지 | |
| M2 | 공조 온도 | 프로필 19℃·27.5℃로 각각 적용 | 로그의 범위·단위, VC 표시값, 0.5 단위 반올림 | |
| M3 | 풍량 | 프로필 풍량 0·4·8 | 범위(0–8?), 0이 꺼짐인지 | |
| M4 | 시트 열선 | 1·2·3단계 | VC 값 일치 여부 | |
| M5 | 시트 통풍 | 1·2·3단계 | VC 값 일치 여부 | |
| M6 | 핸들 열선 | 켬/끔 | 켬일 때 VC가 Low인지(코드는 지원 최저 단계로 켬) | |
| M7 | 시트 자세(메모리) | QA 콘솔에서 시트 슬롯 2 저장 후 적용 | `selectSeatMemory` 오류 여부, 슬롯 범위 | |
| M8 | 시트 자세(위치) | 슬롯 없이 위치값만 저장 후 적용 | 위치 구독 값이 오는지, 이동·정지 여부, FORWARD가 값을 늘리는지 | |
| M9 | 앰비언트 임의 색 | 테마 조명색 `#7C4DFF` | 로그가 `#7C4DFF`인지 `→ Purple`(대체)인지, VC 색 | |
| M10 | 앰비언트 밝기 | 프로필 밝기 30·100 | 범위, VC 값 | |
| M11 | 착석 감지 | VC 운전석 점유 끔→켬 | BoardingTrigger 로그, 한 번만 시작되는지 | |
| M12 | 기어 | VC 기어 D에서 착석 → P로 | D에서는 시작 안 하고 P에서 시작하는지 | |
| M13 | 주행 규제 | S7 | 어느 항목이 어떤 오류로 실패하는지 | |
| M14 | TTS 엔진 | 웰컴 직후 홈 하단 "인사말 음성"·QA 로그 | "Pleos TTS"인지 "안드로이드 기본 TTS"인지 | |
| M15 | TTS 품질 | 웰컴 음성 듣기 | 첫 음절 잘림(200ms 여유로 충분한지), 한국어 발음 | |
| M16 | 앱 화면 크기 | 폼 팩터 테스트(12.9″ 세로, 14.6″ 가로) | 글자 잘림·겹침. 지금 레이아웃은 가로 기준 | |
| M17 | 이미지 | S8 | 배경·캐릭터 표시, 로딩 시간 | |
| M18 | 재실행 | 앱 강제 종료 후 다시 열기 | 다시 등록하지 않고 같은 연결 코드인지(토큰 보관) | |

## 4. 실측 후 할 일

- M7·M8 결과로 시트 자세 방식 확정(메모리 슬롯만 / 위치 이동 병행). FORWARD 방향을 알면 `SeatAxisMover`의 방향 뒤집기 시도를 줄일 수 있습니다.
- M9가 대체 색이면 기획서·화면 문구를 "테마 색에 가장 가까운 실내 조명"으로 고칩니다.
- M14가 기본 TTS면 Pleos 음성 서비스(CAAS) 설치 여부를 확인하고, 필요하면 HYBRID + `registerApp`(My Project Client ID/Secret)을 검토합니다. 시크릿은 저장소에 넣지 않습니다.
- M16에서 세로 화면이 깨지면 `WaitingScreen`·`HomeScreen`을 세로 배치로 나눕니다.
