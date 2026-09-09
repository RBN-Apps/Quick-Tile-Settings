package com.rbn.qtsettings.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay

/** Decorate all stacks continuously, display only the selected one: USB root also exits to Android. */
@Composable
fun AppNavigationContent(
    navigation: AppNavigation,
    modifier: Modifier = Modifier,
    content: @Composable (AppRoute) -> Unit
) {
    val entries = navigation.stacks.mapValues { (area, stack) ->
        key(area) {
            rememberDecoratedNavEntries(
                backStack = stack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                    rememberViewModelStoreNavEntryDecorator<NavKey>()
                ),
                entryProvider = { route ->
                    NavEntry(route) {
                        // Each preview is an opaque page, so both live entries remain readable
                        // while the foreground follows the user's finger.
                        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                            content(it as AppRoute)
                        }
                    }
                }
            )
        }
    }
    NavDisplay(
        entries = entries.getValue(navigation.area),
        onBack = navigation::back,
        transitionSpec = { AppNavigationTransitions.forward() },
        popTransitionSpec = { AppNavigationTransitions.back() },
        predictivePopTransitionSpec = { AppNavigationTransitions.predictive() },
        modifier = modifier
    )
}
