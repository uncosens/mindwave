package com.uncosens.mindwave.speech

import android.content.Context
import com.uncosens.mindwave.analysis.Word
import com.uncosens.mindwave.data.Lang
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

// Offline speech-to-text for the app language: Vosk for English, sherpa-onnx for Korean.
interface Transcriber {
    data class Result(val text: String, val words: List<Word>)

    // Streams audio while recording and reports the text so far through onText.
    interface Live {
        fun feed(chunk: ShortArray)
        fun finish(): String // ends the stream and returns the full text
        fun cancel()
    }

    fun transcribe(pcm: ShortArray): Result
    fun live(onText: (String) -> Unit): Live
    fun close()

    companion object {
        fun load(context: Context, lang: Lang): Transcriber =
            if (lang == Lang.KO) SherpaTranscriber.load(context, lang.modelDir) else VoskTranscriber.load(context, lang.modelDir)
    }
}

// Runs recognition on its own thread so the recorder never waits for the decoder.
abstract class QueuedLive(private val onText: (String) -> Unit) : Transcriber.Live {
    private val queue = LinkedBlockingQueue<ShortArray>()
    private val worker = thread(name = "live-stt") {
        while (true) {
            val chunk = queue.take()
            if (chunk.isEmpty()) break
            onText(accept(chunk))
        }
    }

    protected abstract fun accept(chunk: ShortArray): String // returns the text so far
    protected abstract fun end(): String
    protected abstract fun release()

    override fun feed(chunk: ShortArray) = queue.put(chunk)

    override fun finish(): String {
        stopWorker()
        return end().also { release() }
    }

    override fun cancel() {
        stopWorker()
        release()
    }

    private fun stopWorker() {
        queue.put(ShortArray(0))
        worker.join()
    }
}
