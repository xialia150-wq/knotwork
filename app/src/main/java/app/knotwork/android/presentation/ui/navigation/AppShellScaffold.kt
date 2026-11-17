package app.knotwork.android.presentation.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.get
import app.knotwork.android.presentation.state.TransientMessageRelay
import app.knotwork.android.presentation.ui.common.resolve
import app.knotwork.design.components.misc.KnotworkSnackbarHost
import app.knotwork.design.components.text.Text
import app.knotwork.design.theme.KnotworkTheme
import app.knotwork.design.tokens.KnotworkIconSizes
import app.knotwork.design.tokens.KnotworkTextStyles

/**
 * Root scaffold for the post-splash, post-onboarding app surface.
 *
 * Owns three responsibilities:
 *
 *  1. **Bottom-nav chrome.** Renders the four [TAB_DESTINATIONS] tabs on a bar
 *     that is flush with the bottom and painted the page's own colour, so it
 *     reads as the bottom of the page rather than a raised slab. Selection is
 *     three signals and no pill: the glyph takes the accent colour, the label
 *     goes semi-bold, and a small accent dot appears under the glyph. A hairline
 *     between the body and the bar fades in only once the body has been scrolled
 *     away from its top (see [BottomNavScrollTracker]); a permanent divider would
 *     draw a seam the page-coloured design does not have. Every selection state
 *     change runs on [NAV_STATE_ANIMATION_MS], or instantly under reduced motion.
 *     Visibility per route is decided by [shouldShowBottomNav] (a pure function
 *     so it is unit-testable); show/hide uses an [AnimatedVisibility] slide so
 *     the editor / onboarding entry doesn't snap the body.
 *
 *     The light frosted-glass backdrop was **not** implemented; the bar paints the
 *     page colour as a solid, which is the degradation the spec allows for a slow
 *     device. Two reasons, both structural. A real backdrop blur needs the
 *     scrolling body drawn *under* the bar, and this app lays the body above it
 *     (Scaffold's bottom slot, with the chat composer already pinned at the body's
 *     bottom edge and the whole shell IME-padded); moving the body under the bar
 *     would have every tab screen reserving the bar's height in its own scroll
 *     padding, and no third-party blur library may be added. And because nothing
 *     scrolls beneath the bar today, a translucent colour would composite over
 *     the page's own colour and show nothing — the solid is both the fallback and
 *     the honest rendering. A degradation path needs no setting, so no persisted
 *     switch was added.
 *
 *  2. **Tab-switch state preservation.** Selecting a tab uses the canonical
 *     Compose Navigation pattern (`popUpTo(startDestination) {
 *     saveState = true } + restoreState = true`) — preserves each tab's
 *     inner back-stack and scroll position across switches and rotations.
 *
 *  3. **Root-tab Back behaviour.** While the back stack holds nothing but
 *     tab roots ([isTabRootStack]), [BackHandler] short-circuits to
 *     `activity.finish()`. As soon as any deeper screen is on the stack the
 *     handler is disabled and default Back (pop the inner stack) takes over.
 *
 *     Arming it is not the same as exiting, and the difference is a race
 *     this code should not be relying on: `NavHost` installs its own
 *     `BackHandler` too, and `OnBackPressedDispatcher` gives the press to the
 *     most recently added enabled callback. Measured on a device, an armed
 *     shell handler over a poppable stack lost to the NavHost in four runs and
 *     won in one — so "armed" predicts nothing reliable when there is still
 *     something to pop.
 *
 *     That is the argument for [isTabRootStack] rather than a check on the
 *     current route: over a stack that still has entries underneath, the shell
 *     is simply never armed, and Back pops no matter which handler wins.
 *
 * Both (1)'s highlight and (3)'s exit predicate are derived from the **back
 * stack**, never from the current route alone — see [TabOwnership] for why the
 * old `route → tab` table was removed and what changed with it.
 *
 * The host activity's `KnotworkAppTheme` is the single source of
 * truth for the current scheme, and system-theme changes already cause a
 * natural Compose recomposition.
 *
 * @param navController The host activity's [NavHostController]; the
 *        composable observes the current back stack through it.
 * @param content The nav-graph composable to host. Typically `AppNavGraph`.
 */
