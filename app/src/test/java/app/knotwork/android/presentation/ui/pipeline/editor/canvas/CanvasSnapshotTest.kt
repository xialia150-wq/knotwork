package app.knotwork.android.presentation.ui.pipeline.editor.canvas

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.knotwork.android.domain.models.ConnectionModel
import app.knotwork.android.domain.models.NodeModel
import app.knotwork.android.domain.models.NodeType
import app.knotwork.android.domain.models.PipelineGraph
import app.knotwork.android.presentation.screenshots.AppRoborazziOptions
import app.knotwork.android.presentation.ui.pipeline.editor.core.CanvasTransform
import app.knotwork.android.presentation.ui.pipeline.editor.core.CardMeasure
import app.knotwork.android.presentation.ui.pipeline.editor.core.CoordinatesRef
import app.knotwork.android.presentation.ui.pipeline.editor.core.EditorState
import app.knotwork.design.components.pipelineeditor.NODE_CARD_INBOUND_DOT_TEST_TAG
import app.knotwork.design.components.pipelineeditor.NODE_CARD_OUTBOUND_DOT_TEST_TAG_PREFIX
import app.knotwork.design.components.pipelineeditor.NodePorts
import app.knotwork.design.theme.KnotworkTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import app.knotwork.design.components.pipelineeditor.NodeType as CatalogNodeType

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

    @Test
    fun densePortTargetsAndMeasuredEdgeAnchorsFollowDrawnDots() {
        var count by mutableStateOf(5)
        var fontScale by mutableStateOf(1f)
        var measured: CardMeasure? = null
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                KnotworkTheme {
                    EditorNode(
                        node = NodeModel("router", NodeType.INTENT_ROUTER, 0f, 0f, label = "Router"),
                        transform = CanvasTransform(),
                        selected = false,
                        multiSelected = false,
                        error = null,
                        ports = NodePorts.forType(CatalogNodeType.INTENT_ROUTER, List(count) { "item $it" }),
                        reducedMotion = true,
                        subtitle = null,
                        onSelect = {},
                        onOpenConfig = {},
                        onLongPress = {},
                        onDrag = { _, _ -> },
                        onDragEnd = {},
                        onConnectionStart = { _, _, _ -> },
                        onConnectionMove = { _, _ -> },
                        onConnectionEnd = {},
                        onConnectionCancel = {},
                        canvasLayoutCoordinatesRef = CoordinatesRef(),
                        onCardMeasured = { measured = it },
                    )
                }
            }
        }
        listOf(1f, 1.3f, 2f).forEach { scale ->
            listOf(5, 6).forEach { ports ->
                fontScale = scale
                count = ports
                compose.waitForIdle()
                val top = compose.onNodeWithTag(NODE_CARD_INBOUND_DOT_TEST_TAG, true).getBoundsInRoot().top.value
                assertNotNull(measured)
                repeat(ports) { index ->
                    val dot = compose.onNodeWithTag(NODE_CARD_OUTBOUND_DOT_TEST_TAG_PREFIX + index, true)
                        .getBoundsInRoot()
                    val target = compose.onNodeWithTag(EDITOR_PORT_TARGET_TEST_TAG_PREFIX + index, true)
                        .getBoundsInRoot()
                    assertEquals(
                        (dot.left.value + dot.right.value) / 2,
                        (target.left.value + target.right.value) / 2,
                        1f,
                    )
                    assertEquals(
                        (dot.top.value + dot.bottom.value) / 2,
                        (target.top.value + target.bottom.value) / 2,
                        1f,
                    )
                    assertEquals((dot.top.value + dot.bottom.value) / 2 - top, measured!!.outboundDotLine, 1f)
                }
            }
        }
    }

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
