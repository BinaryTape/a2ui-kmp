package ai.littlepress.a2ui.integration

import androidx.a2ui.compose.ui.testing.A2uiComponentPayload
import kotlin.test.Test
import kotlin.test.assertEquals

/** compose-ui-testing resolves and links from its published artifacts (upstream's own tests run in kmp/). */
class ComposeUiTestingTest {
    @Test
    fun idStubPayload() {
        val payload = A2uiComponentPayload(id = "submit_button", properties = mapOf("label" to "Send"))
        assertEquals("submit_button", payload.id)
        assertEquals(mapOf("label" to "Send"), payload.properties)
    }
}
