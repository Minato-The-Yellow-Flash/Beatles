package com.beatles.player.player.automix

import kotlin.math.abs
import kotlin.math.roundToLong

data class BeatMatchResult(
    val canBeatMatch: Boolean,
    val effectiveIncomingBpm: Float,
    val sharedBpm: Float,
    val outgoingTempoRatio: Float,
    val incomingTempoRatio: Float,
    val alignedStartOffsetMs: Long,
    val beatIntervalMs: Float
)

object BeatGridMatcher {
    const val MAX_TEMPO_STRETCH_PCT = 0.085f // ±8.5% maximum pitch-preserved speed adjustment

    /**
     * Resolves tempo alignment between outgoing and incoming tracks.
     * Evaluates 1x, 2x (double-time), and 0.5x (half-time) tempo relationships.
     */
    fun calculateBeatMatch(
        outgoingBpm: Float,
        incomingBpm: Float,
        outgoingPhaseMs: Long = 0L,
        incomingPhaseMs: Long = 0L
    ): BeatMatchResult {
        if (outgoingBpm <= 0f || incomingBpm <= 0f) {
            return BeatMatchResult(
                canBeatMatch = false,
                effectiveIncomingBpm = incomingBpm,
                sharedBpm = outgoingBpm,
                outgoingTempoRatio = 1.0f,
                incomingTempoRatio = 1.0f,
                alignedStartOffsetMs = 0L,
                beatIntervalMs = if (outgoingBpm > 0f) 60000f / outgoingBpm else 500f
            )
        }

        val candidates = listOf(incomingBpm, incomingBpm * 2f, incomingBpm * 0.5f)
        val bestIncomingBpm = candidates.minByOrNull { abs(outgoingBpm / it - 1f) } ?: incomingBpm
        val ratio = outgoingBpm / bestIncomingBpm
        val diffPct = abs(ratio - 1f)

        return if (diffPct <= MAX_TEMPO_STRETCH_PCT) {
            val shared = (outgoingBpm + bestIncomingBpm) / 2f
            val outRatio = (shared / outgoingBpm).coerceIn(0.92f, 1.08f)
            val inRatio = (shared / bestIncomingBpm).coerceIn(0.92f, 1.08f)
            val beatInterval = 60000f / shared

            // Align beat grid phases so downbeats land on the exact same millisecond
            val inBeatMs = 60000f / bestIncomingBpm
            val phaseDiff = ((outgoingPhaseMs - incomingPhaseMs) % inBeatMs.toLong() + inBeatMs.toLong()) % inBeatMs.toLong()
            val alignedOffset = (inBeatMs.toLong() - phaseDiff) % inBeatMs.toLong()

            BeatMatchResult(
                canBeatMatch = true,
                effectiveIncomingBpm = bestIncomingBpm,
                sharedBpm = shared,
                outgoingTempoRatio = outRatio,
                incomingTempoRatio = inRatio,
                alignedStartOffsetMs = alignedOffset,
                beatIntervalMs = beatInterval
            )
        } else {
            BeatMatchResult(
                canBeatMatch = false,
                effectiveIncomingBpm = incomingBpm,
                sharedBpm = outgoingBpm,
                outgoingTempoRatio = 1.0f,
                incomingTempoRatio = 1.0f,
                alignedStartOffsetMs = 0L,
                beatIntervalMs = 60000f / outgoingBpm
            )
        }
    }

    /**
     * Finds the closest 4-bar phrase boundary (16 beats) after [minPositionMs].
     */
    fun findNextPhraseBoundary(
        minPositionMs: Long,
        bpm: Float,
        downbeatOffsetMs: Long = 0L,
        barsPerPhrase: Int = 4
    ): Long {
        if (bpm <= 0f) return minPositionMs
        val beatMs = 60000.0 / bpm
        val phraseMs = beatMs * 4.0 * barsPerPhrase
        val elapsed = (minPositionMs - downbeatOffsetMs).coerceAtLeast(0L)
        val phraseCount = kotlin.math.ceil(elapsed / phraseMs).toLong()
        return (downbeatOffsetMs + (phraseCount * phraseMs)).roundToLong()
    }
}
