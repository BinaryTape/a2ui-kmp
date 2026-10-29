package ai.littlepress.a2ui.compat.java.util.concurrent

public actual class CopyOnWriteArrayList<E> actual constructor() : AbstractList<E>() {
    private val list = java.util.concurrent.CopyOnWriteArrayList<E>()

    actual override val size: Int
        get() = list.size

    actual override fun get(index: Int): E = list[index]

    // The JDK's iterators are snapshots.
    override fun iterator(): Iterator<E> = list.iterator()

    override fun listIterator(): ListIterator<E> = list.listIterator()

    override fun listIterator(index: Int): ListIterator<E> = list.listIterator(index)

    public actual fun add(element: E): Boolean = list.add(element)

    public actual fun clear() {
        list.clear()
    }
}
