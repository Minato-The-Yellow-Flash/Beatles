package com.beatles.player.data

/**
 * Data structures for Listening Lane (Taste Profile) and Metadata Enrichment.
 */
data class LanguageConfidence(
    val language: String,
    val confidence: Float, // 0.0f .. 1.0f
    val source: String // "catalog", "lyrics_script", "lyrics_text", "title_script", "title_keywords", "artist_vote", "album_vote", "unknown"
)

data class NormalizedGenre(
    val raw: String,
    val canonicalSubGenre: String,
    val parentGenre: String
)

data class CandidateScoreBreakdown(
    val song: Song,
    val passedLanguageGate: Boolean,
    val language: String,
    val languageConfidence: Float,
    val parentGenre: String,
    val subGenre: String,
    val languageScore: Float,
    val genreScore: Float,
    val artistAffinityScore: Float,
    val contentScore: Float,
    val popularityScore: Float,
    val totalScore: Float,
    val reasonLine: String,
    val isExploration: Boolean
)

data class ListeningLane(
    val primaryLanguage: String,
    val languageDistribution: Map<String, Float>,
    val parentGenreDistribution: Map<String, Float>,
    val subGenreDistribution: Map<String, Float>,
    val artistCluster: Set<String>,
    val seedSong: Song,
    val recentHistory: List<Song>
)

data class FilterStats(
    val initialCandidateCount: Int,
    val droppedByLanguageGate: Int,
    val droppedByGenreThreshold: Int,
    val droppedByArtistCap: Int,
    val poolWideningStepUsed: Int,
    val finalPickCount: Int
)

data class LaneRecommenderConfig(
    val languageMatchWeight: Float = 0.35f,
    val genreSimilarityWeight: Float = 0.30f,
    val artistAffinityWeight: Float = 0.20f,
    val contentSimilarityWeight: Float = 0.10f,
    val popularityWeight: Float = 0.05f,
    val genreThreshold: Float = 0.25f,
    val targetPoolSize: Int = 30,
    val explorationRate: Float = 0.20f,
    val maxConsecutivePerArtist: Int = 2,
    val stickToSameLanguage: Boolean = true,
    val allowMixedLanguages: Boolean = false
)
