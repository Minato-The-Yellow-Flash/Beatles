package com.beatles.player.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
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
        Color.White.copy(alpha = 0.60f)
    } else {
        Color.White.copy(alpha = 0.15f)
    }

@Composable
fun glassBarTint(): Color? =
    Color.White.copy(alpha = 0.10f)

/**
 * Pure hardware-accelerated 45° directional highlight rim stroke via Compose Canvas.
 * Combines BitChord's 45° top-left specular reflection with our signature dual-curve
 * inner/outer refraction bevels and exact light/dark mode color placement.
 */
@Composable
fun Modifier.liquidGlassRim(
    shape: CornerBasedShape,
    strokeWidth: Dp = 0.75.dp
): Modifier {
    val isDark = MaterialTheme.colorScheme.surface.luminance() <= 0.5f
    return this.liquidGlassRim(shape, strokeWidth, isDark)
}

fun Modifier.liquidGlassRim(
    shape: CornerBasedShape,
    strokeWidth: Dp = 0.75.dp,
    isDark: Boolean = true
): Modifier = this.drawWithContent {
    drawContent()
    if (size.width < 1f || size.height < 1f) return@drawWithContent

    val reflectionColor = if (isDark) Color.White else Color(0xFF141416)

    val strokePx = strokeWidth.toPx()
    val minDim = size.minDimension
    val cornerPx = shape.topStart.toPx(size, this).coerceAtMost(minDim / 2f)

    // 1. BitChord 45° Specular Highlight Rim (light catching from top-left)
    // Dark mode: 50% white specular highlight. Light mode: 65% crisp specular reflection.
    val peakAlpha = if (isDark) 0.50f else 0.65f
    drawRoundRect(
        brush = Brush.linearGradient(
            0.0f to reflectionColor.copy(alpha = peakAlpha),
            0.20f to reflectionColor.copy(alpha = peakAlpha * 0.45f),
            0.45f to Color.Transparent,
            1.0f to Color.Transparent,
            start = Offset.Zero,
            end = Offset(size.width, size.height)
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

    // 2. Signature outer dual-curve refraction: upper curve + lower curve
    val outerPeakAlpha = if (isDark) 0.35f else 0.40f
    val outerBottomAlpha = if (isDark) outerPeakAlpha * 0.75f else 0.10f
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

    // 3. Inner content refraction bevel: subtle secondary lines inset by 0.5dp
    val innerStrokeW = 0.5.dp.toPx()
    val innerPeakAlpha = if (isDark) 0.09f else 0.12f
    val innerBottomAlpha = innerPeakAlpha * 0.70f
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
}

/**
 * Lightweight liquid glass over a stable background with 10% transparency.
 */
@Composable
fun Modifier.lightweightLiquidGlass(
    shape: CornerBasedShape,
    fallbackColor: Color,
): Modifier {
    val enabled = LocalLiquidGlassEnabled.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() <= 0.5f
    // Pure liquid glass: 10% translucent body
    val glassTint = Color.White.copy(alpha = 0.10f)
    val finalBg = if (enabled) glassTint else fallbackColor
    val edgeColor = if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFF141416).copy(alpha = 0.08f)

    return clip(shape)
        .background(finalBg, shape)
        .then(if (enabled) Modifier.liquidGlassRim(shape, isDark = isDark) else Modifier)
        .border(GLASS_EDGE_WIDTH, edgeColor, shape)
}

/**
 * Renders a pure liquid glass surface with 10% transparency:
 * 1. Live dynamic backdrop blur via [HazeState]
 * 2. 10% translucent crystal glass surface tint
 * 3. Dual-curve chromatic lens sheen on upper/lower curves
 * 4. Hairline edge border
 * 5. Paired with [liquidGlassRim] for directional specular highlights
 */
@Composable
fun Modifier.liquidGlass(
    shape: CornerBasedShape,
    surfaceTint: Color? = null,
): Modifier {
    val enabled = LocalLiquidGlassEnabled.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() <= 0.5f
    val edgeColor = if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFF141416).copy(alpha = 0.08f)

    if (!enabled) {
        val defaultBg = surfaceTint ?: MaterialTheme.colorScheme.surface
        return clip(shape)
            .background(defaultBg, shape)
            .border(GLASS_EDGE_WIDTH, edgeColor, shape)
    }

    val hazeState = LocalHazeState.current
    // Pure liquid glass: 10% translucent body (not cloudy/frosted slab)
    val defaultTint = Color.White.copy(alpha = 0.10f)
    val finalTint = surfaceTint ?: defaultTint

    var base: Modifier = this.clip(shape)

    if (hazeState != null) {
        base = base.hazeChild(
            state = hazeState,
            shape = shape,
            style = GlassHazeStyle
        )
    }

    return base
        .background(finalTint, shape)
        .liquidGlassRim(shape, isDark = isDark)
        .drawWithContent {
            if (size.width < 1f || size.height < 1f) {
                drawContent()
                return@drawWithContent
            }
            // Dual-curve vertical lens sheen (Apple/BitChord physical glass)
            val cornerPx = shape.topStart.toPx(size, this).coerceAtMost(size.minDimension / 2f)
            val reflectionColor = if (isDark) Color.White else Color(0xFF141416)
            val sheenAlpha = if (isDark) 0.04f else 0.03f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    0.0f to reflectionColor.copy(alpha = sheenAlpha),
                    0.22f to Color.Transparent,
                    0.78f to Color.Transparent,
                    1.0f to reflectionColor.copy(alpha = sheenAlpha * 0.6f)
                ),
                cornerRadius = CornerRadius(cornerPx, cornerPx)
            )

            drawContent()
        }
        .border(GLASS_EDGE_WIDTH, edgeColor, shape)
}
