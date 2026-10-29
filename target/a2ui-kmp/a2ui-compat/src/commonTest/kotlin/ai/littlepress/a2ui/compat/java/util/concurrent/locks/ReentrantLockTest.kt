package ai.littlepress.a2ui.compat.java.util.concurrent.locks

import ai.littlepress.a2ui.compat.java.util.concurrent.atomic.AtomicInteger
import ai.littlepress.a2ui.compat.kotlin.concurrent.withLock
import kotlin.test.Test
import kotlin.test.assertEquals

class ReentrantLockTest {
    @Test
    fun isReentrant() {
        val lock = ReentrantLock(true)
        val result = lock.withLock { lock.withLock { lock.withLock { 42 } } }
        assertEquals(42, result)
        // Released completely: another thread can take it.
        val done = AtomicInteger(0)
        startThread { lock.withLock { done.incrementAndGet() } }
        waitUntil { done.get() == 1 }
    }

    /**
     * Upstream passes fair = true so that waiting updates are applied in arrival order. The thread
     * that releases the lock asks for it again at once; a fair lock serves the waiting threads first,
     * an unfair one would let it barge in ahead of them.
     */
    @Test
    fun fairLockServesWaitingThreadsInArrivalOrder() {
        val lock = ReentrantLock(true)
        val order = mutableListOf<Int>()
        val finished = AtomicInteger(0)
        lock.lock()
        for (i in 1..5) {
            startThread {
                lock.withLock { order += i }
                finished.incrementAndGet()
            }
            // Let thread i block on the lock before starting the next one.
            sleepMillis(100)
        }
        lock.unlock()
        lock.withLock { order += 0 }
        waitUntil { finished.get() == 5 }
        assertEquals(listOf(1, 2, 3, 4, 5, 0), lock.withLock { order.toList() })
    }

    private fun waitUntil(condition: () -> Boolean) {
        repeat(500) {
            if (condition()) return
            sleepMillis(10)
        }
        error("timed out")
    }
}

expect fun startThread(block: () -> Unit)

expect fun sleepMillis(millis: Long)
