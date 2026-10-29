package ai.littlepress.a2ui.integration

import androidx.a2ui.engine.catalog.A2uiCoreCatalog
import androidx.a2ui.engine.catalog.A2uiCoreComponentDefinition
import androidx.a2ui.engine.catalog.A2uiCoreComponentDefinitionCollection
import androidx.a2ui.engine.platform.A2uiCoreComponentRegistry
import androidx.a2ui.engine.platform.A2uiCoreDataModel
import androidx.a2ui.engine.processor.A2uiCoreMessageProcessor
import androidx.a2ui.model.catalog.A2uiFunctionCollection
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiCreateSurfaceMessage
import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.a2ui.model.protocol.A2uiDeleteSurfaceMessage
import androidx.a2ui.model.protocol.A2uiException
import androidx.a2ui.model.protocol.A2uiUpdateComponentsMessage
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/** The engine's message processing, the way a renderer drives it, on every target. */
class EngineTest {
    private val registries = mutableListOf<RecordingRegistry>()
    private val processor = A2uiCoreMessageProcessor(
        catalogs = listOf(ButtonCatalog),
        dataModelFactory = { NoDataModel() },
        componentRegistryFactory = { RecordingRegistry().also { registries += it } },
    )

    @Test
    fun surfaceLifecycle() = runTest {
        val collecting = launch { processor.collectMessages() }
        processor.processMessage(A2uiCreateSurfaceMessage("main", ButtonCatalog.id))
        advanceUntilIdle()
        assertEquals(listOf("main"), processor.activeSurfaces.value.map { it.id })

        val button = A2uiComponentPayload("ok", "button", mapOf("label" to "OK"))
        processor.processMessage(A2uiUpdateComponentsMessage("main", listOf(button)))
        advanceUntilIdle()
        assertEquals(listOf(button), registries.single().updates)

        processor.processMessage(A2uiDeleteSurfaceMessage("main"))
        advanceUntilIdle()
        assertEquals(emptyList(), processor.activeSurfaces.value)
        assertEquals(true, registries.single().closed)
        collecting.cancel()
    }

    private object ButtonCatalog : A2uiCoreCatalog {
        override val id = "consumer-catalog"
        override val isInline = false
        override val componentDefinitions = A2uiCoreComponentDefinitionCollection(
            listOf(
                object : A2uiCoreComponentDefinition {
                    override val name = "button"
                    override val description = "A button"
                    override val propertySchema = A2uiObjectSchema()
                }
            )
        )
        override val functions = A2uiFunctionCollection()
        override val themeSchema: A2uiSchema? = null
    }

    private class NoDataModel : A2uiCoreDataModel {
        override fun update(path: A2uiDataPath, value: Any?) {}

        override fun get(path: A2uiDataPath): Any? = null

        override fun close() {}
    }

    private class RecordingRegistry : A2uiCoreComponentRegistry {
        val updates = mutableListOf<A2uiComponentPayload>()
        var closed = false

        override fun update(components: List<A2uiComponentPayload>) {
            updates += components
        }

        override fun reportError(id: String, exception: A2uiException) {}

        override fun close() {
            closed = true
        }
    }
}
