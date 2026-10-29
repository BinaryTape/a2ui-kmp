package ai.littlepress.a2ui.compat.kotlin.concurrent

import ai.littlepress.a2ui.compat.java.util.concurrent.locks.ReentrantLock

/** kotlin.concurrent.withLock, which is JVM-only. */
public inline fun <T> ReentrantLock.withLock(action: () -> T): T {
    lock()
    try {
        return action()
    } finally {
        unlock()
    }
}
