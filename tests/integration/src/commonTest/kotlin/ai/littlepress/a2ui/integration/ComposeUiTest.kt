package ai.littlepress.a2ui.integration

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.compose.ui.toJsonSchemaString
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlin.test.Test
import kotlinx.serialization.json.Json
import kotlin.test.assertEquals
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/**
 * False on iOS: upstream builds schemas in HashMaps, which iterate in Java's hash order on the JVM
 * and in insertion order on Kotlin/Native, so iOS writes the same JSON with keys in another order.
 */
expect val jsonKeyOrderMatchesAndroid: Boolean

/** compose-ui's catalog and message processor, the way an app defines its own component. */
class ComposeUiTest {
    private object Label : A2uiComponent {
        override val name = "Label"
        override val description = "Shows a line of text"
        override val properties = listOf(
            A2uiProperty.string("text", required = true, description = "The text to show"),
            A2uiProperty.string("variant", description = "Text style"),
            A2uiProperty.boolean("bold"),
            A2uiProperty.componentId("icon", description = "An icon before the text"),
            A2uiProperty.any("data"),
            A2uiProperty.action("onTap"),
        )

        @Composable
        override fun A2uiComponentScope.Content(properties: A2uiComponentProperties, modifier: Modifier) {}
    }

    private val catalog = A2uiCatalog("https://example.com/consumer-catalog.json", listOf(Label))

    @Test
    fun catalogSchemaIsTheSameEverywhere() {
        val schema = catalog.toJsonSchemaString()
        println("A2UI-SCHEMA ${platformName()} | $schema")
        if (jsonKeyOrderMatchesAndroid) {
            assertEquals(ANDROID_CATALOG_SCHEMA, schema)
        } else {
            // Known difference: the same JSON, with object keys in another order.
            assertEquals(Json.parseToJsonElement(ANDROID_CATALOG_SCHEMA), Json.parseToJsonElement(schema))
        }
    }

    @Test
    fun processesMessages() = runTest {
        if (!canParseJson) return@runTest
        val parser = A2uiMessageParser()
        val processor = A2uiMessageProcessor(listOf(catalog))
        val collecting = launch { processor.collectMessages() }
        processor.processMessage(parser.parse(
            """{"version": "v0.9", "createSurface": {"surfaceId": "main", "catalogId": "${catalog.id}"}}"""
        ))
        processor.processMessage(parser.parse(
            """{"version": "v0.9", "updateComponents": {"surfaceId": "main", "components": [
                {"id": "root", "component": "Label", "text": "Hello"}]}}"""
        ))
        advanceUntilIdle()
        assertEquals(listOf("main"), processor.activeSurfaces.value.map { it.id })
        collecting.cancel()
    }
}
