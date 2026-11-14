package com.ridipaper.weather

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.ridipaper.weather.data.ContentRepository
import com.ridipaper.weather.data.RegionStore
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
import com.ridipaper.weather.service.LibraryWatcherService
import com.ridipaper.weather.ui.DashboardView
import kotlin.concurrent.thread

class MainActivity : Activity(), DashboardView.Listener {

    companion object {
        const val EXTRA_FROM_LIBRARY = "from_library"
        private const val TICK_MS = 30L * 1000L
    }

    private lateinit var dashboardView: DashboardView
    private lateinit var settingsStore: SettingsStore
    private lateinit var settings: DashboardSettings
    private var currentWeather: WeatherData? = null
    private var mainQuote: QuoteItem? = null
    private var subQuote: QuoteItem? = null
    private var newsList: List<NewsItem> = emptyList()
    private var bookItem: BookItem? = null
    private var movieItem: MovieItem? = null
    private var economyList: List<EconomyIndicator> = emptyList()
    private var bookBitmap: Bitmap? = null
    private var movieBitmap: Bitmap? = null
    private var currentPage: Int = 0
    private var wakeLock: PowerManager.WakeLock? = null

    private fun enabledPages(): List<PageType> {
        val list = ArrayList<PageType>(6)
        list.add(PageType.WEATHER) // Always fixed ON
        if (settings.showQuotesPage) list.add(PageType.QUOTES)
        if (settings.showNewsPage) list.add(PageType.NEWS)
        if (settings.showBookPage) list.add(PageType.BOOK)
        if (settings.showMoviePage) list.add(PageType.MOVIE)
        if (settings.showEconomyPage) list.add(PageType.ECONOMY)
        return list
    }

    private val handler = Handler(Looper.getMainLooper())
    private var keepAliveDeadlineMs: Long = 0L
    private var lastFetchMs: Long = 0L
    private var lastPageChangeMs: Long = System.currentTimeMillis()
    private var isFetching = false

    private val tickRunnable = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            if (keepAliveDeadlineMs > 0L && now >= keepAliveDeadlineMs) {
                releaseKeepScreenOn()
            }
            val refreshMs = settings.refreshIntervalMinutes * 60_000L
            if (isWifiConnected() && now - lastFetchMs >= refreshMs) {
                refreshWeather(force = false)
            }
            val pageCount = enabledPages().size
            if (settings.pageAutoRotateMinutes > 0 &&
                pageCount > 1 &&
                now - lastPageChangeMs >= settings.pageAutoRotateMinutes * 60_000L
            ) {
                currentPage = (currentPage + 1) % pageCount
                lastPageChangeMs = now
            }
            render()
            handler.postDelayed(this, TICK_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsStore = SettingsStore(this)
        settings = settingsStore.load()

        dashboardView = DashboardView(this).apply {
            listener = this@MainActivity
        }
        setContentView(dashboardView)

        startService(Intent(this, LibraryWatcherService::class.java))
        applyBrightness(settings.brightnessPercent)
        resetKeepScreenOnTimer()
        refreshWeather(force = true)
    }

    override fun onResume() {
        super.onResume()
        resetKeepScreenOnTimer()
        handler.removeCallbacks(tickRunnable)
        handler.post(tickRunnable)
    }

    override fun onPause() {
        handler.removeCallbacks(tickRunnable)
        releaseKeepScreenOn()
        super.onPause()
    }

    override fun onBackPressed() {
        onCloseClick()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_PAGE_UP,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                onPrevPageClick()
                true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_PAGE_DOWN,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                onNextPageClick()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun resetKeepScreenOnTimer() {
        keepAliveDeadlineMs = System.currentTimeMillis() + settings.keepScreenHours * 3600_000L
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        try {
            if (wakeLock == null) {
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                @Suppress("DEPRECATION")
                wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "PaperProWeather:KeepOn")
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(settings.keepScreenHours * 3600_000L)
            }
        } catch (_: Exception) {
        }
    }

