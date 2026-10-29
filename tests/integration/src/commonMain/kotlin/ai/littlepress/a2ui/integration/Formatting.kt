package ai.littlepress.a2ui.integration

import ai.littlepress.a2ui.compat.java.util.Locale
import ai.littlepress.a2ui.compat.java.util.Locales
import androidx.a2ui.model.catalog.A2uiFunction
import androidx.a2ui.model.catalog.A2uiFunctionDefinition
import androidx.a2ui.model.catalog.functions.A2uiFormatCurrencyFunction
import androidx.a2ui.model.catalog.functions.A2uiFormatDateFunction
import androidx.a2ui.model.catalog.functions.A2uiFormatNumberFunction
import androidx.a2ui.model.catalog.functions.A2uiLocaleProvider
import androidx.a2ui.model.catalog.functions.A2uiMessageFormatter
import androidx.a2ui.model.catalog.functions.A2uiPluralizeFunction
import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.a2ui.model.protocol.A2uiExecutionContext

/**
 * Shared code as an app would write it against the A2UI model API: its own locale provider and
 * message formatter (both have `Locale` in their signatures), and the basic catalog formatting
 * functions. On Android this compiles and runs against the official androidx.a2ui artifact.
 */
class Formatting(locale: Locale = Locales.US) {
    val localeProvider: A2uiLocaleProvider = A2uiLocaleProvider { locale }

    /** The locale the pluralize function last passed to [messageFormatter]. */
    var lastFormatterLocale: Locale? = null
        private set

    val messageFormatter: A2uiMessageFormatter = A2uiMessageFormatter { pattern, locale, arguments ->
        lastFormatterLocale = locale
        "$pattern ${arguments["count"]}"
    }

    fun currency(value: Double, code: String, decimals: Int? = null): Any? =
        A2uiFormatCurrencyFunction(localeProvider)
            .run(mapOf("value" to value, "currency" to code) + decimalsArg(decimals))

    fun number(value: Double, decimals: Int? = null, grouping: Boolean? = null): Any? =
        A2uiFormatNumberFunction(localeProvider)
            .run(mapOf<String, Any>("value" to value) + decimalsArg(decimals) +
                (grouping?.let { mapOf("grouping" to it) } ?: emptyMap()))

    /** [value] is epoch millis (seconds below 10^10) or an ISO 8601 string. */
    fun date(value: Any, format: String): Any? =
        A2uiFormatDateFunction(localeProvider).run(mapOf("value" to value, "format" to format))

    fun pluralize(value: Double): Any? =
        A2uiPluralizeFunction(messageFormatter, localeProvider)
            .run(mapOf("value" to value, "one" to "item", "other" to "items"))

    private fun decimalsArg(decimals: Int?): Map<String, Any> =
        decimals?.let { mapOf("decimals" to it.toDouble()) } ?: emptyMap()

    private fun A2uiFunction.run(args: Map<String, Any>): Any? = execute(args, NoDataContext)
}

private object NoDataContext : A2uiExecutionContext {
    override val dataPath: A2uiDataPath = A2uiDataPath("")

    override fun evaluatePayload(payload: Any?): Any? = payload

    override fun executeFunction(name: String, args: Map<String, Any>): Any? = null

    override fun resolveValue(path: A2uiDataPath): Any? = null

    override fun <T : Any> getOrCreateFunctionScopedCache(
        functionDefinition: A2uiFunctionDefinition,
        factory: () -> T,
    ): T = factory()
}
