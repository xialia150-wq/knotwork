package app.knotwork.android.presentation.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins where the app lands after the splash screen, and therefore what "home"
 * means for the product: the chat surface, not a launcher grid or a dashboard.
 *
 * The navigation graph itself is Hilt- and Compose-bound and cannot be stood up
 * in a JVM test, so the landing decision is a pure function the splash handler
 * calls — this test is what keeps "the app opens on the chat" true if that
 * handler is ever rewritten.
 */
class PostSplashDestinationTest {

    @Test
    fun `a returning user lands on the chat home`() {
        assertEquals(NavRoutes.CHAT_TAB, postSplashDestination(showOnboarding = false))
    }

    @Test
    fun `a first run is diverted to onboarding`() {
        assertEquals(NavRoutes.ONBOARDING, postSplashDestination(showOnboarding = true))
    }

    @Test
    fun `the home destination is a bottom-nav tab root`() {
        assertTrue(postSplashDestination(showOnboarding = false) in TAB_ROOT_ROUTES)
    }
}
