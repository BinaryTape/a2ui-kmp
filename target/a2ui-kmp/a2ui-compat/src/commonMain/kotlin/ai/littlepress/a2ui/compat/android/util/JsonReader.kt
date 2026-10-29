/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// a2ui-kmp: a Kotlin version of the state machine of android.util.JsonReader, limited to what
// upstream A2UI uses, for desktop and iOS.
package ai.littlepress.a2ui.compat.android.util

import ai.littlepress.a2ui.compat.java.io.EOFException
import ai.littlepress.a2ui.compat.java.io.IOException
import ai.littlepress.a2ui.compat.java.io.StringReader

/** android.util.JsonToken. */
public enum class JsonToken {
    BEGIN_ARRAY,
    END_ARRAY,
    BEGIN_OBJECT,
    END_OBJECT,
    NAME,
    STRING,
    NUMBER,
    BOOLEAN,
    NULL,
    END_DOCUMENT,
}

/** android.util.MalformedJsonException. */
public class MalformedJsonException(message: String) : IOException(message)

/**
 * The part of android.util.JsonReader that upstream uses, in its default strict mode, with the same
 * state machine as Android's: the document must be an object or an array; names must be quoted;
 * true, false and null match regardless of case; other unquoted values must be JSON numbers;
 * nextString() also returns numbers; nextInt(), nextLong() and nextDouble() also parse strings.
 * Syntax errors throw [MalformedJsonException] or [EOFException] (both IOExceptions), and calls that
 * do not match the next token throw IllegalStateException without consuming it.
 */
public class JsonReader(reader: StringReader) {
    private val input = reader.text

    /** Like Android, skips a byte order mark at the start; error columns do not count it. */
    private val bom = if (input.startsWith('\uFEFF')) 1 else 0
    private var pos = bom

    private val stack = mutableListOf(Scope.EMPTY_DOCUMENT)

    /** The current token, or null when it has not been read yet. */
    private var token: JsonToken? = null

    /** The text of the current NAME token. */
    private var name: String? = null

    /** The text of the current STRING, NUMBER or BOOLEAN token. */
    private var value: String? = null

    private enum class Scope {
        EMPTY_ARRAY,
        NONEMPTY_ARRAY,
        EMPTY_OBJECT,
        DANGLING_NAME,
        NONEMPTY_OBJECT,
        EMPTY_DOCUMENT,
        NONEMPTY_DOCUMENT,
        CLOSED,
    }

    public fun beginArray() {
        expect(JsonToken.BEGIN_ARRAY)
    }

    public fun endArray() {
        expect(JsonToken.END_ARRAY)
    }

    public fun beginObject() {
        expect(JsonToken.BEGIN_OBJECT)
    }

    public fun endObject() {
        expect(JsonToken.END_OBJECT)
    }

    private fun expect(expected: JsonToken) {
        peek()
        if (token != expected) throw IllegalStateException("Expected $expected but was ${peek()}")
        advance()
    }

    public fun hasNext(): Boolean {
        peek()
        return token != JsonToken.END_OBJECT && token != JsonToken.END_ARRAY
    }

    public fun peek(): JsonToken {
        token?.let { return it }
        return when (stack.last()) {
            Scope.EMPTY_DOCUMENT -> {
                replaceTop(Scope.NONEMPTY_DOCUMENT)
                val first = nextValue()
                if (first != JsonToken.BEGIN_ARRAY && first != JsonToken.BEGIN_OBJECT) {
                    throw IOException("Expected JSON document to start with '[' or '{' but was $first")
                }
                first
            }
            Scope.EMPTY_ARRAY -> nextInArray(firstElement = true)
            Scope.NONEMPTY_ARRAY -> nextInArray(firstElement = false)
            Scope.EMPTY_OBJECT -> nextInObject(firstElement = true)
            Scope.DANGLING_NAME -> objectValue()
            Scope.NONEMPTY_OBJECT -> nextInObject(firstElement = false)
            Scope.NONEMPTY_DOCUMENT ->
                try {
                    nextValue()
                    throw syntaxError("Expected EOF")
                } catch (e: EOFException) {
                    JsonToken.END_DOCUMENT.also { token = it }
                }
            Scope.CLOSED -> throw IllegalStateException("JsonReader is closed")
        }
    }

    private fun advance(): JsonToken {
        val result = peek()
        token = null
        value = null
        name = null
        return result
    }

    public fun nextName(): String {
        peek()
        if (token != JsonToken.NAME) throw IllegalStateException("Expected a name but was ${peek()}")
        val result = name!!
        advance()
        return result
    }

    public fun nextString(): String {
        peek()
        if (token != JsonToken.STRING && token != JsonToken.NUMBER) {
            throw IllegalStateException("Expected a string but was ${peek()}")
        }
        val result = value!!
        advance()
        return result
    }

