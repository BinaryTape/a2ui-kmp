package sample.custom

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiComponentState
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.runtime.observeA2uiComponentState
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The components this app lets its agent use, drawn in the app's own style. The catalog id is what
 * the agent names in createSurface; the names and properties are what it may send.
 */
val MyCatalog: A2uiCatalog = A2uiCatalog(
    catalogId = "https://example.com/catalogs/coffee/v1",
    components = listOf(Panel, Heading, Note, Action),
)

private val Ink = Color(0xFF2B2118)
private val Paper = Color(0xFFF6EFE6)
private val Accent = Color(0xFF8A4B2A)

/** A column of other components on a card. */
object Panel : A2uiComponent {
    private val children = A2uiProperty.childList("children", required = true)
    override val name = "Panel"
    override val description = "A card with its children one below the other."
    override val properties = listOf(children)

    @Composable
    override fun A2uiComponentScope.Content(properties: A2uiComponentProperties, modifier: Modifier) {
        Column(
            modifier
                .background(Paper, RoundedCornerShape(16.dp))
                .border(1.dp, Accent.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            properties.bindChildReferences(children)?.forEach { child ->
                // Each child is another component of this catalog, drawn by its own Content.
                when (val state = observeA2uiComponentState(child)) {
                    is A2uiComponentState.Success -> androidx.a2ui.compose.ui.A2uiComponent(state.component)
                    is A2uiComponentState.Loading -> {}
                    is A2uiComponentState.Error -> BasicText("Could not show ${child.id}")
                }
            }
        }
    }
}

/** A title. Its text may be bound to the data model. */
object Heading : A2uiComponent {
    private val text = A2uiProperty.dynamicString("text", required = true)
    override val name = "Heading"
    override val description = "A title."
    override val properties = listOf(text)

    @Composable
    override fun A2uiComponentScope.Content(properties: A2uiComponentProperties, modifier: Modifier) {
        BasicText(properties.bind(text).orEmpty(), modifier, TextStyle(Ink, 24.sp, FontWeight.Bold))
    }
}

/** Body text. Its text may be bound to the data model. */
object Note : A2uiComponent {
    private val text = A2uiProperty.dynamicString("text", required = true)
    override val name = "Note"
    override val description = "Body text."
    override val properties = listOf(text)

    @Composable
    override fun A2uiComponentScope.Content(properties: A2uiComponentProperties, modifier: Modifier) {
        BasicText(properties.bind(text).orEmpty(), modifier, TextStyle(Ink.copy(alpha = 0.8f), 16.sp))
    }
}

/** A button whose action goes back to the agent (see the app's outboundEvents). */
object Action : A2uiComponent {
    private val label = A2uiProperty.dynamicString("label", required = true)
    private val action = A2uiProperty.action("action", required = true)
    override val name = "Action"
    override val description = "A button that sends its action to the agent."
    override val properties = listOf(label, action)

    @Composable
    override fun A2uiComponentScope.Content(properties: A2uiComponentProperties, modifier: Modifier) {
        val payload = properties[action]
        BasicText(
            properties.bind(label).orEmpty(),
            modifier
                .background(Accent, RoundedCornerShape(12.dp))
                .clickable(enabled = payload != null, role = Role.Button) { payload?.let(::dispatchAction) }
                .padding(horizontal = 20.dp, vertical = 12.dp),
            TextStyle(Color.White, 16.sp, FontWeight.Medium),
        )
    }
}
