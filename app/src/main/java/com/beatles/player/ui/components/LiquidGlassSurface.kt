package com.beatles.player.ui.components

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.beatles.player.ui.theme.AppTheme
import com.beatles.player.ui.theme.QualityManager
import com.beatles.player.ui.theme.VisualQuality
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

private const val AGSL_REFRACTION_SHADER = """
    uniform shader content;
    uniform float2 size;
    uniform float refraction;
    
    half4 main(float2 coord) {
        float2 norm = coord / size;
        float2 center = float2(0.5, 0.5);
        float2 diff = norm - center;
        float dist = length(diff);
        
        // Edge lens refraction
        if (dist > 0.36) {
            float edge = smoothstep(0.36, 0.50, dist);
            float2 disp = normalize(diff) * (edge * refraction * 5.0);
            coord += disp;
        }
        return content.eval(coord);
    }
"""

@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    hazeState: HazeState? = null,
    tint: Color = Color.White.copy(alpha = 0.10f),
    blurRadius: Dp = 24.dp,
    refractionEnabled: Boolean = true,
    interactive: Boolean = true,
    elevation: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = AppTheme.isDark
    val quality = QualityManager.currentQuality.value
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && interactive && quality != VisualQuality.BATTERY_SAVER) 1.025f else 1f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "glassScale"
    )
    val pressGlow by animateFloatAsState(
        targetValue = if (isPressed && interactive) 0.08f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "glassGlow"
    )

    // Specular rim gradient: brighter on top-left edge, softer towards bottom-right
    val rimHighlightBrush = remember(isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = if (isDark) 0.42f else 0.65f),
                Color.White.copy(alpha = if (isDark) 0.15f else 0.28f),
                Color.White.copy(alpha = if (isDark) 0.05f else 0.10f),
                Color.White.copy(alpha = if (isDark) 0.25f else 0.45f)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    // Inner subtle glow gradient
    val glassFillBrush = remember(isDark, tint, pressGlow) {
        val baseAlpha = (tint.alpha + pressGlow).coerceIn(0f, 0.40f)
        Brush.verticalGradient(
            colors = listOf(
                tint.copy(alpha = baseAlpha + 0.04f),
                tint.copy(alpha = baseAlpha)
            )
        )
    }

    val shadowAmbient = if (isDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.12f)
    val shadowSpot = if (isDark) Color.Black.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.18f)

    var boxModifier = modifier
        .shadow(
            elevation = if (isPressed) elevation + 4.dp else elevation,
            shape = shape,
            ambientColor = shadowAmbient,
            spotColor = shadowSpot
        )
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                refractionEnabled &&
                quality == VisualQuality.HIGH &&
                size.width > 0 && size.height > 0
            ) {
                try {
                    val shader = android.graphics.RuntimeShader(AGSL_REFRACTION_SHADER)
                    shader.setFloatUniform("size", size.width, size.height)
                    shader.setFloatUniform("refraction", 1.0f)
                    val effect = android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "content")
                    renderEffect = effect.asComposeRenderEffect()
                } catch (_: Throwable) {}
            }
        }
        .clip(shape)

    // Backdrop blur integration
    if (hazeState != null && quality != VisualQuality.BATTERY_SAVER) {
        boxModifier = boxModifier.hazeChild(state = hazeState, shape = shape)
    }

    // Frosted glass fill + specular border
    boxModifier = boxModifier
        .background(glassFillBrush)
        .border(width = 0.9.dp, brush = rimHighlightBrush, shape = shape)

    if (onClick != null) {
        boxModifier = boxModifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    }

    Box(
        modifier = boxModifier,
        content = content
    )
}
