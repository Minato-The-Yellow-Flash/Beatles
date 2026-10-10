package com.beatles.player.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Cheap replacement for the old `Offscreen + DstIn` fade mask (which forced a full-screen
 * offscreen layer every frame and was a major scroll-lag source). Simply paints a bg-colored
 * gradient over the top/bottom edges – no offscreen compositing.
 */
fun Modifier.edgeFade(bg: Color, top: Dp = 100.dp, bottom: Dp = 110.dp): Modifier = drawWithContent {
    drawContent()
    if (size.width <= 0f || size.height <= 0f) return@drawWithContent
    val maxH = size.height / 2f
    val t = top.toPx().coerceIn(0f, maxH)
    val b = bottom.toPx().coerceIn(0f, maxH)
    if (t > 0f) {
        drawRect(
            brush = Brush.verticalGradient(listOf(bg, bg.copy(alpha = 0f)), startY = 0f, endY = t),
            size = Size(size.width, t)
        )
    }
    if (b > 0f) {
        drawRect(
            brush = Brush.verticalGradient(listOf(bg.copy(alpha = 0f), bg.copy(alpha = 0.92f)), startY = size.height - b, endY = size.height),
            topLeft = Offset(0f, size.height - b),
            size = Size(size.width, b)
        )
    }
}
