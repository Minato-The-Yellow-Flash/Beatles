package com.beatles.player.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.shadow
import com.beatles.player.ui.glassRefraction
import com.beatles.player.ui.motion.liquidPress
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.beatles.player.data.Album
import com.beatles.player.data.ArtistMixEngine
import com.beatles.player.data.MusicRepository
import com.beatles.player.data.Song
import com.beatles.player.ui.components.AppleMusicGradientCover
import com.beatles.player.ui.components.FrostedButton
import com.beatles.player.ui.theme.isDarkTheme
import com.beatles.player.ui.theme.LocalDynamicBottomPadding
import com.beatles.player.viewmodel.PlayerUiState
import com.beatles.player.ui.components.edgeFade
import kotlin.math.roundToInt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot

@Composable
fun SearchScreen(
    state: PlayerUiState,
    focusRequested: Boolean,
    onSearch: (String) -> Unit,
    onQueryChange: (String) -> Unit = {},
    onSelectAlbum: (Album, Rect?) -> Unit = { _, _ -> },
    onPlay: (Song, List<Song>, Rect?) -> Unit,
    onMoveToPlayNext: (Song) -> Unit = {},
    onPlayArtistMix: (String) -> Unit = {},
    onSelectArtist: (String, Rect?) -> Unit = { _, _ -> },
    onLoadMoreRecommendations: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    // Infinite Search Recommendations trigger
    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            state.searchQuery.isBlank() && totalItems > 0 && lastVisibleItem >= totalItems - 4
        }
    }
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !state.isTopHitsLoading) {
            onLoadMoreRecommendations()
        }
    }

    val isDark = isDarkTheme(state.themeMode)
    val textPrimary = if (isDark) Color.White else Color(0xFF141416)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF6B7280)
    val textMuted = if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF9CA3AF)
    val chipBg = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.06f)
    val cardBg = if (isDark) Color(0xFF22242A) else Color.White

    val autocorrectChips = remember(state.searchQuery, state.searchSuggestions, state.searchResults, state.instantMatchingSongs) {
        val list = mutableListOf<String>()
        val queryNorm = state.searchQuery.trim().lowercase()

        // 1. Google / YouTube Search suggestions (autocorrect / autocomplete)
        for (s in state.searchSuggestions) {
            val trimmed = s.trim()
            if (trimmed.isNotBlank() && !trimmed.equals(queryNorm, ignoreCase = true) && !list.any { it.equals(trimmed, ignoreCase = true) }) {
                list.add(trimmed)
                if (list.size == 4) break
            }
        }

        // 2. Supplement from songs if fewer than 4 (titles and artists)
        val songPool = if (state.searchResults.isNotEmpty()) state.searchResults else state.instantMatchingSongs
        if (list.size < 4) {
            for (song in songPool) {
                val cleanTitle = song.title
                    .replace(Regex("(?i)\\(.*?\\)|\\[.*?\\]|feat\\..*|ft\\..*|official.*"), "")
                    .trim()
                if (cleanTitle.length in 3..35 && !cleanTitle.equals(queryNorm, ignoreCase = true) && !list.any { it.equals(cleanTitle, ignoreCase = true) }) {
                    list.add(cleanTitle)
                    if (list.size == 4) break
                }
                val cleanArtist = song.artist.replace(" - Topic", "").trim()
                if (cleanArtist.length in 3..25 && !cleanArtist.equals(queryNorm, ignoreCase = true) && !list.any { it.equals(cleanArtist, ignoreCase = true) }) {
                    list.add(cleanArtist)
                    if (list.size == 4) break
                }
            }
        }
        list.take(4)
    }

    val songsToDisplay = if (state.searchResults.isNotEmpty()) state.searchResults else state.instantMatchingSongs
    val matchedArtist = remember(songsToDisplay, state.searchQuery) {
        val q = state.searchQuery.trim()
        if (q.isBlank()) null else {
            val direct = songsToDisplay.map { it.artist.replace(" - Topic", "").trim() }
                .firstOrNull { ArtistMixEngine.isArtistMatch(Song("", "", it, "", "", 0), q) }
            if (direct != null) {
                val songForArt = songsToDisplay.firstOrNull { ArtistMixEngine.isArtistMatch(it, direct) }
                Pair(direct, songForArt?.coverUrl ?: "")
            } else {
                val byFreq = songsToDisplay.groupBy { it.artist.replace(" - Topic", "").trim() }
                    .maxByOrNull { it.value.size }
                if (byFreq != null && byFreq.value.size >= 2) {
                    Pair(byFreq.key, byFreq.value.first().coverUrl)
                } else null
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .edgeFade(if (isDark) Color(0xFF141416) else Color(0xFFF6F7F9)),
            contentPadding = PaddingValues(top = 22.dp, bottom = LocalDynamicBottomPadding.current)
        ) {
            if (state.searchQuery.isNotBlank()) {
                // 4 Autocorrect & Suggestion pills displayed prominently above results
                if (autocorrectChips.isNotEmpty()) {
                    item(key = "search_autocorrect_section") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "Autocorrect & Suggestions",
                                color = textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 6.dp)
                            ) {
                                items(autocorrectChips, key = { "chip_$it" }) { chipText ->
                                    val chipInteraction = remember { MutableInteractionSource() }
                                    Box(
                                        modifier = Modifier
                                            .liquidPress(chipInteraction)
                                            .shadow(2.dp, RoundedCornerShape(50), spotColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.12f))
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isDark) Color(0xFF383842).copy(alpha = 0.35f) else Color(0xFFE2E2E2).copy(alpha = 0.55f))
                                            .glassRefraction(50.dp, isDark = isDark)
                                            .clickable(interactionSource = chipInteraction, indication = null) {
                                                onQueryChange(chipText)
                                                onSearch(chipText)
                                                focusManager.clearFocus()
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Search,
                                                contentDescription = null,
                                                tint = Color(0xFFF82C38),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = chipText,
                                                color = if (isDark) Color.White else Color(0xFF141416),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }

                if (matchedArtist != null) {
                    item(key = "search_matched_artist_${matchedArtist.first}") {
                        var artistBounds by remember { mutableStateOf<Rect?>(null) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .shadow(2.dp, RoundedCornerShape(18.dp), spotColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.12f))
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isDark) Color(0xFF1E2026).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.90f))
                                .glassRefraction(18.dp, isDark = isDark)
                                .clickable { onSelectArtist(matchedArtist.first, artistBounds) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = matchedArtist.second,
                                contentDescription = matchedArtist.first,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(54.dp)
                                    .onGloballyPositioned { artistBounds = it.boundsInRoot() }
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "ARTIST",
                                    color = Color(0xFFF82C38),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    matchedArtist.first,
                                    color = textPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                            val viewInteraction = remember { MutableInteractionSource() }
                            Box(
                                modifier = Modifier
                                    .height(36.dp)
                                    .liquidPress(viewInteraction)
                                    .shadow(2.dp, RoundedCornerShape(50), spotColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.12f))
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isDark) Color(0xFF383842).copy(alpha = 0.45f) else Color(0xFFE2E2E2).copy(alpha = 0.65f))
                                    .glassRefraction(50.dp, isDark = isDark)
                                    .clickable(interactionSource = viewInteraction, indication = null) {
                                        onSelectArtist(matchedArtist.first, artistBounds)
                                    }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isDark) Color.White else Color(0xFF141416),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("View Artist", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) Color.White else Color(0xFF141416))
                                }
                            }
                        }
                    }
                }

                if (songsToDisplay.isNotEmpty()) {
                    item(key = "search_results_title") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (state.searchResults.isNotEmpty()) Icons.Default.MusicNote else Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = Color(0xFFF82C38),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (state.searchResults.isNotEmpty()) "Search Results" else "Matching Songs",
                                color = textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    items(songsToDisplay, key = { "search_song_" + it.id }) { song ->
                        SearchSongSwipeItem(
                            song = song,
                            onPlay = { bounds -> onPlay(song, songsToDisplay, bounds) },
                            onMoveToPlayNext = { onMoveToPlayNext(song) },
                            modifier = Modifier.animateItem(
                                fadeInSpec = androidx.compose.animation.core.tween(180),
                                placementSpec = androidx.compose.animation.core.tween(200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                                fadeOutSpec = androidx.compose.animation.core.tween(140)
                            )
                        )
                    }
                }

                // Autocomplete Query Suggestions
                if (state.searchSuggestions.isNotEmpty()) {
                    item(key = "search_suggestions_header") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = textSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Related Searches", color = textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    items(state.searchSuggestions.take(5), key = { "sugg_$it" }) { queryText ->
                        val suggInteraction = remember { MutableInteractionSource() }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 3.dp)
                                .liquidPress(suggInteraction)
                                .shadow(2.dp, RoundedCornerShape(14.dp), spotColor = if (isDark) Color.Black.copy(alpha = 0.30f) else Color.Black.copy(alpha = 0.08f))
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isDark) Color(0xFF1E2026).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.70f))
                                .glassRefraction(14.dp, isDark = isDark)
                                .clickable(interactionSource = suggInteraction, indication = null) {
                                    onQueryChange(queryText)
                                    onSearch(queryText)
                                    focusManager.clearFocus()
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = textMuted, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(queryText, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Normal, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.NorthWest, contentDescription = "Fill", tint = textMuted, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Movie & Original Album card if found
                if (state.searchedAlbum != null) {
                    item(key = "searched_album_card") {
                        val album = state.searchedAlbum!!
                        val isSoundtrack = remember(album) {
                            val t = album.title.lowercase()
                            val g = (album.genre ?: "").lowercase()
                            t.contains("soundtrack") || t.contains("motion picture") || t.contains("from \"") || 
                            t.contains("original score") || g.contains("soundtrack") || t.contains("ost") ||
                            t.contains("movie") || t.contains("film")
                        }
                        var cardBounds by remember { mutableStateOf<Rect?>(null) }
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val cardScale by animateFloatAsState(
                            targetValue = if (isPressed) 0.96f else 1.0f,
                            animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                            label = "searchAlbumCardScale"
                        )
                        val shape = RoundedCornerShape(20.dp)

                        Spacer(modifier = Modifier.height(14.dp))
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (isSoundtrack) Color(0xFFF82C38) else Color(0xFF3B82F6))
                                    )
                                    Spacer(modifier = Modifier.width(7.dp))
                                    Text(
                                        if (isSoundtrack) "Movie Soundtrack Album" else "Original Full Album",
                                        color = textSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    "${album.songs.size} tracks",
                                    color = textMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { coords -> cardBounds = coords.boundsInRoot() }
                                    .graphicsLayer {
                                        scaleX = cardScale
                                        scaleY = cardScale
                                    }
                                    .shadow(4.dp, shape, spotColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.12f))
                                    .clip(shape)
                                    .background(if (isDark) Color(0xFF1E2026).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.90f))
                                    .border(1.dp, if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.30f), shape)
                                    .glassRefraction(20.dp, isDark = isDark)
                                    .clickable(interactionSource = interactionSource, indication = null) {
                                        onSelectAlbum(album, cardBounds)
                                    }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Album Cover with AppleMusicGradientCover fallback and badges
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                    ) {
                                        AppleMusicGradientCover(
                                            title = album.title ?: "",
                                            artist = album.artist ?: "",
                                            modifier = Modifier.fillMaxSize(),
                                            cornerRadius = 16.dp
                                        )
                                        if (album.coverUrl.isNotBlank() && album.coverUrl.startsWith("http")) {
                                            AsyncImage(
                                                model = album.coverUrl,
                                                contentDescription = album.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = if (isDark) 0.55f else 0.35f))))
                                        )
                                        if (isSoundtrack) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(5.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFF82C38).copy(alpha = 0.90f))
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    "MOVIE",
                                                    color = Color.White,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Black,
                                                    letterSpacing = 0.5.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            album.title,
                                            color = textPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            album.artist,
                                            color = textSecondary,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "${album.releaseYear} • ${album.songs.size} tracks",
                                                color = textMuted,
                                                fontSize = 11.sp
                                            )
                                            if (album.genre.isNotBlank() && album.genre != "Music") {
                                                Text(
                                                    " • ${album.genre}",
                                                    color = textMuted,
                                                    fontSize = 11.sp,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Tap to explore full movie album & tracks",
                                            color = Color(0xFFF82C38),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Open Album",
                                            tint = textPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            } else {
                if (state.recommendations.isNotEmpty()) {
                    item(key = "search_rec_header") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                "Recommended for You",
                                color = textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Based on your music taste",
                                color = textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    items(state.recommendations, key = { "search_rec_${it.id}" }) { song ->
                        SearchSongSwipeItem(
                            song = song,
                            onPlay = { bounds -> onPlay(song, state.recommendations, bounds) },
                            onMoveToPlayNext = { onMoveToPlayNext(song) }
                        )
                    }

                    if (state.isTopHitsLoading) {
                        item(key = "search_loading_more") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFFF82C38),
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                } else {
                    item(key = "search_idle") { Spacer(modifier = Modifier.height(1.dp)) }
                }
            }
        }
    }
}

