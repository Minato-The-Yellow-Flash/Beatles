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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.beatles.player.ui.glassRefraction
import com.beatles.player.ui.motion.liquidPress
import com.beatles.player.ui.theme.QualityManager
import com.beatles.player.ui.theme.VisualQuality
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.beatles.player.data.Album
import com.beatles.player.data.AlbumSet
import com.beatles.player.data.Song
import com.beatles.player.ui.theme.isDarkTheme
import com.beatles.player.ui.theme.LocalDynamicBottomPadding
import com.beatles.player.viewmodel.PlayerUiState
import com.beatles.player.viewmodel.PlayerViewModel
import kotlin.math.roundToInt

private val AccentRed = Color(0xFFF82C38)

/**
 * Premium Fluid Album Set Detail Overlay:
 * Smoothly morphs from the tapped album set preview card into an interactive carousel of albums.
 * The middle album sits in the center by default, and swiping left/right navigates seamlessly
 * between all albums in the collection and their full tracklists.
 */
@Composable
fun FluidAlbumSetDetailOverlay(
    albumSet: AlbumSet?,
    sourceBounds: Rect?,
    state: PlayerUiState,
    viewModel: PlayerViewModel,
    onClose: () -> Unit,
    onPlay: (Song, List<Song>) -> Unit,
    onPlayAlbumSet: (AlbumSet) -> Unit = {}
) {
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val view = LocalView.current
    val isDark = isDarkTheme(state.themeMode)
    val bgColor = if (isDark) Color(0xFF141416) else Color(0xFFF6F7F9)
    val textPrimary = if (isDark) Color.White else Color(0xFF141416)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF6B7280)
    val cardBg = if (isDark) Color(0xFF1E2024).copy(alpha = 0.70f) else Color(0xFFF4F5F7)

    var currentSet by remember { mutableStateOf<AlbumSet?>(null) }
    var currentOriginBounds by remember { mutableStateOf<Rect?>(null) }
    val progress = remember { Animatable(0f) }
    var isClosing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun handleClose() {
        if (isClosing) return
        isClosing = true
        scope.launch {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 260,
                    easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
                )
            )
            currentSet = null
            currentOriginBounds = null
            isClosing = false
            onClose()
        }
    }

    LaunchedEffect(albumSet) {
        if (albumSet != null) {
            currentSet = albumSet
            if (sourceBounds != null) {
                currentOriginBounds = sourceBounds
            }
            isClosing = false
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.84f,
                    stiffness = 320f
                )
            )
        } else if (!isClosing && currentSet != null) {
            handleClose()
        }
    }

    val p = progress.value
    val set = currentSet ?: return
    val albums = set.albums ?: emptyList()
    if (albums.isEmpty()) return

    BackHandler(enabled = (albumSet != null || currentSet != null || p > 0.01f)) {
        handleClose()
    }

    if (p > 0f) {
        var dragY by remember { mutableFloatStateOf(0f) }

        // Start at the middle album so the mid album sits proudly in the center
        val midIndex = (albums.size / 2).coerceAtLeast(0)
        val pagerState = rememberPagerState(initialPage = midIndex) { albums.size }
        val view = LocalView.current
        val overlayHazeState = remember { HazeState() }

        var hasInitializedPager by remember { mutableStateOf(false) }
        LaunchedEffect(pagerState.currentPage) {
            if (hasInitializedPager) {
                Haptics.tick(view)
            } else {
                hasInitializedPager = true
            }
        }

        val activeAlbum = albums.getOrNull(pagerState.currentPage) ?: albums.first()
        val resolvedCover = remember(activeAlbum) {
            if (activeAlbum.coverUrl.isNotBlank() && activeAlbum.coverUrl.startsWith("http")) activeAlbum.coverUrl
            else activeAlbum.songs.firstOrNull { it.coverUrl.isNotBlank() && it.coverUrl.startsWith("http") }?.coverUrl
                ?: com.beatles.player.ui.screens.CURATED_ONBOARD_ARTISTS.find { it.name.equals(activeAlbum.artist, ignoreCase = true) }?.imageUrl
                ?: ""
        }

        val clampedP = p.coerceIn(0f, 1f)
        val sheetAlpha = clampedP.coerceIn(0f, 1f)
        val sheetSlideY = (1f - clampedP) * with(density) { 36.dp.toPx() } + dragY
        val origin = currentOriginBounds
        val config = androidx.compose.ui.platform.LocalConfiguration.current
        val screenWpx = with(density) { config.screenWidthDp.dp.toPx() }
        val screenHpx = with(density) { config.screenHeightDp.dp.toPx() }
        val morphProgress = clampedP
        val sheetScaleX = if (origin != null) (origin.width / screenWpx) + (1f - (origin.width / screenWpx)) * morphProgress else 1f
        val sheetScaleY = if (origin != null) (origin.height / screenHpx) + (1f - (origin.height / screenHpx)) * morphProgress else 1f
        val originCenterX = origin?.center?.x ?: (screenWpx / 2f)
        val originCenterY = origin?.center?.y ?: (screenHpx / 2f)
        val morphTransX = (originCenterX - screenWpx / 2f) * (1f - morphProgress)
        val morphTransY = (originCenterY - screenHpx / 2f) * (1f - morphProgress) + sheetSlideY
        val morphCorner = if (origin != null) (26.dp * (1f - morphProgress)) else (28.dp * (1f - clampedP))

        // Video motion animation for ambient banner
        val infiniteTransition = rememberInfiniteTransition(label = "setBannerMotion")
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

        Box(
            modifier = Modifier
                .fillMaxSize()
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

            // Full-screen Sheet with Ambient Blur & Morph Transition
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = sheetAlpha
                        translationX = morphTransX
                        translationY = morphTransY
                        this.scaleX = sheetScaleX
                        this.scaleY = sheetScaleY
                    }
                    .clip(RoundedCornerShape(morphCorner))
                    .background(bgColor)
            ) {
                // Content layer captured by haze
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(state = overlayHazeState)
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
                            .graphicsLayer { alpha = 0.55f }
                    )
                } else {
                    AppleMusicGradientCover(
                        title = activeAlbum.title,
                        artist = activeAlbum.artist,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(80.dp)
                            .graphicsLayer { alpha = 0.50f },
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
                            title = activeAlbum.title,
                            artist = activeAlbum.artist,
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

                // 3. Horizontal Pager: Swiping left and right reveals the other albums in this set
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val alb = albums[page]
                    val albSongs = alb.songs ?: emptyList()
                    val albCover = remember(alb) {
                        if (alb.coverUrl.isNotBlank() && alb.coverUrl.startsWith("http")) alb.coverUrl
                        else alb.songs.firstOrNull { it.coverUrl.isNotBlank() && it.coverUrl.startsWith("http") }?.coverUrl
                            ?: com.beatles.player.ui.screens.CURATED_ONBOARD_ARTISTS.find { it.name.equals(alb.artist, ignoreCase = true) }?.imageUrl
                            ?: ""
                    }
                    val pageListState = rememberLazyListState()

                    // Scroll-driven parallax, scale-down, blur, and 100% disappearance matching expanded album view
                    val pageScrollOffset = if (pageListState.firstVisibleItemIndex == 0) {
                        pageListState.firstVisibleItemScrollOffset.toFloat()
                    } else {
                        pageListState.firstVisibleItemScrollOffset.toFloat() + 800f
                    }
                    val pageArtTranslateY = (-pageScrollOffset * 0.75f).coerceAtLeast(-250f)
                    val pageArtBlur = (pageScrollOffset / 14f).coerceIn(0f, 24f).dp
                    val pageArtScale = (1f - (pageScrollOffset / 1400f)).coerceIn(0.72f, 1f)
                    val pageArtAlpha = (1f - (pageScrollOffset / 260f)).coerceIn(0f, 1f)

                    LazyColumn(
                        state = pageListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 100.dp, bottom = LocalDynamicBottomPadding.current),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Hero Artwork & Titles
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // 200dp Hero Card (Tapping opens the full expanded album sheet)
                                var heroBounds by remember { mutableStateOf<Rect?>(null) }
                                Box(
                                    modifier = Modifier
                                        .size(200.dp)
                                        .onGloballyPositioned { heroBounds = it.boundsInRoot() }
                                        .clickable {
                                            Haptics.click(view)
                                            viewModel.setSelectedAlbum(alb, heroBounds)
                                        }
                                        .graphicsLayer {
                                            translationY = pageArtTranslateY
                                            scaleX = pageArtScale
                                            scaleY = pageArtScale
                                            alpha = pageArtAlpha
                                        }
                                        .then(if (pageArtBlur > 0.dp && pageArtAlpha > 0.01f) Modifier.blur(pageArtBlur) else Modifier)
                                        .shadow(
                                            elevation = (12.dp * pageArtAlpha).coerceAtLeast(0.dp),
                                            shape = RoundedCornerShape(22.dp),
                                            spotColor = if (isDark) Color.Black.copy(alpha = 0.65f) else Color.Black.copy(alpha = 0.25f)
                                        )
                                        .clip(RoundedCornerShape(22.dp))
                                        .border(
                                            0.5.dp,
                                            if (isDark) Color.White.copy(alpha = 0.20f * pageArtAlpha) else Color.Black.copy(alpha = 0.10f * pageArtAlpha),
                                            RoundedCornerShape(22.dp)
                                        )
                                ) {
                                    AppleMusicGradientCover(
                                        title = alb.title,
                                        artist = alb.artist,
                                        modifier = Modifier.fillMaxSize(),
                                        cornerRadius = 22.dp,
                                        showLabel = false
                                    )
                                    if (albCover.isNotBlank() && albCover.startsWith("http")) {
                                        AsyncImage(
                                            model = albCover,
                                            contentDescription = alb.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                val displayAlbumTitle = remember(alb.title) {
                                    com.beatles.player.data.IndicTransliteration.toLatinTitle(alb.title)
                                }
                                val displayAlbumArtist = remember(alb.artist) {
                                    com.beatles.player.data.IndicTransliteration.toLatinTitle(alb.artist)
                                }

                                Text(
                                    text = displayAlbumTitle,
                                    color = textPrimary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (alb.releaseYear.isNotBlank()) "${displayAlbumArtist} • ${alb.releaseYear}" else displayAlbumArtist,
                                    color = textSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                // Liquid Glass Action Controls
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val buttonBg = if (isDark) Color(0xFF383842).copy(alpha = 0.35f) else Color(0xFFE2E2E2).copy(alpha = 0.45f)
                                    val buttonIconTint = if (isDark) Color.White else Color(0xFF141416)
                                    val playBtnBg = if (isDark) Color.White else Color(0xFF141416)
                                    val playBtnIconTint = if (isDark) Color.Black else Color.White

                                    // Shuffle Button (50dp liquid glass circle)
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
                                                if (albSongs.isNotEmpty()) {
                                                    val shuffled = albSongs.shuffled()
                                                    onPlay(shuffled.first(), shuffled)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Shuffle,
                                            contentDescription = "Shuffle",
                                            tint = buttonIconTint,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    // Center Play Button (64dp liquid glass circle)
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
                                                if (albSongs.isNotEmpty()) onPlay(albSongs.first(), albSongs)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = playBtnIconTint,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }

                                    // Play Entire Set Button (50dp liquid glass circle)
                                    val setPlayInteraction = remember { MutableInteractionSource() }
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .liquidPress(setPlayInteraction)
                                            .shadow(3.dp, CircleShape, spotColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.15f))
                                            .clip(CircleShape)
                                            .background(buttonBg)
                                            .glassRefraction(25.dp, isDark = isDark, isCircle = true)
                                            .clickable(
                                                interactionSource = setPlayInteraction,
                                                indication = null
                                            ) {
                                                onPlayAlbumSet(set)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.FolderSpecial,
                                            contentDescription = "Play Set",
                                            tint = buttonIconTint,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }

                        // Tracklist Header
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tracks (${albSongs.size})",
                                    color = textPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Swipe to switch album",
                                    color = AccentRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Tracklist items
                        if (albSongs.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = AccentRed, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                                }
                            }
                        } else {
                            itemsIndexed(albSongs, key = { sIdx, s -> "set_alb_${alb.id}_${s.id}_$sIdx" }) { sIdx, song ->
                                val itemInteraction = remember { MutableInteractionSource() }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f))
                                        .clickable(interactionSource = itemInteraction, indication = null) {
                                            onPlay(song, albSongs)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${sIdx + 1}",
                                        color = textSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.width(26.dp)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    ) {
                                        AppleMusicGradientCover(
                                            title = song.title,
                                            artist = song.artist,
                                            modifier = Modifier.fillMaxSize(),
                                            cornerRadius = 8.dp,
                                            showLabel = false
                                        )
                                        if (song.coverUrl.isNotBlank() && song.coverUrl.startsWith("http")) {
                                            AsyncImage(
                                                model = song.coverUrl,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            color = textPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
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

                        item {
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }

                // 4. Fixed Docked Top Navigation Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding(),
                    color = Color.Transparent
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Liquid glass back button
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

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = set.name,
                                    color = textPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Album ${pagerState.currentPage + 1} of ${albums.size} • ${activeAlbum.title}",
                                    color = textSecondary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        }

                    }
                }
                }

                // ═══ FLOATING ALBUM SET BOTTOM INDICATOR BAR (Mid-size, rock-solid frosted liquid glass) ═══
                val barShape = RoundedCornerShape(21.dp)
                val stableBarBg = if (isDark) Color(0xFF161820).copy(alpha = 0.82f) else Color(0xFFF2F3F7).copy(alpha = 0.88f)
                val barBorder = if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.08f)
                val prevInteraction = remember { MutableInteractionSource() }
                val nextInteraction = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                        .height(42.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = barShape,
                            spotColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.12f)
                        )
                        .clip(barShape)
                        .background(stableBarBg)
                        .border(1.dp, barBorder, barShape)
                        .glassRefraction(21.dp, isDark = isDark)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Previous Album button
                        val canGoPrev = pagerState.currentPage > 0
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .liquidPress(prevInteraction)
                                .clickable(
                                    interactionSource = prevInteraction,
                                    indication = null,
                                    enabled = canGoPrev
                                ) {
                                    Haptics.tick(view)
                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ChevronLeft,
                                contentDescription = "Previous Album",
                                tint = if (canGoPrev) textPrimary else textSecondary.copy(alpha = 0.25f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Sliding dots / stretch indicator (direct in outer capsule - no inner mini pill)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            albums.forEachIndexed { idx, _ ->
                                val isSelected = idx == pagerState.currentPage
                                val dotWidth by animateDpAsState(
                                    targetValue = if (isSelected) 20.dp else 6.dp,
                                    animationSpec = spring(dampingRatio = 0.65f, stiffness = 380f),
                                    label = "floatingSetDotWidth"
                                )
                                val dotAlpha by animateFloatAsState(
                                    targetValue = if (isSelected) 1f else 0.40f,
                                    animationSpec = tween(200),
                                    label = "floatingSetDotAlpha"
                                )
                                val dotColor = if (isSelected) AccentRed else textSecondary.copy(alpha = dotAlpha)

                                Box(
                                    modifier = Modifier
                                        .height(6.dp)
                                        .width(dotWidth)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(dotColor)
                                        .clickable {
                                            Haptics.tick(view)
                                            scope.launch {
                                                pagerState.animateScrollToPage(idx)
                                            }
                                        }
                                )
                            }
                        }

                        // Next Album button
                        val canGoNext = pagerState.currentPage < albums.size - 1
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .liquidPress(nextInteraction)
                                .clickable(
                                    interactionSource = nextInteraction,
                                    indication = null,
                                    enabled = canGoNext
                                ) {
                                    Haptics.tick(view)
                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Next Album",
                                tint = if (canGoNext) textPrimary else textSecondary.copy(alpha = 0.25f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
