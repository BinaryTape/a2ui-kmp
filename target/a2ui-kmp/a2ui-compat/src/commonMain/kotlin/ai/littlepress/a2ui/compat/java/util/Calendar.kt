package ai.littlepress.a2ui.compat.java.util

/**
 * The part of java.util.Calendar upstream uses: a Gregorian calendar in a time zone, its time in
 * epoch milliseconds, and getting and setting the date and time fields. Setting is lenient, as by
 * default in the JDK: 25 hours is 1 o'clock the next day. On desktop this is the JDK's
 * GregorianCalendar, on iOS Foundation's Gregorian calendar; like Android's, both are Julian before
 * 1582-10-15.
 */
public class Calendar private constructor(private val calendar: PlatformCalendar) {
    /** Starts at the current time, as Calendar.getInstance does. */
    public var timeInMillis: Long
        get() = calendar.timeInMillis
        set(value) {
            calendar.timeInMillis = value
        }

    public fun get(field: Int): Int = calendar.get(checkField(field))

    public fun set(field: Int, value: Int) {
        calendar.set(checkField(field), value)
    }

    /** Clears every field: the time becomes 1970-01-01 00:00:00.000 in the calendar's time zone. */
    public fun clear() {
        calendar.clear()
    }

    public companion object {
        public const val YEAR: Int = 1
        public const val MONTH: Int = 2
        public const val DAY_OF_MONTH: Int = 5
        public const val HOUR_OF_DAY: Int = 11
        public const val MINUTE: Int = 12
        public const val SECOND: Int = 13
        public const val MILLISECOND: Int = 14

        public fun getInstance(zone: TimeZone): Calendar = Calendar(PlatformCalendar(zone.id))

        private val FIELDS = setOf(YEAR, MONTH, DAY_OF_MONTH, HOUR_OF_DAY, MINUTE, SECOND, MILLISECOND)

        private fun checkField(field: Int): Int {
            require(field in FIELDS) { "Calendar field $field is not supported" }
            return field
        }
    }
}

/** A Gregorian calendar in the time zone [zoneId]; fields are numbered as in java.util.Calendar. */
internal expect class PlatformCalendar(zoneId: String) {
    var timeInMillis: Long

    fun get(field: Int): Int

    fun set(field: Int, value: Int)

    fun clear()
}
