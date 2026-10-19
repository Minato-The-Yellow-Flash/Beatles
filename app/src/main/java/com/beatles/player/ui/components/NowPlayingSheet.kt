package com.beatles.player.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import com.beatles.player.ui.theme.AppTheme
import com.beatles.player.ui.theme.QualityManager
import com.beatles.player.ui.theme.VisualQuality
import android.os.Build
import androidx.compose.ui.graphics.asComposeRenderEffect
import com.beatles.player.ui.motion.LiquidMotion
import com.beatles.player.ui.motion.liquidPress
import com.beatles.player.ui.glassRefraction
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.pow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.unit.lerp
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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    autoMixMode: com.beatles.player.player.automix.AutoMixMode = com.beatles.player.player.automix.AutoMixMode.ON,
    autoMixEnabled: Boolean = true,
    onToggleAutoMix: () -> Unit = {},
    onLoadMoreUpcoming: () -> Unit = {},
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
    sourceOriginBounds: Rect? = null,
    queueTitle: String? = null,
    isArtistMixLoading: Boolean = false,
    onPlayArtistMix: (String) -> Unit = {},
    sleepTimerState: com.beatles.player.viewmodel.SleepTimerState = com.beatles.player.viewmodel.SleepTimerState(),
    onStartSleepTimerMinutes: (Int) -> Unit = {},
    onStartSleepTimerSongs: (Int) -> Unit = {},
    onStartSleepTimerAlbum: () -> Unit = {},
    onStartSleepTimerEndOfTrack: () -> Unit = {},
    onExtendSleepTimerMinutes: (Int) -> Unit = {},
    onExtendSleepTimerSongs: (Int) -> Unit = {},
    onExtendSleepTimerAlbum: () -> Unit = {},
    onCancelSleepTimer: () -> Unit = {},
    onDismissSleepWarning: () -> Unit = {},
    onSetSleepExtendPopupEnabled: (Boolean) -> Unit = {},
    autoMixTargetSongId: String? = null
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
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var sheetRootBounds by remember { mutableStateOf<Rect?>(null) }
    var threeDotButtonBounds by remember { mutableStateOf<Rect?>(null) }

    LaunchedEffect(isHeartAnimating) { if (isHeartAnimating) { delay(150); isHeartAnimating = false } }
    LaunchedEffect(showBigHeartPopup) {
        if (showBigHeartPopup) {
            delay(700)
            showBigHeartPopup = false
        }
    }

    val view = LocalView.current
    val isDark = AppTheme.isDark

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
            .onGloballyPositioned { coords ->
                sheetRootBounds = coords.boundsInRoot()
            }
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
        val backgroundBlurAmount by animateDpAsState(
            targetValue = ((if (showLyrics || showNextUp) 20.dp else 16.dp) * morphProgress).coerceAtLeast(0.dp),
            label = "bgBlur"
        )
        val sheetBackgroundAlpha = 1f

        val infiniteDrift = rememberInfiniteTransition(label = "driftingAmbient")
        val liveBlurScale by infiniteDrift.animateFloat(
            initialValue = 1.02f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(16000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "liveBlurScale"
        )
        val liveBlurRot by infiniteDrift.animateFloat(
            initialValue = -1.0f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(20000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "liveBlurRot"
        )
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
                            scaleX = 1.12f * liveBlurScale
                            scaleY = 1.12f * liveBlurScale
                            rotationZ = liveBlurRot
                        } else {
                            scaleX = liveBlurScale
                            scaleY = liveBlurScale
                            rotationZ = liveBlurRot
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
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (showLyrics) 0.68f else if (showNextUp) 0.38f else 0.40f)))
        }

        val isLyricsActive = showLyrics && !showNextUp
        val isNextUpActive = showNextUp
        val isCompactTarget = isLyricsActive || isNextUpActive

        var lastActiveSubpage by remember { mutableStateOf("NONE") }
        var isSwitchingSubpages by remember { mutableStateOf(false) }

        LaunchedEffect(isLyricsActive, isNextUpActive) {
            val current = if (isLyricsActive) "LYRICS" else if (isNextUpActive) "NEXT_UP" else "NONE"
            if ((lastActiveSubpage == "LYRICS" && current == "NEXT_UP") || (lastActiveSubpage == "NEXT_UP" && current == "LYRICS")) {
                isSwitchingSubpages = true
            } else if (current == "NONE" || lastActiveSubpage == "NONE") {
                isSwitchingSubpages = false
            }
            lastActiveSubpage = current
        }

        val lyricsProgress by animateFloatAsState(
            targetValue = if (isLyricsActive) 1f else 0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 360f),
            label = "lyricsProgress"
        )
        val nextUpProgress by animateFloatAsState(
            targetValue = if (isNextUpActive) 1f else 0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 360f),
            label = "nextUpProgress"
        )

        // Slide fraction between Lyrics (0f) and Next Up (1f)
        val subpageTarget = if (isNextUpActive) 1f else 0f
        val subpageSlideFraction by animateFloatAsState(
            targetValue = subpageTarget,
            animationSpec = spring(dampingRatio = 0.88f, stiffness = 380f),
            label = "subpageSlideFraction"
        )
        val sideStretchFactor = (1f - kotlin.math.abs(subpageSlideFraction - 0.5f) * 2f).coerceIn(0f, 1f)

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

        val isCompactCover = isCompactTarget

        var lastLyricsInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
        var controlsVisible by remember { mutableStateOf(true) }

        LaunchedEffect(isLyricsActive, lastLyricsInteractionTime) {
            if (isLyricsActive) {
                controlsVisible = true
                delay(4000L) // 4s idle reading
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

        // Single unified compact progress: remains strictly at 1f during lyrics <-> next up toggle so album art never jumps or dips
        val compactProgress by animateFloatAsState(
            targetValue = if (isCompactTarget) 1f else 0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 360f),
            label = "compactProgress"
        )
        val coverSize = defaultCoverSize + (68.dp - defaultCoverSize) * compactProgress
        val coverLeft = defaultCoverLeft + (24.dp - defaultCoverLeft) * compactProgress
        val coverTop = defaultCoverTop + (54.dp - defaultCoverTop) * compactProgress
        val coverCorner = 22.dp + (14.dp - 22.dp) * compactProgress
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
        val morphMotionFactor = (4f * morphProgress * (1f - morphProgress)).coerceIn(0f, 1f)
        val artStretchY = 1f + (0.09f * morphMotionFactor)
        val artSqueezeX = 1f - (0.04f * morphMotionFactor)

        val baseCoverSize = defaultCoverSize.coerceAtLeast(100.dp)
        val coverScale = (currentCoverSize / baseCoverSize).coerceAtLeast(0.01f)
        val effectiveCoverCorner = currentCoverCorner / coverScale.coerceAtLeast(0.01f)

        // Apple Music-Style Interactive Foreground Album Artwork (glides to top-left when Next Up or Lyrics is active, morphs to mini player when closing)
        if (coverAlpha > 0.01f && morphProgress > 0.0001f) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(with(density) { currentCoverLeft.roundToPx() }, with(density) { currentCoverTop.roundToPx() }) }
                    .size(baseCoverSize)
                    .graphicsLayer {
                        val effectiveCoverAlpha = if (isLyricsActive) coverAlpha * controlsAlpha else coverAlpha
                        alpha = effectiveCoverAlpha
                        transformOrigin = TransformOrigin(0f, 0f)
                        if (compactProgress <= 0.001f && morphProgress >= 0.98f) {
                            val dampScale = 1f - (kotlin.math.abs(animatedDragX) / 3000f).coerceIn(0f, 0.05f)
                            scaleX = coverScale * dampScale
                            scaleY = coverScale * dampScale
                            translationX = animatedDragX
                        } else {
                            scaleX = coverScale * artSqueezeX
                            scaleY = coverScale * artStretchY
                        }
                        val baseElevation = lerp(16.dp, 4.dp, compactProgress)
                        shadowElevation = (baseElevation.toPx() * morphProgress)
                        shape = RoundedCornerShape(effectiveCoverCorner)
                        clip = true
                    }
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
        val compactHeaderAlpha = compactProgress
        if (compactHeaderAlpha > 0.01f) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 104.dp, end = 20.dp, top = 54.dp)
                    .graphicsLayer { alpha = if (isLyricsActive) compactHeaderAlpha * controlsAlpha else compactHeaderAlpha },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AnimatedContent(
                        targetState = if (isLyricsActive) "LYRICS" else (queueTitle?.uppercase() ?: "PLAYING NEXT"),
                        transitionSpec = {
                            if (targetState == "LYRICS") {
                                (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                            } else {
                                (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                            }
                        },
                        label = "headerTitleTransition"
                    ) { titleText ->
                        Text(
                            titleText,
                            color = AccentRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(song.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(song.artist, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp, maxLines = 1)
                }
            }
        }

        val mainViewAlpha = (1f - maxOf(lyricsProgress, nextUpProgress)).coerceIn(0f, 1f)
        if (mainViewAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 60.dp, bottom = 220.dp)
                    .graphicsLayer {
                        alpha = mainViewAlpha
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

        // Next Up Queue: Upward stretch when opened from main player, 90° horizontal side-stretch when toggling with lyrics
        if (nextUpProgress > 0.001f || (isSwitchingSubpages && subpageSlideFraction > 0.005f)) {
            NowPlayingQueueLayer(
                nextUpProgress = nextUpProgress,
                isSwitchingSubpages = isSwitchingSubpages,
                subpageSlideFraction = subpageSlideFraction,
                sideStretchFactor = sideStretchFactor,
                playlist = playlist,
                song = song,
                queueTitle = queueTitle,
                isArtistMixLoading = isArtistMixLoading,
                isDark = isDark,
                shuffleEnabled = shuffleEnabled,
                onToggleShuffle = onToggleShuffle,
                repeatMode = repeatMode,
                onToggleRepeat = onToggleRepeat,
                autoMixEnabled = autoMixEnabled,
                onToggleAutoMix = onToggleAutoMix,
                autoPlayEnabled = autoPlayEnabled,
                onToggleAutoPlay = onToggleAutoPlay,
                onLoadMoreUpcoming = onLoadMoreUpcoming,
                onPlayFromQueue = onPlayFromQueue,
                onMoveToPlayNext = onMoveToPlayNext,
                onRemoveFromQueue = onRemoveFromQueue,
                onMoveInQueue = onMoveInQueue,
                autoMixTargetSongId = autoMixTargetSongId,
                onToggleNextUp = onToggleNextUp
            )
        }

        if (lyricsProgress > 0.001f) {
            NowPlayingLyricsLayer(
                lyricsProgress = lyricsProgress,
                isSwitchingSubpages = isSwitchingSubpages,
                subpageSlideFraction = subpageSlideFraction,
                sideStretchFactor = sideStretchFactor,
                lyrics = lyrics,
                duration = duration,
                currentPosition = currentPosition,
                isPlaying = isPlaying,
                isLyricsActive = isLyricsActive,
                controlsVisible = controlsVisible,
                controlsAlpha = controlsAlpha,
                onToggleControls = {
                    lastLyricsInteractionTime = System.currentTimeMillis()
                    controlsVisible = !controlsVisible
                },
                onLyricsDragged = {
                    lastLyricsInteractionTime = System.currentTimeMillis()
                    controlsVisible = true
                },
                onSeek = onSeek,
                onToggleLyrics = onToggleLyrics
            )
        }


        // Bottom Controls Container (Fades out after 10s of inactivity during Lyrics mode or when morphing down)
        val controlsVisibilityAlpha = controlsAlpha * ((morphProgress - 0.20f) / 0.80f).coerceIn(0f, 1f)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = controlsVisibilityAlpha
                }
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            val songInfoProgress = (1f - maxOf(lyricsProgress, nextUpProgress)).coerceIn(0f, 1f)
            if (songInfoProgress > 0.001f) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = songInfoProgress
                            translationY = -20f * (1f - songInfoProgress)
                        }
                ) {
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

                    val dlProgressAnim by animateFloatAsState(
                        targetValue = if (isDownloading) downloadProgress.coerceIn(0f, 1f) else if (isDownloaded) 1f else 0f,
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 100, easing = androidx.compose.animation.core.LinearEasing),
                        label = "dlProgressAnim"
                    )

                    val checkmarkScale by animateFloatAsState(
                        targetValue = if (isDownloaded) 1f else 0.8f,
                        animationSpec = LiquidMotion.Lens,
                        label = "checkmarkLensScale"
                    )

                    if (isLocalTrack) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val dlBg = when {
                            isDownloaded -> Color.White.copy(alpha = 0.85f)
                            isDownloading -> Color.White.copy(alpha = 0.20f)
                            isDownloadFailed -> Color(0xFFE74C3C).copy(alpha = 0.22f)
                            else -> Color.White.copy(alpha = 0.16f)
                        }
                        val dlBorderColor = when {
                            isDownloaded -> Color.Transparent
                            isDownloadFailed -> Color(0xFFE74C3C).copy(alpha = 0.45f)
                            else -> Color.White.copy(alpha = 0.25f)
                        }

                        var isDlPressed by remember { mutableStateOf(false) }
                        val dlPressScale by animateFloatAsState(
                            targetValue = if (isDlPressed) 0.96f else 1.0f,
                            animationSpec = LiquidMotion.Lens,
                            label = "dlPressScale"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .graphicsLayer {
                                    scaleX = dlPressScale
                                    scaleY = dlPressScale
                                }
                                .clip(RoundedCornerShape(19.dp))
                                .background(dlBg)
                                .semantics {
                                    contentDescription = when {
                                        isDownloaded -> "Downloaded song"
                                        isDownloading -> "Downloading song ${(dlProgressAnim * 100).toInt()}%"
                                        isDownloadFailed -> "Retry download"
                                        else -> "Download song"
                                    }
                                }
                                .pointerInput(isDownloading, isDownloaded, isDownloadFailed) {
                                    detectTapGestures(
                                        onPress = {
                                            isDlPressed = true
                                            tryAwaitRelease()
                                            isDlPressed = false
                                        },
                                        onTap = {
                                            Haptics.confirm(view)
                                            when {
                                                isDownloading -> onCancelDownload(song.id)
                                                isDownloaded -> onRemoveDownload(song.id)
                                                else -> onDownloadSong(song)
                                            }
                                        }
                                    )
                                }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                // 24-28dp Live Circular Progress Ring (5 States: R3)
                                Box(
                                    modifier = Modifier.size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        // State 2: Queued / Starting
                                        isDownloading && downloadProgress <= 0.01f -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(22.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp,
                                                trackColor = Color.White.copy(alpha = 0.20f)
                                            )
                                        }
                                        // State 3: Downloading (Determinate smooth progress arc + cancel cross)
                                        isDownloading -> {
                                            CircularProgressIndicator(
                                                progress = { dlProgressAnim },
                                                modifier = Modifier.size(22.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp,
                                                trackColor = Color.White.copy(alpha = 0.20f)
                                            )
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Cancel",
                                                tint = Color.White.copy(alpha = 0.85f),
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                        // State 4: Done / Downloaded (Lens pulse checkmark without circle)
                                        isDownloaded -> {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Downloaded",
                                                tint = Color(0xFF151515),
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .graphicsLayer {
                                                        scaleX = checkmarkScale
                                                        scaleY = checkmarkScale
                                                    }
                                            )
                                        }
                                        // State 5: Error / Failed (Retry icon without circle)
                                        isDownloadFailed -> {
                                            Icon(
                                                Icons.Default.Refresh,
                                                contentDescription = "Retry Download",
                                                tint = Color(0xFFFF6B6B),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        // State 1: Idle (Raw download icon without circle)
                                        else -> {
                                            Icon(
                                                Icons.Default.Download,
                                                contentDescription = "Download",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                val labelText = when {
                                    isDownloaded -> "Downloaded"
                                    isDownloading && downloadProgress > 0.01f -> "${(dlProgressAnim * 100).toInt()}%"
                                    isDownloading -> "Queued"
                                    isDownloadFailed -> "Retry"
                                    else -> "Download"
                                }

                                Text(
                                    labelText,
                                    color = if (isDownloaded) Color(0xFF151515) else if (isDownloadFailed) Color(0xFFFF6B6B) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    val heartBg = if (isFavorite) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.12f)
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

                    // AutoMix Toggle (Classic smooth AutoMix; clean ON/OFF, no Pro or Inactive text)
                    val autoMixLabel = "AutoMix"
                    val isAutoMixActive = autoMixMode == com.beatles.player.player.automix.AutoMixMode.ON
                    val autoMixBg = if (isAutoMixActive) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.12f)
                    val autoMixContentTint = if (isAutoMixActive) Color(0xFF151515) else Color.White

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
                            .onGloballyPositioned { coords ->
                                threeDotButtonBounds = coords.boundsInRoot()
                            }
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

                Spacer(modifier = Modifier.height(14.dp))

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

                    var lastHapticSec by remember { mutableLongStateOf(-1L) }

                    val effectiveDuration = if (duration > 0L) duration else ((song.durationSeconds.toLong() * 1000L).takeIf { it > 0L } ?: 1L)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .pointerInput(effectiveDuration) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val maxDur = effectiveDuration.toFloat()
                                    val w = size.width.toFloat().coerceAtLeast(1f)
                                    val initialFraction = (down.position.x / w).coerceIn(0f, 1f)
                                    var currentDragPos = (initialFraction * maxDur).coerceIn(0f, maxDur)
                                    sliderPos = currentDragPos
                                    isDraggingSlider = true
                                    Haptics.lightScrubTick(view)

                                    var activePointerId = down.id
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == activePointerId } ?: break
                                        if (!change.pressed) {
                                            change.consume()
                                            break
                                        }
                                        val fraction = (change.position.x / w).coerceIn(0f, 1f)
                                        currentDragPos = (fraction * maxDur).coerceIn(0f, maxDur)
                                        sliderPos = currentDragPos
                                        val sec = (currentDragPos / 1000f).toLong()
                                        if (sec != lastHapticSec) {
                                            lastHapticSec = sec
                                            Haptics.lightScrubTick(view)
                                        }
                                        change.consume()
                                    }
                                    isDraggingSlider = false
                                    pendingSeek = true
                                    val clampedSeek = currentDragPos.toLong().coerceIn(0L, effectiveDuration)
                                    onSeek(clampedSeek)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(20.dp)) {
                            val maxDur = effectiveDuration.toFloat()
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
                                    // 85% white active track with blur
                                    drawRoundRect(
                                        color = Color.White.copy(alpha = 0.85f),
                                        topLeft = Offset(0f, centerY - h / 2f),
                                        size = Size(activeWidth, h),
                                        cornerRadius = corner
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val safeDur = effectiveDuration
                        val rawPos = if (isDraggingSlider) sliderPos.toLong() else currentPosition
                        val displayPos = if (safeDur > 0) rawPos.coerceIn(0L, safeDur) else rawPos.coerceAtLeast(0L)
                        Text(
                            "${displayPos / 60000}:${String.format("%02d", (displayPos / 1000) % 60)}",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                        if (autoMixActive && autoMixLabel != null) {
                            Text(
                                autoMixLabel,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
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
            }
        }

            // Controls Row (Mathematically and optically centered across all screens)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Slot 1: Shuffle
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                Haptics.tick(view)
                                onToggleShuffle()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (shuffleEnabled) Color.White else Color.White.copy(alpha = 0.40f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Slot 2: Skip Previous
                Box(
                    modifier = Modifier.weight(1.15f),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            spillDirection = -1
                            Haptics.click(view)
                            onPrevious()
                        },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Rounded.SkipPrevious, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                    }
                }

                // Slot 3: Center Play / Pause Button (Anchored to exact center)
                Box(
                    modifier = Modifier.weight(1.6f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                        if (playPausePulseAnim.value < 0.99f) {
                            val p = playPausePulseAnim.value
                            val pulseScale = 1.0f + 0.45f * p
                            val pulseAlpha = (1f - p) * 0.16f
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .graphicsLayer {
                                        scaleX = pulseScale
                                        scaleY = pulseScale
                                        alpha = pulseAlpha
                                    }
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                            )
                        }
                        IconButton(
                            onClick = {
                                Haptics.click(view)
                                onTogglePlay()
                            },
                            modifier = Modifier.size(92.dp)
                        ) {
                            if (isLoadingStream) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = Color.White,
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Icon(
                                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(68.dp)
                                )
                            }
                        }
                    }
                }

                // Slot 4: Skip Next
                Box(
                    modifier = Modifier.weight(1.15f),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            spillDirection = 1
                            Haptics.click(view)
                            onNext()
                        },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Rounded.SkipNext, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                    }
                }

                // Slot 5: Repeat
                val isRepeatActive = repeatMode != 0
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                Haptics.tick(view)
                                onToggleRepeat()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (repeatMode == 2) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            contentDescription = "Repeat",
                            tint = if (isRepeatActive) Color.White else Color.White.copy(alpha = 0.40f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))

            // Lyrics + Next Up pills (Strictly mutually exclusive, 85% white without border when active)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val lyricsBg = if (isLyricsActive) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.12f)
                val lyricsText = if (isLyricsActive) Color(0xFF151515) else Color.White

                val nextUpBg = if (isNextUpActive) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.12f)
                val nextUpText = if (isNextUpActive) Color(0xFF151515) else Color.White

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

        // 3-Dots iOS 27 Mini Submenu
        NowPlayingThreeDotsMenu(
            showMiniMenu = showMiniMenu,
            onDismissMiniMenu = {
                showMiniMenu = false
                showSavePlaylistView = false
            },
            showSavePlaylistView = showSavePlaylistView,
            onSetShowSavePlaylistView = { showSavePlaylistView = it },
            sheetRootBounds = sheetRootBounds,
            threeDotButtonBounds = threeDotButtonBounds,
            song = song,
            onGoToAlbum = onGoToAlbum,
            onPlayArtistMix = onPlayArtistMix,
            autoMixEnabled = autoMixEnabled,
            onToggleAutoMix = onToggleAutoMix,
            sleepTimerState = sleepTimerState,
            onOpenSleepTimerDialog = { showSleepTimerDialog = true },
            playlists = playlists,
            onCreatePlaylist = onCreatePlaylist,
            onAddToPlaylist = onAddToPlaylist
        )

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

        // Sleep Extend Warning Popup (Floating pill at top center)
        if (sleepTimerState.showWarningPopup) {
            SleepExtendWarningPopup(
                remainingSeconds = sleepTimerState.warningPopupRemainingSeconds,
                onDismiss = onDismissSleepWarning,
                onExtendMinutes = onExtendSleepTimerMinutes,
                onExtendSongs = onExtendSleepTimerSongs,
                onExtendAlbum = onExtendSleepTimerAlbum
            )
        }

        // Custom Beatles Sleep Timer Modal Sheet
        if (showSleepTimerDialog) {
            BackHandler(enabled = true) { showSleepTimerDialog = false }
            BeatlesSleepTimerSheet(
                state = sleepTimerState,
                currentSong = song,
                onDismiss = { showSleepTimerDialog = false },
                onStartMinutes = {
                    onStartSleepTimerMinutes(it)
                    showSleepTimerDialog = false
                },
                onStartSongs = {
                    onStartSleepTimerSongs(it)
                    showSleepTimerDialog = false
                },
                onStartAlbum = {
                    onStartSleepTimerAlbum()
                    showSleepTimerDialog = false
                },
                onStartEndOfTrack = {
                    onStartSleepTimerEndOfTrack()
                    showSleepTimerDialog = false
                },
                onCancelTimer = {
                    onCancelSleepTimer()
                    showSleepTimerDialog = false
                },
                onToggleExtendPopup = onSetSleepExtendPopupEnabled
            )
        }

    }
}

