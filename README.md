# Ridibooks Paper Pro — E-Ink 날씨 · 명언 · 뉴스 대시보드

리디북스 페이퍼 프로(Ridibooks Paper Pro)에서 순정 전자책 뷰어(`com.ridi.paper`)와 겸용으로 사용할 수 있는 **E-Ink 전용 대시보드 앱**입니다.  
서재에 등록한 트리거 EPUB(`날씨 대시보드.epub`)을 열면 대시보드가 자동 실행되며, 물리 페이지 넘김 버튼으로 **날씨 예보 · 오늘의 명언 · 주요 뉴스**를 전환할 수 있습니다.

## 주요 기능
- **상단 정보 영역**: 현재 시각, 양력/음력 날짜, 일출·일몰, 세계시간(최대 2개, 시간대별 남·북반구 주요 대도시 + 시차 표시)
- **현재 날씨 및 5대 환경 지표**: 기온, 어제 대비 기온 비교, 최고/최저, 체감온도, 강수확률, 미세먼지(PM10), 초미세먼지(PM2.5), 자외선 지수(UV), 습도, 풍속/풍향
- **하단 3페이지 전환** (물리 페이지 넘김 버튼 / 하단 좌우 터치):
  1. **1/3 날씨 예보**: 3시간 간격 시간별 8슬롯 + 주간 7일 예보
  2. **2/3 오늘의 명언**: 노력·동기부여·가능성·행동 테마 명언 2건 (Wi-Fi 자동 갱신)
  3. **3/3 주요 뉴스**: 실시간 주요 뉴스 헤드라인 6건 (Wi-Fi 자동 갱신)
- **편의 설정**: 시·구·동 계층형 지역 선택 + 동네 검색 + Wi-Fi 자동 위치, 업데이트 주기(`30분`~`6시간`), 화면 유지 타이머(`1~12시간`), 10% 단위 디스플레이 밝기 조절

## 예정된 다음 개발 항목 (GitHub 공개 준비)
- **언어 설정 (`한국어` / `English`)**: 대시보드 UI 전체, 명언(원문/번역), 뉴스 표시 언어 연동
- **뉴스 대륙/국가 선택**: 대륙별(아시아·태평양, 북미, 유럽 등) 주요 국가 선택에 따른 맞춤형 뉴스 피드 제공
- **GitHub 배포 패키지**: 리디북스 서재용 트리거 EPUB 파일 + APK + 설치 가이드를 릴리즈 형태로 정리

## 문서 및 샘플
- 설계서 및 요구사항 정리: [docs/DESIGN_PLAN.md](docs/DESIGN_PLAN.md)
- 개발 이력 및 현재 상태: [docs/WORK_LOG.md](docs/WORK_LOG.md)
- 대시보드 UI 샘플 (`1404 × 1872`):
  - Page 1 (날씨 예보): [docs/weather_dashboard_sample.png](docs/weather_dashboard_sample.png)
  - Page 2 (오늘의 명언): [docs/sample_page2_quotes.png](docs/sample_page2_quotes.png)
  - Page 3 (주요 뉴스): [docs/sample_page3_news.png](docs/sample_page3_news.png)
- 서재 책 아이콘 표지 샘플 (`600 × 850`): [docs/library_book_cover_sample.png](docs/library_book_cover_sample.png)
- 샘플 목업 생성 스크립트: [docs/generate_mockup.py](docs/generate_mockup.py)

## 설치 및 실행 방법 (배포용 개요)
1. **트리거 EPUB 다운로드**: 저장소 최상단의 [WeatherDashboard.epub](WeatherDashboard.epub) 파일을 다운로드해 기기의 `/sdcard/Books/` (리디북스 서재 폴더)에 복사
2. **APK 설치**: 기기에 `com.ridipaper.weather` APK 설치 (`adb install -r ...`)
3. **권한 부여 및 실행**: 리디북스 서재에서 `날씨 대시보드` 책 아이콘을 열면 대시보드 실행 (최초 1회 SuperSU 루트 권한 허용 필요)
4. **서재 복귀**: 대시보드 우측 상단 `[닫기]` 또는 기기 뒤로가기 버튼으로 리디북스 서재로 즉시 복귀

## 확인된 타겟 기기 정보 (ADB 실측)
- **ADB 시리얼 / 모델:** `PP1A1HA178296` (`product:RBPP1`, `model:PAPER_PRO`, `device:PAPER_PRO`)
- **OS:** Android `4.4.2` (SDK `19`, `KOT49H`, `armeabi-v7a`)
- **디스플레이:** `1404 × 1872`, `density 320` (`213.0 x 213.0 dpi`, E-Ink)
- **저장공간:** `/data` (`6.3G` 중 `3.9G` 여유), `/sdcard` -> `/storage/emulated/legacy`
- **커스텀/루트 상태:** `PaperProKitchen` 적용됨 (`/system/xbin/su`, SuperSU v2.79, `win.limerainne.itoast`, `kor.superuser.config`, `com.halbae87.koreanbasicime`)
- **리디북스 앱:** `com.ridi.paper` (`versionName=P213_1.6.5`, `MainActivityPaper`, `EPubReaderActivity`, `PDFReaderActivity`, `ComicBookReaderActivity`, `BomReaderActivity`)
- **네트워크:** 점검 시점 Wi-Fi `UNINITIALIZED` (날씨/위치 업데이트를 위해 기기 Wi-Fi 연결 필요)