    public fun nextBoolean(): Boolean {
        peek()
        if (token != JsonToken.BOOLEAN) throw IllegalStateException("Expected a boolean but was $token")
        val result = value == TRUE
        advance()
        return result
    }

    public fun nextNull() {
        peek()
        if (token != JsonToken.NULL) throw IllegalStateException("Expected null but was $token")
        advance()
    }

    public fun nextDouble(): Double {
        val text = numberText("a double")
        val result = text.toDouble()
        if (result >= 1.0 && text.startsWith("0")) {
            throw NumberFormatException("JSON forbids octal prefixes: $text")
        }
        if (result.isNaN() || result.isInfinite()) {
            throw NumberFormatException("JSON forbids NaN and infinities: $text")
        }
        advance()
        return result
    }

    public fun nextLong(): Long {
        val text = numberText("a long")
        val result = text.toLongOrNull() ?: run {
            val asDouble = text.toDouble()
            val asLong = asDouble.toLong()
            if (asLong.toDouble() != asDouble) throw NumberFormatException(text)
            asLong
        }
        if (result >= 1L && text.startsWith("0")) {
            throw NumberFormatException("JSON forbids octal prefixes: $text")
        }
        advance()
        return result
    }

    public fun nextInt(): Int {
        val text = numberText("an int")
        val result = text.toIntOrNull() ?: run {
            val asDouble = text.toDouble()
            val asInt = asDouble.toInt()
            if (asInt.toDouble() != asDouble) throw NumberFormatException(text)
            asInt
        }
        if (result >= 1 && text.startsWith("0")) {
            throw NumberFormatException("JSON forbids octal prefixes: $text")
        }
        advance()
        return result
    }

    private fun numberText(expected: String): String {
        peek()
        if (token != JsonToken.STRING && token != JsonToken.NUMBER) {
            throw IllegalStateException("Expected $expected but was ${peek()}")
        }
        return value!!
    }

    public fun close() {
        value = null
        token = null
        stack.clear()
        stack += Scope.CLOSED
    }

    /** Skips the next value; on a name, skips only the name, as Android does. */
    public fun skipValue() {
        if (!hasNext() || peek() == JsonToken.END_DOCUMENT) {
            throw IllegalStateException("No element left to skip")
        }
        var count = 0
        do {
            when (advance()) {
                JsonToken.BEGIN_ARRAY, JsonToken.BEGIN_OBJECT -> count++
                JsonToken.END_ARRAY, JsonToken.END_OBJECT -> count--
                else -> {}
            }
        } while (count != 0)
    }

    private fun replaceTop(scope: Scope) {
        stack[stack.lastIndex] = scope
    }

    private fun nextInArray(firstElement: Boolean): JsonToken {
        if (firstElement) {
            replaceTop(Scope.NONEMPTY_ARRAY)
        } else {
            when (nextNonWhitespace()) {
                ']' -> {
                    stack.removeAt(stack.lastIndex)
                    return JsonToken.END_ARRAY.also { token = it }
                }
                ';' -> checkLenient()
                ',' -> {}
                else -> throw syntaxError("Unterminated array")
            }
        }
        val c = nextNonWhitespace()
        if (c == ']' && firstElement) {
            stack.removeAt(stack.lastIndex)
            return JsonToken.END_ARRAY.also { token = it }
        }
        if (c == ']' || c == ';' || c == ',') {
            // An omitted value, which only lenient mode allows.
            checkLenient()
        }
        pos--
        return nextValue()
    }

    private fun nextInObject(firstElement: Boolean): JsonToken {
        if (firstElement) {
            if (nextNonWhitespace() == '}') {
                stack.removeAt(stack.lastIndex)
                return JsonToken.END_OBJECT.also { token = it }
            }
            pos--
        } else {
            when (nextNonWhitespace()) {
                '}' -> {
                    stack.removeAt(stack.lastIndex)
                    return JsonToken.END_OBJECT.also { token = it }
                }
                ';', ',' -> {}
                else -> throw syntaxError("Unterminated object")
            }
        }
        when (val quote = nextNonWhitespace()) {
            '\'' -> {
                checkLenient()
                name = readString(quote)
            }
            '"' -> name = readString(quote)
            else -> {
                checkLenient()
                pos--
                name = readLiteralText()
                if (name!!.isEmpty()) throw syntaxError("Expected name")
            }
        }
        replaceTop(Scope.DANGLING_NAME)
        return JsonToken.NAME.also { token = it }
    }

    private fun objectValue(): JsonToken {
        when (nextNonWhitespace()) {
            ':' -> {}
            '=' -> {
                checkLenient()
                if (pos < input.length && input[pos] == '>') pos++
            }
            else -> throw syntaxError("Expected ':'")
        }
        replaceTop(Scope.NONEMPTY_OBJECT)
        return nextValue()
    }

