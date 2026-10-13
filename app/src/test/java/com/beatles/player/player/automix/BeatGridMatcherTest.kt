package com.beatles.player.player.automix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BeatGridMatcherTest {

    @Test
    fun testTempoStretchWithinLimit() {
        // 120 BPM outgoing and 124 BPM incoming
        // diff = abs(120/124 - 1) = 3.2% <= 8.5%
        val result = BeatGridMatcher.calculateBeatMatch(
            outgoingBpm = 120f,
            incomingBpm = 124f
        )

        assertTrue("Should be able to beat match within 8.5% tempo difference", result.canBeatMatch)
        assertEquals(122f, result.sharedBpm, 0.01f)
        assertTrue("Outgoing tempo ratio should be slightly sped up", result.outgoingTempoRatio > 1.0f)
        assertTrue("Incoming tempo ratio should be slightly slowed down", result.incomingTempoRatio < 1.0f)
    }

    @Test
    fun testTempoStretchExceedingLimitRejection() {
        // 120 BPM outgoing and 140 BPM incoming
        // diff = abs(120/140 - 1) = 14.3% > 8.5%
        val result = BeatGridMatcher.calculateBeatMatch(
            outgoingBpm = 120f,
            incomingBpm = 140f
        )

        assertFalse("Tempo difference > 8.5% must reject beat match and fall back", result.canBeatMatch)
        assertEquals(1.0f, result.outgoingTempoRatio, 0.001f)
        assertEquals(1.0f, result.incomingTempoRatio, 0.001f)
    }

    @Test
    fun testOctaveResolutionDoubleTime() {
        // 75 BPM outgoing and 148 BPM incoming
        // Best candidate for incoming is half-time: 74 BPM (148 * 0.5)
        // 75 / 74 = 1.35% difference <= 8.5%
        val result = BeatGridMatcher.calculateBeatMatch(
            outgoingBpm = 75f,
            incomingBpm = 148f
        )

        assertTrue("Should detect double/half-time relationship", result.canBeatMatch)
        assertEquals(74f, result.effectiveIncomingBpm, 0.01f)
        assertEquals(74.5f, result.sharedBpm, 0.01f)
    }

    @Test
    fun testOctaveResolutionHalfTime() {
        // 160 BPM outgoing and 82 BPM incoming
        // Best candidate for incoming is double-time: 164 BPM (82 * 2)
        // 160 / 164 = 2.4% difference <= 8.5%
        val result = BeatGridMatcher.calculateBeatMatch(
            outgoingBpm = 160f,
            incomingBpm = 82f
        )

        assertTrue("Should detect half/double-time relationship", result.canBeatMatch)
        assertEquals(164f, result.effectiveIncomingBpm, 0.01f)
        assertEquals(162f, result.sharedBpm, 0.01f)
    }

    @Test
    fun testMillisecondAccuratePhaseAlignment() {
        // Outgoing has downbeat at 200ms, Incoming has downbeat at 100ms
        // Both 120 BPM (500ms per beat)
        val result = BeatGridMatcher.calculateBeatMatch(
            outgoingBpm = 120f,
            incomingBpm = 120f,
            outgoingPhaseMs = 200L,
            incomingPhaseMs = 100L
        )

        assertTrue(result.canBeatMatch)
        assertEquals(500f, result.beatIntervalMs, 0.01f)
        // Phase difference = (200 - 100) = 100ms.
        // Aligned offset to shift incoming = (500 - 100) % 500 = 400ms.
        assertEquals(400L, result.alignedStartOffsetMs)
    }

    @Test
    fun testPhraseBoundaryCalculation() {
        // 120 BPM -> 500ms beat -> 4 bars = 16 beats = 8,000ms per phrase
        val phrase1 = BeatGridMatcher.findNextPhraseBoundary(
            minPositionMs = 180_000L,
            bpm = 120f,
            downbeatOffsetMs = 0L,
            barsPerPhrase = 4
        )
        // 180,000 / 8,000 = 22.5 phrases -> ceil to 23 -> 23 * 8,000 = 184,000ms
        assertEquals(184_000L, phrase1)

        // Exact match on phrase boundary remains on that boundary
        val phrase2 = BeatGridMatcher.findNextPhraseBoundary(
            minPositionMs = 160_000L,
            bpm = 120f,
            downbeatOffsetMs = 0L,
            barsPerPhrase = 4
        )
        assertEquals(160_000L, phrase2)

        // With downbeat offset
        val phraseWithOffset = BeatGridMatcher.findNextPhraseBoundary(
            minPositionMs = 10_000L,
            bpm = 120f,
            downbeatOffsetMs = 500L,
            barsPerPhrase = 4
        )
        // elapsed = (10,000 - 500) = 9500
        // ceil(9500 / 8000) = 2 phrases -> 500 + 2 * 8000 = 16,500ms
        assertEquals(16_500L, phraseWithOffset)
    }
}
