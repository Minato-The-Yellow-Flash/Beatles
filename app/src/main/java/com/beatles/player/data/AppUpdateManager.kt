package com.beatles.player.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val versionName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val apkUrl: String,
    val publishedAt: String,
    val isPrerelease: Boolean = false
)

object AppUpdateManager {
    private const val GITHUB_OWNER = "Minato-The-Yellow-Flash"
    private const val GITHUB_REPO = "Beatles"
    private const val API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * Checks GitHub Releases API for a newer version than currentVersion.
     * Returns UpdateInfo if an update exists, null otherwise.
     */
    suspend fun checkForUpdates(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Beatles-Android-App")
                .build()

            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null
            val body = resp.body?.string() ?: return@withContext null

            val json = gson.fromJson(body, JsonObject::class.java) ?: return@withContext null
            val rawTag = json.get("tag_name")?.asString?.trim() ?: return@withContext null
            val remoteVersion = rawTag.removePrefix("v").removePrefix("V").trim()
            val cleanCurrent = currentVersion.removePrefix("v").removePrefix("V").trim()

            if (!isNewerVersion(remoteVersion, cleanCurrent)) {
                return@withContext null
            }

            val title = json.get("name")?.asString ?: "Beatles Update ($rawTag)"
            val notes = json.get("body")?.asString ?: "Bug fixes and performance improvements."
            val publishedAt = json.get("published_at")?.asString ?: ""
            val prerelease = json.get("prerelease")?.asBoolean ?: false

            // Find APK asset in release
            var apkUrl: String? = null
            val assets = json.getAsJsonArray("assets")
            if (assets != null) {
                for (elem in assets) {
                    val assetObj = elem.asJsonObject
                    val name = assetObj.get("name")?.asString ?: ""
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = assetObj.get("browser_download_url")?.asString
                        break
                    }
                }
            }

            // Fallback to html_url or direct download link
            val downloadUrl = apkUrl ?: json.get("html_url")?.asString ?: return@withContext null

            UpdateInfo(
                versionName = remoteVersion,
                releaseTitle = title,
                releaseNotes = notes,
                apkUrl = downloadUrl,
                publishedAt = publishedAt,
                isPrerelease = prerelease
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Compares two semantic version strings (e.g. "1.2.0" vs "1.1.9").
     */
    fun isNewerVersion(remote: String, current: String): Boolean {
        try {
            val remoteParts = remote.split("-")[0].split(".").map { it.toIntOrNull() ?: 0 }
            val currentParts = current.split("-")[0].split(".").map { it.toIntOrNull() ?: 0 }
            val maxLen = maxOf(remoteParts.size, currentParts.size)

            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        } catch (_: Exception) {
            return remote != current
        }
    }

    /**
     * Downloads the APK file with live progress tracking and initiates native package install.
     */
    suspend fun downloadAndInstallApk(
        context: Context,
        apkUrl: String,
        onProgress: (Float) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(apkUrl)
                .header("User-Agent", "Beatles-Android-App")
                .build()

            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) {
                withContext(Dispatchers.Main) { onError("Download failed: HTTP ${resp.code}") }
                return@withContext
            }

            val respBody = resp.body ?: run {
                withContext(Dispatchers.Main) { onError("Empty response from server") }
                return@withContext
            }

            val totalBytes = respBody.contentLength().coerceAtLeast(1L)
            val cacheDir = context.externalCacheDir ?: context.cacheDir
            val destFile = File(cacheDir, "beatles-update.apk")
            if (destFile.exists()) destFile.delete()

            respBody.byteStream().use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var bytesRead: Int
                    var totalRead = 0L
                    var lastReportTime = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastReportTime > 80L || totalRead == totalBytes) {
                            lastReportTime = now
                            val frac = (totalRead.toFloat() / totalBytes).coerceIn(0f, 1f)
                            withContext(Dispatchers.Main) { onProgress(frac) }
                        }
                    }
                    output.flush()
                }
            }

            withContext(Dispatchers.Main) {
                onProgress(1.0f)
                triggerInstall(context, destFile, onError)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onError(e.message ?: "Failed to download update") }
        }
    }

    private fun triggerInstall(context: Context, file: File, onError: (String) -> Unit) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val permissionIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(permissionIntent)
                    return
                }
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            onError("Could not launch package installer: ${e.message}")
        }
    }
}
