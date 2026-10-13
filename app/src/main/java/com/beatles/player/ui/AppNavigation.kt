package com.beatles.player.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.beatles.player.R
import com.beatles.player.data.AdminTab
import com.beatles.player.data.ArtistTab
import com.beatles.player.data.ThemeMode
import com.beatles.player.data.UserRole
import com.beatles.player.ui.components.AddAlbumDialog
import com.beatles.player.ui.components.AlbumDetailSheet
import com.beatles.player.ui.components.FluidAlbumDetailOverlay
import com.beatles.player.ui.components.NowPlayingSheet
import com.beatles.player.ui.screens.*
import com.beatles.player.ui.theme.isDarkTheme
import com.beatles.player.viewmodel.PlayerViewModel
import com.beatles.player.viewmodel.Tab
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.foundation.isSystemInDarkTheme

private val GrayBlackBg = Color(0xFF141416)
private val AccentRed = Color(0xFFF82C38)
private val AccentPurple = Color(0xFF7B2FBE)
private val AccentBlue = Color(0xFF2471A3)

// Fluid deceleration easing curve [0.32, 0.72, 0, 1] for ultra-smooth transitions
val SmoothEase = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
val AppleEase = SmoothEase

// Frosted Translucent Styling: borderless, high reflection, translucent
private val TranslucentBarBg = Color(0xFF383842).copy(alpha = 0.28f)

