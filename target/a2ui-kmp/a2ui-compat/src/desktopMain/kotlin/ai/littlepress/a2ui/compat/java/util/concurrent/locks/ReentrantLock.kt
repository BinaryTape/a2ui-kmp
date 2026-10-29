package ai.littlepress.a2ui.compat.java.util.concurrent.locks

public actual class ReentrantLock actual constructor(fair: Boolean) {
    private val lock = java.util.concurrent.locks.ReentrantLock(fair)

    public actual fun lock() {
        lock.lock()
    }

    public actual fun unlock() {
        lock.unlock()
    }
}
