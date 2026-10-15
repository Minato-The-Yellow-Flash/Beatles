package com.beatles.player.data

import org.junit.Assert.*
import org.junit.Test

class ArtistMixEngineTest {

    @Test
    fun testNormalizeArtistName_handlesDiacriticsPunctuationAndFeatures() {
        assertEquals("karan aujla", ArtistMixEngine.normalizeArtistName("Karan Aujla"))
        assertEquals("karan aujla", ArtistMixEngine.normalizeArtistName("Karan Aujla feat. Ikky"))
        assertEquals("karan aujla", ArtistMixEngine.normalizeArtistName("Karan Aujla ft. DIVINE"))
        assertEquals("karan aujla", ArtistMixEngine.normalizeArtistName("Karan Aujla & Proof"))
        assertEquals("karan aujla", ArtistMixEngine.normalizeArtistName("Karan Aujla (with YG)"))
        assertEquals("ar rahman", ArtistMixEngine.normalizeArtistName("A.R. Rahman"))
        assertEquals("beyonce", ArtistMixEngine.normalizeArtistName("Beyoncé"))
        assertEquals("mo", ArtistMixEngine.normalizeArtistName("MØ"))
        assertEquals("jay z", ArtistMixEngine.normalizeArtistName("Jay-Z"))
    }

    @Test
    fun testIsArtistMatch_detectsPrimaryCollabAndTitleFeatures() {
        val s1 = Song(id = "1", title = "52 Bars", artist = "Karan Aujla", album = "Four You", coverUrl = "", durationSeconds = 200)
        assertTrue(ArtistMixEngine.isArtistMatch(s1, "Karan Aujla"))
        assertTrue(ArtistMixEngine.isArtistMatch(s1, "karan aujla"))
        assertFalse(ArtistMixEngine.isArtistMatch(s1, "Arijit Singh"))

        // Collaborations in artist field
        val s2 = Song(id = "2", title = "White Brown Black", artist = "Avvy Sra & Karan Aujla", album = "Single", coverUrl = "", durationSeconds = 180)
        assertTrue(ArtistMixEngine.isArtistMatch(s2, "Karan Aujla"))
        assertTrue(ArtistMixEngine.isArtistMatch(s2, "Avvy Sra"))

        val s3 = Song(id = "3", title = "Softly", artist = "Karan Aujla feat. Ikky", album = "Making Memories", coverUrl = "", durationSeconds = 150)
        assertTrue(ArtistMixEngine.isArtistMatch(s3, "Karan Aujla"))
        assertTrue(ArtistMixEngine.isArtistMatch(s3, "Ikky"))

        // Feature in song title
        val s4 = Song(id = "4", title = "God Damn (feat. Karan Aujla)", artist = "Badshah", album = "Ek Tha Raja", coverUrl = "", durationSeconds = 170)
        assertTrue(ArtistMixEngine.isArtistMatch(s4, "Karan Aujla"))
        assertTrue(ArtistMixEngine.isArtistMatch(s4, "Badshah"))
    }

    @Test
    fun testTrackDeduplication_removesDuplicateEditions() {
        val s1 = Song(id = "1", title = "Softly", artist = "Karan Aujla", album = "Making Memories", coverUrl = "", durationSeconds = 150)
        val s2 = Song(id = "2", title = "Softly (Official Music Video)", artist = "Karan Aujla", album = "Single", coverUrl = "", durationSeconds = 150)
        val s3 = Song(id = "3", title = "Softly [Deluxe Edition]", artist = "Karan Aujla", album = "Making Memories Deluxe", coverUrl = "", durationSeconds = 150)

        val key1 = ArtistMixEngine.getTrackDedupeKey(s1)
        val key2 = ArtistMixEngine.getTrackDedupeKey(s2)
        val key3 = ArtistMixEngine.getTrackDedupeKey(s3)

        assertEquals(key1, key2)
        assertEquals(key1, key3)
    }

    @Test
    fun testBuildArtistMix_albumBlockCappedAtTwo() {
        val artistSongs = listOf(
            Song("1", "Song A1", "Artist X", "Album One", "", 100),
            Song("2", "Song A2", "Artist X", "Album One", "", 100),
            Song("3", "Song A3", "Artist X", "Album One", "", 100),
            Song("4", "Song A4", "Artist X", "Album One", "", 100),
            Song("5", "Song B1", "Artist X", "Album Two", "", 100),
            Song("6", "Song B2", "Artist X", "Album Two", "", 100),
            Song("7", "Song B3", "Artist X", "Album Two", "", 100)
        )
        val similarSongs = listOf(
            Song("s1", "Sim 1", "Similar Y", "Sim Album 1", "", 100),
            Song("s2", "Sim 2", "Similar Y", "Sim Album 2", "", 100)
        )

        val mix = ArtistMixEngine.buildArtistMix(
            targetArtist = "Artist X",
            artistTracks = artistSongs,
            similarTracks = similarSongs,
            maxTracks = 8,
            maxConsecutiveFromSameAlbum = 2
        )

        // Check that no album has more than 2 consecutive tracks
        var streak = 0
        var prevAlbum: String? = null
        for (song in mix) {
            if (song.album.isNotEmpty()) {
                if (song.album == prevAlbum) {
                    streak++
                    assertTrue("Album streak exceeded 2: $prevAlbum", streak <= 2)
                } else {
                    prevAlbum = song.album
                    streak = 1
                }
            }
        }
    }

