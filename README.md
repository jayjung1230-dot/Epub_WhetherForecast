# Ridibooks Paper Pro — E-Ink 날씨 · 정보 대시보드

**리디북스 페이퍼 프로(Ridibooks Paper Pro)**에서 순정 전자책 뷰어(`com.ridi.paper`)와 겸용으로 사용할 수 있는 **E-Ink 전용 대시보드 앱**입니다.  
서재에 등록한 트리거 EPUB(`날씨 대시보드.epub`)을 열면 대시보드가 자동 실행되며, 물리 페이지 넘김 버튼 또는 하단 터치로 **날씨 · 명언 · 뉴스 · 책 · 영화 · 경제지표 · 영단어/문장 · 오늘의 역사** 페이지를 전환할 수 있습니다.

> [!WARNING]
> **커스텀 및 타 기기 적용 시 주의사항**
> - **현재 버전은 리디북스 페이퍼 프로(`1404 × 1872`, Android 4.4.2, `ntx_6sl`)에 최적화되어 있습니다.**
> - 타 기기 이식 또는 레이아웃/대기화면 기능을 커스텀할 경우 기기별 화면 비율 및 DPI 차이로 인해 **해상도 문제(레이아웃 깨짐, 여백 불일치, 대기화면 이미지 규격 불일치 등)**가 발생할 수 있습니다.
> - 대기화면 연동 기능은 리디북스 페이퍼 프로의 `/cache/sleep/suspend.jpg` (`1404 × 1872` JPEG) 및 `persist.sys.standbytype` 구조를 기준으로 동작하며 루트 권한(`su`)이 필요합니다.

## 주요 기능
- **상단 정보 영역**: 오전/오후 12시간제 메인 시계, 양력/음력 날짜, 일출·일몰, 상단 잔여 배터리 아이콘/퍼센트, 세계시간(최대 2개, 시간대별 남·북반구 주요 대도시 + 시차 표시)
- **현재 날씨 및 5대 환경 지표**: 기온, 어제 대비 기온 비교, 최고/최저, 체감온도, 강수확률, 미세먼지(PM10), 초미세먼지(PM2.5), 자외선 지수(UV), 습도, 풍속/풍향
- **하단 페이지 전환** (물리 페이지 넘김 버튼 / 하단 좌우 터치, **날씨 고정 + 나머지 페이지 설정에서 ON/OFF 토글**):
  1. **시간별 · 주간 날씨** *(고정)*: 00시~익일 06시(3시간 간격) 시간별 예보 + 기온 변화 그래프 + 현재 시간대 강조 + 주간 7일 예보
  2. **오늘의 명언**: 영화·애니메이션 명대사 포함 **500건**, 작품 배경 및 해석 제공 (새로고침 시 중복 없는 순환 교체)
  3. **주요 뉴스**: 날씨 지역/언어 연동 실시간 뉴스 헤드라인 6건
  4. **오늘의 책 추천**: 도서 상세 줄거리, 표지 이미지 및 **실제 평론가·독자 평가** 표시 (템플릿 0%, 새로고침 시 중복 없는 순환 교체)
  5. **오늘의 영화 소개**: 영화 상세 줄거리, 실제 영화 포스터 및 **실제 평론가·관객 평가** 표시 (템플릿 0%, 새로고침 시 중복 없는 순환 교체)
  6. **주요 경제지표**: 코스피, 코스닥, 원/달러 환율, S&P 500, 나스닥, 국제 금 시세 및 7일 추이 그래프
  7. **오늘의 영단어 · 문장**: 영단어, 발음기호, 뜻, 유의어, 예문(영/한), 연관 표현 및 뉘앙스 팁
  8. **오늘의 역사 · 사건**: 오늘 날짜 기준 연도별 주요 역사적 사건 3건 요약
- **편의 및 E-Ink 최적화 설정**:
  - 시·구·동 계층형 지역 선택 + 국내/해외 지역 검색 + Wi-Fi 자동 위치
  - **언어 설정 (`한국어` / `English`)** 및 날씨 지역 연동 국가별 뉴스 피드
  - 업데이트 주기(`30분`~`6시간`), **하단 페이지 자동 전환(`사용 안 함` / `1분` / `3분` / `5분` / `10분`)**
  - 화면 유지 타이머(**`5분`, `30분`, `1~12시간`**), **10% 단위 디스플레이 밝기 조절**
  - **유지시간 만료 시 대기화면 적용 (ON/OFF)**: ON 시 기존 시스템 대기화면(`/cache/sleep/suspend.jpg` 및 시스템 설정)을 백업한 뒤 유지시간 만료 시 마지막 대시보드 화면을 대기화면으로 적용하고, OFF 시 원래 시스템 대기화면으로 복원
  - E-Ink 고대비 라운드 사각형 ON/OFF 토글 스위치 적용

## 문서 및 샘플
- **초보자용 단계별 설치 가이드 (ADB/USB):** [docs/INSTALL_GUIDE.md](docs/INSTALL_GUIDE.md)
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
- **네트워크:** 날씨/위치/콘텐츠 업데이트를 위해 기기 Wi-Fi 연결 필요