@Composable
fun AppShellScaffold(
    navController: NavHostController,
    transientMessageRelay: TransientMessageRelay,
    content: @Composable (innerPadding: PaddingValues) -> Unit,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute: String? = navBackStackEntry?.destination?.route
    val showBottomNav = shouldShowBottomNav(currentRoute)
    val activity = LocalActivity.current

    // The whole stack, not just its top. `NavController.currentBackStack` is
    // `@RestrictTo(LIBRARY_GROUP)` — its own KDoc points library consumers at
    // the navigator's public `backStack` instead, which is what this reads. It
    // carries only `composable` destinations (no `NavGraph` entries and no
    // root-graph placeholder), which is exactly the set of routes a user can be
    // "on".
    //
    // The read has to happen from an effect: `backStack` throws until the
    // navigator is attached, and attachment happens while `content()`'s NavHost
    // composes — i.e. *after* this scaffold's own composition. Effects run once
    // the composition is applied, so by then the navigator is live. The effect
    // is additionally keyed on `navBackStackEntry` so it re-arms on the next
    // destination change if a host ever composes the NavHost later still, and
    // the `isAttached` guard keeps a scaffold used without a NavHost showing no
    // highlight instead of crashing.
    val composeNavigator = remember(navController) {
        navController.navigatorProvider[ComposeNavigator::class]
    }
    var stackRoutes: List<String> by remember(navController) { mutableStateOf(emptyList()) }
    LaunchedEffect(composeNavigator, navBackStackEntry) {
        if (!composeNavigator.isAttached) return@LaunchedEffect
        composeNavigator.backStack.collect { entries ->
            stackRoutes = entries.mapNotNull { it.destination.route }
        }
    }
    val owningTab: String? = owningTabRoute(stackRoutes)

    // The body's travel decides whether the bar shows its hairline. The tracker
    // is fed from one nested-scroll connection installed over the whole body, so
    // whichever screen owns the scrollable, its consumed deltas are what counts —
    // the shell never reaches into a screen's scroll state.
    val scrollTracker = remember { BottomNavScrollTracker() }
    LaunchedEffect(currentRoute) { scrollTracker.reset() }
    val bodyScrollConnection = remember(scrollTracker) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    scrollTracker.onConsumedScroll(deltaY = consumed.y)
                }
                return Offset.Zero
            }
        }
    }

    BackHandler(enabled = isTabRootStack(stackRoutes)) {
        activity?.finish()
    }

    // Activity-level snackbar host: outlives every NavGraph destination
    // so messages emitted from a screen that pops itself off the
    // back-stack (today: onboarding's skip-flow hint) still render
    // *after* navigation settles on the next destination.
    val shellContext = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(transientMessageRelay) {
        transientMessageRelay.messages.collect { message ->
            snackbarHostState.showSnackbar(message = shellContext.resolve(message))
        }
    }

    // Wrap the whole shell in `imePadding()` so the bottom-nav + body slide
    // up in lockstep with the keyboard. Without this the bottom-nav slot
    // keeps reserving layout space behind the IME and any IME-padded
    // composer above it has to compete with that reserved-but-hidden area,
    // which produces a visible gap + a jump when the IME animation
    // finishes (the keyboard tween and an `AnimatedVisibility` hide-anim
    // run on their own clocks). With the whole Scaffold tracking the IME
    // inset directly, the keyboard, bottom-nav, and composer all move on
    // the same frame.
    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            // The Scaffold lifts it above the bottom nav; with the nav hidden
            // (splash, onboarding, editor, sheets) nothing below it absorbs the
            // system navigation bar — this Scaffold's content insets are zero —
            // so the host clears it itself.
            KnotworkSnackbarHost(
                hostState = snackbarHostState,
                modifier = if (showBottomNav) Modifier else Modifier.navigationBarsPadding(),
            )
        },
        bottomBar = {
            // Bind the bottom-nav slide to `KnotworkTheme.motion.dur3` (`easeStd`)
            // so the duration rides the design-system token rather than the
            // Compose internal default. Reduced-motion collapses the transition
            // to an instant swap — Material's slide spec
            // is not respected by the system `TRANSITION_ANIMATION_SCALE`, so an
            // explicit gate is the only way to honour the user preference.
            val reduceMotion = KnotworkTheme.a11y.reducedMotion()
            val durationMs = KnotworkTheme.motion.dur3
            val easing = KnotworkTheme.motion.easeStd
            val enter = if (reduceMotion) {
                EnterTransition.None
            } else {
                slideInVertically(
                    animationSpec = tween(durationMillis = durationMs, easing = easing),
                    initialOffsetY = { it },
                ) + fadeIn(animationSpec = tween(durationMillis = durationMs, easing = easing))
            }
            val exit = if (reduceMotion) {
                ExitTransition.None
            } else {
                slideOutVertically(
                    animationSpec = tween(durationMillis = durationMs, easing = easing),
                    targetOffsetY = { it },
                ) + fadeOut(animationSpec = tween(durationMillis = durationMs, easing = easing))
            }
            AnimatedVisibility(
                visible = showBottomNav,
                enter = enter,
                exit = exit,
            ) {
                AppBottomNavigationBar(
                    owningTabRoute = owningTab,
                    scrolledUnder = scrollTracker.scrolledUnder,
                    onTabSelected = { tab -> navController.navigateToTab(tab.route) },
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.nestedScroll(bodyScrollConnection).padding(innerPadding)) {
            content(PaddingValues())
        }
    }
}