    @Test
    fun testBuildArtistMix_onlyTargetSingerSongsIncluded() {
        // Artist has only 2 songs
        val artistSongs = listOf(
            Song("1", "Solo 1", "Micro Artist", "Alb 1", "", 100),
            Song("2", "Solo 2", "Micro Artist", "Alb 2", "", 100)
        )
        val similarSongs = (1..15).map {
            Song("sim_$it", "Similar Track $it", "Other Artist", "Other Alb $it", "", 100)
        }

        val mix = ArtistMixEngine.buildArtistMix(
            targetArtist = "Micro Artist",
            artistTracks = artistSongs,
            similarTracks = similarSongs,
            maxTracks = 10
        )

        // Strict target singer requirement: all tracks must belong to target artist
        assertTrue(mix.isNotEmpty())
        assertTrue(mix.all { ArtistMixEngine.isArtistMatch(it, "Micro Artist") })
        assertFalse(mix.any { it.artist == "Other Artist" })
    }

    @Test
    fun testBuildArtistMix_emptyCaseHandling() {
        val mix = ArtistMixEngine.buildArtistMix(
            targetArtist = "Ghost Artist",
            artistTracks = emptyList(),
            similarTracks = emptyList()
        )
        assertTrue(mix.isEmpty())
    }

    @Test
    fun testExtendMix_strictAppendAndNoDuplicates() {
        val currentPlaylist = listOf(
            Song("1", "Track 1", "Artist X", "Alb 1", "", 100),
            Song("2", "Track 2", "Artist X", "Alb 2", "", 100),
            Song("3", "Track 3", "Artist X", "Alb 3", "", 100)
        )

        val newArtistPool = listOf(
            Song("1", "Track 1 (Duplicate)", "Artist X", "Alb 1", "", 100), // Should be skipped
            Song("4", "Track 4", "Artist X", "Alb 4", "", 100),
            Song("5", "Track 5", "Artist X", "Alb 5", "", 100)
        )

        val newSimilarPool = listOf(
            Song("s1", "Sim 1", "Artist Y", "Alb Y", "", 100)
        )

        val extended = ArtistMixEngine.extendMix(
            existingPlaylist = currentPlaylist,
            newArtistTracks = newArtistPool,
            newSimilarTracks = newSimilarPool,
            targetArtist = "Artist X",
            batchSize = 3
        )

        // Verifies strict append: the original 3 elements remain intact at indices 0, 1, 2
        assertEquals("1", extended[0].id)
        assertEquals("2", extended[1].id)
        assertEquals("3", extended[2].id)
        // And size grew by target artist tracks (excluding other artists)
        assertEquals(5, extended.size)
        // No duplicate ID "1"
        assertEquals(1, extended.count { it.id == "1" })
        // Strictly only target artist
        assertTrue(extended.all { ArtistMixEngine.isArtistMatch(it, "Artist X") })
    }

    @Test
    fun testExtendMix_preservesAutoMixArmedCandidate() {
        val currentSong = Song("1", "Playing Track", "Artist X", "Alb 1", "", 180)
        val armedNextSong = Song("2", "Armed Next Track", "Artist X", "Alb 1", "", 200)
        val queueTail = Song("3", "Queue Tail", "Artist X", "Alb 2", "", 190)

        val currentPlaylist = listOf(currentSong, armedNextSong, queueTail)

        val newArtistPool = (4..10).map {
            Song("$it", "New Track $it", "Artist X", "Alb $it", "", 180)
        }
        val newSimilarPool = (1..5).map {
            Song("sim_$it", "Similar Track $it", "Artist Y", "Alb Y", "", 180)
        }

        val extended = ArtistMixEngine.extendMix(
            existingPlaylist = currentPlaylist,
            newArtistTracks = newArtistPool,
            newSimilarTracks = newSimilarPool,
            targetArtist = "Artist X",
            batchSize = 6
        )

        // Index 0 must still be currentSong
        assertEquals("1", extended[0].id)
        // AutoMix invariant R5 I1-I7: index 1 must still be armedNextSong untouched!
        assertEquals("2", extended[1].id)
        // Index 2 must still be queueTail
        assertEquals("3", extended[2].id)
        // New tracks are appended strictly from index 3 onwards
        assertTrue(extended.size >= 6)
        assertEquals("4", extended[3].id)
    }
}
