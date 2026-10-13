package com.beatles.player.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.beatles.player.data.Album
import com.beatles.player.data.Song
import com.beatles.player.ui.theme.isDarkTheme
import com.beatles.player.viewmodel.PlayerUiState
import com.beatles.player.viewmodel.PlayerViewModel
import kotlin.math.roundToInt

private val AccentRed = Color(0xFFF82C38)

/**
 * Premium Fluid Shared-Element Album Transition Overlay:
 * Smoothly morphs from the tapped album thumbnail's exact coordinates into the enlarged
 * detail header view, and seamlessly returns upon closing.
 */
@Composable
fun FluidAlbumDetailOverlay(
    album: Album?,
    sourceBounds: Rect?,
    state: PlayerUiState,
    viewModel: PlayerViewModel,
    onClose: () -> Unit,
    onPlay: (Song, List<Song>) -> Unit
) {
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val isDark = isDarkTheme(state.themeMode)
    val bgColor = if (isDark) Color(0xFF141416) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF141416)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF6B7280)
    val cardBg = if (isDark) Color(0xFF1E2024).copy(alpha = 0.70f) else Color(0xFFF4F5F7)

    var currentAlbum by remember { mutableStateOf<Album?>(null) }
    var currentOriginBounds by remember { mutableStateOf<Rect?>(null) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(album) {
        if (album != null) {
            currentAlbum = album
            if (sourceBounds != null) {
                currentOriginBounds = sourceBounds
            }
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.84f,
                    stiffness = 320f
                )
            )
        } else {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.86f,
                    stiffness = 380f
                )
            )
            currentAlbum = null
            currentOriginBounds = null
        }
    }

    val p = progress.value
    val alb = currentAlbum ?: return

    if (p > 0f) {
        var dragY by remember { mutableFloatStateOf(0f) }

        val screenWpx = with(density) { config.screenWidthDp.dp.toPx() }
        val screenHpx = with(density) { config.screenHeightDp.dp.toPx() }

        val targetArtworkSize = (config.screenWidthDp.dp - 64.dp).coerceAtMost(300.dp)
        val targetArtworkSizePx = with(density) { targetArtworkSize.toPx() }
        val targetLeftPx = (screenWpx - targetArtworkSizePx) / 2f
        val targetTopPx = with(density) { 76.dp.toPx() }

        val origin = currentOriginBounds ?: Rect(
            screenWpx / 2f - targetArtworkSizePx / 4f,
            screenHpx / 2f - targetArtworkSizePx / 4f,
            screenWpx / 2f + targetArtworkSizePx / 4f,
            screenHpx / 2f + targetArtworkSizePx / 4f
        )

        // Fluid interpolation of artwork bounds
        val curLeft = origin.left + (targetLeftPx - origin.left) * p
        val curTop = origin.top + (targetTopPx - origin.top) * p
        val curWidth = origin.width + (targetArtworkSizePx - origin.width) * p
        val curHeight = origin.height + (targetArtworkSizePx - origin.height) * p
        val curCorner = with(density) { (18.dp.toPx() + (22.dp.toPx() - 18.dp.toPx()) * p).toDp() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor.copy(alpha = p.coerceIn(0f, 1f)))
                .offset { IntOffset(0, dragY.roundToInt()) }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (dragY > 100f) {
                                onClose()
                            }
                            dragY = 0f
                        },
                        onVerticalDrag = { _, delta ->
                            if (delta > 0 || dragY > 0f) {
                                dragY = (dragY + delta).coerceAtLeast(0f)
                                if (dragY > 200f) {
                                    onClose()
                                }
                            }
                        }
                    )
                }
        ) {
            // Surrounding detail view content fading and sliding in as artwork expands
            if (p > 0.05f) {
                val contentAlpha = ((p - 0.22f) / 0.78f).coerceIn(0f, 1f)
                val contentSlide = (1f - p) * with(density) { 32.dp.toPx() }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = contentAlpha
                            translationY = contentSlide
                        }
                ) {
                    // Pinned Header Bar: Back/Close button + Title
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f))
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = "Album",
                            color = textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Scrollable content starting below the enlarged artwork
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = targetArtworkSize + 24.dp,
                            bottom = 90.dp
                        )
                    ) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = alb.title,
                                    color = textPrimary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${alb.artist} • ${alb.releaseYear.ifBlank { "2026" }}",
                                    color = textSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                // Minimal Action Row: Play & Shuffle Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (alb.songs.isNotEmpty()) onPlay(alb.songs.first(), alb.songs)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(24.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isDark) Color.White else Color(0xFF141416),
                                            contentColor = if (isDark) Color.Black else Color.White
                                        )
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Play", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            if (alb.songs.isNotEmpty()) {
                                                val shuffled = alb.songs.shuffled()
                                                onPlay(shuffled.first(), shuffled)
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(24.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = cardBg,
                                            contentColor = textPrimary
                                        )
                                    ) {
                                        Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Shuffle", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }

                        // Compact Tracklist Heading
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tracks",
                                    color = textPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${alb.songs.size} songs",
                                    color = textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Neatly Spaced Tracklist
                        if (alb.songs.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No tracks available for this album", color = textSecondary, fontSize = 14.sp)
                                }
                            }
                        } else {
                            itemsIndexed(alb.songs) { index, song ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onPlay(song, alb.songs) }
                                        .padding(horizontal = 24.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = textSecondary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.width(28.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            color = textPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = song.artist,
                                            color = textSecondary,
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                    }
                                    val dur = if (song.durationSeconds > 0) {
                                        "${song.durationSeconds / 60}:${String.format("%02d", song.durationSeconds % 60)}"
                                    } else ""
                                    Text(
                                        text = dur,
                                        color = textSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // The Shared Artwork: smoothly morphs from origin rect into target rect
            Box(
                modifier = Modifier
                    .offset { IntOffset(curLeft.roundToInt(), curTop.roundToInt()) }
                    .size(
                        with(density) { curWidth.coerceAtLeast(1f).toDp() },
                        with(density) { curHeight.coerceAtLeast(1f).toDp() }
                    )
                    .clip(RoundedCornerShape(curCorner))
                    .shadow(elevation = (12.dp * p), shape = RoundedCornerShape(curCorner))
            ) {
                AsyncImage(
                    model = alb.coverUrl,
                    contentDescription = alb.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Standard AlbumDetailSheet for backward-compatibility.
 */
@Composable
fun AlbumDetailSheet(
    album: Album,
    state: PlayerUiState,
    viewModel: PlayerViewModel,
    onClose: () -> Unit,
    onPlay: (Song, List<Song>) -> Unit
) {
    FluidAlbumDetailOverlay(
        album = album,
        sourceBounds = state.selectedAlbumBounds,
        state = state,
        viewModel = viewModel,
        onClose = onClose,
        onPlay = onPlay
    )
}
