package ai.littlepress.a2ui.integration

import android.icu.util.ULocale
import java.text.NumberFormat
import kotlin.test.Test

/**
 * Prints the digits java.text.NumberFormat prints on Android for each locale (named by the numbering
 * system of its zero; upstream formats numbers with java.text), as the smallest table from which
 * looking up language-script-region, language-script, language-region, then language (default
 * "latn") gives Android's answer for every available locale and every language x region and
 * language x script x region combination. Used to record kmp/compat's iOS numbering table.
 */
class NumberingDump {
    @Test
    fun versions() {
        println("A2UI-VERSIONS ICU ${android.icu.util.VersionInfo.ICU_VERSION} Unicode " +
            "${android.icu.lang.UCharacter.getUnicodeVersion()} CLDR ${android.icu.util.LocaleData.getCLDRVersion()}")
    }

    /**
     * The AM/PM markers java.text.SimpleDateFormat uses for each available locale, where they differ
     * from those of the locale's language.
     */
    @Test
    fun amPm() {
        val markers = LinkedHashMap<String, String>()
        val byLanguage = HashMap<String, String>()
        val available = java.util.Locale.getAvailableLocales().filter { it.variant.isEmpty() && it.language.isNotEmpty() }
            .sortedBy { listOf(it.script, it.country).count { part -> part.isNotEmpty() } }
        for (locale in available) {
            val value = java.text.DateFormatSymbols.getInstance(locale).amPmStrings.joinToString("|")
            val tag = listOf(locale.language, locale.script, locale.country).filter { it.isNotEmpty() }.joinToString("-")
            val inherited = byLanguage[locale.language] ?: "AM|PM"
            if (locale.script.isEmpty() && locale.country.isEmpty()) byLanguage[locale.language] = value
            if (value != inherited) markers[tag] = value
        }
        markers.entries.chunked(40).forEachIndexed { i, chunk ->
            println("A2UI-AMPM $i ${chunk.joinToString(";") { "${it.key}=${it.value}" }}")
        }
        println("A2UI-AMPM-DONE ${markers.size}")
    }

    @Test
    fun dump() {
        val table = LinkedHashMap<String, String>()
        fun resolve(language: String, script: String, region: String): String =
            listOfNotNull(
                if (script.isNotEmpty() && region.isNotEmpty()) "$language-$script-$region" else null,
                if (script.isNotEmpty()) "$language-$script" else null,
                if (region.isNotEmpty()) "$language-$region" else null,
                language,
            ).firstNotNullOfOrNull { table[it] } ?: "latn"
        fun record(locale: ULocale) {
            val language = locale.language.ifEmpty { return }
            val zero = NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag(locale.toLanguageTag()))
                .format(0).first { it.isDigit() }
            val actual = ZERO_DIGITS[zero] ?: "u+%04x".format(zero.code)
            if (resolve(language, locale.script, locale.country) != actual) {
                table[listOf(language, locale.script, locale.country).filter { it.isNotEmpty() }.joinToString("-")] = actual
            }
        }
        val available = ULocale.getAvailableLocales().filter { it.variant.isEmpty() }
            .sortedBy { listOf(it.script, it.country).count { part -> part.isNotEmpty() } }
        available.forEach(::record)
        val scripts = available.groupBy({ it.language }, { it.script }).mapValues { (_, s) -> s.toSet() + "" }
        val regions = ULocale.getISOCountries().toList()
        for ((language, languageScripts) in scripts) {
            for (script in languageScripts.sortedBy { it.isNotEmpty() }) {
                for (region in regions) record(ULocale(language, script, region))
            }
        }
        // A locale whose script is not its language's default script does not inherit from the
        // language, so the lookup needs each language's default script.
        val defaultScripts = table.keys.map { it.substringBefore('-') }.distinct().sorted()
            .joinToString(",") { "$it=${ULocale.addLikelySubtags(ULocale(it)).script}" }
        println("A2UI-SCRIPTS $defaultScripts")
        table.entries.chunked(60).forEachIndexed { i, chunk ->
            println("A2UI-NUMBERING $i ${chunk.joinToString(",") { "${it.key}=${it.value}" }}")
        }
        println("A2UI-NUMBERING-DONE ${table.size}")
    }
}

private val ZERO_DIGITS = mapOf(
    '0' to "latn", '\u0660' to "arab", '\u06F0' to "arabext", '\u0966' to "deva", '\u09E6' to "beng",
    '\u0A66' to "guru", '\u0AE6' to "gujr", '\u0B66' to "orya", '\u0BE6' to "tamldec", '\u0C66' to "telu",
    '\u0CE6' to "knda", '\u0D66' to "mlym", '\u0E50' to "thai", '\u0ED0' to "laoo", '\u0F20' to "tibt",
    '\u1040' to "mymr", '\u17E0' to "khmr", '\u1810' to "mong", '\u1C50' to "olck", '\u07C0' to "nkoo",
    '\uABF0' to "mtei", '\uA9D0' to "java", '\u1946' to "limb", '\u19D0' to "talu",
)
