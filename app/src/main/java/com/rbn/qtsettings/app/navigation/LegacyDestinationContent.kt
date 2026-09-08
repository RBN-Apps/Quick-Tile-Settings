package com.rbn.qtsettings.app.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier

/** Existing animation/state preservation only; replaced by NavDisplay in #124. */
@Composable
fun LegacyDestinationContent(
    destination: AppDestination,
    modifier: Modifier = Modifier,
    content: @Composable (AppDestination) -> Unit
) {
    val stateHolder = rememberSaveableStateHolder()
    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            if (targetState != AppDestination.TileSettings) {
                (slideInHorizontally(
                    animationSpec = tween(260),
                    initialOffsetX = { fullWidth -> fullWidth }
                ) + fadeIn(animationSpec = tween(220))) togetherWith
                        (slideOutHorizontally(
                            animationSpec = tween(260),
                            targetOffsetX = { fullWidth -> -(fullWidth / 4) }
                        ) + fadeOut(animationSpec = tween(200)))
            } else {
                (slideInHorizontally(
                    animationSpec = tween(260),
                    initialOffsetX = { fullWidth -> -(fullWidth / 4) }
                ) + fadeIn(animationSpec = tween(220))) togetherWith
                        (slideOutHorizontally(
                            animationSpec = tween(260),
                            targetOffsetX = { fullWidth -> fullWidth }
                        ) + fadeOut(animationSpec = tween(200)))
            }
        },

        modifier = modifier,
        label = "screenTransition"
    ) { target ->
        stateHolder.SaveableStateProvider(target.name) { content(target) }
    }
}
