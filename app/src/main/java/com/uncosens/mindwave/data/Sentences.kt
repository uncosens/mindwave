package com.uncosens.mindwave.data

import java.time.LocalDate

object Sentences {
    // Read three times at calibration, then once at the start of every check-in.
    // About 17-19 s, long enough for stable pitch and voice measures (docs/research/voice-emotion-accuracy.md).
    const val ANCHOR = "We were away all day, and we rode along the valley road. " +
        "The morning air was mild, and the low hills were green. " +
        "Near the old mill, a narrow lane winds over a small bridge. " +
        "Beyond it, a farmer keeps bees, a few goats, and a garden of beans and peas. " +
        "We walked back by the water and reached home before evening."

    private val prompts = listOf(
        "How was your day?",
        "What's on your mind?",
        "What did you do this morning?",
        "Tell me about something you saw today.",
        "What are you looking forward to?",
    )

    fun promptForDay(date: LocalDate = LocalDate.now()): String = prompts[(date.toEpochDay() % prompts.size).toInt()]

    // Neutral, plain statements of similar length so days stay comparable.
    private val daily = listOf(
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

    fun forDay(date: LocalDate = LocalDate.now()): String = daily[(date.toEpochDay() % daily.size).toInt()]
}
