package ai.littlepress.a2ui.compat.java.util.concurrent

import platform.Foundation.NSRecursiveLock

public actual class CopyOnWriteArrayList<E> actual constructor() : AbstractList<E>() {
    private val lock = NSRecursiveLock()
    private var elements: List<E> = emptyList()

    // Every change replaces the list, so a reader keeps a consistent snapshot.
    private fun snapshot(): List<E> {
        lock.lock()
        try {
            return elements
        } finally {
            lock.unlock()
        }
    }

    private fun replace(change: (List<E>) -> List<E>) {
        lock.lock()
        try {
            elements = change(elements)
        } finally {
            lock.unlock()
        }
    }

    actual override val size: Int
        get() = snapshot().size

    actual override fun get(index: Int): E = snapshot()[index]

    override fun iterator(): Iterator<E> = snapshot().iterator()

    override fun listIterator(): ListIterator<E> = snapshot().listIterator()

    override fun listIterator(index: Int): ListIterator<E> = snapshot().listIterator(index)

    public actual fun add(element: E): Boolean {
        replace { it + element }
        return true
    }

    public actual fun clear() {
        replace { emptyList() }
    }
}
