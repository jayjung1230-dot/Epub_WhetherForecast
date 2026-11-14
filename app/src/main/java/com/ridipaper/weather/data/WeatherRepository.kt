package com.ridipaper.weather.data

import com.ridipaper.weather.model.CityPreset
import com.ridipaper.weather.model.DailyItem
import com.ridipaper.weather.model.DashboardSettings
import com.ridipaper.weather.model.HourlyItem
import com.ridipaper.weather.model.WeatherData
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.Socket
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlin.math.roundToInt

object WeatherRepository {

    private data class GeoResult(
        val name: String,
        val lat: Double,
        val lon: Double,
        val countryCode: String = "KR"
    )

    fun fetchWeather(settings: DashboardSettings): WeatherData {
        var locName = settings.manualCityName
        var lat = settings.manualLat
        var lon = settings.manualLon
        var countryCode = settings.manualCountryCode.ifBlank { "KR" }
        var isAuto = false
        val lang = settings.language

        if (settings.autoLocation) {
            val geo = tryFetchIpLocation(lang)
            if (geo != null) {
                locName = geo.name
                lat = geo.lat
                lon = geo.lon
                countryCode = geo.countryCode
                isAuto = true
            }
        }

        return try {
            fetchFromMetNo(locName, isAuto, countryCode, lat, lon, lang)
        } catch (_: Exception) {
            fetchFromWttr(locName, isAuto, countryCode, lat, lon, lang)
        }
    }

    private data class AirQualityResult(val pm10: Double?, val pm25: Double?, val uvIndex: Double?)

    private fun fetchAirQuality(lat: Double, lon: Double): AirQualityResult {
        val aqUrl = "http://air-quality-api.open-meteo.com/v1/air-quality" +
            "?latitude=$lat&longitude=$lon&current=pm10,pm2_5,uv_index"
        var pm10: Double? = null
        var pm25: Double? = null
        var uv: Double? = null
        try {
            val aqJson = JSONObject(httpGet(aqUrl))
            val aqCurrent = aqJson.optJSONObject("current")
            if (aqCurrent != null) {
                if (!aqCurrent.isNull("pm10")) pm10 = aqCurrent.optDouble("pm10")
                if (!aqCurrent.isNull("pm2_5")) pm25 = aqCurrent.optDouble("pm2_5")
                if (!aqCurrent.isNull("uv_index")) uv = aqCurrent.optDouble("uv_index")
            }
        } catch (_: Exception) {
        }
        return AirQualityResult(pm10, pm25, uv)
    }

