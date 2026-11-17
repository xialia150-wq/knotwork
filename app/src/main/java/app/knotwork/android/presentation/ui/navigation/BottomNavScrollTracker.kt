package app.knotwork.android.presentation.ui.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Decides when the bottom bar draws its hairline: once the body above it has
 * been scrolled away from its top, and not before.
 *
 * The bar is the same colour as the page, so a permanent divider would draw a
 * seam the design asks not to have; without any divider, a list that starts to
 * pass under the chrome has nothing separating its edge from the bar's. This
 * tracker is the middle answer: it accumulates the scroll deltas a scrollable
 * inside the shell actually consumed and reports whether the body has travelled
 * at all from its top.
 *
 * Deliberately approximate, and documented as such: it counts consumed travel
 * rather than asking a particular `LazyListState`, because the shell has no
 * handle on the active screen's list and should not grow one — every tab screen
 * owns its own scroll state. A screen that is not scrolled reports zero and
 * therefore no divider; a screen scrolled down and back up returns to zero. A
 * restored scroll position that never produced a delta starts at zero, which
 * under-shows the divider for one frame rather than over-showing it.
 *
 * A class rather than plain state so the arithmetic is unit-testable without a
 * composition, and [scrolledUnder] is Compose state so the bar recomposes only
 * when the answer flips.
 */
@Stable
internal class BottomNavScrollTracker {

    /** Whether the body has been scrolled away from its top since the last reset. */
    var scrolledUnder: Boolean by mutableStateOf(false)
        private set

    /** Net travel from the top, in pixels. Never negative: the top is the floor. */
    private var travelledPx: Float = 0f

    /** Forgets the measured travel; called when the destination changes. */
    fun reset() {
        travelledPx = 0f
        scrolledUnder = false
    }

    /**
     * Feeds one nested-scroll delta the body's scrollable actually consumed.
     *
     * @param deltaY Consumed delta on the vertical axis, in pixels. Negative
     *   means the body moved towards its end (the usual "scroll down" gesture),
     *   so it is subtracted from the travel.
     */
    fun onConsumedScroll(deltaY: Float) {
        travelledPx = (travelledPx - deltaY).coerceAtLeast(0f)
        val next = travelledPx >= SCROLLED_UNDER_THRESHOLD_PX
        if (next != scrolledUnder) scrolledUnder = next
    }

    private companion object {
        /** One pixel is travel enough: below it the deltas are overscroll noise. */
        const val SCROLLED_UNDER_THRESHOLD_PX: Float = 1f
    }
}
