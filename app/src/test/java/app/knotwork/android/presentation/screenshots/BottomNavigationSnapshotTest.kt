package app.knotwork.android.presentation.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import app.knotwork.android.presentation.ui.navigation.AppBottomNavigationBar
import app.knotwork.android.presentation.ui.navigation.NavRoutes
import app.knotwork.design.a11y.FixedKnotworkA11y
import app.knotwork.design.a11y.LocalKnotworkA11y
import app.knotwork.design.theme.KnotworkTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshots of the bottom navigation bar: both themes with Chat selected, plus
 * the scrolled state that draws the hairline between body and bar.
 *
 * No other baseline in this module draws the shell (the translated-screen suite
 * renders screens without it), so without these the accent-coloured glyph, the
 * selection dot and the conditional hairline would be reviewable only on a
 * device.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h640dp-xhdpi")
class BottomNavigationSnapshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bottom_nav_light() {
        snapshot(fileName = "bottom_nav_light.png", darkTheme = false, scrolledUnder = false)
    }

    @Test
    fun bottom_nav_dark() {
        snapshot(fileName = "bottom_nav_dark.png", darkTheme = true, scrolledUnder = false)
    }

    @Test
    fun bottom_nav_scrolled_light() {
        snapshot(fileName = "bottom_nav_scrolled_light.png", darkTheme = false, scrolledUnder = true)
    }

    /**
     * Draws the bar at the bottom of a page-coloured frame, exactly as the shell
     * mounts it, and compares the frame with its baseline.
     */
    private fun snapshot(fileName: String, darkTheme: Boolean, scrolledUnder: Boolean) {
        composeTestRule.setContent {
            KnotworkTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(LocalKnotworkA11y provides FixedKnotworkA11y(reducedMotion = true)) {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        AppBottomNavigationBar(
                            owningTabRoute = NavRoutes.CHAT_TAB,
                            scrolledUnder = scrolledUnder,
                            onTabSelected = {},
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            roborazziOptions = AppRoborazziOptions,
            filePath = "src/test/snapshots/$fileName",
        )
    }
}
