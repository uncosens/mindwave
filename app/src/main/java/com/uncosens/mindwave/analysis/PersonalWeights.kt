package com.uncosens.mindwave.analysis

import com.uncosens.mindwave.data.CheckIn

// Learns per-user voice-valence weights from the daily 1-5 mood rating.
// Ridge regression pulled toward the hand-set prior: w = (XᵀX + λI)⁻¹ (Xᵀy + λ·w0).
object PersonalWeights {
    const val MIN_DAYS = 28
    private const val LAMBDA = 28f // at 28 days the data and the prior weigh about the same

    // Hand-set weights: flat, slow speech with long pauses leans negative.
    val PRIOR: Map<Metric, Float> = mapOf(
        Metric.PITCH_VAR to 0.4f / 1.2f,
        Metric.RATE to 0.2f / 1.2f,
        Metric.PAUSES to -0.3f / 1.2f,
        Metric.JITTER to -0.1f / 1.2f,
    )

    fun fit(history: List<CheckIn>): Map<Metric, Float> {
        if (history.size < MIN_DAYS) return PRIOR
        val ms = Metric.entries
        val k = ms.size
        val a = Array(k) { FloatArray(k) }
        val b = FloatArray(k)
        for (c in history) {
            val x = ms.map { c.z[it.name] ?: 0f }
            val y = (c.mood - 3) / 2f
            for (i in 0 until k) {
                b[i] += x[i] * y
                for (j in 0 until k) a[i][j] += x[i] * x[j]
            }
        }
        for (i in 0 until k) {
            a[i][i] += LAMBDA
            b[i] += LAMBDA * (PRIOR[ms[i]] ?: 0f)
        }
        val w = solve(a, b)
        return ms.indices.associate { ms[it] to w[it] }
    }

    // Gaussian elimination with partial pivoting. The ridge term keeps the matrix well conditioned.
    private fun solve(a: Array<FloatArray>, b: FloatArray): FloatArray {
        val n = b.size
        for (col in 0 until n) {
            val p = (col until n).maxBy { kotlin.math.abs(a[it][col]) }
            a[col] = a[p].also { a[p] = a[col] }
            b[col] = b[p].also { b[p] = b[col] }
            for (r in col + 1 until n) {
                val f = a[r][col] / a[col][col]
                for (c in col until n) a[r][c] -= f * a[col][c]
                b[r] -= f * b[col]
            }
        }
        val x = FloatArray(n)
        for (r in n - 1 downTo 0) {
            var s = b[r]
            for (c in r + 1 until n) s -= a[r][c] * x[c]
            x[r] = s / a[r][r]
        }
        return x
    }
}
