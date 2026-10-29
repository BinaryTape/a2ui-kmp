package ai.littlepress.a2ui.compat.android.content.res

/**
 * Chooses the resource locale Android uses for a requested locale, the way it chooses between
 * values-<locale> directories: a port of the locale parts of ResTable_config::match and
 * isLocaleBetterThan (libs/androidfw/ResourceTypes.cpp) and of LocaleData.cpp, from AOSP
 * platform/frameworks/base at the commit named in LocaleDataTables.kt (Apache License 2.0).
 *
 * Locales are BCP 47 tags with a language, an optional script and an optional region ("zh-TW",
 * "sr-Latn", "pt"); "" stands for the default resources (values/). Variants and extensions are
 * ignored, as no upstream resource uses them.
 */
public object ResourceLocales {
    /**
     * The index in [available] of the locale whose resource Android uses for [requested], or -1 if
     * none matches. For a string missing in some locales, pass only the locales that have it: Android
     * chooses per resource entry.
     */
    public fun bestMatch(available: List<String>, requested: String): Int {
        val request = Config.parse(requested, isRequest = true)
        var best = -1
        var bestConfig: Config? = null
        available.forEachIndexed { index, tag ->
            val config = Config.parse(tag, isRequest = false)
            val current = bestConfig
            if (config.matches(request) && (current == null || config.isBetterThan(current, request))) {
                best = index
                bestConfig = config
            }
        }
        return best
    }
}

/** The locale part of a ResTable_config, with language and region packed as there. */
private class Config(val language: Int, val region: Int, val script: String, val scriptWasComputed: Boolean) {
    val hasLocale: Boolean
        get() = language != 0 || region != 0

    /** ResTable_config::match, locale part: is this resource locale usable for [request]? */
    fun matches(request: Config): Boolean {
        if (!hasLocale) return true
        if (!langsAreEquivalent(language, request.language)) return false
        // If both scripts are known they must be equal; otherwise the regions must be (when given).
        val script = if (script.isEmpty() && !scriptWasComputed) computeScript(language, region) else script
        return if (request.script.isEmpty() || script.isEmpty()) {
            region == 0 || region == request.region
        } else {
            script == request.script
        }
    }

    /** ResTable_config::isBetterThan for configs that differ only in locale. */
    fun isBetterThan(o: Config, request: Config): Boolean =
        request.hasLocale && (hasLocale || o.hasLocale) && isLocaleBetterThan(o, request)

    private fun isLocaleBetterThan(o: Config, request: Config): Boolean {
        if (!hasLocale && !o.hasLocale) return false
        if (!langsAreEquivalent(language, o.language)) {
            // One has no language. The one with a language is better, except that resources without a
            // locale are where US English lives in most apps, so they are better for US English and
            // regions close to it than a descendant of International English (en-001).
            if (request.language == ENGLISH) {
                if (request.region == UNITED_STATES) {
                    return if (language != 0) region == 0 || region == UNITED_STATES
                    else !(o.region == 0 || o.region == UNITED_STATES)
                } else if (isCloseToUsEnglish(request.region)) {
                    return if (language != 0) isCloseToUsEnglish(region) else !isCloseToUsEnglish(o.region)
                }
            }
            return language != 0
        }
        val regions = compareRegions(region, o.region, request.language, request.script, request.region)
        if (regions != 0) return regions > 0
        // Equivalent but not identical languages (Tagalog and Filipino): identical is better.
        return language == request.language && o.language != request.language
    }

