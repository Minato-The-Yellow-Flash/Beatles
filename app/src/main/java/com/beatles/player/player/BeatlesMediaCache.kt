package com.beatles.player.player

import android.content.Context
import android.net.Uri
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.DefaultHttpDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object BeatlesMediaCache {
    private const val MAX_CACHE_SIZE = 250L * 1024 * 1024 // 250 MB LRU disk cache
    @Volatile
    private var simpleCache: SimpleCache? = null

    @Synchronized
    fun getCache(context: Context): SimpleCache? {
        var cache = simpleCache
        if (cache == null) {
            try {
                val cacheDir = File(context.cacheDir, "beatles_stream_cache")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE)
                val databaseProvider = StandaloneDatabaseProvider(context)
                cache = SimpleCache(cacheDir, evictor, databaseProvider)
                simpleCache = cache
            } catch (e: Throwable) {
                android.util.Log.e("BeatlesMediaCache", "Error initializing SimpleCache: ${e.message}")
                return null
            }
        }
        return cache
    }

    fun createDataSourceFactory(context: Context): DataSource.Factory {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20_000)
            .setReadTimeoutMs(25_000)
            .setDefaultRequestProperties(mapOf(
                "Accept-Encoding" to "identity"
            ))

        val upstreamFactory = DefaultDataSource.Factory(context, httpFactory)
        val cache = try { getCache(context) } catch (_: Throwable) { null }
        return if (cache != null) {
            try {
                CacheDataSource.Factory()
                    .setCache(cache)
                    .setUpstreamDataSourceFactory(upstreamFactory)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            } catch (_: Throwable) {
                upstreamFactory
            }
        } else {
            upstreamFactory
        }
    }

    suspend fun preload(context: Context, url: String, startOffsetMs: Long = 0L) = withContext(Dispatchers.IO) {
        if (!url.startsWith("http")) return@withContext
        try {
            val ds = createDataSourceFactory(context).createDataSource()
            if (ds is CacheDataSource) {
                val dataSpec = DataSpec.Builder()
                    .setUri(Uri.parse(url))
                    .setPosition(0L)
                    .setLength(2L * 1024 * 1024)
                    .build()
                val writer = CacheWriter(ds, dataSpec, null, null)
                writer.cache()
            }
        } catch (_: Exception) {}
    }
}
