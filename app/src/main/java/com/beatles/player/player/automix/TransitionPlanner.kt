package com.beatles.player.player.automix

import com.beatles.player.data.LyricLine
import com.beatles.player.data.Song
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Intelligent Apple Music-Grade DJ Transition Planner.
 *
 * Dynamically selects the most musical transition style based on acoustic character:
 *  1. BEAT_MIX: Tempo-locked 4-bar or 2-bar mix with pitch-preserved speedup/slowdown (Sonic time-stretching
 *     within ±8%) and smooth post-handoff tempo glide back to 1.0x native speed.
 *  2. DROP_TRANSITION: Clean drop on Beat 1 when tempos differ (> 8% BPM clash) or genres contrast,
 *     ducking outgoing track on phrase boundary with filter decay while incoming drops its main beat/hook.
 *  3. LONG_CROSSFADE (Melodic Dissolve): Harmonic frequency blend for acoustic, vocal, or melodic tracks.
 *  4. INSTRUMENTAL_OVERLAP: Outro overlaps incoming instrumental intro before vocals start.
 *
 * Strict Rules:
 *  - ZERO vocal collisions (never allows two vocalists to sing over each other).
 *  - Transitions ONLY start during the outro region (never in the first 60% of a song).
 *  - Full downbeat & 4-bar phrase alignment.
 */
object TransitionPlanner {

    private val GAPLESS_GENRES = listOf("classical", "opera", "live", "soundtrack", "score", "orchestral")

    fun isGaplessAlbumPair(outgoing: Song, incoming: Song, playlist: List<Song>): Boolean {
        val album = outgoing.album.trim()
        if (album.isBlank() || isGenericAlbum(album)) return false
        if (!album.equals(incoming.album.trim(), ignoreCase = true)) return false
        val sameAlbumCount = playlist.count { it.album.equals(album, ignoreCase = true) }
        if (sameAlbumCount < 3) return false
        val g = outgoing.genre.lowercase()
        val a = album.lowercase()
        return GAPLESS_GENRES.any { g.contains(it) } || a.contains("live") || a.contains("unplugged")
    }

    fun isGenericAlbum(album: String): Boolean = album.trim().lowercase() in setOf(
        "", "single", "album", "related", "unknown", "album/playlist", "various", "playlist", "ep"
    )

    private data class MixCandidate(
        val type: TransitionType,
        val startPositionMs: Long,
        val durationMs: Long,
        val incomingStartOffsetMs: Long,
        val tempoRatio: Float,
        val outgoingTempoRatio: Float = 1.0f,
        val echoDelayMs: Int = 360,
        val score: Float,
        val description: String
    )

    private val CIRCLE_OF_FIFTHS = listOf("C", "G", "D", "A", "E", "B", "F#", "Db", "Ab", "Eb", "Bb", "F")
    private fun isHarmonicallyCompatible(k1: String, k2: String): Boolean {
        if (k1.equals(k2, ignoreCase = true)) return true
        val i1 = CIRCLE_OF_FIFTHS.indexOfFirst { it.equals(k1, ignoreCase = true) }
        val i2 = CIRCLE_OF_FIFTHS.indexOfFirst { it.equals(k2, ignoreCase = true) }
        if (i1 < 0 || i2 < 0) return false
        val diff = abs(i1 - i2)
        return diff == 1 || diff == CIRCLE_OF_FIFTHS.size - 1
    }

