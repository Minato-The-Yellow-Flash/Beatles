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

/** Liquid glass is supported on Android 8.0+ (API 26+) with our hardware accelerated pipeline. */
fun isGlassSupported(sdkInt: Int = Build.VERSION.SDK_INT): Boolean = sdkInt >= Build.VERSION_CODES.O

val GlassHazeStyle = HazeStyle(
    blurRadius = 24.dp,
    noiseFactor = 0.05f
)

/** The hairline border along a liquid glass edge. */
val GLASS_EDGE_WIDTH: Dp = 0.5.dp
val GLASS_EDGE_COLOR: Color = Color.White.copy(alpha = 0.12f)

@Composable
fun glassContentColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) Color.Black else Color.White

@Composable
fun glassIndicatorColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
        Color.White.copy(alpha = 0.5f)
    } else {
        Color.White.copy(alpha = 0.14f)
    }

@Composable
fun glassBarTint(): Color? =
    if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) null else Color.White.copy(alpha = 0.09f)

/**
 * Directional specular highlight rim catching light at 45° (top-left).
 * Hardware-accelerated and guaranteed crash-proof across all Android versions.
 */
fun Modifier.liquidGlassRim(
    shape: CornerBasedShape,
    strokeWidth: Dp = 0.75.dp
): Modifier = this.drawWithContent {
    drawContent()
    if (size.width < 1f || size.height < 1f) return@drawWithContent

    val strokePx = strokeWidth.toPx()
    val minDim = size.minDimension
    val cornerPx = shape.topStart.toPx(size, this).coerceAtMost(minDim / 2f)

    drawRoundRect(
        brush = Brush.linearGradient(
            0.0f to Color.White.copy(alpha = 0.45f),
            0.20f to Color.White.copy(alpha = 0.22f),
            0.50f to Color.Transparent,
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
}

/**
 * Lightweight liquid glass over a stable background.
 */
@Composable
fun Modifier.lightweightLiquidGlass(
    shape: CornerBasedShape,
    fallbackColor: Color,
): Modifier {
    val enabled = LocalLiquidGlassEnabled.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() <= 0.5f
    val glassTint = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.35f)
    val finalBg = if (enabled) glassTint else fallbackColor

    return clip(shape)
        .background(finalBg, shape)
        .then(if (enabled) Modifier.liquidGlassRim(shape) else Modifier)
        .border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, shape)
}

/**
 * Renders a full liquid glass surface:
 * 1. Live dynamic backdrop blur via [HazeState]
 * 2. Translucent surface tint (light glass on light theme, dark on dark)
 * 3. Dual-curve chromatic lens sheen on upper/lower curves
 * 4. Hairline edge border
 * 5. Directional specular highlight rim (when paired with [liquidGlassRim])
 */
@Composable
fun Modifier.liquidGlass(
    shape: CornerBasedShape,
    surfaceTint: Color? = null,
): Modifier {
    val enabled = LocalLiquidGlassEnabled.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() <= 0.5f

    if (!enabled) {
        val defaultBg = surfaceTint ?: MaterialTheme.colorScheme.surface
        return clip(shape)
            .background(defaultBg, shape)
            .border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, shape)
    }

    val hazeState = LocalHazeState.current
    val defaultTint = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.35f)
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
        .drawWithContent {
            if (size.width < 1f || size.height < 1f) {
                drawContent()
                return@drawWithContent
            }
            // Dual-curve lens sheen
            val cornerPx = shape.topStart.toPx(size, this).coerceAtMost(size.minDimension / 2f)
            val reflectionColor = if (isDark) Color.White else Color(0xFF141416)
            val sheenAlpha = if (isDark) 0.05f else 0.04f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    0.0f to reflectionColor.copy(alpha = sheenAlpha),
                    0.25f to Color.Transparent,
                    0.75f to Color.Transparent,
                    1.0f to reflectionColor.copy(alpha = sheenAlpha * 0.6f)
                ),
                cornerRadius = CornerRadius(cornerPx, cornerPx)
            )

            drawContent()
        }
        .border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, shape)
}
