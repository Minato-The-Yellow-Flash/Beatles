package com.beatles.player.data

data class SyncedWord(
    val text: String,
    val startMs: Long,
    val endMs: Long
)

data class SyncedLyricLine(
    val timeMs: Long,
    val text: String,
    val words: List<SyncedWord>,
    val durationMs: Long = 4000L,
    val isInstrumental: Boolean = false,
    val instrumentalDurationMs: Long = 0L
)

object LyricsProcessor {

    const val MIN_INSTRUMENTAL_GAP_MS = 2500L

    /**
     * Parses raw lyric lines into timed, continuous word-level synced lines.
     * Accurately distinguishes vocal lines from instrumental interludes.
     * Whenever there is an instrumental interlude (intro, inter-verse breaks, outro),
     * inserts an instrumental entry ("♪") with animated music pulses.
     */
    fun processSyncedLyrics(
        lyrics: List<LyricLine>,
        trackDurationMs: Long = 0L
    ): List<SyncedLyricLine> {
        if (lyrics.isEmpty()) {
            return if (trackDurationMs >= MIN_INSTRUMENTAL_GAP_MS) {
                listOf(
                    SyncedLyricLine(
                        timeMs = 0L,
                        text = "♪",
                        words = emptyList(),
                        durationMs = trackDurationMs,
                        isInstrumental = true,
                        instrumentalDurationMs = trackDurationMs
                    )
                )
            } else emptyList()
        }

        val sortedLyrics = lyrics.sortedBy { it.timeMs }
        val result = mutableListOf<SyncedLyricLine>()

        // 1. Intro Instrumental Detection (before first lyric line)
        val firstLineTime = sortedLyrics.first().timeMs
        if (firstLineTime >= MIN_INSTRUMENTAL_GAP_MS) {
            result.add(
                SyncedLyricLine(
                    timeMs = 0L,
                    text = "♪",
                    words = emptyList(),
                    durationMs = firstLineTime,
                    isInstrumental = true,
                    instrumentalDurationMs = firstLineTime
                )
            )
        }

        // 2. Process Lines and detect inter-line instrumental gaps
        for (idx in sortedLyrics.indices) {
            val line = sortedLyrics[idx]
            val nextLine = sortedLyrics.getOrNull(idx + 1)
            val isExplicitInstrumental = line.text.trim().let {
                it == "♪" || it.equals("(instrumental)", ignoreCase = true) || it.equals("instrumental", ignoreCase = true) || it == "//"
            }

            if (isExplicitInstrumental) {
                val instDur = if (nextLine != null && nextLine.timeMs > line.timeMs) {
                    nextLine.timeMs - line.timeMs
                } else {
                    4000L
                }
                result.add(
                    SyncedLyricLine(
                        timeMs = line.timeMs,
                        text = "♪",
                        words = emptyList(),
                        durationMs = instDur,
                        isInstrumental = true,
                        instrumentalDurationMs = instDur
                    )
                )
                continue
            }

            val rawWords = line.text.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
            val naturalVocalDur = (rawWords.size * 420L + 600L).coerceIn(1200L, 4800L)

            val availableGap = if (nextLine != null && nextLine.timeMs > line.timeMs) {
                nextLine.timeMs - line.timeMs
            } else {
                naturalVocalDur
            }

            val hasInstrumentalBreak = availableGap >= naturalVocalDur + MIN_INSTRUMENTAL_GAP_MS
            val lineDur = if (hasInstrumentalBreak) {
                naturalVocalDur
            } else {
                availableGap.coerceIn(1200L, 6500L)
            }

            val words = if (rawWords.isEmpty()) {
                emptyList()
            } else {
                val totalChars = rawWords.sumOf { it.length }.coerceAtLeast(1)
                var currentStart = line.timeMs
                val wordList = mutableListOf<SyncedWord>()
                for (wIdx in rawWords.indices) {
                    val w = rawWords[wIdx]
                    val isLastWord = wIdx == rawWords.size - 1
                    val wDur = if (isLastWord) {
                        (line.timeMs + lineDur - currentStart).coerceAtLeast(150L)
                    } else {
                        (lineDur * w.length.toFloat() / totalChars).toLong().coerceAtLeast(150L)
                    }
                    val wEnd = currentStart + wDur
                    wordList.add(SyncedWord(text = w, startMs = currentStart, endMs = wEnd))
                    currentStart = wEnd
                }
                wordList
            }

            val lineEndMs = if (words.isNotEmpty()) words.last().endMs else (line.timeMs + lineDur)
            result.add(
                SyncedLyricLine(
                    timeMs = line.timeMs,
                    text = line.text,
                    words = words,
                    durationMs = lineDur,
                    isInstrumental = false
                )
            )

            // Check if there is an instrumental break before the next line
            if (nextLine != null) {
                val gapMs = nextLine.timeMs - lineEndMs
                if (gapMs >= MIN_INSTRUMENTAL_GAP_MS) {
                    result.add(
                        SyncedLyricLine(
                            timeMs = lineEndMs,
                            text = "♪",
                            words = emptyList(),
                            durationMs = gapMs,
                            isInstrumental = true,
                            instrumentalDurationMs = gapMs
                        )
                    )
                }
            } else if (trackDurationMs > 0 && (trackDurationMs - lineEndMs) >= MIN_INSTRUMENTAL_GAP_MS) {
                // Outro instrumental break
                val outroDur = trackDurationMs - lineEndMs
                result.add(
                    SyncedLyricLine(
                        timeMs = lineEndMs,
                        text = "♪",
                        words = emptyList(),
                        durationMs = outroDur,
                        isInstrumental = true,
                        instrumentalDurationMs = outroDur
                    )
                )
            }
        }

        return result
    }