    private fun nextValue(): JsonToken {
        val c = nextNonWhitespace()
        return when (c) {
            '{' -> {
                stack += Scope.EMPTY_OBJECT
                JsonToken.BEGIN_OBJECT.also { token = it }
            }
            '[' -> {
                stack += Scope.EMPTY_ARRAY
                JsonToken.BEGIN_ARRAY.also { token = it }
            }
            '\'' -> {
                checkLenient()
                value = readString(c)
                JsonToken.STRING.also { token = it }
            }
            '"' -> {
                value = readString(c)
                JsonToken.STRING.also { token = it }
            }
            else -> {
                pos--
                readLiteral()
            }
        }
    }

    private fun nextNonWhitespace(): Char {
        while (pos < input.length) {
            val c = input[pos++]
            when (c) {
                '\t', ' ', '\n', '\r' -> continue
                // Comments are lenient-only.
                '/', '#' -> checkLenient()
            }
            return c
        }
        throw EOFException("End of input")
    }

    private fun checkLenient() {
        throw syntaxError("Use JsonReader.setLenient(true) to accept malformed JSON")
    }

    /** Reads a string up to the closing [quote], decoding escapes; the opening quote is consumed. */
    private fun readString(quote: Char): String {
        val builder = StringBuilder()
        while (pos < input.length) {
            val c = input[pos++]
            when (c) {
                quote -> return builder.toString()
                '\\' -> builder.append(readEscapeCharacter())
                else -> builder.append(c)
            }
        }
        throw syntaxError("Unterminated string")
    }

    private fun readEscapeCharacter(): Char {
        if (pos >= input.length) throw syntaxError("Unterminated escape sequence")
        return when (val escaped = input[pos++]) {
            'u' -> {
                if (pos + 4 > input.length) throw syntaxError("Unterminated escape sequence")
                val hex = input.substring(pos, pos + 4)
                pos += 4
                // Integer.parseInt(hex, 16) on Android, with its message.
                (hex.toIntOrNull(16) ?: throw NumberFormatException("For input string: \"$hex\" under radix 16"))
                    .toChar()
            }
            't' -> '\t'
            'b' -> '\b'
            'n' -> '\n'
            'r' -> '\r'
            'f' -> '\u000C'
            // '\'', '"', '\\' and, as on Android, any other character stand for themselves.
            else -> escaped
        }
    }

    /** Reads an unquoted name or value up to the next delimiter. */
    private fun readLiteralText(): String {
        val start = pos
        while (pos < input.length) {
            when (input[pos]) {
                '/', '\\', ';', '#', '=' -> {
                    // Android reports this at the start of the literal.
                    pos = start
                    checkLenient()
                }
                '{', '}', '[', ']', ':', ',', ' ', '\t', '\u000C', '\r', '\n' -> break
            }
            pos++
        }
        return input.substring(start, pos)
    }

    private fun readLiteral(): JsonToken {
        val text = readLiteralText()
        if (text.isEmpty()) throw syntaxError("Expected literal value")
        val result = when {
            text.equals("null", ignoreCase = true) -> JsonToken.NULL
            text.equals("true", ignoreCase = true) -> {
                value = TRUE
                JsonToken.BOOLEAN
            }
            text.equals("false", ignoreCase = true) -> {
                value = FALSE
                JsonToken.BOOLEAN
            }
            else -> {
                value = text
                if (isJsonNumber(text)) JsonToken.NUMBER else JsonToken.STRING
            }
        }
        token = result
        // An unquoted string is lenient-only.
        if (result == JsonToken.STRING) checkLenient()
        return result
    }

    private fun syntaxError(message: String): IOException {
        var line = 1
        var lineStart = bom
        for (i in 0 until minOf(pos, input.length)) {
            if (input[i] == '\n') {
                line++
                lineStart = i + 1
            }
        }
        return MalformedJsonException("$message at line $line column ${pos - lineStart + 1}")
    }

    private companion object {
        const val TRUE = "true"
        const val FALSE = "false"

        /** JSON's number grammar: -?(0|[1-9][0-9]*)(\.[0-9]*)?([eE][+-]?[0-9]+)? */
        fun isJsonNumber(text: String): Boolean {
            var i = 0
            fun c() = if (i < text.length) text[i] else '\u0000'
            if (c() == '-') i++
            when (c()) {
                '0' -> i++
                in '1'..'9' -> {
                    i++
                    while (c() in '0'..'9') i++
                }
                else -> return false
            }
            if (c() == '.') {
                i++
                while (c() in '0'..'9') i++
            }
            if (c() == 'e' || c() == 'E') {
                i++
                if (c() == '+' || c() == '-') i++
                if (c() !in '0'..'9') return false
                while (c() in '0'..'9') i++
            }
            return i == text.length
        }
    }
}
