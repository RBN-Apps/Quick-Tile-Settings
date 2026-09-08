package com.rbn.qtsettings.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.app.navigation.AppArea
import com.rbn.qtsettings.app.navigation.AppDestination
import com.rbn.qtsettings.app.navigation.LegacyAreaTabs
import androidx.compose.foundation.layout.Column
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme

// Shell-only preview: no PreferencesManager, real ViewModel, services or permission requests.
@Preview(name = "Shell / permission missing", showBackground = true)
@Preview(name = "Shell / German / large text", locale = "de", fontScale = 1.5f, widthDp = 360)
@Composable
private fun AppShellPreview() {
    QuickTileSettingsTheme {
        AppScaffold(
            destination = AppDestination.TileSettings,
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onOpenDestination = {},
            onShowPermissionHelp = {},
            onShowAbout = {}
        ) { modifier ->
            Column(modifier) {
                PermissionWarningCard({}, Modifier.padding(horizontal = 16.dp))
                LegacyAreaTabs(AppArea.Dns, {})
                Text(stringResource(R.string.setting_title_private_dns), Modifier.padding(16.dp))
            }
        }
    }
}
