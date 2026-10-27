package com.beatles.player.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild

/** Whether liquid glass rendering is enabled globally. */
val LocalLiquidGlassEnabled = staticCompositionLocalOf { true }

/** The active [HazeState] used to sample backdrop blur for liquid glass surfaces. */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/** Backward-compatibility stubs for backdrop types. */
val LocalAppBackdrop = staticCompositionLocalOf<Any?> { null }
internal val LocalGlassExport = staticCompositionLocalOf<Any?> { null }

/** Liquid glass is supported on Android 8.0+ (API 26+) with our hardware-accelerated pipeline. */
fun isGlassSupported(sdkInt: Int = Build.VERSION.SDK_INT): Boolean = sdkInt >= Build.VERSION_CODES.O

val GlassHazeStyle = HazeStyle(
    blurRadius = 10.dp,
    noiseFactor = 0.02f
)

/** The hairline border width along a liquid glass edge. */
val GLASS_EDGE_WIDTH: Dp = 0.5.dp

@Composable
fun glassContentColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) Color(0xFF141416) else Color.White

@Composable
fun glassIndicatorColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
        Color.White.copy(alpha = 0.40f)
    } else {
        Color.White.copy(alpha = 0.08f)
    }

@Composable
fun glassBarTint(): Color? =
    Color.White.copy(alpha = 0.04f)

/**
 * Pure hardware-accelerated dual-curve lens refraction restored from the original
 * nav bar concept. Eliminates the 45° angle and uses vertical curves (upper + lower)
 * and horizontal side curves with disconnected middle gaps and lower edge opacities
 * for an ultra-subtle, elegant crystal glass outline.
 */
@Composable
fun Modifier.liquidGlassRim(
    shape: CornerBasedShape,
    strokeWidth: Dp = 0.5.dp
): Modifier {
    val isDark = MaterialTheme.colorScheme.surface.luminance() <= 0.5f
    return this.liquidGlassRim(shape, strokeWidth, isDark)
}

fun Modifier.liquidGlassRim(
    shape: CornerBasedShape,
    strokeWidth: Dp = 0.5.dp,
    isDark: Boolean = true
): Modifier = this.drawWithContent {
    drawContent()
    if (size.width < 1f || size.height < 1f) return@drawWithContent

    val reflectionColor = if (isDark) Color.White else Color(0xFF141416)

    val strokePx = strokeWidth.toPx()
    val minDim = size.minDimension
    val cornerPx = shape.topStart.toPx(size, this).coerceAtMost(minDim / 2f)

    // 1. Subtle dual-curve lens sheen (vertical gradient with middle gap)
    val sheenAlpha = if (isDark) 0.03f else 0.02f
    drawRoundRect(
        brush = Brush.verticalGradient(
            0.0f to reflectionColor.copy(alpha = sheenAlpha),
            0.22f to Color.Transparent,
            0.78f to Color.Transparent,
            1.0f to reflectionColor.copy(alpha = sheenAlpha * 0.5f)
        ),
        cornerRadius = CornerRadius(cornerPx, cornerPx)
    )

    // 2. Inner content refraction bevel: subtle secondary lines inset by 0.5dp (disconnected)
    val innerStrokeW = 0.5.dp.toPx()
    val innerPeakAlpha = if (isDark) 0.04f else 0.03f
    val innerBottomAlpha = innerPeakAlpha * 0.60f
    drawRoundRect(
        brush = Brush.verticalGradient(
            0.0f to reflectionColor.copy(alpha = innerPeakAlpha),
            0.16f to Color.Transparent,
            0.84f to Color.Transparent,
            1.0f to reflectionColor.copy(alpha = innerBottomAlpha)
        ),
        topLeft = Offset(innerStrokeW, innerStrokeW),
        size = Size(
            (size.width - innerStrokeW * 2).coerceAtLeast(0f),
            (size.height - innerStrokeW * 2).coerceAtLeast(0f)
        ),
        cornerRadius = CornerRadius(
            (cornerPx - innerStrokeW).coerceAtLeast(0f),
            (cornerPx - innerStrokeW).coerceAtLeast(0f)
        ),
        style = Stroke(width = innerStrokeW)
    )

    // 3. Outer content refraction reflection: upper curve + lower curve with vertical middle gap
    // Edge opacity lowered as requested (subtle reflection, never harsh or distracting)
    val outerPeakAlpha = if (isDark) 0.14f else 0.06f
    val outerBottomAlpha = outerPeakAlpha * 0.60f
    drawRoundRect(
        brush = Brush.verticalGradient(
            0.0f to reflectionColor.copy(alpha = outerPeakAlpha),
            0.18f to reflectionColor.copy(alpha = outerPeakAlpha * 0.35f),
            0.28f to Color.Transparent,
            0.72f to Color.Transparent,
            0.82f to reflectionColor.copy(alpha = outerBottomAlpha * 0.35f),
            1.0f to reflectionColor.copy(alpha = outerBottomAlpha)
        ),
        topLeft = Offset(strokePx / 2f, strokePx / 2f),
        size = Size(
            (size.width - strokePx).coerceAtLeast(0f),
            (size.height - strokePx).coerceAtLeast(0f)
        ),
        cornerRadius = CornerRadius(
            (cornerPx - strokePx / 2f).coerceAtLeast(0f),
            (cornerPx - strokePx / 2f).coerceAtLeast(0f)
        ),
        style = Stroke(width = strokePx)
    )

    // 4. Right & Left edges border: subtle horizontal curve with middle gap
    val sidePeakAlpha = if (isDark) 0.03f else 0.05f
    drawRoundRect(
        brush = Brush.horizontalGradient(
            0.0f to reflectionColor.copy(alpha = sidePeakAlpha),
            0.15f to Color.Transparent,
            0.85f to Color.Transparent,
            1.0f to reflectionColor.copy(alpha = sidePeakAlpha)
        ),
        topLeft = Offset(strokePx / 2f, strokePx / 2f),
        size = Size(
            (size.width - strokePx).coerceAtLeast(0f),
            (size.height - strokePx).coerceAtLeast(0f)
        ),
        cornerRadius = CornerRadius(
            (cornerPx - strokePx / 2f).coerceAtLeast(0f),
            (cornerPx - strokePx / 2f).coerceAtLeast(0f)
        ),
        style = Stroke(width = strokePx)
    )
}

