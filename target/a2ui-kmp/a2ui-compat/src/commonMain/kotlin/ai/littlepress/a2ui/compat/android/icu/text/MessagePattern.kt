package ai.littlepress.a2ui.compat.android.icu.text

/**
 * The part of android.icu.text.MessagePattern that upstream uses: parsing an ICU message pattern and
 * listing its argument names and numbers. Supports simple arguments (`{name}`, `{name, type}`,
 * `{name, type, style}`), `plural`, `selectordinal` and `select` with nested messages, `#` in plural
 * messages, and ICU's default apostrophe quoting. Syntax errors throw IllegalArgumentException.
 */
public class MessagePattern(pattern: String) {
    internal val message: Message = Parser(pattern).parseRoot()
    private val parts: List<Part> = buildList { collectParts(message, this) }

    public fun countParts(): Int = parts.size

    public fun getPart(index: Int): Part = parts[index]

    public fun getSubstring(part: Part): String = part.text

    public class Part internal constructor(public val type: Type, internal val text: String) {
        public enum class Type { ARG_NAME, ARG_NUMBER }
    }

    private fun collectParts(message: Message, out: MutableList<Part>) {
        for (element in message.elements) {
            if (element !is Element.Argument) continue
            out += Part(if (element.name.all { it in '0'..'9' }) Part.Type.ARG_NUMBER else Part.Type.ARG_NAME, element.name)
            element.options.values.forEach { collectParts(it, out) }
        }
    }
}

internal class Message(val elements: List<Element>)

internal sealed interface Element {
    class Text(val text: String) : Element

    /** `#` in a plural or selectordinal message. */
    object Number : Element

    /** [type] is null for `{name}`; [options] are the sub-messages of plural, selectordinal and select. */
    class Argument(
        val name: String,
        val type: String?,
        val style: String?,
        val offset: Double,
        val options: Map<String, Message>,
    ) : Element
}

private class Parser(private val pattern: String) {
    private var pos = 0

    fun parseRoot(): Message {
        val message = parseMessage(nesting = 0, inPlural = false)
        if (pos < pattern.length) error("Unmatched '}' braces")
        return message
    }

    /** Parses until the '}' that closes this message (not consumed), or the end at the top level. */
    private fun parseMessage(nesting: Int, inPlural: Boolean): Message {
        val elements = mutableListOf<Element>()
        val text = StringBuilder()
        fun flush() {
            if (text.isNotEmpty()) elements += Element.Text(text.toString())
            text.clear()
        }
        while (pos < pattern.length) {
            val c = pattern[pos]
            when {
                c == '\'' -> readApostrophe(text, inPlural)
                c == '{' -> {
                    flush()
                    pos++
                    elements += parseArgument(nesting + 1)
                }
                c == '}' && nesting > 0 -> {
                    flush()
                    return Message(elements)
                }
                c == '#' && inPlural -> {
                    flush()
                    pos++
                    elements += Element.Number
                }
                else -> {
                    text.append(c)
                    pos++
                }
            }
        }
        if (nesting > 0) error("Unmatched '{' braces")
        flush()
        return Message(elements)
    }

    /**
     * ICU's default quoting: "''" is an apostrophe; an apostrophe before a syntax character starts
     * quoted text up to the next single apostrophe; any other apostrophe is literal.
     */
    private fun readApostrophe(text: StringBuilder, inPlural: Boolean) {
        val next = pattern.getOrNull(pos + 1)
        if (next == '\'') {
            text.append('\'')
            pos += 2
            return
        }
        if (next == '{' || next == '}' || next == '|' || (next == '#' && inPlural)) {
            pos++
            while (pos < pattern.length) {
                val c = pattern[pos]
                if (c == '\'') {
                    if (pattern.getOrNull(pos + 1) == '\'') {
                        text.append('\'')
                        pos += 2
                        continue
                    }
                    pos++
                    return
                }
                text.append(c)
                pos++
            }
            // An unterminated quote runs to the end of the pattern, as in ICU.
            return
        }
        text.append('\'')
        pos++
    }

