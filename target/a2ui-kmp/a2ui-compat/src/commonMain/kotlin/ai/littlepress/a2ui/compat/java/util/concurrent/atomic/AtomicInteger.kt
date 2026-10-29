package ai.littlepress.a2ui.compat.java.util.concurrent.atomic

import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/** The part of java.util.concurrent.atomic.AtomicInteger upstream uses, on Kotlin's AtomicInt. */
@OptIn(ExperimentalAtomicApi::class)
public class AtomicInteger(initialValue: Int) {
    private val value = AtomicInt(initialValue)

    public fun get(): Int = value.load()

    public fun incrementAndGet(): Int = value.addAndFetch(1)

    public fun decrementAndGet(): Int = value.addAndFetch(-1)
}