@Composable
fun SearchSongSwipeItem(
    song: Song,
    onPlay: (Rect?) -> Unit,
    onMoveToPlayNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = androidx.compose.ui.platform.LocalView.current
    var offsetX by remember { mutableFloatStateOf(0f) }
    var thresholdArmed by remember { mutableStateOf(false) }
    var itemBounds by remember { mutableStateOf<Rect?>(null) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "searchSwipeOffset"
    )

    val isDark = com.beatles.player.ui.theme.AppTheme.isDark

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(68.dp)
            .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.12f))
            .clip(RoundedCornerShape(16.dp))
            .glassRefraction(16.dp, isDark = isDark)
    ) {
        val totalWidthPx = constraints.maxWidth.toFloat()
        val thresholdPx = totalWidthPx * 0.35f

        // Swipe Right Action Background: "Play Next"
        if (offsetX > 15f) {
            val isOverThreshold = offsetX >= thresholdPx
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF2E7D32).copy(alpha = if (isOverThreshold) 0.95f else 0.55f))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PlaylistPlay,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isOverThreshold) "Release to Play Next" else "Swipe to Play Next",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Foreground Song Row (solid card to prevent bleed-through)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .background(if (isDark) Color(0xFF1E2024) else Color.White)
                .pointerInput(totalWidthPx) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX >= thresholdPx) {
                                com.beatles.player.ui.components.Haptics.confirm(view)
                                onMoveToPlayNext()
                            }
                            offsetX = 0f
                            thresholdArmed = false
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            // Only allow swipe to the right
                            offsetX = (offsetX + dragAmount).coerceIn(0f, totalWidthPx)
                            val over = offsetX >= thresholdPx
                            if (over != thresholdArmed) {
                                thresholdArmed = over
                                if (over) com.beatles.player.ui.components.Haptics.threshold(view)
                            }
                        }
                    )
                }
                .clickable { onPlay(itemBounds) }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .onGloballyPositioned { coords -> itemBounds = coords.boundsInRoot() }
                    .clip(if (song.isAlbum) RoundedCornerShape(12.dp) else RoundedCornerShape(10.dp))
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title,
                    color = if (isDark) Color.White else Color(0xFF141416),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val subText = if (song.album.isNotBlank() && song.album != "Single" && song.album != "Album") "${song.artist} • ${song.album}" else song.artist
                Text(
                    subText,
                    color = if (isDark) Color.White.copy(alpha = 0.55f) else Color(0xFF6B7280),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = { onPlay(itemBounds) }, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = if (isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF141416),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
