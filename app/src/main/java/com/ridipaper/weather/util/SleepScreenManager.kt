package com.ridipaper.weather.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import java.io.File
import java.io.FileOutputStream

/**
 * Manages backing up the Ridibooks Paper Pro's original sleep screen and
 * applying/restoring the captured weather dashboard as /cache/sleep/suspend.jpg.
 */
object SleepScreenManager {
    private const val PREFS_NAME = "sleep_screen_backup"
    private const val KEY_BACKUP_DONE = "backup_done"
    private const val KEY_ORIG_STANDBY_TYPE = "orig_standby_type"
    private const val KEY_ORIG_SLEEP_PATH = "orig_sleep_path"

    private const val TARGET_SLEEP_PATH = "/cache/sleep/suspend.jpg"
    private const val EPD_PORTRAIT_W = 1404
    private const val EPD_PORTRAIT_H = 1872

    private fun runSu(command: String): Pair<Boolean, String> {
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val out = proc.inputStream.bufferedReader().readText().trim()
            val code = proc.waitFor()
            (code == 0) to out
        } catch (_: Exception) {
            false to ""
        }
    }

    @Synchronized
    fun backupOriginalIfNeeded(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val backupFile = File(context.filesDir, "original_suspend.jpg")
        if (prefs.getBoolean(KEY_BACKUP_DONE, false) && backupFile.exists()) {
            return true
        }

        val (_, standbyType) = runSu("getprop persist.sys.standbytype")
        val (_, sleepPath) = runSu("settings get system sleepscreen_file_path")
        val safePath = backupFile.absolutePath
        val (copied, _) = runSu(
            "if [ -f $TARGET_SLEEP_PATH ]; then cp $TARGET_SLEEP_PATH '$safePath' && chmod 644 '$safePath'; fi"
        )
        if (!copied) return false

        prefs.edit()
            .putString(KEY_ORIG_STANDBY_TYPE, standbyType.ifBlank { "0" })
            .putString(KEY_ORIG_SLEEP_PATH, if (sleepPath == "null") "" else sleepPath)
            .putBoolean(KEY_BACKUP_DONE, true)
            .apply()
        return true
    }

    @Synchronized
    fun applyDashboardSleepScreen(context: Context, viewBitmap: Bitmap): Boolean {
        backupOriginalIfNeeded(context)

        var rotated: Bitmap? = null
        var scaled: Bitmap? = null
        return try {
            val portraitBmp = if (viewBitmap.width > viewBitmap.height) {
                val matrix = Matrix().apply { postRotate(-90f) }
                rotated = Bitmap.createBitmap(
                    viewBitmap, 0, 0, viewBitmap.width, viewBitmap.height, matrix, true
                )
                rotated
            } else {
                viewBitmap
            }

            val finalBmp = if (portraitBmp.width != EPD_PORTRAIT_W || portraitBmp.height != EPD_PORTRAIT_H) {
                scaled = Bitmap.createScaledBitmap(portraitBmp, EPD_PORTRAIT_W, EPD_PORTRAIT_H, true)
                scaled
            } else {
                portraitBmp
            }

            val stagedFile = File(context.filesDir, "dashboard_suspend.jpg")
            FileOutputStream(stagedFile).use { fos ->
                finalBmp.compress(Bitmap.CompressFormat.JPEG, 92, fos)
            }

            val stagedPath = stagedFile.absolutePath
            val (ok, _) = runSu(
                "cp '$stagedPath' $TARGET_SLEEP_PATH && " +
                    "chown system:system $TARGET_SLEEP_PATH && " +
                    "chmod 644 $TARGET_SLEEP_PATH && " +
                    "setprop persist.sys.standbytype 0"
            )
            ok
        } catch (_: Exception) {
            false
        } finally {
            if (scaled != null && scaled !== rotated && scaled !== viewBitmap) {
                scaled?.recycle()
            }
            if (rotated != null && rotated !== viewBitmap) {
                rotated?.recycle()
            }
        }
    }

    @Synchronized
    fun restoreOriginalSleepScreen(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_BACKUP_DONE, false)) {
            return true
        }

        val backupFile = File(context.filesDir, "original_suspend.jpg")
        val origType = prefs.getString(KEY_ORIG_STANDBY_TYPE, "0").orEmpty().ifBlank { "0" }
        val origPath = prefs.getString(KEY_ORIG_SLEEP_PATH, "").orEmpty()

        val cmds = StringBuilder()
        if (backupFile.exists()) {
            val safePath = backupFile.absolutePath
            cmds.append("cp '$safePath' $TARGET_SLEEP_PATH && ")
            cmds.append("chown system:system $TARGET_SLEEP_PATH && ")
            cmds.append("chmod 600 $TARGET_SLEEP_PATH && ")
        }
        cmds.append("setprop persist.sys.standbytype '$origType'")
        if (origPath.isNotBlank()) {
            cmds.append(" && settings put system sleepscreen_file_path '$origPath'")
        }

        val (ok, _) = runSu(cmds.toString())
        if (ok) {
            prefs.edit().putBoolean(KEY_BACKUP_DONE, false).apply()
        }
        return ok
    }
}
