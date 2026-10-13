package com.beatles.player.player.automix

import com.beatles.player.data.LyricLine
import com.beatles.player.data.Song
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Apple-Grade Normal AutoMix Planner.
 *
 * Blends tracks as one continuous DJ-style mix:
 *  - Phrase-aligned transition points at outro and intro boundaries
 *  - Beat-matched tempo convergence (Sonic pitch-preserved time-stretching within ±8.5%)
 *  - Equal-power volume curves + EQ bass swap at midpoint
 *  - Harmonic key compatibility via Camelot wheel
 *  - Automatic fallback to equal-power crossfade when BPM gap exceeds 8.5%
 *  - Gapless playback for continuous live/DJ albums
 */
object NormalAutoMixPlanner {

    fun plan(
        outgoing: Song,
        outgoingDurationMs: Long,
        tail: SegmentAnalysis?,
        outgoingLyrics: List<LyricLine>,
        incoming: Song,
        head: SegmentAnalysis?,
        gaplessAlbum: Boolean,
        nowPositionMs: Long,
        transitionLengthBars: Int = 4
    ): TransitionPlan {
        // 1. Gapless Album detection
        if (gaplessAlbum) {
            return TransitionPlan(
                type = TransitionType.NO_TRANSITION,
                startPositionMs = max(0L, outgoingDurationMs - 100L),
                durationMs = 100L,
                incomingStartOffsetMs = 0L,
                tempoAdjustmentRatio = 1.0f,
                confidenceScore = 1.0f,
                description = "Gapless Album Transition"
            )
        }

        val outInfo = TrackAnalyzer.analyzeTrack(outgoing, outgoingLyrics)
        val inInfo = TrackAnalyzer.analyzeTrack(incoming, emptyList())

        // 2. Real waveform tempo or acoustic analysis
        val outBpm = tail?.bpm?.takeIf { tail.hasReliableTempo } ?: outInfo.globalBpm
        val inBpm = head?.bpm?.takeIf { head.hasReliableTempo } ?: inInfo.globalBpm

        val match = BeatGridMatcher.calculateBeatMatch(
            outgoingBpm = outBpm,
            incomingBpm = inBpm,
            outgoingPhaseMs = tail?.beatPhaseMs ?: outInfo.downbeats.firstOrNull() ?: 0L,
            incomingPhaseMs = head?.beatPhaseMs ?: inInfo.downbeats.firstOrNull() ?: 0L
        )

        // 3. Harmonic Key analysis via Camelot wheel
        val keyOut = CamelotKey.from(outInfo.keyTonic, outInfo.keyScale)
        val keyIn = CamelotKey.from(inInfo.keyTonic, inInfo.keyScale)
        val harmonicallyCompatible = CamelotKey.areHarmonicallyCompatible(keyOut, keyIn)

        // 4. LUFS Loudness normalization
        val gainMatch = 10f.pow(((outInfo.integratedLUFS - inInfo.integratedLUFS) / 20f).coerceIn(-3f, 3f))
            .coerceIn(0.75f, 1.30f)

        // 5. Select Outro Phrase Boundary
        val dur = outgoingDurationMs
        val safeEarliestStart = max(dur * 0.70f, (dur - 38_000L).toFloat()).toLong()
        val minOutroStart = max(safeEarliestStart, outInfo.vocalEndMs)

        // Phrase boundary (every 4 bars)
        val phraseAlignedStart = BeatGridMatcher.findNextPhraseBoundary(
            minPositionMs = minOutroStart,
            bpm = outBpm,
            downbeatOffsetMs = outInfo.downbeats.firstOrNull() ?: 0L,
            barsPerPhrase = 4
        )

        // 6. Transition length calculation
        val barMs = (60_000f / match.sharedBpm) * 4f
        val requestedBars = transitionLengthBars.coerceIn(2, 8)
        val targetDurationMs = (barMs * requestedBars).toLong().coerceIn(4_000L, 24_000L)

        // Ensure transition finishes before the song's audio drops into total trailing silence
        val effectiveLastSound = tail?.lastSoundMs ?: (dur - outInfo.trailingSilenceMs)
        val finalStartMs = min(phraseAlignedStart, max(0L, effectiveLastSound - targetDurationMs))
        val actualDurationMs = min(targetDurationMs, max(3_000L, effectiveLastSound - finalStartMs))

        // 7. Incoming Start Offset (skip leading silence)
        val leadingSilence = head?.firstSoundMs ?: inInfo.leadingSilenceMs
        val incomingOffset = max(leadingSilence, match.alignedStartOffsetMs)

        val bassSwapMs = finalStartMs + (actualDurationMs / 2)

        return if (match.canBeatMatch) {
            TransitionPlan(
                type = TransitionType.BEAT_MIX,
                startPositionMs = finalStartMs,
                durationMs = actualDurationMs,
                incomingStartOffsetMs = incomingOffset,
                tempoAdjustmentRatio = match.incomingTempoRatio,
                outgoingTempoAdjustmentRatio = match.outgoingTempoRatio,
                bassSwapTimestampMs = bassSwapMs,
                confidenceScore = if (harmonicallyCompatible) 0.95f else 0.88f,
                incomingGainMatch = gainMatch,
                description = "Apple Beat-Mix: ${match.sharedBpm.toInt()} BPM (${keyOut} -> ${keyIn})"
            )
        } else {
            // Equal-power crossfade fallback when BPM gap > 8.5%
            val fallbackDuration = min(actualDurationMs, 7_500L)
            TransitionPlan(
                type = TransitionType.SHORT_CROSSFADE,
                startPositionMs = max(0L, dur - fallbackDuration - 1200L),
                durationMs = fallbackDuration,
                incomingStartOffsetMs = leadingSilence,
                tempoAdjustmentRatio = 1.0f,
                outgoingTempoAdjustmentRatio = 1.0f,
                bassSwapTimestampMs = max(0L, dur - (fallbackDuration / 2) - 1200L),
                confidenceScore = 0.80f,
                incomingGainMatch = gainMatch,
                description = "Equal-Power Dissolve: ${outBpm.toInt()} -> ${inBpm.toInt()} BPM"
            )
        }
    }
}
