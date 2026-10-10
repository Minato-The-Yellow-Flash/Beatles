package com.beatles.player.player.automix

import android.media.MediaCodec
import android.media.MediaDataSource
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.SystemClock
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Decodes real audio (local files or HTTP streams) into a short-time RMS envelope and
 * extracts the musical features AutoMix needs:
 *  - leading / trailing silence
 *  - fade-out (outro) start of the outgoing track
 *  - soft-intro length of the incoming track
 *  - loudness (for level matching)
 *  - tempo (BPM) and beat phase via onset-envelope autocorrelation
 */
object WaveformAnalyzer {
    private const val TAG = "AutoMixWaveform"
    private const val HOP_MS = 10
    private const val DECODE_WALL_CLOCK_LIMIT_MS = 12_000L
    private const val SILENCE_FLOOR = 0.0040f // ≈ -48 dBFS

    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val cache = java.util.concurrent.ConcurrentHashMap<String, SegmentAnalysis>()

    /** Analyse the last [lengthMs] of a track whose real duration is [durationMs]. */
    fun analyzeTail(trackId: String, uri: String, durationMs: Long, lengthMs: Long = 32_000L): SegmentAnalysis? {
        val key = "tail:$trackId:${durationMs / 1000}"
        cache[key]?.let { return it }
        val start = (durationMs - lengthMs).coerceAtLeast(0L)
        val env = decodeEnvelope(uri, start, durationMs - start) ?: return null
        return buildAnalysis(env, isTail = true)?.also { cache[key] = it }
    }

    /** Analyse the first [lengthMs] of a track. */
    fun analyzeHead(trackId: String, uri: String, lengthMs: Long = 32_000L): SegmentAnalysis? {
        val key = "head:$trackId"
        cache[key]?.let { return it }
        val env = decodeEnvelope(uri, 0L, lengthMs) ?: return null
        return buildAnalysis(env, isTail = false)?.also { cache[key] = it }
    }

    // ---------------------------------------------------------------------------------
    // Decoding
    // ---------------------------------------------------------------------------------

    private class Envelope(val startMs: Long, val rms: FloatArray)

    private fun decodeEnvelope(uri: String, startMs: Long, lengthMs: Long): Envelope? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        val began = SystemClock.elapsedRealtime()
        try {
            when {
                uri.startsWith("file://") -> extractor.setDataSource(android.net.Uri.parse(uri).path ?: return null)
                uri.startsWith("/") -> extractor.setDataSource(uri)
                uri.startsWith("http") -> extractor.setDataSource(HttpRangeDataSource(uri))
                else -> return null
            }
            val trackIndex = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return null
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null

            var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
            var isFloat = false

            val startUs = startMs * 1000L
            val endUs = (startMs + lengthMs) * 1000L
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val frames = ArrayList<Float>(((lengthMs / HOP_MS) + 16).toInt())
            var hopSamples = sampleRate * HOP_MS / 1000
            var accSq = 0.0
            var accN = 0
            var inputDone = false
            var outputDone = false
            val info = MediaCodec.BufferInfo()

            while (!outputDone) {
                if (SystemClock.elapsedRealtime() - began > DECODE_WALL_CLOCK_LIMIT_MS) break

                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(5_000)
                    if (inIdx >= 0) {
                        val buf = codec.getInputBuffer(inIdx)!!
                        val size = extractor.readSampleData(buf, 0)
                        val t = extractor.sampleTime
                        if (size < 0 || t > endUs + 200_000L) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIdx, 0, size, t, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIdx = codec.dequeueOutputBuffer(info, 5_000)
                when {
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val of = codec.outputFormat
                        sampleRate = of.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channels = of.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
                        isFloat = of.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                            of.getInteger(MediaFormat.KEY_PCM_ENCODING) == android.media.AudioFormat.ENCODING_PCM_FLOAT
                        hopSamples = sampleRate * HOP_MS / 1000
                    }
                    outIdx >= 0 -> {
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                        val out = codec.getOutputBuffer(outIdx)
                        if (out != null && info.size > 0) {
                            out.position(info.offset)
                            out.limit(info.offset + info.size)
                            val bytesPerSample = if (isFloat) 4 else 2
                            val frameCount = info.size / (bytesPerSample * channels)
                            val bufStartUs = info.presentationTimeUs
                            val usPerFrame = 1_000_000.0 / sampleRate
                            val ordered = out.order(ByteOrder.LITTLE_ENDIAN)
                            val fb = if (isFloat) ordered.asFloatBuffer() else null
                            val sb = if (!isFloat) ordered.asShortBuffer() else null
                            for (f in 0 until frameCount) {
                                val tUs = bufStartUs + (f * usPerFrame).toLong()
                                if (tUs < startUs) continue
                                if (tUs >= endUs) { outputDone = true; break }
                                var mono = 0f
                                for (c in 0 until channels) {
                                    val idx = f * channels + c
                                    mono += if (fb != null) fb.get(idx) else sb!!.get(idx) / 32768f
                                }
                                mono /= channels
                                accSq += (mono * mono).toDouble()
                                accN++
                                if (accN >= hopSamples) {
                                    frames.add(sqrt(accSq / accN).toFloat())
                                    accSq = 0.0
                                    accN = 0
                                }
                            }
                        }
                        codec.releaseOutputBuffer(outIdx, false)
                    }
                }
            }
            if (frames.size < 200) return null // < 2 s of audio: not useful
            return Envelope(startMs, frames.toFloatArray())
        } catch (e: Exception) {
            Log.w(TAG, "Waveform decode failed: ${e.message}")
            return null
        } finally {
            try { codec?.stop() } catch (_: Exception) {}
            try { codec?.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
        }
    }

