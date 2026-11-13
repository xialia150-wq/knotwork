package app.knotwork.design.components.pipelineeditor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.knotwork.design.KnotworkRoborazziOptions
import app.knotwork.design.theme.KnotworkTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Captures quiet and vivid node headers in both themes. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class CanvasHeadersSnapshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test fun quietLight() = capture(false, false)

    @Test fun quietDark() = capture(true, false)

    @Test fun vividLight() = capture(false, true)

    @Test fun vividDark() = capture(true, true)

    private fun capture(dark: Boolean, vivid: Boolean) {
        compose.setContent {
            KnotworkTheme(darkTheme = dark, vivid = vivid) {
                Column(Modifier.padding(16.dp)) {
                    for (type in listOf(NodeType.PIPELINE, NodeType.IF_CONDITION, NodeType.TOOL)) {
                        NodeCard(
                            type = type,
                            title = "示例节点",
                            subtitle = null,
                            selected = false,
                            error = null,
                            multiSelected = false,
                            ports = NodePorts.forType(type),
                        )
                    }
                }
            }
        }
        val mode = if (vivid) "vivid" else "quiet"
        val theme = if (dark) "dark" else "light"
        compose.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/canvas_headers_${mode}_$theme.png",
            roborazziOptions = KnotworkRoborazziOptions,
        )
    }
}
