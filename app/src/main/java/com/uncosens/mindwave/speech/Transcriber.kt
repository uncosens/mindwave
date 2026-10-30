package com.uncosens.mindwave.speech

import android.content.Context
import com.uncosens.mindwave.analysis.Word
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

// Offline English speech-to-text with the bundled Vosk model.
class Transcriber(private val model: Model) {
    data class Result(val text: String, val words: List<Word>)

    fun transcribe(pcm: ShortArray): Result {
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
            return Result(parts.map { it.optString("text") }.filter { it.isNotBlank() }.joinToString(" "), words)
        } finally {
            rec.close()
        }
    }

    // Streams audio while recording and reports the text so far through onText.
    inner class Live(private val onText: (String) -> Unit) {
        private val queue = LinkedBlockingQueue<ShortArray>()
        private val rec = Recognizer(model, 16000f)
        private val done = StringBuilder()
        private val worker = thread(name = "live-stt") {
            while (true) {
                val chunk = queue.take()
                if (chunk.isEmpty()) break
                if (rec.acceptWaveForm(chunk, chunk.size)) {
                    append(JSONObject(rec.result).optString("text"))
                    onText(done.toString())
                } else {
                    val partial = JSONObject(rec.partialResult).optString("partial")
                    onText(if (partial.isEmpty()) done.toString() else "$done $partial".trim())
                }
            }
        }

        fun feed(chunk: ShortArray) = queue.put(chunk)

        // Ends the stream and returns the full text.
        fun finish(): String {
            stopWorker()
            append(JSONObject(rec.finalResult).optString("text"))
            rec.close()
            return done.toString()
        }

        fun cancel() {
            stopWorker()
            rec.close()
        }

        private fun stopWorker() {
            queue.put(ShortArray(0))
            worker.join()
        }

        private fun append(text: String) {
            if (text.isNotBlank()) done.append(if (done.isEmpty()) text else " $text")
        }
    }

    companion object {
        private const val ASSET_DIR = "model-en-us"

        // Vosk needs a real folder, so copy the model out of assets once.
        fun load(context: Context): Transcriber {
            val dir = File(context.filesDir, ASSET_DIR)
            val done = File(dir, ".copied")
            if (!done.exists()) {
                dir.deleteRecursively()
                copyAssets(context, ASSET_DIR, dir)
                done.createNewFile()
            }
            return Transcriber(Model(dir.absolutePath))
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
