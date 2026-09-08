package com.rbn.qtsettings.app.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable

/** Isolates the existing single global destination + pager. This is not a back stack.
 * #124 replaces this owner; #125 removes its pager adapter.
 */
class LegacyAppNavigation internal constructor(
    private val currentDestination: MutableState<AppDestination>,
    val pagerState: PagerState
) {
    val destination: AppDestination get() = currentDestination.value
    fun open(destination: AppDestination) { currentDestination.value = destination }
    fun back() { open(AppDestination.TileSettings) }
    suspend fun openArea(area: AppArea) {
        pagerState.scrollToPage(area.ordinal)
        back()
    }
}

@Composable
fun rememberLegacyAppNavigation(
    externalArea: AppArea?,
    onExternalAreaConsumed: () -> Unit,
    backEnabled: Boolean
): LegacyAppNavigation {
    val destination = rememberSaveable { mutableStateOf(AppDestination.TileSettings) }
    val pager = rememberPagerState(initialPage = externalArea?.ordinal ?: 0, pageCount = { AppArea.entries.size })
    val navigation = remember(pager) { LegacyAppNavigation(destination, pager) }
    LaunchedEffect(externalArea) {
        externalArea?.let {
            navigation.openArea(it)
            onExternalAreaConsumed()
        }
    }
    BackHandler(enabled = backEnabled && navigation.destination != AppDestination.TileSettings) {
        navigation.back()
    }
    return navigation
}
