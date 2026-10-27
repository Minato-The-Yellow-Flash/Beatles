package com.beatles.player.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
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
import com.beatles.player.ui.components.AppUpdateDialog
import com.beatles.player.ui.components.FluidAlbumDetailOverlay
import com.beatles.player.ui.components.FluidAlbumSetDetailOverlay
import com.beatles.player.ui.components.FluidArtistDetailOverlay
import com.beatles.player.ui.components.NowPlayingSheet
import com.beatles.player.ui.screens.*
import com.beatles.player.ui.theme.isDarkTheme
import com.beatles.player.viewmodel.PlayerViewModel
import com.beatles.player.viewmodel.Tab
import com.beatles.player.ui.components.getAppleMusicGradientColors
import androidx.compose.ui.graphics.RectangleShape
import com.beatles.player.ui.motion.liquidPress
import com.beatles.player.ui.motion.LiquidMotion
import com.beatles.player.ui.theme.QualityManager
import com.beatles.player.ui.theme.VisualQuality
import com.beatles.player.ui.theme.LocalDynamicBottomPadding
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
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
private val GlassHazeStyle = HazeStyle(blurRadius = 4.dp, noiseFactor = 0.06f)

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
    val scope = rememberCoroutineScope()
    var topMiniArtBounds by remember { mutableStateOf<Rect?>(null) }
    var midMiniArtBounds by remember { mutableStateOf<Rect?>(null) }
    var activeMiniArtBounds by remember { mutableStateOf<Rect?>(null) }
    var standardMiniPillBounds by remember { mutableStateOf<Rect?>(null) }
    var midMiniPillBounds by remember { mutableStateOf<Rect?>(null) }
    var openedFromMidCapsule by remember { mutableStateOf(false) }
    var activeMiniPillBounds by remember { mutableStateOf<Rect?>(null) }
    var activeMiniPillCorner by remember { mutableStateOf(22.dp) }
    val playerProgress = remember { Animatable(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val maxHeaderOffset = with(density) { 110.dp.toPx() }
    var headerOffsetPx by remember { mutableFloatStateOf(0f) }
    var scrolledState by remember { mutableStateOf(false) }

    LaunchedEffect(state.isExpanded) {
        if (state.isExpanded) {
            playerProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 320f)
            )
        } else {
            playerProgress.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 320f)
            )
        }
    }

    // Dynamic Header Collapse on Scroll – raw offset is only read in layout/draw lambdas (no recomposition per frame)

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta > 0f) {
                    val newOffset = (headerOffsetPx + delta).coerceIn(-maxHeaderOffset, 0f)
                    if (newOffset != headerOffsetPx) headerOffsetPx = newOffset
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val delta = consumed.y
                if (delta < 0f) {
                    val newOffset = (headerOffsetPx + delta).coerceIn(-maxHeaderOffset, 0f)
                    if (newOffset != headerOffsetPx) headerOffsetPx = newOffset
                }
                if (headerOffsetPx < -130f) { if (!scrolledState) scrolledState = true }
                else if (headerOffsetPx > -20f) { if (scrolledState) scrolledState = false }
                return Offset.Zero
            }
        }
    }
    val animatedHeaderOffset = { headerOffsetPx }
    val headerAlphaProvider = { ((headerOffsetPx + maxHeaderOffset) / maxHeaderOffset).coerceIn(0f, 1f) }

    LaunchedEffect(state.currentTab) {
        headerOffsetPx = 0f
    }



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

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    LaunchedEffect(state.isExpanded, state.currentTab, state.showSeeAllSection, state.selectedAlbum) {
        if (state.isExpanded || state.currentTab != Tab.SEARCH || state.showSeeAllSection != null || state.selectedAlbum != null) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    // ═══ GLOBAL DETERMINISTIC BACK NAVIGATION (R11) ═══
    // Priority order (highest to lowest, strictly mutually exclusive):
    // 0. Update dialog
    PredictiveBackHandler(enabled = state.availableUpdate != null) { progress ->
        try { progress.collect {}; viewModel.dismissUpdate() } catch (_: Exception) {}
    }
    // 1. Transient overlay (AddAlbumDialog, ProfilePage)
    PredictiveBackHandler(enabled = state.availableUpdate == null && state.showAddAlbumDialog) { progress ->
        try { progress.collect {}; viewModel.setShowAddAlbumDialog(false) } catch (_: Exception) {}
    }
    PredictiveBackHandler(enabled = state.availableUpdate == null && !state.showAddAlbumDialog && state.showProfilePage) { progress ->
        try { progress.collect {}; viewModel.setProfileOpen(false) } catch (_: Exception) {}
    }

    // 2. Player sub-pages: if player is expanded and showing Lyrics or Next Up, exit that sub-page and show player's main view
    PredictiveBackHandler(enabled = !state.showAddAlbumDialog && !state.showProfilePage && state.isExpanded && state.showSeeAllSection == "next_up") { progress ->
        try { progress.collect {}; viewModel.setSeeAllSection(null) } catch (_: Exception) {}
    }
    PredictiveBackHandler(enabled = !state.showAddAlbumDialog && !state.showProfilePage && state.isExpanded && state.showLyrics && state.showSeeAllSection != "next_up") { progress ->
        try { progress.collect {}; viewModel.toggleLyrics() } catch (_: Exception) {}
    }

    // 3. Player collapse: if the player is expanded on its main view, collapse it back to the mini player
    PredictiveBackHandler(enabled = !state.showAddAlbumDialog && !state.showProfilePage && state.isExpanded && !state.showLyrics && state.showSeeAllSection != "next_up") { progress ->
        try {
            progress.collect {}
            if (openedFromMidCapsule && midMiniPillBounds != null) {
                activeMiniPillBounds = midMiniPillBounds
                activeMiniPillCorner = 28.dp
            } else if (standardMiniPillBounds != null) {
                activeMiniPillBounds = standardMiniPillBounds
                activeMiniPillCorner = 22.dp
            }
            viewModel.setExpanded(false)
        } catch (_: Exception) {}
    }

    // 4. Detail screens: album detail takes precedence over artist detail
    PredictiveBackHandler(enabled = !state.showAddAlbumDialog && !state.showProfilePage && !state.isExpanded && state.selectedAlbum != null) { progress ->
        try { progress.collect {}; viewModel.setSelectedAlbum(null) } catch (_: Exception) {}
    }
    PredictiveBackHandler(enabled = !state.showAddAlbumDialog && !state.showProfilePage && !state.isExpanded && state.selectedAlbum == null && state.selectedArtist != null) { progress ->
        try { progress.collect {}; viewModel.setSelectedArtist(null) } catch (_: Exception) {}
    }
    PredictiveBackHandler(enabled = !state.showAddAlbumDialog && !state.showProfilePage && !state.isExpanded && state.selectedAlbum == null && state.selectedArtist == null && state.selectedGenre != null) { progress ->
        try { progress.collect {}; viewModel.setSelectedGenre(null) } catch (_: Exception) {}
    }
    PredictiveBackHandler(enabled = !state.showAddAlbumDialog && !state.showProfilePage && !state.isExpanded && state.selectedArtist == null && state.selectedAlbum == null && state.selectedGenre == null && state.showSeeAllSection != null) { progress ->
        try { progress.collect {}; viewModel.setSeeAllSection(null) } catch (_: Exception) {}
    }

    // 5. Tab stack: if on any tab other than Home (Explore, Library, Search), pop back to Home
    PredictiveBackHandler(enabled = !state.showAddAlbumDialog && !state.showProfilePage && !state.isExpanded && state.selectedArtist == null && state.selectedAlbum == null && state.selectedGenre == null && state.showSeeAllSection == null && state.currentTab != Tab.HOME) { progress ->
        try { progress.collect {}; viewModel.setTab(Tab.HOME) } catch (_: Exception) {}
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
        delay(200)
        launch {
            whiteWavesAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 260, easing = LinearEasing)
            )
        }
        launch {
            // Expands horizontally first across the screen:
            redLineScaleX.animateTo(
                targetValue = 50f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            )
            // Then vertically to cover the ENTIRE screen (600f covers 100% of height!):
            redLineScaleY.animateTo(
                targetValue = 600f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
            // Once the screen is 100% covered in red, smoothly crossfade to reveal the homepage:
            launchAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
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
        4 -> OnboardStepReady(
            name = state.userName,
            onPrepareData = { viewModel.prepareDataDuringOnboarding() },
            onDone = { viewModel.finishOnboarding() },
            hasData = state.recommendations.isNotEmpty()
        )
        else -> {
            val isDark = isDarkTheme(state.themeMode)
            val appBg = if (isDark) GrayBlackBg else Color(0xFFF6F7F9)
            val translucentBarBg = if (isDark) Color(0xFF14151B).copy(alpha = 0.40f) else Color(0xFFFFFFFF).copy(alpha = 0.48f)
            val translucentBarBorder = if (isDark) Color.White.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.20f)
            val translucentPillBg = if (isDark) Color(0xFF181A22).copy(alpha = 0.44f) else Color(0xFFFFFFFF).copy(alpha = 0.52f)
            val translucentPillBorder = if (isDark) Color.White.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.20f)
            val translucentInnerPill = if (isDark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.08f)
            val textPrimary = if (isDark) Color.White else Color(0xFF141416)
            val textSecondary = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF6B7280)
            val navInactiveTint = if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF8E8E93)
            val headerAvatarBg = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
            val searchInputColor = if (isDark) Color.White else Color(0xFF141416)
            val searchHintColor = if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF9CA3AF)
            val noticeBg = if (isDark) Color(0xFF222226).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f)
            val dominantColor = remember(state.currentSong?.id) {
                state.currentSong?.let {
                    getAppleMusicGradientColors(it.title, it.artist).first
                } ?: AccentRed
            }

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


            val targetBottomPadding = if (state.currentSong != null) 98.dp else 40.dp
            val animatedBottomPadding by animateDpAsState(
                targetValue = targetBottomPadding,
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                label = "dynamicBottomPadding"
            )

            CompositionLocalProvider(LocalDynamicBottomPadding provides animatedBottomPadding) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(appBg)
                    .nestedScroll(nestedScrollConnection)
            ) {
                
                // Base Tab content layer (with live backdrop blur sampling and sheet scale-down)
                val contentBehindScale = 1f - 0.08f * playerProgress.value.coerceIn(0f, 1f)
                val contentBehindAlpha = 1f - 0.30f * playerProgress.value.coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(state = hazeState)
                        .graphicsLayer {
                            scaleX = contentBehindScale
                            scaleY = contentBehindScale
                            alpha = contentBehindAlpha
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
                        }
                ) {
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
                                            onPlay = { s, pl, bounds -> viewModel.playSong(s, pl, sourceBounds = bounds) },
                                            onPlayArtistMix = { artist, songs -> viewModel.playArtistMix(artist, songs) },
                                            onSelectArtist = { artist, bounds -> viewModel.setSelectedArtist(artist, bounds) },
                                            onSeeAll = { section -> viewModel.setSeeAllSection(section.ifEmpty { null }) },
                                            onSelectAlbum = { alb, bounds -> viewModel.setSelectedAlbum(alb, bounds) },
                                            onPlayAlbumSet = { viewModel.playAlbumSet(it) },
                                            onSelectAlbumSet = { set, bounds -> viewModel.setSelectedAlbumSet(set, bounds) },
                                            onSelectGenre = { g, bounds -> viewModel.setSelectedGenre(g, bounds) },
                                            onLoadMoreTopSongs = { viewModel.loadMoreTopSongs() },
                                            onRefresh = { viewModel.refreshHomeScreen() }
                                        )
                                        Tab.EXPLORE -> ExploreScreen(
                                            state = screenState,
                                            headerOffset = animatedHeaderOffset,
                                            headerAlpha = headerAlphaProvider,
                                            onPlay = { s, pl, bounds -> viewModel.playSong(s, pl, sourceBounds = bounds) },
                                            onSearch = { viewModel.search(it); viewModel.setTab(Tab.SEARCH) },
                                            onSelectGenre = { g, b -> viewModel.setSelectedGenre(g, b) }
                                        )
                                        Tab.LIBRARY -> LibraryScreen(
                                            state = screenState,
                                            viewModel = viewModel,
                                            headerOffset = animatedHeaderOffset,
                                            headerAlpha = headerAlphaProvider,
                                            hazeState = hazeState,
                                            onPlay = { s, pl, bounds -> viewModel.playSong(s, pl, sourceBounds = bounds) }
                                        )
                                        Tab.SEARCH -> SearchScreen(
                                            state = screenState,
                                            focusRequested = searchFocusRequested,
                                            onSearch = { viewModel.search(it) },
                                            onQueryChange = { viewModel.fetchSearchSuggestions(it) },
                                            onSelectAlbum = { alb, bounds -> viewModel.setSelectedAlbum(alb, bounds) },
                                            onPlay = { s, _, bounds -> viewModel.playSong(s, playlist = null, isFromSearch = true, sourceBounds = bounds) },
                                            onMoveToPlayNext = { s -> viewModel.moveSongToPlayNext(s) },
                                            onPlayArtistMix = { viewModel.playArtistMix(it) },
                                            onSelectArtist = { artist, bounds -> viewModel.setSelectedArtist(artist, bounds) },
                                            onLoadMoreRecommendations = { viewModel.loadMoreTopSongs() }
                                        )
                                    }
                                }
                            }
                        }

                        UserRole.ARTIST -> {
                            ArtistScreen(state = state, viewModel = viewModel, onPlay = { s, pl, bounds -> viewModel.playSong(s, pl, sourceBounds = bounds) })
                        }

                        UserRole.ADMIN -> {
                            AdminScreen(state = state, viewModel = viewModel)
                        }
                    }
                }



                // ═══ 2. TOP HEADER (Floating Logo + User Avatar) ═══
                val isAnyBrowseOrOverlayActive = state.showProfilePage ||
                    state.showSeeAllSection != null ||
                    state.currentTab == Tab.SEARCH ||
                    state.selectedAlbum != null ||
                    state.selectedArtist != null ||
                    state.selectedGenre != null ||
                    state.selectedAlbumSet != null ||
                    playerProgress.value > 0.01f ||
                    state.isExpanded

                if (!isAnyBrowseOrOverlayActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .zIndex(10f)
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
                            
                            val profileInteraction = remember { MutableInteractionSource() }
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .liquidPress(profileInteraction)
                                    .shadow(3.dp, CircleShape, spotColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.15f))
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF383842).copy(alpha = 0.45f) else Color(0xFFE2E2E2).copy(alpha = 0.55f))
                                    .glassRefraction(19.dp, isDark = isDark, isCircle = true)
                                    .clickable(interactionSource = profileInteraction, indication = null) { viewModel.setProfileOpen(true) },
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
                val isOverlayActive = (state.isExpanded && playerProgress.value >= 0.99f) ||
                    state.selectedAlbum != null ||
                    state.selectedArtist != null ||
                    state.selectedGenre != null ||
                    state.selectedAlbumSet != null ||
                    state.showProfilePage

                val playerFade = (1f - playerProgress.value * 1.25f).coerceIn(0f, 1f)
                val targetBottomAlpha = if (isOverlayActive) 0f else playerFade

                val bottomBarAlpha by animateFloatAsState(
                    targetValue = targetBottomAlpha,
                    animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
                    label = "bottomBarAlpha"
                )

                if (bottomBarAlpha > 0.001f) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .zIndex(10f)
                            .navigationBarsPadding()
                            .imePadding()
                            .graphicsLayer {
                                alpha = bottomBarAlpha
                                translationY = (1f - bottomBarAlpha) * 12f
                            }
                            .pointerInput(bottomBarAlpha) {
                                if (bottomBarAlpha > 0.01f) {
                                    detectTapGestures { /* consume touches to prevent mistouches to the list underneath */ }
                                }
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
                                val totalWidthPx = constraints.maxWidth.toFloat()
                                val pillCorner = if (compact) barCorner else 22.dp
                                val pillArt = if (compact) 38.dp else 32.dp

                                var isScrubbingActive by remember { mutableStateOf(false) }
                                var scrubFraction by remember { mutableFloatStateOf(0f) }
                                var scrubPositionMs by remember { mutableLongStateOf(0L) }
                                var dragOffsetX by remember { mutableFloatStateOf(0f) }
                                var hasPassedThreshold by remember { mutableStateOf(false) }

                                val animatedOffsetXPx by animateFloatAsState(
                                    targetValue = dragOffsetX,
                                    animationSpec = LiquidMotion.Lens,
                                    label = "miniSwipeOffset"
                                )

                                // Liquid motion stretch deformation along swipe axis
                                val swipeFraction = if (totalWidthPx > 0f) (kotlin.math.abs(animatedOffsetXPx) / totalWidthPx).coerceIn(0f, 1f) else 0f
                                val stretchX = 1f + (swipeFraction * 0.12f).coerceIn(0f, 0.12f)
                                val squeezeY = 1f - (swipeFraction * 0.04f).coerceIn(0f, 0.04f)

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .onGloballyPositioned { coords ->
                                            if (coords.isAttached && !isSearch) {
                                                val b = coords.boundsInRoot()
                                                if (b.width > 20f && b.height > 20f) {
                                                    if (compact) {
                                                        midMiniPillBounds = b
                                                        if (openedFromMidCapsule || (morph > 0.6f && !state.isExpanded)) {
                                                            activeMiniPillBounds = b
                                                            activeMiniPillCorner = pillCorner
                                                        }
                                                    } else {
                                                        standardMiniPillBounds = b
                                                        if (!openedFromMidCapsule && (!state.isExpanded || activeMiniPillBounds == null)) {
                                                            activeMiniPillBounds = b
                                                            activeMiniPillCorner = pillCorner
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        .graphicsLayer {
                                            translationX = animatedOffsetXPx
                                            scaleX = stretchX
                                            scaleY = squeezeY
                                        }
                                        .clip(RoundedCornerShape(pillCorner))
                                        .then(
                                            if (QualityManager.currentQuality.value != VisualQuality.BATTERY_SAVER) {
                                                Modifier.hazeChild(
                                                    state = hazeState,
                                                    shape = RoundedCornerShape(pillCorner),
                                                    style = GlassHazeStyle
                                                )
                                            } else Modifier
                                        )
                                        .background(translucentPillBg)
                                        .glassRefraction(pillCorner, isDark)
                                ) {
                                    // Gesture detector for horizontal swipe to change track & tap to expand (R6)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .pointerInput(Unit) {
                                                awaitEachGesture {
                                                    val down = awaitFirstDown(requireUnconsumed = false)
                                                    if (isScrubbingActive) return@awaitEachGesture
                                                    val slop = viewConfiguration.touchSlop
                                                    var dx = 0f
                                                    var dy = 0f
                                                    var isHorizontalSwipe = false
                                                    hasPassedThreshold = false
                                                    val velocityTracker = VelocityTracker()
                                                    velocityTracker.addPosition(down.uptimeMillis, down.position)

                                                    do {
                                                        val event = awaitPointerEvent()
                                                        if (isScrubbingActive) {
                                                            dragOffsetX = 0f
                                                            break
                                                        }
                                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                                                        val d = change.positionChange()
                                                        dx += d.x
                                                        dy += d.y

                                                        if (!isHorizontalSwipe && (kotlin.math.abs(dx) > slop || kotlin.math.abs(dy) > slop)) {
                                                            if (kotlin.math.abs(dx) > kotlin.math.abs(dy) * 1.3f) {
                                                                isHorizontalSwipe = true
                                                            }
                                                        }

                                                        if (isHorizontalSwipe) {
                                                            change.consume()
                                                            // Rubber-band drag with resistance
                                                            dragOffsetX = dx * 0.80f
                                                            val thresholdPx = totalWidthPx * 0.20f
                                                            if (kotlin.math.abs(dragOffsetX) >= thresholdPx) {
                                                                if (!hasPassedThreshold) {
                                                                    hasPassedThreshold = true
                                                                    com.beatles.player.ui.components.Haptics.tick(miniView)
                                                                }
                                                            } else {
                                                                hasPassedThreshold = false
                                                            }
                                                        }
                                                    } while (event.changes.any { it.pressed })

                                                    val velocity = velocityTracker.calculateVelocity().x
                                                    val thresholdPx = totalWidthPx * 0.20f
                                                    val flingThresholdPx = with(density) { 450.dp.toPx() }

                                                    if (isHorizontalSwipe) {
                                                        val commitSwipe = kotlin.math.abs(dragOffsetX) >= thresholdPx || kotlin.math.abs(velocity) > flingThresholdPx
                                                        if (commitSwipe) {
                                                            com.beatles.player.ui.components.Haptics.confirm(miniView)
                                                            if (dragOffsetX > 0f || velocity > flingThresholdPx) {
                                                                viewModel.playNext()
                                                            } else {
                                                                viewModel.playPrevious()
                                                            }
                                                        }
                                                        dragOffsetX = 0f
                                                        hasPassedThreshold = false
                                                    } else if (!isScrubbingActive) {
                                                        // Pure tap: expand player (swipes NEVER expand player)
                                                        if (kotlin.math.abs(dx) <= slop && kotlin.math.abs(dy) <= slop) {
                                                            activeMiniPillCorner = pillCorner
                                                            if (compact) {
                                                                openedFromMidCapsule = true
                                                                if (midMiniPillBounds != null) {
                                                                    activeMiniPillBounds = midMiniPillBounds
                                                                }
                                                                if (midMiniArtBounds != null) {
                                                                    activeMiniArtBounds = midMiniArtBounds
                                                                }
                                                            } else {
                                                                openedFromMidCapsule = false
                                                                if (standardMiniPillBounds != null) {
                                                                    activeMiniPillBounds = standardMiniPillBounds
                                                                }
                                                                if (topMiniArtBounds != null) {
                                                                    activeMiniArtBounds = topMiniArtBounds
                                                                }
                                                            }
                                                            viewModel.setExpanded(true)
                                                        }
                                                    }
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 8.dp)
                                                .semantics {
                                                    customActions = listOf(
                                                        CustomAccessibilityAction("Next Track") {
                                                            viewModel.playNext()
                                                            true
                                                        },
                                                        CustomAccessibilityAction("Previous Track") {
                                                            viewModel.playPrevious()
                                                            true
                                                        }
                                                    )
                                                },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (song.coverUrl.isNotEmpty()) {
                                                AsyncImage(
                                                    model = song.coverUrl,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(pillArt)
                                                        .clip(CircleShape)
                                                        .onGloballyPositioned { coords ->
                                                            if (coords.isAttached && !isSearch) {
                                                                val artB = coords.boundsInRoot()
                                                                if (compact) {
                                                                    midMiniArtBounds = artB
                                                                    if (openedFromMidCapsule) activeMiniArtBounds = artB
                                                                } else {
                                                                    topMiniArtBounds = artB
                                                                    if (!openedFromMidCapsule) activeMiniArtBounds = artB
                                                                }
                                                            }
                                                        }
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(pillArt)
                                                        .clip(CircleShape)
                                                        .background(Color.Gray)
                                                        .onGloballyPositioned { coords ->
                                                            if (coords.isAttached && !isSearch) {
                                                                val artB = coords.boundsInRoot()
                                                                if (compact) {
                                                                    midMiniArtBounds = artB
                                                                    if (openedFromMidCapsule) activeMiniArtBounds = artB
                                                                } else {
                                                                    topMiniArtBounds = artB
                                                                    if (!openedFromMidCapsule) activeMiniArtBounds = artB
                                                                }
                                                            }
                                                        }
                                                )
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
                                                modifier = Modifier.size(if (compact) 32.dp else 34.dp)
                                            ) {
                                                if (state.isLoadingStream) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(16.dp),
                                                        color = textPrimary,
                                                        strokeWidth = 2.dp
                                                    )
                                                } else {
                                                    Icon(
                                                        if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                                        contentDescription = null,
                                                        tint = textPrimary,
                                                        modifier = Modifier.size(if (compact) 19.dp else 20.dp)
                                                    )
                                                }
                                            }
                                            if (!compact) {
                                                IconButton(
                                                    onClick = {
                                                        com.beatles.player.ui.components.Haptics.click(miniView)
                                                        viewModel.playNext()
                                                    },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(Icons.Rounded.SkipNext, contentDescription = null, tint = textPrimary, modifier = Modifier.size(20.dp))
                                                }
                                            }
                                        }
                                    }

                                    // Hold-to-expand seek indicator line (R7)
                                    MiniPlayerSeekLine(
                                        position = { stateHolder.value.currentPosition },
                                        duration = { stateHolder.value.duration },
                                        isMixing = { stateHolder.value.autoMixState.isMixing },
                                        isDark = isDark,
                                        onSeek = { viewModel.seekTo(it) },
                                        onScrubbingChanged = { isScrubbingActive = it },
                                        onScrubFractionChanged = { frac, ms ->
                                            scrubFraction = frac
                                            scrubPositionMs = ms
                                        },
                                        modifier = Modifier.align(Alignment.BottomStart)
                                    )
                                }

                                // Liquid connected time display (rendered OUTSIDE the clipped pill so it is never cut by borders)
                                val dur = stateHolder.value.duration
                                val scrubAnimProgress by animateFloatAsState(
                                    targetValue = if (isScrubbingActive) 1f else 0f,
                                    animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
                                    label = "miniSeekBubbleAnim"
                                )

                                if (scrubAnimProgress > 0.001f && dur > 0L) {
                                    LiquidConnectedTimeBubble(
                                        scrubFraction = scrubFraction,
                                        positionMs = scrubPositionMs,
                                        durationMs = dur,
                                        isDark = isDark,
                                        totalWidthPx = totalWidthPx,
                                        animProgress = scrubAnimProgress,
                                        translucentBg = translucentBarBg,
                                        borderColor = translucentBarBorder
                                    )
                                }
                            }
                        }
                    }

                    // Top Mini Player: smoothly collapses height and fades out when scrolling or entering search
                    val isPlayerMorphing = playerProgress.value > 0.001f || state.isExpanded
                    val topMorphScale = (1f - morph).coerceIn(0f, 1f)
                    val topPlayerAlpha = if (isPlayerMorphing) 0f else (1f - combined * 2.5f).coerceIn(0f, 1f)
                    if (hasSong && topMorphScale > 0.01f) {
                        val animatedTopHeight = 45.dp * topMorphScale
                        val animatedTopPad = 4.dp * topMorphScale
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(animatedTopHeight + animatedTopPad * 2)
                                .graphicsLayer {
                                    alpha = topPlayerAlpha
                                    scaleX = 0.92f + 0.08f * topPlayerAlpha
                                    scaleY = 0.92f + 0.08f * topPlayerAlpha
                                }
                                .padding(vertical = animatedTopPad),
                            contentAlignment = Alignment.Center
                        ) {
                            miniPlayerPill(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp).fillMaxHeight(),
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

                            LaunchedEffect(state.currentTab) {
                                if (state.currentTab == Tab.SEARCH) {
                                    delay(220)
                                    try {
                                        bottomSearchFocusRequester.requestFocus()
                                        keyboardController?.show()
                                    } catch (_: Exception) {}
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
                                    val barShape = RoundedCornerShape(barCorner)
                                    Box(
                                        modifier = Modifier
                                            .width(with(density) { leftWidthPx.toDp() })
                                            .fillMaxHeight()
                                            .clip(barShape)
                                            .then(
                                                if (QualityManager.currentQuality.value != VisualQuality.BATTERY_SAVER) {
                                                    Modifier.hazeChild(
                                                        state = hazeState,
                                                        shape = barShape,
                                                        style = GlassHazeStyle
                                                    )
                                                } else Modifier
                                            )
                                            .background(translucentBarBg)
                                            .glassRefraction(barCorner, isDark)
                                    ) {
                                        // Left Pill Content: Both layers blend smoothly with crossfade
                                        val activeTab = tabs[selectedMainTabIdx]
                                        val compactAlpha = ((combined - 0.20f) / 0.65f).coerceIn(0f, 1f)
                                        val fullAlpha = (1f - combined * 2.2f).coerceIn(0f, 1f)

                                        // 1. Full navigation tabs with sliding indicator (fades out smoothly as combined increases)
                                        if (fullAlpha > 0.01f) {
                                            val availableForTabs = (leftWidthPx - with(density) { 8.dp.toPx() }).coerceAtLeast(1f)
                                            val tabWidth = availableForTabs / tabs.size.toFloat()

                                            var isHoldingNav by remember { mutableStateOf(false) }
                                            var dragNavX by remember { mutableFloatStateOf(0f) }
                                            var activeHoverIdx by remember { mutableIntStateOf(selectedMainTabIdx) }

                                            val targetFraction = if (isHoldingNav) {
                                                (dragNavX / tabWidth - 0.5f).coerceIn(0f, (tabs.size - 1).toFloat())
                                            } else {
                                                selectedMainTabIdx.toFloat()
                                            }

                                            // Jiggy elastic spring with lively overshoot and bounce
                                            val indicatorFraction by animateFloatAsState(
                                                targetValue = targetFraction,
                                                animationSpec = if (isHoldingNav) spring(dampingRatio = 0.90f, stiffness = 600f)
                                                               else spring(dampingRatio = 0.58f, stiffness = 320f),
                                                label = "jiggyNavIndicator"
                                            )
                                            val diff = kotlin.math.abs(targetFraction - indicatorFraction)
                                            val stretchX = (1f + diff * 0.35f).coerceIn(1f, 1.14f)
                                            val squashY = (1f - diff * 0.10f).coerceIn(0.95f, 1f)

                                            val holdScale by animateFloatAsState(
                                                targetValue = if (isHoldingNav) 1.08f else 1.0f,
                                                animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
                                                label = "navHoldScale"
                                            )
                                            val holdAlpha by animateFloatAsState(
                                                targetValue = if (isHoldingNav) 0.50f else 0.22f,
                                                label = "navHoldAlpha"
                                            )

                                            // Strictly clamp so the indicator NEVER crosses the outer nav bar borders
                                            val maxOffsetPx = (availableForTabs - tabWidth).coerceAtLeast(0f)
                                            val clampedIndicatorX = (indicatorFraction * tabWidth).coerceIn(0f, maxOffsetPx)

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(4.dp)
                                                    .graphicsLayer { alpha = fullAlpha }
                                                    .pointerInput(tabWidth, availableForTabs) {
                                                        detectDragGesturesAfterLongPress(
                                                            onDragStart = { offset ->
                                                                isHoldingNav = true
                                                                dragNavX = offset.x
                                                                activeHoverIdx = (offset.x / tabWidth).toInt().coerceIn(0, tabs.size - 1)
                                                                com.beatles.player.ui.components.Haptics.confirm(windowView)
                                                            },
                                                            onDrag = { change, dragAmount ->
                                                                change.consume()
                                                                dragNavX = (dragNavX + dragAmount.x).coerceIn(0f, availableForTabs)
                                                                val newIdx = (dragNavX / tabWidth).toInt().coerceIn(0, tabs.size - 1)
                                                                if (newIdx != activeHoverIdx) {
                                                                    activeHoverIdx = newIdx
                                                                    com.beatles.player.ui.components.Haptics.tick(windowView)
                                                                }
                                                            },
                                                            onDragEnd = {
                                                                isHoldingNav = false
                                                                val chosenTab = tabs[activeHoverIdx].first
                                                                viewModel.setTab(chosenTab)
                                                                com.beatles.player.ui.components.Haptics.confirm(windowView)
                                                            },
                                                            onDragCancel = {
                                                                isHoldingNav = false
                                                            }
                                                        )
                                                    }
                                            ) {
                                                // Sliding active indicator with liquid deformation, strictly clamped inside outer bounds
                                                val innerBg = if (isDark) Color.White.copy(alpha = holdAlpha) else Color.Black.copy(alpha = if (isHoldingNav) 0.22f else 0.10f)
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxHeight()
                                                        .width(with(density) { tabWidth.coerceAtLeast(1f).toDp() })
                                                        .offset { IntOffset(clampedIndicatorX.roundToInt(), 0) }
                                                        .graphicsLayer {
                                                            scaleX = stretchX * holdScale
                                                            scaleY = squashY * holdScale
                                                        }
                                                        .clip(RoundedCornerShape(barCorner))
                                                        .background(innerBg)
                                                )

                                                Row(
                                                    modifier = Modifier.fillMaxSize(),
                                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    tabs.forEach { (tab, icon, label) ->
                                                        val isCurrentTabActive = state.currentTab == tab
                                                        val tabScale by animateFloatAsState(
                                                            targetValue = if (isCurrentTabActive) 1f else 0.92f,
                                                            animationSpec = LiquidMotion.Lens,
                                                            label = "tabScale_${tab.name}"
                                                        )
                                                        val tabAlpha by animateFloatAsState(
                                                            targetValue = if (isCurrentTabActive) 1f else 0.65f,
                                                            animationSpec = LiquidMotion.Lens,
                                                            label = "tabAlpha_${tab.name}"
                                                        )

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
                                                                verticalArrangement = Arrangement.Center,
                                                                modifier = Modifier.graphicsLayer {
                                                                    scaleX = tabScale
                                                                    scaleY = tabScale
                                                                    alpha = tabAlpha
                                                                }
                                                            ) {
                                                                Icon(
                                                                    icon,
                                                                    contentDescription = null,
                                                                    tint = if (isCurrentTabActive) AccentRed else navInactiveTint,
                                                                    modifier = Modifier.size(navIconSize)
                                                                )
                                                                if (combined < 0.25f) {
                                                                    Text(
                                                                        label,
                                                                        fontSize = navTextSize,
                                                                        fontWeight = FontWeight.Medium,
                                                                        color = if (isCurrentTabActive) AccentRed else textSecondary,
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

                                    // Scroll mode: mini player sits in the middle (continuous math prevents search button from jiggling)
                                    Spacer(modifier = Modifier.width(with(density) { gapPx.toDp() }))
                                    if (hasSong && morph > 0.0001f) {
                                        val middleWidth = searchModeMiddleWidthPx * morph
                                        Box(
                                            modifier = Modifier
                                                .width(with(density) { middleWidth.toDp() })
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(barCorner))
                                                .graphicsLayer { alpha = if (isPlayerMorphing) 0f else morph }
                                        ) {
                                            miniPlayerPill(Modifier.fillMaxSize(), true)
                                        }
                                        Spacer(modifier = Modifier.width(with(density) { (gapPx * morph).toDp() }))
                                    }

                                    // Search Pill: contracts to circle button (normal mode) or expands into search bar (search mode)
                                    val searchPillShape = if (searchExpandProgress > 0.05f) barShape else CircleShape
                                    Box(
                                        modifier = Modifier
                                            .width(with(density) { searchWidthPx.toDp() })
                                            .fillMaxHeight()
                                            .clip(searchPillShape)
                                            .then(
                                                if (QualityManager.currentQuality.value != VisualQuality.BATTERY_SAVER) {
                                                    Modifier.hazeChild(
                                                        state = hazeState,
                                                        shape = searchPillShape,
                                                        style = GlassHazeStyle
                                                    )
                                                } else Modifier
                                            )
                                            .background(translucentBarBg)
                                            .glassRefraction(barCorner, isDark)
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
                                        if (searchExpandProgress > 0.35f) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .graphicsLayer {
                                                        alpha = ((searchExpandProgress - 0.35f) / 0.65f).coerceIn(0f, 1f)
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
                                                                Text("Search", color = searchHintColor, fontSize = 13.sp)
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
                                                .onGloballyPositioned { coords ->
                                                    if (coords.isAttached && isSearch && searchExpandProgress > 0.5f) {
                                                        val b = coords.boundsInRoot()
                                                        if (b.width > 20f && b.height > 20f) {
                                                            activeMiniPillBounds = b
                                                            activeMiniPillCorner = circleSize / 2
                                                        }
                                                    }
                                                }
                                                .clip(CircleShape)
                                                .then(
                                                    if (QualityManager.currentQuality.value != VisualQuality.BATTERY_SAVER) {
                                                        Modifier.hazeChild(
                                                            state = hazeState,
                                                            shape = CircleShape,
                                                            style = GlassHazeStyle
                                                        )
                                                    } else Modifier
                                                )
                                                .background(translucentBarBg)
                                                .glassRefraction(circleSize / 2, isDark, isCircle = true)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    activeMiniPillCorner = circleSize / 2
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
                                                    modifier = Modifier
                                                        .size(artSize)
                                                        .clip(CircleShape)
                                                        .onGloballyPositioned { coords ->
                                                            if (coords.isAttached && isSearch) {
                                                                activeMiniArtBounds = coords.boundsInRoot()
                                                            }
                                                        }
                                                )
                                            } else if (animatedRightPillWidth >= artSize) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(artSize)
                                                        .clip(CircleShape)
                                                        .background(Color.Gray)
                                                        .onGloballyPositioned { coords ->
                                                            if (coords.isAttached && isSearch) {
                                                                activeMiniArtBounds = coords.boundsInRoot()
                                                            }
                                                        }
                                                )
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
                                        .then(
                                            if (QualityManager.currentQuality.value != VisualQuality.BATTERY_SAVER) {
                                                Modifier.hazeChild(
                                                    state = hazeState,
                                                    shape = RoundedCornerShape(barCorner),
                                                    style = GlassHazeStyle
                                                )
                                            } else Modifier
                                        )
                                        .background(translucentBarBg)
                                        .glassRefraction(barCorner, isDark)
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
                                        .then(
                                            if (QualityManager.currentQuality.value != VisualQuality.BATTERY_SAVER) {
                                                Modifier.hazeChild(
                                                    state = hazeState,
                                                    shape = CircleShape,
                                                    style = GlassHazeStyle
                                                )
                                            } else Modifier
                                        )
                                        .background(translucentBarBg)
                                        .glassRefraction(circleSize / 2, isDark, isCircle = true)
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
                                        .then(
                                            if (QualityManager.currentQuality.value != VisualQuality.BATTERY_SAVER) {
                                                Modifier.hazeChild(
                                                    state = hazeState,
                                                    shape = RoundedCornerShape(barCorner),
                                                    style = GlassHazeStyle
                                                )
                                            } else Modifier
                                        )
                                        .background(translucentBarBg)
                                        .glassRefraction(barCorner, isDark)
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
                                        .then(
                                            if (QualityManager.currentQuality.value != VisualQuality.BATTERY_SAVER) {
                                                Modifier.hazeChild(
                                                    state = hazeState,
                                                    shape = CircleShape,
                                                    style = GlassHazeStyle
                                                )
                                            } else Modifier
                                        )
                                        .background(if (state.adminTab == AdminTab.SETTINGS) AccentBlue else translucentBarBg)
                                        .glassRefraction(circleSize / 2, isDark, isCircle = true)
                                        .clickable { viewModel.setAdminTab(AdminTab.SETTINGS) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = if (state.adminTab == AdminTab.SETTINGS) Color.White else textSecondary, modifier = Modifier.size(navIconSize))
                                }
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

                // Fluid Genre Detail Overlay (Expanding continuous shared-element animation from browse genre card)
                FluidGenreDetailOverlay(
                    genre = state.selectedGenre,
                    sourceBounds = state.selectedGenreBounds,
                    state = state,
                    onClose = { viewModel.setSelectedGenre(null) },
                    onPlay = { s, pl, bounds -> viewModel.playSong(s, pl, sourceBounds = bounds) }
                )

                // Fluid Artist Detail Overlay (Expanding continuous shared-element animation from artist item)
                FluidArtistDetailOverlay(
                    artist = state.selectedArtist,
                    sourceBounds = state.selectedArtistBounds,
                    state = state,
                    viewModel = viewModel,
                    onClose = { viewModel.setSelectedArtist(null) },
                    onPlay = { s, pl -> viewModel.playSong(s, pl) },
                    onPlayArtistMix = { viewModel.playArtistMix(it) }
                )

                // Fluid Album Set Detail Overlay (Interactive HorizontalPager Carousel)
                FluidAlbumSetDetailOverlay(
                    albumSet = state.selectedAlbumSet,
                    sourceBounds = state.selectedAlbumSetBounds,
                    state = state,
                    viewModel = viewModel,
                    onClose = { viewModel.setSelectedAlbumSet(null) },
                    onPlay = { s, pl -> viewModel.playSong(s, pl) },
                    onPlayAlbumSet = { viewModel.playAlbumSet(it) }
                )

                // Fluid Album Detail Overlay (Opens on top of artist page / album set / screens with seamless shared-element morph)
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
                    modifier = Modifier.fillMaxSize().zIndex(60f),
                    enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(240, easing = AppleEase)) + fadeIn(tween(200, easing = AppleEase)),
                    exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(220, easing = AppleEase)) + fadeOut(tween(180, easing = AppleEase))
                ) {
                    ProfileScreen(
                        state = state,
                        viewModel = viewModel,
                        onClose = { viewModel.setProfileOpen(false) }
                    )
                }

                // In-App GitHub Releases Self-Update Dialog
                val currentContext = androidx.compose.ui.platform.LocalContext.current
                if (state.availableUpdate != null) {
                    AppUpdateDialog(
                        update = state.availableUpdate!!,
                        progress = state.updateDownloadProgress,
                        status = state.updateDownloadStatus,
                        isDark = isDark,
                        onInstall = { viewModel.downloadAndInstallUpdate(currentContext) },
                        onDismiss = { viewModel.dismissUpdate() }
                    )
                }

                // Full screen player overlay (Fluid morph directly with mini player & song cards)
                if (state.currentSong != null) {
                    val p = playerProgress.value.coerceIn(0f, 1f)

                    if (playerProgress.value > 0.001f || state.isExpanded) {
                        val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
                        val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
                        val screenWidthPx = with(density) { screenWidthDp.toPx() }
                        val screenHeightPx = with(density) { screenHeightDp.toPx() }

                        // Full-page morph from song card (search) or mini media player pill shape
                        val isExpandingFromSongCard = state.isExpanded && playerProgress.value < 0.99f && state.selectedSongBounds != null
                        val songCardArtBounds = remember(state.selectedSongBounds) {
                            state.selectedSongBounds?.let { b ->
                                val artLeft = b.left + with(density) { 12.dp.toPx() }
                                val artTop = b.top + with(density) { 8.dp.toPx() }
                                val artSize = with(density) { 52.dp.toPx() }
                                Rect(artLeft, artTop, artLeft + artSize, artTop + artSize)
                            }
                        }
                        val startBounds = if (isExpandingFromSongCard) {
                            state.selectedSongBounds!!
                        } else if (openedFromMidCapsule && midMiniPillBounds != null) {
                            midMiniPillBounds!!
                        } else {
                            activeMiniPillBounds ?: standardMiniPillBounds ?: Rect(
                                left = with(density) { 16.dp.toPx() },
                                top = with(density) { 60.dp.toPx() },
                                right = screenWidthPx - with(density) { 16.dp.toPx() },
                                bottom = with(density) { 110.dp.toPx() }
                            )
                        }
                        val startCorner = if (isExpandingFromSongCard) 16.dp else activeMiniPillCorner

                        LaunchedEffect(playerProgress.value) {
                            if (playerProgress.value >= 0.99f && state.selectedSongBounds != null) {
                                viewModel.clearSelectedSongBounds()
                            }
                        }

                        LaunchedEffect(state.isExpanded, p) {
                            if (!state.isExpanded && p <= 0.001f) {
                                openedFromMidCapsule = false
                            }
                        }

                        val curLeft = startBounds.left * (1f - p)
                        val curTop = startBounds.top * (1f - p)
                        val curWidth = startBounds.width + (screenWidthPx - startBounds.width) * p
                        val curHeight = startBounds.height + (screenHeightPx - startBounds.height) * p
                        val curCorner = (startCorner * (1f - p)).coerceAtLeast(0.dp)

                        val curCornerShape = RoundedCornerShape(curCorner)
                        val curWidthDp = with(density) { curWidth.toDp() }
                        val curHeightDp = with(density) { curHeight.toDp() }
                        val curLeftPx = curLeft.roundToInt()
                        val curTopPx = curTop.roundToInt()
                        val playerBg = if (isDark) Color(0xFF0E0E10) else Color.White
                        val containerBg = androidx.compose.ui.graphics.lerp(
                            translucentPillBg,
                            playerBg,
                            (p * 1.6f).coerceIn(0f, 1f)
                        )

                        val morphContainerAlpha = if (p < 0.06f) (p / 0.06f).coerceIn(0f, 1f) else 1f

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(100f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(curLeftPx, curTopPx) }
                                    .size(curWidthDp, curHeightDp)
                                    .graphicsLayer { alpha = morphContainerAlpha }
                                    .shadow(
                                        elevation = (16.dp * p).coerceAtLeast(0.dp),
                                        shape = curCornerShape,
                                        spotColor = Color.Black.copy(alpha = 0.5f)
                                    )
                                    .clip(curCornerShape)
                                    .background(containerBg)
                                    .pointerInput(Unit) {
                                        detectVerticalDragGestures(
                                            onDragEnd = {
                                                val isInSubPage = (state.showLyrics && state.showSeeAllSection != "next_up") || state.showSeeAllSection == "next_up"
                                                if (isInSubPage) return@detectVerticalDragGestures
                                                if (playerProgress.value < 0.85f) {
                                                    // Collapse back into the exact originating pill (mid capsule or standard bottom mini player)
                                                    if (openedFromMidCapsule && midMiniPillBounds != null) {
                                                        activeMiniPillBounds = midMiniPillBounds
                                                        activeMiniPillCorner = 28.dp
                                                    } else if (standardMiniPillBounds != null) {
                                                        activeMiniPillBounds = standardMiniPillBounds
                                                        activeMiniPillCorner = 22.dp
                                                    }
                                                    viewModel.setExpanded(false)
                                                } else {
                                                    scope.launch {
                                                        playerProgress.animateTo(
                                                            targetValue = 1f,
                                                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 360f)
                                                        )
                                                    }
                                                }
                                            },
                                            onVerticalDrag = { _, dragAmount ->
                                                val isInSubPage = (state.showLyrics && state.showSeeAllSection != "next_up") || state.showSeeAllSection == "next_up"
                                                if (state.isExpanded && !isInSubPage) {
                                                    scope.launch {
                                                        val newP = (playerProgress.value - dragAmount / 450f).coerceIn(0f, 1f)
                                                        playerProgress.snapTo(newP)
                                                    }
                                                }
                                            }
                                        )
                                    }
                            ) {
                                // 1. Full Player Content (aligned to screen space so album art and components glide smoothly)
                                val fullPlayerAlpha = if (p <= 0.08f) 0f else ((p - 0.08f) / 0.92f).coerceIn(0f, 1f)
                                if (fullPlayerAlpha > 0.001f) {
                                    Box(
                                        modifier = Modifier
                                            .requiredSize(screenWidthDp, screenHeightDp)
                                            .offset { IntOffset(-curLeftPx, -curTopPx) }
                                            .graphicsLayer { alpha = fullPlayerAlpha }
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
                                            morphProgress = p,
                                            sourceOriginBounds = if (isExpandingFromSongCard && songCardArtBounds != null) {
                                                songCardArtBounds
                                            } else if (openedFromMidCapsule && midMiniArtBounds != null) {
                                                midMiniArtBounds
                                            } else {
                                                activeMiniArtBounds ?: topMiniArtBounds
                                            },
                                            onClose = {
                                                if (openedFromMidCapsule && midMiniPillBounds != null) {
                                                    activeMiniPillBounds = midMiniPillBounds
                                                    activeMiniPillCorner = 28.dp
                                                } else if (standardMiniPillBounds != null) {
                                                    activeMiniPillBounds = standardMiniPillBounds
                                                    activeMiniPillCorner = 22.dp
                                                }
                                                viewModel.setExpanded(false)
                                            },
                                            onTogglePlay = { viewModel.togglePlayPause() },
                                            onNext = { viewModel.playNext() },
                                            onPrevious = { viewModel.playPrevious() },
                                            onSeek = { viewModel.seekTo(it) },
                                            onToggleShuffle = { viewModel.toggleShuffle() },
                                            onToggleRepeat = { viewModel.toggleRepeat() },
                                            onToggleFavorite = { viewModel.toggleFavorite() },
                                            onToggleLyrics = { viewModel.switchToLyrics() },
                                            onToggleNextUp = { viewModel.switchToNextUp() },
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
                                            downloadedSongs = state.downloadedSongs,
                                            downloadingSongId = state.downloadingSongId,
                                            downloadProgress = state.downloadProgress,
                                            downloadFailedSongId = state.downloadFailedSongId,
                                            onCancelDownload = { sId -> viewModel.cancelDownload(sId) },
                                            onRemoveDownload = { sId -> viewModel.removeDownloadedSong(sId) },
                                            onCreatePlaylist = { name, s -> viewModel.createPlaylist(name, s) },
                                            onAddToPlaylist = { pId, s -> viewModel.addSongToPlaylist(pId, s) },
                                            queueTitle = state.queueTitle,
                                            isArtistMixLoading = state.isArtistMixLoading,
                                            onLoadMoreUpcoming = { viewModel.loadMoreUpcoming() },
                                            onPlayArtistMix = { viewModel.playArtistMix(it) },
                                            sleepTimerState = state.sleepTimerState,
                                            onStartSleepTimerMinutes = { viewModel.startSleepTimerMinutes(it) },
                                            onStartSleepTimerSongs = { viewModel.startSleepTimerSongs(it) },
                                            onStartSleepTimerAlbum = { viewModel.startSleepTimerAlbum() },
                                            onStartSleepTimerEndOfTrack = { viewModel.startSleepTimerEndOfTrack() },
                                            onExtendSleepTimerMinutes = { viewModel.extendSleepTimerMinutes(it) },
                                            onExtendSleepTimerSongs = { viewModel.extendSleepTimerSongs(it) },
                                            onExtendSleepTimerAlbum = { viewModel.extendSleepTimerAlbum() },
                                            onCancelSleepTimer = { viewModel.cancelSleepTimer() },
                                            onDismissSleepWarning = { viewModel.dismissSleepWarningPopup() },
                                            onSetSleepExtendPopupEnabled = { viewModel.setSleepExtendPopupEnabled(it) },
                                            autoMixTargetSongId = state.autoMixTargetSongId
                                        )
                                    }
                                }

                                // 2. Mini Player Pill Content (crossfades in when closing/expanding)
                                val miniPillAlpha = if (p >= 0.35f) 0f else ((0.35f - p) / 0.35f).coerceIn(0f, 1f)
                                if (miniPillAlpha > 0.001f) {
                                    val isCircleMini = startBounds.width < 100f
                                    val isMidCompact = !isCircleMini && !isExpandingFromSongCard && (openedFromMidCapsule || activeMiniPillBounds == midMiniPillBounds || startBounds.width < screenWidthPx * 0.85f)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer { alpha = miniPillAlpha }
                                            .padding(horizontal = if (isCircleMini) 0.dp else 8.dp),
                                        contentAlignment = if (isCircleMini) Alignment.Center else Alignment.CenterStart
                                    ) {
                                        if (isCircleMini) {
                                            val coverUrl = state.currentSong!!.coverUrl
                                            if (coverUrl.isNotEmpty()) {
                                                AsyncImage(
                                                    model = coverUrl,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(CircleShape)
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(CircleShape)
                                                        .background(Color.Gray)
                                                )
                                            }
                                        } else if (isExpandingFromSongCard) {
                                            val currentSong = state.currentSong!!
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                AsyncImage(
                                                    model = currentSong.coverUrl,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(52.dp)
                                                        .clip(if (currentSong.isAlbum) RoundedCornerShape(12.dp) else RoundedCornerShape(10.dp))
                                                )
                                                Spacer(modifier = Modifier.width(14.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        currentSong.title,
                                                        color = if (isDark) Color.White else Color(0xFF141416),
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1,
                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    val subText = if (currentSong.album.isNotBlank() && currentSong.album != "Single" && currentSong.album != "Album") "${currentSong.artist} • ${currentSong.album}" else currentSong.artist
                                                    Text(
                                                        subText,
                                                        color = if (isDark) Color.White.copy(alpha = 0.55f) else Color(0xFF6B7280),
                                                        fontSize = 12.sp,
                                                        maxLines = 1,
                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                    )
                                                }
                                                IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                                                    Icon(
                                                        Icons.Default.PlayArrow,
                                                        contentDescription = "Play",
                                                        tint = if (isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF141416),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxSize(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val pillArt = if (isMidCompact || startCorner > 24.dp) 38.dp else 32.dp
                                                if (state.currentSong!!.coverUrl.isNotEmpty()) {
                                                    AsyncImage(
                                                        model = state.currentSong!!.coverUrl,
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(pillArt)
                                                            .clip(CircleShape)
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(pillArt)
                                                            .clip(CircleShape)
                                                            .background(Color.Gray)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        state.currentSong!!.title,
                                                        color = Color.White,
                                                        fontSize = if (isMidCompact) 12.sp else 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1
                                                    )
                                                    if (!isMidCompact) {
                                                        Text(
                                                            state.currentSong!!.artist,
                                                            color = Color.White.copy(alpha = 0.6f),
                                                            fontSize = 10.sp,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                                IconButton(
                                                    onClick = { viewModel.togglePlayPause() },
                                                    modifier = Modifier.size(if (isMidCompact) 32.dp else 34.dp)
                                                 ) {
                                                    Icon(
                                                        if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(if (isMidCompact) 19.dp else 20.dp)
                                                    )
                                                }
                                                if (!isMidCompact) {
                                                    IconButton(
                                                        onClick = { viewModel.playNext() },
                                                        modifier = Modifier.size(34.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Rounded.SkipNext,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(20.dp)
                                                        )
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
                    .size(108.dp)
                    .graphicsLayer {
                        alpha = whiteWavesAlpha.value
                    }
            )
            // Center Red Bar (expands horizontally first, then vertically to reveal app)
            Image(
                painter = painterResource(id = R.drawable.beatles_logo_bar),
                contentDescription = null,
                modifier = Modifier
                    .size(108.dp)
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
    onSeek: (Long) -> Unit,
    onScrubbingChanged: (Boolean) -> Unit,
    onScrubFractionChanged: ((Float, Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dur = duration()
    if (dur <= 0L) return
    val pos = position()
    val progress = (pos.toFloat() / dur).coerceIn(0f, 1f)
    val mixing = isMixing()
    val view = androidx.compose.ui.platform.LocalView.current

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }

    val animatedHeight by animateDpAsState(
        targetValue = if (isScrubbing) 5.dp else if (mixing) 3.dp else 2.dp,
        animationSpec = LiquidMotion.LensDp,
        label = "seekHeight"
    )

    val currentFraction = if (isScrubbing) scrubFraction else progress

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isScrubbing) 24.dp else 12.dp)
            .semantics {
                contentDescription = "Playback seek bar"
                stateDescription = "${pos / 1000}s of ${dur / 1000}s"
                setProgress { targetProgress ->
                    val targetMs = (targetProgress * dur).toLong().coerceIn(0L, dur)
                    onSeek(targetMs)
                    true
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var isDown = true
                    var movedDistance = 0f
                    val timer = withTimeoutOrNull(180L) {
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                isDown = false
                                break
                            }
                            movedDistance = (change.position - down.position).getDistance()
                            if (movedDistance > viewConfiguration.touchSlop) {
                                break
                            }
                        } while (true)
                    }

                    val shouldStartScrub = (timer == null && isDown) || (isDown && movedDistance > viewConfiguration.touchSlop)

                    if (shouldStartScrub) {
                        isScrubbing = true
                        onScrubbingChanged(true)
                        val initialFrac = (down.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        scrubFraction = initialFrac
                        val initialMs = (initialFrac * dur).toLong().coerceIn(0L, dur)
                        onScrubFractionChanged?.invoke(initialFrac, initialMs)
                        com.beatles.player.ui.components.Haptics.tick(view)

                        var lastSec = initialMs / 1000L

                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.pressed) {
                                change.consume()
                                val newFrac = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                scrubFraction = newFrac
                                val curMs = (newFrac * dur).toLong().coerceIn(0L, dur)
                                val curSec = curMs / 1000L
                                if (curSec != lastSec) {
                                    lastSec = curSec
                                    com.beatles.player.ui.components.Haptics.tick(view)
                                }
                                onScrubFractionChanged?.invoke(newFrac, curMs)
                            } else {
                                break
                            }
                        } while (event.changes.any { it.pressed })

                        val targetMs = (scrubFraction * dur).toLong().coerceIn(0L, dur)
                        onSeek(targetMs)
                        com.beatles.player.ui.components.Haptics.confirm(view)
                        isScrubbing = false
                        onScrubbingChanged(false)
                    }
                }
            }
    ) {
        // Seek Track Line
        val trackBg = if (isDark) Color.White.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.12f)
        val trackFill = if (isDark) Color.White else Color(0xFF141416)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedHeight)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(animatedHeight / 2))
                .background(trackBg)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(currentFraction)
                    .background(trackFill)
            )
        }
    }
}

@Composable
private fun LiquidConnectedTimeBubble(
    scrubFraction: Float,
    positionMs: Long,
    durationMs: Long,
    isDark: Boolean,
    totalWidthPx: Float,
    animProgress: Float,
    translucentBg: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val bubbleWidthDp = 58.dp
    val bubbleHeightDp = 26.dp
    val bubbleCorner = 13.dp
    val bubbleWidthPx = with(density) { bubbleWidthDp.toPx() }
    val marginPx = with(density) { 8.dp.toPx() }

    val thumbX = (scrubFraction * totalWidthPx).coerceIn(marginPx, (totalWidthPx - marginPx).coerceAtLeast(marginPx))
    val maxLeftPx = (totalWidthPx - bubbleWidthPx - marginPx).coerceAtLeast(marginPx)
    val clampedBubbleLeft = (thumbX - bubbleWidthPx / 2f).coerceIn(marginPx, maxLeftPx)

    val textPrimary = if (isDark) Color.White else Color(0xFF141416)

    val curM = positionMs / 60000
    val curS = (positionMs / 1000) % 60
    val playedStr = "$curM:${String.format("%02d", curS)}"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(25f)
            .graphicsLayer {
                alpha = animProgress
                scaleX = 0.75f + 0.25f * animProgress
                scaleY = 0.75f + 0.25f * animProgress
                transformOrigin = TransformOrigin(
                    if (totalWidthPx > 0f) (thumbX / totalWidthPx).coerceIn(0f, 1f) else 0.5f,
                    1f
                )
            }
    ) {
        // Compact glowing floating time bubble (no connection bridge, inner shadow/glow)
        val glowColor = if (isDark) Color.White.copy(alpha = 0.38f) else Color.Black.copy(alpha = 0.18f)
        val spotGlow = if (isDark) Color.White.copy(alpha = 0.30f) else Color.Black.copy(alpha = 0.12f)
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        clampedBubbleLeft.roundToInt(),
                        with(density) { (-32).dp.roundToPx() }
                    )
                }
                .width(bubbleWidthDp)
                .height(bubbleHeightDp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(bubbleCorner),
                    spotColor = spotGlow
                )
                .clip(RoundedCornerShape(bubbleCorner))
                .background(translucentBg)
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(bubbleCorner)
                )
                .drawWithContent {
                    drawContent()
                    // Inner shadow / glow along perimeter
                    val strokeWidth = 1.2.dp.toPx()
                    drawRoundRect(
                        color = glowColor,
                        topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2f, strokeWidth / 2f),
                        size = androidx.compose.ui.geometry.Size(size.width - strokeWidth, size.height - strokeWidth),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(bubbleCorner.toPx(), bubbleCorner.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                    )
                    // Inner top glow / gradient
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                (if (isDark) Color.White else Color.Black).copy(alpha = if (isDark) 0.25f else 0.12f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.70f
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(bubbleCorner.toPx(), bubbleCorner.toPx())
                    )
                }
                .glassRefraction(bubbleCorner, isDark = isDark),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = playedStr,
                color = textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Optical Glass Refraction Modifier (iOS 27 Liquid Glass style):
 * Simulates directional ambient light reflection on curved glass pills:
 * 1. Incident specular reflection hugging the upper curve
 * 2. Secondary reflection hugging the lower curve (NOT connected to the top; sides are 100% transparent)
 * 3. In dark mode: subtle white specular reflection (12% alpha)
 * 4. In light mode (white screen): 10% darker reflection (18% alpha) for crisp definition
 */
