package com.beatles.player.ui.theme

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VisualQuality {
    HIGH,          // Full LiquidMotion deformation, RenderEffect motion blur (API 31+), deep backdrop blur
    BALANCED,      // Backdrop blur, spring feedback, subtle deformation, reduced motion blur
    BATTERY_SAVER  // Minimal blur radius, no motion blur, no deformation
}

object QualityManager {
    private const val PREFS = "beatles_quality_prefs"
    private const val KEY_QUALITY = "visual_quality"

    private val _currentQuality = MutableStateFlow(VisualQuality.HIGH)
    val currentQuality = _currentQuality.asStateFlow()

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

    fun isReduceMotionEnabled(context: Context): Boolean {
        return try {
            val durationScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            durationScale == 0.0f
        } catch (_: Exception) {
            false
        }
    }

    private fun detectDefaultQuality(context: Context): VisualQuality {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return VisualQuality.BALANCED
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
}
