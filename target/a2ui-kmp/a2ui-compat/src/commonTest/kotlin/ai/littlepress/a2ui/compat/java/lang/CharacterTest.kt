package ai.littlepress.a2ui.compat.java.lang

import kotlin.test.Test
import kotlin.test.assertEquals

class CharacterTest {
    /** java.lang.Character.forDigit: lower-case letters, '\u0000' when out of range. */
    @Test
    fun forDigit() {
        assertEquals("0123456789abcdef", (0..15).map { Character.forDigit(it, 16) }.joinToString(""))
        assertEquals('z', Character.forDigit(35, 36))
        assertEquals('\u0000', Character.forDigit(16, 16))
        assertEquals('\u0000', Character.forDigit(-1, 16))
        assertEquals('\u0000', Character.forDigit(1, 1))
        assertEquals('\u0000', Character.forDigit(1, 37))
    }
}