/**
 * The four-tab bottom bar: page-coloured, flush with the bottom, and separated
 * from the body only by the hairline that appears once the body has scrolled.
 *
 * The container is [MaterialTheme.colorScheme.background] — the page's own
 * colour — rather than a raised tonal surface, which is what lets the bar sit on
 * the page as its bottom edge. It carries no Material indicator pill: the
 * selected item is marked by the accent-coloured glyph, the semi-bold label and
 * the dot under the glyph, all of which animate over [NAV_STATE_ANIMATION_MS].
 *
 * @param owningTabRoute The tab route that owns the current back stack, or
 *   `null` when no tab is on it (splash, onboarding, a modal sheet).
 * @param scrolledUnder Whether the body has been scrolled away from its top;
 *   the hairline fades in while it has.
 * @param onTabSelected Invoked with the tab the user tapped.
 * @param modifier Layout modifier applied to the bar's column.
 */
@Composable
internal fun AppBottomNavigationBar(
    owningTabRoute: String?,
    scrolledUnder: Boolean,
    onTabSelected: (TabDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = KnotworkTheme.a11y.reducedMotion()
    val barColor = MaterialTheme.colorScheme.background
    val dividerAlpha by animateFloatAsState(
        targetValue = if (scrolledUnder) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (reduceMotion) 0 else NAV_STATE_ANIMATION_MS,
            easing = KnotworkTheme.motion.easeStd,
        ),
        label = "bottomNavDivider",
    )
    Column(modifier = modifier.fillMaxWidth().background(barColor)) {
        HorizontalDivider(modifier = Modifier.alpha(dividerAlpha), color = KnotworkTheme.extended.divider)
        NavigationBar(containerColor = barColor, tonalElevation = 0.dp) {
            TAB_DESTINATIONS.forEach { tab ->
                BottomNavItem(
                    tab = tab,
                    selected = owningTabRoute == tab.route,
                    reduceMotion = reduceMotion,
                    onClick = { onTabSelected(tab) },
                )
            }
        }
    }
}

/**
 * One tab of [AppBottomNavigationBar]: glyph, selection dot and label.
 *
 * @param tab The destination this item renders.
 * @param selected Whether the current back stack is owned by this tab.
 * @param reduceMotion Collapses the state animation to an instant swap.
 * @param onClick Invoked when the item is tapped.
 */
@Composable
private fun RowScope.BottomNavItem(
    tab: TabDestination,
    selected: Boolean,
    reduceMotion: Boolean,
    onClick: () -> Unit,
) {
    val stateAnimation = tween<Color>(
        durationMillis = if (reduceMotion) 0 else NAV_STATE_ANIMATION_MS,
        easing = KnotworkTheme.motion.easeStd,
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else KnotworkTheme.extended.onSurfaceMuted,
        animationSpec = stateAnimation,
        label = "bottomNavIconColor",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSurface else KnotworkTheme.extended.onSurfaceMuted,
        animationSpec = stateAnimation,
        label = "bottomNavLabelColor",
    )
    val dotAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (reduceMotion) 0 else NAV_STATE_ANIMATION_MS,
            easing = KnotworkTheme.motion.easeStd,
        ),
        label = "bottomNavDotAlpha",
    )
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(vertical = KnotworkTheme.spacing.sp1),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (selected) tab.iconSelected else tab.iconUnselected,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(KnotworkIconSizes.AppBar),
        )
        Box(
            modifier = Modifier
                .padding(top = NavDotGap)
                .size(NavDotSize)
                .alpha(dotAlpha)
                .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape),
        )
        Text(
            text = stringResource(tab.labelRes),
            modifier = Modifier.padding(top = NavDotGap),
            style = KnotworkTextStyles.LabelSm.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                letterSpacing = 0.2.sp,
            ),
            color = labelColor,
            maxLines = 1,
        )
    }
}

