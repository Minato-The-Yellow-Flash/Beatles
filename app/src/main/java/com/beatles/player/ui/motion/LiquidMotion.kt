package com.beatles.player.ui.motion

import android.os.Build
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.beatles.player.ui.theme.QualityManager
import com.beatles.player.ui.theme.VisualQuality
import kotlin.math.abs

/**
 * Shared Liquid Motion System (R1).
 *
 * Provides standardized spring presets, velocity-based deformation,
 * directional RenderEffect motion blur (API 31+), and responsive interaction modifiers.
 */
object LiquidMotion {

    /** Lens spring: Medium stiffness with a small organic overshoot (dampingRatio ~0.72). */
    val Lens: AnimationSpec<Float> = spring(
        dampingRatio = 0.72f,
        stiffness = 350f
    )

    /** Morph spring: Smooth expansion transition (dampingRatio ~0.85). */
    val Morph: AnimationSpec<Float> = spring(
        dampingRatio = 0.85f,
        stiffness = 380f
    )

    /** Settle spring: Critically damped, zero overshoot for pixel-exact docking. */
    val Settle: AnimationSpec<Float> = spring(
        dampingRatio = 1.0f,
        stiffness = 380f
    )

    /** DP spring presets for animated layout positions. */
    val LensDp: AnimationSpec<Dp> = spring(
        dampingRatio = 0.72f,
        stiffness = 350f
    )

    val MorphDp: AnimationSpec<Dp> = spring(
        dampingRatio = 0.85f,
        stiffness = 380f
    )

    val SettleDp: AnimationSpec<Dp> = spring(
        dampingRatio = 1.0f,
        stiffness = 380f
    )

    /**
     * Calculates stretch and squeeze scales based on directional velocity (px/s).
     * Deformation is strictly 0.0 at rest (scale 1.0f).
     */
    fun calculateDeformation(velocity: Float, maxStretch: Float = 0.12f): Pair<Float, Float> {
        val magnitude = abs(velocity)
        if (magnitude < 20f) return 1f to 1f
        val stretch = (magnitude / 4200f).coerceIn(0f, maxStretch)
        val squeeze = stretch * 0.40f
        return (1f + stretch) to (1f - squeeze)
    }
}

/**
 * Applies velocity-based liquid deformation and directional RenderEffect motion blur.
 *
 * @param velocityX Horizontal velocity in px/s
 * @param velocityY Vertical velocity in px/s
 * @param maxStretch Maximum axis stretch factor (default: 0.12 = +12%)
 * @param maxBlurDp Maximum directional blur in dp (default: 7.dp)
 */
fun Modifier.liquidMotionDeform(
    velocityX: Float = 0f,
    velocityY: Float = 0f,
    maxStretch: Float = 0.12f,
    maxBlurDp: Dp = 7.dp
): Modifier = composed {
    val context = LocalContext.current
    val density = LocalDensity.current
    val reduceMotion = remember(context) { QualityManager.isReduceMotionEnabled(context) }

    if (reduceMotion) return@composed this

    val absVx = abs(velocityX)
    val absVy = abs(velocityY)

    if (absVx < 25f && absVy < 25f) return@composed this

    val (scaleX, scaleY) = if (absVx >= absVy) {
        val (str, sqz) = LiquidMotion.calculateDeformation(absVx, maxStretch)
        str to sqz
    } else {
        val (str, sqz) = LiquidMotion.calculateDeformation(absVy, maxStretch)
        sqz to str
    }

    val maxBlurPx = with(density) { maxBlurDp.toPx() }
    val blurX = if (absVx > 60f) ((absVx / 4000f) * maxBlurPx).coerceIn(0f, maxBlurPx) else 0f
    val blurY = if (absVy > 60f) ((absVy / 4000f) * maxBlurPx).coerceIn(0f, maxBlurPx) else 0f

    this.graphicsLayer {
        this.scaleX = scaleX
        this.scaleY = scaleY

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (blurX > 0.6f || blurY > 0.6f)) {
            val effBlurX = if (blurX > 0.6f) blurX else 0.1f
            val effBlurY = if (blurY > 0.6f) blurY else 0.1f
            val renderEffect = android.graphics.RenderEffect.createBlurEffect(
                effBlurX,
                effBlurY,
                android.graphics.Shader.TileMode.CLAMP
            )
            this.renderEffect = renderEffect.asComposeRenderEffect()
        }
    }
}

/**
 * Modifier applying an interactive spring press scale (~0.96) using the Lens preset.
 */
fun Modifier.liquidPress(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1.0f,
        animationSpec = LiquidMotion.Lens,
        label = "liquidPressScale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
