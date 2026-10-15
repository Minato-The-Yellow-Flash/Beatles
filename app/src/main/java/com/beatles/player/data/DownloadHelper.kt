package com.beatles.player.data

import android.app.DownloadManager
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

object DownloadHelper {
    private val httpClient = OkHttpClient()

    suspend fun downloadDirectly(
        context: Context, 
        song: Song, 
        streamUrl: String,
        onProgress: ((Float) -> Unit)? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            val safeArtist = song.artist.replace(Regex("[^a-zA-Z0-9.-]"), "_").trim('_')
            val safeTitle = song.title.replace(Regex("[^a-zA-Z0-9.-]"), "_").trim('_')
            val fileName = "${safeArtist}_-_${safeTitle}.m4a"

            val musicDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Beatles")
            if (!musicDir.exists()) musicDir.mkdirs()
            val targetFile = File(musicDir, fileName)

            val req = Request.Builder()
                .url(streamUrl)
                .header("User-Agent", "BeatlesApp/2.0 (Android; en-US)")
                .build()
            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null
            val body = resp.body ?: return@withContext null
            val contentLength = body.contentLength()
            val expectedLength = if (contentLength > 0L) contentLength else {
                if (song.durationSeconds > 0) song.durationSeconds * 16_000L else 3_500_000L
            }

            body.byteStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var bytesRead: Int
                    var totalRead = 0L
                    var lastPercent = -1
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        val percent = ((totalRead * 100L) / expectedLength).toInt().coerceIn(0, 99)
                        if (percent != lastPercent) {
                            lastPercent = percent
                            onProgress?.invoke(percent / 100f)
                        }
                    }
                    onProgress?.invoke(1.0f)
                }
            }

            if (targetFile.exists() && targetFile.length() > 1024L) {
                MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), arrayOf("audio/mp4", "audio/m4a"), null)
                return@withContext Uri.fromFile(targetFile).toString()
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun downloadToMusicFolder(context: Context, song: Song, streamUrl: String): Long {
        return try {
            val safeArtist = song.artist.replace(Regex("[^a-zA-Z0-9.-]"), "_").trim('_')
            val safeTitle = song.title.replace(Regex("[^a-zA-Z0-9.-]"), "_").trim('_')
            val fileName = "${safeArtist}_-_${safeTitle}.m4a"

            val request = DownloadManager.Request(Uri.parse(streamUrl)).apply {
                setTitle(song.title)
                setDescription("${song.artist} • Beatles Offline Audio")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
                try {
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, "Beatles/$fileName")
                } catch (e: Exception) {
                    val musicDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Beatles")
                    if (!musicDir.exists()) musicDir.mkdirs()
                    setDestinationUri(Uri.fromFile(File(musicDir, fileName)))
                }
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            -1L
        }
    }
}
