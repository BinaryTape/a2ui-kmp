package ai.littlepress.a2ui.integration

import ai.littlepress.a2ui.compat.java.lang.Character
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * formatString parses names with Character.isUnicodeIdentifierStart / Part. Our Character uses a
 * table recorded on Android; on an Android device this checks the table against Android's own
 * Character for every UTF-16 char, and prints the ranges again for re-recording.
 */
class IdentifierCharTest {
    @Test
    fun matchesAndroidOnDevice() {
        val reference = androidIdentifierReference() ?: return
        for ((name, ours, android) in listOf(
            Triple("start", Character::isUnicodeIdentifierStart, reference.first),
            Triple("part", Character::isUnicodeIdentifierPart, reference.second),
        )) {
            val differences = (0 until 0x10000).filter { ours(it.toChar()) != android(it.toChar()) }
            assertEquals(emptyList(), differences.map { it.toString(16) }, "chars classified unlike Android ($name)")
            printRanges(name, android)
        }
    }

    @Test
    fun knownCases() {
        // Other_ID_Start, Other_ID_Continue, Unicode 14 letters, controls (JDK only), U+2E2F (JDK only).
        assertEquals(true, Character.isUnicodeIdentifierStart('\u2118'))
        assertEquals(true, Character.isUnicodeIdentifierStart('\u212E'))
        assertEquals(true, Character.isUnicodeIdentifierPart('\u00B7'))
        assertEquals(false, Character.isUnicodeIdentifierStart('\u00B7'))
        assertEquals(true, Character.isUnicodeIdentifierStart('\u0870'))
        assertEquals(false, Character.isUnicodeIdentifierPart('\u0001'))
        assertEquals(false, Character.isUnicodeIdentifierStart('\u2E2F'))
        assertEquals(true, Character.isUnicodeIdentifierPart('_'))
        assertEquals(false, Character.isUnicodeIdentifierStart('_'))
        assertEquals(false, Character.isUnicodeIdentifierPart('-'))
    }

    private fun printRanges(name: String, test: (Char) -> Boolean) {
        val ranges = mutableListOf<String>()
        var from = -1
        for (c in 0..0x10000) {
            val yes = c < 0x10000 && test(c.toChar())
            if (yes && from < 0) from = c
            if (!yes && from >= 0) {
                ranges += "${from.toString(16)}-${(c - 1).toString(16)}"
                from = -1
            }
        }
        ranges.chunked(40).forEachIndexed { i, chunk -> println("A2UI-CHAR $name $i ${chunk.joinToString(",")}") }
    }
}

/** Android's Character.isUnicodeIdentifierStart and Part on an Android device, null elsewhere. */
expect fun androidIdentifierReference(): Pair<(Char) -> Boolean, (Char) -> Boolean>?
