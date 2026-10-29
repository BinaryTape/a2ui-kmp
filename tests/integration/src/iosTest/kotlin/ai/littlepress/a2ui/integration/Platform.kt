package ai.littlepress.a2ui.integration

import platform.UIKit.UIDevice

actual fun platformName(): String = "iOS " + UIDevice.currentDevice.systemVersion

// Foundation names UTC "Greenwich Mean Time"; Android says "Coordinated Universal Time".
actual fun knownPatternDifferences(): Map<String, String> =
    mapOf("zzzz" to "Greenwich Mean Time", "zzzzz" to "Greenwich Mean Time")

actual val canParseJson: Boolean = true

actual fun icuReference(pattern: String, languageTag: String, count: Double): (() -> String)? = null

actual fun androidIdentifierReference(): Pair<(Char) -> Boolean, (Char) -> Boolean>? = null

// Foundation writes the yen sign in Japanese as "¥"; Android's ICU uses the full-width "￥".
actual fun knownPluralDifferences(): Map<String, String> =
    listOf("1.0" to "¥1", "1.5" to "¥2", "1234.5" to "¥1,234").associate { (count, result) ->
        "ja-JP | $count | {count, plural, other {{count, number, currency}}}" to result
    }

actual fun androidNumberReference(languageTag: String, number: Double): String? = null

// Foundation's separators for these locales differ from Android's (the digits are the same).
actual fun knownNumberDifferences(): Map<String, String> =
    mapOf("sd-PK" to "\u0661\u066C\u0662\u0663\u0664.\u0665", "uz-Latn-AF" to "1.234,5")

actual fun androidDateReference(languageTag: String, epochMillis: Long, pattern: String): String? = null

actual fun knownDateDifferences(): Map<String, String> = emptyMap()

actual val runsAndroidFormatting: Boolean = true

actual val jsonKeyOrderMatchesAndroid: Boolean = false

actual fun resourceLocaleReference(languageTag: String): (() -> String)? = null

actual fun knownDateTimeDifferences(): Map<String, String> = emptyMap()

actual fun knownCalendarDifferences(): Map<String, String> = emptyMap()

actual fun basicFunctions(): androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1.Functions? =
    androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1.Functions(ai.littlepress.a2ui.compat.android.content.Context())
