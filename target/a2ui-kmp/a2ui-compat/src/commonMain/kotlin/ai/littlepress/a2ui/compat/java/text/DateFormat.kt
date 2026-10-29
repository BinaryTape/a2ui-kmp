package ai.littlepress.a2ui.compat.java.text

import ai.littlepress.a2ui.compat.android.content.res.likelyScript
import ai.littlepress.a2ui.compat.java.util.Date
import ai.littlepress.a2ui.compat.java.util.Locale
import ai.littlepress.a2ui.compat.java.util.TimeZone

/**
 * The part of java.text.DateFormat upstream uses: a locale's date or time format in one of the
 * standard styles, in a time zone, formatted by [SimpleDateFormat]. The MEDIUM date and SHORT time
 * patterns, which upstream uses, are Android's (recorded in AndroidDatePatterns.kt); other styles,
 * and locales Android does not have, use the platform's pattern (the JDK's on desktop, Foundation's
 * on iOS). tests/integration compares the results with Android's.
 */
public class DateFormat private constructor(private val format: SimpleDateFormat) {
    public var timeZone: TimeZone
        get() = format.timeZone
        set(value) {
            format.timeZone = value
        }

    public fun format(date: Date): String = format.format(date)

    public companion object {
        public const val FULL: Int = 0
        public const val LONG: Int = 1
        public const val MEDIUM: Int = 2
        public const val SHORT: Int = 3

        public fun getDateInstance(style: Int, locale: Locale): DateFormat {
            val android = if (checkStyle(style) == MEDIUM) androidPatterns(locale.languageTag)?.first else null
            return DateFormat(SimpleDateFormat(android ?: datePattern(style, locale), locale))
        }

        public fun getTimeInstance(style: Int, locale: Locale): DateFormat {
            val android = if (checkStyle(style) == SHORT) androidPatterns(locale.languageTag)?.second else null
            return DateFormat(SimpleDateFormat(android ?: timePattern(style, locale), locale))
        }

        private fun checkStyle(style: Int): Int {
            require(style in FULL..SHORT) { "Illegal date style $style" }
            return style
        }
    }
}

private val androidPatterns: Map<String, Pair<String, String>> by lazy {
    ANDROID_DATE_PATTERNS.flatMap { it.lineSequence() }.filter { it.isNotEmpty() }.associate { line ->
        val (tag, date, time) = line.split('\t')
        tag to (date to time)
    }
}

/**
 * Android's MEDIUM date and SHORT time pattern for a language tag: as tagged, with the likely script
 * added (zh-TW is zh-Hant-TW), without the region, or for the language alone.
 */
private fun androidPatterns(tag: String): Pair<String, String>? {
    // Extensions and private use (from the first singleton on) do not select patterns here.
    val parts = tag.split('-').takeWhile { it.length > 1 }
    if (parts.isEmpty()) return null
    val language = parts[0]
    val script = parts.drop(1).firstOrNull { it.length == 4 } ?: likelyScript(language, "")
    val region = parts.drop(1).firstOrNull { it.length == 2 || it.length == 3 && it.all(Char::isDigit) }
    val withScript = if (region != null) {
        val regionScript = parts.drop(1).firstOrNull { it.length == 4 } ?: likelyScript(language, region)
        "$language-$regionScript-$region"
    } else null
    return sequenceOf(parts.joinToString("-"), withScript, "$language-$script", language)
        .firstNotNullOfOrNull { candidate -> candidate?.let { androidPatterns[it] } }
}

/** The platform's pattern for [locale]'s dates in [style] (one of the DateFormat styles). */
internal expect fun datePattern(style: Int, locale: Locale): String

/** The platform's pattern for [locale]'s times in [style] (one of the DateFormat styles). */
internal expect fun timePattern(style: Int, locale: Locale): String

/** The locale's BCP 47 tag, e.g. "zh-Hant-TW". */
internal expect val Locale.languageTag: String
