package ai.littlepress.a2ui.compat.java.lang

// java.lang is imported by default on the JVM. The overlays import these explicitly instead, which
// takes precedence over the default import.

/**
 * The java.lang.Character methods upstream uses. Identifier characters are classified as Android
 * does (recorded in [IDENTIFIER_START_RANGES] and [IDENTIFIER_PART_RANGES]): the JDK also counts
 * control characters as identifier parts, and Kotlin/Native's Unicode data is older.
 */
public object Character {
    public fun isUnicodeIdentifierStart(ch: Char): Boolean = IDENTIFIER_START_RANGES.containsChar(ch)

    public fun isUnicodeIdentifierPart(ch: Char): Boolean = IDENTIFIER_PART_RANGES.containsChar(ch)

    /** The lower-case digit, or '\u0000' when the digit or radix is out of range, as in Java. */
    public fun forDigit(digit: Int, radix: Int): Char =
        if (radix !in 2..36 || digit !in 0 until radix) '\u0000' else digit.digitToChar(radix).lowercaseChar()

    private fun IntArray.containsChar(ch: Char): Boolean {
        // Binary search over the first,last pairs.
        var low = 0
        var high = size / 2 - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            when {
                ch.code < this[2 * mid] -> high = mid - 1
                ch.code > this[2 * mid + 1] -> low = mid + 1
                else -> return true
            }
        }
        return false
    }
}

/** The java.lang.System methods upstream uses. */
public expect object System {
    public fun currentTimeMillis(): Long

    public fun identityHashCode(x: Any?): Int
}
