package com.beatles.player.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/** Consistent, soft OS-native haptic vocabulary for the player. */
object Haptics {
    /** Light tap – play/pause, skip buttons, toggles. */
    fun click(view: View) {
        val c = if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.CLOCK_TICK
        view.performHapticFeedback(c)
    }

    /** Tiny detent – reordering, crossing a swipe threshold. */
    fun tick(view: View) {
        val c = if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_FREQUENT_TICK else HapticFeedbackConstants.CLOCK_TICK
        view.performHapticFeedback(c)
    }

    /** Light slider tick for continuous seekbar scrubbing. */
    fun lightScrubTick(view: View) {
        val c = if (Build.VERSION.SDK_INT >= 34) {
            HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }
        view.performHapticFeedback(c)
    }

    /** Swipe crossed its action threshold. */
    fun threshold(view: View) {
        val c = if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.CLOCK_TICK
        view.performHapticFeedback(c)
    }

    /** Gentle positive feedback – liked, added to playlist, actions. */
    fun confirm(view: View) {
        val c = if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.CLOCK_TICK
        view.performHapticFeedback(c)
    }

    /** Soft notification. */
    fun reject(view: View) {
        val c = if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.KEYBOARD_TAP
        view.performHapticFeedback(c)
    }
}
