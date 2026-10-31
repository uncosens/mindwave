package com.uncosens.mindwave.data

import android.content.Context
import com.uncosens.mindwave.analysis.VoiceFeatures
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class CheckIn(
    val date: String, // ISO yyyy-MM-dd
    val anchor: VoiceFeatures,
    val daily: VoiceFeatures,
    val transcript: String,
    val textSentiment: Float,
    val mood: Int, // self-rated 1..5
    val z: Map<String, Float>, // keyed by Metric.name
    val arousal: Float,
    val valence: Float,
    val stability: Float,
    val label: String,
    val anchorText: String = "", // anchor read that day; only matching days join the baseline
    val readPassage: Boolean = false, // read a passage instead of free talk, so no text sentiment
    val energy: Int? = null, // self-rated 1..5; null on older check-ins
    val transcriptRaw: String = "", // as recognized, before the user's corrections
    val device: String = "", // phone model and mic source; only matching days are compared
    val source: String = "",
    val baselineSize: Int = 0, // anchor readings the day was compared against
)

// App language. Each has its own speech model, texts and baseline.
enum class Lang(val modelDir: String) { EN("model-en-us"), KO("model-ko") }

@Serializable
data class AppData(
    val language: Lang? = null, // null until picked on first launch
    val calibration: List<VoiceFeatures> = emptyList(),
    val calibrationAnchor: String = "", // anchor text used for calibration
    val calibrationDevice: String = "",
    val calibrationSource: String = "",
    val checkIns: List<CheckIn> = emptyList(), // oldest first, one per date
)

// Everything lives in one JSON file in app storage. No network.
class Store(context: Context) {
    private val file = File(context.filesDir, "data.json")
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): AppData = if (file.exists()) json.decodeFromString(file.readText()) else AppData()

    fun save(data: AppData) {
        val tmp = File(file.path + ".tmp")
        tmp.writeText(json.encodeToString(data))
        tmp.renameTo(file)
    }

    fun clear() {
        file.delete()
    }
}
