# 작업 이력 및 현재 상태 (2026-11-14)

## 1. 구현 완료된 소스 코드 (`app/src/main/java/com/ridipaper/weather/`)
- `MainActivity.kt`: 대시보드 메인 액티비티, 30분 자동 갱신(Wi-Fi 연결 시), 기기 절전과 독립된 1~12시간 화면 유지 타이머(`FLAG_KEEP_SCREEN_ON` + `WakeLock`), `[지역]` 선택(Wi-Fi IP 자동 감지/수동 도시), `[설정]`(화면 유지 시간 1~12h, 디스플레이 밝기 슬라이더, **세계시간 1·2 선택**), `[닫기]`(`su`를 통한 리디북스 서재 복귀)
- `ui/DashboardView.kt`: `1404 × 1872` E-Ink 고대비 흑백 대시보드 커스텀 뷰 (현재 시간/날짜/**음력 날짜**/일출·일몰/**세계시간 최대 2개**, 현재 기온·최고/최저, 5대 환경 지표[미세먼지 PM10, 초미세먼지 PM2.5, 자외선 지수, 습도, 풍속], 시간별 8슬롯, 주간 7일 예보)
- `util/LunarCalendar.kt`: API 19 오프라인 음력 변환기 (1900~2100)
- `ui/WeatherIconDrawer.kt`: E-Ink용 벡터 날씨 아이콘 렌더러
- `data/WeatherRepository.kt`: `ip-api.com`(Wi-Fi IP 기반 위치) + **`api.met.no` (7일 예보·시간별·현재 날씨)** + **`http://air-quality-api.open-meteo.com` (PM10/PM2.5/UV)** + NOAA 일출·일몰 계산 (`wttr.in` 폴백 및 Android 4.4.2용 `TLSSocketFactory` 포함)
- `data/SettingsStore.kt`: `SharedPreferences` 기반 설정 저장, 한국 주요 도시 프리셋 및 **세계시간 도시 프리셋**
- `model/WeatherModels.kt`: 데이터 모델 (`WeatherData`, `HourlyItem`, `DailyItem`, `DashboardSettings`, `CityPreset`, `WorldClockPreset`)
- `service/LibraryWatcherService.kt` & `BootReceiver.kt`: `ActivityManager.getRunningAppProcesses()`로 `com.ridi.paper` PID 조회 후 `/proc/<pid>/fd`의 열린 `Annotations.db` UUID와 `extracted/OEBPS/content.opf`의 고유 식별자(`urn:uuid:ridipaper-weather-dashboard-001`)를 매칭해 트리거 EPUB 열림 시 대시보드 자동 실행

## 2. 빌드 및 기기 설치/검증 상태 (`PP1A1HA178296`)
- `gradle assembleDebug` 빌드 성공 (`app/build/outputs/apk/debug/app-debug.apk`, `minSdk 19`, `targetSdk 19`)
- `adb install -r`로 `com.ridipaper.weather` 기기 설치 성공
- Wi-Fi 연결 후 대시보드 실측 데이터(위치 자동 감지, 음력 날짜, 세계시간 2개, 5대 환경 지표[PM10/PM2.5/UV/습도/풍속], 시간별 8슬롯, 주간 7일 예보) 기기 화면 캡처로 정상 표시 검증 완료

## 3. 추가 구현 완료 사항 (하단 3페이지 전환 및 시·구·동/업데이트 주기)
- `data/RegionStore.kt` & `assets/kr_regions.tsv`: 시/도 → 시/군/구 → 읍/면/동 계층형 지역 선택
- `data/SettingsStore.kt`: 시간대순 남·북반구 주요 대도시 세계시간 확장 및 업데이트 주기 설정(`30분`, `1시간`, `2시간`, `3시간`, `6시간`)
- `data/ContentRepository.kt`, `ui/DashboardView.kt`, `MainActivity.kt`:
  - 물리 페이지 넘김 버튼(`KEYCODE_VOLUME_UP/DOWN`, `KEYCODE_PAGE_UP/DOWN`, `KEYCODE_DPAD_LEFT/RIGHT`, `KEYCODE_MEDIA_PREVIOUS/NEXT`) 및 하단 페이지네이션(`◀ 1 / 3 · ... ▶`) 터치로 **1. 날씨 예보 / 2. 오늘의 명언 / 3. 주요 뉴스** 3페이지 순환 전환
  - Wi-Fi 연결 시 날씨 업데이트 주기와 동일한 주기로 명언(`dummyjson.com` + 한국어 번역, 오프라인 폴백 포함)과 주요 뉴스(`news.google.com/rss?hl=ko&gl=KR&ceid=KR:ko`) 동시 갱신
  - 기기(`PP1A1HA178296`)에서 APK 설치 및 `KEYCODE_VOLUME_DOWN` 키 이벤트로 1·2·3페이지 실측 렌더링 검증 완료

## 4. 추가 구현 완료 사항 (다국어 한국어/영어 & 날씨 지역 연동 뉴스)
- `model/WeatherModels.kt` & `data/SettingsStore.kt`: `language` (`"ko"` / `"en"`), `manualCountryCode`, `WeatherData.countryCode` 추가 및 세계시간 영문 도시명 자동 변환
- `data/WeatherRepository.kt`: `ip-api.com` 국가 코드(`countryCode`) 추출, Nominatim 국내/해외 지역 검색 및 국가 코드 연동, 날씨 상태·풍향·대기질/자외선/습도 등급 한/영 지원
- `data/ContentRepository.kt`:
  - `fetchQuotes(language)`: 한국어 설정 시 번역, 영어 설정 시 영문 원문 직접 출력
  - `fetchNews(countryCode, language)`: 날씨 조회 지역의 `countryCode`와 앱 언어 조합에 맞춰 Google News RSS 자동 연동
- `ui/DashboardView.kt` & `MainActivity.kt`: 전체 UI 한/영 전환 및 `[설정]` 내 언어 선택 추가 (언어 변경 시 즉시 재조회), 기기(`PP1A1HA178296`)에서 한국어/영어 1·2·3페이지 실측 검증 완료

## 5. 추가 최적화 및 UI 개선 (2026-11-14)
- `MainActivity.kt`:
  - 하단 페이지 시간 단위 자동 전환 옵션(`사용 안 함`, `1분`, `5분`, `10분`, `30분`, `1시간`) 추가 및 수동 터치/물리 버튼 전환 시 타이머 리셋
  - `[설정]` 다이얼로그를 2열 가로 배치(`ScrollView` + `simple_spinner_item` + `minimumWidth`)로 개편해 세로 길이를 대폭 축소하고 항목 줄바꿈 방지
- `ui/DashboardView.kt`: 다양한 전자책 해상도 및 화면 비율(`600×800` ~ `1860×2480`)에서 비율을 유지한 중앙 정렬 스케일링 및 터치 좌표 오프셋 보정 적용

## 6. GitHub 공개 및 최종 확인 사항
1. 저장소 루트 [WeatherDashboard.epub](WeatherDashboard.epub) 배치 및 소스코드 GitHub(`jayjung1230-dot/Epub_WhetherForecast`) 업로드 완료
2. 재설치된 앱에 대해 기기에서 SuperSU 루트 권한 허용 팝업 승인 후, 리디북스 서재 타일에서 `날씨 대시보드` 탭 → 대시보드 자동 전환 및 `[닫기]` → 서재 복귀 동작 최종 확인

## 7. 후속 기능 확장 및 E-Ink 최적화 (2026-11-14)
- `ui/DashboardView.kt`: 00시~익일 06시 시간별 예보 및 기온 변화 그래프·현재 시간대 강조, 주간 7일 예보, 영화·애니 명대사(작품 배경/해석), 오늘의 책 추천(표지 이미지), 오늘의 영화 소개(실제 영화 포스터), 주요 경제지표(6종 시세 + 7일 추이 그래프) 구현
- `data/ContentRepository.kt`: Open-Meteo / Google News RSS / OpenLibrary(책 표지) / `http://wsrv.nl` 프록시 기반 영화 포스터(Android 4.4.2 호환) / Yahoo Finance 경제지표 연동
- `MainActivity.kt` & `util/SleepScreenManager.kt`:
  - 설정 다이얼로그 E-Ink 라운드 사각형 ON/OFF 토글 스위치 적용 및 하단 페이지 선택 표시(날씨 고정, 나머지 5개 페이지 개별 ON/OFF)
  - 화면 유지 시간 만료 시 대시보드 캡처를 `/cache/sleep/suspend.jpg`(`1404 × 1872`)로 적용하고, OFF 전환 시 원본 시스템 대기화면 및 `persist.sys.standbytype` / `sleepscreen_file_path` 설정을 복원하는 기능 구현 (타겟 기기: 리디북스 페이퍼 프로 최적화)

## 8. 시계·배터리·타이머 개선 및 영단어/역사 페이지 추가 (2026-11-14)
- `ui/DashboardView.kt`:
  - 메인 시계를 오전/오후(`AM`/`PM`) 12시간제로 변경하고, 상단 상태 줄 오른쪽에 E-Ink 배터리 아이콘 및 잔량(`%`) 표시 추가
  - `오늘의 영단어 · 문장`(`drawEnglishPage`) 및 `오늘의 역사 · 사건`(`drawHistoryPage`) 페이지 구현
- `model/WeatherModels.kt`, `data/SettingsStore.kt`, `MainActivity.kt`, `data/ContentRepository.kt`:
  - 화면 유지 시간에 `5분`, `30분` 옵션 추가 (`keepScreenMinutes`)
  - 하단 페이지 자동 전환 선택지를 `사용 안 함`, `1분`, `3분`, `5분`, `10분`으로 정리
  - `오늘의 영단어 · 문장` 및 `오늘의 역사 · 사건` 데이터 로더와 설정 ON/OFF 토글 스위치 추가
- `docs/INSTALL_GUIDE.md` & `README.md`: 리디북스 페이퍼 프로 기준 ADB/USB 단계별 설치 가이드 작성 및 전체 문서 현행화
