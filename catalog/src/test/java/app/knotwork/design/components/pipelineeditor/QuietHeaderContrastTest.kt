package app.knotwork.design.components.pipelineeditor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import app.knotwork.design.tokens.KnotworkPalette
import app.knotwork.design.tokens.ThemeSeed
import app.knotwork.design.tokens.themedColorScheme
import org.junit.Assert.assertTrue
import org.junit.Test

class QuietHeaderContrastTest {
    @Test
    fun `all fixed node colors remain readable on quiet headers`() {
        val colors = listOf(
            KnotworkPalette.NodeInput, KnotworkPalette.NodeOutput, KnotworkPalette.NodeLiteRt,
            KnotworkPalette.NodeCloud, KnotworkPalette.NodeIntentRouter, KnotworkPalette.NodeIfCondition,
            KnotworkPalette.NodeClarification, KnotworkPalette.NodeTool, KnotworkPalette.NodeDecomposition,
            KnotworkPalette.NodeQueueProcessor, KnotworkPalette.NodeEvaluation, KnotworkPalette.NodeSummary,
            KnotworkPalette.NodePipeline,
        )
        for (dark in listOf(false, true)) {
            for (pureBlack in listOf(false, true)) {
                val scheme = themedColorScheme(ThemeSeed(), vivid = false, dark = dark, pureBlack = pureBlack)
                for (node in colors) {
                    val background = quietHeaderBackground(node, scheme.surface)
                    val foreground = quietHeaderForeground(node, background)
                    assertTrue("$node on $background", contrast(foreground, background) >= 4.5)
                }
            }
        }
    }

    private fun contrast(a: Color, b: Color): Double {
        val x = a.luminance().toDouble()
        val y = b.luminance().toDouble()
        return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05)
    }
}
