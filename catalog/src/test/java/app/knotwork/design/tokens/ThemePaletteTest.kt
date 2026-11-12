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
        for (preset in themePresets) for (vivid in listOf(false, true)) for (dark in listOf(false, true)) {
            val scheme = themedColorScheme(ThemeSeed(preset.hue, preset.chroma), vivid, dark, false)
            assertTrue("${preset.id} background", contrast(scheme.background, scheme.onBackground) >= 4.5f)
            assertTrue("${preset.id} primary", contrast(scheme.primary, scheme.onPrimary) >= 4.5f)
        }
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
