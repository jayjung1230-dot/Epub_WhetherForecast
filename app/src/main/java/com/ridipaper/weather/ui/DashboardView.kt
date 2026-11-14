package com.ridipaper.weather.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import com.ridipaper.weather.data.SettingsStore
import com.ridipaper.weather.data.WeatherRepository
import com.ridipaper.weather.model.DashboardSettings
import com.ridipaper.weather.model.NewsItem
import com.ridipaper.weather.model.QuoteItem
import com.ridipaper.weather.model.WeatherData
import com.ridipaper.weather.util.LunarCalendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

class DashboardView(context: Context) : View(context) {

    interface Listener {
        fun onRegionClick()
        fun onSettingsClick()
        fun onRefreshClick()
        fun onCloseClick()
        fun onPrevPageClick()
        fun onNextPageClick()
    }

    var listener: Listener? = null

    private var weather: WeatherData? = null
    private var mainQuote: QuoteItem? = null
    private var subQuote: QuoteItem? = null
    private var newsList: List<NewsItem> = emptyList()
    private var currentPage: Int = 0
    private var settings: DashboardSettings = DashboardSettings()
    private var wifiConnected: Boolean = false
    private var statusMessage: String? = null
    private var keepAliveRemainingMinutes: Int = 60
    private var drawOffsetX: Float = 0f
    private var drawOffsetY: Float = 0f
    private var drawContentW: Float = 1404f
    private var drawContentH: Float = 1872f

