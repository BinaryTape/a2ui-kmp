package androidx.test.espresso

import org.junit.AssumptionViolatedException

object Espresso {
    /** Desktop has no soft keyboard to close. */
    @JvmStatic
    fun closeSoftKeyboard() {}

    /** Desktop has no system back: a test that presses it is skipped (reported, not passed). */
    @JvmStatic
    fun pressBack(): Nothing = throw AssumptionViolatedException("Android back has no desktop equivalent")
}
