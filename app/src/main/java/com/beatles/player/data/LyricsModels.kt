package com.beatles.player.data

data class SyncedWord(
    val text: String,
    val startMs: Long,
    val endMs: Long
)

data class SyncedLyricLine(
    val timeMs: Long,
    val text: String,
    val words: List<SyncedWord>
)

object LyricsProcessor {
    fun processSyncedLyrics(lyrics: List<LyricLine>): List<SyncedLyricLine> {
        return lyrics.mapIndexed { idx, line ->
            val nextTime = lyrics.getOrNull(idx + 1)?.timeMs
            val lineDur = if (nextTime != null && nextTime > line.timeMs) {
                (nextTime - line.timeMs).coerceIn(1200L, 9000L)
            } else {
                4000L
            }
            val rawWords = line.text.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
            if (rawWords.isEmpty()) {
                SyncedLyricLine(line.timeMs, line.text, emptyList())
            } else {
                val totalChars = rawWords.sumOf { it.length }.coerceAtLeast(1)
                var currentStart = line.timeMs
                val words = rawWords.map { w ->
                    val wDur = (lineDur * w.length.toFloat() / totalChars).toLong().coerceAtLeast(150L)
                    val wEnd = currentStart + wDur
                    val sw = SyncedWord(text = w, startMs = currentStart, endMs = wEnd)
                    currentStart = wEnd
                    sw
                }
                SyncedLyricLine(line.timeMs, line.text, words)
            }
        }
    }

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
