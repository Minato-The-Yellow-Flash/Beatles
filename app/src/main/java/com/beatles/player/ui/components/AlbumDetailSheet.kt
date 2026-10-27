package com.beatles.player.ui.components

import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.beatles.player.ui.glassRefraction
import com.beatles.player.ui.motion.liquidPress
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.view.TextureView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
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
    val view = LocalView.current
    val isDark = isDarkTheme(state.themeMode)
    val bgColor = if (isDark) Color(0xFF141416) else Color(0xFFF6F7F9)
    val textPrimary = if (isDark) Color.White else Color(0xFF141416)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF6B7280)
    val cardBg = if (isDark) Color(0xFF1E2024).copy(alpha = 0.70f) else Color(0xFFF4F5F7)

    var currentAlbum by remember { mutableStateOf<Album?>(null) }
    var currentOriginBounds by remember { mutableStateOf<Rect?>(null) }
    val progress = remember { Animatable(0f) }
    var isClosing by remember { mutableStateOf(false) }
    var isVideoPlaying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun handleClose() {
        if (isClosing) return
        isClosing = true
        isVideoPlaying = false
        scope.launch {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.84f,
                    stiffness = 380f
                )
            )
            currentAlbum = null
            currentOriginBounds = null
            isClosing = false
            onClose()
        }
    }

    LaunchedEffect(album) {
        if (album != null) {
            currentAlbum = album
            if (sourceBounds != null) {
                currentOriginBounds = sourceBounds
            }
            isClosing = false
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.84f,
                    stiffness = 380f
                )
            )
        } else if (!isClosing && currentAlbum != null) {
            handleClose()
        }
    }

    val p = progress.value
    val alb = currentAlbum ?: return

    BackHandler(enabled = (album != null || currentAlbum != null || p > 0.01f)) {
        handleClose()
    }

    val resolvedCover = remember(alb) {
        val raw = if (alb.coverUrl.isNotBlank() && alb.coverUrl.startsWith("http")) alb.coverUrl
        else alb.songs.firstOrNull { it.coverUrl.isNotBlank() && it.coverUrl.startsWith("http") }?.coverUrl
            ?: com.beatles.player.ui.screens.CURATED_ONBOARD_ARTISTS.find { it.name.equals(alb.artist, ignoreCase = true) }?.imageUrl
            ?: ""
        com.beatles.player.data.MusicRepository.getHighestQualityCover(raw)
    }

    if (p > 0f) {
        var dragY by remember { mutableFloatStateOf(0f) }
        var showSetDialog by remember { mutableStateOf(false) }
        var newSetName by remember { mutableStateOf("") }
        var isFavorite by remember { mutableStateOf(false) }
        var showInfoDialog by remember { mutableStateOf(false) }

        val screenWpx = with(density) { config.screenWidthDp.dp.toPx() }
        val screenHpx = with(density) { config.screenHeightDp.dp.toPx() }

        val targetArtworkSizeDp = 240.dp
        val targetArtworkSizePx = with(density) { targetArtworkSizeDp.toPx() }
        val targetArtworkWidthPx = targetArtworkSizePx
        val targetArtworkHeightDp = targetArtworkSizeDp
        val targetArtworkHeightPx = targetArtworkSizePx
        val targetLeftPx = (screenWpx - targetArtworkSizePx) / 2f
        val targetTopPx = with(density) { 68.dp.toPx() }
        val targetCornerPx = with(density) { 20.dp.toPx() }

        val origin = currentOriginBounds ?: Rect(
            screenWpx / 2f - 100f,
            screenHpx / 2f - 100f,
            screenWpx / 2f + 100f,
            screenHpx / 2f + 100f
        )


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
        val artTranslateY = (-scrollOffset * 0.55f).coerceAtLeast(-targetArtworkHeightPx) * p
        val artBlur = ((scrollOffset / 20f) * p).coerceIn(0f, 18f).dp
        val artScale = (1f - (scrollOffset / 2200f) * p).coerceIn(0.85f, 1f)
        val artAlpha = (1f - (scrollOffset / 340f) * p).coerceIn(0f, 1f)
        val originCardWidth = origin.width.coerceAtLeast(10f)
        val originCardHeight = origin.height.coerceAtLeast(10f)
        val originCornerPx = if (originCardWidth <= with(density) { 72.dp.toPx() }) originCardWidth / 2f else with(density) { 18.dp.toPx() }

        val clampedP = p.coerceIn(0f, 1f)
        val smoothP = (clampedP * clampedP * (3f - 2f * clampedP)).coerceIn(0f, 1f)
        val sheetAlpha = smoothP
        val sheetSlideY = (1f - smoothP) * with(density) { 36.dp.toPx() } + dragY

        val curArtLeft = origin.left + (targetLeftPx - origin.left) * smoothP
        val curArtTop = origin.top + (targetTopPx + artTranslateY - origin.top) * smoothP + dragY
        val curArtWidth = originCardWidth + (targetArtworkWidthPx - originCardWidth) * smoothP
        val curArtHeight = originCardHeight + (targetArtworkHeightPx - originCardHeight) * smoothP
        val curArtCorner = originCornerPx + (targetCornerPx - originCornerPx) * smoothP

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(55f)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (dragY > 120f) {
                                handleClose()
                            }
                            dragY = 0f
                        },
                        onVerticalDrag = { _, delta ->
                            if (delta > 0 || dragY > 0f) {
                                dragY = (dragY + delta).coerceAtLeast(0f)
                                if (dragY > 220f) {
                                    handleClose()
                                }
                            }
                        }
                    )
                }
        ) {
            // Background scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = (0.50f * clampedP).coerceIn(0f, 1f) }
                    .background(Color.Black)
            )

            // Full-screen Sheet with Ambient Blur (GPU accelerated entrance/exit, zero layout thrashing)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = sheetAlpha
                        translationY = sheetSlideY
                    }
                    .background(albumPageBg)
            ) {
                // 1. Deep Ambient Blurred Base (100% visible, vibrant ambient blur!)
                if (resolvedCover.isNotBlank() && resolvedCover.startsWith("http")) {
                    AsyncImage(
                        model = resolvedCover,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(80.dp)
                            .graphicsLayer { alpha = 0.55f }
                    )
                } else {
                    AppleMusicGradientCover(
                        title = alb.title,
                        artist = alb.artist,
                        modifier = Modifier.fillMaxSize().blur(80.dp).graphicsLayer { alpha = 0.50f },
                        showLabel = false
                    )
                }
            }

            // 2. Shared Morph Hero Artwork: glides seamlessly from tapped origin card to centered hero card
            val albumVideoUrl = alb.videoUrl ?: alb.songs.firstOrNull()?.videoUrl
            val hasVideo = !albumVideoUrl.isNullOrBlank()

            Box(
                modifier = Modifier
                    .offset { IntOffset(curArtLeft.roundToInt(), curArtTop.roundToInt()) }
                    .size(
                        width = with(density) { curArtWidth.toDp() },
                        height = with(density) { curArtHeight.toDp() }
                    )
                    .graphicsLayer {
                        scaleX = 1f
                        scaleY = 1f
                        alpha = if (clampedP >= 0.95f) artAlpha.coerceIn(0f, 1f) else 1f
                    }
                    .shadow(
                        elevation = (16.dp * clampedP * artAlpha).coerceAtLeast(0.dp),
                        shape = RoundedCornerShape(with(density) { curArtCorner.toDp() })
                    )
                    .clip(RoundedCornerShape(with(density) { curArtCorner.toDp() }))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (hasVideo && clampedP >= 0.95f) {
                            Haptics.click(view)
                            isVideoPlaying = !isVideoPlaying
                        }
                    }
            ) {
                // Base Art
                AppleMusicGradientCover(
                    title = alb.title,
                    artist = alb.artist,
                    cornerRadius = with(density) { curArtCorner.toDp() },
                    modifier = Modifier.fillMaxSize()
                )
                if (resolvedCover.isNotBlank() && resolvedCover.startsWith("http")) {
                    AsyncImage(
                        model = resolvedCover,
                        contentDescription = alb.title,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Controlled Video Playback: Only mounts when tapped & sheet fully open (zero expansion jitter)
                if (hasVideo && isVideoPlaying && clampedP >= 0.95f) {
                    val ctx = LocalContext.current
                    var isAlbumVideoReady by remember(albumVideoUrl) { mutableStateOf(false) }
                    var albVideoWidth by remember(albumVideoUrl) { mutableFloatStateOf(1f) }
                    var albVideoHeight by remember(albumVideoUrl) { mutableFloatStateOf(1f) }
                    var albTvRef by remember(albumVideoUrl) { mutableStateOf<TextureView?>(null) }
                    val exoPlayer = remember(albumVideoUrl) {
                        val uri = when {
                            albumVideoUrl!!.startsWith("raw://") -> {
                                val resName = albumVideoUrl.removePrefix("raw://")
                                val resId = ctx.resources.getIdentifier(resName, "raw", ctx.packageName)
                                if (resId != 0) android.net.Uri.parse("android.resource://${ctx.packageName}/$resId")
                                else android.net.Uri.parse("android.resource://${ctx.packageName}/raw/$resName")
                            }
                            albumVideoUrl.startsWith("android.resource://") -> {
                                val parts = albumVideoUrl.split("/")
                                val resName = parts.lastOrNull() ?: ""
                                val resId = if (resName.isNotEmpty()) ctx.resources.getIdentifier(resName, "raw", ctx.packageName) else 0
                                if (resId != 0) android.net.Uri.parse("android.resource://${ctx.packageName}/$resId")
                                else android.net.Uri.parse(albumVideoUrl)
                            }
                            else -> android.net.Uri.parse(albumVideoUrl)
                        }
                        ExoPlayer.Builder(ctx).build().apply {
                            setMediaItem(MediaItem.fromUri(uri))
                            repeatMode = Player.REPEAT_MODE_ALL
                            volume = 0f
                            addListener(object : Player.Listener {
                                override fun onRenderedFirstFrame() {
                                    isAlbumVideoReady = true
                                    albTvRef?.let { tv -> tv.post { applyTopCenterCropMatrix(tv, albVideoWidth, albVideoHeight) } }
                                }
                                override fun onPlaybackStateChanged(state: Int) {
                                    if (state == Player.STATE_READY) {
                                        isAlbumVideoReady = true
                                        albTvRef?.let { tv -> tv.post { applyTopCenterCropMatrix(tv, albVideoWidth, albVideoHeight) } }
                                    }
                                }
                                override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                                    if (videoSize.width > 0 && videoSize.height > 0) {
                                        albVideoWidth = videoSize.width.toFloat()
                                        albVideoHeight = videoSize.height.toFloat()
                                        albTvRef?.let { tv -> tv.post { applyTopCenterCropMatrix(tv, albVideoWidth, albVideoHeight) } }
                                    }
                                }
                            })
                            prepare()
                            playWhenReady = true
                        }
                    }
                    DisposableEffect(exoPlayer) {
                        onDispose { exoPlayer.release() }
                    }
                    val albumVideoAlpha by animateFloatAsState(
                        targetValue = if (isAlbumVideoReady) 1f else 0f,
                        animationSpec = tween(300),
                        label = "albumVideoAlpha"
                    )
                    AndroidView(
                        factory = { c ->
                            TextureView(c).apply {
                                albTvRef = this
                                exoPlayer.setVideoTextureView(this)
                                addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                                    if ((right - left) != (oldRight - oldLeft) || (bottom - top) != (oldBottom - oldTop)) {
                                        applyTopCenterCropMatrix(this, albVideoWidth, albVideoHeight)
                                    }
                                }
                            }
                        },
                        update = { tv ->
                            albTvRef = tv
                            exoPlayer.setVideoTextureView(tv)
                            tv.post { applyTopCenterCropMatrix(tv, albVideoWidth, albVideoHeight) }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = albumVideoAlpha
                            }
                    )
                }

                // Apple Music Motion badge pill overlay at bottom-end
                if (hasVideo && clampedP >= 0.95f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .shadow(3.dp, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isVideoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isVideoPlaying) "MOTION" else "PLAY LIVE",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }
                }
            }

            // 3. Scrollable Content Layer (Metadata, Action Pills, Tracklist & Pinned Nav Bar)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = sheetAlpha
                        translationY = sheetSlideY
                    }
            ) {
                // More albums by this artist
                val otherAlbums = remember(alb, state.albums) {
                    state.albums.filter {
                        it.id != alb.id && (it.artist.equals(alb.artist, ignoreCase = true) || alb.artist.contains(it.artist, ignoreCase = true))
                    }
                }

                // Similar artists
                val similarArtists = remember(alb.artist) {
                    val mapped = com.beatles.player.ui.screens.INSTANT_RELATED_ARTISTS[alb.artist] ?: emptyList()
                    if (mapped.isNotEmpty()) mapped
                    else {
                        val currArt = com.beatles.player.ui.screens.CURATED_ONBOARD_ARTISTS.find { it.name.equals(alb.artist, ignoreCase = true) }
                        com.beatles.player.ui.screens.CURATED_ONBOARD_ARTISTS.filter {
                            !it.name.equals(alb.artist, ignoreCase = true) && (currArt == null || it.genre == currArt.genre || it.language == currArt.language)
                        }.take(6)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = with(density) { targetTopPx.toDp() } + targetArtworkHeightDp + 24.dp, bottom = LocalDynamicBottomPadding.current)
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
                                val displayAlbumTitle = remember(alb.title) {
                                    com.beatles.player.data.IndicTransliteration.toLatinTitle(alb.title)
                                }
                                val displayAlbumArtist = remember(alb.artist) {
                                    com.beatles.player.data.IndicTransliteration.toLatinTitle(alb.artist)
                                }

                                Text(
                                    text = displayAlbumTitle,
                                    style = TextStyle(brush = albumTitleBrush),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (alb.releaseYear.isNotBlank()) "${displayAlbumArtist} • ${alb.releaseYear}" else displayAlbumArtist,
                                    color = textSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.clickable {
                                        Haptics.click(view)
                                        handleClose()
                                        viewModel.openArtist(alb.artist)
                                    }
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
                                    val buttonBg = if (isDark) Color(0xFF383842).copy(alpha = 0.35f) else Color(0xFFE2E2E2).copy(alpha = 0.45f)
                                    val buttonIconTint = if (isDark) Color.White else Color(0xFF141416)
                                    val playBtnBg = if (isDark) Color.White else Color(0xFF141416)
                                    val playBtnIconTint = if (isDark) Color.Black else Color.White

                                    // Shuffle Button (liquid glass circle)
                                    val shuffleInteraction = remember { MutableInteractionSource() }
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .liquidPress(shuffleInteraction)
                                            .shadow(3.dp, CircleShape, spotColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.15f))
                                            .clip(CircleShape)
                                            .background(buttonBg)
                                            .glassRefraction(25.dp, isDark = isDark, isCircle = true)
                                            .clickable(
                                                interactionSource = shuffleInteraction,
                                                indication = null
                                            ) {
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

                                    // Large Center Play Button (liquid glass circle)
                                    val playInteraction = remember { MutableInteractionSource() }
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .liquidPress(playInteraction)
                                            .shadow(4.dp, CircleShape, spotColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.18f))
                                            .clip(CircleShape)
                                            .background(playBtnBg)
                                            .glassRefraction(32.dp, isDark = isDark, isCircle = true)
                                            .clickable(
                                                interactionSource = playInteraction,
                                                indication = null
                                            ) {
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

                                    // Save to Library / Add to Set Button (liquid glass circle)
                                    val saveInteraction = remember { MutableInteractionSource() }
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .liquidPress(saveInteraction)
                                            .shadow(3.dp, CircleShape, spotColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.15f))
                                            .clip(CircleShape)
                                            .background(buttonBg)
                                            .glassRefraction(25.dp, isDark = isDark, isCircle = true)
                                            .clickable(
                                                interactionSource = saveInteraction,
                                                indication = null
                                            ) { showSetDialog = true },
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

                        // 1. More Albums by this Artist
                        if (otherAlbums.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(28.dp))
                                Text(
                                    "More by ${alb.artist}",
                                    color = textPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(otherAlbums) { otherAlb ->
                                        var otherBounds by remember { mutableStateOf<Rect?>(null) }
                                        Column(
                                            modifier = Modifier
                                                .width(135.dp)
                                                .clickable {
                                                    Haptics.click(view)
                                                    viewModel.setSelectedAlbum(otherAlb, otherBounds)
                                                }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(135.dp)
                                                    .onGloballyPositioned { otherBounds = it.boundsInRoot() }
                                                    .clip(RoundedCornerShape(14.dp))
                                            ) {
                                                AppleMusicGradientCover(
                                                    title = otherAlb.title,
                                                    artist = otherAlb.artist,
                                                    modifier = Modifier.fillMaxSize(),
                                                    cornerRadius = 14.dp,
                                                    showLabel = false
                                                )
                                                if (otherAlb.coverUrl.isNotBlank() && otherAlb.coverUrl.startsWith("http")) {
                                                    AsyncImage(
                                                        model = otherAlb.coverUrl,
                                                        contentDescription = otherAlb.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                otherAlb.title,
                                                color = textPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                            Text(
                                                "${otherAlb.releaseYear} • ${otherAlb.songs.size.coerceAtLeast(1)} songs",
                                                color = textSecondary,
                                                fontSize = 11.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Similar Artists
                        if (similarArtists.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(28.dp))
                                Text(
                                    "Similar Artists",
                                    color = textPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(similarArtists) { simArt ->
                                        var simBounds by remember { mutableStateOf<Rect?>(null) }
                                        Column(
                                            modifier = Modifier
                                                .width(105.dp)
                                                .clickable {
                                                    Haptics.click(view)
                                                    handleClose()
                                                    viewModel.openArtist(simArt.name, simBounds)
                                                },
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(92.dp)
                                                    .onGloballyPositioned { simBounds = it.boundsInRoot() }
                                                    .clip(CircleShape)
                                            ) {
                                                AppleMusicGradientCover(
                                                    title = simArt.name,
                                                    artist = simArt.genre,
                                                    modifier = Modifier.fillMaxSize(),
                                                    cornerRadius = 46.dp,
                                                    showLabel = false
                                                )
                                                if (simArt.imageUrl.isNotBlank() && simArt.imageUrl.startsWith("http")) {
                                                    AsyncImage(
                                                        model = simArt.imageUrl,
                                                        contentDescription = simArt.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                simArt.name,
                                                color = textPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                            Text(
                                                simArt.genre,
                                                color = textSecondary,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
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
                        val backInteraction = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .liquidPress(backInteraction)
                                .shadow(3.dp, CircleShape, spotColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.15f))
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF383842).copy(alpha = 0.35f) else Color(0xFFE2E2E2).copy(alpha = 0.45f))
                                .glassRefraction(20.dp, isDark = isDark, isCircle = true)
                                .clickable(interactionSource = backInteraction, indication = null) { handleClose() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = if (isDark) Color.White else Color(0xFF141416),
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

                        val infoInteraction = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .liquidPress(infoInteraction)
                                .shadow(3.dp, CircleShape, spotColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.15f))
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF383842).copy(alpha = 0.35f) else Color(0xFFE2E2E2).copy(alpha = 0.45f))
                                .glassRefraction(20.dp, isDark = isDark, isCircle = true)
                                .clickable(interactionSource = infoInteraction, indication = null) { showInfoDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = "Info",
                                tint = if (isDark) Color.White else Color(0xFF141416),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
            }
        }

        // Minimal Floating Liquid Glass Save Album Dialog
        if (showSetDialog) {
            val isSaved = state.albums.any { it.id == alb.id }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.50f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showSetDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .shadow(8.dp, RoundedCornerShape(26.dp), spotColor = if (isDark) Color.Black.copy(alpha = 0.50f) else Color.Black.copy(alpha = 0.18f))
                        .clip(RoundedCornerShape(26.dp))
                        .background(if (isDark) Color(0xFF1E2024).copy(alpha = 0.90f) else Color.White.copy(alpha = 0.95f))
                        .glassRefraction(26.dp, isDark = isDark)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                        .padding(22.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Save Album", color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { showSetDialog = false }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = textSecondary, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick 1-Tap Save to Library Button
                        val saveInteraction = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .liquidPress(saveInteraction)
                                .shadow(2.dp, RoundedCornerShape(50), spotColor = AccentRed.copy(alpha = 0.35f))
                                .clip(RoundedCornerShape(50))
                                .background(if (isSaved) Color(0xFF2E7D32) else AccentRed)
                                .glassRefraction(50.dp, isDark = isDark)
                                .clickable(interactionSource = saveInteraction, indication = null) {
                                    viewModel.saveAlbumToLibrary(alb)
                                    showSetDialog = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isSaved) Icons.Default.Check else Icons.Default.BookmarkAdd,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (isSaved) "Album Saved in Library" else "Save Album to Library",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Add to set / collections section
                        if (state.albumSets.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Or add to collection:", color = textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                state.albumSets.take(3).forEach { set ->
                                    val setRowInteraction = remember { MutableInteractionSource() }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .liquidPress(setRowInteraction)
                                            .shadow(1.dp, RoundedCornerShape(12.dp), spotColor = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.06f))
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isDark) Color(0xFF26282E).copy(alpha = 0.60f) else Color(0xFFF4F5F7).copy(alpha = 0.80f))
                                            .glassRefraction(12.dp, isDark = isDark)
                                            .clickable(interactionSource = setRowInteraction, indication = null) {
                                                viewModel.addAlbumToSet(set.id, alb)
                                                showSetDialog = false
                                            }
                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = AccentRed, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(set.name, color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                        Icon(Icons.Default.Add, contentDescription = "Add", tint = textSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Minimal Floating Liquid Glass Info Dialog
        if (showInfoDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.50f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showInfoDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .shadow(8.dp, RoundedCornerShape(26.dp), spotColor = if (isDark) Color.Black.copy(alpha = 0.50f) else Color.Black.copy(alpha = 0.18f))
                        .clip(RoundedCornerShape(26.dp))
                        .background(if (isDark) Color(0xFF1E2024).copy(alpha = 0.90f) else Color.White.copy(alpha = 0.95f))
                        .glassRefraction(26.dp, isDark = isDark)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                        .padding(22.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(alb.title, color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
                            IconButton(onClick = { showInfoDialog = false }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = textSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Artist", color = textSecondary, fontSize = 13.sp)
                                Text(alb.artist, color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Genre", color = textSecondary, fontSize = 13.sp)
                                Text(alb.genre.ifBlank { "Pop" }, color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Released", color = textSecondary, fontSize = 13.sp)
                                Text(alb.releaseYear.ifBlank { "2026" }, color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tracks", color = textSecondary, fontSize = 13.sp)
                                Text("${alb.songs.size} songs", color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        val okInteraction = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .liquidPress(okInteraction)
                                .shadow(2.dp, RoundedCornerShape(50), spotColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.12f))
                                .clip(RoundedCornerShape(50))
                                .background(if (isDark) Color(0xFF383842).copy(alpha = 0.45f) else Color(0xFFE2E2E2).copy(alpha = 0.65f))
                                .glassRefraction(50.dp, isDark = isDark)
                                .clickable(interactionSource = okInteraction, indication = null) { showInfoDialog = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("OK", color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
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
