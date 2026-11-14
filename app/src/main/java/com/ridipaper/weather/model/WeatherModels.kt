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
    val language: String = "ko"
)
