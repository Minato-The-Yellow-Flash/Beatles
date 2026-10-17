package com.beatles.player.ui.screens

import com.beatles.player.R
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.ui.zIndex
import com.beatles.player.ui.components.FrostedButton
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import com.beatles.player.ui.glassRefraction
import com.beatles.player.ui.motion.liquidPress
import com.beatles.player.ui.components.edgeFade
import com.beatles.player.ui.components.AppleMusicGradientCover
import androidx.compose.animation.core.spring
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.ui.platform.LocalView
import com.beatles.player.ui.components.Haptics
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import androidx.compose.foundation.isSystemInDarkTheme

private val AccentRed = Color(0xFFF82C38)

@Composable
fun HomeScreen(
    state: PlayerUiState,
    onPlay: (Song, List<Song>, Rect?) -> Unit,
    onPlayArtistMix: (String, List<Song>) -> Unit = { _, _ -> },
    onSelectArtist: (String, Rect?) -> Unit = { _, _ -> },
    onSeeAll: (String) -> Unit,
    onSelectAlbum: (Album, Rect?) -> Unit = { _, _ -> },
    onPlayAlbumSet: (AlbumSet) -> Unit = {},
    onSelectAlbumSet: (AlbumSet, Rect?) -> Unit = { _, _ -> },
    onLoadMoreTopSongs: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    val isDark = isDarkTheme(state.themeMode)
    val textPrimary = if (isDark) Color.White else Color(0xFF141416)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF6B7280)
    val textMuted = if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF9CA3AF)

    val scope = rememberCoroutineScope()
    val recentSongs = remember(state.recentlyPlayed) { 
        state.recentlyPlayed.filter { it.id.isNotBlank() }.distinctBy { it.id }.take(8) 
    }
    val allRecs = remember(state.recommendations) {
        state.recommendations.filter { it.id.isNotBlank() }.distinctBy { it.id }
    }
    val madeForYouSongs = remember(allRecs) { 
        allRecs.drop(3).take(6) 
    }
    val topSongsList = remember(allRecs) { 
        allRecs.filter { !it.isAlbum }.take(6) 
    }
    val homeAlbums = remember(state.albums) {
        state.albums.filter { it.id.isNotBlank() }.distinctBy { it.id }
    }
    val homeAlbumSets = remember(state.albumSets) {
        state.albumSets.filter { it.id.isNotBlank() }.distinctBy { it.id }
    }

    // Ensure at least 5 featured albums are always available
    val allFeaturedAlbums = remember(homeAlbums) {
        val list = homeAlbums.toMutableList()
        if (list.size < 5) {
            val defaults = com.beatles.player.data.MusicRepository.getFeaturedAlbums()
            for (d in defaults) {
                if (list.none { it.id == d.id }) list.add(d)
            }
        }
        list
    }

    // Selected Genre Filter State
    var selectedGenre by remember { mutableStateOf("All") }
    val genres = remember {
        listOf("All", "Hip Hop", "Punjabi", "Bollywood", "Lo-Fi", "Pop", "Classical", "Indie", "R&B")
    }

    // Filtered songs matching selected genre
    val filteredGenreSongs = remember(selectedGenre, allRecs, state.albums) {
        if (selectedGenre == "All") {
            allRecs.take(8)
        } else {
            val g = selectedGenre.lowercase()
            val fromRecs = allRecs.filter { song ->
                val detected = com.beatles.player.data.MusicRepository.detectGenre(song).lowercase()
                val text = "${song.title} ${song.artist} ${song.genre} ${song.album}".lowercase()
                when (selectedGenre) {
                    "Hip Hop" -> detected.contains("hip-hop") || text.contains("hip hop") || text.contains("rap") || text.contains("drill")
                    "Punjabi" -> detected.contains("bhangra") || text.contains("punjabi") || com.beatles.player.data.MusicRepository.detectLanguage(song) == "Punjabi"
                    "Bollywood" -> detected.contains("bollywood") || text.contains("hindi") || com.beatles.player.data.MusicRepository.detectLanguage(song) == "Hindi"
                    "Lo-Fi" -> text.contains("lofi") || text.contains("lo-fi") || text.contains("chill") || detected.contains("acoustic")
                    "Pop" -> detected.contains("pop") || text.contains("pop")
                    "Classical" -> text.contains("classical") || text.contains("ghazal") || detected.contains("devotional")
                    "Indie" -> text.contains("indie") || text.contains("folk")
                    "R&B" -> detected.contains("r&b") || text.contains("r&b") || text.contains("soul")
                    else -> text.contains(g)
                }
            }
            val fromAlbums = state.albums.flatMap { it.songs }.filter { song ->
                val detected = com.beatles.player.data.MusicRepository.detectGenre(song).lowercase()
                val text = "${song.title} ${song.artist} ${song.genre} ${song.album}".lowercase()
                when (selectedGenre) {
                    "Hip Hop" -> detected.contains("hip-hop") || text.contains("hip hop") || text.contains("rap") || text.contains("drill")
                    "Punjabi" -> detected.contains("bhangra") || text.contains("punjabi") || com.beatles.player.data.MusicRepository.detectLanguage(song) == "Punjabi"
                    "Bollywood" -> detected.contains("bollywood") || text.contains("hindi") || com.beatles.player.data.MusicRepository.detectLanguage(song) == "Hindi"
                    "Lo-Fi" -> text.contains("lofi") || text.contains("lo-fi") || text.contains("chill") || detected.contains("acoustic")
                    "Pop" -> detected.contains("pop") || text.contains("pop")
                    "Classical" -> text.contains("classical") || text.contains("ghazal") || detected.contains("devotional")
                    "Indie" -> text.contains("indie") || text.contains("folk")
                    "R&B" -> detected.contains("r&b") || text.contains("r&b") || text.contains("soul")
                    else -> text.contains(g)
                }
            }
            val merged = (fromRecs + fromAlbums).distinctBy { it.id }
            if (merged.isNotEmpty()) merged.take(8) else allRecs.take(6)
        }
    }

    // Adaptive Artist Spotlight derived from user recommendations / listening style
    val spotlightArtist = remember(state.selectedArtists, state.recentlyPlayed, state.favoriteSongs, allRecs) {
        state.selectedArtists.firstOrNull { it.isNotBlank() }
            ?: state.recentlyPlayed.firstOrNull { it.artist.isNotBlank() }?.artist
            ?: state.favoriteSongs.firstOrNull { it.artist.isNotBlank() }?.artist
            ?: allRecs.firstOrNull { it.artist.isNotBlank() }?.artist
            ?: "Karan Aujla"
    }

    val spotlightArtistImage = remember(spotlightArtist) {
        CURATED_ONBOARD_ARTISTS.find { it.name.equals(spotlightArtist, ignoreCase = true) }?.imageUrl
            ?: state.albums.find { it.artist.equals(spotlightArtist, ignoreCase = true) }?.coverUrl
            ?: allRecs.firstOrNull { it.artist.equals(spotlightArtist, ignoreCase = true) }?.coverUrl
            ?: ""
    }

    val spotlightSongs = remember(spotlightArtist, allRecs, state.albums, state.recentlyPlayed, state.favoriteSongs) {
        val matches = (allRecs + state.albums.flatMap { it.songs } + state.recentlyPlayed + state.favoriteSongs)
            .filter { it.artist.contains(spotlightArtist, ignoreCase = true) }
            .distinctBy { it.id }
        if (matches.isNotEmpty()) matches.take(6)
        else allRecs.take(4)
    }

    val view = LocalView.current
    var pullOffsetRaw by remember { mutableFloatStateOf(0f) }
    val pullAnim = remember { androidx.compose.animation.core.Animatable(0f) }
    val currentPullOffset = if (pullOffsetRaw > 0f) pullOffsetRaw else pullAnim.value
    var hasTriggeredThresholdHaptic by remember { mutableStateOf(false) }

    var showRefreshCompletionPopup by remember { mutableStateOf(false) }
    var previousRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(state.isRefreshingHome) {
        if (previousRefreshing && !state.isRefreshingHome) {
            showRefreshCompletionPopup = true
            delay(3500)
            showRefreshCompletionPopup = false
        }
        previousRefreshing = state.isRefreshingHome
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0 && pullOffsetRaw > 0f) {
                    val consumed = available.y
                    pullOffsetRaw = (pullOffsetRaw + consumed).coerceAtLeast(0f)
                    if (pullOffsetRaw < 110f) {
                        hasTriggeredThresholdHaptic = false
                    }
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0 && source == NestedScrollSource.UserInput) {
                    pullOffsetRaw = (pullOffsetRaw + available.y * 0.45f).coerceAtMost(220f)
                    if (pullOffsetRaw >= 110f && !hasTriggeredThresholdHaptic) {
                        hasTriggeredThresholdHaptic = true
                        Haptics.threshold(view)
                    } else if (pullOffsetRaw < 110f) {
                        hasTriggeredThresholdHaptic = false
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullOffsetRaw > 110f) {
                    Haptics.confirm(view)
                    try { onRefresh() } catch (_: Exception) {}
                }
                hasTriggeredThresholdHaptic = false
                try {
                    pullAnim.snapTo(pullOffsetRaw)
                    pullOffsetRaw = 0f
                    pullAnim.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 400f))
                } catch (_: Exception) {
                    pullOffsetRaw = 0f
                }
                return Velocity.Zero
            }
        }
    }

    if (state.showSeeAllSection != null) {
        SeeAllScreen(
            section = state.showSeeAllSection!!,
            state = state,
            onPlay = onPlay,
            onBack = { onSeeAll("") },
            onLoadMore = onLoadMoreTopSongs
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        // Fluid Liquid Pull-to-refresh badge
        if (currentPullOffset > 12f || state.isRefreshingHome) {
            val refreshAlpha = if (state.isRefreshingHome) 1f else (currentPullOffset / 110f).coerceIn(0f, 1f)
            val isThresholdReached = currentPullOffset >= 110f || state.isRefreshingHome

            val eqTransition = rememberInfiniteTransition(label = "pull_eq")
            val bar1Height by eqTransition.animateFloat(
                initialValue = 6f,
                targetValue = 18f,
                animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
                label = "bar1"
            )
            val bar2Height by eqTransition.animateFloat(
                initialValue = 17f,
                targetValue = 6f,
                animationSpec = infiniteRepeatable(tween(380, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
                label = "bar2"
            )
            val bar3Height by eqTransition.animateFloat(
                initialValue = 10f,
                targetValue = 20f,
                animationSpec = infiniteRepeatable(tween(460, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
                label = "bar3"
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(15f)
                    .padding(top = 96.dp)
                    .offset { IntOffset(0, (currentPullOffset * 0.42f).roundToInt()) }
                    .shadow(4.dp, RoundedCornerShape(50), spotColor = if (isDark) Color.Black.copy(alpha = 0.40f) else Color.Black.copy(alpha = 0.15f))
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isDark) Color(0xFF1E1E24).copy(alpha = 0.88f * refreshAlpha)
                        else Color.White.copy(alpha = 0.90f * refreshAlpha)
                    )
                    .glassRefraction(50.dp, isDark = isDark)
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    if (state.isRefreshingHome) {
                        // Dancing soundwave equalizer bars
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(20.dp)
                        ) {
                            Box(modifier = Modifier.width(3.dp).height((bar1Height.toInt()).dp).background(AccentRed, RoundedCornerShape(2.dp)))
                            Box(modifier = Modifier.width(3.dp).height((bar2Height.toInt()).dp).background(Color(0xFFFF5252), RoundedCornerShape(2.dp)))
                            Box(modifier = Modifier.width(3.dp).height((bar3Height.toInt()).dp).background(AccentRed, RoundedCornerShape(2.dp)))
                        }
                    } else {
                        // Rotational wave icon tracking pull progress
                        val rot = (currentPullOffset * 2.4f) % 360f
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            tint = if (isThresholdReached) AccentRed else textPrimary.copy(alpha = 0.65f),
                            modifier = Modifier
                                .size(16.dp)
                                .graphicsLayer { rotationZ = rot }
                        )
                    }

                    Text(
                        text = when {
                            state.isRefreshingHome -> "Syncing fresh albums & mixes..."
                            isThresholdReached -> "Release to tune freshest mixes"
                            else -> "Pull to tune fresh waves"
                        },
                        color = if (isThresholdReached) textPrimary else textPrimary.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        fontWeight = if (isThresholdReached) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }

        // Animated Completion Popup of Updated Items
        AnimatedVisibility(
            visible = showRefreshCompletionPopup,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 380f)
            ) + fadeIn(tween(200)) + scaleIn(initialScale = 0.88f),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            ) + fadeOut(tween(180)) + scaleOut(targetScale = 0.90f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(25f)
                .padding(top = 92.dp)
        ) {
            Box(
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(50), spotColor = if (isDark) Color.Black.copy(alpha = 0.40f) else Color.Black.copy(alpha = 0.15f))
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isDark) Color(0xFF1E1E24).copy(alpha = 0.95f)
                        else Color.White.copy(alpha = 0.96f)
                    )
                    .glassRefraction(50.dp, isDark = isDark)
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(AccentRed.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AccentRed,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Refreshed",
                            color = textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Fresh mixes, albums & trending tracks updated",
                            color = textPrimary.copy(alpha = 0.65f),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }

        val bgFade = if (isDark) Color(0xFF141416) else Color(0xFFF6F7F9)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, currentPullOffset.roundToInt()) }
                .edgeFade(bgFade),
            contentPadding = PaddingValues(top = 110.dp, bottom = LocalDynamicBottomPadding.current)
        ) {
            // Header title
            item(key = "feed_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Listen Now",
                        color = textPrimary,
                        fontSize = 31.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // =========================================================================
            // 1. FEATURED ALBUMS (Top Section - Hero + At least 5 Albums/Playlists)
            // =========================================================================
            val heroAlbum = state.heroAlbum ?: allFeaturedAlbums.firstOrNull()
            if (heroAlbum != null) {
                item(key = "hero_section") {
                    val heroGenre = (heroAlbum.genre ?: "Pop").uppercase()
                    val heroYear = heroAlbum.releaseYear ?: "2026"
                    val heroSongs = heroAlbum.songs ?: emptyList()
                    val heroCover = remember(heroAlbum) {
                        if (heroAlbum.coverUrl.isNotBlank() && heroAlbum.coverUrl.startsWith("http")) heroAlbum.coverUrl
                        else heroAlbum.songs.firstOrNull { it.coverUrl.isNotBlank() && it.coverUrl.startsWith("http") }?.coverUrl
                            ?: CURATED_ONBOARD_ARTISTS.find { it.name.equals(heroAlbum.artist, ignoreCase = true) }?.imageUrl
                            ?: ""
                    }
                    var heroBounds by remember { mutableStateOf<Rect?>(null) }
                    val bannerHazeState = remember { HazeState() }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(280.dp)
                            .onGloballyPositioned { coords -> heroBounds = coords.boundsInRoot() }
                            .clip(RoundedCornerShape(22.dp))
                    ) {
                        // Background layers with Haze blur provider
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .haze(bannerHazeState)
                        ) {
                            AppleMusicGradientCover(
                                title = heroAlbum.title ?: "Featured Album",
                                artist = heroAlbum.artist ?: "",
                                modifier = Modifier.fillMaxSize(),
                                cornerRadius = 22.dp
                            )
                            if (heroCover.isNotBlank() && heroCover.startsWith("http")) {
                                AsyncImage(
                                    model = heroCover,
                                    contentDescription = heroAlbum.title ?: "",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.20f),
                                                Color.Black.copy(alpha = if (isDark) 0.88f else 0.65f)
                                            )
                                        )
                                    )
                            )
                        }
                        // Frosted badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(14.dp)
                                .background(Color(0xFFE2E2E2).copy(alpha = 0.18f), RoundedCornerShape(50))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("FEATURED ALBUM", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 16.dp, bottom = 16.dp, end = 16.dp)
                        ) {
                            Text(
                                "NEW RELEASE • $heroYear • $heroGenre",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 11.sp,
                                letterSpacing = 1.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(heroAlbum.title ?: "", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("${heroAlbum.artist ?: ""} • ${heroSongs.size} tracks", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                val playInteraction = remember { MutableInteractionSource() }
                                Box(
                                    modifier = Modifier
                                        .liquidPress(playInteraction)
                                        .shadow(3.dp, RoundedCornerShape(50), spotColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.15f))
                                        .clip(RoundedCornerShape(50))
                                        .hazeChild(
                                            state = bannerHazeState,
                                            shape = RoundedCornerShape(50),
                                            style = HazeStyle(blurRadius = 4.dp, noiseFactor = 0.06f)
                                        )
                                        .background(if (isDark) Color(0xFF383842).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.88f))
                                        .glassRefraction(50.dp, isDark)
                                        .clickable(
                                            interactionSource = playInteraction,
                                            indication = null
                                        ) {
                                            heroSongs.firstOrNull()?.let { onPlay(it, heroSongs, heroBounds) }
                                        }
                                        .padding(horizontal = 18.dp, vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = if (isDark) Color.White else Color(0xFF141416),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Play Album",
                                            color = if (isDark) Color.White else Color(0xFF141416),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                val viewInteraction = remember { MutableInteractionSource() }
                                Box(
                                    modifier = Modifier
                                        .liquidPress(viewInteraction)
                                        .shadow(3.dp, RoundedCornerShape(50), spotColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.15f))
                                        .clip(RoundedCornerShape(50))
                                        .hazeChild(
                                            state = bannerHazeState,
                                            shape = RoundedCornerShape(50),
                                            style = HazeStyle(blurRadius = 4.dp, noiseFactor = 0.06f)
                                        )
                                        .background(if (isDark) Color(0xFF383842).copy(alpha = 0.28f) else Color.White.copy(alpha = 0.72f))
                                        .glassRefraction(50.dp, isDark)
                                        .clickable(
                                            interactionSource = viewInteraction,
                                            indication = null
                                        ) {
                                            onSelectAlbum(heroAlbum, heroBounds)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = if (isDark) Color.White else Color(0xFF141416),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "View Album",
                                            color = if (isDark) Color.White else Color(0xFF141416),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                }
            }

            // Featured Albums Horizontal Carousel (features at least 5 albums/playlists)
            if (allFeaturedAlbums.isNotEmpty()) {
                item(key = "featured_albums_row") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Featured Albums", color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("${allFeaturedAlbums.size} featured", color = textMuted, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(allFeaturedAlbums, key = { index, album -> "feat_alb_${album.id}_$index" }) { _, album ->
                            val albumSongs = album.songs ?: emptyList()
                            val resolvedAlbumCover = remember(album) {
                                if (album.coverUrl.isNotBlank() && album.coverUrl.startsWith("http")) album.coverUrl
                                else album.songs.firstOrNull { it.coverUrl.isNotBlank() && it.coverUrl.startsWith("http") }?.coverUrl
                                    ?: CURATED_ONBOARD_ARTISTS.find { it.name.equals(album.artist, ignoreCase = true) }?.imageUrl
                                    ?: ""
                            }
                            var cardBounds by remember { mutableStateOf<Rect?>(null) }
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            val cardScale by animateFloatAsState(
                                targetValue = if (isPressed) 0.95f else 1.0f,
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                                label = "albumCardScale"
                            )
                            Column(
                                modifier = Modifier
                                    .width(156.dp)
                                    .graphicsLayer {
                                        scaleX = cardScale
                                        scaleY = cardScale
                                    }
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null
                                    ) { onSelectAlbum(album.copy(coverUrl = resolvedAlbumCover.ifBlank { album.coverUrl }), cardBounds) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(156.dp)
                                        .onGloballyPositioned { coords -> cardBounds = coords.boundsInRoot() }
                                        .clip(RoundedCornerShape(18.dp))
                                ) {
                                    // Live gradient fallback
                                    AppleMusicGradientCover(
                                        title = album.title ?: "",
                                        artist = album.artist ?: "",
                                        modifier = Modifier.fillMaxSize(),
                                        cornerRadius = 18.dp
                                    )
                                    if (resolvedAlbumCover.isNotBlank() && resolvedAlbumCover.startsWith("http")) {
                                        AsyncImage(
                                            model = resolvedAlbumCover,
                                            contentDescription = album.title ?: "",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = if (isDark) 0.65f else 0.40f))))
                                    )
                                    // Track count badge
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp)
                                            .shadow(2.dp, RoundedCornerShape(50), spotColor = if (isDark) Color.Black.copy(alpha = 0.40f) else Color.Black.copy(alpha = 0.15f))
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isDark) Color(0xFF181A22).copy(alpha = 0.65f) else Color(0xFFFFFFFF).copy(alpha = 0.75f))
                                            .glassRefraction(50.dp, isDark = isDark)
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            "${albumSongs.size} tracks",
                                            color = if (isDark) Color.White else Color(0xFF141416),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    // Play button overlay
                                    val playInteraction = remember { MutableInteractionSource() }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                            .size(34.dp)
                                            .liquidPress(playInteraction)
                                            .shadow(3.dp, CircleShape, spotColor = if (isDark) Color.Black.copy(alpha = 0.50f) else Color.Black.copy(alpha = 0.20f))
                                            .clip(CircleShape)
                                            .background(if (isDark) Color(0xFF1E1E24).copy(alpha = 0.70f) else Color(0xFFE2E2E2).copy(alpha = 0.55f))
                                            .glassRefraction(17.dp, isDark = isDark, isCircle = true)
                                            .clickable(
                                                interactionSource = playInteraction,
                                                indication = null
                                            ) {
                                                albumSongs.firstOrNull()?.let { onPlay(it, albumSongs, cardBounds) }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Play Album",
                                            tint = if (isDark) Color.White else Color(0xFF141416),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(album.title ?: "", color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text("${album.artist ?: ""} • ${album.releaseYear ?: "2026"}", color = textSecondary, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }

            // =========================================================================
            // 2. RECENTLY PLAYED (2nd Section)
            // =========================================================================
            if (recentSongs.isNotEmpty()) {
                item(key = "recent_section") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recently Played", color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("See All", color = AccentRed, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { onSeeAll("recently_played") })
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(recentSongs, key = { index, song -> "recent_${song.id}_$index" }) { _, song ->
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            val cardScale by animateFloatAsState(
                                targetValue = if (isPressed) 0.95f else 1.0f,
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                                label = "recentScale"
                            )
                            var itemBounds by remember { mutableStateOf<Rect?>(null) }
                            Box(
                                modifier = Modifier
                                    .width(140.dp)
                                    .height(180.dp)
                                    .onGloballyPositioned { coords -> itemBounds = coords.boundsInRoot() }
                                    .graphicsLayer {
                                        scaleX = cardScale
                                        scaleY = cardScale
                                    }
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable(interactionSource = interactionSource, indication = null) { onPlay(song, recentSongs, itemBounds) }
                            ) {
                                AppleMusicGradientCover(
                                    title = song.title ?: "",
                                    artist = song.artist ?: "",
                                    modifier = Modifier.fillMaxSize(),
                                    cornerRadius = 20.dp
                                )
                                if (song.coverUrl.isNotBlank() && song.coverUrl.startsWith("http")) {
                                    AsyncImage(model = song.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                }
                                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = if (isDark) 0.80f else 0.55f)))))
                                Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                                    Text(song.title ?: "", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Text(song.artist ?: "", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }

            // =========================================================================
            // 3. GENRE FILTER CHIPS & GENRE MIX (3rd Section)
            // =========================================================================
            item(key = "genre_chips_section") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Explore by Genre", color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Filter", color = textMuted, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(genres.size) { idx ->
                        val g = genres[idx]
                        val isSelected = g == selectedGenre
                        val chipInteraction = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .liquidPress(chipInteraction)
                                .shadow(2.dp, RoundedCornerShape(50), spotColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.12f))
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isSelected) AccentRed
                                    else (if (isDark) Color(0xFF383842).copy(alpha = 0.35f) else Color(0xFFE2E2E2).copy(alpha = 0.55f))
                                )
                                .glassRefraction(50.dp, isDark = isDark)
                                .clickable(interactionSource = chipInteraction, indication = null) {
                                    selectedGenre = g
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = g,
                                color = if (isSelected) Color.White else (if (isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF141416)),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))

                // Horizontal tracks for the selected genre
                if (filteredGenreSongs.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(filteredGenreSongs, key = { index, song -> "genre_${song.id}_$index" }) { _, song ->
                            val cardInteraction = remember { MutableInteractionSource() }
                            val isPressed by cardInteraction.collectIsPressedAsState()
                            val scale by animateFloatAsState(
                                targetValue = if (isPressed) 0.95f else 1.0f,
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                                label = "genreSongScale"
                            )
                            var itemBounds by remember { mutableStateOf<Rect?>(null) }
                            Column(
                                modifier = Modifier
                                    .width(140.dp)
                                    .onGloballyPositioned { coords -> itemBounds = coords.boundsInRoot() }
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .clickable(interactionSource = cardInteraction, indication = null) {
                                        onPlay(song, filteredGenreSongs, itemBounds)
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(140.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                ) {
                                    AppleMusicGradientCover(
                                        title = song.title ?: "",
                                        artist = song.artist ?: "",
                                        modifier = Modifier.fillMaxSize(),
                                        cornerRadius = 16.dp
                                    )
                                    if (song.coverUrl.isNotBlank() && song.coverUrl.startsWith("http")) {
                                        AsyncImage(
                                            model = song.coverUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    // Play icon overlay
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                            .size(30.dp)
                                            .background(Color.Black.copy(alpha = 0.65f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    song.title ?: "",
                                    color = textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Text(
                                    song.artist ?: "",
                                    color = textSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }

            // =========================================================================
            // 4. ADAPTIVE ARTIST SPOTLIGHT PLAYLIST CARD (4th Section)
            // =========================================================================
            item(key = "spotlight_artist_section") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .shadow(4.dp, RoundedCornerShape(22.dp), spotColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.15f))
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                if (isDark) listOf(Color(0xFF261828), Color(0xFF191823), Color(0xFF141416))
                                else listOf(Color(0xFFF3E7EC), Color(0xFFE9ECF5), Color(0xFFF6F7F9))
                            )
                        )
                        .glassRefraction(22.dp, isDark = isDark)
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .shadow(2.dp, RoundedCornerShape(50), spotColor = AccentRed.copy(alpha = 0.35f))
                                    .clip(RoundedCornerShape(50))
                                    .background(AccentRed.copy(alpha = 0.18f))
                                    .glassRefraction(50.dp, isDark = isDark)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = AccentRed, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ARTIST SPOTLIGHT", color = AccentRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                }
                            }
                            Text(
                                "Tailored to your taste",
                                color = textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        var spotlightHeaderBounds by remember { mutableStateOf<Rect?>(null) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { spotlightHeaderBounds = it.boundsInRoot() }
                                .clickable { onSelectArtist(spotlightArtist, spotlightHeaderBounds) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                            ) {
                                AppleMusicGradientCover(
                                    title = spotlightArtist,
                                    artist = "Spotlight",
                                    modifier = Modifier.fillMaxSize(),
                                    cornerRadius = 36.dp,
                                    showLabel = false
                                )
                                if (spotlightArtistImage.isNotBlank() && spotlightArtistImage.startsWith("http")) {
                                    AsyncImage(
                                        model = spotlightArtistImage,
                                        contentDescription = spotlightArtist,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "$spotlightArtist Essentials",
                                    color = textPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Dynamic mix updated from your listening style",
                                    color = textSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 2
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Preview tracks in spotlight
                        spotlightSongs.take(3).forEach { song ->
                            var itemBounds by remember { mutableStateOf<Rect?>(null) }
                            val trackInteraction = remember { MutableInteractionSource() }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { coords -> itemBounds = coords.boundsInRoot() }
                                    .liquidPress(trackInteraction)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(interactionSource = trackInteraction, indication = null) { onPlay(song, spotlightSongs, itemBounds) }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    AppleMusicGradientCover(
                                        title = song.title ?: "",
                                        artist = song.artist ?: "",
                                        modifier = Modifier.fillMaxSize(),
                                        cornerRadius = 8.dp,
                                        showLabel = false
                                    )
                                    if (song.coverUrl.isNotBlank() && song.coverUrl.startsWith("http")) {
                                        AsyncImage(model = song.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(song.title ?: "", color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                    Text(song.artist ?: "", color = textSecondary, fontSize = 11.sp, maxLines = 1)
                                }
                                val dur = if (song.durationSeconds > 0) "${song.durationSeconds / 60}:${String.format("%02d", song.durationSeconds % 60)}" else ""
                                Text(dur, color = textMuted, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val mixInteraction = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .liquidPress(mixInteraction)
                                .shadow(3.dp, RoundedCornerShape(50), spotColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.15f))
                                .clip(RoundedCornerShape(50))
                                .background(AccentRed)
                                .glassRefraction(50.dp, isDark)
                                .clickable(
                                    interactionSource = mixInteraction,
                                    indication = null
                                ) {
                                    onPlayArtistMix(spotlightArtist, spotlightSongs)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play Artist Mix", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }

            // =========================================================================
            // 5. TOP SONGS & MADE FOR YOU (5th Section)
            // =========================================================================
            if (topSongsList.isNotEmpty()) {
                item(key = "top_songs_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Top Songs", color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("See All", color = AccentRed, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { onSeeAll("top_songs") })
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                itemsIndexed(topSongsList, key = { index, song -> "top_${song.id}_$index" }) { _, song ->
                    var itemBounds by remember { mutableStateOf<Rect?>(null) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlay(song, allRecs, itemBounds) }
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .onGloballyPositioned { coords -> itemBounds = coords.boundsInRoot() }
                                .clip(RoundedCornerShape(10.dp))
                        ) {
                            AppleMusicGradientCover(
                                title = song.title ?: "",
                                artist = song.artist ?: "",
                                modifier = Modifier.fillMaxSize(),
                                cornerRadius = 10.dp,
                                showLabel = false
                            )
                            if (song.coverUrl.isNotBlank() && song.coverUrl.startsWith("http")) {
                                AsyncImage(model = song.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(song.title ?: "", color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text(song.artist ?: "", color = textSecondary, fontSize = 12.sp, maxLines = 1)
                            val topLang = remember(song.id) { com.beatles.player.data.MusicRepository.detectLanguage(song) }
                            val topGenre = remember(song.id) { song.genre.ifBlank { com.beatles.player.data.MusicRepository.detectGenre(song) } }
                            Text("$topLang • $topGenre", color = AccentRed.copy(alpha = 0.85f), fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        }
                        val dur = if (song.durationSeconds > 0) "${song.durationSeconds / 60}:${String.format("%02d", song.durationSeconds % 60)}" else ""
                        Text(dur, color = textMuted, fontSize = 12.sp)
                    }
                }
                item(key = "top_songs_divider") { Spacer(modifier = Modifier.height(28.dp)) }
            }

            if (madeForYouSongs.isNotEmpty()) {
                item(key = "made_for_you_section") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Made For You", color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("See All", color = AccentRed, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { onSeeAll("made_for_you") })
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(madeForYouSongs, key = { index, song -> "mfy_${song.id}_$index" }) { _, song ->
                            val interaction = remember { MutableInteractionSource() }
                            val isPressed by interaction.collectIsPressedAsState()
                            val scale by animateFloatAsState(
                                targetValue = if (isPressed) 0.95f else 1.0f,
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                                label = "mfyScale"
                            )
                            var itemBounds by remember { mutableStateOf<Rect?>(null) }
                            Column(
                                modifier = Modifier
                                    .width(144.dp)
                                    .onGloballyPositioned { coords -> itemBounds = coords.boundsInRoot() }
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(interactionSource = interaction, indication = null) { onPlay(song, allRecs, itemBounds) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(144.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                ) {
                                    AppleMusicGradientCover(
                                        title = song.title ?: "",
                                        artist = song.artist ?: "",
                                        modifier = Modifier.fillMaxSize(),
                                        cornerRadius = 14.dp
                                    )
                                    if (song.coverUrl.isNotBlank() && song.coverUrl.startsWith("http")) {
                                        AsyncImage(model = song.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(song.title ?: "", color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text(song.artist ?: "", color = textSecondary, fontSize = 11.sp, maxLines = 1)
                                val mfyLang = remember(song.id) { com.beatles.player.data.MusicRepository.detectLanguage(song) }
                                val mfyGenre = remember(song.id) { song.genre.ifBlank { com.beatles.player.data.MusicRepository.detectGenre(song) } }
                                Text("$mfyLang • $mfyGenre", color = AccentRed.copy(alpha = 0.85f), fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }

            // =========================================================================
            // 6. ALBUM SETS / COLLECTIONS
            // =========================================================================
            if (homeAlbumSets.isNotEmpty()) {
                item(key = "album_sets_section") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Album Sets", color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("${homeAlbumSets.size} collections", color = textMuted, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(homeAlbumSets, key = { index, set -> "set_${set.id}_$index" }) { _, set ->
                            AlbumSetHomeCard(
                                set = set,
                                isDark = isDark,
                                onSelectAlbum = { onSelectAlbum(it, null) },
                                onSelectAlbumSet = onSelectAlbumSet,
                                onPlayAlbumSet = onPlayAlbumSet
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun SeeAllScreen(
    section: String,
    state: PlayerUiState,
    onPlay: (Song, List<Song>, Rect?) -> Unit,
    onBack: () -> Unit,
    onLoadMore: () -> Unit = {}
) {
    val songs = remember(section, state.recentlyPlayed, state.recommendations) {
        val raw = when (section) {
            "recently_played" -> state.recentlyPlayed
            "top_songs" -> state.recommendations.filter { !it.isAlbum }
            "made_for_you" -> state.recommendations.drop(3)
            else -> emptyList()
        }
        raw.filter { it.id.isNotBlank() }.distinctBy { it.id }
    }
    val title = when (section) {
        "recently_played" -> "Recently Played"
        "top_songs" -> "Top Songs"
        "made_for_you" -> "Made For You"
        else -> ""
    }

    val listState = rememberLazyListState()

    val headerOffset by remember {
        derivedStateOf {
            val firstItem = listState.firstVisibleItemIndex
            val firstOffset = listState.firstVisibleItemScrollOffset
            if (firstItem > 0) -80f else (-firstOffset.toFloat()).coerceIn(-80f, 0f)
        }
    }
    val headerAlpha by remember {
        derivedStateOf {
            ((80f + headerOffset) / 80f).coerceIn(0f, 1f)
        }
    }

    if (section == "top_songs") {
        LaunchedEffect(listState) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                .collect { lastIdx ->
                    if (lastIdx != null && lastIdx >= songs.size - 3 && !state.isTopHitsLoading) {
                        try { onLoadMore() } catch (_: Exception) {}
                    }
                }
        }
    }

    val isDark = isDarkTheme(state.themeMode)
    val textPrimary = if (isDark) Color.White else Color(0xFF141416)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF6B7280)
    val textMuted = if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF9CA3AF)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .edgeFade(if (isDark) Color(0xFF141416) else Color(0xFFF6F7F9)),
            contentPadding = PaddingValues(top = 170.dp, bottom = LocalDynamicBottomPadding.current)
        ) {
            itemsIndexed(songs, key = { index, song -> "seeall_${song.id}_$index" }) { _, song ->
                var itemBounds by remember { mutableStateOf<Rect?>(null) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlay(song, songs, itemBounds) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .onGloballyPositioned { coords -> itemBounds = coords.boundsInRoot() }
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        AppleMusicGradientCover(
                            title = song.title ?: "",
                            artist = song.artist ?: "",
                            modifier = Modifier.fillMaxSize(),
                            cornerRadius = 10.dp,
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
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(song.title ?: "", color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        Text(song.artist ?: "", color = textSecondary, fontSize = 12.sp, maxLines = 1)
                        val songLang = remember(song.id) { com.beatles.player.data.MusicRepository.detectLanguage(song) }
                        val songGenre = remember(song.id) { song.genre.ifBlank { com.beatles.player.data.MusicRepository.detectGenre(song) } }
                        Text("$songLang • $songGenre", color = AccentRed.copy(alpha = 0.85f), fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    }
                    val dur = if (song.durationSeconds > 0) "${song.durationSeconds / 60}:${String.format("%02d", song.durationSeconds % 60)}" else ""
                    Text(dur, color = textMuted, fontSize = 12.sp)
                }
            }

            if (state.isTopHitsLoading && section == "top_songs") {
                item(key = "top_hits_loading") {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentRed, modifier = Modifier.size(30.dp), strokeWidth = 2.dp)
                    }
                }
            }
        }

        // Floating Title Bar with back arrow
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, headerOffset.roundToInt()) }
                .graphicsLayer { alpha = headerAlpha }
                .statusBarsPadding(),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
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
                        .clickable(interactionSource = backInteraction, indication = null) { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isDark) Color.White else Color(0xFF141416),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        color = textPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${songs.size} tracks available",
                        color = textMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AlbumSetHomeCard(
    set: AlbumSet,
    isDark: Boolean,
    onSelectAlbum: (Album) -> Unit,
    onSelectAlbumSet: (AlbumSet, Rect?) -> Unit,
    onPlayAlbumSet: (AlbumSet) -> Unit = {}
) {
    val textPrimary = if (isDark) Color.White else Color(0xFF141416)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF6B7280)
    val textMuted = if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF9CA3AF)
    val cardBg = if (isDark) Color(0xFF1E1E24).copy(alpha = 0.70f) else Color.White
    val setAlbums = set.albums ?: emptyList()
    var cardBounds by remember { mutableStateOf<Rect?>(null) }
    val cardInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .width(236.dp)
            .onGloballyPositioned { coords -> cardBounds = coords.boundsInRoot() }
            .liquidPress(cardInteraction)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.12f)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(cardBg)
            .border(
                0.5.dp,
                if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.08f),
                RoundedCornerShape(22.dp)
            )
            .clickable(interactionSource = cardInteraction, indication = null) {
                onSelectAlbumSet(set, cardBounds)
            }
            .padding(14.dp)
    ) {
        Column {
            // Authentic physical fan of cards preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentAlignment = Alignment.Center
            ) {
                val covers = setAlbums.mapNotNull { alb ->
                    val c = alb.coverUrl.ifBlank {
                        alb.songs.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl ?: ""
                    }
                    alb to c
                }.take(3)

                if (covers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = textMuted, modifier = Modifier.size(36.dp))
                    }
                } else if (covers.size == 1) {
                    val (alb, cover) = covers[0]
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .shadow(6.dp, RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp))
                            .border(0.5.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(14.dp))
                    ) {
                        AppleMusicGradientCover(
                            title = alb.title,
                            artist = alb.artist,
                            modifier = Modifier.fillMaxSize(),
                            cornerRadius = 14.dp,
                            showLabel = false
                        )
                        if (cover.isNotBlank() && cover.startsWith("http")) {
                            AsyncImage(model = cover, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                    }
                } else if (covers.size == 2) {
                    // 2 cards fanned
                    covers.forEachIndexed { idx, (alb, cover) ->
                        val angle = if (idx == 0) -9f else 9f
                        val offsetX = if (idx == 0) (-22).dp else 22.dp
                        val z = if (idx == 0) 1f else 2f
                        Box(
                            modifier = Modifier
                                .offset(x = offsetX, y = 4.dp)
                                .zIndex(z)
                                .graphicsLayer {
                                    rotationZ = angle
                                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.9f)
                                }
                                .size(96.dp)
                                .shadow(5.dp, RoundedCornerShape(14.dp), spotColor = Color.Black.copy(alpha = 0.40f))
                                .clip(RoundedCornerShape(14.dp))
                                .border(0.5.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(14.dp))
                        ) {
                            AppleMusicGradientCover(
                                title = alb.title,
                                artist = alb.artist,
                                modifier = Modifier.fillMaxSize(),
                                cornerRadius = 14.dp,
                                showLabel = false
                            )
                            if (cover.isNotBlank() && cover.startsWith("http")) {
                                AsyncImage(model = cover, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                } else {
                    // 3 cards physical fan (Left, Right, Center)
                    val card0 = covers[0]
                    val card1 = covers[1] // Center/Middle
                    val card2 = covers[2]

                    // Left Card (Index 0)
                    Box(
                        modifier = Modifier
                            .offset(x = (-34).dp, y = 8.dp)
                            .zIndex(1f)
                            .graphicsLayer {
                                rotationZ = -13f
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.9f)
                            }
                            .size(94.dp)
                            .shadow(4.dp, RoundedCornerShape(14.dp), spotColor = Color.Black.copy(alpha = 0.40f))
                            .clip(RoundedCornerShape(14.dp))
                            .border(0.5.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                    ) {
                        AppleMusicGradientCover(
                            title = card0.first.title,
                            artist = card0.first.artist,
                            modifier = Modifier.fillMaxSize(),
                            cornerRadius = 14.dp,
                            showLabel = false
                        )
                        if (card0.second.isNotBlank() && card0.second.startsWith("http")) {
                            AsyncImage(model = card0.second, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                    }

                    // Right Card (Index 2)
                    Box(
                        modifier = Modifier
                            .offset(x = 34.dp, y = 8.dp)
                            .zIndex(2f)
                            .graphicsLayer {
                                rotationZ = 13f
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.9f)
                            }
                            .size(94.dp)
                            .shadow(4.dp, RoundedCornerShape(14.dp), spotColor = Color.Black.copy(alpha = 0.40f))
                            .clip(RoundedCornerShape(14.dp))
                            .border(0.5.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                    ) {
                        AppleMusicGradientCover(
                            title = card2.first.title,
                            artist = card2.first.artist,
                            modifier = Modifier.fillMaxSize(),
                            cornerRadius = 14.dp,
                            showLabel = false
                        )
                        if (card2.second.isNotBlank() && card2.second.startsWith("http")) {
                            AsyncImage(model = card2.second, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                    }

                    // Middle/Center Card (Index 1) - sits prominently in the center!
                    Box(
                        modifier = Modifier
                            .offset(x = 0.dp, y = (-4).dp)
                            .zIndex(3f)
                            .graphicsLayer {
                                rotationZ = 0f
                                scaleX = 1.04f
                                scaleY = 1.04f
                            }
                            .size(96.dp)
                            .shadow(10.dp, RoundedCornerShape(14.dp), spotColor = Color.Black.copy(alpha = 0.55f))
                            .clip(RoundedCornerShape(14.dp))
                            .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                    ) {
                        AppleMusicGradientCover(
                            title = card1.first.title,
                            artist = card1.first.artist,
                            modifier = Modifier.fillMaxSize(),
                            cornerRadius = 14.dp,
                            showLabel = false
                        )
                        if (card1.second.isNotBlank() && card1.second.startsWith("http")) {
                            AsyncImage(model = card1.second, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(set.name ?: "Album Set", color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text("${setAlbums.size} albums • ${(set.description ?: "").ifBlank { "Curated Set" }}", color = textSecondary, fontSize = 11.sp, maxLines = 1)

            if (setAlbums.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val playSetInteraction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .liquidPress(playSetInteraction)
                            .shadow(3.dp, RoundedCornerShape(50), spotColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.15f))
                            .clip(RoundedCornerShape(50))
                            .background(AccentRed)
                            .glassRefraction(50.dp, isDark = isDark)
                            .clickable(interactionSource = playSetInteraction, indication = null) {
                                onPlayAlbumSet(set)
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play Set", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    val expandInteraction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .liquidPress(expandInteraction)
                            .shadow(3.dp, RoundedCornerShape(50), spotColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.15f))
                            .clip(RoundedCornerShape(50))
                            .background(if (isDark) Color(0xFF383842).copy(alpha = 0.35f) else Color(0xFFE2E2E2).copy(alpha = 0.45f))
                            .glassRefraction(50.dp, isDark = isDark)
                            .clickable(interactionSource = expandInteraction, indication = null) {
                                onSelectAlbumSet(set, cardBounds)
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Expand", color = if (isDark) Color.White else Color(0xFF141416), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = if (isDark) Color.White else Color(0xFF141416),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