    // ---------------------------------------------------------------------------------
    // Feature extraction
    // ---------------------------------------------------------------------------------

    private fun buildAnalysis(env: Envelope, isTail: Boolean): SegmentAnalysis? {
        val rms = env.rms
        val n = rms.size
        fun tMs(i: Int) = env.startMs + i.toLong() * HOP_MS

        val db = FloatArray(n) { 20f * log10(rms[it] + 1e-7f) }
        val smooth = movingAverage(db, 40) // 400 ms loudness curve

        val nonSilent = rms.filter { it > SILENCE_FLOOR }
        if (nonSilent.size < 100) return null
        val sortedDb = smooth.sorted()
        val refDb = sortedDb[(sortedDb.size * 0.70f).toInt().coerceIn(0, sortedDb.size - 1)]
        val floor = max(SILENCE_FLOOR, dbToLin(refDb - 38f))

        val first = rms.indexOfFirst { it > floor }.coerceAtLeast(0)
        var last = rms.indexOfLast { it > floor }.let { if (it < 0) n - 1 else it }

        var fadeStart: Long? = null
        var bodyStart: Long? = null
        if (isTail) {
            // Reference "body" loudness = 70th percentile of the first 12 s of the tail window.
            val bodyEnd = min(n, 1200)
            val body = smooth.copyOfRange(0, bodyEnd).sorted()
            val bodyRef = body[(body.size * 0.70f).toInt().coerceIn(0, body.size - 1)]

            // Detect YouTube outro talking / credits / dead air:
            // Find the last moment energy was actually at musical levels (>= bodyRef - 8.5dB)
            var lastMusical = -1
            for (i in (n - 1) downTo 0) {
                if (smooth[i] >= bodyRef - 8.5f) { lastMusical = i; break }
            }
            // If the last 3-15 seconds dropped into video chatter / silence:
            if (lastMusical in 0 until (n - 250)) {
                // Cut off at lastMusical + 1.2s decay
                last = min(last, lastMusical + 120)
            }

            // Last moment the track is still at body level; after it, energy only decays.
            var lastBody = -1
            for (i in last downTo 0) {
                if (smooth[i] >= bodyRef - 4.5f) { lastBody = i; break }
            }
            if (lastBody >= 0 && (last - lastBody) * HOP_MS >= 2000) {
                fadeStart = tMs(lastBody)
            } else if (last < n - 200) {
                fadeStart = tMs(last)
            }
        } else {
            val headRef = refDb
            val idx = (first until n).firstOrNull { smooth[it] >= headRef - 4.5f }
            if (idx != null) bodyStart = tMs(idx)
        }

        val loud = run {
            var s = 0.0; var c = 0
            for (i in first..last) { if (rms[i] > floor) { s += rms[i] * rms[i]; c++ } }
            if (c == 0) 0f else sqrt(s / c).toFloat()
        }

        val tempo = estimateTempo(db, first, last)

        return SegmentAnalysis(
            segmentStartMs = env.startMs,
            segmentEndMs = tMs(n),
            firstSoundMs = tMs(first),
            lastSoundMs = tMs(last),
            fadeOutStartMs = fadeStart,
            bodyStartMs = bodyStart,
            loudness = loud,
            bpm = tempo?.first,
            bpmConfidence = tempo?.second ?: 0f,
            beatPhaseMs = tempo?.third?.let { env.startMs + (it * HOP_MS).roundToInt() }
        )
    }

