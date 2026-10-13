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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.beatles.player.data.Album
import com.beatles.player.data.AlbumSet
import com.beatles.player.data.Song
import com.beatles.player.ui.theme.isDarkTheme
import com.beatles.player.viewmodel.PlayerUiState
import androidx.compose.ui.zIndex
import com.beatles.player.ui.components.edgeFade
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
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.foundation.isSystemInDarkTheme

private val AccentRed = Color(0xFFF82C38)

@Composable
fun HomeScreen(
    state: PlayerUiState,
    onPlay: (Song, List<Song>) -> Unit,
    onSeeAll: (String) -> Unit,
    onSelectAlbum: (Album, Rect?) -> Unit = { _, _ -> },
    onPlayAlbumSet: (AlbumSet) -> Unit = {},
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

    var pullOffsetRaw by remember { mutableFloatStateOf(0f) }
    val pullAnim = remember { androidx.compose.animation.core.Animatable(0f) }
    val currentPullOffset = if (pullOffsetRaw > 0f) pullOffsetRaw else pullAnim.value

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0 && pullOffsetRaw > 0f) {
                    val consumed = available.y
                    pullOffsetRaw = (pullOffsetRaw + consumed).coerceAtLeast(0f)
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0 && source == NestedScrollSource.UserInput) {
                    pullOffsetRaw = (pullOffsetRaw + available.y * 0.45f).coerceAtMost(220f)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullOffsetRaw > 110f) {
                    try { onRefresh() } catch (_: Exception) {}
                }
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
        // Pull-to-refresh badge (positioned safely below top header bar so it's never covered up)
        if (currentPullOffset > 12f || state.isRefreshingHome) {
            val refreshAlpha = if (state.isRefreshingHome) 1f else (currentPullOffset / 110f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(10f)
                    .padding(top = 100.dp)
                    .offset { IntOffset(0, (currentPullOffset * 0.4f).roundToInt()) }
                    .background(Color(0xFFE2E2E2).copy(alpha = 0.16f * refreshAlpha), RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = AccentRed,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (state.isRefreshingHome) "Refreshing..." else "Pull to refresh",
                        color = textPrimary.copy(alpha = refreshAlpha),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        val bgFade = if (isDark) Color(0xFF141416) else Color(0xFFF6F7F9)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, currentPullOffset.roundToInt()) }
                .edgeFade(bgFade),
            contentPadding = PaddingValues(top = 110.dp, bottom = 120.dp)
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
                        text = "For You",
                        color = textPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 0. Recently Played at the top
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
                            Box(
                                modifier = Modifier
                                    .width(140.dp)
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { onPlay(song, recentSongs) }
                            ) {
                                AsyncImage(model = song.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
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

            // 1. Featured Album – personalised to the user's artists/language (no manual refresh)
            if (homeAlbums.isNotEmpty() || state.heroAlbum != null) {
                val heroAlbum = state.heroAlbum ?: homeAlbums.firstOrNull()
                if (heroAlbum != null) {
                    item(key = "hero_section") {
                        val heroGenre = (heroAlbum.genre ?: "Pop").uppercase()
                        val heroYear = heroAlbum.releaseYear ?: "2026"
                        val heroSongs = heroAlbum.songs ?: emptyList()
                        val heroCover = remember(heroAlbum) {
                            if (heroAlbum.coverUrl.isNotBlank() && heroAlbum.coverUrl.startsWith("http")) heroAlbum.coverUrl
                            else heroAlbum.songs.firstOrNull { it.coverUrl.isNotBlank() && it.coverUrl.startsWith("http") }?.coverUrl
                                ?: com.beatles.player.ui.screens.CURATED_ONBOARD_ARTISTS.find { it.name.equals(heroAlbum.artist, ignoreCase = true) }?.imageUrl
                                ?: "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/11/a6/80/11a680e6-2e48-08fa-5e87-3f18e838d31f/23UM1IM11868.rgb.jpg/1000x1000bb.jpg"
                        }
                        var heroBounds by remember { mutableStateOf<Rect?>(null) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(280.dp)
                                .onGloballyPositioned { coords -> heroBounds = coords.boundsInRoot() }
                                .clip(RoundedCornerShape(22.dp))
                        ) {
                            AsyncImage(
                                model = heroCover,
                                contentDescription = heroAlbum.title ?: "",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                fallback = painterResource(id = if (isDark) R.drawable.beatles_logo else R.drawable.beatles_logo_light),
                                error = painterResource(id = if (isDark) R.drawable.beatles_logo else R.drawable.beatles_logo_light)
                            )
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
                            // Frosted badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(14.dp)
                                    .background(Color(0xFFE2E2E2).copy(alpha = 0.16f), RoundedCornerShape(50))
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
                                    Button(
                                        onClick = {
                                            heroSongs.firstOrNull()?.let { onPlay(it, heroSongs) }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                        shape = RoundedCornerShape(50),
                                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Play Album", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { onSelectAlbum(heroAlbum, heroBounds) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E2E2).copy(alpha = 0.16f), contentColor = Color.White),
                                        shape = RoundedCornerShape(50),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("View Album", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (allRecs.isNotEmpty()) {
                item(key = "featured_rec_section") {
                    val featured = allRecs.first()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(260.dp)
                            .clip(RoundedCornerShape(20.dp))
                    ) {
                        AsyncImage(model = featured.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.20f), Color.Black.copy(alpha = if (isDark) 0.88f else 0.65f)))))
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(14.dp)
                                .background(AccentRed, RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text("FEATURED", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                        Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 16.dp, end = 16.dp)) {
                            Text("NEW RELEASE", color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(featured.title ?: "", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                            Text("${featured.artist ?: ""} · ${featured.album ?: ""}", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onPlay(featured, allRecs) },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 8.dp)
                            ) {
                                Text("Play Now", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Featured Albums Row
            if (homeAlbums.isNotEmpty()) {
                item(key = "featured_albums_row") {
                    Spacer(modifier = Modifier.height(28.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Featured Albums", color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("${homeAlbums.size} albums", color = textMuted, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(homeAlbums, key = { index, album -> "alb_${album.id}_$index" }) { _, album ->
                            val albumSongs = album.songs ?: emptyList()
                            val resolvedAlbumCover = remember(album) {
                                if (album.coverUrl.isNotBlank() && album.coverUrl.startsWith("http")) album.coverUrl
                                else album.songs.firstOrNull { it.coverUrl.isNotBlank() && it.coverUrl.startsWith("http") }?.coverUrl
                                    ?: com.beatles.player.ui.screens.CURATED_ONBOARD_ARTISTS.find { it.name.equals(album.artist, ignoreCase = true) }?.imageUrl
                                    ?: "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/11/a6/80/11a680e6-2e48-08fa-5e87-3f18e838d31f/23UM1IM11868.rgb.jpg/1000x1000bb.jpg"
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
                                    .width(152.dp)
                                    .graphicsLayer {
                                        scaleX = cardScale
                                        scaleY = cardScale
                                    }
                                    .onGloballyPositioned { coords -> cardBounds = coords.boundsInRoot() }
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null
                                    ) { onSelectAlbum(album, cardBounds) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(152.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                ) {
                                    AsyncImage(
                                        model = resolvedAlbumCover,
                                        contentDescription = album.title ?: "",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                        fallback = painterResource(id = if (isDark) R.drawable.beatles_logo else R.drawable.beatles_logo_light),
                                        error = painterResource(id = if (isDark) R.drawable.beatles_logo else R.drawable.beatles_logo_light)
                                    )
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
                                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("${albumSongs.size} tracks", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                    }
                                    // Play button overlay
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                            .size(34.dp)
                                            .background(Color.White, CircleShape)
                                            .clickable {
                                                albumSongs.firstOrNull()?.let { onPlay(it, albumSongs) }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Play Album",
                                            tint = Color.Black,
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
                }
            }

            // 3. Made For You
            if (madeForYouSongs.isNotEmpty()) {
                item(key = "made_for_you_section") {
                    Spacer(modifier = Modifier.height(28.dp))
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
                            Column(
                                modifier = Modifier
                                    .width(144.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onPlay(song, allRecs) }
                            ) {
                                AsyncImage(model = song.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(144.dp).clip(RoundedCornerShape(14.dp)))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(song.title ?: "", color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text(song.artist ?: "", color = textSecondary, fontSize = 11.sp, maxLines = 1)
                                val mfyLang = remember(song.id) { com.beatles.player.data.MusicRepository.detectLanguage(song) }
                                val mfyGenre = remember(song.id) { song.genre.ifBlank { com.beatles.player.data.MusicRepository.detectGenre(song) } }
                                Text("$mfyLang • $mfyGenre", color = AccentRed.copy(alpha = 0.85f), fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // 4. Other Section: Top Songs
            if (topSongsList.isNotEmpty()) {
                item(key = "top_songs_header") {
                    Spacer(modifier = Modifier.height(28.dp))
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlay(song, allRecs) }
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(model = song.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)))
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
            }

            // 5. Album Section / Album Sets (Moved to the bottom all together)
            if (homeAlbumSets.isNotEmpty()) {
                item(key = "album_sets_section") {
                    Spacer(modifier = Modifier.height(28.dp))
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
                                onPlayAlbumSet = onPlayAlbumSet
                            )
                        }
                    }
                }
            }

            item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(30.dp)) }
        }
    }
}

@Composable
fun SeeAllScreen(
    section: String,
    state: PlayerUiState,
    onPlay: (Song, List<Song>) -> Unit,
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

    // Dynamic Subheader collapsing on scroll
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

    // Infinite pagination when scrolling Top Songs
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
            contentPadding = PaddingValues(top = 170.dp, bottom = 130.dp)
        ) {
            itemsIndexed(songs, key = { index, song -> "seeall_${song.id}_$index" }) { _, song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlay(song, songs) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = song.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(54.dp).clip(RoundedCornerShape(10.dp))
                    )
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
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .background(if (isDark) Color(0xFF26262A) else Color(0xFFE5E7EB), CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimary,
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
    onPlayAlbumSet: (AlbumSet) -> Unit = {}
) {
    val textPrimary = if (isDark) Color.White else Color(0xFF141416)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF6B7280)
    val textMuted = if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF9CA3AF)
    val cardBg = if (isDark) Color(0xFFE2E2E2).copy(alpha = 0.12f) else Color.White
    val setAlbums = set.albums ?: emptyList()

    Box(
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .padding(14.dp)
    ) {
        Column {
            // Stacked album covers
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp)
            ) {
                val covers = setAlbums.mapNotNull { it.coverUrl }.filter { it.isNotBlank() }.take(3)
                if (covers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = textMuted, modifier = Modifier.size(36.dp))
                    }
                } else {
                    covers.forEachIndexed { idx, cover ->
                        val offsetXDp = (idx * 24).dp
                        val scale = 1f - (covers.size - 1 - idx) * 0.06f
                        AsyncImage(
                            model = cover,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(90.dp)
                                .offset(x = offsetXDp)
                                .scale(scale)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    setAlbums.getOrNull(idx)?.let { onSelectAlbum(it) }
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(set.name ?: "Album Set", color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text("${setAlbums.size} albums • ${(set.description ?: "").ifBlank { "Curated Set" }}", color = textSecondary, fontSize = 11.sp, maxLines = 1)
            if (setAlbums.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play Set (plays all tracks from all albums in set)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(AccentRed)
                            .clickable { onPlayAlbumSet(set) }
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play Set", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // View Set (opens first album in set)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f))
                            .clickable { setAlbums.firstOrNull()?.let { onSelectAlbum(it) } }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("View", color = textPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = textPrimary, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}