    private val btnRegionRect = RectF()
    private val btnSettingsRect = RectF()
    private val btnRefreshRect = RectF()
    private val btnCloseRect = RectF()
    private val btnPrevPageRect = RectF()
    private val btnNextPageRect = RectF()

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val timeFmt = SimpleDateFormat("HH:mm", Locale.US)
    private val dateFmtKo = SimpleDateFormat("yyyy년 M월 d일 (E)", Locale.KOREA)
    private val dateFmtEn = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US)

    fun updateState(
        weather: WeatherData?,
        mainQuote: QuoteItem?,
        subQuote: QuoteItem?,
        news: List<NewsItem>,
        currentPage: Int,
        settings: DashboardSettings,
        wifiConnected: Boolean,
        remainingMinutes: Int,
        statusMessage: String? = null
    ) {
        this.weather = weather
        this.mainQuote = mainQuote
        this.subQuote = subQuote
        this.newsList = news
        this.currentPage = currentPage
        this.settings = settings
        this.wifiConnected = wifiConnected
        this.keepAliveRemainingMinutes = remainingMinutes
        this.statusMessage = statusMessage
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val x = event.x - drawOffsetX
            val y = event.y - drawOffsetY
            when {
                btnCloseRect.contains(x, y) -> listener?.onCloseClick()
                btnRefreshRect.contains(x, y) -> listener?.onRefreshClick()
                btnSettingsRect.contains(x, y) -> listener?.onSettingsClick()
                btnRegionRect.contains(x, y) -> listener?.onRegionClick()
                btnPrevPageRect.contains(x, y) -> listener?.onPrevPageClick()
                btnNextPageRect.contains(x, y) -> listener?.onNextPageClick()
                y > drawContentH * 0.50f -> {
                    if (x < drawContentW * 0.35f) listener?.onPrevPageClick()
                    else listener?.onNextPageClick()
                }
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.WHITE)

        val screenW = width.toFloat()
        val screenH = height.toFloat()
        if (screenW <= 0f || screenH <= 0f) return

        val s = minOf(screenW / 1404f, screenH / 1872f)
        val w = 1404f * s
        val contentH = 1872f * s
        drawContentW = w
        drawContentH = contentH
        drawOffsetX = (screenW - w) / 2f
        drawOffsetY = (screenH - contentH) / 2f

        canvas.save()
        canvas.translate(drawOffsetX, drawOffsetY)

        val m = 48f * s
        val lang = settings.language
        val isEn = lang == "en"

        // --- 1. Top Bar ---
        val data = weather
        val locLabel = if (data != null) {
            val modeTag = if (data.isAutoLocation) {
                if (isEn) "(Auto)" else "(자동)"
            } else {
                if (isEn) "(Manual)" else "(수동)"
            }
            "${data.locationName} $modeTag"
        } else {
            if (settings.autoLocation) {
                if (isEn) "Detecting location (Auto)" else "위치 확인 중 (자동)"
            } else {
                val tag = if (isEn) "(Manual)" else "(수동)"
                "${settings.manualCityName} $tag"
            }
        }

        textPaint.color = Color.BLACK
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 40f * s
        canvas.drawText(locLabel, m, 82f * s, textPaint)

        val updatedStr = if (data != null) timeFmt.format(Date(data.updatedAtMillis)) else "--:--"
        val wifiStr = if (wifiConnected) {
            if (isEn) "Wi-Fi Connected" else "Wi-Fi 연결됨"
        } else {
            if (isEn) "Wi-Fi Off" else "Wi-Fi 미연결"
        }
        val subLine = statusMessage ?: if (isEn) {
            "Updated: $updatedStr  ·  $wifiStr"
        } else {
            "최근 갱신: $updatedStr  ·  $wifiStr"
        }
        textPaint.color = Color.rgb(85, 85, 85)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 24f * s
        canvas.drawText(subLine, m, 122f * s, textPaint)

        // Top right buttons: [지역] [설정] [새로고침] [닫기]
        val btnH = 64f * s
        val btnY = 44f * s
        var curX = w - m

        curX = drawButton(canvas, btnCloseRect, curX, btnY, 104f * s, btnH, if (isEn) "Close" else "닫기", true, s) - 12f * s
        curX = drawButton(canvas, btnRefreshRect, curX, btnY, 136f * s, btnH, if (isEn) "Refresh" else "새로고침", false, s) - 12f * s
        curX = drawButton(canvas, btnSettingsRect, curX, btnY, 112f * s, btnH, if (isEn) "Settings" else "설정", false, s) - 12f * s
        drawButton(canvas, btnRegionRect, curX, btnY, 104f * s, btnH, if (isEn) "Region" else "지역", false, s)

        strokePaint.color = Color.BLACK
        strokePaint.strokeWidth = 3f * s
        canvas.drawLine(m, 144f * s, w - m, 144f * s, strokePaint)

        // --- 2. Clock & Date Banner + Lunar Date + World Clocks (max 2) ---
        val now = Date()
        textPaint.color = Color.BLACK
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 132f * s
        canvas.drawText(timeFmt.format(now), m, 286f * s, textPaint)

        textPaint.textSize = 36f * s
        canvas.drawText((if (isEn) dateFmtEn else dateFmtKo).format(now), m + 392f * s, 206f * s, textPaint)

        val rawLunar = LunarCalendar.format(now)
        val lunarText = if (isEn) {
            rawLunar.replace("음력 ", "Lunar ").replace("월 ", "/").replace("일", "")
        } else {
            rawLunar
        }
        if (lunarText.isNotEmpty()) {
            textPaint.color = Color.rgb(55, 55, 55)
            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.textSize = 28f * s
            canvas.drawText(lunarText, m + 392f * s, 250f * s, textPaint)
        }

        val sunText = if (data != null) {
            if (isEn) "Sunrise ${data.sunrise}  ·  Sunset ${data.sunset}" else "일출 ${data.sunrise}  ·  일몰 ${data.sunset}"
        } else {
            if (isEn) "Sunrise --:--  ·  Sunset --:--" else "일출 --:--  ·  일몰 --:--"
        }
        textPaint.color = Color.rgb(75, 75, 75)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 26f * s
        canvas.drawText(sunText, m + 392f * s, 294f * s, textPaint)

        drawWorldClocks(canvas, now, w, m, s)

        if (data == null) {
            val box = RectF(m, 340f * s, w - m, 820f * s)
            drawCard(canvas, box, 24f * s, Color.WHITE, 4f * s)
            textPaint.color = Color.BLACK
            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.textSize = 38f * s
            val msg = if (wifiConnected) {
                if (isEn) "Loading weather data..." else "날씨 정보를 불러오는 중입니다..."
            } else {
                if (isEn) "Connect to Wi-Fi and tap [Refresh]." else "Wi-Fi 연결 후 [새로고침]을 눌러주세요."
            }
            canvas.drawText(msg, m + 60f * s, 560f * s, textPaint)
            drawFooter(canvas, w, m, s)
            canvas.restore()
            return
        }

        // --- 3. Hero Weather Card ---
        val heroTop = 340f * s
        val heroBot = 670f * s
        drawCard(canvas, RectF(m, heroTop, w - m, heroBot), 24f * s, Color.WHITE, 4f * s)

        strokePaint.color = Color.BLACK
        strokePaint.strokeWidth = 5f * s
        WeatherIconDrawer.draw(canvas, data.weatherCode, m + 150f * s, heroTop + 130f * s, 2.2f * s, strokePaint, fillPaint)

        val condText = WeatherRepository.weatherLabel(data.weatherCode, lang)
        textPaint.color = Color.BLACK
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 40f * s
        val condW = textPaint.measureText(condText)
        canvas.drawText(condText, m + 150f * s - condW / 2f, heroTop + 275f * s, textPaint)

        textPaint.textSize = 156f * s
        canvas.drawText(String.format(Locale.US, "%.1f°", data.currentTemp), m + 325f * s, heroTop + 205f * s, textPaint)

        // Yesterday comparison + High/Low
        val rx = m + 740f * s
        val pill = RectF(rx, heroTop + 48f * s, w - m - 36f * s, heroTop + 114f * s)
        drawCard(canvas, pill, 14f * s, Color.rgb(235, 235, 235), 2f * s)

        val diffText = when {
            data.yesterdayDiff == null -> if (isEn) "No yesterday comparison" else "어제 비교 데이터 없음"
            abs(data.yesterdayDiff) < 0.15 -> if (isEn) "Same as yesterday" else "어제와 기온 같음"
            data.yesterdayDiff > 0 -> if (isEn) {
                String.format(Locale.US, "▲ %.1f° warmer than yesterday", data.yesterdayDiff)
            } else {
                String.format(Locale.US, "▲ 어제보다 %.1f° 높음", data.yesterdayDiff)
            }
            else -> if (isEn) {
                String.format(Locale.US, "▼ %.1f° cooler than yesterday", abs(data.yesterdayDiff))
            } else {
                String.format(Locale.US, "▼ 어제보다 %.1f° 낮음", abs(data.yesterdayDiff))
            }
        }
        textPaint.color = Color.BLACK
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 27f * s
        canvas.drawText(diffText, rx + 22f * s, heroTop + 92f * s, textPaint)

        textPaint.textSize = 36f * s
        val hlText = if (isEn) {
            "High ${data.todayMax}°   /   Low ${data.todayMin}°"
        } else {
            "최고  ${data.todayMax}°   /   최저  ${data.todayMin}°"
        }
        canvas.drawText(hlText, rx, heroTop + 178f * s, textPaint)

        textPaint.color = Color.rgb(60, 60, 60)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 30f * s
        val feelsText = if (isEn) {
            String.format(Locale.US, "Feels like  %.1f°", data.apparentTemp)
        } else {
            String.format(Locale.US, "체감온도  %.1f°", data.apparentTemp)
        }
        canvas.drawText(feelsText, rx, heroTop + 234f * s, textPaint)
        canvas.drawText(
            if (isEn) "Precip  ${data.precipProb}%" else "강수확률  ${data.precipProb}%",
            rx,
            heroTop + 280f * s,
            textPaint
        )

        // --- 4. 5-Column Environment Metrics ---
        val metTop = 696f * s
        val metBot = 876f * s
        val gap = 16f * s
        val cw = (w - 2f * m - gap * 4f) / 5f

        val metrics = arrayOf(
            Triple(if (isEn) "PM10" else "미세먼지 (PM10)", data.pm10?.let { "${it.toInt()} ㎍/㎥" } ?: "-", WeatherRepository.pm10Grade(data.pm10, lang)),
            Triple(if (isEn) "PM2.5" else "초미세 (PM2.5)", data.pm25?.let { "${it.toInt()} ㎍/㎥" } ?: "-", WeatherRepository.pm25Grade(data.pm25, lang)),
            Triple(if (isEn) "UV Index" else "자외선 지수", data.uvIndex?.let { String.format(Locale.US, "%.1f", it) } ?: "-", WeatherRepository.uvGrade(data.uvIndex, lang)),
            Triple(if (isEn) "Humidity" else "습도", "${data.humidity} %", WeatherRepository.humidityGrade(data.humidity, lang)),
            Triple(if (isEn) "Wind" else "풍속", String.format(Locale.US, "%.1f m/s", data.windSpeed), WeatherRepository.windDirLabel(data.windDirectionDeg, lang))
        )

        for (i in metrics.indices) {
            val (title, valText, badge) = metrics[i]
            val x1 = m + i * (cw + gap)
            val x2 = x1 + cw
            drawCard(canvas, RectF(x1, metTop, x2, metBot), 18f * s, Color.rgb(248, 248, 248), 3f * s)

            textPaint.color = Color.rgb(70, 70, 70)
            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.textSize = 22f * s
            canvas.drawText(title, x1 + 18f * s, metTop + 42f * s, textPaint)

            textPaint.color = Color.BLACK
            textPaint.textSize = 35f * s
            canvas.drawText(valText, x1 + 18f * s, metTop + 98f * s, textPaint)

            val badgeRect = RectF(x1 + 18f * s, metTop + 122f * s, x1 + 124f * s, metTop + 160f * s)
            drawCard(canvas, badgeRect, 8f * s, Color.WHITE, 2f * s)
            textPaint.textSize = 21f * s
            val bw = textPaint.measureText(badge)
            canvas.drawText(badge, badgeRect.centerX() - bw / 2f, metTop + 149f * s, textPaint)
        }

        when (currentPage) {
            1 -> drawQuotesPage(canvas, w, m, s)
            2 -> drawNewsPage(canvas, w, m, s)
            else -> drawWeatherLowerSection(canvas, data, w, m, s)
        }

        drawFooter(canvas, w, m, s)
        canvas.restore()
    }

    private fun drawWeatherLowerSection(canvas: Canvas, data: WeatherData, w: Float, m: Float, s: Float) {
        val lang = settings.language
        val isEn = lang == "en"
        // --- 5. Hourly Forecast ---
        val hrTop = 906f * s
        val hrBot = 1186f * s
        drawCard(canvas, RectF(m, hrTop, w - m, hrBot), 22f * s, Color.WHITE, 3f * s)

        textPaint.color = Color.BLACK
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 32f * s
        canvas.drawText(if (isEn) "Hourly Forecast" else "시간별 날씨", m + 28f * s, hrTop + 52f * s, textPaint)

        strokePaint.color = Color.rgb(160, 160, 160)
        strokePaint.strokeWidth = 2f * s
        canvas.drawLine(m + 24f * s, hrTop + 72f * s, w - m - 24f * s, hrTop + 72f * s, strokePaint)

        if (data.hourly.isNotEmpty()) {
            val innerW = (w - 2f * m - 40f * s) / data.hourly.size
            for (i in data.hourly.indices) {
                val item = data.hourly[i]
                val cx = m + 20f * s + i * innerW + innerW / 2f
                if (i > 0) {
                    val vx = m + 20f * s + i * innerW
                    strokePaint.color = Color.rgb(215, 215, 215)
                    strokePaint.strokeWidth = 2f * s
                    canvas.drawLine(vx, hrTop + 88f * s, vx, hrBot - 22f * s, strokePaint)
                }
                textPaint.color = Color.rgb(50, 50, 50)
                textPaint.typeface = Typeface.DEFAULT_BOLD
                textPaint.textSize = 26f * s
                val tw = textPaint.measureText(item.timeLabel)
                canvas.drawText(item.timeLabel, cx - tw / 2f, hrTop + 114f * s, textPaint)

                strokePaint.color = Color.BLACK
                strokePaint.strokeWidth = 3f * s
                WeatherIconDrawer.draw(canvas, item.weatherCode, cx, hrTop + 162f * s, 0.80f * s, strokePaint, fillPaint)

                textPaint.color = Color.BLACK
                textPaint.textSize = 34f * s
                val tempStr = "${item.temp}°"
                canvas.drawText(tempStr, cx - textPaint.measureText(tempStr) / 2f, hrTop + 230f * s, textPaint)

                textPaint.color = Color.rgb(80, 80, 80)
                textPaint.typeface = Typeface.DEFAULT
                textPaint.textSize = 20f * s
                val popStr = if (isEn) "Precip ${item.precipProb}%" else "강수 ${item.precipProb}%"
                canvas.drawText(popStr, cx - textPaint.measureText(popStr) / 2f, hrTop + 264f * s, textPaint)
            }
        }

        // --- 6. Weekly Forecast (7 Days) ---
        val wkTop = 1216f * s
        val wkBot = 1776f * s
        drawCard(canvas, RectF(m, wkTop, w - m, wkBot), 22f * s, Color.WHITE, 3f * s)

        textPaint.color = Color.BLACK
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 32f * s
        canvas.drawText(if (isEn) "7-Day Forecast" else "주간 날씨 (7일 예보)", m + 28f * s, wkTop + 52f * s, textPaint)

        strokePaint.color = Color.rgb(160, 160, 160)
        strokePaint.strokeWidth = 2f * s
        canvas.drawLine(m + 24f * s, wkTop + 72f * s, w - m - 24f * s, wkTop + 72f * s, strokePaint)

        if (data.daily.isNotEmpty()) {
            val gMin = data.daily.minOf { it.tempMin }.toFloat()
            val gMax = data.daily.maxOf { it.tempMax }.toFloat().let { if (it <= gMin) gMin + 1f else it }
            val rowH = (wkBot - (wkTop + 82f * s) - 14f * s) / data.daily.size
            val barX1 = m + 740f * s
            val barX2 = w - m - 190f * s

            for (i in data.daily.indices) {
                val item = data.daily[i]
                val ry = wkTop + 84f * s + i * rowH
                if (i > 0) {
                    strokePaint.color = Color.rgb(225, 225, 225)
                    strokePaint.strokeWidth = 2f * s
                    canvas.drawLine(m + 28f * s, ry, w - m - 28f * s, ry, strokePaint)
                }
                val cy = ry + rowH / 2f

                textPaint.color = Color.BLACK
                textPaint.typeface = if (i == 0) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                textPaint.textSize = 28f * s
                canvas.drawText("${item.dayLabel} (${item.dateLabel})", m + 36f * s, cy + 10f * s, textPaint)

                strokePaint.color = Color.BLACK
                strokePaint.strokeWidth = 2.6f * s
                WeatherIconDrawer.draw(canvas, item.weatherCode, m + 280f * s, cy, 0.65f * s, strokePaint, fillPaint)

                textPaint.color = Color.rgb(30, 30, 30)
                textPaint.typeface = Typeface.DEFAULT
                textPaint.textSize = 26f * s
                canvas.drawText(WeatherRepository.weatherLabel(item.weatherCode, lang), m + 340f * s, cy + 9f * s, textPaint)

                textPaint.color = Color.rgb(80, 80, 80)
                textPaint.textSize = 24f * s
                canvas.drawText(if (isEn) "Precip ${item.precipProb}%" else "강수 ${item.precipProb}%", m + 530f * s, cy + 8f * s, textPaint)

                textPaint.color = Color.rgb(60, 60, 60)
                textPaint.textSize = 28f * s
                canvas.drawText("${item.tempMin}°", barX1 - 76f * s, cy + 10f * s, textPaint)

                // Range bar
                fillPaint.color = Color.rgb(230, 230, 230)
                fillPaint.style = Paint.Style.FILL
                canvas.drawRoundRect(RectF(barX1, cy - 8f * s, barX2, cy + 8f * s), 8f * s, 8f * s, fillPaint)

                val sx = barX1 + (item.tempMin - gMin) / (gMax - gMin) * (barX2 - barX1)
                val ex = maxOf(sx + 16f * s, barX1 + (item.tempMax - gMin) / (gMax - gMin) * (barX2 - barX1))
                fillPaint.color = Color.rgb(30, 30, 30)
                canvas.drawRoundRect(RectF(sx, cy - 8f * s, ex, cy + 8f * s), 8f * s, 8f * s, fillPaint)

                textPaint.color = Color.BLACK
                textPaint.typeface = Typeface.DEFAULT_BOLD
                textPaint.textSize = 28f * s
                canvas.drawText("${item.tempMax}°", barX2 + 24f * s, cy + 10f * s, textPaint)
            }
        }
    }

    private fun drawQuotesPage(canvas: Canvas, w: Float, m: Float, s: Float) {
        val isEn = settings.language == "en"
        val boxTop = 906f * s
        val boxBot = 1776f * s
        drawCard(canvas, RectF(m, boxTop, w - m, boxBot), 24f * s, Color.WHITE, 3f * s)

        textPaint.color = Color.BLACK
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 32f * s
        canvas.drawText(
            if (isEn) "Daily Quotes  ·  Effort & Potential" else "오늘의 명언  ·  노력과 가능성",
            m + 36f * s,
            boxTop + 56f * s,
            textPaint
        )

        val q1 = mainQuote
        val tagText = q1?.tag ?: if (isEn) "Motivation" else "동기부여"
        val tagRect = RectF(w - m - 230f * s, boxTop + 22f * s, w - m - 32f * s, boxTop + 66f * s)
        drawCard(canvas, tagRect, 12f * s, Color.rgb(240, 240, 240), 2f * s)
        textPaint.color = Color.rgb(40, 40, 40)
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 22f * s
        val twTag = textPaint.measureText(tagText)
        canvas.drawText(tagText, tagRect.centerX() - twTag / 2f, boxTop + 52f * s, textPaint)

        strokePaint.color = Color.rgb(160, 160, 160)
        strokePaint.strokeWidth = 2f * s
        canvas.drawLine(m + 28f * s, boxTop + 84f * s, w - m - 28f * s, boxTop + 84f * s, strokePaint)

        if (q1 == null) {
            textPaint.color = Color.rgb(80, 80, 80)
            textPaint.typeface = Typeface.DEFAULT
            textPaint.textSize = 32f * s
            canvas.drawText(
                if (isEn) "Loading quotes..." else "명언을 불러오는 중입니다...",
                m + 64f * s,
                boxTop + 260f * s,
                textPaint
            )
            return
        }

        textPaint.color = Color.rgb(160, 160, 160)
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 96f * s
        canvas.drawText("“", m + 56f * s, boxTop + 188f * s, textPaint)

        textPaint.color = Color.BLACK
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 44f * s
        val maxTextW = w - 2f * m - 144f * s
        val mainBody = if (isEn) q1.textEn else q1.textKo
        val lines = wrapText(mainBody, textPaint, maxTextW).take(4)
        var qy = boxTop + 245f * s
        for (line in lines) {
            canvas.drawText(line, m + 72f * s, qy, textPaint)
            qy += 68f * s
        }

        textPaint.color = Color.rgb(70, 70, 70)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 28f * s
        canvas.drawText("— ${q1.author}", m + 72f * s, qy + 20f * s, textPaint)

        val q2 = subQuote
        val subTop = boxTop + 560f * s
        val subBot = boxBot - 36f * s
        drawCard(canvas, RectF(m + 36f * s, subTop, w - m - 36f * s, subBot), 18f * s, Color.rgb(246, 246, 246), 2f * s)

        textPaint.color = Color.rgb(60, 60, 60)
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 24f * s
        canvas.drawText(if (isEn) "Daily Reflection" else "오늘의 한 줄 점검", m + 64f * s, subTop + 46f * s, textPaint)

        if (q2 != null) {
            textPaint.color = Color.BLACK
            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.textSize = 28f * s
            val subBody = if (isEn) q2.textEn else q2.textKo
            val subLines = wrapText("“${subBody}”", textPaint, w - 2f * m - 128f * s).take(2)
            var sy = subTop + 96f * s
            for (sl in subLines) {
                canvas.drawText(sl, m + 64f * s, sy, textPaint)
                sy += 40f * s
            }
            textPaint.color = Color.rgb(80, 80, 80)
            textPaint.typeface = Typeface.DEFAULT
            textPaint.textSize = 23f * s
            canvas.drawText("— ${q2.author}", m + 64f * s, subBot - 68f * s, textPaint)
        }

        textPaint.color = Color.rgb(100, 100, 100)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 21f * s
        canvas.drawText(
            if (isEn) "· Use physical page buttons or tap bottom corners to switch pages."
            else "· 물리 페이지 버튼 또는 화면 하단 좌우를 눌러 페이지를 전환합니다.",
            m + 64f * s,
            subBot - 28f * s,
            textPaint
        )
    }

    private fun drawNewsPage(canvas: Canvas, w: Float, m: Float, s: Float) {
        val isEn = settings.language == "en"
        val cc = weather?.countryCode ?: settings.manualCountryCode.ifBlank { "KR" }
        val boxTop = 906f * s
        val boxBot = 1776f * s
        drawCard(canvas, RectF(m, boxTop, w - m, boxBot), 24f * s, Color.WHITE, 3f * s)

        textPaint.color = Color.BLACK
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 32f * s
        canvas.drawText(if (isEn) "Top News Headlines" else "주요 뉴스 헤드라인", m + 36f * s, boxTop + 56f * s, textPaint)

        textPaint.color = Color.rgb(85, 85, 85)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 23f * s
        val srcInfo = if (isEn) "Google News · $cc" else "Google 뉴스 · $cc"
        canvas.drawText(srcInfo, w - m - 36f * s - textPaint.measureText(srcInfo), boxTop + 54f * s, textPaint)

        strokePaint.color = Color.rgb(160, 160, 160)
        strokePaint.strokeWidth = 2f * s
        canvas.drawLine(m + 28f * s, boxTop + 84f * s, w - m - 28f * s, boxTop + 84f * s, strokePaint)

        if (newsList.isEmpty()) {
            textPaint.color = Color.rgb(80, 80, 80)
            textPaint.typeface = Typeface.DEFAULT
            textPaint.textSize = 32f * s
            val msg = if (wifiConnected) {
                if (isEn) "Loading news or no headlines available." else "뉴스를 불러오는 중이거나 기사가 없습니다."
            } else {
                if (isEn) "Connect to Wi-Fi and tap [Refresh]." else "Wi-Fi 연결 후 [새로고침]을 눌러주세요."
            }
            canvas.drawText(msg, m + 56f * s, boxTop + 260f * s, textPaint)
            return
        }

        val count = minOf(newsList.size, 6)
        val rowH = (boxBot - (boxTop + 96f * s) - 20f * s) / count
        val titleMaxW = w - 2f * m - 204f * s

        for (i in 0 until count) {
            val item = newsList[i]
            val ry = boxTop + 94f * s + i * rowH
            if (i > 0) {
                strokePaint.color = Color.rgb(220, 220, 220)
                strokePaint.strokeWidth = 2f * s
                canvas.drawLine(m + 32f * s, ry, w - m - 32f * s, ry, strokePaint)
            }

            val badgeRect = RectF(m + 36f * s, ry + 20f * s, m + 150f * s, ry + 58f * s)
            drawCard(canvas, badgeRect, 10f * s, Color.rgb(238, 238, 238), 2f * s)
            textPaint.color = Color.BLACK
            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.textSize = 21f * s
            val catW = textPaint.measureText(item.category)
            canvas.drawText(item.category, badgeRect.centerX() - catW / 2f, ry + 47f * s, textPaint)

            textPaint.color = Color.BLACK
            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.textSize = 27f * s
            val clippedTitle = ellipsizeText(item.title, textPaint, titleMaxW)
            canvas.drawText(clippedTitle, m + 172f * s, ry + 48f * s, textPaint)

            textPaint.color = Color.rgb(90, 90, 90)
            textPaint.typeface = Typeface.DEFAULT
            textPaint.textSize = 22f * s
            canvas.drawText("${item.source} · ${item.timeAgo}", m + 172f * s, ry + 86f * s, textPaint)
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val result = ArrayList<String>()
        val words = text.split(" ")
        var line = StringBuilder()
        for (word in words) {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(candidate) <= maxWidth) {
                line = StringBuilder(candidate)
            } else {
                if (line.isNotEmpty()) result.add(line.toString())
                line = StringBuilder(word)
            }
        }
        if (line.isNotEmpty()) result.add(line.toString())
        return result
    }

    private fun ellipsizeText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "…") > maxWidth) {
            end--
        }
        return text.substring(0, end) + "…"
    }

    private fun drawWorldClocks(canvas: Canvas, now: Date, w: Float, m: Float, s: Float) {
        val selectedIds = listOf(settings.worldClock1, settings.worldClock2).filter { it.isNotBlank() }
        if (selectedIds.isEmpty()) return

        val wcW = 224f * s
        val wcGap = 14f * s
        val wcTop = 166f * s
        val wcBot = 314f * s
        val totalW = selectedIds.size * wcW + (selectedIds.size - 1) * wcGap
        var wx1 = w - m - totalW

        val localTz = TimeZone.getDefault()
        val nowMs = now.time
        val localOffsetMs = localTz.getOffset(nowMs)

        for (rawVal in selectedIds) {
            val parts = rawVal.split("|", limit = 2)
            val tzId = if (parts.size == 2) parts[1] else parts[0]
            val koName = if (parts.size == 2 && parts[0].isNotBlank()) {
                parts[0]
            } else {
                SettingsStore.WORLD_CLOCKS.firstOrNull { it.timeZoneId == tzId }?.cityName
                    ?: tzId.substringAfterLast('/')
            }
            val cityLabel = SettingsStore.displayWorldClockCity(koName, tzId, settings.language)
            val tz = TimeZone.getTimeZone(tzId)
            val diffHours = (tz.getOffset(nowMs) - localOffsetMs) / 3600_000.0
            val diffStr = when {
                abs(diffHours) < 0.01 -> "0h"
                diffHours % 1.0 == 0.0 -> String.format(Locale.US, "%+.0fh", diffHours)
                else -> String.format(Locale.US, "%+.1fh", diffHours)
            }

            val wcLocale = if (settings.language == "en") Locale.US else Locale.KOREA
            val wcTimeFmt = SimpleDateFormat("HH:mm", wcLocale).apply { timeZone = tz }
            val wcDateFmt = SimpleDateFormat("MM.dd (E)", wcLocale).apply { timeZone = tz }

            val wx2 = wx1 + wcW
            drawCard(canvas, RectF(wx1, wcTop, wx2, wcBot), 16f * s, Color.rgb(248, 248, 248), 3f * s)

            textPaint.color = Color.rgb(60, 60, 60)
            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.textSize = 22f * s
            canvas.drawText("$cityLabel ($diffStr)", wx1 + 16f * s, wcTop + 36f * s, textPaint)

            textPaint.color = Color.BLACK
            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.textSize = 46f * s
            canvas.drawText(wcTimeFmt.format(now), wx1 + 16f * s, wcTop + 90f * s, textPaint)

            textPaint.color = Color.rgb(75, 75, 75)
            textPaint.typeface = Typeface.DEFAULT
            textPaint.textSize = 21f * s
            canvas.drawText(wcDateFmt.format(now), wx1 + 16f * s, wcTop + 128f * s, textPaint)

            wx1 += wcW + wcGap
        }
    }

    private fun drawFooter(canvas: Canvas, w: Float, m: Float, s: Float) {
        val isEn = settings.language == "en"
        val footer = if (isEn) {
            val remText = if (keepAliveRemainingMinutes > 0) "${keepAliveRemainingMinutes}m left" else "Off"
            val refreshLabel = if (settings.refreshIntervalMinutes < 60) {
                "${settings.refreshIntervalMinutes}m"
            } else {
                "${settings.refreshIntervalMinutes / 60}h"
            }
            "Auto: $refreshLabel  ·  Keep On: ${settings.keepScreenHours}h ($remText)  ·  Bright: ${settings.brightnessPercent}%"
        } else {
            val remText = if (keepAliveRemainingMinutes > 0) "남은 ${keepAliveRemainingMinutes}분" else "해제됨"
            val refreshLabel = if (settings.refreshIntervalMinutes < 60) {
                "${settings.refreshIntervalMinutes}분마다"
            } else {
                "${settings.refreshIntervalMinutes / 60}시간마다"
            }
            "자동 갱신: $refreshLabel  ·  화면 유지: ${settings.keepScreenHours}시간 ($remText)  ·  밝기: ${settings.brightnessPercent}%"
        }
        textPaint.color = Color.rgb(80, 80, 80)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 23f * s
        canvas.drawText(footer, m, 1826f * s, textPaint)

        val pageName = when (currentPage) {
            1 -> if (isEn) "Quotes" else "오늘의 명언"
            2 -> if (isEn) "News" else "주요 뉴스"
            else -> if (isEn) "Weather" else "날씨 예보"
        }
        val ind = "◀  ${currentPage + 1} / 3 · $pageName  ▶"
        textPaint.color = Color.BLACK
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 24f * s
        val indW = textPaint.measureText(ind)
        val indX = w - m - indW
        canvas.drawText(ind, indX, 1826f * s, textPaint)

        btnPrevPageRect.set(indX - 24f * s, 1785f * s, indX + indW / 2f, 1860f * s)
        btnNextPageRect.set(indX + indW / 2f, 1785f * s, w - m + 24f * s, 1860f * s)
    }

    private fun drawCard(canvas: Canvas, rect: RectF, radius: Float, bg: Int, strokeW: Float) {
        fillPaint.color = bg
        fillPaint.style = Paint.Style.FILL
        canvas.drawRoundRect(rect, radius, radius, fillPaint)

        strokePaint.color = Color.BLACK
        strokePaint.strokeWidth = strokeW
        canvas.drawRoundRect(rect, radius, radius, strokePaint)
    }

    private fun drawButton(
        canvas: Canvas,
        outRect: RectF,
        rightX: Float,
        topY: Float,
        width: Float,
        height: Float,
        label: String,
        isDark: Boolean,
        s: Float
    ): Float {
        val leftX = rightX - width
        outRect.set(leftX, topY, rightX, topY + height)
        drawCard(canvas, outRect, 14f * s, if (isDark) Color.rgb(20, 20, 20) else Color.rgb(245, 245, 245), 3f * s)

        textPaint.color = if (isDark) Color.WHITE else Color.BLACK
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 25f * s
        val tw = textPaint.measureText(label)
        val fm = textPaint.fontMetrics
        val baseline = outRect.centerY() - (fm.ascent + fm.descent) / 2f
        canvas.drawText(label, outRect.centerX() - tw / 2f, baseline, textPaint)
        return leftX
    }
}
