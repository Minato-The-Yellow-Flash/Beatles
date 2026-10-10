package com.beatles.player.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.beatles.player.data.Song
import com.beatles.player.ui.theme.isDarkTheme
import com.beatles.player.viewmodel.PlayerUiState
import com.beatles.player.ui.components.edgeFade

import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

private val AccentRed = Color(0xFFF82C38)
private val GENRES = listOf("Bollywood" to Color(0xFFC0392B), "Hip-Hop" to Color(0xFF7B2FBE), "Ghazal" to Color(0xFF16A085), "Indie" to Color(0xFFD35400), "Classical" to Color(0xFF2471A3), "Pop" to Color(0xFFE74C3C), "Jazz" to Color(0xFF1E8449), "Electronic" to Color(0xFF1A252F))

@Composable
fun ExploreScreen(
    state: PlayerUiState,
    headerOffset: () -> Float = { 0f },
    headerAlpha: () -> Float = { 1f },
    onPlay: (Song, List<Song>) -> Unit,
    onSearch: (String) -> Unit
) {
    val isDark = isDarkTheme(state.themeMode)
    val textPrimary = if (isDark) Color.White else Color(0xFF141416)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF6B7280)
    val textMuted = if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF9CA3AF)
    val cardBg = if (isDark) Color.White.copy(alpha = 0.05f) else Color.White
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.06f)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
                .edgeFade(if (isDark) Color(0xFF141416) else Color(0xFFF6F7F9)),
            contentPadding = PaddingValues(top = 200.dp, bottom = 120.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    for (i in GENRES.indices step 2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            for (j in i until minOf(i + 2, GENRES.size)) {
                                val (name, color) = GENRES[j]
                                Box(modifier = Modifier.weight(1f).height(96.dp).clip(RoundedCornerShape(12.dp)).background(color).clickable { onSearch("$name music") }) {
                                    Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.1f), Color.Transparent))))
                                    Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(12.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
            if (state.recommendations.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Top Charts", color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(modifier = Modifier.padding(horizontal = 16.dp).background(cardBg, RoundedCornerShape(16.dp))) {
                        state.recommendations.filter { !it.isAlbum }.take(6).forEachIndexed { idx, song ->
                            Row(modifier = Modifier.fillMaxWidth().clickable { onPlay(song, state.recommendations) }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${idx + 1}", color = textMuted, fontSize = 13.sp, modifier = Modifier.width(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                AsyncImage(model = song.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) { Text(song.title, color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1); Text(song.artist, color = textSecondary, fontSize = 11.sp, maxLines = 1) }
                            }
                            if (idx < 5) HorizontalDivider(color = dividerColor)
                        }
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, (headerOffset() * 0.85f).roundToInt()) }
                .graphicsLayer { alpha = headerAlpha() }
                .padding(top = 110.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Text("Browse", color = textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Discover something new", color = textSecondary, fontSize = 13.sp)
        }
    }
}