internal fun Modifier.glassRefraction(
    cornerRadius: Dp,
    isDark: Boolean,
    isCircle: Boolean = false
): Modifier = this.drawWithContent {
    val cornerPx = if (isCircle) size.minDimension / 2f else cornerRadius.toPx()
    val reflectionColor = if (isDark) Color.White else Color(0xFF141416)

    // 1. Subtle dual-curve lens sheen (top and bottom curves, disconnected in middle)
    val sheenAlpha = if (isDark) 0.04f else 0.03f
    drawRoundRect(
        brush = Brush.verticalGradient(
            0.0f to reflectionColor.copy(alpha = sheenAlpha),
            0.22f to Color.Transparent,
            0.78f to Color.Transparent,
            1.0f to reflectionColor.copy(alpha = sheenAlpha * 0.6f)
        ),
        cornerRadius = CornerRadius(cornerPx, cornerPx)
    )

    // 2. Draw child contents (icons, labels, thumbnails)
    drawContent()

    // 3. Inner content refraction bevel: subtle secondary lines inset by 0.5dp (disconnected)
    // Dark mode: increased on up/down (0.09f). Light mode: lowered up/down (0.04f)
    val innerStrokeW = 0.5.dp.toPx()
    val innerPeakAlpha = if (isDark) 0.09f else 0.04f
    val innerBottomAlpha = innerPeakAlpha * 0.70f
    drawRoundRect(
        brush = Brush.verticalGradient(
            0.0f to reflectionColor.copy(alpha = innerPeakAlpha),
            0.16f to Color.Transparent,
            // DISCONNECTED GAP: sides have zero stroke
            0.84f to Color.Transparent,
            1.0f to reflectionColor.copy(alpha = innerBottomAlpha)
        ),
        topLeft = Offset(innerStrokeW, innerStrokeW),
        size = Size(
            (size.width - innerStrokeW * 2).coerceAtLeast(0f),
            (size.height - innerStrokeW * 2).coerceAtLeast(0f)
        ),
        cornerRadius = CornerRadius(
            (cornerPx - innerStrokeW).coerceAtLeast(0f),
            (cornerPx - innerStrokeW).coerceAtLeast(0f)
        ),
        style = Stroke(width = innerStrokeW)
    )

    // 4. Outer content refraction reflection: upper curve + lower curve
    // Dark mode: increased reflective up/down (0.35f). Light mode: lowered up/down to 10% (0.10f)
    val outerStrokeW = 0.5.dp.toPx()
    val outerPeakAlpha = if (isDark) 0.35f else 0.10f
    val outerBottomAlpha = outerPeakAlpha * 0.75f
    drawRoundRect(
        brush = Brush.verticalGradient(
            // Upper curve reflection
            0.0f to reflectionColor.copy(alpha = outerPeakAlpha),
            0.18f to reflectionColor.copy(alpha = outerPeakAlpha * 0.35f),
            0.28f to Color.Transparent,
            // Vertical middle gap
            0.72f to Color.Transparent,
            0.82f to reflectionColor.copy(alpha = outerBottomAlpha * 0.35f),
            // Lower curve reflection
            1.0f to reflectionColor.copy(alpha = outerBottomAlpha)
        ),
        cornerRadius = CornerRadius(cornerPx, cornerPx),
        style = Stroke(width = outerStrokeW)
    )

    // 5. Right & Left edges border:
    // Dark mode: side edges kept at 5% (0.05f). Light mode: increased side edges (0.45f)
    val sidePeakAlpha = if (isDark) 0.05f else 0.45f
    drawRoundRect(
        brush = Brush.horizontalGradient(
            // Left edge border
            0.0f to reflectionColor.copy(alpha = sidePeakAlpha),
            0.15f to Color.Transparent,
            // Horizontal middle gap
            0.85f to Color.Transparent,
            // Right edge border
            1.0f to reflectionColor.copy(alpha = sidePeakAlpha)
        ),
        cornerRadius = CornerRadius(cornerPx, cornerPx),
        style = Stroke(width = outerStrokeW)
    )

    // 6. Inner side bevel border:
    // Dark mode: side bevel at 2% (0.02f). Light mode: increased side bevel (0.14f)
    val innerSideAlpha = if (isDark) 0.02f else 0.14f
    drawRoundRect(
        brush = Brush.horizontalGradient(
            0.0f to reflectionColor.copy(alpha = innerSideAlpha),
            0.12f to Color.Transparent,
            0.88f to Color.Transparent,
            1.0f to reflectionColor.copy(alpha = innerSideAlpha)
        ),
        topLeft = Offset(innerStrokeW, innerStrokeW),
        size = Size(
            (size.width - innerStrokeW * 2).coerceAtLeast(0f),
            (size.height - innerStrokeW * 2).coerceAtLeast(0f)
        ),
        cornerRadius = CornerRadius(
            (cornerPx - innerStrokeW).coerceAtLeast(0f),
            (cornerPx - innerStrokeW).coerceAtLeast(0f)
        ),
        style = Stroke(width = innerStrokeW)
    )
}


