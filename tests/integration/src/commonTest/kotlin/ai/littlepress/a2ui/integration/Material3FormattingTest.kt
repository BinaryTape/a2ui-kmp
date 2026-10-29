package ai.littlepress.a2ui.integration

import ai.littlepress.a2ui.compat.java.text.DateFormat
import ai.littlepress.a2ui.compat.java.util.Calendar
import ai.littlepress.a2ui.compat.java.util.Date
import ai.littlepress.a2ui.compat.java.util.Locales
import ai.littlepress.a2ui.compat.java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * material3-a2ui's DateTimeInput shows dates with java.text.DateFormat (MEDIUM date, SHORT time,
 * in UTC) and computes with java.util.Calendar, which compat provides on desktop and iOS. On Android
 * compat is the platform's own classes, so the device run checks the recorded results; every other
 * platform must match them ([ANDROID_DATE_TIME_RESULTS], [ANDROID_CALENDAR_RESULTS]) except for the
 * differences it lists ([knownDateTimeDifferences], [knownCalendarDifferences]).
 */
class Material3FormattingTest {
    @Test
    fun formatsDatesAndTimesLikeAndroid() {
        val results = DATE_LOCALES.flatMap { tag ->
            val locale = Locales.forLanguageTag(tag)
            listOf(
                "date" to DateFormat.getDateInstance(DateFormat.MEDIUM, locale),
                "time" to DateFormat.getTimeInstance(DateFormat.SHORT, locale),
            ).map { (kind, format) ->
                format.timeZone = TimeZone.getTimeZone("UTC")
                "$tag | $kind | ${format.format(Date(INSTANT))}".also { println("A2UI-DATETIME ${platformName()} | $it") }
            }
        }
        assertEquals(ANDROID_DATE_TIME_RESULTS.size, results.size)
        val expected = ANDROID_DATE_TIME_RESULTS.map { line ->
            val key = line.substringBeforeLast(" | ")
            knownDateTimeDifferences()[key]?.let { "$key | $it" } ?: line
        }
        assertEquals(emptyList(), results.zip(expected).filter { (ours, android) -> ours != android }, "(ours, expected)")
    }

    @Test
    fun calendarLikeAndroid() {
        val results = CALENDAR_CASES.map { (name, millis, change) ->
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = millis
            change(calendar)
            val fields = listOf(Calendar.YEAR, Calendar.MONTH, Calendar.DAY_OF_MONTH, Calendar.HOUR_OF_DAY,
                Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND).joinToString(" ") { calendar.get(it).toString() }
            "$name | ${calendar.timeInMillis} | $fields".also { println("A2UI-CALENDAR ${platformName()} | $it") }
        }
        val expected = ANDROID_CALENDAR_RESULTS.map { line ->
            val name = line.substringBefore(" | ")
            knownCalendarDifferences()[name]?.let { "$name | $it" } ?: line
        }
        assertEquals(expected, results)
    }
}

/** Results that differ from Android's on this platform, by "tag | kind" -> result. */
expect fun knownDateTimeDifferences(): Map<String, String>

/** Results that differ from Android's on this platform, by case name -> "millis | fields". */
expect fun knownCalendarDifferences(): Map<String, String>

/** 2026-10-29T13:05:07.123Z, a Thursday afternoon. */
private const val INSTANT = 1793279107123L

private val DATE_LOCALES = listOf(
    "en-US", "en-GB", "de-DE", "fr-FR", "es-ES", "es-MX", "pt-BR", "it-IT", "nl-NL", "ru-RU", "pl-PL",
    "tr-TR", "ja-JP", "ko-KR", "zh-CN", "zh-TW", "ar-EG", "he-IL", "hi-IN", "fa-IR", "th-TH", "vi-VN",
    "id-ID", "sv-SE", "uk-UA",
    // Extensions and private use: Android keeps the language's patterns (digits follow -u-nu-).
    "en-US-u-nu-arab", "th-TH-u-ca-buddhist", "zh-x-Hant", "sr-x-Latn",
)

private val CALENDAR_CASES: List<Triple<String, Long, (Calendar) -> Unit>> = listOf(
    Triple("fields", INSTANT, {}),
    Triple("hour 25 carries into the next day", INSTANT, { it.set(Calendar.HOUR_OF_DAY, 25) }),
    Triple("time of day set, as TimeInputDialog does", INSTANT, {
        it.set(Calendar.HOUR_OF_DAY, 7)
        it.set(Calendar.MINUTE, 45)
        it.set(Calendar.SECOND, 0)
    }),
    Triple("clear, then a time of day", INSTANT, {
        it.clear()
        it.set(Calendar.HOUR_OF_DAY, 9)
        it.set(Calendar.MINUTE, 30)
    }),
    Triple("minute -1 borrows from the hour", INSTANT, { it.set(Calendar.MINUTE, -1) }),
    Triple("29 February 2024 to 2025", 1709208000000L, { it.set(Calendar.YEAR, 2025) }),
    Triple("31 January to February", 1738324800000L, { it.set(Calendar.MONTH, 1) }),
    Triple("before 1970", -1L, {}),
    Triple("year 1", -62135596800000L, {}),
    Triple("2 August 1500, Julian calendar", -14812502400000L, {}),
    Triple("millisecond 1500 carries", INSTANT, { it.set(Calendar.MILLISECOND, 1500) }),
)
