package com.beatles.player.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.beatles.player.data.MusicRepository
import com.beatles.player.data.Song
import com.beatles.player.ui.components.edgeFade
import com.beatles.player.ui.theme.isDarkTheme
import com.beatles.player.viewmodel.PlayerUiState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val AccentRed = Color(0xFFF82C38)
private val GENRES = listOf(
    "Punjabi" to Color(0xFFE67E22),
    "Bollywood" to Color(0xFFC0392B),
    "Hip-Hop" to Color(0xFF8E44AD),
    "Pop" to Color(0xFFE74C3C),
    "Lo-Fi" to Color(0xFF2980B9),
    "Indie" to Color(0xFF16A085),
    "Ghazal" to Color(0xFF27AE60),
    "Classical" to Color(0xFF34495E),
    "Electronic" to Color(0xFF1ABC9C),
    "R&B" to Color(0xFF9B59B6)
)

@Composable
fun ExploreScreen(
    state: PlayerUiState,
    headerOffset: () -> Float = { 0f },
    headerAlpha: () -> Float = { 1f },
    onPlay: (Song, List<Song>, androidx.compose.ui.geometry.Rect?) -> Unit,
    onSearch: (String) -> Unit
) {
    val isDark = isDarkTheme(state.themeMode)
    val textPrimary = if (isDark) Color.White else Color(0xFF141416)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF6B7280)
    val textMuted = if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF9CA3AF)
    val cardBg = if (isDark) Color.White.copy(alpha = 0.05f) else Color.White
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.06f)

    var selectedGenre by remember { mutableStateOf<String?>("All") }
    var genreTracks by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isGenreLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Dynamically filter or fetch tracks when user selects a genre
    LaunchedEffect(selectedGenre) {
        val genre = selectedGenre
        if (genre != null && genre != "All") {
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
                    val fetched = MusicRepository.search("$genre music hits")
                    genreTracks = (local + fetched).distinctBy { it.id }
                } catch (_: Exception) {}
                isGenreLoading = false
            }
        } else {
            genreTracks = emptyList()
            isGenreLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .edgeFade(if (isDark) Color(0xFF141416) else Color(0xFFF6F7F9)),
            contentPadding = PaddingValues(top = 180.dp, bottom = 120.dp)
        ) {
            // Horizontal Genre Selector Pills
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        val isAllSelected = selectedGenre == null || selectedGenre == "All"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isAllSelected) AccentRed else if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f))
                                .clickable { selectedGenre = "All" }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "All Genres",
                                color = if (isAllSelected) Color.White else textSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    items(GENRES) { (name, color) ->
                        val isSelected = selectedGenre == name
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) color else if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f))
                                .clickable { selectedGenre = if (isSelected) "All" else name }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    name,
                                    color = if (isSelected) Color.White else textSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Genre-Specific Content View (when a genre is selected)
            if (selectedGenre != null && selectedGenre != "All") {
                val currentGenre = selectedGenre!!
                val genreColor = GENRES.find { it.first == currentGenre }?.second ?: AccentRed

                item {
                    // Hero Banner for Selected Genre
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(120.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(genreColor, genreColor.copy(alpha = 0.65f), Color(0xFF141416))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(modifier = Modifier.align(Alignment.CenterStart)) {
                            Text("EXPLORING GENRE", color = Color.White.copy(alpha = 0.70f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(currentGenre, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                if (isGenreLoading) "Fetching latest tracks..." else "${genreTracks.size} tracks curated for you",
                                color = Color.White.copy(alpha = 0.80f),
                                fontSize = 12.sp
                            )
                        }

                        if (genreTracks.isNotEmpty()) {
                            IconButton(
                                onClick = { onPlay(genreTracks.first(), genreTracks, null) },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play Genre", tint = Color.Black, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Genre Tracklist
                if (genreTracks.isNotEmpty()) {
                    item {
                        Text(
                            "$currentGenre Top Hits",
                            color = textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .background(cardBg, RoundedCornerShape(16.dp))
                        ) {
                            genreTracks.take(15).forEachIndexed { idx, song ->
                                var rowBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onGloballyPositioned { rowBounds = it.boundsInRoot() }
                                        .clickable { onPlay(song, genreTracks, rowBounds) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${idx + 1}", color = textMuted, fontSize = 13.sp, modifier = Modifier.width(22.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    AsyncImage(
                                        model = song.coverUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(song.title, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        Text(song.artist, color = textSecondary, fontSize = 12.sp, maxLines = 1)
                                    }
                                    val dur = if (song.durationSeconds > 0) "${song.durationSeconds / 60}:${String.format("%02d", song.durationSeconds % 60)}" else ""
                                    Text(dur, color = textMuted, fontSize = 12.sp)
                                }
                                if (idx < minOf(14, genreTracks.size - 1)) {
                                    HorizontalDivider(color = dividerColor)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                } else if (isGenreLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AccentRed, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            } else {
                // All Genres Grid
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        for (i in GENRES.indices step 2) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                for (j in i until minOf(i + 2, GENRES.size)) {
                                    val (name, color) = GENRES[j]
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(96.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(color)
                                            .clickable { selectedGenre = name }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)
                                                    )
                                                )
                                        )
                                        Text(
                                            name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            modifier = Modifier.padding(14.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                // Top Charts
                if (state.recommendations.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Top Charts",
                            color = textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .background(cardBg, RoundedCornerShape(16.dp))
                        ) {
                            state.recommendations.filter { !it.isAlbum }.take(8).forEachIndexed { idx, song ->
                                var rowBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onGloballyPositioned { rowBounds = it.boundsInRoot() }
                                        .clickable { onPlay(song, state.recommendations, rowBounds) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${idx + 1}", color = textMuted, fontSize = 13.sp, modifier = Modifier.width(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    AsyncImage(
                                        model = song.coverUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(song.title, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                        Text(song.artist, color = textSecondary, fontSize = 12.sp, maxLines = 1)
                                    }
                                    val dur = if (song.durationSeconds > 0) "${song.durationSeconds / 60}:${String.format("%02d", song.durationSeconds % 60)}" else ""
                                    Text(dur, color = textMuted, fontSize = 12.sp)
                                }
                                if (idx < 7) HorizontalDivider(color = dividerColor)
                            }
                        }
                    }
                }
            }
        }

        // Header Title
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, (headerOffset() * 0.85f).roundToInt()) }
                .graphicsLayer { alpha = headerAlpha() }
                .padding(top = 100.dp, start = 16.dp, end = 16.dp, bottom = 12.dp)
        ) {
            Text("Browse", color = textPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(
                if (selectedGenre != null && selectedGenre != "All") "Genre: $selectedGenre" else "Discover something new",
                color = textSecondary,
                fontSize = 13.sp
            )
        }
    }
}
