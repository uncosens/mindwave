package com.uncosens.mindwave.speech

import android.content.Context
import com.uncosens.mindwave.analysis.Word
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File

// English: Vosk small model with word timestamps.
class VoskTranscriber(private val model: Model) : Transcriber {
    override fun close() = model.close()

    override fun transcribe(pcm: ShortArray): Transcriber.Result {
        val rec = Recognizer(model, 16000f)
        try {
            rec.setWords(true)
            // Feed in small chunks: Vosk splits long audio at pauses and returns each part once.
            val parts = mutableListOf<JSONObject>()
            var i = 0
            while (i < pcm.size) {
                val n = minOf(3200, pcm.size - i)
                if (rec.acceptWaveForm(pcm.copyOfRange(i, i + n), n)) parts += JSONObject(rec.result)
                i += n
            }
            parts += JSONObject(rec.finalResult)
            val words = parts.flatMap { p ->
                val arr = p.optJSONArray("result") ?: return@flatMap emptyList()
                (0 until arr.length()).map { k ->
                    arr.getJSONObject(k).let { Word(it.getString("word"), it.getDouble("start").toFloat(), it.getDouble("end").toFloat()) }
                }
            }
            return Transcriber.Result(parts.map { it.optString("text") }.filter { it.isNotBlank() }.joinToString(" "), words)
        } finally {
            rec.close()
        }
    }

    override fun live(onText: (String) -> Unit): Transcriber.Live = object : QueuedLive(onText) {
        private val rec = Recognizer(model, 16000f)
        private val done = StringBuilder()

        override fun accept(chunk: ShortArray): String {
            if (rec.acceptWaveForm(chunk, chunk.size)) {
                append(JSONObject(rec.result).optString("text"))
                return done.toString()
            }
            val partial = JSONObject(rec.partialResult).optString("partial")
            return if (partial.isEmpty()) done.toString() else "$done $partial".trim()
        }

        override fun end(): String {
            append(JSONObject(rec.finalResult).optString("text"))
            return done.toString()
        }

        override fun release() = rec.close()

        private fun append(text: String) {
            if (text.isNotBlank()) done.append(if (done.isEmpty()) text else " $text")
        }
    }

    companion object {
        // Vosk needs a real folder, so copy the model out of assets once.
        fun load(context: Context, assetDir: String): VoskTranscriber {
            val dir = File(context.filesDir, assetDir)
            val done = File(dir, ".copied")
            if (!done.exists()) {
                dir.deleteRecursively()
                copyAssets(context, assetDir, dir)
                done.createNewFile()
            }
            return VoskTranscriber(Model(dir.absolutePath))
        }

        private fun copyAssets(context: Context, path: String, target: File) {
            val children = context.assets.list(path).orEmpty()
            if (children.isEmpty()) {
                target.parentFile?.mkdirs()
                context.assets.open(path).use { input -> target.outputStream().use { input.copyTo(it) } }
                return
            }
            target.mkdirs()
            for (c in children) copyAssets(context, "$path/$c", File(target, c))
        }
    }
}
