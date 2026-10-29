package ai.littlepress.a2ui.integration

import ai.littlepress.a2ui.compat.android.content.res.ResourceLocales
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Which values-<locale> directory serves a locale. On desktop and iOS, material3-a2ui's strings are
 * chosen by ResourceLocales, a port of Android's rules; on an Android device this test asks Android
 * itself, with one resource in each directory material3-a2ui has (src/androidMain/res, generated).
 * Every platform must choose what Android chose ([ANDROID_RESOURCE_LOCALE_RESULTS]).
 */
class ResourceLocaleTest {
    @Test
    fun choosesLikeAndroid() {
        val results = REQUESTED_LOCALES.map { requested ->
            val chosen = resourceLocaleReference(requested)?.invoke()
                ?: RESOURCE_LOCALES.getOrNull(ResourceLocales.bestMatch(RESOURCE_LOCALES, requested))
                    ?.ifEmpty { "(default)" } ?: "(none)"
            "$requested | $chosen".also { println("A2UI-RESOURCE-LOCALE ${platformName()} | $it") }
        }
        val differences = results.zip(ANDROID_RESOURCE_LOCALE_RESULTS).filter { (ours, android) -> ours != android }
        assertEquals(ANDROID_RESOURCE_LOCALE_RESULTS.size, results.size)
        assertEquals(emptyList(), differences, "choices unlike Android's (ours, Android)")
    }
}

/** On an Android device: the resource locale Android chooses for [languageTag]. Elsewhere null. */
expect fun resourceLocaleReference(languageTag: String): (() -> String)?

/** Every resource locale itself, then locales that only a fallback rule serves. */
private val REQUESTED_LOCALES = RESOURCE_LOCALES.filter { it.isNotEmpty() } + listOf(
    // English: US-like regions take values/, others the closest en-* directory.
    "en", "en-US", "en-PR", "en-VI", "en-001", "en-150", "en-NZ", "en-IE", "en-ZA", "en-SG", "en-HK",
    "en-DE", "en-JM", "en-Latn-GB", "en-Shaw",
    // Spanish and Portuguese region parents.
    "es-ES", "es-MX", "es-419", "es-AR", "es-CO", "es-PR", "pt", "pt-AO", "pt-MZ", "pt-MO", "pt-CV",
    // Chinese scripts and Hong Kong / Macau.
    "zh", "zh-Hans", "zh-Hant", "zh-SG", "zh-MY", "zh-MO", "zh-Hant-MO", "zh-Hans-HK", "zh-Hant-CN",
    // Serbian, Bosnian, Uzbek, Punjabi, Azerbaijani, Mongolian scripts.
    "sr", "sr-RS", "sr-Latn-RS", "sr-Cyrl", "sr-ME", "sr-Latn-ME", "bs-Cyrl", "uz-Cyrl", "uz-Arab",
    "pa-Arab", "pa-PK", "az-Cyrl", "mn-Mong",
    // Legacy and equivalent codes.
    "id", "id-ID", "he", "he-IL", "yi", "fil", "fil-PH", "tl-PH", "nb-NO", "no", "nn",
    // Regions without a directory of their own.
    "fr-CH", "fr-BE", "fr-CA", "de-AT", "de-CH", "ar-EG", "ar-MA", "hi-Latn", "ms-SG", "ja-JP",
    "ko-KR", "ru-UA", "sw-KE", "ta-LK", "bn-IN", "ur-IN", "fa-AF",
    // No directory at all.
    "xx", "und", "eo", "ga-IE", "cy-GB",
    // Extensions and private use do not change the resource locale.
    "en-u-nu-arab", "en-GB-u-ca-buddhist", "zh-x-Hant", "sr-x-Latn", "de-DE-u-co-phonebk",
    "zh-TW-u-nu-hanidec", "pt-u-rg-ptzzzz", "en-x-US", "ar-EG-u-nu-latn",
)
