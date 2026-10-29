package ai.littlepress.a2ui.integration

import ai.littlepress.a2ui.compat.java.text.NumberFormat
import ai.littlepress.a2ui.compat.java.util.Locales
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * formatNumber, formatCurrency and plural `#` format with the locale's numbering system. Every
 * platform must give what java.text.NumberFormat gives on Android ([ANDROID_NUMBER_RESULTS]); on
 * an Android device the test checks Android itself against them.
 */
class NumberingTest {
    @Test
    fun formatsDigitsLikeAndroid() {
        val results = NUMBERING_TAGS.map { tag ->
            val result = androidNumberReference(tag, 1234.5)
                ?: NumberFormat.getNumberInstance(Locales.forLanguageTag(tag)).format(1234.5)
            "$tag | $result".also { println("A2UI-NUMBER ${platformName()} | $it") }
        }
        val expected = ANDROID_NUMBER_RESULTS.map { android ->
            val tag = android.substringBefore(" | ")
            knownNumberDifferences()[tag]?.let { "$tag | $it" } ?: android
        }
        assertEquals(expected, results)
    }

    /** formatDate in other locales: the calendar (always Gregorian on Android) and the digits. */
    @Test
    fun formatsDatesLikeAndroid() {
        if (!runsAndroidFormatting) return
        val results = DATE_TAGS.map { tag ->
            val result = androidDateReference(tag, T, DATE_PATTERN)
                ?: Formatting(Locales.forLanguageTag(tag)).date(T, DATE_PATTERN).toString()
            "$tag | $result".also { println("A2UI-DATE ${platformName()} | $it") }
        }
        val expected = ANDROID_DATE_RESULTS.map { android ->
            val tag = android.substringBefore(" | ")
            knownDateDifferences()[tag]?.let { "$tag | $it" } ?: android
        }
        if (ANDROID_DATE_RESULTS.isNotEmpty()) assertEquals(expected, results)
    }
}

/**
 * False in Android local unit tests, where the official artifact formats dates with the JDK rather
 * than Android (the JDK uses the Buddhist calendar for th-TH, for example).
 */
expect val runsAndroidFormatting: Boolean

/** java.text.SimpleDateFormat in UTC on an Android device, null elsewhere. */
expect fun androidDateReference(languageTag: String, epochMillis: Long, pattern: String): String?

/** Language tag -> this platform's result, where it is known to differ from Android. */
expect fun knownDateDifferences(): Map<String, String>

internal const val DATE_PATTERN = "yyyy-MM-dd G EEEE d MMMM yyyy HH:mm a"

internal val DATE_TAGS: List<String> = listOf(
    "en-US", "de-DE", "th-TH", "fa-IR", "ar-EG", "ar-MA", "ja-JP", "zh-TW", "he-IL", "hi-IN", "mr-IN",
    "bn-BD", "my-MM", "ps-AF", "ur-PK", "ko-KR", "ru-RU", "th-TH-u-ca-buddhist", "ja-JP-u-ca-japanese",
)

/** java.text.NumberFormat.getNumberInstance on an Android device, null elsewhere. */
expect fun androidNumberReference(languageTag: String, number: Double): String?

/** Language tag -> this platform's result, where it is known to differ from Android. */
expect fun knownNumberDifferences(): Map<String, String>

internal val NUMBERING_TAGS: List<String> = listOf(
    "en", "en-US", "de-DE", "fr-FR", "ja-JP", "zh-CN", "ru-RU", "hi-IN", "th-TH",
    "ar", "ar-EG", "ar-SA", "ar-AE", "ar-MA", "ar-DZ", "ar-XB",
    "fa-IR", "fa-AF", "ps-AF", "ur-PK", "ur-IN", "ckb-IQ", "sd-PK", "sd-IN", "ks-IN",
    "bn-BD", "bn-IN", "as-IN", "mr-IN", "ne-NP", "my-MM", "dz-BT", "sat-IN", "mni-IN", "mni-Mtei-IN",
    "pa-IN", "pa-PK", "pa-Arab-PK", "pa-Guru-PK", "uz-AF", "uz-Arab-AF", "uz-Latn-AF", "az-IR",
    "ff-Adlm-GN", "ff-Latn-GN",
    "ar-EG-u-nu-latn", "en-US-u-nu-arab", "th-TH-u-nu-thai", "ar-EG-u-ca-islamic-nu-latn", "hi-IN-u-nu-deva",
)
