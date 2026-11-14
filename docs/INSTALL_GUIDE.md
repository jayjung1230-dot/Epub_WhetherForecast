# 리디북스 페이퍼 프로(Ridibooks Paper Pro) 설치 가이드

리디북스 페이퍼 프로와 같은 커스텀 전자책 리더기는 일반 스마트폰과 달리 **파일 탐색기에서 `.apk` 파일을 직접 눌러 설치하는 기능이 제한**되어 있습니다.  
이 문서는 실제 개발·테스트 환경(Windows PC + USB `adb` + `PaperProKitchen` 루팅된 리디북스 페이퍼 프로)에서 진행한 순서를 그대로 정리한 **단계별 설치 가이드**입니다.

> [!WARNING]
> **커스텀 및 타 기기 적용 시 주의사항**  
> - **현재 버전은 리디북스 페이퍼 프로(`1404 × 1872`, Android 4.4.2, `ntx_6sl`)에 최적화되어 있습니다.**  
> - 타 기기나 다른 해상도로 커스텀할 경우 **해상도·화면 비율 차이로 인해 UI/터치/대기화면 이미지 규격(`1404 × 1872`)에서 해상도 문제**가 발생할 수 있습니다.

---

## 0. 준비물 및 사전 확인
1. **리디북스 페이퍼 프로** (`Android 4.4.2`, 루팅 및 `SuperSU` 적용 기기)
2. **Windows PC** 및 데이터 전송 가능한 **USB 케이블**
3. **PC 도구** (미리 빌드된 `app-debug.apk`가 있는 경우 ADB만 있으면 됩니다)
   - **ADB만 사용 (빌드된 APK 보유 시):** Android SDK Platform-Tools (`adb.exe`)
   - **GitHub 소스에서 직접 빌드 시:** JDK, Gradle, Android SDK (`platform-tools` + API 19 빌드 환경)

---

## 1단계: 저장소 받기 (GitHub)
GitHub에서 프로젝트를 내려받습니다. (또는 우측 상단 `Code → Download ZIP` 후 압축 해제)
```powershell
git clone https://github.com/jayjung1230-dot/Epub_WhetherForecast.git
cd Epub_WhetherForecast
```

---

## 2단계: `app-debug.apk` 빌드하기 *(미리 빌드된 APK가 없는 경우)*
GitHub 저장소에는 `app/build/` 폴더가 포함되어 있지 않으므로, 소스에서 APK를 1회 빌드해야 합니다.

1. 프로젝트 루트에 `local.properties` 파일을 만들고 본인의 Android SDK 경로를 지정합니다.
   ```properties
   sdk.dir=C\:\\Users\\<사용자이름>\\android-toolchain\\sdk
   ```
2. PowerShell에서 JDK 경로를 지정한 뒤 Gradle로 디버그 APK를 빌드합니다.
   ```powershell
   $env:JAVA_HOME = "C:\Users\<사용자이름>\android-toolchain\jdk"
   & "C:\Users\<사용자이름>\android-toolchain\gradle\bin\gradle.bat" assembleDebug
   ```
3. `BUILD SUCCESSFUL` 메시지가 나오면 아래 경로에 설치 파일이 생성됩니다.
   - `app\build\outputs\apk\debug\app-debug.apk`

---

## 3단계: 페이퍼 프로 USB 연결 및 ADB 인식 확인
1. USB 케이블로 페이퍼 프로와 PC를 연결합니다.
2. 기기 화면에 **"USB 디버깅을 허용하시겠습니까?"** 팝업이 뜨면 **"항상 허용"** 체크 후 **확인**을 누릅니다.
3. PowerShell에서 `adb.exe`로 기기 연결을 확인합니다.
   ```powershell
   $adb = "C:\Users\<사용자이름>\android-toolchain\sdk\platform-tools\adb.exe"
   & $adb devices
   ```
4. 아래처럼 시리얼 번호 옆에 `device`가 표시되면 연결 완료입니다.
   ```text
   List of devices attached
   PP1A1HA178296   device
   ```
   - `unauthorized`로 표시되면 기기 화면의 USB 디버깅 허용 팝업을 다시 확인하세요.

---

## 4단계: APK 설치 (`adb install -r`)
아래 명령어로 `com.ridipaper.weather` 앱을 기기에 설치합니다. (기존 버전이 있어도 `-r` 옵션으로 덮어쓰기 설치됩니다.)
```powershell
& $adb install -r "app\build\outputs\apk\debug\app-debug.apk"
```
- `Success`가 출력되면 설치 완료입니다.

---

## 5단계: 서재 실행용 트리거 EPUB 복사
서재에서 앱을 바로 열 수 있도록 저장소 루트의 `WeatherDashboard.epub` 파일을 기기의 리디북스 서재 폴더(`/sdcard/Books/`)로 복사합니다.
```powershell
& $adb push WeatherDashboard.epub /sdcard/Books/WeatherDashboard.epub
```
*(PC 파일 탐색기에서 페이퍼 프로 내부 저장소의 `Books` 폴더로 직접 복사해도 동일합니다.)*

---

## 6단계: 최초 1회 실행 및 SuperSU 루트 권한 허용
설치 직후에는 백그라운드 서재 감시 서비스(`LibraryWatcherService`)를 깨워주고 루트 권한을 1회 승인해야 합니다.

1. PC에서 아래 명령어로 대시보드 앱을 1회 실행합니다. *(또는 기기를 1회 재부팅해도 됩니다.)*
   ```powershell
   & $adb shell am start -S -n com.ridipaper.weather/.MainActivity
   ```
2. 페이퍼 프로 화면에 **SuperSU 루트 권한 요청** 팝업이 뜨면 **허용(Grant)** 을 누릅니다.  
   *(서재 열림 감지, `[닫기]`로 리디북스 서재 복귀, 대기화면 연동 기능에 필요합니다.)*
3. 대시보드 화면이 뜨면 우측 상단 **`[닫기]`** 버튼을 눌러 리디북스 서재로 돌아갑니다.
4. 이제 리디북스 서재에 나타난 **`날씨 대시보드`** 책 아이콘을 열면 대시보드로 자동 전환됩니다!

---

## 7단계: 사용 및 문제 해결 팁
- **네트워크 연결:** 날씨, 뉴스, 경제지표, 책 표지, 영화 포스터 갱신을 위해 기기의 **Wi-Fi**가 연결되어 있어야 합니다.
- **서재에서 책을 열어도 대시보드가 안 뜰 때:**  
  1) 페이퍼 프로를 **1회 재부팅**해 부팅 리시버(`BootReceiver`)가 감시 서비스를 시작하게 하거나,  
  2) SuperSU 앱에서 `com.ridipaper.weather` 권한이 허용되어 있는지 확인하세요.
- **기본 조작:**
  - 페이지 전환: 기기 좌우 물리 페이지 넘김 버튼 또는 화면 하단 `◀ / ▶` 터치
  - 서재 복귀: 우측 상단 `[닫기]` 또는 기기 뒤로가기 버튼
