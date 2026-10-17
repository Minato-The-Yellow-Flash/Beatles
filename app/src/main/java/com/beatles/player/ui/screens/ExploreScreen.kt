package com.beatles.player.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.beatles.player.data.Song
import kotlinx.coroutines.async
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.shadow
import com.beatles.player.ui.glassRefraction
import com.beatles.player.ui.motion.liquidPress
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.beatles.player.data.MusicRepository
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.filled.Shuffle
import com.beatles.player.ui.components.FrostedButton
import com.beatles.player.ui.components.FrostedIconButton
import com.beatles.player.ui.components.edgeFade
import com.beatles.player.ui.theme.isDarkTheme
import com.beatles.player.ui.theme.LocalDynamicBottomPadding
import com.beatles.player.viewmodel.PlayerUiState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val AccentRed = Color(0xFFF82C38)

data class GenrePalette(
    val name: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color
)

val GENRE_PALETTES = listOf(
    GenrePalette("Punjabi", Color(0xFFFF5E36), Color(0xFFFF9500), Color(0xFFFF2A6D)),
    GenrePalette("Bollywood", Color(0xFFE50914), Color(0xFFFF3366), Color(0xFF8E44AD)),
    GenrePalette("Hip-Hop", Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121)),
    GenrePalette("Pop", Color(0xFFFF0844), Color(0xFFFFB199), Color(0xFF7F00FF)),
    GenrePalette("Lo-Fi", Color(0xFF2980B9), Color(0xFF6DD5FA), Color(0xFF8E44AD)),
    GenrePalette("Indie", Color(0xFF11998E), Color(0xFF38EF7D), Color(0xFF0575E6)),
    GenrePalette("Ghazal", Color(0xFF134E5E), Color(0xFF71B280), Color(0xFF2C3E50)),
    GenrePalette("Classical", Color(0xFF4B1248), Color(0xFFF0C27B), Color(0xFF702963)),
    GenrePalette("Electronic", Color(0xFF00F260), Color(0xFF0575E6), Color(0xFF00C9FF)),
    GenrePalette("R&B", Color(0xFF654EA3), Color(0xFFEAAFC8), Color(0xFF9B51E0))
)

val GENRES = GENRE_PALETTES.map { it.name to it.primaryColor }

@Composable
fun LiveMorphingGradientCard(
    palette: GenrePalette,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "genreCardGradient")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )

    BoxWithConstraints(modifier = modifier) {
        val w = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val h = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        val startX = (0.1f + 0.35f * phase) * w
        val startY = (0.1f + 0.3f * (1f - phase)) * h
        val endX = (0.9f - 0.3f * phase) * w
        val endY = (0.9f - 0.25f * (1f - phase)) * h

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            palette.primaryColor,
                            palette.secondaryColor,
                            palette.accentColor
                        ),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY)
                    )
                )
        )
        // Moving sheen / soft radial highlight
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
                        center = Offset(
                            x = (0.25f + 0.5f * (1f - phase)) * w,
                            y = (0.2f + 0.6f * phase) * h
                        ),
                        radius = (w * 0.85f)
                    )
                )
        )
    }
}

@Composable
fun LiveMorphingGradientBackground(
    palette: GenrePalette,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "genreBgGradient")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )

    BoxWithConstraints(modifier = modifier) {
        val w = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val h = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        val startX = (0.1f + 0.4f * phase) * w
        val startY = (0.1f + 0.35f * (1f - phase)) * h
        val endX = (0.9f - 0.35f * phase) * w
        val endY = (0.9f - 0.3f * (1f - phase)) * h

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            palette.primaryColor.copy(alpha = 0.85f),
                            palette.secondaryColor.copy(alpha = 0.75f),
                            palette.accentColor.copy(alpha = 0.80f)
                        ),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            palette.accentColor.copy(alpha = 0.55f),
                            Color.Transparent
                        ),
                        center = Offset(
                            x = (0.7f - 0.4f * phase) * w,
                            y = (0.3f + 0.4f * phase) * h
                        ),
                        radius = (w * 0.95f)
                    )
                )
        )
    }
}

