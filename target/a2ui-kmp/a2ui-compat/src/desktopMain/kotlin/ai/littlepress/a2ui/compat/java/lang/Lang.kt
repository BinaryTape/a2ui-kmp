package ai.littlepress.a2ui.compat.java.lang

public actual object System {
    public actual fun currentTimeMillis(): Long = java.lang.System.currentTimeMillis()

    public actual fun identityHashCode(x: Any?): Int = java.lang.System.identityHashCode(x)
}
