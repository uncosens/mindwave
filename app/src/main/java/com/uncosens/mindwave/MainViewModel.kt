package com.uncosens.mindwave

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uncosens.mindwave.analysis.Analyzer
import com.uncosens.mindwave.analysis.Baseline
import com.uncosens.mindwave.analysis.Capture
import com.uncosens.mindwave.analysis.FeatureExtractor
import com.uncosens.mindwave.analysis.PersonalWeights
import com.uncosens.mindwave.analysis.Reject
import com.uncosens.mindwave.analysis.Sentiment
import com.uncosens.mindwave.analysis.TextTools
import com.uncosens.mindwave.analysis.VoiceFeatures
import com.uncosens.mindwave.audio.Dsp
import com.uncosens.mindwave.audio.Recorder
import com.uncosens.mindwave.data.AppData
import com.uncosens.mindwave.data.CheckIn
import com.uncosens.mindwave.data.Passages
import com.uncosens.mindwave.data.Sentences
import com.uncosens.mindwave.data.Store
import com.uncosens.mindwave.speech.Transcriber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

sealed interface Screen {
    data object Loading : Screen
    data class Error(val text: String) : Screen
    data object Welcome : Screen
    data object CalibrationIntro : Screen
    data class Calibrate(val round: Int) : Screen
    data object Ready : Screen
    data object Home : Screen
    data object ReadAnchor : Screen
    data object ReadDaily : Screen
    data object FreeTalk : Screen
    data object Mood : Screen
    data object Analyzing : Screen
    data class Result(val checkIn: CheckIn) : Screen
    data object History : Screen
    data object Settings : Screen
}

