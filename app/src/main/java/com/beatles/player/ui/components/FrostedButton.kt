package com.beatles.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.beatles.player.ui.glassRefraction
import com.beatles.player.ui.motion.liquidPress
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild

object FrostedButtonDefaults {
    val BlurRadius: Dp = 4.dp

    fun containerColor(isActive: Boolean, isDark: Boolean = true, isEnabled: Boolean = true): Color {
        if (!isEnabled) {
            return if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f)
        }
        return if (isActive) {
            if (isDark) Color(0xFF383842).copy(alpha = 0.35f) else Color(0xFFE2E2E2).copy(alpha = 0.45f)
        } else {
            if (isDark) Color(0xFF383842).copy(alpha = 0.22f) else Color(0xFFE2E2E2).copy(alpha = 0.28f)
        }
    }

    fun contentColor(isActive: Boolean, isDark: Boolean = true, isEnabled: Boolean = true): Color {
        if (!isEnabled) {
            return if (isDark) Color.White.copy(alpha = 0.30f) else Color(0xFF141416).copy(alpha = 0.25f)
        }
        return if (isActive) {
            if (isDark) Color.White else Color(0xFF141416)
        } else {
            if (isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF141416).copy(alpha = 0.85f)
        }
    }
}

/**
 * Standard Liquid Glass Frosted Button:
 * Adopts the nav bar visual setting:
 * - Edge refraction (Modifier.glassRefraction)
 * - Subtle shadow
 * - Backdrop blur (Haze 4.dp)
 * - Optimized for light mode and dark mode
 * - Liquid press spring
 */
@Composable
fun FrostedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    enabled: Boolean = true,
    isDark: Boolean = true,
    shape: Shape = RoundedCornerShape(50),
    cornerRadius: Dp = 50.dp,
    isCircle: Boolean = false,
    hazeState: HazeState? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    content: @Composable RowScope.() -> Unit
) {
    val containerBg = FrostedButtonDefaults.containerColor(active, isDark, enabled)
    val contentCol = FrostedButtonDefaults.contentColor(active, isDark, enabled)

    val shadowElevation = if (enabled) 3.dp else 0.dp
    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.15f)

    val baseMod = modifier
        .liquidPress(interactionSource)
        .shadow(shadowElevation, shape, spotColor = shadowColor, ambientColor = shadowColor)
        .clip(shape)

    val blurMod = if (hazeState != null && enabled) {
        baseMod.hazeChild(
            state = hazeState,
            shape = shape,
            style = HazeStyle(blurRadius = FrostedButtonDefaults.BlurRadius, noiseFactor = 0.06f)
        )
    } else {
        baseMod
    }

    val finalMod = blurMod
        .background(containerBg)
        .glassRefraction(cornerRadius, isDark, isCircle = isCircle)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
        .padding(contentPadding)

    Row(
        modifier = finalMod,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentCol) {
            content()
        }
    }
}

/**
 * Standard Liquid Glass Frosted Icon Button.
 */
@Composable
fun FrostedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    enabled: Boolean = true,
    isDark: Boolean = true,
    shape: Shape = CircleShape,
    size: Dp = 40.dp,
    hazeState: HazeState? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    FrostedButton(
        onClick = onClick,
        modifier = modifier.size(size),
        active = active,
        enabled = enabled,
        isDark = isDark,
        shape = shape,
        cornerRadius = size / 2,
        isCircle = true,
        hazeState = hazeState,
        interactionSource = interactionSource,
        contentPadding = PaddingValues(0.dp)
    ) {
        content()
    }
}
