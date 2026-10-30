package com.uncosens.mindwave

import com.uncosens.mindwave.analysis.Analyzer
import com.uncosens.mindwave.analysis.Baseline
import com.uncosens.mindwave.analysis.Capture
import com.uncosens.mindwave.analysis.Metric
import com.uncosens.mindwave.analysis.PersonalWeights
import com.uncosens.mindwave.analysis.Reject
import com.uncosens.mindwave.analysis.Word
import com.uncosens.mindwave.data.CheckIn
import com.uncosens.mindwave.analysis.FeatureExtractor
import com.uncosens.mindwave.analysis.Sentiment
import com.uncosens.mindwave.analysis.TextTools
import com.uncosens.mindwave.analysis.VoiceFeatures
import com.uncosens.mindwave.audio.Dsp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class AnalysisTest {
    private fun tone(hz: Float, seconds: Float, amp: Float = 0.3f) =
        FloatArray((Dsp.RATE * seconds).toInt()) { amp * sin(2 * PI * hz * it / Dsp.RATE).toFloat() }

    private fun pcm(x: FloatArray) = ShortArray(x.size) { (x[it] * 32767).toInt().toShort() }

    @Test fun yinFindsPitch() {
        for (hz in listOf(110f, 180f, 250f)) assertEquals(hz, Dsp.yin(tone(hz, 0.04f), 0, 640), 2f)
        assertEquals(0f, Dsp.yin(FloatArray(640), 0, 640), 0f)
    }

    @Test fun accuracyAndSyllables() {
        assertEquals(1f, TextTools.accuracy("The cat sat.", "the cat sat"), 0f)
        assertEquals(2f / 3f, TextTools.accuracy("the cat sat", "the hat sat"), 1e-4f)
        assertEquals(0f, TextTools.accuracy("the cat sat", ""), 0f)
        assertEquals(3, TextTools.syllables("the table"))
        assertEquals(4, TextTools.syllables("reading these words"))
    }

    @Test fun sentimentDirection() {
        assertTrue(Sentiment.score("I had a really great day and I feel happy").score > 0.4f)
        assertTrue(Sentiment.score("I am stressed and so tired, it was awful").score < -0.4f)
        assertTrue(Sentiment.score("I am not happy").score < 0f)
        assertEquals(0, Sentiment.score("the bus left at noon").hits)
    }

    @Test fun extractorMeasuresSpeechAndPause() {
        // 1 s tone, 0.5 s silence, 1 s tone, with silence padding.
        val x = FloatArray(4000) + tone(150f, 1f) + FloatArray(8000) + tone(150f, 1f) + FloatArray(4000)
        val f = (FeatureExtractor.extract(pcm(x), syllables = 10, accuracy = 1f) as Capture.Ok).features
        assertEquals(150f, f.pitchMean, 3f)
        assertTrue("pitchVar ${f.pitchVar}", f.pitchVar < 0.2f)
        assertEquals(0.2f, f.pauseRatio, 0.05f)
        assertEquals(5f, f.rate, 0.5f)
        assertTrue(FeatureExtractor.extract(ShortArray(Dsp.RATE), 10, 1f) is Capture.Bad)
    }

    @Test fun qualityGateRejectsNoiseAndShortTakes() {
        val rnd = java.util.Random(1)
        val noise = FloatArray(Dsp.RATE * 3) { (rnd.nextGaussian() * 0.05).toFloat() }
        val noisy = FloatArray(noise.size) { noise[it] + if (it in 8000..40000) tone(150f, 3f)[it] * 0.4f else 0f }
        assertEquals(Capture.Bad(Reject.NOISY), FeatureExtractor.extract(pcm(noisy), 10, 1f))
        // 2 s of voicing is too little for a 74-syllable anchor.
        val short = FloatArray(4000) + tone(150f, 2f) + FloatArray(4000)
        assertEquals(Capture.Bad(Reject.TOO_SHORT), FeatureExtractor.extract(pcm(short), 74, 1f))
    }

    @Test fun wordTimingsDriveRateAndPauses() {
        val x = FloatArray(4000) + tone(150f, 3f) + FloatArray(4000)
        // Four one-syllable words, 0.25 s each, with one 0.5 s gap.
        val words = listOf(Word("one", 0.5f, 0.75f), Word("two", 0.75f, 1f), Word("three", 1.5f, 1.75f), Word("four", 1.75f, 2f))
        val f = (FeatureExtractor.extract(pcm(x), 4, 1f, words) as Capture.Ok).features
        assertEquals(4f, f.rate, 0.01f)
        assertEquals(0.5f / 1.5f, f.pauseRatio, 0.01f)
    }

    @Test fun personalWeightsLearnFromMood() {
        assertEquals(PersonalWeights.PRIOR, PersonalWeights.fit(emptyList()))
        val f = VoiceFeatures(150f, 2f, -25f, 4f, 0.15f, 0.95f, 1f, 8f)
        val rnd = java.util.Random(2)
        // This user sounds louder on good days; the prior ignores loudness.
        val days = (0 until 60).map { i ->
            val e = rnd.nextGaussian().toFloat()
            val mood = (3 + 2 * e).toInt().coerceIn(1, 5)
            CheckIn("d$i", f, f, "", 0f, mood, mapOf("ENERGY" to e), 0f, 0f, 1f, "")
        }
        val w = PersonalWeights.fit(days)
        assertTrue("energy weight ${w[Metric.ENERGY]}", w.getValue(Metric.ENERGY) > 0.3f)
    }

    @Test fun assessmentFollowsDeviation() {
        val base = VoiceFeatures(150f, 2f, -25f, 4f, 0.15f, 0.95f, 1f, 8f)
        val baseline = Baseline(listOf(base, base.copy(pitchMean = 152f), base.copy(pitchMean = 148f)))
        val excited = base.copy(pitchMean = 175f, loudness = -20f, rate = 5f, pitchVar = 3f)
        val r = Analyzer.assess(excited, excited, baseline, Baseline(emptyList()), Sentiment.score("great fun"))
        assertTrue(r.arousal > 0.5f)
        assertTrue(r.valence > 0.3f)
        val flat = Analyzer.assess(base, base, baseline, Baseline(emptyList()), Sentiment.score(""))
        assertEquals(0f, flat.arousal, 0.1f)
        assertEquals(1f, flat.stability, 0.05f)
    }
}
