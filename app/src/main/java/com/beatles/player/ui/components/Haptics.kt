package com.beatles.player.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/** Consistent, OS-native haptic vocabulary for the player. */
object Haptics {
    /** Light tap – play/pause, skip buttons, toggles. */
    fun click(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    /** Tiny detent – reordering, crossing a swipe threshold. */
    fun tick(view: View) {
        val c = if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.CLOCK_TICK
        view.performHapticFeedback(c)
    }

    /** Swipe crossed its action threshold. */
    fun threshold(view: View) {
        val c = if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE else HapticFeedbackConstants.KEYBOARD_TAP
        view.performHapticFeedback(c)
    }

    /** Positive completion – song changed by swipe, added to Play Next, liked. */
    fun confirm(view: View) {
        val c = if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS
        view.performHapticFeedback(c)
    }

    /** Destructive completion – removed from queue. */
    fun reject(view: View) {
        val c = if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
        view.performHapticFeedback(c)
    }
}
