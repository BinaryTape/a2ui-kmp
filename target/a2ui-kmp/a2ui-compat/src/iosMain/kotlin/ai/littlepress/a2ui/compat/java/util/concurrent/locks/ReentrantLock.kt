package ai.littlepress.a2ui.compat.java.util.concurrent.locks

import platform.Foundation.NSCondition
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.pthread_self
import platform.posix.pthread_t

/**
 * Reentrant. With [fair] (which upstream always passes), threads get the lock in the order they
 * asked for it, as java.util.concurrent.locks.ReentrantLock(true) grants it: each waiting thread
 * takes a ticket, and the lock goes to the lowest ticket when it is released.
 */
@OptIn(ExperimentalForeignApi::class)
public actual class ReentrantLock actual constructor(private val fair: Boolean) {
    private val condition = NSCondition()
    private var owner: pthread_t? = null
    private var holdCount = 0
    private var nextTicket = 0L
    private var nowServing = 0L

    public actual fun lock() {
        val thread = pthread_self()
        condition.lock()
        try {
            if (owner == thread) {
                holdCount++
                return
            }
            if (fair) {
                val ticket = nextTicket++
                while (owner != null || nowServing != ticket) condition.wait()
                nowServing++
            } else {
                while (owner != null) condition.wait()
            }
            owner = thread
            holdCount = 1
        } finally {
            condition.unlock()
        }
    }

    public actual fun unlock() {
        condition.lock()
        try {
            check(owner == pthread_self()) { "IllegalMonitorStateException: not the lock owner" }
            if (--holdCount == 0) {
                owner = null
                condition.broadcast()
            }
        } finally {
            condition.unlock()
        }
    }
}
