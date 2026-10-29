package ai.littlepress.a2ui.compat.java.text

import ai.littlepress.a2ui.compat.java.util.Currency
import ai.littlepress.a2ui.compat.java.util.Date
import ai.littlepress.a2ui.compat.java.util.Locale
import ai.littlepress.a2ui.compat.java.util.TimeZone
import ai.littlepress.a2ui.compat.java.util.parts
import ai.littlepress.a2ui.compat.java.util.withAndroidNumbering
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSCalendarUnitWeekday
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDecimalNumber
import platform.Foundation.NSLocaleDecimalSeparator
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterCurrencyStyle
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.NSNumberFormatterPercentStyle
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.timeZoneWithName

// iOS implementations on Foundation, adjusted to give what the official artifacts give on Android
// (see AndroidDatePattern and the currency setter). tests/integration checks the cases on every
// platform and prints the ones that still differ.

public actual class SimpleDateFormat actual constructor(pattern: String, locale: Locale) {
    private val androidPattern = AndroidDatePattern(pattern)
    // Android's java.text always uses the Gregorian calendar; Foundation follows the locale
    // (Buddhist for th-TH, Persian for fa-IR).
    private val ns = NSDateFormatter().apply {
        this.locale = locale.withAndroidNumbering
        calendar = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian)!!
        // Android's markers: Foundation's differ in some locales (e.g. "PM" for th-TH).
        val (language, script, region) = locale.parts
        val (am, pm) = androidAmPm(language, script, region)
        AMSymbol = am
        PMSymbol = pm
    }

    public actual var timeZone: TimeZone = TimeZone.getTimeZone(ns.timeZone.name)
        set(value) {
            field = value
            NSTimeZone.timeZoneWithName(value.id)?.let { ns.timeZone = it }
            updatePattern()
        }

    init {
        updatePattern()
    }

    private fun updatePattern(date: NSDate? = null) {
        ns.dateFormat = androidPattern.toIcu(timeZone.id) {
            val calendar = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian)!!
            calendar.timeZone = ns.timeZone
            // NSCalendar counts from Sunday = 1; ISO from Monday = 1.
            (calendar.component(NSCalendarUnitWeekday, fromDate = date ?: NSDate()).toInt() + 5) % 7 + 1
        }
    }

    public actual var isLenient: Boolean
        get() = ns.lenient
        set(value) {
            ns.lenient = value
        }

    public actual fun format(date: Date): String {
        val nsDate = NSDate.dateWithTimeIntervalSince1970(date.time / 1000.0)
        if (androidPattern.dependsOnDate) updatePattern(nsDate)
        return ns.stringFromDate(nsDate)
    }

    public actual fun parse(text: String): Date? {
        val parsed = ns.dateFromString(text) ?: throw ParseException("Unparseable date: \"$text\"", 0)
        return Date((parsed.timeIntervalSince1970 * 1000).toLong())
    }
}

public actual class NumberFormat private constructor(private val ns: NSNumberFormatter) {
    public actual var isGroupingUsed: Boolean
        get() = ns.usesGroupingSeparator
        set(value) {
            ns.usesGroupingSeparator = value
        }

    public actual var minimumFractionDigits: Int
        get() = ns.minimumFractionDigits.toInt()
        set(value) {
            ns.minimumFractionDigits = value.toULong()
        }

    public actual var maximumFractionDigits: Int
        get() = ns.maximumFractionDigits.toInt()
        set(value) {
            ns.maximumFractionDigits = value.toULong()
        }

    public actual var currency: Currency? = null
        set(value) {
            field = value
            if (value == null) return
            // Android keeps the locale's fraction digits (JPY in en_US: "¥1,234.56"); Foundation
            // switches to the currency's own ("¥1,235").
            val min = ns.minimumFractionDigits
            val max = ns.maximumFractionDigits
            ns.currencyCode = value.currencyCode
            ns.minimumFractionDigits = min
            ns.maximumFractionDigits = max
            // Foundation puts a space between a currency code and the number ("KWD 1,234.56");
            // Android does not ("KWD1,234.56").
            ns.positivePrefix = ns.positivePrefix.trimCodeSpace()
            ns.negativePrefix = ns.negativePrefix.trimCodeSpace()
        }

    private fun String.trimCodeSpace(): String =
        if (length >= 2 && last().isWhitespace() && this[length - 2].isLetter()) dropLast(1) else this

    // Formats the shortest decimal form of the Double, as ICU and Java do; formatting the Double
    // itself keeps only about 15 significant digits.
    public actual fun format(number: Double): String {
        val value = if (number.isNaN() || number.isInfinite()) {
            NSNumber(number)
        } else {
            NSDecimalNumber(string = number.toString(), locale = mapOf(NSLocaleDecimalSeparator to "."))
        }
        return ns.stringFromNumber(value) ?: number.toString()
    }

    public actual companion object {
        public actual fun getNumberInstance(locale: Locale): NumberFormat =
            NumberFormat(NSNumberFormatter().apply {
                this.locale = locale.withAndroidNumbering
                numberStyle = NSNumberFormatterDecimalStyle
            })

        public actual fun getCurrencyInstance(locale: Locale): NumberFormat =
            NumberFormat(NSNumberFormatter().apply {
                this.locale = locale.withAndroidNumbering
                numberStyle = NSNumberFormatterCurrencyStyle
            })

        public actual fun getPercentInstance(locale: Locale): NumberFormat =
            NumberFormat(NSNumberFormatter().apply {
                this.locale = locale.withAndroidNumbering
                numberStyle = NSNumberFormatterPercentStyle
            })
    }
}
