package ai.littlepress.a2ui.compat.android.icu.text

import ai.littlepress.a2ui.compat.java.util.Locale
import ai.littlepress.a2ui.compat.java.util.country
import ai.littlepress.a2ui.compat.java.util.language

/**
 * CLDR cardinal plural rules ([CLDR_PLURAL_RULES], the CLDR version of Android 15's ICU), evaluated
 * for a number as MessageFormat formats it: at most three fraction digits, trailing zeros dropped.
 */
internal class PluralRules private constructor(private val rules: List<Pair<String, Condition>>) {

    fun select(number: Double): String {
        val operands = Operands.of(number)
        return rules.firstOrNull { (_, condition) -> condition.matches(operands) }?.first ?: "other"
    }

    companion object {
        private val cache = HashMap<String, PluralRules>()

        /** Cardinal rules, or with [ordinal] the rules for `selectordinal`. */
        fun forLocale(locale: Locale, ordinal: Boolean = false): PluralRules {
            val data = if (ordinal) CLDR_ORDINAL_RULES else CLDR_PLURAL_RULES
            val language = when (val l = locale.language) {
                "iw" -> "he"
                "in" -> "id"
                "ji" -> "yi"
                else -> l
            }
            val key = listOf("$language-${locale.country}", language).firstOrNull { it in data } ?: "root"
            return cache.getOrPut("$ordinal:$key") {
                val source = data[key] ?: emptyMap()
                PluralRules(source.filterKeys { it != "other" }.map { (category, rule) -> category to RuleParser(rule).parse() })
            }
        }
    }
}

/** CLDR operands: n absolute value, i integer digits, v/w and f/t fraction digits, c/e exponent (0). */
internal class Operands(val n: Double, val i: Double, val v: Int, val f: Long) {
    fun get(name: Char): Double = when (name) {
        'n' -> n
        'i' -> i
        'v', 'w' -> v.toDouble()
        'f', 't' -> f.toDouble()
        'c', 'e' -> 0.0
        else -> throw IllegalArgumentException("Unknown plural operand '$name'")
    }

    companion object {
        /** Rounds half-even to three fraction digits on the decimal digits of [number]. */
        fun of(number: Double): Operands {
            val plain = plainDecimal(kotlin.math.abs(number))
            val dot = plain.indexOf('.')
            var integer = if (dot < 0) plain else plain.substring(0, dot)
            var fraction = if (dot < 0) "" else plain.substring(dot + 1)
            if (fraction.length > 3) {
                // Only numbers below 10^16 have digits after the third decimal place.
                val kept = fraction.substring(0, 3)
                val rest = fraction.substring(3)
                val up = rest[0] > '5' ||
                    (rest[0] == '5' && (rest.drop(1).any { it != '0' } || (kept.last() - '0') % 2 == 1))
                var digits = (integer + kept).toLong()
                if (up) digits++
                integer = (digits / 1000).toString()
                fraction = (digits % 1000).toString().padStart(3, '0')
            }
            fraction = fraction.trimEnd('0')
            // Like ICU, i keeps only the lowest 18 digits of a larger integer part (1e19 has i = 0).
            val i = integer.takeLast(18).toDouble()
            val f = if (fraction.isEmpty()) 0L else fraction.toLong()
            return Operands(integer.toDouble() + f / tenPow(fraction.length), i, fraction.length, f)
        }

        private fun tenPow(n: Int): Double {
            var result = 1.0
            repeat(n) { result *= 10 }
            return result
        }

        /** The shortest decimal digits of [number] without an exponent. */
        private fun plainDecimal(number: Double): String {
            val s = number.toString()
            val e = s.indexOfFirst { it == 'E' || it == 'e' }
            if (e < 0) return s
            val mantissa = s.substring(0, e)
            val exponent = s.substring(e + 1).toInt()
            val dot = mantissa.indexOf('.')
            val digits = mantissa.replace(".", "")
            val point = (if (dot < 0) mantissa.length else dot) + exponent
            return when {
                point <= 0 -> "0." + "0".repeat(-point) + digits
                point >= digits.length -> digits + "0".repeat(point - digits.length)
                else -> digits.substring(0, point) + "." + digits.substring(point)
            }
        }
    }
}

internal fun interface Condition {
    fun matches(operands: Operands): Boolean
}

/** CLDR rule syntax: `or` of `and` of `operand [% m] (= | !=) values-and-ranges`. */
private class RuleParser(rule: String) {
    private val tokens = rule.substringBefore('@').trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        .flatMap { splitToken(it) }
    private var pos = 0

    fun parse(): Condition {
        if (tokens.isEmpty()) return Condition { true }
        val condition = parseOr()
        require(pos == tokens.size) { "Unexpected '${tokens[pos]}' in plural rule" }
        return condition
    }

    private fun parseOr(): Condition {
        val alternatives = mutableListOf(parseAnd())
        while (peek() == "or") {
            pos++
            alternatives += parseAnd()
        }
        return Condition { operands -> alternatives.any { it.matches(operands) } }
    }

    private fun parseAnd(): Condition {
        val relations = mutableListOf(parseRelation())
        while (peek() == "and") {
            pos++
            relations += parseRelation()
        }
        return Condition { operands -> relations.all { it.matches(operands) } }
    }

    private fun parseRelation(): Condition {
        val operand = next().single()
        var modulus: Double? = null
        if (peek() == "%") {
            pos++
            modulus = next().toDouble()
        }
        val negate = when (next()) {
            "=" -> false
            "!=" -> true
            else -> throw IllegalArgumentException("Expected = or != in plural rule")
        }
        val ranges = mutableListOf<ClosedFloatingPointRange<Double>>()
        do {
            if (peek() == ",") pos++
            val value = next()
            ranges += if (".." in value) {
                value.substringBefore("..").toDouble()..value.substringAfter("..").toDouble()
            } else {
                value.toDouble()..value.toDouble()
            }
        } while (peek() == ",")
        return Condition { operands ->
            var x = operands.get(operand)
            if (modulus != null) x %= modulus
            // Ranges and values only match integers.
            val inRange = x == kotlin.math.floor(x) && ranges.any { x in it }
            inRange != negate
        }
    }

    private fun peek(): String? = tokens.getOrNull(pos)

    private fun next(): String = tokens.getOrNull(pos++) ?: throw IllegalArgumentException("Incomplete plural rule")

    private companion object {
        /** Splits "n%100=3..10," style tokens; CLDR separates most tokens with spaces already. */
        fun splitToken(token: String): List<String> =
            Regex("!=|=|%|,|[^!=%,]+").findAll(token).map { it.value }.toList()
    }
}
