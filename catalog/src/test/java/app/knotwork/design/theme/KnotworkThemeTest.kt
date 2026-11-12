package app.knotwork.design.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.knotwork.design.components.pipelineeditor.NodeType
import app.knotwork.design.components.pipelineeditor.headerTint
import app.knotwork.design.tokens.KnotworkPalette
import app.knotwork.design.tokens.themePresets
import app.knotwork.design.tokens.ThemeSeed
import app.knotwork.design.tokens.themedColorScheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Verifies that [KnotworkTheme] wires the Knotwork tokens into the
 * underlying [MaterialTheme] and into the `KnotworkTheme.*`
 * composition-local accessors.
 *
 * Each test renders a single [KnotworkTheme] composition with the
 * `darkTheme` parameter pinned explicitly and reads the contract values
 * from inside the composition. The reads are captured into local variables
 * so the assertions can run outside the Compose tree.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class KnotworkThemeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `given KnotworkTheme light when read MaterialTheme primary then equals derived primary`() {
        val captured = captureInTheme(darkTheme = false) { MaterialTheme.colorScheme.primary }
        assertEquals(themedColorScheme(ThemeSeed(), false, false, false).primary, captured)
    }

    @Test
    fun `given KnotworkTheme dark when read MaterialTheme primary then equals derived primary`() {
        val captured = captureInTheme(darkTheme = true) { MaterialTheme.colorScheme.primary }
        assertEquals(themedColorScheme(ThemeSeed(), false, true, false).primary, captured)
    }

    @Test
    fun `given KnotworkTheme light when read extended nodeIntentRouter then equals expected hue`() {
        val captured = captureInTheme(darkTheme = false) { KnotworkTheme.extended.nodeIntentRouter }
        assertEquals(Color(0xFF9283DC), captured)
    }

    @Test
    fun `given KnotworkTheme dark when read extended nodeIntentRouter then equals expected hue`() {
        val captured = captureInTheme(darkTheme = true) { KnotworkTheme.extended.nodeIntentRouter }
        assertEquals(Color(0xFF9283DC), captured)
    }

    @Test
    fun `given KnotworkTheme when read spacing sp4 then equals 16dp`() {
        val captured = captureInTheme(darkTheme = false) { KnotworkTheme.spacing.sp4 }
        assertEquals(16.dp, captured)
    }

    @Test
    fun `given KnotworkTheme dark when read extended chatAgentBg then matches dark surface2`() {
        val captured = captureInTheme(darkTheme = true) { KnotworkTheme.extended.chatAgentBg }
        // KnotworkExtendedColors.chatAgentBg in dark theme aliases surface2 (#221E1A).
        assertEquals(Color(0xFF221E1A), captured)
    }

    @Test
    fun `given KnotworkTheme light when read MaterialTheme tertiaryContainer then equals light TertiaryContainer`() {
        // Guards the M3 expanded slot wiring — a missing tertiaryContainer
        // assignment falls back to Material baseline #FFD8E2 instead of the
        // Knotwork cool-blue tint.
        val captured = captureInTheme(darkTheme = false) { MaterialTheme.colorScheme.tertiaryContainer }
        assertEquals(Color(0xFFD6E5EB), captured)
    }

    @Test
    fun `given KnotworkTheme dark when read MaterialTheme tertiaryContainer then equals dark TertiaryContainer`() {
        val captured = captureInTheme(darkTheme = true) { MaterialTheme.colorScheme.tertiaryContainer }
        assertEquals(Color(0xFF1A3B47), captured)
    }

    @Test
    fun `given KnotworkTheme light when read MaterialTheme surfaceContainer then equals derived light surface`() {
        val captured = captureInTheme(darkTheme = false) { MaterialTheme.colorScheme.surfaceContainer }
        assertEquals(themedColorScheme(ThemeSeed(), false, false, false).surfaceContainer, captured)
    }

    @Test
    fun `given KnotworkTheme dark when read MaterialTheme surfaceContainerHighest then equals derived dark surface`() {
        val captured = captureInTheme(darkTheme = true) { MaterialTheme.colorScheme.surfaceContainerHighest }
        assertEquals(themedColorScheme(ThemeSeed(), false, true, false).surfaceContainerHighest, captured)
    }

    @Test
    fun `every node header tint stays fixed across all theme settings`() {
        val expected = mapOf(
            NodeType.INPUT to KnotworkPalette.NodeInput,
            NodeType.OUTPUT to KnotworkPalette.NodeOutput,
            NodeType.LITE_RT to KnotworkPalette.NodeLiteRt,
            NodeType.CLOUD to KnotworkPalette.NodeCloud,
            NodeType.INTENT_ROUTER to KnotworkPalette.NodeIntentRouter,
            NodeType.IF_CONDITION to KnotworkPalette.NodeIfCondition,
            NodeType.CLARIFICATION to KnotworkPalette.NodeClarification,
            NodeType.TOOL to KnotworkPalette.NodeTool,
            NodeType.DECOMPOSITION to KnotworkPalette.NodeDecomposition,
            NodeType.QUEUE_PROCESSOR to KnotworkPalette.NodeQueueProcessor,
            NodeType.EVALUATION to KnotworkPalette.NodeEvaluation,
            NodeType.SUMMARY to KnotworkPalette.NodeSummary,
            NodeType.PIPELINE to KnotworkPalette.NodePipeline,
            NodeType.SKILL to KnotworkPalette.Accent600,
        )
        val observed = mutableMapOf<String, Map<NodeType, Color>>()
        val signals = mutableMapOf<String, List<Color>>()
        composeTestRule.setContent {
            for (preset in themePresets) for (dark in listOf(false, true)) {
                for (vivid in listOf(false, true)) for (black in listOf(false, true)) {
                    KnotworkTheme(darkTheme = dark, seed = ThemeSeed(preset.hue, preset.chroma), vivid = vivid, pureBlack = black) {
                        val key = "${preset.id}/$dark/$vivid/$black"
                        observed[key] = NodeType.entries.associateWith { it.headerTint() }
                        signals[key] = listOf(
                            KnotworkTheme.extended.signalSuccess, KnotworkTheme.extended.signalWarn,
                            KnotworkTheme.extended.signalError, MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        assertEquals(themePresets.size * 8, observed.size)
        observed.forEach { (name, colors) -> assertEquals(name, expected, colors) }
        val expectedSignals = listOf(
            KnotworkPalette.SignalSuccess, KnotworkPalette.SignalWarn,
            KnotworkPalette.SignalError, KnotworkPalette.SignalError,
        )
        assertEquals(observed.size, signals.size)
        signals.forEach { (name, colors) -> assertEquals(name, expectedSignals, colors) }
    }

    /**
     * Renders a single composition wrapped in [KnotworkTheme] and returns the
     * value produced by [reader].
     *
     * Each invocation must be the only call within a single `@Test` body —
     * `setContent` may only be called once per test rule lifetime.
     */
    private inline fun <reified T : Any> captureInTheme(
        darkTheme: Boolean,
        crossinline reader: @Composable () -> T,
    ): T {
        var captured: T? = null
        composeTestRule.setContent {
            KnotworkTheme(darkTheme = darkTheme) {
                captured = reader()
            }
        }
        composeTestRule.waitForIdle()
        return checkNotNull(captured) {
            "Reader did not run — composition was not invoked."
        }
    }
}