    private fun fetchFromOpenMeteo(
        locName: String,
        isAuto: Boolean,
        lat: Double,
        lon: Double,
        countryCode: String = "KR"
    ): WeatherData {
        val forecastUrl = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,wind_direction_10m" +
            "&hourly=temperature_2m,weather_code,precipitation_probability,uv_index" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset" +
            "&wind_speed_unit=ms&past_days=1&timezone=Asia%2FSeoul"

        val forecastJson = JSONObject(httpGet(forecastUrl))

        val aqUrl = "https://air-quality-api.open-meteo.com/v1/air-quality" +
            "?latitude=$lat&longitude=$lon&current=pm10,pm2_5"
        var pm10: Double? = null
        var pm25: Double? = null
        try {
            val aqJson = JSONObject(httpGet(aqUrl))
            val aqCurrent = aqJson.optJSONObject("current")
            if (aqCurrent != null) {
                if (!aqCurrent.isNull("pm10")) pm10 = aqCurrent.optDouble("pm10")
                if (!aqCurrent.isNull("pm2_5")) pm25 = aqCurrent.optDouble("pm2_5")
            }
        } catch (_: Exception) {
        }

        val current = forecastJson.getJSONObject("current")
        val currentTemp = current.getDouble("temperature_2m")
        val humidity = current.getInt("relative_humidity_2m")
        val apparentTemp = current.getDouble("apparent_temperature")
        val weatherCode = current.getInt("weather_code")
        val windSpeed = current.getDouble("wind_speed_10m")
        val windDirDeg = current.getInt("wind_direction_10m")
        val currentTimeStr = current.getString("time") // e.g. 2026-11-14T07:15

        val hourlyObj = forecastJson.getJSONObject("hourly")
        val hTimes = hourlyObj.getJSONArray("time")
        val hTemps = hourlyObj.getJSONArray("temperature_2m")
        val hCodes = hourlyObj.getJSONArray("weather_code")
        val hPops = hourlyObj.getJSONArray("precipitation_probability")
        val hUvs = hourlyObj.getJSONArray("uv_index")

        // Find current hour index (e.g. 2026-11-14T07:00)
        val currentHourPrefix = currentTimeStr.substring(0, 13) + ":00"
        var currentIdx = -1
        for (i in 0 until hTimes.length()) {
            if (hTimes.getString(i) == currentHourPrefix) {
                currentIdx = i
                break
            }
        }
        if (currentIdx < 0) currentIdx = 24

        // Yesterday same hour comparison (24 hours earlier)
        val yesterdayDiff: Double? = if (currentIdx >= 24) {
            currentTemp - hTemps.getDouble(currentIdx - 24)
        } else null

        val uvIndex: Double? = if (currentIdx in 0 until hUvs.length() && !hUvs.isNull(currentIdx)) {
            hUvs.getDouble(currentIdx)
        } else null

        val currentPrecipProb = if (currentIdx in 0 until hPops.length()) hPops.optInt(currentIdx, 0) else 0

        // Next 8 hourly slots at 3-hour intervals
        val hourlyList = ArrayList<HourlyItem>(8)
        var stepIdx = currentIdx + 2
        while (hourlyList.size < 8 && stepIdx < hTimes.length()) {
            val tStr = hTimes.getString(stepIdx) // yyyy-MM-ddTHH:mm
            val hourLabel = tStr.substring(11, 13) + "시"
            hourlyList.add(
                HourlyItem(
                    timeLabel = hourLabel,
                    weatherCode = hCodes.optInt(stepIdx, 0),
                    temp = hTemps.optDouble(stepIdx, 0.0).roundToInt(),
                    precipProb = hPops.optInt(stepIdx, 0)
                )
            )
            stepIdx += 3
        }

        // Daily: past_days=1 means index 0 is yesterday, index 1 is today, 2..7 are next 6 days
        val dailyObj = forecastJson.getJSONObject("daily")
        val dTimes = dailyObj.getJSONArray("time")
        val dCodes = dailyObj.getJSONArray("weather_code")
        val dMax = dailyObj.getJSONArray("temperature_2m_max")
        val dMin = dailyObj.getJSONArray("temperature_2m_min")
        val dPops = dailyObj.getJSONArray("precipitation_probability_max")
        val dSunrise = dailyObj.getJSONArray("sunrise")
        val dSunset = dailyObj.getJSONArray("sunset")

        val todayIdx = if (dTimes.length() > 1) 1 else 0
        val todayMax = dMax.optDouble(todayIdx, currentTemp).roundToInt()
        val todayMin = dMin.optDouble(todayIdx, currentTemp).roundToInt()
        val sunrise = extractTimeHHmm(dSunrise.optString(todayIdx, ""))
        val sunset = extractTimeHHmm(dSunset.optString(todayIdx, ""))

        val dailyList = ArrayList<DailyItem>(7)
        val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
        val dayNames = arrayOf("일", "월", "화", "수", "목", "금", "토")
        val cal = Calendar.getInstance()
        for (i in todayIdx until minOf(dTimes.length(), todayIdx + 7)) {
            val dateStr = dTimes.getString(i)
            val parsed: Date? = try { sdfIn.parse(dateStr) } catch (_: Exception) { null }
            val dayLabel = if (i == todayIdx) {
                "오늘"
            } else if (parsed != null) {
                cal.time = parsed
                dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
            } else {
                ""
            }
            val dateLabel = dateStr.substring(5, 7) + "." + dateStr.substring(8, 10)
            dailyList.add(
                DailyItem(
                    dayLabel = dayLabel,
                    dateLabel = dateLabel,
                    weatherCode = dCodes.optInt(i, 0),
                    precipProb = dPops.optInt(i, 0),
                    tempMin = dMin.optDouble(i, 0.0).roundToInt(),
                    tempMax = dMax.optDouble(i, 0.0).roundToInt()
                )
            )
        }

        return WeatherData(
            locationName = locName,
            isAutoLocation = isAuto,
            countryCode = countryCode,
            latitude = lat,
            longitude = lon,
            updatedAtMillis = System.currentTimeMillis(),
            sunrise = sunrise,
            sunset = sunset,
            currentTemp = currentTemp,
            apparentTemp = apparentTemp,
            yesterdayDiff = yesterdayDiff,
            todayMax = todayMax,
            todayMin = todayMin,
            precipProb = currentPrecipProb,
            weatherCode = weatherCode,
            humidity = humidity,
            windSpeed = windSpeed,
            windDirectionDeg = windDirDeg,
            pm10 = pm10,
            pm25 = pm25,
            uvIndex = uvIndex,
            hourly = hourlyList,
            daily = dailyList
        )
    }

