package com.beatles.player.player

import android.content.Context
import android.content.Intent
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    var defaultPlayer: ExoPlayer? = null
        private set
    var secondaryPlayer: ExoPlayer? = null
        private set

    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false

    companion object {
        var instance: PlaybackService? = null
    }

    private fun createPlayer(): ExoPlayer {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15_000, 50_000, 500, 1_000)
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
                        if (playWhenReady) {
                            prepare()
                            play()
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
        
        mediaSession = MediaSession.Builder(this, player).build()
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
            if (target != null && target !is androidx.media3.session.MediaController && mediaSession?.player !== target) {
                mediaSession?.setPlayer(target)
            }
        } catch (e: Throwable) {
            android.util.Log.e("PlaybackService", "Error in setSessionPlayer: ${e.message}")
        }
    }

    fun restoreDefaultPlayer() {
        try {
            defaultPlayer?.let {
                if (mediaSession?.player !== it) {
                    mediaSession?.setPlayer(it)
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("PlaybackService", "Error in restoreDefaultPlayer: ${e.message}")
        }
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
