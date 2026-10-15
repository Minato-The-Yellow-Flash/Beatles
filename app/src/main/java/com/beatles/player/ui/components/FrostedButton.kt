package com.beatles.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.beatles.player.ui.motion.liquidPress
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild

object FrostedButtonDefaults {
    val BlurRadius: Dp = 6.dp
    val BorderWidth: Dp = 1.dp

    fun containerColor(isActive: Boolean, isDark: Boolean = true, isEnabled: Boolean = true): Color {
        if (!isEnabled) {
            return if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f)
        }
        return if (isActive) {
            if (isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.10f)
        } else {
            if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
        }
    }

    fun borderColor(isActive: Boolean, isDark: Boolean = true, isEnabled: Boolean = true): Color {
        if (!isEnabled) return Color.Transparent
        return if (isActive) {
            if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.18f)
        } else {
            if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.10f)
        }
    }

    fun contentColor(isActive: Boolean, isDark: Boolean = true, isEnabled: Boolean = true): Color {
        if (!isEnabled) {
            return if (isDark) Color.White.copy(alpha = 0.30f) else Color.Black.copy(alpha = 0.25f)
        }
        return if (isActive) {
            if (isDark) Color.White else Color(0xFF141416)
        } else {
            if (isDark) Color.White.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.75f)
        }
    }
}

/**
 * Standard Frosted Button (R2).
 * Replaces solid white enabled states across the application with a translucent
 * frosted overlay (14-22% alpha, 1dp subtle border, bright white content, lens press scale).
 */
@Composable
fun FrostedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    enabled: Boolean = true,
    isDark: Boolean = true,
    shape: Shape = RoundedCornerShape(50),
    hazeState: HazeState? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    content: @Composable RowScope.() -> Unit
) {
    val containerBg = FrostedButtonDefaults.containerColor(active, isDark, enabled)
    val borderCol = FrostedButtonDefaults.borderColor(active, isDark, enabled)
    val contentCol = FrostedButtonDefaults.contentColor(active, isDark, enabled)

    val baseMod = modifier
        .liquidPress(interactionSource)
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
        .then(if (borderCol != Color.Transparent) Modifier.border(FrostedButtonDefaults.BorderWidth, borderCol, shape) else Modifier)
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
 * Standard Frosted Icon Button (R2).
 */
@Composable
fun FrostedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    enabled: Boolean = true,
    isDark: Boolean = true,
    shape: Shape = CircleShape,
    hazeState: HazeState? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    FrostedButton(
        onClick = onClick,
        modifier = modifier,
        active = active,
        enabled = enabled,
        isDark = isDark,
        shape = shape,
        hazeState = hazeState,
        interactionSource = interactionSource,
        contentPadding = PaddingValues(0.dp)
    ) {
        content()
    }
}