    private fun fetchFromMetNo(
        locName: String,
        isAuto: Boolean,
        countryCode: String,
        lat: Double,
        lon: Double,
        language: String
    ): WeatherData {
        val isEn = language == "en"
        val url = String.format(
            Locale.US,
            "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=%.4f&lon=%.4f",
            lat, lon
        )
        val root = JSONObject(httpGet(url))
        val ts = root.getJSONObject("properties").getJSONArray("timeseries")
        val aq = fetchAirQuality(lat, lon)

        val utcFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val kstTz = java.util.TimeZone.getDefault()
        val kstDateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = kstTz }
        val kstHourFmt = SimpleDateFormat("HH", Locale.US).apply { timeZone = kstTz }
        val dayNames = if (isEn) {
            arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        } else {
            arrayOf("일", "월", "화", "수", "목", "금", "토")
        }
        val cal = Calendar.getInstance(kstTz)

        val first = ts.getJSONObject(0).getJSONObject("data")
        val inst = first.getJSONObject("instant").getJSONObject("details")
        val currentTemp = inst.getDouble("air_temperature")
        val humidity = inst.getDouble("relative_humidity").roundToInt()
        val windSpeed = inst.getDouble("wind_speed")
        val windDirDeg = inst.optDouble("wind_from_direction", 0.0).roundToInt()
        val cloudFrac = inst.optDouble("cloud_area_fraction", 0.0)
        val eVapor = (humidity / 100.0) * 6.105 * Math.exp(17.27 * currentTemp / (237.7 + currentTemp))
        val apparentTemp = currentTemp + 0.33 * eVapor - 0.70 * windSpeed - 4.0

        val n1 = first.optJSONObject("next_1_hours") ?: first.optJSONObject("next_6_hours")
        val sym0 = n1?.optJSONObject("summary")?.optString("symbol_code", "") ?: ""
        val weatherCode = metSymbolToWmoCode(sym0)
        val mm0 = n1?.optJSONObject("details")?.optDouble("precipitation_amount", 0.0) ?: 0.0
        val currentPrecipProb = precipMmToProb(mm0, cloudFrac)

        // Group timeseries by KST date and pick 3-hour hourly slots
        val hourlyList = ArrayList<HourlyItem>(8)
        var lastSlotMs = 0L
        val dailyTemps = LinkedHashMap<String, MutableList<Double>>()
        val dailyPops = LinkedHashMap<String, Int>()
        val dailySymbols = LinkedHashMap<String, MutableList<Pair<Int, Int>>>()

        val nowMs = System.currentTimeMillis()
        for (i in 0 until ts.length()) {
            val item = ts.getJSONObject(i)
            val dateObj = utcFmt.parse(item.getString("time")) ?: continue
            val ms = dateObj.time
            val dObj = item.getJSONObject("data")
            val det = dObj.getJSONObject("instant").getJSONObject("details")
            val temp = det.getDouble("air_temperature")
            val cloud = det.optDouble("cloud_area_fraction", 0.0)
            val nextBlock = dObj.optJSONObject("next_1_hours")
                ?: dObj.optJSONObject("next_6_hours")
                ?: dObj.optJSONObject("next_12_hours")
            val sym = nextBlock?.optJSONObject("summary")?.optString("symbol_code", "") ?: ""
            val mm = nextBlock?.optJSONObject("details")?.optDouble("precipitation_amount", 0.0) ?: 0.0
            val pop = precipMmToProb(mm, cloud)
            val wCode = metSymbolToWmoCode(sym)

            if (i > 0 && ms >= nowMs + 2 * 3600_000L && hourlyList.size < 8) {
                if (lastSlotMs == 0L || ms - lastSlotMs >= 3 * 3600_000L - 60_000L) {
                    hourlyList.add(
                        HourlyItem(
                            timeLabel = kstHourFmt.format(dateObj) + if (isEn) ":00" else "시",
                            weatherCode = wCode,
                            temp = temp.roundToInt(),
                            precipProb = pop
                        )
                    )
                    lastSlotMs = ms
                }
            }

            val dayKey = kstDateFmt.format(dateObj)
            dailyTemps.getOrPut(dayKey) { ArrayList() }.add(temp)
            dailyPops[dayKey] = maxOf(dailyPops[dayKey] ?: 0, pop)
            val kstHour = kstHourFmt.format(dateObj).toIntOrNull() ?: 12
            dailySymbols.getOrPut(dayKey) { ArrayList() }.add(kstHour to wCode)
        }

