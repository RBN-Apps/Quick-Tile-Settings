package com.rbn.qtsettings.app.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

/** NavDisplay seeks these transitions with gesture progress; only its completion callback pops. */
internal object AppNavigationTransitions {
    fun forward(): ContentTransform =
        (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 12 }) togetherWith
            fadeOut(tween(160))

    // The returning page enters from the left, mirroring [forward] and the direction the gesture
    // preview already uses. Without that counter-movement a committed Back reads as a plain fade.
    fun back(): ContentTransform =
        (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { -it / 12 }) togetherWith
            (fadeOut(tween(180)) + slideOutHorizontally(tween(220)) { it / 12 })

    fun predictive(): ContentTransform {
        // Both system edges reveal the previous page on the left.
        // Linear here means finger-controlled progress, not an independently running animation.
        return (fadeIn(tween(300, easing = LinearEasing)) +
            slideInHorizontally(tween(300, easing = LinearEasing)) { -it / 8 }) togetherWith
            slideOutHorizontally(tween(300, easing = LinearEasing)) { it }
    }
}
