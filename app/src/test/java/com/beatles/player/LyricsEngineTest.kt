package com.beatles.player

import com.beatles.player.data.LyricLine
import com.beatles.player.data.LyricsProcessor
import com.beatles.player.data.SyncedWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsEngineTest {

    @Test
    fun testWordProgressCalculation() {
        val word = SyncedWord(text = "Yesterday", startMs = 1000L, endMs = 2000L)

        // Before start
        val progBefore = LyricsProcessor.calculateWordProgress(word, livePositionMs = 500L, isCurrentLine = true, isPastLine = false)
        assertEquals(0.0f, progBefore, 0.001f)

        // Mid-point
        val progMid = LyricsProcessor.calculateWordProgress(word, livePositionMs = 1500L, isCurrentLine = true, isPastLine = false)
        assertEquals(0.5f, progMid, 0.001f)

        // At end
        val progEnd = LyricsProcessor.calculateWordProgress(word, livePositionMs = 2000L, isCurrentLine = true, isPastLine = false)
        assertEquals(1.0f, progEnd, 0.001f)

        // Past line
        val progPast = LyricsProcessor.calculateWordProgress(word, livePositionMs = 500L, isCurrentLine = false, isPastLine = true)
        assertEquals(1.0f, progPast, 0.001f)

        // Future line
        val progFuture = LyricsProcessor.calculateWordProgress(word, livePositionMs = 3000L, isCurrentLine = false, isPastLine = false)
        assertEquals(0.0f, progFuture, 0.001f)
    }

    @Test
    fun testWordStateTransitions() {
        assertEquals("wait", LyricsProcessor.calculateWordState(0.0f))
        assertEquals("sing", LyricsProcessor.calculateWordState(0.05f))
        assertEquals("sing", LyricsProcessor.calculateWordState(0.50f))
        assertEquals("sing", LyricsProcessor.calculateWordState(0.95f))
        assertEquals("done", LyricsProcessor.calculateWordState(1.0f))
    }

    @Test
    fun testSeekResetsAndJumpsProgressCorrectly() {
        val word = SyncedWord(text = "Love", startMs = 2000L, endMs = 3000L)

        // User at 2500ms
        var progress = LyricsProcessor.calculateWordProgress(word, livePositionMs = 2500L, isCurrentLine = true, isPastLine = false)
        assertEquals(0.5f, progress, 0.001f)
        assertEquals("sing", LyricsProcessor.calculateWordState(progress))

        // User scrubs back to 1000ms
        progress = LyricsProcessor.calculateWordProgress(word, livePositionMs = 1000L, isCurrentLine = true, isPastLine = false)
        assertEquals(0.0f, progress, 0.001f)
        assertEquals("wait", LyricsProcessor.calculateWordState(progress))

        // User scrubs forward to 4000ms
        progress = LyricsProcessor.calculateWordProgress(word, livePositionMs = 4000L, isCurrentLine = true, isPastLine = false)
        assertEquals(1.0f, progress, 0.001f)
        assertEquals("done", LyricsProcessor.calculateWordState(progress))
    }

    @Test
    fun testWrappedLineIsolationPerWord() {
        // Line with multiple words that wrap onto multiple rows
        val line = LyricLine(
            timeMs = 1000L,
            text = "Here comes the sun and I say it's all right"
        )
        val nextLine = LyricLine(timeMs = 6000L, text = "Little darling it's been a long cold lonely winter")
        val processed = LyricsProcessor.processSyncedLyrics(listOf(line, nextLine))

        assertEquals(2, processed.size)
        val words = processed[0].words
        assertTrue("Line should have multiple words", words.size >= 8)

        // Mid-way through the 3rd word "the"
        val thirdWord = words[2]
        val midThird = (thirdWord.startMs + thirdWord.endMs) / 2

        // Check word states at midThird:
        // Word 0 ("Here"): should be done
        val prog0 = LyricsProcessor.calculateWordProgress(words[0], midThird, isCurrentLine = true, isPastLine = false)
        assertEquals("done", LyricsProcessor.calculateWordState(prog0))

        // Word 1 ("comes"): should be done
        val prog1 = LyricsProcessor.calculateWordProgress(words[1], midThird, isCurrentLine = true, isPastLine = false)
        assertEquals("done", LyricsProcessor.calculateWordState(prog1))

        // Word 2 ("the"): should be actively singing
        val prog2 = LyricsProcessor.calculateWordProgress(words[2], midThird, isCurrentLine = true, isPastLine = false)
        assertEquals("sing", LyricsProcessor.calculateWordState(prog2))

        // Word 3 ("sun"): must STILL be in wait state (not yet sung)
        val prog3 = LyricsProcessor.calculateWordProgress(words[3], midThird, isCurrentLine = true, isPastLine = false)
        assertEquals("wait", LyricsProcessor.calculateWordState(prog3))

        // Words on next line: must all be in wait state
        val nextLineWords = processed[1].words
        val progNext0 = LyricsProcessor.calculateWordProgress(nextLineWords[0], midThird, isCurrentLine = false, isPastLine = false)
        assertEquals("wait", LyricsProcessor.calculateWordState(progNext0))
    }

    @Test
    fun testCharacterWeightedDurationDistribution() {
        val line = LyricLine(timeMs = 0L, text = "I extraordinary")
        val next = LyricLine(timeMs = 4000L, text = "Next")
        val processed = LyricsProcessor.processSyncedLyrics(listOf(line, next))

        val words = processed[0].words
        assertEquals(2, words.size)
        val shortWordDur = words[0].endMs - words[0].startMs
        val longWordDur = words[1].endMs - words[1].startMs

        assertTrue("Longer word must receive more duration than short single char word", longWordDur > shortWordDur * 5)
    }
}
