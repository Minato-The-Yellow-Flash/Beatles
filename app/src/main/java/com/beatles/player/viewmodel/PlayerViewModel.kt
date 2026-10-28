package com.beatles.player.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beatles.player.data.*
import com.beatles.player.player.AudioPlayerManager
import com.beatles.player.player.PlaybackService
import com.beatles.player.player.automix.AutoMixEngine
import com.beatles.player.player.automix.AutoMixState
import com.beatles.player.player.automix.TransitionPhase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.geometry.Rect

enum class Tab { HOME, EXPLORE, LIBRARY, SEARCH }

val DEFAULT_ALBUMS = emptyList<Album>()

val DEFAULT_MOCK_USERS = listOf(
    MockUser("u_1", "Anil Kumar", "anil@mail.com", UserRole.LISTENER, "2024-01"),
    MockUser("u_2", "Priya Sharma", "priya@mail.com", UserRole.ARTIST, "2024-03"),
    MockUser("u_3", "DJ Raftaar", "raftaar@mail.com", UserRole.ARTIST, "2023-11"),
    MockUser("u_4", "Sneha Patel", "sneha@mail.com", UserRole.LISTENER, "2024-07"),
    MockUser("u_5", "Rohit Verma", "rohit@mail.com", UserRole.LISTENER, "2024-08")
)

val DEFAULT_SUBMISSIONS = listOf(
    TrackSubmission("sub_1", "New Beginnings", "Priya Sharma", "Debut Album", "Pop", "2026-09-28", "pending"),
    TrackSubmission("sub_2", "Dil Ki Baat", "DJ Raftaar", "Volume 2", "Hip-Hop", "2026-09-29", "pending"),
    TrackSubmission("sub_3", "Raaton Mein", "Priya Sharma", "Debut Album", "Bollywood", "2026-09-27", "approved"),
    TrackSubmission("sub_4", "Ghazal Nights", "Ghulam Ali", "Classical Live", "Ghazal", "2026-09-25", "approved")
)

val DEFAULT_FAN_MESSAGES = listOf(
    FanMessage("fm_1", "Anil Kumar", "Your latest track is fire! 🔥 Can't stop playing it!", "2h ago"),
    FanMessage("fm_2", "Sneha Patel", "Are you planning a live concert or album tour anytime soon?", "5h ago"),
    FanMessage("fm_3", "Rohit Verma", "When is the next album dropping? We've been waiting for weeks!", "1d ago")
)

val DEFAULT_FEEDBACK = listOf(
    FeedbackTicket("fb_1", "Anil Kumar", "App audio streaming is super crisp and fast!", "2h ago", "praise"),
    FeedbackTicket("fb_2", "Sneha Patel", "Please add more Punjabi folk and Ghazal playlists.", "4h ago", "feature"),
    FeedbackTicket("fb_3", "Rohit Verma", "Lyrics sync works beautifully on the new update.", "1d ago", "praise")
)

val DEFAULT_PLAYLISTS = emptyList<Playlist>()

enum class SleepTimerMode {
    NONE,
    TIME,
    SONGS,
    ALBUM,
    END_OF_TRACK
}

data class SleepTimerState(
    val mode: SleepTimerMode = SleepTimerMode.NONE,
    val targetTimeMillis: Long = 0L,
    val totalDurationMillis: Long = 0L,
    val remainingSeconds: Long = 0L,
    val remainingSongs: Int = 0,
    val targetAlbumId: String? = null,
    val showWarningPopup: Boolean = false,
    val warningPopupRemainingSeconds: Int = 20,
    val isExtendPopupEnabled: Boolean = true
)

