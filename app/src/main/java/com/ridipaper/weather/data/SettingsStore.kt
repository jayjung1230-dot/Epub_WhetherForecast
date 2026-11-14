package com.ridipaper.weather.data

import android.content.Context
import com.ridipaper.weather.model.CityPreset
import com.ridipaper.weather.model.DashboardSettings
import com.ridipaper.weather.model.WorldClockPreset

import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("paper_weather_prefs", Context.MODE_PRIVATE)

    fun load(): DashboardSettings {
        val rawRefresh = prefs.getInt("refresh_interval_minutes", 30)
        val validRefresh = if (rawRefresh in listOf(30, 60, 120, 180, 360)) rawRefresh else 30
        val rawRotate = prefs.getInt("page_auto_rotate_minutes", 0)
        val validRotate = if (rawRotate in listOf(0, 1, 5, 10, 30, 60)) rawRotate else 0
        val rawLang = prefs.getString("language", "ko") ?: "ko"
        val validLang = if (rawLang == "en") "en" else "ko"
        return DashboardSettings(
            keepScreenHours = prefs.getInt("keep_screen_hours", 1).coerceIn(1, 12),
            refreshIntervalMinutes = validRefresh,
            pageAutoRotateMinutes = validRotate,
            brightnessPercent = prefs.getInt("brightness_percent", 40).coerceIn(0, 100),
            autoLocation = prefs.getBoolean("auto_location", true),
            manualCityName = prefs.getString("manual_city", "서울특별시") ?: "서울특별시",
            manualLat = java.lang.Double.longBitsToDouble(
                prefs.getLong("manual_lat", java.lang.Double.doubleToRawLongBits(37.5665))
            ),
            manualLon = java.lang.Double.longBitsToDouble(
                prefs.getLong("manual_lon", java.lang.Double.doubleToRawLongBits(126.9780))
            ),
            manualCountryCode = prefs.getString("manual_country_code", "KR") ?: "KR",
            worldClock1 = prefs.getString("world_clock_1", "뉴욕|America/New_York") ?: "뉴욕|America/New_York",
            worldClock2 = prefs.getString("world_clock_2", "런던|Europe/London") ?: "런던|Europe/London",
            language = validLang,
            showQuotesPage = prefs.getBoolean("show_quotes_page", true),
            showNewsPage = prefs.getBoolean("show_news_page", true),
            showBookPage = prefs.getBoolean("show_book_page", true),
            showMoviePage = prefs.getBoolean("show_movie_page", true),
            showEconomyPage = prefs.getBoolean("show_economy_page", true),
            useDashboardAsSleepScreen = prefs.getBoolean("use_dashboard_as_sleep_screen", false)
        )
    }

    fun save(settings: DashboardSettings) {
        prefs.edit()
            .putInt("keep_screen_hours", settings.keepScreenHours.coerceIn(1, 12))
            .putInt("refresh_interval_minutes", settings.refreshIntervalMinutes)
            .putInt("page_auto_rotate_minutes", settings.pageAutoRotateMinutes)
            .putInt("brightness_percent", settings.brightnessPercent.coerceIn(0, 100))
            .putBoolean("auto_location", settings.autoLocation)
            .putString("manual_city", settings.manualCityName)
            .putLong("manual_lat", java.lang.Double.doubleToRawLongBits(settings.manualLat))
            .putLong("manual_lon", java.lang.Double.doubleToRawLongBits(settings.manualLon))
            .putString("manual_country_code", settings.manualCountryCode)
            .putString("world_clock_1", settings.worldClock1)
            .putString("world_clock_2", settings.worldClock2)
            .putString("language", if (settings.language == "en") "en" else "ko")
            .putBoolean("show_quotes_page", settings.showQuotesPage)
            .putBoolean("show_news_page", settings.showNewsPage)
            .putBoolean("show_book_page", settings.showBookPage)
            .putBoolean("show_movie_page", settings.showMoviePage)
            .putBoolean("show_economy_page", settings.showEconomyPage)
            .putBoolean("use_dashboard_as_sleep_screen", settings.useDashboardAsSleepScreen)
            .apply()
    }

    companion object {
        val CITIES = listOf(
            CityPreset("서울특별시 강남구 역삼동", 37.5007, 127.0365),
            CityPreset("서울특별시 서초구 서초동", 37.4837, 127.0324),
            CityPreset("서울특별시 송파구 잠실동", 37.5133, 127.1001),
            CityPreset("서울특별시 마포구 서교동", 37.5547, 126.9206),
            CityPreset("서울특별시 종로구 세종로", 37.5720, 126.9769),
            CityPreset("서울특별시 중구 명동", 37.5636, 126.9826),
            CityPreset("서울특별시 용산구 이태원동", 37.5345, 126.9946),
            CityPreset("서울특별시 성동구 성수동", 37.5446, 127.0559),
            CityPreset("서울특별시 영등포구 여의도동", 37.5219, 126.9245),
            CityPreset("서울특별시 구로구 구로동", 37.4954, 126.8874),
            CityPreset("서울특별시 강서구 마곡동", 37.5668, 126.8273),
            CityPreset("서울특별시 노원구 상계동", 37.6542, 127.0568),
            CityPreset("경기도 성남시 분당구 정자동", 37.3674, 127.1082),
            CityPreset("경기도 수원시 영통구 매탄동", 37.2636, 127.0436),
            CityPreset("경기도 용인시 수지구 풍덕천동", 37.3222, 127.0977),
            CityPreset("경기도 고양시 일산동구 장항동", 37.6584, 126.7746),
            CityPreset("인천광역시 연수구 송도동", 37.3863, 126.6435),
            CityPreset("대전광역시 유성구 봉명동", 36.3536, 127.3413),
            CityPreset("세종특별자치시 어진동", 36.5041, 127.2654),
            CityPreset("대구광역시 수성구 범어동", 35.8582, 128.6306),
            CityPreset("부산광역시 해운대구 우동", 35.1631, 129.1636),
            CityPreset("울산광역시 남구 삼산동", 35.5396, 129.3389),
            CityPreset("광주광역시 서구 치평동", 35.1520, 126.8516),
            CityPreset("전북 전주시 완산구 효자동", 35.8152, 127.1195),
            CityPreset("충북 청주시 흥덕구 복대동", 36.6358, 127.4342),
            CityPreset("강원 춘천시 효자동", 37.8686, 127.7412),
            CityPreset("강원 강릉시 교동", 37.7647, 128.8825),
            CityPreset("제주특별자치도 제주시 연동", 33.4890, 126.4983)
        )

        private val RAW_WORLD_CLOCKS = listOf(
            // UTC-11 (남/북)
            WorldClockPreset("파고파고", "Pacific/Pago_Pago"),
            WorldClockPreset("미드웨이", "Pacific/Midway"),
            // UTC-10 (북/남)
            WorldClockPreset("호놀룰루", "Pacific/Honolulu"),
            WorldClockPreset("타히티", "Pacific/Tahiti"),
            // UTC-9 (북/남)
            WorldClockPreset("앵커리지", "America/Anchorage"),
            WorldClockPreset("감비에 제도", "Pacific/Gambier"),
            // UTC-8 (북/남)
            WorldClockPreset("로스앤젤레스", "America/Los_Angeles"),
            WorldClockPreset("샌프란시스코", "America/Los_Angeles"),
            WorldClockPreset("시애틀", "America/Los_Angeles"),
            WorldClockPreset("라스베이거스", "America/Los_Angeles"),
            WorldClockPreset("샌디에이고", "America/Los_Angeles"),
            WorldClockPreset("밴쿠버", "America/Vancouver"),
            WorldClockPreset("핏케언", "Pacific/Pitcairn"),
            // UTC-7 (북)
            WorldClockPreset("덴버", "America/Denver"),
            WorldClockPreset("피닉스", "America/Phoenix"),
            WorldClockPreset("솔트레이크시티", "America/Denver"),
            WorldClockPreset("캘거리", "America/Edmonton"),
            WorldClockPreset("에드먼턴", "America/Edmonton"),
            // UTC-6 (북/남)
            WorldClockPreset("시카고", "America/Chicago"),
            WorldClockPreset("댈러스", "America/Chicago"),
            WorldClockPreset("휴스턴", "America/Chicago"),
            WorldClockPreset("오스틴", "America/Chicago"),
            WorldClockPreset("미니애폴리스", "America/Chicago"),
            WorldClockPreset("멕시코시티", "America/Mexico_City"),
            WorldClockPreset("몬테레이", "America/Monterrey"),
            WorldClockPreset("갈라파고스", "Pacific/Galapagos"),
            // UTC-5 (북/남)
            WorldClockPreset("뉴욕", "America/New_York"),
            WorldClockPreset("워싱턴 D.C.", "America/New_York"),
            WorldClockPreset("보스턴", "America/New_York"),
            WorldClockPreset("필라델피아", "America/New_York"),
            WorldClockPreset("애틀랜타", "America/New_York"),
            WorldClockPreset("마이애미", "America/New_York"),
            WorldClockPreset("디트로이트", "America/Detroit"),
            WorldClockPreset("토론토", "America/Toronto"),
            WorldClockPreset("몬트리올", "America/Toronto"),
            WorldClockPreset("오타와", "America/Toronto"),
            WorldClockPreset("하바나", "America/Havana"),
            WorldClockPreset("파나마시티", "America/Panama"),
            WorldClockPreset("보고타", "America/Bogota"),
            WorldClockPreset("리마", "America/Lima"),
            WorldClockPreset("키토", "America/Guayaquil"),
            // UTC-4 (북/남)
            WorldClockPreset("핼리팩스", "America/Halifax"),
            WorldClockPreset("산후안", "America/Puerto_Rico"),
            WorldClockPreset("카라카스", "America/Caracas"),
            WorldClockPreset("산티아고", "America/Santiago"),
            WorldClockPreset("라파스", "America/La_Paz"),
            WorldClockPreset("아순시온", "America/Asuncion"),
            WorldClockPreset("마나우스", "America/Manaus"),
            // UTC-3 (북/남)
            WorldClockPreset("누크", "America/Nuuk"),
            WorldClockPreset("상파울루", "America/Sao_Paulo"),
            WorldClockPreset("리우데자네이루", "America/Sao_Paulo"),
            WorldClockPreset("브라질리아", "America/Sao_Paulo"),
            WorldClockPreset("부에노스아이레스", "America/Argentina/Buenos_Aires"),
            WorldClockPreset("몬테비데오", "America/Montevideo"),
            // UTC-2 (남)
            WorldClockPreset("페르난두 지 노로냐", "America/Noronha"),
            // UTC-1 (북)
            WorldClockPreset("아조레스", "Atlantic/Azores"),
            WorldClockPreset("카보베르데", "Atlantic/Cape_Verde"),
            // UTC+0 (북/남)
            WorldClockPreset("런던", "Europe/London"),
            WorldClockPreset("맨체스터", "Europe/London"),
            WorldClockPreset("에든버러", "Europe/London"),
            WorldClockPreset("더블린", "Europe/Dublin"),
            WorldClockPreset("리스본", "Europe/Lisbon"),
            WorldClockPreset("포르투", "Europe/Lisbon"),
            WorldClockPreset("레이캬비크", "Atlantic/Reykjavik"),
            WorldClockPreset("아크라", "Africa/Accra"),
            WorldClockPreset("세인트헬레나", "Atlantic/St_Helena"),
            // UTC+1 (북/남)
            WorldClockPreset("파리", "Europe/Paris"),
            WorldClockPreset("리옹", "Europe/Paris"),
            WorldClockPreset("마르세유", "Europe/Paris"),
            WorldClockPreset("브뤼셀", "Europe/Brussels"),
            WorldClockPreset("암스테르담", "Europe/Amsterdam"),
            WorldClockPreset("룩셈부르크", "Europe/Luxembourg"),
            WorldClockPreset("베를린", "Europe/Berlin"),
            WorldClockPreset("프랑크푸르트", "Europe/Berlin"),
            WorldClockPreset("뮌헨", "Europe/Berlin"),
            WorldClockPreset("함부르크", "Europe/Berlin"),
            WorldClockPreset("취리히", "Europe/Zurich"),
            WorldClockPreset("제네바", "Europe/Zurich"),
            WorldClockPreset("빈", "Europe/Vienna"),
            WorldClockPreset("프라하", "Europe/Prague"),
            WorldClockPreset("부다페스트", "Europe/Budapest"),
            WorldClockPreset("바르샤바", "Europe/Warsaw"),
            WorldClockPreset("로마", "Europe/Rome"),
            WorldClockPreset("밀라노", "Europe/Rome"),
            WorldClockPreset("마드리드", "Europe/Madrid"),
            WorldClockPreset("바르셀로나", "Europe/Madrid"),
            WorldClockPreset("스톡홀름", "Europe/Stockholm"),
            WorldClockPreset("오슬로", "Europe/Oslo"),
            WorldClockPreset("코펜하겐", "Europe/Copenhagen"),
            WorldClockPreset("카사블랑카", "Africa/Casablanca"),
            WorldClockPreset("라고스", "Africa/Lagos"),
            WorldClockPreset("루안다", "Africa/Luanda"),
            WorldClockPreset("킨샤사", "Africa/Kinshasa"),
            // UTC+2 (북/남)
            WorldClockPreset("헬싱키", "Europe/Helsinki"),
            WorldClockPreset("탈린", "Europe/Tallinn"),
            WorldClockPreset("부쿠레슈티", "Europe/Bucharest"),
            WorldClockPreset("아테네", "Europe/Athens"),
            WorldClockPreset("키이우", "Europe/Kiev"),
            WorldClockPreset("예루살렘", "Asia/Jerusalem"),
            WorldClockPreset("텔아비브", "Asia/Jerusalem"),
            WorldClockPreset("카이로", "Africa/Cairo"),
            WorldClockPreset("요하네스버그", "Africa/Johannesburg"),
            WorldClockPreset("케이프타운", "Africa/Johannesburg"),
            // UTC+3 (북/남)
            WorldClockPreset("이스탄불", "Europe/Istanbul"),
            WorldClockPreset("앙카라", "Europe/Istanbul"),
            WorldClockPreset("모스크바", "Europe/Moscow"),
            WorldClockPreset("상트페테르부르크", "Europe/Moscow"),
            WorldClockPreset("리야드", "Asia/Riyadh"),
            WorldClockPreset("도하", "Asia/Qatar"),
            WorldClockPreset("쿠웨이트시티", "Asia/Kuwait"),
            WorldClockPreset("바그다드", "Asia/Baghdad"),
            WorldClockPreset("아디스아바바", "Africa/Addis_Ababa"),
            WorldClockPreset("나이로비", "Africa/Nairobi"),
            WorldClockPreset("다르에스살람", "Africa/Dar_es_Salaam"),
            WorldClockPreset("안타나나리보", "Indian/Antananarivo"),
            // UTC+3:30 (북)
            WorldClockPreset("테헤란", "Asia/Tehran"),
            // UTC+4 (북/남)
            WorldClockPreset("두바이", "Asia/Dubai"),
            WorldClockPreset("아부다비", "Asia/Dubai"),
            WorldClockPreset("무스카트", "Asia/Muscat"),
            WorldClockPreset("바쿠", "Asia/Baku"),
            WorldClockPreset("트빌리시", "Asia/Tbilisi"),
            WorldClockPreset("포트루이스", "Indian/Mauritius"),
            // UTC+5 (북)
            WorldClockPreset("타슈켄트", "Asia/Tashkent"),
            WorldClockPreset("카라치", "Asia/Karachi"),
            WorldClockPreset("이슬라마바드", "Asia/Karachi"),
            WorldClockPreset("말레", "Indian/Maldives"),
            // UTC+5:30 (북)
            WorldClockPreset("뉴델리", "Asia/Kolkata"),
            WorldClockPreset("뭄바이", "Asia/Kolkata"),
            WorldClockPreset("벵갈루루", "Asia/Kolkata"),
            WorldClockPreset("첸나이", "Asia/Kolkata"),
            WorldClockPreset("콜롬보", "Asia/Colombo"),
            // UTC+5:45 (북)
            WorldClockPreset("카트만두", "Asia/Kathmandu"),
            // UTC+6 (북)
            WorldClockPreset("알마티", "Asia/Almaty"),
            WorldClockPreset("아스타나", "Asia/Almaty"),
            WorldClockPreset("다카", "Asia/Dhaka"),
            // UTC+6:30 (북)
            WorldClockPreset("양곤", "Asia/Yangon"),
            // UTC+7 (북/남)
            WorldClockPreset("방콕", "Asia/Bangkok"),
            WorldClockPreset("치앙마이", "Asia/Bangkok"),
            WorldClockPreset("하노이", "Asia/Ho_Chi_Minh"),
            WorldClockPreset("호치민", "Asia/Ho_Chi_Minh"),
            WorldClockPreset("프놈펜", "Asia/Phnom_Penh"),
            WorldClockPreset("비엔티안", "Asia/Vientiane"),
            WorldClockPreset("자카르타", "Asia/Jakarta"),
            WorldClockPreset("수라바야", "Asia/Jakarta"),
            // UTC+8 (북/남)
            WorldClockPreset("베이징", "Asia/Shanghai"),
            WorldClockPreset("상하이", "Asia/Shanghai"),
            WorldClockPreset("광저우", "Asia/Shanghai"),
            WorldClockPreset("선전", "Asia/Shanghai"),
            WorldClockPreset("청두", "Asia/Shanghai"),
            WorldClockPreset("충칭", "Asia/Shanghai"),
            WorldClockPreset("홍콩", "Asia/Hong_Kong"),
            WorldClockPreset("마카오", "Asia/Macau"),
            WorldClockPreset("타이베이", "Asia/Taipei"),
            WorldClockPreset("가오슝", "Asia/Taipei"),
            WorldClockPreset("울란바토르", "Asia/Ulaanbaatar"),
            WorldClockPreset("싱가포르", "Asia/Singapore"),
            WorldClockPreset("쿠알라룸푸르", "Asia/Kuala_Lumpur"),
            WorldClockPreset("마닐라", "Asia/Manila"),
            WorldClockPreset("세부", "Asia/Manila"),
            WorldClockPreset("발리", "Asia/Makassar"),
            WorldClockPreset("퍼스", "Australia/Perth"),
            // UTC+9 (북/남)
            WorldClockPreset("도쿄", "Asia/Tokyo"),
            WorldClockPreset("오사카", "Asia/Tokyo"),
            WorldClockPreset("나고야", "Asia/Tokyo"),
            WorldClockPreset("삿포로", "Asia/Tokyo"),
            WorldClockPreset("후쿠오카", "Asia/Tokyo"),
            WorldClockPreset("오키나와", "Asia/Tokyo"),
            WorldClockPreset("딜리", "Asia/Dili"),
            WorldClockPreset("자야푸라", "Asia/Jayapura"),
            // UTC+9:30 (남)
            WorldClockPreset("다윈", "Australia/Darwin"),
            WorldClockPreset("애들레이드", "Australia/Adelaide"),
            // UTC+10 (북/남)
            WorldClockPreset("블라디보스토크", "Asia/Vladivostok"),
            WorldClockPreset("하바롭스크", "Asia/Vladivostok"),
            WorldClockPreset("괌", "Pacific/Guam"),
            WorldClockPreset("사이판", "Pacific/Saipan"),
            WorldClockPreset("시드니", "Australia/Sydney"),
            WorldClockPreset("멜버른", "Australia/Melbourne"),
            WorldClockPreset("캔버라", "Australia/Sydney"),
            WorldClockPreset("브리즈번", "Australia/Brisbane"),
            WorldClockPreset("골드코스트", "Australia/Brisbane"),
            WorldClockPreset("포트모르즈비", "Pacific/Port_Moresby"),
            // UTC+11 (북/남)
            WorldClockPreset("마가단", "Asia/Magadan"),
            WorldClockPreset("누메아", "Pacific/Noumea"),
            // UTC+12 (북/남)
            WorldClockPreset("페트로파블롭스크", "Asia/Kamchatka"),
            WorldClockPreset("오클랜드", "Pacific/Auckland"),
            WorldClockPreset("웰링턴", "Pacific/Auckland"),
            WorldClockPreset("크라이스트처치", "Pacific/Auckland"),
            WorldClockPreset("수바", "Pacific/Fiji"),
            // UTC+13 (남)
            WorldClockPreset("아피아", "Pacific/Apia"),
            WorldClockPreset("누쿠알로파", "Pacific/Tongatapu")
        )

        val WORLD_CLOCKS: List<WorldClockPreset> by lazy {
            val nowMs = System.currentTimeMillis()
            listOf(WorldClockPreset("표시 안 함", "")) +
                RAW_WORLD_CLOCKS.sortedBy { TimeZone.getTimeZone(it.timeZoneId).getOffset(nowMs) }
        }

        fun formatTimeDiff(tzId: String, nowMs: Long = System.currentTimeMillis()): String {
            val localOffsetMs = TimeZone.getDefault().getOffset(nowMs)
            val diffHours = (TimeZone.getTimeZone(tzId).getOffset(nowMs) - localOffsetMs) / 3600_000.0
            return when {
                abs(diffHours) < 0.01 -> "0h"
                diffHours % 1.0 == 0.0 -> String.format(Locale.US, "%+.0fh", diffHours)
                else -> String.format(Locale.US, "%+.1fh", diffHours)
            }
        }

        fun displayWorldClockCity(cityNameKo: String, tzId: String, language: String): String {
            if (tzId.isBlank()) return if (language == "en") "None" else "표시 안 함"
            return if (language == "en") {
                tzId.substringAfterLast('/').replace('_', ' ')
            } else {
                cityNameKo
            }
        }

        fun formatWorldClockLabel(
            preset: WorldClockPreset,
            nowMs: Long = System.currentTimeMillis(),
            language: String = "ko"
        ): String {
            val name = displayWorldClockCity(preset.cityName, preset.timeZoneId, language)
            if (preset.timeZoneId.isBlank()) return name
            return "$name (${formatTimeDiff(preset.timeZoneId, nowMs)})"
        }
    }
}
