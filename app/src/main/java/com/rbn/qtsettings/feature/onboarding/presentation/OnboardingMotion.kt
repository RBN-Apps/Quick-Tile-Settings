package com.rbn.qtsettings.feature.onboarding.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.absoluteValue

/**
 * The page the pager is leaving fades and shrinks a little, so a swipe reads as depth rather than a
 * jump. Read inside the layer block: the values change per frame and never recompose the page.
 *
 * An arriving permission drops the two setup pages, so the count can shrink while a page composed
 * for the longer flow is still on screen for a frame. The pager rejects a page it no longer has,
 * and that rejection lands in the draw pass, so the stale page leaves at its plain size instead.
 */
internal fun Modifier.onboardingPageDepth(pager: PagerState, page: Int) = graphicsLayer {
    if (page !in 0 until pager.pageCount) return@graphicsLayer
    val distance = pager.getOffsetDistanceInPages(page).absoluteValue.coerceIn(0f, 1f)
    alpha = 1f - distance * 0.5f
    scaleX = 1f - distance * 0.06f
    scaleY = 1f - distance * 0.06f
}

/**
 * A repeating nudge for the arrow on the start button, in the range 0..1.
 * Returns a standing zero when the device has its animations turned down.
 */
@Composable
internal fun rememberNudge(active: Boolean): State<Float> {
    if (!active) return remember { mutableFloatStateOf(0f) }
    val transition = rememberInfiniteTransition(label = "onboarding_nudge")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = NudgeSpec,
        label = "onboarding_nudge_offset"
    )
}

private val NudgeSpec: InfiniteRepeatableSpec<Float> =
    infiniteRepeatable(tween(680, easing = FastOutSlowInEasing), RepeatMode.Reverse)

/** One short fade for anything that swaps in place: a status line, an icon, a button row. */
@Composable
internal fun <T> OnboardingCrossfade(target: T, content: @Composable (T) -> Unit) {
    AnimatedContent(target, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
        label = "onboarding_swap") { content(it) }
}
