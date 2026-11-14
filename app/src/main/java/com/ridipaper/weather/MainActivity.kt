package com.ridipaper.weather

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
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
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.ridipaper.weather.data.ContentRepository
import com.ridipaper.weather.data.RegionStore
import com.ridipaper.weather.data.SettingsStore
import com.ridipaper.weather.data.WeatherRepository
import com.ridipaper.weather.model.DashboardSettings
import com.ridipaper.weather.model.NewsItem
import com.ridipaper.weather.model.QuoteItem
import com.ridipaper.weather.model.WeatherData
import com.ridipaper.weather.service.LibraryWatcherService
import com.ridipaper.weather.ui.DashboardView
import kotlin.concurrent.thread

class MainActivity : Activity(), DashboardView.Listener {

    companion object {
        const val EXTRA_FROM_LIBRARY = "from_library"
        private const val TICK_MS = 30L * 1000L
        private const val PAGE_COUNT = 3
    }

    private lateinit var dashboardView: DashboardView
    private lateinit var settingsStore: SettingsStore
    private lateinit var settings: DashboardSettings
    private var currentWeather: WeatherData? = null
    private var mainQuote: QuoteItem? = null
    private var subQuote: QuoteItem? = null
    private var newsList: List<NewsItem> = emptyList()
    private var currentPage: Int = 0
    private var wakeLock: PowerManager.WakeLock? = null

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
            if (settings.pageAutoRotateMinutes > 0 &&
                now - lastPageChangeMs >= settings.pageAutoRotateMinutes * 60_000L
            ) {
                currentPage = (currentPage + 1) % PAGE_COUNT
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
        dashboardView.updateState(
            weather = currentWeather,
            mainQuote = mainQuote,
            subQuote = subQuote,
            news = newsList,
            currentPage = currentPage,
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
        currentPage = (currentPage - 1 + PAGE_COUNT) % PAGE_COUNT
        lastPageChangeMs = System.currentTimeMillis()
        render()
    }

    override fun onNextPageClick() {
        resetKeepScreenOnTimer()
        currentPage = (currentPage + 1) % PAGE_COUNT
        lastPageChangeMs = System.currentTimeMillis()
        render()
    }

    override fun onRefreshClick() {
        resetKeepScreenOnTimer()
        refreshWeather(force = true)
    }

    override fun onRegionClick() {
        resetKeepScreenOnTimer()
        val items = arrayOf(
            "현재 위치 자동 감지 (Wi-Fi)",
            "시 · 구 · 동 단계별 선택",
            "동네 검색 (직접 입력)"
        )

        AlertDialog.Builder(this)
            .setTitle("지역 선택")
            .setItems(items) { _, which ->
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
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showSidoPicker() {
        val sidos = RegionStore.getSidos(this)
        if (sidos.isEmpty()) {
            Toast.makeText(this, "지역 데이터를 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("시 / 도 선택")
            .setItems(sidos.toTypedArray()) { _, idx ->
                val sido = sidos[idx]
                val sigungus = RegionStore.getSigungus(this, sido)
                if (sigungus.size == 1) {
                    showDongPicker(sido, sigungus[0])
                } else {
                    showSigunguPicker(sido)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showSigunguPicker(sido: String) {
        val sigungus = RegionStore.getSigungus(this, sido)
        AlertDialog.Builder(this)
            .setTitle("$sido · 시 / 군 / 구 선택")
            .setItems(sigungus.toTypedArray()) { _, idx ->
                showDongPicker(sido, sigungus[idx])
            }
            .setNeutralButton("이전") { _, _ -> showSidoPicker() }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showDongPicker(sido: String, sigungu: String) {
        val dongs = RegionStore.getDongs(this, sido, sigungu)
        val labels = dongs.map { preset ->
            val prefix = if (sido == sigungu) "$sido " else "$sido $sigungu "
            preset.name.removePrefix(prefix)
        }.toTypedArray()
        val title = if (sido == sigungu) "$sido · 읍 / 면 / 동 선택" else "$sido $sigungu · 읍 / 면 / 동 선택"
        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(labels) { _, idx ->
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
            .show()
    }

    private fun showRegionSearchDialog() {
        val isEn = settings.language == "en"
        val input = EditText(this).apply {
            hint = if (isEn) "e.g. Tokyo, London, New York, Gangnam" else "예: 역삼동, 분당구 정자동, Tokyo, London"
            setSingleLine(true)
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 12)
            addView(input)
        }
        AlertDialog.Builder(this)
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
                                AlertDialog.Builder(this)
                                    .setTitle(if (isEn) "Select Location" else "검색 결과 선택")
                                    .setItems(names) { _, idx ->
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
                                    .show()
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
            .show()
    }

    override fun onSettingsClick() {
        resetKeepScreenOnTimer()
        val isEn = settings.language == "en"
        val hourChoices = arrayOf(1, 2, 3, 4, 6, 8, 12)
        val hourLabels = hourChoices.map { if (isEn) "${it}h" else "${it}시간" }.toTypedArray()
        val initialIdx = hourChoices.indexOf(settings.keepScreenHours).let { if (it >= 0) it else 0 }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 24)
        }

        val langChoices = arrayOf("ko", "en")
        val langLabels = arrayOf("한국어 (Korean)", "English")
        val langIdx = langChoices.indexOf(settings.language).let { if (it >= 0) it else 0 }
        val tvLang = TextView(this).apply {
            text = if (isEn) "Language" else "언어 (Language)"
            textSize = 16f
        }
        val spinnerLang = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, langLabels)
            setSelection(langIdx)
        }

        val tvKeep = TextView(this).apply {
            text = if (isEn) "Keep Screen On (1-12h)" else "화면 유지 시간 (기기 절전 무시, 1~12시간)"
            textSize = 16f
            setPadding(0, 24, 0, 4)
        }
        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, hourLabels)
            setSelection(initialIdx)
        }

        val initialBrightStep = ((settings.brightnessPercent + 5) / 10).coerceIn(0, 10)
        val tvBright = TextView(this).apply {
            text = if (isEn) "Brightness: ${initialBrightStep * 10}%" else "디스플레이 밝기: ${initialBrightStep * 10}%"
            textSize = 16f
            setPadding(0, 28, 0, 12)
        }
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
        val wc1Idx = findWcIndex(settings.worldClock1)
        val wc2Idx = findWcIndex(settings.worldClock2)

        val tvWc1 = TextView(this).apply {
            text = if (isEn) "World Clock 1" else "세계시간 1"
            textSize = 16f
            setPadding(0, 24, 0, 4)
        }
        val spinnerWc1 = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, wcLabels)
            setSelection(wc1Idx)
        }

        val tvWc2 = TextView(this).apply {
            text = if (isEn) "World Clock 2" else "세계시간 2"
            textSize = 16f
            setPadding(0, 18, 0, 4)
        }
        val spinnerWc2 = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, wcLabels)
            setSelection(wc2Idx)
        }

        val refreshChoices = arrayOf(30, 60, 120, 180, 360)
        val refreshLabels = if (isEn) {
            arrayOf("30 min", "1 hour", "2 hours", "3 hours", "6 hours")
        } else {
            arrayOf("30분", "1시간", "2시간", "3시간", "6시간")
        }
        val refreshIdx = refreshChoices.indexOf(settings.refreshIntervalMinutes).let { if (it >= 0) it else 0 }

        val tvRefresh = TextView(this).apply {
            text = if (isEn) "Auto Refresh Interval (Wi-Fi)" else "업데이트 주기 (Wi-Fi 연결 시)"
            textSize = 16f
            setPadding(0, 24, 0, 4)
        }
        val spinnerRefresh = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, refreshLabels)
            setSelection(refreshIdx)
        }

        val rotateChoices = arrayOf(0, 1, 5, 10, 30, 60)
        val rotateLabels = if (isEn) {
            arrayOf("Off", "Every 1 min", "Every 5 min", "Every 10 min", "Every 30 min", "Every 1 hour")
        } else {
            arrayOf("사용 안 함", "1분마다", "5분마다", "10분마다", "30분마다", "1시간마다")
        }
        val rotateIdx = rotateChoices.indexOf(settings.pageAutoRotateMinutes).let { if (it >= 0) it else 0 }
        val tvRotate = TextView(this).apply {
            text = if (isEn) "Auto Switch Page (Weather/Quotes/News)" else "하단 페이지 자동 전환 (날씨·명언·뉴스)"
            textSize = 16f
            setPadding(0, 24, 0, 4)
        }
        val spinnerRotate = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, rotateLabels)
            setSelection(rotateIdx)
        }

        container.addView(tvLang)
        container.addView(spinnerLang)
        container.addView(tvKeep)
        container.addView(spinner)
        container.addView(tvRefresh)
        container.addView(spinnerRefresh)
        container.addView(tvRotate)
        container.addView(spinnerRotate)
        container.addView(tvBright)
        container.addView(seekBar)
        container.addView(tvWc1)
        container.addView(spinnerWc1)
        container.addView(tvWc2)
        container.addView(spinnerWc2)

        AlertDialog.Builder(this)
            .setTitle(if (isEn) "Dashboard Settings" else "대시보드 설정")
            .setView(container)
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
                    language = chosenLang
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
            .show()
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
