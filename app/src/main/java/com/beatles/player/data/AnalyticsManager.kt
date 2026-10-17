package com.beatles.player.data

import android.content.Context
import android.os.Build
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object AnalyticsManager {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private var cachedGeoLocation: UserGeoLocation? = null
    private val recentLocalEvents = java.util.concurrent.CopyOnWriteArrayList<String>()

    val APP_VERSION = "1.9"

    /**
     * Fetches real-time download statistics directly from GitHub Releases API.
     * Completely free, no token required.
     */
    suspend fun fetchGitHubDownloadStats(): Pair<Int, List<ReleaseStat>> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://api.github.com/repos/Minato-The-Yellow-Flash/Beatles/releases")
                .header("User-Agent", "BeatlesApp/$APP_VERSION (Android; en-US)")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val resp = httpClient.newCall(req).execute()
            val body = resp.body?.string() ?: return@withContext 0 to emptyList()
            if (!resp.isSuccessful) return@withContext 0 to emptyList()

            val releasesArr = gson.fromJson(body, JsonArray::class.java) ?: return@withContext 0 to emptyList()
            var grandTotal = 0
            val statsList = mutableListOf<ReleaseStat>()

            for (elem in releasesArr) {
                val relObj = elem.asJsonObject
                val tag = relObj.get("tag_name")?.asString ?: "Unknown"
                val rawDate = relObj.get("published_at")?.asString ?: ""
                val cleanDate = if (rawDate.length >= 10) rawDate.substring(0, 10) else rawDate
                val htmlUrl = relObj.get("html_url")?.asString ?: ""

                val assets = relObj.getAsJsonArray("assets")
                var releaseDownloadCount = 0
                var apkFileName = "Beatles-App.apk"

                if (assets != null && assets.size() > 0) {
                    for (a in assets) {
                        val assetObj = a.asJsonObject
                        val name = assetObj.get("name")?.asString ?: ""
                        val count = assetObj.get("download_count")?.asInt ?: 0
                        releaseDownloadCount += count
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkFileName = name
                        }
                    }
                }

                grandTotal += releaseDownloadCount
                statsList.add(
                    ReleaseStat(
                        tagName = tag,
                        apkName = apkFileName,
                        downloadCount = releaseDownloadCount,
                        publishedAt = cleanDate,
                        htmlUrl = htmlUrl
                    )
                )
            }

            grandTotal to statsList
        } catch (_: Exception) {
            0 to emptyList()
        }
    }

    /**
     * Resolves user country, city, ISP and device info using free IP Geolocation.
     * 100% free, no API key required.
     */
    suspend fun fetchUserGeoLocation(ctx: Context): UserGeoLocation = withContext(Dispatchers.IO) {
        cachedGeoLocation?.let { return@withContext it }

        val devModel = "${Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }} ${Build.MODEL}"
        val osVer = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

        try {
            val req = Request.Builder()
                .url("http://ip-api.com/json/?fields=status,country,countryCode,region,regionName,city,isp,query")
                .header("User-Agent", "BeatlesApp/$APP_VERSION (Android)")
                .build()

            val resp = httpClient.newCall(req).execute()
            val body = resp.body?.string()
            if (resp.isSuccessful && !body.isNullOrBlank()) {
                val json = gson.fromJson(body, JsonObject::class.java)
                if (json.get("status")?.asString == "success") {
                    val country = json.get("country")?.asString ?: "Unknown"
                    val countryCode = json.get("countryCode")?.asString ?: ""
                    val city = json.get("city")?.asString ?: "Unknown"
                    val region = json.get("regionName")?.asString ?: ""
                    val isp = json.get("isp")?.asString ?: ""
                    val ip = json.get("query")?.asString ?: ""

                    val resolved = UserGeoLocation(
                        country = country,
                        countryCode = countryCode,
                        city = city,
                        region = region,
                        isp = isp,
                        ip = maskIp(ip),
                        deviceModel = devModel,
                        androidVersion = osVer,
                        appVersion = APP_VERSION
                    )
                    cachedGeoLocation = resolved
                    return@withContext resolved
                }
            }
        } catch (_: Exception) {}

        // Fallback to device locale
        val fallbackCountry = Locale.getDefault().displayCountry.ifBlank { "Global" }
        val fallback = UserGeoLocation(
            country = fallbackCountry,
            countryCode = Locale.getDefault().country,
            city = Locale.getDefault().displayLanguage,
            region = "",
            isp = "Carrier/WiFi",
            ip = "Anonymous",
            deviceModel = devModel,
            androidVersion = osVer,
            appVersion = APP_VERSION
        )
        cachedGeoLocation = fallback
        fallback
    }

    private fun maskIp(ip: String): String {
        val parts = ip.split(".")
        return if (parts.size == 4) "${parts[0]}.${parts[1]}.*.*" else "Masked"
    }

    /**
     * Dispatches an analytics event.
     * If a custom webhook/Google Apps Script URL is set in settings, sends the telemetry payload.
     * Also records in local event history.
     */
    suspend fun logEvent(
        ctx: Context,
        eventName: String,
        extraParams: Map<String, String> = emptyMap()
    ) = withContext(Dispatchers.IO) {
        try {
            val distinctId = LocalStorage.getDistinctUserId(ctx)
            val geo = cachedGeoLocation ?: fetchUserGeoLocation(ctx)
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val timestamp = sdf.format(Date())

            val payload = JsonObject().apply {
                addProperty("event", eventName)
                addProperty("distinct_id", distinctId)
                addProperty("timestamp", timestamp)
                addProperty("country", geo.country)
                addProperty("country_code", geo.countryCode)
                addProperty("city", geo.city)
                addProperty("region", geo.region)
                addProperty("device_model", geo.deviceModel)
                addProperty("android_version", geo.androidVersion)
                addProperty("app_version", APP_VERSION)
                addProperty("isp", geo.isp)

                val paramsObj = JsonObject()
                for ((k, v) in extraParams) {
                    paramsObj.addProperty(k, v)
                }
                add("params", paramsObj)
            }

            // Local audit log
            val logEntry = "$timestamp • $eventName (${geo.country}, ${geo.city})"
            recentLocalEvents.add(0, logEntry)
            if (recentLocalEvents.size > 40) {
                recentLocalEvents.removeAt(recentLocalEvents.size - 1)
            }

            // Send to user's configured Webhook / Google Sheet / Supabase endpoint
            val webhookUrl = LocalStorage.getAnalyticsWebhookUrl(ctx).trim()
            if (webhookUrl.isNotBlank() && webhookUrl.startsWith("http")) {
                val reqBody = payload.toString().toRequestBody(jsonMediaType)
                val postReq = Request.Builder()
                    .url(webhookUrl)
                    .post(reqBody)
                    .header("User-Agent", "BeatlesApp/$APP_VERSION (Android)")
                    .build()
                httpClient.newCall(postReq).execute().close()
            }
        } catch (_: Exception) {}
    }

    fun getRecentAuditEvents(): List<String> = recentLocalEvents.toList()

    /**
     * Initializes analytics on app start.
     * Fires 'app_install' if first launch, and 'app_open' on every session.
     */
    suspend fun initAndTrack(ctx: Context) = withContext(Dispatchers.IO) {
        fetchUserGeoLocation(ctx)
        if (!LocalStorage.hasLoggedInstall(ctx)) {
            logEvent(ctx, "app_install", mapOf("source" to "apk_direct"))
            LocalStorage.setLoggedInstall(ctx)
        }
        logEvent(ctx, "app_open")
    }

    /**
     * Tests a custom webhook URL (e.g. Google Apps Script Web App URL).
     */
    suspend fun sendTestPing(ctx: Context, targetUrl: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
            return@withContext false to "URL must start with http:// or https://"
        }
        try {
            val geo = cachedGeoLocation ?: fetchUserGeoLocation(ctx)
            val testPayload = JsonObject().apply {
                addProperty("event", "test_ping")
                addProperty("message", "Beatles Analytics test ping successful!")
                addProperty("distinct_id", LocalStorage.getDistinctUserId(ctx))
                addProperty("country", geo.country)
                addProperty("city", geo.city)
                addProperty("device_model", geo.deviceModel)
                addProperty("app_version", APP_VERSION)
            }
            val body = testPayload.toString().toRequestBody(jsonMediaType)
            val req = Request.Builder()
                .url(targetUrl)
                .post(body)
                .header("User-Agent", "BeatlesApp/$APP_VERSION (Android)")
                .build()

            val resp = httpClient.newCall(req).execute()
            val code = resp.code
            resp.close()
            if (code in 200..299 || code == 302) {
                true to "Ping successful (HTTP $code)!"
            } else {
                false to "Server returned HTTP $code"
            }
        } catch (e: Exception) {
            false to (e.localizedMessage ?: "Connection error")
        }
    }
}
