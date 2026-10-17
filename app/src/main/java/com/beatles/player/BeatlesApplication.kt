package com.beatles.player

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class BeatlesApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(32, 5, TimeUnit.MINUTES))
            .dispatcher(Dispatcher().apply {
                maxRequests = 128
                maxRequestsPerHost = 32
            })
            .retryOnConnectionFailure(true)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.30) // 30% of app memory for instant cover recycling
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(300L * 1024 * 1024) // 300 MB high-speed persistent disk cache
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .allowHardware(true)
            .crossfade(150)
            .respectCacheHeaders(false) // Aggressively cache album artwork even with short TTL headers
            .build()
    }
}
