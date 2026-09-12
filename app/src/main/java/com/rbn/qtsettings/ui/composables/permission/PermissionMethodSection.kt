package com.rbn.qtsettings.ui.composables.permission

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.rbn.qtsettings.core.designsystem.SettingsSection

@Composable
internal fun PermissionMethodSection(title: String, description: String, content: @Composable () -> Unit) {
    SettingsSection(title = title) {
        Text(description)
        content()
    }
}
