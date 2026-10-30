package ai.littlepress.a2ui.swiftui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class SwiftBridgeTest {
    @Test fun messagesUpdatesActionsAndClose() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val bridge = SwiftBridge()
            val renders = mutableListOf<SwiftNode?>()
            val events = mutableListOf<SwiftEvent>()
            val errors = mutableListOf<String>()
            bridge.start(renders::add, events::add, errors::add)
            runCurrent()
            bridge.send("""{"version":"v0.9","createSurface":{"surfaceId":"demo","catalogId":"https://example.org/a2ui/swiftui-slice"}}""")
            bridge.send("""{"version":"v0.9","updateDataModel":{"surfaceId":"demo","path":"/status","value":"Ready"}}""")
            bridge.send("""{"version":"v0.9","updateComponents":{"surfaceId":"demo","components":[{"id":"root","component":"Column","children":["status","button"]},{"id":"status","component":"Text","text":{"path":"/status"}},{"id":"button","component":"Button","child":"label","action":{"event":{"name":"confirm"}}},{"id":"label","component":"Text","text":"Confirm"}]}}""")
            advanceUntilIdle()
            assertEquals("Ready", renders.last()!!.children.first().text)
            bridge.send("""{"version":"v0.9","updateDataModel":{"surfaceId":"demo","path":"/status","value":"Updated"}}""")
            advanceUntilIdle()
            assertEquals("Updated", renders.last()!!.children.first().text)
            bridge.tap("demo", "button")
            advanceUntilIdle()
            assertEquals("confirm", events.single().type)
            assertEquals("button", events.single().componentId)
            assertTrue(errors.isEmpty(), errors.toString())
            bridge.close()
            val count = renders.size
            bridge.tap("demo", "button")
            advanceUntilIdle()
            assertEquals(count, renders.size)
            assertEquals(1, events.size)
            assertFailsWith<IllegalStateException> { bridge.send("{}") }
        } finally { Dispatchers.resetMain() }
    }
    @Test fun sampleFormAndCustomActionsResolveContext() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val bridge = SwiftBridge()
            val renders = mutableListOf<SwiftNode?>()
            val events = mutableListOf<SwiftEvent>()
            val errors = mutableListOf<String>()
            bridge.start(renders::add, events::add, errors::add)
            bridge.startSample("Form")
            advanceUntilIdle()
            bridge.setString("booking", "/guest", "Grace")
            bridge.setChoice("booking", "/party", "3")
            advanceUntilIdle()
            bridge.tap("booking", "submit")
            advanceUntilIdle()
            assertEquals("book_table", events.single().type)
            val context = Json.parseToJsonElement(events.single().context).jsonObject
            assertEquals("Grace", context.getValue("guest").jsonPrimitive.content)
            assertEquals("3", context.getValue("party").jsonArray.single().jsonPrimitive.content)
            assertTrue(events.single().timestamp > 0)
            assertTrue(errors.isEmpty(), errors.toString())
            bridge.close()
            advanceUntilIdle()
            val custom = SwiftBridge()
            val customEvents = mutableListOf<SwiftEvent>()
            custom.start({}, customEvents::add, errors::add)
            custom.startSample("Custom")
            advanceUntilIdle()
            custom.tap("order", "confirm")
            advanceUntilIdle()
            assertEquals("confirm_order", customEvents.single().type)
            assertTrue(customEvents.single().context.contains("flat_white"))
            assertTrue(errors.isEmpty(), errors.toString())
            custom.close()
            advanceUntilIdle()
        } finally { Dispatchers.resetMain() }
    }

}
