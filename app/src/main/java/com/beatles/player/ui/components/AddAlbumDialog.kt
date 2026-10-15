package com.beatles.player.ui.components
import com.beatles.player.ui.theme.AppTheme
import com.beatles.player.ui.theme.isDarkTheme
import com.beatles.player.ui.components.FrostedButton

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.beatles.player.data.Song
import com.beatles.player.viewmodel.PlayerUiState
import com.beatles.player.viewmodel.PlayerViewModel

private val DarkModalBg = Color(0xFF1E1E22)
private val PRESET_COVERS = listOf(
    "https://c.saavncdn.com/artists/Karan_Aujla_004_20230809115159_500x500.jpg",
    "https://c.saavncdn.com/artists/Arijit_Singh_002_20230804114223_500x500.jpg",
    "https://c.saavncdn.com/artists/Diljit_Dosanjh_005_20231025073052_500x500.jpg",
    "https://c.saavncdn.com/artists/Raftaar_005_20220902120517_500x500.jpg",
    "https://c.saavncdn.com/artists/AR_Rahman_002_20201222074824_500x500.jpg",
    "https://c.saavncdn.com/artists/Shreya_Ghoshal_006_20200827112411_500x500.jpg"
)

@Composable
fun AddAlbumDialog(
    state: PlayerUiState,
    viewModel: PlayerViewModel,
    onClose: () -> Unit
) {
    var albumTitle by remember { mutableStateOf("") }
    var artistName by remember { mutableStateOf(state.userName.ifBlank { "Beatles Artist" }) }
    var selectedPresetCover by remember { mutableStateOf<String?>(null) }
    
    // Tracks to add — starts clean (no dummy intro track)
    val trackList = remember { mutableStateListOf<Song>() }

    var newTrackTitle by remember { mutableStateOf("") }
    var newTrackDuration by remember { mutableStateOf("3:15") }

    // Dynamic cover: first track's cover, or selected preset, or fallback
    val effectiveCover = selectedPresetCover
        ?: trackList.firstOrNull()?.coverUrl
        ?: PRESET_COVERS.first()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(AppTheme.sheet)
                .border(1.dp, AppTheme.fg(0.1f), RoundedCornerShape(24.dp))
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(AppTheme.fg(0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = AppTheme.fg(), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Create New Album", color = AppTheme.fg(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = AppTheme.fg(0.6f))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Dynamic Album Cover preview and presets
                item {
                    Text("ALBUM COVER", color = AppTheme.fg(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = effectiveCover,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(74.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, AppTheme.fg(0.15f), RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Artwork (Auto-syncs with added songs):", color = AppTheme.fg(0.65f), fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PRESET_COVERS.forEach { url ->
                                    val isSelected = selectedPresetCover == url
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) Color.White else AppTheme.fg(0.15f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedPresetCover = url }
                                    ) {
                                        AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    }
                                }
                            }
                        }
                    }
                }

                // Title & Artist Inputs (Genre removed as requested)
                item {
                    Text("METADATA", color = AppTheme.fg(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    BasicTextField(
                        value = albumTitle,
                        onValueChange = { albumTitle = it },
                        textStyle = TextStyle(color = AppTheme.fg(), fontSize = 14.sp),
                        singleLine = true,
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppTheme.fg(0.07f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        decorationBox = { inner ->
                            if (albumTitle.isEmpty()) Text("Album Title (e.g. Midnight Beats)", color = AppTheme.fg(0.35f), fontSize = 13.sp)
                            inner()
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    BasicTextField(
                        value = artistName,
                        onValueChange = { artistName = it },
                        textStyle = TextStyle(color = AppTheme.fg(), fontSize = 14.sp),
                        singleLine = true,
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppTheme.fg(0.07f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        decorationBox = { inner ->
                            if (artistName.isEmpty()) Text("Artist Name", color = AppTheme.fg(0.35f), fontSize = 13.sp)
                            inner()
                        }
                    )
                }

                // Option to add from Saved Songs (Favorites)
                if (state.favoriteSongs.isNotEmpty()) {
                    item {
                        Text("ADD FROM SAVED SONGS", color = AppTheme.fg(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(state.favoriteSongs) { song ->
                                val isAdded = trackList.any { it.id == song.id }
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(if (isAdded) AppTheme.fg(0.2f) else AppTheme.fg(0.07f))
                                        .clickable {
                                            if (!isAdded) {
                                                trackList.add(song)
                                                if (selectedPresetCover == null) {
                                                    selectedPresetCover = song.coverUrl
                                                }
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = song.coverUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(20.dp).clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        song.title,
                                        color = if (isAdded) Color.White else AppTheme.fg(0.8f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        if (isAdded) Icons.Default.Check else Icons.Default.Add,
                                        contentDescription = null,
                                        tint = if (isAdded) Color.Green else AppTheme.fg(0.5f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Manual Track Add Section
                item {
                    Text("ADD CUSTOM TRACK", color = AppTheme.fg(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppTheme.fg(0.05f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BasicTextField(
                                value = newTrackTitle,
                                onValueChange = { newTrackTitle = it },
                                textStyle = TextStyle(color = AppTheme.fg(), fontSize = 13.sp),
                                singleLine = true,
                                cursorBrush = SolidColor(Color.White),
                                modifier = Modifier
                                    .weight(1f)
                                    .background(AppTheme.fg(0.08f), RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                decorationBox = { inner ->
                                    if (newTrackTitle.isEmpty()) Text("Track Title", color = AppTheme.fg(0.35f), fontSize = 12.sp)
                                    inner()
                                }
                            )

                            BasicTextField(
                                value = newTrackDuration,
                                onValueChange = { newTrackDuration = it },
                                textStyle = TextStyle(color = AppTheme.fg(), fontSize = 13.sp),
                                singleLine = true,
                                cursorBrush = SolidColor(Color.White),
                                modifier = Modifier
                                    .width(70.dp)
                                    .background(AppTheme.fg(0.08f), RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                decorationBox = { inner ->
                                    if (newTrackDuration.isEmpty()) Text("3:00", color = AppTheme.fg(0.35f), fontSize = 12.sp)
                                    inner()
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (newTrackTitle.isNotBlank()) {
                                    val parts = newTrackDuration.split(":")
                                    val secs = if (parts.size == 2) {
                                        (parts[0].toIntOrNull() ?: 3) * 60 + (parts[1].toIntOrNull() ?: 0)
                                    } else 180

                                    trackList.add(
                                        Song(
                                            id = "trk_${System.currentTimeMillis()}_${trackList.size + 1}",
                                            title = newTrackTitle.trim(),
                                            artist = artistName.ifBlank { "Beatles Artist" },
                                            album = albumTitle.ifBlank { "New Album" },
                                            coverUrl = effectiveCover,
                                            durationSeconds = secs
                                        )
                                    )
                                    newTrackTitle = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.fg(0.12f))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = AppTheme.fg(), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Track", color = AppTheme.fg(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // List of added tracks
                item {
                    Text("ALBUM SONGS (${trackList.size})", color = AppTheme.fg(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                if (trackList.isEmpty()) {
                    item {
                        Text(
                            "No songs added yet. Add from saved songs above or type a custom track.",
                            color = AppTheme.fg(0.35f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                itemsIndexed(trackList) { index, track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppTheme.fg(0.04f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${index + 1}", color = AppTheme.fg(0.4f), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(track.title, color = AppTheme.fg(), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("${track.durationSeconds / 60}:${String.format("%02d", track.durationSeconds % 60)}", color = AppTheme.fg(0.4f), fontSize = 11.sp)
                        }
                        IconButton(
                            onClick = { trackList.removeAt(index) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = AppTheme.fg(0.5f), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Footer Actions (iOS native style: cancel in translucent gray, create in clean solid white)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onClose,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.fg(0.08f))
                ) {
                    Text("Cancel", color = AppTheme.fg(), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }

                FrostedButton(
                    onClick = {
                        val finalTitle = albumTitle.ifBlank { "Untitled Project" }
                        val finalArtist = artistName.ifBlank { "Beatles Artist" }
                        val songsWithMeta = trackList.map {
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
                    active = true,
                    isDark = isDarkTheme(state.themeMode),
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Create Album", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