        val dailyList = ArrayList<DailyItem>(7)
        var todayMax = currentTemp.roundToInt()
        var todayMin = currentTemp.roundToInt()
        var dayIdx = 0
        for ((dayKey, temps) in dailyTemps) {
            if (dailyList.size >= 7) break
            val dMin = temps.minOrNull()?.roundToInt() ?: currentTemp.roundToInt()
            val dMax = temps.maxOrNull()?.roundToInt() ?: currentTemp.roundToInt()
            if (dayIdx == 0) {
                todayMax = dMax
                todayMin = dMin
            }
            val parsed = kstDateFmt.parse(dayKey)
            val dayLabel = if (dayIdx == 0) {
                if (isEn) "Today" else "오늘"
            } else if (parsed != null) {
                cal.time = parsed
                dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
            } else ""
            val dateLabel = dayKey.substring(5, 7) + "." + dayKey.substring(8, 10)
            val syms = dailySymbols[dayKey] ?: emptyList()
            val midCode = syms.minByOrNull { kotlin.math.abs(it.first - 13) }?.second ?: weatherCode
            dailyList.add(
                DailyItem(
                    dayLabel = dayLabel,
                    dateLabel = dateLabel,
                    weatherCode = midCode,
                    precipProb = dailyPops[dayKey] ?: 0,
                    tempMin = dMin,
                    tempMax = dMax
                )
            )
            dayIdx++
        }

        val (sunrise, sunset) = computeSunriseSunset(lat, lon, Date())

