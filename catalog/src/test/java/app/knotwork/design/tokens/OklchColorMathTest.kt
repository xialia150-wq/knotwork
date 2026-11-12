package app.knotwork.design.tokens

import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

/** Checks conversion accuracy, gamut preservation, and WCAG calculations. */
class OklchColorMathTest {
    @Test
    fun nodePipelineCommentReproducesTheExistingSrgbToken() {
        // Color.kt documents NodePipeline as oklch(0.47 0.16 274), #424DB2.
        val color = OklchColorMath.gamutMap(Oklch(0.47, 0.16, 274.0))
        val actual = listOf(color.red, color.green, color.blue).map { (it * 255).roundToInt() }
        val token = KnotworkPalette.NodePipeline.toArgb()
        val expected = listOf((token shr 16) and 255, (token shr 8) and 255, token and 255)
        actual.zip(expected).forEach { (channel, reference) -> assertTrue(kotlin.math.abs(channel - reference) <= 1) }
    }

    @Test
    fun gamutMappingKeepsLightnessAndHueWhileReducingChroma() {
        val input = Oklch(0.65, 0.4, 145.0)
        val mapped = OklchColorMath.gamutMap(input)
        assertTrue(listOf(mapped.red, mapped.green, mapped.blue).all { it in 0.0..1.0 })
        val decoded = OklchColorMath.toOklch(OklchColorMath.toOklab(OklchColorMath.toLinear(mapped)))
        assertEquals(input.lightness, decoded.lightness, 0.000001)
        assertEquals(input.hue, decoded.hue, 0.0001)
        assertTrue(decoded.chroma < input.chroma)
    }

    @Test
    fun conversionAndContrastMatchKnownReferenceValues() {
        val red = Srgb(1.0, 0.0, 0.0)
        val lab = OklchColorMath.toOklab(OklchColorMath.toLinear(red))
        assertEquals(0.627955, lab.lightness, 0.000001)
        val result = OklchColorMath.toSrgb(OklchColorMath.toLinear(lab))
        assertEquals(red.red, result.red, 0.00001)
        assertEquals(red.green, result.green, 0.00001)
        assertEquals(red.blue, result.blue, 0.00001)
        assertEquals(21.0, OklchColorMath.contrast(Srgb(0.0, 0.0, 0.0), Srgb(1.0, 1.0, 1.0)), 0.000001)
    }
}
