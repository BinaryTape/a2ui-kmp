package ai.littlepress.a2ui.compat.android.icu.text

import ai.littlepress.a2ui.compat.java.text.NumberFormat
import ai.littlepress.a2ui.compat.java.util.Locale

/**
 * The part of android.icu.text.MessageFormat that upstream uses: `format(Map)` with named
 * arguments. Supports what [MessagePattern] parses: `plural` and `selectordinal` with CLDR rules
 * ([PluralRules]) and `=N` selectors compared by value, `select`, and number arguments with no
 * style or the `integer`, `percent` and `currency` styles (`#` and plain numbers use the locale's
 * number format, at most three fraction digits). Other argument types and number styles (patterns,
 * skeletons, dates, times) throw IllegalArgumentException. A missing argument is written as
 * `{name}`, as ICU does.
 */
public class MessageFormat(pattern: String, private val locale: Locale) {
    private val message = MessagePattern(pattern).message

    public fun format(arguments: Map<String, Any?>): String = buildString { append(message, arguments, null) }

    private fun StringBuilder.append(message: Message, arguments: Map<String, Any?>, pluralNumber: Double?) {
        for (element in message.elements) {
            when (element) {
                is Element.Text -> append(element.text)
                Element.Number -> append(formatNumber(pluralNumber ?: 0.0))
                is Element.Argument -> appendArgument(element, arguments)
            }
        }
    }

    private fun StringBuilder.appendArgument(argument: Element.Argument, arguments: Map<String, Any?>) {
        if (argument.name !in arguments) {
            append("{${argument.name}}")
            return
        }
        val value = arguments[argument.name]
        when (argument.type) {
            null -> append(if (value is Number) formatNumber(value.toDouble()) else value.toString())
            "number" -> append(formatNumber(toNumber(value, argument), argument.style))
            "plural", "selectordinal" -> {
                val number = toNumber(value, argument)
                val explicit = argument.options.entries.firstOrNull { (key, _) ->
                    key.startsWith("=") && key.substring(1).toDoubleOrNull() == number
                }?.value
                val rules = PluralRules.forLocale(locale, ordinal = argument.type == "selectordinal")
                val chosen = explicit ?: argument.options[rules.select(number - argument.offset)]
                    ?: argument.options.getValue("other")
                append(chosen, arguments, number - argument.offset)
            }
            "select" -> {
                val chosen = argument.options[value.toString()] ?: argument.options.getValue("other")
                append(chosen, arguments, null)
            }
            else -> unsupported("argument type '${argument.type}'")
        }
    }

    private fun toNumber(value: Any?, argument: Element.Argument): Double =
        (value as? Number)?.toDouble() ?: throw IllegalArgumentException("'$value' is not a Number for argument ${argument.name}")

    private fun formatNumber(number: Double, style: String? = null): String = when (style) {
        null, "" -> NumberFormat.getNumberInstance(locale)
        // ICU's integer instance: no fraction digits, rounding half-even.
        "integer" -> NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 0 }
        "percent" -> NumberFormat.getPercentInstance(locale)
        "currency" -> NumberFormat.getCurrencyInstance(locale)
        else -> unsupported("number style '$style'")
    }.format(number)

    private fun unsupported(what: String): Nothing =
        throw IllegalArgumentException("a2ui-kmp: MessageFormat does not support $what")
}
