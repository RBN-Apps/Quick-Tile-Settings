package com.rbn.qtsettings.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.rbn.qtsettings.core.designsystem.theme.AppShapes
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing

/** For short settings groups; keys retain row state when items are reordered. */
@Composable
fun <T> SettingsGroup(
    items: List<T>,
    itemKey: (T) -> Any,
    modifier: Modifier = Modifier,
    title: String? = null,
    itemContent: @Composable (T) -> Unit
) {
    if (items.isEmpty()) return
    SettingsSection(modifier = modifier, title = title) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.groupGap)) {
            items.forEachIndexed { index, item ->
                key(itemKey(item)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.groupItem(index, items.size)
                    ) {
                        itemContent(item)
                    }
                }
            }
        }
    }
}
