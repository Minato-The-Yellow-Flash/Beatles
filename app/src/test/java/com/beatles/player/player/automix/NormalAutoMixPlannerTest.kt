package com.beatles.player.player.automix

import com.beatles.player.data.LyricLine
import com.beatles.player.data.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NormalAutoMixPlannerTest {

    @Test
    fun testCamelotKeyParsing() {
        val am = CamelotKey.from("A", "MINOR")
        assertEquals(8, am.number)
        assertEquals('A', am.letter)

        val cMaj = CamelotKey.from("C", "MAJOR")
        assertEquals(8, cMaj.number)
        assertEquals('B', cMaj.letter)

        val gSharpMinor = CamelotKey.from("G#m", "MINOR")
        assertEquals(1, gSharpMinor.number)
        assertEquals('A', gSharpMinor.letter)

        val fSharpMinor = CamelotKey.from("F#", "MINOR")
        assertEquals(11, fSharpMinor.number)
        assertEquals('A', fSharpMinor.letter)

        val eMaj = CamelotKey.from("E", "MAJOR")
        assertEquals(12, eMaj.number)
        assertEquals('B', eMaj.letter)
    }

    @Test
    fun testCamelotHarmonicCompatibilityMatrix() {
        val key8A = CamelotKey(8, 'A') // Am
        val key8B = CamelotKey(8, 'B') // C
        val key7A = CamelotKey(7, 'A') // Dm
        val key9A = CamelotKey(9, 'A') // Em
        val key10A = CamelotKey(10, 'A') // Bm
        val key1A = CamelotKey(1, 'A')  // Abm
        val key12A = CamelotKey(12, 'A') // Dbm

        // 1. Same key
        assertTrue("Same key must be harmonically compatible", CamelotKey.areHarmonicallyCompatible(key8A, key8A))

        // 2. Relative Major/Minor
        assertTrue("Relative major/minor (8A -> 8B) must be compatible", CamelotKey.areHarmonicallyCompatible(key8A, key8B))

        // 3. Adjacent steps (+1 / -1)
        assertTrue("Adjacent step -1 (8A -> 7A) must be compatible", CamelotKey.areHarmonicallyCompatible(key8A, key7A))
        assertTrue("Adjacent step +1 (8A -> 9A) must be compatible", CamelotKey.areHarmonicallyCompatible(key8A, key9A))

        // 4. Wrap-around (1A <-> 12A)
        assertTrue("Wrap-around 1A -> 12A must be compatible", CamelotKey.areHarmonicallyCompatible(key1A, key12A))
        assertTrue("Wrap-around 12A -> 1A must be compatible", CamelotKey.areHarmonicallyCompatible(key12A, key1A))

        // 5. Incompatible keys
        assertFalse("Jump of 2 steps (8A -> 10A) is dissonant", CamelotKey.areHarmonicallyCompatible(key8A, key10A))
        assertFalse("Cross-letter distant key (8A -> 1B) is dissonant", CamelotKey.areHarmonicallyCompatible(key8A, CamelotKey(1, 'B')))
    }

    @Test
    fun testGaplessAlbumTransition() {
        val song1 = Song(id = "1", title = "Intro", artist = "Beatles", album = "Abbey Road", coverUrl = "", durationSeconds = 120)
        val song2 = Song(id = "2", title = "Come Together", artist = "Beatles", album = "Abbey Road", coverUrl = "", durationSeconds = 240)

        val plan = NormalAutoMixPlanner.plan(
            outgoing = song1,
            outgoingDurationMs = 120_000L,
            tail = null,
            outgoingLyrics = emptyList(),
            incoming = song2,
            head = null,
            gaplessAlbum = true,
            nowPositionMs = 110_000L
        )

        assertEquals(TransitionType.NO_TRANSITION, plan.type)
        assertEquals(1.0f, plan.confidenceScore, 0.001f)
        assertEquals(0L, plan.incomingStartOffsetMs)
        assertEquals(100L, plan.durationMs)
    }

    @Test
    fun testBeatMatchingPlanWithinBpmLimit() {
        val song1 = Song(id = "1", title = "Song 1", artist = "Artist", album = "Album", coverUrl = "", durationSeconds = 200)
        val song2 = Song(id = "2", title = "Song 2", artist = "Artist", album = "Album", coverUrl = "", durationSeconds = 200)

        val tail = SegmentAnalysis(
            segmentStartMs = 170_000L,
            segmentEndMs = 200_000L,
            firstSoundMs = 170_000L,
            lastSoundMs = 198_000L,
            bpm = 120f,
            bpmConfidence = 0.9f,
            beatPhaseMs = 0L
        )
        val head = SegmentAnalysis(
            segmentStartMs = 0L,
            segmentEndMs = 30_000L,
            firstSoundMs = 300L,
            lastSoundMs = 30_000L,
            bpm = 124f, // 120 vs 124 is ~3.2% difference, within 8.5%
            bpmConfidence = 0.9f,
            beatPhaseMs = 0L
        )

        val plan = NormalAutoMixPlanner.plan(
            outgoing = song1,
            outgoingDurationMs = 200_000L,
            tail = tail,
            outgoingLyrics = emptyList(),
            incoming = song2,
            head = head,
            gaplessAlbum = false,
            nowPositionMs = 150_000L
        )

        assertEquals(TransitionType.BEAT_MIX, plan.type)
        assertTrue("Confidence score should be high for beat mix", plan.confidenceScore >= 0.88f)
        assertTrue("Plan duration must be at least 4 seconds", plan.durationMs >= 4_000L)

        // EQ bass swap must land at the exact midpoint of transition duration
        val expectedBassSwap = plan.startPositionMs + (plan.durationMs / 2)
        assertEquals(expectedBassSwap, plan.bassSwapTimestampMs)

        // Tempo ratios must be adjusted
        assertTrue("Outgoing tempo adjustment ratio should be close to shared/out", plan.outgoingTempoAdjustmentRatio in 0.95f..1.05f)
        assertTrue("Incoming tempo adjustment ratio should be close to shared/in", plan.tempoAdjustmentRatio in 0.95f..1.05f)
    }

    @Test
    fun testBpmFallbackEqualPowerCrossfade() {
        val song1 = Song(id = "1", title = "Slow Jam", artist = "Artist", album = "Album", coverUrl = "", durationSeconds = 200)
        val song2 = Song(id = "2", title = "Fast Track", artist = "Artist", album = "Album", coverUrl = "", durationSeconds = 200)

        val tail = SegmentAnalysis(
            segmentStartMs = 170_000L,
            segmentEndMs = 200_000L,
            firstSoundMs = 170_000L,
            lastSoundMs = 198_000L,
            bpm = 100f,
            bpmConfidence = 0.9f,
            beatPhaseMs = 0L
        )
        val head = SegmentAnalysis(
            segmentStartMs = 0L,
            segmentEndMs = 30_000L,
            firstSoundMs = 200L,
            lastSoundMs = 30_000L,
            bpm = 140f, // 100 vs 140 is 40% difference, far beyond 8.5%
            bpmConfidence = 0.9f,
            beatPhaseMs = 0L
        )

        val plan = NormalAutoMixPlanner.plan(
            outgoing = song1,
            outgoingDurationMs = 200_000L,
            tail = tail,
            outgoingLyrics = emptyList(),
            incoming = song2,
            head = head,
            gaplessAlbum = false,
            nowPositionMs = 150_000L
        )

        assertEquals("BPM gap > 8.5% must trigger SHORT_CROSSFADE fallback", TransitionType.SHORT_CROSSFADE, plan.type)
        assertEquals("Fallback crossfade must not stretch incoming tempo", 1.0f, plan.tempoAdjustmentRatio, 0.001f)
        assertEquals("Fallback crossfade must not stretch outgoing tempo", 1.0f, plan.outgoingTempoAdjustmentRatio, 0.001f)
        assertTrue("Fallback crossfade duration should be capped at 7.5s", plan.durationMs <= 7500L)
    }

    @Test
    fun testVocalAvoidanceOutroSelection() {
        val song1 = Song(id = "1", title = "Vocal Song", artist = "Artist", album = "Album", coverUrl = "", durationSeconds = 200)
        val song2 = Song(id = "2", title = "Next Song", artist = "Artist", album = "Album", coverUrl = "", durationSeconds = 200)

        // Lyrics end at 175,000ms
        val lyrics = listOf(
            LyricLine(timeMs = 10_000L, text = "Starting song"),
            LyricLine(timeMs = 175_000L, text = "Last singing word")
        )

        val tail = SegmentAnalysis(
            segmentStartMs = 170_000L,
            segmentEndMs = 200_000L,
            firstSoundMs = 170_000L,
            lastSoundMs = 198_000L,
            bpm = 120f,
            bpmConfidence = 0.9f,
            beatPhaseMs = 0L
        )
        val head = SegmentAnalysis(
            segmentStartMs = 0L,
            segmentEndMs = 30_000L,
            firstSoundMs = 200L,
            lastSoundMs = 30_000L,
            bpm = 120f,
            bpmConfidence = 0.9f,
            beatPhaseMs = 0L
        )

        val plan = NormalAutoMixPlanner.plan(
            outgoing = song1,
            outgoingDurationMs = 200_000L,
            tail = tail,
            outgoingLyrics = lyrics,
            incoming = song2,
            head = head,
            gaplessAlbum = false,
            nowPositionMs = 150_000L
        )

        // Vocal ends at 175,000 + 2500 = 177,500ms
        // The mix start position must respect outro vocals and start around/after vocal finish
        assertTrue(
            "Mix start position ${plan.startPositionMs} must be placed near or after vocal conclusion (>= 170,000ms)",
            plan.startPositionMs >= 170_000L
        )
    }

    @Test
    fun testNaturalOutroWindowNotTooEarly() {
        val song1 = Song(id = "1", title = "Track 1", artist = "Artist", album = "Album", coverUrl = "", durationSeconds = 180)
        val song2 = Song(id = "2", title = "Track 2", artist = "Artist", album = "Album", coverUrl = "", durationSeconds = 180)

        val plan = NormalAutoMixPlanner.plan(
            outgoing = song1,
            outgoingDurationMs = 180_000L,
            tail = null,
            outgoingLyrics = emptyList(),
            incoming = song2,
            head = null,
            gaplessAlbum = false,
            nowPositionMs = 120_000L
        )

        // Must start within the final 14 seconds of the track (>= 166,000ms), NEVER 30+ seconds early
        assertTrue(
            "Transition start ${plan.startPositionMs} must be in the natural outro (>= 166,000ms), not premature",
            plan.startPositionMs >= 166_000L
        )
        assertTrue(
            "Transition duration ${plan.durationMs} must provide a smooth equal-power blend (>= 4,000ms)",
            plan.durationMs >= 4_000L
        )
    }
}
