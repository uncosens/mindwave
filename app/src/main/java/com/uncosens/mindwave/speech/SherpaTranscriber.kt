package com.uncosens.mindwave.speech

import android.content.Context
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizerResult
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import com.uncosens.mindwave.analysis.Word
import java.io.File

// Korean: streaming zipformer trained on about 6,500 h of Korean (kangkyu/icefall-asr-ko-streaming-zipformer-174m,
// chunk 32, int8), run by sherpa-onnx straight from the APK assets.
class SherpaTranscriber(private val recognizer: OnlineRecognizer) : Transcriber {
    override fun close() = recognizer.release()

    override fun transcribe(pcm: ShortArray): Transcriber.Result {
        val stream = recognizer.createStream()
        try {
            val x = toFloat(pcm)
            stream.acceptWaveform(amplify(x, gainFor(speechLevel(x))), RATE)
            stream.acceptWaveform(FloatArray(RATE / 2), RATE) // trailing silence flushes the last word
            stream.inputFinished()
            decode(stream)
            val r = recognizer.getResult(stream)
            return Transcriber.Result(text(r), words(r))
        } finally {
            stream.release()
        }
    }

    override fun live(onText: (String) -> Unit): Transcriber.Live = object : QueuedLive(onText) {
        private val stream = recognizer.createStream()
        private var peak = 0f // loudest chunk so far, to set the gain while recording

        override fun accept(chunk: ShortArray): String {
            val x = toFloat(chunk)
            peak = maxOf(peak, rms(x, 0, x.size))
            stream.acceptWaveform(amplify(x, gainFor(peak * PEAK_TO_SPEECH)), RATE)
            decode(stream)
            return text(recognizer.getResult(stream))
        }

        override fun end(): String {
            stream.acceptWaveform(FloatArray(RATE / 2), RATE)
            stream.inputFinished()
            decode(stream)
            return text(recognizer.getResult(stream))
        }

        override fun release() = stream.release()
    }

    private fun decode(stream: OnlineStream) {
        while (recognizer.isReady(stream)) recognizer.decode(stream)
    }

    companion object {
        private const val RATE = 16000
        private const val TOKEN_SEC = 0.25f // tokens carry only a start time; assume this length for the last one
        // The model hears nothing below about -50 dBFS, and the unprocessed mic path has no gain control,
        // so quiet takes came back empty. Speech is raised to about -20 dBFS first.
        private const val TARGET = 0.1f
        private const val MAX_GAIN = 100f
        private const val PEAK_TO_SPEECH = 0.5f // loudest 40 ms chunk vs typical speech level
        private const val FRAME = 640

        fun load(context: Context, assetDir: String): SherpaTranscriber {
            // The Vosk Korean model used before took about 250 MB of app storage.
            File(context.filesDir, "model-ko").deleteRecursively()
            val config = OnlineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = RATE, featureDim = 80),
                modelConfig = OnlineModelConfig(
                    transducer = OnlineTransducerModelConfig(
                        encoder = "$assetDir/encoder.int8.onnx",
                        decoder = "$assetDir/decoder.int8.onnx",
                        joiner = "$assetDir/joiner.int8.onnx",
                    ),
                    tokens = "$assetDir/tokens.txt",
                    numThreads = 2,
                ),
                enableEndpoint = false, // one stream per take
            )
            return SherpaTranscriber(OnlineRecognizer(context.assets, config))
        }

        private fun toFloat(pcm: ShortArray) = FloatArray(pcm.size) { pcm[it] / 32768f }

        private fun rms(x: FloatArray, from: Int, n: Int): Float {
            var s = 0.0
            for (i in from until from + n) s += x[i] * x[i]
            return if (n > 0) kotlin.math.sqrt(s / n).toFloat() else 0f
        }

        // Typical speech level of a take: the 90th percentile of 40 ms frame levels, so pauses do not count.
        private fun speechLevel(x: FloatArray): Float {
            val levels = (0 until x.size / FRAME).map { rms(x, it * FRAME, FRAME) }.sorted()
            return if (levels.isEmpty()) 0f else levels[levels.size * 9 / 10]
        }

        private fun gainFor(level: Float) = if (level <= 0f) 1f else (TARGET / level).coerceIn(1f, MAX_GAIN)

        private fun amplify(x: FloatArray, gain: Float) =
            if (gain == 1f) x else FloatArray(x.size) { (x[it] * gain).coerceIn(-1f, 1f) }

        // The model's text drops word spaces; tokens that start a word begin with a space.
        private fun text(r: OnlineRecognizerResult) = r.tokens.joinToString("").trim().replace(Regex(" +"), " ")

        private fun words(r: OnlineRecognizerResult): List<Word> {
            val out = mutableListOf<Word>()
            val cur = StringBuilder()
            var start = 0f
            var last = 0f
            fun flush(next: Float) {
                val t = cur.toString().trim()
                if (t.isNotEmpty()) out += Word(t, start, minOf(last + TOKEN_SEC, next))
                cur.clear()
            }
            r.tokens.forEachIndexed { i, tok ->
                val ts = r.timestamps.getOrElse(i) { last }
                if (cur.isBlank()) start = ts
                if (tok.startsWith(" ") && cur.isNotBlank()) { flush(ts); start = ts }
                cur.append(tok)
                last = ts
            }
            flush(Float.MAX_VALUE)
            return out
        }
    }
}
