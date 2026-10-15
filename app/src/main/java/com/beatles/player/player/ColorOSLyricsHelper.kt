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
            }

            // OPlus specific broadcast
            val oplusIntent = Intent("com.oplus.music.metachanged").apply {
                putExtras(extras)
                setPackage(context.packageName)
            }
            context.sendBroadcast(oplusIntent)

            // General Android media change broadcast with lyric payload
            val generalIntent = Intent("com.android.music.metachanged").apply {
                putExtras(extras)
            }
            context.sendBroadcast(generalIntent)
        } catch (_: Exception) {}
    }
}
