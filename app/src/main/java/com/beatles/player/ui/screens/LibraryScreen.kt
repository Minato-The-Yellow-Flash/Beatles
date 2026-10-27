package com.beatles.player.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.zIndex
import com.beatles.player.ui.glassRefraction
import com.beatles.player.ui.motion.liquidPress
import dev.chrisbanes.haze.HazeStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.beatles.player.data.Album
import com.beatles.player.data.Song
import com.beatles.player.ui.theme.isDarkTheme
import com.beatles.player.ui.theme.LocalDynamicBottomPadding
import com.beatles.player.viewmodel.PlayerUiState
import com.beatles.player.ui.components.edgeFade
import com.beatles.player.viewmodel.PlayerViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import kotlin.math.roundToInt

private val AccentRed = Color(0xFFF82C38)

@Composable
fun LibraryScreen(
    state: PlayerUiState,
    viewModel: PlayerViewModel,
    headerOffset: () -> Float = { 0f },
    headerAlpha: () -> Float = { 1f },
    hazeState: HazeState? = null,
    onPlay: (Song, List<Song>, androidx.compose.ui.geometry.Rect?) -> Unit
) {
    var section by remember { mutableStateOf("liked songs") }

    val isDark = isDarkTheme(state.themeMode)
    val textPrimary = if (isDark) Color.White else Color(0xFF141416)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF6B7280)
    val textMuted = if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF9CA3AF)
    val cardBg = if (isDark) Color(0xFFE2E2E2).copy(alpha = 0.12f) else Color.White
    val tabPillBg = if (isDark) Color(0xFFE2E2E2).copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f)
    val tabIndicatorBg = if (isDark) Color.White.copy(alpha = 0.16f) else Color.White

    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .edgeFade(if (isDark) Color(0xFF141416) else Color(0xFFF6F7F9)),
            contentPadding = PaddingValues(top = 230.dp, bottom = LocalDynamicBottomPadding.current)
        ) {
            when (section) {
                "albums" -> {
                    // Album section: grid/cards of user & default albums
                    item {
                        val createAlbumInteraction = remember { MutableInteractionSource() }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .liquidPress(createAlbumInteraction)
                                .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.10f))
                                .clip(RoundedCornerShape(16.dp))
                                .background(cardBg)
                                .glassRefraction(16.dp, isDark = isDark)
                                .clickable(interactionSource = createAlbumInteraction, indication = null) { viewModel.setShowAddAlbumDialog(true) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(AccentRed.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = AccentRed, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Create New Album", color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text("Curate custom tracks and artwork into an album", color = textSecondary, fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // User-Created & Curated Album Sets
                    if (state.albumSets.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Album Sets (${state.albumSets.size})",
                                color = textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.albumSets) { set ->
                                    Box(
                                        modifier = Modifier
                                            .width(200.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(cardBg)
                                            .clickable {
                                                if (set.albums.isNotEmpty()) {
                                                    viewModel.setSelectedAlbum(set.albums.first())
                                                }
                                            }
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                val covers = set.albums.map { it.coverUrl }.filter { it.isNotBlank() }.take(2)
                                                if (covers.isEmpty()) {
                                                    Box(
                                                        modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)).background(if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = textMuted, modifier = Modifier.size(24.dp))
                                                    }
                                                } else {
                                                    covers.forEach { cover ->
                                                        AsyncImage(
                                                            model = cover,
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp))
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(set.name, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                                    Text("${set.albums.size} albums", color = textSecondary, fontSize = 11.sp)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(AccentRed)
                                                        .clickable { viewModel.playAlbumSet(set) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play Set", tint = Color.White, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "All Albums (${state.albums.size})",
                                color = textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }

                    if (state.albums.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Album, contentDescription = null, tint = textMuted, modifier = Modifier.size(32.dp))
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No albums yet", color = textSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Text("Tap '+ Create New Album' to build your first release.", color = textMuted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    } else {
                        // 2 items per row
                        val chunkedAlbums = state.albums.chunked(2)
                        items(chunkedAlbums) { row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for (album in row) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(cardBg)
                                            .clickable { viewModel.setSelectedAlbum(album) }
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            AsyncImage(
                                                model = album.coverUrl,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(album.title, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                            Text(album.artist, color = textSecondary, fontSize = 12.sp, maxLines = 1)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("${album.songs.size} tracks", color = textMuted, fontSize = 10.sp)
                                        }
                                    }
                                }
                                if (row.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                "liked songs" -> {
                    val songs = state.favoriteSongs
                    if (songs.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, tint = textMuted, modifier = Modifier.size(32.dp))
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No liked songs yet", color = textSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Text("Tap the heart or double-tap anywhere on the media player to save songs here.", color = textMuted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp, start = 20.dp, end = 20.dp))
                            }
                        }
                    } else {
                        items(songs) { song ->
                            var rowBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPlay(song, songs, rowBounds) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = song.coverUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .onGloballyPositioned { rowBounds = it.boundsInRoot() }
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(song.title, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                    Text(song.artist, color = textSecondary, fontSize = 12.sp, maxLines = 1)
                                }
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = textMuted)
                            }
                        }
                    }
                }

                "artists" -> {
                    val songs = state.favoriteSongs
                    val artists = songs.distinctBy { it.artist }
                    if (artists.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("No artists followed yet", color = textSecondary, fontSize = 15.sp)
                            }
                        }
                    } else {
                        items(artists) { song ->
                            var rowBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.openArtist(song.artist, rowBounds) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = song.coverUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .onGloballyPositioned { rowBounds = it.boundsInRoot() }
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(song.artist, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text("Artist", color = textSecondary, fontSize = 12.sp)
                                }
                                val mixInteraction = remember { MutableInteractionSource() }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .liquidPress(mixInteraction)
                                        .shadow(2.dp, CircleShape, spotColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.12f))
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFF383842).copy(alpha = 0.35f) else Color(0xFFE2E2E2).copy(alpha = 0.55f))
                                        .glassRefraction(18.dp, isDark = isDark, isCircle = true)
                                        .clickable(interactionSource = mixInteraction, indication = null) { viewModel.playArtistMix(song.artist) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Radio, contentDescription = "Play Artist Mix", tint = AccentRed, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = textMuted)
                            }
                        }
                    }
                }

                "offline" -> {
                    val downloads = state.downloadedSongs
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (downloads.isNotEmpty()) "${downloads.size} Offline Songs (HQ)" else "Offline Songs",
                                color = textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (downloads.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF27AE60).copy(alpha = 0.15f), RoundedCornerShape(50))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text("Ready Offline", color = Color(0xFF2ECC71), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (downloads.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 40.dp, start = 24.dp, end = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = textMuted,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No offline songs downloaded yet", color = textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Download songs by tapping the 3 dots (•••) on any track in the Now Playing player to stream them offline without an internet connection.",
                                    color = textSecondary,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(downloads) { song ->
                            var rowBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPlay(song, downloads, rowBounds) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .onGloballyPositioned { rowBounds = it.boundsInRoot() }
                                ) {
                                    AsyncImage(
                                        model = song.coverUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(18.dp)
                                            .background(Color(0xFF27AE60), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.DownloadDone, contentDescription = "Offline", tint = Color.White, modifier = Modifier.size(11.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(song.title, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                    Text("${song.artist} • Offline HQ", color = textSecondary, fontSize = 12.sp, maxLines = 1)
                                }
                                IconButton(onClick = { viewModel.removeDownloadedSong(song.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = textMuted, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Fixed Top Sticky Header with Subtle Layer Blur (no hard blur, no edges) & Glass Segmented Control
        val density = LocalDensity.current
        val sections = listOf("liked songs", "albums", "artists", "offline")
        val currentIdx = sections.indexOf(section).coerceAtLeast(0)
        val indicatorFraction by animateFloatAsState(
            targetValue = currentIdx.toFloat(),
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
            label = "librarySegmentIndicator"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, (headerOffset() * 0.85f).roundToInt()) }
                .graphicsLayer { alpha = headerAlpha() }
                .padding(bottom = 12.dp)
        ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 110.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Library", color = textPrimary, fontSize = 31.sp, fontWeight = FontWeight.Black)
                    val addInteraction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .liquidPress(addInteraction)
                            .shadow(3.dp, CircleShape, spotColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.15f))
                            .clip(CircleShape)
                            .background(AccentRed)
                            .glassRefraction(17.dp, isDark = isDark, isCircle = true)
                            .clickable(interactionSource = addInteraction, indication = null) { viewModel.setShowAddAlbumDialog(true) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Album", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                // Nav bar liquid glass segmented tab pill with smooth sliding indicator
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(44.dp)
                        .shadow(3.dp, RoundedCornerShape(50), spotColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.12f))
                        .clip(RoundedCornerShape(50))
                        .background(if (isDark) Color(0xFF181A22).copy(alpha = 0.44f) else Color(0xFFFFFFFF).copy(alpha = 0.60f))
                        .glassRefraction(50.dp, isDark = isDark)
                        .padding(3.dp)
                ) {
                    val tabWidth = constraints.maxWidth / sections.size.toFloat()
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(with(density) { tabWidth.toDp() })
                            .offset { IntOffset((indicatorFraction * tabWidth).roundToInt(), 0) }
                            .shadow(2.dp, RoundedCornerShape(50), spotColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.12f))
                            .clip(RoundedCornerShape(50))
                            .background(if (isDark) Color.White.copy(alpha = 0.20f) else Color(0xFFE2E2E2))
                            .glassRefraction(50.dp, isDark = isDark)
                    )
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        sections.forEach { s ->
                            val isActive = section == s
                            val label = if (s == "liked songs") "Liked" else s.replaceFirstChar { it.uppercaseChar() }
                            val tabInteraction = remember { MutableInteractionSource() }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .liquidPress(tabInteraction)
                                    .clickable(
                                        interactionSource = tabInteraction,
                                        indication = null
                                    ) { section = s },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    color = if (isActive) (if (isDark) Color.White else Color(0xFF141416)) else (if (isDark) Color.White.copy(alpha = 0.60f) else Color(0xFF141416).copy(alpha = 0.60f)),
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