    companion object {
        fun parse(tag: String, isRequest: Boolean): Config {
            // Language, script and region come before the first singleton (extensions such as
            // "-u-nu-arab" and private use "-x-..."), which do not change the resource locale.
            val parts = tag.split('-', '_').filter { it.isNotEmpty() }.takeWhile { it.length > 1 }
            if (parts.isEmpty()) return Config(0, 0, "", scriptWasComputed = false)
            // Android looks up resources with the legacy codes of these languages (ResourcesImpl).
            val language = when (val code = parts[0].lowercase()) {
                "id" -> "in"
                "he" -> "iw"
                "yi" -> "ji"
                else -> code
            }
            var script = ""
            var region = ""
            for (part in parts.drop(1)) {
                when {
                    script.isEmpty() && region.isEmpty() && part.length == 4 && part.all { it.isLetter() } ->
                        script = part.lowercase().replaceFirstChar { it.uppercaseChar() }
                    region.isEmpty() && (part.length == 2 && part.all { it.isLetter() } ||
                        part.length == 3 && part.all { it.isDigit() }) -> region = part.uppercase()
                }
            }
            val packedLanguage = pack(language, 'a')
            val packedRegion = pack(region, '0')
            // A request without a script gets the likely one (ResTable_config::setBcp47Locale);
            // a resource's is computed when matching.
            return if (isRequest && script.isEmpty()) {
                Config(packedLanguage, packedRegion, computeScript(packedLanguage, packedRegion), scriptWasComputed = true)
            } else {
                Config(packedLanguage, packedRegion, script, scriptWasComputed = false)
            }
        }
    }
}

/** ResTable_config::packLanguageOrRegion: two letters as they are, three in 15 bits with the top bit set. */
private fun pack(code: String, base: Char): Int = when (code.length) {
    0 -> 0
    2 -> (code[0].code shl 8) or code[1].code
    3 -> {
        val first = (code[0] - base) and 0x7f
        val second = (code[1] - base) and 0x7f
        val third = (code[2] - base) and 0x7f
        ((0x80 or (third shl 2) or (second shr 3)) shl 8) or ((second shl 5) and 0xff) or first
    }
    else -> 0
}

private val ENGLISH = pack("en", 'a')
private val UNITED_STATES = pack("US", '0')
private val FILIPINO = pack("fil", 'a')
private val TAGALOG = pack("tl", 'a')

private fun langsAreEquivalent(a: Int, b: Int): Boolean =
    a == b || (a == TAGALOG && b == FILIPINO) || (a == FILIPINO && b == TAGALOG)

// LocaleData.cpp. A packed locale is language << 16 | region.

private fun packLocale(language: Int, region: Int): Int = (language shl 16) or region

private fun dropRegion(locale: Int): Int = locale and 0xFFFF0000.toInt()

private fun hasRegion(locale: Int): Boolean = locale and 0xFFFF != 0

private const val ROOT = 0

private fun hex(s: String, start: Int, digits: Int): Long = s.substring(start, start + digits).toULong(16).toLong()

private val likelyScripts: Map<Int, String> by lazy {
    (0 until LIKELY_SCRIPTS.length / 10).associate { i ->
        val index = hex(LIKELY_SCRIPTS, i * 10 + 8, 2).toInt()
        hex(LIKELY_SCRIPTS, i * 10, 8).toInt() to SCRIPT_CODES.substring(index * 4, index * 4 + 4)
    }
}

private val representativeLocales: Set<Long> by lazy {
    (0 until REPRESENTATIVE_LOCALES.length / 16).mapTo(HashSet()) { hex(REPRESENTATIVE_LOCALES, it * 16, 16) }
}

private val scriptParents: Map<String, Map<Int, Int>> by lazy {
    SCRIPT_PARENTS.mapValues { (_, entries) ->
        (0 until entries.length / 16).associate { hex(entries, it * 16, 8).toInt() to hex(entries, it * 16 + 8, 8).toInt() }
    }
}

private fun computeScript(language: Int, region: Int): String {
    if (language == 0) return ""
    return likelyScripts[packLocale(language, region)]
        ?: if (region != 0) likelyScripts[packLocale(language, 0)] ?: "" else ""
}

private fun findParent(locale: Int, script: String): Int {
    if (!hasRegion(locale)) return ROOT
    scriptParents[script]?.get(locale)?.let { return it }
    return dropRegion(locale)
}

