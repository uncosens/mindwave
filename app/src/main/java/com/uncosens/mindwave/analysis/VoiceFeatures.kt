package com.uncosens.mindwave.analysis

import com.uncosens.mindwave.audio.Dsp
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.sqrt

@Serializable
data class VoiceFeatures(
    val pitchMean: Float,   // Hz, median of voiced frames
    val pitchVar: Float,    // semitone std
    val loudness: Float,    // mean dBFS of speech frames
    val rate: Float,        // syllables per second of speaking time
    val pauseRatio: Float,  // pause time / speech span
    val accuracy: Float,    // 1 - word error rate
    // Frame-level tremor proxies, not clinical cycle-level jitter/shimmer.
    val jitter: Float,      // % pitch period change between frames
    val shimmer: Float,     // % amplitude change between frames
)

// One recognized word with times in seconds.
data class Word(val text: String, val start: Float, val end: Float)

enum class Reject { TOO_QUIET, NOISY, TOO_SHORT }

sealed interface Capture {
    data class Ok(val features: VoiceFeatures) : Capture
    data class Bad(val reason: Reject) : Capture
}

object FeatureExtractor {
    const val MIN_SNR_DB = 15f
    const val MIN_SPEECH_DB = -45f
    const val VOICED_SEC_PER_SYLLABLE = 0.09f // 74-syllable anchor needs about 6.7 s of voicing
    private const val HOP = 320      // 20 ms
    private const val WIN = 640      // 40 ms
    private const val MIN_PAUSE = 13 // frames, about 260 ms

    // Rejects quiet, noisy or too short takes so they never reach the baseline.
    // With word timings, rate and pauses come from the words; otherwise from energy.
    fun extract(pcm: ShortArray, syllables: Int, accuracy: Float, words: List<Word> = emptyList()): Capture {
        val x = Dsp.toFloat(pcm)
        val frames = (x.size - WIN) / HOP
        if (frames < 25) return Capture.Bad(Reject.TOO_SHORT)
        val rms = FloatArray(frames) { Dsp.rms(x, it * HOP, WIN) }
        val pitch = FloatArray(frames) { Dsp.yin(x, it * HOP, WIN) }

        val floor = rms.sorted()[frames / 10]
        val thr = maxOf(floor * 2.5f, 0.004f)
        val active = BooleanArray(frames) { rms[it] > thr }
        val first = active.indexOfFirst { it }
        val last = active.indexOfLast { it }
        // Nothing rises above the floor: blame noise if the floor itself is loud.
        if (first < 0 || active.count { it } < 15) return Capture.Bad(if (Dsp.db(floor) > -40f) Reject.NOISY else Reject.TOO_QUIET)

        var pauseFrames = 0
        var run = 0
        for (i in first..last) {
            if (!active[i]) run++ else {
                if (run >= MIN_PAUSE) pauseFrames += run
                run = 0
            }
        }
        val spanFrames = last - first + 1
        val speakSec = (spanFrames - pauseFrames) * HOP / Dsp.RATE.toFloat()

        val voiced = (first..last).filter { active[it] && pitch[it] > 0f }
        if (voiced.size * HOP / Dsp.RATE.toFloat() < maxOf(syllables * VOICED_SEC_PER_SYLLABLE, 0.2f)) return Capture.Bad(Reject.TOO_SHORT)
        val loudness = (first..last).filter { active[it] }.map { Dsp.db(rms[it]) }.average().toFloat()
        if (loudness < MIN_SPEECH_DB) return Capture.Bad(Reject.TOO_QUIET)
        if (loudness - Dsp.db(floor) < MIN_SNR_DB) return Capture.Bad(Reject.NOISY)
        val pitches = voiced.map { pitch[it] }.sorted()
        val median = pitches[pitches.size / 2]
        val st = pitches.map { Dsp.semitones(it, median) }
        val stMean = st.average().toFloat()
        val pitchVar = sqrt(st.sumOf { ((it - stMean) * (it - stMean)).toDouble() } / st.size).toFloat()

        // Compare neighbouring voiced frames; skip octave errors and big glides.
        val periodDiffs = mutableListOf<Float>()
        val ampDiffs = mutableListOf<Float>()
        for (k in 1 until voiced.size) {
            val a = voiced[k - 1]; val b = voiced[k]
            if (b != a + 1) continue
            val ratio = pitch[a] / pitch[b]
            if (ratio !in 0.8f..1.25f) continue
            periodDiffs += abs(1f / pitch[a] - 1f / pitch[b])
            ampDiffs += abs(rms[a] - rms[b])
        }
        val meanPeriod = voiced.map { 1f / pitch[it] }.average().toFloat()
        val meanAmp = voiced.map { rms[it] }.average().toFloat()

        val timed = words.size >= 3
        val wordSec = words.sumOf { (it.end - it.start).toDouble() }.toFloat()
        val wordSpan = if (timed) words.last().end - words.first().start else 0f
        val gaps = words.zipWithNext { a, b -> b.start - a.end }.filter { it >= MIN_PAUSE * HOP / Dsp.RATE.toFloat() }.sum()

        return Capture.Ok(VoiceFeatures(
            pitchMean = median,
            pitchVar = pitchVar,
            loudness = loudness,
            rate = when {
                timed && wordSec > 0f -> words.sumOf { TextTools.syllables(it.text) } / wordSec
                speakSec > 0f -> syllables / speakSec
                else -> 0f
            },
            pauseRatio = if (timed && wordSpan > 0f) gaps / wordSpan else pauseFrames / spanFrames.toFloat(),
            accuracy = accuracy,
            jitter = if (periodDiffs.isEmpty()) 0f else 100f * periodDiffs.average().toFloat() / meanPeriod,
            shimmer = if (ampDiffs.isEmpty()) 0f else 100f * ampDiffs.average().toFloat() / meanAmp,
        ))
    }
}

object TextTools {
    fun words(text: String): List<String> =
        text.lowercase().replace(Regex("[^a-z' ]"), " ").split(' ').filter { it.isNotBlank() }

    // Rough English syllable count: vowel groups, minus a silent final "e".
    fun syllables(text: String): Int = words(text).sumOf { w ->
        var n = Regex("[aeiouy]+").findAll(w).count()
        if (w.length > 2 && w.endsWith("e") && !w.endsWith("le") && n > 1) n--
        maxOf(n, 1)
    }

    // 1 - word error rate, clamped to 0..1.
    fun accuracy(reference: String, heard: String): Float {
        val r = words(reference); val h = words(heard)
        if (r.isEmpty()) return 0f
        var prev = IntArray(h.size + 1) { it }
        for (i in 1..r.size) {
            val cur = IntArray(h.size + 1)
            cur[0] = i
            for (j in 1..h.size) {
                val sub = prev[j - 1] + if (r[i - 1] == h[j - 1]) 0 else 1
                cur[j] = minOf(sub, prev[j] + 1, cur[j - 1] + 1)
            }
            prev = cur
        }
        return (1f - prev[h.size] / r.size.toFloat()).coerceIn(0f, 1f)
    }
}