@Composable
fun ExploreScreen(
    state: PlayerUiState,
    headerOffset: () -> Float = { 0f },
    headerAlpha: () -> Float = { 1f },
    onPlay: (Song, List<Song>, Rect?) -> Unit,
    onSearch: (String) -> Unit,
    onSelectGenre: ((String, Rect?) -> Unit)? = null
) {
    val isDark = isDarkTheme(state.themeMode)
    val textPrimary = if (isDark) Color.White else Color(0xFF141416)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF6B7280)
    val textMuted = if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF9CA3AF)
    val cardBg = if (isDark) Color.White.copy(alpha = 0.05f) else Color.White
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.06f)

    var selectedGenreForDetail by remember { mutableStateOf<String?>(null) }
    var selectedGenreOriginBounds by remember { mutableStateOf<Rect?>(null) }
    val view = LocalView.current

    var selectedChartGenre by remember { mutableStateOf("Global") }
    var isTopChartsExpanded by remember { mutableStateOf(false) }
    var isChartRefreshing by remember { mutableStateOf(false) }
    var dynamicChartSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    val chartGenres = remember { listOf("Global", "Punjabi", "Bollywood", "Pop", "Hip-Hop", "Lo-Fi") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedChartGenre) {
        isChartRefreshing = true
        try {
            val fetched = MusicRepository.getLiveGlobalTopCharts(selectedChartGenre)
            if (fetched.isNotEmpty()) dynamicChartSongs = fetched
        } catch (_: Exception) {}
        isChartRefreshing = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .edgeFade(if (isDark) Color(0xFF141416) else Color(0xFFF6F7F9)),
            contentPadding = PaddingValues(top = 185.dp, bottom = LocalDynamicBottomPadding.current)
        ) {
            // Horizontal Genre Selector Pills
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(GENRES) { (name, color) ->
                        var pillBounds by remember { mutableStateOf<Rect?>(null) }
                        val pillInteraction = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .liquidPress(pillInteraction)
                                .shadow(2.dp, RoundedCornerShape(50), spotColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.12f))
                                .clip(RoundedCornerShape(50))
                                .background(if (isDark) Color(0xFF383842).copy(alpha = 0.35f) else Color(0xFFE2E2E2).copy(alpha = 0.55f))
                                .glassRefraction(50.dp, isDark = isDark)
                                .onGloballyPositioned { pillBounds = it.boundsInRoot() }
                                .clickable(interactionSource = pillInteraction, indication = null) {
                                    if (onSelectGenre != null) {
                                        onSelectGenre(name, pillBounds)
                                    } else {
                                        selectedGenreOriginBounds = pillBounds
                                        selectedGenreForDetail = name
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    name,
                                    color = if (isDark) Color.White else Color(0xFF141416),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // All Genres 2-Column Grid
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    for (i in GENRES.indices step 2) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (j in i until minOf(i + 2, GENRES.size)) {
                                val (name, _) = GENRES[j]
                                val palette = GENRE_PALETTES.find { it.name.equals(name, ignoreCase = true) }
                                    ?: GenrePalette(name, AccentRed, AccentRed, Color(0xFFFF5E36))
                                var cardBounds by remember { mutableStateOf<Rect?>(null) }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(96.dp)
                                ) {
                                    // Ambient color blur bleed into canvas: only in dark mode to prevent rectangular blur bleed in light mode
                                    if (isDark) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .blur(14.dp)
                                                .background(palette.primaryColor.copy(alpha = 0.40f), RoundedCornerShape(18.dp))
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(18.dp))
                                            .border(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                                            .onGloballyPositioned { cardBounds = it.boundsInRoot() }
                                            .clickable {
                                                if (onSelectGenre != null) {
                                                    onSelectGenre(name, cardBounds)
                                                } else {
                                                    selectedGenreOriginBounds = cardBounds
                                                    selectedGenreForDetail = name
                                                }
                                            }
                                    ) {
                                        LiveMorphingGradientCard(
                                            palette = palette,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.15f))
                                        )
                                        Text(
                                            name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            // Top Charts Section with Multiple Charts, Refresh, and See More
            val currentChartList = if (dynamicChartSongs.isNotEmpty()) {
                dynamicChartSongs
            } else {
                state.recommendations.filter { !it.isAlbum }
            }

            if (currentChartList.isNotEmpty() || isChartRefreshing) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Top Charts",
                                color = textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    com.beatles.player.ui.components.Haptics.tick(view)
                                    scope.launch {
                                        isChartRefreshing = true
                                        try {
                                            val fresh = MusicRepository.getLiveGlobalTopCharts(selectedChartGenre)
                                            if (fresh.isNotEmpty()) dynamicChartSongs = fresh
                                        } catch (_: Exception) {}
                                        isChartRefreshing = false
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                if (isChartRefreshing) {
                                    CircularProgressIndicator(
                                        color = AccentRed,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Refresh Charts",
                                        tint = textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        if (currentChartList.size > 8) {
                            Text(
                                text = if (isTopChartsExpanded) "See Less" else "See More",
                                color = AccentRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { isTopChartsExpanded = !isTopChartsExpanded }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // Language / Genre Chart Selector Chips
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(chartGenres) { g ->
                            val isSelected = selectedChartGenre == g
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
                                        selectedChartGenre = g
                                        isTopChartsExpanded = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = g,
                                    color = if (isSelected) Color.White else (if (isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF141416)),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .background(cardBg, RoundedCornerShape(16.dp))
                    ) {
                        val tracksToShow = if (isTopChartsExpanded) currentChartList else currentChartList.take(8)
                        for ((idx, song) in tracksToShow.withIndex()) {
                            var rowBounds by remember { mutableStateOf<Rect?>(null) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPlay(song, currentChartList, rowBounds) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${idx + 1}", color = textMuted, fontSize = 13.sp, modifier = Modifier.width(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                AsyncImage(
                                    model = song.coverUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .onGloballyPositioned { rowBounds = it.boundsInRoot() }
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(song.title, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                    Text(song.artist, color = textSecondary, fontSize = 12.sp, maxLines = 1)
                                }
                                val dur = if (song.durationSeconds > 0) "${song.durationSeconds / 60}:${String.format("%02d", song.durationSeconds % 60)}" else ""
                                Text(dur, color = textMuted, fontSize = 12.sp)
                            }
                            if (idx < tracksToShow.size - 1) HorizontalDivider(color = dividerColor)
                        }
                    }
                }
            }
        }

        // Header: "Browse" Title (Hidden when category detail overlay is open)
        if (selectedGenreForDetail == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, (headerOffset() * 0.85f).roundToInt()) }
                    .graphicsLayer { alpha = headerAlpha() }
                    .padding(top = 110.dp, start = 16.dp, end = 16.dp, bottom = 12.dp)
            ) {
                Text("Browse", color = textPrimary, fontSize = 31.sp, fontWeight = FontWeight.Black)
                Text(
                    "Discover something new",
                    color = textSecondary,
                    fontSize = 13.sp
                )
            }
        }

        // Fluid Genre Detail Overlay: expanding animation matching expanded album page, page color matching artwork/genre
        if (onSelectGenre == null) {
            FluidGenreDetailOverlay(
                genre = selectedGenreForDetail,
                sourceBounds = selectedGenreOriginBounds,
                state = state,
                onClose = { selectedGenreForDetail = null },
                onPlay = onPlay
            )
        }
    }
}

