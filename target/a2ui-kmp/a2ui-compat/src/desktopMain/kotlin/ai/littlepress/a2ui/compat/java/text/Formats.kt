package ai.littlepress.a2ui.compat.java.text

import ai.littlepress.a2ui.compat.java.util.Currency
import ai.littlepress.a2ui.compat.java.util.Date
import ai.littlepress.a2ui.compat.java.util.Locale
import ai.littlepress.a2ui.compat.java.util.withAndroidNumbering
import ai.littlepress.a2ui.compat.java.util.TimeZone

public actual class SimpleDateFormat actual constructor(pattern: String, locale: Locale) {
    // Android's java.text always uses the Gregorian calendar; the JDK uses the Buddhist one for th-TH.
    private val jdk = java.text.SimpleDateFormat(pattern, locale.withAndroidNumbering).apply {
        calendar = java.util.GregorianCalendar(timeZone, locale)
        dateFormatSymbols = dateFormatSymbols.apply {
            amPmStrings = androidAmPm(locale.language, locale.script, locale.country).toTypedArray()
        }
    }

    public actual var timeZone: TimeZone = TimeZone.getTimeZone(jdk.timeZone.id)
        set(value) {
            field = value
            jdk.timeZone = java.util.TimeZone.getTimeZone(value.id)
        }

    public actual var isLenient: Boolean
        get() = jdk.isLenient
        set(value) {
            jdk.isLenient = value
        }

    public actual fun format(date: Date): String = jdk.format(java.util.Date(date.time))

    public actual fun parse(text: String): Date? =
        try {
            jdk.parse(text)?.let { Date(it.time) }
        } catch (e: java.text.ParseException) {
            throw ParseException(e.message, e.errorOffset)
        }
}

public actual class NumberFormat private constructor(private val jdk: java.text.NumberFormat) {
    public actual var isGroupingUsed: Boolean
        get() = jdk.isGroupingUsed
        set(value) {
            jdk.isGroupingUsed = value
        }

    public actual var minimumFractionDigits: Int
        get() = jdk.minimumFractionDigits
        set(value) {
            jdk.minimumFractionDigits = value
        }

    public actual var maximumFractionDigits: Int
        get() = jdk.maximumFractionDigits
        set(value) {
            jdk.maximumFractionDigits = value
        }

    public actual var currency: Currency? = null
        set(value) {
            field = value
            if (value != null) jdk.currency = java.util.Currency.getInstance(value.currencyCode)
        }

    public actual fun format(number: Double): String = jdk.format(number)

    public actual companion object {
        public actual fun getNumberInstance(locale: Locale): NumberFormat =
            NumberFormat(java.text.NumberFormat.getNumberInstance(locale.withAndroidNumbering))

        public actual fun getCurrencyInstance(locale: Locale): NumberFormat =
            NumberFormat(java.text.NumberFormat.getCurrencyInstance(locale.withAndroidNumbering))

        public actual fun getPercentInstance(locale: Locale): NumberFormat =
            NumberFormat(java.text.NumberFormat.getPercentInstance(locale.withAndroidNumbering))
    }
}
