package com.rbn.qtsettings.feature.usb.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R

@Composable
internal fun UsbAutoRevertSection(
    enableAutoRevert: Boolean,
    autoRevertDelay: Int,
    configurationEnabled: Boolean,
    onAction: (UsbSettingsAction) -> Unit
) {
    val contentColor = LocalContentColor.current
    // Auto-Revert Section
    val interactionSourceAutoRevert = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSourceAutoRevert,
                indication = null,
                onClick = {
                    if (configurationEnabled) onAction(UsbSettingsAction.EnableAutoRevert(!enableAutoRevert))
                },
                enabled = configurationEnabled
            )
    ) {
        Checkbox(
            checked = enableAutoRevert,
            onCheckedChange = { onAction(UsbSettingsAction.EnableAutoRevert(it)) },
            enabled = configurationEnabled
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.setting_enable_auto_revert),
            style = MaterialTheme.typography.titleMedium,
            color = contentColor.copy(
                alpha = if (configurationEnabled) 1f else 0.38f
            )
        )
    }


    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.setting_auto_revert_delay),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = contentColor.copy(
                alpha = if (
                    enableAutoRevert && configurationEnabled
                ) 1f else 0.38f
            )
        )
        OutlinedTextField(
            value = autoRevertDelay.toString(),
            onValueChange = { value ->
                val newDelay =
                    value.toIntOrNull() ?: autoRevertDelay
                onAction(UsbSettingsAction.AutoRevertDelaySeconds(newDelay))
            },
            modifier = Modifier.width(80.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            enabled = enableAutoRevert && configurationEnabled
        )
    }

    Spacer(modifier = Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(16.dp))

}
