package ai.littlepress.a2ui.integration

import ai.littlepress.a2ui.compat.android.icu.text.MessageFormat
import ai.littlepress.a2ui.compat.java.util.Locales
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * compose-ui formats pluralize messages with android.icu.text.MessageFormat on Android and with our
 * MessageFormat (CLDR rules) elsewhere. Every platform must give the results recorded from Android's
 * ICU ([ANDROID_PLURAL_RESULTS]); on an Android device the test checks ICU itself against them.
 */
class PluralTest {
    @Test
    fun formatsLikeIcu() {
        val results = PLURAL_CASES.map { (pattern, tag, count) ->
            val result = format(
                icuReference(pattern, tag, count)
                    ?: { MessageFormat(pattern, Locales.forLanguageTag(tag)).format(mapOf("count" to count)) }
            )
            "$tag | $count | $pattern | $result".also { println("A2UI-PLURAL ${platformName()} | $it") }
        }
        val expected = ANDROID_PLURAL_RESULTS.map { android ->
            val case = android.substringBeforeLast(" | ")
            knownPluralDifferences()[case]?.let { "$case | $it" } ?: android
        }
        val differences = results.zip(expected).filter { (ours, want) -> ours != want }
        assertEquals(ANDROID_PLURAL_RESULTS.size, results.size)
        assertEquals(emptyList(), differences, "results unlike Android's ICU (ours, Android)")
    }

    private fun format(block: () -> String): String =
        runCatching(block).fold({ it }, { "!" + it::class.simpleName })
}

/** "tag | count | pattern" -> this platform's result, where it is known to differ from Android. */
expect fun knownPluralDifferences(): Map<String, String>

/** android.icu.text.MessageFormat on an Android device, null elsewhere. */
expect fun icuReference(pattern: String, languageTag: String, count: Double): (() -> String)?

private const val ALL = "{count, plural, zero {Z #} one {O #} two {T #} few {F #} many {M #} other {X #}}"

private val LOCALES = listOf(
    "en", "de", "fr", "it", "es", "pt-BR", "pt-PT", "ru", "uk", "pl", "cs", "sl", "lt", "lv",
    "ro", "ar", "he", "ga", "cy", "ja",
)

private val COUNTS = listOf(
    0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 11.0, 12.0, 21.0, 22.0, 25.0, 100.0, 101.0, 102.0, 111.0,
    1_000_000.0, 0.5, 1.5, 2.5, 21.3, 1.0005, 1.0015,
)

internal val PLURAL_CASES: List<Triple<String, String, Double>> =
    LOCALES.flatMap { tag -> COUNTS.map { Triple(ALL, tag, it) } } +
        listOf(
            "{count, plural, =0 {none} one {one #} other {# items}}",
            "{count, plural, one {it''s #} other {'{#}' and '#' it's}}",
            "{count, plural, other {{count} vs #}}",
            "{count, plural, offset:1 =1 {just you} one {you and # other} other {you and # others}}",
            "it's {count}",
            "{other}",
            "{count, plural, one {x}}",
            "{count, plural, other {x}",
            "{count",
        ).flatMap { pattern ->
            listOf("en", "de", "ar").flatMap { tag -> listOf(0.0, 1.0, 2.0, 1234.5).map { Triple(pattern, tag, it) } }
        } +
        // Numbers beyond Long.
        listOf("en", "fr", "ru", "ar").flatMap { tag ->
            listOf(1e19, 1.5e20, 123456789012345678.0, 1e21).map { Triple(ALL, tag, it) }
        } +
        // Text that pluralize passes through: =N by value, number styles, ordinals.
        listOf(
            "{count, plural, =1.0 {EXACT} one {ONE} other {OTHER}}",
            "{count, plural, =2 {TWO} other {{count, number, integer} items}}",
            "{count, plural, other {{count, number, percent}}}",
            "{count, plural, other {{count, number}}}",
            "{count, selectordinal, one {#st} two {#nd} few {#rd} other {#th}}",
        ).flatMap { pattern ->
            listOf("en", "de", "ar", "fr").flatMap { tag ->
                listOf(0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 4.0, 11.0, 21.0, 22.0, 23.0, 1234.5).map { Triple(pattern, tag, it) }
            }
        } +
        listOf("en-US", "de-DE", "ar-EG", "ja-JP").flatMap { tag ->
            listOf(1.0, 1.5, 1234.5).map { Triple("{count, plural, other {{count, number, currency}}}", tag, it) }
        }
