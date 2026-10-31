package com.uncosens.mindwave.analysis

import com.uncosens.mindwave.audio.Dsp
import kotlinx.serialization.Serializable
import kotlin.math.sqrt

@Serializable
data class VoiceFeatures(
    val pitchMean: Float,   // Hz, median of voiced frames
    val pitchVar: Float,    // semitone std
    val loudness: Float,    // mean dBFS of speech frames
    val rate: Float,        // syllables per second of speaking time
    val pauseRatio: Float,  // pause time / speech span
    val accuracy: Float,    // 1 - word error rate
    // Cycle-level, as in Praat (local). Null when too few clean cycles were found.
    val jitter: Float?,     // % period change between neighbouring cycles
    val shimmer: Float?,    // % peak amplitude change between neighbouring cycles
    // 1: frame-level jitter/shimmer from before FeatureExtractor.VERSION 2. Not comparable.
    val version: Int = 1,
)

// One recognized word with times in seconds.
data class Word(val text: String, val start: Float, val end: Float)

enum class Reject { TOO_QUIET, NOISY, TOO_SHORT, CLIPPED }

sealed interface Capture {
    data class Ok(val features: VoiceFeatures) : Capture
    data class Bad(val reason: Reject) : Capture {
        var detail = "" // for logs only; not part of equality
    }
}

object FeatureExtractor {
    const val VERSION = 2 // bump when a feature changes scale; old baselines then need recalibration
    const val MIN_SNR_DB = 15f
    const val MIN_SPEECH_DB = -45f
    const val VOICED_SEC_PER_SYLLABLE = 0.09f // 74-syllable anchor needs about 6.7 s of voicing
    const val VOICED_SEC_PER_SYLLABLE_KO = 0.05f // Korean syllables are short; 106-syllable anchor needs about 5.3 s
    private const val MIN_VOICED_SEC = 0.5f
    private const val CLIP_LEVEL = 32700 // at or near 16-bit full scale
    private const val MAX_CLIPPED = 0.001f // share of speech samples
    private const val FULL_READ = 0.8f // reading accuracy that counts as having read the whole text
    private const val HOP = 320      // 20 ms
    private const val WIN = 640      // 40 ms
    private const val MIN_PAUSE = 13 // frames, about 260 ms

    // Rejects quiet, noisy or too short takes so they never reach the baseline.
    // With word timings, rate and pauses come from the words; otherwise from energy.
    fun extract(
        pcm: ShortArray, syllables: Int, accuracy: Float, words: List<Word> = emptyList(),
        secPerSyllable: Float = VOICED_SEC_PER_SYLLABLE,
        reachedEnd: Boolean = false, // the transcript got to the end of the text
    ): Capture {
        val x = Dsp.toFloat(pcm)
        val frames = (x.size - WIN) / HOP
        if (frames < 25) return Capture.Bad(Reject.TOO_SHORT).apply { detail = "take ${pcm.size} samples" }
        val rms = FloatArray(frames) { Dsp.rms(x, it * HOP, WIN) }
        val pitch = FloatArray(frames) { Dsp.yin(x, it * HOP, WIN) }

        val floor = rms.sorted()[frames / 10]
        val thr = maxOf(floor * 2.5f, 0.004f)
        val active = BooleanArray(frames) { rms[it] > thr }
        val first = active.indexOfFirst { it }
        val last = active.indexOfLast { it }
        // Nothing rises above the floor: blame noise if the floor itself is loud.
        if (first < 0 || active.count { it } < 15) return Capture.Bad(if (Dsp.db(floor) > -40f) Reject.NOISY else Reject.TOO_QUIET)

        // Clipped samples flatten the peaks that pitch and tremor are measured from.
        val from = first * HOP
        val to = minOf(last * HOP + WIN, pcm.size)
        val clipped = (from until to).count { pcm[it] >= CLIP_LEVEL || pcm[it] <= -CLIP_LEVEL }
        if (clipped > (to - from) * MAX_CLIPPED) return Capture.Bad(Reject.CLIPPED)

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
        // Pitch tracking misses some voiced frames on real phones, so when the words show the whole
        // text was read, only a small floor of voicing is needed for the pitch statistics.
        val voicedSec = voiced.size * HOP / Dsp.RATE.toFloat()
        val needed = if (accuracy >= FULL_READ || reachedEnd) MIN_VOICED_SEC else maxOf(syllables * secPerSyllable, MIN_VOICED_SEC)
        if (voicedSec < needed) return Capture.Bad(Reject.TOO_SHORT).apply { detail = "voiced $voicedSec s of $needed s" }
        val loudness = (first..last).filter { active[it] }.map { Dsp.db(rms[it]) }.average().toFloat()
        if (loudness < MIN_SPEECH_DB) return Capture.Bad(Reject.TOO_QUIET)
        if (loudness - Dsp.db(floor) < MIN_SNR_DB) return Capture.Bad(Reject.NOISY)
        val pitches = voiced.map { pitch[it] }.sorted()
        val median = pitches[pitches.size / 2]
        val st = pitches.map { Dsp.semitones(it, median) }
        val stMean = st.average().toFloat()
        val pitchVar = sqrt(st.sumOf { ((it - stMean) * (it - stMean)).toDouble() } / st.size).toFloat()

        val cycles = Perturbation.measure(x, pitch, active, HOP, WIN)

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
            jitter = cycles.jitter,
            shimmer = cycles.shimmer,
            version = VERSION,
        ))
    }
}