        return WeatherData(
            locationName = locName,
            isAutoLocation = isAuto,
            countryCode = countryCode,
            latitude = lat,
            longitude = lon,
            updatedAtMillis = System.currentTimeMillis(),
            sunrise = sunrise,
            sunset = sunset,
            currentTemp = currentTemp,
            apparentTemp = apparentTemp,
            yesterdayDiff = null,
            todayMax = todayMax,
            todayMin = todayMin,
            precipProb = currentPrecipProb,
            weatherCode = weatherCode,
            humidity = humidity,
            windSpeed = windSpeed,
            windDirectionDeg = windDirDeg,
            pm10 = aq.pm10,
            pm25 = aq.pm25,
            uvIndex = aq.uvIndex,
            hourly = hourlyList,
            daily = dailyList
        )
    }

    private fun precipMmToProb(mm: Double, cloudFrac: Double): Int = when {
        mm >= 2.0 -> 90
        mm >= 0.5 -> 75
        mm >= 0.1 -> 55
        cloudFrac >= 85 -> 25
        cloudFrac >= 50 -> 15
        else -> 5
    }

    private fun metSymbolToWmoCode(sym: String): Int {
        val base = sym.substringBefore("_")
        return when {
            base == "clearsky" -> 0
            base == "fair" -> 1
            base == "partlycloudy" -> 2
            base == "cloudy" -> 3
            base == "fog" -> 45
            base.contains("thunder") -> 95
            base.contains("snow") || base.contains("sleet") -> 71
            base.contains("heavyrain") -> 65
            base.contains("rain") -> 61
            else -> 2
        }
    }

    private fun computeSunriseSunset(lat: Double, lon: Double, date: Date): Pair<String, String> {
        return try {
            val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Seoul")).apply { time = date }
            val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
            val zenith = 90.833
            fun calc(isSunrise: Boolean): String {
                val lngHour = lon / 15.0
                val t = dayOfYear + ((if (isSunrise) 6.0 else 18.0) - lngHour) / 24.0
                val m = 0.9856 * t - 3.289
                var l = m + 1.916 * Math.sin(Math.toRadians(m)) + 0.020 * Math.sin(Math.toRadians(2 * m)) + 282.634
                l = ((l % 360.0) + 360.0) % 360.0
                var ra = Math.toDegrees(Math.atan(0.91764 * Math.tan(Math.toRadians(l))))
                ra = ((ra % 360.0) + 360.0) % 360.0
                ra += (Math.floor(l / 90.0) * 90.0) - (Math.floor(ra / 90.0) * 90.0)
                ra /= 15.0
                val sinDec = 0.39782 * Math.sin(Math.toRadians(l))
                val cosDec = Math.cos(Math.asin(sinDec))
                val cosH = (Math.cos(Math.toRadians(zenith)) - sinDec * Math.sin(Math.toRadians(lat))) /
                    (cosDec * Math.cos(Math.toRadians(lat)))
                if (cosH > 1.0 || cosH < -1.0) return "--:--"
                val hDeg = Math.toDegrees(Math.acos(cosH))
                val h = (if (isSunrise) 360.0 - hDeg else hDeg) / 15.0
                val bigT = h + ra - 0.06571 * t - 6.622
                var ut = bigT - lngHour
                ut = ((ut % 24.0) + 24.0) % 24.0
                val localT = ((ut + 9.0) % 24.0 + 24.0) % 24.0
                val hh = localT.toInt()
                val mm = ((localT - hh) * 60.0).roundToInt() % 60
                return String.format(Locale.KOREA, "%02d:%02d", hh, mm)
            }
            calc(true) to calc(false)
        } catch (_: Exception) {
            "--:--" to "--:--"
        }
    }

    private fun fetchFromWttr(
        locName: String,
        isAuto: Boolean,
        countryCode: String,
        lat: Double,
        lon: Double,
        language: String
    ): WeatherData {
        val isEn = language == "en"
        val json = JSONObject(httpGet("http://wttr.in/$lat,$lon?format=j1"))
        val aq = fetchAirQuality(lat, lon)
        val cc = json.getJSONArray("current_condition").getJSONObject(0)
        val currentTemp = cc.getString("temp_C").toDouble()
        val apparentTemp = cc.getString("FeelsLikeC").toDouble()
        val humidity = cc.getString("humidity").toInt()
        val uvIndex = aq.uvIndex ?: cc.optString("uvIndex", "").toDoubleOrNull()
        val weatherCode = wwoToWmoCode(cc.optString("weatherCode", "113").toIntOrNull() ?: 113)
        val windSpeed = (cc.optString("windspeedKmph", "0").toDoubleOrNull() ?: 0.0) / 3.6
        val windDirDeg = cc.optString("winddirDegree", "0").toIntOrNull() ?: 0

        val days = json.getJSONArray("weather")
        val todayObj = days.getJSONObject(0)
        val todayMax = todayObj.getString("maxtempC").toInt()
        val todayMin = todayObj.getString("mintempC").toInt()
        val astro = todayObj.optJSONArray("astronomy")?.optJSONObject(0)
        val sunrise = parseAmPmTime(astro?.optString("sunrise", "") ?: "")
        val sunset = parseAmPmTime(astro?.optString("sunset", "") ?: "")

        val nowHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val hourlyList = ArrayList<HourlyItem>(8)
        var currentPrecipProb = 0
        var foundCurrent = false
        for (d in 0 until days.length()) {
            val hArr = days.getJSONObject(d).getJSONArray("hourly")
            for (h in 0 until hArr.length()) {
                val hObj = hArr.getJSONObject(h)
                val slotHour = (hObj.optString("time", "0").toIntOrNull() ?: 0) / 100
                if (d == 0 && slotHour < nowHour) continue
                if (!foundCurrent) {
                    currentPrecipProb = hObj.optString("chanceofrain", "0").toIntOrNull() ?: 0
                    foundCurrent = true
                    continue
                }
                if (hourlyList.size < 8) {
                    hourlyList.add(
                        HourlyItem(
                            timeLabel = String.format(Locale.US, if (isEn) "%02d:00" else "%02d시", slotHour),
                            weatherCode = wwoToWmoCode(hObj.optString("weatherCode", "113").toIntOrNull() ?: 113),
                            temp = hObj.optString("tempC", "0").toIntOrNull() ?: 0,
                            precipProb = hObj.optString("chanceofrain", "0").toIntOrNull() ?: 0
                        )
                    )
                }
            }
        }

        val dailyList = ArrayList<DailyItem>(7)
        val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayNames = if (isEn) {
            arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        } else {
            arrayOf("일", "월", "화", "수", "목", "금", "토")
        }
        val cal = Calendar.getInstance()
        for (d in 0 until days.length()) {
            val dObj = days.getJSONObject(d)
            val dateStr = dObj.getString("date")
            val parsed: Date? = try { sdfIn.parse(dateStr) } catch (_: Exception) { null }
            val dayLabel = if (d == 0) {
                if (isEn) "Today" else "오늘"
            } else if (parsed != null) {
                cal.time = parsed
                dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
            } else ""
            val dateLabel = dateStr.substring(5, 7) + "." + dateStr.substring(8, 10)
            val hArr = dObj.getJSONArray("hourly")
            var maxPop = 0
            var midCode = 0
            for (h in 0 until hArr.length()) {
                val hObj = hArr.getJSONObject(h)
                val pop = hObj.optString("chanceofrain", "0").toIntOrNull() ?: 0
                if (pop > maxPop) maxPop = pop
                if (h == hArr.length() / 2) {
                    midCode = wwoToWmoCode(hObj.optString("weatherCode", "113").toIntOrNull() ?: 113)
                }
            }
            dailyList.add(
                DailyItem(
                    dayLabel = dayLabel,
                    dateLabel = dateLabel,
                    weatherCode = midCode,
                    precipProb = maxPop,
                    tempMin = dObj.getString("mintempC").toInt(),
                    tempMax = dObj.getString("maxtempC").toInt()
                )
            )
        }

        return WeatherData(
            locationName = locName,
            isAutoLocation = isAuto,
            countryCode = countryCode,
            latitude = lat,
            longitude = lon,
            updatedAtMillis = System.currentTimeMillis(),
            sunrise = sunrise,
            sunset = sunset,
            currentTemp = currentTemp,
            apparentTemp = apparentTemp,
            yesterdayDiff = null,
            todayMax = todayMax,
            todayMin = todayMin,
            precipProb = currentPrecipProb,
            weatherCode = weatherCode,
            humidity = humidity,
            windSpeed = windSpeed,
            windDirectionDeg = windDirDeg,
            pm10 = aq.pm10,
            pm25 = aq.pm25,
            uvIndex = uvIndex,
            hourly = hourlyList,
            daily = dailyList
        )
    }

    private fun wwoToWmoCode(wwo: Int): Int = when (wwo) {
        113 -> 0
        116 -> 2
        119, 122 -> 3
        143, 248, 260 -> 45
        176, 263, 266, 293, 296, 353 -> 61
        299, 302, 305, 308, 356, 359 -> 65
        179, 182, 185, 227, 230, 323, 326, 329, 332, 335, 338, 368, 371 -> 71
        200, 386, 389, 392, 395 -> 95
        else -> 2
    }

    private fun parseAmPmTime(str: String): String {
        return try {
            val inFmt = SimpleDateFormat("hh:mm a", Locale.US)
            val outFmt = SimpleDateFormat("HH:mm", Locale.KOREA)
            val d = inFmt.parse(str.trim())
            if (d != null) outFmt.format(d) else "--:--"
        } catch (_: Exception) {
            "--:--"
        }
    }

    private fun tryFetchIpLocation(language: String): GeoResult? {
        val ipLang = if (language == "en") "en" else "ko"
        return try {
            val json = JSONObject(httpGet("http://ip-api.com/json/?lang=$ipLang"))
            if (json.optString("status") == "success") {
                val region = json.optString("regionName", "").trim()
                val city = json.optString("city", "").trim()
                val cc = json.optString("countryCode", "KR").trim().uppercase(Locale.US).ifEmpty { "KR" }
                val display = when {
                    region.isNotEmpty() && city.isNotEmpty() && region != city -> "$region $city"
                    city.isNotEmpty() -> city
                    region.isNotEmpty() -> region
                    else -> if (language == "en") "Current Location" else "현재 위치"
                }
                GeoResult(display, json.getDouble("lat"), json.getDouble("lon"), cc)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun searchKoreanLocations(query: String, language: String = "ko"): List<CityPreset> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val encoded = URLEncoder.encode(trimmed, "UTF-8")
        val hasHangul = trimmed.any { it in '\uAC00'..'\uD7A3' || it in '\u3131'..'\u318E' }
        val acceptLang = if (language == "en") "en" else "ko"
        val countryParam = if (hasHangul) "&countrycodes=kr" else ""
        val url = "https://nominatim.openstreetmap.org/search" +
            "?q=$encoded&format=json$countryParam&accept-language=$acceptLang&addressdetails=1&limit=12"
        val arr = JSONArray(httpGet(url))
        val results = ArrayList<CityPreset>()
        val seen = HashSet<String>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val lat = obj.optString("lat", "").toDoubleOrNull() ?: continue
            val lon = obj.optString("lon", "").toDoubleOrNull() ?: continue
            val addr = obj.optJSONObject("address")
            val cc = addr?.optString("country_code", "")?.trim()?.uppercase(Locale.US)
                ?.takeIf { it.length == 2 } ?: if (hasHangul) "KR" else "US"
            val label = if (cc == "KR" && !hasHangul && language == "en") {
                obj.optString("display_name", trimmed).split(",").take(2).joinToString(", ").trim()
            } else if (cc == "KR") {
                formatKoreanAddress(addr, obj.optString("display_name", trimmed))
            } else {
                val cityPart = addr?.optString("city", "")
                    ?.ifEmpty { addr.optString("town", "") }
                    ?.ifEmpty { addr.optString("state", "") }
                    .orEmpty()
                val countryPart = addr?.optString("country", "").orEmpty()
                listOf(cityPart, countryPart).filter { it.isNotBlank() }.distinct().joinToString(", ")
                    .ifEmpty { obj.optString("display_name", trimmed).split(",").take(2).joinToString(", ").trim() }
            }
            if (label.isNotEmpty() && seen.add(label)) {
                results.add(CityPreset(label, lat, lon, cc))
            }
        }
        return results
    }

    private fun formatKoreanAddress(addr: JSONObject?, fallbackDisplay: String): String {
        if (addr == null) return fallbackDisplay.split(",").take(3).reversed().joinToString(" ").trim()
        val sido = addr.optString("province", "").ifEmpty { addr.optString("city", "") }
        val sigungu = when {
            addr.optString("city", "").isNotEmpty() && addr.optString("city", "") != sido -> addr.optString("city")
            addr.optString("county", "").isNotEmpty() -> addr.optString("county")
            addr.optString("borough", "").isNotEmpty() -> addr.optString("borough")
            else -> ""
        }
        val gu = if (addr.optString("borough", "").isNotEmpty() && addr.optString("borough", "") != sigungu) {
            addr.optString("borough")
        } else ""
        val dong = addr.optString("quarter", "")
            .ifEmpty { addr.optString("neighbourhood", "") }
            .ifEmpty { addr.optString("suburb", "") }
            .ifEmpty { addr.optString("town", "") }
            .ifEmpty { addr.optString("village", "") }
        val parts = listOf(sido, sigungu, gu, dong).filter { it.isNotBlank() }.distinct()
        return if (parts.isNotEmpty()) parts.joinToString(" ") else fallbackDisplay.split(",").firstOrNull()?.trim().orEmpty()
    }

    private fun extractTimeHHmm(iso: String): String {
        return if (iso.length >= 16) iso.substring(11, 16) else "--:--"
    }

    private val tlsSocketFactory: SSLSocketFactory by lazy {
        val ctx = SSLContext.getInstance("TLS")
        ctx.init(null, null, null)
        TLSSocketFactory(ctx.socketFactory)
    }

    internal fun httpGet(urlStr: String, redirectsLeft: Int = 3): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            if (this is HttpsURLConnection) {
                sslSocketFactory = tlsSocketFactory
            }
            connectTimeout = 5000
            readTimeout = 5000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", "PaperProWeather/1.0")
        }
        return try {
            val code = conn.responseCode
            if (code in listOf(301, 302, 303, 307, 308) && redirectsLeft > 0) {
                val loc = conn.getHeaderField("Location")
                if (!loc.isNullOrBlank()) {
                    conn.disconnect()
                    return httpGet(loc, redirectsLeft - 1)
                }
            }
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                ?: throw IOException("HTTP $code")
            val body = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            if (code !in 200..299) throw IOException("HTTP $code: $body")
            body
        } finally {
            conn.disconnect()
        }
    }

    private class TLSSocketFactory(private val delegate: SSLSocketFactory) : SSLSocketFactory() {
        private val protocols = arrayOf("TLSv1.2", "TLSv1.1", "TLSv1")

        override fun getDefaultCipherSuites(): Array<String> = delegate.defaultCipherSuites
        override fun getSupportedCipherSuites(): Array<String> = delegate.supportedCipherSuites

        override fun createSocket(s: Socket?, host: String?, port: Int, autoClose: Boolean): Socket =
            enableTls(delegate.createSocket(s, host, port, autoClose))

        override fun createSocket(host: String?, port: Int): Socket =
            enableTls(delegate.createSocket(host, port))

        override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket =
            enableTls(delegate.createSocket(host, port, localHost, localPort))

        override fun createSocket(host: InetAddress?, port: Int): Socket =
            enableTls(delegate.createSocket(host, port))

        override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket =
            enableTls(delegate.createSocket(address, port, localAddress, localPort))

        private fun enableTls(socket: Socket): Socket {
            if (socket is SSLSocket) {
                val supported = socket.supportedProtocols.toSet()
                val enabled = protocols.filter { it in supported }.toTypedArray()
                if (enabled.isNotEmpty()) {
                    socket.enabledProtocols = enabled
                }
            }
            return socket
        }
    }

    fun weatherLabel(code: Int, lang: String = "ko"): String = if (lang == "en") {
        when (code) {
            0 -> "Clear"
            1 -> "Mostly Clear"
            2 -> "Partly Cloudy"
            3 -> "Cloudy"
            45, 48 -> "Fog"
            51, 53, 55, 56, 57 -> "Drizzle"
            61, 63, 65, 66, 67, 80, 81, 82 -> "Rain"
            71, 73, 75, 77, 85, 86 -> "Snow"
            95, 96, 99 -> "Thunderstorm"
            else -> "Mostly Cloudy"
        }
    } else {
        when (code) {
            0 -> "맑음"
            1 -> "대체로 맑음"
            2 -> "구름 조금"
            3 -> "흐림"
            45, 48 -> "안개"
            51, 53, 55, 56, 57 -> "이슬비"
            61, 63, 65, 66, 67, 80, 81, 82 -> "비"
            71, 73, 75, 77, 85, 86 -> "눈"
            95, 96, 99 -> "뇌우"
            else -> "구름 많음"
        }
    }

    fun windDirLabel(deg: Int, lang: String = "ko"): String {
        val dirs = if (lang == "en") {
            arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        } else {
            arrayOf("북풍", "북동풍", "동풍", "남동풍", "남풍", "남서풍", "서풍", "북서풍")
        }
        val idx = (((deg % 360) + 360) % 360 + 22) / 45 % 8
        return dirs[idx]
    }

    fun pm10Grade(v: Double?, lang: String = "ko"): String = if (lang == "en") {
        when {
            v == null -> "-"
            v <= 30 -> "Good"
            v <= 80 -> "Moderate"
            v <= 150 -> "Poor"
            else -> "Very Poor"
        }
    } else {
        when {
            v == null -> "-"
            v <= 30 -> "좋음"
            v <= 80 -> "보통"
            v <= 150 -> "나쁨"
            else -> "매우나쁨"
        }
    }

    fun pm25Grade(v: Double?, lang: String = "ko"): String = if (lang == "en") {
        when {
            v == null -> "-"
            v <= 15 -> "Good"
            v <= 35 -> "Moderate"
            v <= 75 -> "Poor"
            else -> "Very Poor"
        }
    } else {
        when {
            v == null -> "-"
            v <= 15 -> "좋음"
            v <= 35 -> "보통"
            v <= 75 -> "나쁨"
            else -> "매우나쁨"
        }
    }

    fun uvGrade(v: Double?, lang: String = "ko"): String = if (lang == "en") {
        when {
            v == null -> "-"
            v < 3 -> "Low"
            v < 6 -> "Moderate"
            v < 8 -> "High"
            v < 11 -> "Very High"
            else -> "Extreme"
        }
    } else {
        when {
            v == null -> "-"
            v < 3 -> "낮음"
            v < 6 -> "보통"
            v < 8 -> "높음"
            v < 11 -> "매우높음"
            else -> "위험"
        }
    }

    fun humidityGrade(h: Int, lang: String = "ko"): String = if (lang == "en") {
        when {
            h < 35 -> "Dry"
            h <= 65 -> "Comfortable"
            else -> "Humid"
        }
    } else {
        when {
            h < 35 -> "건조"
            h <= 65 -> "쾌적"
            else -> "다소 습함"
        }
    }
}
