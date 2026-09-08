package com.rbn.qtsettings.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.app.PermissionWarningCard
import kotlinx.coroutines.launch

/** Rendering-only pager adapter. #125 replaces this with the adaptive rail. */
@Composable
fun LegacyTilesScreen(
    pagerState: PagerState,
    hasWriteSecureSettings: Boolean,
    onShowPermissionHelp: () -> Unit,
    content: @Composable (AppArea) -> Unit
) {
    val scope = rememberCoroutineScope()
    val titles = listOf(stringResource(R.string.tab_title_dns), stringResource(R.string.tab_title_usb))
    Column(Modifier.fillMaxSize()) {
        if (!hasWriteSecureSettings) {
            PermissionWarningCard(onShowPermissionHelp, Modifier.padding(horizontal = 16.dp))
        }
        SecondaryTabRow(selectedTabIndex = pagerState.currentPage, containerColor = MaterialTheme.colorScheme.surface) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(title) }
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.background),
            key = { AppArea.entries[it].name }
        ) { page -> content(AppArea.entries[page]) }
    }
}
