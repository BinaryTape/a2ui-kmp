package ai.littlepress.a2ui.compat.kotlin.collections

/** java.util.Map.putIfAbsent. On the JVM the Map member wins over this extension. */
public fun <K, V> MutableMap<K, V>.putIfAbsent(key: K, value: V): V? {
    val existing = get(key)
    if (existing == null) put(key, value)
    return existing
}
