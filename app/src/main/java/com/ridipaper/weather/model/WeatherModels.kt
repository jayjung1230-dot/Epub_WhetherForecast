package com.ridipaper.weather.model

data class HourlyItem(
    val timeLabel: String,
    val weatherCode: Int,
    val temp: Int,
    val precipProb: Int,
    val isCurrent: Boolean = false,
    val isTomorrow: Boolean = false
)

data class DailyItem(
    val dayLabel: String,
    val dateLabel: String,
    val weatherCode: Int,
    val precipProb: Int,
    val tempMin: Int,
    val tempMax: Int
)

data class WeatherData(
    val locationName: String,
    val isAutoLocation: Boolean,
    val countryCode: String = "KR",
    val latitude: Double,
    val longitude: Double,
    val updatedAtMillis: Long,
    val sunrise: String,
    val sunset: String,
    val currentTemp: Double,
    val apparentTemp: Double,
    val yesterdayDiff: Double?,
    val todayMax: Int,
    val todayMin: Int,
    val precipProb: Int,
    val weatherCode: Int,
    val humidity: Int,
    val windSpeed: Double,
    val windDirectionDeg: Int,
    val pm10: Double?,
    val pm25: Double?,
    val uvIndex: Double?,
    val hourly: List<HourlyItem>,
    val daily: List<DailyItem>
)

data class CityPreset(
    val name: String,
    val lat: Double,
    val lon: Double,
    val countryCode: String = "KR"
)

data class WorldClockPreset(
    val cityName: String,
    val timeZoneId: String
)

data class QuoteItem(
    val textKo: String,
    val textEn: String,
    val author: String,
    val tag: String,
    val authorEn: String = "",
    val categoryKo: String = "고전 명언",
    val categoryEn: String = "Classic Quote",
    val backgroundKo: String = "",
    val backgroundEn: String = "",
    val interpretationKo: String = "",
    val interpretationEn: String = ""
)

data class NewsItem(
    val category: String,
    val title: String,
    val source: String,
    val timeAgo: String
)

enum class PageType {
    WEATHER, QUOTES, NEWS, BOOK, MOVIE, ECONOMY
}

data class BookItem(
    val titleKo: String,
    val titleEn: String,
    val authorKo: String,
    val authorEn: String,
    val publisherYear: String,
    val genreKo: String,
    val genreEn: String,
    val rating: String,
    val pagesTimeKo: String,
    val pagesTimeEn: String,
    val summaryKo: String,
    val summaryEn: String,
    val quoteKo: String,
    val quoteEn: String,
    val keywordsKo: String,
    val keywordsEn: String,
    val coverUrl: String = ""
)

data class MovieItem(
    val titleKo: String,
    val titleEn: String,
    val originalTitleYear: String,
    val directorKo: String,
    val directorEn: String,
    val genreKo: String,
    val genreEn: String,
    val rating: String,
    val runtimeRatingKo: String,
    val runtimeRatingEn: String,
    val synopsisKo: String,
    val synopsisEn: String,
    val highlightKo: String,
    val highlightEn: String,
    val castOttKo: String,
    val castOttEn: String,
    val posterTitleTop: String,
    val posterTitleBottom: String,
    val posterUrl: String = ""
)

data class EconomyIndicator(
    val nameKo: String,
    val nameEn: String,
    val valueStr: String,
    val diffStr: String,
    val isUp: Boolean,
    val history: List<Float>
)

data class DashboardBundle(
    val weather: WeatherData,
    val mainQuote: QuoteItem,
    val subQuote: QuoteItem,
    val news: List<NewsItem>,
    val updatedAtMillis: Long
)

data class DashboardSettings(
    val keepScreenHours: Int = 1,
    val refreshIntervalMinutes: Int = 30,
    val pageAutoRotateMinutes: Int = 0,
    val brightnessPercent: Int = 40,
    val autoLocation: Boolean = true,
    val manualCityName: String = "서울특별시",
    val manualLat: Double = 37.5665,
    val manualLon: Double = 126.9780,
    val manualCountryCode: String = "KR",
    val worldClock1: String = "America/New_York",
    val worldClock2: String = "Europe/London",
    val language: String = "ko",
    val showQuotesPage: Boolean = true,
    val showNewsPage: Boolean = true,
    val showBookPage: Boolean = true,
    val showMoviePage: Boolean = true,
    val showEconomyPage: Boolean = true,
    val useDashboardAsSleepScreen: Boolean = false
)
