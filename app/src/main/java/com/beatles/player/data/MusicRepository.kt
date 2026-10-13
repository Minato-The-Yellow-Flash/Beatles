package com.beatles.player.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.net.URLEncoder

object MusicRepository {
    private val httpClient = OkHttpClient()
    private val gson = Gson()

    init { NewPipe.init(OkHttpDownloader()) }

    fun getBestThumbnailUrl(thumb: String): String {
        if (thumb.isEmpty()) return ""
        val ytMatch = Regex(".*/vi/([^/]+)/.*").find(thumb)
        if (ytMatch != null) {
            val vid = ytMatch.groupValues[1]
            return "https://i.ytimg.com/vi/$vid/hqdefault.jpg"
        }
        if (thumb.contains("googleusercontent.com") || thumb.contains("ggpht.com")) {
            return thumb.replace(Regex("=w\\d+-h\\d+.*"), "=w800-h800-l90-rj")
                .replace(Regex("=s\\d+.*"), "=s800-c")
        }
        return thumb
    }

    suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.trim().isEmpty()) return@withContext emptyList()
        try {
            val enc = URLEncoder.encode(query.trim(), "UTF-8")
            val req = Request.Builder()
                .url("https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&q=$enc")
                .header("User-Agent", "BeatlesApp/2.0 (Android; en-US)")
                .build()
            val resp = httpClient.newCall(req).execute()
            val body = resp.body?.string() ?: return@withContext emptyList()
            val jsonArr = gson.fromJson(body, com.google.gson.JsonArray::class.java)
            if (jsonArr != null && jsonArr.size() > 1 && jsonArr[1].isJsonArray) {
                return@withContext jsonArr[1].asJsonArray.mapNotNull { it.asString }.take(8)
            }
        } catch (_: Exception) {}
        emptyList()
    }

    fun fuzzyScore(query: String, target: String): Double {
        val q = query.trim().lowercase()
        val t = target.trim().lowercase()
        if (q.isEmpty() || t.isEmpty()) return 0.0
        if (t == q) return 2.0
        if (t.startsWith(q)) return 1.5 + (q.length.toDouble() / t.length)
        if (t.contains(q)) return 1.0 + (q.length.toDouble() / t.length)
        
        val tWords = t.split(Regex("[\\s\\-_|/]+")).filter { it.isNotBlank() }
        val qWords = q.split(Regex("[\\s\\-_|/]+")).filter { it.isNotBlank() }
        var wordMatches = 0
        for (qw in qWords) {
            if (tWords.any { tw -> tw.startsWith(qw) || qw.startsWith(tw) || levenshteinDistance(qw, tw) <= 1 }) {
                wordMatches++
            }
        }
        if (wordMatches > 0) {
            return 0.7 * (wordMatches.toDouble() / qWords.size)
        }

        val compareLen = minOf(t.length, q.length + 3)
        val dist = levenshteinDistance(q, t.take(compareLen))
        val maxLen = maxOf(q.length, compareLen)
        return (1.0 - (dist.toDouble() / maxLen)).coerceAtLeast(0.0)
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    private val NOISE_TERMS = listOf(
        "cover", "karaoke", "remix", "slowed", "reverb", "8d", "instrumental", "lofi", "lo-fi", "sped up",
        "nightcore", "bass boosted", "reaction", "tutorial", "status", "ringtone", "mashup", "shorts",
        "official video", "music video", "full video", "video song", "official music video", "full hd video", "4k video", "video", "skit", "teaser", "trailer"
    )

    private val VIDEO_EXCLUDE_REGEX = Regex("(?i)\\b(official\\s+video|music\\s+video|video\\s+song|full\\s+video|lyrical\\s+video|lyrics\\s+video|official\\s+music\\s+video|full\\s+hd\\s+video|4k\\s+video|hd\\s+video|visualizer|lyric\\s+video|dance\\s+video|short\\s+film|behind\\s+the\\s+scenes|making\\s+of)\\b")

    fun cleanSongTitle(raw: String): String {
        var t = raw
        // 1. Strip bracketed text containing video/audio/resolution tags: [Official Video], (8K Video), [Full Video], etc.
        t = t.replace(Regex("(?i)\\s*[\\[\\(]([^)\\]]*?(?:official|music\\s*video|full\\s*video|video\\s*song|video|audio|lyric|lyrics|8k|4k|hd|hq|1080p|720p|remaster|visualizer|teaser|trailer|clip)[^)\\]]*?)[\\]\\)]"), "")
        // 2. Strip trailing pipe/dash/slash tags: | 8K Video, | Full Video, | Official Video, etc.
        t = t.replace(Regex("(?i)\\s*(\\||//|/|•|–|-)\\s*(?:8k|4k|hd|hq|full\\s*hd|1080p)?\\s*(?:official\\s*video|music\\s*video|full\\s*video|video\\s*song|video|official\\s*audio|audio\\s*song|audio|lyric\\s*video|visualizer)\\s*.*$"), "")
        // 3. Strip standalone resolution and video junk
        t = t.replace(Regex("(?i)\\b(?:8k|4k|1080p|720p)\\s*(?:uhd|video|video\\s*song|full\\s*video)?\\b"), "")
        t = t.replace(Regex("(?i)\\b(?:full\\s+video|official\\s+video|video\\s+song)\\b"), "")
        // 4. Clean leading/trailing punctuation & excess whitespace
        t = t.replace(Regex("^[\\s\\|\\-\\–/:•]+|[\\s\\|\\-\\–/:•]+$"), "")
        t = t.replace(Regex("\\s+"), " ").trim()
        return if (t.isBlank()) raw.trim() else t
    }

    private fun normalizeForMatch(s: String): String =
        s.lowercase()
            .replace(" - topic", " ")
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()

    /** 0..1 – how well the query tokens are covered by title + artist (prefix + typo tolerant). */
    private fun tokenMatch(query: String, song: Song): Double {
        val qTokens = normalizeForMatch(query).split(" ").filter { it.isNotBlank() }
        if (qTokens.isEmpty()) return 0.0
        val hayTokens = normalizeForMatch("${song.title} ${song.artist}").split(" ").filter { it.isNotBlank() }
        var matched = 0.0
        for (qt in qTokens) {
            matched += when {
                hayTokens.any { it == qt } -> 1.0
                hayTokens.any { it.startsWith(qt) } -> 0.85
                qt.length >= 4 && hayTokens.any {
                    kotlin.math.abs(it.length - qt.length) <= 2 &&
                        levenshteinDistance(qt, it) <= (if (qt.length >= 7) 2 else 1)
                } -> 0.6
                else -> 0.0
            }
        }
        return matched / qTokens.size
    }

    private fun relevance(query: String, song: Song, rank: Int, total: Int): Double {
        val q = normalizeForMatch(query)
        val title = normalizeForMatch(song.title)
        val artist = normalizeForMatch(song.artist)
        val rankScore = (1.0 - rank.toDouble() / total.coerceAtLeast(1)) * 5.0
        val tMatch = tokenMatch(query, song)

        var score = tMatch * 25.0 + rankScore

        // High reward for exact or prefix title matches
        if (title == q) score += 50.0
        else if (title.startsWith(q)) score += 30.0
        else if (title.contains(q)) score += 20.0

        // Artist + Title combination matches
        if ("$title $artist" == q || "$artist $title" == q) score += 40.0
        else if ("$title $artist".contains(q) || "$artist $title".contains(q)) score += 15.0

        // Penalize noise terms (karaoke, status, ringtone, reaction) unless query specifically asks for them
        for (t in NOISE_TERMS) {
            if (title.contains(t) && !q.contains(t)) score -= 15.0
        }

        // Tie-breaker bonus for studio audio / official topic track ONLY if song already matches query!
        if (tMatch >= 0.5) {
            if (song.artist.contains("Topic", ignoreCase = true) || title.contains("audio")) {
                score += 3.0
            }
        }

        return score
    }

    private fun fetchSongs(query: String, filter: String = "all"): List<Song> {
        val isExplicitVideoSearch = query.lowercase().let {
            it.contains("video") || it.contains("mv") || it.contains("film") || it.contains("clip")
        }

        fun extractItems(qStr: String, filterList: List<String>): List<Song> {
            try {
                val extractor = ServiceList.YouTube.getSearchExtractor(qStr, filterList, "")
                extractor.fetchPage()
                val items = extractor.initialPage.items
                val list = mutableListOf<Song>()
                for (item in items) {
                    when (item) {
                        is StreamInfoItem -> {
                            val lowTitle = item.name.lowercase()
                            // Strictly filter short clips (< 45s previews/ringtones) and obvious non-music noise
                            if (item.duration < 45 || lowTitle.contains("teaser") || lowTitle.contains("trailer") ||
                                lowTitle.contains("reaction") || lowTitle.contains("ringtone") || lowTitle.contains("status") ||
                                (item.duration > 1200 && !query.lowercase().contains("mix") && !query.lowercase().contains("album"))) {
                                continue
                            }
                            val isVideo = VIDEO_EXCLUDE_REGEX.containsMatchIn(item.name) ||
                                    lowTitle.contains("official video") || lowTitle.contains("video song") ||
                                    lowTitle.contains("music video") || lowTitle.contains("full video")
                            val isAudio = item.uploaderName.contains("Topic", ignoreCase = true) ||
                                    lowTitle.contains("audio") || lowTitle.contains("provided to youtube")

                            val highRes = getBestThumbnailUrl(item.thumbnails.firstOrNull()?.url ?: "")
                            val cleanTitle = cleanSongTitle(item.name)
                            val song = Song(
                                item.url,
                                cleanTitle,
                                item.uploaderName.replace(" - Topic", "").trim(),
                                "Single",
                                highRes,
                                item.duration.toInt(),
                                isAlbum = false
                            )

                            if (isExplicitVideoSearch || isAudio || !isVideo || filterList.contains("music_songs")) {
                                list.add(song)
                            }
                        }
                        is PlaylistInfoItem -> {
                            val highRes = getBestThumbnailUrl(item.thumbnails.firstOrNull()?.url ?: "")
                            list.add(Song(item.url, item.name, item.uploaderName, "Album", highRes, 0, isAlbum = true))
                        }
                    }
                }
                return list
            } catch (_: Exception) {
                return emptyList()
            }
        }

        if (!isExplicitVideoSearch) {
            // First search directly in YouTube Music ("music_songs") to guarantee real studio audio tracks
            val ytmResults = extractItems(query, listOf("music_songs"))
            if (ytmResults.isNotEmpty()) {
                val pureAudio = ytmResults.filter { !VIDEO_EXCLUDE_REGEX.containsMatchIn(it.title) }
                if (pureAudio.isNotEmpty()) return pureAudio
                return ytmResults
            }
            // Fallback: search with audio keyword and filter out videos
            val audioResults = extractItems("$query audio", listOf("all"))
            val directResults = extractItems(query, listOf("all"))
            val combined = (audioResults + directResults).distinctBy { it.id }

            val pureAudio = combined.filter { song ->
                song.artist.contains("Topic", ignoreCase = true) ||
                song.title.contains("audio", ignoreCase = true) ||
                !VIDEO_EXCLUDE_REGEX.containsMatchIn(song.title)
            }
            if (pureAudio.isNotEmpty()) return pureAudio
            return combined
        }

        return extractItems(query, listOf("all"))
    }

    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        val q = query.trim().replace(Regex("\\s+"), " ")
        if (q.isEmpty()) return@withContext emptyList()
        try {
            var songs = try { fetchSongs(q, "all") } catch (_: Exception) { emptyList() }

            val bestMatch = songs.take(5).maxOfOrNull { tokenMatch(q, it) } ?: 0.0
            if (songs.size < 3 || bestMatch < 0.5) {
                val corrected = getSearchSuggestions(q).firstOrNull()
                if (!corrected.isNullOrBlank() && !corrected.equals(q, ignoreCase = true)) {
                    val fixed = try { fetchSongs(corrected, "all") } catch (_: Exception) { emptyList() }
                    if (fixed.isNotEmpty()) songs = fixed + songs
                }
            }

            val total = songs.size
            songs.withIndex()
                .map { (i, s) -> s to relevance(q, s, i, total) }
                .sortedByDescending { it.second }
                .map { it.first }
                .distinctBy { normalizeForMatch(it.title) + "|" + normalizeForMatch(it.artist) }
                .distinctBy { it.id }
        } catch (e: Exception) { emptyList() }
    }

    private val streamUrlCache = java.util.concurrent.ConcurrentHashMap<String, Pair<String, Long>>()

    fun clearStreamCache(url: String) {
        streamUrlCache.remove(url)
    }

    suspend fun getStreamUrl(url: String, fallbackSong: Song? = null): String = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cached = streamUrlCache[url]
        if (cached != null && (now - cached.second) < 30 * 60 * 1000L) {
            return@withContext cached.first
        }
        
        // If url is already a direct playable audio stream
        if (url.startsWith("http") && (url.endsWith(".mp3") || url.endsWith(".m4a") || url.contains(".googlevideo.com"))) {
            return@withContext url
        }

        val isDirectYt = (url.startsWith("http") && (url.contains("youtube.com") || url.contains("youtu.be"))) ||
                (url.length in 10..12 && !url.contains(" ") && !url.startsWith("ts_") && !url.startsWith("ka_") &&
                 !url.startsWith("dd_") && !url.startsWith("tb_") && !url.startsWith("sm_") && !url.startsWith("as_") &&
                 !url.startsWith("cp_") && !url.startsWith("feat_") && !url.startsWith("alb_") && !url.startsWith("trk_"))

        var targetUrl = url

        // Prioritize pure studio audio / Topic track over music videos, or resolve non-direct YouTube IDs
        if (fallbackSong != null && (!isDirectYt || fallbackSong.title.lowercase().contains("video"))) {
            try {
                val cleanTitle = cleanSongTitle(fallbackSong.title)
                val cleanArtist = fallbackSong.artist.replace(" - Topic", "").trim()
                if (cleanTitle.isNotBlank()) {
                    val searchQ = if (cleanArtist.isNotBlank()) "$cleanTitle $cleanArtist Topic" else "$cleanTitle audio"
                    val results = search(searchQ)
                    val topMatch = results.firstOrNull {
                        !it.isAlbum &&
                        (it.artist.contains("Topic", ignoreCase = true) || it.title.contains("audio", ignoreCase = true)) &&
                        !it.title.lowercase().contains("video")
                    } ?: results.firstOrNull { !it.isAlbum && !it.title.lowercase().contains("video") }
                      ?: results.firstOrNull { !it.isAlbum }
                    if (topMatch != null) {
                        targetUrl = topMatch.id
                    }
                }
            } catch (_: Exception) {}
        }

        try {
            val fullUrl = when {
                targetUrl.startsWith("http") -> targetUrl
                targetUrl.startsWith("/") -> "https://www.youtube.com$targetUrl"
                targetUrl.contains("watch?v=") -> "https://www.youtube.com/$targetUrl"
                else -> "https://www.youtube.com/watch?v=$targetUrl"
            }
            val streamInfo = StreamInfo.getInfo(ServiceList.YouTube, fullUrl)
            val extractedUrl = streamInfo.audioStreams.maxByOrNull { it.bitrate }?.content
                ?: streamInfo.videoStreams.maxByOrNull { it.resolution }?.content
                ?: throw Exception("No stream found")
            streamUrlCache[url] = extractedUrl to now
            streamUrlCache[targetUrl] = extractedUrl to now
            extractedUrl
        } catch (e: Exception) {
            // Secondary fallback: Search YouTube for pure audio / Topic track if direct extraction failed
            if (fallbackSong != null) {
                try {
                    val cleanTitle = cleanSongTitle(fallbackSong.title)
                    val cleanArtist = fallbackSong.artist.replace(" - Topic", "").trim()
                    val searchQ = "$cleanTitle $cleanArtist"
                    val results = search(searchQ)
                    val topMatch = results.firstOrNull { !it.isAlbum && it.id != targetUrl }
                    if (topMatch != null) {
                        val fbUrl = if (topMatch.id.startsWith("http")) topMatch.id else "https://www.youtube.com/watch?v=${topMatch.id}"
                        val streamInfo = StreamInfo.getInfo(ServiceList.YouTube, fbUrl)
                        val fbExtracted = streamInfo.audioStreams.maxByOrNull { it.bitrate }?.content
                            ?: streamInfo.videoStreams.maxByOrNull { it.resolution }?.content
                            ?: throw Exception("No fallback stream")
                        streamUrlCache[url] = fbExtracted to now
                        return@withContext fbExtracted
                    }
                } catch (_: Exception) {}
            }
            throw e
        }
    }

    suspend fun searchOriginalAlbum(query: String): Album? = withContext(Dispatchers.IO) {
        try {
            val cleanQ = query.replace(Regex("(?i)official|audio|video|lyrics|full song"), "").trim()
            if (cleanQ.length < 2) return@withContext null
            val encoded = URLEncoder.encode(cleanQ, "UTF-8")
            val req = Request.Builder()
                .url("https://itunes.apple.com/search?term=$encoded&media=music&entity=album&limit=1")
                .header("User-Agent", "BeatlesApp/2.0 (Android; en-US)")
                .build()
            val resp = httpClient.newCall(req).execute()
            val body = resp.body?.string() ?: return@withContext null
            val json = gson.fromJson(body, JsonObject::class.java)
            val results = json.getAsJsonArray("results")
            if (results != null && results.size() > 0) {
                val item = results[0].asJsonObject
                val collectionId = item.get("collectionId")?.asString ?: "alb_${System.currentTimeMillis()}"
                val albumName = item.get("collectionName")?.asString ?: cleanQ
                val artistName = item.get("artistName")?.asString ?: "Artist"
                val rawCover = item.get("artworkUrl100")?.asString ?: ""
                val hqCover = rawCover.replace("100x100bb", "1000x1000bb").replace("60x60bb", "1000x1000bb")
                val genre = item.get("primaryGenreName")?.asString ?: "Music"
                val rawDate = item.get("releaseDate")?.asString ?: "2026-01-01"
                val releaseYear = if (rawDate.length >= 4) rawDate.take(4) else "2026"
                val releaseDate = if (rawDate.length >= 10) rawDate.take(10) else "2026"

                // Fetch tracks for this album
                val trackReq = Request.Builder()
                    .url("https://itunes.apple.com/lookup?id=$collectionId&entity=song")
                    .header("User-Agent", "BeatlesApp/2.0 (Android; en-US)")
                    .build()
                val trackResp = httpClient.newCall(trackReq).execute()
                val trackBody = trackResp.body?.string()
                val songs = mutableListOf<Song>()
                if (trackBody != null) {
                    val trackJson = gson.fromJson(trackBody, JsonObject::class.java)
                    val trackResults = trackJson.getAsJsonArray("results")
                    if (trackResults != null) {
                        for (tr in trackResults) {
                            val trObj = tr.asJsonObject
                            if (trObj.get("wrapperType")?.asString == "track") {
                                val tName = trObj.get("trackName")?.asString ?: ""
                                val tArtist = trObj.get("artistName")?.asString ?: artistName
                                val tDur = (trObj.get("trackTimeMillis")?.asLong ?: 210000L) / 1000L
                                val tPreview = trObj.get("previewUrl")?.asString
                                if (tName.isNotBlank()) {
                                    songs.add(
                                        Song(
                                            id = "itunes_${trObj.get("trackId")?.asString ?: tName}",
                                            title = tName,
                                            artist = tArtist,
                                            album = albumName,
                                            coverUrl = hqCover,
                                            durationSeconds = tDur.toInt(),
                                            streamUrl = null
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                return@withContext lookupAlbumById(collectionId)
            }
        } catch (_: Exception) {}
        null
    }

    suspend fun lookupAlbumById(collectionId: String): Album? = withContext(Dispatchers.IO) {
        try {
            val trackReq = Request.Builder()
                .url("https://itunes.apple.com/lookup?id=$collectionId&entity=song")
                .header("User-Agent", "BeatlesApp/2.0 (Android; en-US)")
                .build()
            val trackResp = httpClient.newCall(trackReq).execute()
            val trackBody = trackResp.body?.string() ?: return@withContext null
            val trackJson = gson.fromJson(trackBody, JsonObject::class.java)
            val trackResults = trackJson.getAsJsonArray("results") ?: return@withContext null
            if (trackResults.size() == 0) return@withContext null

            val albHeader = trackResults[0].asJsonObject
            val albumName = albHeader.get("collectionName")?.asString ?: "Album"
            val artistName = albHeader.get("artistName")?.asString ?: "Artist"
            val rawCover = albHeader.get("artworkUrl100")?.asString ?: ""
            val hqCover = rawCover.replace("100x100bb", "1000x1000bb").replace("60x60bb", "1000x1000bb")
            val genre = albHeader.get("primaryGenreName")?.asString ?: "Music"
            val rawDate = albHeader.get("releaseDate")?.asString ?: "2026-01-01"
            val releaseYear = if (rawDate.length >= 4) rawDate.take(4) else "2026"
            val releaseDate = if (rawDate.length >= 10) rawDate.take(10) else "2026"

            val songs = mutableListOf<Song>()
            for (tr in trackResults) {
                val trObj = tr.asJsonObject
                if (trObj.get("wrapperType")?.asString == "track") {
                    val tName = trObj.get("trackName")?.asString ?: ""
                    val tArtist = trObj.get("artistName")?.asString ?: artistName
                    val tDur = (trObj.get("trackTimeMillis")?.asLong ?: 210000L) / 1000L
                    if (tName.isNotBlank()) {
                        songs.add(
                            Song(
                                id = "itunes_${trObj.get("trackId")?.asString ?: tName}",
                                title = tName,
                                artist = tArtist,
                                album = albumName,
                                coverUrl = hqCover,
                                durationSeconds = tDur.toInt(),
                                streamUrl = null
                            )
                        )
                    }
                }
            }

            Album(
                id = "alb_$collectionId",
                title = albumName,
                artist = artistName,
                coverUrl = hqCover,
                genre = genre,
                releaseYear = releaseYear,
                releaseDate = releaseDate,
                type = "Album",
                songs = songs
            )
        } catch (_: Exception) { null }
    }

    suspend fun getAlbumForSong(song: Song): Album = withContext(Dispatchers.IO) {
        // 1. Try track-level search to find the exact collectionId on iTunes
        try {
            val q = URLEncoder.encode("${song.title} ${song.artist}".trim(), "UTF-8")
            val req = Request.Builder()
                .url("https://itunes.apple.com/search?term=$q&media=music&entity=song&limit=1")
                .header("User-Agent", "BeatlesApp/2.0 (Android; en-US)")
                .build()
            val resp = httpClient.newCall(req).execute()
            val body = resp.body?.string()
            if (body != null) {
                val json = gson.fromJson(body, JsonObject::class.java)
                val results = json.getAsJsonArray("results")
                if (results != null && results.size() > 0) {
                    val trackObj = results[0].asJsonObject
                    val collectionId = trackObj.get("collectionId")?.asString
                    if (!collectionId.isNullOrEmpty()) {
                        val alb = lookupAlbumById(collectionId)
                        if (alb != null && alb.songs.isNotEmpty()) {
                            return@withContext alb
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Search original album with album and artist
        val isGenericAlbum = listOf("single", "related", "recommended", "album/playlist", "various", "audio", "video")
            .contains(song.album.lowercase().trim())
        val albumName = if (song.album.isNotBlank() && !isGenericAlbum) song.album else song.title
        val searched = searchOriginalAlbum("$albumName ${song.artist}")
        if (searched != null && searched.songs.isNotEmpty()) {
            return@withContext searched
        }
        val searchedFallback = searchOriginalAlbum(albumName)
        if (searchedFallback != null) {
            return@withContext searchedFallback.copy(songs = if (searchedFallback.songs.isEmpty()) listOf(song) else searchedFallback.songs)
        }
        Album(
            id = "alb_${song.id.hashCode()}",
            title = albumName,
            artist = song.artist,
            coverUrl = song.coverUrl,
            genre = song.genre,
            releaseYear = "2024",
            releaseDate = "2024",
            type = if (song.isAlbum) "Album" else "Single",
            songs = listOf(song)
        )
    }

    suspend fun getDynamicTrendingSearches(): List<String> = withContext(Dispatchers.IO) {
        val indianHitsFirst = listOf(
            "Arijit Singh", "Karan Aujla", "Diljit Dosanjh", "Shreya Ghoshal", "Sidhu Moose Wala",
            "Punjabi Pop", "Bollywood Hits", "Taylor Swift", "The Beatles", "Coldplay", "Lofi Beats", "EDM Hits"
        )
        try {
            val suggestions = getSearchSuggestions("trending songs")
            if (suggestions.isNotEmpty()) {
                val cleaned = suggestions.map { it.replace("trending songs", "").replace("2024", "").replace("2025", "").replace("2026", "").trim() }
                    .filter { it.length > 2 }
                    .take(6)
                if (cleaned.isNotEmpty()) {
                    return@withContext (indianHitsFirst.take(7) + cleaned + indianHitsFirst.drop(7)).distinct().take(12)
                }
            }
        } catch (_: Exception) {}
        indianHitsFirst
    }

    suspend fun searchInstantMatchingSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        if (query.trim().length < 2) return@withContext emptyList()
        val allResults = search(query.trim())
        allResults.take(4)
    }

    suspend fun getTopHits(page: Int = 1): List<Song> = withContext(Dispatchers.IO) {
        val query = when (page % 4) {
            0 -> "global top 50 hits 2024"
            1 -> "trending billboard music 2024"
            2 -> "top bollywood hits 2024"
            else -> "top punjabi pop songs 2024"
        }
        val res = search(query)
        res.map { s ->
            val hq = getHighResCover(s.title, s.artist, s.coverUrl)
            s.copy(coverUrl = hq)
        }
    }

    fun getFeaturedAlbums(): List<Album> {
        return listOf(
            Album(
                id = "feat_alb_1",
                title = "1989 (Taylor's Version)",
                artist = "Taylor Swift",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/11/a6/80/11a680e6-2e48-08fa-5e87-3f18e838d31f/23UM1IM11868.rgb.jpg/1000x1000bb.jpg",
                genre = "Pop",
                releaseYear = "2023",
                releaseDate = "Oct 27, 2023",
                type = "Album",
                songs = listOf(
                    Song("ts_1", "Style (Taylor's Version)", "Taylor Swift", "1989 (Taylor's Version)", "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/11/a6/80/11a680e6-2e48-08fa-5e87-3f18e838d31f/23UM1IM11868.rgb.jpg/1000x1000bb.jpg", 231),
                    Song("ts_2", "Blank Space (Taylor's Version)", "Taylor Swift", "1989 (Taylor's Version)", "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/11/a6/80/11a680e6-2e48-08fa-5e87-3f18e838d31f/23UM1IM11868.rgb.jpg/1000x1000bb.jpg", 231),
                    Song("ts_3", "Shake It Off (Taylor's Version)", "Taylor Swift", "1989 (Taylor's Version)", "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/11/a6/80/11a680e6-2e48-08fa-5e87-3f18e838d31f/23UM1IM11868.rgb.jpg/1000x1000bb.jpg", 219),
                    Song("ts_4", "Bad Blood (Taylor's Version)", "Taylor Swift", "1989 (Taylor's Version)", "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/11/a6/80/11a680e6-2e48-08fa-5e87-3f18e838d31f/23UM1IM11868.rgb.jpg/1000x1000bb.jpg", 211),
                    Song("ts_5", "Wildest Dreams (Taylor's Version)", "Taylor Swift", "1989 (Taylor's Version)", "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/11/a6/80/11a680e6-2e48-08fa-5e87-3f18e838d31f/23UM1IM11868.rgb.jpg/1000x1000bb.jpg", 220)
                )
            ),
            Album(
                id = "feat_alb_2",
                title = "Making Memories",
                artist = "Karan Aujla",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/d3/08/bc/d308bc6a-20e1-6532-d933-35d1b429210e/5054197755538.jpg/1000x1000bb.jpg",
                genre = "Punjabi Pop",
                releaseYear = "2023",
                releaseDate = "Aug 18, 2023",
                type = "Album",
                songs = listOf(
                    Song("ka_1", "Softly", "Karan Aujla", "Making Memories", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/d3/08/bc/d308bc6a-20e1-6532-d933-35d1b429210e/5054197755538.jpg/1000x1000bb.jpg", 154),
                    Song("ka_2", "White Brown Black", "Karan Aujla", "Making Memories", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/d3/08/bc/d308bc6a-20e1-6532-d933-35d1b429210e/5054197755538.jpg/1000x1000bb.jpg", 178),
                    Song("ka_3", "Admiring You", "Karan Aujla", "Making Memories", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/d3/08/bc/d308bc6a-20e1-6532-d933-35d1b429210e/5054197755538.jpg/1000x1000bb.jpg", 204),
                    Song("ka_4", "Winning Speech", "Karan Aujla", "Making Memories", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/d3/08/bc/d308bc6a-20e1-6532-d933-35d1b429210e/5054197755538.jpg/1000x1000bb.jpg", 192)
                )
            ),
            Album(
                id = "feat_alb_3",
                title = "Ghost",
                artist = "Diljit Dosanjh",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/ec/9a/1f/ec9a1fb9-dc98-cd4f-d4c9-01eed5e67b19/859778016276_cover.jpg/1000x1000bb.jpg",
                genre = "Punjabi Fusion",
                releaseYear = "2023",
                releaseDate = "Sep 29, 2023",
                type = "Album",
                songs = listOf(
                    Song("dd_1", "Kinni Kinni", "Diljit Dosanjh", "Ghost", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/ec/9a/1f/ec9a1fb9-dc98-cd4f-d4c9-01eed5e67b19/859778016276_cover.jpg/1000x1000bb.jpg", 202),
                    Song("dd_2", "Lover", "Diljit Dosanjh", "Ghost", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/ec/9a/1f/ec9a1fb9-dc98-cd4f-d4c9-01eed5e67b19/859778016276_cover.jpg/1000x1000bb.jpg", 188),
                    Song("dd_3", "Case", "Diljit Dosanjh", "Ghost", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/ec/9a/1f/ec9a1fb9-dc98-cd4f-d4c9-01eed5e67b19/859778016276_cover.jpg/1000x1000bb.jpg", 175),
                    Song("dd_4", "Naina", "Diljit Dosanjh", "Ghost", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/ec/9a/1f/ec9a1fb9-dc98-cd4f-d4c9-01eed5e67b19/859778016276_cover.jpg/1000x1000bb.jpg", 180)
                )
            ),
            Album(
                id = "feat_alb_4",
                title = "Abbey Road",
                artist = "The Beatles",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/b8/fc/2d/b8fc2d07-d23f-a0ce-4fca-b7b418bb7ce6/13UMGIM63890.rgb.jpg/1000x1000bb.jpg",
                genre = "Classic Rock",
                releaseYear = "1969",
                releaseDate = "Sep 26, 1969",
                type = "Album",
                songs = listOf(
                    Song("tb_1", "Come Together", "The Beatles", "Abbey Road", "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/b8/fc/2d/b8fc2d07-d23f-a0ce-4fca-b7b418bb7ce6/13UMGIM63890.rgb.jpg/1000x1000bb.jpg", 259),
                    Song("tb_2", "Something", "The Beatles", "Abbey Road", "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/b8/fc/2d/b8fc2d07-d23f-a0ce-4fca-b7b418bb7ce6/13UMGIM63890.rgb.jpg/1000x1000bb.jpg", 182),
                    Song("tb_3", "Here Comes The Sun", "The Beatles", "Abbey Road", "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/b8/fc/2d/b8fc2d07-d23f-a0ce-4fca-b7b418bb7ce6/13UMGIM63890.rgb.jpg/1000x1000bb.jpg", 185),
                    Song("tb_4", "Octopus's Garden", "The Beatles", "Abbey Road", "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/b8/fc/2d/b8fc2d07-d23f-a0ce-4fca-b7b418bb7ce6/13UMGIM63890.rgb.jpg/1000x1000bb.jpg", 171)
                )
            ),
            Album(
                id = "feat_alb_5",
                title = "Moosetape",
                artist = "Sidhu Moose Wala",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/97/69/58/976958ae-725e-bd41-6755-f0921c697840/810063889609_cover.jpg/1000x1000bb.jpg",
                genre = "Punjabi Hip-Hop",
                releaseYear = "2021",
                releaseDate = "May 15, 2021",
                type = "Album",
                songs = listOf(
                    Song("sm_1", "295", "Sidhu Moose Wala", "Moosetape", "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/97/69/58/976958ae-725e-bd41-6755-f0921c697840/810063889609_cover.jpg/1000x1000bb.jpg", 270),
                    Song("sm_2", "These Days", "Sidhu Moose Wala", "Moosetape", "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/97/69/58/976958ae-725e-bd41-6755-f0921c697840/810063889609_cover.jpg/1000x1000bb.jpg", 212),
                    Song("sm_3", "Goat", "Sidhu Moose Wala", "Moosetape", "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/97/69/58/976958ae-725e-bd41-6755-f0921c697840/810063889609_cover.jpg/1000x1000bb.jpg", 215),
                    Song("sm_4", "Burberry", "Sidhu Moose Wala", "Moosetape", "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/97/69/58/976958ae-725e-bd41-6755-f0921c697840/810063889609_cover.jpg/1000x1000bb.jpg", 190)
                )
            ),
            Album(
                id = "feat_alb_6",
                title = "Best of Arijit Singh",
                artist = "Arijit Singh",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/2d/11/b9/2d11b994-b4fa-19eb-953d-70b472165e95/8903431566911_cover.jpg/1000x1000bb.jpg",
                genre = "Bollywood Romance",
                releaseYear = "2024",
                releaseDate = "Jan 12, 2024",
                type = "Album",
                songs = listOf(
                    Song("as_1", "Tum Hi Ho", "Arijit Singh", "Aashiqui 2", "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/2d/11/b9/2d11b994-b4fa-19eb-953d-70b472165e95/8903431566911_cover.jpg/1000x1000bb.jpg", 262),
                    Song("as_2", "Channa Mereya", "Arijit Singh", "Ae Dil Hai Mushkil", "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/2d/11/b9/2d11b994-b4fa-19eb-953d-70b472165e95/8903431566911_cover.jpg/1000x1000bb.jpg", 289),
                    Song("as_3", "Kesariya", "Arijit Singh", "Brahmastra", "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/2d/11/b9/2d11b994-b4fa-19eb-953d-70b472165e95/8903431566911_cover.jpg/1000x1000bb.jpg", 268),
                    Song("as_4", "Apna Bana Le", "Arijit Singh", "Bhediya", "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/2d/11/b9/2d11b994-b4fa-19eb-953d-70b472165e95/8903431566911_cover.jpg/1000x1000bb.jpg", 261)
                )
            ),
            Album(
                id = "feat_alb_7",
                title = "UTOPIA",
                artist = "Travis Scott",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/7d/4f/94/7d4f9468-56e1-3a2d-7186-c8088170ef58/196871341899.jpg/1000x1000bb.jpg",
                genre = "Hip-Hop",
                releaseYear = "2023",
                releaseDate = "Jul 28, 2023",
                type = "Album",
                songs = listOf(
                    Song("ts_u1", "FE!N", "Travis Scott", "UTOPIA", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/7d/4f/94/7d4f9468-56e1-3a2d-7186-c8088170ef58/196871341899.jpg/1000x1000bb.jpg", 191),
                    Song("ts_u2", "MELTDOWN", "Travis Scott", "UTOPIA", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/7d/4f/94/7d4f9468-56e1-3a2d-7186-c8088170ef58/196871341899.jpg/1000x1000bb.jpg", 246),
                    Song("ts_u3", "TELEKINESIS", "Travis Scott", "UTOPIA", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/7d/4f/94/7d4f9468-56e1-3a2d-7186-c8088170ef58/196871341899.jpg/1000x1000bb.jpg", 353),
                    Song("ts_u4", "MY EYES", "Travis Scott", "UTOPIA", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/7d/4f/94/7d4f9468-56e1-3a2d-7186-c8088170ef58/196871341899.jpg/1000x1000bb.jpg", 251)
                )
            ),
            Album(
                id = "feat_alb_8",
                title = "A Rush of Blood to the Head",
                artist = "Coldplay",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/b9/b4/2a/b9b42ad1-1e25-5096-da43-497a247e69a3/190295978051.jpg/1000x1000bb.jpg",
                genre = "Alternative Rock",
                releaseYear = "2002",
                releaseDate = "Aug 26, 2002",
                type = "Album",
                songs = listOf(
                    Song("cp_1", "The Scientist", "Coldplay", "A Rush of Blood to the Head", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/b9/b4/2a/b9b42ad1-1e25-5096-da43-497a247e69a3/190295978051.jpg/1000x1000bb.jpg", 309),
                    Song("cp_2", "Clocks", "Coldplay", "A Rush of Blood to the Head", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/b9/b4/2a/b9b42ad1-1e25-5096-da43-497a247e69a3/190295978051.jpg/1000x1000bb.jpg", 307),
                    Song("cp_3", "In My Place", "Coldplay", "A Rush of Blood to the Head", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/b9/b4/2a/b9b42ad1-1e25-5096-da43-497a247e69a3/190295978051.jpg/1000x1000bb.jpg", 228),
                    Song("cp_4", "Warning Sign", "Coldplay", "A Rush of Blood to the Head", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/b9/b4/2a/b9b42ad1-1e25-5096-da43-497a247e69a3/190295978051.jpg/1000x1000bb.jpg", 331)
                )
            ),
            Album(
                id = "feat_alb_9",
                title = "After Hours",
                artist = "The Weeknd",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/a4/be/89/a4be896e-21ef-e79e-436f-6f91d904719c/20UMGIM10300.rgb.jpg/1000x1000bb.jpg",
                genre = "Pop",
                releaseYear = "2020",
                releaseDate = "Mar 20, 2020",
                type = "Album",
                songs = listOf(
                    Song("tw_1", "Blinding Lights", "The Weeknd", "After Hours", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/a4/be/89/a4be896e-21ef-e79e-436f-6f91d904719c/20UMGIM10300.rgb.jpg/1000x1000bb.jpg", 200),
                    Song("tw_2", "Save Your Tears", "The Weeknd", "After Hours", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/a4/be/89/a4be896e-21ef-e79e-436f-6f91d904719c/20UMGIM10300.rgb.jpg/1000x1000bb.jpg", 215),
                    Song("tw_3", "In Your Eyes", "The Weeknd", "After Hours", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/a4/be/89/a4be896e-21ef-e79e-436f-6f91d904719c/20UMGIM10300.rgb.jpg/1000x1000bb.jpg", 237),
                    Song("tw_4", "Heartless", "The Weeknd", "After Hours", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/a4/be/89/a4be896e-21ef-e79e-436f-6f91d904719c/20UMGIM10300.rgb.jpg/1000x1000bb.jpg", 198)
                )
            ),
            Album(
                id = "feat_alb_10",
                title = "HIT ME HARD AND SOFT",
                artist = "Billie Eilish",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/a6/50/66/a65066db-6c58-dc80-994f-4d37562916db/24UMGIM39294.rgb.jpg/1000x1000bb.jpg",
                genre = "Alternative Pop",
                releaseYear = "2024",
                releaseDate = "May 17, 2024",
                type = "Album",
                songs = listOf(
                    Song("be_1", "BIRDS OF A FEATHER", "Billie Eilish", "HIT ME HARD AND SOFT", "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/a6/50/66/a65066db-6c58-dc80-994f-4d37562916db/24UMGIM39294.rgb.jpg/1000x1000bb.jpg", 190),
                    Song("be_2", "LUNCH", "Billie Eilish", "HIT ME HARD AND SOFT", "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/a6/50/66/a65066db-6c58-dc80-994f-4d37562916db/24UMGIM39294.rgb.jpg/1000x1000bb.jpg", 179),
                    Song("be_3", "CHIHIRO", "Billie Eilish", "HIT ME HARD AND SOFT", "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/a6/50/66/a65066db-6c58-dc80-994f-4d37562916db/24UMGIM39294.rgb.jpg/1000x1000bb.jpg", 303),
                    Song("be_4", "WILDFLOWER", "Billie Eilish", "HIT ME HARD AND SOFT", "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/a6/50/66/a65066db-6c58-dc80-994f-4d37562916db/24UMGIM39294.rgb.jpg/1000x1000bb.jpg", 261)
                )
            ),
            Album(
                id = "feat_alb_11",
                title = "Best of Shreya Ghoshal",
                artist = "Shreya Ghoshal",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/07/21/df/0721df26-f7be-db50-7dc4-6e06821262d6/8903431940988_cover.jpg/1000x1000bb.jpg",
                genre = "Bollywood Romance",
                releaseYear = "2024",
                releaseDate = "Feb 10, 2024",
                type = "Album",
                songs = listOf(
                    Song("sg_1", "Sunn Raha Hai (Female)", "Shreya Ghoshal", "Aashiqui 2", "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/07/21/df/0721df26-f7be-db50-7dc4-6e06821262d6/8903431940988_cover.jpg/1000x1000bb.jpg", 314),
                    Song("sg_2", "Deewani Mastani", "Shreya Ghoshal", "Bajirao Mastani", "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/07/21/df/0721df26-f7be-db50-7dc4-6e06821262d6/8903431940988_cover.jpg/1000x1000bb.jpg", 340),
                    Song("sg_3", "Param Sundari", "Shreya Ghoshal", "Mimi", "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/07/21/df/0721df26-f7be-db50-7dc4-6e06821262d6/8903431940988_cover.jpg/1000x1000bb.jpg", 200),
                    Song("sg_4", "Manwa Laage", "Shreya Ghoshal", "Happy New Year", "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/07/21/df/0721df26-f7be-db50-7dc4-6e06821262d6/8903431940988_cover.jpg/1000x1000bb.jpg", 271)
                )
            ),
            Album(
                id = "feat_alb_12",
                title = "Atif Aslam Essentials",
                artist = "Atif Aslam",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/19/22/e1/1922e118-e3c3-3e12-40ee-c59ce1a4235e/8902894356499_cover.jpg/1000x1000bb.jpg",
                genre = "Bollywood Romance",
                releaseYear = "2023",
                releaseDate = "Sep 15, 2023",
                type = "Album",
                songs = listOf(
                    Song("aa_1", "Tajdar-e-Haram", "Atif Aslam", "Coke Studio", "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/19/22/e1/1922e118-e3c3-3e12-40ee-c59ce1a4235e/8902894356499_cover.jpg/1000x1000bb.jpg", 320),
                    Song("aa_2", "Jeene Laga Hoon", "Atif Aslam", "Ramaiya Vastavaiya", "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/19/22/e1/1922e118-e3c3-3e12-40ee-c59ce1a4235e/8902894356499_cover.jpg/1000x1000bb.jpg", 237),
                    Song("aa_3", "Tera Hone Laga Hoon", "Atif Aslam", "Ajab Prem Ki Ghazab Kahani", "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/19/22/e1/1922e118-e3c3-3e12-40ee-c59ce1a4235e/8902894356499_cover.jpg/1000x1000bb.jpg", 299),
                    Song("aa_4", "Dil Diyan Gallan", "Atif Aslam", "Tiger Zinda Hai", "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/19/22/e1/1922e118-e3c3-3e12-40ee-c59ce1a4235e/8902894356499_cover.jpg/1000x1000bb.jpg", 260)
                )
            ),
            Album(
                id = "feat_alb_13",
                title = "Still Rollin",
                artist = "Shubh",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/44/26/51/4426514e-4f36-3a7b-3b8c-57cae4db23d9/cover.jpg/500x500bb.jpg",
                genre = "Punjabi Hip-Hop",
                releaseYear = "2023",
                releaseDate = "May 19, 2023",
                type = "Album",
                songs = listOf(
                    Song("sh_1", "Cheques", "Shubh", "Still Rollin", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/44/26/51/4426514e-4f36-3a7b-3b8c-57cae4db23d9/cover.jpg/500x500bb.jpg", 183),
                    Song("sh_2", "Baller", "Shubh", "Still Rollin", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/44/26/51/4426514e-4f36-3a7b-3b8c-57cae4db23d9/cover.jpg/500x500bb.jpg", 148),
                    Song("sh_3", "No Love", "Shubh", "Still Rollin", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/44/26/51/4426514e-4f36-3a7b-3b8c-57cae4db23d9/cover.jpg/500x500bb.jpg", 170),
                    Song("sh_4", "Still Rollin", "Shubh", "Still Rollin", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/44/26/51/4426514e-4f36-3a7b-3b8c-57cae4db23d9/cover.jpg/500x500bb.jpg", 174)
                )
            ),
            Album(
                id = "feat_alb_14",
                title = "Rockstar",
                artist = "A.R. Rahman",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music118/v4/9c/64/74/9c647413-5683-1121-6679-b1d6d45f3c05/886443216892.jpg/1000x1000bb.jpg",
                genre = "Bollywood Sufi",
                releaseYear = "2011",
                releaseDate = "Sep 30, 2011",
                type = "Album",
                songs = listOf(
                    Song("ar_1", "Nadaan Parinde", "A.R. Rahman", "Rockstar", "https://is1-ssl.mzstatic.com/image/thumb/Music118/v4/9c/64/74/9c647413-5683-1121-6679-b1d6d45f3c05/886443216892.jpg/1000x1000bb.jpg", 386),
                    Song("ar_2", "Kun Faya Kun", "A.R. Rahman", "Rockstar", "https://is1-ssl.mzstatic.com/image/thumb/Music118/v4/9c/64/74/9c647413-5683-1121-6679-b1d6d45f3c05/886443216892.jpg/1000x1000bb.jpg", 473),
                    Song("ar_3", "Jo Bhi Main", "A.R. Rahman", "Rockstar", "https://is1-ssl.mzstatic.com/image/thumb/Music118/v4/9c/64/74/9c647413-5683-1121-6679-b1d6d45f3c05/886443216892.jpg/1000x1000bb.jpg", 275),
                    Song("ar_4", "Phir Se Ud Chala", "A.R. Rahman", "Rockstar", "https://is1-ssl.mzstatic.com/image/thumb/Music118/v4/9c/64/74/9c647413-5683-1121-6679-b1d6d45f3c05/886443216892.jpg/1000x1000bb.jpg", 271)
                )
            ),
            Album(
                id = "feat_alb_15",
                title = "Ek Tha Raja",
                artist = "Badshah",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/58/01/fa/5801fa04-7a91-4470-3444-eaae3b25f549/8903431976079_cover.jpg/1000x1000bb.jpg",
                genre = "Indian Hip-Hop",
                releaseYear = "2024",
                releaseDate = "Mar 18, 2024",
                type = "Album",
                songs = listOf(
                    Song("bad_1", "Daaku", "Badshah", "Ek Tha Raja", "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/58/01/fa/5801fa04-7a91-4470-3444-eaae3b25f549/8903431976079_cover.jpg/1000x1000bb.jpg", 178),
                    Song("bad_2", "Soulmate", "Badshah", "Ek Tha Raja", "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/58/01/fa/5801fa04-7a91-4470-3444-eaae3b25f549/8903431976079_cover.jpg/1000x1000bb.jpg", 213),
                    Song("bad_3", "God Damn", "Badshah", "Ek Tha Raja", "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/58/01/fa/5801fa04-7a91-4470-3444-eaae3b25f549/8903431976079_cover.jpg/1000x1000bb.jpg", 188)
                )
            ),
            Album(
                id = "feat_alb_16",
                title = "Hard Drive Vol. 1",
                artist = "Raftaar",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/13/2e/51/132e5192-d9c9-5246-8149-14a9386c9d09/cover.jpg/1000x1000bb.jpg",
                genre = "Desi Hip-Hop",
                releaseYear = "2022",
                releaseDate = "Nov 16, 2022",
                type = "Album",
                songs = listOf(
                    Song("raf_1", "RAASHAH", "Raftaar", "Hard Drive Vol. 1", "https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/13/2e/51/132e5192-d9c9-5246-8149-14a9386c9d09/cover.jpg/1000x1000bb.jpg", 168),
                    Song("raf_2", "No China", "Raftaar", "Hard Drive Vol. 1", "https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/13/2e/51/132e5192-d9c9-5246-8149-14a9386c9d09/cover.jpg/1000x1000bb.jpg", 204),
                    Song("raf_3", "Ice", "Raftaar", "Hard Drive Vol. 1", "https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/13/2e/51/132e5192-d9c9-5246-8149-14a9386c9d09/cover.jpg/1000x1000bb.jpg", 175)
                )
            ),
            Album(
                id = "feat_alb_17",
                title = "Deewana",
                artist = "Sonu Nigam",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/28/62/1d/28621d9f-a89c-5674-3c6c-a49688a202c4/8901112440122.jpg/1000x1000bb.jpg",
                genre = "Indipop Melody",
                releaseYear = "1999",
                releaseDate = "Jan 01, 1999",
                type = "Album",
                songs = listOf(
                    Song("sn_1", "Ab Mujhe Raat Din", "Sonu Nigam", "Deewana", "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/28/62/1d/28621d9f-a89c-5674-3c6c-a49688a202c4/8901112440122.jpg/1000x1000bb.jpg", 284),
                    Song("sn_2", "Deewana Tera", "Sonu Nigam", "Deewana", "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/28/62/1d/28621d9f-a89c-5674-3c6c-a49688a202c4/8901112440122.jpg/1000x1000bb.jpg", 270),
                    Song("sn_3", "Iss Qadar Pyar Hai", "Sonu Nigam", "Deewana", "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/28/62/1d/28621d9f-a89c-5674-3c6c-a49688a202c4/8901112440122.jpg/1000x1000bb.jpg", 298)
                )
            ),
            Album(
                id = "feat_alb_18",
                title = "Two Hearts Never Break the Same",
                artist = "AP Dhillon",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/7d/5d/44/7d5d4444-245c-e40f-7649-650058b725b7/859765275822_cover.jpg/1000x1000bb.jpg",
                genre = "Punjabi Pop",
                releaseYear = "2022",
                releaseDate = "Oct 07, 2022",
                type = "Album",
                songs = listOf(
                    Song("ap_1", "Summer High", "AP Dhillon", "Two Hearts Never Break the Same", "https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/7d/5d/44/7d5d4444-245c-e40f-7649-650058b725b7/859765275822_cover.jpg/1000x1000bb.jpg", 177),
                    Song("ap_2", "Dil Nu", "AP Dhillon", "Two Hearts Never Break the Same", "https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/7d/5d/44/7d5d4444-245c-e40f-7649-650058b725b7/859765275822_cover.jpg/1000x1000bb.jpg", 228),
                    Song("ap_3", "Wo Noor", "AP Dhillon", "Two Hearts Never Break the Same", "https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/7d/5d/44/7d5d4444-245c-e40f-7649-650058b725b7/859765275822_cover.jpg/1000x1000bb.jpg", 188)
                )
            ),
            Album(
                id = "feat_alb_19",
                title = "Yeh Jawaani Hai Deewani",
                artist = "Pritam",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/c3/00/62/c30062df-a4bb-3ef7-4a0b-1915ea0aa9c7/886443993212.jpg/1000x1000bb.jpg",
                genre = "Bollywood Hits",
                releaseYear = "2013",
                releaseDate = "May 31, 2013",
                type = "Album",
                songs = listOf(
                    Song("pr_1", "Badtameez Dil", "Pritam", "Yeh Jawaani Hai Deewani", "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/c3/00/62/c30062df-a4bb-3ef7-4a0b-1915ea0aa9c7/886443993212.jpg/1000x1000bb.jpg", 252),
                    Song("pr_2", "Balam Pichkari", "Pritam", "Yeh Jawaani Hai Deewani", "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/c3/00/62/c30062df-a4bb-3ef7-4a0b-1915ea0aa9c7/886443993212.jpg/1000x1000bb.jpg", 289),
                    Song("pr_3", "Ilahi", "Pritam", "Yeh Jawaani Hai Deewani", "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/c3/00/62/c30062df-a4bb-3ef7-4a0b-1915ea0aa9c7/886443993212.jpg/1000x1000bb.jpg", 229),
                    Song("pr_4", "Kabira", "Pritam", "Yeh Jawaani Hai Deewani", "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/c3/00/62/c30062df-a4bb-3ef7-4a0b-1915ea0aa9c7/886443993212.jpg/1000x1000bb.jpg", 223)
                )
            ),
            Album(
                id = "feat_alb_20",
                title = "Jawan (Original Motion Picture Soundtrack)",
                artist = "Anirudh Ravichander",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/21/04/e7/2104e76a-549d-e4fb-79f9-e3954bb04c6f/8903431945198_cover.jpg/1000x1000bb.jpg",
                genre = "Kollywood Action",
                releaseYear = "2023",
                releaseDate = "Sep 07, 2023",
                type = "Album",
                songs = listOf(
                    Song("ar_j1", "Chaleya", "Anirudh Ravichander", "Jawan", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/21/04/e7/2104e76a-549d-e4fb-79f9-e3954bb04c6f/8903431945198_cover.jpg/1000x1000bb.jpg", 200),
                    Song("ar_j2", "Zinda Banda", "Anirudh Ravichander", "Jawan", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/21/04/e7/2104e76a-549d-e4fb-79f9-e3954bb04c6f/8903431945198_cover.jpg/1000x1000bb.jpg", 264),
                    Song("ar_j3", "Not Ramaiya Vastavaiya", "Anirudh Ravichander", "Jawan", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/21/04/e7/2104e76a-549d-e4fb-79f9-e3954bb04c6f/8903431945198_cover.jpg/1000x1000bb.jpg", 203)
                )
            ),
            Album(
                id = "feat_alb_21",
                title = "Pushpa: The Rise",
                artist = "Devi Sri Prasad",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/27/ef/82/27ef822c-a2da-dc80-92a5-4dc9cfda4752/cover.jpg/1000x1000bb.jpg",
                genre = "Tollywood Mass",
                releaseYear = "2021",
                releaseDate = "Dec 17, 2021",
                type = "Album",
                songs = listOf(
                    Song("dsp_1", "Oo Antava Mawa", "Devi Sri Prasad", "Pushpa: The Rise", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/27/ef/82/27ef822c-a2da-dc80-92a5-4dc9cfda4752/cover.jpg/1000x1000bb.jpg", 223),
                    Song("dsp_2", "Srivalli", "Devi Sri Prasad", "Pushpa: The Rise", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/27/ef/82/27ef822c-a2da-dc80-92a5-4dc9cfda4752/cover.jpg/1000x1000bb.jpg", 221),
                    Song("dsp_3", "Saami Saami", "Devi Sri Prasad", "Pushpa: The Rise", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/27/ef/82/27ef822c-a2da-dc80-92a5-4dc9cfda4752/cover.jpg/1000x1000bb.jpg", 223)
                )
            ),
            Album(
                id = "feat_alb_22",
                title = "Soul of Sid Sriram",
                artist = "Sid Sriram",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/92/ff/fb/92fffb8d-6490-482a-0a79-c8c7f3e8b093/8903431872166_cover.jpg/1000x1000bb.jpg",
                genre = "Carnatic Pop",
                releaseYear = "2023",
                releaseDate = "Aug 25, 2023",
                type = "Album",
                songs = listOf(
                    Song("ss_1", "Srivalli (Tamil)", "Sid Sriram", "Soul of Sid Sriram", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/92/ff/fb/92fffb8d-6490-482a-0a79-c8c7f3e8b093/8903431872166_cover.jpg/1000x1000bb.jpg", 221),
                    Song("ss_2", "Inkem Inkem", "Sid Sriram", "Soul of Sid Sriram", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/92/ff/fb/92fffb8d-6490-482a-0a79-c8c7f3e8b093/8903431872166_cover.jpg/1000x1000bb.jpg", 269),
                    Song("ss_3", "Samajavaragamana", "Sid Sriram", "Soul of Sid Sriram", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/92/ff/fb/92fffb8d-6490-482a-0a79-c8c7f3e8b093/8903431872166_cover.jpg/1000x1000bb.jpg", 214)
                )
            ),
            Album(
                id = "feat_alb_23",
                title = "Lollipop Lagelu Hits",
                artist = "Pawan Singh",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/fa/1c/bf/fa1cbf5c-20ce-e555-520e-6f8c0a8bc02a/8903431782298_cover.jpg/1000x1000bb.jpg",
                genre = "Bhojpuri Hits",
                releaseYear = "2022",
                releaseDate = "Jul 11, 2022",
                type = "Album",
                songs = listOf(
                    Song("ps_1", "Lollipop Lagelu", "Pawan Singh", "Lollipop Lagelu Hits", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/fa/1c/bf/fa1cbf5c-20ce-e555-520e-6f8c0a8bc02a/8903431782298_cover.jpg/1000x1000bb.jpg", 252),
                    Song("ps_2", "Pudina Ae Hasina", "Pawan Singh", "Lollipop Lagelu Hits", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/fa/1c/bf/fa1cbf5c-20ce-e555-520e-6f8c0a8bc02a/8903431782298_cover.jpg/1000x1000bb.jpg", 231)
                )
            ),
            Album(
                id = "feat_alb_24",
                title = "52 Gaj Ka Daman Hits",
                artist = "Renuka Panwar",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/44/21/df/4421df65-8b38-d7e7-a9a5-731ff7ec56bb/cover.jpg/500x500bb.jpg",
                genre = "Haryanvi Folk",
                releaseYear = "2021",
                releaseDate = "Oct 02, 2021",
                type = "Album",
                songs = listOf(
                    Song("rp_1", "52 Gaj Ka Daman", "Renuka Panwar", "52 Gaj Ka Daman Hits", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/44/21/df/4421df65-8b38-d7e7-a9a5-731ff7ec56bb/cover.jpg/500x500bb.jpg", 188),
                    Song("rp_2", "Kabootar", "Renuka Panwar", "52 Gaj Ka Daman Hits", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/44/21/df/4421df65-8b38-d7e7-a9a5-731ff7ec56bb/cover.jpg/500x500bb.jpg", 195)
                )
            ),
            Album(
                id = "feat_alb_25",
                title = "For All the Dogs",
                artist = "Drake",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/8a/95/96/8a959663-883a-d68a-6ce4-ea04bc999818/cover.jpg/1000x1000bb.jpg",
                genre = "Hip-Hop",
                releaseYear = "2023",
                releaseDate = "Oct 06, 2023",
                type = "Album",
                songs = listOf(
                    Song("dr_1", "First Person Shooter", "Drake", "For All the Dogs", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/8a/95/96/8a959663-883a-d68a-6ce4-ea04bc999818/cover.jpg/1000x1000bb.jpg", 247),
                    Song("dr_2", "IDGAF", "Drake", "For All the Dogs", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/8a/95/96/8a959663-883a-d68a-6ce4-ea04bc999818/cover.jpg/1000x1000bb.jpg", 260)
                )
            ),
            Album(
                id = "feat_alb_26",
                title = "Proof",
                artist = "BTS",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/05/23/94/0523945a-37c2-f1d7-2eb1-76634563a3fe/196922060876_Cover.jpg/1000x1000bb.jpg",
                genre = "K-Pop",
                releaseYear = "2022",
                releaseDate = "Jun 10, 2022",
                type = "Album",
                songs = listOf(
                    Song("bts_1", "Yet To Come", "BTS", "Proof", "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/05/23/94/0523945a-37c2-f1d7-2eb1-76634563a3fe/196922060876_Cover.jpg/1000x1000bb.jpg", 193),
                    Song("bts_2", "Dynamite", "BTS", "Proof", "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/05/23/94/0523945a-37c2-f1d7-2eb1-76634563a3fe/196922060876_Cover.jpg/1000x1000bb.jpg", 199)
                )
            )
        )
    }

    fun getRealPredictiveSongs(): List<Song> {
        return listOf(
            Song(
                id = "cWMxCE2SPag",
                title = "Softly",
                artist = "Karan Aujla",
                album = "Making Memories",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/d3/08/bc/d308bc6a-20e1-6532-d933-35d1b429210e/5054197755538.jpg/1000x1000bb.jpg",
                durationSeconds = 154
            ),
            Song(
                id = "3y-O_pQnOcs",
                title = "Kinni Kinni",
                artist = "Diljit Dosanjh",
                album = "Ghost",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/ec/9a/1f/ec9a1fb9-dc98-cd4f-d4c9-01eed5e67b19/859778016276_cover.jpg/1000x1000bb.jpg",
                durationSeconds = 202
            ),
            Song(
                id = "BddP6PYo2gs",
                title = "Kesariya",
                artist = "Arijit Singh",
                album = "Brahmastra",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/2d/11/b9/2d11b994-b4fa-19eb-953d-70b472165e95/8903431566911_cover.jpg/1000x1000bb.jpg",
                durationSeconds = 268
            ),
            Song(
                id = "sm_1",
                title = "295",
                artist = "Sidhu Moose Wala",
                album = "Moosetape",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/97/69/58/976958ae-725e-bd41-6755-f0921c697840/810063889609_cover.jpg/1000x1000bb.jpg",
                durationSeconds = 270
            ),
            Song(
                id = "sg_1",
                title = "Sunn Raha Hai",
                artist = "Shreya Ghoshal",
                album = "Aashiqui 2",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/2d/11/b9/2d11b994-b4fa-19eb-953d-70b472165e95/8903431566911_cover.jpg/1000x1000bb.jpg",
                durationSeconds = 390
            ),
            Song(
                id = "_s4j9gB2i7E",
                title = "Style",
                artist = "Taylor Swift",
                album = "1989 (Taylor's Version)",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/11/a6/80/11a680e6-2e48-08fa-5e87-3f18e838d31f/23UM1IM11868.rgb.jpg/1000x1000bb.jpg",
                durationSeconds = 231
            ),
            Song(
                id = "45cYwDMibGo",
                title = "Come Together",
                artist = "The Beatles",
                album = "Abbey Road",
                coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/b8/fc/2d/b8fc2d07-d23f-a0ce-4fca-b7b418bb7ce6/13UMGIM63890.rgb.jpg/1000x1000bb.jpg",
                durationSeconds = 259
            )
        )
    }

    suspend fun getTrending(): List<Song> = withContext(Dispatchers.IO) {
        val trending = search("trending hits 2024").take(10)
        trending.map { song ->
            val highRes = getHighResCover(song.title, song.artist, song.coverUrl)
            song.copy(coverUrl = highRes)
        }
    }

    suspend fun getAlbumTracks(url: String): List<Song> = withContext(Dispatchers.IO) {
        try {
            val playlistInfo = PlaylistInfo.getInfo(ServiceList.YouTube, url)
            playlistInfo.relatedItems.filterIsInstance<StreamInfoItem>().map { item ->
                val rawThumb = item.thumbnails.firstOrNull()?.url ?: ""
                val highRes = getBestThumbnailUrl(rawThumb)
                Song(item.url, item.name, item.uploaderName, playlistInfo.name, highRes, item.duration.toInt(), isAlbum = false)
            }
        } catch (e: Exception) { emptyList() }
    }

    val ENGLISH_ARTISTS = setOf(
        "the weeknd", "taylor swift", "drake", "billie eilish", "ed sheeran", "coldplay",
        "post malone", "dua lipa", "eminem", "travis scott", "ariana grande", "justin bieber",
        "kendrick lamar", "imagine dragons", "bruno mars", "maroon 5", "adele", "rihanna",
        "selena gomez", "shawn mendes", "katy perry", "charlie puth", "olivia rodrigo", "lady gaga",
        "harry styles", "beyoncé", "sam smith", "sia", "alan walker", "marshmello", "chainsmokers",
        "david guetta", "calvin harris", "avicii", "martin garrix", "queen", "the beatles", "beatles",
        "michael jackson", "snoop dogg", "kanye west", "linkin park", "twenty one pilots", "sabrina carpenter",
        "chappell roan", "benson boone", "teddy swims", "tate mcrae", "sza", "jack harlow", "doja cat",
        "dua lipa", "maroon 5", "onerepublic", "shawn mendes", "camila cabello", "halsey", "marshmello"
    )

    val PUNJABI_ARTISTS = setOf(
        "karan aujla", "diljit dosanjh", "sidhu moose wala", "sidhu moosewala", "ap dhillon",
        "shubh", "ammy virk", "gurdas maan", "babbu maan", "garry sandhu", "jass manak",
        "b praak", "jaani", "parmish verma", "jordan sandhu", "mankirt aulakh", "gurinder gill",
        "intense", "prem dhillon", "arjan dhillon", "tarsem jassar", "kulwinder billa",
        "nimrat khaira", "sunanda sharma", "prabh gill", "jasmine sandlas", "amrinder gill",
        "gippy grewal", "bohemia", "prophec", "sukha", "chani nattan", "korala maan"
    )

    val HINDI_ARTISTS = setOf(
        "arijit singh", "shreya ghoshal", "pritam", "a.r. rahman", "ar rahman", "atif aslam",
        "neha kakkar", "jubin nautiyal", "sonu nigam", "vishal mishra", "sachin-jigar", "sachin jigar",
        "kumar sanu", "udit narayan", "alka yagnik", "badshah", "yo yo honey singh", "honey singh",
        "kk", "mohit chauhan", "sunidhi chauhan", "armaan malik", "darshan raval", "anuv jain",
        "prateek kuhad", "stebin ben", "mithoon", "tanishk bagchi", "shankar ehsaan loy",
        "vishal-shekhar", "amit trivedi", "rahat fateh ali khan", "lucky ali", "javed ali",
        "himesh reshammiya", "monali thakur", "shaan", "papon", "raftaar", "divine", "emiway bantai"
    )

    val HARYANVI_ARTISTS = setOf(
        "renuka panwar", "gulzaar chhaniwala", "masoom sharma", "diler kharkiya", "raju punjabi",
        "ajay hooda", "ruchika jangid", "sumit goswami", "khasa aala chahar", "amit saini rohtakiya",
        "kd desi rock", "pranjal dahiya", "anu kadyan"
    )

    val BHOJPURI_ARTISTS = setOf(
        "pawan singh", "khesari lal yadav", "shilpi raj", "samar singh", "ritesh pandey",
        "pramod premi", "pramod premi yadav", "arvind akela kallu", "ankush raja", "neelkamal singh"
    )

    fun detectLanguage(song: Song): String {
        val a = song.artist.lowercase().replace(" - topic", "").trim()
        val t = song.title.lowercase()
        val combined = "$a $t ${song.album.lowercase()}"

        // 1. Exact or prefix artist matches
        if (ENGLISH_ARTISTS.any { a.contains(it) || it.contains(a) }) return "English"
        if (PUNJABI_ARTISTS.any { a.contains(it) || it.contains(a) }) return "Punjabi"
        if (HINDI_ARTISTS.any { a.contains(it) || it.contains(a) }) return "Hindi"
        if (HARYANVI_ARTISTS.any { a.contains(it) || it.contains(a) }) return "Haryanvi"
        if (BHOJPURI_ARTISTS.any { a.contains(it) || it.contains(a) }) return "Bhojpuri"

        // 2. Unicode script & regional artist detection
        if (combined.any { it in '\u0A00'..'\u0A7F' }) return "Punjabi" // Gurmukhi script
        if (combined.any { it in '\u0B80'..'\u0BFF' } || combined.contains("tamil") || combined.contains("anirudh")) return "Tamil"
        if (combined.any { it in '\u0C00'..'\u0C7F' } || combined.contains("telugu") || combined.contains("thaman")) return "Telugu"
        if (combined.any { it in '\u0D00'..'\u0D7F' } || combined.contains("malayalam")) return "Malayalam"
        if (combined.any { it in '\u0C80'..'\u0CFF' } || combined.contains("kannada")) return "Kannada"
        if (combined.any { it in '\u0980'..'\u09FF' } || combined.contains("bengali")) return "Bengali"
        if (combined.any { it in '\uAC00'..'\uD7AF' } || combined.contains("k-pop") || combined.contains("kpop") || combined.contains("bts") || combined.contains("blackpink") || combined.contains("newjeans")) return "Korean"
        if (combined.any { it in '\u3040'..'\u30FF' } || combined.contains("j-pop") || combined.contains("jpop") || combined.contains("yoasobi")) return "Japanese"
        if (combined.contains("reggaeton") || combined.contains("bad bunny") || combined.contains("karol g") || combined.contains("j balvin") || combined.contains("rosalía") || combined.contains("rosalia") || combined.contains("ozuna") || combined.contains("rauw alejandro")) return "Spanish"

        if (combined.any { it in '\u0900'..'\u097F' }) { // Devanagari script
            if (combined.contains("bhojpuri") || combined.contains("khesari") || combined.contains("pawan")) return "Bhojpuri"
            if (combined.contains("haryanvi")) return "Haryanvi"
            return "Hindi"
        }

        // 3. Distinctive keyword tokens
        val punjabiTokens = setOf("jatt", "pind", "gabru", "yaar", "kudi", "soorma", "mitran", "geet mp3", "speed records", "bhangra", "dhol", "punjabi", "chitta", "peg", "gedi", "parahuna", "daang")
        if (punjabiTokens.any { combined.contains(it) }) return "Punjabi"

        val haryanviTokens = setOf("haryanvi", "chhora", "daman", "hookah", "gaam", "badmash", "systum", "gaddi")
        if (haryanviTokens.any { combined.contains(it) }) return "Haryanvi"

        val bhojpuriTokens = setOf("bhojpuri", "kamariya", "patna", "bhatar", "saiyan", "laika", "piya", "arkestra")
        if (bhojpuriTokens.any { combined.contains(it) }) return "Bhojpuri"

        val hindiTokens = setOf("tum", "dil", "pyar", "ishq", "tera", "meri", "hum", "mohabbat", "khuda", "zindagi", "raatein", "sanam", "aashiq", "jaan", "tere", "mere", "sajna", "duniya", "naina", "saath", "deewana", "t-series", "zee music", "bollywood", "saregama")
        if (hindiTokens.any { combined.contains(it) }) return "Hindi"

        // Default to English for international Latin titles
        return "English"
    }

    fun detectGenre(song: Song): String {
        val text = "${song.title} ${song.artist} ${song.genre} ${song.album}".lowercase()
        return when {
            text.contains("hip-hop") || text.contains("hip hop") || text.contains("rap") || text.contains("drill") || text.contains("trap") -> "Hip-Hop"
            text.contains("rock") || text.contains("metal") || text.contains("guitar") -> "Rock"
            text.contains("r&b") || text.contains("soul") -> "R&B"
            text.contains("edm") || text.contains("club") || text.contains("remix") || text.contains("dance") || text.contains("electronic") -> "EDM"
            text.contains("bhangra") || text.contains("dhol") -> "Bhangra"
            text.contains("acoustic") || text.contains("unplugged") || text.contains("piano") -> "Acoustic"
            text.contains("devotional") || text.contains("bhajan") || text.contains("aarti") -> "Devotional"
            detectLanguage(song) == "Hindi" -> "Bollywood"
            detectLanguage(song) == "Punjabi" -> "Bhangra"
            else -> "Pop"
        }
    }

    fun fuzzySearchCatalog(query: String, catalog: List<Song>): List<Song> {
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase()
        return catalog.mapNotNull { song ->
            val titleScore = fuzzyScore(q, song.title)
            val artistScore = fuzzyScore(q, song.artist)
            val score = maxOf(titleScore, artistScore * 0.9)
            if (score >= 0.35) song to score else null
        }.sortedByDescending { it.second }
        .map { it.first }
        .distinctBy { it.id }
    }

    suspend fun getRelatedSongs(song: Song): List<Song> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Song>()
        val targetLang = detectLanguage(song)
        val targetGenre = detectGenre(song)
        val cleanArtist = song.artist.replace(" - Topic", "").trim()

        try {
            val fullUrl = if (song.id.startsWith("http")) song.id else "https://www.youtube.com/watch?v=${song.id}"
            val streamInfo = StreamInfo.getInfo(ServiceList.YouTube, fullUrl)
            val extracted = streamInfo.relatedItems.filterIsInstance<StreamInfoItem>()
                .filter { it.duration in 60..900 && it.url != song.id }
                .map { item ->
                    val rawThumb = item.thumbnails.firstOrNull()?.url ?: ""
                    val highRes = getBestThumbnailUrl(rawThumb)
                    Song(item.url, cleanSongTitle(item.name), item.uploaderName.replace(" - Topic", "").trim(), "Recommended", highRes, item.duration.toInt(), genre = "$targetLang • $targetGenre", isAlbum = false)
                }
            // Filter strictly to match the same language as the seed song!
            val matched = extracted.filter { detectLanguage(it) == targetLang }
            result.addAll(matched)
        } catch (_: Exception) {}

        // If fewer than 8, complement with targeted searches strictly in that language and genre
        if (result.size < 8 && cleanArtist.isNotBlank()) {
            try {
                val query = when (targetLang) {
                    "English" -> "$cleanArtist songs"
                    "Hindi" -> "$cleanArtist bollywood songs"
                    "Punjabi" -> "$cleanArtist punjabi songs"
                    "Haryanvi" -> "$cleanArtist haryanvi songs"
                    "Bhojpuri" -> "$cleanArtist bhojpuri songs"
                    "Tamil" -> "$cleanArtist tamil songs"
                    "Telugu" -> "$cleanArtist telugu songs"
                    "Malayalam" -> "$cleanArtist malayalam songs"
                    "Kannada" -> "$cleanArtist kannada songs"
                    "Bengali" -> "$cleanArtist bengali songs"
                    "Marathi" -> "$cleanArtist marathi songs"
                    "Gujarati" -> "$cleanArtist gujarati songs"
                    "Rajasthani" -> "$cleanArtist rajasthani songs"
                    "Spanish" -> "$cleanArtist spanish songs"
                    "Korean" -> "$cleanArtist kpop songs"
                    "Japanese" -> "$cleanArtist jpop songs"
                    "French" -> "$cleanArtist french songs"
                    "Arabic" -> "$cleanArtist arabic songs"
                    else -> if (targetLang.isNotBlank() && targetLang != "All") "$cleanArtist ${targetLang.lowercase()} songs" else "$cleanArtist hits"
                }
                val searchResults = search(query).filter { it.id != song.id && detectLanguage(it) == targetLang }
                result.addAll(searchResults.map { it.copy(genre = targetGenre) })
            } catch (_: Exception) {}
        }

        // If still fewer than 6, search popular hits strictly in the same language and genre
        if (result.size < 6) {
            try {
                val fallbackQuery = when (targetLang) {
                    "English" -> "top english ${targetGenre.lowercase()} songs"
                    "Hindi" -> "top bollywood hits"
                    "Punjabi" -> "top punjabi hits"
                    "Haryanvi" -> "top haryanvi hits"
                    "Bhojpuri" -> "top bhojpuri hits"
                    "Tamil" -> "top tamil hits"
                    "Telugu" -> "top telugu hits"
                    "Malayalam" -> "top malayalam hits"
                    "Kannada" -> "top kannada hits"
                    "Bengali" -> "top bengali hits"
                    "Marathi" -> "top marathi hits"
                    "Gujarati" -> "top gujarati hits"
                    "Rajasthani" -> "top rajasthani hits"
                    "Spanish" -> "top spanish hits"
                    "Korean" -> "top kpop hits"
                    "Japanese" -> "top japanese hits"
                    "French" -> "top french hits"
                    "Arabic" -> "top arabic hits"
                    else -> if (targetLang.isNotBlank() && targetLang != "All") "top ${targetLang.lowercase()} hits" else "trending music hits"
                }
                val moreSongs = search(fallbackQuery).filter { it.id != song.id && detectLanguage(it) == targetLang }
                result.addAll(moreSongs.map { it.copy(genre = targetGenre) })
            } catch (_: Exception) {}
        }

        result.distinctBy { "${it.title.lowercase()}_${it.artist.lowercase()}" }
    }

    suspend fun getRelatedSongs(url: String): List<Song> = getRelatedSongs(Song(id = url, title = "", artist = "", album = "", coverUrl = "", durationSeconds = 0))

    fun getHighResCover(title: String, artist: String, fallback: String): String {
        return try {
            val cleanTitle = title.replace(Regex("(?i)\\[.*?\\]|\\(.*?\\)"), "").replace(Regex("(?i)ft\\..*|feat\\..*|\\|.*|-.*"), "").trim()
            val cleanArtist = artist.replace(" - Topic", "").trim()
            val q = URLEncoder.encode("$cleanTitle $cleanArtist", "UTF-8")
            val req = Request.Builder().url("https://itunes.apple.com/search?term=$q&media=music&entity=song&limit=1").build()
            val resp = httpClient.newCall(req).execute()
            val body = resp.body?.string()
            if (body != null) {
                val json = gson.fromJson(body, JsonObject::class.java)
                val results = json.getAsJsonArray("results")
                if (results != null && results.size() > 0) {
                    val itunesArt = results[0].asJsonObject.get("artworkUrl100")?.asString
                        ?.replace("100x100bb", "1000x1000bb")
                    if (!itunesArt.isNullOrEmpty()) return itunesArt
                }
            }

            // Deezer API fallback for international and regional tracks
            try {
                val reqD = Request.Builder().url("https://api.deezer.com/search?q=$q&limit=1").build()
                val respD = httpClient.newCall(reqD).execute()
                val bodyD = respD.body?.string()
                if (bodyD != null) {
                    val jsonD = gson.fromJson(bodyD, JsonObject::class.java)
                    val dataD = jsonD.getAsJsonArray("data")
                    if (dataD != null && dataD.size() > 0) {
                        val albumObj = dataD[0].asJsonObject.getAsJsonObject("album")
                        val coverXl = albumObj?.get("cover_xl")?.asString ?: albumObj?.get("cover_big")?.asString
                        if (!coverXl.isNullOrEmpty()) return coverXl
                    }
                }
            } catch (_: Exception) {}

            fallback
        } catch (e: Exception) { fallback }
    }

    suspend fun getSuggestedArtists(artistName: String): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val q = URLEncoder.encode(artistName, "UTF-8")
            val req = Request.Builder().url("https://api.deezer.com/search/artist?q=$q&limit=6").build()
            val resp = httpClient.newCall(req).execute()
            val body = resp.body?.string() ?: return@withContext emptyList()
            val json = gson.fromJson(body, JsonObject::class.java)
            val data = json.getAsJsonArray("data") ?: return@withContext emptyList()
            data.map { it.asJsonObject }.map {
                val name = it.get("name")?.asString ?: ""
                val pic = it.get("picture_xl")?.asString ?: it.get("picture_big")?.asString ?: ""
                name to pic
            }.filter { it.first.isNotEmpty() && it.second.isNotEmpty() }
        } catch (e: Exception) { emptyList() }
    }

    private val recordLabels = listOf("sony music", "t-series", "saregama", "zee music", "speed records", "tips", "yrf", "white hill", "geet mp3")
    val lyricsCache = java.util.concurrent.ConcurrentHashMap<String, List<LyricLine>>()

    suspend fun getLyrics(title: String, artist: String, durationSeconds: Int = 0): List<LyricLine> = withContext(Dispatchers.IO) {
        val cacheKey = "${title.trim().lowercase()}|${artist.trim().lowercase()}"
        lyricsCache[cacheKey]?.let { return@withContext it }

        fun parseLrc(syncedLyrics: String): List<LyricLine> {
            return syncedLyrics.split("\n").filter { it.isNotBlank() }.mapNotNull { line ->
                val match = "\\[(\\d+):(\\d+\\.?\\d*)]".toRegex().find(line)
                if (match != null) {
                    val min = match.groupValues[1].toLongOrNull() ?: 0L
                    val sec = match.groupValues[2].toDoubleOrNull() ?: 0.0
                    val ms = min * 60000L + (sec * 1000).toLong()
                    val text = line.substring(match.range.last + 1).trim()
                    if (text.isNotEmpty()) LyricLine(ms, text) else null
                } else null
            }
        }

        suspend fun fetchExactLrcLib(track: String, art: String, dur: Int): List<LyricLine>? {
            try {
                val encTrack = URLEncoder.encode(track, "UTF-8")
                val encArt = URLEncoder.encode(art, "UTF-8")
                var url = "https://lrclib.net/api/get?track_name=$encTrack&artist_name=$encArt"
                if (dur > 0) url += "&duration=$dur"
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "BeatlesApp/2.0 (Android; en-US)")
                    .build()
                val body = httpClient.newCall(req).execute().body?.string() ?: return null
                val json = gson.fromJson(body, JsonObject::class.java) ?: return null
                val syncedLyrics = json.get("syncedLyrics")?.takeIf { !it.isJsonNull }?.asString
                if (!syncedLyrics.isNullOrEmpty()) {
                    val parsed = parseLrc(syncedLyrics)
                    if (parsed.isNotEmpty()) return parsed
                }
            } catch (_: Exception) {}
            return null
        }

        suspend fun searchLrcLibDirect(url: String, targetDur: Int = 0): List<LyricLine>? {
            try {
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "BeatlesApp/2.0 (Android; en-US)")
                    .build()
                val body = httpClient.newCall(req).execute().body?.string() ?: return null
                val arr = gson.fromJson(body, com.google.gson.JsonArray::class.java)
                if (arr != null && arr.size() > 0) {
                    val syncedItems = arr.filter {
                        val obj = it.asJsonObject
                        obj.get("syncedLyrics")?.isJsonNull == false && !obj.get("syncedLyrics").asString.isNullOrBlank()
                    }.map { it.asJsonObject }

                    val bestMatch = if (syncedItems.isNotEmpty()) {
                        if (targetDur > 0) {
                            syncedItems.minByOrNull {
                                val d = it.get("duration")?.asDouble ?: 0.0
                                kotlin.math.abs(d - targetDur)
                            } ?: syncedItems.first()
                        } else {
                            syncedItems.first()
                        }
                    } else {
                        arr.find { it.asJsonObject.get("plainLyrics")?.isJsonNull == false }?.asJsonObject
                    }
                    
                    if (bestMatch != null) {
                        val syncedLyrics = bestMatch.get("syncedLyrics")?.takeIf { !it.isJsonNull }?.asString
                        val plainLyrics = bestMatch.get("plainLyrics")?.takeIf { !it.isJsonNull }?.asString
                        
                        if (!syncedLyrics.isNullOrEmpty()) {
                            val parsed = parseLrc(syncedLyrics)
                            if (parsed.isNotEmpty()) return parsed
                        } else if (!plainLyrics.isNullOrEmpty()) {
                            return plainLyrics.split("\n").filter { it.isNotBlank() }.mapIndexed { idx, line ->
                                LyricLine(timeMs = idx * 3000L, text = line.trim())
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
            return null
        }

        suspend fun searchLrcLib(q: String, targetDur: Int = 0): List<LyricLine>? {
            return searchLrcLibDirect("https://lrclib.net/api/search?q=${URLEncoder.encode(q, "UTF-8")}", targetDur)
        }

        try {
            val t = title.replace(Regex("(?i)\\[.*?\\]|\\(.*?\\)"), "").trim()
            val parts = t.split(Regex("[|\\-–—]")).map { it.trim() }.filter { it.isNotEmpty() }
            val cleanArtist = artist.replace(" - Topic", "")
                .replace(Regex("(?i)vevo|official"), "")
                .replace(Regex("(?i)\\s+(feat\\.|ft\\.|with|&|x)\\s+.*"), "")
                .trim()
            val isLabel = recordLabels.any { cleanArtist.lowercase().contains(it) }

            var mainTitle = if (parts.isNotEmpty()) {
                parts[0].replace(Regex("(?i)official.*|video.*|audio.*|lyric.*|full song.*"), "").trim()
            } else t
            if (mainTitle.isEmpty()) mainTitle = t

            // 1. Try exact match on LRCLIB with duration if artist is known
            if (!isLabel && cleanArtist.isNotEmpty()) {
                if (durationSeconds > 0) {
                    val exactDur = fetchExactLrcLib(mainTitle, cleanArtist, durationSeconds)
                    if (exactDur != null && exactDur.isNotEmpty()) return@withContext exactDur
                }
                val exactNoDur = fetchExactLrcLib(mainTitle, cleanArtist, 0)
                if (exactNoDur != null && exactNoDur.isNotEmpty()) return@withContext exactNoDur

                // Dedicated parameter search
                try {
                    val pUrl = "https://lrclib.net/api/search?track_name=${URLEncoder.encode(mainTitle, "UTF-8")}&artist_name=${URLEncoder.encode(cleanArtist, "UTF-8")}"
                    val pRes = searchLrcLibDirect(pUrl, durationSeconds)
                    if (pRes != null && pRes.isNotEmpty()) return@withContext pRes
                } catch (_: Exception) {}
            }

            // 2. Search queries
            val queries = mutableListOf<String>()
            if (parts.isNotEmpty()) {
                if (!isLabel && cleanArtist.isNotEmpty()) {
                    queries.add("$mainTitle $cleanArtist")
                }
                queries.add(mainTitle)
                
                for (p in parts.drop(1)) {
                    val pClean = p.replace(Regex("(?i)official.*|video.*|audio.*|lyric.*|latest.*|punjabi.*|hindi.*|song.*"), "").trim()
                    if (pClean.length in 3..29) {
                        queries.add("$mainTitle $pClean")
                    }
                }
            }
            queries.add(t)
            if (!isLabel && cleanArtist.isNotEmpty()) {
                queries.add("$t $cleanArtist")
            }

            for (q in queries.distinct()) {
                if (q.length < 2) continue
                val res = searchLrcLib(q, durationSeconds)
                if (res != null && res.isNotEmpty()) {
                    lyricsCache[cacheKey] = res
                    return@withContext res
                }
            }

            // Fallback: lyrics.ovh
            if (!isLabel && cleanArtist.isNotEmpty()) {
                val reqOvh = Request.Builder().url("https://api.lyrics.ovh/v1/${URLEncoder.encode(cleanArtist, "UTF-8")}/${URLEncoder.encode(mainTitle, "UTF-8")}").build()
                val bodyOvh = httpClient.newCall(reqOvh).execute().body?.string()
                if (bodyOvh != null) {
                    val ovhJson = gson.fromJson(bodyOvh, JsonObject::class.java)
                    val ovhLyrics = ovhJson.get("lyrics")?.asString
                    if (!ovhLyrics.isNullOrEmpty()) {
                        val parsed = ovhLyrics.split("\n").filter { it.isNotBlank() }.mapIndexed { idx, line ->
                            LyricLine(timeMs = idx * 3000L, text = line.trim())
                        }
                        if (parsed.isNotEmpty()) {
                            lyricsCache[cacheKey] = parsed
                            return@withContext parsed
                        }
                    }
                }
            }

            emptyList()
        } catch (e: Exception) { emptyList() }
    }

    fun getPersonalizedFeaturedAlbums(
        selectedArtists: Set<String>,
        recentlyPlayed: List<Song>,
        favorites: List<Song> = emptyList(),
        language: String = "All",
        rotationSeed: Long = 0L
    ): List<Album> {
        val baseAlbums = getFeaturedAlbums()

        val userArtistNames = (selectedArtists.map { it.lowercase().trim() } +
                recentlyPlayed.map { it.artist.lowercase().trim() } +
                favorites.map { it.artist.lowercase().trim() }).filter { it.isNotBlank() }.toSet()
        val langLower = language.lowercase().trim()

        val filteredAlbums: List<Album> = if (userArtistNames.isNotEmpty()) {
            // Strictly match ONLY against user's actual selected artists and listening habits!
            val matched = baseAlbums.filter { alb ->
                val aLower = alb.artist.lowercase().trim()
                userArtistNames.any { aLower.contains(it) || it.contains(aLower) }
            }
            val missingArtists = selectedArtists.filter { sa ->
                baseAlbums.none { it.artist.contains(sa, ignoreCase = true) || sa.contains(it.artist, ignoreCase = true) }
            }
            val dynamicAlbums = missingArtists.mapNotNull { artistName ->
                val artistPic = com.beatles.player.ui.screens.CURATED_ONBOARD_ARTISTS.find { it.name.equals(artistName, ignoreCase = true) }?.imageUrl
                if (!artistPic.isNullOrBlank()) {
                    Album(
                        id = "feat_dyn_${artistName.hashCode()}",
                        title = "$artistName: Top Hits",
                        artist = artistName,
                        coverUrl = artistPic,
                        genre = com.beatles.player.ui.screens.CURATED_ONBOARD_ARTISTS.find { it.name.equals(artistName, ignoreCase = true) }?.genre ?: "Hits",
                        releaseYear = "2024",
                        releaseDate = "2024",
                        type = "Album",
                        songs = listOf(
                            Song("dyn_1_${artistName.hashCode()}", "$artistName Hits", artistName, "$artistName: Top Hits", artistPic, 210)
                        )
                    )
                } else null
            }
            val allMatched = (matched + dynamicAlbums).distinctBy { it.id }
            if (allMatched.isNotEmpty()) {
                allMatched
            } else if (langLower != "all" && langLower.isNotBlank()) {
                baseAlbums.filter { alb ->
                    alb.genre.lowercase().contains(langLower)
                }.ifEmpty { baseAlbums }
            } else {
                baseAlbums
            }
        } else if (langLower != "all" && langLower.isNotBlank()) {
            baseAlbums.filter { alb ->
                alb.genre.lowercase().contains(langLower)
            }.ifEmpty { baseAlbums }
        } else {
            baseAlbums
        }

        if (filteredAlbums.isEmpty()) return baseAlbums
        // Clean deterministic rotation on every refresh seed
        val offset = (kotlin.math.abs(rotationSeed) % filteredAlbums.size).toInt()
        return filteredAlbums.drop(offset) + filteredAlbums.take(offset)
    }

    fun getPersonalizedAlbumSets(selectedArtists: Set<String>, albums: List<Album>): List<AlbumSet> {
        val artistNames = selectedArtists.map { it.lowercase().trim() }
        val sets = mutableListOf<AlbumSet>()

        fun List<Album>.diverseArtists(): List<Album> = this.distinctBy { it.artist.lowercase().trim() }

        // 1. Personalized Favorite Artist Collection if user selected artists
        val matchedAlbums = albums.filter { alb ->
            val aLower = alb.artist.lowercase()
            artistNames.any { aLower.contains(it) || it.contains(aLower) }
        }.diverseArtists()
        if (matchedAlbums.isNotEmpty()) {
            sets.add(
                AlbumSet(
                    id = "set_personalized_picks",
                    name = "Curated For You",
                    description = "Handpicked albums based on your favorite artists",
                    albums = matchedAlbums.take(4)
                )
            )
        }

        // 2. Punjabi & Desi Beats Set
        val desiAlbums = albums.filter { it.genre.contains("Punjabi", ignoreCase = true) || it.genre.contains("Bollywood", ignoreCase = true) }.diverseArtists()
        if (desiAlbums.isNotEmpty()) {
            sets.add(
                AlbumSet(
                    id = "set_desi_vibes",
                    name = "Punjabi & Bollywood Hits",
                    description = "Top Punjabi blockbusters and soulful Bollywood albums",
                    albums = desiAlbums.take(3)
                )
            )
        }

        // 3. Pop & Rock Legends Set
        val popRockAlbums = albums.filter { it.genre.contains("Pop", ignoreCase = true) || it.genre.contains("Rock", ignoreCase = true) }.diverseArtists()
        if (popRockAlbums.isNotEmpty()) {
            sets.add(
                AlbumSet(
                    id = "set_pop_rock",
                    name = "Pop & Rock Essentials",
                    description = "Chart-topping pop anthems and iconic rock collections",
                    albums = popRockAlbums.take(3)
                )
            )
        }

        // 4. Global Hip-Hop & Rap Set
        val hipHopAlbums = albums.filter { it.genre.contains("Hip-Hop", ignoreCase = true) || it.genre.contains("Rap", ignoreCase = true) }.diverseArtists()
        if (hipHopAlbums.isNotEmpty()) {
            sets.add(
                AlbumSet(
                    id = "set_hiphop_vibes",
                    name = "Hip-Hop & Urban",
                    description = "Modern hip-hop heavyweights and rap mastertapes",
                    albums = hipHopAlbums.take(3)
                )
            )
        }

        return sets
    }
}
