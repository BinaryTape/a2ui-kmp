package ai.littlepress.a2ui.compat.java.util.concurrent.locks

actual fun startThread(block: () -> Unit) {
    Thread(block).start()
}

actual fun sleepMillis(millis: Long) {
    Thread.sleep(millis)
}
