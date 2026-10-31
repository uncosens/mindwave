package com.uncosens.mindwave

import com.uncosens.mindwave.analysis.Metric
import com.uncosens.mindwave.data.Lang
import java.util.Locale

// All user-facing text, one object per app language.
interface Strings {
    val locale: Locale
    val dayPattern: String   // Home date, e.g. "Monday, June 2"
    val shortPattern: String // e.g. "Jun 2"

    // Errors and problems
    val preparing: String
    val modelMissing: String
    val shortTitle: String
    val readWholePassage: String
    val talkLongerTitle: String
    val talkLongerBody: String
    val mismatchTitle: String
    val mismatchBody: String
    val quietTitle: String
    val quietBody: String
    val noisyTitle: String
    val noisyBody: String
    val readWholeSentence: String
    val loudTitle: String
    val loudBody: String

    // Onboarding
    val welcomeTitle: String
    val getStarted: String
    val micPrompt: String
    val allowMic: String
    val calibTitle: String
    val calibBody: String
    val start: String
    val setupTime: String
    val readyTitle: String
    val goToday: String
    val micNeeded: String
    val openSettings: String

    // Home
    val morning: String
    val afternoon: String
    val evening: String
    val history: String
    val settings: String
    val setupChanged: String
    val recalibrate: String
    fun streak(n: Int): String
    val todaysCheckIn: String
    fun todayIs(label: String): String
    fun lastCheckIn(day: String): String
    val energy: String
    val mood: String
    val stability: String
    val startCheckIn: String
    val seeTodaysResult: String
    val redoToday: String
    val yesterday: String

    // Result
    val today: String
    val learning: String
    val yourState: String
    val calm: String
    val energized: String
    val low: String
    val positive: String
    val steady: String
    fun youSaid(mood: Int, label: String, energy: Int?): String
    val comparedToUsual: String
    val notMedical: String
    val done: String
    val usual: String
    fun lastN(n: Int): String

    // Settings
    val recalibrateVoice: String
    val deleteAll: String
    val about: String
    fun version(v: String): String
    val deleteTitle: String
    val deleteBody: String
    val delete: String
    val cancel: String

    // Voice steps
    val tapAndRead: String
    val tapAndTalk: String
    val startReading: String
    val startTalking: String
    val oneMoment: String
    val tooQuietLive: String
    val listening: String
    val tapDoneAnytime: String
    val keepGoing: String
    val tryAgain: String
    val stopTitle: String
    val stopBody: String
    val keepGoingButton: String
    val stop: String
    val stepSetup: String
    val stepWarmUp: String
    val stepDaily: String
    val stepTalk: String
    val talkSub: String
    val stepReadAloud: String
    fun roundOf(n: Int): String
    val readPassageInstead: String
    val anotherPassage: String
    val talkInstead: String

    // Rating, review, analyzing
    val moodQuestion: String
    val moodLabels: List<String>
    val energyQuestion: String
    val energyLabels: List<String>
    val seeResult: String
    val reviewTitle: String
    val looksRight: String
    val listeningBack: String

    // History
    val noCheckIns: String
    val checkInNow: String
    val ranges: List<String>
    val moodAndEnergy: String
    val selfRating: String
    fun saidN(n: Int): String

    // Metrics: label, chip, more, less
    val tone: List<String>
    val rate: List<String>
    val pitch: List<String>
    val pitchVariety: List<String>
    val pauses: List<String>
    val tremor: List<String>
    val asUsual: String
    fun aBit(dir: String): String
    fun much(dir: String): String
    fun metricName(m: Metric): String
    val syllablesPerSec: String

    // Summary label
    val energyLevels: List<String> // calm, balanced, energized
    val moodLevels: List<String>   // low .. bright
    fun label(energy: String, mood: String, unsteady: Boolean): String

