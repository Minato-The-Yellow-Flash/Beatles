package com.beatles.player.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import com.beatles.player.ui.theme.AppTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.shadow
import android.os.SystemClock
import kotlinx.coroutines.isActive
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import com.beatles.player.ui.components.Haptics
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.beatles.player.data.LyricLine
import com.beatles.player.data.Playlist
import com.beatles.player.data.Song
import com.beatles.player.data.SyncedWord
import com.beatles.player.data.SyncedLyricLine
import com.beatles.player.data.LyricsProcessor
import kotlinx.coroutines.delay

private val AccentRed = Color(0xFFF82C38)
private val DarkBg = Color(0xFF0E0E10)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NowPlayingSheet(
    song: Song,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    isFavorite: Boolean,
    lyrics: List<LyricLine>,
    playlist: List<Song>,
    showLyrics: Boolean,
    showNextUp: Boolean,
    playlists: List<Playlist> = emptyList(),
    isLoadingStream: Boolean = false,
    autoMixActive: Boolean = false,
    autoMixProgress: Float = 0f,
    autoMixLabel: String? = null,
    onClose: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleLyrics: () -> Unit,
    onToggleNextUp: () -> Unit,
    onPlayFromQueue: (Song) -> Unit,
    autoPlayEnabled: Boolean = true,
    onToggleAutoPlay: () -> Unit = {},
    autoMixMode: com.beatles.player.player.automix.AutoMixMode = com.beatles.player.player.automix.AutoMixMode.NEW,
    autoMixEnabled: Boolean = true,
    onToggleAutoMix: () -> Unit = {},
    onGoToAlbum: (Song) -> Unit = {},
    onMoveToPlayNext: (Song) -> Unit = {},
    onRemoveFromQueue: (String) -> Unit = {},
    onMoveInQueue: (Int, Int) -> Unit = { _, _ -> },
    onDownloadSong: (Song) -> Unit = {},
    downloadedSongs: List<Song> = emptyList(),
    downloadingSongId: String? = null,
    downloadProgress: Float = 0f,
    downloadFailedSongId: String? = null,
    onCancelDownload: (String) -> Unit = {},
    onRemoveDownload: (String) -> Unit = {},
    onCreatePlaylist: (String, Song) -> Unit = { _, _ -> },
    onAddToPlaylist: (String, Song) -> Unit = { _, _ -> },
    morphProgress: Float = 1f,
    sourceOriginBounds: Rect? = null
) {
    var isHeartAnimating by remember { mutableStateOf(false) }
    val heartScale by animateFloatAsState(targetValue = if (isHeartAnimating) 1.5f else 1f, animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f), label = "heartScale")
    val heartColor by animateColorAsState(targetValue = if (isFavorite) Color(0xFFF82C38) else Color.White.copy(alpha = 0.6f), animationSpec = tween(300), label = "heartColor")

    var showBigHeartPopup by remember { mutableStateOf(false) }
    val bigHeartScale by animateFloatAsState(
        targetValue = if (showBigHeartPopup) 1.25f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "bigHeartScale"
    )
    val bigHeartAlpha by animateFloatAsState(
        targetValue = if (showBigHeartPopup) 1f else 0f,
        animationSpec = tween(250),
        label = "bigHeartAlpha"
    )

    val sheetHaze = remember { HazeState() }
    var showMiniMenu by remember { mutableStateOf(false) }
    var showSavePlaylistView by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    LaunchedEffect(isHeartAnimating) { if (isHeartAnimating) { delay(150); isHeartAnimating = false } }
    LaunchedEffect(showBigHeartPopup) {
        if (showBigHeartPopup) {
            delay(700)
            showBigHeartPopup = false
        }
    }

    val view = LocalView.current

    // Ink-spill transition state keyed on song.id
    var displayedSongId by remember { mutableStateOf(song.id) }
    var currentCoverUrl by remember { mutableStateOf(song.coverUrl) }
    var previousCoverUrl by remember { mutableStateOf<String?>(null) }
    var spillDirection by remember { mutableIntStateOf(0) } // -1: prev (left), +1: next (right), 0: center/auto
    val spillProgress = remember { Animatable(1f) }
    val playPausePulseAnim = remember { Animatable(1f) }

    LaunchedEffect(isPlaying) {
        playPausePulseAnim.snapTo(0f)
        playPausePulseAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(450, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(song.id) {
        if (song.id != displayedSongId) {
            if (currentCoverUrl.isNotBlank()) {
                previousCoverUrl = currentCoverUrl
            }
            currentCoverUrl = song.coverUrl
            displayedSongId = song.id
            spillProgress.snapTo(0f)
            val duration = if (autoMixActive) 1200 else 650
            val easing = if (autoMixActive) LinearEasing else FastOutSlowInEasing
            spillProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(duration, easing = easing)
            )
            previousCoverUrl = null
        } else if (song.coverUrl != currentCoverUrl) {
            // High-res cover upgrade for same song
            currentCoverUrl = song.coverUrl
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (!isFavorite) {
                            Haptics.confirm(view)
                            onToggleFavorite()
                        }
                        isHeartAnimating = true
                        showBigHeartPopup = true
                    }
                )
            }
    ) {
        val backgroundBlurAmount by animateDpAsState(targetValue = if (showLyrics || showNextUp) 85.dp else 70.dp, label = "bgBlur")
        val sheetBackgroundAlpha = (morphProgress / 0.70f).coerceIn(0f, 1f)

        val infiniteDrift = rememberInfiniteTransition(label = "driftingAmbient")
        val lyricsDriftX by infiniteDrift.animateFloat(
            initialValue = -25f,
            targetValue = 25f,
            animationSpec = infiniteRepeatable(
                animation = tween(14000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "lyricsDriftX"
        )
        val lyricsDriftY by infiniteDrift.animateFloat(
            initialValue = -15f,
            targetValue = 15f,
            animationSpec = infiniteRepeatable(
                animation = tween(18000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "lyricsDriftY"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = sheetBackgroundAlpha }
                .haze(state = sheetHaze)
        ) {
        // Previous cover underneath during transition
        if (previousCoverUrl != null && spillProgress.value < 1f) {
            val prevAlpha = (1f - spillProgress.value).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = prevAlpha }
            ) {
                if (previousCoverUrl!!.isNotBlank()) {
                    AsyncImage(
                        model = previousCoverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().blur(backgroundBlurAmount)
                    )
                }
            }
        }

        // Active cover with blooming ink-spill transition
        if (currentCoverUrl.isNotEmpty()) {
            val progress = spillProgress.value
            val currentAlpha = progress.coerceIn(0f, 1f)
            val isSpilling = previousCoverUrl != null && progress < 0.999f
            val inkShape = remember(progress, isSpilling, spillDirection) {
                object : androidx.compose.ui.graphics.Shape {
                    override fun createOutline(
                        size: androidx.compose.ui.geometry.Size,
                        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                        density: androidx.compose.ui.unit.Density
                    ): androidx.compose.ui.graphics.Outline {
                        if (!isSpilling) {
                            return androidx.compose.ui.graphics.Outline.Rectangle(
                                androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height)
                            )
                        }
                        val maxR = kotlin.math.sqrt(size.width * size.width + size.height * size.height) * 1.30f
                        val r = maxR * progress.coerceIn(0f, 1f)
                        val centerOffset = when (spillDirection) {
                            1 -> androidx.compose.ui.geometry.Offset(size.width * 1.15f, size.height * 0.45f) // Next track: enters from right
                            -1 -> androidx.compose.ui.geometry.Offset(-size.width * 0.15f, size.height * 0.45f) // Prev track: enters from left
                            else -> androidx.compose.ui.geometry.Offset(size.width / 2f, size.height * 0.40f) // AutoMix / Center: middle
                        }
                        val path = androidx.compose.ui.graphics.Path().apply {
                            addOval(
                                androidx.compose.ui.geometry.Rect(
                                    center = centerOffset,
                                    radius = r
                                )
                            )
                        }
                        return androidx.compose.ui.graphics.Outline.Generic(path)
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(inkShape)
                    .graphicsLayer {
                        alpha = if (isSpilling) 1f else currentAlpha
                        if (showLyrics && !showNextUp) {
                            translationX = lyricsDriftX
                            translationY = lyricsDriftY
                            scaleX = 1.12f
                            scaleY = 1.12f
                        }
                    }
            ) {
                if (currentCoverUrl.isNotBlank()) {
                    AsyncImage(
                        model = currentCoverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().blur(backgroundBlurAmount)
                    )
                } else {
                    AppleMusicGradientCover(
                        title = song.title,
                        artist = song.artist,
                        modifier = Modifier.fillMaxSize().blur(backgroundBlurAmount),
                        showLabel = false
                    )
                }
            }
        }
        
        // Blurred background overlay when lyrics or next up is active
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (showLyrics || showNextUp) 0.68f else 0.40f)))
        }

        val isLyricsActive = showLyrics && !showNextUp
        val isNextUpActive = showNextUp

        var dragOffsetX by remember { mutableFloatStateOf(0f) }
        val animatedDragX by animateFloatAsState(
            targetValue = dragOffsetX,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
            label = "dragSpring"
        )

        val config = LocalConfiguration.current
        val screenWidth = config.screenWidthDp.dp
        val screenHeight = config.screenHeightDp.dp
        val defaultCoverSize = (screenWidth - 72.dp).coerceAtMost(300.dp)
        val defaultCoverLeft = (screenWidth - defaultCoverSize) / 2
        val defaultCoverTop = 84.dp

        val isCompactCover = isNextUpActive || isLyricsActive

        var lastLyricsInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
        var controlsVisible by remember { mutableStateOf(true) }

        LaunchedEffect(isLyricsActive, lastLyricsInteractionTime) {
            if (isLyricsActive) {
                controlsVisible = true
                delay(10000L) // 10s idle reading
                controlsVisible = false
            } else {
                controlsVisible = true
            }
        }

        val controlsAlpha by animateFloatAsState(
            targetValue = if (isLyricsActive && !controlsVisible) 0f else 1f,
            animationSpec = tween(400, easing = FastOutSlowInEasing),
            label = "controlsAlpha"
        )

        val coverSize by animateDpAsState(
            targetValue = if (isCompactCover) 68.dp else defaultCoverSize,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 340f),
            label = "coverSize"
        )
        val coverLeft by animateDpAsState(
            targetValue = if (isCompactCover) 24.dp else defaultCoverLeft,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 340f),
            label = "coverLeft"
        )
        val coverTop by animateDpAsState(
            targetValue = if (isCompactCover) 54.dp else defaultCoverTop,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 340f),
            label = "coverTop"
        )
        val coverCorner by animateDpAsState(
            targetValue = if (isCompactCover) 14.dp else 22.dp,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 340f),
            label = "coverCorner"
        )
        val coverAlpha by animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(220),
            label = "coverAlpha"
        )

        val density = LocalDensity.current
        val miniArtSize = 33.dp
        val miniArtLeft = 24.dp
        val miniArtTop = (screenHeight - 120.dp).coerceAtLeast(0.dp)

        val originArtLeft = sourceOriginBounds?.left?.let { with(density) { it.toDp() } } ?: miniArtLeft
        val originArtTop = sourceOriginBounds?.top?.let { with(density) { it.toDp() } } ?: miniArtTop
        val rawWidth = sourceOriginBounds?.width?.let { with(density) { it.toDp() } } ?: miniArtSize
        val rawHeight = sourceOriginBounds?.height?.let { with(density) { it.toDp() } } ?: miniArtSize
        val originArtSize = minOf(rawWidth, rawHeight).coerceIn(24.dp, 300.dp)
        val originArtCorner = if (sourceOriginBounds != null && sourceOriginBounds.width <= with(density) { 48.dp.toPx() }) {
            with(density) { (sourceOriginBounds.width / 2f).toDp() }
        } else if (sourceOriginBounds != null) {
            16.dp
        } else {
            13.dp
        }

        val currentCoverLeft = originArtLeft + (coverLeft - originArtLeft) * morphProgress
        val currentCoverTop = originArtTop + (coverTop - originArtTop) * morphProgress
        val currentCoverSize = originArtSize + (coverSize - originArtSize) * morphProgress
        val currentCoverCorner = originArtCorner + (coverCorner - originArtCorner) * morphProgress

        // Apple Music-Style Interactive Foreground Album Artwork (glides to top-left when Next Up or Lyrics is active, morphs to mini player when closing)
        if (coverAlpha > 0.01f && morphProgress > 0.0001f) {
            Box(
                modifier = Modifier
                    .offset(x = currentCoverLeft, y = currentCoverTop)
                    .size(currentCoverSize.coerceAtLeast(1.dp))
                    .graphicsLayer {
                        alpha = coverAlpha
                        if (!isCompactCover && morphProgress >= 0.98f) {
                            translationX = animatedDragX
                            val dampScale = 1f - (kotlin.math.abs(animatedDragX) / 3000f).coerceIn(0f, 0.05f)
                            scaleX = dampScale
                            scaleY = dampScale
                        }
                    }
                    .clip(RoundedCornerShape(currentCoverCorner))
                    .shadow(if (isCompactCover) 4.dp else (18.dp * morphProgress), RoundedCornerShape(currentCoverCorner))
            ) {
                if (currentCoverUrl.isNotBlank()) {
                    AsyncImage(
                        model = currentCoverUrl,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AppleMusicGradientCover(
                        title = song.title,
                        artist = song.artist,
                        cornerRadius = coverCorner,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Header beside the left-glided cover (Next Up or Lyrics)
        if (isCompactCover) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 104.dp, end = 20.dp, top = 54.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (isLyricsActive) "LYRICS" else "PLAYING NEXT",
                        color = AccentRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(song.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(song.artist, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp, maxLines = 1)
                }
            }
        }

        if (!isLyricsActive && !isNextUpActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 60.dp, bottom = 220.dp)
                    .graphicsLayer {
                        translationX = animatedDragX
                        val dampScale = 1f - (kotlin.math.abs(animatedDragX) / 3000f).coerceIn(0f, 0.05f)
                        scaleX = dampScale
                        scaleY = dampScale
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var totalDx = 0f
                            var totalDy = 0f
                            var isHorizontal: Boolean? = null
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break
                                val dragX = change.position.x - change.previousPosition.x
                                val dragY = change.position.y - change.previousPosition.y
                                totalDx += dragX
                                totalDy += dragY
                                if (isHorizontal == null) {
                                    val absX = kotlin.math.abs(totalDx)
                                    val absY = kotlin.math.abs(totalDy)
                                    if (absX + absY > 24f) {
                                        if (absX > 2.75f * absY) {
                                            isHorizontal = true
                                            change.consume()
                                        } else {
                                            isHorizontal = false
                                        }
                                    }
                                } else if (isHorizontal == true) {
                                    change.consume()
                                    dragOffsetX += dragX
                                }
                            }
                            if (isHorizontal == true) {
                                val currentIdx = playlist.indexOfFirst { it.id == song.id }
                                val canGoNext = (currentIdx in 0 until playlist.size - 1) || autoPlayEnabled || repeatMode == 1
                                val canGoPrevious = (currentIdx > 0) || (repeatMode == 1 && playlist.isNotEmpty())

                                if (dragOffsetX < -110f) {
                                    spillDirection = 1
                                    if (canGoNext) {
                                        Haptics.confirm(view)
                                        onNext()
                                    }
                                } else if (dragOffsetX > 110f) {
                                    spillDirection = -1
                                    if (canGoPrevious) {
                                        Haptics.confirm(view)
                                        onPrevious()
                                    }
                                }
                            }
                            dragOffsetX = 0f
                        }
                    }
            )
        }

        // Next Up Queue with stationary scroll container
        if (isNextUpActive) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 190.dp, top = 136.dp)
                    .pointerInput(Unit) {
                        var totalDx = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDx = 0f },
                            onDragEnd = {
                                if (totalDx > 90f) {
                                    Haptics.confirm(view)
                                    onToggleNextUp()
                                }
                                totalDx = 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                if (dragAmount > 0) totalDx += dragAmount
                            }
                        )
                    }
            ) {
                val currentIdx = playlist.indexOfFirst { it.id == song.id }
                val upcoming = if (currentIdx >= 0) playlist.drop(currentIdx + 1) else playlist

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.0f to Color.Transparent,
                                    0.025f to Color.Black,
                                    0.94f to Color.Black,
                                    1.0f to Color.Transparent
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                ) {
                    if (upcoming.isEmpty()) {
                        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFE2E2E2).copy(alpha = 0.12f))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(if (autoPlayEnabled) AccentRed.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Autorenew,
                                            contentDescription = null,
                                            tint = if (autoPlayEnabled) AccentRed else Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Autoplay similar tracks", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text(if (autoPlayEnabled) "Continues playing similar music" else "Only plays this album/queue", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                    }
                                }
                                Switch(
                                    checked = autoPlayEnabled,
                                    onCheckedChange = { onToggleAutoPlay() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = AccentRed,
                                        uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(48.dp))
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No more songs in queue", color = Color.White.copy(alpha = 0.5f), fontSize = 15.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 4.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFE2E2E2).copy(alpha = 0.12f))
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .background(if (autoPlayEnabled) AccentRed.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Autorenew,
                                                contentDescription = null,
                                                tint = if (autoPlayEnabled) AccentRed else Color.White.copy(alpha = 0.6f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                "Autoplay similar tracks",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                if (autoPlayEnabled) "Continues playing similar music" else "Only plays this album/queue",
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = autoPlayEnabled,
                                        onCheckedChange = { onToggleAutoPlay() },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = AccentRed,
                                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                            uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                                        )
                                    )
                                }
                            }
                            itemsIndexed(upcoming, key = { _, s -> s.id }) { idx, s ->
                                QueueSongItem(
                                    song = s,
                                    index = idx,
                                    totalCount = upcoming.size,
                                    onPlay = { onPlayFromQueue(s) },
                                    onMoveToPlayNext = { onMoveToPlayNext(s) },
                                    onRemove = { onRemoveFromQueue(s.id) },
                                    onMoveUp = { if (idx > 0) onMoveInQueue(idx, idx - 1) },
                                    onMoveDown = { if (idx < upcoming.size - 1) onMoveInQueue(idx, idx + 1) }
                                )
                            }
                        }
                    }
                }
            }
        } else if (isLyricsActive) {
            // Apple Music Synced Word-by-Word Lyrics View
            val processedLyrics = remember(lyrics, duration) { LyricsProcessor.processSyncedLyrics(lyrics, trackDurationMs = duration) }

            // High-refresh (60/120fps) frame clock interpolating position from audio playback with ~35ms latency compensation
            var livePositionMs by remember { mutableLongStateOf(currentPosition) }
            LaunchedEffect(isPlaying, currentPosition) {
                livePositionMs = currentPosition
                if (isPlaying) {
                    val anchorRealtime = SystemClock.elapsedRealtime()
                    val anchorPos = currentPosition
                    while (isActive) {
                        withFrameMillis {
                            val elapsed = SystemClock.elapsedRealtime() - anchorRealtime
                            // Compensate for Bluetooth / speaker audio output buffer latency (~35ms)
                            livePositionMs = (anchorPos + elapsed - 35L).coerceAtLeast(0L)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 130.dp, bottom = if (isLyricsActive && !controlsVisible) 30.dp else 180.dp)
                    .pointerInput(Unit) {
                        var totalDx = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDx = 0f },
                            onDragEnd = {
                                if (totalDx > 90f) {
                                    Haptics.confirm(view)
                                    onToggleLyrics()
                                }
                                totalDx = 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                if (dragAmount > 0) totalDx += dragAmount
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                lastLyricsInteractionTime = System.currentTimeMillis()
                                controlsVisible = true
                            }
                        )
                    }
            ) {
                if (processedLyrics.isNotEmpty()) {
                    val listState = rememberLazyListState()
                    val currentIdx = remember(processedLyrics, livePositionMs) {
                        LyricsProcessor.findActiveLineIndex(processedLyrics, livePositionMs)
                    }

                    val isLyricsDragged by listState.interactionSource.collectIsDraggedAsState()
                    var manualScrolled by remember { mutableStateOf(false) }

                    LaunchedEffect(isLyricsDragged) {
                        if (isLyricsDragged) {
                            manualScrolled = true
                            lastLyricsInteractionTime = System.currentTimeMillis()
                            controlsVisible = true
                        } else if (manualScrolled) {
                            // Resume auto-scroll after ~3s idle
                            delay(3000L)
                            manualScrolled = false
                        }
                    }

                    // Auto-scroll active line to top anchor (~24% viewport) smoothly
                    LaunchedEffect(currentIdx, manualScrolled) {
                        if (currentIdx in processedLyrics.indices && !manualScrolled && !isLyricsDragged) {
                            val viewportHeight = listState.layoutInfo.viewportSize.height
                            if (viewportHeight > 0) {
                                val topAnchor = (viewportHeight * 0.24f).toInt()
                                val item = listState.layoutInfo.visibleItemsInfo.find { it.index == currentIdx }
                                if (item != null) {
                                    val delta = item.offset - topAnchor
                                    if (kotlin.math.abs(delta) > 4) {
                                        listState.animateScrollBy(
                                            value = delta.toFloat(),
                                            animationSpec = spring(
                                                dampingRatio = 0.85f,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
                                    }
                                } else {
                                    listState.animateScrollToItem(
                                        index = currentIdx,
                                        scrollOffset = -topAnchor
                                    )
                                }
                            } else {
                                listState.animateScrollToItem(
                                    index = currentIdx
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    lastLyricsInteractionTime = System.currentTimeMillis()
                                    controlsVisible = true
                                }
                            }
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0.0f to Color.Transparent,
                                        0.07f to Color.Black,
                                        0.88f to Color.Black,
                                        1.0f to Color.Transparent
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 28.dp, end = 28.dp),
                            contentPadding = PaddingValues(top = 16.dp, bottom = if (isLyricsActive && !controlsVisible) 120.dp else 260.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(processedLyrics, key = { idx, line -> "lyric_${line.timeMs}_$idx" }) { idx, line ->
                                val isActive = idx == currentIdx
                                val distance = if (currentIdx >= 0) kotlin.math.abs(idx - currentIdx) else 1
                                val cappedDistance = distance.coerceAtMost(4)

                                val targetScale = if (isActive) 1.02f else 1.0f
                                val targetAlpha = if (isActive) 1.0f else (0.50f - cappedDistance * 0.08f).coerceAtLeast(0.20f)
                                val targetBlur = if (isActive) 0.dp else (cappedDistance.coerceAtMost(2) * 0.8f).dp

                                val lineScale by animateFloatAsState(
                                    targetValue = targetScale,
                                    animationSpec = tween(450, easing = FastOutSlowInEasing),
                                    label = "lineScale_$idx"
                                )
                                val lineAlpha by animateFloatAsState(
                                    targetValue = targetAlpha,
                                    animationSpec = tween(450, easing = FastOutSlowInEasing),
                                    label = "lineAlpha_$idx"
                                )
                                val lineBlur by animateDpAsState(
                                    targetValue = targetBlur,
                                    animationSpec = tween(450, easing = FastOutSlowInEasing),
                                    label = "lineBlur_$idx"
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            scaleX = lineScale
                                            scaleY = lineScale
                                            alpha = lineAlpha
                                            transformOrigin = TransformOrigin(0f, 0.5f)
                                            if (lineBlur > 0.dp) {
                                                compositingStrategy = CompositingStrategy.Offscreen
                                            }
                                        }
                                        .then(if (lineBlur > 0.dp) Modifier.blur(lineBlur) else Modifier)
                                        .padding(vertical = 4.dp, horizontal = 4.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            manualScrolled = false
                                            onSeek(line.timeMs)
                                        }
                                ) {
                                    if (line.isInstrumental) {
                                        // Animated Instrumental Interlude: "♪" + 3 dots filling in across the duration
                                        val breakElapsed = (livePositionMs - line.timeMs).coerceIn(0L, line.durationMs)
                                        val breakProgress = if (line.durationMs > 0) (breakElapsed.toFloat() / line.durationMs).coerceIn(0f, 1f) else 0f
                                        val breathScale = if (isActive) (1.0f + 0.04f * kotlin.math.sin(breakElapsed / 350.0).toFloat()) else 1.0f

                                        val dot1Active = if (isActive) (breakProgress >= 0.15f) else (currentIdx >= 0 && idx < currentIdx)
                                        val dot2Active = if (isActive) (breakProgress >= 0.50f) else (currentIdx >= 0 && idx < currentIdx)
                                        val dot3Active = if (isActive) (breakProgress >= 0.85f) else (currentIdx >= 0 && idx < currentIdx)

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .graphicsLayer {
                                                    scaleX = breathScale
                                                    scaleY = breathScale
                                                }
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Start
                                        ) {
                                            Text(
                                                text = "♪",
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isActive) Color.White else Color.White.copy(alpha = 0.40f)
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                listOf(dot1Active, dot2Active, dot3Active).forEach { dotFilled ->
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(if (dotFilled) Color.White else Color.White.copy(alpha = 0.30f))
                                                    )
                                                }
                                            }
                                        }
                                    } else if (line.words.isNotEmpty()) {
                                        val lineSweep = if (isActive) LyricsProcessor.calculateLineSweepProgress(line, livePositionMs) else 0f
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            line.words.forEachIndexed { wordIdx, word ->
                                                val wordProgress = if (isActive) {
                                                    LyricsProcessor.calculateWordProgressFromLine(wordIdx, lineSweep)
                                                } else if (currentIdx >= 0 && idx < currentIdx) {
                                                    1.0f
                                                } else {
                                                    0.0f
                                                }
                                                SyncedWordItem(
                                                    word = word,
                                                    progress = wordProgress,
                                                    isCurrentLine = isActive,
                                                    isPastLine = currentIdx >= 0 && idx < currentIdx,
                                                    onSeek = {
                                                        manualScrolled = false
                                                        onSeek(it)
                                                    }
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = line.text,
                                            fontSize = 29.sp,
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 37.sp,
                                            color = if (isActive) Color.White else Color.White.copy(alpha = 0.40f)
                                        )
                                    }
                                }
                            }
                        }

                        // Ambient upper & bottom progressive blur fade
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Black.copy(alpha = 0.60f), Color.Transparent)
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f))
                                    )
                                )
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Lyrics not available", color = Color.White.copy(alpha = 0.5f), fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }


        // Bottom Controls Container (Fades out after 10s of inactivity during Lyrics mode or when morphing down)
        val controlsVisibilityAlpha = controlsAlpha * ((morphProgress - 0.20f) / 0.80f).coerceIn(0f, 1f)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = controlsVisibilityAlpha
                }
                .then(
                    if (controlsAlpha < 0.1f) Modifier.pointerInput(Unit) {
                        detectTapGestures {
                            lastLyricsInteractionTime = System.currentTimeMillis()
                            controlsVisible = true
                        }
                    } else Modifier
                )
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            if (!showLyrics && !showNextUp) {
                // Song title + artist + Language & Genre badge (shifted 6px up)
                Column(
                    modifier = Modifier
                        .offset(y = (-6).dp)
                        .padding(horizontal = 24.dp)
                        .padding(top = 10.dp)
                ) {
                    Text(song.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(song.artist, color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                        val playingLang = remember(song.id) { com.beatles.player.data.MusicRepository.detectLanguage(song) }
                        val playingGenre = remember(song.id) { song.genre.ifBlank { com.beatles.player.data.MusicRepository.detectGenre(song) } }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.22f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "$playingLang • $playingGenre",
                                color = Color.White.copy(alpha = 0.90f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Download + Heart + AutoMix Toggle + 3-dots mini menu row (matching Lyrics & Next Up button background settings)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .offset(y = (-5).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isLocalTrack = song.streamUrl?.startsWith("content://") == true ||
                            song.streamUrl?.startsWith("file://") == true ||
                            song.id.startsWith("local_")
                    val isDownloaded = downloadedSongs.any { it.id == song.id }
                    val isDownloading = downloadingSongId == song.id
                    val isDownloadFailed = downloadFailedSongId == song.id

                    if (isLocalTrack) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val dlBg = when {
                            isDownloaded -> Color.White.copy(alpha = 0.28f)
                            isDownloading -> Color.White.copy(alpha = 0.22f)
                            isDownloadFailed -> Color(0xFFE74C3C).copy(alpha = 0.25f)
                            else -> Color.White.copy(alpha = 0.20f)
                        }
                        val dlTint = when {
                            isDownloaded -> Color.White
                            isDownloading -> Color.White
                            isDownloadFailed -> Color(0xFFFF6B6B)
                            else -> Color.White
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(dlBg)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    Haptics.confirm(view)
                                    when {
                                        isDownloading -> onCancelDownload(song.id)
                                        isDownloaded -> onRemoveDownload(song.id)
                                        else -> onDownloadSong(song)
                                    }
                                }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                when {
                                    isDownloading -> {
                                        CircularProgressIndicator(
                                            progress = { downloadProgress },
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp,
                                            trackColor = Color.White.copy(alpha = 0.25f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Downloading",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Cancel",
                                            tint = Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    isDownloaded -> {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Downloaded",
                                            tint = dlTint,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Downloaded",
                                            color = dlTint,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                    }
                                    isDownloadFailed -> {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = "Retry Download",
                                            tint = dlTint,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Retry",
                                            color = dlTint,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                    }
                                    else -> {
                                        Icon(
                                            Icons.Default.Download,
                                            contentDescription = "Download",
                                            tint = dlTint,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Download",
                                            color = dlTint,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    val heartBg = if (isFavorite) Color.White else Color.White.copy(alpha = 0.20f)
                    val heartTint = if (isFavorite) AccentRed else Color.White
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(heartBg)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                Haptics.confirm(view)
                                onToggleFavorite()
                                isHeartAnimating = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = heartTint,
                            modifier = Modifier.size(20.dp).scale(heartScale)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Triple-state AutoMix Toggle (New: AutoMix, Old: AutoMix Pro, Off: AutoMix Off)
                    val autoMixLabel = when (autoMixMode) {
                        com.beatles.player.player.automix.AutoMixMode.NEW -> "AutoMix"
                        com.beatles.player.player.automix.AutoMixMode.OLD -> "AutoMix Pro"
                        com.beatles.player.player.automix.AutoMixMode.OFF -> "AutoMix Off"
                    }
                    val isAutoMixActive = autoMixMode != com.beatles.player.player.automix.AutoMixMode.OFF
                    val autoMixBg = if (isAutoMixActive) Color.White else Color.White.copy(alpha = 0.20f)
                    val autoMixContentTint = if (isAutoMixActive) Color(0xFF141416) else Color.White

                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(autoMixBg)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                Haptics.confirm(view)
                                onToggleAutoMix()
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.GraphicEq,
                                contentDescription = autoMixLabel,
                                tint = autoMixContentTint,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                autoMixLabel,
                                color = autoMixContentTint,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.20f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                Haptics.click(view)
                                showMiniMenu = !showMiniMenu
                                showSavePlaylistView = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MoreHoriz,
                            contentDescription = "More Options",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Custom Canvas Seekbar (no vertical thumb bar, no active glow while moving, starry white glow during AutoMix)
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    var isDraggingSlider by remember { mutableStateOf(false) }
                    var sliderPos by remember { mutableFloatStateOf(currentPosition.toFloat()) }
                    var pendingSeek by remember { mutableStateOf(false) }

                    LaunchedEffect(currentPosition) {
                        if (!isDraggingSlider) {
                            sliderPos = currentPosition.toFloat()
                            pendingSeek = false
                        }
                    }

                    val trackAnimHeight by animateDpAsState(
                        targetValue = 8.dp, // same thickness whether or not AutoMix is mixing
                        animationSpec = tween(200),
                        label = "trackAnimHeight"
                    )

                    val infiniteTransition = rememberInfiniteTransition(label = "automixGlow")
                    val automixPulse by infiniteTransition.animateFloat(
                        initialValue = 0.40f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "automixPulse"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .pointerInput(duration) {
                                val maxDur = kotlin.math.max(1f, duration.toFloat())
                                detectTapGestures { offset ->
                                    val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    val safeMax = if (duration > 0) duration else Long.MAX_VALUE
                                    val newPos = (fraction * maxDur).toLong().coerceIn(0L, safeMax)
                                    sliderPos = newPos.toFloat()
                                    pendingSeek = true
                                    Haptics.tick(view)
                                    onSeek(newPos)
                                }
                            }
                            .pointerInput(duration) {
                                val maxDur = kotlin.math.max(1f, duration.toFloat())
                                detectHorizontalDragGestures(
                                    onDragStart = { offset ->
                                        isDraggingSlider = true
                                        val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                        sliderPos = (fraction * maxDur).coerceIn(0f, maxDur)
                                    },
                                    onDragEnd = {
                                        isDraggingSlider = false
                                        pendingSeek = true
                                        val safeMax = if (duration > 0) duration else Long.MAX_VALUE
                                        val clampedSeek = sliderPos.toLong().coerceIn(0L, safeMax)
                                        onSeek(clampedSeek)
                                    },
                                    onDragCancel = {
                                        isDraggingSlider = false
                                    },
                                    onHorizontalDrag = { change, _ ->
                                        val fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                        sliderPos = (fraction * maxDur).coerceIn(0f, maxDur)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(20.dp)) {
                            val maxDur = kotlin.math.max(1f, duration.toFloat())
                            val currentFraction = (sliderPos / maxDur).coerceIn(0f, 1f)
                            val w = size.width
                            val h = trackAnimHeight.toPx()
                            val centerY = size.height / 2f
                            val activeWidth = w * currentFraction
                            val corner = CornerRadius(h / 2f, h / 2f)

                            // Subtle starry white blurry glow halo behind active seekbar during AutoMix (does NOT increase bar thickness)
                            if (autoMixActive && activeWidth > 0f) {
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.28f * automixPulse),
                                    topLeft = Offset(0f, centerY - h / 2f),
                                    size = Size(activeWidth, h),
                                    cornerRadius = corner
                                )
                            }

                            // Inactive track (smooth pill)
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.22f),
                                topLeft = Offset(0f, centerY - h / 2f),
                                size = Size(w, h),
                                cornerRadius = corner
                            )

                            if (activeWidth > 0f) {
                                // Clip active track inside the EXACT pill shape of the inactive track so rounded corners match 100%
                                val inactiveTrackPath = androidx.compose.ui.graphics.Path().apply {
                                    addRoundRect(
                                        androidx.compose.ui.geometry.RoundRect(
                                            left = 0f,
                                            top = centerY - h / 2f,
                                            right = w,
                                            bottom = centerY + h / 2f,
                                            radiusX = h / 2f,
                                            radiusY = h / 2f
                                        )
                                    )
                                }
                                clipPath(inactiveTrackPath) {
                                    if (autoMixActive) {
                                        // Starry white crystalline active track with pulsating starry brightness
                                        drawRoundRect(
                                            brush = Brush.horizontalGradient(
                                                listOf(
                                                    Color(0xFFE2E8F0),
                                                    Color.White,
                                                    Color(0xFFE0E7FF),
                                                    Color.White
                                                )
                                            ),
                                            topLeft = Offset(0f, centerY - h / 2f),
                                            size = Size(activeWidth, h),
                                            cornerRadius = corner
                                        )
                                    } else {
                                        // Clean solid white active track
                                        drawRoundRect(
                                            color = Color.White.copy(alpha = 0.95f),
                                            topLeft = Offset(0f, centerY - h / 2f),
                                            size = Size(activeWidth, h),
                                            cornerRadius = corner
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val safeDur = duration.coerceAtLeast(0L)
                        val rawPos = if (isDraggingSlider) sliderPos.toLong() else currentPosition
                        val displayPos = if (safeDur > 0) rawPos.coerceIn(0L, safeDur) else rawPos.coerceAtLeast(0L)
                        Text(
                            "${displayPos / 60000}:${String.format("%02d", (displayPos / 1000) % 60)}",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                        if (autoMixActive && autoMixLabel != null) {
                            val pulseTransition = rememberInfiniteTransition(label = "pulse")
                            val pulseScale by pulseTransition.animateFloat(
                                initialValue = 1f, targetValue = 1.25f,
                                animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                                label = "scale"
                            )
                            val pulseAlpha by pulseTransition.animateFloat(
                                initialValue = 1f, targetValue = 0.5f,
                                animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                                label = "alpha"
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(contentAlignment = Alignment.Center) {
                                    Box(modifier = Modifier.size(6.dp).graphicsLayer(scaleX = pulseScale, scaleY = pulseScale, alpha = pulseAlpha).clip(CircleShape).background(Color.White))
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    autoMixLabel,
                                    color = Color.White.copy(alpha = pulseAlpha.coerceAtLeast(0.7f)),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        val rem = if (safeDur > 0) (safeDur - displayPos).coerceAtLeast(0L) else 0L
                        Text(
                            "-${rem / 60000}:${String.format("%02d", (rem / 1000) % 60)}",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            } else {
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Controls Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    Haptics.tick(view)
                    onToggleShuffle()
                }) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, tint = if (shuffleEnabled) AccentRed else Color.White.copy(alpha = 0.4f), modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = {
                    spillDirection = -1
                    Haptics.click(view)
                    onPrevious()
                }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = Color.White, modifier = Modifier.size(46.dp))
                }
                IconButton(onClick = {
                    Haptics.click(view)
                    onTogglePlay()
                }, modifier = Modifier.size(80.dp)) {
                    if (isLoadingStream) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(44.dp),
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp))
                    }
                }
                IconButton(onClick = {
                    spillDirection = 1
                    Haptics.click(view)
                    onNext()
                }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.White, modifier = Modifier.size(46.dp))
                }
                IconButton(onClick = {
                    Haptics.tick(view)
                    onToggleRepeat()
                }) {
                    Icon(
                        if (repeatMode == 2) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = null,
                        tint = if (repeatMode != 0) AccentRed else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))

            // Lyrics + Next Up pills (Strictly mutually exclusive, bright frosted glass overlay)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val lyricsBg = if (isLyricsActive) Color.White else Color.White.copy(alpha = 0.20f)
                val lyricsBorder = if (isLyricsActive) Color.White else Color.White.copy(alpha = 0.32f)
                val lyricsText = if (isLyricsActive) Color.Black else Color.White

                val nextUpBg = if (isNextUpActive) Color.White else Color.White.copy(alpha = 0.20f)
                val nextUpBorder = if (isNextUpActive) Color.White else Color.White.copy(alpha = 0.32f)
                val nextUpText = if (isNextUpActive) Color.Black else Color.White

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(lyricsBg)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            onToggleLyrics()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Lyrics", color = lyricsText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(nextUpBg)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            onToggleNextUp()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Next Up", color = nextUpText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 3-Dots iOS 27 Mini Submenu (Pops out from 3-dots with rounded corners blur, shrinks into it)
        val miniMenuAnim by animateFloatAsState(
            targetValue = if (showMiniMenu) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.80f, stiffness = 420f),
            label = "miniMenuAnim"
        )

        if (miniMenuAnim > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f * miniMenuAnim))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showMiniMenu = false
                        showSavePlaylistView = false
                    }
            ) {
                val isDark = AppTheme.isDark
                val menuBg = if (isDark) Color(0xFF1E2026).copy(alpha = 0.95f) else Color(0xFFFFFFFF).copy(alpha = 0.96f)
                val menuBorder = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 246.dp)
                        .width(230.dp)
                        .graphicsLayer {
                            scaleX = miniMenuAnim
                            scaleY = miniMenuAnim
                            alpha = miniMenuAnim.coerceIn(0f, 1f)
                            transformOrigin = TransformOrigin(0.96f, 1.0f)
                        }
                        .clip(RoundedCornerShape(22.dp))
                        .background(menuBg)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                        .padding(vertical = 6.dp)
                ) {
                    if (!showSavePlaylistView) {
                        Column {
                            // Option 1: Add to Playlist
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        Haptics.tick(view)
                                        showSavePlaylistView = true
                                    }
                                    .padding(horizontal = 16.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = AppTheme.fg(), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Add to Playlist", color = AppTheme.fg(), fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AppTheme.fg(0.4f), modifier = Modifier.size(16.dp))
                            }

                            HorizontalDivider(color = AppTheme.fg(0.08f), modifier = Modifier.padding(horizontal = 14.dp))

                            // Option 2: Go to Album
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        Haptics.confirm(view)
                                        showMiniMenu = false
                                        onGoToAlbum(song)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Album, contentDescription = null, tint = AppTheme.fg(), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Go to Album", color = AppTheme.fg(), fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AppTheme.fg(0.4f), modifier = Modifier.size(16.dp))
                            }


                            HorizontalDivider(color = AppTheme.fg(0.08f), modifier = Modifier.padding(horizontal = 14.dp))

                            // Option 4: AutoMix Toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        Haptics.confirm(view)
                                        onToggleAutoMix()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = AppTheme.fg(), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("AutoMix", color = AppTheme.fg(), fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                Text(if (autoMixEnabled) "ON" else "OFF", color = AppTheme.fg(0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Save to Playlist inside popover
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { showSavePlaylistView = false }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AppTheme.fg(), modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Playlists", color = AppTheme.fg(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                BasicTextField(
                                    value = newPlaylistName,
                                    onValueChange = { newPlaylistName = it },
                                    textStyle = TextStyle(color = AppTheme.fg(), fontSize = 12.sp),
                                    singleLine = true,
                                    cursorBrush = SolidColor(AppTheme.fg()),
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(AppTheme.fg(0.08f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    decorationBox = { inner ->
                                        if (newPlaylistName.isEmpty()) Text("New playlist...", color = AppTheme.fg(0.4f), fontSize = 12.sp)
                                        inner()
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        if (newPlaylistName.isNotBlank()) {
                                            onCreatePlaylist(newPlaylistName.trim(), song)
                                            newPlaylistName = ""
                                            showMiniMenu = false
                                            showSavePlaylistView = false
                                        }
                                    },
                                    enabled = newPlaylistName.isNotBlank(),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.fg(), disabledContainerColor = AppTheme.fg(0.1f))
                                ) {
                                    Text("+", color = AppTheme.bg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            if (playlists.isEmpty()) {
                                Text("No playlists yet", color = AppTheme.fg(0.4f), fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp))
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp)) {
                                    items(playlists) { pl ->
                                        val alreadyIn = pl.songs.any { it.id == song.id }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    onAddToPlaylist(pl.id, song)
                                                    showMiniMenu = false
                                                    showSavePlaylistView = false
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(pl.name, color = AppTheme.fg(), fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1)
                                            if (alreadyIn) Text("Added", color = Color(0xFF4ADE80), fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Double-Tap Heart Pop Animation Overlay (at top of Box hierarchy so it displays over artwork & queue)
        if (showBigHeartPopup || bigHeartAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(99f)
                    .graphicsLayer {
                        scaleX = bigHeartScale
                        scaleY = bigHeartScale
                        alpha = bigHeartAlpha
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = "Liked",
                    tint = Color(0xFFF82C38),
                    modifier = Modifier.size(92.dp)
                )
            }
        }

        // Center expansion blur pulse from the middle on Play / Pause
        if (playPausePulseAnim.value < 0.99f) {
            val p = playPausePulseAnim.value
            val pulseScale = 0.85f + 0.45f * p
            val pulseAlpha = (1f - p) * 0.28f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(99f)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = pulseAlpha
                    }
                    .blur(24.dp)
                    .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.45f), Color.Transparent))),
                contentAlignment = Alignment.Center
            ) {}
        }
    }
}

@Composable
fun QueueSongItem(
    song: Song,
    index: Int,
    totalCount: Int,
    onPlay: () -> Unit,
    onMoveToPlayNext: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val view = LocalView.current
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "queueDragOffset"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        val totalWidthPx = constraints.maxWidth.toFloat()
        val thresholdPx = totalWidthPx * 0.50f // Enforce at least 50% swipe threshold
        val dragFraction = if (thresholdPx > 0f) (kotlin.math.abs(animatedOffsetX) / thresholdPx).coerceIn(0f, 1f) else 0f
        val dragBlur = (dragFraction * 18f).dp
        val dragAlpha = (1f - dragFraction * 0.70f).coerceIn(0.25f, 1f)

        // Swipe Action Backgrounds
        if (offsetX > 20f) {
            val isOverThreshold = offsetX >= thresholdPx
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF2E7D32).copy(alpha = if (isOverThreshold) 0.95f else 0.55f))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isOverThreshold) "Release to Play Next" else "Swipe 50% to Play Next",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        } else if (offsetX < -20f) {
            val isOverThreshold = offsetX <= -thresholdPx
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFD32F2F).copy(alpha = if (isOverThreshold) 0.95f else 0.55f))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isOverThreshold) "Release to Remove" else "Swipe 50% to Remove",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }

        // Foreground Card Row: transparent glass at rest; a solid backing fades in only while swiping so it never overlaps the green/red action colour
        Row(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1E2024).copy(alpha = (kotlin.math.abs(animatedOffsetX) / 40f).coerceIn(0f, 1f) * 0.95f))
                .background(Color(0xFFE2E2E2).copy(alpha = 0.12f))
                .pointerInput(totalWidthPx) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX >= thresholdPx) {
                                Haptics.confirm(view)
                                onMoveToPlayNext()
                            } else if (offsetX <= -thresholdPx) {
                                Haptics.reject(view)
                                onRemove()
                            }
                            offsetX = 0f
                        },
                        onDragCancel = {
                            offsetX = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX = (offsetX + dragAmount).coerceIn(-totalWidthPx, totalWidthPx)
                        }
                    )
                }
                .clickable {
                    Haptics.click(view)
                    onPlay()
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer { alpha = dragAlpha },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Fixed hardware-clipped Box guarantees rounded corners never blur into squares
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .graphicsLayer {
                            shape = RoundedCornerShape(10.dp)
                            clip = true
                        }
                ) {
                    AsyncImage(
                        model = song.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(dragBlur)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .blur(dragBlur)
                ) {
                    Text(song.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(song.artist, color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp, maxLines = 1)
                }
            }
            // Up / Down Reorder buttons with haptic feedback
            Row(
                modifier = Modifier
                    .graphicsLayer { alpha = dragAlpha }
                    .blur(dragBlur),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (index > 0) {
                    IconButton(onClick = {
                        Haptics.tick(view)
                        onMoveUp()
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(22.dp))
                    }
                }
                if (index < totalCount - 1) {
                    IconButton(onClick = {
                        Haptics.tick(view)
                        onMoveDown()
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncedWordItem(
    word: SyncedWord,
    progress: Float,
    isCurrentLine: Boolean,
    isPastLine: Boolean,
    onSeek: (Long) -> Unit
) {
    val wordProgress = when {
        isPastLine -> 1f
        !isCurrentLine -> 0f
        else -> progress.coerceIn(0f, 1f)
    }

    val wordState = when {
        wordProgress >= 0.999f -> "done"
        wordProgress > 0.001f -> "sing"
        else -> "wait"
    }

    // Continuous sweep gradient mask with feather of about 14dp equivalent
    val feather = 0.18f
    val wordBrush = when (wordState) {
        "sing" -> Brush.horizontalGradient(
            0.0f to Color.White,
            (wordProgress - feather * 0.5f).coerceIn(0f, 1f) to Color.White,
            (wordProgress + feather * 0.5f).coerceIn(0f, 1f) to Color.White.copy(alpha = 0.40f),
            1.0f to Color.White.copy(alpha = 0.40f)
        )
        else -> null
    }

    val density = LocalDensity.current
    // Bright glow, lift (~1.9dp) and subtle scale (~1.03x) peaking around the middle of the word
    val midWordFactor = if (wordState == "sing") {
        kotlin.math.sin(wordProgress * Math.PI.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val liftPx = -1.9f * midWordFactor
    val wordScale = 1.0f + 0.03f * midWordFactor

    val textShadow = when (wordState) {
        "sing" -> androidx.compose.ui.graphics.Shadow(
            color = Color.White.copy(alpha = 0.45f + 0.50f * midWordFactor),
            blurRadius = 10f + 20f * midWordFactor,
            offset = Offset.Zero
        )
        "done" -> androidx.compose.ui.graphics.Shadow(
            color = Color.White.copy(alpha = 0.15f),
            blurRadius = 4f,
            offset = Offset.Zero
        )
        else -> null
    }

    val textStyle = if (wordBrush != null) {
        TextStyle(
            brush = wordBrush,
            shadow = textShadow
        )
    } else {
        TextStyle(
            color = if (wordState == "done") Color.White else Color.White.copy(alpha = 0.40f),
            shadow = textShadow
        )
    }

    val unsungBlur = if (wordState == "wait" && isCurrentLine) 0.6.dp else 0.dp

    Box(
        modifier = Modifier
            .graphicsLayer {
                translationY = liftPx * density.density
                scaleX = wordScale
                scaleY = wordScale
            }
            .then(if (unsungBlur > 0.dp) Modifier.blur(unsungBlur) else Modifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onSeek(word.startMs)
            }
    ) {
        Text(
            text = word.text,
            fontSize = 29.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 37.sp,
            style = textStyle
        )
    }
}
