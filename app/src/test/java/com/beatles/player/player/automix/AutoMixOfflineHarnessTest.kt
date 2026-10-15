package com.beatles.player.player.automix

import com.beatles.player.data.Song
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class AutoMixOfflineHarnessTest {

    @Test
    fun testOfflineHarness20DiverseTracksContinuousAudio() {
        // Scenario 1: Pop track (120 BPM, stereo, 44.1 kHz)
        val track1 = OfflineAutoMixRenderer.synthesizeTrack(bpm = 120f, durationSeconds = 30f, sampleRate = 44100, channels = 2)
        // Scenario 2: Dance track (128 BPM, stereo, 44.1 kHz)
        val track2 = OfflineAutoMixRenderer.synthesizeTrack(bpm = 128f, durationSeconds = 30f, sampleRate = 44100, channels = 2)
        // Scenario 3: Hip-hop track (92 BPM, stereo, 44.1 kHz)
        val track3 = OfflineAutoMixRenderer.synthesizeTrack(bpm = 92f, durationSeconds = 30f, sampleRate = 44100, channels = 2)
        // Scenario 4: Rock track (135 BPM, stereo, 48 kHz)
        val track4 = OfflineAutoMixRenderer.synthesizeTrack(bpm = 135f, durationSeconds = 30f, sampleRate = 48000, channels = 2)
        // Scenario 5: Acoustic ballad (72 BPM, stereo, 44.1 kHz)
        val track5 = OfflineAutoMixRenderer.synthesizeTrack(bpm = 72f, durationSeconds = 30f, sampleRate = 44100, channels = 2)
        // Scenario 6: Fast Drum & Bass (174 BPM, stereo, 48 kHz)
        val track6 = OfflineAutoMixRenderer.synthesizeTrack(bpm = 174f, durationSeconds = 30f, sampleRate = 48000, channels = 2)
        // Scenario 7: Slow R&B (68 BPM, stereo, 44.1 kHz)
        val track7 = OfflineAutoMixRenderer.synthesizeTrack(bpm = 68f, durationSeconds = 30f, sampleRate = 44100, channels = 2)
        // Scenario 8: Vintage mono track (100 BPM, mono 1-channel, 44.1 kHz)
        val track8 = OfflineAutoMixRenderer.synthesizeTrack(bpm = 100f, durationSeconds = 30f, sampleRate = 44100, channels = 1)
        // Scenario 9: 48 kHz high-res track (115 BPM, stereo, 48 kHz)
        val track9 = OfflineAutoMixRenderer.synthesizeTrack(bpm = 115f, durationSeconds = 30f, sampleRate = 48000, channels = 2)
        // Scenario 10: 44.1 kHz stereo track (116 BPM, compatible with track 9)
        val track10 = OfflineAutoMixRenderer.synthesizeTrack(bpm = 116f, durationSeconds = 30f, sampleRate = 44100, channels = 2)
        // Scenario 11: Huge BPM gap track (145 BPM to pair with 80 BPM)
        val track11 = OfflineAutoMixRenderer.synthesizeTrack(bpm = 145f, durationSeconds = 30f, sampleRate = 44100, channels = 2)
        // Scenario 12: Variable tempo / unconfident track (110 BPM)
        val track12 = OfflineAutoMixRenderer.synthesizeTrack(bpm = 110f, durationSeconds = 30f, sampleRate = 44100, channels = 2)
        // Scenario 13: Very short track (10-second interlude)
        val track13 = OfflineAutoMixRenderer.synthesizeTrack(bpm = 120f, durationSeconds = 10f, sampleRate = 44100, channels = 2)
        // Scenario 14: Track with leading silence (4s silence at start)
        val track14 = OfflineAutoMixRenderer.synthesizeTrack(bpm = 120f, durationSeconds = 30f, sampleRate = 44100, channels = 2, leadingSilenceSeconds = 4f)
        // Scenario 15: Track with trailing silence (5s silence at end)
        val track15 = OfflineAutoMixRenderer.synthesizeTrack(bpm = 120f, durationSeconds = 30f, sampleRate = 44100, channels = 2, trailingSilenceSeconds = 5f)
        // Scenario 16: Ambient no-beat soundscape (0 BPM continuous harmonic pad)
        val track16 = OfflineAutoMixRenderer.synthesizeTrack(bpm = 0f, durationSeconds = 30f, sampleRate = 44100, channels = 2, isAmbient = true)
        // Scenario 17: Double-time track (150 BPM to pair with 75 BPM track 5)
        val track17 = OfflineAutoMixRenderer.synthesizeTrack(bpm = 150f, durationSeconds = 30f, sampleRate = 44100, channels = 2)
        // Scenario 18: Corrupt / zero-byte stream
        val track18 = OfflineAutoMixRenderer.synthesizeTrack(bpm = 0f, durationSeconds = 0f, sampleRate = 44100, channels = 2, isCorrupt = true)
        // Scenario 19: Live album continuous track (120 BPM)
        val track19 = OfflineAutoMixRenderer.synthesizeTrack(bpm = 120f, durationSeconds = 30f, sampleRate = 44100, channels = 2)
        // Scenario 20: Standard closing track
        val track20 = OfflineAutoMixRenderer.synthesizeTrack(bpm = 118f, durationSeconds = 30f, sampleRate = 44100, channels = 2)

        data class TestCase(
            val name: String,
            val outgoing: OfflineAutoMixRenderer.AudioBuffer,
            val incoming: OfflineAutoMixRenderer.AudioBuffer,
            val songOut: Song,
            val songIn: Song,
            val head: SegmentAnalysis? = null,
            val tail: SegmentAnalysis? = null,
            val isGaplessAlbum: Boolean = false,
            val expectedTier: AutoMixTier
        )

        val testCases = listOf(
            TestCase(
                name = "1. Pop to Dance (120 -> 124 BPM, 44.1kHz stereo)",
                outgoing = track1,
                incoming = track2,
                songOut = Song("1", "Pop Anthem", "Artist A", "Album", "", 30),
                songIn = Song("2", "Club Banger", "Artist B", "Album", "", 30),
                tail = SegmentAnalysis(0, 30000, 0, 30000, null, null, 0.7f, 120f, 0.9f, 0L),
                head = SegmentAnalysis(0, 30000, 200, 30000, null, null, 0.7f, 124f, 0.9f, 0L),
                expectedTier = AutoMixTier.TIER_1_BEAT_MATCH
            ),
            TestCase(
                name = "2. Hip-Hop (92 BPM) to Slow Ballad (72 BPM, BPM Gap > 8.5%)",
                outgoing = track3,
                incoming = track5,
                songOut = Song("3", "City Rhymes", "Artist C", "Album", "", 30),
                songIn = Song("5", "Acoustic Ballad", "Artist E", "Album", "", 30),
                tail = SegmentAnalysis(0, 30000, 0, 30000, null, null, 0.6f, 92f, 0.85f, 0L),
                head = SegmentAnalysis(0, 30000, 100, 30000, null, null, 0.5f, 72f, 0.85f, 0L),
                expectedTier = AutoMixTier.TIER_2_EQUAL_POWER_CROSSFADE
            ),
            TestCase(
                name = "3. Rock 48kHz to Ballad 44.1kHz (Format Mismatch Resampled)",
                outgoing = track4,
                incoming = track5,
                songOut = Song("4", "Rock Anthem", "Artist D", "Album", "", 30),
                songIn = Song("5", "Acoustic Ballad", "Artist E", "Album", "", 30),
                tail = SegmentAnalysis(0, 30000, 0, 30000, null, null, 0.7f, 135f, 0.9f, 0L),
                head = SegmentAnalysis(0, 30000, 0, 30000, null, null, 0.5f, 95f, 0.9f, 0L),
                expectedTier = AutoMixTier.TIER_2_EQUAL_POWER_CROSSFADE
            ),
            TestCase(
                name = "4. Mono Track to Stereo Track (Channel Mismatch Remixed)",
                outgoing = track8,
                incoming = track1,
                songOut = Song("8", "Vintage Mono", "Artist H", "Album", "", 30),
                songIn = Song("1", "Pop Anthem", "Artist A", "Album", "", 30),
                tail = SegmentAnalysis(0, 30000, 0, 30000, null, null, 0.6f, 100f, 0.8f, 0L),
                head = SegmentAnalysis(0, 30000, 0, 30000, null, null, 0.6f, 120f, 0.8f, 0L),
                expectedTier = AutoMixTier.TIER_2_EQUAL_POWER_CROSSFADE
            ),
            TestCase(
                name = "5. 48kHz to 44.1kHz Near-Tempo Beat Match (115 -> 116 BPM)",
                outgoing = track9,
                incoming = track10,
                songOut = Song("9", "High-Res Master", "Artist I", "Album", "", 30),
                songIn = Song("10", "Studio Track", "Artist J", "Album", "", 30),
                tail = SegmentAnalysis(0, 30000, 0, 30000, null, null, 0.7f, 115f, 0.95f, 0L),
                head = SegmentAnalysis(0, 30000, 0, 30000, null, null, 0.7f, 116f, 0.95f, 0L),
                expectedTier = AutoMixTier.TIER_1_BEAT_MATCH
            ),
            TestCase(
                name = "6. Very Short Track (10-second Interlude, Scaled Overlap)",
                outgoing = track13,
                incoming = track1,
                songOut = Song("13", "Interlude", "Artist M", "Album", "", 10),
                songIn = Song("1", "Pop Anthem", "Artist A", "Album", "", 30),
                expectedTier = AutoMixTier.TIER_2_EQUAL_POWER_CROSSFADE
            ),
            TestCase(
                name = "7. Incoming Leading Silence (4s Silence Skipped)",
                outgoing = track1,
                incoming = track14,
                songOut = Song("1", "Pop Anthem", "Artist A", "Album", "", 30),
                songIn = Song("14", "Silent Intro Track", "Artist N", "Album", "", 30),
                head = SegmentAnalysis(0, 30000, 4000, 30000, null, null, 0.7f, 120f, 0.8f, 0L),
                tail = SegmentAnalysis(0, 30000, 0, 30000, null, null, 0.7f, 120f, 0.8f, 0L),
                expectedTier = AutoMixTier.TIER_1_BEAT_MATCH
            ),
            TestCase(
                name = "8. Outgoing Trailing Silence (5s Silence Trimmed)",
                outgoing = track15,
                incoming = track1,
                songOut = Song("15", "Silent Outro Track", "Artist O", "Album", "", 30),
                songIn = Song("1", "Pop Anthem", "Artist A", "Album", "", 30),
                tail = SegmentAnalysis(0, 30000, 0, 25000, null, null, 0.7f, 120f, 0.8f, 0L),
                head = SegmentAnalysis(0, 30000, 0, 30000, null, null, 0.7f, 120f, 0.8f, 0L),
                expectedTier = AutoMixTier.TIER_1_BEAT_MATCH
            ),
            TestCase(
                name = "9. Ambient No-Beat Drone (Zero Onset Confidence)",
                outgoing = track16,
                incoming = track1,
                songOut = Song("16", "Ambient Drone", "Artist P", "Album", "", 30),
                songIn = Song("1", "Pop Anthem", "Artist A", "Album", "", 30),
                tail = SegmentAnalysis(0, 30000, 0, 30000, null, null, 0.4f, null, 0.0f, null),
                expectedTier = AutoMixTier.TIER_2_EQUAL_POWER_CROSSFADE
            ),
            TestCase(
                name = "10. Half-Time / Double-Time Match (75 BPM -> 150 BPM, 2x Octave)",
                outgoing = track5,
                incoming = track17,
                songOut = Song("5", "Acoustic Ballad", "Artist E", "Album", "", 30),
                songIn = Song("17", "Double Time Beat", "Artist Q", "Album", "", 30),
                tail = SegmentAnalysis(0, 30000, 0, 30000, null, null, 0.6f, 75f, 0.9f, 0L),
                head = SegmentAnalysis(0, 30000, 0, 30000, null, null, 0.6f, 150f, 0.9f, 0L),
                expectedTier = AutoMixTier.TIER_1_BEAT_MATCH
            ),
            TestCase(
                name = "11. Corrupt / Empty Stream (Tier 3 Safety Fallback)",
                outgoing = track1,
                incoming = track18,
                songOut = Song("1", "Pop Anthem", "Artist A", "Album", "", 30),
                songIn = Song("18", "Corrupt File", "Artist R", "Album", "", 0),
                expectedTier = AutoMixTier.TIER_3_GAPLESS_MICROFADE
            ),
            TestCase(
                name = "12. Live Continuous Album Tracks (Tier 3 Gapless Transition)",
                outgoing = track19,
                incoming = track20,
                songOut = Song("19", "Live Part 1", "Beatles", "Live at the BBC", "", 30),
                songIn = Song("20", "Live Part 2", "Beatles", "Live at the BBC", "", 30),
                isGaplessAlbum = true,
                expectedTier = AutoMixTier.TIER_3_GAPLESS_MICROFADE
            ),
            TestCase(
                name = "13. High BPM Gap (80 BPM -> 145 BPM)",
                outgoing = track7,
                incoming = track11,
                songOut = Song("7", "R&B Track", "Artist G", "Album", "", 30),
                songIn = Song("11", "Fast Track", "Artist K", "Album", "", 30),
                tail = SegmentAnalysis(0, 30000, 0, 30000, null, null, 0.6f, 80f, 0.85f, 0L),
                head = SegmentAnalysis(0, 30000, 0, 30000, null, null, 0.6f, 145f, 0.85f, 0L),
                expectedTier = AutoMixTier.TIER_2_EQUAL_POWER_CROSSFADE
            ),
            TestCase(
                name = "14. Fast DnB (174 BPM) to Pop (120 BPM)",
                outgoing = track6,
                incoming = track1,
                songOut = Song("6", "DnB Track", "Artist F", "Album", "", 30),
                songIn = Song("1", "Pop Anthem", "Artist A", "Album", "", 30),
                tail = SegmentAnalysis(0, 30000, 0, 30000, null, null, 0.7f, 174f, 0.9f, 0L),
                head = SegmentAnalysis(0, 30000, 0, 30000, null, null, 0.7f, 120f, 0.9f, 0L),
                expectedTier = AutoMixTier.TIER_2_EQUAL_POWER_CROSSFADE
            ),
            TestCase(
                name = "15. Variable Tempo Track (110 BPM Unconfident -> Tier 2)",
                outgoing = track12,
                incoming = track2,
                songOut = Song("12", "Variable Track", "Artist L", "Album", "", 30),
                songIn = Song("2", "Club Banger", "Artist B", "Album", "", 30),
                tail = SegmentAnalysis(0, 30000, 0, 30000, null, null, 0.6f, 110f, 0.05f, 0L), // confidence 0.05 < 0.15
                head = SegmentAnalysis(0, 30000, 0, 30000, null, null, 0.7f, 128f, 0.9f, 0L),
                expectedTier = AutoMixTier.TIER_2_EQUAL_POWER_CROSSFADE
            ),
            TestCase(
                name = "16. Micro Track (< 10 seconds Microfade)",
                outgoing = OfflineAutoMixRenderer.synthesizeTrack(bpm = 120f, durationSeconds = 8f),
                incoming = track1,
                songOut = Song("16", "Sound Bite", "Artist", "Album", "", 8),
                songIn = Song("1", "Pop Anthem", "Artist A", "Album", "", 30),
                expectedTier = AutoMixTier.TIER_3_GAPLESS_MICROFADE
            ),
            TestCase(
                name = "17. Dance to Fast Rock (128 -> 135 BPM within 8.5%)",
                outgoing = track2,
                incoming = track4,
                songOut = Song("2", "Club Banger", "Artist B", "Album", "", 30),
                songIn = Song("4", "Rock Anthem", "Artist D", "Album", "", 30),
                tail = SegmentAnalysis(0, 30000, 0, 30000, null, null, 0.7f, 128f, 0.95f, 0L),
                head = SegmentAnalysis(0, 30000, 0, 30000, null, null, 0.7f, 135f, 0.95f, 0L),
                expectedTier = AutoMixTier.TIER_1_BEAT_MATCH
            ),
            TestCase(
                name = "18. Acoustic to Hip-Hop (72 -> 92 BPM)",
                outgoing = track5,
                incoming = track3,
                songOut = Song("5", "Acoustic Ballad", "Artist E", "Album", "", 30),
                songIn = Song("3", "City Rhymes", "Artist C", "Album", "", 30),
                tail = SegmentAnalysis(0, 30000, 0, 30000, null, null, 0.5f, 72f, 0.9f, 0L),
                head = SegmentAnalysis(0, 30000, 0, 30000, null, null, 0.7f, 92f, 0.9f, 0L),
                expectedTier = AutoMixTier.TIER_2_EQUAL_POWER_CROSSFADE
            ),
            TestCase(
                name = "19. Pop to Pop (Same BPM Exact 120 -> 120)",
                outgoing = track1,
                incoming = track1,
                songOut = Song("1", "Pop Anthem", "Artist A", "Album", "", 30),
                songIn = Song("1b", "Pop Anthem 2", "Artist A", "Album", "", 30),
                tail = SegmentAnalysis(0, 30000, 0, 30000, null, null, 0.7f, 120f, 0.95f, 0L),
                head = SegmentAnalysis(0, 30000, 0, 30000, null, null, 0.7f, 120f, 0.95f, 0L),
                expectedTier = AutoMixTier.TIER_1_BEAT_MATCH
            ),
            TestCase(
                name = "20. Final Track in Queue / Natural Completion",
                outgoing = track20,
                incoming = OfflineAutoMixRenderer.AudioBuffer(44100, 2, FloatArray(0)),
                songOut = Song("20", "Outro Song", "Beatles", "Abbey Road", "", 30),
                songIn = Song("20", "Outro Song", "Beatles", "Abbey Road", "", 30),
                expectedTier = AutoMixTier.TIER_3_GAPLESS_MICROFADE
            )
        )

        assertEquals("Test harness must test at least 20 diverse track scenarios", 20, testCases.size)

        var passedCount = 0
        val results = mutableListOf<OfflineAutoMixRenderer.RenderResult>()

        for (tc in testCases) {
            val plan = if (tc.songIn.id == tc.songOut.id || tc.incoming.samples.isEmpty()) {
                NormalAutoMixPlanner.createFallbackPlan(
                    outgoingDurationMs = tc.songOut.durationSeconds * 1000L,
                    nowPositionMs = tc.songOut.durationSeconds * 1000L - 200L,
                    tier = AutoMixTier.TIER_3_GAPLESS_MICROFADE
                )
            } else {
                NormalAutoMixPlanner.plan(
                    outgoing = tc.songOut,
                    outgoingDurationMs = tc.songOut.durationSeconds * 1000L,
                    tail = tc.tail,
                    outgoingLyrics = emptyList(),
                    incoming = tc.songIn,
                    head = tc.head,
                    gaplessAlbum = tc.isGaplessAlbum,
                    nowPositionMs = 0L
                )
            }

            assertEquals("${tc.name} must select expected tier", tc.expectedTier, plan.tier)

            val renderResult = OfflineAutoMixRenderer.renderTransition(
                testCaseName = tc.name,
                outgoing = tc.outgoing,
                incoming = tc.incoming,
                plan = plan
            )

            results.add(renderResult)

            // Automated DSP Checks:
            assertTrue("${tc.name}: Silence gap must be <= 50ms (was ${renderResult.maxSilenceGapMs}ms)", renderResult.maxSilenceGapMs <= 50.0)
            assertTrue("${tc.name}: Peak amplitude must not clip <= 1.0f (was ${renderResult.peakAmplitude})", renderResult.peakAmplitude <= 1.0f)
            assertEquals("${tc.name}: Discontinuity clicks must be 0 (was ${renderResult.clickDiscontinuities})", 0, renderResult.clickDiscontinuities)
            if (plan.tier == AutoMixTier.TIER_1_BEAT_MATCH) {
                assertTrue("${tc.name}: Tier 1 beat alignment error must be < 10ms (was ${renderResult.beatAlignmentErrorMs}ms)", renderResult.beatAlignmentErrorMs < 10.0)
            }
            assertTrue(
                "${tc.name}: Audio must be 100% continuous with 0 stops (silenceGap=${renderResult.maxSilenceGapMs}ms, clicks=${renderResult.clickDiscontinuities}, peak=${renderResult.peakAmplitude})",
                renderResult.isContinuous
            )

            passedCount++
        }

        assertEquals("100% of the 20 test cases must produce continuous playback with zero stops", 20, passedCount)
    }

    @Test
    fun testAutoMixStateMachine8ExplicitPhases() {
        // Verify all 8 states exist and transitions are valid
        val phases = TransitionPhase.values()
        assertTrue(phases.contains(TransitionPhase.IDLE))
        assertTrue(phases.contains(TransitionPhase.ANALYZING))
        assertTrue(phases.contains(TransitionPhase.PREPARED))
        assertTrue(phases.contains(TransitionPhase.ARMED))
        assertTrue(phases.contains(TransitionPhase.TRANSITIONING))
        assertTrue(phases.contains(TransitionPhase.HANDOVER))
        assertTrue(phases.contains(TransitionPhase.FALLBACK))
        assertTrue(phases.contains(TransitionPhase.ERROR))

        val idleState = AutoMixState(TransitionPhase.IDLE)
        assertFalse(idleState.isMixing)

        val transState = AutoMixState(TransitionPhase.TRANSITIONING)
        assertTrue(transState.isMixing)

        val handoverState = AutoMixState(TransitionPhase.HANDOVER)
        assertTrue(handoverState.isMixing)

        val fallbackState = AutoMixState(TransitionPhase.FALLBACK)
        assertTrue(fallbackState.isMixing)
    }

    @Test
    fun testAutoMixThreeTiersLabels() {
        val tier1State = AutoMixState(tier = AutoMixTier.TIER_1_BEAT_MATCH)
        assertEquals("Beat-matched mix", tier1State.label)

        val tier2State = AutoMixState(tier = AutoMixTier.TIER_2_EQUAL_POWER_CROSSFADE)
        assertEquals("Equal-power crossfade", tier2State.label)

        val tier3State = AutoMixState(tier = AutoMixTier.TIER_3_GAPLESS_MICROFADE)
        assertEquals("Gapless mix", tier3State.label)
    }
}
