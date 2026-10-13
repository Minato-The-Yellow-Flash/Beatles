package com.beatles.player.ui.theme

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VisualQuality {
    HIGH,          // Full AGSL refraction shader (API 33+), deep backdrop blur, specular highlights, spring feedback
    BALANCED,      // Realtime backdrop blur + rim specular highlights + liquid spring feedback
    BATTERY_SAVER  // Translucent frosted glass + rim highlights, static transforms, minimized blur
}

object QualityManager {
    private const val PREFS = "beatles_quality_prefs"
    private const val KEY_QUALITY = "visual_quality"

    private val _currentQuality = MutableStateFlow(VisualQuality.HIGH)
    val currentQuality = _currentQuality.asStateFlow()

    var showDevFpsOverlay by mutableStateOf(false)
    var currentFps by mutableFloatStateOf(60f)
    var lastFrameTimeMs by mutableLongStateOf(0L)

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_QUALITY, null)
        if (saved != null) {
            _currentQuality.value = try {
                VisualQuality.valueOf(saved)
            } catch (_: Exception) {
                detectDefaultQuality(context)
            }
        } else {
            _currentQuality.value = detectDefaultQuality(context)
        }
    }

    fun setQuality(context: Context, quality: VisualQuality) {
        _currentQuality.value = quality
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_QUALITY, quality.name)
            .apply()
    }

    private fun detectDefaultQuality(context: Context): VisualQuality {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return VisualQuality.BATTERY_SAVER
        }
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am?.getMemoryInfo(memInfo)
            val totalRamGb = memInfo.totalMem / (1024 * 1024 * 1024.0)
            if (totalRamGb < 3.8 || am?.isLowRamDevice == true) {
                VisualQuality.BALANCED
            } else {
                VisualQuality.HIGH
            }
        } catch (_: Exception) {
            VisualQuality.BALANCED
        }
    }

    private var frameCount = 0
    private var lastFpsTimestamp = SystemClock.elapsedRealtime()

    fun onFrameRendered() {
        val now = SystemClock.elapsedRealtime()
        frameCount++
        val delta = now - lastFpsTimestamp
        if (delta >= 500) {
            currentFps = (frameCount * 1000f) / delta
            frameCount = 0
            lastFpsTimestamp = now
        }
        lastFrameTimeMs = now
    }
}
