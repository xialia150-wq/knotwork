package app.knotwork.design.screens.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.knotwork.design.KnotworkRoborazziOptions
import app.knotwork.design.theme.KnotworkTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Captures the default appearance controls in both themes. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h760dp-xhdpi")
class AppearanceSettingsSnapshotTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test fun appearanceLight() = snapshot(false)

    @Test fun appearanceDark() = snapshot(true)

    private fun snapshot(dark: Boolean) {
        composeTestRule.setContent {
            KnotworkTheme(darkTheme = dark) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppearanceSettingsContent(
                        source = "wallpaper", preset = "mist_blue", color = Color.Blue,
                        vivid = false, mode = "system", pureBlack = false,
                        onSource = {}, onPreset = {}, onChannel = { _, _ -> },
                        onVivid = {}, onMode = {}, onPureBlack = {},
                    )
                }
            }
        }
        val suffix = if (dark) "dark" else "light"
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/appearance_default_$suffix.png",
            roborazziOptions = KnotworkRoborazziOptions,
        )
    }
}
