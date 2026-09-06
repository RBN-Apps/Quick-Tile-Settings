package com.rbn.qtsettings.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.data.KnownWifiNetwork
import com.rbn.qtsettings.data.WifiDnsRule
import com.rbn.qtsettings.data.WifiIdentityAccessState
import com.rbn.qtsettings.data.WifiNetworkIdentity
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON
import com.rbn.qtsettings.utils.Constants.WIFI_DNS_ACTION_DEFAULT

@Composable
fun WifiNetworkRulesSection(
    enabled: Boolean,
    rules: List<WifiDnsRule>,
    dnsHostnames: List<DnsHostnameEntry>,
    currentNetwork: WifiNetworkIdentity?,
    scannedNetworks: List<WifiNetworkIdentity>,
    knownNetworks: List<KnownWifiNetwork>,
    isLoadingNetworks: Boolean,
    wifiIdentityAccessState: WifiIdentityAccessState,
    onEnabledChange: (Boolean) -> Unit,
    onAddRule: (ssid: String?, bssid: String?, action: String, hostname: String?) -> Boolean,
    onUpdateRule: (id: String, ssid: String?, bssid: String?, action: String, hostname: String?) -> Boolean,
    onDeleteRule: (String) -> Unit,
    onRefreshNetworks: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    hasBackgroundLocationPermission: Boolean,
    onOpenAppPermissionSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editorSeed by rememberSaveable(stateSaver = WifiRuleEditorSeed.NullableSaver) {
        mutableStateOf(null)
    }
    var rulePendingDeletionId by rememberSaveable { mutableStateOf<String?>(null) }
    var showNetworkPicker by rememberSaveable { mutableStateOf(false) }
    val rulePendingDeletion = rules.firstOrNull { it.id == rulePendingDeletionId }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("wifi_network_rules_section")
    ) {
        Text(
            text = stringResource(R.string.setting_wifi_network_rules_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(R.string.setting_wifi_network_rules_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("wifi_network_rules_toggle")
                .toggleable(value = enabled, role = Role.Switch, onValueChange = onEnabledChange)
                .padding(vertical = 4.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.wifi_rules_enabled),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.wifi_rules_enabled_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = enabled, onCheckedChange = null)
        }

        if (enabled) {
            Spacer(modifier = Modifier.height(12.dp))
            if (wifiIdentityAccessState != WifiIdentityAccessState.AVAILABLE) {
                WifiIdentityAccessCard(
                    accessState = wifiIdentityAccessState,
                    onRequestPermission = onRequestPermission,
                    onOpenLocationSettings = onOpenLocationSettings
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (wifiIdentityAccessState == WifiIdentityAccessState.AVAILABLE) {
                if (hasBackgroundLocationPermission) {
                    Text(
                        stringResource(R.string.wifi_boot_ready),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                } else {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(stringResource(R.string.wifi_boot_title), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.wifi_boot_description), style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(
                                onClick = onOpenAppPermissionSettings,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("wifi_boot_permission_action")
                            ) { Text(stringResource(R.string.wifi_permission_open_settings)) }
                        }
                    }
                }
            }

            Text(text = stringResource(R.string.wifi_rules_title), style = MaterialTheme.typography.titleSmall)
            if (rules.isEmpty()) {
                Text(
                    text = stringResource(R.string.wifi_rules_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    rules.forEachIndexed { index, rule ->
                        WifiRuleRow(
                            rule = rule,
                            dnsHostnames = dnsHostnames,
                            onEdit = { editorSeed = WifiRuleEditorSeed.fromRule(rule) },
                            onDelete = { rulePendingDeletionId = rule.id }
                        )
                        if (index < rules.lastIndex) HorizontalDivider()
                    }
                }
            }

            currentNetwork?.let { identity ->
                OutlinedButton(
                    onClick = { editorSeed = WifiRuleEditorSeed.fromIdentity(identity) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .testTag("wifi_rule_add_current")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.wifi_rule_add_current))
                }
            }

            OutlinedButton(
                onClick = { showNetworkPicker = true; onRefreshNetworks() },
                enabled = wifiIdentityAccessState == WifiIdentityAccessState.AVAILABLE,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("wifi_rule_choose_network")
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.wifi_rule_choose_network))
            }

            OutlinedButton(
                onClick = { editorSeed = WifiRuleEditorSeed() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("wifi_rule_add_manual")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.wifi_rule_add_manual))
            }
        }
    }

    LaunchedEffect(enabled) {
        if (!enabled) {
            editorSeed = null
            rulePendingDeletionId = null
            showNetworkPicker = false
        }
    }

    if (enabled && showNetworkPicker) {
        WifiNetworkPickerDialog(
            currentNetwork = currentNetwork,
            scannedNetworks = scannedNetworks,
            knownNetworks = knownNetworks,
            isLoadingNetworks = isLoadingNetworks,
            onRefresh = onRefreshNetworks,
            onSelect = { identity ->
                showNetworkPicker = false
                editorSeed = WifiRuleEditorSeed.fromIdentity(identity)
            },
            onDismiss = { showNetworkPicker = false }
        )
    }

    if (enabled) editorSeed?.let { seed ->
        WifiRuleEditDialog(
            seed = seed,
            dnsHostnames = dnsHostnames,
            onDismiss = { editorSeed = null },
            onSave = { id, ssid, bssid, action, hostname ->
                val saved = if (id == null) {
                    onAddRule(ssid, bssid, action, hostname)
                } else {
                    onUpdateRule(id, ssid, bssid, action, hostname)
                }
                if (saved) editorSeed = null
                saved
            }
        )
    }

    rulePendingDeletion?.let { rule ->
        AlertDialog(
            onDismissRequest = { rulePendingDeletionId = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(stringResource(R.string.wifi_rule_delete_title)) },
            text = { Text(stringResource(R.string.wifi_rule_delete_message, rule.displayIdentity())) },
            confirmButton = {
                TextButton(
                    onClick = { onDeleteRule(rule.id); rulePendingDeletionId = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.button_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { rulePendingDeletionId = null }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

@Composable
private fun WifiRuleRow(
    rule: WifiDnsRule,
    dnsHostnames: List<DnsHostnameEntry>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text(rule.ssid ?: rule.bssid.orEmpty(), style = MaterialTheme.typography.bodyLarge)
            rule.bssid?.takeIf { rule.ssid != null }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text(rule.actionLabel(dnsHostnames), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.wifi_rule_edit_description, rule.displayIdentity()))
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = stringResource(R.string.wifi_rule_delete_description, rule.displayIdentity()),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun WifiIdentityAccessCard(
    accessState: WifiIdentityAccessState,
    onRequestPermission: () -> Unit,
    onOpenLocationSettings: () -> Unit
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val explanation = buildAnnotatedString {
        append(
            stringResource(
                when (accessState) {
                    WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_REQUIRED ->
                        R.string.wifi_ssid_permission_description

                    WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_BLOCKED ->
                        R.string.wifi_permission_blocked_description

                    WifiIdentityAccessState.LOCATION_SERVICES_DISABLED ->
                        R.string.wifi_location_services_description

                    WifiIdentityAccessState.AVAILABLE ->
                        R.string.wifi_ssid_permission_description
                }
            )
        )
        append("\n\n")
        append(stringResource(R.string.wifi_ssid_manual_entry_notice))
        append("\n")
        withLink(
            LinkAnnotation.Url(
                stringResource(R.string.wifi_ssid_permission_docs_url),
                TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
            )
        ) { append(stringResource(R.string.wifi_ssid_permission_docs_link)) }
    }
    val title = when (accessState) {
        WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_REQUIRED ->
            R.string.wifi_ssid_permission_title

        WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_BLOCKED ->
            R.string.wifi_ssid_permission_title

        WifiIdentityAccessState.LOCATION_SERVICES_DISABLED ->
            R.string.wifi_location_services_title

        WifiIdentityAccessState.AVAILABLE -> R.string.wifi_ssid_permission_title
    }
    val actionLabel = when (accessState) {
        WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_REQUIRED ->
            R.string.wifi_ssid_permission_grant

        WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_BLOCKED ->
            R.string.wifi_permission_open_settings

        WifiIdentityAccessState.LOCATION_SERVICES_DISABLED ->
            R.string.wifi_location_services_open

        WifiIdentityAccessState.AVAILABLE -> R.string.wifi_ssid_permission_grant
    }
    val onAction = when (accessState) {
        WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_REQUIRED -> onRequestPermission
        WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_BLOCKED -> onRequestPermission
        WifiIdentityAccessState.LOCATION_SERVICES_DISABLED -> onOpenLocationSettings
        WifiIdentityAccessState.AVAILABLE -> onRequestPermission
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            Text(explanation, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            OutlinedButton(
                onClick = onAction,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .testTag("wifi_identity_access_action")
            ) {
                Text(stringResource(actionLabel))
            }
        }
    }
}

@Composable
private fun WifiNetworkPickerDialog(
    currentNetwork: WifiNetworkIdentity?,
    scannedNetworks: List<WifiNetworkIdentity>,
    knownNetworks: List<KnownWifiNetwork>,
    isLoadingNetworks: Boolean,
    onRefresh: () -> Unit,
    onSelect: (WifiNetworkIdentity) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text(stringResource(R.string.wifi_network_picker_title)) },
        text = {
            Column(
                Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                currentNetwork?.let {
                    CandidateSectionTitle(R.string.wifi_network_current_title)
                    CandidateRow(it, onSelect)
                }
                CandidateSectionTitle(R.string.wifi_network_nearby_title)
                if (isLoadingNetworks) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.wifi_network_scanning))
                    }
                } else if (scannedNetworks.isEmpty()) {
                    Text(stringResource(R.string.wifi_network_none_found), style = MaterialTheme.typography.bodySmall)
                } else {
                    scannedNetworks.forEach { CandidateRow(it, onSelect) }
                }
                if (knownNetworks.isNotEmpty()) {
                    CandidateSectionTitle(R.string.wifi_network_history_title)
                    knownNetworks.forEach { CandidateRow(it.identity, onSelect) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onRefresh, enabled = !isLoadingNetworks) { Text(stringResource(R.string.wifi_network_refresh)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } }
    )
}

@Composable
private fun CandidateSectionTitle(titleRes: Int) {
    Text(stringResource(titleRes), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
}

@Composable
private fun CandidateRow(identity: WifiNetworkIdentity, onSelect: (WifiNetworkIdentity) -> Unit) {
    Column(Modifier.fillMaxWidth().clickable { onSelect(identity) }.padding(vertical = 8.dp)) {
        Text(identity.ssid ?: stringResource(R.string.wifi_hidden_network))
        identity.bssid?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun WifiRuleEditDialog(
    seed: WifiRuleEditorSeed,
    dnsHostnames: List<DnsHostnameEntry>,
    onDismiss: () -> Unit,
    onSave: (String?, String?, String?, String, String?) -> Boolean
) {
    var ssid by rememberSaveable(seed) { mutableStateOf(seed.ssid.orEmpty()) }
    var bssid by rememberSaveable(seed) { mutableStateOf(seed.bssid.orEmpty()) }
    var action by rememberSaveable(seed) {
        mutableStateOf(seed.actionMode ?: DNS_MODE_OFF)
    }
    var hostname by rememberSaveable(seed) { mutableStateOf(seed.dnsHostname) }
    var error by rememberSaveable(seed) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text(stringResource(if (seed.ruleId == null) R.string.wifi_rule_add_title else R.string.wifi_rule_edit_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = ssid,
                    onValueChange = { ssid = it; error = false },
                    label = { Text(stringResource(R.string.wifi_ssid_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bssid,
                    onValueChange = { bssid = it; error = false },
                    label = { Text(stringResource(R.string.wifi_bssid_label)) },
                    supportingText = { Text(stringResource(R.string.wifi_bssid_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                seed.detectedBssid?.let { detectedBssid ->
                    TextButton(
                        onClick = { bssid = detectedBssid; error = false },
                        modifier = Modifier.testTag("wifi_rule_use_detected_bssid")
                    ) {
                        Text(stringResource(R.string.wifi_rule_use_detected_bssid, detectedBssid))
                    }
                }
                Text(
                    stringResource(R.string.wifi_rule_action_label),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
                WifiRuleActionSelector(
                    action = action,
                    hostname = hostname,
                    dnsHostnames = dnsHostnames,
                    onActionSelected = { selectedAction, selectedHostname ->
                        action = selectedAction
                        hostname = selectedHostname
                        error = false
                    }
                )
                if (error) {
                    Text(
                        stringResource(R.string.wifi_rule_invalid_or_duplicate),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = !onSave(
                    seed.ruleId,
                    ssid.takeIf { it.isNotBlank() },
                    bssid.takeIf { it.isNotBlank() },
                    action,
                    hostname
                )
            }) { Text(stringResource(R.string.dialog_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } }
    )
}

@Composable
private fun WifiRuleActionSelector(
    action: String,
    hostname: String?,
    dnsHostnames: List<DnsHostnameEntry>,
    onActionSelected: (String, String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
        Text(
            when (action) {
                WIFI_DNS_ACTION_DEFAULT -> stringResource(R.string.wifi_rule_action_default)
                DNS_MODE_OFF -> stringResource(R.string.dns_mode_off_label)
                DNS_MODE_AUTO -> stringResource(R.string.dns_mode_auto_label)
                DNS_MODE_ON -> dnsHostnames.find { it.hostname == hostname }?.name ?: hostname.orEmpty()
                else -> stringResource(R.string.wifi_rule_action_default)
            },
            modifier = Modifier.weight(1f)
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.wifi_rule_action_default)) },
            onClick = { onActionSelected(WIFI_DNS_ACTION_DEFAULT, null); expanded = false }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.dns_mode_off_label)) },
            onClick = { onActionSelected(DNS_MODE_OFF, null); expanded = false }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.dns_mode_auto_label)) },
            onClick = { onActionSelected(DNS_MODE_AUTO, null); expanded = false }
        )
        if (dnsHostnames.isNotEmpty()) HorizontalDivider()
        dnsHostnames.forEach { entry ->
            DropdownMenuItem(
                text = { Column { Text(entry.name); Text(entry.hostname, style = MaterialTheme.typography.bodySmall) } },
                onClick = { onActionSelected(DNS_MODE_ON, entry.hostname); expanded = false }
            )
        }
    }
}

private data class WifiRuleEditorSeed(
    val ruleId: String? = null,
    val ssid: String? = null,
    val bssid: String? = null,
    val actionMode: String? = null,
    val dnsHostname: String? = null,
    val detectedBssid: String? = null
) {
    companion object {
        fun fromRule(rule: WifiDnsRule) = WifiRuleEditorSeed(
            ruleId = rule.id,
            ssid = rule.ssid,
            bssid = rule.bssid,
            actionMode = rule.actionMode,
            dnsHostname = rule.dnsHostname
        )

        fun fromIdentity(identity: WifiNetworkIdentity) = WifiRuleEditorSeed(
            ssid = identity.ssid,
            detectedBssid = identity.bssid
        )

        val NullableSaver = listSaver<WifiRuleEditorSeed?, String>(
            save = { seed ->
                if (seed == null) {
                    emptyList()
                } else {
                    listOf(
                        seed.ruleId.orEmpty(),
                        seed.ssid.orEmpty(),
                        seed.bssid.orEmpty(),
                        seed.actionMode.orEmpty(),
                        seed.dnsHostname.orEmpty(),
                        seed.detectedBssid.orEmpty()
                    )
                }
            },
            restore = { values ->
                if (values.isEmpty()) {
                    null
                } else {
                    WifiRuleEditorSeed(
                        ruleId = values[0].ifEmpty { null },
                        ssid = values[1].ifEmpty { null },
                        bssid = values[2].ifEmpty { null },
                        actionMode = values[3].ifEmpty { null },
                        dnsHostname = values[4].ifEmpty { null },
                        detectedBssid = values.getOrNull(5)?.ifEmpty { null }
                    )
                }
            }
        )
    }
}

private fun WifiDnsRule.displayIdentity(): String = listOfNotNull(ssid, bssid).joinToString(" / ")

@Composable
private fun WifiDnsRule.actionLabel(hostnames: List<DnsHostnameEntry>): String = when (actionMode) {
    WIFI_DNS_ACTION_DEFAULT -> stringResource(R.string.wifi_rule_action_default)
    DNS_MODE_OFF -> stringResource(R.string.dns_mode_off_label)
    DNS_MODE_AUTO -> stringResource(R.string.dns_mode_auto_label)
    DNS_MODE_ON -> hostnames.find { it.hostname == dnsHostname }?.name ?: dnsHostname.orEmpty()
    else -> stringResource(R.string.wifi_rule_action_default)
}
