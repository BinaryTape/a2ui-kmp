package androidx.test.platform.app

import org.junit.AssumptionViolatedException

object InstrumentationRegistry {
    @JvmStatic
    fun getInstrumentation(): Instrumentation = Instrumentation
}

/** Desktop has no system back key: a test that sends one is skipped (reported, not passed). */
object Instrumentation {
    fun sendKeyDownUpSync(keyCode: Int): Nothing =
        throw AssumptionViolatedException("Android key $keyCode has no desktop equivalent")
}
