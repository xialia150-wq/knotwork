package app.knotwork.design.screens.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.knotwork.design.KnotworkRoborazziOptions
import app.knotwork.design.a11y.FixedKnotworkA11y
import app.knotwork.design.a11y.LocalKnotworkA11y
import app.knotwork.design.theme.KnotworkTheme
import app.knotwork.design.tokens.OklchColorMath
import app.knotwork.design.tokens.Srgb
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import android.graphics.Color as AndroidColor

/**
 * Roborazzi baseline for `SplashContent`. Covers the documented states
 * (Initializing / Loading / Error / DataLocked) in both themes.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h760dp-xhdpi")
class SplashContentSnapshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun splash_loading_light() = snapshot(name = "loading", dark = false) {
        SplashContent(appName = "Knotwork Agent", state = SplashPreview.loading())
    }

    @Test
    @Config(qualifiers = "w360dp-h760dp-night-xhdpi")
    fun splash_loading_dark() = snapshot(name = "loading", dark = true) {
        SplashContent(appName = "Knotwork Agent", state = SplashPreview.loading())
    }

    @Test
    fun splash_error_light() = snapshot(name = "error", dark = false) {
        SplashContent(appName = "Knotwork Agent", state = SplashPreview.error())
    }

    @Test
    @Config(qualifiers = "w360dp-h760dp-night-xhdpi")
    fun splash_error_dark() = snapshot(name = "error", dark = true) {
        SplashContent(appName = "Knotwork Agent", state = SplashPreview.error())
    }

    @Test
    fun splash_data_locked_light() = snapshot(name = "data_locked", dark = false) {
        SplashContent(appName = "Knotwork Agent", state = SplashPreview.dataLocked())
    }

    @Test
    @Config(qualifiers = "w360dp-h760dp-night-xhdpi")
    fun splash_data_locked_dark() = snapshot(name = "data_locked", dark = true) {
        SplashContent(appName = "Knotwork Agent", state = SplashPreview.dataLocked())
    }

    @Test
    fun splash_weave_light_frames() = weaveFrames(dark = false)

    @Test
    @Config(qualifiers = "w360dp-h760dp-night-xhdpi")
    fun splash_weave_dark_frames() = weaveFrames(dark = true)

    @Test
    fun splash_reduced_motion_does_not_animate() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            KnotworkTheme {
                CompositionLocalProvider(LocalKnotworkA11y provides FixedKnotworkA11y(reducedMotion = true)) {
                    SplashContent(appName = "Knotwork Agent", state = SplashPreview.loading())
                }
            }
        }
        composeTestRule.mainClock.advanceTimeByFrame()
        val before = composeTestRule.onRoot().captureToImage().toPixelMap().buffer.copyOf()
        composeTestRule.mainClock.advanceTimeBy(1200)
        val after = composeTestRule.onRoot().captureToImage().toPixelMap().buffer
        assertArrayEquals(before, after)
    }

    @Test
    fun thread_reuses_platform_animation_progress_and_clamps_clock_edges() {
        assertEquals(400L, splashThreadElapsed(startMillis = 1_000L, nowMillis = 1_400L))
        assertEquals(1_000L, splashThreadElapsed(startMillis = 1_000L, nowMillis = 5_000L))
        assertEquals(0L, splashThreadElapsed(startMillis = 2_000L, nowMillis = 1_000L))
        assertEquals(1_000L, splashThreadElapsed(startMillis = 0L, nowMillis = 1_000L))
        assertEquals(0L, splashThreadElapsed(startMillis = null, nowMillis = 1_000L))
    }

    @Test
    fun launcher_and_splash_share_the_centered_safe_strokes() {
        val source = File("../brand/zhiwen-thread.svg").readText()
        val svgPaths = Regex("<path id=\"stroke\\d+\" d=\"([^\"]+)\"")
            .findAll(source).map { it.groupValues[1] }.toList()
        assertEquals(9, svgPaths.size)
        assertEquals(svgPaths, BRAND_PATHS)
        listOf(
            "ic_launcher_foreground",
            "ic_launcher_monochrome",
            "splash_foreground",
        ).forEach { name ->
            val xml = File("../app/src/main/res/drawable/$name.xml").readText()
            val paths = Regex("android:pathData=\"([^\"]+)\"").findAll(xml).map { it.groupValues[1] }.toList()
            assertEquals(svgPaths, paths)
            val widths = Regex("android:strokeWidth=\"([^\"]+)\"").findAll(xml).map { it.groupValues[1] }.toList()
            assertEquals(List(svgPaths.size) { "3.2" }, widths)
        }
        val points = Regex("([0-9.]+),([0-9.]+)").findAll(svgPaths.joinToString(" ")).map {
            it.groupValues[1].toFloat() to it.groupValues[2].toFloat()
        }.toList()
        val minX = points.minOf { it.first } - 1.6f
        val maxX = points.maxOf { it.first } + 1.6f
        val minY = points.minOf { it.second } - 1.6f
        val maxY = points.maxOf { it.second } + 1.6f
        assertTrue(minX >= 21f && maxX <= 87f && minY >= 21f && maxY <= 87f)
        assertTrue((maxX - minX) / 108f in 0.54f..0.58f)
        assertTrue(kotlin.math.abs((minX + maxX) / 2f - 54f) < 2f)
        assertTrue(kotlin.math.abs((minY + maxY) / 2f - 54f) < 2f)
        svgPaths.forEach { path ->
            val measure = PathMeasure().apply { setPath(PathParser().parsePathString(path).toPath(), false) }
            repeat(501) { step ->
                val point = measure.getPosition(measure.length * step / 500f)
                val distance = kotlin.math.hypot(point.x - 54f, point.y - 54f)
                assertTrue("Every stroke must fit the circular safe area", distance + 1.6f <= 33f)
            }
        }
        fun rightEdge(path: String): Float = Regex("([0-9.]+),([0-9.]+)").findAll(path)
            .maxOf { it.groupValues[1].toFloat() }
        assertTrue(
            "The inner box must have a stroke-width gap",
            rightEdge(svgPaths[7]) - rightEdge(svgPaths[8]) >= 6.39f,
        )
    }

    @Test
    fun platform_strokes_form_one_second_sequence_without_gaps() {
        var end = 0
        for (index in 1..9) {
            val xml = File("../app/src/main/res/animator/splash_stroke_$index.xml").readText()
            fun attribute(name: String): Int = requireNotNull(Regex("android:$name=\"(\\d+)\"").find(xml))
                .groupValues[1].toInt()
            assertEquals(end, attribute("startOffset"))
            end += attribute("duration")
        }
        assertEquals(1_000, end)
    }

    @Test
    fun startup_ink_contrasts_with_its_background_in_both_modes() {
        listOf(false, true).forEach { dark ->
            val variant = if (dark) "values-night" else "values"
            val xml = File("src/main/res/$variant/brand_startup.xml").readText()
            fun color(name: String): Int {
                val match = requireNotNull(Regex("<color name=\"$name\">([^<]+)</color>").find(xml))
                return AndroidColor.parseColor(match.groupValues[1])
            }
            fun srgb(value: Int) = Srgb(
                AndroidColor.red(value) / 255.0,
                AndroidColor.green(value) / 255.0,
                AndroidColor.blue(value) / 255.0,
            )
            val background = srgb(color("brand_startup_background"))
            val ink = srgb(color("brand_startup_ink"))
            assertTrue(OklchColorMath.contrast(background, ink) >= 4.5)
        }
    }

    private fun weaveFrames(dark: Boolean) {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            KnotworkTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalKnotworkA11y provides FixedKnotworkA11y(reducedMotion = false)) {
                    SplashContent(appName = "Knotwork Agent", state = SplashPreview.loading())
                }
            }
        }
        val theme = if (dark) "dark" else "light"
        val frames = mutableListOf<IntArray>()
        composeTestRule.mainClock.advanceTimeByFrame()
        listOf(0L, 500L, 1000L).forEachIndexed { index, millis ->
            if (index > 0) composeTestRule.mainClock.advanceTimeBy(500L)
            composeTestRule.onRoot().captureRoboImage(
                roborazziOptions = KnotworkRoborazziOptions,
                filePath = "src/test/snapshots/splash_weave_${theme}_$millis.png",
            )
            frames += composeTestRule.onRoot().captureToImage().toPixelMap().buffer.copyOf()
        }
        assertFalse("The thread should be revealed over time", frames[0].contentEquals(frames[1]))
        assertFalse("The thread should continue until its end", frames[1].contentEquals(frames[2]))
    }

    private fun snapshot(name: String, dark: Boolean, content: @Composable () -> Unit) {
        composeTestRule.setContent {
            KnotworkTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalKnotworkA11y provides FixedKnotworkA11y(reducedMotion = true)) {
                    content()
                }
            }
        }
        val themeTag = if (dark) "dark" else "light"
        composeTestRule.onRoot().captureRoboImage(
            roborazziOptions = KnotworkRoborazziOptions,
            filePath = "src/test/snapshots/splash_${name}_$themeTag.png",
        )
    }
}

/** Snapshot fixtures backing the splash test suite. */
internal object SplashPreview {
    fun loading(): SplashViewState = SplashViewState.Loading(
        message = "Loading model • Gemma 4 E2B",
        progress = 0.62f,
    )

    fun error(): SplashViewState = SplashViewState.Error(
        message = "Initialisation failed: model file not found. Reinstall the model from Settings.",
    )

    fun dataLocked(): SplashViewState = SplashViewState.DataLocked(
        title = "Your data can’t be unlocked",
        body = "The encryption key protecting your local data could not be read. " +
            "This is often temporary — try again first. If the problem persists, " +
            "the only way forward is erasing all local data.",
        resetLabel = "Erase all data",
    )
}
