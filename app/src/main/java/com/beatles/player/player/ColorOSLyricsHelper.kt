package com.beatles.player.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.beatles.player.data.LyricLine
import com.beatles.player.data.Song
import org.json.JSONObject
import java.util.Locale

object ColorOSLyricsHelper {

    private var lastBroadcastLine = ""
    private var lastBroadcastSongId = ""

    fun formatLrc(lyrics: List<LyricLine>): String {
        if (lyrics.isEmpty()) return ""
        val sb = StringBuilder()
        for (line in lyrics) {
            val min = line.timeMs / 60000
            val sec = (line.timeMs % 60000) / 1000
            val cs = (line.timeMs % 1000) / 10
            sb.append(String.format(Locale.US, "[%02d:%02d.%02d]%s\n", min, sec, cs, line.text))
        }
        return sb.toString()
    }

    fun updateLockscreenLyrics(
        context: Context,
        song: Song,
        lyrics: List<LyricLine>,
        currentPositionMs: Long,
        forceUpdate: Boolean = false
    ) {
        val currentLine = lyrics.lastOrNull { it.timeMs <= currentPositionMs }?.text ?: song.artist
        val isNewSong = song.id != lastBroadcastSongId
        val isNewLine = currentLine != lastBroadcastLine

        if (!forceUpdate && !isNewSong && !isNewLine) {
            return
        }

        lastBroadcastSongId = song.id
        lastBroadcastLine = currentLine

        val lrcText = formatLrc(lyrics)

        // 1. Update MediaSession extras in PlaybackService
        PlaybackService.instance?.updateSessionLyrics(song, lrcText, currentLine)

        // 2. Broadcast to ColorOS / OxygenOS / realme UI SystemUI lockscreen and status bar receivers
        try {
            val jsonPayload = try {
                JSONObject().apply {
                    put("songName", song.title)
                    put("artist", song.artist)
                    put("lyric", lrcText)
                    put("rawLyric", lrcText)
                    put("songId", song.id)
                }.toString()
            } catch (_: Exception) { "" }

            val extras = Bundle().apply {
                putString("id", song.id)
                putString("track", song.title)
                putString("artist", song.artist)
                putString("album", song.album)
                putLong("position", currentPositionMs)
                putLong("duration", song.durationSeconds * 1000L)
                putBoolean("playing", true)
                putString("lyricInfo", jsonPayload)
                putString("lyric", lrcText)
                putString("rawLyric", lrcText)
                putString("com.oplus.media.lyric", lrcText)
                putString("oplus:media:lyric", lrcText)
                putString("com.coloros.media.lyric", lrcText)
                putString("android.media.metadata.LYRIC", lrcText)
                putString("currentLyric", currentLine)
                putBoolean("oplus_support_lyrics", true)
                putBoolean("coloros_support_lyrics", true)
                putBoolean("com.oplus.lockscreen.lyrics_enabled", true)
                putBoolean("coloros_flag_support_live_lyrics", true)
                putBoolean("is_support_lyrics", true)
                putString("com.oplus.music.extra.TITLE", song.title)
                putString("com.oplus.music.extra.ARTIST", song.artist)
                putString("com.oplus.music.extra.ALBUM", song.album)
                putLong("com.oplus.music.extra.DURATION", song.durationSeconds * 1000L)
                putLong("com.oplus.music.extra.POSITION", currentPositionMs)
                putBoolean("com.oplus.music.extra.IS_PLAYING", true)
                putBoolean("com.oplus.music.extra.HAS_LYRICS", lyrics.isNotEmpty())
                putString("com.oplus.music.extra.LYRICS_TEXT", lrcText)
                putString("com.oplus.music.extra.CURRENT_LYRIC", currentLine)
            }

            // OPlus/ColorOS/OxygenOS SystemUI lockscreen broadcasts (MUST NOT restrict to app package)
            context.sendBroadcast(Intent("com.oplus.music.metachanged").apply { putExtras(extras) })
            context.sendBroadcast(Intent("com.oplus.music.playstatechanged").apply { putExtras(extras) })

            // General Android media change broadcasts
            context.sendBroadcast(Intent("com.android.music.metachanged").apply { putExtras(extras) })
            context.sendBroadcast(Intent("com.android.music.playstatechanged").apply { putExtras(extras) })
        } catch (_: Exception) {}
    }
}
