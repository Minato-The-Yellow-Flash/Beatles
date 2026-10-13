package com.beatles.player.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.beatles.player.ui.theme.AppTheme
import com.beatles.player.ui.theme.QualityManager
import com.beatles.player.ui.theme.VisualQuality
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

private const val AGSL_PROGRESSIVE_BLUR_SHADER = """
    uniform shader content;
    uniform float2 size;
    uniform float isTop; // 1.0 for top fade, 0.0 for bottom fade
    
    half4 main(float2 coord) {
        float normY = coord.y / size.y;
        float blurFactor = isTop > 0.5 ? (1.0 - smoothstep(0.0, 1.0, normY)) : smoothstep(0.0, 1.0, normY);
        // Soft gradient sample distribution
        half4 color = content.eval(coord);
        color.a *= blurFactor;
        return color;
    }
"""

@Composable
fun TopProgressiveBlur(
    modifier: Modifier = Modifier,
    height: Dp = 100.dp,
    hazeState: HazeState? = null,
    strength: Float = 1.0f
) {
    val isDark = AppTheme.isDark
    val quality = QualityManager.currentQuality.value
    val baseColor = if (isDark) Color(0xFF0D0D0E) else Color(0xFFF6F6F8)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        // Multi-stage stacked progressive blur for smooth iOS 26 dissolution
        if (hazeState != null && quality != VisualQuality.BATTERY_SAVER) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .hazeChild(state = hazeState)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.0f to Color.Black.copy(alpha = 0.95f * strength),
                                0.40f to Color.Black.copy(alpha = 0.70f * strength),
                                0.75f to Color.Black.copy(alpha = 0.25f * strength),
                                1.0f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            )
        } else if (quality != VisualQuality.BATTERY_SAVER) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(20.dp)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.0f to Color.Black.copy(alpha = 0.90f * strength),
                                0.50f to Color.Black.copy(alpha = 0.40f * strength),
                                1.0f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            )
        }

        // Ambient progressive color dissipation
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to baseColor.copy(alpha = (if (isDark) 0.88f else 0.82f) * strength),
                        0.35f to baseColor.copy(alpha = (if (isDark) 0.55f else 0.45f) * strength),
                        0.70f to baseColor.copy(alpha = (if (isDark) 0.20f else 0.15f) * strength),
                        1.0f to Color.Transparent
                    )
                )
        )
    }
}

@Composable
fun BottomProgressiveBlur(
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    hazeState: HazeState? = null,
    strength: Float = 1.0f
) {
    val isDark = AppTheme.isDark
    val quality = QualityManager.currentQuality.value
    val baseColor = if (isDark) Color(0xFF0D0D0E) else Color(0xFFF6F6F8)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        if (hazeState != null && quality != VisualQuality.BATTERY_SAVER) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .hazeChild(state = hazeState)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.30f to Color.Black.copy(alpha = 0.30f * strength),
                                0.65f to Color.Black.copy(alpha = 0.75f * strength),
                                1.0f to Color.Black.copy(alpha = 0.98f * strength)
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            )
        } else if (quality != VisualQuality.BATTERY_SAVER) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(20.dp)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.50f to Color.Black.copy(alpha = 0.45f * strength),
                                1.0f to Color.Black.copy(alpha = 0.90f * strength)
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            )
        }

        // Ambient progressive color dissipation
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.30f to baseColor.copy(alpha = (if (isDark) 0.20f else 0.15f) * strength),
                        0.65f to baseColor.copy(alpha = (if (isDark) 0.65f else 0.55f) * strength),
                        1.0f to baseColor.copy(alpha = (if (isDark) 0.94f else 0.88f) * strength)
                    )
                )
        )
    }
}
