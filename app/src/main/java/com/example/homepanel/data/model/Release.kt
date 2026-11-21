package com.example.homepanel.data.model

// Озвучка, найденная в названии раздачи. Подписи — в strings.xml (переводятся).
enum class Voice { DUB, MULTI, DUAL, AUTHOR, RUSSIAN, ORIGINAL, SUBS }

// Раздача с трекера (через Prowlarr). Качество и озвучку достаём из названия.
data class Release(
    val guid: String,
    val indexerId: Int,
    val indexer: String,     // «RuTracker.org»
    val title: String,       // как на трекере
    val sizeBytes: Long,
    val seeders: Int,
    val leechers: Int,
) {
    val resolution: String = ReleaseParser.resolution(title)
    val source: String = ReleaseParser.source(title)
    val extras: List<String> = ReleaseParser.extras(title)        // HDR, HEVC, AV1
    val voices: List<Voice> = ReleaseParser.voices(title, indexer)
    val seasons: List<Int> = ReleaseParser.seasons(title)

    val hasRussian: Boolean get() = voices.any { it != Voice.ORIGINAL && it != Voice.SUBS }
    val hasOriginal: Boolean get() = Voice.ORIGINAL in voices
}

// Разбор названий раздач русских и зарубежных трекеров (как в десктопном homepanel)
object ReleaseParser {
    private val res = listOf(
        Regex("2160p|\\b4K\\b|\\bUHD\\b", RegexOption.IGNORE_CASE) to "2160p",
        Regex("1080[pi]", RegexOption.IGNORE_CASE) to "1080p",
        Regex("720p", RegexOption.IGNORE_CASE) to "720p",
        Regex("\\b(576|480|396|360)p\\b|DVDRip|SATRip", RegexOption.IGNORE_CASE) to "SD",
    )
    private val sources = listOf(
        "Remux" to "Remux",
        "BR-?DISK|Blu-?ray\\s*Disc|\\bBDMV\\b" to "Blu-ray",
        "BDRip|BRRip|Blu-?Ray|\\bBD\\b" to "BDRip",
        "WEB-?DL" to "WEB-DL",
        "WEB-?Rip|\\bWEB\\b" to "WEBRip",
        "HDTV" to "HDTV",
        "DVDRip|DVD5|DVD9" to "DVD",
        "\\bCAM(Rip)?\\b|\\bTS\\b|TeleSync|\\bTC\\b|Telecine|HDCAM" to "CAM",
    ).map { (p, n) -> Regex(p, RegexOption.IGNORE_CASE) to n }
    private val extras = listOf(
        Regex("HDR|Dolby\\s*Vision|\\bDV\\b", RegexOption.IGNORE_CASE) to "HDR",
        Regex("HEVC|[hx]\\.?265", RegexOption.IGNORE_CASE) to "HEVC",
        Regex("\\bAV1\\b", RegexOption.IGNORE_CASE) to "AV1",
    )
    private val voiceRules = listOf(
        Regex("\\bDub\\b|Дубляж|\\bD\\b(?=\\s*[,+\\]|)])") to Voice.DUB,
        Regex("\\bMVO\\b|многоголос") to Voice.MULTI,
        Regex("\\bDVO\\b|двухголос") to Voice.DUAL,
        Regex("\\bAVO\\b|авторск") to Voice.AUTHOR,
        Regex("\\bRUS\\b|\\bRus\\b|русск") to Voice.RUSSIAN,
    )
    private val original = Regex("Original|\\bEng\\b|\\bENG\\b|\\bEN\\b|\\braw\\b|\\bRaw\\b|оригинал", RegexOption.IGNORE_CASE)
    private val subs = Regex("\\bSub(s)?\\b|субтитр", RegexOption.IGNORE_CASE)
    // Трекеры, где почти всё без русской озвучки
    private val foreignIndexers = setOf("The Pirate Bay", "Nyaa.si", "Anime Tosho")
    private val seasonRules = listOf(
        Regex("\\bS(\\d{1,2})(?:(?:-|\\s*[-–]\\s*S)(\\d{1,2}))?(?:E\\d+)?\\b", RegexOption.IGNORE_CASE),
        Regex("(?:Сезон|сезон|Season)[:\\s]*(\\d{1,2})(?:\\s*[-–]\\s*(\\d{1,2}))?", RegexOption.IGNORE_CASE),
        Regex("\\bTV-(\\d)\\b"),
    )

    fun resolution(title: String) = res.firstOrNull { it.first.containsMatchIn(title) }?.second.orEmpty()

    fun source(title: String) = sources.firstOrNull { it.first.containsMatchIn(title) }?.second.orEmpty()

    fun extras(title: String) = extras.filter { it.first.containsMatchIn(title) }.map { it.second }

    fun voices(title: String, indexer: String): List<Voice> {
        val out = voiceRules.filter { it.first.containsMatchIn(title) }.map { it.second }.toMutableList()
        if (Voice.RUSSIAN in out && out.size > 1) out.remove(Voice.RUSSIAN)  // «Dub RUS» — хватит «дубляж»
        if (original.containsMatchIn(title) || (out.isEmpty() && indexer in foreignIndexers)) out += Voice.ORIGINAL
        if (out.isEmpty() && subs.containsMatchIn(title)) out += Voice.SUBS
        return out
    }

    // «S01-03» -> [1, 2, 3]; «Сезон 2» -> [2]
    fun seasons(title: String): List<Int> {
        for (rx in seasonRules) {
            val m = rx.find(title) ?: continue
            val a = m.groupValues[1].toInt()
            val b = m.groupValues.getOrNull(2)?.toIntOrNull() ?: a
            return if (b in a..a + 30) (a..b).toList() else listOf(a)
        }
        return emptyList()
    }
}
