package com.beatles.player.ui.screens
import com.beatles.player.ui.theme.AppTheme
import com.beatles.player.ui.theme.LocalDynamicBottomPadding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.beatles.player.data.Album
import com.beatles.player.data.ArtistTab
import com.beatles.player.data.Song
import com.beatles.player.viewmodel.PlayerUiState
import com.beatles.player.viewmodel.PlayerViewModel

private val ArtistPurple = Color(0xFF7B2FBE)
private val DarkCardBg = Color.White.copy(alpha = 0.06f)

@Composable
fun ArtistScreen(
    state: PlayerUiState,
    viewModel: PlayerViewModel,
    onPlay: (Song, List<Song>, androidx.compose.ui.geometry.Rect?) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (state.artistTab) {
            ArtistTab.FEED -> ArtistFeedTab(state = state, viewModel = viewModel, onPlay = onPlay)
            ArtistTab.UPLOAD -> ArtistUploadTab(state = state, viewModel = viewModel, onPlay = { s, pl -> onPlay(s, pl, null) })
            ArtistTab.ANALYTICS -> ArtistAnalyticsTab(state = state)
            ArtistTab.MESSAGES -> ArtistMessagesTab(state = state, viewModel = viewModel)
        }
    }
}

@Composable
fun ArtistFeedTab(
    state: PlayerUiState,
    viewModel: PlayerViewModel,
    onPlay: (Song, List<Song>, androidx.compose.ui.geometry.Rect?) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 110.dp, bottom = LocalDynamicBottomPadding.current)
    ) {
        // Hero Artist Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppTheme.card)
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(ArtistPurple.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = ArtistPurple, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Artist Hub", color = ArtistPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(state.userName.ifBlank { "Verified Creator" }, color = AppTheme.fg(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("32.8K Monthly Listeners · 4 Albums", color = AppTheme.fg(0.5f), fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Stats row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArtistStatBox("124.5K", "Total Streams", "+18% this month", Modifier.weight(1f))
                    ArtistStatBox("₹48,200", "Est. Royalties", "Next payout 5th", Modifier.weight(1f))
                }
            }
        }

        // Action Quick Access
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Your Discography", color = AppTheme.fg(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "+ Add Album",
                    color = ArtistPurple,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { viewModel.setShowAddAlbumDialog(true) }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Albums list
        items(state.albums) { album ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setSelectedAlbum(album) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(DarkCardBg, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = album.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(54.dp).clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(album.title, color = AppTheme.fg(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("${album.songs.size} songs · ${album.genre} · ${album.releaseYear}", color = AppTheme.fg(0.5f), fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier
                        .background(ArtistPurple.copy(alpha = 0.15f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("MANAGE", color = ArtistPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Top Performing Tracks
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Top Streaming Tracks", color = AppTheme.fg(), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(8.dp))
        }

        val allSongs = state.albums.flatMap { it.songs }.ifEmpty { state.recommendations }
        items(allSongs.take(5)) { song ->
            var rowBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (AppTheme.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                    .border(
                        0.5.dp,
                        if (AppTheme.isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { onPlay(song, allSongs, rowBounds) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    Text(song.title, color = AppTheme.fg(), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(song.artist, color = AppTheme.fg(0.5f), fontSize = 12.sp)
                }
                Text("34.2K plays", color = AppTheme.fg(0.4f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun ArtistUploadTab(
    state: PlayerUiState,
    viewModel: PlayerViewModel,
    onPlay: (Song, List<Song>) -> Unit
) {
    var trackTitle by remember { mutableStateOf("") }
    var trackAlbum by remember { mutableStateOf("") }
    var trackGenre by remember { mutableStateOf("Pop") }
    var trackSubmitted by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 110.dp, bottom = LocalDynamicBottomPadding.current)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Upload & Releases", color = AppTheme.fg(), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("Add singles or bundle tracks into albums", color = AppTheme.fg(0.5f), fontSize = 13.sp)
                    }
                    Button(
                        onClick = { viewModel.setShowAddAlbumDialog(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = ArtistPurple),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.LibraryMusic, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Add Album", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Upload Single Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(AppTheme.card)
                        .padding(16.dp)
                ) {
                    Text("QUICK SINGLE RELEASE", color = AppTheme.fg(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    BasicTextField(
                        value = trackTitle,
                        onValueChange = { trackTitle = it },
                        textStyle = TextStyle(color = AppTheme.fg(), fontSize = 14.sp),
                        singleLine = true,
                        cursorBrush = SolidColor(ArtistPurple),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppTheme.fg(0.08f), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        decorationBox = { inner ->
                            if (trackTitle.isEmpty()) Text("Track Title *", color = AppTheme.fg(0.35f), fontSize = 14.sp)
                            inner()
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    BasicTextField(
                        value = trackAlbum,
                        onValueChange = { trackAlbum = it },
                        textStyle = TextStyle(color = AppTheme.fg(), fontSize = 14.sp),
                        singleLine = true,
                        cursorBrush = SolidColor(ArtistPurple),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppTheme.fg(0.08f), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        decorationBox = { inner ->
                            if (trackAlbum.isEmpty()) Text("Album / Project Name *", color = AppTheme.fg(0.35f), fontSize = 14.sp)
                            inner()
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Audio file drag mock area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppTheme.fg(0.04f))
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = ArtistPurple, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Audio file attached: demo_master.wav", color = AppTheme.fg(0.7f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("FLAC / WAV / 320kbps MP3 (Ready to upload)", color = AppTheme.fg(0.4f), fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (trackTitle.isNotBlank()) {
                                viewModel.submitArtistTrack(trackTitle, trackAlbum, trackGenre)
                                trackTitle = ""
                                trackAlbum = ""
                                trackSubmitted = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = ArtistPurple)
                    ) {
                        Text("Submit Track for Review", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    if (trackSubmitted) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("✓ Track submitted to Admin queue for approval!", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Your Submissions & Status", color = AppTheme.fg(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        items(state.trackSubmissions) { sub ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .background(DarkCardBg, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(ArtistPurple.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = ArtistPurple, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(sub.title, color = AppTheme.fg(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("${sub.album} · ${sub.genre} · Submitted ${sub.submittedDate}", color = AppTheme.fg(0.45f), fontSize = 11.sp)
                }
                val statusColor = when (sub.status) {
                    "approved" -> Color(0xFF4CAF50)
                    "rejected" -> Color(0xFFE53935)
                    else -> Color(0xFFFFB300)
                }
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(sub.status.uppercase(), color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ArtistAnalyticsTab(state: PlayerUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 110.dp, bottom = LocalDynamicBottomPadding.current)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Performance & Stats", color = AppTheme.fg(), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Real-time listener analytics and stream metrics", color = AppTheme.fg(0.5f), fontSize = 13.sp)

                Spacer(modifier = Modifier.height(18.dp))

                // KPI Grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArtistStatBox("84.2K", "Total Plays", "+14.8% vs last week", Modifier.weight(1f))
                    ArtistStatBox("19.4K", "Unique Listeners", "+6.2% new fans", Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArtistStatBox("4:12", "Avg. Listen Time", "High completion rate", Modifier.weight(1f))
                    ArtistStatBox("1,420", "Playlist Saves", "Added to 318 public lists", Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Audience Geography Breakdown
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCardBg, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Text("Top Streaming Regions", color = AppTheme.fg(), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    RegionBar("Delhi NCR, India", 0.85f, "42.5K plays")
                    RegionBar("Punjab & Chandigarh", 0.72f, "31.2K plays")
                    RegionBar("Mumbai & Maharashtra", 0.58f, "24.1K plays")
                    RegionBar("Toronto & Vancouver, Canada", 0.40f, "18.3K plays")
                    RegionBar("London & Midlands, UK", 0.28f, "11.9K plays")
                }
            }
        }
    }
}

@Composable
fun ArtistMessagesTab(
    state: PlayerUiState,
    viewModel: PlayerViewModel
) {
    var replyText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 110.dp, bottom = LocalDynamicBottomPadding.current)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Fan Messages & Feedback", color = AppTheme.fg(), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Direct interactions from your active listeners", color = AppTheme.fg(0.5f), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Messages list
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.fanMessages) { msg ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCardBg, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(ArtistPurple.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(msg.fromUser.first().toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(msg.fromUser, color = AppTheme.fg(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(msg.timeAgo, color = AppTheme.fg(0.4f), fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(msg.text, color = AppTheme.fg(0.85f), fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
        }

        // Reply input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = replyText,
                onValueChange = { replyText = it },
                textStyle = TextStyle(color = AppTheme.fg(), fontSize = 14.sp),
                singleLine = true,
                cursorBrush = SolidColor(ArtistPurple),
                modifier = Modifier
                    .weight(1f)
                    .background(AppTheme.fg(0.08f), RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                decorationBox = { inner ->
                    if (replyText.isEmpty()) Text("Reply to community as ${state.userName}...", color = AppTheme.fg(0.35f), fontSize = 13.sp)
                    inner()
                }
            )

            Spacer(modifier = Modifier.width(10.dp))

            IconButton(
                onClick = {
                    if (replyText.isNotBlank()) {
                        viewModel.sendFanMessage(state.userName, replyText.trim())
                        replyText = ""
                    }
                },
                modifier = Modifier
                    .size(46.dp)
                    .background(ArtistPurple, CircleShape)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ArtistStatBox(value: String, label: String, trend: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(DarkCardBg, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Text(label, color = AppTheme.fg(0.5f), fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = AppTheme.fg(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(trend, color = ArtistPurple, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RegionBar(name: String, progress: Float, stats: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, color = AppTheme.fg(0.85f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(stats, color = AppTheme.fg(0.5f), fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
            color = ArtistPurple,
            trackColor = AppTheme.fg(0.1f)
        )
    }
}
