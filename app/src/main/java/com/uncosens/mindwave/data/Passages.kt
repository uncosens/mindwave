package com.uncosens.mindwave.data

// Longer neutral passages for people who would rather read than talk freely.
// About 55-65 words each, roughly 20 seconds read aloud.
object Passages {
    private val all = listOf(
        "The old lighthouse stands at the end of a narrow road. Every evening a keeper climbs the spiral stairs, checks the lamp, and writes the weather in a thick notebook. Ships no longer need the light as much as they once did, but the town keeps it running. Visitors walk out on calm days to watch the waves and the gulls.",
        "Our neighborhood library moved into a larger building last spring. The new rooms have tall windows, long wooden tables, and a quiet corner for reading maps. On weekday mornings the staff sort returned books onto metal carts. Children come in after school to borrow comics, and older readers sit by the windows with the daily papers.",
        "A small bakery opens before sunrise on the corner of the main street. The baker mixes flour, water, and salt in a large bowl, then shapes the dough into round loaves. While the bread rises, she sweeps the floor and sets out paper bags. By seven o'clock a short line of customers waits patiently by the door.",
        "The river that runs through the valley is slow and wide in the summer. Farmers use its water for their fields, and a narrow path follows the bank for several miles. In the autumn the leaves turn yellow and drift on the surface. People walk their dogs along the path and stop to watch the herons standing in the shallows.",
        "The train from the city reaches the coast in about three hours. It passes farms, small stations, and a long tunnel under the hills. Most passengers read, sleep, or look out at the changing fields. A trolley comes through the cars twice, selling tea, sandwiches, and bottles of water. The last stop is a quiet harbor town.",
        "In the middle of the park there is a round pond with a stone edge. Ducks swim near the reeds, and a few benches face the water. Gardeners trim the hedges every Tuesday and rake the gravel paths. On weekends families bring small sailing boats and push them gently across the pond with long wooden sticks.",
        "The museum on the hill keeps a large collection of old clocks. Some are tall and stand against the walls, while others are small enough to fit in a pocket. A guide winds a few of them each morning so that visitors can hear them tick. At noon several clocks chime together, and the sound fills the long hall.",
        "The weekly market sets up in the town square every Saturday. Traders arrive early with folding tables, canvas roofs, and crates of fruit and vegetables. There are stalls for cheese, bread, flowers, and secondhand books. By midday the square is busy, and by late afternoon the traders pack their vans and the cleaners sweep the stones.",
        "The mountain hut sits beside a small lake above the tree line. Hikers reach it after a long climb on a rocky trail. Inside there are bunk beds, a wood stove, and a shelf of old guidebooks. In the evening people cook simple meals, compare their maps, and plan the next day's route before the lights go out.",
        "A ferry crosses the bay between the two islands four times a day. Cars drive onto the lower deck, and passengers climb the stairs to sit by the windows. The crossing takes about forty minutes when the sea is calm. On clear days you can see the hills of the far island long before the ferry reaches the harbor.",
    )

    fun random(except: String? = null): String = all.filter { it != except }.random()
}