    /**
     * Binary-search active line lookup over sorted start times.
     * Guaranteed O(log N) lookup without drift or stale cached indexing.
     * Returns -1 if position is before the first line.
     */
    fun findActiveLineIndex(lines: List<SyncedLyricLine>, positionMs: Long): Int {
        if (lines.isEmpty() || positionMs < lines.first().timeMs) return -1
        var low = 0
        var high = lines.size - 1
        var result = -1
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (lines[mid].timeMs <= positionMs) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }

    /**
     * Calculates continuous sweep progress across all words in a line [0.0 .. words.size].
     * Smooth and continuous at all word boundaries without snaps or discontinuities.
     */
    fun calculateLineSweepProgress(line: SyncedLyricLine, positionMs: Long): Float {
        val words = line.words
        if (words.isEmpty()) return 0f
        if (positionMs <= words.first().startMs) return 0f
        if (positionMs >= words.last().endMs) return words.size.toFloat()

        for (i in words.indices) {
            val word = words[i]
            if (positionMs in word.startMs..word.endMs) {
                val dur = (word.endMs - word.startMs).coerceAtLeast(1L)
                val fraction = (positionMs - word.startMs).toFloat() / dur
                return i.toFloat() + fraction.coerceIn(0f, 1f)
            }
        }
        return words.size.toFloat()
    }

    /**
     * Maps continuous line sweep progress to individual word progress [0.0 .. 1.0].
     */
    fun calculateWordProgressFromLine(wordIndex: Int, lineSweepProgress: Float): Float {
        return (lineSweepProgress - wordIndex).coerceIn(0f, 1f)
    }

    /**
     * Legacy single-word progress evaluator.
     */
    fun calculateWordProgress(
        word: SyncedWord,
        livePositionMs: Long,
        isCurrentLine: Boolean,
        isPastLine: Boolean
    ): Float {
        val duration = (word.endMs - word.startMs).coerceAtLeast(1L)
        return when {
            isPastLine -> 1f
            !isCurrentLine -> 0f
            livePositionMs <= word.startMs -> 0f
            livePositionMs >= word.endMs -> 1f
            else -> ((livePositionMs - word.startMs).toFloat() / duration).coerceIn(0f, 1f)
        }
    }

    fun calculateWordState(progress: Float): String {
        return when {
            progress >= 0.999f -> "done"
            progress > 0.001f -> "sing"
            else -> "wait"
        }
    }
}
