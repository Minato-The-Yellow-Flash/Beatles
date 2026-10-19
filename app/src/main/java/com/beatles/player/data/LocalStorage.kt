package com.beatles.player.data

import android.content.Context
import com.beatles.player.data.db.PlaylistDao
import com.beatles.player.data.db.SongDao
import com.beatles.player.data.db.UserDao
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object LocalStorage {
    private const val PREFS = "beatles_prefs"
    private val gson = Gson()

    private var songDaoInstance: SongDao? = null
    private var playlistDaoInstance: PlaylistDao? = null
    private var userDaoInstance: UserDao? = null

    private fun getSongDao(ctx: Context): SongDao {
        if (songDaoInstance == null) songDaoInstance = SongDao(ctx)
        return songDaoInstance!!
    }

    private fun getPlaylistDao(ctx: Context): PlaylistDao {
        if (playlistDaoInstance == null) playlistDaoInstance = PlaylistDao(ctx)
        return playlistDaoInstance!!
    }

    private fun getUserDao(ctx: Context): UserDao {
        if (userDaoInstance == null) userDaoInstance = UserDao(ctx)
        return userDaoInstance!!
    }

    private inline fun <reified T> getList(ctx: Context, key: String, clazz: Class<T>): List<T> {
        val json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key, null) ?: return emptyList()
        return try {
            val list = mutableListOf<T>()
            val arr = com.google.gson.JsonParser.parseString(json).asJsonArray
            for (elem in arr) {
                try {
                    list.add(gson.fromJson(elem, clazz))
                } catch (_: Exception) {}
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private inline fun <reified T> saveList(ctx: Context, key: String, list: List<T>) {
        try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(key, gson.toJson(list)).apply()
        } catch (_: Exception) {}
    }

    fun getFavorites(ctx: Context): List<Song> {
        return try {
            val dbList = getSongDao(ctx).favorites
            if (dbList.isNotEmpty()) dbList else getList(ctx, "favorites", Song::class.java)
        } catch (_: Exception) {
            getList(ctx, "favorites", Song::class.java)
        }
    }

    fun saveFavorite(ctx: Context, song: Song) {
        try {
            getSongDao(ctx).addFavorite(song)
            val list = getList(ctx, "favorites", Song::class.java).toMutableList()
            if (list.none { it.id == song.id }) {
                list.add(song)
                saveList(ctx, "favorites", list)
            }
        } catch (_: Exception) {}
    }

    fun removeFavorite(ctx: Context, id: String) {
        try {
            getSongDao(ctx).removeFavorite(id)
            saveList(ctx, "favorites", getList(ctx, "favorites", Song::class.java).filter { it.id != id })
        } catch (_: Exception) {}
    }

    fun isFavorite(ctx: Context, id: String): Boolean = try {
        getSongDao(ctx).isFavorite(id) || getList(ctx, "favorites", Song::class.java).any { it.id == id }
    } catch (_: Exception) { false }

    fun getRecentlyPlayed(ctx: Context): List<Song> {
        return try {
            val dbList = getSongDao(ctx).getRecentlyPlayed(30)
            if (dbList.isNotEmpty()) dbList else getList(ctx, "recent", Song::class.java)
        } catch (_: Exception) {
            getList(ctx, "recent", Song::class.java)
        }
    }

    fun addToRecentlyPlayed(ctx: Context, song: Song) {
        try {
            getSongDao(ctx).addToRecentlyPlayed(song)
            val list = getList(ctx, "recent", Song::class.java).toMutableList()
            list.removeAll { it.id == song.id }
            list.add(0, song)
            saveList(ctx, "recent", list.take(30))
        } catch (_: Exception) {}
    }

    fun isOnboarded(ctx: Context): Boolean = try {
        val pref = getUserDao(ctx).getPreference("onboarded", null)
        pref?.toBoolean() ?: ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("onboarded", false)
    } catch (_: Exception) { false }

    fun setOnboarded(ctx: Context) {
        try {
            getUserDao(ctx).setPreference("onboarded", "true")
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("onboarded", true).apply()
        } catch (_: Exception) {}
    }

    fun getUserName(ctx: Context): String = try {
        val name = getUserDao(ctx).getPreference("user_name", "")
        if (name.isNotBlank()) name else ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("user_name", "") ?: ""
    } catch (_: Exception) { "" }

    fun setUserName(ctx: Context, name: String) {
        try {
            getUserDao(ctx).setPreference("user_name", name)
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("user_name", name).apply()
        } catch (_: Exception) {}
    }

    fun isSleepExtendPopupEnabled(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("sleep_extend_popup_enabled", true)

    fun setSleepExtendPopupEnabled(ctx: Context, enabled: Boolean) {
        try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("sleep_extend_popup_enabled", enabled).apply()
        } catch (_: Exception) {}
    }

    fun getUserRole(ctx: Context): UserRole {
        val r = try {
            val roleStr = getUserDao(ctx).getPreference("user_role", null)
            roleStr ?: ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("user_role", "LISTENER") ?: "LISTENER"
        } catch (_: Exception) { "LISTENER" }
        return try { UserRole.valueOf(r) } catch (_: Exception) { UserRole.LISTENER }
    }

    fun setUserRole(ctx: Context, role: UserRole) {
        try {
            getUserDao(ctx).setPreference("user_role", role.name)
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("user_role", role.name).apply()
        } catch (_: Exception) {}
    }

    fun getAlbums(ctx: Context): List<Album> = getList(ctx, "albums", Album::class.java)

    fun saveAlbums(ctx: Context, albums: List<Album>) = saveList(ctx, "albums", albums)

    fun getPlaylists(ctx: Context): List<Playlist> {
        return try {
            val dbList = getPlaylistDao(ctx).playlists
            if (dbList.isNotEmpty()) dbList else getList(ctx, "playlists", Playlist::class.java)
        } catch (_: Exception) {
            getList(ctx, "playlists", Playlist::class.java)
        }
    }

    fun savePlaylists(ctx: Context, playlists: List<Playlist>) {
        try {
            for (pl in playlists) {
                getPlaylistDao(ctx).savePlaylist(pl)
            }
            saveList(ctx, "playlists", playlists)
        } catch (_: Exception) {}
    }

    fun getDownloadedSongs(ctx: Context): List<Song> {
        return try {
            val dbList = getSongDao(ctx).downloadedSongs
            if (dbList.isNotEmpty()) dbList else getList(ctx, "downloads", Song::class.java)
        } catch (_: Exception) {
            getList(ctx, "downloads", Song::class.java)
        }
    }

    fun saveDownloadedSong(ctx: Context, song: Song) {
        try {
            getSongDao(ctx).saveDownloadedSong(song, song.streamUrl ?: "")
            val list = getList(ctx, "downloads", Song::class.java).toMutableList()
            val existingIdx = list.indexOfFirst { it.id == song.id }
            if (existingIdx >= 0) {
                list[existingIdx] = song
            } else {
                list.add(song)
            }
            saveList(ctx, "downloads", list)
        } catch (_: Exception) {}
    }

    fun removeDownloadedSong(ctx: Context, id: String) {
        try {
            getSongDao(ctx).removeDownloadedSong(id)
            saveList(ctx, "downloads", getList(ctx, "downloads", Song::class.java).filter { it.id != id })
        } catch (_: Exception) {}
    }

    fun getAlbumSets(ctx: Context): List<AlbumSet> = getList(ctx, "album_sets", AlbumSet::class.java)

    fun saveAlbumSets(ctx: Context, sets: List<AlbumSet>) = saveList(ctx, "album_sets", sets)

    fun getThemeMode(ctx: Context): ThemeMode {
        val str = try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("theme_mode", "SYSTEM") ?: "SYSTEM"
        } catch (_: Exception) { "SYSTEM" }
        return try { ThemeMode.valueOf(str) } catch (_: Exception) { ThemeMode.SYSTEM }
    }

    fun setThemeMode(ctx: Context, mode: ThemeMode) {
        try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("theme_mode", mode.name).apply()
        } catch (_: Exception) {}
    }

    fun getSelectedLanguage(ctx: Context): String = try {
        val lang = getUserDao(ctx).getPreference("selected_language", "")
        if (lang.isNotBlank()) lang else ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("selected_language", "") ?: ""
    } catch (_: Exception) { "" }

    fun setSelectedLanguage(ctx: Context, lang: String) {
        try {
            getUserDao(ctx).setPreference("selected_language", lang)
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("selected_language", lang).apply()
        } catch (_: Exception) {}
    }

    fun getSelectedLanguages(ctx: Context): Set<String> {
        return try {
            val set = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet("selected_languages", null)
            if (!set.isNullOrEmpty()) set else {
                val single = getSelectedLanguage(ctx)
                if (single.isNotBlank()) setOf(single) else emptySet()
            }
        } catch (_: Exception) { emptySet() }
    }

    fun setSelectedLanguages(ctx: Context, langs: Set<String>) {
        try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putStringSet("selected_languages", langs).apply()
            if (langs.isNotEmpty()) setSelectedLanguage(ctx, langs.first())
        } catch (_: Exception) {}
    }

    fun isExtendedAutoMix(ctx: Context): Boolean = try {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("extended_automix", true)
    } catch (_: Exception) { true }

    fun setExtendedAutoMix(ctx: Context, enabled: Boolean) {
        try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("extended_automix", enabled).apply()
        } catch (_: Exception) {}
    }

    // ═══ USER PREFERENCE MEMORY (Artistic Affinity & Listening History) ═══

    fun getSelectedArtists(ctx: Context): Set<String> {
        return try {
            val set = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet("selected_artists", null)
            if (!set.isNullOrEmpty()) set else emptySet()
        } catch (_: Exception) { emptySet() }
    }

    fun saveSelectedArtists(ctx: Context, artists: Set<String>) {
        try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putStringSet("selected_artists", artists).apply()
            for (artist in artists) {
                updateArtistAffinity(ctx, artist, 15.0f)
            }
        } catch (_: Exception) {}
    }

    fun getArtistAffinityMap(ctx: Context): Map<String, Float> {
        val json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("artist_affinities", null) ?: return emptyMap()
        return try {
            val type = object : com.google.gson.reflect.TypeToken<Map<String, Float>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (_: Exception) { emptyMap() }
    }

    fun updateArtistAffinity(ctx: Context, artist: String, delta: Float) {
        val cleanArtist = artist.replace(" - Topic", "").trim()
        if (cleanArtist.isBlank()) return
        try {
            val current = getArtistAffinityMap(ctx).toMutableMap()
            val existing = current[cleanArtist] ?: 0f
            current[cleanArtist] = (existing + delta).coerceIn(0f, 200f)
            val json = gson.toJson(current)
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("artist_affinities", json).apply()
        } catch (_: Exception) {}
    }

    // ═══ GENRE & LANGUAGE AFFINITY TASTE MEMORY ═══

    fun getGenreAffinityMap(ctx: Context): Map<String, Float> {
        val json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("genre_affinities", null) ?: return emptyMap()
        return try {
            val type = object : com.google.gson.reflect.TypeToken<Map<String, Float>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (_: Exception) { emptyMap() }
    }

    fun updateGenreAffinity(ctx: Context, genre: String, delta: Float) {
        val cleanGenre = genre.trim()
        if (cleanGenre.isBlank()) return
        try {
            val current = getGenreAffinityMap(ctx).toMutableMap()
            val existing = current[cleanGenre] ?: 0f
            current[cleanGenre] = (existing + delta).coerceIn(0f, 200f)
            val json = gson.toJson(current)
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("genre_affinities", json).apply()
        } catch (_: Exception) {}
    }

    fun getLanguageAffinityMap(ctx: Context): Map<String, Float> {
        val json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("language_affinities", null) ?: return emptyMap()
        return try {
            val type = object : com.google.gson.reflect.TypeToken<Map<String, Float>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (_: Exception) { emptyMap() }
    }

    fun updateLanguageAffinity(ctx: Context, language: String, delta: Float) {
        val cleanLanguage = language.trim()
        if (cleanLanguage.isBlank()) return
        try {
            val current = getLanguageAffinityMap(ctx).toMutableMap()
            val existing = current[cleanLanguage] ?: 0f
            current[cleanLanguage] = (existing + delta).coerceIn(0f, 200f)
            val json = gson.toJson(current)
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("language_affinities", json).apply()
        } catch (_: Exception) {}
    }

    fun recordPlaybackContext(ctx: Context, artist: String, genre: String, language: String) {
        try {
            updateArtistAffinity(ctx, artist, 4.0f)
            updateGenreAffinity(ctx, genre, 4.0f)
            updateLanguageAffinity(ctx, language, 4.0f)
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("last_played_artist", artist)
                .putString("last_played_genre", genre)
                .putString("last_played_language", language)
                .apply()
        } catch (_: Exception) {}
    }

    fun getLastPlayedContext(ctx: Context): Triple<String, String, String> {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Triple(
            prefs.getString("last_played_artist", "") ?: "",
            prefs.getString("last_played_genre", "") ?: "",
            prefs.getString("last_played_language", "") ?: ""
        )
    }

    // ═══ APP UPDATE DISMISSAL & INSTALLATION STATE ═══

    fun getDismissedUpdateVersion(ctx: Context): String {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("dismissed_update_version", "") ?: ""
    }

    fun setDismissedUpdateVersion(ctx: Context, version: String) {
        try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("dismissed_update_version", version.trim())
                .apply()
        } catch (_: Exception) {}
    }

    fun getLastInstalledUpdateVersion(ctx: Context): String {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("last_installed_update_version", "") ?: ""
    }

    fun setLastInstalledUpdateVersion(ctx: Context, version: String) {
        try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("last_installed_update_version", version.trim())
                .apply()
        } catch (_: Exception) {}
    }

    fun getCachedPersonalizedAlbums(ctx: Context): List<Album> = getList(ctx, "personalized_albums", Album::class.java)

    fun savePersonalizedAlbums(ctx: Context, albums: List<Album>) = saveList(ctx, "personalized_albums", albums)

    fun getAnalyticsWebhookUrl(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("analytics_webhook_url", "") ?: ""

    fun setAnalyticsWebhookUrl(ctx: Context, url: String) {
        try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("analytics_webhook_url", url.trim())
                .apply()
        } catch (_: Exception) {}
    }

    fun getDistinctUserId(ctx: Context): String {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var id = prefs.getString("distinct_user_id", null)
        if (id.isNullOrBlank()) {
            id = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("distinct_user_id", id).apply()
        }
        return id
    }

    fun hasLoggedInstall(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("has_logged_install", false)

    fun setLoggedInstall(ctx: Context) {
        try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("has_logged_install", true).apply()
        } catch (_: Exception) {}
    }
}

enum class ThemeMode { SYSTEM, DARK, LIGHT }
