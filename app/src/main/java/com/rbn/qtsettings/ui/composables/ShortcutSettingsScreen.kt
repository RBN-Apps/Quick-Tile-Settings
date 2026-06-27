package com.rbn.qtsettings.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.utils.ShortcutUtils
import com.rbn.qtsettings.viewmodel.MainViewModel

@Composable
fun ShortcutSettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val dnsHostnames by viewModel.dnsHostnames.collectAsState()
    val enabledShortcutIds by viewModel.enabledShortcutIds.collectAsState()
    val favoriteShortcutIds by viewModel.favoriteShortcutIds.collectAsState()
    val allowPinnedShortcutsWhenDisabled by viewModel.allowPinnedShortcutsWhenDisabled.collectAsState()
    val shortcutMaxCount by viewModel.shortcutMaxCount.collectAsState()
    val showShortcutLimitReachedDialog = remember { mutableStateOf(false) }
    val showFavoriteLimitReachedDialog = remember { mutableStateOf(false) }
    val showPinnedShortcutInfoDialog = remember { mutableStateOf(false) }
    val showPinnedShortcutWarningDialog = remember { mutableStateOf(false) }
    val infoExpanded = rememberSaveable { mutableStateOf(false) }
    val dismissShortcutLimitDialog: () -> Unit = { showShortcutLimitReachedDialog.value = false }
    val dismissFavoriteLimitDialog: () -> Unit = { showFavoriteLimitReachedDialog.value = false }

    val showShortcutLimitReachedDialogAction = { showShortcutLimitReachedDialog.value = true }
    val showFavoriteLimitReachedDialogAction = { showFavoriteLimitReachedDialog.value = true }

    val privateDnsItems = buildPrivateDnsShortcutItems(dnsHostnames)
    val usbItems = listOf(
        ShortcutUiItem(
            id = ShortcutUtils.SHORTCUT_ID_USB_ON,
            title = stringResource(R.string.shortcut_usb_on_short),
            subtitle = stringResource(R.string.shortcut_usb_on_long),
            iconRes = R.drawable.ic_usb_on
        ),
        ShortcutUiItem(
            id = ShortcutUtils.SHORTCUT_ID_USB_OFF,
            title = stringResource(R.string.shortcut_usb_off_short),
            subtitle = stringResource(R.string.shortcut_usb_off_long),
            iconRes = R.drawable.ic_usb_off
        )
    )

    Column(
        modifier = modifier
            .padding(top = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = RoundedCornerShape(16.dp),
            onClick = { infoExpanded.value = !infoExpanded.value }
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = stringResource(
                            R.string.shortcut_settings_limit,
                            enabledShortcutIds.size,
                            shortcutMaxCount
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (infoExpanded.value) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                AnimatedVisibility(visible = infoExpanded.value) {
                    Text(
                        text = stringResource(
                            R.string.shortcut_settings_description,
                            shortcutMaxCount,
                            ShortcutUtils.MAX_FAVORITE_SHORTCUTS
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            ShortcutPinnedFallbackRow(
                checked = allowPinnedShortcutsWhenDisabled,
                onCheckedChange = { shouldEnable ->
                    if (shouldEnable) {
                        showPinnedShortcutWarningDialog.value = true
                    } else {
                        viewModel.setAllowPinnedShortcutsWhenDisabled(false)
                    }
                },
                onInfoClick = { showPinnedShortcutInfoDialog.value = true }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        ShortcutGroup(
            title = stringResource(R.string.setting_title_private_dns),
            items = privateDnsItems,
            enabledShortcutIds = enabledShortcutIds,
            favoriteShortcutIds = favoriteShortcutIds,
            maxShortcutCount = shortcutMaxCount,
            onLimitReached = showShortcutLimitReachedDialogAction,
            onFavoriteLimitReached = showFavoriteLimitReachedDialogAction,
            onToggle = { shortcutId, shouldEnable ->
                viewModel.setShortcutExposureEnabled(shortcutId, shouldEnable)
            },
            onFavorite = { shortcutId, shouldFavorite ->
                viewModel.setShortcutFavorite(shortcutId, shouldFavorite)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))
        ShortcutGroup(
            title = stringResource(R.string.setting_title_usb_debugging),
            items = usbItems,
            enabledShortcutIds = enabledShortcutIds,
            favoriteShortcutIds = favoriteShortcutIds,
            maxShortcutCount = shortcutMaxCount,
            onLimitReached = showShortcutLimitReachedDialogAction,
            onFavoriteLimitReached = showFavoriteLimitReachedDialogAction,
            onToggle = { shortcutId, shouldEnable ->
                viewModel.setShortcutExposureEnabled(shortcutId, shouldEnable)
            },
            onFavorite = { shortcutId, shouldFavorite ->
                viewModel.setShortcutFavorite(shortcutId, shouldFavorite)
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showShortcutLimitReachedDialog.value) {
        AlertDialog(
            onDismissRequest = dismissShortcutLimitDialog,
            title = { Text(text = stringResource(R.string.shortcut_settings_limit_reached_title)) },
            text = { Text(text = stringResource(R.string.shortcut_settings_limit_reached)) },
            confirmButton = {
                TextButton(onClick = dismissShortcutLimitDialog) {
                    Text(text = stringResource(R.string.dialog_close))
                }
            }
        )
    }

    if (showFavoriteLimitReachedDialog.value) {
        AlertDialog(
            onDismissRequest = dismissFavoriteLimitDialog,
            title = { Text(text = stringResource(R.string.shortcut_settings_favorite_limit_reached_title)) },
            text = { Text(text = stringResource(R.string.shortcut_settings_favorite_limit_reached)) },
            confirmButton = {
                TextButton(onClick = dismissFavoriteLimitDialog) {
                    Text(text = stringResource(R.string.dialog_close))
                }
            }
        )
    }

    if (showPinnedShortcutInfoDialog.value) {
        AlertDialog(
            onDismissRequest = { showPinnedShortcutInfoDialog.value = false },
            title = { Text(text = stringResource(R.string.shortcut_settings_allow_pinned_when_disabled)) },
            text = { Text(text = stringResource(R.string.shortcut_settings_allow_pinned_when_disabled_desc)) },
            confirmButton = {
                TextButton(onClick = { showPinnedShortcutInfoDialog.value = false }) {
                    Text(text = stringResource(R.string.dialog_close))
                }
            }
        )
    }

    if (showPinnedShortcutWarningDialog.value) {
        AlertDialog(
            onDismissRequest = { showPinnedShortcutWarningDialog.value = false },
            title = { Text(text = stringResource(R.string.shortcut_settings_allow_pinned_warning_title)) },
            text = { Text(text = stringResource(R.string.shortcut_settings_allow_pinned_warning_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setAllowPinnedShortcutsWhenDisabled(true)
                        showPinnedShortcutWarningDialog.value = false
                    }
                ) {
                    Text(text = stringResource(R.string.shortcut_settings_allow_pinned_warning_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinnedShortcutWarningDialog.value = false }) {
                    Text(text = stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

@Composable
private fun ShortcutPinnedFallbackRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onInfoClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.LockOpen,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.shortcut_settings_allow_pinned_when_disabled),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onInfoClick) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(R.string.shortcut_settings_allow_pinned_info_desc)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ShortcutGroup(
    title: String,
    items: List<ShortcutUiItem>,
    enabledShortcutIds: Set<String>,
    favoriteShortcutIds: Set<String>,
    maxShortcutCount: Int,
    onLimitReached: () -> Unit,
    onFavoriteLimitReached: () -> Unit,
    onToggle: (shortcutId: String, shouldEnable: Boolean) -> Unit,
    onFavorite: (shortcutId: String, shouldFavorite: Boolean) -> Unit
) {
    val innerCornerRadius = 4.dp
    val outerCornerRadius = 20.dp

    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items.forEachIndexed { index, item ->
            val isEnabled = enabledShortcutIds.contains(item.id)
            val isFavorite = favoriteShortcutIds.contains(item.id)
            val canEnableMore = enabledShortcutIds.size < maxShortcutCount
            val rowEnabled = isEnabled || canEnableMore
            val canFavoriteMore = favoriteShortcutIds.size < ShortcutUtils.MAX_FAVORITE_SHORTCUTS
            val favoriteToggleEnabled = isEnabled && (isFavorite || canFavoriteMore)
            val rowShape = when {
                items.size == 1 -> RoundedCornerShape(outerCornerRadius)
                index == 0 -> RoundedCornerShape(
                    topStart = outerCornerRadius,
                    topEnd = outerCornerRadius,
                    bottomStart = innerCornerRadius,
                    bottomEnd = innerCornerRadius
                )

                index == items.lastIndex -> RoundedCornerShape(
                    topStart = innerCornerRadius,
                    topEnd = innerCornerRadius,
                    bottomStart = outerCornerRadius,
                    bottomEnd = outerCornerRadius
                )

                else -> RoundedCornerShape(innerCornerRadius)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = rowShape
            ) {
                ShortcutToggleRow(
                    item = item,
                    checked = isEnabled,
                    favorite = isFavorite,
                    enabled = rowEnabled,
                    favoriteEnabled = favoriteToggleEnabled,
                    onCheckedChange = { shouldEnable ->
                        onToggle(item.id, shouldEnable)
                    },
                    onFavoriteChange = { shouldFavorite ->
                        onFavorite(item.id, shouldFavorite)
                    },
                    onDisabledClick = onLimitReached,
                    onFavoriteLimitReached = onFavoriteLimitReached
                )
            }
        }
    }
}

@Composable
private fun ShortcutToggleRow(
    item: ShortcutUiItem,
    checked: Boolean,
    favorite: Boolean,
    enabled: Boolean,
    favoriteEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onFavoriteChange: (Boolean) -> Unit,
    onDisabledClick: () -> Unit,
    onFavoriteLimitReached: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (enabled) {
                        onCheckedChange(!checked)
                    } else {
                        onDisabledClick()
                    }
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            painter = painterResource(item.iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = iconTint
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                }
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
        }
        IconButton(
            enabled = checked,
            onClick = {
                if (favoriteEnabled) {
                    onFavoriteChange(!favorite)
                } else {
                    onFavoriteLimitReached()
                }
            },
            modifier = Modifier.alpha(if (checked && favoriteEnabled) 1f else 0.5f)
        ) {
            Icon(
                imageVector = if (favorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = if (favorite) {
                    stringResource(R.string.shortcut_settings_remove_favorite_desc, item.title)
                } else {
                    stringResource(R.string.shortcut_settings_mark_favorite_desc, item.title)
                },
                tint = if (favorite) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        Switch(
            checked = checked,
            enabled = true,
            modifier = Modifier.alpha(if (enabled) 1f else 0.5f),
            onCheckedChange = { shouldEnable ->
                if (enabled) {
                    onCheckedChange(shouldEnable)
                } else {
                    onDisabledClick()
                }
            }
        )
    }
}

@Composable
private fun buildPrivateDnsShortcutItems(dnsHostnames: List<DnsHostnameEntry>): List<ShortcutUiItem> {
    val items = mutableListOf(
        ShortcutUiItem(
            id = ShortcutUtils.SHORTCUT_ID_DNS_OFF,
            title = stringResource(R.string.shortcut_dns_off_short),
            subtitle = stringResource(R.string.shortcut_dns_off_long),
            iconRes = R.drawable.ic_dns_off
        ),
        ShortcutUiItem(
            id = ShortcutUtils.SHORTCUT_ID_DNS_AUTO,
            title = stringResource(R.string.shortcut_dns_auto_short),
            subtitle = stringResource(R.string.shortcut_dns_auto_long),
            iconRes = R.drawable.ic_dns_auto
        )
    )

    dnsHostnames.forEach { entry ->
        val shortcutId = ShortcutUtils.getShortcutIdForDnsEntry(entry)
        if (entry.isPredefined) {
            val predefinedItem = when (shortcutId) {
                ShortcutUtils.SHORTCUT_ID_DNS_ADGUARD -> ShortcutUiItem(
                    id = shortcutId,
                    title = stringResource(R.string.shortcut_dns_adguard_short),
                    subtitle = entry.hostname,
                    iconRes = R.drawable.ic_dns_on_adguard
                )

                ShortcutUtils.SHORTCUT_ID_DNS_CLOUDFLARE -> ShortcutUiItem(
                    id = shortcutId,
                    title = stringResource(R.string.shortcut_dns_cloudflare_short),
                    subtitle = entry.hostname,
                    iconRes = R.drawable.ic_dns_on_cloudflare
                )

                ShortcutUtils.SHORTCUT_ID_DNS_QUAD9 -> ShortcutUiItem(
                    id = shortcutId,
                    title = stringResource(R.string.shortcut_dns_quad9_short),
                    subtitle = entry.hostname,
                    iconRes = R.drawable.ic_dns_on_quad9_security
                )

                else -> null
            }
            predefinedItem?.let { items.add(it) }
        } else {
            items.add(
                ShortcutUiItem(
                    id = shortcutId,
                    title = entry.name,
                    subtitle = entry.hostname,
                    iconRes = R.drawable.ic_dns_on
                )
            )
        }
    }

    return items
}

private data class ShortcutUiItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconRes: Int
)