    private fun releaseKeepScreenOn() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        }
    }

    private fun applyBrightness(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        val lp = window.attributes
        lp.screenBrightness = (clamped / 100f).coerceAtLeast(0.02f)
        window.attributes = lp

        try {
            val sysVal = (clamped * 255 / 100).coerceIn(5, 255)
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0)
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, sysVal)
        } catch (_: Exception) {
        }
    }

    private fun isWifiConnected(): Boolean {
        return try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            @Suppress("DEPRECATION")
            val info: NetworkInfo? = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            info?.isConnected == true
        } catch (_: Exception) {
            false
        }
    }

    private fun remainingMinutes(): Int {
        val remMs = keepAliveDeadlineMs - System.currentTimeMillis()
        return if (remMs > 0) ((remMs + 59_999L) / 60_000L).toInt() else 0
    }

    private fun render(statusMsg: String? = null) {
        val pages = enabledPages()
        if (currentPage >= pages.size) currentPage = 0
        dashboardView.updateState(
            weather = currentWeather,
            mainQuote = mainQuote,
            subQuote = subQuote,
            news = newsList,
            book = bookItem,
            movie = movieItem,
            economy = economyList,
            bookBitmap = bookBitmap,
            movieBitmap = movieBitmap,
            activePageType = pages[currentPage],
            pageNumber = currentPage + 1,
            totalPages = pages.size,
            settings = settings,
            wifiConnected = isWifiConnected(),
            remainingMinutes = remainingMinutes(),
            statusMessage = statusMsg
        )
    }

    private fun refreshWeather(force: Boolean) {
        if (isFetching) return
        val isEn = settings.language == "en"
        if (!isWifiConnected()) {
            render(if (force) (if (isEn) "Wi-Fi connection required" else "Wi-Fi 연결이 필요합니다") else null)
            return
        }
        isFetching = true
        render(if (isEn) "Updating weather · quotes · news..." else "날씨 · 명언 · 뉴스 갱신 중...")
        val snapshot = settings
        val fallbackCountry = currentWeather?.countryCode ?: snapshot.manualCountryCode
        thread {
            var weatherErr: Exception? = null
            var fetchedWeather: WeatherData? = null
            try {
                fetchedWeather = WeatherRepository.fetchWeather(snapshot)
            } catch (e: Exception) {
                weatherErr = e
            }

            val newsCountry = fetchedWeather?.countryCode ?: fallbackCountry

            val fetchedQuotes = try {
                ContentRepository.fetchQuotes(snapshot.language)
            } catch (_: Exception) {
                null
            }

            val fetchedNews = try {
                ContentRepository.fetchNews(newsCountry, snapshot.language)
            } catch (_: Exception) {
                null
            }

            val fetchedBook = ContentRepository.fetchBook()
            val fetchedBookBmp = ContentRepository.fetchBitmap(fetchedBook.coverUrl)
            val fetchedMovie = ContentRepository.fetchMovie()
            val fetchedMovieBmp = ContentRepository.fetchBitmap(fetchedMovie.posterUrl)
            val fetchedEconomy = ContentRepository.fetchEconomyIndicators()

            handler.post {
                isFetching = false
                if (fetchedWeather != null) {
                    currentWeather = fetchedWeather
                    lastFetchMs = System.currentTimeMillis()
                }
                if (fetchedQuotes != null) {
                    mainQuote = fetchedQuotes.first
                    subQuote = fetchedQuotes.second
                }
                if (fetchedNews != null && fetchedNews.isNotEmpty()) {
                    newsList = fetchedNews
                }
                bookItem = fetchedBook
                if (fetchedBookBmp != null) bookBitmap = fetchedBookBmp
                movieItem = fetchedMovie
                if (fetchedMovieBmp != null) movieBitmap = fetchedMovieBmp
                if (fetchedEconomy.isNotEmpty()) {
                    economyList = fetchedEconomy
                }
                if (weatherErr != null && fetchedWeather == null) {
                    val errPrefix = if (snapshot.language == "en") "Update failed: " else "갱신 실패: "
                    val errFallback = if (snapshot.language == "en") "Network error" else "네트워크 오류"
                    render(errPrefix + (weatherErr.message ?: errFallback))
                } else {
                    render()
                }
            }
        }
    }

    override fun onPrevPageClick() {
        resetKeepScreenOnTimer()
        val count = enabledPages().size
        currentPage = (currentPage - 1 + count) % count
        lastPageChangeMs = System.currentTimeMillis()
        render()
    }

    override fun onNextPageClick() {
        resetKeepScreenOnTimer()
        val count = enabledPages().size
        currentPage = (currentPage + 1) % count
        lastPageChangeMs = System.currentTimeMillis()
        render()
    }

    override fun onRefreshClick() {
        resetKeepScreenOnTimer()
        refreshWeather(force = true)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

    private fun makeUniformListAdapter(items: Array<String>): ArrayAdapter<String> {
        return object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items) {
            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val tv = super.getView(position, convertView, parent) as TextView
                tv.textSize = 15f
                tv.setTextColor(android.graphics.Color.BLACK)
                tv.setPadding(dp(18), dp(12), dp(18), dp(12))
                return tv
            }
        }
    }

    private fun showUniformDialog(dialog: AlertDialog, widthRatio: Float = 0.80f): AlertDialog {
        dialog.show()
        val targetW = (resources.displayMetrics.widthPixels * widthRatio).toInt()
        dialog.window?.setLayout(targetW, WindowManager.LayoutParams.WRAP_CONTENT)
        val parentId = resources.getIdentifier("parentPanel", "id", "android")
        if (parentId != 0) {
            dialog.findViewById<android.view.View>(parentId)?.let { panel ->
                panel.layoutParams = panel.layoutParams?.apply { width = targetW }
            }
        }
        dialog.listView?.let { lv ->
            lv.divider = android.graphics.drawable.ColorDrawable(android.graphics.Color.rgb(110, 110, 110))
            lv.dividerHeight = dp(1).coerceAtLeast(2)
        }
        return dialog
    }

    override fun onRegionClick() {
        resetKeepScreenOnTimer()
        val isEn = settings.language == "en"
        val items = if (isEn) {
            arrayOf(
                "Auto Detect Current Location (Wi-Fi)",
                "Select Region (Province / City / District)",
                "Search Location (Global / Domestic)"
            )
        } else {
            arrayOf(
                "현재 위치 자동 감지 (Wi-Fi)",
                "시 · 구 · 동 단계별 선택",
                "동네 검색 (직접 입력)"
            )
        }

        val dlg = AlertDialog.Builder(this)
            .setTitle(if (isEn) "Select Region" else "지역 선택")
            .setAdapter(makeUniformListAdapter(items)) { _, which ->
                when (which) {
                    0 -> {
                        settings = settings.copy(autoLocation = true)
                        settingsStore.save(settings)
                        refreshWeather(force = true)
                    }
                    1 -> showSidoPicker()
                    2 -> showRegionSearchDialog()
                }
            }
            .setNegativeButton(if (isEn) "Cancel" else "취소", null)
            .create()
        showUniformDialog(dlg)
    }

    private fun showSidoPicker() {
        val sidos = RegionStore.getSidos(this)
        if (sidos.isEmpty()) {
            Toast.makeText(this, "지역 데이터를 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
            return
        }
        val dlg = AlertDialog.Builder(this)
            .setTitle("시 / 도 선택")
            .setAdapter(makeUniformListAdapter(sidos.toTypedArray())) { _, idx ->
                val sido = sidos[idx]
                val sigungus = RegionStore.getSigungus(this, sido)
                if (sigungus.size == 1) {
                    showDongPicker(sido, sigungus[0])
                } else {
                    showSigunguPicker(sido)
                }
            }
            .setNegativeButton("취소", null)
            .create()
        showUniformDialog(dlg)
    }

    private fun showSigunguPicker(sido: String) {
        val sigungus = RegionStore.getSigungus(this, sido)
        val dlg = AlertDialog.Builder(this)
            .setTitle("$sido · 시 / 군 / 구 선택")
            .setAdapter(makeUniformListAdapter(sigungus.toTypedArray())) { _, idx ->
                showDongPicker(sido, sigungus[idx])
            }
            .setNeutralButton("이전") { _, _ -> showSidoPicker() }
            .setNegativeButton("취소", null)
            .create()
        showUniformDialog(dlg)
    }

    private fun showDongPicker(sido: String, sigungu: String) {
        val dongs = RegionStore.getDongs(this, sido, sigungu)
        val labels = dongs.map { preset ->
            val prefix = if (sido == sigungu) "$sido " else "$sido $sigungu "
            preset.name.removePrefix(prefix)
        }.toTypedArray()
        val title = if (sido == sigungu) "$sido · 읍 / 면 / 동 선택" else "$sido $sigungu · 읍 / 면 / 동 선택"
        val dlg = AlertDialog.Builder(this)
            .setTitle(title)
            .setAdapter(makeUniformListAdapter(labels)) { _, idx ->
                val chosen = dongs[idx]
                settings = settings.copy(
                    autoLocation = false,
                    manualCityName = chosen.name,
                    manualLat = chosen.lat,
                    manualLon = chosen.lon,
                    manualCountryCode = "KR"
                )
                settingsStore.save(settings)
                refreshWeather(force = true)
            }
            .setNeutralButton("이전") { _, _ ->
                if (RegionStore.getSigungus(this, sido).size == 1) {
                    showSidoPicker()
                } else {
                    showSigunguPicker(sido)
                }
            }
            .setNegativeButton("취소", null)
            .create()
        showUniformDialog(dlg)
    }

    private fun showRegionSearchDialog() {
        val isEn = settings.language == "en"
        val input = EditText(this).apply {
            hint = if (isEn) "e.g. Tokyo, London, New York, Gangnam" else "예: 역삼동, 분당구 정자동, Tokyo, London"
            textSize = 15f
            setSingleLine(true)
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(8))
            addView(input)
        }
        val searchDlg = AlertDialog.Builder(this)
            .setTitle(if (isEn) "Search Location" else "지역 검색 (국내/해외)")
            .setView(box)
            .setPositiveButton(if (isEn) "Search" else "검색") { _, _ ->
                val q = input.text?.toString()?.trim().orEmpty()
                if (q.isEmpty()) return@setPositiveButton
                if (!isWifiConnected()) {
                    Toast.makeText(
                        this,
                        if (isEn) "Wi-Fi connection required" else "Wi-Fi 연결이 필요합니다",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }
                render(if (isEn) "Searching '$q'..." else "'$q' 지역 검색 중...")
                val lang = settings.language
                thread {
                    try {
                        val found = WeatherRepository.searchKoreanLocations(q, lang)
                        handler.post {
                            render()
                            if (found.isEmpty()) {
                                Toast.makeText(
                                    this,
                                    if (isEn) "No results found" else "검색 결과가 없습니다",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                val names = found.map { "${it.name} (${it.countryCode})" }.toTypedArray()
                                val resDlg = AlertDialog.Builder(this)
                                    .setTitle(if (isEn) "Select Location" else "검색 결과 선택")
                                    .setAdapter(makeUniformListAdapter(names)) { _, idx ->
                                        val chosen = found[idx]
                                        settings = settings.copy(
                                            autoLocation = false,
                                            manualCityName = chosen.name,
                                            manualLat = chosen.lat,
                                            manualLon = chosen.lon,
                                            manualCountryCode = chosen.countryCode
                                        )
                                        settingsStore.save(settings)
                                        refreshWeather(force = true)
                                    }
                                    .setNegativeButton(if (isEn) "Cancel" else "취소", null)
                                    .create()
                                showUniformDialog(resDlg)
                            }
                        }
                    } catch (_: Exception) {
                        handler.post {
                            render()
                            Toast.makeText(
                                this,
                                if (isEn) "Location search failed" else "지역 검색에 실패했습니다",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            .setNegativeButton(if (isEn) "Cancel" else "취소", null)
            .create()
        showUniformDialog(searchDlg)
    }

    override fun onSettingsClick() {
        resetKeepScreenOnTimer()
        val isEn = settings.language == "en"
        val dm = resources.displayMetrics

        fun makeAdapter(items: Array<String>): ArrayAdapter<String> {
            return object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items) {
                override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                    val tv = super.getView(position, convertView, parent) as TextView
                    tv.textSize = 15f
                    tv.setTextColor(android.graphics.Color.BLACK)
                    return tv
                }

                override fun getDropDownView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                    val tv = super.getDropDownView(position, convertView, parent) as TextView
                    tv.textSize = 15f
                    tv.setTextColor(android.graphics.Color.BLACK)
                    return tv
                }
            }.apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }

        fun makeSpinner(items: Array<String>, selectedIdx: Int): Spinner {
            return Spinner(this).apply {
                adapter = makeAdapter(items)
                setSelection(selectedIdx)
                setPadding(dp(4), 0, dp(4), 0)
            }
        }

        fun makeLabel(text: String): TextView {
            return TextView(this).apply {
                this.text = text
                textSize = 15f
                setTextColor(android.graphics.Color.BLACK)
                setSingleLine(true)
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            minimumWidth = (dm.widthPixels * 0.76f).toInt()
            setPadding(dp(18), dp(10), dp(18), dp(8))
        }

        fun addRow(label: TextView, control: android.view.View) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(4), 0, dp(4))
            }
            val labelLp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.45f).apply {
                rightMargin = dp(8)
            }
            val ctrlLp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.55f)
            row.addView(label, labelLp)
            row.addView(control, ctrlLp)
            container.addView(row)
        }

        val langChoices = arrayOf("ko", "en")
        val langLabels = arrayOf("한국어 (Korean)", "English")
        val langIdx = langChoices.indexOf(settings.language).let { if (it >= 0) it else 0 }
        val spinnerLang = makeSpinner(langLabels, langIdx)
        addRow(makeLabel(if (isEn) "Language" else "언어 (Language)"), spinnerLang)

        val hourChoices = arrayOf(1, 2, 3, 4, 6, 8, 12)
        val hourLabels = hourChoices.map { if (isEn) "${it}h" else "${it}시간" }.toTypedArray()
        val initialIdx = hourChoices.indexOf(settings.keepScreenHours).let { if (it >= 0) it else 0 }
        val spinner = makeSpinner(hourLabels, initialIdx)
        addRow(makeLabel(if (isEn) "Keep Screen On" else "화면 유지 시간"), spinner)

        val refreshChoices = arrayOf(30, 60, 120, 180, 360)
        val refreshLabels = if (isEn) {
            arrayOf("30 min", "1 hour", "2 hours", "3 hours", "6 hours")
        } else {
            arrayOf("30분", "1시간", "2시간", "3시간", "6시간")
        }
        val refreshIdx = refreshChoices.indexOf(settings.refreshIntervalMinutes).let { if (it >= 0) it else 0 }
        val spinnerRefresh = makeSpinner(refreshLabels, refreshIdx)
        addRow(makeLabel(if (isEn) "Auto Refresh (Wi-Fi)" else "업데이트 주기 (Wi-Fi)"), spinnerRefresh)

        val rotateChoices = arrayOf(0, 1, 5, 10, 30, 60)
        val rotateLabels = if (isEn) {
            arrayOf("Off", "1 min", "5 min", "10 min", "30 min", "1 hour")
        } else {
            arrayOf("사용 안 함", "1분마다", "5분마다", "10분마다", "30분마다", "1시간마다")
        }
        val rotateIdx = rotateChoices.indexOf(settings.pageAutoRotateMinutes).let { if (it >= 0) it else 0 }
        val spinnerRotate = makeSpinner(rotateLabels, rotateIdx)
        addRow(makeLabel(if (isEn) "Auto Page Switch" else "하단 페이지 자동 전환"), spinnerRotate)

        val initialBrightStep = ((settings.brightnessPercent + 5) / 10).coerceIn(0, 10)
        val tvBright = makeLabel(
            if (isEn) "Brightness: ${initialBrightStep * 10}%" else "디스플레이 밝기: ${initialBrightStep * 10}%"
        )
        val seekBar = SeekBar(this).apply {
            max = 10
            progress = initialBrightStep
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    val percent = progress * 10
                    tvBright.text = if (isEn) "Brightness: ${percent}%" else "디스플레이 밝기: ${percent}%"
                    applyBrightness(percent)
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        addRow(tvBright, seekBar)

        val nowMs = System.currentTimeMillis()
        val wcLabels = SettingsStore.WORLD_CLOCKS.map {
            SettingsStore.formatWorldClockLabel(it, nowMs, settings.language)
        }.toTypedArray()
        fun findWcIndex(saved: String): Int {
            if (saved.isBlank()) return 0
            val parts = saved.split("|", limit = 2)
            return if (parts.size == 2) {
                SettingsStore.WORLD_CLOCKS.indexOfFirst { it.cityName == parts[0] && it.timeZoneId == parts[1] }
            } else {
                SettingsStore.WORLD_CLOCKS.indexOfFirst { it.timeZoneId == saved }
            }.let { if (it >= 0) it else 0 }
        }
        val spinnerWc1 = makeSpinner(wcLabels, findWcIndex(settings.worldClock1))
        val spinnerWc2 = makeSpinner(wcLabels, findWcIndex(settings.worldClock2))
        addRow(makeLabel(if (isEn) "World Clock 1" else "세계시간 1"), spinnerWc1)
        addRow(makeLabel(if (isEn) "World Clock 2" else "세계시간 2"), spinnerWc2)

        // Page visibility section (Weather fixed ON, other pages toggleable)
        val div = android.view.View(this).apply {
            setBackgroundColor(android.graphics.Color.rgb(180, 180, 180))
        }
        val divLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
            topMargin = dp(8)
            bottomMargin = dp(8)
        }
        container.addView(div, divLp)

        val secHeader = TextView(this).apply {
            text = if (isEn) "Lower Pages (Weather Fixed ON)" else "하단 페이지 표시 (날씨 고정 · 선택 토글)"
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.BLACK)
            setPadding(0, dp(2), 0, dp(6))
        }
        container.addView(secHeader)

        val fixedLabel = TextView(this).apply {
            text = if (isEn) "Fixed ON" else "고정 ON"
            textSize = 14f
            setTextColor(android.graphics.Color.rgb(90, 90, 90))
            gravity = Gravity.END
        }
        addRow(makeLabel(if (isEn) "Hourly & 7-Day Weather" else "시간별 · 주간 날씨"), fixedLabel)

        fun makeSwitch(checked: Boolean): Switch {
            return Switch(this).apply {
                isChecked = checked
            }
        }

        val swQuotes = makeSwitch(settings.showQuotesPage)
        val swNews = makeSwitch(settings.showNewsPage)
        val swBook = makeSwitch(settings.showBookPage)
        val swMovie = makeSwitch(settings.showMoviePage)
        val swEconomy = makeSwitch(settings.showEconomyPage)

        addRow(makeLabel(if (isEn) "Daily Quotes" else "오늘의 명언 (영화·애니 포함)"), swQuotes)
        addRow(makeLabel(if (isEn) "Top News Headlines" else "주요 뉴스 헤드라인"), swNews)
        addRow(makeLabel(if (isEn) "Today's Book Pick" else "오늘의 책 추천"), swBook)
        addRow(makeLabel(if (isEn) "Today's Movie Spotlight" else "오늘의 영화 소개"), swMovie)
        addRow(makeLabel(if (isEn) "Key Economic Indicators" else "주요 경제지표 (그래프)"), swEconomy)

        val scrollView = ScrollView(this).apply {
            addView(container)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (isEn) "Dashboard Settings" else "대시보드 설정")
            .setView(scrollView)
            .setPositiveButton(if (isEn) "Save" else "저장") { _, _ ->
                val chosenLang = langChoices[spinnerLang.selectedItemPosition]
                val langChanged = chosenLang != settings.language
                val chosenHours = hourChoices[spinner.selectedItemPosition]
                val chosenRefresh = refreshChoices[spinnerRefresh.selectedItemPosition]
                val chosenRotate = rotateChoices[spinnerRotate.selectedItemPosition]
                val wc1 = SettingsStore.WORLD_CLOCKS[spinnerWc1.selectedItemPosition]
                val wc2 = SettingsStore.WORLD_CLOCKS[spinnerWc2.selectedItemPosition]
                val chosenWc1 = if (wc1.timeZoneId.isBlank()) "" else "${wc1.cityName}|${wc1.timeZoneId}"
                val chosenWc2 = if (wc2.timeZoneId.isBlank()) "" else "${wc2.cityName}|${wc2.timeZoneId}"
                settings = settings.copy(
                    keepScreenHours = chosenHours,
                    refreshIntervalMinutes = chosenRefresh,
                    pageAutoRotateMinutes = chosenRotate,
                    brightnessPercent = seekBar.progress * 10,
                    worldClock1 = chosenWc1,
                    worldClock2 = chosenWc2,
                    language = chosenLang,
                    showQuotesPage = swQuotes.isChecked,
                    showNewsPage = swNews.isChecked,
                    showBookPage = swBook.isChecked,
                    showMoviePage = swMovie.isChecked,
                    showEconomyPage = swEconomy.isChecked
                )
                settingsStore.save(settings)
                lastPageChangeMs = System.currentTimeMillis()
                applyBrightness(settings.brightnessPercent)
                resetKeepScreenOnTimer()
                if (langChanged) {
                    refreshWeather(force = true)
                } else {
                    render()
                }
            }
            .setNegativeButton(if (isEn) "Cancel" else "취소") { _, _ ->
                applyBrightness(settings.brightnessPercent)
            }
            .create()

        showUniformDialog(dialog, 0.80f)
    }

    override fun onCloseClick() {
        releaseKeepScreenOn()
        thread {
            var launched = false
            try {
                val proc = Runtime.getRuntime().exec(
                    arrayOf(
                        "su", "-c",
                        "am start -n com.ridi.paper/com.ridi.books.viewer.main.activity.MainActivityPaper --activity-clear-top --activity-single-top"
                    )
                )
                launched = (proc.waitFor() == 0)
            } catch (_: Exception) {
            }
            if (!launched) {
                try {
                    val launch = packageManager.getLaunchIntentForPackage("com.ridi.paper")
                    if (launch != null) {
                        launch.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(launch)
                    }
                } catch (_: Exception) {
                }
            }
            handler.post { finish() }
        }
    }
}
