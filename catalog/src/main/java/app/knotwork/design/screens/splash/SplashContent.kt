package app.knotwork.design.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.knotwork.design.R
import app.knotwork.design.components.brand.KnotworkLogoSize
import app.knotwork.design.components.buttons.KnotworkPrimaryButton
import app.knotwork.design.components.buttons.KnotworkSecondaryButton
import app.knotwork.design.components.dialogs.TypedConfirmDialog
import app.knotwork.design.components.dialogs.TypedConfirmDialogState
import app.knotwork.design.components.text.Text
import app.knotwork.design.theme.KnotworkTheme
import app.knotwork.design.tokens.KnotworkTextStyles

/** Maximum content width of the splash column — keeps the layout readable on tablets. */
private val SplashMaxWidth = 360.dp
private val PlatformSplashIconSize = 288.dp

// Kept in sync with brand/zhiwen-thread.svg and the adaptive icon.
internal val BRAND_PATHS = listOf(
    "M30.16,40.11 L25.36,48.43 Q24.91,49.52 26.19,49.52 L32.08,48.88",
    "M35.28,51.63 L25.68,57.71 Q25.04,58.93 26.45,58.54 L35.92,56.11",
    "M25.55,66.67 L35.92,61.87",
    "M42.64,41.71 L50.38,41.71 Q52.94,41.71 52.94,44.27 L52.94,51.76 Q52.94,54.32 50.38,54.32 " +
        "L42.64,54.32 Q40.08,54.32 40.08,51.76 L40.08,44.27 Q40.08,41.71 42.64,41.71",
    "M43.09,59.18 L36.24,67.89",
    "M49.87,59.25 Q56.08,67.31 58.64,67.31 Q63.38,67.31 63.38,62.51 L63.38,47.54",
    "M65.1,40.5 L68.18,43.44",
    "M72.98,41.9 L80.85,41.9 Q82.64,41.9 82.64,43.7 L82.64,64.11 Q82.64,67.31 80.08,67.31 " +
        "L78.54,67.31",
    "M70.61,49.71 L74.19,49.71 Q76.24,49.71 76.24,51.76 L76.24,58.29 Q76.24,60.34 74.19,60.34 " +
        "L70.61,60.34 Q68.56,60.34 68.56,58.29 L68.56,51.76 Q68.56,49.71 70.61,49.71",
)
private const val THREAD_VIEWPORT = 108f
private const val THREAD_STROKE = 3.2f
private const val THREAD_DURATION_MS = 1000
private const val PROGRESS_TRACK_ALPHA = 0.2f
private const val MARK_MAX_HEIGHT_FRACTION = 0.45f

/**
 * Stateless Knotwork splash surface. Renders the brand mark + app name +
 * determinate progress bar + status label; on error, swaps the indicator
 * for an inline error message and Retry CTA.
 *
 * @param appName brand text rendered under the logo.
 * @param state render state — drives loading vs. error layout.
 * @param modifier optional layout modifier applied to the root box.
 * @param callbacks one-shot callback bundle (Retry).
 * @param splashAnimationStartMillis Platform icon animation start time, in epoch milliseconds.
 */
