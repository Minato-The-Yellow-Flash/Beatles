package com.beatles.player.data

import okhttp3.OkHttpClient
import okhttp3.Request as OkHttpRequest
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.util.concurrent.TimeUnit

class OkHttpDownloader : Downloader() {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    override fun execute(request: Request): Response {
        val reqBuilder = OkHttpRequest.Builder().url(request.url())
        
        var hasUserAgent = false
        request.headers()?.forEach { (key, list) ->
            if (key.equals("User-Agent", ignoreCase = true)) hasUserAgent = true
            list.forEach { reqBuilder.addHeader(key, it) }
        }
        if (!hasUserAgent) {
            reqBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
        }

        val method = request.httpMethod()
        if (method == "POST") {
            reqBuilder.post(request.dataToSend()?.toRequestBody() ?: byteArrayOf().toRequestBody())
        } else if (method == "HEAD") {
            reqBuilder.head()
        } else {
            reqBuilder.get()
        }

        val response = client.newCall(reqBuilder.build()).execute()
        val resHeaders = mutableMapOf<String, List<String>>()
        response.headers.names().forEach { resHeaders[it] = response.headers.values(it) }
        return Response(response.code, response.message, resHeaders, response.body?.string(), response.request.url.toString())
    }
}
