package com.beatles.player.player.automix

import com.beatles.player.data.Song

enum class TransitionType {
    NO_TRANSITION,
    SILENCE_REMOVAL,
    SHORT_CROSSFADE,
    LONG_CROSSFADE,
    BEAT_MIX,
    PHRASE_MIX,
    INSTRUMENTAL_OVERLAP,
    DROP_TRANSITION,
    ECHO_OUT,
    FILTER_FADE,
    BASS_SWAP
}

enum class AutoMixMode {
    ON,
    OFF;

    companion object {
        val NEW get() = ON
        val OLD get() = ON
    }
}

enum class AutoMixTier {
    TIER_1_BEAT_MATCH,
    TIER_2_EQUAL_POWER_CROSSFADE,
    TIER_3_GAPLESS_MICROFADE
}

enum class TransitionPhase {
    IDLE,
    ANALYZING,
    PREPARED,
    ARMED,
    TRANSITIONING,
    HANDOVER,
    FALLBACK,
    ERROR;

    companion object {
        val PREPARING get() = PREPARED
        val TRANSITION_ACTIVE get() = TRANSITIONING
        val HANDOFF_COMPLETE get() = HANDOVER
    }
}

data class TimeRegion(
    val startMs: Long,
    val endMs: Long,
    val description: String = ""
)

/** Metadata-based estimate used only when waveform analysis is unavailable. */
data class TrackAnalysis(
    val trackId: String,
    val durationMs: Long,
    val globalBpm: Float,
    val keyTonic: String,
    val keyScale: String, // "MAJOR" or "MINOR"
    val integratedLUFS: Float,
    val introDurationMs: Long,
    val outroStartMs: Long,
    val vocalStartMs: Long,
    val vocalEndMs: Long,
    val leadingSilenceMs: Long,
    val trailingSilenceMs: Long,
    val beatIntervalMs: Long,
    val downbeats: List<Long> = emptyList(),
    val phraseBoundaries: List<Long> = emptyList(),
    val bassEnergy: Float = 0.5f,
    val isBassHeavy: Boolean = false
)

/**
 * Result of real PCM waveform analysis of one region of a track
 * (the last ~32 s of the outgoing song, or the first ~22 s of the incoming song).
 * All timestamps are absolute positions within the track, in milliseconds.
 */
data class SegmentAnalysis(
    val segmentStartMs: Long,
    val segmentEndMs: Long,
    /** First frame above the silence floor. */
    val firstSoundMs: Long,
    /** Last frame above the silence floor (trailing silence starts after this). */
    val lastSoundMs: Long,
    /** Tail only: where the track's energy starts falling into a fade-out / outro. */
    val fadeOutStartMs: Long? = null,
    /** Head only: where the intro reaches full "body" energy (end of the soft intro). */
    val bodyStartMs: Long? = null,
    /** RMS loudness of the non-silent part (linear, 0..1). */
    val loudness: Float = 0f,
    val bpm: Float? = null,
    /** 0..1 – normalised autocorrelation peak strength of the onset envelope. */
    val bpmConfidence: Float = 0f,
    /** Absolute time of one detected beat (the beat grid phase). */
    val beatPhaseMs: Long? = null
) {
    val beatIntervalMs: Float? get() = bpm?.let { 60000f / it }
    val hasReliableTempo: Boolean get() = bpm != null && bpmConfidence >= 0.15f
}

data class TransitionPlan(
    val type: TransitionType,
    val startPositionMs: Long,
    val durationMs: Long,
    val incomingStartOffsetMs: Long,
    /** Playback speed applied to the incoming track during the mix (tempo match). */
    val tempoAdjustmentRatio: Float = 1.0f,
    /** Playback speed applied to the outgoing track during the mix (active tempo convergence). */
    val outgoingTempoAdjustmentRatio: Float = 1.0f,
    val bassSwapTimestampMs: Long = 0L,
    val confidenceScore: Float = 0.85f,
    val description: String = "",
    /** Loudness-matching multiplier for the incoming track at the start of the mix. */
    val incomingGainMatch: Float = 1.0f,
    /** True when the plan was built from decoded audio rather than metadata estimates. */
    val fromWaveform: Boolean = false,
    val echoDelayMs: Int = 360,
    val tier: AutoMixTier = AutoMixTier.TIER_2_EQUAL_POWER_CROSSFADE
)

data class AutoMixState(
    val phase: TransitionPhase = TransitionPhase.IDLE,
    val progress: Float = 0f, // 0.0 to 1.0
    val activePlan: TransitionPlan? = null,
    val incomingSong: Song? = null,
    val tier: AutoMixTier? = null,
    val debugLog: String? = null
) {
    val isMixing: Boolean
        get() = phase == TransitionPhase.TRANSITIONING || phase == TransitionPhase.HANDOVER || phase == TransitionPhase.FALLBACK

    val label: String
        get() = when (tier) {
            AutoMixTier.TIER_1_BEAT_MATCH -> if (activePlan?.type == TransitionType.DROP_TRANSITION) "Beat drop mix" else "Beat-matched mix"
            AutoMixTier.TIER_2_EQUAL_POWER_CROSSFADE -> "Equal-power crossfade"
            AutoMixTier.TIER_3_GAPLESS_MICROFADE -> "Gapless mix"
            null -> when (activePlan?.type) {
                TransitionType.BEAT_MIX -> "Beat-matched mix"
                TransitionType.DROP_TRANSITION -> "Beat drop mix"
                TransitionType.ECHO_OUT -> "DJ Echo Out mix"
                TransitionType.FILTER_FADE -> "DJ Filter Fade"
                TransitionType.BASS_SWAP -> "DJ Bass Swap mix"
                TransitionType.PHRASE_MIX -> "Phrase mix"
                TransitionType.INSTRUMENTAL_OVERLAP -> "Intro overlap"
                TransitionType.LONG_CROSSFADE -> "Outro blend"
                TransitionType.SILENCE_REMOVAL -> "Silence trimmed"
                TransitionType.SHORT_CROSSFADE -> "Crossfade"
                else -> "AutoMix"
            }
        }
}
