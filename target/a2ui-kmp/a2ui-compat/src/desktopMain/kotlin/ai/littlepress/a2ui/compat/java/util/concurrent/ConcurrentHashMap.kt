package ai.littlepress.a2ui.compat.java.util.concurrent

public actual class ConcurrentHashMap<K : Any, V : Any> actual constructor() {
    private val map = java.util.concurrent.ConcurrentHashMap<K, V>()

    public actual operator fun get(key: K): V? = map[key]

    public actual operator fun set(key: K, value: V) {
        map[key] = value
    }

    public actual fun computeIfAbsent(key: K, mappingFunction: (K) -> V): V =
        map.computeIfAbsent(key) { mappingFunction(it) }

    public actual fun compute(key: K, remappingFunction: (K, V?) -> V?): V? =
        map.compute(key) { k, v -> remappingFunction(k, v) }

    public actual fun remove(key: K): V? = map.remove(key)

    public actual fun clear() {
        map.clear()
    }
}
