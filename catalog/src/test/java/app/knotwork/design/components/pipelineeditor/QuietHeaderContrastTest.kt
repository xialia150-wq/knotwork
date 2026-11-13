package app.knotwork.design.components.pipelineeditor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import app.knotwork.design.tokens.KnotworkElevation
import app.knotwork.design.tokens.KnotworkPalette
import app.knotwork.design.tokens.OklchColorMath
import app.knotwork.design.tokens.Srgb
import app.knotwork.design.tokens.ThemeSeed
import app.knotwork.design.tokens.themedColorScheme
import app.knotwork.design.tokens.themedExtendedColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** Checks quiet header contrast for all fixed node colors. */
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
                for (vivid in listOf(false, true)) {
                    assertPalette(colors, dark, pureBlack, vivid)
                }
            }
        }
    }

    private fun assertPalette(colors: List<Color>, dark: Boolean, pureBlack: Boolean, vivid: Boolean) {
        val elevation = KnotworkElevation()
        val seed = ThemeSeed()
        val scheme = themedColorScheme(seed, vivid = vivid, dark = dark, pureBlack = pureBlack)
        val canvas = themedExtendedColors(seed, scheme, dark).surface1
        for (height in listOf(elevation.el1, elevation.el3)) {
            val surface = nodeCardSurfaceColor(scheme, height)
            for (node in colors) assertNode(node, surface, canvas)
        }
    }

    private fun assertNode(node: Color, surface: Color, canvas: Color) {
        val background = quietHeaderBackground(node, surface)
        val foreground = quietHeaderForeground(node, background)
        assertTrue("$node on $background", contrast(foreground, background) >= 4.5)
        val port = portForeground(node, surface, canvas)
        assertTrue("port $node on $surface", contrast(port, surface) >= 3.0)
        assertTrue("port $node on $canvas", contrast(port, canvas) >= 3.0)
        if (contrast(node, surface) >= 3.0 && contrast(node, canvas) >= 3.0) {
            assertEquals("readable colors stay unchanged", node, port)
        } else {
            val difference = abs(hue(node) - hue(port))
            // Compose's sRGB color is quantized to 8-bit channels.
            assertTrue("$node to $port: hue changed by $difference", minOf(difference, 360 - difference) < 2.0)
        }
    }

    private fun hue(color: Color): Double {
        val rgb = Srgb(color.red.toDouble(), color.green.toDouble(), color.blue.toDouble())
        return OklchColorMath.toOklch(OklchColorMath.toOklab(OklchColorMath.toLinear(rgb))).hue
    }

    private fun contrast(a: Color, b: Color): Double {
        val x = a.luminance().toDouble()
        val y = b.luminance().toDouble()
        return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05)
    }
}
