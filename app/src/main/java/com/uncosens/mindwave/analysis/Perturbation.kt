package com.uncosens.mindwave.analysis

import com.uncosens.mindwave.audio.Dsp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

// Cycle-level jitter and shimmer, close to Praat's "To PointProcess (periodic, cc)" followed by
// "Get jitter (local)" and "Get shimmer (local)". YIN frames only guide the search; each glottal
// cycle is found by cross-correlating it with the previous one, refined to a fraction of a sample.
object Perturbation {
    const val MIN_CYCLES = 130       // fewer valid periods: drop the value, keep the sample
    private const val MIN_RUN = 5    // periods in a run before its edges are trimmed
    private const val PERIOD_FACTOR = 1.3f
    private const val AMP_FACTOR = 1.6f
    private const val MIN_CORR = 0.7f // cycle-to-cycle correlation; below this the run breaks

    data class Result(val jitter: Float?, val shimmer: Float?, val periods: Int)

    // A run of marked cycles: periods[i] is the time from mark i to mark i+1, amps has one per mark.
    private class Run(val periods: List<Float>, val amps: List<Float>)

    // pitch holds YIN Hz per frame (0 = unvoiced); frame i covers [i*hop, i*hop + win).
    fun measure(
        x: FloatArray, pitch: FloatArray, active: BooleanArray, hop: Int, win: Int,
        rate: Int = Dsp.RATE, fMin: Float = 70f, fMax: Float = 400f,
    ): Result {
        val runs = mutableListOf<Run>()
        var a = 0
        while (a < pitch.size) {
            if (!active[a] || pitch[a] <= 0f) { a++; continue }
            var b = a
            // Octave jumps end a voiced run.
            while (b + 1 < pitch.size && active[b + 1] && pitch[b + 1] > 0f && pitch[b] / pitch[b + 1] in 0.8f..1.25f) b++
            if (b > a) mark(x, pitch, a, b, hop, win, rate, fMin, fMax, runs)
            a = b + 1
        }

        val minT = 1f / fMax; val maxT = 1f / fMin
        var periodSum = 0.0; var periodN = 0
        var jitSum = 0.0; var jitN = 0
        var ampSum = 0.0; var ampN = 0
        var shimSum = 0.0; var shimN = 0
        for (r in runs) {
            if (r.periods.size < MIN_RUN) continue
            // Onsets and offsets are irregular, so keep the middle.
            val p = r.periods.subList(1, r.periods.size - 1)
            val amp = r.amps.subList(1, r.amps.size - 1)
            for (t in p) if (t in minT..maxT) { periodSum += t; periodN++ }
            for (i in 1 until p.size) {
                val t0 = p[i - 1]; val t1 = p[i]
                if (t0 !in minT..maxT || t1 !in minT..maxT || maxOf(t0, t1) / minOf(t0, t1) > PERIOD_FACTOR) continue
                jitSum += abs(t1 - t0); jitN++
            }
            for (v in amp) if (v > 0f) { ampSum += v; ampN++ }
            for (i in 1 until amp.size) {
                val v0 = amp[i - 1]; val v1 = amp[i]
                if (v0 <= 0f || v1 <= 0f || maxOf(v0, v1) / minOf(v0, v1) > AMP_FACTOR) continue
                shimSum += abs(v1 - v0); shimN++
            }
        }
        val jitter = if (jitN >= MIN_CYCLES) (100 * (jitSum / jitN) / (periodSum / periodN)).toFloat() else null
        val shimmer = if (shimN >= MIN_CYCLES) (100 * (shimSum / shimN) / (ampSum / ampN)).toFloat() else null
        return Result(jitter, shimmer, periodN)
    }