    object En : Strings {
        override val locale: Locale = Locale.ENGLISH
        override val dayPattern = "EEEE, MMMM d"
        override val shortPattern = "MMM d"
        override val preparing = "Preparing…"
        override val modelMissing = "Speech model not found. Run scripts/fetch-model.ps1 and rebuild."
        override val shortTitle = "That was a bit short."
        override val readWholePassage = "Please read the whole passage."
        override val talkLongerTitle = "Talk a little longer."
        override val talkLongerBody = "Anything works — what you ate, what you saw."
        override val mismatchTitle = "That didn't match the text."
        override val mismatchBody = "Please read the whole sentence as written."
        override val quietTitle = "That was too quiet to analyze."
        override val quietBody = "Hold the phone about 20 cm from your mouth."
        override val noisyTitle = "It's noisy around you."
        override val noisyBody = "Find a quieter spot and try again."
        override val readWholeSentence = "Please read the whole sentence."
        override val loudTitle = "That was too loud."
        override val loudBody = "Hold the phone a little farther away and read again."
        override val welcomeTitle = "Hear how you feel."
        override val getStarted = "Get started"
        override val micPrompt = "mindwave listens only while the orb is lit."
        override val allowMic = "Allow microphone"
        override val calibTitle = "Let's learn your voice."
        override val calibBody = "Read the same sentence three times. Find a quiet spot."
        override val start = "Start"
        override val setupTime = "Setup · 1 minute"
        override val readyTitle = "You're all set."
        override val goToday = "Go to today"
        override val micNeeded = "mindwave needs the microphone"
        override val openSettings = "Open settings"
        override val morning = "Good morning"
        override val afternoon = "Good afternoon"
        override val evening = "Good evening"
        override val history = "History"
        override val settings = "Settings"
        override val setupChanged = "Your phone or microphone changed."
        override val recalibrate = "Recalibrate"
        override fun streak(n: Int) = "$n-day streak"
        override val todaysCheckIn = "Today's check-in"
        override fun todayIs(label: String) = "Today: $label"
        override fun lastCheckIn(day: String) = "Last check-in · $day"
        override val energy = "Energy"
        override val mood = "Mood"
        override val stability = "Stability"
        override val startCheckIn = "Start check-in"
        override val seeTodaysResult = "See today's result"
        override val redoToday = "Redo today's check-in"
        override val yesterday = "Yesterday"
        override val today = "Today"
        override val learning = "Still learning your usual voice"
        override val yourState = "Your state"
        override val calm = "calm"
        override val energized = "energized"
        override val low = "low"
        override val positive = "positive"
        override val steady = "steady"
        override fun youSaid(mood: Int, label: String, energy: Int?) =
            "You said $mood · $label" + energy?.let { " · Energy $it" }.orEmpty()
        override val comparedToUsual = "Compared to your usual"
        override val notMedical = "mindwave is not a medical tool."
        override val done = "Done"
        override val usual = "Usual"
        override fun lastN(n: Int) = "Last $n check-ins"
        override val recalibrateVoice = "Recalibrate voice"
        override val deleteAll = "Delete all data"
        override val about = "About"
        override fun version(v: String) = "Version $v"
        override val deleteTitle = "Delete everything?"
        override val deleteBody = "This can't be undone."
        override val delete = "Delete"
        override val cancel = "Cancel"
        override val tapAndRead = "Tap the orb and read aloud"
        override val tapAndTalk = "Tap the orb and start talking"
        override val startReading = "Start reading"
        override val startTalking = "Start talking"
        override val oneMoment = "One moment…"
        override val tooQuietLive = "I can barely hear you — move a little closer"
        override val listening = "Listening…"
        override val tapDoneAnytime = "Tap Done anytime"
        override val keepGoing = "Keep going…"
        override val tryAgain = "Try again"
        override val stopTitle = "Stop check-in?"
        override val stopBody = "Nothing from today will be saved."
        override val keepGoingButton = "Keep going"
        override val stop = "Stop"
        override val stepSetup = "Setup"
        override val stepWarmUp = "Warm up"
        override val stepDaily = "Sentence of the day"
        override val stepTalk = "Talk freely"
        override val talkSub = "Anything is fine. 10–20 s."
        override val stepReadAloud = "Read aloud"
        override fun roundOf(n: Int) = "$n of 3"
        override val readPassageInstead = "Rather not talk? Read a passage"
        override val anotherPassage = "Another passage"
        override val talkInstead = "Talk instead"
        override val moodQuestion = "How do you feel right now?"
        override val moodLabels = listOf("Very low", "Low", "Okay", "Good", "Great")
        override val energyQuestion = "How much energy do you have?"
        override val energyLabels = listOf("Drained", "Low", "Okay", "High", "Full of energy")
        override val seeResult = "See result"
        override val reviewTitle = "Is this what you said?"
        override val looksRight = "Looks right"
        override val listeningBack = "Listening back…"
        override val noCheckIns = "No check-ins yet."
        override val checkInNow = "Check in now"
        override val ranges = listOf("7 days", "30 days", "All")
        override val moodAndEnergy = "Mood & energy"
        override val selfRating = "Self rating"
        override fun saidN(n: Int) = "You said $n"
        override val tone = listOf("Tone", "Tone", "brighter", "darker")
        override val rate = listOf("Speech rate", "Speech rate", "faster", "slower")
        override val pitch = listOf("Pitch", "Pitch", "higher", "lower")
        override val pitchVariety = listOf("Pitch variety", "Pitch variety", "more varied", "flatter")
        override val pauses = listOf("Pauses & breath", "Pauses", "more pauses", "fewer pauses")
        override val tremor = listOf("Voice tremor", "Tremor", "shakier", "steadier")
        override val asUsual = "As usual"
        override fun aBit(dir: String) = "A bit $dir"
        override fun much(dir: String) = "Much $dir"
        override fun metricName(m: Metric) = when (m) {
            Metric.PITCH -> "Pitch"
            Metric.PITCH_VAR -> "Pitch variation"
            Metric.ENERGY -> "Tone energy"
            Metric.RATE -> "Speech rate"
            Metric.PAUSES -> "Breathing pauses"
            Metric.JITTER -> "Pitch tremor"
            Metric.SHIMMER -> "Volume tremor"
        }
        override val syllablesPerSec = "syl/s"
        override val energyLevels = listOf("Calm", "Balanced", "Energized")
        override val moodLevels = listOf("low", "a little low", "steady", "a little bright", "bright")
        override fun label(energy: String, mood: String, unsteady: Boolean) =
            "$energy, $mood" + if (unsteady) ", less steady than usual" else ""
    }

