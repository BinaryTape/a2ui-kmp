package ai.littlepress.a2ui.compat.kotlin.jvm

import kotlin.jvm.javaClass as jvmClass

public actual typealias JavaClass<T> = Class<T>

public actual val <T : Any> T.javaClass: JavaClass<T>
    get() = jvmClass

public actual val JavaClass<*>.simpleName: String
    get() = getSimpleName()
