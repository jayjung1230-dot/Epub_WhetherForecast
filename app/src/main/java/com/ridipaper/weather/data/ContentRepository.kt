package com.ridipaper.weather.data

import com.ridipaper.weather.model.NewsItem
import com.ridipaper.weather.model.QuoteItem
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.random.Random

object ContentRepository {

    private val CURATED_QUOTES = listOf(
        // --- 애니메이션 명대사 (Animation) ---
        QuoteItem(
            textKo = "한 번 만난 인연은 잊혀지지 않는 거야. 다만 기억해내지 못할 뿐이지.",
            textEn = "Once you've met someone, you never really forget them. It just takes a while for your memories to return.",
            author = "제니바 · 영화 《센과 치히로의 행방불명》 (2001)",
            authorEn = "Zeniba · Spirited Away (2001)",
            tag = "기억과 인연",
            categoryKo = "애니메이션 명대사",
            categoryEn = "Animation Quote",
            backgroundKo = "미야자키 하야오 감독의 스튜디오 지브리 장편 애니메이션 《센과 치히로의 행방불명》에서, 치히로가 하쿠의 본명을 떠올리기 전 제니바가 건네는 따뜻한 조언입니다.",
            backgroundEn = "In Hayao Miyazaki's Studio Ghibli classic Spirited Away, Zeniba shares these reassuring words with Chihiro before she recalls Haku's true name.",
            interpretationKo = "살아가며 스쳐 간 소중한 인연과 경험은 완전히 사라지는 것이 아니라 마음 깊은 곳에 남아 있으며, 우리가 스스로를 다시 찾을 때 든든한 힘이 되어 준다는 의미를 담고 있습니다.",
            interpretationEn = "Meaningful bonds and experiences never truly vanish; they stay quietly within us and resurface as strength when we need them most."
        ),
        QuoteItem(
            textKo = "누구나 요리를 할 수 있다는 건 아무나 위대한 요리사가 된다는 뜻은 아니야. 하지만 위대한 요리사는 어디서든 나올 수 있지.",
            textEn = "Not everyone can become a great artist, but a great artist can come from anywhere.",
            author = "안톤 이고 · 영화 《라따뚜이》 (2007)",
            authorEn = "Anton Ego · Ratatouille (2007)",
            tag = "편견과 재능",
            categoryKo = "애니메이션 명대사",
            categoryEn = "Animation Quote",
            backgroundKo = "픽사 애니메이션 《라따뚜이》 후반부, 냉철한 음식 평론가 안톤 이고가 생쥐 레미의 요리를 맛본 뒤 자신의 편견을 내려놓고 작성한 평론의 한 구절입니다.",
            backgroundEn = "At the climax of Pixar's Ratatouille, stern food critic Anton Ego writes this reflection after tasting Remy's dish and letting go of his elitism.",
            interpretationKo = "배경이나 조건보다 중요한 것은 진심과 재능을 꽃피우려는 태도입니다. 타인과 나 자신을 출신이나 편견으로 미리 재단하지 말라는 울림을 줍니다.",
            interpretationEn = "True greatness is not limited by background or status. Never underestimate where passion, craft, and originality might emerge."
        ),
        QuoteItem(
            textKo = "삶은 완벽하지 않아도 괜찮아. 슬픔이 있어야 기쁨의 깊이도 알 수 있는 법이니까.",
            textEn = "Crying helps me slow down and obsess over the weight of life's problems.",
            author = "슬픔이 · 영화 《인사이드 아웃》 (2015)",
            authorEn = "Sadness · Inside Out (2015)",
            tag = "감정과 위로",
            categoryKo = "애니메이션 명대사",
            categoryEn = "Animation Quote",
            backgroundKo = "픽사 《인사이드 아웃》에서 늘 억눌려 있던 감정 '슬픔이'가 라일리와 빙봉의 아픔에 조용히 공감하며 곁을 지켜주는 장면에서 비롯된 메시지입니다.",
            backgroundEn = "In Pixar's Inside Out, Sadness shows that acknowledging grief and vulnerability is essential for emotional healing and genuine connection.",
            interpretationKo = "늘 밝고 긍정적이어야 한다는 압박에서 벗어나, 때로는 슬픔과 좌절을 있는 그대로 인정하는 것이 마음을 회복하고 성장하는 출발점이 됩니다.",
            interpretationEn = "Accepting sorrow rather than hiding it allows us to process loss, receive empathy, and grow emotionally stronger."
        ),
        QuoteItem(
            textKo = "세상이 너에게 뭐라고 하든, 네가 얼마나 멋진 존재인지 절대 잊지 마.",
            textEn = "Life's a little messy. We all make mistakes. No matter what type of animal you are, change starts with you.",
            author = "주디 홉스 · 영화 《주토피아》 (2016)",
            authorEn = "Judy Hopps · Zootopia (2016)",
            tag = "용기와 변화",
            categoryKo = "애니메이션 명대사",
            categoryEn = "Animation Quote",
            backgroundKo = "디즈니 《주토피아》에서 작은 토끼라는 한계와 세상의 선입견을 딛고 경찰이 된 주디 홉스가, 실수와 갈등 끝에도 더 나은 세상을 향해 다짐하는 말입니다.",
            backgroundEn = "In Disney's Zootopia, rookie officer Judy Hopps reflects on overcoming prejudice, owning up to mistakes, and choosing to do better.",
            interpretationKo = "완벽한 사람도, 편견 없는 세상도 없지만 변화는 결국 나 자신의 작은 선택과 태도에서 시작된다는 희망의 메시지입니다.",
            interpretationEn = "Nobody is immune to mistakes, yet meaningful change always begins with personal accountability and courage."
        ),
        QuoteItem(
            textKo = "심장은 무거운 법이야. 하지만 지켜야 할 사람이 생기면 그 무게를 견딜 수 있게 되지.",
            textEn = "A heart's a heavy burden.",
            author = "소피 & 하울 · 영화 《하울의 움직이는 성》 (2004)",
            authorEn = "Sophie · Howl's Moving Castle (2004)",
            tag = "사랑과 책임",
            categoryKo = "애니메이션 명대사",
            categoryEn = "Animation Quote",
            backgroundKo = "스튜디오 지브리 《하울의 움직이는 성》에서 저주에 걸린 소피가 하울의 심장을 되돌려주며 인간의 감정과 삶의 무게를 깨닫는 명장면의 대사입니다.",
            backgroundEn = "In Studio Ghibli's Howl's Moving Castle, Sophie speaks this line as she understands the weight and warmth of carrying a living heart.",
            interpretationKo = "누군가를 사랑하고 책임진다는 것은 분명 두렵고 버거운 일이지만, 바로 그 무게 덕분에 우리의 삶이 진짜로 살아 숨 쉬게 됩니다.",
            interpretationEn = "Caring deeply brings vulnerability and weight, yet that very burden is what makes life meaningful and human."
        ),
        QuoteItem(
            textKo = "내일 무슨 일이 일어날지는 아무도 몰라. 그러니 오늘을 가장 소중하게 살아야 해.",
            textEn = "Yesterday is history, tomorrow is a mystery, but today is a gift. That is why it is called the present.",
            author = "우그웨이 대사부 · 영화 《쿵푸팬더》 (2008)",
            authorEn = "Master Oogway · Kung Fu Panda (2008)",
            tag = "현재의 가치",
            categoryKo = "애니메이션 명대사",
            categoryEn = "Animation Quote",
            backgroundKo = "드림웍스 《쿵푸팬더》에서 과거의 후회와 미래의 불안에 흔들리는 포에게 우그웨이 대사부가 복숭아나무 아래에서 건네는 유명한 가르침입니다.",
            backgroundEn = "Under the peach tree in DreamWorks' Kung Fu Panda, Master Oogway calms Po's doubts with this timeless play on the word 'present.'",
            interpretationKo = "지나간 어제와 아직 오지 않은 내일에 마음을 빼앗기기보다, 지금 눈앞에 주어진 오늘 하루를 선물처럼 온전히 누리고 집중하라는 뜻입니다.",
            interpretationEn = "Instead of dwelling on regret or worrying about the future, cherish and act fully within the gift of today."
        ),
        QuoteItem(
            textKo = "우리가 함께했던 시간은 결코 사라지지 않아. 새로운 모험이 우리를 기다리고 있을 뿐이야.",
            textEn = "To infinity and beyond!",
            author = "버즈 라이트이어 · 영화 《토이 스토리》 (1995)",
            authorEn = "Buzz Lightyear · Toy Story (1995)",
            tag = "도전과 우정",
            categoryKo = "애니메이션 명대사",
            categoryEn = "Animation Quote",
            backgroundKo = "픽사의 첫 장편 애니메이션 《토이 스토리》 시리즈를 관통하는 버즈 라이트이어의 상징적인 구호이자, 두려움 없는 도전을 상징하는 대사입니다.",
            backgroundEn = "Buzz Lightyear's signature motto across Pixar's Toy Story series represents boundless optimism, loyalty, and courage.",
            interpretationKo = "스스로 한계를 긋지 않고 한 걸음 더 내딛는 태도가 평범한 일상을 설레는 모험으로 바꾼다는 긍정의 에너지를 전해 줍니다.",
            interpretationEn = "Refusing to box yourself in turns ordinary days into bold adventures."
        ),
        QuoteItem(
            textKo = "바람이 분다. 살아야겠다.",
            textEn = "The wind is rising! We must try to live!",
            author = "지로 & 나호코 · 영화 《바람이 분다》 (2013)",
            authorEn = "The Wind Rises (2013)",
            tag = "삶의 의지",
            categoryKo = "애니메이션 명대사",
            categoryEn = "Animation Quote",
            backgroundKo = "프랑스 시인 폴 발레리의 시 《해변의 묘지》에서 인용되어, 미야자키 하야오 감독의 애니메이션 《바람이 분다》의 핵심 주제로 쓰인 문장입니다.",
            backgroundEn = "Borrowed from Paul Valéry's poem 'Le Cimetière marin,' this line anchors Hayao Miyazaki's film The Wind Rises.",
            interpretationKo = "시대가 거칠고 바람이 세차게 불어와도, 그럼에도 불구하고 오늘을 성실히 살아내고 꿈을 빚어내겠다는 단단한 의지를 일깨워 줍니다.",
            interpretationEn = "Even when turbulent winds blow through life, we choose to rise, endure, and keep living with purpose."
        ),

        // --- 영화 명대사 (Movie) ---
        QuoteItem(
            textKo = "인생은 초콜릿 상자와 같은 거야. 네가 무엇을 고를지 아무도 모른단다.",
            textEn = "Life was like a box of chocolates. You never know what you're gonna get.",
            author = "포레스트 검프 · 영화 《포레스트 검프》 (1994)",
            authorEn = "Forrest Gump · Forrest Gump (1994)",
            tag = "인생과 기대",
            categoryKo = "영화 명대사",
            categoryEn = "Movie Quote",
            backgroundKo = "로버트 저메키스 감독의 영화 《포레스트 검프》에서, 버스 정류장 벤치에 앉은 포레스트가 어머니에게 들었던 말이라며 낯선 이에게 건네는 첫 대사입니다.",
            backgroundEn = "Sitting on a bus stop bench in Robert Zemeckis's Forrest Gump, Forrest shares his mother's favorite metaphor for life.",
            interpretationKo = "내일의 결과는 예측할 수 없지만, 그 불확실함 자체를 두려워하기보다 뜻밖의 선물을 열어보듯 담담하고 성실하게 맞이하라는 지혜를 담고 있습니다.",
            interpretationEn = "Life's unpredictability is not something to fear—each unknown turn can hold an unexpected gift."
        ),
        QuoteItem(
            textKo = "희망은 좋은 거예요. 어쩌면 가장 좋은 것이죠. 그리고 좋은 것은 결코 사라지지 않아요.",
            textEn = "Hope is a good thing, maybe the best of things, and no good thing ever dies.",
            author = "앤디 듀프레인 · 영화 《쇼생크 탈출》 (1994)",
            authorEn = "Andy Dufresne · The Shawshank Redemption (1994)",
            tag = "희망과 인내",
            categoryKo = "영화 명대사",
            categoryEn = "Movie Quote",
            backgroundKo = "프랭크 다라본트 감독의 《쇼생크 탈출》에서 누명을 쓰고 수감된 앤디가, 오랜 친구 레드에게 남긴 편지 속에 적힌 명대사입니다.",
            backgroundEn = "In The Shawshank Redemption, Andy writes these words in a letter to Red, defending hope even inside prison walls.",
            interpretationKo = "아무리 답답하고 막막한 현실 속에서도 마음속 희망의 불씨를 놓지 않는 사람만이 결국 새로운 내일의 문을 열 수 있습니다.",
            interpretationEn = "No matter how bleak the circumstances, holding onto hope keeps the door to freedom and renewal open."
        ),
        QuoteItem(
            textKo = "현재를 즐겨라(카르페 디엠). 너희들의 삶을 특별하게 만들어라.",
            textEn = "Carpe diem. Seize the day, boys. Make your lives extraordinary.",
            author = "존 키팅 · 영화 《죽은 시인의 사회》 (1989)",
            authorEn = "John Keating · Dead Poets Society (1989)",
            tag = "주체적 삶",
            categoryKo = "영화 명대사",
            categoryEn = "Movie Quote",
            backgroundKo = "피터 위어 감독의 《죽은 시인의 사회》에서, 웰튼 아카데미의 새 문학 교사 키팅(로빈 윌리엄스)이 학생들에게 졸업생들의 옛 사진을 보여주며 던진 외침입니다.",
            backgroundEn = "In Dead Poets Society, teacher John Keating urges his students to look at old alumni photos and live boldly in the present.",
            interpretationKo = "남들이 정해 놓은 틀에 맞춰 시간을 흘려보내지 말고, 단 한 번뿐인 오늘을 내 목소리와 열정으로 채우라는 강렬한 권유입니다.",
            interpretationEn = "Do not let life slip by on autopilot; claim today with your own voice, curiosity, and courage."
        ),
        QuoteItem(
            textKo = "세상을 보고, 장애물을 넘어, 벽을 허물고 더 가까이 다가가 서로를 알아가고 느끼는 것. 그것이 인생의 목적이다.",
            textEn = "To see the world, things dangerous to come to, to see behind walls, draw closer, to find each other and to feel. That is the purpose of life.",
            author = "라이프 매거진 · 영화 《월터의 상상은 현실이 된다》 (2013)",
            authorEn = "LIFE Motto · The Secret Life of Walter Mitty (2013)",
            tag = "모험과 실행",
            categoryKo = "영화 명대사",
            categoryEn = "Movie Quote",
            backgroundKo = "벤 스틸러 주연·감독의 《월터의 상상은 현실이 된다》에서, 평범한 회사원 월터 미티가 사라진 25번 필름을 찾아 떠나며 마주하게 되는 잡지사 라이프의 모토입니다.",
            backgroundEn = "In The Secret Life of Walter Mitty, this LIFE magazine motto inspires Walter to step out of daydreams into real adventure.",
            interpretationKo = "머릿속 상상에만 머물지 않고 직접 부딪히며 세상과 사람을 경험할 때, 비로소 삶이 선명하고 풍요로워집니다.",
            interpretationEn = "Life comes alive when we step beyond comfort and hesitation to truly experience the world and one another."
        ),
        QuoteItem(
            textKo = "우리는 답을 찾을 것이다. 늘 그랬듯이.",
            textEn = "We will find a way. We always have.",
            author = "쿠퍼 · 영화 《인터스텔라》 (2014)",
            authorEn = "Cooper · Interstellar (2014)",
            tag = "도전과 신념",
            categoryKo = "영화 명대사",
            categoryEn = "Movie Quote",
            backgroundKo = "크리스토퍼 놀란 감독의 SF 영화 《인터스텔라》에서 인류의 생존이 위태로운 절체절명의 상황 속에서도, 파일럿 쿠퍼가 포기하지 않고 내뱉는 다짐입니다.",
            backgroundEn = "In Christopher Nolan's Interstellar, Cooper voices humanity's stubborn resilience when facing seemingly impossible odds.",
            interpretationKo = "당장 완벽한 해법이 보이지 않더라도 끝까지 물러서지 않고 탐구하다 보면, 길은 반드시 열린다는 믿음을 보여 줍니다.",
            interpretationEn = "Even when the path ahead is unclear, persistence and ingenuity have a way of creating breakthroughs."
        ),
        QuoteItem(
            textKo = "인생은 모두가 함께하는 시간 여행이다. 우리가 할 수 있는 최선은 이 멋진 여행을 만끽하는 것이다.",
            textEn = "We're all traveling through time together, every day of our lives. All we can do is do our best to relish this remarkable ride.",
            author = "팀 · 영화 《어바웃 타임》 (2013)",
            authorEn = "Tim · About Time (2013)",
            tag = "일상의 행복",
            categoryKo = "영화 명대사",
            categoryEn = "Movie Quote",
            backgroundKo = "리처드 커티스 감독의 《어바웃 타임》 마지막 장면에서, 시간 여행 능력을 더 이상 쓰지 않기로 결심한 주인공 팀이 깨달은 삶의 진실입니다.",
            backgroundEn = "At the end of Richard Curtis's About Time, Tim realizes he no longer needs to rewind time to appreciate ordinary days.",
            interpretationKo = "특별한 능력으로 어제를 되돌리지 않더라도, 평범한 오늘을 감사하며 살아가는 태도 자체가 가장 위대한 기적입니다.",
            interpretationEn = "The real magic isn't reliving the past—it's savoring each ordinary day as if it were a second chance."
        ),

        // --- 고전 명언 (Classic Quotes) ---
        QuoteItem(
            textKo = "게으름은 쇠에 스며드는 녹과 같다. 사용하지 않는 열쇠는 녹슬지만, 끊임없이 쓰는 열쇠는 언제나 빛난다.",
            textEn = "Sloth makes all things difficult, but industry all easy.",
            author = "벤자민 프랭클린 (Benjamin Franklin)",
            authorEn = "Benjamin Franklin",
            tag = "근면과 실행",
            categoryKo = "고전 명언",
            categoryEn = "Classic Quote",
            backgroundKo = "미국의 정치가이자 과학자였던 벤자민 프랭클린이 《가난한 리처드의 연감》을 통해 일상의 근면함과 꾸준한 자기 관리의 중요성을 강조하며 남긴 격언입니다.",
            backgroundEn = "Benjamin Franklin shared this maxim in Poor Richard's Almanack to remind readers that steady effort keeps skills sharp.",
            interpretationKo = "재능이나 열정도 쓰지 않고 방치하면 무뎌지지만, 매일 조금씩 갈고닦는 꾸준함이 결국 가장 빛나는 실력으로 이어집니다.",
            interpretationEn = "Consistent daily effort keeps our abilities bright and sharp, while neglect slowly dulls even the finest talent."
        ),
        QuoteItem(
            textKo = "완벽한 준비를 기다리다 보면, 시작할 수 있는 날은 결코 오지 않는다.",
            textEn = "If we wait for the moment when everything is ready, we shall never begin.",
            author = "이반 투르게네프 (Ivan Turgenev)",
            authorEn = "Ivan Turgenev",
            tag = "시작의 용기",
            categoryKo = "고전 명언",
            categoryEn = "Classic Quote",
            backgroundKo = "19세기 러시아의 대문호 이반 투르게네프가 망설임과 완벽주의에 갇혀 행동을 미루는 이들에게 건넨 날카로운 통찰입니다.",
            backgroundEn = "19th-century Russian novelist Ivan Turgenev warned against letting perfectionism become an excuse for inaction.",
            interpretationKo = "충분히 준비되지 않았다고 느껴질 때라도 일단 첫 발을 내디뎌야, 비로소 무엇을 보완해야 할지 보이고 길이 열립니다.",
            interpretationEn = "Clarity comes from starting, not from waiting for flawless conditions that never arrive."
        ),
        QuoteItem(
            textKo = "할 수 있다고 믿든 할 수 없다고 믿든, 당신의 믿음이 옳다.",
            textEn = "Whether you think you can, or you think you can't—you're right.",
            author = "헨리 포드 (Henry Ford)",
            authorEn = "Henry Ford",
            tag = "마음가짐",
            categoryKo = "고전 명언",
            categoryEn = "Classic Quote",
            backgroundKo = "자동차 대중화의 시대를 연 기업가 헨리 포드가 수많은 기술적 난관과 회의론을 돌파하며 강조했던 자기 확신의 원칙입니다.",
            backgroundEn = "Industrialist Henry Ford often emphasized how mindset shapes persistence and outcome long before results appear.",
            interpretationKo = "가능성을 믿는 사람은 방법을 찾아 움직이고, 불가능을 단정 짓는 사람은 시도조차 멈추기에 결국 각자의 믿음대로 결과가 만들어집니다.",
            interpretationEn = "Your belief determines whether you look for solutions or stop trying—making your mindset a self-fulfilling prophecy."
        ),
        QuoteItem(
            textKo = "천 리 길도 한 걸음부터 시작된다. 멈추지 않는 한 얼마나 천천히 가는지는 중요하지 않다.",
            textEn = "It does not matter how slowly you go as long as you do not stop.",
            author = "공자 (Confucius)",
            authorEn = "Confucius",
            tag = "꾸준함",
            categoryKo = "고전 명언",
            categoryEn = "Classic Quote",
            backgroundKo = "동양 사상의 큰 스승 공자가 학문과 수양은 단번에 이루는 것이 아니라 끊임없는 정진으로 완성됨을 비유하며 남긴 가르침입니다.",
            backgroundEn = "Confucius taught that lasting character and mastery are built through steady, unbroken progress over time.",
            interpretationKo = "남들과 속도를 비교하며 조급해할 필요는 없습니다. 오늘 나만의 속도로 한 걸음 더 나아가는 꾸준함이 결국 목적지에 닿게 합니다.",
            interpretationEn = "Speed matters far less than continuity; steady steps forward will always carry you farther than bursts followed by quitting."
        ),
        QuoteItem(
            textKo = "천재는 1%의 영감과 99%의 땀으로 이루어진다.",
            textEn = "Genius is one percent inspiration and ninety-nine percent perspiration.",
            author = "토마스 에디슨 (Thomas Edison)",
            authorEn = "Thomas Edison",
            tag = "노력과 끈기",
            categoryKo = "고전 명언",
            categoryEn = "Classic Quote",
            backgroundKo = "수천 번의 실패 끝에 백열전구를 상용화한 발명가 토마스 에디슨이 번뜩이는 아이디어보다 끈질긴 실험 정신의 가치를 설명하며 한 말입니다.",
            backgroundEn = "After thousands of experiments, Thomas Edison credited tireless testing—far more than sudden flashes of insight—for invention.",
            interpretationKo = "좋은 아이디어만으로는 세상이 바뀌지 않습니다. 묵묵히 실패를 수정하고 반복하는 성실한 땀방울이 결과를 완성합니다.",
            interpretationEn = "Inspiration sparks the idea, but patient, persistent work is what turns an idea into reality."
        ),
        QuoteItem(
            textKo = "아무것도 하지 않으면 아무 일도 일어나지 않는다. 작은 실행이 거대한 가능성을 연다.",
            textEn = "Nothing will work unless you do.",
            author = "마야 안젤루 (Maya Angelou)",
            authorEn = "Maya Angelou",
            tag = "실행력",
            categoryKo = "고전 명언",
            categoryEn = "Classic Quote",
            backgroundKo = "미국의 시인이자 인권운동가 마야 안젤루가 변화와 성취는 막연한 바람이 아니라 직접 몸을 움직이는 실천에서 나온다고 강조하며 남긴 문장입니다.",
            backgroundEn = "Poet and civil rights activist Maya Angelou distilled the essence of personal agency into six simple words.",
            interpretationKo = "고민이 길어질수록 마음은 무거워집니다. 오늘 할 수 있는 가장 작은 행동 하나를 실행에 옮기는 순간, 멈춰 있던 흐름이 움직이기 시작합니다.",
            interpretationEn = "Even the best plans remain dormant until you take the first concrete action today."
        )
    )