    object Ko : Strings {
        override val locale: Locale = Locale.KOREAN
        override val dayPattern = "M월 d일 EEEE"
        override val shortPattern = "M월 d일"
        override val preparing = "준비 중…"
        override val modelMissing = "음성 인식 모델이 없습니다. scripts/fetch-model.ps1을 실행한 뒤 다시 빌드하세요."
        override val shortTitle = "조금 짧았어요."
        override val readWholePassage = "글 전체를 읽어 주세요."
        override val talkLongerTitle = "조금 더 길게 말해 주세요."
        override val talkLongerBody = "무엇이든 괜찮아요. 먹은 것, 본 것도 좋아요."
        override val mismatchTitle = "문장과 다르게 들렸어요."
        override val mismatchBody = "적힌 문장 그대로 끝까지 읽어 주세요."
        override val quietTitle = "소리가 너무 작아요."
        override val quietBody = "휴대폰을 입에서 20cm 정도 거리에 두세요."
        override val noisyTitle = "주변이 시끄러워요."
        override val noisyBody = "조용한 곳에서 다시 해 주세요."
        override val readWholeSentence = "문장 전체를 읽어 주세요."
        override val loudTitle = "소리가 너무 커요."
        override val loudBody = "휴대폰을 조금 멀리 두고 다시 읽어 주세요."
        override val welcomeTitle = "목소리로 마음을 들어 보세요."
        override val getStarted = "시작하기"
        override val micPrompt = "mindwave는 구슬이 빛날 때만 듣습니다."
        override val allowMic = "마이크 허용"
        override val calibTitle = "목소리를 익혀 볼게요."
        override val calibBody = "같은 문장을 세 번 읽어 주세요. 조용한 곳이 좋아요."
        override val start = "시작"
        override val setupTime = "설정 · 1분"
        override val readyTitle = "준비가 끝났어요."
        override val goToday = "오늘로 가기"
        override val micNeeded = "mindwave에는 마이크가 필요해요"
        override val openSettings = "설정 열기"
        override val morning = "좋은 아침이에요"
        override val afternoon = "좋은 오후예요"
        override val evening = "좋은 저녁이에요"
        override val history = "기록"
        override val settings = "설정"
        override val setupChanged = "휴대폰이나 마이크가 바뀌었어요."
        override val recalibrate = "다시 설정"
        override fun streak(n: Int) = "${n}일 연속"
        override val todaysCheckIn = "오늘의 체크인"
        override fun todayIs(label: String) = "오늘: $label"
        override fun lastCheckIn(day: String) = "지난 체크인 · $day"
        override val energy = "에너지"
        override val mood = "기분"
        override val stability = "안정감"
        override val startCheckIn = "체크인 시작"
        override val seeTodaysResult = "오늘 결과 보기"
        override val redoToday = "오늘 체크인 다시 하기"
        override val yesterday = "어제"
        override val today = "오늘"
        override val learning = "아직 평소 목소리를 익히는 중이에요"
        override val yourState = "지금 상태"
        override val calm = "차분"
        override val energized = "활기"
        override val low = "낮음"
        override val positive = "밝음"
        override val steady = "안정"
        override fun youSaid(mood: Int, label: String, energy: Int?) =
            "내가 고른 기분 $mood · $label" + energy?.let { " · 에너지 $it" }.orEmpty()
        override val comparedToUsual = "평소와 비교하면"
        override val notMedical = "mindwave는 의료 도구가 아닙니다."
        override val done = "완료"
        override val usual = "평소"
        override fun lastN(n: Int) = "최근 체크인 ${n}회"
        override val recalibrateVoice = "목소리 다시 설정"
        override val deleteAll = "모든 데이터 삭제"
        override val about = "정보"
        override fun version(v: String) = "버전 $v"
        override val deleteTitle = "모두 삭제할까요?"
        override val deleteBody = "되돌릴 수 없어요."
        override val delete = "삭제"
        override val cancel = "취소"
        override val tapAndRead = "구슬을 누르고 소리 내어 읽어 주세요"
        override val tapAndTalk = "구슬을 누르고 말해 주세요"
        override val startReading = "읽기 시작"
        override val startTalking = "말하기 시작"
        override val oneMoment = "잠시만요…"
        override val tooQuietLive = "잘 안 들려요. 조금 더 가까이 와 주세요"
        override val listening = "듣고 있어요…"
        override val tapDoneAnytime = "언제든 완료를 누르세요"
        override val keepGoing = "계속 말해 주세요…"
        override val tryAgain = "다시 하기"
        override val stopTitle = "체크인을 그만할까요?"
        override val stopBody = "오늘 내용은 저장되지 않아요."
        override val keepGoingButton = "계속하기"
        override val stop = "그만하기"
        override val stepSetup = "설정"
        override val stepWarmUp = "준비"
        override val stepDaily = "오늘의 문장"
        override val stepTalk = "자유롭게 말하기"
        override val talkSub = "무엇이든 괜찮아요. 10–20초."
        override val stepReadAloud = "소리 내어 읽기"
        override fun roundOf(n: Int) = "$n / 3"
        override val readPassageInstead = "말하기 대신 글 읽기"
        override val anotherPassage = "다른 글"
        override val talkInstead = "말하기로 하기"
        override val moodQuestion = "지금 기분은 어떤가요?"
        override val moodLabels = listOf("아주 나쁨", "나쁨", "보통", "좋음", "아주 좋음")
        override val energyQuestion = "에너지는 얼마나 있나요?"
        override val energyLabels = listOf("지침", "낮음", "보통", "높음", "넘침")
        override val seeResult = "결과 보기"
        override val reviewTitle = "이렇게 말했나요?"
        override val looksRight = "맞아요"
        override val listeningBack = "다시 듣는 중…"
        override val noCheckIns = "아직 체크인이 없어요."
        override val checkInNow = "지금 체크인"
        override val ranges = listOf("7일", "30일", "전체")
        override val moodAndEnergy = "기분과 에너지"
        override val selfRating = "내 평가"
        override fun saidN(n: Int) = "내가 고른 기분 $n"
        override val tone = listOf("음색", "음색", "더 밝음", "더 어두움")
        override val rate = listOf("말 속도", "말 속도", "더 빠름", "더 느림")
        override val pitch = listOf("음높이", "음높이", "더 높음", "더 낮음")
        override val pitchVariety = listOf("억양 변화", "억양", "더 다채로움", "더 단조로움")
        override val pauses = listOf("쉼과 호흡", "쉼", "쉼이 더 많음", "쉼이 더 적음")
        override val tremor = listOf("목소리 떨림", "떨림", "더 떨림", "더 안정됨")
        override val asUsual = "평소와 같음"
        override fun aBit(dir: String) = "조금 $dir"
        override fun much(dir: String) = "훨씬 $dir"
        override fun metricName(m: Metric) = when (m) {
            Metric.PITCH -> "음높이"
            Metric.PITCH_VAR -> "억양 변화"
            Metric.ENERGY -> "음량"
            Metric.RATE -> "말 속도"
            Metric.PAUSES -> "쉼"
            Metric.JITTER -> "음높이 떨림"
            Metric.SHIMMER -> "음량 떨림"
        }
        override val syllablesPerSec = "음절/초"
        override val energyLevels = listOf("차분함", "보통", "활기참")
        override val moodLevels = listOf("가라앉음", "조금 가라앉음", "무난함", "조금 밝음", "밝음")
        override fun label(energy: String, mood: String, unsteady: Boolean) =
            "$energy, $mood" + if (unsteady) ", 평소보다 덜 안정적" else ""
    }

    companion object {
        fun of(lang: Lang): Strings = when (lang) {
            Lang.EN -> En
            Lang.KO -> Ko
        }
    }
}
