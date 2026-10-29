package ai.littlepress.a2ui.compat.java.util

public actual class Currency private constructor(internal val jdk: java.util.Currency) {
    public actual val currencyCode: String get() = jdk.currencyCode

    public actual companion object {
        public actual fun getInstance(currencyCode: String): Currency =
            Currency(java.util.Currency.getInstance(currencyCode))
    }
}
