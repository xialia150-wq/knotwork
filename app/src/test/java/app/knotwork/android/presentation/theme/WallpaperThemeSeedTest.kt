package app.knotwork.android.presentation.theme

import androidx.compose.ui.graphics.Color
import app.knotwork.design.tokens.ThemeSeed
import app.knotwork.design.tokens.themeSeed
import org.junit.Assert.assertEquals
import org.junit.Test

/** Verifies wallpaper fallback and successful seed selection. */
class WallpaperThemeSeedTest {
    @Test
    fun unavailableWallpaperFallsBackToMistBlue() {
        assertEquals(ThemeSeed(), wallpaperThemeSeed { null })
        assertEquals(ThemeSeed(), wallpaperThemeSeed { error("unavailable") })
        assertEquals(themeSeed(Color.Red), wallpaperThemeSeed { Color.Red })
    }
}