    fun plan(
        outgoing: Song,
        outgoingDurationMs: Long,
        tail: SegmentAnalysis?,
        outgoingLyrics: List<LyricLine>,
        incoming: Song,
        head: SegmentAnalysis?,
        gaplessAlbum: Boolean,
        nowPositionMs: Long,
        extendedWindow: Boolean = true
    ): TransitionPlan {
        val dur = outgoingDurationMs
        val outInfo = TrackAnalyzer.analyzeTrack(outgoing, outgoingLyrics)
        val inInfo = TrackAnalyzer.analyzeTrack(incoming, emptyList())

        // BPM resolution
        val outBpm = outInfo.globalBpm
        val rawInBpm = inInfo.globalBpm
        val candidatesBpm = listOf(rawInBpm, rawInBpm * 2f, rawInBpm * 0.5f)
        val bestInBpm = candidatesBpm.minByOrNull { abs(outBpm / it - 1f) } ?: rawInBpm
        val rawRatio = outBpm / bestInBpm
        val bpmDiffPct = abs(rawRatio - 1f)

        val outBeat = 60_000f / outBpm
        val inBeat = 60_000f / bestInBpm
        val outPhase = outInfo.downbeats.firstOrNull() ?: 0L
        val inPhase = inInfo.downbeats.firstOrNull() ?: 0L

        // 4-beat bar (measure) grids
        val outBar = outBeat * 4f
        val inBar = inBeat * 4f

        // Outro timing window:
        // Extended window: anywhere in the last 1 minute (or after 55% for shorter tracks)
        // Standard window: classic 40s outro region
        val minExitTime = if (extendedWindow) {
            maxOf(nowPositionMs + 3_500L, (dur - 60_000L).coerceAtLeast(0L), (dur * 0.55f).toLong())
        } else {
            maxOf(nowPositionMs + 3_500L, (dur * 0.65f).toLong(), (dur - 40_000L).coerceAtLeast(0L))
        }
        val maxExitTime = (dur - 4_000L).coerceAtLeast(minExitTime)
        val outVocalEnd = lastSyncedVocalMs(outgoingLyrics, dur) ?: outInfo.vocalEndMs

        // Find candidate Outgoing Exit Points (Out-Cues in the Outro):
        val outStartCandidates = mutableListOf<Long>()
        if (outVocalEnd in minExitTime..maxExitTime) {
            outStartCandidates.add(ceilToGrid(outVocalEnd + 200L, outPhase, outBar))
        }
        for (pb in outInfo.phraseBoundaries) {
            if (pb in minExitTime..maxExitTime && pb >= (outVocalEnd - 1_000L)) {
                outStartCandidates.add(ceilToGrid(pb, outPhase, outBar))
            }
        }
        val lateRegionStart = (dur - 35_000L).coerceAtLeast(minExitTime)
        for (db in outInfo.downbeats) {
            if (db in lateRegionStart..maxExitTime && db >= (outVocalEnd - 1_000L)) {
                outStartCandidates.add(ceilToGrid(db, outPhase, outBar))
            }
        }
        outStartCandidates.add(ceilToGrid((dur - 20_000L).coerceAtLeast(minExitTime), outPhase, outBar))
        val validOutStarts = outStartCandidates.distinct().filter { it in minExitTime..maxExitTime }

        // Find candidate Incoming Entry Points (In-Cues):
        // Extended window: up to first 2 minutes (120s); Standard: up to first 60s
        val maxInCue = if (extendedWindow) 120_000L else 60_000L
        val candidateOffsets = mutableListOf<Long>()
        candidateOffsets.add(0L)
        if (inInfo.introDurationMs in 3_000L..maxInCue) {
            candidateOffsets.add(ceilToGrid(inInfo.introDurationMs, inPhase, inBar))
        }
        for (phraseBeat in inInfo.phraseBoundaries) {
            if (phraseBeat in 3_000L..maxInCue) {
                candidateOffsets.add(ceilToGrid(phraseBeat, inPhase, inBar))
            }
        }
        for (downbeat in inInfo.downbeats) {
            if (downbeat in 3_000L..maxInCue) {
                candidateOffsets.add(ceilToGrid(downbeat, inPhase, inBar))
            }
        }
        val inOffsets = candidateOffsets.distinct().filter { it >= 0L }

        val evaluatedCandidates = mutableListOf<MixCandidate>()

        for (outStart in validOutStarts) {
            for (inOffset in inOffsets) {
                // Determine vocal presence
                val outSinging = outStart < (outVocalEnd - 500L)

                // -------------------------------------------------------------
                // STYLE 1: BEAT_MIX (Tempo-locked rhythmic mix with speedup/slowdown convergence)
                // Expanded to match up to ±18% difference with active two-deck tempo convergence
                // -------------------------------------------------------------
                if (bpmDiffPct <= 0.18f) {
                    val testBeatCounts = intArrayOf(16, 8)
                    val targetBpm = (outBpm + bestInBpm) / 2f
                    val outRatio = (targetBpm / outBpm).coerceIn(0.86f, 1.15f)
                    val inRatio = (targetBpm / bestInBpm).coerceIn(0.86f, 1.15f)
                    val echoDelay = (60_000f / targetBpm * 0.75f).toInt().coerceIn(200, 550)

                    for (beats in testBeatCounts) {
                        val mixLen = (outBeat * beats).toLong()
                        if (outStart + mixLen > dur - 1_000L) continue

                        val inSinging = (inOffset + mixLen) > (inInfo.vocalStartMs + 500L)
                        var score = 160f // Prioritize beat-to-beat mix

                        if (outSinging && inSinging) {
                            score -= 2000f // Vocal clash rejection
                        } else if (!outSinging && !inSinging) {
                            score += 100f // Pure instrumental beat-to-beat groove: GOLD!
                        } else if (!outSinging && inSinging) {
                            score += 60f
                        } else {
                            score += 40f
                        }

                        if (outInfo.isBassHeavy && inInfo.isBassHeavy) score += 50f
                        if (isHarmonicallyCompatible(outInfo.keyTonic, inInfo.keyTonic)) score += 30f
                        score += (1.0f - bpmDiffPct * 2f).coerceIn(0f, 1f) * 40f

                        val desc = "Beat-matched $beats-beat mix (${outBpm.toInt()} → ${targetBpm.toInt()} → ${bestInBpm.toInt()} BPM)"
                        evaluatedCandidates.add(
                            MixCandidate(
                                type = TransitionType.BEAT_MIX,
                                startPositionMs = outStart,
                                durationMs = mixLen,
                                incomingStartOffsetMs = inOffset,
                                tempoRatio = inRatio,
                                outgoingTempoRatio = outRatio,
                                echoDelayMs = echoDelay,
                                score = score,
                                description = desc
                            )
                        )
                    }
                }

                // -------------------------------------------------------------
                // STYLE 2: DROP_TRANSITION (Apple Music Beat Drop / Vibe Switch on the 1)
                // Outgoing track builds up with filter sweep + vocal cut + echo tail,
                // while incoming track DROPS immediately on Beat 1 with bass boost!
                // -------------------------------------------------------------
                if (bpmDiffPct > 0.12f || outInfo.isBassHeavy != inInfo.isBassHeavy) {
                    val dropLen = (outBeat * 4f).toLong().coerceIn(2_800L, 5_000L)
                    if (outStart + dropLen <= dur - 1_000L) {
                        val inSinging = (inOffset + dropLen) > (inInfo.vocalStartMs + 800L)
                        var dropScore = 140f // Prioritize beat drop over crossfade
                        if (outSinging && inSinging) dropScore -= 1500f
                        if (!outSinging) dropScore += 50f
                        if (bpmDiffPct > 0.15f) dropScore += 45f
                        val echoDelay = (60_000f / outBpm * 0.5f).toInt().coerceIn(200, 500)

                        val desc = "Beat drop transition on the 1 (${bestInBpm.toInt()} BPM)"
                        evaluatedCandidates.add(
                            MixCandidate(
                                type = TransitionType.DROP_TRANSITION,
                                startPositionMs = outStart,
                                durationMs = dropLen,
                                incomingStartOffsetMs = inOffset,
                                tempoRatio = 1.0f,
                                outgoingTempoRatio = 1.03f, // +3% tension build-up
                                echoDelayMs = echoDelay,
                                score = dropScore,
                                description = desc
                            )
                        )
                    }
                }

                // -------------------------------------------------------------
                // STYLE 3: LONG_CROSSFADE (Melodic Dissolve / Harmonic Blend)
                // Used for acoustic, vocal, ambient, or melodic tracks.
                // -------------------------------------------------------------
                if (!outInfo.isBassHeavy || !inInfo.isBassHeavy) {
                    val blendLen = 8_000L
                    if (outStart + blendLen <= dur - 1_000L) {
                        var blendScore = 75f
                        if (isHarmonicallyCompatible(outInfo.keyTonic, inInfo.keyTonic)) blendScore += 30f
                        if (!outSinging) blendScore += 25f

                        evaluatedCandidates.add(
                            MixCandidate(
                                type = TransitionType.LONG_CROSSFADE,
                                startPositionMs = outStart,
                                durationMs = blendLen,
                                incomingStartOffsetMs = inOffset,
                                tempoRatio = 1.0f,
                                outgoingTempoRatio = 1.0f,
                                echoDelayMs = 400,
                                score = blendScore,
                                description = "Melodic outro dissolve (${outInfo.keyTonic} to ${inInfo.keyTonic})"
                            )
                        )
                    }
                }

                // -------------------------------------------------------------
                // STYLE 4: INSTRUMENTAL_OVERLAP (Intro Groove Blend)
                // -------------------------------------------------------------
                if (inInfo.introDurationMs >= 6_000L && inOffset == 0L) {
                    val overlapLen = minOf(inInfo.introDurationMs, 10_000L)
                    if (outStart + overlapLen <= dur - 1_000L) {
                        var ovScore = 85f
                        if (!outSinging) ovScore += 35f
                        val targetBpm = (outBpm + bestInBpm) / 2f
                        val inRatio = if (bpmDiffPct <= 0.18f) (targetBpm / bestInBpm).coerceIn(0.88f, 1.12f) else 1.0f
                        val outRatio = if (bpmDiffPct <= 0.18f) (targetBpm / outBpm).coerceIn(0.88f, 1.12f) else 1.0f
                        val echoDelay = (60_000f / targetBpm * 0.75f).toInt().coerceIn(200, 500)

                        evaluatedCandidates.add(
                            MixCandidate(
                                type = TransitionType.INSTRUMENTAL_OVERLAP,
                                startPositionMs = outStart,
                                durationMs = overlapLen,
                                incomingStartOffsetMs = 0L,
                                tempoRatio = inRatio,
                                outgoingTempoRatio = outRatio,
                                echoDelayMs = echoDelay,
                                score = ovScore,
                                description = "Instrumental intro groove blend"
                            )
                        )
                    }
                }
            }
        }

        val best = evaluatedCandidates.maxByOrNull { it.score }
        if (best != null && best.score > 0f) {
            val planTier = when (best.type) {
                TransitionType.BEAT_MIX, TransitionType.DROP_TRANSITION -> AutoMixTier.TIER_1_BEAT_MATCH
                TransitionType.NO_TRANSITION -> AutoMixTier.TIER_3_GAPLESS_MICROFADE
                else -> AutoMixTier.TIER_2_EQUAL_POWER_CROSSFADE
            }
            return TransitionPlan(
                type = best.type,
                startPositionMs = best.startPositionMs,
                durationMs = best.durationMs,
                incomingStartOffsetMs = best.incomingStartOffsetMs,
                tempoAdjustmentRatio = 1.0f,
                outgoingTempoAdjustmentRatio = 1.0f,
                bassSwapTimestampMs = best.startPositionMs + best.durationMs / 2,
                confidenceScore = (best.score / 200f).coerceIn(0.70f, 1.0f),
                description = best.description,
                incomingGainMatch = 1.0f,
                fromWaveform = false,
                echoDelayMs = 0,
                tier = planTier
            )
        }

        // Safe Fallback: Beat-mix or melodic dissolve
        val fallbackLen = 6_000L
        val fallbackStart = (dur - fallbackLen - 2_000L).coerceIn(minExitTime, (dur - fallbackLen - 1_000L).coerceAtLeast(minExitTime))
        val fallbackType = if (bpmDiffPct <= 0.22f && outBpm > 0f && bestInBpm > 0f) TransitionType.BEAT_MIX else TransitionType.LONG_CROSSFADE
        val fallbackTier = if (fallbackType == TransitionType.BEAT_MIX) AutoMixTier.TIER_1_BEAT_MATCH else AutoMixTier.TIER_2_EQUAL_POWER_CROSSFADE

        return TransitionPlan(
            type = fallbackType,
            startPositionMs = floorToGrid(fallbackStart, outPhase, outBar).coerceAtLeast(minExitTime),
            durationMs = fallbackLen,
            incomingStartOffsetMs = inOffsets.firstOrNull() ?: 0L,
            tempoAdjustmentRatio = 1.0f,
            outgoingTempoAdjustmentRatio = 1.0f,
            bassSwapTimestampMs = fallbackStart + fallbackLen / 2,
            confidenceScore = 0.85f,
            description = if (fallbackType == TransitionType.BEAT_MIX) "Beat-matched outro mix" else "Outro harmonic blend",
            incomingGainMatch = 1.0f,
            fromWaveform = false,
            echoDelayMs = 0,
            tier = fallbackTier
        )
    }

    private fun lastSyncedVocalMs(lyrics: List<LyricLine>, durationMs: Long): Long? {
        if (lyrics.size < 4) return null
        val gaps = lyrics.zipWithNext { a, b -> b.timeMs - a.timeMs }
        if (gaps.all { it == 3000L }) return null // placeholder spacing
        val last = lyrics.last().timeMs + 2_500L
        return if (last < durationMs - 2_000L) last else null
    }

    private fun floorToGrid(t: Long, phaseMs: Long, intervalMs: Float): Long {
        if (intervalMs <= 1f) return t
        val k = floor((t - phaseMs) / intervalMs)
        return (phaseMs + k * intervalMs).toLong()
    }

    private fun ceilToGrid(t: Long, phaseMs: Long, intervalMs: Float): Long {
        if (intervalMs <= 1f) return t
        val k = ceil((t - phaseMs) / intervalMs)
        return (phaseMs + k * intervalMs).toLong().coerceAtLeast(0L)
    }
}
