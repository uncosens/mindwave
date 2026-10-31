package com.uncosens.mindwave.analysis

import com.uncosens.mindwave.Strings
import kotlin.math.abs
import kotlin.math.tanh

enum class Metric(
    val get: (VoiceFeatures) -> Float?, // null when the take had too little to measure
    val minSpread: (Float) -> Float,
    // Holds up when the sentence changes, so the daily sentence can use the anchor baseline.
    val textRobust: Boolean,
) {
    PITCH({ it.pitchMean }, { it * 0.04f }, true),
    PITCH_VAR({ it.pitchVar }, { 0.3f }, true),
    ENERGY({ it.loudness }, { 1.5f }, true),
    RATE({ it.rate }, { 0.25f }, false),
    PAUSES({ it.pauseRatio }, { 0.03f }, false),
    // Typical voices: jitter 0.2-1 %, shimmer 2-6 %.
    JITTER({ it.jitter }, { it * 0.2f + 0.05f }, true),
    SHIMMER({ it.shimmer }, { it * 0.2f + 0.3f }, true),
}

class Baseline(samples: List<VoiceFeatures>) {
    val size = samples.size
    private val values = Metric.entries.associateWith { m -> samples.mapNotNull(m.get) }
    private val center = values.mapValues { (_, v) -> median(v) }
    private val spread = Metric.entries.associateWith { m ->
        val c = center.getValue(m)
        maxOf(median(values.getValue(m).map { abs(it - c) }) * 1.4826f, m.minSpread(c))
    }

    // Median of the baseline, or null when no sample has this metric.
    fun center(m: Metric): Float? = if (values.getValue(m).isEmpty()) null else center.getValue(m)

    // Null when today's value or every baseline value is missing.
    fun z(m: Metric, f: VoiceFeatures): Float? {
        val v = m.get(f) ?: return null
        if (values.getValue(m).isEmpty()) return null
        return ((v - center.getValue(m)) / spread.getValue(m)).coerceIn(-4f, 4f)
    }

    private fun median(v: List<Float>): Float = v.sorted().let { if (it.isEmpty()) 0f else it[it.size / 2] }
}

data class Assessment(val z: Map<Metric, Float>, val arousal: Float, val valence: Float, val stability: Float)

object Analyzer {
    const val WINDOW_DAYS = 14

    // anchorBase: calibration + recent anchors. dailyBase: recent daily-sentence readings.
    fun assess(
        anchor: VoiceFeatures,
        daily: VoiceFeatures,
        anchorBase: Baseline,
        dailyBase: Baseline,
        text: Sentiment.Result,
        valenceWeights: Map<Metric, Float> = PersonalWeights.VALENCE_PRIOR,
        arousalWeights: Map<Metric, Float> = PersonalWeights.AROUSAL_PRIOR,
    ): Assessment {
        val z = Metric.entries.associateWith { m ->
            val parts = mutableListOf(anchorBase.z(m, anchor))
            if (m.textRobust) parts += anchorBase.z(m, daily)
            else if (dailyBase.size >= 3) parts += dailyBase.z(m, daily)
            parts.filterNotNull().let { if (it.isEmpty()) 0f else it.average().toFloat() }
        }
        fun v(m: Metric) = z.getValue(m)

        // Weights start hand-set and adapt to the user's own ratings over time.
        val arousal = tanh(arousalWeights.entries.sumOf { (m, w) -> (w * v(m)).toDouble() }.toFloat())
        // Weak signal, so text dominates when present. Weights adapt to the user over time.
        val voiceValence = tanh(valenceWeights.entries.sumOf { (m, w) -> (w * v(m)).toDouble() }.toFloat())
        val valence = if (text.hits > 0) 0.6f * text.score + 0.4f * voiceValence else voiceValence
        val strain = pos(v(Metric.JITTER)) + pos(v(Metric.SHIMMER)) + 0.5f * abs(v(Metric.PAUSES))
        val stability = 1f - tanh(strain / 2.5f)
        return Assessment(z, arousal, valence, stability)
    }

    // Summary such as "Calm, a little low" (design/DESIGN.md 4.8), in the app language.
    fun label(arousal: Float, valence: Float, stability: Float, s: Strings = Strings.En): String {
        val energy = when {
            arousal < -0.33f -> 0
            arousal > 0.33f -> 2
            else -> 1
        }
        val mood = when {
            valence < -0.5f -> 0
            valence < -0.15f -> 1
            valence <= 0.15f -> 2
            valence <= 0.5f -> 3
            else -> 4
        }
        return s.label(s.energyLevels[energy], s.moodLevels[mood], stability < 0.4f)
    }

    private fun pos(x: Float) = maxOf(x, 0f)
}
