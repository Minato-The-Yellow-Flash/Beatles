package com.beatles.player.data

import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Core engine for the Listening-Lane taste profile, metadata enrichment,
 * hard language gating, taxonomy mapping, and candidate scoring.
 */
object ListeningLaneEngine {

    // ─────────────────────────────────────────────────────────────────────────
    // 1. GENRE TAXONOMY & NORMALIZATION
    // ─────────────────────────────────────────────────────────────────────────

    private val GENRE_SYNONYMS = mapOf(
        "hip hop" to "hip-hop",
        "hiphop" to "hip-hop",
        "rap" to "hip-hop",
        "trap" to "trap",
        "boom bap" to "hip-hop",
        "drill" to "drill",
        "desi hip hop" to "hip-hop",
        "r&b" to "r&b",
        "rnb" to "r&b",
        "soul" to "r&b",
        "neo-soul" to "r&b",
        "bhangra" to "bhangra",
        "punjabi pop" to "punjabi pop",
        "punjabi" to "punjabi pop",
        "punjabi rap" to "punjabi pop",
        "desi pop" to "punjabi pop",
        "bollywood" to "bollywood",
        "filmi" to "filmi",
        "hindi" to "bollywood",
        "ghazal" to "ghazal",
        "sufi" to "sufi",
        "k-pop" to "k-pop",
        "kpop" to "k-pop",
        "j-pop" to "j-pop",
        "latin" to "latin",
        "reggaeton" to "reggaeton",
        "electronic" to "electronic",
        "edm" to "electronic",
        "house" to "electronic",
        "dance" to "dance pop",
        "techno" to "electronic",
        "trance" to "electronic",
        "alt-rock" to "alt-rock",
        "alternative rock" to "alt-rock",
        "indie rock" to "indie rock",
        "hard rock" to "rock",
        "metal" to "metal",
        "punk" to "punk",
        "indie pop" to "indie pop",
        "acoustic" to "acoustic",
        "classical" to "classical",
        "jazz" to "jazz",
        "blues" to "blues",
        "country" to "country",
        "folk" to "folk"
    )

    private val SUBGENRE_TO_PARENT = mapOf(
        "trap" to "hip-hop",
        "drill" to "hip-hop",
        "hip-hop" to "hip-hop",
        "rap" to "hip-hop",
        "boom bap" to "hip-hop",
        "desi hip hop" to "hip-hop",
        "r&b" to "r&b",
        "soul" to "r&b",
        "bhangra" to "punjabi",
        "punjabi pop" to "punjabi",
        "punjabi rap" to "punjabi",
        "desi pop" to "punjabi",
        "bollywood" to "bollywood",
        "filmi" to "bollywood",
        "ghazal" to "bollywood",
        "sufi" to "bollywood",
        "k-pop" to "k-pop",
        "j-pop" to "j-pop",
        "latin" to "latin",
        "reggaeton" to "latin",
        "pop" to "pop",
        "dance pop" to "pop",
        "synthpop" to "pop",
        "electropop" to "pop",
        "indie pop" to "pop",
        "electronic" to "electronic",
        "edm" to "electronic",
        "house" to "electronic",
        "techno" to "electronic",
        "rock" to "rock",
        "alt-rock" to "rock",
        "indie rock" to "rock",
        "metal" to "rock",
        "punk" to "rock",
        "acoustic" to "acoustic",
        "classical" to "classical",
        "jazz" to "jazz",
        "country" to "country"
    )

    private val ADJACENT_PARENTS = mapOf(
        "hip-hop" to setOf("r&b", "pop", "punjabi"),
        "r&b" to setOf("hip-hop", "pop"),
        "punjabi" to setOf("hip-hop", "bollywood", "pop"),
        "bollywood" to setOf("punjabi", "pop", "acoustic"),
        "pop" to setOf("dance pop", "electronic", "r&b", "rock", "indie"),
        "rock" to setOf("alt-rock", "pop", "metal"),
        "electronic" to setOf("pop", "dance pop"),
        "latin" to setOf("pop", "reggaeton", "r&b"),
        "k-pop" to setOf("pop", "electronic", "hip-hop")
    )

