package ai.littlepress.a2ui.swiftui

import androidx.a2ui.engine.catalog.*
import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.a2ui.engine.platform.*
import androidx.a2ui.engine.processor.A2uiCoreMessageProcessor
import androidx.a2ui.model.catalog.A2uiFunctionCollection
import androidx.a2ui.model.processor.processInput
import androidx.a2ui.model.protocol.*
import androidx.a2ui.model.schema.A2uiAnySchema
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

private const val CATALOG = "https://example.org/a2ui/swiftui-slice"
private class Definition(override val name: String) : A2uiCoreComponentDefinition {
    override val description = "SwiftUI slice component"
    override val propertySchema = A2uiAnySchema()
}
private class Catalog(override val id: String) : A2uiCoreCatalog {
    override val componentDefinitions = A2uiCoreComponentDefinitionCollection(
        listOf("Column", "Card", "Text", "Button", "TextField", "ChoicePicker", "Panel", "Heading", "Note", "Action").map(::Definition))
    override val functions = A2uiFunctionCollection()
}

/** The sample supports top-level strings and lists; unsupported data shapes are rejected. */
private class Data(private val changed: () -> Unit) : A2uiCoreDataModel {
    private val values = mutableMapOf<String, Any?>()
    override fun update(path: A2uiDataPath, value: Any?) {
        require(path.segments.size == 1 && (value == null || value is String || value is List<*>))
        if (value == null) values.remove(path.segments[0]) else values[path.segments[0]] = value
        changed()
    }
    override fun get(path: A2uiDataPath): Any? = path.segments.singleOrNull()?.let(values::get)
    override fun close() { values.clear() }
}
private class Registry(private val changed: () -> Unit) : A2uiCoreComponentRegistry {
    val components = mutableMapOf<String, A2uiComponentPayload>()
    override fun update(components: List<A2uiComponentPayload>) {
        components.forEach { this.components[it.id] = it }; changed()
    }
    override fun reportError(id: String, exception: A2uiException) { components.remove(id); changed() }
    override fun close() { components.clear() }
}

/** Swift-friendly immutable render node. */
class SwiftNode(val id: String, val type: String, val text: String, val variant: String, val value: String, val path: String, val options: List<String>, val children: List<SwiftNode>)
class SwiftEvent(
    val type: String, val surfaceId: String, val componentId: String,
    val timestamp: Long, val context: String, val clientDataModel: String?,
)

private fun jsonValue(value: Any?): JsonElement = when (value) {
    null -> JsonNull
    is String -> JsonPrimitive(value)
    is Boolean -> JsonPrimitive(value)
    is Number -> JsonPrimitive(value)
    is List<*> -> JsonArray(value.map(::jsonValue))
    is Map<*, *> -> JsonObject(value.entries.associate { (key, item) ->
        require(key is String) { "JSON object keys must be strings" }
        key to jsonValue(item)
    })
    else -> error("Unsupported JSON value: ${value::class}")
}

/** One instance per visible screen. All calls and callbacks must be on the main thread. */
class SwiftBridge {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val changes = MutableStateFlow(0)
    private var closed = false
    private val processor = A2uiCoreMessageProcessor(
        listOf(Catalog(CATALOG), Catalog("https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json"), Catalog("https://example.com/catalogs/coffee/v1")), { Data { changes.value++ } }, { Registry { changes.value++ } })
    private val parser = A2uiMessageParser()
    private var onRender: ((SwiftNode?) -> Unit)? = null
    private var onEvent: ((SwiftEvent) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    fun start(render: (SwiftNode?) -> Unit, event: (SwiftEvent) -> Unit, error: (String) -> Unit) {
        check(!closed && onRender == null) { "Bridge is already started or closed" }
        onRender = render; onEvent = event; onError = error
        scope.launch { processor.collectMessages() }
        scope.launch { processor.outboundEvents.collect { message ->
            when (message) {
                is A2uiClientEventMessage -> onEvent?.invoke(SwiftEvent(message.type, message.surfaceId, message.componentId, message.timestamp, jsonValue(message.context).toString(), message.clientDataModel?.let { jsonValue(it.toPayloadMap()).toString() }))
                is A2uiClientErrorMessage -> onError?.invoke("${message.code}: ${message.message}")
            }
        } }
        scope.launch { combine(processor.activeSurfaces, changes) { surfaces, _ -> surfaces.firstOrNull() }
            .collect { onRender?.invoke((it as? A2uiCoreSurfaceModel)?.let(::snapshot)) } }
    }
    fun send(json: String) { check(!closed); processor.processInput(parser, json) }
    fun startSample(name: String) {
        check(!closed && onRender != null)
        scope.launch {
            val messages = when (name) {
                "Hello" -> sample.material3.AgentMessages.hello.map { 0L to it }
                "Form" -> sample.material3.AgentMessages.form.map { 0L to it }
                "Updates" -> sample.material3.AgentMessages.updates
                "Custom" -> sample.custom.AgentMessages.order
                else -> error("Unknown sample: $name")
            }
            messages.forEach { (wait, json) -> delay(wait); send(json) }
        }
    }
    fun setString(surfaceId: String, path: String, value: String) {
        val surface = processor.activeSurfaces.value.find { it.id == surfaceId } as? A2uiCoreSurfaceModel ?: return
        surface.dataModel.update(A2uiDataPath(path), value)
    }
    fun setChoice(surfaceId: String, path: String, value: String) {
        val surface = processor.activeSurfaces.value.find { it.id == surfaceId } as? A2uiCoreSurfaceModel ?: return
        surface.dataModel.update(A2uiDataPath(path), listOf(value))
    }
    fun tap(surfaceId: String, componentId: String) {
        if (closed) return
        val surface = processor.activeSurfaces.value.find { it.id == surfaceId } as? A2uiCoreSurfaceModel ?: return
        val component = (surface.componentRegistry as Registry).components[componentId] ?: return
        @Suppress("UNCHECKED_CAST")
        val action = component.properties["action"] as? Map<String, Any?> ?: return
        surface.dispatchAction(componentId, action)
    }
    fun close() {
        if (closed) return
        closed = true; onRender = null; onEvent = null; onError = null; scope.cancel()
    }
    private fun snapshot(surface: A2uiCoreSurfaceModel): SwiftNode? {
        val registry = surface.componentRegistry as Registry
        fun node(id: String, seen: Set<String>): SwiftNode? {
            if (id in seen) return null
            val c = registry.components[id] ?: return null
            val p = c.properties
            val childIds = when (c.type) {
                "Column", "Panel" -> (p["children"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                "Button", "Card" -> listOfNotNull(p["child"] as? String)
                else -> emptyList()
            }
            val raw = p["text"] ?: p["label"]
            val text = when (raw) {
                is String -> raw
                is Map<*, *> -> (raw["path"] as? String)?.let { surface.dataModel[A2uiDataPath(it)] as? String } ?: ""
                else -> ""
            }
            val path = (p["value"] as? Map<*, *>)?.get("path") as? String ?: ""
            val value = if (path.isEmpty()) null else surface.dataModel[A2uiDataPath(path)]
            val valueText = when (value) { is String -> value; is List<*> -> value.firstOrNull() as? String ?: ""; else -> "" }
            val options = (p["options"] as? List<*>)?.mapNotNull { (it as? Map<*, *>)?.get("value") as? String } ?: emptyList()
            return SwiftNode(c.id, c.type, text, p["variant"] as? String ?: "", valueText, path, options, childIds.mapNotNull { node(it, seen + id) })
        }
        return node("root", emptySet())
    }
}
