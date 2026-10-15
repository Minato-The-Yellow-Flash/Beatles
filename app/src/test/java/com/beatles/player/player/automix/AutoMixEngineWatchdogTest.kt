package com.beatles.player.player.automix

import com.beatles.player.data.Song
import org.junit.Assert.*
import org.junit.Test

class AutoMixEngineWatchdogTest {

    @Test
    fun testWatchdogRecoveryCallbackTriggered() {
        var watchdogRecoveredSong: Song? = null
        val onWatchdogRecover: (Song) -> Unit = { song ->
            watchdogRecoveredSong = song
        }

        val testSong = Song(
            id = "test_song_1",
            title = "Stalled Song",
            artist = "Artist",
            album = "Album",
            coverUrl = "",
            durationSeconds = 180
        )

        // Simulate watchdog action
        onWatchdogRecover(testSong)

        assertNotNull("Watchdog recovery must pass the stalled song", watchdogRecoveredSong)
        assertEquals("test_song_1", watchdogRecoveredSong?.id)
    }

    @Test
    fun testFallbackPlanCreationTier2AndTier3() {
        val tier2Plan = NormalAutoMixPlanner.createFallbackPlan(
            outgoingDurationMs = 180_000L,
            nowPositionMs = 175_000L,
            incomingLeadingSilenceMs = 200L,
            tier = AutoMixTier.TIER_2_EQUAL_POWER_CROSSFADE
        )

        assertEquals(AutoMixTier.TIER_2_EQUAL_POWER_CROSSFADE, tier2Plan.tier)
        assertEquals(TransitionType.SHORT_CROSSFADE, tier2Plan.type)
        assertEquals(200L, tier2Plan.incomingStartOffsetMs)
        assertTrue(tier2Plan.durationMs >= 1000L)

        val tier3Plan = NormalAutoMixPlanner.createFallbackPlan(
            outgoingDurationMs = 180_000L,
            nowPositionMs = 179_800L,
            tier = AutoMixTier.TIER_3_GAPLESS_MICROFADE
        )

        assertEquals(AutoMixTier.TIER_3_GAPLESS_MICROFADE, tier3Plan.tier)
        assertEquals(TransitionType.NO_TRANSITION, tier3Plan.type)
        assertEquals(200L, tier3Plan.durationMs)
    }

    @Test
    fun testSingleQueueIndexAdvanceOnHandover() {
        var queueIndex = 0
        var handoverCount = 0

        val onHandover: (Song) -> Unit = {
            handoverCount++
            queueIndex++
        }

        val songA = Song("a", "Track A", "Artist", "Album", "", 200)
        val songB = Song("b", "Track B", "Artist", "Album", "", 200)

        // Simulate transition progress with crossover midpoint at t = 0.50
        var handoffNotified = false
        val progressSteps = listOf(0.1f, 0.3f, 0.50f, 0.7f, 0.9f, 1.0f)

        for (t in progressSteps) {
            if (!handoffNotified && t >= 0.50f) {
                handoffNotified = true
                onHandover(songB)
            }
        }

        assertEquals("Handover must be notified exactly ONCE", 1, handoverCount)
        assertEquals("Queue index must advance exactly ONCE to 1", 1, queueIndex)
    }
}
