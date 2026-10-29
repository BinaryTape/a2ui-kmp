package ai.littlepress.a2ui.compat.java.util.concurrent

/** The part of java.util.concurrent.ConcurrentHashMap upstream uses; each call is atomic. */
public expect class ConcurrentHashMap<K : Any, V : Any>() {
    public operator fun get(key: K): V?

    /** put, as `map[key] = value`. */
    public operator fun set(key: K, value: V)

    public fun computeIfAbsent(key: K, mappingFunction: (K) -> V): V

    public fun compute(key: K, remappingFunction: (K, V?) -> V?): V?

    public fun remove(key: K): V?

    public fun clear()
}
