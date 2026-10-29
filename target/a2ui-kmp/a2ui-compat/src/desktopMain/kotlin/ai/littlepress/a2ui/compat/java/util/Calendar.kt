package ai.littlepress.a2ui.compat.java.util

internal actual class PlatformCalendar actual constructor(zoneId: String) {
    private val jdk = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone(zoneId))

    actual var timeInMillis: Long
        get() = jdk.timeInMillis
        set(value) {
            jdk.timeInMillis = value
        }

    actual fun get(field: Int): Int = jdk.get(field)

    actual fun set(field: Int, value: Int) {
        jdk.set(field, value)
    }

    actual fun clear() {
        jdk.clear()
    }
}
