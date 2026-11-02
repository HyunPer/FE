# 제출용 릴리스 APK

공모전 안내: **실행 가능한 릴리스 빌드**를 저장소 `apk/` 폴더 또는 GitHub Releases에 `팀명_앱이름_release.apk` 이름으로 올립니다.

## 한 번에 만들기

```bash
./gradlew :vehicle-app:exportReleaseApk
```

→ `apk/귤까먹는헬창_<앱이름>_release.apk`. `.gitignore`는 `apk/*_release.apk`만 올라가게 되어 있습니다.

빌드가 끝날 때 두 가지를 경고합니다. 둘 다 해결한 뒤 올립니다.

| 경고 | 해결 |
| --- | --- |
| `keystore.properties가 없어 디버그 키로 서명` | 아래 "서명 키" |
| `릴리스 서버 주소가 HTTP` | HTTPS 서버를 배포하고 `gradle.properties`의 `pleos.releaseApiBaseUrl` 채우기. 릴리스는 HTTP 접속이 막혀 있습니다(Pleos 보안 가이드 TLS 1.2+) |

## 앱 이름이 정해지면

`gradle.properties` 한 곳만 바꿉니다.

```properties
pleos.appName=정한이름
```

화면에 보이는 이름은 `vehicle-app/src/main/res/values/strings.xml`의 `app_name`입니다. Gleo `invokeWord`(docs/gleo/schema.json)도 같은 이름으로 맞춥니다.

## 서명 키

키 파일과 비밀번호는 **저장소에 올리지 않습니다**(`keystore.properties`, `*.jks`는 .gitignore). 팀 대표 한 명이 만들고 안전한 곳에 백업합니다. 키를 잃으면 같은 앱으로 업데이트할 수 없습니다.

```bash
keytool -genkeypair -v -keystore release.jks -alias pleos -keyalg RSA -keysize 2048 -validity 10000
```

FE 루트에 `keystore.properties`를 만듭니다(경로는 FE 루트 기준).

```properties
storeFile=release.jks
storePassword=...
keyAlias=pleos
keyPassword=...
```

## 올리기 전 확인

- [ ] 에뮬레이터에 릴리스 APK를 설치해 S1–S5(docs/EMULATOR_QA.md)가 HTTPS 서버로 동작한다
- [ ] 런처 아이콘이 "Pleos 프로필" 하나뿐이다(QA 화면은 디버그에만 들어감)
- [ ] 파일 이름이 `귤까먹는헬창_<앱이름>_release.apk`
- [ ] 접수 메일 제목 "차량용 앱 개발 공모전_앱명_팀명", 팀원 전원 참조
