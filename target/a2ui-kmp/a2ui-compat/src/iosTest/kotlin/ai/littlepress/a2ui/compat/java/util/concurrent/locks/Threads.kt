package ai.littlepress.a2ui.compat.java.util.concurrent.locks

import platform.Foundation.NSThread

actual fun startThread(block: () -> Unit) {
    NSThread(block = block).start()
}

actual fun sleepMillis(millis: Long) {
    NSThread.sleepForTimeInterval(millis / 1000.0)
}