    fun fetchQuotes(language: String = "ko"): Pair<QuoteItem, QuoteItem> {
        val i1 = Random.nextInt(CURATED_QUOTES.size)
        val i2 = (i1 + 1 + Random.nextInt(CURATED_QUOTES.size - 1)) % CURATED_QUOTES.size
        return CURATED_QUOTES[i1] to CURATED_QUOTES[i2]
    }

    private val EN_NATIVE_NEWS_COUNTRIES = setOf(
        "US", "GB", "CA", "AU", "NZ", "IE", "SG", "IN", "PH", "ZA", "MY"
    )

    private fun buildNewsRssUrl(countryCode: String, language: String): String {
        val cc = countryCode.trim().uppercase(Locale.US).ifEmpty { "KR" }
        if (language == "en") {
            return if (cc in EN_NATIVE_NEWS_COUNTRIES) {
                "https://news.google.com/rss?hl=en&gl=$cc&ceid=$cc:en"
            } else {
                val countryName = Locale("", cc).getDisplayCountry(Locale.ENGLISH).ifBlank { cc }
                val q = URLEncoder.encode(countryName, "UTF-8")
                "https://news.google.com/rss/search?q=$q&hl=en-US&gl=US&ceid=US:en"
            }
        }
        return "https://news.google.com/rss?hl=ko&gl=$cc&ceid=$cc:ko"
    }

