package com.ridipaper.weather.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import com.ridipaper.weather.data.SettingsStore
import com.ridipaper.weather.data.WeatherRepository
import com.ridipaper.weather.model.BookItem
import com.ridipaper.weather.model.DashboardSettings
import com.ridipaper.weather.model.EconomyIndicator
import com.ridipaper.weather.model.MovieItem
import com.ridipaper.weather.model.NewsItem
import com.ridipaper.weather.model.PageType
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
    private var bookItem: BookItem? = null
    private var movieItem: MovieItem? = null
    private var economyList: List<EconomyIndicator> = emptyList()
    private var bookBitmap: Bitmap? = null
    private var movieBitmap: Bitmap? = null
    private var activePageType: PageType = PageType.WEATHER
    private var pageNumber: Int = 1
    private var totalPages: Int = 6
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

    private val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
    }

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
        book: BookItem?,
        movie: MovieItem?,
        economy: List<EconomyIndicator>,
        bookBitmap: Bitmap?,
        movieBitmap: Bitmap?,
        activePageType: PageType,
        pageNumber: Int,
        totalPages: Int,
        settings: DashboardSettings,
        wifiConnected: Boolean,
        remainingMinutes: Int,
        statusMessage: String? = null
    ) {
        this.weather = weather
        this.mainQuote = mainQuote
        this.subQuote = subQuote
        this.newsList = news
        this.bookItem = book
        this.movieItem = movie
        this.economyList = economy
        this.bookBitmap = bookBitmap
        this.movieBitmap = movieBitmap
        this.activePageType = activePageType
        this.pageNumber = pageNumber
        this.totalPages = totalPages
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

        when (activePageType) {
            PageType.QUOTES -> drawQuotesPage(canvas, w, m, s)
            PageType.NEWS -> drawNewsPage(canvas, w, m, s)
            PageType.BOOK -> drawBookPage(canvas, w, m, s)
            PageType.MOVIE -> drawMoviePage(canvas, w, m, s)
            PageType.ECONOMY -> drawEconomyPage(canvas, w, m, s)
            PageType.WEATHER -> drawWeatherLowerSection(canvas, data, w, m, s)
        }

        drawFooter(canvas, w, m, s)
        canvas.restore()
    }

    private fun drawWeatherLowerSection(canvas: Canvas, data: WeatherData, w: Float, m: Float, s: Float) {
        val lang = settings.language
        val isEn = lang == "en"
        // --- 5. Hourly Forecast (Today 00:00 ~ Tomorrow 06:00 + Temp Graph + Current Slot Highlight) ---
        val hrTop = 906f * s
        val hrBot = 1254f * s
        drawCard(canvas, RectF(m, hrTop, w - m, hrBot), 22f * s, Color.WHITE, 3f * s)

        textPaint.color = Color.BLACK
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 32f * s
        val hrTitle = if (isEn) "Hourly Forecast" else "시간별 날씨"
        canvas.drawText(hrTitle, m + 28f * s, hrTop + 50f * s, textPaint)
        val hrTitleW = textPaint.measureText(hrTitle)

        textPaint.color = Color.rgb(85, 85, 85)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 23f * s
        val hrSub = if (isEn) "Today 00:00 ~ Next 06:00" else "오늘 00시 ~ 내일 06시"
        canvas.drawText(hrSub, m + 28f * s + hrTitleW + 18f * s, hrTop + 49f * s, textPaint)

        val curItem = data.hourly.firstOrNull { it.isCurrent }
        if (curItem != null) {
            val curHour = curItem.timeLabel.take(2).toIntOrNull() ?: 0
            val nextHour = (curHour + 3) % 24
            val badgeText = if (isEn) {
                String.format(Locale.US, "Now: %02d:00~%02d:00", curHour, nextHour)
            } else {
                String.format(Locale.US, "현재 구간: %02d시~%02d시", curHour, nextHour)
            }
            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.textSize = 21f * s
            val bw = textPaint.measureText(badgeText) + 32f * s
            val badgeRect = RectF(w - m - 24f * s - bw, hrTop + 18f * s, w - m - 24f * s, hrTop + 58f * s)
            drawCard(canvas, badgeRect, 10f * s, Color.rgb(24, 24, 24), 2f * s)
            textPaint.color = Color.WHITE
            canvas.drawText(badgeText, badgeRect.left + 16f * s, hrTop + 45f * s, textPaint)
        }

        strokePaint.color = Color.rgb(170, 170, 170)
        strokePaint.strokeWidth = 2f * s
        canvas.drawLine(m + 24f * s, hrTop + 70f * s, w - m - 24f * s, hrTop + 70f * s, strokePaint)

        if (data.hourly.isNotEmpty()) {
            val padX = 20f * s
            val innerW = (w - 2f * m - 2f * padX) / data.hourly.size
            val colTop = hrTop + 82f * s
            val colBot = hrBot - 14f * s

            // 1) Column backgrounds and dividers
            for (i in data.hourly.indices) {
                val item = data.hourly[i]
                val x1 = m + padX + i * innerW
                val x2 = x1 + innerW
                if (item.isCurrent) {
                    drawCard(
                        canvas,
                        RectF(x1 + 3f * s, colTop, x2 - 3f * s, colBot),
                        14f * s,
                        Color.rgb(236, 236, 236),
                        3f * s
                    )
                } else if (i > 0) {
                    if (item.isTomorrow && item.timeLabel.startsWith("00")) {
                        strokePaint.color = Color.rgb(90, 90, 90)
                        strokePaint.strokeWidth = 2f * s
                        var dy = colTop + 6f * s
                        while (dy < colBot - 6f * s) {
                            canvas.drawLine(x1, dy, x1, minOf(dy + 6f * s, colBot - 6f * s), strokePaint)
                            dy += 12f * s
                        }
                    } else {
                        strokePaint.color = Color.rgb(224, 224, 224)
                        strokePaint.strokeWidth = 2f * s
                        canvas.drawLine(x1, colTop + 8f * s, x1, colBot - 8f * s, strokePaint)
                    }
                }
            }

            // 2) Temperature graph coordinates
            val tMin = data.hourly.minOf { it.temp }
            val tMax = data.hourly.maxOf { it.temp }
            val tSpan = maxOf(1, tMax - tMin).toFloat()
            val graphTop = hrTop + 216f * s
            val graphBot = hrTop + 286f * s

            val ptsX = FloatArray(data.hourly.size)
            val ptsY = FloatArray(data.hourly.size)
            for (i in data.hourly.indices) {
                ptsX[i] = m + padX + i * innerW + innerW / 2f
                val norm = (data.hourly[i].temp - tMin) / tSpan
                ptsY[i] = graphBot - norm * (graphBot - graphTop)
            }

            // Subtle dashed mid-line
            val midY = (graphTop + graphBot) / 2f
            strokePaint.color = Color.rgb(215, 215, 215)
            strokePaint.strokeWidth = 1.5f * s
            var dx = m + padX + 16f * s
            val maxDx = w - m - padX - 16f * s
            while (dx < maxDx) {
                canvas.drawLine(dx, midY, minOf(dx + 6f * s, maxDx), midY, strokePaint)
                dx += 14f * s
            }

            // Polyline connecting temperatures
            strokePaint.color = Color.rgb(40, 40, 40)
            strokePaint.strokeWidth = 4f * s
            for (i in 0 until data.hourly.size - 1) {
                canvas.drawLine(ptsX[i], ptsY[i], ptsX[i + 1], ptsY[i + 1], strokePaint)
            }

            // 3) Per-column labels, icons, graph nodes, and precip
            for (i in data.hourly.indices) {
                val item = data.hourly[i]
                val cx = ptsX[i]
                val py = ptsY[i]

                val displayTime = if (isEn) item.timeLabel.replace("시", ":00") else item.timeLabel
                if (item.isCurrent) {
                    val pillRect = RectF(cx - 42f * s, colTop + 8f * s, cx + 42f * s, colTop + 42f * s)
                    drawCard(canvas, pillRect, 10f * s, Color.rgb(20, 20, 20), 2f * s)
                    textPaint.color = Color.WHITE
                    textPaint.typeface = Typeface.DEFAULT_BOLD
                    textPaint.textSize = 22f * s
                    val tw = textPaint.measureText(displayTime)
                    canvas.drawText(displayTime, cx - tw / 2f, colTop + 33f * s, textPaint)
                } else {
                    val isMidnightTmr = item.isTomorrow && item.timeLabel.startsWith("00")
                    if (isMidnightTmr) {
                        textPaint.color = Color.rgb(80, 80, 80)
                        textPaint.typeface = Typeface.DEFAULT_BOLD
                        textPaint.textSize = 15f * s
                        val nextTag = if (isEn) "Next" else "내일"
                        canvas.drawText(nextTag, cx - textPaint.measureText(nextTag) / 2f, colTop + 15f * s, textPaint)
                    }
                    val tBaseline = if (isMidnightTmr) colTop + 38f * s else colTop + 34f * s
                    textPaint.color = Color.rgb(40, 40, 40)
                    textPaint.typeface = if (item.timeLabel.startsWith("00")) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    textPaint.textSize = 22f * s
                    canvas.drawText(displayTime, cx - textPaint.measureText(displayTime) / 2f, tBaseline, textPaint)
                }

                strokePaint.color = Color.BLACK
                strokePaint.strokeWidth = 2.6f * s
                WeatherIconDrawer.draw(canvas, item.weatherCode, cx, hrTop + 150f * s, 0.62f * s, strokePaint, fillPaint)

                // Temperature above node
                textPaint.color = Color.BLACK
                textPaint.typeface = Typeface.DEFAULT_BOLD
                textPaint.textSize = 26f * s
                val tempStr = "${item.temp}°"
                canvas.drawText(tempStr, cx - textPaint.measureText(tempStr) / 2f, py - 16f * s, textPaint)

                // Graph node
                if (item.isCurrent) {
                    fillPaint.color = Color.BLACK
                    fillPaint.style = Paint.Style.FILL
                    canvas.drawCircle(cx, py, 9f * s, fillPaint)
                    fillPaint.color = Color.WHITE
                    canvas.drawCircle(cx, py, 3.5f * s, fillPaint)
                } else {
                    fillPaint.color = Color.WHITE
                    fillPaint.style = Paint.Style.FILL
                    canvas.drawCircle(cx, py, 6f * s, fillPaint)
                    strokePaint.color = Color.rgb(20, 20, 20)
                    strokePaint.strokeWidth = 3f * s
                    canvas.drawCircle(cx, py, 6f * s, strokePaint)
                }

                // Precipitation %
                textPaint.color = if (item.isCurrent) Color.rgb(20, 20, 20) else Color.rgb(90, 90, 90)
                textPaint.typeface = if (item.isCurrent) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                textPaint.textSize = 19f * s
                val popStr = "${item.precipProb}%"
                canvas.drawText(popStr, cx - textPaint.measureText(popStr) / 2f, colBot - 12f * s, textPaint)
            }
        }

        // --- 6. Weekly Forecast (7 Days) ---
        val wkTop = 1278f * s
        val wkBot = 1776f * s
        drawCard(canvas, RectF(m, wkTop, w - m, wkBot), 22f * s, Color.WHITE, 3f * s)

        textPaint.color = Color.BLACK
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 32f * s
        canvas.drawText(if (isEn) "7-Day Forecast" else "주간 날씨 (7일 예보)", m + 28f * s, wkTop + 48f * s, textPaint)

        strokePaint.color = Color.rgb(160, 160, 160)
        strokePaint.strokeWidth = 2f * s
        canvas.drawLine(m + 24f * s, wkTop + 68f * s, w - m - 24f * s, wkTop + 68f * s, strokePaint)

        if (data.daily.isNotEmpty()) {
            val gMin = data.daily.minOf { it.tempMin }.toFloat()
            val gMax = data.daily.maxOf { it.tempMax }.toFloat().let { if (it <= gMin) gMin + 1f else it }
            val rowH = (wkBot - (wkTop + 78f * s) - 12f * s) / data.daily.size
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

        val q1 = mainQuote
        val headerTag = q1?.tag ?: if (isEn) "Effort & Potential" else "노력과 가능성"
        textPaint.color = Color.BLACK
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 32f * s
        canvas.drawText(
            if (isEn) "Daily Quotes  ·  $headerTag" else "오늘의 명언  ·  $headerTag",
            m + 36f * s,
            boxTop + 56f * s,
            textPaint
        )

        val catLabel = if (isEn) {
            q1?.categoryEn?.ifBlank { "Classic Quote" } ?: "Classic Quote"
        } else {
            q1?.categoryKo?.ifBlank { "고전 명언" } ?: "고전 명언"
        }
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 22f * s
        val catW = textPaint.measureText(catLabel) + 36f * s
        val tagRect = RectF(w - m - 32f * s - catW, boxTop + 22f * s, w - m - 32f * s, boxTop + 66f * s)
        drawCard(canvas, tagRect, 12f * s, Color.rgb(24, 24, 24), 2f * s)
        textPaint.color = Color.WHITE
        canvas.drawText(catLabel, tagRect.left + 18f * s, boxTop + 52f * s, textPaint)

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
        canvas.drawText("“", m + 56f * s, boxTop + 178f * s, textPaint)

        textPaint.color = Color.BLACK
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 42f * s
        val maxTextW = w - 2f * m - 144f * s
        val mainBody = if (isEn) q1.textEn else q1.textKo
        val lines = wrapText(mainBody, textPaint, maxTextW).take(3)
        var qy = boxTop + 230f * s
        for (line in lines) {
            canvas.drawText(line, m + 72f * s, qy, textPaint)
            qy += 64f * s
        }

        val authorStr = if (isEn && q1.authorEn.isNotBlank()) q1.authorEn else q1.author
        textPaint.color = Color.rgb(70, 70, 70)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 27f * s
        canvas.drawText("— $authorStr", m + 72f * s, qy + 16f * s, textPaint)

        // Source · Background & Interpretation Box (replaces "오늘의 한 줄 점검")
        val subTop = boxTop + 476f * s
        val subBot = boxBot - 32f * s
        drawCard(canvas, RectF(m + 36f * s, subTop, w - m - 36f * s, subBot), 18f * s, Color.rgb(246, 246, 246), 2f * s)

        val badgeTitle = if (isEn) "Source · Context & Interpretation" else "작품 출처 · 배경 및 해석"
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 22f * s
        val badgeW = textPaint.measureText(badgeTitle) + 32f * s
        drawCard(canvas, RectF(m + 62f * s, subTop + 20f * s, m + 62f * s + badgeW, subTop + 58f * s), 8f * s, Color.rgb(232, 232, 232), 1.5f * s)
        textPaint.color = Color.rgb(20, 20, 20)
        canvas.drawText(badgeTitle, m + 78f * s, subTop + 47f * s, textPaint)

        val innerTextW = w - 2f * m - 128f * s
        var curY = subTop + 94f * s

        val bgText = if (isEn) q1.backgroundEn else q1.backgroundKo
        val interpText = if (isEn) q1.interpretationEn else q1.interpretationKo
        val sections = listOf(
            (if (isEn) "■ Context & Source" else "■ 작품 배경") to bgText,
            (if (isEn) "■ Interpretation" else "■ 명대사 해석") to interpText
        )

        for ((secTitle, secBody) in sections) {
            if (secBody.isBlank()) continue
            textPaint.color = Color.rgb(20, 20, 20)
            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.textSize = 23f * s
            canvas.drawText(secTitle, m + 64f * s, curY, textPaint)
            curY += 34f * s

            textPaint.color = Color.rgb(45, 45, 45)
            textPaint.typeface = Typeface.DEFAULT
            textPaint.textSize = 23f * s
            for (sLine in wrapText(secBody, textPaint, innerTextW).take(3)) {
                canvas.drawText(sLine, m + 64f * s, curY, textPaint)
                curY += 32f * s
            }
            curY += 12f * s
        }

        textPaint.color = Color.rgb(100, 100, 100)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 20f * s
        canvas.drawText(
            if (isEn) "· Use physical page buttons or tap bottom corners to switch pages."
            else "· 물리 페이지 버튼 또는 화면 하단 좌우를 눌러 페이지를 전환합니다.",
            m + 64f * s,
            subBot - 22f * s,
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

        val pageName = when (activePageType) {
            PageType.QUOTES -> if (isEn) "Quotes" else "오늘의 명언"
            PageType.NEWS -> if (isEn) "News" else "주요 뉴스"
            PageType.BOOK -> if (isEn) "Book Pick" else "책 추천"
            PageType.MOVIE -> if (isEn) "Movie Pick" else "영화 소개"
            PageType.ECONOMY -> if (isEn) "Economy" else "주요 경제지표"
            PageType.WEATHER -> if (isEn) "Weather" else "날씨 예보"
        }
        val ind = "◀  $pageNumber / $totalPages · $pageName  ▶"
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

    private fun drawBookPage(canvas: Canvas, w: Float, m: Float, s: Float) {
        val isEn = settings.language == "en"
        val boxTop = 906f * s
        val boxBot = 1776f * s
        drawCard(canvas, RectF(m, boxTop, w - m, boxBot), 24f * s, Color.WHITE, 3f * s)

        textPaint.color = Color.BLACK
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 32f * s
        canvas.drawText(if (isEn) "Today's Book Pick" else "오늘의 책 추천", m + 36f * s, boxTop + 56f * s, textPaint)

        val bk = bookItem
        val genre = if (isEn) (bk?.genreEn ?: "Book") else (bk?.genreKo ?: "도서")
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 21f * s
        val gw = textPaint.measureText(genre) + 32f * s
        val gRect = RectF(w - m - 32f * s - gw, boxTop + 22f * s, w - m - 32f * s, boxTop + 66f * s)
        drawCard(canvas, gRect, 12f * s, Color.rgb(24, 24, 24), 2f * s)
        textPaint.color = Color.WHITE
        canvas.drawText(genre, gRect.left + 16f * s, boxTop + 52f * s, textPaint)

        strokePaint.color = Color.rgb(160, 160, 160)
        strokePaint.strokeWidth = 2f * s
        canvas.drawLine(m + 28f * s, boxTop + 84f * s, w - m - 28f * s, boxTop + 84f * s, strokePaint)

        if (bk == null) {
            textPaint.color = Color.rgb(80, 80, 80)
            textPaint.typeface = Typeface.DEFAULT
            textPaint.textSize = 32f * s
            canvas.drawText(if (isEn) "Loading book pick..." else "추천 도서를 불러오는 중입니다...", m + 64f * s, boxTop + 260f * s, textPaint)
            return
        }

        // Left Book Cover
        val covX1 = m + 48f * s
        val covY1 = boxTop + 124f * s
        val covW = 330f * s
        val covH = 480f * s
        val covX2 = covX1 + covW
        val covY2 = covY1 + covH

        drawCard(canvas, RectF(covX1 + 10f * s, covY1 + 10f * s, covX2 + 10f * s, covY2 + 10f * s), 12f * s, Color.rgb(210, 210, 210), 1f * s)
        drawCard(canvas, RectF(covX1, covY1, covX2, covY2), 12f * s, Color.rgb(244, 244, 244), 3f * s)

        val bmp = bookBitmap
        if (bmp != null && !bmp.isRecycled) {
            val dest = RectF(covX1 + 12f * s, covY1 + 12f * s, covX2 - 12f * s, covY2 - 12f * s)
            canvas.drawBitmap(bmp, Rect(0, 0, bmp.width, bmp.height), dest, monoPaint)
            strokePaint.color = Color.BLACK
            strokePaint.strokeWidth = 2f * s
            canvas.drawRect(dest, strokePaint)
        } else {
            // Stylized monochrome book cover fallback
            strokePaint.color = Color.rgb(120, 120, 120)
            strokePaint.strokeWidth = 2f * s
            canvas.drawLine(covX1 + 22f * s, covY1 + 4f * s, covX1 + 22f * s, covY2 - 4f * s, strokePaint)

            val artBox = RectF(covX1 + 44f * s, covY1 + 36f * s, covX2 - 26f * s, covY1 + 270f * s)
            fillPaint.color = Color.rgb(30, 30, 30)
            fillPaint.style = Paint.Style.FILL
            canvas.drawRect(artBox, fillPaint)
            fillPaint.color = Color.rgb(245, 245, 245)
            canvas.drawCircle(covX1 + 195f * s, covY1 + 99f * s, 35f * s, fillPaint)
            val m1 = Path().apply {
                moveTo(covX1 + 46f * s, covY1 + 268f * s)
                lineTo(covX1 + 135f * s, covY1 + 150f * s)
                lineTo(covX1 + 215f * s, covY1 + 268f * s)
                close()
            }
            fillPaint.color = Color.rgb(130, 130, 130)
            canvas.drawPath(m1, fillPaint)
            val m2 = Path().apply {
                moveTo(covX1 + 145f * s, covY1 + 268f * s)
                lineTo(covX1 + 235f * s, covY1 + 172f * s)
                lineTo(covX2 - 28f * s, covY1 + 268f * s)
                close()
            }
            fillPaint.color = Color.rgb(185, 185, 185)
            canvas.drawPath(m2, fillPaint)

            val coverTitle = if (isEn) bk.titleEn else bk.titleKo
            textPaint.color = Color.BLACK
            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.textSize = 30f * s
            var cty = covY1 + 324f * s
            for (tl in wrapText(coverTitle, textPaint, covW - 76f * s).take(2)) {
                canvas.drawText(tl, covX1 + 44f * s, cty, textPaint)
                cty += 40f * s
            }
            strokePaint.color = Color.rgb(140, 140, 140)
            strokePaint.strokeWidth = 2f * s
            canvas.drawLine(covX1 + 44f * s, covY1 + 402f * s, covX2 - 34f * s, covY1 + 402f * s, strokePaint)
            textPaint.color = Color.rgb(60, 60, 60)
            textPaint.typeface = Typeface.DEFAULT
            textPaint.textSize = 20f * s
            val authorShort = (if (isEn) bk.authorEn else bk.authorKo).substringBefore("·").trim()
            canvas.drawText(ellipsizeText(authorShort, textPaint, covW - 76f * s), covX1 + 44f * s, covY1 + 438f * s, textPaint)
        }

        // Right Book Details
        val rx = covX2 + 52f * s
        val maxRw = w - m - 44f * s - rx
        textPaint.color = Color.BLACK
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 40f * s
        canvas.drawText(ellipsizeText(if (isEn) bk.titleEn else bk.titleKo, textPaint, maxRw), rx, covY1 + 38f * s, textPaint)

        textPaint.color = Color.rgb(75, 75, 75)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 24f * s
        val authLine = "${if (isEn) bk.authorEn else bk.authorKo} · ${bk.publisherYear}"
        canvas.drawText(ellipsizeText(authLine, textPaint, maxRw), rx, covY1 + 84f * s, textPaint)

        val chips = listOf(bk.rating, genre, if (isEn) bk.pagesTimeEn else bk.pagesTimeKo)
        var tx = rx
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 20f * s
        for (tg in chips) {
            val tw = textPaint.measureText(tg) + 28f * s
            if (tx + tw > w - m - 36f * s) break
            drawCard(canvas, RectF(tx, covY1 + 114f * s, tx + tw, covY1 + 152f * s), 8f * s, Color.rgb(240, 240, 240), 2f * s)
            textPaint.color = Color.rgb(20, 20, 20)
            canvas.drawText(tg, tx + 14f * s, covY1 + 140f * s, textPaint)
            tx += tw + 12f * s
        }

        textPaint.color = Color.rgb(20, 20, 20)
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 24f * s
        canvas.drawText(if (isEn) "■ About & Why Read" else "■ 책 소개 & 추천 이유", rx, covY1 + 206f * s, textPaint)

        textPaint.color = Color.rgb(45, 45, 45)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 24f * s
        var cy = covY1 + 248f * s
        for (line in wrapText(if (isEn) bk.summaryEn else bk.summaryKo, textPaint, maxRw).take(4)) {
            canvas.drawText(line, rx, cy, textPaint)
            cy += 34f * s
        }

        val qTop = covY1 + 392f * s
        drawCard(canvas, RectF(rx, qTop, w - m - 44f * s, covY2), 14f * s, Color.rgb(246, 246, 246), 2f * s)
        textPaint.color = Color.rgb(80, 80, 80)
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 20f * s
        canvas.drawText(if (isEn) "Notable Line" else "책 속의 한 문장", rx + 22f * s, qTop + 32f * s, textPaint)

        textPaint.color = Color.BLACK
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 24f * s
        val qStr = "“${if (isEn) bk.quoteEn else bk.quoteKo}”"
        canvas.drawText(ellipsizeText(qStr, textPaint, maxRw - 44f * s), rx + 22f * s, qTop + 68f * s, textPaint)

        strokePaint.color = Color.rgb(210, 210, 210)
        strokePaint.strokeWidth = 2f * s
        canvas.drawLine(m + 48f * s, boxBot - 104f * s, w - m - 48f * s, boxBot - 104f * s, strokePaint)
        textPaint.color = Color.rgb(70, 70, 70)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 23f * s
        val kwPrefix = if (isEn) "· Keywords: " else "· 추천 키워드: "
        canvas.drawText(kwPrefix + (if (isEn) bk.keywordsEn else bk.keywordsKo), m + 48f * s, boxBot - 52f * s, textPaint)
    }

    private fun drawMoviePage(canvas: Canvas, w: Float, m: Float, s: Float) {
        val isEn = settings.language == "en"
        val boxTop = 906f * s
        val boxBot = 1776f * s
        drawCard(canvas, RectF(m, boxTop, w - m, boxBot), 24f * s, Color.WHITE, 3f * s)

        textPaint.color = Color.BLACK
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 32f * s
        canvas.drawText(if (isEn) "Today's Movie Spotlight" else "오늘의 영화 소개", m + 36f * s, boxTop + 56f * s, textPaint)

        val mv = movieItem
        val genre = if (isEn) (mv?.genreEn ?: "Movie") else (mv?.genreKo ?: "영화")
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 21f * s
        val gw = textPaint.measureText(genre) + 32f * s
        val gRect = RectF(w - m - 32f * s - gw, boxTop + 22f * s, w - m - 32f * s, boxTop + 66f * s)
        drawCard(canvas, gRect, 12f * s, Color.rgb(24, 24, 24), 2f * s)
        textPaint.color = Color.WHITE
        canvas.drawText(genre, gRect.left + 16f * s, boxTop + 52f * s, textPaint)

        strokePaint.color = Color.rgb(160, 160, 160)
        strokePaint.strokeWidth = 2f * s
        canvas.drawLine(m + 28f * s, boxTop + 84f * s, w - m - 28f * s, boxTop + 84f * s, strokePaint)

        if (mv == null) {
            textPaint.color = Color.rgb(80, 80, 80)
            textPaint.typeface = Typeface.DEFAULT
            textPaint.textSize = 32f * s
            canvas.drawText(if (isEn) "Loading movie spotlight..." else "영화 소개를 불러오는 중입니다...", m + 64f * s, boxTop + 260f * s, textPaint)
            return
        }

        // Left Movie Poster
        val px1 = m + 48f * s
        val py1 = boxTop + 124f * s
        val pw = 330f * s
        val ph = 480f * s
        val px2 = px1 + pw
        val py2 = py1 + ph
        drawCard(canvas, RectF(px1, py1, px2, py2), 14f * s, Color.rgb(22, 22, 22), 3f * s)

        val bmp = movieBitmap
        if (bmp != null && !bmp.isRecycled) {
            val dest = RectF(px1 + 10f * s, py1 + 10f * s, px2 - 10f * s, py2 - 10f * s)
            canvas.drawBitmap(bmp, Rect(0, 0, bmp.width, bmp.height), dest, monoPaint)
        } else {
            // Film perforations
            fillPaint.color = Color.rgb(235, 235, 235)
            fillPaint.style = Paint.Style.FILL
            var fx = px1 + 22f * s
            while (fx < px2 - 22f * s) {
                canvas.drawRect(fx, py1 + 12f * s, fx + 18f * s, py1 + 22f * s, fillPaint)
                canvas.drawRect(fx, py2 - 22f * s, fx + 18f * s, py2 - 12f * s, fillPaint)
                fx += 34f * s
            }
            // Stylized sun/moon + mountains
            fillPaint.color = Color.rgb(235, 235, 235)
            canvas.drawCircle(px1 + 165f * s, py1 + 138f * s, 58f * s, fillPaint)
            val m1 = Path().apply {
                moveTo(px1 + 16f * s, py1 + 310f * s)
                lineTo(px1 + 125f * s, py1 + 185f * s)
                lineTo(px1 + 240f * s, py1 + 310f * s)
                close()
            }
            fillPaint.color = Color.rgb(120, 120, 120)
            canvas.drawPath(m1, fillPaint)
            val m2 = Path().apply {
                moveTo(px1 + 120f * s, py1 + 310f * s)
                lineTo(px1 + 245f * s, py1 + 205f * s)
                lineTo(px2 - 16f * s, py1 + 310f * s)
                close()
            }
            fillPaint.color = Color.rgb(165, 165, 165)
            canvas.drawPath(m2, fillPaint)

            textPaint.color = Color.WHITE
            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.textSize = 38f * s
            canvas.drawText(mv.posterTitleTop, px1 + 30f * s, py1 + 368f * s, textPaint)
            canvas.drawText(mv.posterTitleBottom, px1 + 30f * s, py1 + 414f * s, textPaint)
            textPaint.color = Color.rgb(195, 195, 195)
            textPaint.typeface = Typeface.DEFAULT
            textPaint.textSize = 18f * s
            canvas.drawText(ellipsizeText(mv.originalTitleYear, textPaint, pw - 60f * s), px1 + 30f * s, py1 + 446f * s, textPaint)
        }

        // Right Movie Details
        val rx = px2 + 52f * s
        val maxRw = w - m - 44f * s - rx
        textPaint.color = Color.BLACK
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 38f * s
        canvas.drawText(ellipsizeText(if (isEn) mv.titleEn else mv.titleKo, textPaint, maxRw), rx, py1 + 38f * s, textPaint)

        textPaint.color = Color.rgb(80, 80, 80)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 24f * s
        canvas.drawText(ellipsizeText(mv.originalTitleYear, textPaint, maxRw), rx, py1 + 82f * s, textPaint)

        val chips = listOf(
            mv.rating,
            if (isEn) mv.directorEn else mv.directorKo,
            if (isEn) mv.runtimeRatingEn else mv.runtimeRatingKo
        )
        var cx = rx
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 20f * s
        for (ch in chips) {
            val tw = textPaint.measureText(ch) + 28f * s
            if (cx + tw > w - m - 36f * s) break
            drawCard(canvas, RectF(cx, py1 + 110f * s, cx + tw, py1 + 148f * s), 8f * s, Color.rgb(240, 240, 240), 2f * s)
            textPaint.color = Color.rgb(20, 20, 20)
            canvas.drawText(ch, cx + 14f * s, py1 + 136f * s, textPaint)
            cx += tw + 12f * s
        }

        textPaint.color = Color.rgb(20, 20, 20)
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 24f * s
        canvas.drawText(if (isEn) "■ Synopsis & Highlights" else "■ 작품 줄거리 & 감상 포인트", rx, py1 + 202f * s, textPaint)

        textPaint.color = Color.rgb(45, 45, 45)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 24f * s
        var cy = py1 + 244f * s
        for (line in wrapText(if (isEn) mv.synopsisEn else mv.synopsisKo, textPaint, maxRw).take(4)) {
            canvas.drawText(line, rx, cy, textPaint)
            cy += 34f * s
        }

        val qTop = py1 + 392f * s
        drawCard(canvas, RectF(rx, qTop, w - m - 44f * s, py2), 14f * s, Color.rgb(246, 246, 246), 2f * s)
        textPaint.color = Color.rgb(80, 80, 80)
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 20f * s
        canvas.drawText(if (isEn) "Why Watch" else "추천 관람 포인트", rx + 22f * s, qTop + 32f * s, textPaint)

        textPaint.color = Color.BLACK
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 23f * s
        canvas.drawText(ellipsizeText(if (isEn) mv.highlightEn else mv.highlightKo, textPaint, maxRw - 44f * s), rx + 22f * s, qTop + 68f * s, textPaint)

        strokePaint.color = Color.rgb(210, 210, 210)
        strokePaint.strokeWidth = 2f * s
        canvas.drawLine(m + 48f * s, boxBot - 104f * s, w - m - 48f * s, boxBot - 104f * s, strokePaint)
        textPaint.color = Color.rgb(70, 70, 70)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 23f * s
        canvas.drawText("· " + (if (isEn) mv.castOttEn else mv.castOttKo), m + 48f * s, boxBot - 52f * s, textPaint)
    }

    private fun drawEconomyPage(canvas: Canvas, w: Float, m: Float, s: Float) {
        val isEn = settings.language == "en"
        val boxTop = 906f * s
        val boxBot = 1776f * s
        drawCard(canvas, RectF(m, boxTop, w - m, boxBot), 24f * s, Color.WHITE, 3f * s)

        textPaint.color = Color.BLACK
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 32f * s
        canvas.drawText(if (isEn) "Key Economic Indicators" else "주요 경제지표", m + 36f * s, boxTop + 56f * s, textPaint)

        textPaint.color = Color.rgb(85, 85, 85)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 22f * s
        val sub = if (isEn) "7-Day Trend" else "최근 7일 추이 · 기준"
        canvas.drawText(sub, w - m - 36f * s - textPaint.measureText(sub), boxTop + 54f * s, textPaint)

        strokePaint.color = Color.rgb(160, 160, 160)
        strokePaint.strokeWidth = 2f * s
        canvas.drawLine(m + 28f * s, boxTop + 84f * s, w - m - 28f * s, boxTop + 84f * s, strokePaint)

        if (economyList.isEmpty()) {
            textPaint.color = Color.rgb(80, 80, 80)
            textPaint.typeface = Typeface.DEFAULT
            textPaint.textSize = 32f * s
            canvas.drawText(if (isEn) "Loading indicators..." else "경제지표를 불러오는 중입니다...", m + 64f * s, boxTop + 260f * s, textPaint)
            return
        }

        val cols = 2
        val rows = 3
        val gapX = 24f * s
        val gapY = 22f * s
        val padX = 36f * s
        val padTop = 108f * s
        val padBot = 28f * s
        val cardW = (w - 2f * m - 2f * padX - gapX) / cols
        val cardH = (boxBot - boxTop - padTop - padBot - gapY * (rows - 1)) / rows

        val count = minOf(economyList.size, 6)
        for (idx in 0 until count) {
            val ind = economyList[idx]
            val r = idx / cols
            val c = idx % cols
            val x1 = m + padX + c * (cardW + gapX)
            val y1 = boxTop + padTop + r * (cardH + gapY)
            val x2 = x1 + cardW
            val y2 = y1 + cardH

            drawCard(canvas, RectF(x1, y1, x2, y2), 18f * s, Color.rgb(249, 249, 249), 2f * s)

            textPaint.color = Color.rgb(55, 55, 55)
            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.textSize = 24f * s
            canvas.drawText(if (isEn) ind.nameEn else ind.nameKo, x1 + 24f * s, y1 + 42f * s, textPaint)

            textPaint.color = Color.BLACK
            textPaint.textSize = 40f * s
            canvas.drawText(ind.valueStr, x1 + 24f * s, y1 + 98f * s, textPaint)

            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.textSize = 19f * s
            val dw = textPaint.measureText(ind.diffStr) + 24f * s
            val diffRect = RectF(x1 + 24f * s, y1 + 116f * s, minOf(x1 + 24f * s + dw, x1 + 286f * s), y1 + 150f * s)
            drawCard(canvas, diffRect, 8f * s, if (ind.isUp) Color.rgb(24, 24, 24) else Color.rgb(232, 232, 232), 2f * s)
            textPaint.color = if (ind.isUp) Color.WHITE else Color.rgb(20, 20, 20)
            canvas.drawText(ellipsizeText(ind.diffStr, textPaint, diffRect.width() - 16f * s), diffRect.left + 12f * s, y1 + 140f * s, textPaint)

            // Sparkline graph on right side
            val gx1 = x1 + 310f * s
            val gy1 = y1 + 28f * s
            val gx2 = x2 - 26f * s
            val gy2 = y2 - 28f * s
            drawCard(canvas, RectF(gx1 - 12f * s, gy1 - 10f * s, gx2 + 12f * s, gy2 + 10f * s), 12f * s, Color.WHITE, 1f * s)

            if (ind.history.size >= 2) {
                val sMin = ind.history.minOrNull() ?: 0f
                val sMax = ind.history.maxOrNull() ?: 1f
                val sSpan = maxOf(0.0001f, sMax - sMin)
                val ptsX = FloatArray(ind.history.size)
                val ptsY = FloatArray(ind.history.size)
                for (i in ind.history.indices) {
                    ptsX[i] = gx1 + i * (gx2 - gx1) / (ind.history.size - 1)
                    ptsY[i] = gy2 - ((ind.history[i] - sMin) / sSpan) * (gy2 - gy1)
                }

                // Dashed baseline
                val midY = (gy1 + gy2) / 2f
                strokePaint.color = Color.rgb(220, 220, 220)
                strokePaint.strokeWidth = 1f * s
                var bx = gx1
                while (bx < gx2) {
                    canvas.drawLine(bx, midY, minOf(bx + 4f * s, gx2), midY, strokePaint)
                    bx += 10f * s
                }

                strokePaint.color = Color.rgb(20, 20, 20)
                strokePaint.strokeWidth = 4f * s
                for (i in 0 until ind.history.size - 1) {
                    canvas.drawLine(ptsX[i], ptsY[i], ptsX[i + 1], ptsY[i + 1], strokePaint)
                }
                for (i in ind.history.indices) {
                    if (i == ind.history.lastIndex) {
                        fillPaint.color = Color.BLACK
                        fillPaint.style = Paint.Style.FILL
                        canvas.drawCircle(ptsX[i], ptsY[i], 6f * s, fillPaint)
                    } else {
                        fillPaint.color = Color.WHITE
                        fillPaint.style = Paint.Style.FILL
                        canvas.drawCircle(ptsX[i], ptsY[i], 3f * s, fillPaint)
                        strokePaint.color = Color.rgb(40, 40, 40)
                        strokePaint.strokeWidth = 2f * s
                        canvas.drawCircle(ptsX[i], ptsY[i], 3f * s, strokePaint)
                    }
                }
            }
        }
    }
}
