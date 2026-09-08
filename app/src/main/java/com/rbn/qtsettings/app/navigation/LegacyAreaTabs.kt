package com.rbn.qtsettings.app.navigation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R

/** Stateless selection adapter until the adaptive rail in #125. No pager or second nav owner. */
@Composable
fun LegacyAreaTabs(area: AppArea, onSelectArea: (AppArea) -> Unit) {
    SecondaryTabRow(selectedTabIndex = area.ordinal, containerColor = MaterialTheme.colorScheme.surface) {
        AppArea.entries.forEach { target ->
            Tab(
                selected = area == target,
                onClick = { onSelectArea(target) },
                text = { Text(stringResource(if (target == AppArea.Dns) R.string.tab_title_dns else R.string.tab_title_usb)) }
            )
        }
    }
}
