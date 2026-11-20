package app.knotwork.android.presentation.ui.orchestrator.components

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.knotwork.android.R
import app.knotwork.android.presentation.screenshots.AppRoborazziOptions
import app.knotwork.design.components.pipelineeditor.NodeConfigSheetBody
import app.knotwork.design.components.pipelineeditor.OutputConfig
import app.knotwork.design.theme.KnotworkTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale

/** Production form plus the app-provided input-data group, including larger translated text. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h1600dp-xhdpi")
class NodeContextConfigSnapshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test fun sheetLight() = capture("light", false, 1f, null)

    @Test fun sheetDark() = capture("dark", true, 1f, null)

    @Test fun sheetLarge() = capture("large", false, 1.3f, null)

    @Test fun sheetSpanishLarge() = capture("es_large", false, 1.3f, "es")

    @Test fun sheetRussianLarge() = capture("ru_large", false, 1.3f, "ru")

    private fun capture(name: String, dark: Boolean, fontScale: Float, language: String?) {
        var enabled by mutableStateOf(true)
        var labels = emptyList<String>()
        compose.setContent {
            val context = LocalContext.current
            val density = LocalDensity.current
            val config = Configuration(LocalConfiguration.current).apply {
                if (language != null) setLocale(Locale.forLanguageTag(language))
            }
            val translated = context.createConfigurationContext(config)
            CompositionLocalProvider(
                LocalContext provides translated,
                LocalConfiguration provides config,
                LocalDensity provides Density(density.density, fontScale),
            ) {
                labels = listOf(
                    R.string.orchestrator_context_original_task_label,
                    R.string.orchestrator_context_chat_history_label,
                    R.string.orchestrator_context_long_term_memory_label,
                    R.string.orchestrator_context_tool_results_label,
                ).map(translated::getString)
                KnotworkTheme(darkTheme = dark) {
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        NodeConfigSheetBody(
                            config = OutputConfig(title = "Reply", systemPrompt = "Summarise the result."),
                            errors = emptyMap(),
                            onChange = {},
                            onCancel = {},
                            onSave = {},
                            extraSection = {
                                NodeContextConfigSection(
                                    originalTask = enabled,
                                    chatHistory = false,
                                    longTermMemory = false,
                                    toolResults = false,
                                    onOriginalTaskChange = { enabled = it },
                                    onChatHistoryChange = {},
                                    onLongTermMemoryChange = {},
                                    onToolResultsChange = {},
                                )
                            },
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        val row = compose.onNodeWithText(labels.first())
        row.assertIsDisplayed().assertHasClickAction().assertIsOn()
        labels.forEachIndexed { index, label ->
            val option = compose.onNodeWithText(label)
            option.assertIsDisplayed().assertHasClickAction()
            option.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            if (index > 0) option.assertIsOff()
            val bounds = option.getBoundsInRoot()
            assertTrue("Checkbox $label must be at least 48 dp", (bounds.bottom - bounds.top).value >= 48f)
        }
        if (fontScale == 1f && language == null) {
            compose.onNodeWithText("Less input makes runs faster and cheaper.").assertIsDisplayed()
            val bounds = compose.onAllNodes(hasSetTextAction())[0].getBoundsInRoot()
            val height = (bounds.bottom - bounds.top).value
            assertTrue("Text field should be at least 48 dp, was $height", height >= 48f)
            assertTrue("Text field should be shorter than the original 56 dp, was $height", height < 56f)
            val variable = compose.onNodeWithText("\$DATE").getBoundsInRoot()
            assertTrue(
                "Variable chip touch height should be at least 48 dp",
                (variable.bottom - variable.top).value >= 48f,
            )
            assertTrue(
                "Variable chip touch width should be at least 48 dp",
                (variable.right - variable.left).value >= 48f,
            )
        }
        compose.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/node_context_sheet_$name.png",
            roborazziOptions = AppRoborazziOptions,
        )
        row.performClick()
        compose.runOnIdle { assertFalse("The whole row must toggle the original-task checkbox", enabled) }
    }
}
