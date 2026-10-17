package com.beatles.player.player

import android.content.Context
import android.content.Intent
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class BeatlesForwardingPlayer(player: Player) : ForwardingPlayer(player) {
    override fun getAvailableCommands(): Player.Commands {
        return super.getAvailableCommands().buildUpon()
            .add(Player.COMMAND_SEEK_TO_NEXT)
            .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            .build()
    }

    override fun isCommandAvailable(command: Int): Boolean {
        return command == Player.COMMAND_SEEK_TO_NEXT ||
               command == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM ||
               command == Player.COMMAND_SEEK_TO_PREVIOUS ||
               command == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM ||
               super.isCommandAvailable(command)
    }

    override fun seekToNext() {
        AudioPlayerManager.onExternalSkipNext?.invoke() ?: super.seekToNext()
    }

    override fun seekToNextMediaItem() {
        AudioPlayerManager.onExternalSkipNext?.invoke() ?: super.seekToNextMediaItem()
    }

    override fun seekToPrevious() {
        AudioPlayerManager.onExternalSkipPrevious?.invoke() ?: super.seekToPrevious()
    }

    override fun seekToPreviousMediaItem() {
        AudioPlayerManager.onExternalSkipPrevious?.invoke() ?: super.seekToPreviousMediaItem()
    }
}

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    var defaultPlayer: ExoPlayer? = null
        private set
    var secondaryPlayer: ExoPlayer? = null
        private set
    private var forwardingPlayer: BeatlesForwardingPlayer? = null

    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false

    companion object {
        var instance: PlaybackService? = null
    }

    private fun createPlayer(): ExoPlayer {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15_000, 50_000, 250, 750)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(BeatlesMediaCache.createDataSourceFactory(this))
        return ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ false
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build().apply {
                addListener(object : androidx.media3.common.Player.Listener {
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        android.util.Log.e("PlaybackService", "ExoPlayer error: ${error.errorCodeName} - ${error.message}")
                        currentMediaItem?.mediaId?.let { id ->
                            com.beatles.player.data.MusicRepository.clearStreamCache(id)
                        }
                    }
                })
            }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        val player = createPlayer()
        defaultPlayer = player
        secondaryPlayer = createPlayer().apply { volume = 0f }
        
        val fPlayer = BeatlesForwardingPlayer(player)
        forwardingPlayer = fPlayer
        mediaSession = MediaSession.Builder(this, fPlayer).build()
        ensureAudioFocus()
    }

    fun ensureAudioFocus(): Boolean {
        if (hasAudioFocus) return true
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange ->
                    handleAudioFocusChange(focusChange)
                }
                .build()
            audioFocusRequest = req
            audioManager.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                { focusChange -> handleAudioFocusChange(focusChange) },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasAudioFocus
    }

    fun abandonAudioFocus() {
        if (!hasAudioFocus) return
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
        hasAudioFocus = false
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        val active = mediaSession?.player ?: defaultPlayer
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                active?.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                active?.volume = 0.2f
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                active?.volume = 1.0f
                active?.play()
            }
        }
    }

    fun setSessionPlayer(player: androidx.media3.common.Player?) {
        try {
            val target = player ?: defaultPlayer
            if (target != null && target !is androidx.media3.session.MediaController) {
                val currentWrapped = (mediaSession?.player as? BeatlesForwardingPlayer)?.wrappedPlayer
                if (currentWrapped !== target) {
                    val fPlayer = BeatlesForwardingPlayer(target)
                    forwardingPlayer = fPlayer
                    mediaSession?.setPlayer(fPlayer)
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("PlaybackService", "Error in setSessionPlayer: ${e.message}")
        }
    }

    fun restoreDefaultPlayer() {
        try {
            defaultPlayer?.let { def ->
                val currentWrapped = (mediaSession?.player as? BeatlesForwardingPlayer)?.wrappedPlayer
                if (currentWrapped !== def) {
                    val fPlayer = BeatlesForwardingPlayer(def)
                    forwardingPlayer = fPlayer
                    mediaSession?.setPlayer(fPlayer)
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("PlaybackService", "Error in restoreDefaultPlayer: ${e.message}")
        }
    }

    fun updateSessionLyrics(song: com.beatles.player.data.Song, lrcText: String, currentLine: String) {
        val extras = android.os.Bundle().apply {
            val jsonPayload = try {
                org.json.JSONObject().apply {
                    put("songName", song.title)
                    put("artist", song.artist)
                    put("lyric", lrcText)
                    put("rawLyric", lrcText)
                    put("songId", song.id)
                }.toString()
            } catch (_: Exception) { "" }
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
        try {
            mediaSession?.setSessionExtras(extras)
        } catch (_: Throwable) {}
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        if (instance == this) instance = null
        abandonAudioFocus()
        defaultPlayer?.release()
        defaultPlayer = null
        secondaryPlayer?.release()
        secondaryPlayer = null
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