    // Walks one voiced run cycle by cycle. A weak match ends the current chain and starts a new one.
    private fun mark(
        x: FloatArray, pitch: FloatArray, a: Int, b: Int, hop: Int, win: Int,
        rate: Int, fMin: Float, fMax: Float, out: MutableList<Run>,
    ) {
        val start = a * hop + (win - hop) / 2
        val end = minOf(b * hop + (win + hop) / 2, x.size)
        // Expected period in samples from the nearest YIN frame.
        fun period(t: Int): Float {
            val f = ((t - win / 2f) / hop).roundToInt().coerceIn(a, b)
            return rate / pitch[f]
        }
        // Follow the stronger polarity so marks sit on the main peak of each cycle.
        var hi = 0f; var lo = 0f
        for (i in start until end) { hi = maxOf(hi, x[i]); lo = minOf(lo, x[i]) }
        val sign = if (hi >= -lo) 1f else -1f
        fun y(i: Int) = sign * x[i]

        var m = peak(x, sign, start, minOf(start + period(start).roundToInt(), end))
        var pos = m.toFloat() // exact mark position; m is the nearest sample
        var periods = mutableListOf<Float>()
        var amps = mutableListOf(amp(x, sign, m, period(m)))
        while (true) {
            val t = period(m)
            val h = (t / 2).roundToInt()
            val lagLo = maxOf(floor(t / 1.2f).toInt(), floor(rate / fMax).toInt())
            val lagHi = minOf(ceil(t * 1.2f).toInt(), ceil(rate / fMin).toInt())
            if (m - h < 0 || m + lagHi + h >= x.size || m + lagLo >= end) break
            // Normalized cross-correlation of this cycle with the next one at each lag.
            var e0 = 0.0
            for (k in -h..h) e0 += y(m + k) * y(m + k)
            val c = FloatArray(lagHi - lagLo + 1) { j ->
                val lag = lagLo + j
                var s = 0.0; var e1 = 0.0
                for (k in -h..h) { val v = y(m + lag + k); s += y(m + k) * v; e1 += v * v }
                if (e0 > 0 && e1 > 0) (s / sqrt(e0 * e1)).toFloat() else 0f
            }
            var best = 0
            for (j in 1 until c.size) if (c[j] > c[best]) best = j
            val lag = lagLo + best
            if (c[best] < MIN_CORR || best == 0 || best == c.size - 1 || m + lag >= end) {
                // Lost the cycle: keep what we have and restart one period later.
                if (periods.isNotEmpty()) out += Run(periods, amps)
                val from = m + t.roundToInt()
                if (from >= end) return
                m = peak(x, sign, from, minOf(from + period(from).roundToInt(), end))
                pos = m.toFloat()
                periods = mutableListOf()
                amps = mutableListOf(amp(x, sign, m, period(m)))
                continue
            }
            // Parabolic fit of the correlation peak gives the period to a fraction of a sample.
            val p0 = c[best - 1]; val p1 = c[best]; val p2 = c[best + 1]
            val den = p0 - 2 * p1 + p2
            val frac = if (den != 0f) ((p0 - p2) / (2 * den)).coerceIn(-0.5f, 0.5f) else 0f
            periods += (lag + frac) / rate
            // Correlation gives the shift between cycles, so add it to the exact position;
            // stepping from the rounded mark would drift off the peaks over hundreds of cycles.
            pos += lag + frac
            m = pos.roundToInt()
            amps += amp(x, sign, m, period(m))
        }
        if (periods.isNotEmpty()) out += Run(periods, amps)
    }

    private fun peak(x: FloatArray, sign: Float, from: Int, to: Int): Int {
        var best = from
        for (i in from until to) if (sign * x[i] > sign * x[best]) best = i
        return best
    }

    // Peak amplitude of the cycle around mark m, with a parabolic fit of the top.
    private fun amp(x: FloatArray, sign: Float, m: Int, t: Float): Float {
        val q = maxOf((t / 4).roundToInt(), 1)
        val i = peak(x, sign, maxOf(m - q, 1), minOf(m + q + 1, x.size - 1))
        val a = sign * x[i - 1]; val b = sign * x[i]; val c = sign * x[i + 1]
        val den = a - 2 * b + c
        return if (den < 0f) b - (a - c) * (a - c) / (8 * den) else b
    }
}