// A failed capture: title, body, shown in place of the hint.
data class Problem(val title: String, val body: String)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)
    val recorder = Recorder(app)
    private var transcriber: Transcriber? = null

    var data by mutableStateOf(AppData()); private set
    var screen by mutableStateOf<Screen>(Screen.Loading); private set
    var recording by mutableStateOf(false); private set
    var busy by mutableStateOf(false); private set
    var problem by mutableStateOf<Problem?>(null); private set
    var confirmStop by mutableStateOf(false)
    var redoLeft by mutableStateOf(true); private set   // one restart per step
    var liveText by mutableStateOf(""); private set     // free-talk transcript so far
    var passage by mutableStateOf<String?>(null); private set // set when reading instead of talking
    private var live: Transcriber.Live? = null

    val dailySentence = Sentences.forDay()
    val freeTalkPrompt = Sentences.promptForDay()
    private val calibration = mutableListOf<VoiceFeatures>()
    private var anchor: VoiceFeatures? = null
    private var daily: VoiceFeatures? = null
    private var transcript = ""

    // Calibration only counts if it was done with the current anchor text and feature version.
    private val calibrated get() = data.calibration.isNotEmpty() && data.calibrationAnchor == Sentences.ANCHOR &&
        data.calibration.all { it.version == FeatureExtractor.VERSION }

    // Days before `date` that can join the baseline and the personal weights.
    private fun comparable(c: CheckIn, date: String) =
        c.date < date && c.anchorText == Sentences.ANCHOR && c.anchor.version == FeatureExtractor.VERSION

    // The anchor baseline as it was on that day, for showing usual values.
    fun baselineBefore(date: String) =
        Baseline((data.calibration + data.checkIns.filter { comparable(it, date) }.map { it.anchor }).takeLast(Analyzer.WINDOW_DAYS))

    // The user's own pitch centers the orb's color response.
    val basePitchHz: Float
        get() = data.calibration.map { it.pitchMean }.sorted().let { if (it.isEmpty()) 165f else it[it.size / 2] }

    init {
        viewModelScope.launch {
            try {
                val (loaded, t) = withContext(Dispatchers.IO) { store.load() to Transcriber.load(app) }
                data = loaded
                transcriber = t
                screen = when {
                    calibrated -> Screen.Home
                    loaded.calibration.isEmpty() -> Screen.Welcome
                    else -> Screen.CalibrationIntro // anchor text or feature version changed
                }
            } catch (e: Exception) {
                screen = Screen.Error("Speech model not found. Run scripts/fetch-model.ps1 and rebuild.\n\n${e.message}")
            }
        }
    }

    fun openCalibrationIntro() = go(Screen.CalibrationIntro)
    fun startCalibration() { calibration.clear(); go(Screen.Calibrate(1)) }
    fun startCheckIn() = go(Screen.ReadAnchor)
    fun openHistory() = go(Screen.History)
    fun openSettings() = go(Screen.Settings)
    fun openResult(c: CheckIn) = go(Screen.Result(c))

    fun goHome() {
        if (recording) discardRecording()
        confirmStop = false
        go(if (calibrated) Screen.Home else Screen.Welcome)
    }

    fun deleteAll() {
        store.clear()
        data = AppData()
        go(Screen.Welcome)
    }

    fun startRecording() {
        problem = null
        // Live text drives the free-talk transcript and the read-progress highlight.
        liveText = ""
        val l = transcriber!!.Live { text -> viewModelScope.launch { liveText = text } }
        live = l
        recorder.listener = l::feed
        recorder.start()
        recording = true
    }

    // Stops mid-way and throws the take away. Allowed once per step.
    fun redo() {
        if (!recording || !redoLeft) return
        discardRecording()
        redoLeft = false
        liveText = ""
    }

    fun choosePassage() { passage = Passages.random(passage); problem = null }
    fun talkInstead() { passage = null; problem = null }

    private fun discardRecording() {
        recorder.stop()
        recorder.listener = null
        recording = false
        live?.cancel()
        live = null
    }

    fun stopRecording() {
        if (!recording) return
        val pcm = recorder.stop()
        recorder.listener = null
        recording = false
        val step = screen
        val stream = live
        live = null
        busy = true
        viewModelScope.launch {
            // Reading steps need word timings, so they transcribe the whole take again.
            val talk = step == Screen.FreeTalk
            val result = withContext(Dispatchers.Default) {
                if (talk && stream != null) Transcriber.Result(stream.finish(), emptyList())
                else { stream?.cancel(); transcriber!!.transcribe(pcm) }
            }
            val heard = result.text
            if (talk) liveText = heard
            when (step) {
                is Screen.Calibrate -> measure(pcm, Sentences.ANCHOR, result)?.let { f ->
                    calibration += f
                    if (step.round < 3) go(Screen.Calibrate(step.round + 1)) else {
                        data = data.copy(calibration = calibration.toList(), calibrationAnchor = Sentences.ANCHOR)
                        persist()
                        go(Screen.Ready)
                    }
                }
                Screen.ReadAnchor -> measure(pcm, Sentences.ANCHOR, result)?.let { anchor = it; go(Screen.ReadDaily) }
                Screen.ReadDaily -> measure(pcm, dailySentence, result)?.let { daily = it; go(Screen.FreeTalk) }
                Screen.FreeTalk -> if (passage != null) {
                    if (TextTools.accuracy(passage!!, heard) < 0.3f) {
                        problem = Problem("That was a bit short.", "Please read the whole passage.")
                    } else {
                        transcript = heard
                        go(Screen.Mood)
                    }
                } else if (pcm.size < Dsp.RATE * 8 || TextTools.words(heard).size < 5) {
                    problem = Problem("Talk a little longer.", "Anything works — what you ate, what you saw.")
                } else {
                    transcript = heard
                    go(Screen.Mood)
                }
                else -> Unit
            }
            busy = false
        }
    }

    fun finish(mood: Int) {
        val a = anchor ?: return
        val d = daily ?: return
        go(Screen.Analyzing)
        viewModelScope.launch {
            delay(1200) // Let the analyzing orb show instead of flashing.
            val today = LocalDate.now().toString()
            val recent = data.checkIns.filter { comparable(it, today) }.takeLast(Analyzer.WINDOW_DAYS)
            val anchorBase = Baseline((data.calibration + recent.map { it.anchor }).takeLast(Analyzer.WINDOW_DAYS))
            val dailyBase = Baseline(recent.map { it.daily })
            // A read passage says nothing about mood, so only free talk is scored.
            val read = passage != null
            val text = if (read) Sentiment.Result(0f, 0) else Sentiment.score(transcript)
            // Weights come only from earlier days, so today's rating never scores today.
            val weights = PersonalWeights.fit(data.checkIns.filter { comparable(it, today) })
            val r = Analyzer.assess(a, d, anchorBase, dailyBase, text, weights)
            val checkIn = CheckIn(
                date = today, anchor = a, daily = d, transcript = transcript, textSentiment = text.score, mood = mood,
                z = r.z.mapKeys { it.key.name }, arousal = r.arousal, valence = r.valence, stability = r.stability,
                label = Analyzer.label(r.arousal, r.valence, r.stability), anchorText = Sentences.ANCHOR, readPassage = read,
            )
            data = data.copy(checkIns = data.checkIns.filter { it.date != today } + checkIn)
            persist()
            go(Screen.Result(checkIn))
        }
    }

    // Features of one read-aloud sentence, or null with a problem to show. Bad takes are never stored.
    private suspend fun measure(pcm: ShortArray, sentence: String, heard: Transcriber.Result): VoiceFeatures? {
        // Accuracy only checks that the right text was read; it is not part of the analysis.
        val accuracy = TextTools.accuracy(sentence, heard.text)
        if (accuracy < 0.3f) {
            problem = Problem("That didn't match the text.", "Please read the whole sentence as written.")
            return null
        }
        val c = withContext(Dispatchers.Default) {
            FeatureExtractor.extract(pcm, TextTools.syllables(sentence), accuracy, heard.words)
        }
        return when (c) {
            is Capture.Ok -> c.features
            is Capture.Bad -> {
                problem = when (c.reason) {
                    Reject.TOO_QUIET -> Problem("That was too quiet to analyze.", "Hold the phone about 20 cm from your mouth.")
                    Reject.NOISY -> Problem("It's noisy around you.", "Find a quieter spot and try again.")
                    Reject.TOO_SHORT -> Problem("That was a bit short.", "Please read the whole sentence.")
                }
                null
            }
        }
    }

    private fun persist() {
        val snapshot = data
        viewModelScope.launch(Dispatchers.IO) { store.save(snapshot) }
    }

    private fun go(s: Screen) {
        problem = null
        redoLeft = true
        liveText = ""
        if (s == Screen.ReadAnchor) passage = null
        screen = s
    }

    override fun onCleared() {
        if (recording) discardRecording()
    }
}