/**
 * Convenience compatibility helper matching the original [glassRefraction] signature.
 */
fun Modifier.glassRefraction(
    cornerRadius: Dp,
    isDark: Boolean,
    isCircle: Boolean = false,
    strokeWidth: Dp = 0.5.dp
): Modifier {
    val shape = if (isCircle) CircleShape else RoundedCornerShape(cornerRadius)
    return this.liquidGlassRim(shape, strokeWidth, isDark)
}

/**
 * Lightweight liquid glass over a stable background with ultra-clear transparency.
 */
@Composable
fun Modifier.lightweightLiquidGlass(
    shape: CornerBasedShape,
    fallbackColor: Color,
    showBorder: Boolean = false,
): Modifier {
    val enabled = LocalLiquidGlassEnabled.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() <= 0.5f
    // Ultra-clear pure liquid glass body tint (0.04f in dark mode)
    val glassTint = if (isDark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.06f)
    val finalBg = if (enabled) glassTint else fallbackColor

    var m = clip(shape)
        .background(finalBg, shape)
        .then(if (enabled) Modifier.liquidGlassRim(shape, isDark = isDark) else Modifier)

    if (showBorder) {
        val edgeColor = if (isDark) Color.White.copy(alpha = 0.04f) else Color(0xFF141416).copy(alpha = 0.04f)
        m = m.border(GLASS_EDGE_WIDTH, edgeColor, shape)
    }
    return m
}

/**
 * Renders an ultra-clear pure liquid glass surface (almost crystal-clear, ~4% body tint):
 * 1. Live dynamic backdrop blur via [HazeState]
 * 2. Ultra-clear transparent crystal surface tint (no cloudy blur)
 * 3. Restored vertical & horizontal lens refraction with lowered edge opacity
 * 4. Borderless by default for clean media player controls
 */
@Composable
fun Modifier.liquidGlass(
    shape: CornerBasedShape,
    surfaceTint: Color? = null,
    showBorder: Boolean = false,
): Modifier {
    val enabled = LocalLiquidGlassEnabled.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() <= 0.5f

    if (!enabled) {
        val defaultBg = surfaceTint ?: MaterialTheme.colorScheme.surface
        var m = clip(shape).background(defaultBg, shape)
        if (showBorder) {
            val edgeColor = if (isDark) Color.White.copy(alpha = 0.04f) else Color(0xFF141416).copy(alpha = 0.04f)
            m = m.border(GLASS_EDGE_WIDTH, edgeColor, shape)
        }
        return m
    }

    val hazeState = LocalHazeState.current
    // Ultra-clear pure liquid glass: 4% translucent body (almost clear crystal glass, not milky blur)
    val defaultTint = if (isDark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.06f)
    val finalTint = surfaceTint ?: defaultTint

    var base: Modifier = this.clip(shape)

    if (hazeState != null) {
        base = base.hazeChild(
            state = hazeState,
            shape = shape,
            style = GlassHazeStyle
        )
    }

    var result = base
        .background(finalTint, shape)
        .liquidGlassRim(shape, isDark = isDark)

    if (showBorder) {
        val edgeColor = if (isDark) Color.White.copy(alpha = 0.04f) else Color(0xFF141416).copy(alpha = 0.04f)
        result = result.border(GLASS_EDGE_WIDTH, edgeColor, shape)
    }

    return result
}