    fun fetchNews(countryCode: String = "KR", language: String = "ko"): List<NewsItem> {
        return try {
            val xml = WeatherRepository.httpGet(buildNewsRssUrl(countryCode, language))
            parseGoogleNewsRss(xml, language)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseGoogleNewsRss(xml: String, language: String): List<NewsItem> {
        val items = ArrayList<NewsItem>(6)
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var inItem = false
        var tag = ""
        var title = ""
        var source = ""
        var pubDate = ""

        val rssDateFmt = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }
        val nowMs = System.currentTimeMillis()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT && items.size < 6) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    tag = parser.name ?: ""
                    if (tag == "item") {
                        inItem = true
                        title = ""
                        source = ""
                        pubDate = ""
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inItem) {
                        val txt = parser.text ?: ""
                        when (tag) {
                            "title" -> title += txt
                            "source" -> source += txt
                            "pubDate" -> pubDate += txt
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "item" && inItem) {
                        inItem = false
                        val cleanSource = source.trim().ifEmpty { if (language == "en") "News" else "뉴스" }
                        val suffix = " - $cleanSource"
                        val cleanTitle = title.trim().let {
                            if (it.endsWith(suffix)) it.removeSuffix(suffix).trim() else it
                        }
                        if (cleanTitle.isNotEmpty()) {
                            items.add(
                                NewsItem(
                                    category = inferCategory(cleanTitle, language),
                                    title = cleanTitle,
                                    source = cleanSource,
                                    timeAgo = formatTimeAgo(pubDate.trim(), rssDateFmt, nowMs, language)
                                )
                            )
                        }
                    }
                    tag = ""
                }
            }
            event = parser.next()
        }
        return items
    }

    private fun formatTimeAgo(
        pubDate: String,
        fmt: SimpleDateFormat,
        nowMs: Long,
        language: String
    ): String {
        val isEn = language == "en"
        return try {
            val d = fmt.parse(pubDate) ?: return if (isEn) "Recent" else "최근"
            val diffMin = ((nowMs - d.time) / 60_000L).coerceAtLeast(0L)
            if (isEn) {
                when {
                    diffMin < 1L -> "Just now"
                    diffMin < 60L -> "${diffMin}m ago"
                    diffMin < 24L * 60L -> "${diffMin / 60L}h ago"
                    else -> "${diffMin / (24L * 60L)}d ago"
                }
            } else {
                when {
                    diffMin < 1L -> "방금 전"
                    diffMin < 60L -> "${diffMin}분 전"
                    diffMin < 24L * 60L -> "${diffMin / 60L}시간 전"
                    else -> "${diffMin / (24L * 60L)}일 전"
                }
            }
        } catch (_: Exception) {
            if (isEn) "Recent" else "최근"
        }
    }

    private fun inferCategory(title: String, language: String): String = if (language == "en") {
        when {
            listOf("stock", "market", "economy", "inflation", "rate", "trade", "bank", "gdp", "tariff")
                .any { title.contains(it, ignoreCase = true) } -> "Business"
            listOf("AI", "tech", "robot", "space", "science", "apple", "google", "chip", "software")
                .any { title.contains(it, ignoreCase = true) } -> "Tech"
            listOf("president", "congress", "senate", "election", "minister", "government", "parliament", "policy")
                .any { title.contains(it, ignoreCase = true) } -> "Politics"
            listOf("world", "china", "russia", "ukraine", "europe", "asia", "summit", "war", "un ")
                .any { title.contains(it, ignoreCase = true) } -> "World"
            listOf("weather", "storm", "flight", "health", "hospital", "school", "climate")
                .any { title.contains(it, ignoreCase = true) } -> "Society"
            else -> "Top"
        }
    } else {
        when {
            listOf("코스피", "코스닥", "환율", "금리", "수출", "물가", "경제", "증시", "반도체", "삼성", "현대", "무역", "예산").any { title.contains(it) } -> "경제"
            listOf("AI", "IT", "로봇", "우주", "과학", "애플", "구글", "출시", "기술", "배터리").any { title.contains(it, ignoreCase = true) } -> "IT/과학"
            listOf("대통령", "국회", "여당", "야당", "총리", "외교", "장관", "정부", "선거").any { title.contains(it) } -> "정치"
            listOf("미국", "중국", "일본", "유럽", "트럼프", "러시아", "이란", "우크라", "글로벌").any { title.contains(it) } -> "국제"
            listOf("날씨", "기온", "태풍", "폭염", "한파", "건강", "의료", "병원", "학교").any { title.contains(it) } -> "사회"
            else -> "주요"
        }
    }
}
