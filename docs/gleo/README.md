# Gleo Interface 연동 (초안)

운영사무국 답변(11/2): **공모전 앱에서 Gleo Interface 사용 가능.** 신청·승인 절차, App Schema 등록 방법, 일정은 아직 안내 전입니다.

## 무엇을 등록하나

[`schema.json`](schema.json): function 3개(최대치).

| function | 말하면 | 앱 동작 |
| --- | --- | --- |
| `APPLY_NEXT_DRIVER` | "가족 프로필, 예약한 사람 설정으로 해줘" | 예약된 다음 운전자 적용(= "지금 적용") |
| `APPLY_DRIVER_PROFILE` | "가족 프로필, 엄마 출근 프로필로" | `driverName`(필수)·`profileName`으로 멤버·프로필을 찾아 적용 |
| `SHOW_DRIVER_PROFILES` | "가족 프로필, 운전자 목록 보여줘" | 1탭 선택 화면 |

작성 규칙(App Schema 가이드): description은 영어 1–2문장, 예시 문장 금지, parameter type은 string/number/boolean/array, null 불가, required에는 필수만.

## 앱 쪽은 이미 들어가 있음

- `VehicleActivity`에 function 이름과 같은 action의 intent-filter(`singleTask`).
- `voice/GleoActions`: extra `ACTION_PARAM`(JSON)을 읽어 `VehicleController`로 넘기고, 7초 안에 `ai.pleos.playground.intent.action.APP_ACTION_RESPONSE` 브로드캐스트(`packageName`, `response`)로 답합니다. 차량 설정은 7초를 넘길 수 있어 "적용을 시작했다"고 먼저 답합니다.
- 이름 맞추기는 `core-api`의 `DriverMatcher`(공백 무시, 부분 일치, 단위 테스트 있음).
- 승인 전에도 `adb shell am start ... -a APPLY_NEXT_DRIVER`로 앱 쪽 처리를 시험할 수 있습니다(docs/EMULATOR_QA.md).

## 승인되면 할 일

1. `invokeWord`를 정한 앱 이름으로, `packageName`을 최종 applicationId로, `version`을 versionName으로 맞춘다.
2. `schema.json`을 `vehicle-app/src/main/assets/schema.json`으로 옮긴다(Gleo는 부팅·설치 때 스캔).
3. 응답 브로드캐스트에 Gleo 패키지를 지정하라는 안내가 있으면 `GleoActions.respond`에 `setPackage`를 추가한다.

## 승인이 늦어지면

시연은 화면 버튼과 착석+P단 트리거로 충분합니다. 기획서·PT에는 "Gleo 연동 준비 완료(스키마·앱 처리 구현), 승인 대기"로 씁니다. 앱 안에 마이크 버튼을 두고 SpeechToText SDK(ON_DEVICE)로 같은 세 명령을 처리하는 대안도 있습니다(미구현).