    /** Returns (bpm, confidence, beatPhaseFrames) using onset-envelope autocorrelation. */
    private fun estimateTempo(db: FloatArray, from: Int, to: Int): Triple<Float, Float, Float>? {
        val len = to - from + 1
        if (len < 600) return null // need ≥ 6 s of sound

        // Onset strength: positive log-energy flux minus its local mean.
        val flux = FloatArray(len)
        for (i in 1 until len) flux[i] = max(0f, db[from + i] - db[from + i - 1])
        val local = movingAverage(flux, 50)
        val onset = FloatArray(len) { max(0f, flux[it] - local[it]) }

        val minLag = (60_000f / 185f / HOP_MS).toInt()   // 185 BPM
        val maxLag = (60_000f / 68f / HOP_MS).toInt()    // 68 BPM
        if (len <= maxLag * 3) return null

        var r0 = 0.0
        for (v in onset) r0 += v * v
        if (r0 <= 1e-9) return null

        val ac = DoubleArray(maxLag + 2)
        for (lag in (minLag - 1)..(maxLag + 1)) {
            var s = 0.0
            for (i in 0 until len - lag) s += onset[i] * onset[i + lag]
            ac[lag] = s / (len - lag) * len
        }
        var bestLag = -1
        var bestScore = 0.0
        for (lag in minLag..maxLag) {
            val bpm = 60_000.0 / (lag * HOP_MS)
            val octaves = ln(bpm / 120.0) / ln(2.0)
            val prior = exp(-0.5 * (octaves / 0.9) * (octaves / 0.9))
            val score = ac[lag] * prior
            if (score > bestScore) { bestScore = score; bestLag = lag }
        }
        if (bestLag < 0) return null

        // Parabolic interpolation for sub-frame accuracy.
        val a = ac[bestLag - 1]; val b = ac[bestLag]; val c = ac[bestLag + 1]
        val denom = a - 2 * b + c
        val refined = if (abs(denom) > 1e-12) bestLag + 0.5 * (a - c) / denom else bestLag.toDouble()
        val bpm = (60_000.0 / (refined * HOP_MS)).toFloat()
        val confidence = (ac[bestLag] / r0).toFloat().coerceIn(0f, 1f)

        // Beat phase: offset maximizing onset energy on the beat grid.
        val period = refined.toFloat()
        var bestPhase = 0
        var bestPhaseScore = -1.0
        for (phase in 0 until period.toInt().coerceAtLeast(1)) {
            var s = 0.0
            var k = 0
            while (true) {
                val idx = (phase + k * period).roundToInt()
                if (idx >= len) break
                s += onset[idx]
                k++
            }
            if (s > bestPhaseScore) { bestPhaseScore = s; bestPhase = phase }
        }
        return Triple(bpm, confidence, (from + bestPhase).toFloat())
    }

    private fun movingAverage(x: FloatArray, window: Int): FloatArray {
        val out = FloatArray(x.size)
        var sum = 0.0
        val half = window / 2
        var lo = 0
        var hi = -1
        for (i in x.indices) {
            val wantLo = max(0, i - half)
            val wantHi = min(x.size - 1, i + half)
            while (hi < wantHi) { hi++; sum += x[hi] }
            while (lo < wantLo) { sum -= x[lo]; lo++ }
            out[i] = (sum / (hi - lo + 1)).toFloat()
        }
        return out
    }

    private fun dbToLin(db: Float): Float = Math.pow(10.0, db / 20.0).toFloat()

    /**
     * Random-access HTTP source for MediaExtractor using range requests with a small block cache,
     * so only the analysed region of the stream is downloaded.
     */
    private class HttpRangeDataSource(private val url: String) : MediaDataSource() {
        private val blockSize = 192 * 1024
        private val blocks = object : LinkedHashMap<Long, ByteArray>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, ByteArray>?) = size > 24
        }
        private var totalSize = -1L

        override fun getSize(): Long {
            if (totalSize >= 0) return totalSize
            val req = Request.Builder().url(url).header("Range", "bytes=0-0").build()
            http.newCall(req).execute().use { resp ->
                val cr = resp.header("Content-Range")
                totalSize = cr?.substringAfter('/')?.toLongOrNull()
                    ?: resp.header("Content-Length")?.toLongOrNull()
                    ?: -1L
            }
            return totalSize
        }

        override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
            val total = getSize()
            if (total in 0..position) return -1
            var copied = 0
            var pos = position
            while (copied < size) {
                if (total in 0..pos) break
                val blockIdx = pos / blockSize
                val block = blocks[blockIdx] ?: fetchBlock(blockIdx) ?: break
                val inBlock = (pos - blockIdx * blockSize).toInt()
                if (inBlock >= block.size) break
                val n = min(size - copied, block.size - inBlock)
                System.arraycopy(block, inBlock, buffer, offset + copied, n)
                copied += n
                pos += n
            }
            return if (copied == 0) -1 else copied
        }

        private fun fetchBlock(idx: Long): ByteArray? {
            val start = idx * blockSize
            val end = start + blockSize - 1
            val req = Request.Builder().url(url).header("Range", "bytes=$start-$end").build()
            return try {
                http.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return null
                    resp.body?.bytes()?.also { blocks[idx] = it }
                }
            } catch (_: Exception) { null }
        }

        override fun close() { blocks.clear() }
    }
}
