package ai.littlepress.a2ui.integration

actual fun platformName(): String = "Android host JVM " + System.getProperty("java.version")

// The JDK formatter, which our desktop build and Android host tests use, gives these instead of
// Android's results ("!" = throws).
actual fun knownPatternDifferences(): Map<String, String> =
    listOf("B", "b", "c").flatMap { c -> (1..5).map { c.toString().repeat(it) to "!" } }.toMap() + mapOf(
        "EEEEE" to "Tuesday",
        "LLLLL" to "November",
        "MMMMM" to "November",
        "S" to "123",
        "SS" to "123",
        "SSSS" to "0123",
        "SSSSS" to "00123",
        "ZZZZ" to "+0000",
        "ZZZZZ" to "+0000",
    )

// android.util.JsonReader, which the official parser uses, is only a stub in local unit tests.
actual val canParseJson: Boolean = false

actual fun icuReference(pattern: String, languageTag: String, count: Double): (() -> String)? = null

actual fun androidIdentifierReference(): Pair<(Char) -> Boolean, (Char) -> Boolean>? = null

actual fun knownPluralDifferences(): Map<String, String> = emptyMap()

actual fun androidNumberReference(languageTag: String, number: Double): String? = null

// The JDK's separators for these locales differ from Android's (the digits are the same).
actual fun knownNumberDifferences(): Map<String, String> =
    mapOf("pa-PK" to "\u06F1,\u06F2\u06F3\u06F4\u066B\u06F5", "az-IR" to "1.234,5")

actual fun androidDateReference(languageTag: String, epochMillis: Long, pattern: String): String? = null

actual fun knownDateDifferences(): Map<String, String> = emptyMap()

actual val runsAndroidFormatting: Boolean = false

actual val jsonKeyOrderMatchesAndroid: Boolean = true

actual fun resourceLocaleReference(languageTag: String): (() -> String)? = null

actual fun knownDateTimeDifferences(): Map<String, String> = emptyMap()

actual fun knownCalendarDifferences(): Map<String, String> = emptyMap()

// Functions needs an android.content.Context, which local unit tests do not have.
actual fun basicFunctions(): androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1.Functions? = null
