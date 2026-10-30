package com.uncosens.mindwave.analysis

import kotlin.math.sqrt

// Small lexicon scorer in the style of VADER. Swap for an on-device model later.
object Sentiment {
    data class Result(val score: Float, val hits: Int) // score in -1..1

    private val lexicon: Map<String, Float> = buildMap {
        fun put(score: Float, vararg ws: String) = ws.forEach { put(it, score) }
        put(3f, "amazing", "wonderful", "fantastic", "excellent", "love", "loved", "thrilled", "awesome", "perfect", "joy", "delighted", "brilliant")
        put(2f, "happy", "great", "glad", "excited", "proud", "grateful", "thankful", "fun", "enjoy", "enjoyed", "beautiful",
            "relaxed", "peaceful", "hopeful", "confident", "good", "nice", "lovely", "calm", "rested", "refreshed", "laugh", "laughed", "success")
        put(1f, "fine", "okay", "ok", "better", "interesting", "like", "liked", "comfortable", "productive", "ready", "smile", "free", "easy", "win", "won")
        put(-1f, "tired", "bored", "busy", "meh", "confused", "slow", "late", "hard", "difficult", "problem", "boring", "sore", "weird")
        put(-2f, "sad", "stressed", "stress", "worried", "worry", "anxious", "upset", "angry", "annoyed", "lonely", "exhausted", "sick",
            "frustrated", "nervous", "scared", "hurt", "bad", "afraid", "disappointed", "overwhelmed", "pressure", "fail", "failed", "cry", "cried", "lost")
        put(-3f, "terrible", "awful", "horrible", "hate", "hated", "miserable", "depressed", "hopeless", "devastated", "worst", "panic")
    }
    private val negations = setOf("not", "no", "never", "dont", "don't", "isn't", "wasn't", "can't", "cannot", "didn't", "nothing", "hardly")
    private val boosters = setOf("very", "really", "so", "extremely", "super", "too", "totally")

    fun score(text: String): Result {
        val w = TextTools.words(text)
        var sum = 0f
        var hits = 0
        for (i in w.indices) {
            var s = lexicon[w[i]] ?: continue
            hits++
            if (i > 0 && w[i - 1] in boosters) s *= 1.3f
            if ((1..3).any { i - it >= 0 && w[i - it] in negations }) s *= -0.75f
            sum += s
        }
        return Result((sum / sqrt(sum * sum + 15f)), hits)
    }
}
