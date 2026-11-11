package com.rbn.qtsettings.feature.automation.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.AppConfirmationDialog

/**
 * Sits above the navigation entries: both editor steps are composed at once while a back gesture
 * previews the pop, and a dialog owned by an entry would open a second window during that preview.
 */
@Composable
fun WifiRuleDeleteRoot(
    editor: WifiRuleEditorViewModel,
    actions: AutomationUiActions,
    onDeleted: () -> Unit
) {
    val confirmDelete by editor.confirmDelete.collectAsStateWithLifecycle()
    val draft by editor.draft.collectAsStateWithLifecycle()
    val current = draft
    val ruleId = current?.id
    if (!confirmDelete || current == null || ruleId == null) return
    AppConfirmationDialog(
        title = stringResource(R.string.wifi_rule_delete_title),
        message = stringResource(R.string.wifi_rule_delete_message, current.identity),
        confirmLabel = stringResource(R.string.button_delete),
        onConfirm = { editor.cancelDelete(); actions.deleteWifiNetworkRule(ruleId); onDeleted() },
        onDismissRequest = editor::cancelDelete,
        destructive = true
    )
}
