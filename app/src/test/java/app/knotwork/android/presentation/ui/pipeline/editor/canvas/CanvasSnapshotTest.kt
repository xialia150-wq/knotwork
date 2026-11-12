package app.knotwork.android.presentation.ui.pipeline.editor.canvas

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.knotwork.android.domain.models.ConnectionModel
import app.knotwork.android.domain.models.NodeModel
import app.knotwork.android.domain.models.NodeType
import app.knotwork.android.domain.models.PipelineGraph
import app.knotwork.android.presentation.screenshots.AppRoborazziOptions
import app.knotwork.android.presentation.ui.pipeline.editor.core.EditorState
import app.knotwork.design.theme.KnotworkTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Captures the actual editor canvas in both themes and selection states. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class CanvasSnapshotTest {
    @get:Rule val compose = createComposeRule()

    @Test fun idleLight() = capture(false, false)

    @Test fun idleDark() = capture(true, false)

    @Test fun selectedLight() = capture(false, true)

    @Test fun selectedDark() = capture(true, true)

    private fun capture(dark: Boolean, selected: Boolean) {
        val graph = PipelineGraph(
            id = "snapshot",
            name = "示例",
            nodes = listOf(
                NodeModel("input", NodeType.INPUT, 100f, 0f, label = "输入"),
                NodeModel("branch", NodeType.IF_CONDITION, 100f, 210f, label = "判断"),
                NodeModel("output", NodeType.OUTPUT, 100f, 420f, label = "输出"),
            ),
            connections = listOf(
                ConnectionModel("one", "input", "branch"),
                ConnectionModel("two", "branch", "output", label = "True"),
                ConnectionModel("three", "input", "output"),
            ),
        )
        val editor = EditorState().apply { selection = if (selected) setOf("branch") else emptySet() }
        compose.setContent {
            KnotworkTheme(darkTheme = dark) {
                EditorCanvas(
                    graph = graph,
                    editor = editor,
                    errorsByNodeId = emptyMap(),
                    reducedMotion = true,
                    onMoveNode = { _, _, _ -> },
                    onAddNode = { _, _, _ -> },
                    onAddConnection = { _, _, _ -> },
                    onConnectionDropped = {},
                    onOpenNodeConfig = {},
                    onLongPressEdge = {},
                    onStartWithInput = {},
                    onFromTemplate = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        compose.waitForIdle()
        val mode = if (selected) "selected" else "idle"
        val theme = if (dark) "dark" else "light"
        compose.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/canvas_${mode}_$theme.png",
            roborazziOptions = AppRoborazziOptions,
        )
    }
}
