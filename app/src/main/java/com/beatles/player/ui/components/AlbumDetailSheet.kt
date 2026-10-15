package com.beatles.player.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.beatles.player.data.Album
import com.beatles.player.data.Song
import com.beatles.player.ui.theme.AppTheme
import com.beatles.player.ui.theme.isDarkTheme
import com.beatles.player.ui.theme.LocalDynamicBottomPadding
import com.beatles.player.viewmodel.PlayerUiState
import com.beatles.player.viewmodel.PlayerViewModel
import kotlin.math.roundToInt

private val AccentRed = Color(0xFFF82C38)

/**
 * Premium Fluid Shared-Element Album Detail Overlay:
 * Smoothly morphs from the tapped album thumbnail's exact coordinates into the full rich
 * previous Album UI (ambient video banner, parallax title, full action controls, and tracklist),
 * and seamlessly returns upon closing.
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
    val bgColor = if (isDark) Color(0xFF141416) else Color(0xFFF6F7F9)
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
                animationSpec = tween(
                    durationMillis = 240,
                    easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
                )
            )
            currentAlbum = null
            currentOriginBounds = null
        }
    }

    val p = progress.value
    val alb = currentAlbum ?: return

    val resolvedCover = remember(alb) {
        if (alb.coverUrl.isNotBlank() && alb.coverUrl.startsWith("http")) alb.coverUrl
        else alb.songs.firstOrNull { it.coverUrl.isNotBlank() && it.coverUrl.startsWith("http") }?.coverUrl
            ?: com.beatles.player.ui.screens.CURATED_ONBOARD_ARTISTS.find { it.name.equals(alb.artist, ignoreCase = true) }?.imageUrl
            ?: ""
    }

    if (p > 0f) {
        var dragY by remember { mutableFloatStateOf(0f) }
        var showSetDialog by remember { mutableStateOf(false) }
        var newSetName by remember { mutableStateOf("") }
        var isFavorite by remember { mutableStateOf(false) }
        var showInfoDialog by remember { mutableStateOf(false) }

        val screenWpx = with(density) { config.screenWidthDp.dp.toPx() }
        val screenHpx = with(density) { config.screenHeightDp.dp.toPx() }

        val targetArtworkSize = 200.dp
        val targetArtworkSizePx = with(density) { targetArtworkSize.toPx() }
        val targetLeftPx = (screenWpx - targetArtworkSizePx) / 2f
        val targetTopPx = with(density) { 88.dp.toPx() }

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

        val hasRealCover = resolvedCover.isNotBlank() && resolvedCover.startsWith("http")
        val defaultArtColors = remember(alb.title, alb.artist) {
            getAppleMusicGradientColors(alb.title, alb.artist)
        }

        // Adaptive album tone derived from default artwork or metadata
        val albumOverlayColor = if (!hasRealCover) {
            defaultArtColors.first
        } else {
            remember(alb.id, alb.artist) {
                val hue = (kotlin.math.abs(alb.artist.hashCode() * 31 + alb.title.hashCode()) % 360).toFloat()
                Color.hsl(hue, 0.65f, 0.72f)
            }
        }
        val albumPageBg = if (!hasRealCover) {
            if (isDark) defaultArtColors.third.copy(alpha = 0.65f) else defaultArtColors.first.copy(alpha = 0.14f)
        } else {
            bgColor
        }
        val albumTitleBrush = remember(albumOverlayColor) {
            Brush.verticalGradient(
                listOf(
                    Color.White,
                    albumOverlayColor.copy(alpha = 0.90f)
                )
            )
        }

        // Ambient Video Banner Motion Animation
        val infiniteTransition = rememberInfiniteTransition(label = "bannerVideoMotion")
        val bannerScale by infiniteTransition.animateFloat(
            initialValue = 1.15f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(9000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bannerScale"
        )
        val bannerShiftY by infiniteTransition.animateFloat(
            initialValue = -10f,
            targetValue = 10f,
            animationSpec = infiniteRepeatable(
                animation = tween(7000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bannerShiftY"
        )

        val listState = rememberLazyListState()

        // Scroll-driven swipe up parallax and blur calculation for the hero album artwork
        val scrollOffset = if (listState.firstVisibleItemIndex == 0) {
            listState.firstVisibleItemScrollOffset.toFloat()
        } else {
            listState.firstVisibleItemScrollOffset.toFloat() + 800f
        }
        val artTranslateY = (-scrollOffset * 0.75f).coerceAtLeast(-targetTopPx - 200f) * p
        val artBlur = ((scrollOffset / 14f) * p).coerceIn(0f, 24f).dp
        val artScale = (1f - (scrollOffset / 1400f) * p).coerceIn(0.72f, 1f)
        val artAlpha = (1f - (scrollOffset / 340f) * p).coerceIn(0f, 1f)
        val clampedP = p.coerceIn(0f, 1f)
        val curContainerCorner = with(density) { (18.dp.toPx() * (1f - clampedP)).coerceAtLeast(0f).toDp() }
        val curScaleX = (origin.width / screenWpx) + (1f - (origin.width / screenWpx)) * clampedP
        val curScaleY = (origin.height / screenHpx) + (1f - (origin.height / screenHpx)) * clampedP
        val curTransX = origin.left * (1f - clampedP)
        val curTransY = origin.top * (1f - clampedP) + dragY

        Box(modifier = Modifier.fillMaxSize()) {
            // Background scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = (0.40f * clampedP).coerceIn(0f, 1f)))
            )

            // Morphing Card Container (GPU-accelerated RenderThread scaling and translation)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = curScaleX.coerceIn(0.01f, 1.2f)
                        scaleY = curScaleY.coerceIn(0.01f, 1.2f)
                        translationX = curTransX
                        translationY = curTransY
                        transformOrigin = TransformOrigin(0f, 0f)
                        clip = true
                        shape = RoundedCornerShape(curContainerCorner)
                    }
                    .background(albumPageBg)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (dragY > 120f) {
                                    onClose()
                                }
                                dragY = 0f
                            },
                            onVerticalDrag = { _, delta ->
                                if (delta > 0 || dragY > 0f) {
                                    dragY = (dragY + delta).coerceAtLeast(0f)
                                    if (dragY > 220f) {
                                        onClose()
                                    }
                                }
                            }
                        )
                    }
            ) {
                if (p < 0.22f) {
                    if (resolvedCover.isNotBlank() && resolvedCover.startsWith("http")) {
                        AsyncImage(
                            model = resolvedCover,
                            contentDescription = alb.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AppleMusicGradientCover(
                            title = alb.title,
                            artist = alb.artist,
                            modifier = Modifier.fillMaxSize(),
                            showLabel = false
                        )
                    }
                }

                // Surrounding rich detail view content fading and sliding in smoothly
                if (p > 0.05f) {
                    val contentAlpha = ((p - 0.18f) / 0.82f).coerceIn(0f, 1f)
                    val contentSlide = (1f - p) * with(density) { 36.dp.toPx() }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = contentAlpha
                                translationY = contentSlide
                            }
                    ) {
                    // 1. Deep Ambient Blurred Base
                    if (resolvedCover.isNotBlank() && resolvedCover.startsWith("http")) {
                        AsyncImage(
                            model = resolvedCover,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(80.dp)
                                .graphicsLayer { alpha = 0.50f }
                        )
                    } else {
                        AppleMusicGradientCover(
                            title = alb.title,
                            artist = alb.artist,
                            modifier = Modifier.fillMaxSize().blur(80.dp).graphicsLayer { alpha = 0.45f },
                            showLabel = false
                        )
                    }

                    // 2. Parallax Video Motion Banner at top
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0.0f to Color.Black,
                                        0.65f to Color.Black.copy(alpha = 0.80f),
                                        1.0f to Color.Transparent
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                    ) {
                        if (resolvedCover.isNotBlank() && resolvedCover.startsWith("http")) {
                            AsyncImage(
                                model = resolvedCover,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scale(bannerScale)
                                    .offset { IntOffset(0, bannerShiftY.roundToInt()) }
                                    .blur(20.dp)
                            )
                        } else {
                            AppleMusicGradientCover(
                                title = alb.title,
                                artist = alb.artist,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scale(bannerScale)
                                    .offset { IntOffset(0, bannerShiftY.roundToInt()) }
                                    .blur(20.dp),
                                showLabel = false
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.30f),
                                            Color.Black.copy(alpha = if (isDark) 0.85f else 0.60f)
                                        )
                                    )
                                )
                        )
                    }

                    // 3. Scrollable Content (Header, Metadata, Action Pills, Tracklist)
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = targetArtworkSize + 110.dp, bottom = LocalDynamicBottomPadding.current)
                    ) {
                        // Metadata card & Titles
                        item {
                            val heroAlpha = (1f - (scrollOffset / 260f) * p).coerceIn(0f, 1f)
                            val heroScale = (1f - (scrollOffset / 1200f) * p).coerceIn(0.85f, 1f)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .graphicsLayer {
                                        alpha = heroAlpha
                                        scaleX = heroScale
                                        scaleY = heroScale
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = alb.title,
                                    style = TextStyle(brush = albumTitleBrush),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${alb.artist} • ${alb.releaseYear.ifBlank { "2026" }} • ${alb.genre.ifBlank { "Album" }}",
                                    color = textSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                // Action Controls Row: Shuffle, Large Center Play, Save to Set / Library (Favorite removed per request)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val buttonBg = if (!hasRealCover) {
                                        defaultArtColors.first.copy(alpha = if (isDark) 0.30f else 0.20f)
                                    } else {
                                        if (isDark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.08f)
                                    }
                                    val buttonIconTint = if (!hasRealCover) {
                                        if (isDark) Color.White else defaultArtColors.third
                                    } else {
                                        textPrimary
                                    }
                                    val playBtnBg = if (!hasRealCover) defaultArtColors.first else textPrimary
                                    val playBtnIconTint = if (!hasRealCover) Color.White else if (isDark) Color.Black else Color.White

                                    // Shuffle Button (transparent borderless circle)
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(buttonBg)
                                            .clickable {
                                                if (alb.songs.isNotEmpty()) {
                                                    val shuffled = alb.songs.shuffled()
                                                    onPlay(shuffled.first(), shuffled)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Shuffle,
                                            contentDescription = "Shuffle",
                                            tint = buttonIconTint,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    // Large Center Play Button
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(playBtnBg)
                                            .clickable {
                                                if (alb.songs.isNotEmpty()) onPlay(alb.songs.first(), alb.songs)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = playBtnIconTint,
                                            modifier = Modifier.size(38.dp)
                                        )
                                    }

                                    // Save to Library / Add to Set Button (transparent borderless circle)
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(buttonBg)
                                            .clickable { showSetDialog = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.BookmarkAdd,
                                            contentDescription = "Save Album or Add to Set",
                                            tint = buttonIconTint,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }

                        // Tracklist Section Heading
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
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${alb.songs.size} songs",
                                    color = textSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Track Rows
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
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                                ) {
                                    for ((idx, song) in alb.songs.withIndex()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onPlay(song, alb.songs) }
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${idx + 1}",
                                                color = Color.White.copy(alpha = 0.55f),
                                                fontSize = 13.sp,
                                                modifier = Modifier.width(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            val coverToUse = song.coverUrl.ifBlank { resolvedCover }
                                            if (coverToUse.isNotBlank() && coverToUse.startsWith("http")) {
                                                AsyncImage(
                                                    model = coverToUse,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(46.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                )
                                            } else {
                                                AppleMusicGradientCover(
                                                    title = song.title,
                                                    artist = song.artist,
                                                    modifier = Modifier
                                                        .size(46.dp)
                                                        .clip(RoundedCornerShape(8.dp)),
                                                    cornerRadius = 8.dp,
                                                    showLabel = false
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = song.title,
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = song.artist,
                                                    color = Color.White.copy(alpha = 0.70f),
                                                    fontSize = 12.sp,
                                                    maxLines = 1
                                                )
                                            }
                                            val dur = if (song.durationSeconds > 0) {
                                                "${song.durationSeconds / 60}:${String.format("%02d", song.durationSeconds % 60)}"
                                            } else ""
                                            Text(
                                                text = dur,
                                                color = Color.White.copy(alpha = 0.55f),
                                                fontSize = 12.sp
                                            )
                                        }
                                        if (idx < alb.songs.size - 1) {
                                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Pinned Top Navigation Bar (Fixed frosted header, back button, title on scroll)
                    val firstVisibleItem = listState.firstVisibleItemIndex
                    val firstVisibleOffset = listState.firstVisibleItemScrollOffset
                    val dockedAlpha = if (firstVisibleItem > 0) 1f else ((firstVisibleOffset - 80f) / 240f).coerceIn(0f, 1f)
                    val dockedScale = 0.60f + 0.40f * dockedAlpha

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
                                .background(if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f))
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = alb.title,
                            color = textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .padding(horizontal = 12.dp)
                                .graphicsLayer {
                                    alpha = dockedAlpha
                                    scaleX = dockedScale
                                    scaleY = dockedScale
                                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                                }
                        )

                        IconButton(
                            onClick = { showInfoDialog = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f))
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = "Info",
                                tint = textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 5. Shared Morph Artwork: centered hero artwork with swipe-up parallax and progressive blur
            Box(
                modifier = Modifier
                    .offset { IntOffset(targetLeftPx.roundToInt(), (targetTopPx + artTranslateY).roundToInt()) }
                    .size(targetArtworkSize)
                    .graphicsLayer {
                        scaleX = artScale
                        scaleY = artScale
                        alpha = if (p >= 0.95f) artAlpha.coerceIn(0.08f, 1f) else 1f
                    }
                    .then(if (artBlur > 0.dp && p >= 0.95f) Modifier.blur(artBlur) else Modifier)
                    .clip(RoundedCornerShape(22.dp))
                    .shadow(elevation = (12.dp * clampedP).coerceAtLeast(0.dp), shape = RoundedCornerShape(22.dp))
            ) {
                // Live gradient cover as robust foundation
                AppleMusicGradientCover(
                    title = alb.title,
                    artist = alb.artist,
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxSize()
                )
                if (resolvedCover.isNotBlank() && resolvedCover.startsWith("http")) {
                    AsyncImage(
                        model = resolvedCover,
                        contentDescription = alb.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

        // Save Album or Add to Album Set Dialog
        if (showSetDialog) {
            AlertDialog(
                onDismissRequest = { showSetDialog = false },
                containerColor = if (isDark) Color(0xFF1E2024) else Color.White,
                title = {
                    Text("Album Options", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val dialogBtnContainer = if (isDark) Color.White else Color(0xFF141416)
                        val dialogBtnContent = if (isDark) Color.Black else Color.White
                        Button(
                            onClick = {
                                viewModel.saveAlbumToLibrary(alb)
                                showSetDialog = false
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = dialogBtnContainer, contentColor = dialogBtnContent)
                        ) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = dialogBtnContent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Album to Library", color = dialogBtnContent, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = textPrimary.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(14.dp))

                        Text("Add to Album Set", color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Group multiple albums together into a collection.", color = textSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        if (state.albumSets.isNotEmpty()) {
                            state.albumSets.forEach { set ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.addAlbumToSet(set.id, alb)
                                            showSetDialog = false
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = textPrimary.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(set.name, color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Text("${set.albums.size} albums", color = textSecondary, fontSize = 10.sp)
                                    }
                                    Icon(Icons.Default.Add, contentDescription = "Add", tint = textPrimary.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = newSetName,
                                onValueChange = { newSetName = it },
                                textStyle = TextStyle(color = textPrimary, fontSize = 13.sp),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .background(textPrimary.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                decorationBox = { inner ->
                                    if (newSetName.isEmpty()) {
                                        Text("New set name...", color = textSecondary, fontSize = 13.sp)
                                    }
                                    inner()
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newSetName.isNotBlank()) {
                                        viewModel.createAlbumSet(newSetName.trim(), alb)
                                        showSetDialog = false
                                    }
                                },
                                enabled = newSetName.isNotBlank(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = dialogBtnContainer, disabledContainerColor = textPrimary.copy(alpha = 0.15f))
                            ) {
                                Text("Create", color = if (newSetName.isNotBlank()) dialogBtnContent else textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSetDialog = false }) {
                        Text("Close", color = textSecondary)
                    }
                }
            )
        }

        // Info Dialog
        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                containerColor = if (isDark) Color(0xFF1E2024) else Color.White,
                title = { Text(alb.title, color = textPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Artist: ${alb.artist}", color = textSecondary, fontSize = 14.sp)
                        Text("Genre: ${alb.genre.ifBlank { "Pop" }}", color = textSecondary, fontSize = 14.sp)
                        Text("Released: ${alb.releaseYear.ifBlank { "2026" }}", color = textSecondary, fontSize = 14.sp)
                        Text("Tracks: ${alb.songs.size}", color = textSecondary, fontSize = 14.sp)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showInfoDialog = false }) {
                        Text("OK", color = textPrimary)
                    }
                }
            )
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
