package ai.littlepress.a2ui.compat.java.util

/** java.util.Date: a point in time in epoch milliseconds. */
public class Date(public val time: Long)

/** java.util.TimeZone, by ID; upstream only uses "UTC". */
public class TimeZone private constructor(public val id: String) {
    public companion object {
        public fun getTimeZone(id: String): TimeZone = TimeZone(id)
    }
}

/** java.util.Currency: an ISO 4217 code. getInstance throws for an unknown code, as the JDK does. */
public expect class Currency {
    public val currencyCode: String

    public companion object {
        public fun getInstance(currencyCode: String): Currency
    }
}
