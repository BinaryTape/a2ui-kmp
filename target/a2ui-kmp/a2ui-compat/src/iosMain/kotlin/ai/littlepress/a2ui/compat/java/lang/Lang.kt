package ai.littlepress.a2ui.compat.java.lang

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.identityHashCode
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

public actual object System {
    public actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

    @OptIn(ExperimentalNativeApi::class)
    public actual fun identityHashCode(x: Any?): Int = x.identityHashCode()
}
