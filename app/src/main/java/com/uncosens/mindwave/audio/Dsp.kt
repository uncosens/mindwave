package com.uncosens.mindwave.audio

import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.sqrt

object Dsp {
    const val RATE = 16000

    fun toFloat(pcm: ShortArray): FloatArray = FloatArray(pcm.size) { pcm[it] / 32768f }

    fun rms(x: FloatArray, from: Int, n: Int): Float {
        var s = 0.0
        for (i in from until from + n) s += x[i] * x[i]
        return sqrt(s / n).toFloat()
    }

    fun db(rms: Float): Float = 20f * log10(rms.coerceAtLeast(1e-6f))

    fun semitones(hz: Float, ref: Float): Float = (12.0 * ln((hz / ref).toDouble()) / ln(2.0)).toFloat()

    // YIN pitch estimate in Hz, or 0 when the frame is unvoiced.
    fun yin(x: FloatArray, from: Int, n: Int, rate: Int = RATE, fMin: Float = 70f, fMax: Float = 400f): Float {
        val tauMin = (rate / fMax).toInt()
        val tauMax = min((rate / fMin).toInt(), n / 2)
        val w = n - tauMax
        val d = FloatArray(tauMax + 1)
        for (tau in 1..tauMax) {
            var s = 0f
            for (i in 0 until w) {
                val diff = x[from + i] - x[from + i + tau]
                s += diff * diff
            }
            d[tau] = s
        }
        d[0] = 1f
        var sum = 0f
        for (tau in 1..tauMax) {
            sum += d[tau]
            d[tau] = if (sum > 0f) d[tau] * tau / sum else 1f
        }
        var tau = tauMin
        while (tau < tauMax) {
            if (d[tau] < 0.15f) {
                while (tau + 1 < tauMax && d[tau + 1] < d[tau]) tau++
                // Parabolic fit around the minimum for sub-sample accuracy.
                val a = d[tau - 1]; val b = d[tau]; val c = d[tau + 1]
                val den = a - 2 * b + c
                val t = if (den != 0f) tau + (a - c) / (2 * den) else tau.toFloat()
                return rate / t
            }
            tau++
        }
        return 0f
    }
}
