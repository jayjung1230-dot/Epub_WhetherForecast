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

    private val MOTIVATION_KEYWORDS = listOf(
        "work", "effort", "hard", "lazy", "laziness", "idle", "action", "start", "begin",
        "possible", "impossible", "dream", "goal", "success", "fail", "courage", "persist",
        "discipline", "habit", "today", "tomorrow", "time", "strive", "achieve", "will"
    )

    private val FALLBACK_QUOTES = listOf(
        QuoteItem(
            textKo = "게으름은 쇠에 스며드는 녹과 같다. 사용하지 않는 열쇠는 녹슬지만, 끊임없이 쓰는 열쇠는 언제나 빛난다.",
            textEn = "Sloth makes all things difficult, but industry all easy.",
            author = "벤자민 프랭클린 (Benjamin Franklin)",
            tag = "게으름 경계"
        ),
        QuoteItem(
            textKo = "완벽한 준비를 기다리다 보면, 시작할 수 있는 날은 결코 오지 않는다.",
            textEn = "If we wait for the moment when everything is ready, we shall never begin.",
            author = "이반 투르게네프 (Ivan Turgenev)",
            tag = "실행 · 시작"
        ),
        QuoteItem(
            textKo = "할 수 있다고 믿든 할 수 없다고 믿든, 당신의 믿음이 옳다.",
            textEn = "Whether you think you can, or you think you can't—you're right.",
            author = "헨리 포드 (Henry Ford)",
            tag = "가능성"
        ),
        QuoteItem(
            textKo = "오늘 걸을 수 없다면, 내일은 뛰어야 한다.",
            textEn = "If you don't walk today, you'll have to run tomorrow.",
            author = "서양 격언",
            tag = "노력 · 절실함"
        ),
        QuoteItem(
            textKo = "천재는 1%의 영감과 99%의 땀으로 이루어진다.",
            textEn = "Genius is one percent inspiration and ninety-nine percent perspiration.",
            author = "토마스 에디슨 (Thomas Edison)",
            tag = "노력 · 끈기"
        ),
        QuoteItem(
            textKo = "천 리 길도 한 걸음부터 시작된다. 멈추지 않는 한 얼마나 천천히 가는지는 중요하지 않다.",
            textEn = "It does not matter how slowly you go as long as you do not stop.",
            author = "공자 (Confucius)",
            tag = "지속성"
        ),
        QuoteItem(
            textKo = "아무것도 하지 않으면 아무 일도 일어나지 않는다. 작은 실행이 거대한 가능성을 연다.",
            textEn = "Nothing will work unless you do.",
            author = "마야 안젤루 (Maya Angelou)",
            tag = "동기부여"
        ),
        QuoteItem(
            textKo = "승리는 가장 끈기 있게 버틴 사람에게 돌아간다.",
            textEn = "Victory belongs to the most persevering.",
            author = "나폴레옹 보나파르트 (Napoleon Bonaparte)",
            tag = "끈기 · 노력"
        )
    )

    fun fetchQuotes(language: String = "ko"): Pair<QuoteItem, QuoteItem> {
        val isEn = language == "en"
        return try {
            val raw = WeatherRepository.httpGet("https://dummyjson.com/quotes?limit=200")
            val arr = JSONObject(raw).getJSONArray("quotes")
            val candidates = ArrayList<Pair<String, String>>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val q = obj.getString("quote").trim()
                val a = obj.getString("author").trim()
                val lower = q.lowercase(Locale.US)
                if (q.length in 25..150 && MOTIVATION_KEYWORDS.any { lower.contains(it) }) {
                    candidates.add(q to a)
                }
            }
            if (candidates.size < 2) return pickFallbackPair()
            val idx1 = Random.nextInt(candidates.size)
            var idx2 = Random.nextInt(candidates.size)
            if (idx2 == idx1) idx2 = (idx1 + 1) % candidates.size

            val (en1, au1) = candidates[idx1]
            val (en2, au2) = candidates[idx2]
            if (isEn) {
                QuoteItem(en1, en1, au1, "Effort · Potential") to
                    QuoteItem(en2, en2, au2, "Motivation")
            } else {
                val ko1 = translateEnToKo(en1) ?: return pickFallbackPair()
                val ko2 = translateEnToKo(en2) ?: FALLBACK_QUOTES.first().textKo
                QuoteItem(ko1, en1, au1, "노력 · 가능성") to QuoteItem(ko2, en2, au2, "동기부여")
            }
        } catch (_: Exception) {
            pickFallbackPair()
        }
    }

    private fun pickFallbackPair(): Pair<QuoteItem, QuoteItem> {
        val i1 = Random.nextInt(FALLBACK_QUOTES.size)
        val i2 = (i1 + 1 + Random.nextInt(FALLBACK_QUOTES.size - 1)) % FALLBACK_QUOTES.size
        return FALLBACK_QUOTES[i1] to FALLBACK_QUOTES[i2]
    }

    private fun translateEnToKo(text: String): String? {
        return try {
            val encoded = URLEncoder.encode(text, "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=ko&dt=t&q=$encoded"
            val body = WeatherRepository.httpGet(url)
            val root = JSONArray(body)
            val segs = root.getJSONArray(0)
            val sb = StringBuilder()
            for (i in 0 until segs.length()) {
                sb.append(segs.getJSONArray(i).getString(0))
            }
            val result = sb.toString().trim()
            if (result.isNotEmpty()) result else null
        } catch (_: Exception) {
            null
        }
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
