package com.ridipaper.weather.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.ridipaper.weather.model.BookItem
import com.ridipaper.weather.model.EconomyIndicator
import com.ridipaper.weather.model.MovieItem
import com.ridipaper.weather.model.NewsItem
import com.ridipaper.weather.model.QuoteItem
import java.net.HttpURLConnection
import java.net.URL
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

    private val CURATED_BOOKS = listOf(
        BookItem(
            titleKo = "물고기는 존재하지 않는다",
            titleEn = "Why Fish Don't Exist",
            authorKo = "룰루 밀러 (Lulu Miller) · 곰출판",
            authorEn = "Lulu Miller · Simon & Schuster",
            publisherYear = "2021",
            genreKo = "과학 에세이",
            genreEn = "Science Memoir",
            rating = "★ 4.8 / 5.0",
            pagesTimeKo = "300쪽 · 완독 4시간",
            pagesTimeEn = "300p · ~4h read",
            summaryKo = "과학 전문 기자 룰루 밀러가 집착에 가까운 열정으로 '질서'를 세우려 했던 분류학자 데이비드 스타 조던의 삶을 추적하며, 우리가 당연하게 믿어온 세상의 기준과 삶의 의미를 다시 묻는 매혹적인 논픽션입니다.",
            summaryEn = "Science journalist Lulu Miller traces the life of taxonomist David Starr Jordan, weaving biography and memoir into a gripping meditation on chaos, obsession, and resilience.",
            quoteKo = "혼돈은 우리가 생각하는 것보다 훨씬 자비롭다.",
            quoteEn = "Chaos is much more merciful than we think.",
            keywordsKo = "#질서와혼돈  #자기이해  #주말독서",
            keywordsEn = "#ChaosAndOrder  #Memoir  #Science",
            coverUrl = "http://covers.openlibrary.org/b/isbn/9781501160271-M.jpg"
        ),
        BookItem(
            titleKo = "아몬드",
            titleEn = "Almond",
            authorKo = "손원평 · 창비",
            authorEn = "Won-pyung Sohn · HarperVia",
            publisherYear = "2017",
            genreKo = "한국 소설",
            genreEn = "Korean Fiction",
            rating = "★ 4.7 / 5.0",
            pagesTimeKo = "264쪽 · 완독 3시간",
            pagesTimeEn = "264p · ~3h read",
            summaryKo = "감정을 느끼지 못하는 소년 윤재가 세상과 부딪히며 타인의 마음을 배우고 성장해 가는 과정을 담백하면서도 강렬한 문체로 그려낸 현대 소설입니다.",
            summaryEn = "Born with alexithymia, young Yunjae navigates grief, unlikely friendship, and what it truly means to feel and connect with others.",
            quoteKo = "멀리서 보면 비극이지만 가까이서 보면 희극이 아니라, 그냥 삶일 뿐이다.",
            quoteEn = "Love makes a monster into a human, and a human into a monster.",
            keywordsKo = "#공감과성장  #청소년문학  #몰입감",
            keywordsEn = "#Empathy  #ComingOfAge  #Fiction",
            coverUrl = "http://covers.openlibrary.org/b/isbn/9780062961372-M.jpg"
        ),
        BookItem(
            titleKo = "사피엔스",
            titleEn = "Sapiens",
            authorKo = "유발 하라리 (Yuval Noah Harari) · 김영사",
            authorEn = "Yuval Noah Harari · Harper",
            publisherYear = "2015",
            genreKo = "인문 · 역사",
            genreEn = "History · Big Ideas",
            rating = "★ 4.8 / 5.0",
            pagesTimeKo = "636쪽 · 완독 9시간",
            pagesTimeEn = "464p · ~8h read",
            summaryKo = "변방의 유인원이었던 호모 사피엔스가 어떻게 인지 혁명, 농업 혁명, 과학 혁명을 거쳐 지구의 지배자가 되었는지 거시적인 시선으로 탐구하는 인류 문명사입니다.",
            summaryEn = "From foraging bands to the digital age, Harari explores how shared myths, cooperation, and science shaped human civilization.",
            quoteKo = "역사는 소수의 사람만이 아니라 우리가 믿는 이야기로 만들어진다.",
            quoteEn = "Large numbers of strangers can cooperate successfully by believing in common myths.",
            keywordsKo = "#인류사  #인문학필독  #거시적통찰",
            keywordsEn = "#HumanHistory  #BigIdeas  #Nonfiction",
            coverUrl = "http://covers.openlibrary.org/b/isbn/9780062316097-M.jpg"
        ),
        BookItem(
            titleKo = "프로젝트 헤일메리",
            titleEn = "Project Hail Mary",
            authorKo = "앤디 위어 (Andy Weir) · 알에이치코리아",
            authorEn = "Andy Weir · Ballantine Books",
            publisherYear = "2021",
            genreKo = "SF 소설",
            genreEn = "Sci-Fi Thriller",
            rating = "★ 4.9 / 5.0",
            pagesTimeKo = "648쪽 · 완독 8시간",
            pagesTimeEn = "496p · ~7h read",
            summaryKo = "기억을 잃은 채 우주선에서 깨어난 중학교 과학 교사 라일랜드 그레이스가 멸망 위기에 처한 지구를 구하기 위해 치밀한 과학 추론과 뜻밖의 우정을 펼쳐 나가는 SF 대작입니다.",
            summaryEn = "Waking up alone on a spacecraft light-years from Earth, Ryland Grace must solve an extinction-level mystery with science and an unexpected ally.",
            quoteKo = "두려움은 문제를 해결해 주지 않는다. 한 번에 하나씩 계산할 뿐이다.",
            quoteEn = "Science doesn't care what you believe; it only cares about what works.",
            keywordsKo = "#우주SF  #지적쾌감  #우정과희망",
            keywordsEn = "#SpaceSciFi  #ProblemSolving  #PageTurner",
            coverUrl = "http://covers.openlibrary.org/b/isbn/9780593135204-M.jpg"
        ),
        BookItem(
            titleKo = "아주 작은 습관의 힘",
            titleEn = "Atomic Habits",
            authorKo = "제임스 클리어 (James Clear) · 비즈니스북스",
            authorEn = "James Clear · Avery",
            publisherYear = "2019",
            genreKo = "자기계발",
            genreEn = "Self-Improvement",
            rating = "★ 4.8 / 5.0",
            pagesTimeKo = "400쪽 · 완독 5시간",
            pagesTimeEn = "320p · ~5h read",
            summaryKo = "거창한 목표보다 매일 1%의 작은 개선과 환경 설계가 어떻게 복리 효과를 만들어 삶을 근본적으로 바꾸는지 구체적인 행동 과학으로 설명합니다.",
            summaryEn = "Tiny 1% improvements compound into remarkable results when you focus on systems, identity, and environment design.",
            quoteKo = "목표의 수준까지 올라가는 것이 아니라, 시스템의 수준까지 떨어진다.",
            quoteEn = "You do not rise to the level of your goals. You fall to the level of your systems.",
            keywordsKo = "#습관설계  #실행력  #루틴",
            keywordsEn = "#Habits  #Systems  #Productivity",
            coverUrl = "http://covers.openlibrary.org/b/isbn/9780735211292-M.jpg"
        ),
        BookItem(
            titleKo = "미드나잇 라이브러리",
            titleEn = "The Midnight Library",
            authorKo = "매트 헤이그 (Matt Haig) · 인플루엔셜",
            authorEn = "Matt Haig · Viking",
            publisherYear = "2021",
            genreKo = "영미 소설",
            genreEn = "Contemporary Fiction",
            rating = "★ 4.6 / 5.0",
            pagesTimeKo = "408쪽 · 완독 5시간",
            pagesTimeEn = "304p · ~4.5h read",
            summaryKo = "삶의 경계에 선 주인공 노라가 '내가 다른 선택을 했더라면'이라는 수많은 평행 우주의 삶을 책으로 꺼내 읽어보며 지금 이 순간의 가치를 깨닫는 따뜻한 판타지 소설입니다.",
            summaryEn = "Between life and death lies a library of infinite books, each offering Nora Seed a chance to live out the choices she never made.",
            quoteKo = "후회는 우리가 보지 못한 가능성을 비추는 그림자일 뿐이다.",
            quoteEn = "It is easy to mourn the lives we aren't living.",
            keywordsKo = "#인생의선택  #위로와회복  #베스트셀러",
            keywordsEn = "#SecondChances  #WarmFiction  #Reflection",
            coverUrl = "http://covers.openlibrary.org/b/isbn/9780525559474-M.jpg"
        )
    )

    private val CURATED_MOVIES = listOf(
        MovieItem(
            titleKo = "월터의 상상은 현실이 된다",
            titleEn = "The Secret Life of Walter Mitty",
            originalTitleYear = "The Secret Life of Walter Mitty (2013)",
            directorKo = "감독: 벤 스틸러",
            directorEn = "Dir. Ben Stiller",
            genreKo = "드라마 · 모험",
            genreEn = "Drama · Adventure",
            rating = "★ IMDb 7.3",
            runtimeRatingKo = "114분 · 전체관람가",
            runtimeRatingEn = "114m · PG",
            synopsisKo = "잡지사 '라이프'에서 16년째 필름을 관리하며 소심한 일상을 보내던 월터가, 폐간호 표지용 마지막 필름을 찾기 위해 그린란드와 아이슬란드, 히말라야로 직접 떠나며 진짜 삶을 마주하는 이야기입니다.",
            synopsisEn = "Daydreamer Walter Mitty steps out of his quiet photo-archive routine and crosses Greenland, Iceland, and the Himalayas in search of a missing negative.",
            highlightKo = "광활한 아이슬란드 풍경과 가슴 뛰는 OST, 용기의 메시지",
            highlightEn = "Breathtaking Iceland landscapes, uplifting soundtrack, and quiet courage",
            castOttKo = "출연: 벤 스틸러, 크리스틴 위그, 숀 펜  |  OTT 스트리밍 추천",
            castOttEn = "Cast: Ben Stiller, Kristen Wiig, Sean Penn  |  Feel-Good Classic",
            posterTitleTop = "WALTER",
            posterTitleBottom = "MITTY",
            posterUrl = "https://upload.wikimedia.org/wikipedia/en/f/f2/The_Secret_Life_of_Walter_Mitty_2013_poster.jpg"
        ),
        MovieItem(
            titleKo = "어바웃 타임",
            titleEn = "About Time",
            originalTitleYear = "About Time (2013)",
            directorKo = "감독: 리처드 커티스",
            directorEn = "Dir. Richard Curtis",
            genreKo = "로맨스 · 드라마",
            genreEn = "Romance · Drama",
            rating = "★ IMDb 7.8",
            runtimeRatingKo = "123분 · 15세관람가",
            runtimeRatingEn = "123m · R",
            synopsisKo = "시간을 되돌릴 수 있는 가문의 비밀을 알게 된 팀이 사랑과 가족의 소중함을 배워가며, 결국 평범한 오늘 하루를 두 번째로 사는 마음으로 살아가는 법을 깨닫는 작품입니다.",
            synopsisEn = "Tim discovers he can travel back in his own timeline, learning that happiness lies not in fixing every flaw, but in cherishing ordinary days.",
            highlightKo = "평범한 일상을 다시 바라보게 만드는 따뜻한 여운",
            highlightEn = "A heartfelt reminder to savor every ordinary day as a gift",
            castOttKo = "출연: 도널 글리슨, 레이첼 맥아담스, 빌 나이  |  인생 영화",
            castOttEn = "Cast: Domhnall Gleeson, Rachel McAdams, Bill Nighy",
            posterTitleTop = "ABOUT",
            posterTitleBottom = "TIME",
            posterUrl = "https://upload.wikimedia.org/wikipedia/en/7/7c/About_Time_%282013_film%29_Poster.jpg"
        ),
        MovieItem(
            titleKo = "인턴",
            titleEn = "The Intern",
            originalTitleYear = "The Intern (2015)",
            directorKo = "감독: 낸시 마이어스",
            directorEn = "Dir. Nancy Meyers",
            genreKo = "코미디 · 드라마",
            genreEn = "Comedy · Drama",
            rating = "★ IMDb 7.1",
            runtimeRatingKo = "121분 · 12세관람가",
            runtimeRatingEn = "121m · PG-13",
            synopsisKo = "은퇴 후 시니어 인턴으로 패션 스타트업에 합류한 70세 벤이, 바쁘고 치열한 젊은 대표 줄스와 세대를 뛰어넘는 신뢰와 우정을 쌓아가는 유쾌하고 따뜻한 이야기입니다.",
            synopsisEn = "Seventy-year-old widower Ben joins an online fashion startup as a senior intern, bringing calm wisdom to driven founder Jules.",
            highlightKo = "경험에서 우러나는 품격과 세대 간 따뜻한 연대",
            highlightEn = "Timeless grace, mentorship, and cross-generational warmth",
            castOttKo = "출연: 로버트 드 니로, 앤 해서웨이  |  주말 힐링 추천",
            castOttEn = "Cast: Robert De Niro, Anne Hathaway  |  Warm Workplace Drama",
            posterTitleTop = "THE",
            posterTitleBottom = "INTERN",
            posterUrl = "https://upload.wikimedia.org/wikipedia/en/c/c9/The_Intern_Poster.jpg"
        ),
        MovieItem(
            titleKo = "인터스텔라",
            titleEn = "Interstellar",
            originalTitleYear = "Interstellar (2014)",
            directorKo = "감독: 크리스토퍼 놀란",
            directorEn = "Dir. Christopher Nolan",
            genreKo = "SF · 드라마",
            genreEn = "Sci-Fi · Drama",
            rating = "★ IMDb 8.7",
            runtimeRatingKo = "169분 · 12세관람가",
            runtimeRatingEn = "169m · PG-13",
            synopsisKo = "식량난에 직면한 미래의 지구에서 인류의 새로운 터전을 찾기 위해 웜홀 너머 미지의 은하로 떠난 탐사대와, 지구에 남은 가족의 애틋한 시간을 그린 장대한 SF 서사시입니다.",
            synopsisEn = "With Earth's future fading, former pilot Cooper journeys through a wormhole to find a habitable world while racing against relativistic time.",
            highlightKo = "압도적인 우주 시각미와 한스 짐머의 웅장한 파이프 오르간 선율",
            highlightEn = "Awe-inspiring astrophysics, Hans Zimmer's score, and father-daughter bond",
            castOttKo = "출연: 매튜 맥커너히, 앤 해서웨이, 제시카 차스테인  |  SF 명작",
            castOttEn = "Cast: Matthew McConaughey, Anne Hathaway, Jessica Chastain",
            posterTitleTop = "INTER",
            posterTitleBottom = "STELLAR",
            posterUrl = "https://upload.wikimedia.org/wikipedia/en/b/bc/Interstellar_film_poster.jpg"
        ),
        MovieItem(
            titleKo = "그린 북",
            titleEn = "Green Book",
            originalTitleYear = "Green Book (2018)",
            directorKo = "감독: 피터 패럴리",
            directorEn = "Dir. Peter Farrelly",
            genreKo = "드라마 · 실화",
            genreEn = "Drama · Biography",
            rating = "★ IMDb 8.2",
            runtimeRatingKo = "130분 · 12세관람가",
            runtimeRatingEn = "130m · PG-13",
            synopsisKo = "1962년 미국 남부 콘서트 투어에 나선 천재 피아니스트 돈 셜리와 그의 운전사 토니 발레롱가가 서로의 다름을 이해하며 편견을 넘어선 우정을 쌓아가는 실화 바탕의 로드 무비입니다.",
            synopsisEn = "In 1962, a working-class bouncer drives renowned pianist Dr. Don Shirley through the Deep South, forging an unexpected friendship.",
            highlightKo = "유쾌한 티키타카 속에 담긴 존중과 용기의 메시지",
            highlightEn = "Sharp humor, soulful piano music, and dignity in the face of prejudice",
            castOttKo = "출연: 비고 모텐슨, 마허샬라 알리  |  아카데미 작품상",
            castOttEn = "Cast: Viggo Mortensen, Mahershala Ali  |  Oscar Best Picture",
            posterTitleTop = "GREEN",
            posterTitleBottom = "BOOK",
            posterUrl = "https://upload.wikimedia.org/wikipedia/en/5/5b/Green_Book_%282018_poster%29.png"
        ),
        MovieItem(
            titleKo = "리틀 포레스트",
            titleEn = "Little Forest",
            originalTitleYear = "Little Forest (2018)",
            directorKo = "감독: 임순례",
            directorEn = "Dir. Yim Soon-rye",
            genreKo = "드라마 · 힐링",
            genreEn = "Drama · Slice of Life",
            rating = "★ 4.5 / 5.0",
            runtimeRatingKo = "103분 · 전체관람가",
            runtimeRatingEn = "103m · All Ages",
            synopsisKo = "도시 생활에 지쳐 고향으로 돌아온 혜원이 사계절의 변화 속에서 직접 농사짓고 제철 음식을 만들어 먹으며 자신만의 작은 숲을 가꾸어 가는 담백한 성장 영화입니다.",
            synopsisEn = "Exhausted by city life, Hye-won returns to her rural hometown, cooking seasonal meals and rediscovering her own rhythm across four seasons.",
            highlightKo = "사계절의 정갈한 음식과 마음을 쉬게 하는 조용한 여백",
            highlightEn = "Comforting seasonal cooking and gentle breathing room for the mind",
            castOttKo = "출연: 김태리, 류준열, 진기주  |  사계절 힐링 영화",
            castOttEn = "Cast: Kim Tae-ri, Ryu Jun-yeol, Jin Ki-joo",
            posterTitleTop = "LITTLE",
            posterTitleBottom = "FOREST",
            posterUrl = "https://upload.wikimedia.org/wikipedia/en/a/a4/Little_Forest_%28%EB%A6%AC%ED%8B%80_%ED%8F%AC%EB%A0%88%EC%8A%A4%ED%8A%B8%29.jpg"
        )
    )

    fun fetchBook(): BookItem = CURATED_BOOKS[Random.nextInt(CURATED_BOOKS.size)]

    fun fetchMovie(): MovieItem = CURATED_MOVIES[Random.nextInt(CURATED_MOVIES.size)]

    fun fetchBitmap(urlStr: String, redirectsLeft: Int = 3): Bitmap? {
        if (urlStr.isBlank()) return null
        return try {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                if (this is javax.net.ssl.HttpsURLConnection) {
                    sslSocketFactory = WeatherRepository.tlsSocketFactory
                }
                connectTimeout = 7000
                readTimeout = 7000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "PaperProWeather/1.0 (https://github.com/jayjung1230-dot/Epub_WhetherForecast)")
            }
            val code = conn.responseCode
            if (code in listOf(301, 302, 303, 307, 308) && redirectsLeft > 0) {
                val loc = conn.getHeaderField("Location")
                if (!loc.isNullOrBlank()) {
                    return fetchBitmap(loc, redirectsLeft - 1)
                }
            }
            if (code in 200..299) {
                conn.inputStream.use { BitmapFactory.decodeStream(it) }
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private data class SymbolSpec(
        val symbol: String,
        val nameKo: String,
        val nameEn: String,
        val prefix: String,
        val decimals: Int,
        val fallbackVal: String,
        val fallbackDiff: String,
        val fallbackUp: Boolean,
        val fallbackHistory: List<Float>
    )

    private val ECONOMY_SPECS = listOf(
        SymbolSpec("^KS11", "코스피 (KOSPI)", "KOSPI", "", 2, "2,486.35", "▲ +18.42 (+0.75%)", true, listOf(2440f, 2455f, 2448f, 2462f, 2470f, 2468f, 2486f)),
        SymbolSpec("^KQ11", "코스닥 (KOSDAQ)", "KOSDAQ", "", 2, "712.80", "▼ -3.15 (-0.44%)", false, listOf(726f, 722f, 719f, 721f, 715f, 716f, 713f)),
        SymbolSpec("KRW=X", "원/달러 환율", "USD / KRW", "", 2, "1,394.50", "▲ +4.20 (+0.30%)", true, listOf(1382f, 1385f, 1388f, 1386f, 1391f, 1390f, 1395f)),
        SymbolSpec("^GSPC", "S&P 500", "S&P 500", "", 2, "5,948.71", "▲ +24.10 (+0.41%)", true, listOf(5870f, 5895f, 5910f, 5902f, 5925f, 5924f, 5949f)),
        SymbolSpec("^IXIC", "나스닥 (NASDAQ)", "NASDAQ", "", 2, "19,107.65", "▲ +112.4 (+0.59%)", true, listOf(18820f, 18900f, 18875f, 18960f, 19010f, 18995f, 19108f)),
        SymbolSpec("GC=F", "국제 금 (Gold)", "Gold (oz)", "$", 1, "$2,572.4", "▼ -8.6 (-0.33%)", false, listOf(2615f, 2602f, 2595f, 2588f, 2590f, 2581f, 2572f))
    )

    fun fetchEconomyIndicators(): List<EconomyIndicator> {
        return ECONOMY_SPECS.map { spec ->
            fetchYahooIndicator(spec) ?: EconomyIndicator(
                nameKo = spec.nameKo,
                nameEn = spec.nameEn,
                valueStr = spec.fallbackVal,
                diffStr = spec.fallbackDiff,
                isUp = spec.fallbackUp,
                history = spec.fallbackHistory
            )
        }
    }

    private fun fetchYahooIndicator(spec: SymbolSpec): EconomyIndicator? {
        return try {
            val enc = URLEncoder.encode(spec.symbol, "UTF-8")
            val url = "https://query1.finance.yahoo.com/v8/finance/chart/$enc?range=10d&interval=1d"
            val root = JSONObject(WeatherRepository.httpGet(url))
            val result = root.getJSONObject("chart").getJSONArray("result").getJSONObject(0)
            val meta = result.getJSONObject("meta")
            val closesArr = result.getJSONObject("indicators")
                .getJSONArray("quote").getJSONObject(0).getJSONArray("close")

            val closes = ArrayList<Float>()
            for (i in 0 until closesArr.length()) {
                if (!closesArr.isNull(i)) {
                    closes.add(closesArr.getDouble(i).toFloat())
                }
            }
            val recent = if (closes.size > 7) closes.takeLast(7) else closes
            if (recent.size < 2) return null

            val lastVal = meta.optDouble("regularMarketPrice", recent.last().toDouble())
            val prevVal = meta.optDouble("chartPreviousClose", recent[recent.size - 2].toDouble())
            val prevClose = if (recent.size >= 2) recent[recent.size - 2].toDouble() else prevVal
            val diff = lastVal - prevClose
            val pct = if (prevClose != 0.0) (diff / prevClose) * 100.0 else 0.0
            val isUp = diff >= 0.0

            val fmtPattern = if (spec.decimals == 1) "%,.1f" else "%,.2f"
            val valStr = spec.prefix + String.format(Locale.US, fmtPattern, lastVal)
            val sign = if (isUp) "+" else ""
            val arrow = if (isUp) "▲" else "▼"
            val diffStr = "$arrow $sign${String.format(Locale.US, fmtPattern, diff)} ($sign${String.format(Locale.US, "%.2f", pct)}%)"

            EconomyIndicator(
                nameKo = spec.nameKo,
                nameEn = spec.nameEn,
                valueStr = valStr,
                diffStr = diffStr,
                isUp = isUp,
                history = recent
            )
        } catch (_: Exception) {
            null
        }
    }
}
