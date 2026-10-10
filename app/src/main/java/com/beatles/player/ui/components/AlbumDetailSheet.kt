package com.beatles.player.ui.components
import com.beatles.player.ui.theme.AppTheme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.beatles.player.data.Album
import com.beatles.player.data.Song
import com.beatles.player.viewmodel.PlayerUiState
import com.beatles.player.viewmodel.PlayerViewModel

private val AccentRed = Color(0xFFF82C38)
private val DarkBg = Color(0xFF0F1012)
private val CardBg = Color(0xFF1E2024).copy(alpha = 0.75f)

@Composable
fun AlbumDetailSheet(
    album: Album,
    state: PlayerUiState,
    viewModel: PlayerViewModel,
    onClose: () -> Unit,
    onPlay: (Song, List<Song>) -> Unit
) {
    var showSetDialog by remember { mutableStateOf(false) }
    var newSetName by remember { mutableStateOf("") }
    var isFavorite by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // Adaptive album tone derived from the album metadata to match album color overlay
    val albumOverlayHue = remember(album.id, album.artist) {
        (kotlin.math.abs(album.artist.hashCode() * 31 + album.title.hashCode()) % 360).toFloat()
    }
    val albumOverlayColor = remember(albumOverlayHue) {
        Color.hsl(albumOverlayHue, 0.65f, 0.72f)
    }
    val albumTitleBrush = remember(albumOverlayColor) {
        Brush.verticalGradient(
            listOf(
                Color.White,
                albumOverlayColor.copy(alpha = 0.90f)
            )
        )
    }

    // Ambient Video Banner Motion Animation (Edge-bleed scaling ensures corners are 100% covered)
    val infiniteTransition = rememberInfiniteTransition(label = "bannerVideoMotion")
    val bannerScale by infiniteTransition.animateFloat(
        initialValue = 1.18f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bannerScale"
    )
    val bannerShiftY by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bannerShiftY"
    )
    val glowHue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glowHue"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.bg)
    ) {
        // Deep Ambient Blurred Base
        AsyncImage(
            model = album.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(70.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (AppTheme.isDark) Color.Black.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.85f))
        )

        val listState = rememberLazyListState()
        val firstVisibleItem = listState.firstVisibleItemIndex
        val firstVisibleOffset = listState.firstVisibleItemScrollOffset

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // 1. Ambient Video Banner with Motion Reflections & Parallax Title
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(390.dp)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.0f to Color.Black,
                                    0.70f to Color.Black,
                                    1.0f to Color.Transparent
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                ) {
                    // Moving Video / Animated Poster Image
                    AsyncImage(
                        model = album.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = bannerScale
                                scaleY = bannerScale
                                translationY = bannerShiftY
                            }
                    )

                    // Ambient animated color reflection wash
                    val ambientGlow = Color.hsl(glowHue, 0.45f, 0.25f).copy(alpha = 0.35f)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        if (AppTheme.isDark) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.25f),
                                        Color.Transparent,
                                        ambientGlow,
                                        if (AppTheme.isDark) Color.Black.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.92f)
                                    )
                                )
                            )
                    )

                    // Hero Album Name and Singer Name with Parallax Translation
                    val parallaxY: Float = (firstVisibleOffset.toFloat() * 0.35f)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                            .graphicsLayer { translationY = parallaxY }
                    ) {
                        Text(
                            text = album.title,
                            color = AppTheme.fg(),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 36.sp,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${album.artist} • ${album.releaseYear}",
                            color = AppTheme.fg(0.70f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 2. Hero Action Controls Row (Info, Big Circular Play, Favorite Heart)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Info button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (AppTheme.isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.08f))
                            .clickable { showInfoDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = "Album Info", tint = AppTheme.fg(), modifier = Modifier.size(22.dp))
                    }

                    // Large circular Play button
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(AppTheme.fg())
                            .clickable {
                                if (album.songs.isNotEmpty()) {
                                    onPlay(album.songs.first(), album.songs)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play Album", tint = AppTheme.bg, modifier = Modifier.size(36.dp))
                    }

                    // Favorite Heart button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (AppTheme.isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.08f))
                            .clickable { isFavorite = !isFavorite },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite Album",
                            tint = if (isFavorite) AccentRed else AppTheme.fg(),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 3. Transparent Frosted Glass Metadata Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(AppTheme.card)
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Thumbnail
                        AsyncImage(
                            model = album.coverUrl,
                            contentDescription = album.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        // Metadata Texts
                        Column(modifier = Modifier.weight(1f)) {
                            Text(album.releaseDate, color = AppTheme.fg(0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(album.title, color = AppTheme.fg(), fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("• ${album.type}   ${album.songs.size} songs", color = AppTheme.fg(0.7f), fontSize = 12.sp)
                        }
                        // Plus (+) button to Save Album or Add to Album Set
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(AppTheme.fg(0.18f))
                                .clickable { showSetDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Save Album or Add to Set", tint = AppTheme.fg(), modifier = Modifier.size(22.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))
            }

            // 4. "Top Songs >" Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Top Songs",
                        color = AppTheme.fg(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = AppTheme.fg(0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 5. Tracklist (Directly playable from album page)
            if (album.songs.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No songs available in this album", color = AppTheme.fg(0.5f), fontSize = 14.sp)
                    }
                }
            } else {
                itemsIndexed(album.songs) { idx, track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlay(track, album.songs) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = track.coverUrl.ifBlank { album.coverUrl },
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(track.title, color = AppTheme.fg(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "${album.artist} • ${album.title} • ${album.releaseYear}",
                                color = AppTheme.fg(0.5f),
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Pinned Top Navigation Bar (Fixed with uniform liquid glass blur, stays visible during scroll)
        val dockedAlpha = if (firstVisibleItem > 0) 1f else (firstVisibleOffset / 500f).coerceIn(0f, 1f)
        val dockedOffset by animateFloatAsState(targetValue = if (dockedAlpha > 0.1f) 0f else 20f, label = "dockedOffset")

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Liquid Glass Back button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(AppTheme.card)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AppTheme.fg(),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Docked Album Title + Artist (Fades in smoothly as user scrolls past hero banner)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .graphicsLayer {
                        alpha = dockedAlpha
                        translationY = dockedOffset
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    album.title,
                    color = AppTheme.fg(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    album.artist,
                    color = AppTheme.fg(0.65f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }

            // Liquid Glass Info button (replaces Share & 3-dots, always pinned)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(AppTheme.card)
                    .clickable { showInfoDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = "Album Info",
                    tint = AppTheme.fg(),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Save Album or Add to Album Set Dialog
        if (showSetDialog) {
            AlertDialog(
                onDismissRequest = { showSetDialog = false },
                containerColor = AppTheme.sheet,
                title = {
                    Text("Album Options", color = AppTheme.fg(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Option 1: Save directly to Library
                        val dialogBtnContainer = if (AppTheme.isDark) Color.White else Color(0xFF141416)
                        val dialogBtnContent = if (AppTheme.isDark) Color.Black else Color.White
                        Button(
                            onClick = {
                                viewModel.saveAlbumToLibrary(album)
                                showSetDialog = false
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = dialogBtnContainer, contentColor = dialogBtnContent)
                        ) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = dialogBtnContent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Album to Library", color = dialogBtnContent, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = AppTheme.fg(0.1f))
                        Spacer(modifier = Modifier.height(14.dp))

                        Text("Add to Album Set", color = AppTheme.fg(), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Group multiple albums together into a collection.", color = AppTheme.fg(0.5f), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Existing Album Sets
                        if (state.albumSets.isNotEmpty()) {
                            state.albumSets.forEach { set ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.addAlbumToSet(set.id, album)
                                            showSetDialog = false
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = AppTheme.fg(0.7f), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(set.name, color = AppTheme.fg(), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Text("${set.albums.size} albums", color = AppTheme.fg(0.45f), fontSize = 10.sp)
                                    }
                                    Icon(Icons.Default.Add, contentDescription = "Add", tint = AppTheme.fg(0.6f), modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Create New Album Set input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = newSetName,
                                onValueChange = { newSetName = it },
                                textStyle = TextStyle(color = AppTheme.fg(), fontSize = 13.sp),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .background(AppTheme.fg(0.08f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                decorationBox = { inner ->
                                    if (newSetName.isEmpty()) {
                                        Text("New set name...", color = AppTheme.fg(0.35f), fontSize = 13.sp)
                                    }
                                    inner()
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newSetName.isNotBlank()) {
                                        viewModel.createAlbumSet(newSetName.trim(), album)
                                        showSetDialog = false
                                    }
                                },
                                enabled = newSetName.isNotBlank(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = dialogBtnContainer, disabledContainerColor = AppTheme.fg(0.15f))
                            ) {
                                Text("Create", color = if (newSetName.isNotBlank()) dialogBtnContent else AppTheme.fg(0.4f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSetDialog = false }) {
                        Text("Close", color = AppTheme.fg(0.6f))
                    }
                }
            )
        }

        // Info Dialog
        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                containerColor = AppTheme.sheet,
                title = { Text(album.title, color = AppTheme.fg(), fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Artist: ${album.artist}", color = AppTheme.fg(0.8f), fontSize = 14.sp)
                        Text("Genre: ${album.genre}", color = AppTheme.fg(0.8f), fontSize = 14.sp)
                        Text("Released: ${album.releaseDate}", color = AppTheme.fg(0.8f), fontSize = 14.sp)
                        Text("Tracks: ${album.songs.size}", color = AppTheme.fg(0.8f), fontSize = 14.sp)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showInfoDialog = false }) {
                        Text("OK", color = AppTheme.fg())
                    }
                }
            )
        }
    }
}
