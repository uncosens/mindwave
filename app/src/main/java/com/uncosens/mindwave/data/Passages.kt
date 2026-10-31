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
        "바닷가 마을 끝에는 오래된 등대가 서 있어요. 저녁이 되면 관리하는 분이 좁은 계단을 올라가 불을 켜고, 공책에 그날의 날씨를 적어요. 요즘은 배들이 예전만큼 등대에 기대지 않지만, 마을 사람들은 여전히 불을 끄지 않아요. 바람이 잔잔한 날에는 산책 나온 사람들이 파도와 갈매기를 구경해요.",
        "동네 도서관이 지난봄에 더 넓은 건물로 옮겼어요. 새 건물에는 큰 창문과 긴 나무 책상이 있고, 구석에는 조용히 앉아 있을 자리도 있어요. 평일 아침에는 직원들이 반납된 책을 정리해서 제자리에 꽂아요. 오후가 되면 아이들이 동화책을 빌리러 오고, 어르신들은 창가에 앉아 신문을 읽으세요.",
        "큰길 모퉁이에 있는 작은 빵집은 해가 뜨기 전에 문을 열어요. 주인은 커다란 그릇에 밀가루와 물, 소금을 넣고 반죽을 만들어요. 반죽이 부푸는 동안 가게를 쓸고 봉투를 꺼내 둬요. 갓 구운 빵 냄새가 골목까지 퍼져 나가요. 출근 시간이 되면 빵을 사려는 사람들이 문 앞에 줄을 서서 기다려요.",
        "골짜기를 따라 흐르는 강은 여름이면 넓고 느리게 흘러요. 농부들은 이 물을 끌어다 논에 대고, 강둑을 따라 좁은 길이 길게 이어져 있어요. 가을이 되면 나뭇잎이 노랗게 물들어 물 위로 떠내려가요. 사람들은 강아지와 함께 그 길을 걷다가 물가에 서 있는 새들을 한참 바라보곤 해요.",
        "도시에서 출발한 기차는 몇 시간을 달려 바닷가에 도착해요. 가는 길에 논과 밭, 작은 역들을 지나고 산 아래 긴 굴을 통과해요. 사람들은 대부분 책을 읽거나 잠을 자거나 창밖을 내다봐요. 중간에 간식 수레가 지나가면서 음료와 김밥을 팔아요. 마지막 역은 조용한 항구 마을이에요.",
        "공원 한가운데에는 돌로 둘레를 쌓은 동그란 연못이 있어요. 오리들이 갈대 근처를 헤엄치고, 연못을 바라보는 의자가 몇 개 놓여 있어요. 관리하시는 분들은 일주일에 한 번 나무를 다듬고 길을 정리해요. 주말이면 가족들이 나와서 연못 둘레를 천천히 걷거나 의자에 앉아 쉬어요.",
        "언덕 위 박물관에는 오래된 시계가 많이 모여 있어요. 벽에 기대 선 커다란 시계도 있고, 주머니에 쏙 들어가는 작은 시계도 있어요. 안내하시는 분이 아침마다 몇 개의 태엽을 감아서, 사람들이 째깍거리는 소리를 들을 수 있게 해요. 정오가 되면 여러 시계가 한꺼번에 울려서 긴 복도가 소리로 가득 차요.",
        "주말마다 마을 광장에는 장이 열려요. 상인들은 아침 일찍 접이식 탁자와 천막, 과일과 채소 상자를 싣고 와요. 떡과 반찬, 꽃과 헌책을 파는 자리도 있어요. 점심 무렵에는 광장이 사람들로 붐비고, 해가 질 무렵이면 상인들이 짐을 싸고 청소하시는 분들이 바닥을 쓸어요.",
        "산 중턱의 작은 호숫가 옆에는 대피소가 하나 있어요. 등산객들은 돌이 많은 길을 한참 올라가야 그곳에 닿아요. 안에는 나무 침대와 난로, 낡은 지도가 꽂힌 선반이 있어요. 저녁이 되면 사람들은 간단히 밥을 지어 먹고, 지도를 펼쳐 놓고 다음 날 갈 길을 함께 정해요.",
        "두 섬 사이를 오가는 배는 하루에 몇 번씩 다녀요. 차는 아래층에 싣고, 사람들은 계단을 올라가 창가 자리에 앉아요. 바다가 잔잔하면 건너편까지 금방 가요. 갑판에 나가 바닷바람을 쐬는 사람들도 있어요. 날이 맑은 날에는 배가 항구에 닿기 한참 전부터 건너편 섬의 산이 또렷하게 보여요.",
    )

    fun random(lang: Lang, except: String? = null): String =
        (if (lang == Lang.KO) ko else en).filter { it != except }.random()
}