/** Duration of a bottom-nav selection change, per spec: about 150 ms. */
private const val NAV_STATE_ANIMATION_MS: Int = 150

/** Diameter of the dot under the selected tab's glyph. */
@Suppress("MagicNumber") // Spec value, not derived from another token.
private val NavDotSize = 4.dp

/** Gap between a glyph, its selection dot and its label. */
@Suppress("MagicNumber") // Spec value, not derived from another token.
private val NavDotGap = 3.dp

/**
 * Anchor route used as the [popUpTo] target during tab switches.
 *
 * We intentionally do **not** use `navController.graph.findStartDestination()`
 * here: this graph's start destination is [NavRoutes.SPLASH], and the
 * splash handler removes itself from the back-stack with
 * `popUpTo(SPLASH) { inclusive = true }` once initialization completes.
 * After that, the splash id is no longer on the stack, so passing it as
 * the `popUpTo` target makes the pop a silent no-op — root-tab entries
 * accumulate and the documented `saveState` / `restoreState` behaviour
 * stops working.
 *
 * Pinning the anchor to [NavRoutes.CHAT_TAB] (the first tab, navigated
 * to right after splash) gives us a route that is guaranteed to be on
 * the stack at the bottom whenever a tab-switch can happen. The Chat
 * tab itself stays permanently anchored at the bottom; every other tab
 * enters above it and is popped + saved on the next switch, which is
 * exactly the Material 3 NavigationSuite back-stack contract.
 */
internal const val TAB_BACK_STACK_ANCHOR: String = NavRoutes.CHAT_TAB

/**
 * Canonical nav-options for switching to a tab destination.
 *
 * - `popUpTo(TAB_BACK_STACK_ANCHOR) { saveState = true }` pops every
 *   entry above the anchor while preserving the popped tabs' state
 *   (back-stack, scroll position, ViewModel state) so re-selection
 *   restores the user where they left off.
 * - `launchSingleTop = true` avoids stacking duplicate copies of a tab
 *   when the user taps the same tab repeatedly.
 * - `restoreState = true` replays the saved state when revisiting a tab.
 */
internal fun NavOptionsBuilder.applyTabSwitchOptions() {
    popUpTo(TAB_BACK_STACK_ANCHOR) {
        saveState = true
    }
    launchSingleTop = true
    restoreState = true
}

/**
 * The **only** sanctioned way to reach a bottom-nav tab root.
 *
 * A tab root pushed on top of another subtree's back stack is the defect behind
 * the closed-test finding `#14`: `Settings → Tools & workspace → Manage tools`
 * ran a bare `navigate(NavRoutes.TOOLS)`, which left the user on the Tools tab
 * with the settings subtree still buried underneath, and flipped the bottom-nav
 * highlight to a tab they had not chosen — the app claiming to be somewhere the
 * user never went.
 *
 * Routing every tab-root entry through this helper makes that shape
 * unrepresentable: a tab is always entered as a *tab switch*, collapsing the
 * stack to the anchor exactly as tapping the nav bar does.
 * `TabRootEntryKonsistTest` fails the build if a bare `navigate(<tab root>)`
 * reappears anywhere in the navigation package.
 *
 * Screens that legitimately want to *push* a destination (a tool detail, the
 * MCP form, a settings category) keep using `navigate` directly — the rule is
 * about tab **roots**, not about navigation in general.
 *
 * @param route a tab root route, i.e. a member of [TAB_ROOT_ROUTES].
 */
internal fun NavHostController.navigateToTab(route: String) {
    navigate(route) { applyTabSwitchOptions() }
}
