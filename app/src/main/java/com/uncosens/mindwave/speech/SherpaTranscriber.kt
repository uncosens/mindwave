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

// Korean: sherpa-onnx streaming zipformer (int8), read straight from the APK assets.
// About 3x fewer errors than the small Vosk Korean model in our tests.
class SherpaTranscriber(private val recognizer: OnlineRecognizer) : Transcriber {
    override fun close() = recognizer.release()

    override fun transcribe(pcm: ShortArray): Transcriber.Result {
        val stream = recognizer.createStream()
        try {
            stream.acceptWaveform(toFloat(pcm), RATE)
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

        override fun accept(chunk: ShortArray): String {
            stream.acceptWaveform(toFloat(chunk), RATE)
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
