package com.beatles.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.beatles.player.data.Song
import com.beatles.player.ui.glassRefraction
import com.beatles.player.ui.motion.liquidPress
import com.beatles.player.ui.theme.isDarkTheme
import com.beatles.player.viewmodel.PlayerUiState
import com.beatles.player.viewmodel.PlayerViewModel

private val PRESET_COVERS = listOf(
    "https://c.saavncdn.com/artists/Karan_Aujla_004_20230809115159_500x500.jpg",
    "https://c.saavncdn.com/artists/Arijit_Singh_002_20230804114223_500x500.jpg",
    "https://c.saavncdn.com/artists/Diljit_Dosanjh_005_20231025073052_500x500.jpg",
    "https://c.saavncdn.com/artists/Raftaar_005_20220902120517_500x500.jpg",
    "https://c.saavncdn.com/artists/AR_Rahman_002_20201222074824_500x500.jpg",
    "https://c.saavncdn.com/artists/Shreya_Ghoshal_006_20200827112411_500x500.jpg"
)

/**
 * Minimal, clean, and easy Create New Album modal with Navbar Liquid Glass UI.
 */
@Composable
fun AddAlbumDialog(
    state: PlayerUiState,
    viewModel: PlayerViewModel,
    onClose: () -> Unit
) {
    val isDark = isDarkTheme(state.themeMode)
    val textPrimary = if (isDark) Color.White else Color(0xFF141416)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF6B7280)

    var albumTitle by remember { mutableStateOf("") }
    var artistName by remember { mutableStateOf(state.userName.ifBlank { "Beatles Artist" }) }
    var selectedPresetIndex by remember { mutableIntStateOf(0) }

    // Selected tracks from favorites/saved songs
    val selectedSongs = remember { mutableStateListOf<Song>() }

    val effectiveCover = PRESET_COVERS[selectedPresetIndex % PRESET_COVERS.size]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .shadow(10.dp, RoundedCornerShape(26.dp), spotColor = if (isDark) Color.Black.copy(alpha = 0.50f) else Color.Black.copy(alpha = 0.18f))
                .clip(RoundedCornerShape(26.dp))
                .background(if (isDark) Color(0xFF1E2024).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.95f))
                .glassRefraction(26.dp, isDark = isDark)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(22.dp)
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Create Album", color = textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Cover & Inputs Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tap cover to cycle preset image
                    val coverInteraction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .liquidPress(coverInteraction)
                            .shadow(3.dp, RoundedCornerShape(16.dp), spotColor = if (isDark) Color.Black.copy(alpha = 0.40f) else Color.Black.copy(alpha = 0.15f))
                            .clip(RoundedCornerShape(16.dp))
                            .glassRefraction(16.dp, isDark = isDark)
                            .clickable(interactionSource = coverInteraction, indication = null) {
                                selectedPresetIndex = (selectedPresetIndex + 1) % PRESET_COVERS.size
                            }
                    ) {
                        AsyncImage(
                            model = effectiveCover,
                            contentDescription = "Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .size(20.dp)
                                .background(Color.Black.copy(alpha = 0.65f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Change", tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Title Input
                        BasicTextField(
                            value = albumTitle,
                            onValueChange = { albumTitle = it },
                            textStyle = TextStyle(color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                            singleLine = true,
                            cursorBrush = SolidColor(if (isDark) Color.White else Color(0xFFF82C38)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(1.dp, RoundedCornerShape(12.dp), spotColor = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.08f))
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF26282E).copy(alpha = 0.70f) else Color(0xFFF4F5F7))
                                .glassRefraction(12.dp, isDark = isDark)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            decorationBox = { inner ->
                                if (albumTitle.isEmpty()) Text("Album Title...", color = textSecondary.copy(alpha = 0.6f), fontSize = 13.sp)
                                inner()
                            }
                        )

                        // Artist Input
                        BasicTextField(
                            value = artistName,
                            onValueChange = { artistName = it },
                            textStyle = TextStyle(color = textSecondary, fontSize = 12.sp),
                            singleLine = true,
                            cursorBrush = SolidColor(if (isDark) Color.White else Color(0xFFF82C38)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(1.dp, RoundedCornerShape(12.dp), spotColor = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.08f))
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF26282E).copy(alpha = 0.70f) else Color(0xFFF4F5F7))
                                .glassRefraction(12.dp, isDark = isDark)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            decorationBox = { inner ->
                                if (artistName.isEmpty()) Text("Artist Name...", color = textSecondary.copy(alpha = 0.6f), fontSize = 12.sp)
                                inner()
                            }
                        )
                    }
                }

                // Add from Saved Songs (1-Tap Selection Chips)
                if (state.favoriteSongs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text("Select Songs (${selectedSongs.size}):", color = textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(state.favoriteSongs, key = { it.id }) { song ->
                            val isAdded = selectedSongs.any { it.id == song.id }
                            val chipInteraction = remember { MutableInteractionSource() }
                            Row(
                                modifier = Modifier
                                    .liquidPress(chipInteraction)
                                    .shadow(1.dp, RoundedCornerShape(50), spotColor = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.08f))
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (isAdded) Color(0xFFF82C38).copy(alpha = 0.85f)
                                        else if (isDark) Color(0xFF26282E).copy(alpha = 0.70f)
                                        else Color(0xFFE2E2E2).copy(alpha = 0.60f)
                                    )
                                    .glassRefraction(50.dp, isDark = isDark)
                                    .clickable(interactionSource = chipInteraction, indication = null) {
                                        if (isAdded) selectedSongs.removeAll { it.id == song.id }
                                        else selectedSongs.add(song)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = song.coverUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(18.dp).clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    song.title,
                                    color = if (isAdded) Color.White else textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    if (isAdded) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = null,
                                    tint = if (isAdded) Color.White else textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Footer Buttons (Minimal Liquid Glass Navbar Styling)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val cancelInteraction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .liquidPress(cancelInteraction)
                            .shadow(2.dp, RoundedCornerShape(50), spotColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.10f))
                            .clip(RoundedCornerShape(50))
                            .background(if (isDark) Color(0xFF26282E).copy(alpha = 0.60f) else Color(0xFFE2E2E2).copy(alpha = 0.55f))
                            .glassRefraction(50.dp, isDark = isDark)
                            .clickable(interactionSource = cancelInteraction, indication = null) { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cancel", color = textSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    val canCreate = albumTitle.isNotBlank()
                    val createInteraction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .liquidPress(createInteraction)
                            .shadow(2.dp, RoundedCornerShape(50), spotColor = Color(0xFFF82C38).copy(alpha = 0.35f))
                            .clip(RoundedCornerShape(50))
                            .background(if (canCreate) Color(0xFFF82C38) else Color(0xFFF82C38).copy(alpha = 0.40f))
                            .glassRefraction(50.dp, isDark = isDark)
                            .clickable(enabled = canCreate, interactionSource = createInteraction, indication = null) {
                                val finalTitle = albumTitle.trim()
                                val finalArtist = artistName.ifBlank { "Beatles Artist" }
                                val songsWithMeta = selectedSongs.map {
                                    it.copy(artist = finalArtist, album = finalTitle, coverUrl = effectiveCover)
                                }
                                viewModel.createAlbum(
                                    title = finalTitle,
                                    artist = finalArtist,
                                    genre = "Music",
                                    coverUrl = effectiveCover,
                                    songs = songsWithMeta
                                )
                                onClose()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Create Album", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
