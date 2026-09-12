package com.rbn.qtsettings.feature.settings.presentation

import android.content.res.Configuration
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme
import com.rbn.qtsettings.feature.settings.presentation.*
import com.rbn.qtsettings.feature.permissions.presentation.*

@Preview(locale = "en", showBackground = true)
@Preview(locale = "de", uiMode = Configuration.UI_MODE_NIGHT_YES, fontScale = 1.3f)
@Composable
private fun BackupSettingsPreview() {
    QuickTileSettingsTheme { Surface { AppSettingsScreen(null, {}, {}, {}, {}) } }
}

@Preview(locale = "en")
@Preview(locale = "de", uiMode = Configuration.UI_MODE_NIGHT_YES, fontScale = 1.3f)
@Composable
private fun RestoreConfirmationPreview() {
    QuickTileSettingsTheme { AppSettingsScreen("content://preview/backup", {}, {}, {}, {}) }
}

@Preview(locale = "en")
@Preview(locale = "de", uiMode = Configuration.UI_MODE_NIGHT_YES, fontScale = 1.3f)
@Composable
private fun PermissionHelpPreview() {
    QuickTileSettingsTheme { PermissionGrantDialog({}, {}, {}, {}, {}, {}, false, false, false) }
}

@Preview(locale = "de", widthDp = 640, heightDp = 360, fontScale = 1.3f)
@Composable
private fun AvailablePermissionMethodsPreview() {
    QuickTileSettingsTheme { PermissionGrantDialog({}, {}, {}, {}, {}, {}, true, true, true, true) }
}

@Preview(locale = "en")
@Preview(locale = "de", uiMode = Configuration.UI_MODE_NIGHT_YES, fontScale = 1.3f)
@Composable
private fun AdbInstructionsPreview() {
    QuickTileSettingsTheme { AdbInstructionDialog({}, {}) }
}

@Preview(locale = "en")
@Preview(locale = "de", uiMode = Configuration.UI_MODE_NIGHT_YES, fontScale = 1.3f)
@Composable
private fun AboutPreview() {
    QuickTileSettingsTheme { AboutDialog("1.5.0", {}, {}) }
}

@Preview(locale = "de", fontScale = 1.3f)
@Composable
private fun NotificationExplanationPreview() {
    QuickTileSettingsTheme { NotificationPermissionExplanationDialog(false, {}, {}) }
}

@Preview(locale = "en")
@Composable
private fun NotificationFallbackPreview() {
    QuickTileSettingsTheme { NotificationPermissionFallbackDialog({}, {}) }
}

@Preview(locale = "de", uiMode = Configuration.UI_MODE_NIGHT_YES, fontScale = 1.3f)
@Composable
private fun NotificationSettingsPreview() {
    QuickTileSettingsTheme { NotificationPermissionSettingsDialog({}, {}) }
}
