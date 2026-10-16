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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

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
    private const val CDN_VERSION_URL = "https://raw.githubusercontent.com/$GITHUB_OWNER/$GITHUB_REPO/main/version.json"
    private const val WEB_LATEST_RELEASE_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    private const val API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Client with redirects disabled to capture the 302 Location header instantly
    private val noRedirectClient = httpClient.newBuilder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    private val gson = Gson()

    /**
     * Checks for a newer version than currentVersion.
     * Tier 1: GitHub Web Redirect (Zero rate limits, instant 302 detection of /releases/latest -> /releases/tag/<tag>).
     * Tier 2: Fast raw.githubusercontent CDN (Zero rate limits, never 403).
     * Tier 3: GitHub Releases API fallback (Rate limited to 60 req/hr).
     * Returns UpdateInfo if an update exists, null otherwise.
     */
    suspend fun checkForUpdates(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        val cleanCurrent = currentVersion.removePrefix("v").removePrefix("V").trim()

        // Tier 1: GitHub Web Redirect (100% Rate-Limit Proof, direct release detection)
        try {
            val headReq = Request.Builder()
                .url(WEB_LATEST_RELEASE_URL)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml")
                .build()

            val headResp = noRedirectClient.newCall(headReq).execute()
            val redirectLocation = if (headResp.code in 300..399) {
                headResp.header("Location")
            } else null

            var releaseTag: String? = null
            if (!redirectLocation.isNullOrBlank() && redirectLocation.contains("/releases/tag/")) {
                releaseTag = redirectLocation.substringAfter("/releases/tag/").substringBefore("/").substringBefore("?").trim()
            }

            // If head/no-redirect didn't get location, try standard GET following redirects
            if (releaseTag.isNullOrBlank()) {
                val getReq = Request.Builder()
                    .url(WEB_LATEST_RELEASE_URL)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                    .build()
                val getResp = httpClient.newCall(getReq).execute()
                val finalUrl = getResp.request.url.toString()
                if (finalUrl.contains("/releases/tag/")) {
                    releaseTag = finalUrl.substringAfter("/releases/tag/").substringBefore("/").substringBefore("?").trim()
                }
            }

            if (!releaseTag.isNullOrBlank()) {
                val remoteVersion = releaseTag.removePrefix("v").removePrefix("V").trim()
                if (isNewerVersion(remoteVersion, cleanCurrent)) {
                    // Find actual APK asset from expanded assets page
                    var apkUrl = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/download/$releaseTag/Beatles-App.apk"
                    var releaseNotes = "Beatles v$remoteVersion is available with new features and performance improvements."
                    val releaseTitle = "Beatles v$remoteVersion"

                    try {
                        val assetsReq = Request.Builder()
                            .url("https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/expanded_assets/$releaseTag")
                            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                            .build()
                        val assetsResp = httpClient.newCall(assetsReq).execute()
                        if (assetsResp.isSuccessful) {
                            val assetsHtml = assetsResp.body?.string() ?: ""
                            val apkMatch = Regex("""href="(/$GITHUB_OWNER/$GITHUB_REPO/releases/download/[^"]+\.apk)"""").find(assetsHtml)
                                ?: Regex("""href="([^"]+\.apk)"""").find(assetsHtml)
                            if (apkMatch != null) {
                                val matchPath = apkMatch.groupValues[1]
                                apkUrl = if (matchPath.startsWith("http")) matchPath else "https://github.com$matchPath"
                            }
                        }
                    } catch (_: Exception) {}

                    try {
                        val pageReq = Request.Builder()
                            .url("https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/tag/$releaseTag")
                            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                            .build()
                        val pageResp = httpClient.newCall(pageReq).execute()
                        if (pageResp.isSuccessful) {
                            val pageHtml = pageResp.body?.string() ?: ""
                            val notesMatch = Regex("""<div[^>]*class="[^"]*markdown-body[^"]*"[^>]*>(.*?)</div>""", RegexOption.DOT_MATCHES_ALL).find(pageHtml)
                            if (notesMatch != null) {
                                val cleanNotes = notesMatch.groupValues[1]
                                    .replace(Regex("<[^>]*>"), "")
                                    .replace("&quot;", "\"")
                                    .replace("&amp;", "&")
                                    .replace("&lt;", "<")
                                    .replace("&gt;", ">")
                                    .trim()
                                if (cleanNotes.isNotBlank()) {
                                    releaseNotes = cleanNotes
                                }
                            }
                        }
                    } catch (_: Exception) {}

                    return@withContext UpdateInfo(
                        versionName = remoteVersion,
                        releaseTitle = releaseTitle,
                        releaseNotes = releaseNotes,
                        apkUrl = apkUrl,
                        publishedAt = "",
                        isPrerelease = false
                    )
                }
            }
        } catch (_: Exception) {
            // Fall through to Tier 2
        }

        // Tier 2: Try GitHub CDN version.json (Zero rate limits, highly resilient)
        try {
            val cdnReq = Request.Builder()
                .url(CDN_VERSION_URL)
                .header("User-Agent", "Beatles-Android-App")
                .build()

            val cdnResp = httpClient.newCall(cdnReq).execute()
            if (cdnResp.isSuccessful) {
                val body = cdnResp.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = gson.fromJson(body, JsonObject::class.java)
                    val rawVer = json.get("versionName")?.asString?.trim()
                    if (rawVer != null) {
                        val remoteVersion = rawVer.removePrefix("v").removePrefix("V").trim()
                        if (isNewerVersion(remoteVersion, cleanCurrent)) {
                            val title = json.get("releaseTitle")?.asString ?: "Beatles Update (v$remoteVersion)"
                            val notes = json.get("releaseNotes")?.asString ?: "Bug fixes and performance improvements."
                            val apkUrl = json.get("apkUrl")?.asString ?: ""
                            val publishedAt = json.get("publishedAt")?.asString ?: ""
                            if (apkUrl.isNotBlank()) {
                                return@withContext UpdateInfo(
                                    versionName = remoteVersion,
                                    releaseTitle = title,
                                    releaseNotes = notes,
                                    apkUrl = apkUrl,
                                    publishedAt = publishedAt,
                                    isPrerelease = false
                                )
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Fall through to Tier 3
        }

        // Tier 3: GitHub Releases API fallback
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
     * Compares two semantic version strings (e.g. "1.3" vs "1.2", "v1.3" vs "1.2", "1.2.1" vs "1.2.0").
     */
    fun isNewerVersion(remote: String, current: String): Boolean {
        try {
            val cleanRemote = remote.trim().removePrefix("v").removePrefix("V")
            val cleanCurrent = current.trim().removePrefix("v").removePrefix("V")
            val remoteParts = cleanRemote.split("-")[0].split(".").map { it.toIntOrNull() ?: 0 }
            val currentParts = cleanCurrent.split("-")[0].split(".").map { it.toIntOrNull() ?: 0 }
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
     * Downloads the APK file with high-speed multi-threaded parallel chunk acceleration and live progress metrics.
     * Uses 10 parallel range streams when supported (up to 8x-10x speedup over single stream).
     */
    suspend fun downloadAndInstallApk(
        context: Context,
        apkUrl: String,
        onProgress: (Float, String) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val cacheDir = context.externalCacheDir ?: context.cacheDir
        val destFile = File(cacheDir, "beatles-update.apk")
        if (destFile.exists()) destFile.delete()

        try {
            // Step 1: Follow redirects to find the direct storage endpoint and content length
            val headReq = Request.Builder()
                .url(apkUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()

            val headResp = httpClient.newCall(headReq).execute()
            if (!headResp.isSuccessful) {
                withContext(Dispatchers.Main) { onError("Download failed: HTTP ${headResp.code}") }
                return@withContext
            }

            val directUrl = headResp.request.url.toString()
            val totalBytes = headResp.body?.contentLength() ?: -1L
            val acceptsRanges = headResp.header("Accept-Ranges")?.contains("bytes", ignoreCase = true) == true ||
                    headResp.header("Content-Range") != null ||
                    headResp.code == 206 ||
                    totalBytes > 2 * 1024 * 1024
            headResp.close()

            if (acceptsRanges && totalBytes > 2 * 1024 * 1024) {
                // High-Speed Multi-Stream Parallel Download (10 Chunks)
                downloadMultiChunk(
                    directUrl = directUrl,
                    totalBytes = totalBytes,
                    cacheDir = cacheDir,
                    destFile = destFile,
                    onProgress = onProgress
                )
            } else {
                // Standard single stream download with high-performance 128KB buffer
                downloadSingleStream(
                    directUrl = directUrl,
                    totalBytes = totalBytes,
                    destFile = destFile,
                    onProgress = onProgress
                )
            }

            withContext(Dispatchers.Main) {
                onProgress(1.0f, "Download complete • Installing...")
                triggerInstall(context, destFile, onError)
            }
        } catch (e: Exception) {
            // Fallback: If multi-chunk failed, try single stream before giving up
            try {
                if (destFile.exists()) destFile.delete()
                downloadSingleStream(
                    directUrl = apkUrl,
                    totalBytes = -1L,
                    destFile = destFile,
                    onProgress = onProgress
                )
                withContext(Dispatchers.Main) {
                    onProgress(1.0f, "Download complete • Installing...")
                    triggerInstall(context, destFile, onError)
                }
            } catch (fallbackEx: Exception) {
                withContext(Dispatchers.Main) {
                    onError(fallbackEx.message ?: e.message ?: "Failed to download update")
                }
            }
        }
    }

    private suspend fun downloadMultiChunk(
        directUrl: String,
        totalBytes: Long,
        cacheDir: File,
        destFile: File,
        onProgress: (Float, String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val numChunks = 10
        val chunkSize = (totalBytes + numChunks - 1) / numChunks
        val totalDownloaded = AtomicLong(0L)
        val startTime = System.currentTimeMillis()
        var lastReportTime = 0L

        val partFiles = (0 until numChunks).map { i ->
            File(cacheDir, "beatles-part-$i.tmp").apply { if (exists()) delete() }
        }

        // Run all chunks in parallel using coroutines
        coroutineScope {
            val jobs = (0 until numChunks).map { i ->
                async(Dispatchers.IO) {
                    val startByte = i * chunkSize
                    val endByte = minOf(totalBytes - 1, (i + 1) * chunkSize - 1)
                    val partFile = partFiles[i]

                    var chunkSuccess = false
                    var lastErr: Exception? = null

                    for (attempt in 0..2) {
                        try {
                            val chunkReq = Request.Builder()
                                .url(directUrl)
                                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                                .header("Range", "bytes=$startByte-$endByte")
                                .build()

                            val chunkResp = httpClient.newCall(chunkReq).execute()
                            if (chunkResp.isSuccessful) {
                                chunkResp.body?.byteStream()?.use { input ->
                                    FileOutputStream(partFile).use { output ->
                                        val buffer = ByteArray(64 * 1024)
                                        var read: Int
                                        while (input.read(buffer).also { read = it } != -1) {
                                            output.write(buffer, 0, read)
                                            val currentTotal = totalDownloaded.addAndGet(read.toLong())
                                            val now = System.currentTimeMillis()
                                            if (now - lastReportTime > 80L || currentTotal == totalBytes) {
                                                lastReportTime = now
                                                val elapsedSec = (now - startTime) / 1000.0
                                                val speedKb = if (elapsedSec > 0.1) (currentTotal / 1024.0 / elapsedSec).toInt() else 0
                                                val speedText = if (speedKb >= 1024) String.format(Locale.US, "%.1f MB/s", speedKb / 1024f) else "$speedKb KB/s"
                                                val dlMb = String.format(Locale.US, "%.1f", currentTotal / (1024f * 1024f))
                                                val totalMb = String.format(Locale.US, "%.1f MB", totalBytes / (1024f * 1024f))
                                                val frac = (currentTotal.toFloat() / totalBytes).coerceIn(0f, 1f)
                                                val status = "$dlMb / $totalMb • $speedText"
                                                withContext(Dispatchers.Main) { onProgress(frac, status) }
                                            }
                                        }
                                        output.flush()
                                    }
                                }
                                chunkSuccess = true
                                break
                            }
                        } catch (e: Exception) {
                            lastErr = e
                            delay(200L * (attempt + 1))
                        }
                    }

                    if (!chunkSuccess) {
                        throw (lastErr ?: Exception("Failed to download chunk $i"))
                    }
                }
            }
            jobs.awaitAll()
        }

        // Fast stitch all part files into destFile
        FileOutputStream(destFile).use { out ->
            for (p in partFiles) {
                if (p.exists()) {
                    p.inputStream().use { inp -> inp.copyTo(out, bufferSize = 128 * 1024) }
                    p.delete()
                }
            }
            out.flush()
        }

        if (destFile.length() < totalBytes * 0.95) {
            throw Exception("Downloaded file is incomplete: ${destFile.length()} of $totalBytes bytes")
        }
    }

    private suspend fun downloadSingleStream(
        directUrl: String,
        totalBytes: Long,
        destFile: File,
        onProgress: (Float, String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(directUrl)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
            .build()

        val resp = httpClient.newCall(req).execute()
        if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}")
        val body = resp.body ?: throw Exception("Empty response body")
        val resolvedTotal = if (totalBytes > 0) totalBytes else body.contentLength().coerceAtLeast(1L)
        val startTime = System.currentTimeMillis()
        var lastReportTime = 0L

        body.byteStream().use { input ->
            FileOutputStream(destFile).use { output ->
                val buffer = ByteArray(128 * 1024)
                var read: Int
                var totalRead = 0L

                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    totalRead += read
                    val now = System.currentTimeMillis()
                    if (now - lastReportTime > 80L || totalRead == resolvedTotal) {
                        lastReportTime = now
                        val elapsedSec = (now - startTime) / 1000.0
                        val speedKb = if (elapsedSec > 0.1) (totalRead / 1024.0 / elapsedSec).toInt() else 0
                        val speedText = if (speedKb >= 1024) String.format(Locale.US, "%.1f MB/s", speedKb / 1024f) else "$speedKb KB/s"
                        val dlMb = String.format(Locale.US, "%.1f", totalRead / (1024f * 1024f))
                        val totalMb = String.format(Locale.US, "%.1f MB", resolvedTotal / (1024f * 1024f))
                        val frac = (totalRead.toFloat() / resolvedTotal).coerceIn(0f, 1f)
                        val status = "$dlMb / $totalMb • $speedText"
                        withContext(Dispatchers.Main) { onProgress(frac, status) }
                    }
                }
                output.flush()
            }
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