object TextTools {
    fun words(text: String): List<String> =
        text.lowercase().replace(Regex("[^\\p{L}\\p{N}' ]"), " ").split(' ').filter { it.isNotBlank() }

    private fun hangul(c: Char) = c in '가'..'힣'

    // Korean: one Hangul block is one syllable. English: vowel groups, minus a silent final "e".
    fun syllables(text: String): Int = words(text).sumOf { w ->
        val h = w.count(::hangul)
        if (h > 0) return@sumOf h
        var n = Regex("[aeiouy]+").findAll(w).count()
        if (w.length > 2 && w.endsWith("e") && !w.endsWith("le") && n > 1) n--
        maxOf(n, 1)
    }

    // Units for comparing texts: syllables for Korean, since word spacing varies; words otherwise.
    private fun units(text: String): List<String> =
        if (text.any(::hangul)) words(text).flatMap { w -> w.map { it.toString() } } else words(text)

    // How many words of the target have been read, from a live transcript: the target prefix
    // closest (edit distance) to everything heard, so misheard words do not stall it.
    fun readProgress(target: List<String>, heard: String): Int {
        if (target.none { w -> w.any(::hangul) }) return prefix(target, words(heard))
        // Korean: align by syllable, then count the words fully covered.
        val read = prefix(target.flatMap { w -> w.map { it.toString() } }, units(heard))
        return target.runningFold(0) { n, w -> n + w.length }.drop(1).count { it <= read }
    }

    private fun prefix(target: List<String>, h: List<String>): Int {
        if (h.isEmpty()) return 0
        // col[i] = distance between target[0, i) and the heard units so far.
        var col = IntArray(target.size + 1) { it }
        for (w in h) {
            val next = IntArray(target.size + 1)
            next[0] = col[0] + 1
            for (i in 1..target.size) {
                next[i] = minOf(col[i - 1] + if (target[i - 1] == w) 0 else 1, col[i] + 1, next[i - 1] + 1)
            }
            col = next
        }
        // On a tie keep the shorter prefix, so a misheard last word does not count as read.
        return col.indices.minBy { col[it] }
    }

    // True when the transcript reaches the last words of the text, even with errors along the way.
    // Recognition mistakes lower accuracy, but only an early stop leaves the end unread.
    fun reachedEnd(text: String, heard: String): Boolean {
        val target = words(text)
        return target.isNotEmpty() && readProgress(target, heard) >= target.size * 0.85f
    }

    // 1 - error rate (words, or syllables for Korean), clamped to 0..1.
    fun accuracy(reference: String, heard: String): Float {
        val r = units(reference); val h = units(heard)
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
