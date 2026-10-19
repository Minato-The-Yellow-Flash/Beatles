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
                description = "Gapless Album Transition",
                tier = AutoMixTier.TIER_3_GAPLESS_MICROFADE
            )
        }

        // 2. Very short tracks (< 20s) safety handling
        if (outgoingDurationMs < 20_000L) {
            val isMicro = outgoingDurationMs < 10_000L
            val durMs = if (isMicro) 200L else min(3_000L, outgoingDurationMs / 4)
            val startMs = max(0L, outgoingDurationMs - durMs - 250L)
            return TransitionPlan(
                type = if (isMicro) TransitionType.NO_TRANSITION else TransitionType.SHORT_CROSSFADE,
                startPositionMs = startMs,
                durationMs = durMs,
                incomingStartOffsetMs = head?.firstSoundMs ?: 0L,
                tempoAdjustmentRatio = 1.0f,
                outgoingTempoAdjustmentRatio = 1.0f,
                bassSwapTimestampMs = startMs + (durMs / 2),
                confidenceScore = 0.80f,
                description = if (isMicro) "Tier 3: Short Track Microfade" else "Tier 2: Short Track Equal-Power Blend",
                tier = if (isMicro) AutoMixTier.TIER_3_GAPLESS_MICROFADE else AutoMixTier.TIER_2_EQUAL_POWER_CROSSFADE
            )
        }

        val outInfo = TrackAnalyzer.analyzeTrack(outgoing, outgoingLyrics)
        val inInfo = TrackAnalyzer.analyzeTrack(incoming, emptyList())

        // 3. Real waveform tempo or acoustic analysis
        val isOutReliable = if (tail != null) tail.hasReliableTempo else true
        val isInReliable = if (head != null) head.hasReliableTempo else true

        val outBpm = tail?.bpm?.takeIf { tail.hasReliableTempo } ?: outInfo.globalBpm
        val inBpm = head?.bpm?.takeIf { head.hasReliableTempo } ?: inInfo.globalBpm

        val match = if (isOutReliable && isInReliable) {
            BeatGridMatcher.calculateBeatMatch(
                outgoingBpm = outBpm,
                incomingBpm = inBpm,
                outgoingPhaseMs = tail?.beatPhaseMs ?: outInfo.downbeats.firstOrNull() ?: 0L,
                incomingPhaseMs = head?.beatPhaseMs ?: inInfo.downbeats.firstOrNull() ?: 0L
            )
        } else {
            BeatGridMatcher.calculateBeatMatch(0f, 0f)
        }

        // 4. Harmonic Key analysis via Camelot wheel
        val keyOut = CamelotKey.from(outInfo.keyTonic, outInfo.keyScale)
        val keyIn = CamelotKey.from(inInfo.keyTonic, inInfo.keyScale)
        val harmonicallyCompatible = CamelotKey.areHarmonicallyCompatible(keyOut, keyIn)

        // 5. LUFS Loudness normalization
        val gainMatch = 10f.pow(((outInfo.integratedLUFS - inInfo.integratedLUFS) / 20f).coerceIn(-3f, 3f))
            .coerceIn(0.75f, 1.30f)

        // 6. Select Natural Outro Boundary (Apple Music style: extended smooth transition beyond 10s window)
        val dur = outgoingDurationMs
        val targetDurationMs = 12_000L // 12 seconds smooth transition length
        val effectiveLastSound = tail?.lastSoundMs ?: (dur - outInfo.trailingSilenceMs)

        // Apple Music switches smoothly when vocals conclude (in midway / 16-28s before end)
        // or in the natural outro window (final 12-14s) when lyrics are absent
        val hasConfirmedLyricsOutro = outgoingLyrics.isNotEmpty() && outInfo.vocalEndMs > 0 && outInfo.vocalEndMs < dur - 10_000L
        val minOutroStart = if (hasConfirmedLyricsOutro) {
            max(outInfo.vocalEndMs, max(0L, dur - 28_000L))
        } else {
            max(0L, dur - 12_000L)
        }

        val outroWindowStart = if (hasConfirmedLyricsOutro) max(0L, dur - 28_000L) else max(0L, dur - 13_500L)
        val safeStartMs = (dur - targetDurationMs - 1_200L).coerceAtLeast(0L)

        // Phrase boundary aligned with outro
        val phraseAlignedStart = BeatGridMatcher.findNextPhraseBoundary(
            minPositionMs = minOutroStart,
            bpm = outBpm,
            downbeatOffsetMs = outInfo.downbeats.firstOrNull() ?: 0L,
            barsPerPhrase = 4
        )

        // Lock start to natural outro window allowing smooth transitions
        val finalStartMs = phraseAlignedStart.coerceIn(
            outroWindowStart,
            max(0L, dur - 5_000L)
        ).takeIf { it > 0L } ?: safeStartMs

        val actualDurationMs = min(targetDurationMs, max(5_000L, effectiveLastSound - finalStartMs))

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
                description = "Tier 1: Apple Beat-Mix ${match.sharedBpm.toInt()} BPM (${keyOut} -> ${keyIn})",
                tier = AutoMixTier.TIER_1_BEAT_MATCH
            )
        } else {
            // Equal-power crossfade fallback with smooth blend (5.0s - 7.5s)
            val fallbackDuration = min(7_500L, max(5_000L, effectiveLastSound - (dur - 7_500L)))
            val fallbackStart = (dur - fallbackDuration - 800L).coerceAtLeast(0L)
            TransitionPlan(
                type = TransitionType.SHORT_CROSSFADE,
                startPositionMs = fallbackStart,
                durationMs = fallbackDuration,
                incomingStartOffsetMs = leadingSilence,
                tempoAdjustmentRatio = 1.0f,
                outgoingTempoAdjustmentRatio = 1.0f,
                bassSwapTimestampMs = fallbackStart + (fallbackDuration / 2),
                confidenceScore = 0.85f,
                incomingGainMatch = gainMatch,
                description = "Tier 2: Equal-Power Dissolve ${outBpm.toInt()} -> ${inBpm.toInt()} BPM",
                tier = AutoMixTier.TIER_2_EQUAL_POWER_CROSSFADE
            )
        }
    }

    /**
     * Creates an immediate safe fallback plan when incoming track preparation has delayed or errored.
     */
    fun createFallbackPlan(
        outgoingDurationMs: Long,
        nowPositionMs: Long,
        incomingLeadingSilenceMs: Long = 0L,
        tier: AutoMixTier = AutoMixTier.TIER_2_EQUAL_POWER_CROSSFADE
    ): TransitionPlan {
        val dur = outgoingDurationMs.coerceAtLeast(1000L)
        return when (tier) {
            AutoMixTier.TIER_1_BEAT_MATCH,
            AutoMixTier.TIER_2_EQUAL_POWER_CROSSFADE -> {
                val fadeDur = min(6000L, max(3000L, dur / 4))
                val startMs = max(nowPositionMs, dur - fadeDur - 500L).coerceAtMost(max(0L, dur - 1000L))
                val actualFade = max(2500L, dur - startMs)
                TransitionPlan(
                    type = TransitionType.SHORT_CROSSFADE,
                    startPositionMs = startMs,
                    durationMs = actualFade,
                    incomingStartOffsetMs = incomingLeadingSilenceMs,
                    tempoAdjustmentRatio = 1.0f,
                    outgoingTempoAdjustmentRatio = 1.0f,
                    bassSwapTimestampMs = startMs + actualFade / 2,
                    confidenceScore = 0.75f,
                    description = "Tier 2: Equal-Power Safety Fallback (${actualFade / 1000f}s)",
                    tier = AutoMixTier.TIER_2_EQUAL_POWER_CROSSFADE
                )
            }
            AutoMixTier.TIER_3_GAPLESS_MICROFADE -> {
                val fadeDur = 200L
                val startMs = max(nowPositionMs, dur - fadeDur).coerceAtMost(max(0L, dur - 50L))
                TransitionPlan(
                    type = TransitionType.NO_TRANSITION,
                    startPositionMs = startMs,
                    durationMs = fadeDur,
                    incomingStartOffsetMs = incomingLeadingSilenceMs,
                    tempoAdjustmentRatio = 1.0f,
                    outgoingTempoAdjustmentRatio = 1.0f,
                    confidenceScore = 0.70f,
                    description = "Tier 3: Gapless Safety Microfade (200ms)",
                    tier = AutoMixTier.TIER_3_GAPLESS_MICROFADE
                )
            }
        }
    }
}
