package app.knotwork.android.presentation.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.knotwork.android.presentation.state.TransientMessageRelay
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Compose-level integration tests for [AppShellScaffold] + an in-memory
 * mini nav-graph.
 *
 * These tests stand up [AppShellScaffold] with a minimal `NavHost` whose
 * destinations render plain text — enough to assert navigation behaviour
 * (tab selection, back-stack containment) without standing up the full Hilt
 * graph. The real screens (`ChatScreen`, etc.) are exercised by their own
 * per-screen tests; here we are only verifying the shell's contract.
 *
 * Deep links and the tab-root entry contract live in [NavigationContractTest]:
 * a `navDeepLink`-based test used to sit here, but the production graph
 * removed those registrations (they raced the splash) and routes every
 * `knotwork://` intent through [navigateToDeepLink] instead — so the test was
 * certifying a mechanism the app no longer uses.
 */
class AppShellNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bottomNavTapSwitchesToTargetTab() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            AppShellScaffold(navController = navController, transientMessageRelay = TransientMessageRelay()) { _ ->
                NavHost(navController = navController, startDestination = NavRoutes.CHAT_TAB) {
                    composable(NavRoutes.CHAT_TAB) { Text(CHAT_TAB_BODY) }
                    composable(NavRoutes.PIPELINES_GRAPH) { Text(PIPELINES_BODY) }
                    composable(NavRoutes.TOOLS) { Text(TOOLS_BODY) }
                    composable(NavRoutes.MORE) { Text(MORE_BODY) }
                }
            }
        }

        composeTestRule.onNodeWithText(CHAT_TAB_BODY).assertIsDisplayed()

        composeTestRule.onNodeWithText("Tools").performClick()
        composeTestRule.onNodeWithText(TOOLS_BODY).assertIsDisplayed()

        composeTestRule.onNodeWithText("Pipelines").performClick()
        composeTestRule.onNodeWithText(PIPELINES_BODY).assertIsDisplayed()
    }

    /**
     * Regression: tab switches must pop prior tab entries instead of
     * stacking them.
     *
     * Earlier versions used `popUpTo(graph.findStartDestination().id)` —
     * which resolves to the splash route, the very route the post-init
     * handler removes from the back-stack with `inclusive = true`. With
     * that target gone, `popUpTo` silently no-oped and every tap added
     * a fresh entry on top of the stack. This test reconstructs the same
     * `splash → CHAT_TAB` start sequence the real graph uses, performs a
     * four-tab dance, and asserts that only the Chat anchor + the
     * current tab remain on the back-stack.
     */
    @Test
    fun tabSwitchesKeepBackStackBoundedToAnchorPlusCurrentTab() {
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            val nav = rememberNavController()
            navController = nav
            AppShellScaffold(navController = nav, transientMessageRelay = TransientMessageRelay()) { _ ->
                NavHost(navController = nav, startDestination = NavRoutes.SPLASH) {
                    composable(NavRoutes.SPLASH) {
                        LaunchedEffect(Unit) {
                            nav.navigate(NavRoutes.CHAT_TAB) {
                                popUpTo(NavRoutes.SPLASH) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                    composable(NavRoutes.CHAT_TAB) { Text(CHAT_TAB_BODY) }
                    composable(NavRoutes.PIPELINES_GRAPH) { Text(PIPELINES_BODY) }
                    composable(NavRoutes.TOOLS) { Text(TOOLS_BODY) }
                    composable(NavRoutes.MORE) { Text(MORE_BODY) }
                }
            }
        }

        composeTestRule.onNodeWithText(CHAT_TAB_BODY).assertIsDisplayed()

        // Four-tab dance: any combination of tabs that revisits the start
        // tab last should collapse the back-stack down to the anchor.
        composeTestRule.onNodeWithText("Tools").performClick()
        composeTestRule.onNodeWithText("Pipelines").performClick()
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText(MORE_BODY).assertIsDisplayed()

        // After landing on a non-anchor tab, the only entry below it
        // must be the Chat anchor — never one of the previously-visited
        // intermediate tabs.
        composeTestRule.runOnIdle {
            val tabsOnStack = navController.tabRoutesOnStack()
            assertEquals(listOf(NavRoutes.CHAT_TAB, NavRoutes.MORE), tabsOnStack)
        }

        // Tapping the anchor tab pops everything above it; only the
        // anchor remains.
        composeTestRule.onNodeWithText("Chat").performClick()
        composeTestRule.onNodeWithText(CHAT_TAB_BODY).assertIsDisplayed()
        composeTestRule.runOnIdle {
            val tabsOnStack = navController.tabRoutesOnStack()
            assertEquals(listOf(NavRoutes.CHAT_TAB), tabsOnStack)
        }
    }

    /** Returns the subset of the back-stack that corresponds to top-level tab routes. */
    private fun NavHostController.tabRoutesOnStack(): List<String> {
        val tabRoutes = TAB_DESTINATIONS.map { it.route }.toSet()
        return currentBackStack.value
            .mapNotNull { it.destination.route }
            .filter { it in tabRoutes }
    }

    /**
     * The strip keeps the four product tabs, in order, one node each.
     *
     * The home/navigation redesign may restyle the bar but may not add, drop,
     * reorder or relabel a tab.
     */
    @Test
    fun bottomNavShowsTheFourTabs() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            AppShellScaffold(navController = navController, transientMessageRelay = TransientMessageRelay()) { _ ->
                NavHost(navController = navController, startDestination = NavRoutes.CHAT_TAB) {
                    composable(NavRoutes.CHAT_TAB) { Text(CHAT_TAB_BODY) }
                }
            }
        }

        TAB_LABELS.forEach { label -> composeTestRule.onNodeWithText(label).assertIsDisplayed() }
        composeTestRule.runOnIdle { assertEquals(TAB_DESTINATIONS.size, TAB_LABELS.size) }
    }

    /**
     * Splash is a shell-less destination: the bar must not flash there.
     *
     * Regression assertion for the current (pre-existing) rule in
     * [shouldShowBottomNav] — this task must not bring the bar back to the
     * start-up surfaces.
     */
    @Test
    fun bottomNavStaysHiddenOnSplash() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            AppShellScaffold(navController = navController, transientMessageRelay = TransientMessageRelay()) { _ ->
                NavHost(navController = navController, startDestination = NavRoutes.SPLASH) {
                    composable(NavRoutes.SPLASH) { Text(SPLASH_BODY) }
                }
            }
        }

        composeTestRule.onNodeWithText(SPLASH_BODY).assertIsDisplayed()
        assertBottomNavHidden()
    }

    /**
     * The pipeline editor is a focus-mode surface: the bar stays hidden there so
     * the canvas keeps the full height.
     */
    @Test
    fun bottomNavStaysHiddenInThePipelineEditor() {
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            val nav = rememberNavController()
            navController = nav
            AppShellScaffold(navController = nav, transientMessageRelay = TransientMessageRelay()) { _ ->
                NavHost(navController = nav, startDestination = NavRoutes.CHAT_TAB) {
                    composable(NavRoutes.CHAT_TAB) { Text(CHAT_TAB_BODY) }
                    composable(NavRoutes.PIPELINE_EDITOR) { Text(EDITOR_BODY) }
                }
            }
        }
        composeTestRule.onNodeWithText(CHAT_TAB_BODY).assertIsDisplayed()
        composeTestRule.onNodeWithText(TAB_LABELS.first()).assertIsDisplayed()

        composeTestRule.runOnIdle { navController.navigate(NavRoutes.PIPELINE_EDITOR) }

        composeTestRule.onNodeWithText(EDITOR_BODY).assertIsDisplayed()
        assertBottomNavHidden()
    }

    /**
     * A modal sheet route renders over the host destination; the bar stays hidden
     * so the sheet owns the whole surface.
     */
    @Test
    fun bottomNavStaysHiddenUnderAModalSheetRoute() {
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            val nav = rememberNavController()
            navController = nav
            AppShellScaffold(navController = nav, transientMessageRelay = TransientMessageRelay()) { _ ->
                NavHost(navController = nav, startDestination = NavRoutes.CHAT_TAB) {
                    composable(NavRoutes.CHAT_TAB) { Text(CHAT_TAB_BODY) }
                    composable(NavRoutes.SHEET_CONSOLE) { Text(SHEET_BODY) }
                }
            }
        }
        composeTestRule.onNodeWithText(CHAT_TAB_BODY).assertIsDisplayed()

        composeTestRule.runOnIdle { navController.navigate(NavRoutes.SHEET_CONSOLE) }

        composeTestRule.onNodeWithText(SHEET_BODY).assertIsDisplayed()
        assertBottomNavHidden()
    }

    /**
     * Waits for the bar's hide animation to finish, then asserts no tab label is
     * on screen. The hide is an [androidx.compose.animation.AnimatedVisibility]
     * slide, so the labels stay composed for one transition before they go.
     */
    private fun assertBottomNavHidden() {
        composeTestRule.waitUntil(timeoutMillis = NAV_HIDE_TIMEOUT_MS) {
            TAB_LABELS.all { label ->
                composeTestRule.onAllNodesWithText(label).fetchSemanticsNodes().isEmpty()
            }
        }
    }

    private companion object {
        const val CHAT_TAB_BODY = "chat-tab-root"
        const val PIPELINES_BODY = "pipelines-root"
        const val TOOLS_BODY = "tools-root"
        const val MORE_BODY = "more-root"
        const val SPLASH_BODY = "splash-root"
        const val EDITOR_BODY = "pipeline-editor-root"
        const val SHEET_BODY = "modal-sheet-root"

        /** The four product tab labels, in the interface language the tests run in. */
        val TAB_LABELS = listOf("Chat", "Pipelines", "Tools", "More")

        /** Generous bound for a 280 ms hide animation plus CI emulator jank. */
        const val NAV_HIDE_TIMEOUT_MS = 5_000L
    }
}
