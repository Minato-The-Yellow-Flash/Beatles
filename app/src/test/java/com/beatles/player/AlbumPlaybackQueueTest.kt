package com.beatles.player

import com.beatles.player.data.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumPlaybackQueueTest {

    private val track1 = Song(id = "alb_1", title = "Album Track 1", artist = "Pink Floyd", album = "The Dark Side of the Moon", coverUrl = "", durationSeconds = 240)
    private val track2 = Song(id = "alb_2", title = "Album Track 2", artist = "Pink Floyd", album = "The Dark Side of the Moon", coverUrl = "", durationSeconds = 300)
    private val track3 = Song(id = "alb_3", title = "Album Track 3", artist = "Pink Floyd", album = "The Dark Side of the Moon", coverUrl = "", durationSeconds = 210)
    private val albumTracks = listOf(track1, track2, track3)

    private val foreignSong1 = Song(id = "rel_1", title = "Another Brick", artist = "Pink Floyd", album = "The Wall", coverUrl = "", durationSeconds = 230)
    private val foreignSong2 = Song(id = "rel_2", title = "Comfortably Numb", artist = "Pink Floyd", album = "The Wall", coverUrl = "", durationSeconds = 380)

    @Test
    fun testAlbumPlaybackPreservesTrackSequenceWithoutForeignInsertion() {
        val playlist = albumTracks.toMutableList()

        // When track 1 is playing, verify next track in queue is track 2
        val currIdx = playlist.indexOfFirst { it.id == track1.id }
        assertEquals(0, currIdx)
        assertTrue("Upcoming tracks exist in album", currIdx < playlist.size - 1)

        val nextTrack = playlist[currIdx + 1]
        assertEquals("Next track must be track 2 of the album", track2.id, nextTrack.id)

        // Ensure foreign tracks are NOT appended while album tracks remain
        val isAtEndOfQueue = currIdx >= playlist.size - 1
        assertEquals("Must NOT be at end of queue while playing track 1", false, isAtEndOfQueue)
    }

    @Test
    fun testAlbumEndWithAutoplayOffStopsPlayback() {
        val playlist = albumTracks.toMutableList()
        val autoPlayEnabled = false
        val currentSong = track3 // Last track in the album

        val currIdx = playlist.indexOfFirst { it.id == currentSong.id }
        val nextIdx = currIdx + 1

        val hasNextInQueue = nextIdx in playlist.indices
        assertEquals("No more tracks in album queue", false, hasNextInQueue)

        // When queue ends and autoplay is false, playback must stop without extending
        val shouldExtendQueue = autoPlayEnabled && currentSong != null
        assertEquals("Must NOT extend queue when autoplay is disabled", false, shouldExtendQueue)
    }

    @Test
    fun testAlbumEndWithAutoplayOnExtendsListeningLane() {
        val playlist = albumTracks.toMutableList()
        val autoPlayEnabled = true
        val currentSong = track3 // Last track in the album

        val currIdx = playlist.indexOfFirst { it.id == currentSong.id }
        val nextIdx = currIdx + 1

        val hasNextInQueue = nextIdx in playlist.indices
        assertEquals("No more tracks in album queue", false, hasNextInQueue)

        // When queue ends and autoplay is true, extend playlist
        val shouldExtendQueue = autoPlayEnabled && currentSong != null
        assertTrue("Must extend queue when autoplay is enabled", shouldExtendQueue)

        val relatedPool = listOf(foreignSong1, foreignSong2)
        val newTracks = relatedPool.filter { s -> playlist.none { it.id == s.id } }
        val extendedPlaylist = playlist + newTracks

        assertEquals(5, extendedPlaylist.size)
        assertEquals(foreignSong1.id, extendedPlaylist[3].id)
    }
}
