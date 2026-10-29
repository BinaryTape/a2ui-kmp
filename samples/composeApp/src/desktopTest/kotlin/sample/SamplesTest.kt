package sample

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

/**
 * Runs the app as a user would: each tab, the form's button, the agent's updates. Each test saves a
 * screenshot to build/screenshots/ (for the README).
 */
@OptIn(ExperimentalTestApi::class)
class SamplesTest {
    @Test
    fun hello() = runApp("hello") {
        waitForText("Hello from your agent")
    }

    @Test
    fun formSendsTheEventToTheAgent() = runApp("form") {
        onNode(hasText("Form")).performClick()
        waitForText("Book")
        onNode(hasText("Book")).performClick()
        // The event the app would send to its agent, with the field's value in its context.
        waitForText("book_table from submit on booking: {guest=Ada, party=[2]}")
    }

    @Test
    fun updatesFollowTheAgent() = runApp("updates") {
        onNode(hasText("Updates")).performClick()
        waitForText("Order received")
        waitForText("Arrives in about 10 minutes")
        waitForText("On its way")
    }

    @Test
    fun customComponentsShowTheSurfaceAndSendTheAction() = runApp("custom") {
        onNode(hasText("Custom")).performClick()
        waitForText("Flat white, oat milk")
        waitForText("Ready now, at the counter")
        // Screen readers announce it as a button.
        onNode(hasText("Confirm order") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)).performClick()
        waitForText("confirm_order from confirm on order: {drink=flat_white}")
    }

    private fun runApp(name: String, block: DesktopComposeUiTest.() -> Unit) =
        runDesktopComposeUiTest(width = 960, height = 1280) {
            setContent { CompositionLocalProvider(LocalDensity provides Density(2f)) { App() } }
            block()
            // Move the pointer off the tabs, so the screenshot shows no hover highlight.
            onRoot().performMouseInput { moveTo(Offset(1f, 1279f)) }
            waitForIdle()
            val file = File("build/screenshots/desktop-$name.png").apply { parentFile.mkdirs() }
            ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", file)
        }

    private fun DesktopComposeUiTest.waitForText(text: String) =
        waitUntil(timeoutMillis = 10_000) { onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }
}
