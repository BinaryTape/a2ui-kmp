package ai.littlepress.a2ui.integration

import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.model.protocol.A2uiCreateSurfaceMessage
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Parses A2UI messages with compose-runtime's parser. On Android that is the official artifact on
 * android.util.JsonReader; elsewhere it is our JsonReader. Every platform has to give Android's
 * results ([ANDROID_JSON_RESULTS]), including the exception messages.
 */
class JsonParsingTest {
    private val parser = A2uiMessageParser()

    @Test
    fun parsesLikeAndroid() {
        if (!canParseJson) return
        val results = JSON_CASES.mapIndexed { index, json ->
            runCatching { (parser.parse(json) as A2uiCreateSurfaceMessage).theme }
                .fold({ "$it" }, { "${it::class.simpleName}: ${it.message}" })
                .replace("\n", "\\n")
                .also { println("A2UI-JSON ${platformName()} | $index | $it") }
        }
        assertEquals(ANDROID_JSON_RESULTS, results)
    }
}

/** False where the platform's JSON reader is unavailable. */
expect val canParseJson: Boolean

private fun surface(theme: String) =
    """{"version": "v0.9", "createSurface": {"surfaceId": "s", "catalogId": "c", "theme": $theme}}"""

internal val JSON_CASES: List<String> = listOf(
    surface("""{"a": 1, "b": -1.5, "c": 1e3, "d": 0, "e": 12345678901234, "f": 1.0}"""),
    surface("""{"s": "é\n\"\\\/\t", "t": true, "u": false, "n": null}"""),
    surface("""{"list": [1, "x", [true, null], {"k": "v"}], "o": {"p": {"q": []}}}"""),
    surface("""{"t": TRUE, "n": Null}"""),
    surface("""{"a": "A\u00"}"""),
    surface("""{"a": "\x"}"""),
    surface("""{"a": 01}"""),
    surface("""{"a": 1.}"""),
    surface("""{"a": NaN}"""),
    surface("""{"a": -}"""),
    surface("""{"a": 1,}"""),
    surface("""{"a": [1,]}"""),
    surface("""{'a': 1}"""),
    surface("""{a: 1}"""),
    surface("""{"a" = 1}"""),
    surface("""{"a": 1; "b": 2}"""),
    surface("""{"a": 1 /* c */}"""),
    surface("""{"a": "unterminated}"""),
    surface("""{"a": 1}""") + " x",
    surface("""{"a": 1}""") + " ",
    "",
    "   ",
    "{",
    "\"v0.9\"",
    "[]",
    """{"version": "v0.9"}""",
    "{\n  \"version\": \"v0.9\",\n  \"createSurface\": {\n    \"surfaceId\": oops\n  }\n}",
    "\uFEFF" + surface("""{"a": 1}"""),
    "\uFEFF" + surface("""{"a": 1,}"""),
    " \uFEFF" + surface("""{"a": 1}"""),
)
