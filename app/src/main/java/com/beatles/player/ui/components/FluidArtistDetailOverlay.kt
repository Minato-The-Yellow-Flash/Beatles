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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import com.beatles.player.ui.glassRefraction
import com.beatles.player.ui.motion.liquidPress
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
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
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
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import com.beatles.player.ui.components.liquidGlass
import com.beatles.player.ui.components.liquidGlassRim
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
    val listState = rememberLazyListState()

    fun handleClose() {
        if (isClosing) return
        isClosing = true
        scope.launch {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.84f,
                    stiffness = 380f
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
            try { listState.scrollToItem(0) } catch (_: Exception) {}
            currentArtist = artist
            currentOriginBounds = sourceBounds
            isClosing = false

            // 1. Immediate local tracks
            val localTracks = (state.favoriteSongs + state.recentlyPlayed + state.downloadedSongs +
                    state.albums.flatMap { it.songs } + state.recommendations)
                .filter { s ->
                    ArtistMixEngine.isArtistMatch(s, artist) ||
                    s.artist.contains(artist, ignoreCase = true) ||
                    artist.contains(s.artist, ignoreCase = true)
                }
                .distinctBy { it.id }
            artistTracks = localTracks

            // 2. Local albums
            val localAlbums = state.albums.filter { alb ->
                ArtistMixEngine.isArtistMatch(Song(alb.id, alb.title, alb.artist, alb.title, "", 0), artist) ||
                alb.artist.contains(artist, ignoreCase = true) ||
                artist.contains(alb.artist, ignoreCase = true)
            }
            realArtistAlbums = localAlbums

            isTracksLoading = true
            scope.launch {
                try {
                    val fetched = withContext(Dispatchers.IO) {
                        val s1 = async { MusicRepository.search("$artist best songs") }
                        val s2 = async { MusicRepository.search(artist) }
                        val raw = (s1.await() + s2.await()).distinctBy { it.id }
                        val matched = raw.filter { ArtistMixEngine.isArtistMatch(it, artist) }
                        if (matched.isNotEmpty()) matched
                        else raw.filter { s ->
                            s.artist.contains(artist, ignoreCase = true) ||
                            artist.contains(s.artist, ignoreCase = true) ||
                            s.title.contains(artist, ignoreCase = true)
                        }.ifEmpty { raw.take(25) }
                    }
                    val combined = (localTracks + fetched).distinctBy { it.id }
                    if (combined.isNotEmpty()) {
                        artistTracks = combined
                    }
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

            if (progress.value > 0.4f) {
                progress.snapTo(0.12f)
            }
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.84f, stiffness = 380f)
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
            val raw = CURATED_ONBOARD_ARTISTS.find { it.name.equals(curArtist, ignoreCase = true) }?.imageUrl
                ?: state.albums.find { it.artist.equals(curArtist, ignoreCase = true) }?.coverUrl
                ?: artistTracks.firstOrNull()?.coverUrl
                ?: "https://c.saavncdn.com/artists/Karan_Aujla_004_20230809115159_500x500.jpg"
            com.beatles.player.data.MusicRepository.getHighestQualityCover(raw)
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
        val originCardHeight = origin.height.coerceAtLeast(10f)

        val clampedP = p.coerceIn(0f, 1f)
        val smoothP = (clampedP * clampedP * (3f - 2f * clampedP)).coerceIn(0f, 1f)
        val sheetAlpha = smoothP
        val sheetSlideY = (1f - smoothP) * with(density) { 36.dp.toPx() } + dragY

        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (available.y > 0 && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                        dragY = (dragY + available.y * 0.75f).coerceAtLeast(0f)
                        if (dragY > 200f) {
                            handleClose()
                        }
                        return Offset(0f, available.y)
                    } else if (available.y < 0 && dragY > 0f) {
                        val consumed = minOf(-available.y, dragY)
                        dragY = (dragY - consumed).coerceAtLeast(0f)
                        return Offset(0f, -consumed)
                    }
                    return Offset.Zero
                }
                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (available.y > 0 && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                        dragY = (dragY + available.y * 0.75f).coerceAtLeast(0f)
                        if (dragY > 200f) {
                            handleClose()
                        }
                        return Offset(0f, available.y)
                    }
                    return Offset.Zero
                }
                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (dragY > 100f) {
                        handleClose()
                    }
                    dragY = 0f
                    return Velocity.Zero
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(120f)
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
                    .background(bgColor)
            ) {
                // 1. Deep Ambient Blurred Base (100% visible, vibrant ambient blur!)
                AsyncImage(
                    model = artistImage,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(80.dp)
                        .graphicsLayer { alpha = 0.55f }
                )

                // 2. Parallax Motion Banner at top
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        if (dragY > 100f) handleClose()
                                        dragY = 0f
                                    },
                                    onVerticalDrag = { _, delta ->
                                        if (delta > 0 || dragY > 0f) {
                                            dragY = (dragY + delta).coerceAtLeast(0f)
                                            if (dragY > 200f) handleClose()
                                        }
                                    }
                                )
                            }
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

                    // Similar Artists / Fans Also Like Section
                    val similarArtists = remember(artist) {
                        if (artist == null) emptyList()
                        else {
                            val mapped = com.beatles.player.ui.screens.INSTANT_RELATED_ARTISTS[artist] ?: emptyList()
                            if (mapped.isNotEmpty()) mapped
                            else {
                                val currArt = com.beatles.player.ui.screens.CURATED_ONBOARD_ARTISTS.find { it.name.equals(artist, ignoreCase = true) }
                                val sameGenre = com.beatles.player.ui.screens.CURATED_ONBOARD_ARTISTS.filter {
                                    !it.name.equals(artist, ignoreCase = true) && (currArt == null || it.genre == currArt.genre || it.language == currArt.language)
                                }
                                sameGenre.take(6)
                            }
                        }
                    }

                    // 3. Scrollable Content (Header, Actions, Tracklist, Albums/EPs)
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(nestedScrollConnection),
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
                                            .liquidGlass(CircleShape)
                                            .liquidGlassRim(CircleShape)
                                            .clickable(
                                                interactionSource = shuffleInteraction,
                                                indication = null
                                            ) {
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

                                    // Center Play Button (64dp liquid glass circle)
                                    val playInteraction = remember { MutableInteractionSource() }
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .liquidPress(playInteraction)
                                            .shadow(4.dp, CircleShape, spotColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.18f))
                                            .clip(CircleShape)
                                            .liquidGlass(CircleShape, surfaceTint = playBtnBg)
                                            .liquidGlassRim(CircleShape)
                                            .clickable(
                                                interactionSource = playInteraction,
                                                indication = null
                                            ) {
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

                                    // Artist Mix Button (50dp liquid glass circle)
                                    val mixInteraction = remember { MutableInteractionSource() }
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .liquidPress(mixInteraction)
                                            .shadow(3.dp, CircleShape, spotColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.15f))
                                            .clip(CircleShape)
                                            .liquidGlass(CircleShape)
                                            .liquidGlassRim(CircleShape)
                                            .clickable(
                                                interactionSource = mixInteraction,
                                                indication = null
                                            ) {
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

                        // Tracklist (Translucent frosted glass design matching AlbumDetailSheet)
                        if (artistTracks.isNotEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                                        .border(
                                            0.5.dp,
                                            if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                                            RoundedCornerShape(20.dp)
                                        )
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
                        } else {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp, horizontal = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "No local tracks found for $curArtist",
                                        color = textSecondary,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .liquidGlass(RoundedCornerShape(50))
                                            .liquidGlassRim(RoundedCornerShape(50))
                                            .clickable {
                                                Haptics.confirm(view)
                                                onPlayArtistMix(curArtist)
                                                scope.launch {
                                                    progress.animateTo(0f, tween(200))
                                                    onClose()
                                                }
                                            }
                                            .padding(horizontal = 20.dp, vertical = 10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentRed, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Play $curArtist Mix", color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
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

                        // Similar Artists / Fans Also Like Section
                        if (similarArtists.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(28.dp))
                                Text(
                                    "Fans Also Like",
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
                                                    viewModel.openArtist(simArt.name, simBounds)
                                                },
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(92.dp)
                                                    .onGloballyPositioned { coordinates ->
                                                        simBounds = coordinates.boundsInRoot()
                                                    }
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
                                                color = textMuted,
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

                    // 4. Back Button (Top Left)
                    val backInteraction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(start = 12.dp, top = 8.dp)
                            .size(40.dp)
                            .liquidPress(backInteraction)
                            .shadow(3.dp, CircleShape, spotColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.15f))
                            .clip(CircleShape)
                            .liquidGlass(CircleShape)
                            .liquidGlassRim(CircleShape)
                            .clickable(interactionSource = backInteraction, indication = null) {
                                Haptics.click(view)
                                handleClose()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDark) Color.White else Color(0xFF141416),
                            modifier = Modifier.size(20.dp)
                        )
                    }
            } // Closes Full-screen Sheet Box

            // 5. Shared Morph Hero Artist Avatar: glides seamlessly from tapped origin card to centered hero position
            val curArtLeft = origin.left + (targetLeftPx - origin.left) * smoothP
            val curArtTop = origin.top + (targetTopPx + artTranslateY - origin.top) * smoothP + dragY
            val curArtWidth = originCardWidth + (targetArtworkSizePx - originCardWidth) * smoothP
            val curArtHeight = originCardHeight + (targetArtworkSizePx - originCardHeight) * smoothP
            val curArtCorner = originCornerPx + (with(density) { 24.dp.toPx() } - originCornerPx) * smoothP

            Box(
                modifier = Modifier
                    .offset { IntOffset(curArtLeft.roundToInt(), curArtTop.roundToInt()) }
                    .size(
                        width = with(density) { curArtWidth.toDp() },
                        height = with(density) { curArtHeight.toDp() }
                    )
                    .graphicsLayer {
                        scaleX = if (clampedP >= 0.95f) artScale else 1f
                        scaleY = if (clampedP >= 0.95f) artScale else 1f
                        alpha = if (clampedP >= 0.95f) artAlpha.coerceIn(0.08f, 1f) else 1f
                    }
                    .then(if (artBlur > 0.dp && clampedP >= 0.95f) Modifier.blur(artBlur) else Modifier)
                    .clip(RoundedCornerShape(with(density) { curArtCorner.toDp() }))
                    .shadow(elevation = (12.dp * clampedP).coerceAtLeast(0.dp), shape = RoundedCornerShape(with(density) { curArtCorner.toDp() }))
            ) {
                AsyncImage(
                    model = artistImage,
                    contentDescription = curArtist,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
