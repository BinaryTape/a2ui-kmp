package sample.material3

/**
 * What the agent sends, as it arrives on the wire: A2UI v0.9 JSON messages. In an app these come
 * from your agent over SSE, a WebSocket or any other transport; here they are fixed.
 */
object AgentMessages {
    private const val CATALOG = "https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json"

    val hello = listOf(
        """{"version": "v0.9", "createSurface": {"surfaceId": "hello", "catalogId": "$CATALOG"}}""",
        """
        {"version": "v0.9", "updateComponents": {"surfaceId": "hello", "components": [
          {"id": "root", "component": "Card", "child": "content"},
          {"id": "content", "component": "Column", "children": ["title", "body"]},
          {"id": "title", "component": "Text", "text": "Hello from your agent", "variant": "h3"},
          {"id": "body", "component": "Text", "text": "This card is two JSON messages from the agent, drawn with Material 3."}
        ]}}
        """,
    )

    val form = listOf(
        """{"version": "v0.9", "createSurface": {"surfaceId": "booking", "catalogId": "$CATALOG"}}""",
        """{"version": "v0.9", "updateDataModel": {"surfaceId": "booking", "path": "/guest", "value": "Ada"}}""",
        """{"version": "v0.9", "updateDataModel": {"surfaceId": "booking", "path": "/party", "value": ["2"]}}""",
        """
        {"version": "v0.9", "updateComponents": {"surfaceId": "booking", "components": [
          {"id": "root", "component": "Card", "child": "content"},
          {"id": "content", "component": "Column", "children": ["title", "place", "name", "party", "submit"]},
          {"id": "title", "component": "Text", "text": "Book a table", "variant": "h3"},
          {"id": "place", "component": "Text", "text": "Trattoria Luigi, tonight at 19:30", "variant": "caption"},
          {"id": "name", "component": "TextField", "label": "Name", "value": {"path": "/guest"}},
          {"id": "party", "component": "ChoicePicker", "label": "Guests", "displayStyle": "chips",
           "options": [{"label": "1", "value": "1"}, {"label": "2", "value": "2"},
                       {"label": "3", "value": "3"}, {"label": "4", "value": "4"}],
           "value": {"path": "/party"}},
          {"id": "submit", "component": "Button", "child": "submit_label", "variant": "primary",
           "action": {"event": {"name": "book_table",
                                "context": {"guest": {"path": "/guest"}, "party": {"path": "/party"}}}}},
          {"id": "submit_label", "component": "Text", "text": "Book"}
        ]}}
        """,
    )

    /** Each message with the time to wait before it, in milliseconds. */
    val updates = listOf(
        0L to """{"version": "v0.9", "createSurface": {"surfaceId": "order", "catalogId": "$CATALOG"}}""",
        0L to """{"version": "v0.9", "updateDataModel": {"surfaceId": "order", "path": "/status", "value": "Order received"}}""",
        0L to """
        {"version": "v0.9", "updateComponents": {"surfaceId": "order", "components": [
          {"id": "root", "component": "Card", "child": "content"},
          {"id": "content", "component": "Column", "children": ["title", "status"]},
          {"id": "title", "component": "Text", "text": "Your order", "variant": "h3"},
          {"id": "status", "component": "Text", "text": {"path": "/status"}}
        ]}}
        """,
        1500L to """{"version": "v0.9", "updateDataModel": {"surfaceId": "order", "path": "/status", "value": "Being prepared"}}""",
        1500L to """{"version": "v0.9", "updateDataModel": {"surfaceId": "order", "path": "/status", "value": "On its way"}}""",
        0L to """
        {"version": "v0.9", "updateComponents": {"surfaceId": "order", "components": [
          {"id": "content", "component": "Column", "children": ["title", "status", "eta"]},
          {"id": "eta", "component": "Text", "text": "Arrives in about 10 minutes", "variant": "caption"}
        ]}}
        """,
    )
}
