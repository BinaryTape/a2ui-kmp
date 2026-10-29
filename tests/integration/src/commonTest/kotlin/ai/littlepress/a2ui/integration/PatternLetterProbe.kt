package ai.littlepress.a2ui.integration

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Formats every ASCII letter at widths 1 to 5 and compares with the official artifact on Android
 * ([ANDROID_PATTERN_LETTERS]). Where this platform is known to differ, its own exact result is
 * expected instead ([knownPatternDifferences]).
 */
class PatternLetterProbe {
    private val formatting = Formatting()

    @Test
    fun patternLettersMatchAndroid() {
        val mismatches = mutableListOf<String>()
        for ((letter, expected) in ANDROID_PATTERN_LETTERS) {
            val actual = (1..5).map { width ->
                runCatching { formatting.date(T, letter.toString().repeat(width)) }
                    .fold({ "$it" }, { "!" })
            }
            println("A2UI-LETTER ${platformName()} | $letter | ${actual.joinToString(" | ")}")
            for (width in 1..5) {
                val pattern = letter.toString().repeat(width)
                val want = knownPatternDifferences()[pattern] ?: expected[width - 1]
                if (actual[width - 1] != want) {
                    mismatches += "$pattern: expected \"$want\" (Android \"${expected[width - 1]}\"), got \"${actual[width - 1]}\""
                }
            }
        }
        assertEquals(emptyList(), mismatches, "patterns that format differently from Android")
    }
}

/** 2023-11-14T22:13:20.123Z, a Tuesday. */
internal const val T = 1_700_000_000_123L

/** Patterns that format differently from Android on this platform, with this platform's result. */
expect fun knownPatternDifferences(): Map<String, String>
