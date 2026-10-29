// The JVM class is named after the module: another module with its own string resources would have
// a stringResource in this package too, and two classes of one name cannot both be on the classpath.
@file:kotlin.jvm.JvmName("Material3A2uiStringResource")

package ai.littlepress.a2ui.compat.androidx.compose.ui.res

import ai.littlepress.a2ui.compat.android.content.res.ResourceLocales
import ai.littlepress.a2ui.compat.android.content.res.formatString
import androidx.compose.material3.a2ui.STRING_RESOURCES
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLocale

/**
 * Stands in for androidx.compose.ui.res.stringResource, which only Android has: the string with
 * this R.string id (resources-generated/.../R.kt) in the resource locale Android would choose for
 * LocalLocale ([ResourceLocales]), formatted as Resources.getString formats it.
 */
@Composable
internal fun stringResource(id: Int, vararg formatArgs: Any): String {
    val tag = LocalLocale.current.toLanguageTag()
    val value = remember(id, tag) {
        val values = STRING_RESOURCES[id - 1]
        values.values.elementAt(ResourceLocales.bestMatch(values.keys.toList(), tag))
    }
    return if (formatArgs.isEmpty()) value else formatString(value, *formatArgs)
}
