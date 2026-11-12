package app.knotwork.design.tokens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemePaletteTest {
    @Test
    fun textAndPrimaryRemainReadableForEveryPresetAndIntensity() {
        fun contrast(a: Color, b: Color): Float {
            val x = a.luminance()
            val y = b.luminance()
            return (maxOf(x, y) + 0.05f) / (minOf(x, y) + 0.05f)
        }
        val seeds = themePresets.map { ThemeSeed(it.hue, it.chroma) } +
            (0 until 360 step 10).map { ThemeSeed(it.toDouble(), 0.4) } +
            listOf(themeSeed(Color.Black), themeSeed(Color.White))
        for (seed in seeds) for (vivid in listOf(false, true)) for (dark in listOf(false, true)) {
            val scheme = themedColorScheme(seed, vivid, dark, false)
            assertTrue("$seed background", contrast(scheme.background, scheme.onBackground) >= 4.5f)
            assertTrue("$seed primary", contrast(scheme.primary, scheme.onPrimary) >= 4.5f)
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
    fun blackModeOnlyChangesDarkBackgroundAndSignalsArePreserved() {
        val seed = ThemeSeed()
        val dark = themedColorScheme(seed, false, true, true)
        assertEquals(Color.Black, dark.background)
        assertEquals(knotworkDarkColorScheme().error, dark.error)
        assertTrue(themedColorScheme(seed, false, false, true).background != Color.Black)
    }
}
