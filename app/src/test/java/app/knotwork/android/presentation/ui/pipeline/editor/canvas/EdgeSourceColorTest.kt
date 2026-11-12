package app.knotwork.android.presentation.ui.pipeline.editor.canvas

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.knotwork.android.domain.models.ConnectionModel
import app.knotwork.android.domain.models.NodeModel
import app.knotwork.android.domain.models.NodeType
import app.knotwork.design.theme.KnotworkTheme
import app.knotwork.design.tokens.KnotworkPalette
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Verifies that edge colors come from the source node, not the target. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class EdgeSourceColorTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `edges use their source node color`() {
        val nodes = mapOf(
            "input" to NodeModel("input", NodeType.INPUT, 0f, 0f),
            "pipe" to NodeModel("pipe", NodeType.PIPELINE, 0f, 0f),
        )
        var first = Color.Unspecified
        var second = Color.Unspecified
        compose.setContent {
            KnotworkTheme(darkTheme = false) {
                first = sourceEdgeColor(ConnectionModel("a", "input", "pipe"), nodes)
                second = sourceEdgeColor(ConnectionModel("b", "pipe", "input"), nodes)
            }
        }
        compose.waitForIdle()
        assertEquals(KnotworkPalette.NodeInput, first)
        assertEquals(KnotworkPalette.NodePipeline, second)
    }
}
