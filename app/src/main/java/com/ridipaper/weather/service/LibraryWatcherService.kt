package com.ridipaper.weather.service

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import com.ridipaper.weather.MainActivity
import java.io.BufferedReader
import java.io.InputStreamReader

class LibraryWatcherService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var am: ActivityManager
    private lateinit var pm: PowerManager
    private var lastTriggerTime = 0L

    private val pollRunnable = object : Runnable {
        override fun run() {
            try {
                if (isScreenInteractive()) {
                    checkTopActivity()
                }
            } catch (_: Exception) {
            }
            handler.postDelayed(this, 450L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        handler.post(pollRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(pollRunnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @Suppress("DEPRECATION")
    private fun isScreenInteractive(): Boolean = pm.isScreenOn

    @Suppress("DEPRECATION")
    private fun checkTopActivity() {
        val tasks = am.getRunningTasks(1)
        if (tasks.isNullOrEmpty()) return
        val top = tasks[0].topActivity ?: return
        if (top.packageName == "com.ridi.paper" &&
            top.className.contains("EPubReaderActivity")
        ) {
            val now = System.currentTimeMillis()
            if (now - lastTriggerTime < 2500L) return
            if (isWeatherTriggerBookOpen()) {
                lastTriggerTime = now
                val launch = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra(MainActivity.EXTRA_FROM_LIBRARY, true)
                }
                startActivity(launch)
            }
        }
    }

    private val uuidRegex = Regex("/data/data/com\\.ridi\\.paper/files/([0-9a-fA-F-]{36})/Annotations\\.db")
    private val uuidTriggerCache = HashMap<String, Boolean>()

    private fun findRidiPid(): Int? {
        val procs = am.runningAppProcesses ?: return null
        return procs.firstOrNull { it.processName == "com.ridi.paper" }?.pid
    }

    private fun isWeatherTriggerBookOpen(): Boolean {
        val pid = findRidiPid() ?: return false
        return try {
            val proc = Runtime.getRuntime().exec(
                arrayOf("su", "-c", "ls -l /proc/$pid/fd 2>/dev/null")
            )
            val openUuids = HashSet<String>()
            var directMatch = false
            BufferedReader(InputStreamReader(proc.inputStream)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    if (line.contains("weather_dashboard_trigger", ignoreCase = true)) {
                        directMatch = true
                    }
                    val match = uuidRegex.find(line)
                    if (match != null) {
                        openUuids.add(match.groupValues[1])
                    }
                    line = reader.readLine()
                }
            }
            proc.waitFor()
            if (directMatch) return true
            openUuids.any { isTriggerBookUuid(it) }
        } catch (_: Exception) {
            false
        }
    }

    private fun isTriggerBookUuid(uuid: String): Boolean {
        uuidTriggerCache[uuid]?.let { return it }
        val paths = listOf(
            "/mnt/media_rw/extsd/Android/data/com.ridi.paper/files/$uuid/extracted/OEBPS/content.opf",
            "/sdcard/Android/data/com.ridi.paper/files/$uuid/extracted/OEBPS/content.opf",
            "/data/data/com.ridi.paper/files/$uuid/extracted/OEBPS/content.opf"
        )
        var found = false
        for (p in paths) {
            try {
                val proc = Runtime.getRuntime().exec(
                    arrayOf("su", "-c", "cat '$p' 2>/dev/null")
                )
                val content = BufferedReader(InputStreamReader(proc.inputStream)).use { it.readText() }
                proc.waitFor()
                if (content.contains("urn:uuid:ridipaper-weather-dashboard-001")) {
                    found = true
                    break
                }
            } catch (_: Exception) {
            }
        }
        uuidTriggerCache[uuid] = found
        return found
    }
}
