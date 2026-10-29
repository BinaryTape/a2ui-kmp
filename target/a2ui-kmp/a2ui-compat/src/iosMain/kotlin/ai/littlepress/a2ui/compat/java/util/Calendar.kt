package ai.littlepress.a2ui.compat.java.util

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitEra
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitSecond
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.timeZoneWithName

internal actual class PlatformCalendar actual constructor(zoneId: String) {
    private val calendar = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian)!!.apply {
        timeZone = requireNotNull(NSTimeZone.timeZoneWithName(zoneId)) { "Unknown time zone $zoneId" }
    }

    actual var timeInMillis: Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

    actual fun get(field: Int): Int {
        if (field == Calendar.MILLISECOND) return timeInMillis.mod(1000L).toInt()
        val c = components()
        return when (field) {
            Calendar.YEAR -> c.year
            Calendar.MONTH -> c.month - 1
            Calendar.DAY_OF_MONTH -> c.day
            Calendar.HOUR_OF_DAY -> c.hour
            Calendar.MINUTE -> c.minute
            else -> c.second
        }.toInt()
    }

    actual fun set(field: Int, value: Int) {
        val millis = timeInMillis.mod(1000L)
        if (field == Calendar.MILLISECOND) {
            timeInMillis = timeInMillis - millis + value
            return
        }
        val c = components()
        when (field) {
            Calendar.YEAR -> c.year = value.toLong()
            Calendar.MONTH -> c.month = value.toLong() + 1
            Calendar.DAY_OF_MONTH -> c.day = value.toLong()
            Calendar.HOUR_OF_DAY -> c.hour = value.toLong()
            Calendar.MINUTE -> c.minute = value.toLong()
            else -> c.second = value.toLong()
        }
        // Foundation, like the lenient JDK calendar, carries values out of range into larger fields.
        val date = requireNotNull(calendar.dateFromComponents(c)) { "No date for field $field = $value" }
        timeInMillis = date.timeIntervalSince1970.toLong() * 1000 + millis
    }

    actual fun clear() {
        val epoch = NSDateComponents().apply {
            year = 1970
            month = 1
            day = 1
        }
        timeInMillis = calendar.dateFromComponents(epoch)!!.timeIntervalSince1970.toLong() * 1000
    }

    private fun components(): NSDateComponents = calendar.components(
        NSCalendarUnitEra or NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or
            NSCalendarUnitHour or NSCalendarUnitMinute or NSCalendarUnitSecond,
        fromDate = NSDate.dateWithTimeIntervalSince1970(timeInMillis.floorDiv(1000L).toDouble()),
    )
}
