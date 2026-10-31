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

    // Same plan in Korean: about 109 syllables, mostly voiced, five short neutral sentences
    // in everyday spoken style, with no numbers or loanwords that the recognizer may spell differently.
    const val ANCHOR_KO = "오늘 아침에는 일찍 일어나서 동네를 한 바퀴 걸었어요. " +
        "골목마다 나무가 많고, 길가에는 작은 꽃이 피어 있었어요. " +
        "놀이터 옆 의자에 앉아 쉬다가, 천천히 걸어서 시장 쪽으로 갔어요. " +
        "가게 앞에는 과일과 채소가 가지런히 놓여 있었어요. " +
        "저는 사과 몇 개를 사서 집으로 돌아왔어요."

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
        "도서관은 아침 일찍 문을 열고, 저녁에는 조금 일찍 닫아요.",
        "작은 배 한 척이 오래된 다리 밑으로 천천히 지나갔어요.",
        "모퉁이 빵집은 매일 아침 해가 뜨기 전에 빵을 구워요.",
        "화분을 창가에 두었더니 하루 종일 햇볕이 잘 들어요.",
        "기차는 저쪽 승강장에서 출발하니까 안내판을 한번 보세요.",
        "동생 자전거에는 바구니가 달려 있고, 안장이 조금 높아요.",
        "바람이 불어서 책상 위에 있던 종이가 바닥으로 떨어졌어요.",
        "정원 길은 돌로 되어 있고, 양쪽에 낮은 등이 놓여 있어요.",
        "빨래를 개고 나서 수건은 맨 위 칸에 올려 두었어요.",
        "시장은 주말에 늦게까지 열어서 저녁에도 사람이 많아요.",
        "우리 집 식탁은 동그랗고, 의자가 둘레에 나란히 놓여 있어요.",
        "길모퉁이에서 차가 멈추자 아이와 엄마가 천천히 내렸어요.",
        "밤사이 눈이 내려서 아침에는 언덕이 하얗게 덮였어요.",
        "거실 벽시계는 다른 시계보다 늘 몇 분씩 늦게 가요.",
        "언니가 봉투에 주소를 적어서 현관 옆에 놓아두었어요.",
        "여름이 되면 섬으로 가는 배가 아침부터 저녁까지 자주 다녀요.",
        "마을로 들어가는 길을 따라 키 큰 나무들이 줄지어 서 있어요.",
        "회의실에는 긴 탁자가 있고, 맞은편 벽에 큰 화면이 걸려 있어요.",
        "주전자에 물을 올려놓은 동안 빗방울이 지붕을 두드렸어요.",
        "문구점에서는 종이와 연필, 여러 가지 색깔의 공책을 팔아요.",
        "우리는 모래가 부드러운 바닷가를 따라 한참 동안 걸었어요.",
        "사무실은 계단을 올라가서 복도 오른쪽 끝에 있어요.",
        "회색 고양이가 아침 내내 우체국 앞 의자에서 잠을 잤어요.",
        "농부가 사과 상자를 수레에 하나씩 차곡차곡 쌓았어요.",
        "마을 종은 낮에 한 번 울리고, 저녁에 한 번 더 울려요.",
        "할머니가 찻잔을 쟁반에 올려서 마당으로 가지고 나오셨어요.",
        "강은 마을 근처에서 넓어지다가 언덕 쪽으로 굽어 흘러요.",
        "담장에 칠을 새로 해서 마를 때까지 대문을 열어 두었어요.",
        "공원에는 작은 연못과 의자 몇 개, 잔디밭을 도는 길이 있어요.",
        "집 앞 화단에 물을 주고 나서 우편함을 열어 보았어요.",
    )

    fun forDay(lang: Lang, date: LocalDate = LocalDate.now()): String =
        (if (lang == Lang.KO) dailyKo else dailyEn).let { it[(date.toEpochDay() % it.size).toInt()] }
}
