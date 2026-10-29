package ai.littlepress.a2ui.integration

import ai.littlepress.a2ui.compat.java.text.DateFormat
import ai.littlepress.a2ui.compat.java.util.Date
import ai.littlepress.a2ui.compat.java.util.TimeZone
import android.icu.util.ULocale
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Android's MEDIUM date and SHORT time patterns for every available locale, which compat's
 * DateFormat uses on desktop and iOS (AndroidDatePatterns.kt). Prints them for
 * scripts/record-android-date-patterns.py, and checks that compat formats every locale as Android.
 */
class AndroidDatePatternsTest {
    @Test
    fun compatFormatsEveryLocaleAsAndroid() {
        val differences = mutableListOf<String>()
        for (locale in Locale.getAvailableLocales().sortedBy { it.toLanguageTag() }) {
            val tag = locale.toLanguageTag()
            if (tag == "und") continue
            val date = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, locale) as SimpleDateFormat
            val time = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, locale) as SimpleDateFormat
            println("A2UI-DATE-PATTERN\t$tag\t${ULocale.addLikelySubtags(ULocale.forLocale(locale)).toLanguageTag()}\t${date.toPattern()}\t${time.toPattern()}")
            val compatLocale = ai.littlepress.a2ui.compat.java.util.Locales.forLanguageTag(tag)
            for ((android, ours) in listOf(
                date to DateFormat.getDateInstance(DateFormat.MEDIUM, compatLocale),
                time to DateFormat.getTimeInstance(DateFormat.SHORT, compatLocale),
            )) {
                android.timeZone = java.util.TimeZone.getTimeZone("UTC")
                ours.timeZone = TimeZone.getTimeZone("UTC")
                val expected = android.format(java.util.Date(INSTANT))
                val actual = ours.format(Date(INSTANT))
                if (expected != actual) differences += "$tag: $actual, Android $expected"
            }
        }
        assertEquals(emptyList<String>(), differences)
    }

    private companion object {
        const val INSTANT = 1793279107123L
    }
}
