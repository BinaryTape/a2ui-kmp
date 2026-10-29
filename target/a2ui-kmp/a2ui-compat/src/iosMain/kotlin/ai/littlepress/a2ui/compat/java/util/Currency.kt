package ai.littlepress.a2ui.compat.java.util

import platform.Foundation.ISOCurrencyCodes
import platform.Foundation.NSLocale

public actual class Currency private constructor(public actual val currencyCode: String) {
    public actual companion object {
        /** Throws IllegalArgumentException for a code that is not an ISO 4217 currency, as the JDK does. */
        public actual fun getInstance(currencyCode: String): Currency {
            require(NSLocale.ISOCurrencyCodes.contains(currencyCode)) { "Unknown currency code: $currencyCode" }
            return Currency(currencyCode)
        }
    }
}
