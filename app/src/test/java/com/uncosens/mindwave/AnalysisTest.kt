package com.uncosens.mindwave

import com.uncosens.mindwave.analysis.Analyzer
import com.uncosens.mindwave.analysis.Baseline
import com.uncosens.mindwave.analysis.Capture
import com.uncosens.mindwave.analysis.Metric
import com.uncosens.mindwave.analysis.PersonalWeights
import com.uncosens.mindwave.analysis.Reject
import com.uncosens.mindwave.analysis.Word
import com.uncosens.mindwave.data.CheckIn
import com.uncosens.mindwave.data.Sentences
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

    // Source-filter voice: each cycle kicks a decaying 700 Hz resonance, with Gaussian period and
    // amplitude perturbation. Returns the signal and the true local jitter and shimmer (%).
    private fun voice(hz: Float, seconds: Float, jitterSd: Double, shimmerSd: Double, seed: Long): Triple<FloatArray, Float, Float> {
        val rnd = java.util.Random(seed)
        val out = FloatArray((Dsp.RATE * seconds).toInt())
        val starts = mutableListOf<Double>(); val amps = mutableListOf<Double>()
        var t = 0.0
        while (t < out.size) {
            starts += t; amps += 0.3 * (1 + shimmerSd * rnd.nextGaussian())
            t += Dsp.RATE / hz * (1 + jitterSd * rnd.nextGaussian())
        }
        var k = 0
        for (i in out.indices) {
            while (k + 1 < starts.size && starts[k + 1] <= i) k++
            var v = 0.0
            for (c in maxOf(k - 3, 0)..k) {
                val tau = (i - starts[c]) / Dsp.RATE
                v += amps[c] * kotlin.math.exp(-tau / 0.0015) * sin(2 * PI * 700 * tau)
            }
            out[i] = v.toFloat()
        }
        val p = starts.zipWithNext { x, y -> y - x }; val am = amps.dropLast(1)
        val jit = 100 * p.zipWithNext { x, y -> kotlin.math.abs(x - y) }.average() / p.average()
        val shim = 100 * am.zipWithNext { x, y -> kotlin.math.abs(x - y) }.average() / am.average()
        return Triple(out, jit.toFloat(), shim.toFloat())
    }

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

    @Test fun readProgressFollowsTheText() {
        val target = TextTools.words("“We were away all day, and we rode along the valley road.”")
        assertEquals(0, TextTools.readProgress(target, ""))
        assertEquals(4, TextTools.readProgress(target, "we were away all"))
        // A skipped word counts as read once a later one is heard; noise words are ignored.
        assertEquals(4, TextTools.readProgress(target, "we uh away all"))
        assertEquals(12, TextTools.readProgress(target, "we were away all day and we rode along the valley road"))
        // A wrong word far ahead does not jump the highlight.
        assertEquals(0, TextTools.readProgress(target, "road"))
        // Several misheard words in a row do not stall it.
        assertEquals(9, TextTools.readProgress(target, "we were xx yy zz qq we rode along"))
    }

    @Test fun sentimentDirection() {
        assertTrue(Sentiment.score("I had a really great day and I feel happy").score > 0.4f)
        assertTrue(Sentiment.score("I am stressed and so tired, it was awful").score < -0.4f)
        assertTrue(Sentiment.score("I am not happy").score < 0f)
        assertEquals(0, Sentiment.score("the bus left at noon").hits)
    }

    @Test fun koreanText() {
        assertEquals(106, TextTools.syllables(Sentences.ANCHOR_KO))
        // Compared by syllable, so different word spacing does not count as an error.
        assertEquals(1f, TextTools.accuracy("작은 다리가 있습니다.", "작은다리가 있습니다"), 0f)
        assertEquals(11f / 12f, TextTools.accuracy("우리는 아침 일찍 걸었습니다", "우리는 아침 일직 걸었습니다"), 1e-4f)
        val target = TextTools.words("“우리는 아침 일찍 마을 앞 냇가를 따라 걸었습니다.”")
        assertEquals(2, TextTools.readProgress(target, "우리는 아침"))
        assertEquals(2, TextTools.readProgress(target, "우리는아침 일"))
        assertEquals(8, TextTools.readProgress(target, "우리는 아침 일찍 마을 앞 냇가를 따라 걸었습니다"))
    }

    @Test fun koreanSentiment() {
        assertTrue(Sentiment.score("오늘 정말 즐거웠고 행복했어요").score > 0.4f)
        assertTrue(Sentiment.score("너무 피곤하고 스트레스 받아서 우울했어").score < -0.4f)
        assertTrue(Sentiment.score("오늘은 기분이 안 좋아").score < 0f)
        assertTrue(Sentiment.score("별로 좋지 않았어").score < 0f)
        assertEquals(0, Sentiment.score("버스가 정오에 떠났다").hits)
    }

    @Test fun labelInEachLanguage() {
        assertEquals("Calm, a little low", Analyzer.label(-0.5f, -0.3f, 1f))
        assertEquals("차분함, 조금 가라앉음", Analyzer.label(-0.5f, -0.3f, 1f, Strings.Ko))
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

    @Test fun sameRecordingGivesSameFeatures() {
        val rnd = java.util.Random(3)
        // A wobbling tone with light noise, so every feature has something to measure.
        val x = FloatArray(4000) + FloatArray(Dsp.RATE * 3) {
            val hz = 150f + 8f * sin(2 * PI * 5 * it / Dsp.RATE).toFloat()
            0.3f * sin(2 * PI * hz * it / Dsp.RATE).toFloat() + (rnd.nextGaussian() * 0.002).toFloat()
        } + FloatArray(4000)
        val words = listOf(Word("one", 0.3f, 0.8f), Word("two", 1f, 1.6f), Word("three", 2f, 3f))
        assertEquals(FeatureExtractor.extract(pcm(x), 10, 1f, words), FeatureExtractor.extract(pcm(x), 10, 1f, words))
    }

    @Test fun qualityGateRejectsClipping() {
        val loud = FloatArray(4000) + FloatArray(Dsp.RATE * 2) { (1.5f * sin(2 * PI * 150 * it / Dsp.RATE).toFloat()).coerceIn(-1f, 1f) } +
            FloatArray(4000)
        assertEquals(Capture.Bad(Reject.CLIPPED), FeatureExtractor.extract(pcm(loud), 10, 1f))
    }

    @Test fun arousalWeightsLearnFromEnergy() {
        val f = VoiceFeatures(150f, 2f, -25f, 4f, 0.15f, 0.95f, 1f, 8f)
        val rnd = java.util.Random(4)
        // This user pauses less on high-energy days; the prior gives pauses only a small weight.
        val days = (0 until 60).map { i ->
            val p = rnd.nextGaussian().toFloat()
            val energy = (3 - 2 * p).toInt().coerceIn(1, 5)
            CheckIn("d$i", f, f, "", 0f, 3, mapOf("PAUSES" to p), 0f, 0f, 1f, "", energy = energy)
        }
        assertTrue(PersonalWeights.arousal(days).getValue(Metric.PAUSES) < -0.3f)
        // Days without an energy rating are ignored.
        assertEquals(PersonalWeights.AROUSAL_PRIOR, PersonalWeights.arousal(days.map { it.copy(energy = null) }))
    }

    @Test fun qualityGateRejectsNoiseAndShortTakes() {
        val rnd = java.util.Random(1)
        val noise = FloatArray(Dsp.RATE * 3) { (rnd.nextGaussian() * 0.05).toFloat() }
        val noisy = FloatArray(noise.size) { noise[it] + if (it in 8000..40000) tone(150f, 3f)[it] * 0.4f else 0f }
        assertEquals(Capture.Bad(Reject.NOISY), FeatureExtractor.extract(pcm(noisy), 10, 1f))
        // 2 s of voicing is too little for a 74-syllable anchor.
        val short = FloatArray(4000) + tone(150f, 2f) + FloatArray(4000)
        assertEquals(Capture.Bad(Reject.TOO_SHORT), FeatureExtractor.extract(pcm(short), 74, 0.5f))
        // When the words show the whole text was read, the same take is accepted.
        assertTrue(FeatureExtractor.extract(pcm(short), 74, 0.9f) is Capture.Ok)
    }

    @Test fun wordTimingsDriveRateAndPauses() {
        val x = FloatArray(4000) + tone(150f, 3f) + FloatArray(4000)
        // Four one-syllable words, 0.25 s each, with one 0.5 s gap.
        val words = listOf(Word("one", 0.5f, 0.75f), Word("two", 0.75f, 1f), Word("three", 1.5f, 1.75f), Word("four", 1.75f, 2f))
        val f = (FeatureExtractor.extract(pcm(x), 4, 1f, words) as Capture.Ok).features
        assertEquals(4f, f.rate, 0.01f)
        assertEquals(0.5f / 1.5f, f.pauseRatio, 0.01f)
    }

    @Test fun cycleJitterAndShimmerMatchTheSignal() {
        for ((hz, seed) in listOf(120f to 3L, 210f to 4L)) {
            val (x, jit, shim) = voice(hz, 3f, 0.005, 0.04, seed)
            val f = (FeatureExtractor.extract(pcm(FloatArray(4000) + x + FloatArray(4000)), 10, 1f) as Capture.Ok).features
            assertEquals("jitter at $hz Hz", jit, f.jitter!!, jit * 0.15f)
            assertEquals("shimmer at $hz Hz", shim, f.shimmer!!, shim * 0.15f)
        }
        // A steady tone has almost no perturbation.
        val f = (FeatureExtractor.extract(pcm(FloatArray(4000) + tone(150f, 2f) + FloatArray(4000)), 10, 1f) as Capture.Ok).features
        assertTrue("jitter ${f.jitter}", f.jitter!! < 0.05f)
        assertTrue("shimmer ${f.shimmer}", f.shimmer!! < 0.2f)
        assertEquals(FeatureExtractor.VERSION, f.version)
    }

    @Test fun tooFewCyclesDropsTheValueNotTheTake() {
        // 0.7 s at 150 Hz is about 100 cycles, under the minimum.
        val x = FloatArray(4000) + tone(150f, 0.7f) + FloatArray(4000)
        val f = (FeatureExtractor.extract(pcm(x), 2, 1f) as Capture.Ok).features
        assertEquals(null, f.jitter)
        assertEquals(null, f.shimmer)
        val base = VoiceFeatures(150f, 2f, -25f, 4f, 0.15f, 0.95f, 0.5f, 4f)
        val baseline = Baseline(listOf(base, base.copy(jitter = 0.6f), base.copy(jitter = null)))
        assertEquals(null, baseline.z(Metric.JITTER, f))
        assertEquals(null, Baseline(listOf(base.copy(jitter = null))).z(Metric.JITTER, base))
        val r = Analyzer.assess(f, f, baseline, Baseline(emptyList()), Sentiment.score(""))
        assertEquals(0f, r.z.getValue(Metric.JITTER), 0f)
    }

    @Test fun personalWeightsLearnFromMood() {
        assertEquals(PersonalWeights.VALENCE_PRIOR, PersonalWeights.valence(emptyList()))
        val f = VoiceFeatures(150f, 2f, -25f, 4f, 0.15f, 0.95f, 1f, 8f)
        val rnd = java.util.Random(2)
        // This user sounds louder on good days; the prior ignores loudness.
        val days = (0 until 60).map { i ->
            val e = rnd.nextGaussian().toFloat()
            val mood = (3 + 2 * e).toInt().coerceIn(1, 5)
            CheckIn("d$i", f, f, "", 0f, mood, mapOf("ENERGY" to e), 0f, 0f, 1f, "")
        }
        val w = PersonalWeights.valence(days)
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
