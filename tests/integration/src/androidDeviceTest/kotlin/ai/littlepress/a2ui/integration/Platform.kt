package ai.littlepress.a2ui.integration

import android.os.Build

actual fun platformName(): String = "Android API " + Build.VERSION.SDK_INT

actual fun knownPatternDifferences(): Map<String, String> = emptyMap()

actual val canParseJson: Boolean = true

actual fun icuReference(pattern: String, languageTag: String, count: Double): (() -> String)? = {
    android.icu.text.MessageFormat(pattern, java.util.Locale.forLanguageTag(languageTag)).format(mapOf("count" to count))
}

actual fun androidIdentifierReference(): Pair<(Char) -> Boolean, (Char) -> Boolean>? =
    Pair({ c: Char -> java.lang.Character.isUnicodeIdentifierStart(c) }, { c: Char -> java.lang.Character.isUnicodeIdentifierPart(c) })

actual fun knownPluralDifferences(): Map<String, String> = emptyMap()

actual fun androidNumberReference(languageTag: String, number: Double): String? =
    java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag(languageTag)).format(number)

actual fun knownNumberDifferences(): Map<String, String> = emptyMap()

actual fun androidDateReference(languageTag: String, epochMillis: Long, pattern: String): String? =
    java.text.SimpleDateFormat(pattern, java.util.Locale.forLanguageTag(languageTag))
        .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
        .format(java.util.Date(epochMillis))

actual fun knownDateDifferences(): Map<String, String> = emptyMap()

actual val runsAndroidFormatting: Boolean = true

actual val jsonKeyOrderMatchesAndroid: Boolean = true

actual fun resourceLocaleReference(languageTag: String): (() -> String)? = {
    val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
    val configuration = android.content.res.Configuration(context.resources.configuration)
    configuration.setLocale(java.util.Locale.forLanguageTag(languageTag))
    context.createConfigurationContext(configuration).getString(ai.littlepress.a2ui.integration.R.string.a2ui_resource_locale)
}

actual fun knownDateTimeDifferences(): Map<String, String> = emptyMap()

actual fun knownCalendarDifferences(): Map<String, String> = emptyMap()

actual fun basicFunctions(): androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1.Functions? =
    androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1.Functions(
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
    )