@Composable
fun SplashContent(
    appName: String,
    state: SplashViewState,
    modifier: Modifier = Modifier,
    retryLabel: String = stringResource(R.string.knotwork_splash_retry),
    callbacks: SplashCallbacks = noopSplashCallbacks(),
    splashAnimationStartMillis: Long? = null,
) {
    val starting = state is SplashViewState.Initializing || state is SplashViewState.Loading
    val startupInk = colorResource(R.color.brand_startup_ink)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (starting) colorResource(R.color.brand_startup_background) else MaterialTheme.colorScheme.background,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (starting) {
            val markSize = minOf(PlatformSplashIconSize, maxHeight * MARK_MAX_HEIGHT_FRACTION)
            SplashThread(splashAnimationStartMillis, Modifier.size(markSize))
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .systemBarsPadding()
                    .widthIn(max = SplashMaxWidth)
                    .padding(horizontal = KnotworkTheme.spacing.sp8, vertical = KnotworkTheme.spacing.sp8),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(KnotworkTheme.spacing.sp6),
            ) {
                SplashDetails(appName, state, retryLabel, callbacks, startupInk)
            }
        } else {
            Column(
                modifier = Modifier
                    .systemBarsPadding()
                    .widthIn(max = SplashMaxWidth)
                    .padding(horizontal = KnotworkTheme.spacing.sp8),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(KnotworkTheme.spacing.sp6),
            ) {
                SplashThread(splashAnimationStartMillis, Modifier.size(KnotworkLogoSize.Lg.size))
                SplashDetails(appName, state, retryLabel, callbacks, MaterialTheme.colorScheme.onSurface)
            }
        }
    }
    if (state is SplashViewState.DataLocked && state.resetDialog != null) {
        SplashResetConfirmDialog(
            payload = state.resetDialog,
            onInputChange = callbacks.onResetInputChange,
            onConfirm = callbacks.onResetConfirm,
            onDismiss = callbacks.onResetDismiss,
        )
    }
}

@Composable
private fun SplashDetails(
    appName: String,
    state: SplashViewState,
    retryLabel: String,
    callbacks: SplashCallbacks,
    textColor: Color,
) {
    Text(
        text = appName,
        style = KnotworkTextStyles.Display2xl,
        color = textColor,
        textAlign = TextAlign.Center,
    )
    when (state) {
        SplashViewState.Initializing -> SplashProgress(progress = 0f, message = "")
        is SplashViewState.Loading -> SplashProgress(progress = state.progress, message = state.message)
        is SplashViewState.Error -> SplashError(
            message = state.message,
            retryLabel = retryLabel,
            onRetry = callbacks.onRetry,
        )
        is SplashViewState.DataLocked -> SplashDataLocked(state = state, retryLabel = retryLabel, callbacks = callbacks)
    }
}

