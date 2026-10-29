package ai.littlepress.a2ui.compat.java.util.concurrent.locks

/** java.util.concurrent.locks.ReentrantLock as upstream uses it, with [withLock]. */
public expect class ReentrantLock(fair: Boolean) {
    public fun lock()

    public fun unlock()
}
