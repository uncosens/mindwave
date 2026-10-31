package com.uncosens.mindwave.data

import java.time.LocalDate

object Sentences {
    // Read three times at calibration, then once at the start of every check-in.
    // About 17-19 s, long enough for stable pitch and voice measures (docs/research/voice-emotion-accuracy.md).
    const val ANCHOR_EN = "We were away all day, and we rode along the valley road. " +
        "The morning air was mild, and the low hills were green. " +
        "Near the old mill, a narrow lane winds over a small bridge. " +
        "Beyond it, a farmer keeps bees, a few goats, and a garden of beans and peas. " +
        "We walked back by the water and reached home before evening."

    // Same plan in Korean: about 106 syllables, mostly voiced, five short neutral sentences.
    const val ANCHOR_KO = "우리는 아침 일찍 마을 앞 냇가를 따라 걸었습니다. " +
        "물은 느리게 흘렀고, 낮은 언덕이 길게 이어졌습니다. " +
        "오래된 방앗간 옆에는 좁은 길과 작은 다리가 있습니다. " +
        "다리 건너편에서는 농부가 콩과 토마토를 기릅니다. " +
        "우리는 물가를 따라 돌아와 저녁 전에 집에 닿았습니다."

    fun anchor(lang: Lang) = if (lang == Lang.KO) ANCHOR_KO else ANCHOR_EN

    private val promptsEn = listOf(
        "How was your day?",
        "What's on your mind?",
        "What did you do this morning?",
        "Tell me about something you saw today.",
        "What are you looking forward to?",
    )

    private val promptsKo = listOf(
        "오늘 하루는 어땠나요?",
        "요즘 무슨 생각을 하고 있나요?",
        "오늘 아침에는 무엇을 했나요?",
        "오늘 본 것 하나를 이야기해 주세요.",
        "기대하고 있는 일이 있나요?",
    )

    fun promptForDay(lang: Lang, date: LocalDate = LocalDate.now()): String =
        (if (lang == Lang.KO) promptsKo else promptsEn).let { it[(date.toEpochDay() % it.size).toInt()] }

    // Neutral, plain statements of similar length so days stay comparable.
    private val dailyEn = listOf(
        "The library opens at nine, and the reading room is on the second floor.",
        "A small boat drifted along the river, past the old wooden bridge.",
        "The bakery on the corner sells fresh bread every morning before eight.",
        "We placed the plants near the window, where they get light all day.",
        "The train leaves from the north platform, so please check the board.",
        "Her bicycle has a blue basket, and the seat is a little too high.",
        "The museum keeps its maps in a cool room below the main hall.",
        "A gentle wind moved the curtains, and the paper slid off the desk.",
        "The garden path is made of stone, with low lamps along each side.",
        "He folded the shirts, then put the towels on the top shelf.",
        "The market stays open late on Friday, when the streets are busy.",
        "Our kitchen table is round, and it fits four chairs around it.",
        "The bus stopped at the corner, and two people stepped down slowly.",
        "Snow covered the hills by morning, and the roads were very quiet.",
        "The clock in the hallway runs a few minutes behind the others.",
        "She wrote the address on a card and left it by the front door.",
        "The ferry crosses the bay twice an hour during the summer months.",
        "A row of tall trees lines the road that leads into the village.",
        "The meeting room has a long table and a screen on the far wall.",
        "Rain tapped on the roof while the kettle warmed on the stove.",
        "The shop sells paper, pens, and small notebooks in many colors.",
        "We walked along the beach, where the sand was cool and smooth.",
        "The office is on the third floor, next to the stairs and the lift.",
        "A gray cat slept on the bench outside the post office all morning.",
        "The farmer stacked the boxes of apples near the back of the truck.",
        "The tower bell rings once at noon and again in the evening.",
        "He set the cups on the tray and carried them into the garden.",
        "The river is wide near the town, and it bends toward the hills.",
        "Fresh paint dried on the fence, and the gate was left open.",
        "The park has a pond, a few benches, and a path around the lawn.",
    )

    private val dailyKo = listOf(
        "도서관은 아홉 시에 문을 열고, 열람실은 이층에 있습니다.",
        "작은 배 한 척이 오래된 나무다리 옆으로 천천히 지나갔습니다.",
        "모퉁이 빵집은 매일 아침 여덟 시 전에 빵을 굽습니다.",
        "화분은 창가에 두어서 하루 종일 햇빛을 받습니다.",
        "기차는 북쪽 승강장에서 출발하니 안내판을 확인해 주세요.",
        "그녀의 자전거에는 파란 바구니가 있고, 안장이 조금 높습니다.",
        "박물관은 지도를 중앙 홀 아래 서늘한 방에 보관합니다.",
        "바람이 커튼을 흔들자 종이가 책상에서 미끄러졌습니다.",
        "정원 길은 돌로 되어 있고, 양옆에 낮은 등이 있습니다.",
        "그는 셔츠를 개고 나서 수건을 맨 위 선반에 올렸습니다.",
        "시장은 금요일에 늦게까지 열고, 그날은 거리가 붐빕니다.",
        "우리 집 식탁은 둥글고, 의자 네 개가 둘레에 놓입니다.",
        "버스가 모퉁이에 멈추자 두 사람이 천천히 내렸습니다.",
        "아침이 되자 언덕은 눈으로 덮였고, 길은 아주 조용했습니다.",
        "복도에 있는 시계는 다른 시계보다 몇 분 늦게 갑니다.",
        "그녀는 카드에 주소를 적어 현관문 옆에 두었습니다.",
        "여름에는 배가 한 시간에 두 번 만을 건넙니다.",
        "마을로 들어가는 길을 따라 키 큰 나무들이 서 있습니다.",
        "회의실에는 긴 탁자가 있고, 맞은편 벽에 화면이 걸려 있습니다.",
        "주전자가 난로 위에서 데워지는 동안 빗방울이 지붕을 두드렸습니다.",
        "그 가게는 종이와 펜, 여러 색의 작은 공책을 팝니다.",
        "우리는 모래가 부드럽고 서늘한 바닷가를 따라 걸었습니다.",
        "사무실은 삼층에 있고, 계단과 승강기 바로 옆입니다.",
        "회색 고양이가 아침 내내 우체국 앞 의자에서 잤습니다.",
        "농부는 사과 상자를 트럭 뒤쪽에 차곡차곡 쌓았습니다.",
        "탑의 종은 정오에 한 번, 저녁에 한 번 더 울립니다.",
        "그는 컵을 쟁반에 올려 정원으로 가지고 나갔습니다.",
        "강은 마을 근처에서 넓어지고, 언덕 쪽으로 휘어집니다.",
        "울타리에 칠한 페인트가 마르는 동안 문은 열려 있었습니다.",
        "공원에는 연못과 의자 몇 개, 잔디를 도는 길이 있습니다.",
    )

    fun forDay(lang: Lang, date: LocalDate = LocalDate.now()): String =
        (if (lang == Lang.KO) dailyKo else dailyEn).let { it[(date.toEpochDay() % it.size).toInt()] }
}
