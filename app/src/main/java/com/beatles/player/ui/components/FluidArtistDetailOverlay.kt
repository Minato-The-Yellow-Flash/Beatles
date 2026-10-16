package com.beatles.player.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
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
import com.beatles.player.data.ArtistMixEngine
import com.beatles.player.data.MusicRepository
import com.beatles.player.data.Song
import com.beatles.player.ui.screens.CURATED_ONBOARD_ARTISTS
import com.beatles.player.ui.theme.LocalDynamicBottomPadding
import com.beatles.player.ui.theme.isDarkTheme
import com.beatles.player.viewmodel.PlayerUiState
import com.beatles.player.viewmodel.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val AccentRed = Color(0xFFF82C38)

@Composable
fun FluidArtistDetailOverlay(
    artist: String?,
    sourceBounds: Rect?,
    state: PlayerUiState,
    viewModel: PlayerViewModel,
    onClose: () -> Unit,
    onPlay: (Song, List<Song>) -> Unit,
    onPlayArtistMix: (String) -> Unit
) {
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val view = LocalView.current
    val isDark = isDarkTheme(state.themeMode)
    val bgColor = if (isDark) Color(0xFF141416) else Color(0xFFF6F7F9)
    val textPrimary = if (isDark) Color.White else Color(0xFF141416)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF6B7280)
    val textMuted = if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF9CA3AF)
    val cardBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f)

    var currentArtist by remember { mutableStateOf<String?>(null) }
    var currentOriginBounds by remember { mutableStateOf<Rect?>(null) }
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    var artistTracks by remember { mutableStateOf<List<Song>>(emptyList()) }
    var realArtistAlbums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var isTracksLoading by remember { mutableStateOf(false) }
    var isClosing by remember { mutableStateOf(false) }

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
            currentArtist = null
            currentOriginBounds = null
            artistTracks = emptyList()
            realArtistAlbums = emptyList()
            isClosing = false
            onClose()
        }
    }

    LaunchedEffect(artist) {
        if (artist != null) {
            currentArtist = artist
            if (sourceBounds != null) {
                currentOriginBounds = sourceBounds
            }
            isClosing = false

            // 1. Immediate local tracks
            val localTracks = (state.favoriteSongs + state.recentlyPlayed + state.downloadedSongs +
                    state.albums.flatMap { it.songs } + state.recommendations)
                .filter { ArtistMixEngine.isArtistMatch(it, artist) }
                .distinctBy { it.id }
            artistTracks = localTracks

            // 2. Local albums
            val localAlbums = state.albums.filter { ArtistMixEngine.isArtistMatch(Song(it.id, it.title, it.artist, it.title, "", 0), artist) }
            realArtistAlbums = localAlbums

            isTracksLoading = true
            scope.launch {
                try {
                    val fetched = withContext(Dispatchers.IO) {
                        MusicRepository.search("$artist best songs")
                            .filter { ArtistMixEngine.isArtistMatch(it, artist) }
                    }
                    artistTracks = (localTracks + fetched).distinctBy { it.id }
                } catch (_: Exception) {}
                isTracksLoading = false
            }

            // Concurrently fetch all real albums & EPs from iTunes
            scope.launch {
                try {
                    val itunesAlbums = withContext(Dispatchers.IO) {
                        MusicRepository.fetchRealAlbumsForArtist(artist, limit = 35, loadTracksForFirst = false)
                    }
                    if (itunesAlbums.isNotEmpty()) {
                        realArtistAlbums = (localAlbums + itunesAlbums).distinctBy { it.id }
                    }
                } catch (_: Exception) {}
            }

            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.84f, stiffness = 320f)
            )
        } else if (!isClosing && currentArtist != null) {
            handleClose()
        }
    }

    val p = progress.value
    val curArtist = currentArtist

    BackHandler(enabled = state.selectedAlbum == null && (p > 0.01f || artist != null)) {
        handleClose()
    }

    if (curArtist != null && p > 0.001f) {
        val artistImage = remember(curArtist, artistTracks) {
            CURATED_ONBOARD_ARTISTS.find { it.name.equals(curArtist, ignoreCase = true) }?.imageUrl
                ?: state.albums.find { it.artist.equals(curArtist, ignoreCase = true) }?.coverUrl
                ?: artistTracks.firstOrNull()?.coverUrl
                ?: "https://c.saavncdn.com/artists/Karan_Aujla_004_20230809115159_500x500.jpg"
        }

        val targetArtworkSize = 200.dp
        val targetArtworkSizePx = with(density) { targetArtworkSize.toPx() }
        val screenWpx = with(density) { config.screenWidthDp.dp.toPx() }.coerceAtLeast(100f)
        val screenHpx = with(density) { config.screenHeightDp.dp.toPx() }.coerceAtLeast(100f)
        val targetTopPx = with(density) { 56.dp.toPx() }
        val targetLeftPx = (screenWpx - targetArtworkSizePx) / 2f

        var dragY by remember { mutableFloatStateOf(0f) }

        // Parallax Video Motion Banner Animation (Identical to AlbumDetailSheet)
        val infiniteTransition = rememberInfiniteTransition(label = "artistBannerMotion")
        val bannerScale by infiniteTransition.animateFloat(
            initialValue = 1.15f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(9000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "artistBannerScale"
        )
        val bannerShiftY by infiniteTransition.animateFloat(
            initialValue = -10f,
            targetValue = 10f,
            animationSpec = infiniteRepeatable(
                animation = tween(7000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "artistBannerShiftY"
        )

        val listState = rememberLazyListState()

        // Scroll-driven swipe up parallax and progressive blur for hero artwork
        val scrollOffset = if (listState.firstVisibleItemIndex == 0) {
            listState.firstVisibleItemScrollOffset.toFloat()
        } else {
            listState.firstVisibleItemScrollOffset.toFloat() + 800f
        }
        val artTranslateY = (-scrollOffset * 0.75f).coerceAtLeast(-targetTopPx - 200f) * p
        val artBlur = ((scrollOffset / 14f) * p).coerceIn(0f, 24f).dp
        val artScale = (1f - (scrollOffset / 1400f) * p).coerceIn(0.72f, 1f)
        val artAlpha = (1f - (scrollOffset / 340f) * p).coerceIn(0f, 1f)

        val origin = currentOriginBounds ?: Rect(
            screenWpx / 2f - targetArtworkSizePx / 2f,
            screenHpx / 2f - targetArtworkSizePx / 2f,
            screenWpx / 2f + targetArtworkSizePx / 2f,
            screenHpx / 2f + targetArtworkSizePx / 2f
        )
        val originCornerPx = if (origin.width <= with(density) { 72.dp.toPx() }) origin.width / 2f else with(density) { 24.dp.toPx() }

        val originCardWidth = origin.width.coerceAtLeast(10f)
        val originCardHeight = if (origin.height > origin.width * 1.15f) origin.width else origin.height.coerceAtLeast(10f)

        val clampedP = p.coerceIn(0f, 1f)
        val curCorner = with(density) { (originCornerPx * (1f - clampedP)).coerceIn(0f, originCornerPx).toDp() }
        val curLeft = origin.left * (1f - clampedP)
        val curTop = (origin.top * (1f - clampedP) + dragY).coerceAtLeast(0f)
        val curWidth = (originCardWidth + (screenWpx - originCardWidth) * clampedP).coerceAtLeast(10f)
        val curHeight = (originCardHeight + (screenHpx - originCardHeight) * clampedP).coerceAtLeast(10f)

        Box(modifier = Modifier.fillMaxSize()) {
            // Background scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = (0.40f * clampedP).coerceIn(0f, 1f)))
            )

            // Morphing Card Container (interpolated physical bounds with zero aspect ratio distortion)
            Box(
                modifier = Modifier
                    .offset { IntOffset(curLeft.roundToInt(), curTop.roundToInt()) }
                    .size(with(density) { curWidth.toDp() }, with(density) { curHeight.toDp() })
                    .clip(RoundedCornerShape(curCorner))
                    .background(bgColor)
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
                // Shared Element Artist Image cropped smoothly to container
                AsyncImage(
                    model = artistImage,
                    contentDescription = curArtist,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (clampedP > 0.60f) {
                    val contentAlpha = ((clampedP - 0.60f) / 0.40f).coerceIn(0f, 1f)
                    val contentSlide = (1f - clampedP) * with(density) { 24.dp.toPx() }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = contentAlpha
                                translationY = contentSlide
                            }
                    ) {
                    // 1. Deep Ambient Blurred Base
                    AsyncImage(
                        model = artistImage,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(80.dp)
                            .graphicsLayer { alpha = 0.50f }
                    )

                    // 2. Parallax Motion Banner at top
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
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
                        AsyncImage(
                            model = artistImage,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(bannerScale)
                                .offset { IntOffset(0, bannerShiftY.roundToInt()) }
                                .blur(20.dp)
                        )
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

                    // 3. Scrollable Content (Header, Actions, Tracklist, Albums/EPs)
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = targetArtworkSize + 110.dp, bottom = LocalDynamicBottomPadding.current + 32.dp)
                    ) {
                        // Title & Metadata
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = curArtist,
                                        color = textPrimary,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Verified",
                                        tint = AccentRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Verified Artist • Essential Catalog • ${artistTracks.size} Songs",
                                    color = textSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                // Controls Row: 50dp Shuffle, 64dp Play, 50dp Artist Mix (Matching AlbumDetailSheet)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val buttonBg = if (isDark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.08f)
                                    val buttonIconTint = textPrimary
                                    val playBtnBg = textPrimary
                                    val playBtnIconTint = if (isDark) Color.Black else Color.White

                                    // Shuffle Button (50dp borderless circle)
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(buttonBg)
                                            .clickable {
                                                if (artistTracks.isNotEmpty()) {
                                                    val shuffled = artistTracks.shuffled()
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

                                    // Center Play Button (64dp borderless circle)
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(playBtnBg)
                                            .clickable {
                                                if (artistTracks.isNotEmpty()) {
                                                    onPlay(artistTracks.first(), artistTracks)
                                                }
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

                                    // Artist Mix Button (50dp borderless circle)
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(buttonBg)
                                            .clickable {
                                                Haptics.confirm(view)
                                                onPlayArtistMix(curArtist)
                                                scope.launch {
                                                    progress.animateTo(0f, tween(200))
                                                    onClose()
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Radio,
                                            contentDescription = "Artist Mix",
                                            tint = AccentRed,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }

                        // Top Tracks Heading
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Top Tracks",
                                    color = textPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (artistTracks.isNotEmpty()) {
                                    Text(
                                        "${artistTracks.size} tracks",
                                        color = textMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // Tracklist (Matching AlbumDetailSheet rounded card design)
                        if (artistTracks.isNotEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .background(cardBg, RoundedCornerShape(20.dp))
                                ) {
                                    artistTracks.take(30).forEachIndexed { index, song ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    Haptics.click(view)
                                                    onPlay(song, artistTracks)
                                                }
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                color = textMuted,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.width(26.dp)
                                            )
                                            AsyncImage(
                                                model = song.coverUrl,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = song.title,
                                                    color = textPrimary,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = song.album.ifBlank { song.artist },
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
                                                color = textMuted,
                                                fontSize = 12.sp
                                            )
                                        }
                                        if (index < minOf(29, artistTracks.size - 1)) {
                                            HorizontalDivider(
                                                color = dividerColor,
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (isTracksLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = AccentRed, strokeWidth = 2.5.dp, modifier = Modifier.size(32.dp))
                                }
                            }
                        }

                        // Albums & EPs Section
                        if (realArtistAlbums.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(28.dp))
                                Text(
                                    "Albums & EPs",
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
                                    items(realArtistAlbums) { album ->
                                        var cardBounds by remember { mutableStateOf<Rect?>(null) }
                                        Column(
                                            modifier = Modifier
                                                .width(140.dp)
                                                .clickable {
                                                    Haptics.click(view)
                                                    viewModel.setSelectedAlbum(album, cardBounds)
                                                }
                                        ) {
                                            AsyncImage(
                                                model = album.coverUrl,
                                                contentDescription = album.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(140.dp)
                                                    .onGloballyPositioned { coordinates ->
                                                        cardBounds = coordinates.boundsInRoot()
                                                    }
                                                    .clip(RoundedCornerShape(14.dp))
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                album.title,
                                                color = textPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                            Text(
                                                "${album.releaseYear} · ${album.songs.size.coerceAtLeast(1)} tracks",
                                                color = textMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Parallax Hero Artwork (200dp with swipe up blur & scale)
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(targetLeftPx.roundToInt(), (targetTopPx + artTranslateY).roundToInt()) }
                            .size(targetArtworkSize)
                            .graphicsLayer {
                                scaleX = artScale
                                scaleY = artScale
                                alpha = artAlpha
                            }
                            .then(if (artBlur > 0.dp) Modifier.blur(artBlur) else Modifier)
                            .clip(RoundedCornerShape(22.dp))
                    ) {
                        AsyncImage(
                            model = artistImage,
                            contentDescription = curArtist,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // 5. Back Button (Top Left)
                    IconButton(
                        onClick = {
                            Haptics.click(view)
                            handleClose()
                        },
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(start = 12.dp, top = 8.dp)
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
}
