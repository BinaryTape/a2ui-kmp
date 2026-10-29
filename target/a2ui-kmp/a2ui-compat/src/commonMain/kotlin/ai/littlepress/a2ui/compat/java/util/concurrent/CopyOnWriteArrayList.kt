package ai.littlepress.a2ui.compat.java.util.concurrent

/**
 * The part of java.util.concurrent.CopyOnWriteArrayList upstream uses: a list that threads can add
 * to and clear while others read it; iterating sees the elements at the time it started.
 */
public expect class CopyOnWriteArrayList<E>() : AbstractList<E> {
    override val size: Int

    override fun get(index: Int): E

    public fun add(element: E): Boolean

    public fun clear()
}
