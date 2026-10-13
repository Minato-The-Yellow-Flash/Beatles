package com.beatles.player.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

fun getAppleMusicGradientColors(title: String, artist: String): Triple<Color, Color, Color> {
    val seed = abs((title.hashCode() * 31 + artist.hashCode()))
    val hue1 = (seed % 360).toFloat()
    val hue2 = ((seed + 75) % 360).toFloat()
    val hue3 = ((seed + 150) % 360).toFloat()
    return Triple(
        Color.hsl(hue1, 0.75f, 0.48f),
        Color.hsl(hue2, 0.85f, 0.40f),
        Color.hsl(hue3, 0.70f, 0.25f)
    )
}

/**
 * Procedural Apple Music-style live dynamic gradient artwork.
 * Displays vibrant, harmonized multi-stop gradients with gentle ambient shifts
 * whenever a track or album lacks high-resolution artwork.
 */
@Composable
fun AppleMusicGradientCover(
    title: String,
    artist: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    showLabel: Boolean = true
) {
    val (color1, color2, color3) = remember(title, artist) {
        getAppleMusicGradientColors(title, artist)
    }

    // Ambient live shift animation (gentle floating gradient)
    val infiniteTransition = rememberInfiniteTransition(label = "ambientGradientShift")
    val shiftProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientShift"
    )

    val gradient = remember(shiftProgress, color1, color2, color3) {
        val stop1 = (0.0f + 0.15f * shiftProgress).coerceIn(0f, 0.3f)
        val stop2 = (0.50f + 0.10f * shiftProgress).coerceIn(0.4f, 0.7f)
        Brush.linearGradient(
            listOf(color1, color2, color3),
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(800f * (1f + 0.3f * shiftProgress), 800f * (1f + 0.3f * shiftProgress))
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        // Radial lighting overlay for Apple Music sheen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                        radius = 450f
                    )
                )
        )

        // Minimalist vinyl / wave badge
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (showLabel) 44.dp else 36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(if (showLabel) 22.dp else 18.dp)
                )
            }

            if (showLabel && title.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                if (artist.isNotBlank()) {
                    Text(
                        text = artist,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
