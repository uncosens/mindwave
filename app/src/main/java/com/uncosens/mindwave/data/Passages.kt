package com.uncosens.mindwave.data

// Longer neutral passages for people who would rather read than talk freely.
// About 55-65 words each, roughly 20 seconds read aloud.
object Passages {
    private val en = listOf(
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

    // Korean, about 110-130 syllables each, also roughly 20 seconds.
    private val ko = listOf(
        "오래된 등대는 좁은 길 끝에 서 있습니다. 저녁마다 관리인은 나선 계단을 올라 등불을 살피고, 두꺼운 공책에 날씨를 적습니다. 요즘 배들은 예전만큼 등대를 찾지 않지만, 마을은 여전히 불을 켜 둡니다. 바람이 잔잔한 날이면 사람들이 걸어 나와 파도와 갈매기를 구경합니다.",
        "동네 도서관은 지난봄에 더 큰 건물로 옮겼습니다. 새 방에는 높은 창문과 긴 나무 탁자, 지도를 보는 조용한 구석이 있습니다. 평일 아침에는 직원들이 돌아온 책을 수레에 나누어 담습니다. 아이들은 학교가 끝나면 만화책을 빌리러 오고, 어른들은 창가에 앉아 신문을 읽습니다.",
        "큰길 모퉁이의 작은 빵집은 해가 뜨기 전에 문을 엽니다. 제빵사는 큰 그릇에 밀가루와 물과 소금을 섞고, 반죽을 둥글게 빚습니다. 빵이 부푸는 동안 바닥을 쓸고 종이봉투를 꺼내 둡니다. 일곱 시쯤이면 손님 몇 사람이 문 앞에서 차례를 기다립니다.",
        "골짜기를 지나는 강은 여름이면 느리고 넓게 흐릅니다. 농부들은 그 물을 밭에 대고, 좁은 오솔길이 강둑을 따라 몇 킬로미터나 이어집니다. 가을에는 나뭇잎이 노랗게 물들어 물 위를 떠다닙니다. 사람들은 개와 함께 길을 걷다가 얕은 물가에 선 왜가리를 바라봅니다.",
        "도시에서 출발한 기차는 세 시간쯤 걸려 바닷가에 닿습니다. 기차는 농장과 작은 역을 지나고, 언덕 아래 긴 터널을 통과합니다. 승객들은 대부분 책을 읽거나 잠을 자거나 창밖의 들판을 봅니다. 손수레가 두 번 지나가며 차와 샌드위치와 물을 팝니다. 마지막 역은 조용한 항구 마을입니다.",
        "공원 한가운데에는 돌로 둘레를 두른 둥근 연못이 있습니다. 오리들은 갈대 근처를 헤엄치고, 의자 몇 개가 물을 바라보고 놓여 있습니다. 정원사는 화요일마다 울타리를 다듬고 자갈길을 고릅니다. 주말에는 가족들이 작은 돛단배를 가져와 긴 막대로 연못 위에 살며시 밀어 줍니다.",
        "언덕 위 박물관에는 오래된 시계가 많이 모여 있습니다. 어떤 시계는 키가 커서 벽에 기대 서 있고, 어떤 시계는 주머니에 들어갈 만큼 작습니다. 안내원은 아침마다 몇 개의 태엽을 감아 방문객이 째깍거리는 소리를 듣게 합니다. 정오가 되면 여러 시계가 함께 울려 긴 복도를 채웁니다.",
        "토요일마다 마을 광장에는 장이 섭니다. 상인들은 아침 일찍 접이식 탁자와 천막, 과일과 채소 상자를 싣고 옵니다. 치즈와 빵, 꽃과 헌책을 파는 자리도 있습니다. 한낮이 되면 광장이 붐비고, 늦은 오후에는 상인들이 짐을 싸고 청소부가 돌바닥을 씁니다.",
        "산장은 나무가 자라지 않는 높은 곳의 작은 호수 옆에 있습니다. 등산객들은 바위투성이 길을 한참 올라 그곳에 닿습니다. 안에는 이층 침대와 장작 난로, 낡은 안내서가 꽂힌 선반이 있습니다. 저녁이면 사람들은 간단히 밥을 지어 먹고, 지도를 맞대어 보며 다음 날 길을 정합니다.",
        "두 섬 사이의 만을 건너는 배는 하루에 네 번 다닙니다. 자동차는 아래 갑판으로 들어가고, 승객들은 계단을 올라 창가에 앉습니다. 바다가 잔잔하면 건너는 데 사십 분쯤 걸립니다. 맑은 날에는 배가 항구에 닿기 한참 전부터 건너편 섬의 언덕이 보입니다.",
    )

    fun random(lang: Lang, except: String? = null): String =
        (if (lang == Lang.KO) ko else en).filter { it != except }.random()
}