    fun normalizeGenre(rawGenre: String?): NormalizedGenre {
        if (rawGenre.isNullOrBlank()) {
            return NormalizedGenre(raw = "", canonicalSubGenre = "various", parentGenre = "various")
        }
        val clean = rawGenre.trim().lowercase(Locale.ROOT)
        val canonical = GENRE_SYNONYMS[clean] ?: run {
            // Check substring matching
            GENRE_SYNONYMS.entries.firstOrNull { clean.contains(it.key) }?.value ?: clean
        }
        val parent = SUBGENRE_TO_PARENT[canonical] ?: run {
            SUBGENRE_TO_PARENT.entries.firstOrNull { canonical.contains(it.key) }?.value ?: canonical
        }
        return NormalizedGenre(raw = rawGenre, canonicalSubGenre = canonical, parentGenre = parent)
    }

    fun areGenresAdjacent(genre1: String, genre2: String): Boolean {
        val p1 = normalizeGenre(genre1).parentGenre
        val p2 = normalizeGenre(genre2).parentGenre
        if (p1 == p2) return true
        val adj1 = ADJACENT_PARENTS[p1] ?: emptySet()
        val adj2 = ADJACENT_PARENTS[p2] ?: emptySet()
        return adj1.contains(p2) || adj2.contains(p1)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. LANGUAGE INFERENCE & SCRIPT DETECTION
    // ─────────────────────────────────────────────────────────────────────────

    private val artistLanguageCache = ConcurrentHashMap<String, String>()
    private val albumLanguageCache = ConcurrentHashMap<String, String>()

    private val PUNJABI_ARTISTS = setOf(
        "karan aujla", "sidhu moose wala", "sidhu moosewala", "ap dhillon", "diljit dosanjh",
        "amrit maan", "jordan sandhu", "sharry maan", "ammy virk", "gurlez akhtar",
        "korala maan", "chani nattan", "hustinder", "wazir patar", "tarsem jassar",
        "jassi gill", "kulwinder billa", "nimrat khaira", "sunanda sharma", "baani sandhu",
        "kaka", "cheema y", "sukha", "arjan dhillon", "prem dhillon", "raftaar", "bohemia",
        "deep jandu", "mankirt aulakh", "parmish verma", "b praak", "jaani", "harrdy sandhu"
    )

    private val HINDI_ARTISTS = setOf(
        "arijit singh", "shreya ghoshal", "neha kakkar", "badshah", "pritam",
        "a r rahman", "ar rahman", "atif aslam", "sonu nigam", "alka yagnik",
        "kumar sanu", "udit narayan", "jubin nautiyal", "darshan raval", "armaan malik",
        "yo yo honey singh", "honey singh", "mohit chauhan", "kk", "sunidhi chauhan",
        "vishal mishra", "sachet tandon", "parampara tandon", "anuv jain", "amit trivedi"
    )

    private val PUNJABI_ROMANIZED_KEYWORDS = setOf(
        "jatt", "pind", "tere", "mera", "meri", "gaddi", "sohneya", "yaar", "gabru",
        "karan", "aujla", "sidhu", "moosewala", "dhillon", "diljit", "dosanjh", "bhangra",
        "shinda", "sharry", "ammy", "akhtar", "korala", "maan", "sandhu", "chani", "nattan",
        "hustinder", "wazir", "patar", "tarsem", "jassi", "gill", "jordan", "billa",
        "khaira", "sunanda", "baani", "kaka", "cheema", "sukha", "arjan", "husan",
        "kudi", "munda", "kurti", "surma", "asla", "akh", "akhiyan", "gallan", "sohni",
        "billo", "jatti", "bandook", "geetan", "da", "machine", "makhaul", "four you", "on top",
        "softly", "winning speech", "white brown black", "admirin you", "52 bars", "tauba tauba"
    )

    private val HINDI_ROMANIZED_KEYWORDS = setOf(
        "tum", "hum", "dil", "pyar", "ishq", "zindagi", "mohabbat", "khuda", "tu",
        "hai", "kuch", "sanam", "arijit", "singh", "shreya", "ghoshal", "neha",
        "badshah", "pritam", "rahman", "atif", "aslam", "sonu", "nigam", "alka",
        "sanu", "narayan", "jubin", "raval", "armaan", "malik", "bollywood",
        "deewana", "dhadkan", "humsafar", "raatan", "lambiyan", "kesariya", "channa",
        "mereya", "tujhe", "kitna", "chahein", "aur", "tera", "meri", "aashiqui", "pal",
        "kahani", "suno", "mast", "magan", "shayad", "bekhayali", "lut gaye"
    )

    private val ENGLISH_WORDS = setOf(
        "the", "and", "you", "that", "was", "for", "are", "with", "his", "they",
        "this", "have", "from", "one", "had", "word", "but", "not", "what", "all",
        "were", "when", "your", "can", "said", "there", "use", "each", "which",
        "she", "how", "their", "will", "other", "about", "out", "many", "then",
        "them", "these", "some", "her", "would", "make", "like", "him", "into",
        "time", "has", "look", "two", "more", "write", "go", "see", "number",
        "no", "way", "could", "people", "my", "than", "first", "water", "been",
        "call", "who", "oil", "its", "now", "find", "long", "down", "day",
        "did", "get", "come", "made", "may", "part", "love", "night", "baby",
        "girl", "boy", "heart", "break", "dance", "song", "dont", "wanna",
        "gonna", "feeling", "world", "summer", "dream", "eyes", "rain", "sun",
        "english", "pop", "rock", "band", "artist", "live", "music", "track"
    )

    fun detectLanguage(song: Song, lyricsText: String? = null): LanguageConfidence {
        // 1. Check explicit catalog metadata if present
        if (song.genre.isNotBlank() && !song.genre.equals("various", ignoreCase = true)) {
            val lowGenre = song.genre.lowercase(Locale.ROOT)
            if (lowGenre.contains("punjabi") || lowGenre.contains("bhangra")) {
                return LanguageConfidence("Punjabi", 0.95f, "catalog")
            }
            if (lowGenre.contains("bollywood") || lowGenre.contains("hindi")) {
                return LanguageConfidence("Hindi", 0.95f, "catalog")
            }
            if (lowGenre.contains("k-pop") || lowGenre.contains("kpop") || lowGenre.contains("korean")) {
                return LanguageConfidence("Korean", 0.95f, "catalog")
            }
            if (lowGenre.contains("latin") || lowGenre.contains("reggaeton") || lowGenre.contains("spanish")) {
                return LanguageConfidence("Spanish", 0.95f, "catalog")
            }
        }

        // 2. Lyrics Script & Text Analysis
        if (!lyricsText.isNullOrBlank()) {
            val hasGurmukhi = lyricsText.any { it in '\u0A00'..'\u0A7F' }
            if (hasGurmukhi) return LanguageConfidence("Punjabi", 0.98f, "lyrics_script")

            val hasDevanagari = lyricsText.any { it in '\u0900'..'\u097F' }
            if (hasDevanagari) return LanguageConfidence("Hindi", 0.98f, "lyrics_script")

            val hasHangul = lyricsText.any { (it in '\uAC00'..'\uD7AF') || (it in '\u1100'..'\u11FF') }
            if (hasHangul) return LanguageConfidence("Korean", 0.98f, "lyrics_script")

            // Latin script lyrics: check keyword/token overlaps
            val tokens = lyricsText.lowercase(Locale.ROOT).split(Regex("[^\\p{L}]+")).filter { it.length >= 2 }
            if (tokens.isNotEmpty()) {
                val punjabiMatches = tokens.count { it in PUNJABI_ROMANIZED_KEYWORDS }
                val hindiMatches = tokens.count { it in HINDI_ROMANIZED_KEYWORDS }
                val englishMatches = tokens.count { it in ENGLISH_WORDS }

                if (punjabiMatches >= 3 && punjabiMatches > englishMatches) {
                    return LanguageConfidence("Punjabi", 0.90f, "lyrics_text")
                }
                if (hindiMatches >= 3 && hindiMatches > englishMatches) {
                    return LanguageConfidence("Hindi", 0.90f, "lyrics_text")
                }
                if (englishMatches >= 4 && englishMatches > (punjabiMatches + hindiMatches) * 2) {
                    return LanguageConfidence("English", 0.90f, "lyrics_text")
                }
            }
        }

        // 3. Title & Artist Non-Latin Script
        val fullCombined = "${song.title} ${song.artist}"
        if (fullCombined.any { it in '\u0A00'..'\u0A7F' }) {
            return LanguageConfidence("Punjabi", 0.96f, "title_script")
        }
        if (fullCombined.any { it in '\u0900'..'\u097F' }) {
            return LanguageConfidence("Hindi", 0.96f, "title_script")
        }
        if (fullCombined.any { (it in '\uAC00'..'\uD7AF') || (it in '\u1100'..'\u11FF') }) {
            return LanguageConfidence("Korean", 0.96f, "title_script")
        }

        // 4. Known Artist Mapping & Majority Vote
        val normArtist = ArtistMixEngine.normalizeArtistName(song.artist)
        if (normArtist.isNotBlank()) {
            if (PUNJABI_ARTISTS.any { normArtist.contains(it) || it.contains(normArtist) }) {
                artistLanguageCache[normArtist] = "Punjabi"
                return LanguageConfidence("Punjabi", 0.92f, "artist_known")
            }
            if (HINDI_ARTISTS.any { normArtist.contains(it) || it.contains(normArtist) }) {
                artistLanguageCache[normArtist] = "Hindi"
                return LanguageConfidence("Hindi", 0.92f, "artist_known")
            }
            val cachedArtistLang = artistLanguageCache[normArtist]
            if (cachedArtistLang != null) {
                return LanguageConfidence(cachedArtistLang, 0.85f, "artist_vote")
            }
        }

        // 5. Title Romanized Keyword Heuristics (IMPORTANT: Latin titles are NOT always English!)
        val titleTokens = song.title.lowercase(Locale.ROOT).split(Regex("[^\\p{L}]+")).filter { it.isNotBlank() }
        val pKeywords = titleTokens.count { it in PUNJABI_ROMANIZED_KEYWORDS }
        val hKeywords = titleTokens.count { it in HINDI_ROMANIZED_KEYWORDS }
        val eKeywords = titleTokens.count { it in ENGLISH_WORDS }

        if (pKeywords >= 2 || (pKeywords >= 1 && normArtist.contains("singh"))) {
            return LanguageConfidence("Punjabi", 0.82f, "title_keywords")
        }
        if (hKeywords >= 2) {
            return LanguageConfidence("Hindi", 0.82f, "title_keywords")
        }

        // English tokens dominant
        if (eKeywords >= 1 && pKeywords == 0 && hKeywords == 0) {
            return LanguageConfidence("English", 0.78f, "title_keywords")
        }

        // 6. Western catalog genres default to English if no non-English tokens found
        val normGenre = normalizeGenre(song.genre).parentGenre
        if (normGenre in setOf("pop", "rock", "country", "electronic", "r&b") && pKeywords == 0 && hKeywords == 0) {
            return LanguageConfidence("English", 0.75f, "genre_western")
        }

        // 6. Album Language Cache
        val normAlbum = song.album.lowercase(Locale.ROOT).trim()
        if (normAlbum.isNotBlank() && normAlbum != "single" && normAlbum != "album") {
            val cachedAlbumLang = albumLanguageCache[normAlbum]
            if (cachedAlbumLang != null) {
                return LanguageConfidence(cachedAlbumLang, 0.75f, "album_vote")
            }
        }

        // 7. Fallback to Unknown
        return LanguageConfidence("unknown", 0.20f, "unknown")
    }

    fun recordTrackEnrichment(song: Song, language: String) {
        if (language != "unknown") {
            val normArtist = ArtistMixEngine.normalizeArtistName(song.artist)
            if (normArtist.isNotBlank()) artistLanguageCache[normArtist] = language
            val normAlbum = song.album.lowercase(Locale.ROOT).trim()
            if (normAlbum.isNotBlank() && normAlbum != "single" && normAlbum != "album") {
                albumLanguageCache[normAlbum] = language
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. THE LISTENING-LANE (TASTE PROFILE) MODEL
    // ─────────────────────────────────────────────────────────────────────────

    fun buildListeningLane(
        seedSong: Song,
        sessionHistory: List<Song>,
        userTasteProfiles: Map<String, Float> = emptyMap()
    ): ListeningLane {
        val seedLangConf = detectLanguage(seedSong)
        val seedLang = if (seedLangConf.language != "unknown") seedLangConf.language else "English"
        val seedGenre = normalizeGenre(seedSong.genre)

        // Seed gets dominant weight (60%), history gets 40% with exponential decay
        val langWeights = mutableMapOf<String, Float>()
        langWeights[seedLang] = 0.60f

        val parentGenreWeights = mutableMapOf<String, Float>()
        parentGenreWeights[seedGenre.parentGenre] = 0.60f

        val subGenreWeights = mutableMapOf<String, Float>()
        subGenreWeights[seedGenre.canonicalSubGenre] = 0.60f

        val artists = mutableSetOf<String>()
        artists.add(ArtistMixEngine.normalizeArtistName(seedSong.artist))

        val recent = sessionHistory.takeLast(5).reversed()
        val decayWeights = floatArrayOf(0.18f, 0.10f, 0.06f, 0.04f, 0.02f)

        recent.forEachIndexed { idx, s ->
            val w = decayWeights.getOrElse(idx) { 0.01f }
            val lang = detectLanguage(s).language
            if (lang != "unknown") {
                langWeights[lang] = (langWeights[lang] ?: 0f) + w
            }
            val g = normalizeGenre(s.genre)
            parentGenreWeights[g.parentGenre] = (parentGenreWeights[g.parentGenre] ?: 0f) + w
            subGenreWeights[g.canonicalSubGenre] = (subGenreWeights[g.canonicalSubGenre] ?: 0f) + w
            artists.add(ArtistMixEngine.normalizeArtistName(s.artist))
        }

        // Normalize distributions so they sum to 1.0f
        val totalLang = langWeights.values.sum().coerceAtLeast(0.001f)
        val normalizedLang = langWeights.mapValues { it.value / totalLang }

        val totalParentGenre = parentGenreWeights.values.sum().coerceAtLeast(0.001f)
        val normalizedParentGenre = parentGenreWeights.mapValues { it.value / totalParentGenre }

        val totalSubGenre = subGenreWeights.values.sum().coerceAtLeast(0.001f)
        val normalizedSubGenre = subGenreWeights.mapValues { it.value / totalSubGenre }

        val dominantLang = normalizedLang.maxByOrNull { it.value }?.key ?: seedLang

        return ListeningLane(
            primaryLanguage = dominantLang,
            languageDistribution = normalizedLang,
            parentGenreDistribution = normalizedParentGenre,
            subGenreDistribution = normalizedSubGenre,
            artistCluster = artists,
            seedSong = seedSong,
            recentHistory = sessionHistory
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. CANDIDATE FILTERING & SCORING (THE CORE FIX)
    // ─────────────────────────────────────────────────────────────────────────

    fun scoreCandidate(
        candidate: Song,
        lane: ListeningLane,
        config: LaneRecommenderConfig = LaneRecommenderConfig()
    ): CandidateScoreBreakdown {
        val candLangConf = detectLanguage(candidate)
        var candLang = candLangConf.language
        val candGenre = normalizeGenre(candidate.genre)
        val candArtistNorm = ArtistMixEngine.normalizeArtistName(candidate.artist)

        // Resolve unknown language if possible via artist cache
        if (candLang == "unknown") {
            val resolved = artistLanguageCache[candArtistNorm]
            if (resolved != null) {
                candLang = resolved
            }
        }

        // A) Hard Language Gate
        // Primary languages are those with >= 25% share in the lane, plus the seed song's language always.
        val primaryLangs = lane.languageDistribution.filter { it.value >= 0.25f }.keys + lane.seedSong.let { detectLanguage(it).language }
        val passesGate = when {
            !config.stickToSameLanguage -> true
            candLang == "unknown" -> {
                // If unknown, allow ONLY if artist matches lane strongly, but with severe penalty, never as exploration
                candArtistNorm in lane.artistCluster
            }
            candLang in primaryLangs -> true
            else -> false
        }

        // Language Match Score (0..1)
        val langShare = lane.languageDistribution[candLang] ?: 0f
        val languageScore = when {
            candLang == lane.primaryLanguage -> 1.0f
            candLang in primaryLangs -> 0.70f * (langShare / 0.25f).coerceIn(0f, 1f)
            candLang == "unknown" && passesGate -> 0.30f
            else -> 0.0f
        }

        // B) Genre Affinity Score (0..1)
        val sameSubGenre = lane.subGenreDistribution.containsKey(candGenre.canonicalSubGenre)
        val sameParentGenre = lane.parentGenreDistribution.containsKey(candGenre.parentGenre)
        val isAdjacent = areGenresAdjacent(lane.seedSong.genre, candGenre.canonicalSubGenre)

        val genreScore = when {
            sameSubGenre -> 1.0f
            sameParentGenre -> 0.65f
            isAdjacent -> 0.35f
            else -> 0.05f
        }

        // C) Artist Affinity Score (0..1)
        val isSameArtist = ArtistMixEngine.isArtistMatch(candidate, lane.seedSong.artist)
        val inCluster = candArtistNorm in lane.artistCluster
        val artistScore = when {
            isSameArtist -> 1.0f
            inCluster -> 0.80f
            candLang == lane.primaryLanguage && sameParentGenre -> 0.45f
            else -> 0.10f
        }

        // D) Content Similarity (tempo / mood / title token heuristics)
        val contentScore = if (sameParentGenre && candLang == lane.primaryLanguage) 0.60f else 0.20f

        // E) Popularity (Weakest term: weight 0.05, cannot override gate)
        val popularityScore = 0.50f // Baseline normalized popularity

        // Total weighted score
        val totalScore = if (!passesGate) 0f else {
            (config.languageMatchWeight * languageScore) +
            (config.genreSimilarityWeight * genreScore) +
            (config.artistAffinityWeight * artistScore) +
            (config.contentSimilarityWeight * contentScore) +
            (config.popularityWeight * popularityScore)
        }

        // Reason line explainability
        val reasonLine = when {
            isSameArtist -> "Same artist · $candLang · ${candGenre.parentGenre.replaceFirstChar { it.uppercaseChar() }}"
            inCluster -> "Similar artist · $candLang · ${candGenre.parentGenre.replaceFirstChar { it.uppercaseChar() }}"
            sameSubGenre -> "Lane match · $candLang · ${candGenre.canonicalSubGenre.replaceFirstChar { it.uppercaseChar() }}"
            sameParentGenre -> "Genre match · $candLang · ${candGenre.parentGenre.replaceFirstChar { it.uppercaseChar() }}"
            else -> "Suggested for you · $candLang"
        }

        return CandidateScoreBreakdown(
            song = candidate,
            passedLanguageGate = passesGate,
            language = candLang,
            languageConfidence = candLangConf.confidence,
            parentGenre = candGenre.parentGenre,
            subGenre = candGenre.canonicalSubGenre,
            languageScore = languageScore,
            genreScore = genreScore,
            artistAffinityScore = artistScore,
            contentScore = contentScore,
            popularityScore = popularityScore,
            totalScore = totalScore,
            reasonLine = reasonLine,
            isExploration = !inCluster && !isSameArtist
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. POOL SIZE FALLBACKS & AUTOPLAY PICKER
    // ─────────────────────────────────────────────────────────────────────────

    fun selectAutoplayQueue(
        lane: ListeningLane,
        candidatePool: List<Song>,
        requestedCount: Int = 10,
        config: LaneRecommenderConfig = LaneRecommenderConfig()
    ): Pair<List<Song>, FilterStats> {
        val initialCount = candidatePool.size
        var droppedByLang = 0
        var droppedByGenre = 0
        var droppedByArtistCap = 0
        var wideningStep = 1

        val scored = candidatePool.map { scoreCandidate(it, lane, config) }

        // Filter: Passed language gate
        val gated = scored.filter { 
            if (it.passedLanguageGate) true else { droppedByLang++; false }
        }

        // Filter: Meets genre similarity threshold
        var pool = gated.filter {
            if (it.genreScore >= config.genreThreshold) true else { droppedByGenre++; false }
        }

        // Widening Steps if pool size is less than targetPoolSize
        if (pool.size < config.targetPoolSize) {
            wideningStep = 2
            // Step 2: Same language + adjacent genres
            pool = gated.filter { it.genreScore >= 0.30f }
        }
        if (pool.size < config.targetPoolSize) {
            wideningStep = 3
            // Step 3: Same language + any genre ordered by lane similarity
            pool = gated.filter { it.language == lane.primaryLanguage }
        }
        if (pool.size < config.targetPoolSize) {
            wideningStep = 4
            // Step 4: Secondary language with >= 10% share
            val secondaryLangs = lane.languageDistribution.filter { it.value >= 0.10f }.keys
            pool = scored.filter { it.language in secondaryLangs }
        }
        if (pool.size < config.targetPoolSize) {
            wideningStep = 5
            // Step 5: Full library fallback only as last resort with lowest priority
            pool = scored
        }

        // Sort descending by score
        val sorted = pool.sortedByDescending { it.totalScore }

        // Controlled exploration: mix in 20-25% exploration tracks within the same lane
        val finalPicks = mutableListOf<Song>()
        val remainingCandidates = sorted.toMutableList()
        var consecutiveArtist = ""
        var consecutiveCount = 0

        while (finalPicks.size < requestedCount && remainingCandidates.isNotEmpty()) {
            val candidateIndex = remainingCandidates.indexOfFirst { cand ->
                val candArtist = ArtistMixEngine.normalizeArtistName(cand.song.artist)
                if (candArtist == consecutiveArtist && consecutiveCount >= config.maxConsecutivePerArtist) {
                    false
                } else true
            }

            val chosen = if (candidateIndex >= 0) {
                remainingCandidates.removeAt(candidateIndex)
            } else {
                // If every remaining candidate is by the same artist, take the top one to guarantee target pool size
                droppedByArtistCap++
                remainingCandidates.removeAt(0)
            }

            val chosenArtist = ArtistMixEngine.normalizeArtistName(chosen.song.artist)
            if (chosenArtist == consecutiveArtist) {
                consecutiveCount++
            } else {
                consecutiveArtist = chosenArtist
                consecutiveCount = 1
            }
            finalPicks.add(chosen.song)
        }

        val stats = FilterStats(
            initialCandidateCount = initialCount,
            droppedByLanguageGate = droppedByLang,
            droppedByGenreThreshold = droppedByGenre,
            droppedByArtistCap = droppedByArtistCap,
            poolWideningStepUsed = wideningStep,
            finalPickCount = finalPicks.size
        )

        return Pair(finalPicks, stats)
    }
}