@Composable
fun FluidGenreDetailOverlay(
    genre: String?,
    sourceBounds: Rect?,
    state: PlayerUiState,
    onClose: () -> Unit,
    onPlay: (Song, List<Song>, Rect?) -> Unit
) {
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val isDark = isDarkTheme(state.themeMode)

    var currentGenre by remember { mutableStateOf<String?>(null) }
    var currentOriginBounds by remember { mutableStateOf<Rect?>(null) }
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    var genreTracks by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isGenreLoading by remember { mutableStateOf(false) }

    LaunchedEffect(genre) {
        if (genre != null) {
            currentGenre = genre
            if (sourceBounds != null) {
                currentOriginBounds = sourceBounds
            }
            isGenreLoading = true
            val local = (state.recommendations + state.recentlyPlayed + state.albums.flatMap { it.songs })
                .filter { s ->
                    s.genre.contains(genre, ignoreCase = true) ||
                    s.title.contains(genre, ignoreCase = true) ||
                    s.artist.contains(genre, ignoreCase = true)
                }.distinctBy { it.id }
            genreTracks = local
            scope.launch {
                try {
                    val (q1, q2) = when (genre.lowercase().trim()) {
                        "punjabi" -> "top punjabi songs 2024" to "latest punjabi hits"
                        "bollywood" -> "top bollywood songs 2024" to "hindi romantic hits"
                        "hip-hop" -> "top hip hop songs 2024" to "rap drill hits"
                        "pop" -> "top pop hits 2024" to "viral pop songs"
                        "lo-fi" -> "lofi chill beats songs" to "lofi study beats"
                        "indie" -> "top indie songs 2024" to "best indie folk"
                        "ghazal" -> "best ghazals hits" to "jagjit singh ghazals"
                        "classical" -> "indian classical music hits" to "classical instrumental"
                        "electronic" -> "electronic dance music hits" to "edm festival hits"
                        "r&b" -> "top r&b soul songs" to "modern r&b playlist"
                        else -> "$genre hits 2024" to "best $genre tracks"
                    }
                    val d1 = async { MusicRepository.search(q1) }
                    val d2 = async { MusicRepository.search(q2) }
                    val combined = (local + d1.await() + d2.await()).distinctBy { it.id }
                    if (combined.isNotEmpty()) {
                        genreTracks = combined
                    }
                } catch (_: Exception) {}
                isGenreLoading = false
            }
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.84f, stiffness = 320f)
            )
        } else {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 240, easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f))
            )
            currentGenre = null
            currentOriginBounds = null
            genreTracks = emptyList()
        }
    }

    val p = progress.value
    val curGenre = currentGenre

    fun handleClose() {
        scope.launch {
            progress.animateTo(0f, tween(240, easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)))
            onClose()
        }
    }

    BackHandler(enabled = (p > 0.01f || genre != null)) {
        handleClose()
    }

    if (curGenre != null && p > 0.001f) {
        val genreColor = GENRES.find { it.first.equals(curGenre, ignoreCase = true) }?.second ?: AccentRed
        val screenWpx = with(density) { config.screenWidthDp.dp.toPx() }.coerceAtLeast(100f)
        val screenHpx = with(density) { config.screenHeightDp.dp.toPx() }.coerceAtLeast(100f)

        val origin = currentOriginBounds ?: Rect(
            screenWpx / 2f - 120f,
            screenHpx / 2f - 120f,
            screenWpx / 2f + 120f,
            screenHpx / 2f + 120f
        )

        // Interpolate container bounds exactly like FluidAlbumDetailOverlay
        val curLeft = origin.left * (1f - p)
        val curTop = origin.top * (1f - p)
        val curWidth = (origin.width + (screenWpx - origin.width) * p).coerceAtLeast(20f)
        val curHeight = (origin.height + (screenHpx - origin.height) * p).coerceAtLeast(20f)
        val curCorner = with(density) { (18.dp.toPx() * (1f - p)).coerceAtLeast(0f).toDp() }

        val palette = GENRE_PALETTES.find { it.name.equals(curGenre, ignoreCase = true) }
            ?: GenrePalette(curGenre, genreColor, genreColor, AccentRed)

        val listState = rememberLazyListState()

        // Scroll-driven swipe up parallax and blur calculation for the hero album artwork
        val scrollOffset = if (listState.firstVisibleItemIndex == 0) {
            listState.firstVisibleItemScrollOffset.toFloat()
        } else {
            listState.firstVisibleItemScrollOffset.toFloat() + 800f
        }
        val artTranslateY = (-scrollOffset * 0.75f).coerceAtLeast(-400f) * p
        val artBlur = ((scrollOffset / 14f) * p).coerceIn(0f, 24f).dp
        val artScale = (1f - (scrollOffset / 1400f) * p).coerceIn(0.72f, 1f)
        val artAlpha = (1f - (scrollOffset / 340f) * p).coerceIn(0f, 1f)

        var dragY by remember { mutableFloatStateOf(0f) }
        val pageOpaqueBg = if (isDark) Color(0xFF0F0F12) else Color(0xFFF6F7F9)

        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.40f * p))
            )

            // 2. Full-screen Sheet with Vibrant Ambient Blur (GPU accelerated entrance/exit)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = p.coerceIn(0f, 1f)
                        translationY = (1f - p) * with(density) { 36.dp.toPx() } + dragY
                    }
                    .background(pageOpaqueBg)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (dragY > 140f) {
                                    handleClose()
                                } else {
                                    dragY = 0f
                                }
                            },
                            onVerticalDrag = { _, dragAmount ->
                                if (dragAmount > 0 || dragY > 0) {
                                    dragY = (dragY + dragAmount).coerceAtLeast(0f)
                                    if (dragY > 220f) {
                                        handleClose()
                                    }
                                }
                            }
                        )
                    }
            ) {
                // Ambient Blurred Artwork Backdrop inside morphing card
                Box(modifier = Modifier.fillMaxSize()) {
                    LiveMorphingGradientBackground(
                        palette = palette,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(80.dp)
                            .graphicsLayer { alpha = 0.65f }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = if (isDark) 0.65f else 0.35f))
                    )
                }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 44.dp, bottom = LocalDynamicBottomPadding.current + 28.dp)
                        ) {
                    // Album Page Hero Section: Centered 200dp Cover placeholder + Parallax & Blur
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.size(200.dp))
                            Spacer(modifier = Modifier.height(20.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        alpha = (1f - (scrollOffset / 320f) * p).coerceIn(0f, 1f)
                                        translationY = (-scrollOffset * 0.4f).coerceAtLeast(-200f) * p
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "GENRE SPOTLIGHT",
                                    color = palette.accentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    curGenre,
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    if (isGenreLoading) "Fetching curated tracks..." else "${genreTracks.size} tracks curated for you",
                                    color = Color.White.copy(alpha = 0.70f),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Action Controls Row: Shuffle & Large Center Play (matching AlbumDetailSheet borderless circular buttons)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
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
                                        if (genreTracks.isNotEmpty()) {
                                            val shuffled = genreTracks.shuffled()
                                            onPlay(shuffled.first(), shuffled, null)
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

                            // Large Center Play Button (64dp liquid glass circle)
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
                                        if (genreTracks.isNotEmpty()) onPlay(genreTracks.first(), genreTracks, null)
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

                            // Save Button (50dp liquid glass circle on right side)
                            val saveInteraction = remember { MutableInteractionSource() }
                            var isGenreSaved by remember { mutableStateOf(false) }
                            val context = androidx.compose.ui.platform.LocalContext.current
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
                                    ) {
                                        isGenreSaved = !isGenreSaved
                                        if (isGenreSaved && genreTracks.isNotEmpty()) {
                                            for (s in genreTracks.take(15)) {
                                                com.beatles.player.data.LocalStorage.saveFavorite(context, s)
                                            }
                                            android.widget.Toast.makeText(context, "Saved category tracks to Library", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            android.widget.Toast.makeText(context, "Removed from Library", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isGenreSaved) Icons.Default.Bookmark else Icons.Default.BookmarkAdd,
                                    contentDescription = "Save to Library",
                                    tint = if (isGenreSaved) Color(0xFFF82C38) else buttonIconTint,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Tracklist (matching AlbumDetailSheet styling)
                    if (genreTracks.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                            ) {
                                for ((idx, song) in genreTracks.withIndex()) {
                                    var rowBounds by remember { mutableStateOf<Rect?>(null) }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onPlay(song, genreTracks, rowBounds) }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${idx + 1}", color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp, modifier = Modifier.width(24.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        AsyncImage(
                                            model = song.coverUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(46.dp)
                                                .onGloballyPositioned { rowBounds = it.boundsInRoot() }
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(song.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                            Text(song.artist, color = Color.White.copy(alpha = 0.70f), fontSize = 12.sp, maxLines = 1)
                                        }
                                        val dur = if (song.durationSeconds > 0) "${song.durationSeconds / 60}:${String.format("%02d", song.durationSeconds % 60)}" else ""
                                        Text(dur, color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
                                    }
                                    if (idx < genreTracks.size - 1) {
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                    }
                                }
                            }
                        }
                    } else if (isGenreLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = AccentRed, strokeWidth = 2.5.dp, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }

            // 3. Back Button (Top Left)
            val backInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 12.dp, top = 8.dp)
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

            // 4. Shared Morph Hero Category Card: glides seamlessly from tapped origin card to centered hero position
            val targetArtworkSize = 200.dp
            val targetArtworkSizePx = with(density) { targetArtworkSize.toPx() }
            val targetTopPx = with(density) { 44.dp.toPx() }
            val targetLeftPx = (screenWpx - targetArtworkSizePx) / 2f

            val curArtLeft = origin.left + (targetLeftPx - origin.left) * p
            val curArtTop = origin.top + (targetTopPx + artTranslateY - origin.top) * p + dragY
            val curArtWidth = origin.width + (targetArtworkSizePx - origin.width) * p
            val curArtHeight = origin.height + (targetArtworkSizePx - origin.height) * p
            val curArtCorner = with(density) { (18.dp.toPx() + (22.dp.toPx() - 18.dp.toPx()) * p).toDp() }

            Box(
                modifier = Modifier
                    .offset { IntOffset(curArtLeft.roundToInt(), curArtTop.roundToInt()) }
                    .size(
                        width = with(density) { curArtWidth.toDp() },
                        height = with(density) { curArtHeight.toDp() }
                    )
                    .graphicsLayer {
                        scaleX = if (p >= 0.95f) artScale else 1f
                        scaleY = if (p >= 0.95f) artScale else 1f
                        alpha = if (p >= 0.95f) artAlpha.coerceIn(0.08f, 1f) else 1f
                    }
                    .then(if (artBlur > 0.dp && p >= 0.95f) Modifier.blur(artBlur) else Modifier)
                    .clip(RoundedCornerShape(curArtCorner))
                    .shadow(elevation = (18.dp * p).coerceAtLeast(0.dp), shape = RoundedCornerShape(curArtCorner))
            ) {
                LiveMorphingGradientCard(
                    palette = palette,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.12f))
                )
                if (p < 0.85f) {
                    val titleAlpha = ((0.85f - p) / 0.85f).coerceIn(0f, 1f)
                    Text(
                        curGenre,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        modifier = Modifier
                            .padding(16.dp)
                            .graphicsLayer { alpha = titleAlpha }
                    )
                }
            }
        }
    }
}
