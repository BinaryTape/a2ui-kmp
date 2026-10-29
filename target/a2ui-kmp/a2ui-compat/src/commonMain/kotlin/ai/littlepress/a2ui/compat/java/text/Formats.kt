package ai.littlepress.a2ui.compat.java.text

import ai.littlepress.a2ui.compat.java.util.Currency
import ai.littlepress.a2ui.compat.java.util.Date
import ai.littlepress.a2ui.compat.java.util.Locale
import ai.littlepress.a2ui.compat.java.util.TimeZone

/** java.text.ParseException. */
public open class ParseException(message: String?, public val errorOffset: Int) : Exception(message)

/** The part of java.text.SimpleDateFormat upstream uses: a pattern, a locale, a time zone, format and parse. */
public expect class SimpleDateFormat(pattern: String, locale: Locale) {
    public var timeZone: TimeZone
    public var isLenient: Boolean

    public fun format(date: Date): String

    /** Throws [ParseException] when [text] does not match, as the JDK does. */
    public fun parse(text: String): Date?
}

/** The part of java.text.NumberFormat upstream uses: number and currency instances. */
public expect class NumberFormat {
    public var isGroupingUsed: Boolean
    public var minimumFractionDigits: Int
    public var maximumFractionDigits: Int
    public var currency: Currency?

    public fun format(number: Double): String

    public companion object {
        public fun getNumberInstance(locale: Locale): NumberFormat

        public fun getCurrencyInstance(locale: Locale): NumberFormat

        /** For ICU MessageFormat's `{arg, number, percent}`. */
        public fun getPercentInstance(locale: Locale): NumberFormat
    }
}
