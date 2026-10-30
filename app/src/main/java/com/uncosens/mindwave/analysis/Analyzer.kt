package com.uncosens.mindwave.analysis

import kotlin.math.abs
import kotlin.math.tanh

enum class Metric(
    val label: String,
    val get: (VoiceFeatures) -> Float,
    val minSpread: (Float) -> Float,
    // Holds up when the sentence changes, so the daily sentence can use the anchor baseline.
    val textRobust: Boolean,
) {
    PITCH("Pitch", { it.pitchMean }, { it * 0.04f }, true),
    PITCH_VAR("Pitch variation", { it.pitchVar }, { 0.3f }, true),
    ENERGY("Tone energy", { it.loudness }, { 1.5f }, true),
    RATE("Speech rate", { it.rate }, { 0.25f }, false),
    PAUSES("Breathing pauses", { it.pauseRatio }, { 0.03f }, false),
    JITTER("Pitch tremor", { it.jitter }, { it * 0.15f + 0.05f }, true),
    SHIMMER("Volume tremor", { it.shimmer }, { it * 0.15f + 0.5f }, true),
}

class Baseline(samples: List<VoiceFeatures>) {
    val size = samples.size
    private val center = Metric.entries.associateWith { m -> median(samples.map(m.get)) }
    private val spread = Metric.entries.associateWith { m ->
        val c = center.getValue(m)
        maxOf(median(samples.map { abs(m.get(it) - c) }) * 1.4826f, m.minSpread(c))
    }

    fun z(m: Metric, f: VoiceFeatures): Float = ((m.get(f) - center.getValue(m)) / spread.getValue(m)).coerceIn(-4f, 4f)

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
        valenceWeights: Map<Metric, Float> = PersonalWeights.PRIOR,
    ): Assessment {
        val z = Metric.entries.associateWith { m ->
            val parts = mutableListOf(anchorBase.z(m, anchor))
            if (m.textRobust) parts += anchorBase.z(m, daily)
            else if (dailyBase.size >= 3) parts += dailyBase.z(m, daily)
            parts.average().toFloat()
        }
        fun v(m: Metric) = z.getValue(m)

        // Heuristic weights: louder, higher, faster speech reads as more aroused.
        val arousal = tanh((0.35f * v(Metric.PITCH) + 0.3f * v(Metric.ENERGY) + 0.25f * v(Metric.RATE) +
            0.2f * v(Metric.PITCH_VAR) - 0.15f * v(Metric.PAUSES)) / 1.5f)
        // Weak signal, so text dominates when present. Weights adapt to the user over time.
        val voiceValence = tanh(valenceWeights.entries.sumOf { (m, w) -> (w * v(m)).toDouble() }.toFloat())
        val valence = if (text.hits > 0) 0.6f * text.score + 0.4f * voiceValence else voiceValence
        val strain = pos(v(Metric.JITTER)) + pos(v(Metric.SHIMMER)) + 0.5f * abs(v(Metric.PAUSES))
        val stability = 1f - tanh(strain / 2.5f)
        return Assessment(z, arousal, valence, stability)
    }

    // Summary such as "Calm, a little low" (design/DESIGN.md 4.8).
    fun label(arousal: Float, valence: Float, stability: Float): String {
        val energy = when {
            arousal < -0.33f -> "Calm"
            arousal > 0.33f -> "Energized"
            else -> "Balanced"
        }
        val mood = when {
            valence < -0.5f -> "low"
            valence < -0.15f -> "a little low"
            valence <= 0.15f -> "steady"
            valence <= 0.5f -> "a little bright"
            else -> "bright"
        }
        return "$energy, $mood" + if (stability < 0.4f) ", less steady than usual" else ""
    }

    private fun pos(x: Float) = maxOf(x, 0f)
}