/**
 * The ancestors of [locale] (itself first), stopping after the first one in [stopList]; and the
 * index in [stopList] of that one, or -1.
 */
private fun findAncestors(locale: Int, script: String, stopList: List<Int>): Pair<List<Int>, Int> {
    val ancestors = mutableListOf<Int>()
    var ancestor = locale
    do {
        ancestors += ancestor
        val stop = stopList.indexOf(ancestor)
        if (stop >= 0) return ancestors to stop
        ancestor = findParent(ancestor, script)
    } while (ancestor != ROOT)
    return ancestors to -1
}

private fun findDistance(supported: Int, script: String, requestAncestors: List<Int>): Int {
    val (ancestors, requestIndex) = findAncestors(supported, script, requestAncestors)
    return ancestors.size + requestIndex - 1
}

private fun isRepresentative(languageAndRegion: Int, script: String): Boolean {
    var packed = (languageAndRegion.toLong() and 0xFFFFFFFFL) shl 32
    script.forEachIndexed { i, c -> packed = packed or (c.code.toLong() shl (24 - 8 * i)) }
    return packed in representativeLocales
}

private val US_SPANISH = packLocale(pack("es", 'a'), pack("US", '0'))
private val MEXICAN_SPANISH = packLocale(pack("es", 'a'), pack("MX", '0'))
private val LATIN_AMERICAN_SPANISH = packLocale(pack("es", 'a'), pack("419", '0'))

/** localeDataCompareRegions: positive if [leftRegion] is the better match for the request. */
private fun compareRegions(leftRegion: Int, rightRegion: Int, language: Int, script: String, region: Int): Int {
    if (leftRegion == rightRegion) return 0
    var left = packLocale(language, leftRegion)
    var right = packLocale(language, rightRegion)
    val request = packLocale(language, region)
    // es-US and es-MX stand in for es-419 when compared with another region.
    val leftSpecial = left == US_SPANISH || left == MEXICAN_SPANISH
    val rightSpecial = right == US_SPANISH || right == MEXICAN_SPANISH
    if (leftSpecial && !rightSpecial && right != LATIN_AMERICAN_SPANISH) {
        left = LATIN_AMERICAN_SPANISH
    } else if (rightSpecial && !leftSpecial && left != LATIN_AMERICAN_SPANISH) {
        right = LATIN_AMERICAN_SPANISH
    }
    val (requestAncestors, seen) = findAncestors(request, script, listOf(left, right))
    if (seen == 0) return 1
    if (seen == 1) return -1
    val leftDistance = findDistance(left, script, requestAncestors)
    val rightDistance = findDistance(right, script, requestAncestors)
    if (leftDistance != rightDistance) return rightDistance - leftDistance
    val leftRepresentative = isRepresentative(left, script)
    val rightRepresentative = isRepresentative(right, script)
    if (leftRepresentative != rightRepresentative) return if (leftRepresentative) 1 else -1
    // The lower region code (as packed, so two letters before three digits) for stability.
    return ((right.toLong() and 0xFFFFFFFFL) - (left.toLong() and 0xFFFFFFFFL)).coerceIn(-1L, 1L).toInt()
}

private val ENGLISH_STOP_LIST = listOf(packLocale(ENGLISH, 0), packLocale(ENGLISH, pack("001", '0')))

/** localeDataIsCloseToUsEnglish: "en" comes before "en-001" among the region's English ancestors. */
private fun isCloseToUsEnglish(region: Int): Boolean =
    findAncestors(packLocale(ENGLISH, region), "Latn", ENGLISH_STOP_LIST).second == 0

/** Android's likely script for a language and optional region ("zh", "TW" -> "Hant"), or "". */
internal fun likelyScript(language: String, region: String): String =
    computeScript(pack(language, 'a'), pack(region, '0'))
