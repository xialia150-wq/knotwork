package app.knotwork.design.components.pipelineeditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.knotwork.design.KnotworkRoborazziOptions
import app.knotwork.design.theme.KnotworkTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Captures selected-node connections using the editor's shared edge painter. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class CanvasHighlightSnapshotTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun arrowTipStopsOutsidePortAtAnyZoom() {
        val end = Offset(50f, 100f)
        val tangent = Offset(0.6f, 0.8f)
        for (scale in listOf(0.4f, 1f, 2f)) {
            val radius = NodePortGeometry.DOT_RADIUS * scale
            val tip = canvasArrowTip(end, tangent, radius, 3.5f)
            assertEquals(end.x - tangent.x * (radius + 3.5f), tip.x, 0.0001f)
            assertEquals(end.y - tangent.y * (radius + 3.5f), tip.y, 0.0001f)
        }
    }

    @Test fun light() = capture(false)

    @Test fun dark() = capture(true)

    @Test fun denseLabelsLight() = captureLabels(false)

    @Test fun denseLabelsDark() = captureLabels(true)

    private fun captureLabels(dark: Boolean) {
        compose.setContent {
            KnotworkTheme(darkTheme = dark) {
                Column(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp),
                    verticalArrangement = Arrangement.spacedBy(48.dp),
                ) {
                    val names = listOf("simple", "complex", "tool", "one", "two", "three")
                    listOf(2, 3, 4, 5, 6).forEach { count ->
                        NodeCard(
                            type = NodeType.INTENT_ROUTER,
                            title = "Route",
                            subtitle = null,
                            selected = false,
                            error = null,
                            multiSelected = false,
                            ports = NodePorts.forType(NodeType.INTENT_ROUTER, names.take(count)),
                        )
                    }
                }
            }
        }
        val theme = if (dark) "dark" else "light"
        compose.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/canvas_port_labels_$theme.png",
            roborazziOptions = KnotworkRoborazziOptions,
        )
    }

    private fun capture(dark: Boolean) {
        compose.setContent {
            KnotworkTheme(darkTheme = dark) {
                val first = NodeType.INPUT.headerTint()
                val second = NodeType.IF_CONDITION.headerTint()
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    Canvas(Modifier.fillMaxSize()) {
                        fun point(x: Float, y: Float) = Offset(x.dp.toPx(), y.dp.toPx())
                        drawCanvasEdge(
                            point(174f, 104f),
                            point(174f, 160f),
                            point(174f, 190f),
                            point(174f, 246f),
                            first,
                            3.5.dp.toPx(),
                            true,
                        )
                        drawCanvasEdge(
                            point(138f, 310f),
                            point(138f, 360f),
                            point(174f, 396f),
                            point(174f, 446f),
                            second,
                            3.5.dp.toPx(),
                            true,
                        )
                        drawCanvasEdge(
                            point(174f, 104f),
                            point(300f, 190f),
                            point(300f, 390f),
                            point(174f, 446f),
                            first.copy(alpha = 0.25f),
                            2.5.dp.toPx(),
                            false,
                        )
                    }
                    listOf(NodeType.INPUT, NodeType.IF_CONDITION, NodeType.OUTPUT).forEachIndexed { index, type ->
                        NodeCard(
                            type = type,
                            title = "节点",
                            subtitle = null,
                            selected = index == 1,
                            error = null,
                            multiSelected = false,
                            ports = NodePorts.forType(type),
                            modifier = Modifier.offset(x = 90.dp, y = (40 + index * 200).dp),
                        )
                    }
                }
            }
        }
        val theme = if (dark) "dark" else "light"
        compose.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/canvas_highlight_$theme.png",
            roborazziOptions = KnotworkRoborazziOptions,
        )
    }
}
