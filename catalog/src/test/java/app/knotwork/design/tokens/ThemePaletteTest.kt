package app.knotwork.design.tokens

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks contrast and intensity for presets and extreme custom colors. */
class ThemePaletteTest {
    @Test
    fun textAndPrimaryRemainReadableForEveryPresetAndIntensity() {
        fun contrast(a: Color, b: Color): Float {
            fun rgb(c: Color) = Srgb(c.red.toDouble(), c.green.toDouble(), c.blue.toDouble())
            return OklchColorMath.contrast(rgb(a), rgb(b)).toFloat()
        }
        val seeds = themePresets.map { ThemeSeed(it.hue, it.chroma) } +
            (0 until 360 step 10).map { ThemeSeed(it.toDouble(), 0.4) } +
            listOf(Color.Black, Color.White, Color.Yellow, Color.Blue).map(::themeSeed)
        for (seed in seeds) {
            for (vivid in listOf(false, true)) {
                for (dark in listOf(false, true)) {
                    for (black in listOf(false, true)) {
                        val scheme = themedColorScheme(seed, vivid, dark, black)
                        val extended = themedExtendedColors(seed, scheme, dark)
                        assertTrue("$seed background", contrast(scheme.background, scheme.onBackground) >= 4.5f)
                        assertTrue("$seed primary", contrast(scheme.primary, scheme.onPrimary) >= 4.5f)
                        // Dim text is deliberately de-emphasized; outlines are non-text UI boundaries.
                        assertTrue("$seed dim", contrast(scheme.background, extended.onSurfaceDim) >= 3f)
                        assertTrue("$seed outline", contrast(scheme.background, scheme.outline) >= 3f)
                    }
                }
            }
        }
    }

    @Test
    fun vividRaisesChromaAtSameSeed() {
        val seed = ThemeSeed()
        val quiet = themeSeed(themedColorScheme(seed, false, false, false).primary)
        val vivid = themeSeed(themedColorScheme(seed, true, false, false).primary)
        assertTrue(vivid.chroma > quiet.chroma)
    }

    @Test
    fun inversePrimaryTracksTheSeedAndUsesTheOppositeTone() {
        val blue = ThemeSeed(240.0, 0.15)
        val orange = ThemeSeed(55.0, 0.15)
        for (dark in listOf(false, true)) {
            val first = themedColorScheme(blue, false, dark, false)
            val second = themedColorScheme(orange, false, dark, false)
            assertTrue(first.inversePrimary != second.inversePrimary)
            fun luminance(color: Color) = OklchColorMath.relativeLuminance(
                Srgb(color.red.toDouble(), color.green.toDouble(), color.blue.toDouble()),
            )
            if (dark) {
                assertTrue(luminance(first.inversePrimary) < luminance(first.primary))
            } else {
                assertTrue(luminance(first.inversePrimary) > luminance(first.primary))
            }
        }
    }

    @Test
    fun redMatchesPublishedOklabReferenceAndRoundTrips() {
        val seed = themeSeed(Color.Red)
        assertEquals(29.2339, seed.hue, 0.02)
        assertEquals(0.25768, seed.chroma, 0.0001)
        val red = themeTone(0.627955, seed.chroma, seed.hue)
        assertEquals(1f, red.red, 0.002f)
        assertEquals(0f, red.green, 0.002f)
        assertEquals(0f, red.blue, 0.002f)
    }

    @Test
    fun signalsMatchOriginalModeAndThirteenNodeColorsNeverFollowTheSeed() {
        val nodes = listOf(
            KnotworkPalette.NodeInput, KnotworkPalette.NodeIntentRouter, KnotworkPalette.NodeIfCondition,
            KnotworkPalette.NodeClarification, KnotworkPalette.NodeLiteRt, KnotworkPalette.NodeCloud,
            KnotworkPalette.NodeTool, KnotworkPalette.NodeDecomposition, KnotworkPalette.NodeQueueProcessor,
            KnotworkPalette.NodeEvaluation, KnotworkPalette.NodeSummary, KnotworkPalette.NodeOutput,
            KnotworkPalette.NodePipeline,
        )
        val seeds = themePresets.map { ThemeSeed(it.hue, it.chroma) } +
            listOf(Color.White, Color.Black, Color.Yellow, Color.Blue).map(::themeSeed)
        for (seed in seeds) {
            for (vivid in listOf(false, true)) {
                for (dark in listOf(false, true)) {
                    for (black in listOf(false, true)) {
                        assertFixedColors(seed, vivid, dark, black, nodes)
                    }
                }
            }
        }
    }

    private fun assertFixedColors(seed: ThemeSeed, vivid: Boolean, dark: Boolean, black: Boolean, nodes: List<Color>) {
        val scheme = themedColorScheme(seed, vivid, dark, black)
        val extended = themedExtendedColors(seed, scheme, dark)
        val original = if (dark) knotworkExtendedColorsDark() else knotworkExtendedColorsLight()
        val base = if (dark) knotworkDarkColorScheme() else knotworkLightColorScheme()
        assertEquals(original.signalSuccess, extended.signalSuccess)
        assertEquals(original.signalWarn, extended.signalWarn)
        assertEquals(original.signalError, extended.signalError)
        assertEquals(original.riskSensitive, extended.riskSensitive)
        assertEquals(original.riskDestructive, extended.riskDestructive)
        assertEquals(base.error, scheme.error)
        assertEquals(
            nodes,
            listOf(
                extended.nodeInput, extended.nodeIntentRouter, extended.nodeIfCondition,
                extended.nodeClarification, extended.nodeLiteRt, extended.nodeCloud,
                extended.nodeTool, extended.nodeDecomposition, extended.nodeQueueProcessor,
                extended.nodeEvaluation, extended.nodeSummary, extended.nodeOutput,
                extended.nodePipeline,
            ),
        )
    }

    @Test
    fun blackModeOnlyChangesDarkBackgroundAndSignalsArePreserved() {
        val seed = ThemeSeed()
        val dark = themedColorScheme(seed, false, true, true)
        assertEquals(Color.Black, dark.background)
        assertEquals(knotworkDarkColorScheme().error, dark.error)
        assertTrue(themedColorScheme(seed, false, false, true).background != Color.Black)
    }
}