/** Reveals the rounded strokes in order without delaying initialization. */
@Composable
private fun SplashThread(splashAnimationStartMillis: Long?, modifier: Modifier = Modifier) {
    val reducedMotion = KnotworkTheme.a11y.reducedMotion()
    val progress = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val measures = remember {
        BRAND_PATHS.map { path -> PathMeasure().apply { setPath(PathParser().parsePathString(path).toPath(), false) } }
    }
    val totalLength = remember(measures) { measures.sumOf { it.length.toDouble() }.toFloat() }
    val segment = remember { Path() }
    val ink = colorResource(R.color.brand_startup_ink)
    LaunchedEffect(reducedMotion, splashAnimationStartMillis) {
        if (reducedMotion) {
            progress.snapTo(1f)
        } else {
            val elapsed = splashThreadElapsed(splashAnimationStartMillis, System.currentTimeMillis())
            progress.snapTo(elapsed.toFloat() / THREAD_DURATION_MS)
            val remaining = THREAD_DURATION_MS - elapsed.toInt()
            if (remaining > 0) progress.animateTo(1f, tween(remaining, easing = LinearEasing))
        }
    }
    Canvas(modifier) {
        var remainingLength = totalLength * progress.value
        scale(size.width / THREAD_VIEWPORT, pivot = Offset.Zero) {
            measures.forEach { measure ->
                segment.reset()
                if (remainingLength > 0f) {
                    measure.getSegment(0f, minOf(measure.length, remainingLength), segment, true)
                    drawPath(
                        segment,
                        ink,
                        style = Stroke(width = THREAD_STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
                remainingLength -= measure.length
            }
        }
    }
}

/** Uses the platform clock when available; a missing platform timestamp shows the complete mark. */
internal fun splashThreadElapsed(startMillis: Long?, nowMillis: Long): Long = when {
    startMillis == null -> 0L // Standalone catalog previews have no platform splash.
    startMillis <= 0L -> THREAD_DURATION_MS.toLong()
    else -> (nowMillis - startMillis).coerceIn(0, THREAD_DURATION_MS.toLong())
}

@Composable
private fun SplashProgress(progress: Float, message: String) {
    val ink = colorResource(R.color.brand_startup_ink)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KnotworkTheme.spacing.sp3),
        modifier = Modifier.fillMaxWidth(),
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = ink,
            trackColor = ink.copy(alpha = PROGRESS_TRACK_ALPHA),
            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
        )
        if (message.isNotEmpty()) {
            Text(
                text = message,
                style = KnotworkTextStyles.BodyBase,
                color = ink,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SplashError(message: String, retryLabel: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KnotworkTheme.spacing.sp4),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = message,
            style = KnotworkTextStyles.BodyBase,
            color = KnotworkTheme.extended.signalError,
            textAlign = TextAlign.Center,
        )
        KnotworkPrimaryButton(text = retryLabel, onClick = onRetry)
    }
}

/**
 * Recovery surface for [SplashViewState.DataLocked]: headline, plain-language
 * explanation, a primary Retry CTA and a destructive secondary reset action.
 * The reset itself is armed only through [SplashResetConfirmDialog].
 */
@Composable
private fun SplashDataLocked(state: SplashViewState.DataLocked, retryLabel: String, callbacks: SplashCallbacks) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KnotworkTheme.spacing.sp4),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = state.title,
            style = KnotworkTextStyles.TitleMd,
            color = KnotworkTheme.extended.signalError,
            textAlign = TextAlign.Center,
        )
        Text(
            text = state.body,
            style = KnotworkTextStyles.BodyBase,
            color = KnotworkTheme.extended.onSurfaceMuted,
            textAlign = TextAlign.Center,
        )
        KnotworkPrimaryButton(
            text = retryLabel,
            onClick = callbacks.onRetry,
            modifier = Modifier.testTag(SPLASH_DATA_LOCKED_RETRY_TEST_TAG),
        )
        KnotworkSecondaryButton(
            text = state.resetLabel,
            onClick = callbacks.onResetRequest,
            destructive = true,
            modifier = Modifier.testTag(SPLASH_DATA_LOCKED_RESET_TEST_TAG),
        )
    }
}

/**
 * Typed-confirm dialog gating the irreversible full data reset. Thin wrapper
 * over the shared [TypedConfirmDialog], so the splash and Settings destructive
 * confirmations share one interaction contract.
 */
@Composable
private fun SplashResetConfirmDialog(
    payload: SplashResetDialogState,
    onInputChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    TypedConfirmDialog(
        state = TypedConfirmDialogState(
            title = payload.title,
            body = payload.body,
            keyword = payload.keyword,
            hint = payload.hint,
            pendingInput = payload.pendingInput,
        ),
        confirmLabel = payload.confirmLabel,
        cancelLabel = payload.cancelLabel,
        onInputChange = onInputChange,
        onConfirm = onConfirm,
        onCancel = onDismiss,
        fieldTestTag = SPLASH_RESET_TYPED_FIELD_TEST_TAG,
        confirmTestTag = SPLASH_RESET_CONFIRM_BUTTON_TEST_TAG,
    )
}

/** Test tag of the Retry button on the data-locked recovery surface. */
const val SPLASH_DATA_LOCKED_RETRY_TEST_TAG: String = "splash_data_locked_retry"

/** Test tag of the destructive reset button on the data-locked recovery surface. */
const val SPLASH_DATA_LOCKED_RESET_TEST_TAG: String = "splash_data_locked_reset"

/** Test tag of the typed-confirm input inside the reset dialog. */
const val SPLASH_RESET_TYPED_FIELD_TEST_TAG: String = "splash_reset_typed_field"

/** Test tag of the confirm button inside the reset dialog. */
const val SPLASH_RESET_CONFIRM_BUTTON_TEST_TAG: String = "splash_reset_confirm_button"
