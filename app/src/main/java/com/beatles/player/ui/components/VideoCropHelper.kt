package com.beatles.player.ui.components

import android.graphics.Matrix
import android.view.TextureView

/**
 * Applies a top-centered crop matrix to TextureView so that:
 * 1. The video aspect ratio is strictly preserved (no stretching or distortion).
 * 2. The video starts from the top side (upper part is never cut off).
 * 3. The video is centered horizontally to fill the container seamlessly.
 */
fun applyTopCenterCropMatrix(
    textureView: TextureView,
    videoWidth: Float,
    videoHeight: Float
) {
    val viewWidth = textureView.width.toFloat()
    val viewHeight = textureView.height.toFloat()
    if (viewWidth <= 0f || viewHeight <= 0f) return

    val vWidth = if (videoWidth > 0f) videoWidth else 1f
    val vHeight = if (videoHeight > 0f) videoHeight else 1f

    val scale = maxOf(viewWidth / vWidth, viewHeight / vHeight)
    val scaledWidth = vWidth * scale
    val scaledHeight = vHeight * scale

    val sx = scaledWidth / viewWidth
    val sy = scaledHeight / viewHeight
    val dx = (viewWidth - scaledWidth) / 2f
    val dy = 0f // Align strictly to top: starts from top side going down, upper part never gets cut!

    val matrix = Matrix()
    matrix.setScale(sx, sy, 0f, 0f)
    matrix.postTranslate(dx, dy)
    textureView.setTransform(matrix)
}