data class PlayerUiState(
    val onboardStep: Int = 1,
    val userName: String = "",
    val userRole: UserRole = UserRole.LISTENER,
    val showProfilePage: Boolean = false,
    val artistTab: ArtistTab = ArtistTab.FEED,
    val adminTab: AdminTab = AdminTab.DASHBOARD,
    val selectedArtists: Set<String> = emptySet(),
    val currentTab: Tab = Tab.HOME,
    val searchQuery: String = "",
    val searchResults: List<Song> = emptyList(),
    val searchedAlbum: Album? = null,
    val searchSuggestions: List<String> = emptyList(),
    val instantMatchingSongs: List<Song> = emptyList(),
    val trendingTags: List<String> = emptyList(),
    val isSearching: Boolean = false,
    val recommendations: List<Song> = MusicRepository.getRealPredictiveSongs(),
    val topHitsPage: Int = 1,
    val isTopHitsLoading: Boolean = false,
    val recentlyPlayed: List<Song> = emptyList(),
    val favoriteSongs: List<Song> = emptyList(),
    val playlists: List<Playlist> = DEFAULT_PLAYLISTS,
    val albumSets: List<AlbumSet> = emptyList(),
    val downloadedSongs: List<Song> = emptyList(),
    val downloadingSongId: String? = null,
    val downloadProgress: Float = 0f,
    val downloadFailedSongId: String? = null,
    val downloadNotice: String? = null,
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isExpanded: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val isLoadingStream: Boolean = false,
    val playlist: List<Song> = emptyList(),
    val shuffleModeEnabled: Boolean = false,
    val repeatMode: Int = 0,
    val lyrics: List<LyricLine> = emptyList(),
    val showLyrics: Boolean = false,
    val showSeeAllSection: String? = null,
    val isFavorite: Boolean = false,
    val autoPlayEnabled: Boolean = true,
    val autoMixMode: com.beatles.player.player.automix.AutoMixMode = com.beatles.player.player.automix.AutoMixMode.ON,
    val autoMixEnabled: Boolean = true,
    val autoMixState: AutoMixState = AutoMixState(),
    val albums: List<Album> = DEFAULT_ALBUMS,
    val heroAlbum: Album? = null,
    val selectedAlbum: Album? = null,
    val selectedAlbumBounds: Rect? = null,
    val showAddAlbumDialog: Boolean = false,
    val mockUsers: List<MockUser> = DEFAULT_MOCK_USERS,
    val trackSubmissions: List<TrackSubmission> = DEFAULT_SUBMISSIONS,
    val fanMessages: List<FanMessage> = DEFAULT_FAN_MESSAGES,
    val feedbackTickets: List<FeedbackTicket> = DEFAULT_FEEDBACK,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isRefreshingHome: Boolean = false,
    val selectedLanguage: String = "All",
    val selectedLanguages: Set<String> = emptySet(),
    val extendedAutoMixEnabled: Boolean = true,
    val selectedSongBounds: Rect? = null,
    val queueTitle: String? = null,
    val isArtistMixActive: Boolean = false,
    val artistMixTarget: String? = null,
    val isArtistMixLoading: Boolean = false,
    val lastFailedArtistMix: String? = null,
    val selectedArtist: String? = null,
    val selectedArtistBounds: Rect? = null,
    val selectedGenre: String? = null,
    val selectedGenreBounds: Rect? = null,
    val selectedAlbumSet: AlbumSet? = null,
    val selectedAlbumSetBounds: Rect? = null,
    val availableUpdate: com.beatles.player.data.UpdateInfo? = null,
    val isCheckingUpdate: Boolean = false,
    val updateDownloadProgress: Float? = null,
    val updateDownloadStatus: String? = null,
    val updateErrorMessage: String? = null,
    val updateNotice: String? = null,
    val totalAppDownloads: Int = 0,
    val releaseStats: List<ReleaseStat> = emptyList(),
    val userGeoLocation: UserGeoLocation? = null,
    val isAnalyticsLoading: Boolean = false,
    val customAnalyticsWebhookUrl: String = "",
    val analyticsPingStatus: String? = null,
    val sleepTimerState: SleepTimerState = SleepTimerState(),
    val autoMixTargetSongId: String? = null
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState = _uiState.asStateFlow()
    private val player = AudioPlayerManager(application)
    private val autoMixEngine = AutoMixEngine(application, player)
    private val repo = MusicRepository
    private val storage = LocalStorage
    private var autoMixTriggeredSongId: String? = null
    private var lastAutoMixHandoffTime = 0L
    private var lastHandoffSongId: String? = null
    private val sessionPlayedSongIds = mutableSetOf<String>()
    private val userQueuedSongIds = mutableSetOf<String>()
    @Volatile private var precomputedAutoMixCandidate: Song? = null
    private var precomputeJob: kotlinx.coroutines.Job? = null
    private var activePlayJob: kotlinx.coroutines.Job? = null
    private var activeLyricsJob: kotlinx.coroutines.Job? = null
    private var activeDownloadJob: kotlinx.coroutines.Job? = null
    @Volatile private var activeTargetSongId: String? = null
    private var sleepTimerJob: kotlinx.coroutines.Job? = null
    private var warningPopupJob: kotlinx.coroutines.Job? = null

    init {
        val userRole = if (com.beatles.player.BuildConfig.IS_ADMIN_EDITION) {
            UserRole.ADMIN
        } else {
            try { storage.getUserRole(application) } catch (_: Exception) { UserRole.LISTENER }
        }
        val isOnboarded = if (com.beatles.player.BuildConfig.IS_ADMIN_EDITION) true else (try { storage.isOnboarded(application) } catch (_: Exception) { false })
        val userName = try { storage.getUserName(application) } catch (_: Exception) { "" }
        val savedFavorites = try { storage.getFavorites(application) } catch (_: Exception) { emptyList() }
        val savedRecent = try { storage.getRecentlyPlayed(application) } catch (_: Exception) { emptyList() }
        val savedLanguage = try { storage.getSelectedLanguage(application).ifBlank { "All" } } catch (_: Exception) { "All" }
        val savedLanguages = try { storage.getSelectedLanguages(application) } catch (_: Exception) { emptySet() }
        val extendedAutoMix = try { storage.isExtendedAutoMix(application) } catch (_: Exception) { true }
        autoMixEngine.extendedWindow = extendedAutoMix
        val savedSelectedArtists = try { storage.getSelectedArtists(application) } catch (_: Exception) { emptySet() }
        val initialArtists = if (savedSelectedArtists.isNotEmpty()) savedSelectedArtists else try { savedFavorites.mapNotNull { it.artist }.toSet() } catch (_: Exception) { emptySet() }
        val affinityMap = try { storage.getArtistAffinityMap(application) } catch (_: Exception) { emptyMap() }
        val cachedAlbums = try { storage.getCachedPersonalizedAlbums(application) } catch (_: Exception) { emptyList() }
        val featured = if (cachedAlbums.isNotEmpty()) {
            cachedAlbums
        } else {
            try {
                repo.getPersonalizedFeaturedAlbums(
                    selectedArtists = initialArtists,
                    recentlyPlayed = savedRecent,
                    favorites = savedFavorites,
                    language = savedLanguage,
                    affinityMap = affinityMap
                )
            } catch (_: Exception) { repo.getFeaturedAlbums() }
        }
        val hero = featured.firstOrNull()
        val savedAlbums = try {
            storage.getAlbums(application).filter { alb ->
                featured.none { it.id == alb.id } && !alb.id.startsWith("alb_1") && !alb.id.startsWith("alb_2") && !alb.id.startsWith("alb_3")
            }
        } catch (_: Exception) { emptyList() }
        val allAlbums = featured + savedAlbums
        val savedSets = try { storage.getAlbumSets(application) } catch (_: Exception) { emptyList() }
        val defaultSets = try { repo.getPersonalizedAlbumSets(initialArtists, featured, affinityMap) } catch (_: Exception) { emptyList() }
        val allSets = if (savedSets.isEmpty()) {
            try { storage.saveAlbumSets(application, defaultSets) } catch (_: Exception) {}
            defaultSets
        } else {
            savedSets
        }
        val savedPlaylists = try {
            storage.getPlaylists(application).filter { !it.id.startsWith("pl_1") && !it.id.startsWith("pl_2") && !it.id.startsWith("pl_3") }
        } catch (_: Exception) { emptyList() }
        val themeMode = try { storage.getThemeMode(application) } catch (_: Exception) { ThemeMode.SYSTEM }
        val downloadedSongs = try { storage.getDownloadedSongs(application) } catch (_: Exception) { emptyList() }
        val initialRecs = try { repo.getRealPredictiveSongs() } catch (_: Exception) { emptyList() }
        val isSleepExtendEnabled = try { storage.isSleepExtendPopupEnabled(application) } catch (_: Exception) { true }

        val restoredSong = com.beatles.player.player.PlaybackService.activeSong ?: try { storage.getLastPlayedSong(application) } catch (_: Exception) { null }
        val restoredPlaylist = if (com.beatles.player.player.PlaybackService.activePlaylist.isNotEmpty()) {
            com.beatles.player.player.PlaybackService.activePlaylist
        } else {
            try { storage.getLastPlayedPlaylist(application) } catch (_: Exception) { emptyList() }
        }
        val isServicePlaying = com.beatles.player.player.PlaybackService.instance?.defaultPlayer?.isPlaying == true ||
            (autoMixEngine.currentDeck?.isPlaying == true)

        _uiState.update { it.copy(
            onboardStep = if (isOnboarded) 0 else 1,
            userName = userName,
            userRole = userRole,
            themeMode = themeMode,
            selectedLanguage = savedLanguage,
            selectedLanguages = if (savedLanguages.isNotEmpty()) savedLanguages else if (savedLanguage.isNotBlank() && savedLanguage != "All") setOf(savedLanguage) else emptySet(),
            extendedAutoMixEnabled = extendedAutoMix,
            heroAlbum = hero,
            albums = allAlbums,
            albumSets = allSets,
            playlists = savedPlaylists,
            downloadedSongs = downloadedSongs,
            favoriteSongs = savedFavorites,
            recentlyPlayed = savedRecent,
            recommendations = initialRecs,
            sleepTimerState = SleepTimerState(isExtendPopupEnabled = isSleepExtendEnabled),
            currentSong = restoredSong,
            playlist = if (restoredPlaylist.isNotEmpty()) restoredPlaylist else (restoredSong?.let { listOf(it) } ?: emptyList()),
            isPlaying = isServicePlaying,
            duration = (restoredSong?.durationSeconds?.toLong()?.times(1000L)) ?: 0L
        )}

        if (initialArtists.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val discovered = repo.discoverPersonalizedAlbums(initialArtists.toList())
                    if (discovered.isNotEmpty()) {
                        storage.savePersonalizedAlbums(application, discovered)
                        val updatedAlbums = (discovered + _uiState.value.albums).distinctBy { it.id }
                        val updatedSets = repo.getPersonalizedAlbumSets(initialArtists, updatedAlbums, affinityMap)
                        _uiState.update {
                            it.copy(
                                heroAlbum = discovered.firstOrNull() ?: it.heroAlbum,
                                albums = updatedAlbums,
                                albumSets = if (updatedSets.isNotEmpty()) updatedSets else it.albumSets
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        AudioPlayerManager.onExternalSkipNext = { playNext() }
        AudioPlayerManager.onExternalSkipPrevious = { playPrevious() }

        player.onPlaybackStateChanged = { isPlaying ->
            val actuallyPlaying = isPlaying || (autoMixEngine.currentDeck?.isPlaying == true)
            _uiState.update { it.copy(isPlaying = actuallyPlaying) }
        }
        var lastSongEndedTime = 0L
        player.onSongEnded = {
            val stState = _uiState.value.sleepTimerState
            if (stState.mode == SleepTimerMode.END_OF_TRACK || (stState.mode == SleepTimerMode.SONGS && stState.remainingSongs <= 1)) {
                executeSleepTimerShutdown()
            } else {
                val now = System.currentTimeMillis()
                val state = _uiState.value
                val currentSong = state.currentSong
                val isNotImmediatePostHandoffRepeat = (now - lastAutoMixHandoffTime >= 3_000L) || (currentSong != null && currentSong.id != lastHandoffSongId)
                if (isNotImmediatePostHandoffRepeat && (now - lastSongEndedTime > 1500L)) {
                    if (autoMixEngine.isBusy) {
                        viewModelScope.launch {
                            delay(500)
                            if (autoMixEngine.currentDeck?.isPlaying != true) {
                                autoMixEngine.abortTransition()
                                playNext(isAutoMix = true)
                            }
                        }
                    } else {
                        lastSongEndedTime = now
                        playNext(isAutoMix = true)
                    }
                }
            }
        }
        autoMixEngine.onSongEnded = {
            val stState = _uiState.value.sleepTimerState
            if (stState.mode == SleepTimerMode.END_OF_TRACK || (stState.mode == SleepTimerMode.SONGS && stState.remainingSongs <= 1)) {
                executeSleepTimerShutdown()
            } else {
                val now = System.currentTimeMillis()
                if (now - lastSongEndedTime > 1500L) {
                    lastSongEndedTime = now
                    android.util.Log.i("PlayerViewModel", "Deck B onSongEnded triggered; advancing playback")
                    playNext(isAutoMix = true)
                }
            }
        }
        autoMixEngine.urlResolver = { song -> resolvePlayableUrl(song) }
        autoMixEngine.onWatchdogRecover = { failedSong ->
            val now = System.currentTimeMillis()
            if (now - lastSongEndedTime > 1500L) {
                lastSongEndedTime = now
                android.util.Log.w("PlayerViewModel", "AutoMix watchdog recovered for ${failedSong.title}; advancing queue to prevent stop")
                playNext(isAutoMix = true)
            }
        }
        autoMixEngine.onMixerPosition = { pos, dur ->
            if (dur > 0) _uiState.update { it.copy(currentPosition = pos, duration = dur) }
        }
        viewModelScope.launch {
            autoMixEngine.autoMixState.collect { mixState ->
                _uiState.update { it.copy(autoMixState = mixState) }
            }
        }
        player.onPositionChanged = { pos, dur ->
            if (!_uiState.value.isLoadingStream && !autoMixEngine.isHandingOff) {
                _uiState.update { current ->
                    val effectiveDur = if (dur > 0L) dur else current.duration
                    current.copy(currentPosition = pos, duration = effectiveDur)
                }
            }
            val state = _uiState.value
            val currentSong = state.currentSong
            // Intelligent AutoMix: Evaluate toward outro across all track durations
            val minPlayTime = if (dur < 30_000L) (dur * 0.30f).toLong() else maxOf(12_000L, (dur * 0.55f).toLong())
            if (state.autoMixEnabled && state.repeatMode != 2 && dur >= 5_000L && currentSong != null && !state.isLoadingStream && (pos >= minPlayTime || dur - pos <= 65_000L)) {
                val now = System.currentTimeMillis()
                if (now - lastAutoMixHandoffTime >= 8_000L) {
                    val remaining = dur - pos
                    if (remaining in 1_200L..65_000L) {
                        val next = precomputedAutoMixCandidate ?: peekNextSong(state)
                        if (precomputedAutoMixCandidate == null) {
                            triggerCandidatePrecomputation(currentSong)
                        }
                        autoMixEngine.onPlaybackPositionUpdate(
                            posMs = pos,
                            durationMs = dur,
                            currentSong = currentSong,
                            nextSong = next,
                            lyrics = state.lyrics,
                            playlist = state.playlist,
                            onHandoff = { incomingSong, handoffPos -> onAutoMixHandoff(incomingSong, handoffPos) }
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            val tags = repo.getDynamicTrendingSearches()
            _uiState.update { it.copy(trendingTags = tags) }
        }
        player.onSongTransition = onTransition@ { mediaId ->
            val stState = _uiState.value.sleepTimerState
            if (stState.mode == SleepTimerMode.END_OF_TRACK) {
                executeSleepTimerShutdown()
            } else if (stState.mode == SleepTimerMode.SONGS) {
                val rem = stState.remainingSongs - 1
                if (rem <= 0) {
                    executeSleepTimerShutdown()
                } else {
                    _uiState.update { it.copy(sleepTimerState = it.sleepTimerState.copy(remainingSongs = rem)) }
                }
            } else if (stState.mode == SleepTimerMode.ALBUM) {
                val targetAlb = stState.targetAlbumId
                val newSong = _uiState.value.playlist.find { it.id == mediaId }
                if (targetAlb != null && newSong != null && newSong.album != targetAlb) {
                    executeSleepTimerShutdown()
                }
            }
            val now = System.currentTimeMillis()
            // Strictly guard against stale media events from cleared or inactive decks post-AutoMix
            if (!autoMixEngine.isBusy && !autoMixEngine.isHandingOff) {
                val activeDeckPlayer = autoMixEngine.currentDeck
                val activeMediaId = activeDeckPlayer?.currentMediaItem?.mediaId
                // If active deck is playing a specific media item and mediaId differs, ignore ghost transition
                if (activeMediaId != null && activeMediaId != mediaId) {
                    return@onTransition
                }
                // If recent AutoMix handoff occurred, ignore transitions for anything other than the handoff song
                if (lastHandoffSongId != null && mediaId != lastHandoffSongId && (now - lastAutoMixHandoffTime < 180_000L)) {
                    return@onTransition
                }
                val s = _uiState.value.playlist.find { it.id == mediaId }
                if (s != null && s.id != _uiState.value.currentSong?.id) {
                    _uiState.update { it.copy(currentSong = s, isFavorite = storage.isFavorite(getApplication(), s.id), lyrics = emptyList()) }
                    storage.addToRecentlyPlayed(getApplication(), s)
                    sessionPlayedSongIds.add(s.id)
                    _uiState.update { it.copy(recentlyPlayed = storage.getRecentlyPlayed(getApplication())) }
                    fetchLyrics(s)
                    enhanceCoverArt(s)
                    triggerCandidatePrecomputation(s)
                }
            }
        }
        player.onShuffleModeEnabledChanged = { enabled -> _uiState.update { it.copy(shuffleModeEnabled = enabled) } }
        player.onRepeatModeChanged = { mode -> _uiState.update { it.copy(repeatMode = mode) } }
        var playerErrorRetryCount = 0
        player.onPlayerError = { error ->
            val cur = _uiState.value.currentSong
            if (cur != null && playerErrorRetryCount < 2) {
                playerErrorRetryCount++
                repo.clearStreamCache(cur.id)
                viewModelScope.launch {
                    try {
                        val freshUrl = repo.getStreamUrl(cur.id, cur)
                        withContext(Dispatchers.Main) {
                            player.play(freshUrl, cur)
                        }
                    } catch (_: Exception) {
                        withContext(Dispatchers.Main) {
                            playNext()
                        }
                    }
                }
            } else if (cur != null) {
                playerErrorRetryCount = 0
                playNext()
            }
        }
        if (isOnboarded) {
            loadRecommendations()
            checkForUpdates(silent = true)
        }
        val savedWebhook = try { storage.getAnalyticsWebhookUrl(application) } catch (_: Exception) { "" }
        _uiState.update { it.copy(customAnalyticsWebhookUrl = savedWebhook) }
        viewModelScope.launch {
            try {
                AnalyticsManager.initAndTrack(application)
                refreshAnalytics()
            } catch (_: Exception) {}
        }
    }

    fun setOnboardStep(step: Int) { 
        _uiState.update { it.copy(onboardStep = step) } 
        if (step == 4) {
            loadRecommendationsFromArtists()
        }
    }
    fun setUserName(name: String) {
        storage.setUserName(getApplication(), name)
        _uiState.update { it.copy(userName = name) }
    }
    fun setSelectedLanguage(lang: String) {
        storage.setSelectedLanguage(getApplication(), lang)
        _uiState.update { it.copy(selectedLanguage = lang, selectedLanguages = setOf(lang)) }
        refreshPersonalizedMusic()
        loadRecommendations()
    }
    fun setSelectedLanguages(langs: Set<String>) {
        storage.setSelectedLanguages(getApplication(), langs)
        val primary = langs.firstOrNull() ?: "All"
        _uiState.update { it.copy(selectedLanguages = langs, selectedLanguage = primary) }
        refreshPersonalizedMusic()
        loadRecommendations()
    }
    fun toggleExtendedAutoMix() {
        val next = !_uiState.value.extendedAutoMixEnabled
        storage.setExtendedAutoMix(getApplication(), next)
        autoMixEngine.extendedWindow = next
        _uiState.update { it.copy(
            extendedAutoMixEnabled = next,
            downloadNotice = if (next) "Extended AutoMix on (60s out / 120s in)" else "Standard AutoMix on"
        )}
    }
    fun setSelectedArtists(artists: Set<String>) {
        storage.saveSelectedArtists(getApplication(), artists)
        _uiState.update { it.copy(selectedArtists = artists) }
        refreshPersonalizedMusic(forceFetchNew = true)
    }
    fun finishOnboarding() {
        storage.setOnboarded(getApplication())
        _uiState.update { it.copy(onboardStep = 0) }
        if (_uiState.value.recommendations.isEmpty() || _uiState.value.albums.isEmpty()) {
            refreshPersonalizedMusic(forceFetchNew = true)
        }
    }

    private var homeRefreshCount = 0L

    fun refreshPersonalizedMusic(forceFetchNew: Boolean = false) {
        val artists = _uiState.value.selectedArtists
        val recent = _uiState.value.recentlyPlayed
        val favorites = _uiState.value.favoriteSongs
        val lang = _uiState.value.selectedLanguage
        val affinityMap = storage.getArtistAffinityMap(getApplication())
        val cachedAlbums = storage.getCachedPersonalizedAlbums(getApplication())
        homeRefreshCount++

        val albums = repo.getPersonalizedFeaturedAlbums(
            selectedArtists = artists,
            recentlyPlayed = recent,
            favorites = favorites,
            language = lang,
            rotationSeed = homeRefreshCount,
            discoveredRealAlbums = if (!forceFetchNew) cachedAlbums else emptyList(),
            affinityMap = affinityMap
        )
        val sets = repo.getPersonalizedAlbumSets(artists, albums, affinityMap)
        val hero = albums.firstOrNull()
        _uiState.update { it.copy(albums = albums, albumSets = sets, heroAlbum = hero) }

        // Dynamic discovery for user's preference memory
        viewModelScope.launch {
            try {
                val affinityArtists = affinityMap.entries.sortedByDescending { it.value }.map { it.key }
                val targetArtists = (affinityArtists + artists + recent.map { it.artist } + favorites.map { it.artist })
                    .filter { it.isNotBlank() }
                    .distinct()
                    .take(6)

                if (targetArtists.isNotEmpty()) {
                    val realAlbums = repo.discoverPersonalizedAlbums(targetArtists, rotationSeed = homeRefreshCount)
                    if (realAlbums.isNotEmpty()) {
                        storage.savePersonalizedAlbums(getApplication(), realAlbums)
                        val updatedSets = repo.getPersonalizedAlbumSets(artists, realAlbums, affinityMap)
                        val updatedHero = realAlbums.firstOrNull()
                        _uiState.update {
                            it.copy(
                                albums = realAlbums,
                                albumSets = updatedSets,
                                heroAlbum = updatedHero
                            )
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }
    fun setTab(tab: Tab) {
        if (tab != Tab.SEARCH) {
            clearSearch()
        }
        _uiState.update { it.copy(currentTab = tab) }
    }
    fun setSeeAllSection(section: String?) { _uiState.update { it.copy(showSeeAllSection = section) } }

    fun setProfileOpen(open: Boolean) { _uiState.update { it.copy(showProfilePage = open) } }
    
    fun setUserRole(role: UserRole) {
        storage.setUserRole(getApplication(), role)
        _uiState.update { it.copy(userRole = role, showProfilePage = false) }
    }

    fun checkForUpdates(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _uiState.update { it.copy(isCheckingUpdate = true, updateErrorMessage = null, updateNotice = null) }
            val currentVersion = try {
                com.beatles.player.BuildConfig.VERSION_NAME
            } catch (_: Exception) {
                try {
                    val app = getApplication<Application>()
                    app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: "1.4"
                } catch (_: Exception) {
                    "1.4"
                }
            }
            val update = com.beatles.player.data.AppUpdateManager.checkForUpdates(currentVersion)
            val dismissedVersion = storage.getDismissedUpdateVersion(getApplication())
            val lastInstalledVersion = storage.getLastInstalledUpdateVersion(getApplication())

            // If checked silently on app launch, do NOT pop up modal if user already dismissed or installed this version!
            val shouldShowDialog = update != null && (!silent || (update.versionName != dismissedVersion && update.versionName != lastInstalledVersion))

            _uiState.update {
                it.copy(
                    isCheckingUpdate = false,
                    availableUpdate = if (shouldShowDialog) update else null,
                    updateNotice = if (update == null && !silent) "Beatles is up to date (v$currentVersion)" else null
                )
            }
        }
    }

    fun dismissUpdate() {
        val currentUpdate = _uiState.value.availableUpdate
        if (currentUpdate != null) {
            storage.setDismissedUpdateVersion(getApplication(), currentUpdate.versionName)
        }
        _uiState.update { it.copy(availableUpdate = null, updateDownloadProgress = null, updateDownloadStatus = null, updateErrorMessage = null) }
    }

    fun dismissUpdateNotice() {
        _uiState.update { it.copy(updateNotice = null) }
    }

    fun downloadAndInstallUpdate(context: android.content.Context) {
        val update = _uiState.value.availableUpdate ?: return
        storage.setLastInstalledUpdateVersion(getApplication(), update.versionName)
        viewModelScope.launch {
            _uiState.update { it.copy(updateDownloadProgress = 0f, updateDownloadStatus = "Connecting...", updateErrorMessage = null) }
            com.beatles.player.data.AppUpdateManager.downloadAndInstallApk(
                context = context,
                apkUrl = update.apkUrl,
                onProgress = { progress, status ->
                    _uiState.update { it.copy(updateDownloadProgress = progress, updateDownloadStatus = status) }
                },
                onError = { err ->
                    _uiState.update { it.copy(updateDownloadProgress = null, updateDownloadStatus = null, updateErrorMessage = err) }
                }
            )
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        storage.setThemeMode(getApplication(), mode)
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setArtistTab(tab: ArtistTab) { _uiState.update { it.copy(artistTab = tab) } }
    fun refreshAnalytics() {
        _uiState.update { it.copy(isAnalyticsLoading = true) }
        viewModelScope.launch {
            try {
                val (total, releases) = AnalyticsManager.fetchGitHubDownloadStats()
                val geo = AnalyticsManager.fetchUserGeoLocation(getApplication())
                _uiState.update {
                    it.copy(
                        totalAppDownloads = total,
                        releaseStats = releases,
                        userGeoLocation = geo,
                        isAnalyticsLoading = false
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isAnalyticsLoading = false) }
            }
        }
    }

    fun setAnalyticsWebhookUrl(url: String) {
        storage.setAnalyticsWebhookUrl(getApplication(), url)
        _uiState.update { it.copy(customAnalyticsWebhookUrl = url, analyticsPingStatus = null) }
    }

    fun testAnalyticsPing(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(analyticsPingStatus = "Sending test ping...") }
            val (success, msg) = AnalyticsManager.sendTestPing(getApplication(), url)
            _uiState.update { it.copy(analyticsPingStatus = if (success) "✓ $msg" else "✗ $msg") }
        }
    }

    fun setAdminTab(tab: AdminTab) { _uiState.update { it.copy(adminTab = tab) } }

    fun setSelectedAlbum(album: Album?, bounds: Rect? = null) {
        val initialVideo = album?.let { it.videoUrl ?: repo.getCuratedMotionArtwork(it.title, it.artist, it.title) }
        val effectiveAlbum = if (album != null && initialVideo != null && album.videoUrl == null) album.copy(videoUrl = initialVideo) else album
        _uiState.update { it.copy(selectedAlbum = effectiveAlbum, selectedAlbumBounds = bounds) }
        if (effectiveAlbum != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val hq = repo.getHighestQualityCover(effectiveAlbum.coverUrl)
                    if (hq.isNotBlank() && hq.startsWith("http")) {
                        val req = coil.request.ImageRequest.Builder(getApplication())
                            .data(hq)
                            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                            .build()
                        coil.Coil.imageLoader(getApplication()).enqueue(req)
                    }
                    val albVid = effectiveAlbum.videoUrl ?: effectiveAlbum.songs.firstOrNull()?.videoUrl
                    if (!albVid.isNullOrBlank() && albVid.startsWith("http")) {
                        com.beatles.player.player.BeatlesMediaCache.preload(getApplication(), albVid)
                    }
                } catch (_: Exception) {}
            }
        }
        if (album != null) {
            viewModelScope.launch {
                val motionVideo = effectiveAlbum?.videoUrl ?: repo.resolveMotionArtwork(album.title, album.artist, album.title)
                if (album.songs.isEmpty()) {
                    val resolved = try {
                        repo.lookupAlbumById(album.id) ?: repo.searchOriginalAlbum("${album.title} ${album.artist}")
                    } catch (_: Exception) { null }
                    if (resolved != null && resolved.songs.isNotEmpty()) {
                        val fullAlbum = album.copy(songs = resolved.songs, videoUrl = motionVideo ?: album.videoUrl)
                        _uiState.update { st ->
                            if (st.selectedAlbum?.id == album.id) {
                                st.copy(selectedAlbum = fullAlbum)
                            } else st
                        }
                        return@launch
                    }
                }
                if (motionVideo != null && effectiveAlbum?.videoUrl != motionVideo) {
                    _uiState.update { st ->
                        if (st.selectedAlbum?.id == album.id) {
                            st.copy(selectedAlbum = st.selectedAlbum?.copy(videoUrl = motionVideo))
                        } else st
                    }
                }
            }
        }
    }

    fun setSelectedArtist(artistName: String?, bounds: Rect? = null) {
        _uiState.update { it.copy(selectedArtist = artistName, selectedArtistBounds = bounds) }
    }

    fun openArtist(artistName: String, bounds: Rect? = null) {
        val clean = artistName.replace(" - Topic", "").trim()
            .split(",", "&", "feat.", "ft.", "/").firstOrNull()?.trim()
            ?.ifBlank { artistName.trim() } ?: artistName.trim()
        _uiState.update { it.copy(selectedArtist = clean, selectedArtistBounds = bounds, isExpanded = false, selectedAlbum = null) }
    }

    fun setSelectedGenre(genre: String?, bounds: Rect? = null) {
        _uiState.update { it.copy(selectedGenre = genre, selectedGenreBounds = bounds) }
    }

    fun clearSelectedSongBounds() {
        _uiState.update { it.copy(selectedSongBounds = null) }
    }

    fun setSelectedAlbumSet(set: AlbumSet?, bounds: Rect? = null) {
        _uiState.update { it.copy(selectedAlbumSet = set, selectedAlbumSetBounds = bounds) }
        if (set != null && set.albums.any { it.songs.isEmpty() }) {
            viewModelScope.launch {
                val updatedAlbums = set.albums.map { alb ->
                    if (alb.songs.isNotEmpty()) alb
                    else {
                        val resolved = try {
                            repo.lookupAlbumById(alb.id) ?: repo.searchOriginalAlbum("${alb.title} ${alb.artist}")
                        } catch (_: Exception) { null }
                        if (resolved != null && resolved.songs.isNotEmpty()) alb.copy(songs = resolved.songs) else alb
                    }
                }
                val fullSet = set.copy(albums = updatedAlbums)
                _uiState.update { st ->
                    if (st.selectedAlbumSet?.id == set.id) {
                        st.copy(selectedAlbumSet = fullSet)
                    } else st
                }
            }
        }
    }
    
    fun openAlbumForSong(song: Song) {
        viewModelScope.launch {
            val existing = _uiState.value.albums.find { alb ->
                alb.songs.any { s -> s.id == song.id } ||
                (alb.title.equals(song.album, ignoreCase = true) && !listOf("single", "related", "recommended", "various").contains(song.album.lowercase().trim())) ||
                (alb.artist.equals(song.artist, ignoreCase = true) && alb.title.equals(song.album, ignoreCase = true))
            } ?: _uiState.value.albums.find { alb ->
                alb.artist.equals(song.artist, ignoreCase = true) && 
                !listOf("single", "related", "recommended", "various").contains(song.album.lowercase().trim()) &&
                (alb.title.contains(song.album, ignoreCase = true) || song.album.contains(alb.title, ignoreCase = true))
            }
            val album = existing ?: repo.getAlbumForSong(song)
            setSelectedAlbum(album)
            _uiState.update { it.copy(isExpanded = false) }
        }
    }

    fun setShowAddAlbumDialog(show: Boolean) { _uiState.update { it.copy(showAddAlbumDialog = show) } }

    fun createAlbum(title: String, artist: String, genre: String, coverUrl: String, songs: List<Song>) {
        val newAlbum = Album(
            id = "alb_${System.currentTimeMillis()}",
            title = title.ifBlank { "Untitled Album" },
            artist = artist.ifBlank { _uiState.value.userName.ifBlank { "Artist" } },
            coverUrl = coverUrl.ifBlank { "https://c.saavncdn.com/artists/Karan_Aujla_004_20230809115159_500x500.jpg" },
            genre = genre.ifBlank { "Pop" },
            releaseYear = "2026",
            songs = songs
        )
        val updatedAlbums = listOf(newAlbum) + _uiState.value.albums
        storage.saveAlbums(getApplication(), updatedAlbums)
        _uiState.update { it.copy(albums = updatedAlbums, showAddAlbumDialog = false) }
    }

    fun addSongToAlbum(albumId: String, song: Song) {
        val updated = _uiState.value.albums.map { alb ->
            if (alb.id == albumId) {
                alb.copy(songs = alb.songs + song)
            } else alb
        }
        storage.saveAlbums(getApplication(), updated)
        val currentSelected = _uiState.value.selectedAlbum
        val updatedSelected = if (currentSelected?.id == albumId) {
            currentSelected.copy(songs = currentSelected.songs + song)
        } else currentSelected
        _uiState.update { it.copy(albums = updated, selectedAlbum = updatedSelected) }
    }

    fun approveTrack(id: String) {
        _uiState.update { state ->
            state.copy(trackSubmissions = state.trackSubmissions.map {
                if (it.id == id) it.copy(status = "approved") else it
            })
        }
    }

    fun rejectTrack(id: String) {
        _uiState.update { state ->
            state.copy(trackSubmissions = state.trackSubmissions.map {
                if (it.id == id) it.copy(status = "rejected") else it
            })
        }
    }

    fun toggleUserRole(id: String) {
        _uiState.update { state ->
            state.copy(mockUsers = state.mockUsers.map { u ->
                if (u.id == id) {
                    val nextRole = if (u.role == UserRole.LISTENER) UserRole.ARTIST else UserRole.LISTENER
                    u.copy(role = nextRole)
                } else u
            })
        }
    }

    fun deleteUser(id: String) {
        _uiState.update { state ->
            state.copy(mockUsers = state.mockUsers.filter { it.id != id })
        }
    }

    fun sendFanMessage(fromUser: String, text: String) {
        if (text.isBlank()) return
        val newMsg = FanMessage(
            id = "fm_${System.currentTimeMillis()}",
            fromUser = fromUser.ifBlank { _uiState.value.userName.ifBlank { "You" } },
            text = text,
            timeAgo = "Just now"
        )
        _uiState.update { state ->
            state.copy(fanMessages = listOf(newMsg) + state.fanMessages)
        }
    }

    fun submitArtistTrack(title: String, album: String, genre: String) {
        val newSub = TrackSubmission(
            id = "sub_${System.currentTimeMillis()}",
            title = title.ifBlank { "New Single" },
            artist = _uiState.value.userName.ifBlank { "You" },
            album = album.ifBlank { "Single" },
            genre = genre.ifBlank { "Pop" },
            submittedDate = "Today",
            status = "pending"
        )
        _uiState.update { state ->
            state.copy(trackSubmissions = listOf(newSub) + state.trackSubmissions)
        }
    }

    fun logout() {
        storage.setUserRole(getApplication(), UserRole.LISTENER)
        storage.setOnboarded(getApplication()) // or keep onboarded
        _uiState.update { it.copy(userRole = UserRole.LISTENER, showProfilePage = false) }
    }

    fun createPlaylist(name: String, song: Song? = null) {
        val newPl = Playlist(
            id = "pl_${System.currentTimeMillis()}",
            name = name.ifBlank { "New Playlist" },
            songs = if (song != null) listOf(song) else emptyList()
        )
        val updated = listOf(newPl) + _uiState.value.playlists
        storage.savePlaylists(getApplication(), updated)
        _uiState.update { it.copy(playlists = updated, downloadNotice = "Added to playlist \"${newPl.name}\"") }
    }

    fun addSongToPlaylist(playlistId: String, song: Song) {
        val updated = _uiState.value.playlists.map { pl ->
            if (pl.id == playlistId) {
                if (pl.songs.none { it.id == song.id }) pl.copy(songs = pl.songs + song) else pl
            } else pl
        }
        storage.savePlaylists(getApplication(), updated)
        val targetName = _uiState.value.playlists.find { it.id == playlistId }?.name ?: "Playlist"
        _uiState.update { it.copy(playlists = updated, downloadNotice = "Added to \"$targetName\"") }
    }

    fun downloadSongHighestQuality(song: Song) {
        activeDownloadJob?.cancel()
        activeDownloadJob = viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    downloadingSongId = song.id,
                    downloadProgress = 0.0f,
                    downloadFailedSongId = null,
                    downloadNotice = "⬇ Downloading \"${song.title}\" in highest quality..."
                ) 
            }
            try {
                val streamUrl = repo.getStreamUrl(song.id)
                _uiState.update { 
                    if (it.downloadingSongId == song.id) it.copy(downloadProgress = 0.01f) else it 
                }
                val savedUri = DownloadHelper.downloadDirectly(
                    context = getApplication(),
                    song = song,
                    streamUrl = streamUrl,
                    onProgress = { progressFraction ->
                        _uiState.update {
                            if (it.downloadingSongId == song.id) {
                                it.copy(downloadProgress = progressFraction)
                            } else it
                        }
                    }
                )
                DownloadHelper.downloadToMusicFolder(getApplication(), song, streamUrl)

                val safeArtist = song.artist.replace(Regex("[^a-zA-Z0-9.-]"), "_").trim('_')
                val safeTitle = song.title.replace(Regex("[^a-zA-Z0-9.-]"), "_").trim('_')
                val fileName = "${safeArtist}_-_${safeTitle}.m4a"
                val appMusicDir = java.io.File(getApplication<Application>().getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC), "Beatles")
                val targetFile = java.io.File(appMusicDir, fileName)
                val finalUri = savedUri ?: android.net.Uri.fromFile(targetFile).toString()
                val downloadedSong = song.copy(streamUrl = finalUri)

                storage.saveDownloadedSong(getApplication(), downloadedSong)
                _uiState.update { 
                    it.copy(
                        downloadingSongId = null,
                        downloadProgress = 1f,
                        downloadedSongs = storage.getDownloadedSongs(getApplication()),
                        downloadNotice = "✓ Saved \"${song.title}\" for offline streaming"
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        downloadingSongId = null,
                        downloadProgress = 0f,
                        downloadFailedSongId = song.id,
                        downloadNotice = "Could not start download for \"${song.title}\""
                    ) 
                }
            }
        }
    }

    fun cancelDownload(songId: String) {
        if (_uiState.value.downloadingSongId == songId) {
            activeDownloadJob?.cancel()
            _uiState.update { it.copy(downloadingSongId = null, downloadProgress = 0f) }
        }
    }

    fun removeDownloadedSong(songId: String) {
        storage.removeDownloadedSong(getApplication(), songId)
        _uiState.update { 
            it.copy(
                downloadedSongs = storage.getDownloadedSongs(getApplication()),
                downloadNotice = "Removed from offline library"
            ) 
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update { it.copy(searchQuery = "", searchResults = emptyList(), searchedAlbum = null, searchSuggestions = emptyList(), instantMatchingSongs = emptyList(), isSearching = false) }
    }

    fun dismissDownloadNotice() {
        _uiState.update { it.copy(downloadNotice = null) }
    }

    /** Called on every keystroke. Debounced; keeps the previous results on screen until new ones land. */
    fun fetchSearchSuggestions(query: String) = onSearchQueryChanged(query)

    fun onSearchQueryChanged(query: String) {
        if (query.isBlank()) { clearSearch(); return }
        _uiState.update { it.copy(searchQuery = query) }
        val key = normalizeQuery(query)

        // Instant 0ms fuzzy search against active catalog (recommendations, recents, favorites, albums, downloaded)
        val state = _uiState.value
        val localCatalog = (state.recommendations + state.recentlyPlayed + state.favoriteSongs + state.downloadedSongs + state.albums.flatMap { it.songs }).distinctBy { it.id }
        val instantFuzzyMatches = repo.fuzzySearchCatalog(query, localCatalog)
        if (instantFuzzyMatches.isNotEmpty()) {
            _uiState.update { it.copy(instantMatchingSongs = instantFuzzyMatches.take(6)) }
        }

        searchCache[key]?.let { cached ->
            searchJob?.cancel()
            _uiState.update { it.copy(searchResults = cached.songs, instantMatchingSongs = cached.songs.take(4), searchedAlbum = cached.album, searchSuggestions = cached.suggestions, isSearching = false) }
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            // Live fetch related suggestions in parallel
            launch(Dispatchers.IO) {
                val suggs = repo.getSearchSuggestions(query)
                if (normalizeQuery(_uiState.value.searchQuery) == key && suggs.isNotEmpty()) {
                    _uiState.update { it.copy(searchSuggestions = suggs) }
                }
            }
            kotlinx.coroutines.delay(280)
            runSearch(query)
        }
    }

    private data class CachedSearch(val songs: List<Song>, val album: Album?, val suggestions: List<String>)
    private val searchCache = object : LinkedHashMap<String, CachedSearch>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedSearch>?) = size > 40
    }
    private var searchJob: kotlinx.coroutines.Job? = null

    private fun normalizeQuery(q: String) = q.trim().lowercase().replace(Regex("\\s+"), " ")

    private suspend fun runSearch(query: String) {
        val key = normalizeQuery(query)
        _uiState.update { it.copy(isSearching = true) }
        kotlinx.coroutines.coroutineScope {
            val songsD = async { repo.search(query) }
            val suggD = async { repo.getSearchSuggestions(query) }
            val albumD = async { try { repo.searchOriginalAlbum(query) } catch (_: Exception) { null } }

            val songs = songsD.await()
            if (normalizeQuery(_uiState.value.searchQuery) != key) return@coroutineScope
            _uiState.update { it.copy(searchResults = songs, instantMatchingSongs = songs.take(4), isSearching = false) }

            val suggestions = suggD.await()
            if (normalizeQuery(_uiState.value.searchQuery) == key) _uiState.update { it.copy(searchSuggestions = suggestions) }

            val album = albumD.await()
            if (normalizeQuery(_uiState.value.searchQuery) == key) _uiState.update { it.copy(searchedAlbum = album) }

            if (songs.isNotEmpty()) searchCache[key] = CachedSearch(songs, album, suggestions)
        }
    }

    fun moveSongToPlayNext(song: Song) {
        userQueuedSongIds.add(song.id)
        cachedAutoMixSongId = null
        cachedAutoMixCandidate = null
        precomputedAutoMixCandidate = song
        autoMixEngine.resetPlanning(keepSongId = true)
        _uiState.update { state ->
            val currentSong = state.currentSong
            val currentPl = state.playlist.toMutableList()
            currentPl.removeAll { it.id == song.id }
            if (currentSong != null) {
                val currIdx = currentPl.indexOfFirst { it.id == currentSong.id }
                if (currIdx >= 0) {
                    currentPl.add(currIdx + 1, song)
                } else {
                    currentPl.add(0, song)
                }
            } else {
                currentPl.add(0, song)
            }
            state.copy(
                playlist = currentPl,
                autoMixTargetSongId = song.id,
                downloadNotice = "Playing \"${song.title}\" next"
            )
        }
        val currentSong = _uiState.value.currentSong
        if (currentSong != null) {
            triggerCandidatePrecomputation(currentSong)
        }
    }

    fun removeSongFromQueue(songId: String) {
        _uiState.update { state ->
            val currentPl = state.playlist.filter { it.id != songId }
            state.copy(playlist = currentPl)
        }
    }

    fun moveSongInQueue(fromIdx: Int, toIdx: Int) {
        _uiState.update { state ->
            try {
                val currentSong = state.currentSong
                val currentPl = state.playlist.toMutableList()
                val currIdx = if (currentSong != null) currentPl.indexOfFirst { it.id == currentSong.id } else -1
                val upcomingStart = if (currIdx >= 0) currIdx + 1 else 0
                val actualFrom = upcomingStart + fromIdx
                val actualTo = upcomingStart + toIdx
                if (actualFrom in currentPl.indices && actualTo in currentPl.indices) {
                    val item = currentPl.removeAt(actualFrom)
                    currentPl.add(actualTo, item)
                    state.copy(playlist = currentPl)
                } else {
                    state
                }
            } catch (_: Exception) {
                state
            }
        }
    }

    fun playAlbumSet(set: AlbumSet) {
        val allSongs = set.albums.flatMap { it.songs }.distinctBy { it.id }
        if (allSongs.isNotEmpty()) {
            playSong(allSongs.first(), allSongs)
            _uiState.update { it.copy(downloadNotice = "Playing all songs from \"${set.name}\" (${allSongs.size} tracks)") }
        }
    }

    fun createAlbumSet(name: String, album: Album? = null) {
        val newSet = AlbumSet(
            id = "set_${System.currentTimeMillis()}",
            name = name.ifBlank { "My Album Set" },
            albums = if (album != null) listOf(album) else emptyList()
        )
        val updated = listOf(newSet) + _uiState.value.albumSets
        storage.saveAlbumSets(getApplication(), updated)
        _uiState.update { it.copy(albumSets = updated, downloadNotice = "Created album set \"${newSet.name}\"") }
    }

    fun addAlbumToSet(setId: String, album: Album) {
        val updated = _uiState.value.albumSets.map { s ->
            if (s.id == setId) {
                if (s.albums.none { it.id == album.id }) s.copy(albums = s.albums + album) else s
            } else s
        }
        storage.saveAlbumSets(getApplication(), updated)
        val setName = _uiState.value.albumSets.find { it.id == setId }?.name ?: "Album Set"
        _uiState.update { it.copy(albumSets = updated, downloadNotice = "Added to \"$setName\"") }
    }

    fun saveAlbumToLibrary(album: Album) {
        val existing = _uiState.value.albums
        if (existing.none { it.id == album.id }) {
            val updated = listOf(album) + existing
            storage.saveAlbums(getApplication(), updated)
            _uiState.update { it.copy(albums = updated, downloadNotice = "Saved \"${album.title}\" to Library") }
        } else {
            _uiState.update { it.copy(downloadNotice = "\"${album.title}\" is already in Library") }
        }
    }

    fun loadMoreTopSongs() {
        if (_uiState.value.isTopHitsLoading) return
        val nextPage = _uiState.value.topHitsPage + 1
        _uiState.update { it.copy(isTopHitsLoading = true, topHitsPage = nextPage) }
        viewModelScope.launch {
            try {
                val moreSongs = repo.getTopHits(nextPage)
                val existingIds = _uiState.value.recommendations.map { it.id }.toSet()
                val newUnique = moreSongs.filter { it.id !in existingIds }
                _uiState.update { it.copy(
                    recommendations = it.recommendations + newUnique,
                    isTopHitsLoading = false
                ) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isTopHitsLoading = false) }
            }
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            clearSearch()
            return
        }
        _uiState.update { it.copy(searchQuery = query) }
        val key = normalizeQuery(query)
        searchJob?.cancel()
        searchCache[key]?.let { cached ->
            _uiState.update { it.copy(searchResults = cached.songs, instantMatchingSongs = cached.songs.take(4), searchedAlbum = cached.album, searchSuggestions = cached.suggestions, isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch { runSearch(query) }
    }

    fun playSong(
        song: Song,
        playlist: List<Song>? = null,
        isFromSearch: Boolean = false,
        isAutoMix: Boolean = false,
        sourceBounds: Rect? = null,
        keepExpandedState: Boolean = false
    ) {
        if (song.isAlbum) {
            viewModelScope.launch {
                val tracks = repo.getAlbumTracks(song.id)
                if (tracks.isNotEmpty()) playSong(tracks.first(), tracks, sourceBounds = sourceBounds)
            }
            return
        }
        if (_uiState.value.currentSong?.id == song.id && !_uiState.value.isLoadingStream && !isAutoMix) { togglePlayPause(); return }

        activePlayJob?.cancel()
        activeLyricsJob?.cancel()

        if (!isAutoMix) {
            autoMixEngine.abortTransition(stopPlayback = true)
            autoMixEngine.resetToDefaultDeck()
            // Immediately stop old song and reset seekbar to 0 so there is no residual audio playback
            player.pause()
            player.stop()
            autoMixTriggeredSongId = null
            cachedAutoMixSongId = null
            cachedAutoMixCandidate = null
        }

        val songLang = repo.detectLanguage(song)
        val songGenre = repo.detectGenre(song)
        storage.recordPlaybackContext(getApplication(), song.artist, songGenre, songLang)

        val isExplicitAlbumOrPlaylist = playlist != null && playlist.size > 1 && 
            (playlist.all { it.album.isNotBlank() && it.album == song.album } || _uiState.value.isArtistMixActive)

        val rawPl = if (isFromSearch) mutableListOf(song) else (playlist?.toMutableList() ?: mutableListOf(song))
        val pl = if (isExplicitAlbumOrPlaylist) {
            rawPl
        } else {
            // Strictly preserve genre & language consistency: never mix disparate languages in autoplay queue
            val cohesive = rawPl.filter { it.id == song.id || repo.detectLanguage(it).equals(songLang, ignoreCase = true) }.toMutableList()
            if (cohesive.none { it.id == song.id }) cohesive.add(0, song)
            cohesive
        }
        
        // Immediate Next Up population when autoplay is enabled so Next Up is NEVER empty,
        // strictly matching the active track's genre and language without flooding the queue with one singer
        if (_uiState.value.autoPlayEnabled && pl.size <= 1) {
            val cleanArtist = song.artist.replace(" - Topic", "").trim()
            val localPool = (if (isFromSearch) _uiState.value.searchResults else emptyList()) +
                    _uiState.value.albums.flatMap { it.songs } +
                    _uiState.value.favoriteSongs +
                    _uiState.value.recentlyPlayed

            // Take at most 1 same-artist match to keep the queue versatile
            val sameArtistMatches = localPool.filter {
                it.id != song.id && !it.isAlbum && ArtistMixEngine.isArtistMatch(it, cleanArtist) &&
                repo.detectLanguage(it).equals(songLang, ignoreCase = true)
            }.distinctBy { it.id }

            if (sameArtistMatches.isNotEmpty()) {
                pl.add(sameArtistMatches.first())
            }

            // Complement with 1-2 tracks from other singers in the same language & genre
            val otherArtistMatches = localPool.filter {
                it.id != song.id && !it.isAlbum && !ArtistMixEngine.isArtistMatch(it, cleanArtist) &&
                repo.detectLanguage(it).equals(songLang, ignoreCase = true)
            }.distinctBy { it.id }

            if (otherArtistMatches.isNotEmpty()) {
                pl.addAll(otherArtistMatches.take(2))
            }
        }

        val isRetainingArtistMix = _uiState.value.isArtistMixActive && 
            (_uiState.value.playlist.any { it.id == song.id } || pl.any { it.id == song.id } || isAutoMix ||
             (_uiState.value.artistMixTarget != null && ArtistMixEngine.isArtistMatch(song, _uiState.value.artistMixTarget!!)))

        val effectiveVideo = song.videoUrl ?: repo.getCuratedMotionArtwork(song.title, song.artist, song.album)
        val immediateHqCover = repo.resolveImmediateHqCover(song)
        val readySong = song.copy(
            coverUrl = if (immediateHqCover.isNotBlank()) immediateHqCover else song.coverUrl,
            videoUrl = if (effectiveVideo != null) effectiveVideo else song.videoUrl
        )

        _uiState.update { it.copy(
            currentSong = readySong, isPlaying = true, isExpanded = if (isAutoMix || keepExpandedState) it.isExpanded else true, isLoadingStream = true,
            selectedSongBounds = sourceBounds,
            isArtistMixActive = isRetainingArtistMix,
            queueTitle = if (isRetainingArtistMix) it.queueTitle else null,
            artistMixTarget = if (isRetainingArtistMix) it.artistMixTarget else null,
            playlist = pl.map { s -> if (s.id == readySong.id) readySong else s }, currentPosition = 0L, duration = readySong.durationSeconds * 1000L,
            isFavorite = storage.isFavorite(getApplication(), readySong.id), lyrics = emptyList(), showLyrics = false
        )}
        com.beatles.player.player.PlaybackService.activeSong = readySong
        com.beatles.player.player.PlaybackService.activePlaylist = pl
        storage.saveLastPlayedSong(getApplication(), readySong)
        storage.saveLastPlayedPlaylist(getApplication(), pl)
        storage.addToRecentlyPlayed(getApplication(), readySong)
        sessionPlayedSongIds.add(readySong.id)
        viewModelScope.launch {
            try {
                AnalyticsManager.logEvent(
                    getApplication(),
                    "song_play",
                    mapOf("title" to readySong.title, "artist" to readySong.artist, "genre" to readySong.genre)
                )
            } catch (_: Exception) {}
        }
        _uiState.update { it.copy(recentlyPlayed = storage.getRecentlyPlayed(getApplication())) }
        fetchLyrics(readySong)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val hqCover = repo.getHighestQualityCover(readySong.coverUrl)
                if (hqCover.isNotBlank() && hqCover.startsWith("http")) {
                    val req = coil.request.ImageRequest.Builder(getApplication())
                        .data(hqCover)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        .build()
                    coil.Coil.imageLoader(getApplication()).enqueue(req)
                }
                if (!readySong.videoUrl.isNullOrBlank() && readySong.videoUrl.startsWith("http")) {
                    com.beatles.player.player.BeatlesMediaCache.preload(getApplication(), readySong.videoUrl)
                }
                val nextSong = pl.getOrNull(1)
                if (nextSong != null) {
                    val nextCover = repo.getHighestQualityCover(nextSong.coverUrl)
                    if (nextCover.isNotBlank() && nextCover.startsWith("http")) {
                        val reqNext = coil.request.ImageRequest.Builder(getApplication())
                            .data(nextCover)
                            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                            .build()
                        coil.Coil.imageLoader(getApplication()).enqueue(reqNext)
                    }
                    if (!nextSong.videoUrl.isNullOrBlank() && nextSong.videoUrl.startsWith("http")) {
                        com.beatles.player.player.BeatlesMediaCache.preload(getApplication(), nextSong.videoUrl)
                    }
                }
            } catch (_: Exception) {}
        }
        
        activePlayJob?.cancel()
        activeTargetSongId = readySong.id

        activePlayJob = viewModelScope.launch {
            try {
                val url = resolvePlayableUrl(readySong) ?: throw Exception("No playable source")

                if (activeTargetSongId != readySong.id) return@launch

                if (isAutoMix) {
                    player.playWithFadeIn(url, readySong, 1800L)
                } else {
                    player.play(url, readySong)
                }

                if (activeTargetSongId != readySong.id) return@launch

                _uiState.update { it.copy(isLoadingStream = false) }
                triggerCandidatePrecomputation(readySong)
                enhanceCoverArt(readySong)
                
                // Only fetch and append related songs if autoplay is enabled AND we are near the end of the current playlist
                if (_uiState.value.autoPlayEnabled) {
                    val currentPl = _uiState.value.playlist
                    val currIdx = currentPl.indexOfFirst { it.id == song.id }
                    val isNearEndOfQueue = currIdx < 0 || currIdx >= currentPl.size - 2
                    if (isNearEndOfQueue) {
                        launch {
                            if (_uiState.value.isArtistMixActive && _uiState.value.artistMixTarget != null) {
                                extendArtistMixIfNeeded()
                            } else {
                                val related = if (isFromSearch) {
                                    val searchArtist = song.artist.replace(" - Topic", "").trim()
                                    // 1. YouTube's recommendation algorithm provides related tracks & similar singers
                                    val rel = repo.getRelatedSongs(song).filter { !it.isAlbum && it.id != song.id }
                                    // 2. Fetch at most 2 tracks from the same artist
                                    val artistTracks = repo.search("$searchArtist songs")
                                        .filter { !it.isAlbum && it.id != song.id && repo.detectLanguage(it).equals(songLang, ignoreCase = true) }
                                        .take(2)
                                    // 3. Search for top hits in the same genre and language
                                    val genreQuery = when (songLang) {
                                        "Hindi" -> "top bollywood $songGenre hits"
                                        "Punjabi" -> "top punjabi $songGenre hits"
                                        "English" -> "top $songGenre hits"
                                        else -> "top $songLang $songGenre hits"
                                    }
                                    val genreTracks = repo.search(genreQuery)
                                        .filter { !it.isAlbum && it.id != song.id && !ArtistMixEngine.isArtistMatch(it, searchArtist) }
                                        .take(6)

                                    // Interleave: 1 track from same artist, 3 diverse tracks from related/genre, 1 track from same artist, then diverse tracks
                                    val versatilePool = mutableListOf<Song>()
                                    if (artistTracks.isNotEmpty()) versatilePool.add(artistTracks.first())
                                    versatilePool.addAll(rel.take(3))
                                    versatilePool.addAll(genreTracks.take(3))
                                    if (artistTracks.size > 1) versatilePool.add(artistTracks[1])
                                    versatilePool.addAll(rel.drop(3))
                                    versatilePool.addAll(genreTracks.drop(3))
                                    versatilePool.distinctBy { it.id }
                                } else {
                                    repo.getRelatedSongs(song)
                                }
                                if (related.isNotEmpty()) {
                                    val updatedPl = _uiState.value.playlist.toMutableList()
                                    val existingIds = updatedPl.map { it.id }.toSet()
                                    val cohesive = related.filter {
                                        repo.detectLanguage(it).equals(songLang, ignoreCase = true)
                                    }
                                    val newTracks = (if (cohesive.isNotEmpty()) cohesive else related)
                                        .filter { it.id !in existingIds && it.id !in sessionPlayedSongIds }
                                    if (newTracks.isNotEmpty()) {
                                        val activeCurrIdx = updatedPl.indexOfFirst { it.id == song.id }
                                        val insertIdx = if (activeCurrIdx >= 0) activeCurrIdx + 1 else updatedPl.size
                                        updatedPl.addAll(insertIdx, newTracks)
                                        _uiState.update { it.copy(playlist = updatedPl.distinctBy { t -> t.id }) }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (activeTargetSongId == song.id) {
                    repo.clearStreamCache(song.id)
                    _uiState.update { it.copy(isLoadingStream = false, downloadNotice = "Skipping unplayable \"${song.title}\"") }
                    val pl = _uiState.value.playlist
                    if (pl.size > 1) {
                        launch {
                            delay(400)
                            playNext(isAutoMix = isAutoMix)
                        }
                    } else {
                        _uiState.update { it.copy(isPlaying = false) }
                    }
                }
            }
        }
    }

    private var isLoadingMoreUpcoming = false

    fun loadMoreUpcoming() {
        if (isLoadingMoreUpcoming) return
        val current = _uiState.value
        val song = current.currentSong ?: return
        if (!current.autoPlayEnabled) return

        viewModelScope.launch(Dispatchers.IO) {
            isLoadingMoreUpcoming = true
            try {
                if (current.isArtistMixActive && current.artistMixTarget != null) {
                    extendArtistMixInternal(current.artistMixTarget)
                } else {
                    val songLang = repo.detectLanguage(song)
                    val songGenre = repo.detectGenre(song)
                    val searchArtist = song.artist.replace(" - Topic", "").trim()
                    val rel = repo.getRelatedSongs(song).filter { !it.isAlbum && it.id != song.id }
                    val artistTracks = repo.search("$searchArtist songs")
                        .filter { !it.isAlbum && it.id != song.id && repo.detectLanguage(it).equals(songLang, ignoreCase = true) }
                        .take(2)
                    val genreQuery = when (songLang) {
                        "Hindi" -> "top bollywood $songGenre hits"
                        "Punjabi" -> "top punjabi $songGenre hits"
                        "English" -> "top $songGenre hits"
                        else -> "top $songLang $songGenre hits"
                    }
                    val genreTracks = repo.search(genreQuery)
                        .filter { !it.isAlbum && it.id != song.id && !ArtistMixEngine.isArtistMatch(it, searchArtist) }
                        .take(6)

                    val candidatePool = mutableListOf<Song>()
                    if (artistTracks.isNotEmpty()) candidatePool.add(artistTracks.first())
                    candidatePool.addAll(rel.take(4))
                    candidatePool.addAll(genreTracks.take(4))
                    if (artistTracks.size > 1) candidatePool.add(artistTracks[1])
                    candidatePool.addAll(rel.drop(4))
                    candidatePool.addAll(genreTracks.drop(4))
                    val uniqueCandidates = candidatePool.distinctBy { it.id }
                    
                    val existingIds = _uiState.value.playlist.map { it.id }.toSet()
                    val cohesive = uniqueCandidates.filter {
                        repo.detectLanguage(it).equals(songLang, ignoreCase = true)
                    }
                    val newTracks = (if (cohesive.isNotEmpty()) cohesive else uniqueCandidates)
                        .filter { it.id !in existingIds && it.id !in sessionPlayedSongIds }
                        .distinctBy { it.id }

                    if (newTracks.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            val updatedPl = (_uiState.value.playlist + newTracks.take(12)).distinctBy { it.id }
                            _uiState.update { it.copy(playlist = updatedPl) }
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                isLoadingMoreUpcoming = false
            }
        }
    }

    fun prepareDataDuringOnboarding() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                refreshPersonalizedMusic(forceFetchNew = true)
                loadRecommendationsInternal()
                repo.getLiveGlobalTopCharts()
                // Pre-warm Coil cache for albums, album sets & recommendations to completely eliminate scrolling lag
                val context = getApplication<android.app.Application>()
                val imageLoader = coil.Coil.imageLoader(context)
                val allAlbumCovers = (_uiState.value.albums.map { it.coverUrl } +
                        _uiState.value.recommendations.map { it.coverUrl } +
                        repo.getFeaturedAlbums().flatMap { listOf(it.coverUrl) + it.songs.take(2).map { s -> s.coverUrl } } +
                        _uiState.value.albumSets.flatMap { it.albums.map { a -> a.coverUrl } }
                ).filter { it.isNotBlank() && it.startsWith("http") }.distinct()
                for (url in allAlbumCovers.take(30)) {
                    val req = coil.request.ImageRequest.Builder(context)
                        .data(url)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        .build()
                    imageLoader.enqueue(req)
                }
            } catch (_: Exception) {}
        }
    }

    private var isExtendingArtistMix = false

    /** Asynchronously extends the ongoing Artist Mix in background without disturbing armed AutoMix track */
    suspend fun extendArtistMixInternal(artistName: String) = withContext(Dispatchers.IO) {
        if (isExtendingArtistMix) return@withContext
        isExtendingArtistMix = true
        try {
            val query1 = "$artistName songs"
            val query2 = "$artistName top hits"
            val songs1 = repo.search(query1).filter { !it.isAlbum }
            val songs2 = repo.search(query2).filter { !it.isAlbum }
            val allNetworkTracks = (songs1 + songs2).distinctBy { it.id }
            
            val newArtist = allNetworkTracks.filter { ArtistMixEngine.isArtistMatch(it, artistName) }
            
            withContext(Dispatchers.Main) {
                val current = _uiState.value
                if (current.isArtistMixActive && current.artistMixTarget.equals(artistName, ignoreCase = true)) {
                    val extendedPl = ArtistMixEngine.extendMix(
                        existingPlaylist = current.playlist,
                        newArtistTracks = newArtist,
                        newSimilarTracks = emptyList(),
                        targetArtist = artistName,
                        batchSize = 16
                    ).distinctBy { it.id }
                    if (extendedPl.size > current.playlist.size) {
                        _uiState.update { it.copy(playlist = extendedPl) }
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            isExtendingArtistMix = false
        }
    }

    /** Extends the Artist Mix if the upcoming queue runs low (<= 4 tracks remaining) */
    fun extendArtistMixIfNeeded() {
        val state = _uiState.value
        val artist = state.artistMixTarget ?: return
        val currentSong = state.currentSong ?: return
        val idx = state.playlist.indexOfFirst { it.id == currentSong.id }
        val upcomingCount = if (idx >= 0) state.playlist.size - idx - 1 else 0
        if (upcomingCount <= 4) {
            viewModelScope.launch {
                extendArtistMixInternal(artist)
            }
        }
    }

    fun retryLastFailedArtistMix() {
        val failed = _uiState.value.lastFailedArtistMix ?: return
        playArtistMix(failed)
    }

    /** Plays a curated artist mix obeying R7 requirements (<300ms start, MMR diversity, 70/30 ratio, album cap <= 2, continuous extension) */
    fun playArtistMix(artistName: String, initialSongs: List<Song> = emptyList(), seedSong: Song? = null) {
        val trimmedArtist = artistName.trim()
        if (trimmedArtist.isBlank()) return
        storage.updateArtistAffinity(getApplication(), trimmedArtist, 6.0f)
        
        _uiState.update { it.copy(lastFailedArtistMix = null) }
        val state = _uiState.value
        
        // 1. Gather all local tracks synchronously (<300ms start target)
        val localCatalog = (state.favoriteSongs + state.recentlyPlayed + state.downloadedSongs +
                state.albums.flatMap { it.songs } + state.recommendations + initialSongs)
            .distinctBy { it.id }
        
        val localArtistTracks = localCatalog.filter { ArtistMixEngine.isArtistMatch(it, trimmedArtist) }
        
        val initialMix = ArtistMixEngine.buildArtistMix(
            targetArtist = trimmedArtist,
            artistTracks = localArtistTracks,
            similarTracks = emptyList(),
            seedSong = seedSong ?: localArtistTracks.firstOrNull() ?: initialSongs.firstOrNull(),
            maxTracks = 15,
            artistRatio = 1.00f
        )
        
        if (initialMix.isNotEmpty()) {
            _uiState.update { it.copy(
                queueTitle = "Artist Mix: $trimmedArtist",
                isArtistMixActive = true,
                artistMixTarget = trimmedArtist,
                isArtistMixLoading = false
            )}
            playSong(initialMix.first(), playlist = initialMix)
            
            // Background extension to enrich the mix up to 30+ tracks
            viewModelScope.launch {
                extendArtistMixInternal(trimmedArtist)
            }
        } else {
            // No local tracks for artist: show loading state and fetch from network
            _uiState.update { it.copy(
                queueTitle = "Artist Mix: $trimmedArtist",
                isArtistMixActive = true,
                artistMixTarget = trimmedArtist,
                isArtistMixLoading = true
            )}
            viewModelScope.launch {
                try {
                    val searchResults = withContext(Dispatchers.IO) {
                        repo.search("$trimmedArtist songs")
                    }
                    val artistTracks = searchResults.filter { ArtistMixEngine.isArtistMatch(it, trimmedArtist) }
                    
                    val builtMix = ArtistMixEngine.buildArtistMix(
                        targetArtist = trimmedArtist,
                        artistTracks = artistTracks,
                        similarTracks = emptyList(),
                        seedSong = artistTracks.firstOrNull(),
                        maxTracks = 25,
                        artistRatio = 1.00f
                    )
                    
                    if (builtMix.isNotEmpty()) {
                        _uiState.update { it.copy(isArtistMixLoading = false) }
                        playSong(builtMix.first(), playlist = builtMix)
                    } else {
                        _uiState.update { it.copy(
                            isArtistMixLoading = false,
                            isArtistMixActive = false,
                            queueTitle = null,
                            lastFailedArtistMix = trimmedArtist,
                            downloadNotice = "No tracks found for \"$trimmedArtist\". Tap to retry."
                        )}
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(
                        isArtistMixLoading = false,
                        isArtistMixActive = false,
                        queueTitle = null,
                        lastFailedArtistMix = trimmedArtist,
                        downloadNotice = "Offline: Unable to load \"$trimmedArtist\" mix. Tap to retry."
                    )}
                }
            }
        }
    }

    /** Offline file first (downloaded songs), then a direct stream URL, then stream extraction. */
    private suspend fun resolvePlayableUrl(song: Song): String? = withContext(Dispatchers.IO) {
        val localFile = if (song.streamUrl?.startsWith("file://") == true) {
            java.io.File(android.net.Uri.parse(song.streamUrl).path ?: "")
        } else if (song.streamUrl?.startsWith("/") == true) {
            java.io.File(song.streamUrl)
        } else null

        val safeArtist = song.artist.replace(Regex("[^a-zA-Z0-9.-]"), "_").trim('_')
        val safeTitle = song.title.replace(Regex("[^a-zA-Z0-9.-]"), "_").trim('_')
        val fileName = "${safeArtist}_-_${safeTitle}.m4a"
        val publicFile = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC), "Beatles/$fileName")
        val appMusicDir = java.io.File(getApplication<Application>().getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC), "Beatles")
        val fallbackFile = java.io.File(appMusicDir, fileName)

        val resolvedLocalFile = when {
            localFile != null && localFile.exists() && localFile.length() > 1024L -> localFile
            publicFile.exists() && publicFile.length() > 1024L -> publicFile
            fallbackFile.exists() && fallbackFile.length() > 1024L -> fallbackFile
            else -> null
        }

        when {
            resolvedLocalFile != null -> android.net.Uri.fromFile(resolvedLocalFile).toString()
            song.streamUrl != null && song.streamUrl.startsWith("http") && !song.streamUrl.contains("itunes.apple.com") -> song.streamUrl
            else -> try { repo.getStreamUrl(song.id, song) } catch (_: Exception) { null }
        }
    }

    /** The song that will play after the current one (strictly language and genre consistent). */
    private fun peekNextSong(state: PlayerUiState = _uiState.value): Song? {
        val current = state.currentSong ?: return null
        val pl = state.playlist
        val idx = pl.indexOfFirst { it.id == current.id }
        if (idx in 0 until pl.size - 1) return pl[idx + 1]
        if (state.repeatMode == 1 && pl.size > 1) return pl.first()
        if (state.autoPlayEnabled) {
            val queued = pl.map { it.id }.toSet()
            if (state.isArtistMixActive && state.artistMixTarget != null) {
                val target = state.artistMixTarget!!
                val catalog = (state.favoriteSongs + state.recentlyPlayed + state.downloadedSongs +
                        state.albums.flatMap { it.songs } + state.recommendations).distinctBy { it.id }
                val match = catalog.firstOrNull {
                    !it.isAlbum && it.id != current.id && it.id !in queued && it.id !in sessionPlayedSongIds &&
                    ArtistMixEngine.isArtistMatch(it, target)
                } ?: catalog.firstOrNull {
                    !it.isAlbum && it.id != current.id && it.id !in queued &&
                    ArtistMixEngine.isArtistMatch(it, target)
                }
                if (match != null) return match
                extendArtistMixIfNeeded()
                return null
            }
            val lane = ListeningLaneEngine.buildListeningLane(current, state.recentlyPlayed)
            val candidatePool = (state.recommendations + state.favoriteSongs + state.recentlyPlayed).distinctBy { it.id }
                .filter { !it.isAlbum && it.id != current.id && it.id !in queued }
            val (picks, _) = ListeningLaneEngine.selectAutoplayQueue(lane, candidatePool, requestedCount = 1)
            if (picks.isNotEmpty()) {
                return picks.first()
            }
        }
        return null
    }

    private fun onAutoMixHandoff(incomingSong: Song, handoffPositionMs: Long) {
        lastAutoMixHandoffTime = System.currentTimeMillis()
        lastHandoffSongId = incomingSong.id
        cachedAutoMixSongId = null
        cachedAutoMixCandidate = null
        sessionPlayedSongIds.add(incomingSong.id)
        val ctx = getApplication<Application>()
        val incomingDuration = if (incomingSong.durationSeconds > 0) incomingSong.durationSeconds * 1000L else _uiState.value.duration
        var finalPlaylist: List<Song> = emptyList()
        _uiState.update { s ->
            val pl = if (s.playlist.any { it.id == incomingSong.id }) s.playlist else {
                val idx = s.playlist.indexOfFirst { it.id == s.currentSong?.id }
                s.playlist.toMutableList().apply { add(if (idx >= 0) idx + 1 else size, incomingSong) }
            }
            finalPlaylist = pl
            s.copy(
                currentSong = incomingSong,
                duration = incomingDuration,
                currentPosition = handoffPositionMs,
                playlist = pl,
                isPlaying = true,
                isFavorite = storage.isFavorite(ctx, incomingSong.id),
                lyrics = emptyList()
            )
        }
        com.beatles.player.player.PlaybackService.activeSong = incomingSong
        com.beatles.player.player.PlaybackService.activePlaylist = finalPlaylist
        storage.saveLastPlayedSong(ctx, incomingSong)
        storage.saveLastPlayedPlaylist(ctx, finalPlaylist)
        storage.addToRecentlyPlayed(ctx, incomingSong)
        _uiState.update { it.copy(recentlyPlayed = storage.getRecentlyPlayed(ctx)) }
        fetchLyrics(incomingSong)
        enhanceCoverArt(incomingSong)
        topUpQueueIfNeeded(incomingSong)
        triggerCandidatePrecomputation(incomingSong)
    }

    /** Keeps at least a few songs queued so AutoMix always has a partner track matching the same language and genre. */
    private fun topUpQueueIfNeeded(seed: Song) {
        val s = _uiState.value
        if (s.isArtistMixActive && s.artistMixTarget != null) {
            extendArtistMixIfNeeded()
            return
        }
        if (!s.autoPlayEnabled) return
        val idx = s.playlist.indexOfFirst { it.id == seed.id }
        val upcoming = if (idx >= 0) s.playlist.size - idx - 1 else 0
        if (upcoming >= 4) return
        viewModelScope.launch {
            val related = try { repo.getRelatedSongs(seed) } catch (_: Exception) { emptyList() }
            val candidates = if (related.isNotEmpty()) related else {
                try { repo.search("${seed.artist} songs").filter { !it.isAlbum } } catch (_: Exception) { emptyList() }
            }
            val lane = ListeningLaneEngine.buildListeningLane(seed, _uiState.value.recentlyPlayed)
            val existingIds = _uiState.value.playlist.map { it.id }.toSet()
            val filteredPool = candidates.filter { it.id !in existingIds && !it.isAlbum && it.id != seed.id }
            val (picks, _) = ListeningLaneEngine.selectAutoplayQueue(lane, filteredPool, requestedCount = 6)
            if (picks.isNotEmpty()) {
                _uiState.update { st ->
                    val ids = st.playlist.map { it.id }.toSet()
                    val toAdd = picks.filter { it.id !in ids }
                    st.copy(playlist = st.playlist + toAdd)
                }
                triggerCandidatePrecomputation(seed)
            }
        }
    }

    private fun preloadUpcomingSong(song: Song) {
        // Removed CacheWriter logic since it clashes with ExoPlayer buffering
        // and causes EOF (403 Range request) errors after 3MB of playback.
    }

    private fun enhanceCoverArt(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            val hqCover = repo.getHighResCover(song.title, song.artist, song.coverUrl)
            val motionVideo = song.videoUrl ?: repo.resolveMotionArtwork(song.title, song.artist, song.album)
            val hasNewCover = hqCover.isNotBlank() && hqCover != song.coverUrl
            val hasNewVideo = motionVideo != null && motionVideo != song.videoUrl
            if (hasNewCover || hasNewVideo) {
                val updatedSong = song.copy(
                    coverUrl = if (hasNewCover) hqCover else song.coverUrl,
                    videoUrl = if (hasNewVideo) motionVideo else song.videoUrl
                )
                storage.addToRecentlyPlayed(getApplication(), updatedSong)
                withContext(Dispatchers.Main) {
                    _uiState.update { 
                        it.copy(
                            recentlyPlayed = storage.getRecentlyPlayed(getApplication()),
                            currentSong = if (it.currentSong?.id == song.id) updatedSong else it.currentSong,
                            playlist = it.playlist.map { s -> if (s.id == song.id) updatedSong else s }
                        )
                    }
                }
            }
        }
    }

    fun syncPlaybackWithService() {
        if (autoMixEngine.isBusy || autoMixEngine.isHandingOff) return
        val currentDeckPlayer = autoMixEngine.currentDeck ?: PlaybackService.instance?.defaultPlayer
        val activeDeckSong = currentDeckPlayer?.currentMediaItem?.mediaId?.let { id ->
            _uiState.value.playlist.find { it.id == id }
        }
        val serviceSong = activeDeckSong ?: PlaybackService.activeSong ?: try { storage.getLastPlayedSong(getApplication()) } catch (_: Exception) { null }
        val servicePl = if (PlaybackService.activePlaylist.isNotEmpty()) {
            PlaybackService.activePlaylist
        } else {
            try { storage.getLastPlayedPlaylist(getApplication()) } catch (_: Exception) { emptyList() }
        }
        val isPlayingNow = PlaybackService.instance?.defaultPlayer?.isPlaying == true || (autoMixEngine.currentDeck?.isPlaying == true)

        if (serviceSong != null) {
            val now = System.currentTimeMillis()
            val isRecentAutoMix = (lastHandoffSongId != null && (now - lastAutoMixHandoffTime < 180_000L || _uiState.value.currentSong?.id == lastHandoffSongId))
            val needsSongUpdate = _uiState.value.currentSong == null || (_uiState.value.currentSong?.id != serviceSong.id && isPlayingNow && !isRecentAutoMix)
            if (needsSongUpdate || _uiState.value.isPlaying != isPlayingNow) {
                _uiState.update { it.copy(
                    currentSong = if (needsSongUpdate) serviceSong else it.currentSong,
                    playlist = if (it.playlist.isNotEmpty()) it.playlist else if (servicePl.isNotEmpty()) servicePl else listOf(serviceSong),
                    isPlaying = isPlayingNow,
                    duration = if (it.duration > 0L) it.duration else serviceSong.durationSeconds * 1000L
                )}
            }
        }
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) {
            if (autoMixEngine.isBusy) autoMixEngine.abortTransition()
            player.pause()
        } else player.resume()
    }
    fun setExpanded(exp: Boolean) { _uiState.update { it.copy(isExpanded = exp) } }
    fun seekTo(pos: Long) {
        autoMixEngine.abortTransition()
        _uiState.update { it.copy(currentPosition = pos) }
        player.seekTo(pos)
    }
    fun playNext(isAutoMix: Boolean = false, keepExpandedState: Boolean = true) {
        val state = _uiState.value
        val currentSong = state.currentSong
        val currentPl = state.playlist
        if (currentPl.isEmpty()) return

        val currIdx = if (currentSong != null) currentPl.indexOfFirst { it.id == currentSong.id } else -1
        // Upcoming songs are already shuffled in-place by toggleShuffle(), so always advance sequentially.
        val nextIdx = currIdx + 1

        if (nextIdx in currentPl.indices) {
            val nextSong = currentPl[nextIdx]
            playSong(nextSong, currentPl, isFromSearch = false, isAutoMix = isAutoMix, keepExpandedState = keepExpandedState)
        } else if (state.autoPlayEnabled && currentSong != null) {
            if (state.isArtistMixActive && state.artistMixTarget != null) {
                viewModelScope.launch {
                    extendArtistMixInternal(state.artistMixTarget)
                    val updatedPl = _uiState.value.playlist
                    val newCurrIdx = updatedPl.indexOfFirst { it.id == currentSong.id }
                    if (newCurrIdx in 0 until updatedPl.size - 1) {
                        val nextTrack = updatedPl[newCurrIdx + 1]
                        playSong(nextTrack, updatedPl, isFromSearch = false, isAutoMix = isAutoMix, keepExpandedState = keepExpandedState)
                    }
                }
            } else {
                viewModelScope.launch {
                    val related = repo.getRelatedSongs(currentSong)
                    val newTracks = related.filter { s -> currentPl.none { it.id == s.id } }
                    if (newTracks.isNotEmpty()) {
                        val updatedPl = currentPl + newTracks
                        _uiState.update { it.copy(playlist = updatedPl) }
                        playSong(newTracks.first(), updatedPl, isFromSearch = false, isAutoMix = isAutoMix, keepExpandedState = keepExpandedState)
                    } else if (currentPl.isNotEmpty()) {
                        // Loop back to the beginning of the queue so music never abruptly dies
                        playSong(currentPl.first(), currentPl, isFromSearch = false, isAutoMix = isAutoMix, keepExpandedState = keepExpandedState)
                    } else {
                        // End of queue and no more related tracks found: stop playback cleanly
                        player.pause()
                        player.stop()
                        autoMixEngine.abortTransition()
                        autoMixEngine.resetToDefaultDeck()
                        _uiState.update { it.copy(isPlaying = false, currentPosition = 0L) }
                    }
                }
            }
        } else if (state.repeatMode == 1 && currentPl.isNotEmpty()) {
            playSong(currentPl.first(), currentPl, isFromSearch = false, isAutoMix = isAutoMix, keepExpandedState = keepExpandedState)
        } else {
            // End of queue/album and autoplay is disabled: stop playback cleanly
            player.pause()
            player.stop()
            autoMixEngine.abortTransition()
            autoMixEngine.resetToDefaultDeck()
            _uiState.update { it.copy(isPlaying = false, currentPosition = 0L) }
        }
    }

    fun playPrevious(keepExpandedState: Boolean = true) {
        val state = _uiState.value
        val currentSong = state.currentSong
        val currentPl = state.playlist
        if (currentPl.isEmpty()) return

        val currIdx = if (currentSong != null) currentPl.indexOfFirst { it.id == currentSong.id } else -1
        val prevIdx = currIdx - 1
        if (prevIdx in currentPl.indices) {
            val prevSong = currentPl[prevIdx]
            playSong(prevSong, currentPl, isFromSearch = false, keepExpandedState = keepExpandedState)
        } else if (currentPl.isNotEmpty()) {
            playSong(currentPl.last(), currentPl, isFromSearch = false, keepExpandedState = keepExpandedState)
        }
    }

    fun toggleShuffle() {
        val newState = !_uiState.value.shuffleModeEnabled
        _uiState.update { it.copy(shuffleModeEnabled = newState) }
        player.toggleShuffle()
        val currentSong = _uiState.value.currentSong
        val pl = _uiState.value.playlist.toMutableList()
        if (newState && currentSong != null) {
            val currIdx = pl.indexOfFirst { it.id == currentSong.id }
            if (currIdx >= 0) {
                val current = pl.removeAt(currIdx)
                pl.shuffle()
                pl.add(0, current)
                _uiState.update { it.copy(playlist = pl) }
            }
        }
    }

    fun toggleRepeat() {
        val nextMode = (_uiState.value.repeatMode + 1) % 3
        _uiState.update { it.copy(repeatMode = nextMode) }
        player.toggleRepeat()
    }

    fun toggleFavorite() {
        val song = _uiState.value.currentSong ?: return
        val ctx = getApplication<android.app.Application>()
        if (storage.isFavorite(ctx, song.id)) {
            storage.removeFavorite(ctx, song.id)
            storage.updateArtistAffinity(ctx, song.artist, -4.0f)
        } else {
            storage.saveFavorite(ctx, song)
            storage.updateArtistAffinity(ctx, song.artist, 10.0f)
        }
        _uiState.update {
            it.copy(
                isFavorite = storage.isFavorite(ctx, song.id),
                favoriteSongs = storage.getFavorites(ctx)
            )
        }
    }

    fun toggleLyrics() {
        _uiState.update { it.copy(showLyrics = !it.showLyrics) }
    }

    fun switchToNextUp() {
        _uiState.update {
            val isAlreadyNextUp = it.showSeeAllSection == "next_up"
            it.copy(showLyrics = false, showSeeAllSection = if (isAlreadyNextUp) null else "next_up")
        }
    }

    fun switchToLyrics() {
        _uiState.update {
            val isAlreadyLyrics = it.showLyrics && it.showSeeAllSection != "next_up"
            it.copy(showSeeAllSection = null, showLyrics = !isAlreadyLyrics)
        }
    }

    private fun fetchLyrics(song: Song) {
        activeLyricsJob?.cancel()
        activeLyricsJob = viewModelScope.launch {
            val lyrics = repo.getLyrics(song.title, song.artist, song.durationSeconds)
            if (_uiState.value.currentSong?.id == song.id) {
                _uiState.update { it.copy(lyrics = lyrics) }
            }
        }
    }

    // --- Sleep Timer Engine ---
    fun startSleepTimerMinutes(minutes: Int) {
        val totalMs = minutes * 60_000L
        val targetMs = System.currentTimeMillis() + totalMs
        _uiState.update {
            it.copy(
                sleepTimerState = it.sleepTimerState.copy(
                    mode = SleepTimerMode.TIME,
                    targetTimeMillis = targetMs,
                    totalDurationMillis = totalMs,
                    remainingSeconds = (minutes * 60).toLong(),
                    showWarningPopup = false
                ),
                downloadNotice = "Sleep timer set for $minutes min"
            )
        }
        startSleepTimerTicker()
    }

    fun startSleepTimerSongs(songCount: Int) {
        val mode = if (songCount <= 1) SleepTimerMode.END_OF_TRACK else SleepTimerMode.SONGS
        _uiState.update {
            it.copy(
                sleepTimerState = it.sleepTimerState.copy(
                    mode = mode,
                    remainingSongs = songCount,
                    showWarningPopup = false
                ),
                downloadNotice = if (songCount <= 1) "Sleep timer set for end of track" else "Sleep timer set for $songCount songs"
            )
        }
        startSleepTimerTicker()
    }

    fun startSleepTimerAlbum() {
        val currentAlb = _uiState.value.currentSong?.album ?: "Album"
        _uiState.update {
            it.copy(
                sleepTimerState = it.sleepTimerState.copy(
                    mode = SleepTimerMode.ALBUM,
                    targetAlbumId = currentAlb,
                    showWarningPopup = false
                ),
                downloadNotice = "Sleep timer set for end of album"
            )
        }
        startSleepTimerTicker()
    }

    fun startSleepTimerEndOfTrack() {
        startSleepTimerSongs(1)
    }

    fun extendSleepTimerMinutes(addedMinutes: Int) {
        val currState = _uiState.value.sleepTimerState
        val addedMs = addedMinutes * 60_000L
        val baseTarget = if (currState.mode == SleepTimerMode.TIME && currState.targetTimeMillis > System.currentTimeMillis()) {
            currState.targetTimeMillis
        } else {
            System.currentTimeMillis()
        }
        val newTarget = baseTarget + addedMs
        val remSec = ((newTarget - System.currentTimeMillis()).coerceAtLeast(0L) + 999L) / 1000L
        _uiState.update {
            it.copy(
                sleepTimerState = it.sleepTimerState.copy(
                    mode = SleepTimerMode.TIME,
                    targetTimeMillis = newTarget,
                    totalDurationMillis = currState.totalDurationMillis + addedMs,
                    remainingSeconds = remSec,
                    showWarningPopup = false
                ),
                downloadNotice = "Extended sleep timer by $addedMinutes min"
            )
        }
        startSleepTimerTicker()
    }

    fun extendSleepTimerSongs(addedSongs: Int) {
        val currState = _uiState.value.sleepTimerState
        val baseRemaining = if (currState.mode == SleepTimerMode.SONGS || currState.mode == SleepTimerMode.END_OF_TRACK) {
            currState.remainingSongs
        } else 0
        val newCount = baseRemaining + addedSongs
        _uiState.update {
            it.copy(
                sleepTimerState = it.sleepTimerState.copy(
                    mode = SleepTimerMode.SONGS,
                    remainingSongs = newCount,
                    showWarningPopup = false
                ),
                downloadNotice = "Extended sleep timer by $addedSongs song${if (addedSongs > 1) "s" else ""}"
            )
        }
        startSleepTimerTicker()
    }

    fun extendSleepTimerAlbum() {
        startSleepTimerAlbum()
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        warningPopupJob?.cancel()
        warningPopupJob = null
        val popupEnabled = storage.isSleepExtendPopupEnabled(getApplication())
        _uiState.update {
            it.copy(
                sleepTimerState = SleepTimerState(isExtendPopupEnabled = popupEnabled),
                downloadNotice = "Sleep timer turned off"
            )
        }
    }

    fun dismissSleepWarningPopup() {
        warningPopupJob?.cancel()
        warningPopupJob = null
        _uiState.update {
            it.copy(sleepTimerState = it.sleepTimerState.copy(showWarningPopup = false))
        }
    }

    fun setSleepExtendPopupEnabled(enabled: Boolean) {
        storage.setSleepExtendPopupEnabled(getApplication(), enabled)
        _uiState.update {
            it.copy(sleepTimerState = it.sleepTimerState.copy(isExtendPopupEnabled = enabled))
        }
    }

    private fun startSleepTimerTicker() {
        sleepTimerJob?.cancel()
        sleepTimerJob = viewModelScope.launch {
            var warningFired = false
            while (isActive) {
                val state = _uiState.value.sleepTimerState
                if (state.mode == SleepTimerMode.NONE) break

                if (state.mode == SleepTimerMode.TIME) {
                    val now = System.currentTimeMillis()
                    val remMs = (state.targetTimeMillis - now).coerceAtLeast(0L)
                    val remSec = (remMs + 999L) / 1000L
                    _uiState.update { it.copy(sleepTimerState = it.sleepTimerState.copy(remainingSeconds = remSec)) }

                    if (state.isExtendPopupEnabled && remSec in 1L..60L && !warningFired) {
                        warningFired = true
                        triggerSleepWarningPopup()
                    }

                    if (remSec <= 0L) {
                        executeSleepTimerShutdown()
                        break
                    }
                } else if (state.mode == SleepTimerMode.END_OF_TRACK || state.mode == SleepTimerMode.SONGS) {
                    val dur = _uiState.value.duration
                    val pos = _uiState.value.currentPosition
                    if (state.isExtendPopupEnabled && state.remainingSongs <= 1 && dur > 0L && (dur - pos) in 1L..35_000L && !warningFired) {
                        warningFired = true
                        triggerSleepWarningPopup()
                    }
                } else if (state.mode == SleepTimerMode.ALBUM) {
                    val dur = _uiState.value.duration
                    val pos = _uiState.value.currentPosition
                    val pl = _uiState.value.playlist
                    val curr = _uiState.value.currentSong
                    val currIdx = pl.indexOfFirst { it.id == curr?.id }
                    val isLastInAlbum = currIdx < 0 || currIdx >= pl.size - 1 || pl.getOrNull(currIdx + 1)?.album != state.targetAlbumId
                    if (state.isExtendPopupEnabled && isLastInAlbum && dur > 0L && (dur - pos) in 1L..35_000L && !warningFired) {
                        warningFired = true
                        triggerSleepWarningPopup()
                    }
                }
                delay(1000L)
            }
        }
    }

    private fun triggerSleepWarningPopup() {
        warningPopupJob?.cancel()
        warningPopupJob = viewModelScope.launch {
            _uiState.update { it.copy(sleepTimerState = it.sleepTimerState.copy(showWarningPopup = true, warningPopupRemainingSeconds = 20)) }
            for (sec in 19 downTo 0) {
                delay(1000L)
                if (!_uiState.value.sleepTimerState.showWarningPopup) break
                _uiState.update { it.copy(sleepTimerState = it.sleepTimerState.copy(warningPopupRemainingSeconds = sec)) }
            }
            _uiState.update { it.copy(sleepTimerState = it.sleepTimerState.copy(showWarningPopup = false)) }
        }
    }

    private fun executeSleepTimerShutdown() {
        viewModelScope.launch {
            val defaultDeck = player.player
            val secondaryDeck = autoMixEngine.currentDeck
            for (step in 10 downTo 0) {
                val vol = step / 10f
                defaultDeck?.volume = vol
                secondaryDeck?.volume = vol
                delay(250L)
            }
            player.pause()
            player.stop()
            autoMixEngine.abortTransition(stopPlayback = true)
            defaultDeck?.volume = 1f
            cancelSleepTimer()
        }
    }

    fun toggleAutoMix() {
        val nextMode = when (_uiState.value.autoMixMode) {
            com.beatles.player.player.automix.AutoMixMode.ON -> com.beatles.player.player.automix.AutoMixMode.OFF
            com.beatles.player.player.automix.AutoMixMode.OFF -> com.beatles.player.player.automix.AutoMixMode.ON
        }
        when (nextMode) {
            com.beatles.player.player.automix.AutoMixMode.ON -> {
                autoMixEngine.harmonicMode = false
                autoMixEngine.extendedWindow = false
                _uiState.update { it.copy(autoMixMode = nextMode, autoMixEnabled = true, downloadNotice = "AutoMix on") }
                applyAutoMixRequirements(announce = false)
            }
            com.beatles.player.player.automix.AutoMixMode.OFF -> {
                autoMixEngine.abortTransition()
                _uiState.update { it.copy(autoMixMode = nextMode, autoMixEnabled = false, downloadNotice = "AutoMix off") }
            }
        }
    }

    /**
     * AutoMix needs: a next track to always exist (Autoplay on), no Repeat-One (it would loop the
     * current song instead of mixing), and a few songs queued ahead.
     */
    private fun applyAutoMixRequirements(announce: Boolean) {
        val adjusted = mutableListOf<String>()
        val s = _uiState.value
        if (!s.autoPlayEnabled) adjusted += "Autoplay on"
        if (s.repeatMode == 2) {
            player.setRepeatModeDirect(androidx.media3.common.Player.REPEAT_MODE_OFF)
            adjusted += "Repeat-One off"
        }
        _uiState.update {
            it.copy(
                autoMixEnabled = true,
                autoPlayEnabled = true,
                repeatMode = if (it.repeatMode == 2) 0 else it.repeatMode,
                downloadNotice = if (announce) {
                    if (adjusted.isEmpty()) "AutoMix on – songs will flow into each other"
                    else "AutoMix on · ${adjusted.joinToString(" · ")}"
                } else it.downloadNotice
            )
        }
        _uiState.value.currentSong?.let { topUpQueueIfNeeded(it) }
    }

    fun toggleAutoPlay() {
        val nextState = !_uiState.value.autoPlayEnabled
        if (nextState) {
            val current = _uiState.value.currentSong
            val currentPl = _uiState.value.playlist.toMutableList()
            val currentLang = if (current != null) repo.detectLanguage(current) else _uiState.value.selectedLanguage
            val currentGenre = if (current != null) repo.detectGenre(current) else "Pop"
            val freshCandidates = _uiState.value.recommendations
                .filter { !it.isAlbum && it.id != current?.id && it.id !in sessionPlayedSongIds && repo.detectLanguage(it).equals(currentLang, ignoreCase = true) }
                .shuffled()
                .take(8)
            if (current != null) {
                val currIdx = currentPl.indexOfFirst { it.id == current.id }
                val pastAndCurr = if (currIdx >= 0) currentPl.take(currIdx + 1) else listOf(current)
                val newPl = pastAndCurr + freshCandidates
                _uiState.update { it.copy(
                    autoPlayEnabled = true,
                    playlist = newPl,
                    downloadNotice = "Autoplay on · Queue tuned to $currentLang ($currentGenre)"
                )}
                topUpQueueIfNeeded(current)
            } else {
                _uiState.update { it.copy(autoPlayEnabled = true, downloadNotice = "Autoplay enabled") }
            }
        } else {
            _uiState.update { it.copy(
                autoPlayEnabled = false,
                downloadNotice = "Autoplay disabled: playing current track only"
            )}
        }
    }

    // Apple Music / DJ Camelot Wheel Mapping (1A-12A Minor, 1B-12B Major)
    private val CAMELOT_MAJOR = mapOf(
        "B" to 1, "F#" to 2, "Db" to 3, "Ab" to 4, "Eb" to 5, "Bb" to 6,
        "F" to 7, "C" to 8, "G" to 9, "D" to 10, "A" to 11, "E" to 12,
        "C#" to 3, "D#" to 5, "G#" to 4, "A#" to 6
    )
    private val CAMELOT_MINOR = mapOf(
        "Ab" to 1, "Eb" to 2, "Bb" to 3, "F" to 4, "C" to 5, "G" to 6,
        "D" to 7, "A" to 8, "E" to 9, "B" to 10, "F#" to 11, "Db" to 12,
        "G#" to 1, "D#" to 2, "A#" to 3, "C#" to 12
    )

    private fun getHarmonicScore(key1: String, scale1: String, key2: String, scale2: String): Float {
        val num1 = (if (scale1 == "MINOR") CAMELOT_MINOR[key1] else CAMELOT_MAJOR[key1]) ?: 8
        val num2 = (if (scale2 == "MINOR") CAMELOT_MINOR[key2] else CAMELOT_MAJOR[key2]) ?: 8
        val isSameLetter = scale1 == scale2

        if (num1 == num2 && isSameLetter) return 50f // Exact harmonic match
        if (num1 == num2 && !isSameLetter) return 40f // Relative major/minor modulation
        val diff = kotlin.math.abs(num1 - num2)
        val circularDiff = minOf(diff, 12 - diff)
        if (circularDiff == 1 && isSameLetter) return 35f // Adjacent Camelot step (+1 / -1 energy shift)
        if (circularDiff == 1 && !isSameLetter) return 20f
        return 0f
    }

    private var cachedAutoMixSongId: String? = null
    private var cachedAutoMixCandidate: Song? = null

    fun triggerCandidatePrecomputation(current: Song) {
        precomputeJob?.cancel()
        precomputeJob = viewModelScope.launch(Dispatchers.Default) {
            val candidate = computeBestAutoMixCandidate(current, _uiState.value)
            precomputedAutoMixCandidate = candidate
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(autoMixTargetSongId = candidate?.id) }
            }
        }
    }

    /** Apple Music AutoMix Next-Song Selection: Evaluates Camelot Key, Tempo Clustering, and Vibe Continuity. */
    suspend fun computeBestAutoMixCandidate(current: Song, state: PlayerUiState): Song? = withContext(Dispatchers.Default) {
        val pl = state.playlist
        val idx = pl.indexOfFirst { it.id == current.id }
        val upcomingInQueue = if (idx >= 0 && idx < pl.size - 1) pl.drop(idx + 1) else emptyList()

        // 1. If user explicitly added songs to Next Up, respect the user's manual queue first!
        val userQueued = upcomingInQueue.firstOrNull { it.id in userQueuedSongIds }
        if (userQueued != null) {
            cachedAutoMixSongId = current.id
            cachedAutoMixCandidate = userQueued
            return@withContext userQueued
        }

        // If playing an album or ordered playlist, preserve the original track sequence!
        // Never reorder or promote other tracks ahead of scheduled queue/album tracks.
        if (upcomingInQueue.isNotEmpty()) {
            val nextTrack = upcomingInQueue.first()
            cachedAutoMixSongId = current.id
            cachedAutoMixCandidate = nextTrack
            return@withContext nextTrack
        }

        // Return stably cached candidate for the current track if still valid
        if (cachedAutoMixSongId == current.id && cachedAutoMixCandidate != null) {
            val c = cachedAutoMixCandidate!!
            if (state.recommendations.any { it.id == c.id }) {
                return@withContext c
            }
        }

        // If queue ended and autoplay is disabled, do not select foreign candidates
        if (!state.autoPlayEnabled) {
            return@withContext null
        }

        val targetArtist = if (state.isArtistMixActive) state.artistMixTarget else null
        val availableRecs = state.recommendations.filter { 
            !it.isAlbum && it.id != current.id && it.id !in sessionPlayedSongIds &&
            (targetArtist == null || ArtistMixEngine.isArtistMatch(it, targetArtist))
        }
        val allCandidates = (upcomingInQueue + availableRecs).distinctBy { it.id }
            .filter { targetArtist == null || ArtistMixEngine.isArtistMatch(it, targetArtist) }
        if (allCandidates.isEmpty()) return@withContext null

        val currentLang = repo.detectLanguage(current)
        val currentGenre = repo.detectGenre(current)

        // Strict language filtering: enforce same language!
        val sameLangCandidates = allCandidates.filter { repo.detectLanguage(it).equals(currentLang, ignoreCase = true) }
        val candidates = if (sameLangCandidates.isNotEmpty()) sameLangCandidates else allCandidates

        val currentAnalysis = com.beatles.player.player.automix.TrackAnalyzer.analyzeTrack(current)
        val outBpm = currentAnalysis.globalBpm
        val outKey = currentAnalysis.keyTonic
        val outScale = currentAnalysis.keyScale

        fun isPartyTrack(s: Song): Boolean {
            val text = (s.title + " " + s.artist + " " + s.genre + " " + s.album).lowercase()
            return text.contains("party") || text.contains("dance") || text.contains("club") ||
                   text.contains("bhangra") || text.contains("remix") || text.contains("edm") ||
                   text.contains("dj") || text.contains("bass") || text.contains("punjabi") ||
                   text.contains("groove") || text.contains("beat") || text.contains("dhol")
        }

        val currentIsParty = isPartyTrack(current)

        val scored = candidates.map { cand ->
            val candAnalysis = com.beatles.player.player.automix.TrackAnalyzer.analyzeTrack(cand)
            val candBpm = candAnalysis.globalBpm
            val candKey = candAnalysis.keyTonic
            val candScale = candAnalysis.keyScale
            val candLang = repo.detectLanguage(cand)
            val candGenre = repo.detectGenre(cand)

            val ratio = minOf(
                kotlin.math.abs(outBpm / candBpm - 1f),
                kotlin.math.abs(outBpm / (candBpm * 2f) - 1f),
                kotlin.math.abs((outBpm * 2f) / candBpm - 1f)
            )
            
            // 1. Apple Music Tempo Clustering:
            var score = if (ratio <= 0.08f) {
                (1f - ratio) * 100f // Up to 100 points for tight tempo lock
            } else {
                -500f // Massive penalty for incompatible tempo
            }

            // 2. Apple Music Camelot Wheel Harmonic Scoring:
            score += getHarmonicScore(outKey, outScale, candKey, candScale)

            // 3. Language match bonus / penalty:
            if (candLang.equals(currentLang, ignoreCase = true)) score += 40f
            else score -= 100f

            // 4. Genre & Vibe match:
            if (candGenre.equals(currentGenre, ignoreCase = true)) score += 20f

            // 4. Bass Heaviness & Vibe match (15+ yr sound engineer standard):
            if (currentAnalysis.isBassHeavy) {
                if (candAnalysis.isBassHeavy) score += 50f else score -= 35f
                if (candBpm >= 105f) score += 15f
            } else {
                if (!candAnalysis.isBassHeavy) score += 30f else score -= 15f
            }

            if (currentIsParty) {
                if (isPartyTrack(cand)) score += 35f else score -= 20f
                if (candBpm >= 115f) score += 15f
            }

            if (!cand.artist.equals(current.artist, ignoreCase = true)) {
                score += 15f
            } else {
                score -= 10f
            }

            if (upcomingInQueue.any { it.id == cand.id }) {
                score += 15f
            }

            cand to score
        }

        val best = scored.maxByOrNull { it.second }?.first ?: candidates.first()
        cachedAutoMixSongId = current.id
        cachedAutoMixCandidate = best

        // Dynamically promote the best matching song to index + 1 only when queue ended and autoplay is on
        if (upcomingInQueue.isEmpty() && state.autoPlayEnabled && idx >= 0 && (idx + 1 >= pl.size || pl[idx + 1].id != best.id)) {
            val updatedPl = pl.toMutableList()
            updatedPl.removeAll { it.id == best.id }
            val insertAt = (idx + 1).coerceAtMost(updatedPl.size)
            updatedPl.add(insertAt, best)
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(playlist = updatedPl) }
            }
        }

        best
    }

    fun selectBestAutoMixCandidate(current: Song, state: PlayerUiState): Song? {
        return precomputedAutoMixCandidate ?: peekNextSong(state)
    }

    fun refreshHomeScreen() {
        _uiState.update { it.copy(isRefreshingHome = true) }
        viewModelScope.launch {
            try {
                homeRefreshCount++
                coroutineScope {
                    val deferredPersonalized = async(Dispatchers.IO) { refreshPersonalizedMusic(forceFetchNew = true) }
                    val deferredFreshAlbum = async(Dispatchers.IO) {
                        val artists = _uiState.value.selectedArtists
                        val freshQuery = if (artists.isNotEmpty()) {
                            artists.shuffled().first() + " album"
                        } else {
                            listOf("trending album 2024", "billboard top album", "punjabi top album 2024", "bollywood hit album", "pop album 2024").shuffled().first()
                        }
                        try { repo.searchOriginalAlbum(freshQuery) } catch (_: Exception) { null }
                    }
                    val deferredRecs = async(Dispatchers.IO) { loadRecommendationsInternal() }
                    val deferredTags = async(Dispatchers.IO) { repo.getDynamicTrendingSearches() }

                    val freshAlbum = deferredFreshAlbum.await()
                    deferredPersonalized.await()
                    deferredRecs.await()
                    val tags = deferredTags.await()

                    if (freshAlbum != null && freshAlbum.songs.isNotEmpty()) {
                        val affinityMap = storage.getArtistAffinityMap(getApplication())
                        val artists = _uiState.value.selectedArtists
                        val currentAlbums = _uiState.value.albums.filter { it.id != freshAlbum.id }
                        val updatedAlbums = listOf(freshAlbum) + currentAlbums
                        val updatedSets = repo.getPersonalizedAlbumSets(artists, updatedAlbums, affinityMap)
                        _uiState.update { it.copy(albums = updatedAlbums, albumSets = updatedSets, heroAlbum = freshAlbum) }
                    }

                    val ctx = getApplication<Application>()
                    val favs = storage.getFavorites(ctx)
                    val recents = storage.getRecentlyPlayed(ctx)
                    val downloaded = storage.getDownloadedSongs(ctx)
                    _uiState.update { it.copy(
                        trendingTags = tags,
                        favoriteSongs = favs,
                        recentlyPlayed = recents,
                        downloadedSongs = downloaded
                    ) }
                }
            } finally {
                _uiState.update { it.copy(isRefreshingHome = false) }
            }
        }
    }

    private fun loadRecommendations() {
        viewModelScope.launch {
            loadRecommendationsInternal()
        }
    }

    private fun loadRecommendationsFromArtists() {
        viewModelScope.launch {
            loadRecommendationsInternal()
        }
    }

    private suspend fun loadRecommendationsInternal() = withContext(Dispatchers.IO) {
        val recent = storage.getRecentlyPlayed(getApplication())
        val favorites = storage.getFavorites(getApplication())
        val selectedArtists = _uiState.value.selectedArtists
        val lang = _uiState.value.selectedLanguage
        val currentRecIds = _uiState.value.recommendations.map { it.id }.toSet()

        val affinityArtists = storage.getArtistAffinityMap(getApplication()).entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(6)
        val userArtists = (affinityArtists + selectedArtists + recent.map { it.artist } + favorites.map { it.artist })
            .filter { it.isNotBlank() }
            .distinct()

        val distinctArtists = mutableListOf<String>()
        if (userArtists.isNotEmpty()) {
            distinctArtists.addAll(userArtists.shuffled().take(6))
        } else {
            val curatedByLanguage = when (lang.lowercase()) {
                "punjabi" -> listOf("Karan Aujla", "Diljit Dosanjh", "Sidhu Moose Wala", "AP Dhillon", "Shubh", "Ammy Virk")
                "hindi", "bollywood" -> listOf("Arijit Singh", "Shreya Ghoshal", "Pritam", "Atif Aslam", "A.R. Rahman", "Sonu Nigam")
                "english", "pop" -> listOf("The Weeknd", "Taylor Swift", "Billie Eilish", "Coldplay", "Ed Sheeran", "Dua Lipa")
                "haryanvi" -> listOf("Renuka Panwar", "Gulzaar Chhaniwala", "Diler Kharkiya", "Masoom Sharma")
                "bhojpuri" -> listOf("Pawan Singh", "Khesari Lal Yadav", "Shilpi Raj", "Samar Singh")
                else -> listOf("The Weeknd", "Taylor Swift", "Arijit Singh", "Coldplay", "Billie Eilish", "Ed Sheeran")
            }
            distinctArtists.addAll(curatedByLanguage.shuffled().take(6))
        }

        val queryModifiers = listOf(
            "latest hits", "top songs", "best tracks", "popular audio", "essentials", "superhits", "audio songs"
        )

        val combinedSongs = mutableListOf<Song>()
        val artistCount = mutableMapOf<String, Int>()

        // 1. Seed search based on taste memory (top genre & top language) or broader variety
        try {
            val topGenre = storage.getGenreAffinityMap(getApplication()).maxByOrNull { it.value }?.key
            val topLang = storage.getLanguageAffinityMap(getApplication()).maxByOrNull { it.value }?.key
            val seedArtist = distinctArtists.randomOrNull()

            val seedQuery = if (seedArtist != null) {
                val modifier = queryModifiers.random()
                "$seedArtist $modifier"
            } else if (!topGenre.isNullOrBlank() && !topLang.isNullOrBlank()) {
                "top $topLang $topGenre hits 2024"
            } else if (lang.equals("All", ignoreCase = true) || lang.isBlank()) {
                "top global hits 2024"
            } else {
                "top $lang hits 2024"
            }
            val seedResults = repo.search(seedQuery).filter { !it.isAlbum && it.id !in sessionPlayedSongIds }
            for (s in seedResults.shuffled().take(4)) {
                if (combinedSongs.none { it.id == s.id }) combinedSongs.add(s)
            }
        } catch (_: Exception) {}

        // 2. Randomized artist queries with dynamic modifiers
        for (art in distinctArtists) {
            try {
                val mod = queryModifiers.random()
                val query = "$art $mod"
                val results = repo.search(query).filter { !it.isAlbum }
                // Prioritize fresh songs that aren't already shown
                val (fresh, seen) = results.partition { it.id !in currentRecIds && it.id !in sessionPlayedSongIds }
                val candidates = (fresh + seen)
                for (s in candidates.take(3)) {
                    val count = artistCount.getOrDefault(s.artist.lowercase(), 0)
                    if (count < 2 && combinedSongs.none { it.id == s.id }) {
                        combinedSongs.add(s)
                        artistCount[s.artist.lowercase()] = count + 1
                    }
                }
            } catch (_: Exception) {}
            if (combinedSongs.size >= 20) break
        }

        if (combinedSongs.isEmpty()) {
            combinedSongs.addAll(repo.getRealPredictiveSongs())
        }

        val finalRecs = combinedSongs.distinctBy { it.id }
        withContext(Dispatchers.Main) {
            _uiState.update { it.copy(recommendations = finalRecs) }
        }

        // Background enhance cover art with high-res iTunes artwork
        launch(Dispatchers.IO) {
            val enhanced = finalRecs.map { s ->
                val hq = repo.getHighResCover(s.title, s.artist, s.coverUrl)
                if (hq != s.coverUrl) s.copy(coverUrl = hq) else s
            }
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(recommendations = enhanced) }
            }
        }
    }

    override fun onCleared() { 
        sleepTimerJob?.cancel()
        warningPopupJob?.cancel()
        autoMixEngine.release()
        player.release()
        super.onCleared() 
    }
}