    private fun parseArgument(nesting: Int): Element.Argument {
        skipWhitespace()
        val name = readIdentifier()
        if (name.isEmpty()) error("Bad argument syntax")
        if (name.all { it in '0'..'9' } && name.length > 1 && name[0] == '0') error("Bad argument number")
        skipWhitespace()
        when (pattern.getOrNull(pos)) {
            '}' -> {
                pos++
                return Element.Argument(name, null, null, 0.0, emptyMap())
            }
            ',' -> pos++
            else -> error("Bad argument syntax")
        }
        skipWhitespace()
        val type = readIdentifier()
        if (type.isEmpty()) error("Bad argument syntax")
        skipWhitespace()
        when (pattern.getOrNull(pos)) {
            '}' -> {
                if (type in COMPLEX_TYPES) error("No style field for complex argument")
                pos++
                return Element.Argument(name, type, null, 0.0, emptyMap())
            }
            ',' -> pos++
            else -> error("Bad argument syntax")
        }
        if (type !in COMPLEX_TYPES) {
            val style = readStyle()
            return Element.Argument(name, type, style.trim(), 0.0, emptyMap())
        }
        return parseOptions(name, type, nesting)
    }

    private fun parseOptions(name: String, type: String, nesting: Int): Element.Argument {
        val plural = type != "select"
        var offset = 0.0
        val options = LinkedHashMap<String, Message>()
        while (true) {
            skipWhitespace()
            if (pos >= pattern.length) error("Unmatched '{' braces")
            if (pattern[pos] == '}') {
                pos++
                break
            }
            val selector = if (plural && pattern[pos] == '=') {
                pos++
                "=" + readNumber()
            } else {
                readIdentifier()
            }
            if (selector.isEmpty()) error("Bad ${type} pattern syntax")
            if (plural && selector == "offset" && pattern.getOrNull(pos) == ':') {
                if (options.isNotEmpty()) error("Plural argument 'offset:' must precede key-message pairs")
                pos++
                skipWhitespace()
                offset = readNumber().toDoubleOrNull() ?: error("Bad plural offset")
                continue
            }
            skipWhitespace()
            if (pattern.getOrNull(pos) != '{') error("No message fragment after $type selector")
            pos++
            val message = parseMessage(nesting, inPlural = plural)
            pos++ // '}'
            if (selector in options) error("Duplicate $type selector '$selector'")
            options[selector] = message
        }
        if ("other" !in options) error("Missing 'other' keyword in $type pattern")
        return Element.Argument(name, type, null, offset, options)
    }

    /** The style of a simple argument, up to the closing '}', which is consumed. */
    private fun readStyle(): String {
        val start = pos
        var depth = 0
        while (pos < pattern.length) {
            when (pattern[pos]) {
                '\'' -> {
                    val end = pattern.indexOf('\'', pos + 1)
                    if (end < 0) error("Quoted literal argument style text reaches to the end of the message")
                    pos = end
                }
                '{' -> depth++
                '}' -> {
                    if (depth == 0) {
                        val style = pattern.substring(start, pos)
                        pos++
                        return style
                    }
                    depth--
                }
            }
            pos++
        }
        error("Unmatched '{' braces")
    }

    private fun readIdentifier(): String {
        val start = pos
        while (pos < pattern.length && (pattern[pos].isLetterOrDigit() || pattern[pos] == '_' || pattern[pos] == '-')) pos++
        return pattern.substring(start, pos)
    }

    private fun readNumber(): String {
        val start = pos
        while (pos < pattern.length && (pattern[pos].isDigit() || pattern[pos] in "+-.eE")) pos++
        return pattern.substring(start, pos)
    }

    private fun skipWhitespace() {
        while (pos < pattern.length && pattern[pos].isWhitespace()) pos++
    }

    private fun error(reason: String): Nothing =
        throw IllegalArgumentException("$reason at pattern index $pos in \"$pattern\"")

    private companion object {
        val COMPLEX_TYPES = setOf("plural", "select", "selectordinal", "choice")
    }
}
