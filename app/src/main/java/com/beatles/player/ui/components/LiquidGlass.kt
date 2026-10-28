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
        Color.White.copy(alpha = 0.65f)
    } else {
        Color.White.copy(alpha = 0.16f)
    }

@Composable
fun glassBarTint(): Color? =
    if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
        Color.White.copy(alpha = 0.82f)
    } else {
        Color.White.copy(alpha = 0.10f)
    }

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
    val sheenAlpha = if (isDark) 0.08f else 0.10f
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
    val innerPeakAlpha = if (isDark) 0.12f else 0.15f
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

    // 3. BitChord Specular Edge at our signature position with 30% less opacity:
    // Peak alpha ~0.35f in dark mode and ~0.45f in light mode (reduced 30% from 0.50f/0.65f)
    val outerPeakAlpha = if (isDark) 0.35f else 0.45f
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
    val sidePeakAlpha = if (isDark) 0.12f else 0.16f
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

    // 5. Chromatic Dispersion (Prismatic edge refraction of true liquid glass)
    // Grazing angles split light into subtle cyan (leading) and magenta (trailing) wavelengths
    val chromaticCyan = if (isDark) Color(0x3500E5FF) else Color(0x240080FF)
    val chromaticMagenta = if (isDark) Color(0x28FF007F) else Color(0x20E040FB)
    drawRoundRect(
        brush = Brush.linearGradient(
            0.0f to chromaticCyan,
            0.18f to Color.Transparent,
            0.82f to Color.Transparent,
            1.0f to chromaticMagenta,
            start = Offset(0f, 0f),
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
    // 90% transparent pure liquid glass body tint (0.10f in dark mode, 0.82f in light mode)
    val glassTint = if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.82f)
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
 * Renders unified pure liquid glass with optical chromatic refraction and seamless morphing:
 * 1. Merged GPU blur & body tint pass via [HazeState] with [HazeStyle(tint)] (no separate milky background modifier)
 * 2. Integrated chromatic dispersion & dual-curve lens refraction on the unified canvas
 * 3. 90% crystal clear transmission in dark mode & crisp iOS glass in light mode
 * 4. Borderless by default for ultra-smooth fluid shape morphing
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
    // 90% transparent pure liquid glass body tint (crystal glass)
    val defaultTint = if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.82f)
    val finalTint = surfaceTint ?: defaultTint

    // Merge Layer 1 (blur) and Layer 2 (tint) into a single hardware shader pass via HazeStyle.
    // This eliminates the separate Compose .background() overlay that previously caused a milky "frosted" look!
    val unifiedStyle = androidx.compose.runtime.remember(finalTint) {
        HazeStyle(
            tint = finalTint,
            blurRadius = 12.dp,
            noiseFactor = 0.015f
        )
    }

    var base: Modifier = this.clip(shape)

    if (hazeState != null) {
        base = base.hazeChild(
            state = hazeState,
            shape = shape,
            style = unifiedStyle
        )
    } else {
        base = base.background(finalTint, shape)
    }

    // Layer 3: Directional lens refraction with chromatic dispersion directly on the same node
    var result = base.liquidGlassRim(shape, isDark = isDark)

    if (showBorder) {
        val edgeColor = if (isDark) Color.White.copy(alpha = 0.04f) else Color(0xFF141416).copy(alpha = 0.04f)
        result = result.border(GLASS_EDGE_WIDTH, edgeColor, shape)
    }

    return result
}
