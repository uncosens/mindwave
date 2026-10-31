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

    // Korean words take endings ("슬펐어요", "좋았다"), so entries are stems matched at the start of a word.
    // Longest stems are tried first, so "재미없" wins over shorter ones.
    private val stemsKo: List<Pair<String, Float>> = buildList {
        fun put(score: Float, vararg ws: String) = ws.forEach { add(it to score) }
        put(3f, "최고", "행복", "사랑", "신나", "신났", "기뻐", "기쁘", "기뻤", "감동", "완벽", "환상")
        put(2f, "좋", "즐거", "즐겁", "즐겼", "재밌", "재미있", "뿌듯", "감사", "고마", "고맙", "설레", "설렜", "편안", "상쾌",
            "개운", "만족", "든든", "웃었", "웃음", "평화", "여유", "희망", "자신감", "기대돼", "기대된")
        put(1f, "괜찮", "나아", "나았", "무난", "편해", "편했", "쉬웠", "쉬워")
        put(-1f, "피곤", "졸려", "졸리", "지루", "심심", "바빠", "바빴", "바쁘", "귀찮", "아파", "아프", "아팠", "어려웠", "어려워", "애매")
        put(-2f, "슬퍼", "슬프", "슬펐", "우울", "스트레스", "걱정", "불안", "화나", "화났", "짜증", "외로", "외롭", "지쳐", "지쳤",
            "지치", "속상", "답답", "긴장", "무서", "무섭", "두려", "실망", "부담", "망했", "울었", "울고", "울컥", "싫", "나빠", "나쁘",
            "나빴", "힘들", "힘든", "괴로", "괴롭", "재미없", "외로웠")
        put(-3f, "최악", "끔찍", "비참", "절망", "죽겠", "미치겠", "공황", "증오")
    }.sortedByDescending { it.first.length }
    private val negationsKo = setOf("안", "못", "전혀")
    private val boostersKo = setOf("너무", "정말", "진짜", "아주", "엄청", "완전", "되게", "매우", "많이")

    fun score(text: String): Result {
        if (text.any { it in '가'..'힣' }) return scoreKo(text)
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
        return squash(sum, hits)
    }

    private fun scoreKo(text: String): Result {
        val w = TextTools.words(text)
        var sum = 0f
        var hits = 0
        for (i in w.indices) {
            var s = stemsKo.firstOrNull { w[i].startsWith(it.first) }?.second ?: continue
            hits++
            if (i > 0 && w[i - 1] in boostersKo) s *= 1.3f
            // "안 좋아", "좋지 않아", "좋지않았어"
            val next = w.getOrNull(i + 1).orEmpty()
            val negated = (i > 0 && w[i - 1] in negationsKo) || next.startsWith("않") || next.startsWith("못") ||
                "지않" in w[i] || "지못" in w[i]
            if (negated) s *= -0.75f
            sum += s
        }
        return squash(sum, hits)
    }

    private fun squash(sum: Float, hits: Int) = Result((sum / sqrt(sum * sum + 15f)), hits)
}