@Composable
fun BeatlesApp(viewModel: PlayerViewModel) {
    val stateHolder = viewModel.uiState.collectAsStateWithLifecycle()
    val state by stateHolder
    // Position ticks (10 Hz) and AutoMix progress (60 Hz) must NOT invalidate the tab screens → big scroll-smoothness win.
    val screenState by remember {
        derivedStateOf {
            stateHolder.value.copy(
                currentPosition = 0L,
                duration = 0L,
                autoMixState = com.beatles.player.player.automix.AutoMixState()
            )
        }
    }
    var searchFocusRequested by remember { mutableStateOf(false) }
    val hazeState = remember { HazeState() }

    // Dynamic Header Collapse on Scroll – raw offset is only read in layout/draw lambdas (no recomposition per frame)
    val density = LocalDensity.current
    val maxHeaderOffset = with(density) { 110.dp.toPx() }
    var headerOffsetPx by remember { mutableFloatStateOf(0f) }
    var scrolledState by remember { mutableStateOf(false) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta == 0f) return Offset.Zero
                val newOffset = (headerOffsetPx + delta).coerceIn(-maxHeaderOffset, 0f)
                if (newOffset != headerOffsetPx) headerOffsetPx = newOffset
                // Hysteresis so the nav bar doesn't flip back and forth on minor scrolls
                if (newOffset < -130f) { if (!scrolledState) scrolledState = true }
                else if (newOffset > -20f) { if (scrolledState) scrolledState = false }
                return Offset.Zero
            }
        }
    }
    val animatedHeaderOffset = { headerOffsetPx }
    val headerAlphaProvider = { ((headerOffsetPx + maxHeaderOffset) / maxHeaderOffset).coerceIn(0f, 1f) }



    // Auto-dismiss download notice
    LaunchedEffect(state.downloadNotice) {
        if (state.downloadNotice != null) {
            delay(3500)
            viewModel.dismissDownloadNotice()
        }
    }

    // Reset header offset on page change so headers are never stuck swiped up
    LaunchedEffect(state.currentTab) {
        headerOffsetPx = 0f; scrolledState = false
    }

    // Android Predictive Back Gestures: fluid gestures that smoothly scale & dismiss overlays
    PredictiveBackHandler(enabled = state.showProfilePage) { progress ->
        try { progress.collect {}; viewModel.setProfileOpen(false) } catch (_: Exception) {}
    }
    PredictiveBackHandler(enabled = state.selectedAlbum != null) { progress ->
        try { progress.collect {}; viewModel.setSelectedAlbum(null) } catch (_: Exception) {}
    }
    PredictiveBackHandler(enabled = state.showAddAlbumDialog) { progress ->
        try { progress.collect {}; viewModel.setShowAddAlbumDialog(false) } catch (_: Exception) {}
    }
    PredictiveBackHandler(enabled = state.isExpanded) { progress ->
        try { progress.collect {}; viewModel.setExpanded(false) } catch (_: Exception) {}
    }

    // Remove the keyboard the moment the user navigates away from Search or opens an overlay
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    LaunchedEffect(state.isExpanded, state.currentTab, state.showSeeAllSection, state.selectedAlbum) {
        if (state.isExpanded || state.currentTab != Tab.SEARCH || state.showSeeAllSection != null || state.selectedAlbum != null) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }
    PredictiveBackHandler(enabled = state.currentTab == Tab.SEARCH) { progress ->
        try { progress.collect {}; viewModel.setTab(Tab.HOME) } catch (_: Exception) {}
    }
    PredictiveBackHandler(enabled = state.showSeeAllSection != null) { progress ->
        try { progress.collect {}; viewModel.setSeeAllSection(null) } catch (_: Exception) {}
    }

    val currentRoleColor = when (state.userRole) {
        UserRole.LISTENER -> AccentRed
        UserRole.ARTIST -> AccentPurple
        UserRole.ADMIN -> AccentBlue
    }

    var splashVisible by remember { mutableStateOf(true) }
    val whiteWavesAlpha = remember { Animatable(1.0f) }
    val redLineScaleX = remember { Animatable(1.0f) }
    val redLineScaleY = remember { Animatable(1.0f) }
    val launchAlpha = remember { Animatable(1.0f) }

    LaunchedEffect(Unit) {
        delay(220)
        launch {
            whiteWavesAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 280, easing = LinearEasing)
            )
        }
        launch {
            // Expands horizontally first:
            redLineScaleX.animateTo(
                targetValue = 42f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
            // Then vertically:
            redLineScaleY.animateTo(
                targetValue = 55f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
        }
        delay(480)
        launch {
            launchAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 400, easing = LinearEasing)
            )
            splashVisible = false
        }
    }

    BackHandler(enabled = state.onboardStep in 2..3) {
        viewModel.setOnboardStep(state.onboardStep - 1)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (state.onboardStep) {
        1 -> OnboardStep1(onNext = { name -> viewModel.setUserName(name); viewModel.setOnboardStep(2) })
        2 -> OnboardStepLanguage(
            initialLanguages = state.selectedLanguages,
            onBack = { viewModel.setOnboardStep(1) },
            onNext = { langs ->
                viewModel.setSelectedLanguages(langs)
                viewModel.setOnboardStep(3)
            }
        )
        3 -> OnboardStepArtists(
            selectedLanguage = state.selectedLanguage,
            onBack = { viewModel.setOnboardStep(2) },
            onNext = { artists ->
                viewModel.setSelectedArtists(artists)
                viewModel.setOnboardStep(4)
            }
        )
        4 -> OnboardStepReady(name = state.userName, onDone = { viewModel.finishOnboarding() }, hasData = state.recommendations.isNotEmpty())
        else -> {
            val isDark = isDarkTheme(state.themeMode)
            val appBg = if (isDark) GrayBlackBg else Color(0xFFF6F7F9)
            val translucentBarBg = if (isDark) Color(0xFF16171D).copy(alpha = 0.65f) else Color(0xFFFFFFFF).copy(alpha = 0.72f)
            val translucentBarBorder = if (isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.10f)
            val translucentInnerPill = if (isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.08f)
            val textPrimary = if (isDark) Color.White else Color(0xFF141416)
            val textSecondary = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF6B7280)
            val navInactiveTint = if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF8E8E93)
            val headerAvatarBg = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
            val searchInputColor = if (isDark) Color.White else Color(0xFF141416)
            val searchHintColor = if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF9CA3AF)
            val noticeBg = if (isDark) Color(0xFF222226).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f)

            // Dynamic edge-to-edge status bar coloring that responds to active album/player
            val windowView = androidx.compose.ui.platform.LocalView.current
            DisposableEffect(state.isExpanded, state.selectedAlbum, isDark) {
                val activity = windowView.context as? android.app.Activity
                    ?: (windowView.context as? android.content.ContextWrapper)?.baseContext as? android.app.Activity
                if (activity != null) {
                    val window = activity.window
                    val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                    window.statusBarColor = android.graphics.Color.TRANSPARENT
                    window.navigationBarColor = android.graphics.Color.TRANSPARENT
                    val isSheetOpen = state.isExpanded || state.selectedAlbum != null
                    insetsController.isAppearanceLightStatusBars = !isSheetOpen && !isDark
                    insetsController.isAppearanceLightNavigationBars = !isDark
                }
                onDispose {}
            }


            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(appBg)
                    .nestedScroll(nestedScrollConnection)
            ) {
                
                // Base Tab content layer (with live backdrop blur sampling)
                Box(modifier = Modifier.fillMaxSize().haze(state = hazeState)) {
                    when (state.userRole) {
                        UserRole.LISTENER -> {
                            AnimatedContent(
                                targetState = state.currentTab,
                                transitionSpec = {
                                    if (initialState == Tab.SEARCH || targetState == Tab.SEARCH) {
                                        (fadeIn(animationSpec = tween(160, easing = LinearEasing)) + 
                                         scaleIn(initialScale = 0.96f, animationSpec = tween(180, easing = AppleEase))) togetherWith
                                        (fadeOut(animationSpec = tween(140, easing = LinearEasing)) + 
                                         scaleOut(targetScale = 0.96f, animationSpec = tween(160, easing = AppleEase)))
                                    } else {
                                        val direction = if (targetState.ordinal > initialState.ordinal) {
                                            slideInHorizontally(animationSpec = tween(240, easing = AppleEase)) { it / 2 } + fadeIn(tween(180)) togetherWith
                                                    slideOutHorizontally(animationSpec = tween(240, easing = AppleEase)) { -it / 2 } + fadeOut(tween(160))
                                        } else {
                                            slideInHorizontally(animationSpec = tween(240, easing = AppleEase)) { -it / 2 } + fadeIn(tween(180)) togetherWith
                                                    slideOutHorizontally(animationSpec = tween(240, easing = AppleEase)) { it / 2 } + fadeOut(tween(160))
                                        }
                                        direction.using(SizeTransform(clip = false))
                                    }
                                },
                                label = "listener_smooth_tab", modifier = Modifier.fillMaxSize()
                            ) { targetTab ->
                                Box(modifier = Modifier.fillMaxSize()) {
                                    when (targetTab) {
                                        Tab.HOME -> HomeScreen(
                                            state = screenState,
                                            onPlay = { s, pl -> viewModel.playSong(s, pl) },
                                            onPlayArtistMix = { artist, songs -> viewModel.playArtistMix(artist, songs) },
                                            onSeeAll = { section -> viewModel.setSeeAllSection(section.ifEmpty { null }) },
                                            onSelectAlbum = { alb, bounds -> viewModel.setSelectedAlbum(alb, bounds) },
                                            onPlayAlbumSet = { viewModel.playAlbumSet(it) },
                                            onLoadMoreTopSongs = { viewModel.loadMoreTopSongs() },
                                            onRefresh = { viewModel.refreshHomeScreen() }
                                        )
                                        Tab.EXPLORE -> ExploreScreen(
                                            state = screenState,
                                            headerOffset = animatedHeaderOffset,
                                            headerAlpha = headerAlphaProvider,
                                            onPlay = { s, pl -> viewModel.playSong(s, pl) },
                                            onSearch = { viewModel.search(it); viewModel.setTab(Tab.SEARCH) }
                                        )
                                        Tab.LIBRARY -> LibraryScreen(
                                            state = screenState,
                                            viewModel = viewModel,
                                            headerOffset = animatedHeaderOffset,
                                            headerAlpha = headerAlphaProvider,
                                            hazeState = hazeState,
                                            onPlay = { s, pl -> viewModel.playSong(s, pl) }
                                        )
                                        Tab.SEARCH -> SearchScreen(
                                            state = screenState,
                                            focusRequested = searchFocusRequested,
                                            onSearch = { viewModel.search(it) },
                                            onQueryChange = { viewModel.fetchSearchSuggestions(it) },
                                            onSelectAlbum = { viewModel.setSelectedAlbum(it) },
                                            onPlay = { s, _ -> viewModel.playSong(s, playlist = null, isFromSearch = true) },
                                            onMoveToPlayNext = { s -> viewModel.moveSongToPlayNext(s) }
                                        )
                                    }
                                }
                            }
                        }

                        UserRole.ARTIST -> {
                            ArtistScreen(state = state, viewModel = viewModel, onPlay = { s, pl -> viewModel.playSong(s, pl) })
                        }

                        UserRole.ADMIN -> {
                            AdminScreen(state = state, viewModel = viewModel)
                        }
                    }
                }


                // ═══ UPPER PROGRESSIVE BLUR OVERLAY (iOS 26 Full-Screen Progressive Header Blur) ═══
                if (state.selectedAlbum == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .align(Alignment.TopCenter)
                            .graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0.0f to Color.Black,
                                        0.50f to Color.Black.copy(alpha = 0.85f),
                                        0.85f to Color.Black.copy(alpha = 0.30f),
                                        1.0f to Color.Transparent
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                            .hazeChild(state = hazeState)
                    )
                }

                // ═══ 2. STICKY TOP HEADER (Floating Logo + User Avatar) ═══
                if (state.showSeeAllSection == null && state.currentTab != Tab.SEARCH && state.selectedAlbum == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .offset { IntOffset(0, headerOffsetPx.roundToInt()) }
                            .graphicsLayer { alpha = headerAlphaProvider() }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = if (isDark) R.drawable.beatles_logo else R.drawable.beatles_logo_light),
                                contentDescription = "Beatles",
                                modifier = Modifier.size(40.dp)
                            )
                            
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(headerAvatarBg)
                                    .clickable { viewModel.setProfileOpen(true) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    when (state.userRole) {
                                        UserRole.LISTENER -> Icons.Default.Person
                                        UserRole.ARTIST -> Icons.Default.Mic
                                        UserRole.ADMIN -> Icons.Default.Security
                                    },
                                    contentDescription = "Profile",
                                    tint = currentRoleColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Download / Action Notice Banner
                if (state.downloadNotice != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 96.dp)
                            .padding(horizontal = 24.dp)
                            .clip(RoundedCornerShape(50))
                            .background(noticeBg)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            state.downloadNotice!!,
                            color = textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Bottom: Mini Player + Borderless Translucent Nav
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .imePadding()
                        .pointerInput(Unit) {
                            detectTapGestures { /* consume touches to prevent mistouches to the list underneath */ }
                        }
                ) {
                    val isScrolling = scrolledState
                    val scrollShrinkRaw by animateFloatAsState(
                        targetValue = if (isScrolling) 1f else 0f,
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        label = "scrollShrinkProgress"
                    )
                    val scrollShrinkProgress = scrollShrinkRaw.coerceIn(0f, 1f)

                    val bottomBarHeight = 56.dp
                    val circleSize = 56.dp
                    val barCorner = 28.dp
                    val navIconSize = 24.dp
                    val navTextSize = 9.sp

                    val isSearch = state.currentTab == Tab.SEARCH
                    val hasSong = state.currentSong != null

                    val searchExpandProgress by animateFloatAsState(
                        targetValue = if (isSearch) 1f else 0f,
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 240, easing = AppleEase),
                        label = "searchMorphProgress"
                    )

                    // Scroll morph (0..1, clamped: spring overshoot must never reach layout as negative padding / fraction > 1)
                    val morph = if (hasSong) (scrollShrinkProgress.coerceIn(0f, 1f) * (1f - searchExpandProgress.coerceIn(0f, 1f))) else 0f
                    val combined = maxOf(searchExpandProgress.coerceIn(0f, 1f), morph)
                    val miniView = androidx.compose.ui.platform.LocalView.current

                    // Shared mini-player pill: full width above the nav bar, compact capsule in the middle while scrolling
                    val miniPlayerPill: @Composable (Modifier, Boolean) -> Unit = { mod, compact ->
                        val song = state.currentSong
                        if (song != null) {
                            BoxWithConstraints(modifier = mod) {
                                if (maxWidth < 100.dp) return@BoxWithConstraints
                                var miniDragX by remember { mutableFloatStateOf(0f) }
                                val pillCorner = 24.dp
                                val pillArt = if (compact) 26.dp else 33.dp

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(pillCorner))
                                        .hazeChild(state = hazeState, shape = RoundedCornerShape(pillCorner))
                                        .border(0.5.dp, translucentBarBorder, RoundedCornerShape(pillCorner))
                                        .background(translucentBarBg)
                                        .pointerInput(Unit) {
                                            // Angle math: a swipe only counts when it is within ~20° of horizontal
                                            // (|dx| > 2.75·|dy|). Diagonal / vertical drags never change the song.
                                            awaitEachGesture {
                                                val down = awaitFirstDown(requireUnconsumed = false)
                                                val slop = viewConfiguration.touchSlop
                                                var dx = 0f
                                                var dy = 0f
                                                var mode = 0 // 0 = undecided, 1 = horizontal swipe, -1 = ignore
                                                do {
                                                    val event = awaitPointerEvent()
                                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                                    val d = change.positionChange()
                                                    dx += d.x
                                                    dy += d.y
                                                    if (mode == 0 && (kotlin.math.abs(dx) > slop || kotlin.math.abs(dy) > slop)) {
                                                        mode = if (kotlin.math.abs(dx) > kotlin.math.abs(dy) * 2.75f) 1 else -1
                                                    }
                                                    if (mode == 1) change.consume()
                                                } while (event.changes.any { it.pressed })
                                                if (mode == 1) {
                                                    if (dx < -80f) {
                                                        com.beatles.player.ui.components.Haptics.confirm(miniView)
                                                        viewModel.playNext()
                                                    } else if (dx > 80f) {
                                                        com.beatles.player.ui.components.Haptics.confirm(miniView)
                                                        viewModel.playPrevious()
                                                    }
                                                }
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) { viewModel.setExpanded(true) }
                                            .padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                    if (song.coverUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = song.coverUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(pillArt).clip(CircleShape)
                                        )
                                    } else {
                                        Box(modifier = Modifier.size(pillArt).clip(CircleShape).background(Color.Gray))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            song.title,
                                            color = textPrimary,
                                            fontSize = if (compact) 12.sp else 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        if (!compact) {
                                            Text(song.artist, color = textSecondary, fontSize = 11.sp, maxLines = 1)
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            com.beatles.player.ui.components.Haptics.click(miniView)
                                            viewModel.togglePlayPause()
                                        },
                                        modifier = Modifier.size(if (compact) 30.dp else 36.dp)
                                    ) {
                                        if (state.isLoadingStream) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = textPrimary,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = textPrimary,
                                                modifier = Modifier.size(if (compact) 18.dp else 22.dp)
                                            )
                                        }
                                    }
                                    if (!compact) {
                                        IconButton(
                                            onClick = {
                                                com.beatles.player.ui.components.Haptics.click(miniView)
                                                viewModel.playNext()
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.SkipNext, contentDescription = null, tint = textPrimary, modifier = Modifier.size(22.dp))
                                        }
                                    }
                                }

                                // Seek indicator line (isolated from BeatlesApp recomposition)
                                MiniPlayerSeekLine(
                                    position = { stateHolder.value.currentPosition },
                                    duration = { stateHolder.value.duration },
                                    isMixing = { stateHolder.value.autoMixState.isMixing },
                                    isDark = isDark,
                                    modifier = Modifier.align(Alignment.BottomStart)
                                )
                            }
                        }
                    }
                }

                    // Top Mini Player: fades out when entering search mode or when the page is scrolled (capsule takes over)
                    val topPlayerAlpha = (1f - combined * 2.5f).coerceIn(0f, 1f)
                    if (hasSong && !state.isExpanded && topPlayerAlpha > 0.01f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    alpha = topPlayerAlpha
                                    scaleX = 0.92f + 0.08f * topPlayerAlpha
                                    scaleY = 0.92f + 0.08f * topPlayerAlpha
                                }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            miniPlayerPill(
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(49.dp),
                                false
                            )
                        }
                    }

                    // Bottom Navigation Bar — Borderless Translucent Glass
                    when (state.userRole) {
                        UserRole.LISTENER -> {
                            val tabs = listOf(
                                Triple(Tab.HOME, Icons.Default.Home, "Home"),
                                Triple(Tab.EXPLORE, Icons.Default.Explore, "Explore"),
                                Triple(Tab.LIBRARY, Icons.Default.GridView, "Library")
                            )

                            var selectedMainTabIdx by remember { mutableIntStateOf(0) }
                            if (state.currentTab != Tab.SEARCH) {
                                val idx = tabs.indexOfFirst { it.first == state.currentTab }
                                if (idx >= 0) selectedMainTabIdx = idx
                            }

                            var bottomSearchText by remember { mutableStateOf(state.searchQuery) }
                            val bottomSearchFocusRequester = remember { FocusRequester() }

                            LaunchedEffect(state.searchQuery) {
                                if (state.searchQuery != bottomSearchText) bottomSearchText = state.searchQuery
                            }

                            LaunchedEffect(state.currentTab, searchExpandProgress) {
                                if (state.currentTab == Tab.SEARCH) {
                                    if (searchExpandProgress >= 0.15f) {
                                        try {
                                            bottomSearchFocusRequester.requestFocus()
                                            keyboardController?.show()
                                        } catch (_: Exception) {}
                                    }
                                } else {
                                    bottomSearchText = ""
                                    viewModel.onSearchQueryChanged("")
                                }
                            }

                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .padding(bottom = 14.dp, top = 2.dp)
                            ) {
                                val totalWidth = constraints.maxWidth.toFloat()
                                val circlePx = with(density) { circleSize.toPx() }
                                val gapPx = with(density) { 8.dp.toPx() }

                                val normalLeftPillPx = (totalWidth - circlePx - gapPx).coerceAtLeast(circlePx)
                                val searchModeMiddleWidthPx = if (hasSong) {
                                    (totalWidth - circlePx * 2f - gapPx * 2f).coerceAtLeast(circlePx)
                                } else {
                                    (totalWidth - circlePx - gapPx).coerceAtLeast(circlePx)
                                }

                                val leftWidthPx = androidx.compose.ui.util.lerp(normalLeftPillPx, circlePx, combined)
                                val searchWidthPx = androidx.compose.ui.util.lerp(circlePx, searchModeMiddleWidthPx, searchExpandProgress)

                                Row(
                                    modifier = Modifier.fillMaxWidth().height(bottomBarHeight),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left Pill: shrinks to circle with active tab icon in search mode, expands to full tabs in normal mode
                                    Box(
                                        modifier = Modifier
                                            .width(with(density) { leftWidthPx.toDp() })
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(barCorner))
                                            .hazeChild(state = hazeState, shape = RoundedCornerShape(barCorner))
                                            .border(0.5.dp, translucentBarBorder, RoundedCornerShape(barCorner))
                                            .background(translucentBarBg)
                                            .padding(4.dp)
                                    ) {
                                        // Left Pill Content: Both layers blend smoothly with crossfade
                                        val activeTab = tabs[selectedMainTabIdx]
                                        val compactAlpha = ((combined - 0.20f) / 0.65f).coerceIn(0f, 1f)
                                        val fullAlpha = (1f - combined * 2.2f).coerceIn(0f, 1f)

                                        // 1. Full navigation tabs with sliding indicator (fades out smoothly as combined increases)
                                        if (fullAlpha > 0.01f) {
                                            val availableForTabs = (leftWidthPx - with(density) { 8.dp.toPx() }).coerceAtLeast(1f)
                                            val tabWidth = availableForTabs / tabs.size.toFloat()
                                            val indicatorFraction by animateFloatAsState(
                                                targetValue = selectedMainTabIdx.toFloat(),
                                                animationSpec = androidx.compose.animation.core.tween(durationMillis = 200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                                                label = "smoothNavIndicator"
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .graphicsLayer { alpha = fullAlpha }
                                            ) {
                                                // Sliding active indicator
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxHeight()
                                                        .width(with(density) { (tabWidth - 2).coerceAtLeast(1f).toDp() })
                                                        .offset { IntOffset((indicatorFraction * tabWidth).roundToInt(), 0) }
                                                        .clip(RoundedCornerShape((barCorner - 4.dp).coerceAtLeast(4.dp)))
                                                        .background(translucentInnerPill)
                                                )

                                                Row(
                                                    modifier = Modifier.fillMaxSize(),
                                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    tabs.forEach { (tab, icon, label) ->
                                                        val isActive = state.currentTab == tab
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .fillMaxHeight()
                                                                .clickable(
                                                                    interactionSource = remember { MutableInteractionSource() },
                                                                    indication = null
                                                                ) { viewModel.setTab(tab) },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Column(
                                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                                verticalArrangement = Arrangement.Center
                                                            ) {
                                                                Icon(
                                                                    icon,
                                                                    contentDescription = null,
                                                                    tint = if (isActive) AccentRed else navInactiveTint,
                                                                    modifier = Modifier.size(navIconSize)
                                                                )
                                                                if (combined < 0.25f) {
                                                                    Text(
                                                                        label,
                                                                        fontSize = navTextSize,
                                                                        fontWeight = FontWeight.Medium,
                                                                        color = if (isActive) AccentRed else textSecondary,
                                                                        modifier = Modifier.offset(y = (-1).dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // 2. Compact active tab button (fades in centered as left pill contracts to circle)
                                        if (compactAlpha > 0.01f) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .graphicsLayer {
                                                        alpha = compactAlpha
                                                        scaleX = 0.85f + 0.15f * compactAlpha
                                                        scaleY = 0.85f + 0.15f * compactAlpha
                                                    }
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) {
                                                        if (isSearch) viewModel.setTab(activeTab.first) else headerOffsetPx = 0f
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    activeTab.second,
                                                    contentDescription = activeTab.third,
                                                    tint = AccentRed,
                                                    modifier = Modifier.size(navIconSize)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(with(density) { gapPx.toDp() }))

                                    // Scroll mode: mini player sits in the middle (nav pill → circle on the left, search → circle on the right)
                                    if (hasSong && morph > 0.01f) {
                                        val middleWidth = searchModeMiddleWidthPx * morph
                                        Box(
                                            modifier = Modifier
                                                .width(with(density) { middleWidth.toDp() })
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(barCorner))
                                                .graphicsLayer { alpha = morph }
                                        ) {
                                            if (middleWidth >= with(density) { 100.dp.toPx() }) {
                                                miniPlayerPill(Modifier.fillMaxSize(), true)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(with(density) { (gapPx * morph).toDp() }))
                                    }

                                    // Search Pill: contracts to circle button (normal mode) or expands into search bar (search mode)
                                    Box(
                                        modifier = Modifier
                                            .width(with(density) { searchWidthPx.toDp() })
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(barCorner))
                                            .hazeChild(state = hazeState, shape = RoundedCornerShape(barCorner))
                                            .border(0.5.dp, translucentBarBorder, RoundedCornerShape(barCorner))
                                            .background(translucentBarBg)
                                            .padding(4.dp)
                                    ) {
                                        // Compact search button (normal mode)
                                        if (searchExpandProgress < 0.6f) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .graphicsLayer { alpha = (1f - searchExpandProgress * 1.8f).coerceIn(0f, 1f) }
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) {
                                                        viewModel.setTab(Tab.SEARCH)
                                                        keyboardController?.show()
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Search,
                                                    contentDescription = "Search",
                                                    tint = textSecondary,
                                                    modifier = Modifier.size(navIconSize)
                                                )
                                            }
                                        }

                                        // Expanded Search input (search mode)
                                        if (searchExpandProgress > 0.10f) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .graphicsLayer {
                                                        alpha = ((searchExpandProgress - 0.10f) / 0.85f).coerceIn(0f, 1f)
                                                    }
                                                    .padding(horizontal = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Search,
                                                    contentDescription = null,
                                                    tint = AccentRed,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(modifier = Modifier.weight(1f)) {
                                                    BasicTextField(
                                                        value = bottomSearchText,
                                                        onValueChange = {
                                                            bottomSearchText = it
                                                            viewModel.onSearchQueryChanged(it)
                                                        },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .focusRequester(bottomSearchFocusRequester),
                                                        textStyle = TextStyle(color = searchInputColor, fontSize = 14.sp, fontWeight = FontWeight.Normal),
                                                        singleLine = true,
                                                        cursorBrush = SolidColor(AccentRed),
                                                        decorationBox = { inner ->
                                                            if (bottomSearchText.isEmpty()) {
                                                                Text("Search songs, artists, albums...", color = searchHintColor, fontSize = 13.sp)
                                                            }
                                                            inner()
                                                        }
                                                    )
                                                }
                                                if (bottomSearchText.isNotEmpty()) {
                                                    IconButton(
                                                        onClick = {
                                                            bottomSearchText = ""
                                                            viewModel.clearSearch()
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Close,
                                                            contentDescription = "Clear",
                                                            tint = textSecondary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Right: Mini Player circular button when searching (and song exists)
                                    if (hasSong && searchExpandProgress > 0.001f) {
                                        val animatedRightPillWidth = circleSize * searchExpandProgress
                                        Spacer(modifier = Modifier.width(with(density) { (gapPx * searchExpandProgress).toDp() }))
                                        Box(
                                            modifier = Modifier
                                                .width(animatedRightPillWidth)
                                                .fillMaxHeight()
                                                .graphicsLayer {
                                                    alpha = searchExpandProgress
                                                }
                                                .clip(CircleShape)
                                                .hazeChild(state = hazeState, shape = CircleShape)
                                                .border(0.5.dp, translucentBarBorder, CircleShape)
                                                .background(translucentBarBg)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    viewModel.setExpanded(true)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val artSize = (circleSize - 16.dp).coerceAtLeast(24.dp)
                                            val coverUrl = state.currentSong!!.coverUrl
                                            if (coverUrl.isNotEmpty() && animatedRightPillWidth >= artSize) {
                                                AsyncImage(
                                                    model = coverUrl,
                                                    contentDescription = "Now Playing",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.size(artSize).clip(CircleShape)
                                                )
                                            } else if (animatedRightPillWidth >= artSize) {
                                                Box(modifier = Modifier.size(artSize).clip(CircleShape).background(Color.Gray))
                                            }

                                            // Mini play indicator ring if playing
                                            if (state.isPlaying) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(artSize + 4.dp)
                                                        .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        UserRole.ARTIST -> {
                            val tabs = listOf(
                                Triple(ArtistTab.FEED, Icons.Default.Dashboard, "Feed"),
                                Triple(ArtistTab.UPLOAD, Icons.Default.CloudUpload, "Upload"),
                                Triple(ArtistTab.ANALYTICS, Icons.Default.BarChart, "Stats")
                            )
                            val currentIdx = tabs.indexOfFirst { it.first == state.artistTab }.coerceAtLeast(0)
                            val indicatorFraction by animateFloatAsState(
                                targetValue = currentIdx.toFloat(),
                                animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f), label = "artistIndicator"
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .padding(bottom = 14.dp, top = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BoxWithConstraints(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(bottomBarHeight)
                                        .clip(RoundedCornerShape(barCorner))
                                        .hazeChild(state = hazeState, shape = RoundedCornerShape(barCorner))
                                        .background(translucentBarBg)
                                        .padding(4.dp)
                                ) {
                                    val tabWidth = constraints.maxWidth / tabs.size.toFloat()
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(with(density) { (tabWidth - 2).toDp() })
                                            .offset { IntOffset((indicatorFraction * tabWidth).roundToInt(), 0) }
                                            .clip(RoundedCornerShape(barCorner - 4.dp))
                                            .background(AccentPurple.copy(alpha = 0.35f))
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        tabs.forEach { (tab, icon, label) ->
                                            val isActive = state.artistTab == tab
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) { viewModel.setArtistTab(tab) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                                    Icon(icon, contentDescription = null, tint = if (isActive) AccentPurple else navInactiveTint, modifier = Modifier.size(navIconSize))
                                                    if (scrollShrinkProgress < 0.8f) {
                                                        Text(label, fontSize = navTextSize, fontWeight = FontWeight.Medium, color = if (isActive) AccentPurple else textSecondary, modifier = Modifier.offset(y = (-1).dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(circleSize)
                                        .clip(CircleShape)
                                        .border(0.5.dp, translucentBarBorder, CircleShape)
                                        .background(translucentBarBg)
                                        .clickable { viewModel.setShowAddAlbumDialog(true) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Album", tint = AccentPurple, modifier = Modifier.size(navIconSize + 4.dp))
                                }
                            }
                        }

                        UserRole.ADMIN -> {
                            val tabs = listOf(
                                Triple(AdminTab.DASHBOARD, Icons.Default.Analytics, "Overview"),
                                Triple(AdminTab.CONTENT, Icons.Default.LibraryMusic, "Catalog"),
                                Triple(AdminTab.USERS, Icons.Default.Group, "Users")
                            )
                            val currentIdx = tabs.indexOfFirst { it.first == state.adminTab }.coerceAtLeast(0)
                            val indicatorFraction by animateFloatAsState(
                                targetValue = currentIdx.toFloat(),
                                animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f), label = "adminIndicator"
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .padding(bottom = 14.dp, top = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BoxWithConstraints(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(bottomBarHeight)
                                        .clip(RoundedCornerShape(barCorner))
                                        .border(0.5.dp, translucentBarBorder, RoundedCornerShape(barCorner))
                                        .background(translucentBarBg)
                                        .padding(4.dp)
                                ) {
                                    val tabWidth = constraints.maxWidth / tabs.size.toFloat()
                                    if (state.adminTab != AdminTab.SETTINGS) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .width(with(density) { (tabWidth - 2).toDp() })
                                                .offset { IntOffset((indicatorFraction * tabWidth).roundToInt(), 0) }
                                                .clip(RoundedCornerShape(barCorner - 4.dp))
                                                .background(AccentBlue.copy(alpha = 0.35f))
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        tabs.forEach { (tab, icon, label) ->
                                            val isActive = state.adminTab == tab
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) { viewModel.setAdminTab(tab) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                                    Icon(icon, contentDescription = null, tint = if (isActive) AccentBlue else navInactiveTint, modifier = Modifier.size(navIconSize))
                                                    if (scrollShrinkProgress < 0.8f) {
                                                        Text(label, fontSize = navTextSize, fontWeight = FontWeight.Medium, color = if (isActive) AccentBlue else textSecondary, modifier = Modifier.offset(y = (-1).dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(circleSize)
                                        .clip(CircleShape)
                                        .border(0.5.dp, translucentBarBorder, CircleShape)
                                        .background(if (state.adminTab == AdminTab.SETTINGS) AccentBlue else translucentBarBg)
                                        .clickable { viewModel.setAdminTab(AdminTab.SETTINGS) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = if (state.adminTab == AdminTab.SETTINGS) Color.White else textSecondary, modifier = Modifier.size(navIconSize))
                                }
                            }
                        }
                    }
                }

                // Add Album Dialog Overlay
                AnimatedVisibility(
                    visible = state.showAddAlbumDialog,
                    enter = fadeIn(tween(200, easing = AppleEase)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220, easing = AppleEase)),
                    exit = fadeOut(tween(180, easing = AppleEase)) + scaleOut(targetScale = 0.94f, animationSpec = tween(180, easing = AppleEase))
                ) {
                    AddAlbumDialog(
                        state = state,
                        viewModel = viewModel,
                        onClose = { viewModel.setShowAddAlbumDialog(false) }
                    )
                }

                // Fluid Album Detail Overlay (Expanding continuous shared-element animation from album cover)
                FluidAlbumDetailOverlay(
                    album = state.selectedAlbum,
                    sourceBounds = state.selectedAlbumBounds,
                    state = state,
                    viewModel = viewModel,
                    onClose = { viewModel.setSelectedAlbum(null) },
                    onPlay = { s, pl -> viewModel.playSong(s, pl) }
                )

                // Profile Screen Overlay (Native iOS push slide transition)
                AnimatedVisibility(
                    visible = state.showProfilePage,
                    enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(240, easing = AppleEase)) + fadeIn(tween(200, easing = AppleEase)),
                    exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(220, easing = AppleEase)) + fadeOut(tween(180, easing = AppleEase))
                ) {
                    ProfileScreen(
                        state = state,
                        viewModel = viewModel,
                        onClose = { viewModel.setProfileOpen(false) }
                    )
                }

                // Full screen player overlay
                if (state.currentSong != null) {
                    val density = LocalDensity.current
                    var offsetY by remember { mutableFloatStateOf(if (state.isExpanded) 0f else with(density) { 2000.dp.toPx() }) }
                    LaunchedEffect(state.isExpanded) {
                        offsetY = if (state.isExpanded) 0f else with(density) { 2000.dp.toPx() }
                    }
                    val animatedOffset by animateFloatAsState(
                        targetValue = offsetY,
                        animationSpec = tween(durationMillis = 280, easing = AppleEase), label = "playerOffset"
                    )

                    if (state.isExpanded || animatedOffset < with(density) { 1990.dp.toPx() }) {
                        val maxOff = with(density) { 2000.dp.toPx() }
                        val expandFraction = (1f - (animatedOffset / maxOff)).coerceIn(0f, 1f)
                        val playerScale = 0.90f + 0.10f * expandFraction
                        val playerCorner = (28.dp * (1f - expandFraction)).coerceAtLeast(0.dp)

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .offset { IntOffset(0, animatedOffset.roundToInt()) }
                                .graphicsLayer {
                                    scaleX = playerScale
                                    scaleY = playerScale
                                    alpha = expandFraction
                                }
                                .clip(RoundedCornerShape(playerCorner))
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onDragEnd = {
                                            if (offsetY > 400f) {
                                                viewModel.setExpanded(false)
                                                offsetY = 2000f * density.density
                                            } else {
                                                offsetY = 0f
                                            }
                                        },
                                        onVerticalDrag = { _, dragAmount ->
                                            if (dragAmount > 0) offsetY = (offsetY + dragAmount).coerceAtLeast(0f)
                                        }
                                    )
                                }
                        ) {
                            NowPlayingSheet(
                                song = state.currentSong!!,
                                isPlaying = state.isPlaying,
                                currentPosition = state.currentPosition,
                                duration = state.duration,
                                shuffleEnabled = state.shuffleModeEnabled,
                                repeatMode = state.repeatMode,
                                isFavorite = state.isFavorite,
                                lyrics = state.lyrics,
                                playlist = state.playlist,
                                showLyrics = state.showLyrics && state.showSeeAllSection != "next_up",
                                showNextUp = state.showSeeAllSection == "next_up",
                                playlists = state.playlists,
                                isLoadingStream = state.isLoadingStream,
                                autoMixActive = state.autoMixState.isMixing,
                                autoMixProgress = state.autoMixState.progress,
                                autoMixLabel = state.autoMixState.label,
                                onClose = { viewModel.setExpanded(false) },
                                onTogglePlay = { viewModel.togglePlayPause() },
                                onNext = { viewModel.playNext() },
                                onPrevious = { viewModel.playPrevious() },
                                onSeek = { viewModel.seekTo(it) },
                                onToggleShuffle = { viewModel.toggleShuffle() },
                                onToggleRepeat = { viewModel.toggleRepeat() },
                                onToggleFavorite = { viewModel.toggleFavorite() },
                                onToggleLyrics = { 
                                    if (state.showSeeAllSection == "next_up") viewModel.setSeeAllSection(null)
                                    viewModel.toggleLyrics()
                                },
                                onToggleNextUp = {
                                    if (state.showLyrics) viewModel.toggleLyrics()
                                    viewModel.setSeeAllSection(if (state.showSeeAllSection == "next_up") null else "next_up")
                                },
                                onPlayFromQueue = { s -> viewModel.playSong(s, state.playlist) },
                                autoPlayEnabled = state.autoPlayEnabled,
                                onToggleAutoPlay = { viewModel.toggleAutoPlay() },
                                autoMixMode = state.autoMixMode,
                                autoMixEnabled = state.autoMixEnabled,
                                onToggleAutoMix = { viewModel.toggleAutoMix() },
                                onGoToAlbum = { s -> viewModel.openAlbumForSong(s) },
                                onMoveToPlayNext = { s -> viewModel.moveSongToPlayNext(s) },
                                onRemoveFromQueue = { sId -> viewModel.removeSongFromQueue(sId) },
                                onMoveInQueue = { from, to -> viewModel.moveSongInQueue(from, to) },
                                onDownloadSong = { s -> viewModel.downloadSongHighestQuality(s) },
                                onCreatePlaylist = { name, s -> viewModel.createPlaylist(name, s) },
                                onAddToPlaylist = { pId, s -> viewModel.addSongToPlaylist(pId, s) }
                            )
                        }
                    }
                }


            }
        }
    }

    // Authentic Pre-Crash Twitter Opening Splash Animation Overlay
    if (splashVisible) {
        val isDark = isDarkTheme(state.themeMode)
        val splashBg = if (isDark) GrayBlackBg else Color(0xFFF6F7F9)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f)
                .graphicsLayer {
                    alpha = launchAlpha.value
                }
                .background(splashBg),
            contentAlignment = Alignment.Center
        ) {
            // Soundwaves (dissolves)
            Image(
                painter = painterResource(id = R.drawable.beatles_logo_waves),
                contentDescription = null,
                colorFilter = if (!isDark) androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFF141416)) else null,
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer {
                        alpha = whiteWavesAlpha.value
                    }
            )
            // Center Red Bar (expands horizontally first, then vertically to reveal app)
            Image(
                painter = painterResource(id = R.drawable.beatles_logo_bar),
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer {
                        scaleX = redLineScaleX.value
                        scaleY = redLineScaleY.value
                    }
            )
        }
    }
}
}

@Composable
private fun MiniPlayerSeekLine(
    position: () -> Long,
    duration: () -> Long,
    isMixing: () -> Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val dur = duration()
    if (dur <= 0L) return
    val pos = position()
    val progress = (pos.toFloat() / dur).coerceIn(0f, 1f)
    val mixing = isMixing()
    val seekColor = Color.White
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (mixing) 3.dp else 2.dp)
            .background(if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(seekColor)
        )
    }
}

