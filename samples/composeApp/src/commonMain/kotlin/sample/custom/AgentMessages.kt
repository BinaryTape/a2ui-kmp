package sample.custom

/**
 * What the agent sends: A2UI v0.9 JSON messages that use this app's catalog (MyCatalog) instead of
 * the basic one. Each message with the time to wait before it, in milliseconds.
 */
object AgentMessages {
    private const val CATALOG = "https://example.com/catalogs/coffee/v1"

    val order = listOf(
        0L to """{"version": "v0.9", "createSurface": {"surfaceId": "order", "catalogId": "$CATALOG"}}""",
        0L to """{"version": "v0.9", "updateDataModel": {"surfaceId": "order", "path": "/pickup", "value": "Ready in about 5 minutes"}}""",
        0L to """
        {"version": "v0.9", "updateComponents": {"surfaceId": "order", "components": [
          {"id": "root", "component": "Panel", "children": ["drink", "pickup", "confirm"]},
          {"id": "drink", "component": "Heading", "text": "Flat white, oat milk"},
          {"id": "pickup", "component": "Note", "text": {"path": "/pickup"}},
          {"id": "confirm", "component": "Action", "label": "Confirm order",
           "action": {"event": {"name": "confirm_order", "context": {"drink": "flat_white"}}}}
        ]}}
        """,
        2000L to """{"version": "v0.9", "updateDataModel": {"surfaceId": "order", "path": "/pickup", "value": "Ready now, at the counter"}}""",
    )
}
