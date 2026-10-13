package com.beatles.player.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beatles.player.data.*
import com.beatles.player.player.AudioPlayerManager
import com.beatles.player.player.automix.AutoMixEngine
import com.beatles.player.player.automix.AutoMixState
import com.beatles.player.player.automix.TransitionPhase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    val autoMixMode: com.beatles.player.player.automix.AutoMixMode = com.beatles.player.player.automix.AutoMixMode.NEW,
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
    val selectedSongBounds: Rect? = null
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
    private val sessionPlayedSongIds = mutableSetOf<String>()
    private val userQueuedSongIds = mutableSetOf<String>()
    @Volatile private var precomputedAutoMixCandidate: Song? = null
    private var precomputeJob: kotlinx.coroutines.Job? = null
    private var activePlayJob: kotlinx.coroutines.Job? = null
    private var activeLyricsJob: kotlinx.coroutines.Job? = null
    @Volatile private var activeTargetSongId: String? = null

    init {
        val isOnboarded = try { storage.isOnboarded(application) } catch (_: Exception) { false }
        val userName = try { storage.getUserName(application) } catch (_: Exception) { "" }
        val userRole = try { storage.getUserRole(application) } catch (_: Exception) { UserRole.LISTENER }
        val savedFavorites = try { storage.getFavorites(application) } catch (_: Exception) { emptyList() }
        val savedRecent = try { storage.getRecentlyPlayed(application) } catch (_: Exception) { emptyList() }
        val savedLanguage = try { storage.getSelectedLanguage(application).ifBlank { "All" } } catch (_: Exception) { "All" }
        val savedLanguages = try { storage.getSelectedLanguages(application) } catch (_: Exception) { emptySet() }
        val extendedAutoMix = try { storage.isExtendedAutoMix(application) } catch (_: Exception) { true }
        autoMixEngine.extendedWindow = extendedAutoMix
        val initialArtists = try { savedFavorites.mapNotNull { it.artist }.toSet() } catch (_: Exception) { emptySet() }
        val featured = try { repo.getPersonalizedFeaturedAlbums(selectedArtists = initialArtists, recentlyPlayed = savedRecent, favorites = savedFavorites, language = savedLanguage) } catch (_: Exception) { repo.getFeaturedAlbums() }
        val hero = featured.firstOrNull()
        val savedAlbums = try {
            storage.getAlbums(application).filter { alb ->
                featured.none { it.id == alb.id } && !alb.id.startsWith("alb_1") && !alb.id.startsWith("alb_2") && !alb.id.startsWith("alb_3")
            }
        } catch (_: Exception) { emptyList() }
        val allAlbums = featured + savedAlbums
        val savedSets = try { storage.getAlbumSets(application) } catch (_: Exception) { emptyList() }
        val defaultSets = try { repo.getPersonalizedAlbumSets(initialArtists, featured) } catch (_: Exception) { emptyList() }
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
            recommendations = initialRecs
        )}
        player.onPlaybackStateChanged = { isPlaying ->
            val actuallyPlaying = isPlaying || (autoMixEngine.currentDeck?.isPlaying == true)
            _uiState.update { it.copy(isPlaying = actuallyPlaying) }
        }
        var lastSongEndedTime = 0L
        player.onSongEnded = {
            val now = System.currentTimeMillis()
            if (!autoMixEngine.isBusy && (now - lastAutoMixHandoffTime >= 2_000L) && (now - lastSongEndedTime > 1000L)) {
                lastSongEndedTime = now
                playNext(isAutoMix = true)
            }
        }
        autoMixEngine.urlResolver = { song -> resolvePlayableUrl(song) }
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
                _uiState.update { it.copy(currentPosition = pos, duration = dur) }
            }
            val state = _uiState.value
            val currentSong = state.currentSong
            // Intelligent AutoMix Music Transition System: Only evaluate towards the outro (at least 60% in and at least 35s in)
            val minPlayTime = maxOf(35_000L, (dur * 0.60f).toLong())
            if (state.autoMixEnabled && state.repeatMode != 2 && dur > 40_000L && currentSong != null && !state.isLoadingStream && pos >= minPlayTime) {
                val now = System.currentTimeMillis()
                if (now - lastAutoMixHandoffTime >= 10_000L) {
                    val remaining = dur - pos
                    if (remaining in 4_000L..45_000L) {
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
        player.onSongTransition = { mediaId ->
            val now = System.currentTimeMillis()
            if (now - lastAutoMixHandoffTime >= 3_000L) {
                if (!autoMixEngine.isBusy) {
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
        }
        player.onShuffleModeEnabledChanged = { enabled -> _uiState.update { it.copy(shuffleModeEnabled = enabled) } }
        player.onRepeatModeChanged = { mode -> _uiState.update { it.copy(repeatMode = mode) } }
        if (isOnboarded) loadRecommendations()
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
        _uiState.update { it.copy(selectedArtists = artists) }
        refreshPersonalizedMusic()
    }
    fun finishOnboarding() {
        storage.setOnboarded(getApplication())
        _uiState.update { it.copy(onboardStep = 0) }
        refreshPersonalizedMusic()
    }

    private var homeRefreshCount = 0L

    fun refreshPersonalizedMusic() {
        val artists = _uiState.value.selectedArtists
        val recent = _uiState.value.recentlyPlayed
        val favorites = _uiState.value.favoriteSongs
        val lang = _uiState.value.selectedLanguage
        homeRefreshCount++
        val albums = repo.getPersonalizedFeaturedAlbums(artists, recent, favorites, lang, homeRefreshCount)
        val sets = repo.getPersonalizedAlbumSets(artists, albums)
        val hero = albums.firstOrNull()
        _uiState.update { it.copy(albums = albums, albumSets = sets, heroAlbum = hero) }
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

    fun setThemeMode(mode: ThemeMode) {
        storage.setThemeMode(getApplication(), mode)
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setArtistTab(tab: ArtistTab) { _uiState.update { it.copy(artistTab = tab) } }
    fun setAdminTab(tab: AdminTab) { _uiState.update { it.copy(adminTab = tab) } }

    fun setSelectedAlbum(album: Album?, bounds: Rect? = null) {
        _uiState.update { it.copy(selectedAlbum = album, selectedAlbumBounds = bounds) }
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
            _uiState.update { it.copy(selectedAlbum = album, isExpanded = false) }
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
        viewModelScope.launch {
            _uiState.update { 
                it.copy(downloadNotice = "⬇ Downloading \"${song.title}\" in highest quality...") 
            }
            try {
                val streamUrl = repo.getStreamUrl(song.id)
                val savedUri = DownloadHelper.downloadDirectly(getApplication(), song, streamUrl)
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
                        downloadedSongs = storage.getDownloadedSongs(getApplication()),
                        downloadNotice = "✓ Saved \"${song.title}\" for offline streaming"
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(downloadNotice = "Could not start download for \"${song.title}\"") 
                }
            }
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
            state.copy(playlist = currentPl, downloadNotice = "Playing \"${song.title}\" next")
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
        sourceBounds: Rect? = null
    ) {
        if (song.isAlbum) {
            viewModelScope.launch {
                val tracks = repo.getAlbumTracks(song.id)
                if (tracks.isNotEmpty()) playSong(tracks.first(), tracks, sourceBounds = sourceBounds)
            }
            return
        }
        if (_uiState.value.currentSong?.id == song.id && !_uiState.value.isLoadingStream && !isAutoMix) { togglePlayPause(); return }

        if (!isAutoMix) {
            autoMixEngine.abortTransition()
            autoMixEngine.resetToDefaultDeck()
            // Immediately stop old song and reset seekbar to 0 so there is no residual audio playback
            player.pause()
            player.stop()
            autoMixTriggeredSongId = null
            cachedAutoMixSongId = null
            cachedAutoMixCandidate = null
        }

        val pl = if (isFromSearch) mutableListOf(song) else (playlist?.toMutableList() ?: mutableListOf(song))
        
        // Immediate Next Up population when autoplay is enabled so Next Up is NEVER empty
        if (_uiState.value.autoPlayEnabled && pl.size <= 1) {
            val songLang = repo.detectLanguage(song)
            val quickUpcoming = _uiState.value.recommendations.filter { it.id != song.id && repo.detectLanguage(it).equals(songLang, ignoreCase = true) }
            if (quickUpcoming.isNotEmpty()) {
                pl.addAll(quickUpcoming.take(8))
            } else {
                pl.addAll(_uiState.value.recommendations.filter { it.id != song.id }.take(8))
            }
        }

        _uiState.update { it.copy(
            currentSong = song, isPlaying = true, isExpanded = if (isAutoMix) it.isExpanded else true, isLoadingStream = true,
            selectedSongBounds = sourceBounds,
            playlist = pl, currentPosition = 0L, duration = song.durationSeconds * 1000L,
            isFavorite = storage.isFavorite(getApplication(), song.id), lyrics = emptyList(), showLyrics = false
        )}
        storage.addToRecentlyPlayed(getApplication(), song)
        sessionPlayedSongIds.add(song.id)
        _uiState.update { it.copy(recentlyPlayed = storage.getRecentlyPlayed(getApplication())) }
        fetchLyrics(song)
        
        activePlayJob?.cancel()
        activeTargetSongId = song.id

        activePlayJob = viewModelScope.launch {
            try {
                val url = resolvePlayableUrl(song) ?: throw Exception("No playable source")

                if (activeTargetSongId != song.id) return@launch

                if (isAutoMix) {
                    player.playWithFadeIn(url, song, 1800L)
                } else {
                    player.play(url, song)
                }

                if (activeTargetSongId != song.id) return@launch

                _uiState.update { it.copy(isLoadingStream = false) }
                triggerCandidatePrecomputation(song)
                enhanceCoverArt(song)
                
                // Only fetch and append related songs if autoplay is enabled
                if (_uiState.value.autoPlayEnabled) {
                    launch {
                        val related = repo.getRelatedSongs(song)
                        if (related.isNotEmpty()) {
                            val currentPl = _uiState.value.playlist.toMutableList()
                            val existingIds = currentPl.map { it.id }.toSet()
                            val newTracks = related.filter { it.id !in existingIds && it.id !in sessionPlayedSongIds }
                            if (newTracks.isNotEmpty()) {
                                currentPl.addAll(newTracks)
                                _uiState.update { it.copy(playlist = currentPl) }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (activeTargetSongId == song.id) {
                    repo.clearStreamCache(song.id)
                    _uiState.update { it.copy(isLoadingStream = false, isPlaying = false, downloadNotice = "Unable to stream \"${song.title}\"") }
                }
            }
        }
    }

    /** Plays a curated artist mix where the Next Up queue strictly contains tracks by that specific artist */
    fun playArtistMix(artistName: String, initialSongs: List<Song> = emptyList()) {
        viewModelScope.launch {
            val artistNorm = artistName.trim().lowercase()
            val fromAlbums = _uiState.value.albums.flatMap { it.songs }.filter { it.artist.lowercase().contains(artistNorm) }
            val fromRecs = _uiState.value.recommendations.filter { it.artist.lowercase().contains(artistNorm) }
            val fromLibrary = _uiState.value.favoriteSongs.filter { it.artist.lowercase().contains(artistNorm) }
            val combined = (initialSongs + fromAlbums + fromRecs + fromLibrary).filter { it.id.isNotBlank() }.distinctBy { it.id }

            val fullArtistPlaylist = if (combined.size < 4) {
                val searched = repo.search("$artistName best songs").filter { 
                    it.artist.lowercase().contains(artistNorm) || it.title.lowercase().contains(artistNorm) 
                }
                (combined + searched).distinctBy { it.id }
            } else combined

            val firstTrack = fullArtistPlaylist.firstOrNull() ?: initialSongs.firstOrNull() ?: return@launch
            playSong(firstTrack, playlist = fullArtistPlaylist)
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
            val currentLang = repo.detectLanguage(current)
            val currentGenre = repo.detectGenre(current)
            // Strict language & genre matching!
            return state.recommendations.firstOrNull { 
                !it.isAlbum && it.id != current.id && it.id !in queued && it.id !in sessionPlayedSongIds &&
                repo.detectLanguage(it).equals(currentLang, ignoreCase = true) &&
                repo.detectGenre(it).equals(currentGenre, ignoreCase = true)
            } ?: state.recommendations.firstOrNull {
                !it.isAlbum && it.id != current.id && it.id !in queued && it.id !in sessionPlayedSongIds &&
                repo.detectLanguage(it).equals(currentLang, ignoreCase = true)
            } ?: state.recommendations.firstOrNull {
                !it.isAlbum && it.id != current.id && it.id !in queued && it.id !in sessionPlayedSongIds
            } ?: state.recommendations.firstOrNull {
                // Continuous loop fallback: if all songs were played this session, cycle unqueued recommendations
                !it.isAlbum && it.id != current.id && it.id !in queued
            }
        }
        return null
    }

    private fun onAutoMixHandoff(incomingSong: Song, handoffPositionMs: Long) {
        lastAutoMixHandoffTime = System.currentTimeMillis()
        cachedAutoMixSongId = null
        cachedAutoMixCandidate = null
        sessionPlayedSongIds.add(incomingSong.id)
        val ctx = getApplication<Application>()
        val incomingDuration = if (incomingSong.durationSeconds > 0) incomingSong.durationSeconds * 1000L else _uiState.value.duration
        _uiState.update { s ->
            val pl = if (s.playlist.any { it.id == incomingSong.id }) s.playlist else {
                val idx = s.playlist.indexOfFirst { it.id == s.currentSong?.id }
                s.playlist.toMutableList().apply { add(if (idx >= 0) idx + 1 else size, incomingSong) }
            }
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
        if (!s.autoPlayEnabled) return
        val idx = s.playlist.indexOfFirst { it.id == seed.id }
        val upcoming = if (idx >= 0) s.playlist.size - idx - 1 else 0
        if (upcoming >= 4) return
        viewModelScope.launch {
            val related = try { repo.getRelatedSongs(seed) } catch (_: Exception) { emptyList() }
            val seedLang = repo.detectLanguage(seed)
            val seedGenre = repo.detectGenre(seed)
            val candidatesToAdd = if (related.isNotEmpty()) {
                val filteredRelated = related.filter { cand ->
                    val candLang = repo.detectLanguage(cand)
                    val candGenre = repo.detectGenre(cand)
                    val langMatch = seedLang.isBlank() || candLang.isBlank() || seedLang.equals(candLang, ignoreCase = true)
                    val genreMatch = seedGenre.isBlank() || candGenre.isBlank() || seedGenre.equals(candGenre, ignoreCase = true)
                    langMatch && genreMatch
                }
                if (filteredRelated.isNotEmpty()) filteredRelated else related
            } else {
                try { repo.search("${seed.artist} songs").filter { !it.isAlbum } } catch (_: Exception) { emptyList() }
            }
            if (candidatesToAdd.isNotEmpty()) {
                _uiState.update { st ->
                    val ids = st.playlist.map { it.id }.toSet()
                    val fresh = candidatesToAdd.filter { it.id !in ids && !it.isAlbum && it.id !in sessionPlayedSongIds }
                    val toAdd = if (fresh.isNotEmpty()) fresh else candidatesToAdd.filter { it.id !in ids && !it.isAlbum && it.id != seed.id }
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
            if (hqCover != song.coverUrl) {
                val updatedSong = song.copy(coverUrl = hqCover)
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

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) {
            if (autoMixEngine.isBusy) autoMixEngine.abortTransition()
            player.pause()
        } else player.resume()
    }
    fun setExpanded(exp: Boolean) { _uiState.update { it.copy(isExpanded = exp) } }
    fun seekTo(pos: Long) {
        autoMixEngine.abortTransition()
        player.seekTo(pos)
    }
    fun playNext(isAutoMix: Boolean = false) {
        val state = _uiState.value
        val currentSong = state.currentSong
        val currentPl = state.playlist
        if (currentPl.isEmpty()) return

        val currIdx = if (currentSong != null) currentPl.indexOfFirst { it.id == currentSong.id } else -1
        // Upcoming songs are already shuffled in-place by toggleShuffle(), so always advance sequentially.
        val nextIdx = currIdx + 1

        if (nextIdx in currentPl.indices) {
            val nextSong = currentPl[nextIdx]
            playSong(nextSong, currentPl, isFromSearch = false, isAutoMix = isAutoMix)
        } else if (state.autoPlayEnabled && currentSong != null) {
            viewModelScope.launch {
                val related = repo.getRelatedSongs(currentSong)
                val newTracks = related.filter { s -> currentPl.none { it.id == s.id } }
                if (newTracks.isNotEmpty()) {
                    val updatedPl = currentPl + newTracks
                    _uiState.update { it.copy(playlist = updatedPl) }
                    playSong(newTracks.first(), updatedPl, isFromSearch = false, isAutoMix = isAutoMix)
                }
            }
        } else if (state.repeatMode == 1 && currentPl.isNotEmpty()) {
            playSong(currentPl.first(), currentPl, isFromSearch = false, isAutoMix = isAutoMix)
        }
    }

    fun playPrevious() {
        val state = _uiState.value
        val currentSong = state.currentSong
        val currentPl = state.playlist
        if (currentPl.isEmpty()) return

        val currIdx = if (currentSong != null) currentPl.indexOfFirst { it.id == currentSong.id } else -1
        val prevIdx = currIdx - 1
        if (prevIdx in currentPl.indices) {
            val prevSong = currentPl[prevIdx]
            playSong(prevSong, currentPl, isFromSearch = false)
        } else if (currentPl.isNotEmpty()) {
            playSong(currentPl.last(), currentPl, isFromSearch = false)
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
        } else {
            storage.saveFavorite(ctx, song)
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

    private fun fetchLyrics(song: Song) {
        activeLyricsJob?.cancel()
        activeLyricsJob = viewModelScope.launch {
            val lyrics = repo.getLyrics(song.title, song.artist, song.durationSeconds)
            if (_uiState.value.currentSong?.id == song.id) {
                _uiState.update { it.copy(lyrics = lyrics) }
            }
        }
    }

    fun toggleAutoMix() {
        val nextMode = when (_uiState.value.autoMixMode) {
            com.beatles.player.player.automix.AutoMixMode.NEW -> com.beatles.player.player.automix.AutoMixMode.OLD
            com.beatles.player.player.automix.AutoMixMode.OLD -> com.beatles.player.player.automix.AutoMixMode.OFF
            com.beatles.player.player.automix.AutoMixMode.OFF -> com.beatles.player.player.automix.AutoMixMode.NEW
        }
        when (nextMode) {
            com.beatles.player.player.automix.AutoMixMode.NEW -> {
                autoMixEngine.harmonicMode = true
                autoMixEngine.extendedWindow = true
                _uiState.update { it.copy(autoMixMode = nextMode, autoMixEnabled = true, downloadNotice = "AutoMix: Harmonic Pro (New Algorithm)") }
                applyAutoMixRequirements(announce = false)
            }
            com.beatles.player.player.automix.AutoMixMode.OLD -> {
                autoMixEngine.harmonicMode = false
                autoMixEngine.extendedWindow = false
                _uiState.update { it.copy(autoMixMode = nextMode, autoMixEnabled = true, downloadNotice = "AutoMix: Classic Crossfade (Old Algorithm)") }
                applyAutoMixRequirements(announce = false)
            }
            com.beatles.player.player.automix.AutoMixMode.OFF -> {
                autoMixEngine.abortTransition()
                _uiState.update { it.copy(autoMixMode = nextMode, autoMixEnabled = false, downloadNotice = "AutoMix: Inactive (Off)") }
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

        // Return stably cached candidate for the current track if still valid
        if (cachedAutoMixSongId == current.id && cachedAutoMixCandidate != null) {
            val c = cachedAutoMixCandidate!!
            if (upcomingInQueue.any { it.id == c.id } || state.recommendations.any { it.id == c.id }) {
                return@withContext c
            }
        }

        val availableRecs = state.recommendations.filter { !it.isAlbum && it.id != current.id && it.id !in sessionPlayedSongIds }
        val allCandidates = (upcomingInQueue + availableRecs).distinctBy { it.id }
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

        // Dynamically promote the best matching song to index + 1
        if (idx >= 0 && (idx + 1 >= pl.size || pl[idx + 1].id != best.id)) {
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
                loadRecommendationsInternal()
                refreshPersonalizedMusic()
            } finally {
                _uiState.update { it.copy(isRefreshingHome = false, downloadNotice = "Home feed refreshed") }
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

        val userArtists = (selectedArtists + recent.map { it.artist } + favorites.map { it.artist })
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

        // 1. Seed search for broader variety based on user selection or language
        try {
            val seedArtist = distinctArtists.randomOrNull()
            val seedQuery = if (seedArtist != null) {
                val modifier = queryModifiers.random()
                "$seedArtist $modifier"
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
        autoMixEngine.release()
        player.release()
        super.onCleared() 
    }
}
