package com.beatles.player.player
import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.beatles.player.data.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

class AudioPlayerManager(private val context: Context) {
    var player: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    var onPlaybackStateChanged: ((Boolean) -> Unit)? = null
    var onPositionChanged: ((Long, Long) -> Unit)? = null
    var onSongTransition: ((String) -> Unit)? = null
    var onShuffleModeEnabledChanged: ((Boolean) -> Unit)? = null
    var onRepeatModeChanged: ((Int) -> Unit)? = null
    var onSongEnded: (() -> Unit)? = null
    var onPlayerError: ((Throwable) -> Unit)? = null
    private var pendingPlayAction: (() -> Unit)? = null

    init {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                player = controllerFuture?.get()
                player?.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (activePlayerOverride == null) {
                            onPlaybackStateChanged?.invoke(isPlaying)
                        }
                    }
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        // Handled centrally by startPolling() on activePlayer to prevent deck-switch race conditions
                    }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        onPlayerError?.invoke(error)
                    }
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        if (activePlayerOverride == null) {
                            mediaItem?.mediaId?.let { onSongTransition?.invoke(it) }
                        }
                    }
                    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) { onShuffleModeEnabledChanged?.invoke(shuffleModeEnabled) }
                    override fun onRepeatModeChanged(repeatMode: Int) { onRepeatModeChanged?.invoke(repeatMode) }
                })
                pendingPlayAction?.invoke()
                pendingPlayAction = null
                startPolling()
            } catch (e: Exception) {
                android.util.Log.e("AudioPlayerManager", "Error connecting controller: ${e.message}")
            }
        }, MoreExecutors.directExecutor())
    }
    var activePlayerOverride: Player? = null
        set(value) {
            field = value
            lastPlaybackState = null
            try {
                if (value != null && value !is MediaController) {
                    PlaybackService.instance?.setSessionPlayer(value)
                } else {
                    PlaybackService.instance?.restoreDefaultPlayer()
                }
            } catch (e: Throwable) {
                android.util.Log.e("AudioPlayerManager", "Failed to update session player: ${e.message}")
            }
        }

    val activePlayer: Player? get() = activePlayerOverride ?: PlaybackService.instance?.defaultPlayer ?: player
    val isPlaying: Boolean get() = try { activePlayer?.isPlaying == true } catch (_: Throwable) { false }

    private var lastIsPlaying: Boolean? = null
    private var lastPlaybackState: Int? = null

    private fun startPolling() {
        CoroutineScope(Dispatchers.Main.immediate).launch {
            while (isActive) {
                try {
                    val p = activePlayer
                    val currentlyPlaying = p?.isPlaying == true
                    if (currentlyPlaying != lastIsPlaying) {
                        lastIsPlaying = currentlyPlaying
                        onPlaybackStateChanged?.invoke(currentlyPlaying)
                    }
                    val state = p?.playbackState
                    if (state != lastPlaybackState) {
                        lastPlaybackState = state
                        if (state == Player.STATE_ENDED) {
                            val pos = p?.currentPosition ?: 0L
                            val dur = p?.duration ?: 0L
                            if (dur <= 0L || (dur - pos <= 4000L)) {
                                onSongEnded?.invoke()
                            }
                        }
                    }
                    val pos = p?.currentPosition ?: 0L
                    val dur = p?.duration ?: 0L
                    val safeDur = if (dur == androidx.media3.common.C.TIME_UNSET) 0L else dur
                    if (currentlyPlaying || (pos > 0L && safeDur > 0L)) {
                        onPositionChanged?.invoke(pos, safeDur)
                    }
                } catch (_: Throwable) {}
                delay(100)
            }
        }
    }

    private fun buildMediaItem(streamUrl: String, song: Song): MediaItem {
        val artUri = if (song.coverUrl.isNotBlank()) {
            try { android.net.Uri.parse(song.coverUrl) } catch (_: Exception) { null }
        } else null
        val extras = android.os.Bundle().apply {
            putBoolean("oplus_support_lyrics", true)
            putBoolean("coloros_support_lyrics", true)
            putBoolean("com.oplus.lockscreen.lyrics_enabled", true)
            putBoolean("coloros_flag_support_live_lyrics", true)
            putBoolean("is_support_lyrics", true)
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .setDisplayTitle(song.title)
            .setSubtitle(song.artist)
            .setExtras(extras)
            .apply { if (artUri != null) setArtworkUri(artUri) }
            .build()
        return MediaItem.Builder().setMediaId(song.id).setUri(streamUrl).setMediaMetadata(metadata).build()
    }

    fun play(streamUrl: String, song: Song) {
        lastPlaybackState = null
        PlaybackService.instance?.ensureAudioFocus()
        activePlayerOverride?.stop()
        activePlayerOverride?.clearMediaItems()
        activePlayerOverride = null
        PlaybackService.instance?.restoreDefaultPlayer()

        val mediaItem = buildMediaItem(streamUrl, song)
        val target = PlaybackService.instance?.defaultPlayer ?: player
        if (target != null) {
            target.stop()
            target.clearMediaItems()
            target.volume = 1.0f
            target.setMediaItem(mediaItem)
            target.prepare()
            target.play()
        } else {
            pendingPlayAction = { play(streamUrl, song) }
        }
    }

    fun playWithFadeIn(streamUrl: String, song: Song, fadeInMs: Long = 1800L) {
        lastPlaybackState = null
        PlaybackService.instance?.ensureAudioFocus()
        activePlayerOverride?.stop()
        activePlayerOverride?.clearMediaItems()
        activePlayerOverride = null
        PlaybackService.instance?.restoreDefaultPlayer()

        val mediaItem = buildMediaItem(streamUrl, song)
        val target = PlaybackService.instance?.defaultPlayer ?: player
        if (target != null) {
            target.stop()
            target.clearMediaItems()
            target.volume = 0f
            target.setMediaItem(mediaItem)
            target.prepare()
            target.play()
            fadeVolume(1.0f, fadeInMs)
        } else {
            pendingPlayAction = { playWithFadeIn(streamUrl, song, fadeInMs) }
        }
    }

    fun playAt(streamUrl: String, song: Song, startPositionMs: Long) {
        lastPlaybackState = null
        PlaybackService.instance?.ensureAudioFocus()
        activePlayerOverride?.stop()
        activePlayerOverride?.clearMediaItems()
        activePlayerOverride = null
        PlaybackService.instance?.restoreDefaultPlayer()

        val mediaItem = buildMediaItem(streamUrl, song)
        val target = PlaybackService.instance?.defaultPlayer ?: player
        if (target != null) {
            target.stop()
            target.clearMediaItems()
            target.volume = 1.0f
            target.setMediaItem(mediaItem, startPositionMs.coerceAtLeast(0L))
            target.prepare()
            target.play()
        } else {
            pendingPlayAction = { playAt(streamUrl, song, startPositionMs) }
        }
    }

    fun fadeVolume(targetVolume: Float, durationMs: Long, onDone: (() -> Unit)? = null) {
        val p = activePlayer ?: return
        val startVol = p.volume
        val steps = 18
        val interval = (durationMs / steps).coerceAtLeast(10L)
        val delta = (targetVolume - startVol) / steps
        CoroutineScope(Dispatchers.Main).launch {
            for (i in 1..steps) {
                p.volume = (startVol + delta * i).coerceIn(0f, 1f)
                delay(interval)
            }
            p.volume = targetVolume
            onDone?.invoke()
        }
    }

    fun stop() {
        activePlayerOverride?.stop()
        activePlayerOverride?.clearMediaItems()
        activePlayerOverride = null
        val target = player ?: PlaybackService.instance?.defaultPlayer
        target?.stop()
        target?.clearMediaItems()
        PlaybackService.instance?.abandonAudioFocus()
    }

    val isReady: Boolean get() = activePlayer?.playbackState == Player.STATE_READY

    fun setRepeatModeDirect(mode: Int) { activePlayer?.repeatMode = mode }
    fun pause() = activePlayer?.pause()
    fun resume() {
        PlaybackService.instance?.ensureAudioFocus()
        val p = activePlayer
        if (p?.playbackState == Player.STATE_IDLE) {
            p.prepare()
        }
        p?.play()
    }
    fun seekTo(pos: Long) {
        val p = activePlayer ?: return
        val dur = p.duration
        val safeDur = if (dur != androidx.media3.common.C.TIME_UNSET && dur > 0L) dur else 0L
        val clampedPos = if (safeDur > 0L) pos.coerceIn(0L, safeDur) else pos.coerceAtLeast(0L)
        p.seekTo(clampedPos)
        if (p.playWhenReady) {
            if (p.playbackState == Player.STATE_IDLE) {
                p.prepare()
            }
            p.play()
        }
        if (safeDur > 0L) {
            onPositionChanged?.invoke(clampedPos, safeDur)
        }
    }
    fun skipToNext() = activePlayer?.seekToNextMediaItem()
    fun skipToPrevious() = activePlayer?.seekToPreviousMediaItem()
    fun toggleShuffle() {
        val p = activePlayer
        p?.shuffleModeEnabled = !(p?.shuffleModeEnabled ?: false)
    }
    fun toggleRepeat() {
        val p = activePlayer
        val current = p?.repeatMode ?: Player.REPEAT_MODE_OFF
        p?.repeatMode = if (current == Player.REPEAT_MODE_OFF) Player.REPEAT_MODE_ALL else if (current == Player.REPEAT_MODE_ALL) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }
    fun release() {
        PlaybackService.instance?.abandonAudioFocus()
        activePlayerOverride?.release()
        activePlayerOverride = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}
