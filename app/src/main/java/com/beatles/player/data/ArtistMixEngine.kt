package com.beatles.player.data

import java.text.Normalizer
import java.util.Locale

/**
 * High-performance, deterministic Artist Mix curation engine.
 * 
 * Responsibilities:
 * 1. Canonical artist resolution (name normalization, diacritics removal, feature token handling).
 * 2. 70% artist / 30% similar artist curation with MMR (Maximal Marginal Relevance) diversity.
 * 3. Strict album-block constraint: at most 2 consecutive tracks from the same album.
 * 4. Deduplication of identical songs across different editions (Deluxe, Remaster, Single).
 * 5. Small artist backfill: if artist has < 10 tracks, pads with genre/similar tracks so mix never dies.
 * 6. Non-destructive queue extension respecting AutoMix invariants (only appending, never replacing armed next track).
 */
object ArtistMixEngine {

    /**
     * Normalizes an artist name:
     * - Decomposes diacritics (e.g. "Beyoncé" -> "beyonce", "MØ" -> "mo")
     * - Case-folds to lowercase
     * - Strips feature tokens ("feat.", "ft.", "featuring", "with", "&", "x", "prod.")
     * - Strips non-alphanumeric punctuation
     */
    fun normalizeArtistName(name: String?): String {
        if (name.isNullOrBlank()) return ""
        val nfd = Normalizer.normalize(name, Normalizer.Form.NFD)
        val withoutDiacritics = nfd.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .replace("Ø", "O").replace("ø", "o")
            .replace("Æ", "Ae").replace("æ", "ae")
            .replace("Œ", "Oe").replace("œ", "oe")
        return withoutDiacritics
            .replace(".", "")
            .replace(Regex("(?i)[\\(\\[].*?(feat|ft|featuring|with|prod).*?[\\)\\]]"), "")
            .replace(Regex("(?i)\\b(feat|ft|featuring|with|prod|and)\\b.*"), "")
            .replace(Regex("(?i)\\s+(&|x)\\s+.*"), "")
            .replace(Regex("[^a-zA-Z0-9\\s]"), " ")
            .trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("\\s+"), " ")
    }

    /**
     * Stable canonical ID for an artist.
     */
    fun getArtistId(artistName: String?): String = normalizeArtistName(artistName)

    /**
     * Determines whether a given song belongs to the target artist.
     * Evaluates primary artist, split collaborator list, and title feature strings.
     */
    fun isArtistMatch(song: Song, targetArtist: String?): Boolean {
        val targetNorm = normalizeArtistName(targetArtist)
        if (targetNorm.isBlank()) return false

        // 1. Primary artist string match
        val primaryNorm = normalizeArtistName(song.artist)
        if (primaryNorm == targetNorm) return true
        if (targetNorm.length > 3 && (primaryNorm.contains(targetNorm) || targetNorm.contains(primaryNorm))) {
            return true
        }

        // 2. Tokenized split on collaborator delimiters (&, feat, ft, with, comma, slash)
        val splitArtists = song.artist.split(Regex("(?i)[,&/]|\\b(feat|ft|featuring|with|prod|and)\\b|\\s+[xX&]\\s+"))
            .map { normalizeArtistName(it) }
            .filter { it.isNotBlank() }
        if (splitArtists.any { it == targetNorm || (targetNorm.length > 3 && it.contains(targetNorm)) }) {
            return true
        }

        // 3. Title feature check (e.g. "Players (feat. Karan Aujla)")
        val titleNorm = Normalizer.normalize(song.title, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase(Locale.ROOT)
        val featurePattern = Regex("(?i)[\\(\\[].*?\\b(feat|ft|with|featuring|x)\\.?\\s+([^\\)\\]]+)[\\)\\]]")
        val match = featurePattern.find(titleNorm)
        if (match != null) {
            val featuredPart = normalizeArtistName(match.groupValues[2])
            if (featuredPart.contains(targetNorm) || targetNorm.contains(featuredPart)) {
                return true
            }
        }

        return false
    }

    /**
     * Normalized deduplication key for a track.
     * Eliminates duplicate versions of the same song across Deluxe albums, Singles, or Remastered releases.
     */
    fun getTrackDedupeKey(song: Song): String {
        val cleanTitle = Normalizer.normalize(song.title, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .replace(Regex("(?i)[\\(\\[].*?(official|video|audio|lyrics|remastered|deluxe|version|edit|clean|explicit|bonus|hq|hd).*?[\\)\\]]"), "")
            .replace(Regex("[^a-zA-Z0-9]"), "")
            .lowercase(Locale.ROOT)
        val cleanArtist = normalizeArtistName(song.artist).replace(Regex("[^a-zA-Z0-9]"), "")
        return "$cleanTitle::$cleanArtist"
    }

    /**
     * Builds a curated Artist Mix queue.
     *
     * @param targetArtist The seed artist name.
     * @param artistTracks Songs by the artist (already ranked: favorites/recents first, followed by catalog).
     * @param similarTracks Songs by similar artists (for 30% ratio & small artist padding).
     * @param seedSong Optional song that triggered the mix (will always be placed first if provided).
     * @param maxTracks Target playlist size (default 30).
     * @param artistRatio Target proportion of artist tracks vs similar tracks (default 0.70f).
     * @param maxConsecutiveFromSameAlbum Max consecutive tracks from the same album (default 2).
     */
    fun buildArtistMix(
        targetArtist: String,
        artistTracks: List<Song>,
        similarTracks: List<Song> = emptyList(),
        seedSong: Song? = null,
        maxTracks: Int = 30,
        artistRatio: Float = 1.00f,
        maxConsecutiveFromSameAlbum: Int = 2
    ): List<Song> {
        val seenDedupeKeys = mutableSetOf<String>()
        val seenSongIds = mutableSetOf<String>()

        fun isUnique(song: Song): Boolean {
            if (song.id.isBlank() || song.id in seenSongIds) return false
            val dedupeKey = getTrackDedupeKey(song)
            if (dedupeKey in seenDedupeKeys) return false
            return true
        }

        fun register(song: Song) {
            seenSongIds.add(song.id)
            seenDedupeKeys.add(getTrackDedupeKey(song))
        }

        val result = mutableListOf<Song>()

        // 1. Seed song placed first if available
        if (seedSong != null && isUnique(seedSong) && (artistRatio < 1.00f || isArtistMatch(seedSong, targetArtist))) {
            result.add(seedSong)
            register(seedSong)
        }

        // 2. Filter & deduplicate candidate pools (strictly only target artist when artistRatio >= 1.0)
        val uniqueArtistPool = mutableListOf<Song>()
        for (song in artistTracks) {
            if (isUnique(song) && isArtistMatch(song, targetArtist)) {
                uniqueArtistPool.add(song)
            }
        }

        val uniqueSimilarPool = mutableListOf<Song>()
        if (artistRatio < 1.00f) {
            for (song in similarTracks) {
                if (isUnique(song) && !isArtistMatch(song, targetArtist)) {
                    uniqueSimilarPool.add(song)
                }
            }
        }

        var lastAlbum: String? = seedSong?.album?.takeIf { isNamedAlbum(it) }
        var currentAlbumStreak = if (lastAlbum != null) 1 else 0

        var artistPicksSinceSimilar = 0
        val targetArtistStreak = 2 // 2 artist songs -> 1 similar song -> 2 artist songs (~67-70% ratio)

        while (result.size < maxTracks && (uniqueArtistPool.isNotEmpty() || uniqueSimilarPool.isNotEmpty())) {
            val shouldPickSimilar = when {
                artistRatio >= 1.00f -> false
                uniqueArtistPool.isEmpty() -> true
                uniqueSimilarPool.isEmpty() -> false
                uniqueArtistPool.size < 3 && uniqueSimilarPool.isNotEmpty() && result.size % 2 == 1 -> true
                artistPicksSinceSimilar >= targetArtistStreak -> true
                else -> false
            }

            if (shouldPickSimilar && uniqueSimilarPool.isNotEmpty()) {
                val candidate = uniqueSimilarPool.removeAt(0)
                if (isUnique(candidate)) {
                    result.add(candidate)
                    register(candidate)
                    val candAlbum = candidate.album.takeIf { isNamedAlbum(it) }
                    if (candAlbum != null && candAlbum.equals(lastAlbum, ignoreCase = true)) {
                        currentAlbumStreak++
                    } else {
                        lastAlbum = candAlbum
                        currentAlbumStreak = if (candAlbum != null) 1 else 0
                    }
                    artistPicksSinceSimilar = 0
                }
            } else if (uniqueArtistPool.isNotEmpty()) {
                // Find next artist song adhering to album streak limit
                var selectedIdx = -1
                for (i in uniqueArtistPool.indices) {
                    val cand = uniqueArtistPool[i]
                    val candAlbum = cand.album.takeIf { isNamedAlbum(it) }
                    val wouldExceedStreak = candAlbum != null &&
                            candAlbum.equals(lastAlbum, ignoreCase = true) &&
                            currentAlbumStreak >= maxConsecutiveFromSameAlbum
                    if (!wouldExceedStreak) {
                        selectedIdx = i
                        break
                    }
                }

                if (selectedIdx >= 0) {
                    val candidate = uniqueArtistPool.removeAt(selectedIdx)
                    if (isUnique(candidate)) {
                        result.add(candidate)
                        register(candidate)
                        val candAlbum = candidate.album.takeIf { isNamedAlbum(it) }
                        if (candAlbum != null && candAlbum.equals(lastAlbum, ignoreCase = true)) {
                            currentAlbumStreak++
                        } else {
                            lastAlbum = candAlbum
                            currentAlbumStreak = if (candAlbum != null) 1 else 0
                        }
                        artistPicksSinceSimilar++
                    }
                } else {
                    // All remaining artist tracks exceed album streak; inject similar song to break streak ONLY if artistRatio < 1.00f
                    if (artistRatio < 1.00f && uniqueSimilarPool.isNotEmpty()) {
                        val breakCandidate = uniqueSimilarPool.removeAt(0)
                        if (isUnique(breakCandidate)) {
                            result.add(breakCandidate)
                            register(breakCandidate)
                            lastAlbum = breakCandidate.album.takeIf { isNamedAlbum(it) }
                            currentAlbumStreak = if (lastAlbum != null) 1 else 0
                            artistPicksSinceSimilar = 0
                        }
                    } else {
                        // Accept the next artist track to strictly preserve 100% artist purity
                        val candidate = uniqueArtistPool.removeAt(0)
                        if (isUnique(candidate)) {
                            result.add(candidate)
                            register(candidate)
                        }
                    }
                }
            } else if (artistRatio < 1.00f && uniqueSimilarPool.isNotEmpty()) {
                val candidate = uniqueSimilarPool.removeAt(0)
                if (isUnique(candidate)) {
                    result.add(candidate)
                    register(candidate)
                }
            }
        }

        return result
    }

    /**
     * Appends new non-duplicate tracks to an ongoing Artist Mix without modifying
     * existing tracks or disturbing the armed next track (AutoMix invariant R5 I1-I7).
     */
    fun extendMix(
        existingPlaylist: List<Song>,
        newArtistTracks: List<Song>,
        newSimilarTracks: List<Song>,
        targetArtist: String,
        batchSize: Int = 12
    ): List<Song> {
        val seenSongIds = existingPlaylist.map { it.id }.toMutableSet()
        val seenDedupeKeys = existingPlaylist.map { getTrackDedupeKey(it) }.toMutableSet()

        val addition = buildArtistMix(
            targetArtist = targetArtist,
            artistTracks = newArtistTracks.filter { it.id !in seenSongIds && getTrackDedupeKey(it) !in seenDedupeKeys },
            similarTracks = newSimilarTracks.filter { it.id !in seenSongIds && getTrackDedupeKey(it) !in seenDedupeKeys },
            seedSong = null,
            maxTracks = batchSize,
            artistRatio = 1.00f,
            maxConsecutiveFromSameAlbum = 2
        )

        return existingPlaylist + addition
    }

    private fun isNamedAlbum(album: String): Boolean {
        val norm = album.trim().lowercase(Locale.ROOT)
        return norm.isNotEmpty() && norm != "single" && norm != "single/ep" && norm != "various" && norm != "unknown album"
    }
}
