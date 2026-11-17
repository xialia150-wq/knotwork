package app.knotwork.android.presentation.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [BottomNavScrollTracker] — the decision behind the bottom bar's
 * hairline.
 *
 * The sign convention is the thing to keep straight: a consumed negative delta
 * means the body moved towards its end ("scrolled down"), i.e. travel away from
 * the top, and that is what shows the divider.
 */
class BottomNavScrollTrackerTest {

    @Test
    fun `a body at its top shows no divider`() {
        assertFalse(BottomNavScrollTracker().scrolledUnder)
    }

    @Test
    fun `scrolling away from the top shows the divider`() {
        val tracker = BottomNavScrollTracker()

        tracker.onConsumedScroll(deltaY = -48f)

        assertTrue(tracker.scrolledUnder)
    }

    @Test
    fun `scrolling back to the top hides the divider`() {
        val tracker = BottomNavScrollTracker()

        tracker.onConsumedScroll(deltaY = -48f)
        tracker.onConsumedScroll(deltaY = 48f)

        assertFalse(tracker.scrolledUnder)
    }

    @Test
    fun `pulling down at the top is not travel`() {
        val tracker = BottomNavScrollTracker()

        tracker.onConsumedScroll(deltaY = 48f)

        assertFalse(tracker.scrolledUnder)
    }

    @Test
    fun `a destination change forgets the travel`() {
        val tracker = BottomNavScrollTracker()
        tracker.onConsumedScroll(deltaY = -48f)

        tracker.reset()

        assertFalse(tracker.scrolledUnder)
    }
}
