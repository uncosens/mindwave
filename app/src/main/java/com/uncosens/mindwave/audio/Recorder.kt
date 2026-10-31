package com.uncosens.mindwave.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import com.uncosens.mindwave.ui.VoiceOrbInput
import kotlin.concurrent.thread

// Records 16 kHz mono PCM and publishes live level and pitch for the orb.
class Recorder(context: Context) {
    private val unprocessed = (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager)
        .getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"

    val orb = VoiceOrbInput()
    // Stored with each reading, so only takes from the same mic path are compared.
    val sourceName = if (unprocessed) "UNPROCESSED" else "VOICE_RECOGNITION"
    // Gets a copy of every chunk on the recorder thread, e.g. for live speech-to-text.
    @Volatile var listener: ((ShortArray) -> Unit)? = null
    // True after a few seconds with nothing above a whisper (design/DESIGN.md 7, "Too quiet (live)").
    @Volatile var tooQuiet = false; private set

    @Volatile private var running = false
    private var worker: Thread? = null
    private var out = ShortArray(0)
    private var size = 0

    @SuppressLint("MissingPermission")
    fun start() {
        // UNPROCESSED skips the phone's gain control and noise suppression.
        val source = if (unprocessed) MediaRecorder.AudioSource.UNPROCESSED else MediaRecorder.AudioSource.VOICE_RECOGNITION
        val minBuf = AudioRecord.getMinBufferSize(Dsp.RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val rec = AudioRecord(source, Dsp.RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf, CHUNK * 8))
        out = ShortArray(Dsp.RATE * 30)
        size = 0
        running = true
        worker = thread(name = "recorder") {
            rec.startRecording()
            val buf = ShortArray(CHUNK)
            var lastVoice = 0L
            var lastHeard = System.nanoTime()
            while (running) {
                val n = rec.read(buf, 0, CHUNK)
                if (n <= 0) continue
                if (size + n > out.size) out = out.copyOf(out.size * 2)
                System.arraycopy(buf, 0, out, size, n)
                size += n
                listener?.invoke(buf.copyOf(n))
                val x = Dsp.toFloat(buf)
                val level = ((Dsp.db(Dsp.rms(x, 0, n)) + 60f) / 50f).coerceIn(0f, 1f)
                val now = System.nanoTime()
                if (level > 0.35f) lastVoice = now
                if (level >= QUIET_LEVEL) lastHeard = now
                tooQuiet = now - lastHeard > QUIET_NANOS
                orb.level = level
                orb.pitchHz = if (n == CHUNK) Dsp.yin(x, 0, n) else 0f
                orb.speaking = now - lastVoice < 250_000_000L
            }
            rec.stop()
            rec.release()
        }
    }

    fun stop(): ShortArray {
        running = false
        worker?.join()
        worker = null
        orb.level = 0f
        orb.pitchHz = 0f
        orb.speaking = false
        tooQuiet = false
        return out.copyOf(size)
    }

    private companion object {
        const val CHUNK = 640 // 40 ms
        const val QUIET_LEVEL = 0.08f // about -56 dBFS
        const val QUIET_NANOS = 3_000_000_000L
    }
}
