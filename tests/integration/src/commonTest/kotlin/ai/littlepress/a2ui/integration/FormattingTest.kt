package ai.littlepress.a2ui.integration

import ai.littlepress.a2ui.compat.java.util.Locales
import androidx.a2ui.model.catalog.functions.A2uiLocaleProvider
import androidx.a2ui.model.protocol.A2uiException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Runs on every target: Android (official artifact), desktop JVM and iOS (our builds). */
class FormattingTest {
    private val formatting = Formatting()

    @Test
    fun localeRoundTripsThroughTheApi() {
        assertEquals(Locales.US, formatting.localeProvider.getLocale())
        assertEquals(Locales.getDefault(), A2uiLocaleProvider.Default.getLocale())
        formatting.pluralize(1.0)
        assertEquals(Locales.US, formatting.lastFormatterLocale)
    }

    @Test
    fun formatsTheSameEverywhere() {
        assertEquals("$1,234.56", formatting.currency(1234.56, "USD"))
        assertEquals("€1,234.6", formatting.currency(1234.56, "EUR", decimals = 1))
        assertEquals("1,234.56", formatting.number(1234.56))
        assertEquals("1235", formatting.number(1234.56, decimals = 0, grouping = false))
        assertEquals("1970-01-01 00:00", formatting.date(0L, "yyyy-MM-dd HH:mm"))
        assertEquals("2023-11-14T22:13:20Z", formatting.date(T, "ISO"))
        // Values below 10^10 are read as seconds.
        assertEquals("1970-01-01 00:02:03", formatting.date(123L, "yyyy-MM-dd HH:mm:ss"))
        // ISO strings are parsed with java.text patterns too.
        assertEquals("2026-10-29 12:34:56", formatting.date("2026-10-29T12:34:56.789Z", "yyyy-MM-dd HH:mm:ss"))
        assertEquals("2026-10-29 00:00", formatting.date("2026-10-29", "yyyy-MM-dd HH:mm"))
        assertEquals("Thursday, January 1, 1970", formatting.date(0L, "EEEE, MMMM d, yyyy"))
    }

    @Test
    fun formatsLikeAndroid() {
        // Values from the official artifact on an Android 15 emulator (see OfficialArtifactTest).
        assertEquals("¥1,234.56", formatting.currency(1234.56, "JPY"))
        assertEquals("KWD1,234.56", formatting.currency(1234.56, "KWD"))
        assertEquals("2023-11-14 22:13:20.123", formatting.date(T, "yyyy-MM-dd HH:mm:ss.SSS"))
        assertEquals("2026-10-29 11:34:56.789",
            formatting.date("2026-10-29T12:34:56.789+01:00", "yyyy-MM-dd HH:mm:ss.SSS"))
        assertFailsWith<A2uiException.A2uiRuntimeException> { formatting.date(T, "invalid-pattern") }
        assertEquals("2023-11-14T22 o'clock", formatting.date(T, "yyyy-MM-dd'T'HH 'o''clock'"))
        assertEquals("'22'", formatting.date(T, "''HH''"))
        assertFailsWith<A2uiException.A2uiRuntimeException> { formatting.date(T, "'unterminated") }
        assertFailsWith<A2uiException.A2uiRuntimeException> { formatting.date(T, "yyyy-MM-dd 'open") }
    }
}

expect fun platformName(): String

/**
 * androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1.Functions(context) with this platform's context;
 * null without one (host tests). Used by Material3CatalogTest, in the "all" run only.
 */
expect fun basicFunctions(): androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1.Functions?