@Composable
fun NextUpControlPillsRow(
    shuffleEnabled: Boolean,
    onToggleShuffle: () -> Unit,
    repeatMode: Int,
    onToggleRepeat: () -> Unit,
    autoMixEnabled: Boolean,
    onToggleAutoMix: () -> Unit,
    autoPlayEnabled: Boolean,
    onToggleAutoPlay: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val pills = listOf(
            Triple("Shuffle", Icons.Default.Shuffle, shuffleEnabled) to onToggleShuffle,
            Triple(
                when (repeatMode) { 2 -> "Repeat 1"; else -> "Repeat" },
                when (repeatMode) { 2 -> Icons.Default.RepeatOne; else -> Icons.Default.Repeat },
                repeatMode != 0
            ) to onToggleRepeat,
            Triple("AutoMix", Icons.Default.GraphicEq, autoMixEnabled) to onToggleAutoMix,
            Triple("Autoplay", Icons.Default.Autorenew, autoPlayEnabled) to onToggleAutoPlay
        )

        pills.forEach { (item, action) ->
            val (label, icon, isActive) = item
            val interactionSource = remember { MutableInteractionSource() }
            val pillShape = CircleShape
            // Match the UI of the Next Up button: 85% white when active with dark icon, 12% white when inactive with white icon
            val bg = if (isActive) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.12f)
            val tint = if (isActive) Color(0xFF151515) else Color.White

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(pillShape)
                    .background(bg)
                    .liquidPress(interactionSource)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        Haptics.tick(view)
                        action()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
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
    onMoveDown: () -> Unit,
    isAutoMixTarget: Boolean = false
) {
    val view = LocalView.current
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "queueDragOffset"
    )

    val cardShape = RoundedCornerShape(30)
    val cardBg = Color.White.copy(alpha = 0.12f)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 3.dp)
            .height(56.dp)
            .clip(cardShape)
    ) {
        val totalWidthPx = constraints.maxWidth.toFloat()
        val thresholdPx = totalWidthPx * 0.50f
        val dragFraction = if (thresholdPx > 0f) (kotlin.math.abs(animatedOffsetX) / thresholdPx).coerceIn(0f, 1f) else 0f
        val dragAlpha = (1f - dragFraction * 0.40f).coerceIn(0.4f, 1f)

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
                    Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isOverThreshold) "Release to Play Next" else "Swipe 50% to Play Next",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
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
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }

        // Foreground Card Row: clean pill matching Next Up button UI
        Row(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .clip(cardShape)
                .background(if (kotlin.math.abs(animatedOffsetX) > 2f) Color(0xFF1E2024) else cardBg)
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
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    Haptics.click(view)
                    onPlay()
                }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer { alpha = dragAlpha },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // High-res album artwork in rounded thumbnail
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = song.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(song.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                        if (isAutoMixTarget) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF82C38).copy(alpha = 0.22f))
                                    .border(1.dp, Color(0xFFF82C38).copy(alpha = 0.50f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 6.dp, vertical = 1.5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFFF82C38),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        "AutoMix",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(song.artist, color = Color.White.copy(alpha = 0.60f), fontSize = 11.sp, maxLines = 1)
                }
            }
            // Up / Down Reorder buttons with haptic feedback
            Row(
                modifier = Modifier.graphicsLayer { alpha = dragAlpha },
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                Haptics.tick(view)
                                onMoveUp()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move up",
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (index < totalCount - 1) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                Haptics.tick(view)
                                onMoveDown()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move down",
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(20.dp)
                        )
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
            color = Color.White.copy(alpha = 0.45f + 0.35f * midWordFactor),
            blurRadius = 2f + 4f * midWordFactor,
            offset = Offset.Zero
        )
        "done" -> androidx.compose.ui.graphics.Shadow(
            color = Color.White.copy(alpha = 0.15f),
            blurRadius = 2f,
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

    val unsungBlur = 0.dp

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

@Composable
fun SleepExtendWarningPopup(
    remainingSeconds: Int,
    onDismiss: () -> Unit,
    onExtendMinutes: (Int) -> Unit,
    onExtendSongs: (Int) -> Unit,
    onExtendAlbum: () -> Unit
) {
    val isDark = AppTheme.isDark
    val view = LocalView.current
    val popupBg = if (isDark) Color(0xFF1E2026) else Color(0xFFFFFFFF)
    val popupBorder = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.10f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            .zIndex(150f),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = if (isDark) Color.Black.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.2f))
                .clip(RoundedCornerShape(24.dp))
                .background(popupBg)
                .border(1.dp, popupBorder, RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF82C38).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Bedtime,
                        contentDescription = null,
                        tint = Color(0xFFF82C38),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Music stopping soon",
                        color = AppTheme.fg(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Sleep timer ends in ${remainingSeconds}s",
                        color = AppTheme.fg(0.6f),
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(AppTheme.fg(0.08f))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            Haptics.tick(view)
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = AppTheme.fg(0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Extension Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(
                    "+5m" to { onExtendMinutes(5) },
                    "+15m" to { onExtendMinutes(15) },
                    "+1 Song" to { onExtendSongs(1) },
                    "+Album" to { onExtendAlbum() }
                )
                presets.forEach { (label, onClick) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppTheme.fg(0.08f))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                Haptics.confirm(view)
                                onClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = AppTheme.fg(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CameraLensHorizontalDial(
    value: Int,
    range: IntRange = 5..180,
    step: Int = 1,
    majorInterval: Int = 15,
    unitLabel: String = "min",
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val density = LocalDensity.current

    val tickSpacingDp = 12.dp
    val tickSpacingPx = with(density) { tickSpacingDp.toPx() }

    var currentFloatVal by remember(value) { mutableFloatStateOf(value.toFloat()) }
    var lastHapticInt by remember { mutableIntStateOf(value) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(AppTheme.fg(0.05f))
            .pointerInput(range, step) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val deltaVal = -dragAmount / tickSpacingPx * step
                        val newVal = (currentFloatVal + deltaVal).coerceIn(range.first.toFloat(), range.last.toFloat())
                        currentFloatVal = newVal
                        val newInt = (Math.round(newVal / step) * step).coerceIn(range.first, range.last)
                        if (newInt != lastHapticInt) {
                            lastHapticInt = newInt
                            Haptics.tick(view)
                            onValueChange(newInt)
                        }
                    },
                    onDragEnd = {
                        val finalInt = (Math.round(currentFloatVal / step) * step).coerceIn(range.first, range.last)
                        currentFloatVal = finalInt.toFloat()
                        onValueChange(finalInt)
                    }
                )
            }
    ) {
        val totalWidthPx = constraints.maxWidth.toFloat()
        val centerXPx = totalWidthPx / 2f

        // Digital display
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$value",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFF82C38),
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unitLabel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.fg(0.7f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                if (unitLabel == "min" && value >= 60) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${value / 60}h ${value % 60}m)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppTheme.fg(0.45f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }

        // Camera lens marking ruler canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .align(Alignment.BottomCenter)
        ) {
            val canvasHeight = size.height
            val activeVal = currentFloatVal

            val totalTicksVisible = (totalWidthPx / tickSpacingPx).toInt() + 4
            val minVisibleTick = (activeVal - totalTicksVisible / 2).toInt().coerceAtLeast(range.first)
            val maxVisibleTick = (activeVal + totalTicksVisible / 2).toInt().coerceAtMost(range.last)

            for (tickVal in minVisibleTick..maxVisibleTick step step) {
                val tickX = centerXPx + (tickVal - activeVal) * (tickSpacingPx / step)
                if (tickX < -20f || tickX > totalWidthPx + 20f) continue

                val isMajor = tickVal % majorInterval == 0
                val isMedium = tickVal % (majorInterval / 3).coerceAtLeast(5) == 0

                val tickHeight = when {
                    isMajor -> 26.dp.toPx()
                    isMedium -> 18.dp.toPx()
                    else -> 10.dp.toPx()
                }

                val distFromCenter = kotlin.math.abs(tickX - centerXPx)
                val edgeAlpha = (1f - (distFromCenter / (totalWidthPx * 0.48f)).pow(2)).coerceIn(0f, 1f)
                val tickColor = when {
                    isMajor -> AppTheme.fg(0.85f * edgeAlpha)
                    isMedium -> AppTheme.fg(0.50f * edgeAlpha)
                    else -> AppTheme.fg(0.25f * edgeAlpha)
                }

                val strokeWidth = if (isMajor) 2.dp.toPx() else 1.2.dp.toPx()
                val startY = canvasHeight - tickHeight - 8.dp.toPx()
                val endY = canvasHeight - 8.dp.toPx()

                drawLine(
                    color = tickColor,
                    start = Offset(tickX, startY),
                    end = Offset(tickX, endY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // Fixed Center Indicator Needle in AccentRed
            val needleTop = canvasHeight - 34.dp.toPx()
            val needleBottom = canvasHeight - 6.dp.toPx()
            drawLine(
                color = Color(0xFFF82C38),
                start = Offset(centerXPx, needleTop),
                end = Offset(centerXPx, needleBottom),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                color = Color(0xFFF82C38),
                radius = 3.dp.toPx(),
                center = Offset(centerXPx, needleTop - 2.dp.toPx())
            )
        }
    }
}

@Composable
fun BeatlesSleepTimerSheet(
    state: com.beatles.player.viewmodel.SleepTimerState,
    currentSong: Song,
    onDismiss: () -> Unit,
    onStartMinutes: (Int) -> Unit,
    onStartSongs: (Int) -> Unit,
    onStartAlbum: () -> Unit,
    onStartEndOfTrack: () -> Unit,
    onCancelTimer: () -> Unit,
    onToggleExtendPopup: (Boolean) -> Unit
) {
    val isDark = AppTheme.isDark
    val view = LocalView.current
    val navCorner = 28.dp
    val sheetShape = RoundedCornerShape(topStart = navCorner, topEnd = navCorner, bottomStart = 0.dp, bottomEnd = 0.dp)
    val sheetBg = if (isDark) Color(0xFF1E2026).copy(alpha = 0.94f) else Color.White.copy(alpha = 0.94f)

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Time, 1: Songs, 2: 1 Track, 3: Album
    var customMinutes by remember { mutableIntStateOf(30) }
    var customSongs by remember { mutableIntStateOf(3) }

    val isTimerActive = state.mode != com.beatles.player.viewmodel.SleepTimerMode.NONE

    BackHandler(enabled = true) {
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(140f)
            .background(Color.Black.copy(alpha = 0.60f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                Haptics.tick(view)
                onDismiss()
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(24.dp, sheetShape, spotColor = if (isDark) Color.Black.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.25f))
                .clip(sheetShape)
                .background(sheetBg)
                .drawWithContent {
                    drawContent()
                    drawLine(
                        color = if (isDark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.08f),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount > 25f) onDismiss()
                    }
                }
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { /* swallow touches */ }
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 28.dp)
        ) {
            // Drag handle pill
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AppTheme.fg(0.20f))
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Bedtime,
                    contentDescription = null,
                    tint = if (isTimerActive) Color(0xFFF82C38) else AppTheme.fg(),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "Sleep Timer",
                    color = AppTheme.fg(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        Haptics.tick(view)
                        onDismiss()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AppTheme.fg(0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Active Timer Banner (if currently running)
            if (isTimerActive) {
                val activeDescription = when (state.mode) {
                    com.beatles.player.viewmodel.SleepTimerMode.TIME -> {
                        val m = state.remainingSeconds / 60
                        val s = state.remainingSeconds % 60
                        "Music turns off in ${m}m ${String.format("%02d", s)}s"
                    }
                    com.beatles.player.viewmodel.SleepTimerMode.SONGS -> {
                        "Music turns off after ${state.remainingSongs} song${if (state.remainingSongs > 1) "s" else ""}"
                    }
                    com.beatles.player.viewmodel.SleepTimerMode.ALBUM -> {
                        "Music turns off after current album: ${state.targetAlbumId ?: "Album"}"
                    }
                    com.beatles.player.viewmodel.SleepTimerMode.END_OF_TRACK -> {
                        "Music turns off after this song finishes"
                    }
                    else -> "Active"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF82C38).copy(alpha = 0.12f))
                        .border(1.dp, Color(0xFFF82C38).copy(alpha = 0.30f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "TIMER ACTIVE",
                            color = Color(0xFFF82C38),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            activeDescription,
                            color = AppTheme.fg(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            Haptics.confirm(view)
                            onCancelTimer()
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF82C38)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Turn Off", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // Tab selector pills: [Time | Songs | 1 Track | Album]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppTheme.fg(0.06f))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabs = listOf("Time", "Songs", "1 Track", "Album")
                tabs.forEachIndexed { idx, title ->
                    val isSelected = selectedTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(if (isSelected) AppTheme.fg() else Color.Transparent)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                Haptics.tick(view)
                                selectedTab = idx
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            title,
                            color = if (isSelected) (if (isDark) Color(0xFF141416) else Color.White) else AppTheme.fg(0.6f),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // Time Mode: Camera Lens Dial + Presets
                    Text("Slide to Adjust Duration", color = AppTheme.fg(0.6f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    CameraLensHorizontalDial(
                        value = customMinutes,
                        range = 5..180,
                        step = 1,
                        majorInterval = 15,
                        unitLabel = "min",
                        onValueChange = { customMinutes = it }
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Quick Presets", color = AppTheme.fg(0.6f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val minutePresets = listOf(15, 30, 45, 60, 90)
                        minutePresets.forEach { m ->
                            val isPresetSelected = customMinutes == m
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isPresetSelected) Color(0xFFF82C38).copy(alpha = 0.20f) else AppTheme.fg(0.08f))
                                    .border(1.dp, if (isPresetSelected) Color(0xFFF82C38) else Color.Transparent, RoundedCornerShape(12.dp))
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                        Haptics.tick(view)
                                        customMinutes = m
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${m}m", color = if (isPresetSelected) Color(0xFFF82C38) else AppTheme.fg(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            Haptics.confirm(view)
                            onStartMinutes(customMinutes)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF82C38))
                    ) {
                        Text("Start Sleep Timer ($customMinutes min)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                1 -> {
                    // Songs Mode: Camera Lens Dial + Presets
                    Text("Slide to Adjust Songs", color = AppTheme.fg(0.6f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    CameraLensHorizontalDial(
                        value = customSongs,
                        range = 1..30,
                        step = 1,
                        majorInterval = 5,
                        unitLabel = "songs",
                        onValueChange = { customSongs = it }
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Quick Presets", color = AppTheme.fg(0.6f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val songPresets = listOf(1, 2, 3, 5, 10)
                        songPresets.forEach { count ->
                            val isPresetSelected = customSongs == count
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isPresetSelected) Color(0xFFF82C38).copy(alpha = 0.20f) else AppTheme.fg(0.08f))
                                    .border(1.dp, if (isPresetSelected) Color(0xFFF82C38) else Color.Transparent, RoundedCornerShape(12.dp))
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                        Haptics.tick(view)
                                        customSongs = count
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("$count", color = if (isPresetSelected) Color(0xFFF82C38) else AppTheme.fg(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            Haptics.confirm(view)
                            onStartSongs(customSongs)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF82C38))
                    ) {
                        Text("Start Sleep Timer ($customSongs song${if (customSongs > 1) "s" else ""})", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                2 -> {
                    // End of current track
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppTheme.fg(0.06f))
                            .padding(16.dp)
                    ) {
                        Text("Current Song", color = AppTheme.fg(0.5f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(currentSong.title, color = AppTheme.fg(), fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(currentSong.artist, color = AppTheme.fg(0.7f), fontSize = 12.sp, maxLines = 1)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Playback will stop smoothly as soon as this song completes.", color = AppTheme.fg(0.6f), fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            Haptics.confirm(view)
                            onStartEndOfTrack()
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF82C38))
                    ) {
                        Text("Stop at End of Track", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                3 -> {
                    // Album Timer
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppTheme.fg(0.06f))
                            .padding(16.dp)
                    ) {
                        Text("Current Album", color = AppTheme.fg(0.5f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(currentSong.album.ifBlank { "Current Album" }, color = AppTheme.fg(), fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(currentSong.artist, color = AppTheme.fg(0.7f), fontSize = 12.sp, maxLines = 1)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Playback will stop when all upcoming songs in this album finish playing.", color = AppTheme.fg(0.6f), fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            Haptics.confirm(view)
                            onStartAlbum()
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF82C38))
                    ) {
                        Text("Stop at End of Album", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = AppTheme.fg(0.08f))
            Spacer(modifier = Modifier.height(14.dp))

            // Extend Reminder Settings Toggle Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        Haptics.tick(view)
                        onToggleExtendPopup(!state.isExtendPopupEnabled)
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Extend reminder popup",
                        color = AppTheme.fg(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Show a 20s popup with extension presets before sleep",
                        color = AppTheme.fg(0.5f),
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = state.isExtendPopupEnabled,
                    onCheckedChange = {
                        Haptics.tick(view)
                        onToggleExtendPopup(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFF82C38),
                        uncheckedThumbColor = AppTheme.fg(0.6f),
                        uncheckedTrackColor = AppTheme.fg(0.12f)
                    )
                )
            }
        }
    }
}

@Composable
fun NowPlayingQueueLayer(
    nextUpProgress: Float,
    isSwitchingSubpages: Boolean,
    subpageSlideFraction: Float,
    sideStretchFactor: Float,
    playlist: List<Song>,
    song: Song,
    queueTitle: String?,
    isArtistMixLoading: Boolean,
    isDark: Boolean,
    shuffleEnabled: Boolean,
    onToggleShuffle: () -> Unit,
    repeatMode: Int,
    onToggleRepeat: () -> Unit,
    autoMixEnabled: Boolean,
    onToggleAutoMix: () -> Unit,
    autoPlayEnabled: Boolean,
    onToggleAutoPlay: () -> Unit,
    onLoadMoreUpcoming: () -> Unit,
    onPlayFromQueue: (Song) -> Unit,
    onMoveToPlayNext: (Song) -> Unit,
    onRemoveFromQueue: (String) -> Unit,
    onMoveInQueue: (Int, Int) -> Unit,
    autoMixTargetSongId: String?,
    onToggleNextUp: () -> Unit
) {
    val density = LocalDensity.current
    val view = LocalView.current
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val nextUpSlideX = if (isSwitchingSubpages) (1f - subpageSlideFraction) * screenWidthPx else 0f
    val nextUpTransY = if (isSwitchingSubpages) 0f else with(density) { 36.dp.toPx() * (1f - nextUpProgress) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 190.dp, top = 136.dp)
            .graphicsLayer {
                translationX = nextUpSlideX
                translationY = nextUpTransY
                alpha = if (isSwitchingSubpages) subpageSlideFraction.coerceIn(0f, 1f) else nextUpProgress.coerceIn(0f, 1f)
            }
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
                Column(modifier = Modifier.fillMaxSize().padding(vertical = 12.dp)) {
                    NextUpControlPillsRow(
                        shuffleEnabled = shuffleEnabled,
                        onToggleShuffle = onToggleShuffle,
                        repeatMode = repeatMode,
                        onToggleRepeat = onToggleRepeat,
                        autoMixEnabled = autoMixEnabled,
                        onToggleAutoMix = onToggleAutoMix,
                        autoPlayEnabled = autoPlayEnabled,
                        onToggleAutoPlay = onToggleAutoPlay,
                        isDark = isDark
                    )
                    LaunchedEffect(Unit) {
                        onLoadMoreUpcoming()
                    }
                    Spacer(modifier = Modifier.height(48.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = AccentRed, strokeWidth = 2.5.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Finding more songs for your queue...", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (queueTitle != null) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Radio, contentDescription = null, tint = AccentRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(queueTitle, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                if (isArtistMixLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White.copy(alpha = 0.7f), strokeWidth = 2.dp)
                                }
                            }
                        }
                    }
                    item {
                        NextUpControlPillsRow(
                            shuffleEnabled = shuffleEnabled,
                            onToggleShuffle = onToggleShuffle,
                            repeatMode = repeatMode,
                            onToggleRepeat = onToggleRepeat,
                            autoMixEnabled = autoMixEnabled,
                            onToggleAutoMix = onToggleAutoMix,
                            autoPlayEnabled = autoPlayEnabled,
                            onToggleAutoPlay = onToggleAutoPlay,
                            isDark = isDark
                        )
                    }
                    itemsIndexed(upcoming, key = { _, s -> s.id }) { idx, s ->
                        if (idx >= upcoming.size - 4) {
                            LaunchedEffect(upcoming.size) {
                                onLoadMoreUpcoming()
                            }
                        }
                        QueueSongItem(
                            song = s,
                            index = idx,
                            totalCount = upcoming.size,
                            onPlay = { onPlayFromQueue(s) },
                            onMoveToPlayNext = { onMoveToPlayNext(s) },
                            onRemove = { onRemoveFromQueue(s.id) },
                            onMoveUp = { if (idx > 0) onMoveInQueue(idx, idx - 1) },
                            onMoveDown = { if (idx < upcoming.size - 1) onMoveInQueue(idx, idx + 1) },
                            isAutoMixTarget = (s.id == autoMixTargetSongId)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NowPlayingLyricsLayer(
    lyricsProgress: Float,
    isSwitchingSubpages: Boolean,
    subpageSlideFraction: Float,
    sideStretchFactor: Float,
    lyrics: List<LyricLine>,
    duration: Long,
    currentPosition: Long,
    isPlaying: Boolean,
    isLyricsActive: Boolean,
    controlsVisible: Boolean,
    controlsAlpha: Float,
    onToggleControls: () -> Unit,
    onLyricsDragged: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleLyrics: () -> Unit
) {
    val density = LocalDensity.current
    val view = LocalView.current
    val lyrStretchY = 1.0f + 0.08f * (1f - lyricsProgress)
    val lyrTransY = with(density) { 36.dp.toPx() * (1f - lyricsProgress) }

    // Apple Music Synced Word-by-Word Lyrics View
    val processedLyrics = remember(lyrics, duration) { LyricsProcessor.processSyncedLyrics(lyrics, trackDurationMs = duration) }

    // High-refresh (60/120fps) frame clock interpolating position from audio playback with ~35ms latency compensation
    var livePositionMs by remember { mutableLongStateOf(currentPosition) }
    LaunchedEffect(isPlaying, currentPosition, isLyricsActive) {
        livePositionMs = currentPosition
        if (isPlaying && isLyricsActive) {
            val anchorRealtime = android.os.SystemClock.elapsedRealtime()
            val anchorPos = currentPosition
            while (isLyricsActive) {
                withFrameMillis {
                    val elapsed = android.os.SystemClock.elapsedRealtime() - anchorRealtime
                    // Compensate for Bluetooth / speaker audio output buffer latency (~35ms)
                    livePositionMs = (anchorPos + elapsed - 35L).coerceAtLeast(0L)
                }
            }
        }
    }

    val lyricsTopPad by animateDpAsState(
        targetValue = if (controlsVisible && controlsAlpha > 0.05f) 110.dp else 16.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 320f),
        label = "lyricsTopPad"
    )
    val lyricsBottomPad by animateDpAsState(
        targetValue = if (controlsVisible && controlsAlpha > 0.05f) 180.dp else 16.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 320f),
        label = "lyricsBottomPad"
    )

    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val lyricsSlideX = if (isSwitchingSubpages) -subpageSlideFraction * screenWidthPx else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = lyricsTopPad, bottom = lyricsBottomPad)
            .graphicsLayer {
                translationX = lyricsSlideX
                translationY = if (isSwitchingSubpages) 0f else lyrTransY
                alpha = if (isSwitchingSubpages) (1f - subpageSlideFraction).coerceIn(0f, 1f) else lyricsProgress.coerceIn(0f, 1f)
            }
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
    ) {
        if (processedLyrics.isNotEmpty()) {
            val initialLineIdx = remember(processedLyrics) {
                LyricsProcessor.findActiveLineIndex(processedLyrics, currentPosition).coerceAtLeast(0)
            }
            val listState = rememberLazyListState(
                initialFirstVisibleItemIndex = (initialLineIdx - 1).coerceAtLeast(0)
            )
            val currentIdx by remember(processedLyrics) {
                derivedStateOf {
                    LyricsProcessor.findActiveLineIndex(processedLyrics, livePositionMs)
                }
            }

            val isLyricsDragged by listState.interactionSource.collectIsDraggedAsState()
            var manualScrolled by remember { mutableStateOf(false) }

            LaunchedEffect(isLyricsDragged) {
                if (isLyricsDragged) {
                    manualScrolled = true
                    onLyricsDragged()
                }
            }

            LaunchedEffect(manualScrolled) {
                if (manualScrolled) {
                    // Resume auto-scroll after ~6s idle
                    delay(6000L)
                    manualScrolled = false
                }
            }

            val anchorSpotDp = if (controlsVisible && controlsAlpha > 0.05f) 72.dp else 28.dp
            val anchorSpotPx = with(density) { anchorSpotDp.roundToPx() }

            // Auto-scroll active line smoothly into the fixed anchor spot without thrashing intermediate items
            LaunchedEffect(currentIdx, manualScrolled, isLyricsActive) {
                if (isLyricsActive && currentIdx in processedLyrics.indices && !manualScrolled && !isLyricsDragged) {
                    val item = listState.layoutInfo.visibleItemsInfo.find { it.index == currentIdx }
                    if (item != null) {
                        // Only perform animated delta spring once lyrics entry transition is completed/settled
                        if (lyricsProgress >= 0.88f) {
                            val delta = item.offset - anchorSpotPx
                            if (kotlin.math.abs(delta) > 2) {
                                listState.animateScrollBy(
                                    value = delta.toFloat(),
                                    animationSpec = spring(
                                        dampingRatio = 0.52f,
                                        stiffness = 260f
                                    )
                                )
                            }
                        }
                    } else {
                        // Instantly position at target index so LazyColumn doesn't compose/measure dozens of intermediate items during album cover morph
                        listState.scrollToItem(
                            index = (currentIdx - 1).coerceAtLeast(0),
                            scrollOffset = 0
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onToggleControls()
                    }
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        val fadeTopRatio = if (controlsVisible && controlsAlpha > 0.05f) 0.09f else 0.02f
                        val fadeBottomRatio = if (controlsVisible && controlsAlpha > 0.05f) 0.90f else 0.98f
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                (fadeTopRatio * 0.4f) to Color.Black.copy(alpha = 0.30f),
                                fadeTopRatio to Color.Black,
                                fadeBottomRatio to Color.Black,
                                (fadeBottomRatio + (1f - fadeBottomRatio) * 0.6f) to Color.Black.copy(alpha = 0.30f),
                                1.0f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            ) {
                val lyricsListBottomPad by animateDpAsState(
                    targetValue = if (controlsVisible && controlsAlpha > 0.05f) 160.dp else 24.dp,
                    label = "lyricsListBottomPad"
                )
                val lyricsListTopPad by animateDpAsState(
                    targetValue = if (controlsVisible && controlsAlpha > 0.05f) anchorSpotDp else 20.dp,
                    label = "lyricsListTopPad"
                )
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 28.dp, end = 28.dp),
                    contentPadding = PaddingValues(top = lyricsListTopPad, bottom = lyricsListBottomPad),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(processedLyrics, key = { idx, line -> "lyric_${line.timeMs}_$idx" }) { idx, line ->
                        val isActive = idx == currentIdx
                        val distance = if (currentIdx >= 0) kotlin.math.abs(idx - currentIdx) else 1
                        val cappedDistance = distance.coerceAtMost(4)

                        val targetScale = if (isActive) 1.05f else 1.0f
                        val targetAlpha = if (isActive) 1.0f else (0.50f - cappedDistance * 0.08f).coerceAtLeast(0.20f)
                        val targetBlur = if (isActive) 0.dp else (cappedDistance.coerceAtMost(2) * 0.8f).dp
                        val targetBounceY = if (isActive) (-6).dp else 0.dp

                        val lineBounceY by animateDpAsState(
                            targetValue = targetBounceY,
                            animationSpec = spring(
                                dampingRatio = 0.44f,
                                stiffness = 240f
                            ),
                            label = "lineBounceY_$idx"
                        )
                        val lineScale by animateFloatAsState(
                            targetValue = targetScale,
                            animationSpec = spring(
                                dampingRatio = 0.70f,
                                stiffness = 320f
                            ),
                            label = "lineScale_$idx"
                        )
                        val lineAlpha by animateFloatAsState(
                            targetValue = targetAlpha,
                            animationSpec = spring(
                                dampingRatio = 0.70f,
                                stiffness = 400f
                            ),
                            label = "lineAlpha_$idx"
                        )
                        val lineBlur by animateDpAsState(
                            targetValue = targetBlur,
                            animationSpec = tween(300),
                            label = "lineBlur_$idx"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    translationY = lineBounceY.toPx()
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
                                val breathScale = if (isActive) (1.0f + 0.012f * kotlin.math.sin(breakElapsed / 700.0).toFloat()) else 1.0f

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
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Lyrics not available", color = Color.White.copy(alpha = 0.5f), fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun NowPlayingThreeDotsMenu(
    showMiniMenu: Boolean,
    onDismissMiniMenu: () -> Unit,
    showSavePlaylistView: Boolean,
    onSetShowSavePlaylistView: (Boolean) -> Unit,
    sheetRootBounds: Rect?,
    threeDotButtonBounds: Rect?,
    song: Song,
    onGoToAlbum: (Song) -> Unit,
    onPlayArtistMix: (String) -> Unit,
    autoMixEnabled: Boolean,
    onToggleAutoMix: () -> Unit,
    sleepTimerState: com.beatles.player.viewmodel.SleepTimerState,
    onOpenSleepTimerDialog: () -> Unit,
    playlists: List<Playlist>,
    onCreatePlaylist: (String, Song) -> Unit,
    onAddToPlaylist: (String, Song) -> Unit
) {
    val miniMenuAnim by animateFloatAsState(
        targetValue = if (showMiniMenu) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.80f, stiffness = 420f),
        label = "miniMenuAnim"
    )

    if (miniMenuAnim > 0.01f) {
        val view = LocalView.current
        var newPlaylistName by remember { mutableStateOf("") }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f * miniMenuAnim))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onDismissMiniMenu()
                }
        ) {
            val isDark = AppTheme.isDark
            val currentCornerDp = lerp(19.dp, 24.dp, miniMenuAnim)
            val menuShape = RoundedCornerShape(currentCornerDp)
            val solidMenuBg = if (isDark) Color(0xFF1E2026) else Color(0xFFFFFFFF)
            val solidMenuBorder = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)

            val density = LocalDensity.current
            val endPaddingDp = remember(sheetRootBounds, threeDotButtonBounds) {
                val sb = sheetRootBounds
                val tb = threeDotButtonBounds
                if (sb != null && tb != null) {
                    with(density) { (sb.right - tb.right).toDp().coerceAtLeast(16.dp) }
                } else {
                    20.dp
                }
            }
            val bottomPaddingDp = remember(sheetRootBounds, threeDotButtonBounds) {
                val sb = sheetRootBounds
                val tb = threeDotButtonBounds
                if (sb != null && tb != null) {
                    with(density) { (sb.bottom - tb.top + 8.dp.toPx()).toDp().coerceAtLeast(180.dp) }
                } else {
                    246.dp
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = endPaddingDp, bottom = bottomPaddingDp)
                    .width(230.dp)
                    .graphicsLayer {
                        scaleX = miniMenuAnim
                        scaleY = miniMenuAnim
                        alpha = miniMenuAnim.coerceIn(0f, 1f)
                        transformOrigin = TransformOrigin(0.92f, 1.0f)
                    }
                    .shadow(12.dp, menuShape, spotColor = if (isDark) Color.Black.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.18f))
                    .clip(menuShape)
                    .background(solidMenuBg)
                    .border(1.dp, solidMenuBorder, menuShape)
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
                                    onSetShowSavePlaylistView(true)
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
                                    onDismissMiniMenu()
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

                        // Option 3: Play Artist Mix
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    Haptics.confirm(view)
                                    onDismissMiniMenu()
                                    onPlayArtistMix(song.artist)
                                }
                                .padding(horizontal = 16.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Radio, contentDescription = null, tint = AppTheme.fg(), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Play Artist Mix", color = AppTheme.fg(), fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
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

                        HorizontalDivider(color = AppTheme.fg(0.08f), modifier = Modifier.padding(horizontal = 14.dp))

                        // Option 5: Sleep Timer
                        val isTimerActive = sleepTimerState.mode != com.beatles.player.viewmodel.SleepTimerMode.NONE
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    Haptics.confirm(view)
                                    onDismissMiniMenu()
                                    onOpenSleepTimerDialog()
                                }
                                .padding(horizontal = 16.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Bedtime,
                                contentDescription = null,
                                tint = if (isTimerActive) Color(0xFFF82C38) else AppTheme.fg(),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Sleep Timer",
                                color = if (isTimerActive) Color(0xFFF82C38) else AppTheme.fg(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            if (isTimerActive) {
                                val label = when (sleepTimerState.mode) {
                                    com.beatles.player.viewmodel.SleepTimerMode.TIME -> "${sleepTimerState.remainingSeconds / 60}m"
                                    com.beatles.player.viewmodel.SleepTimerMode.SONGS -> "${sleepTimerState.remainingSongs} songs"
                                    com.beatles.player.viewmodel.SleepTimerMode.ALBUM -> "Album"
                                    com.beatles.player.viewmodel.SleepTimerMode.END_OF_TRACK -> "1 track"
                                    else -> "Active"
                                }
                                Text(label, color = Color(0xFFF82C38), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AppTheme.fg(0.4f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                } else {
                    // Save to Playlist inside popover
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onSetShowSavePlaylistView(false) }, modifier = Modifier.size(28.dp)) {
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
                                        onDismissMiniMenu()
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
                                                onDismissMiniMenu()
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
}


