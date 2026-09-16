package com.rbn.qtsettings.feature.settings.presentation

import android.content.res.Configuration
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme
import com.rbn.qtsettings.feature.permissions.domain.PermissionSnapshot
import com.rbn.qtsettings.platform.permissions.adbPermissionGrantCommand
import com.rbn.qtsettings.feature.permissions.presentation.*

@Preview(locale = "en", showBackground = true)
@Preview(locale = "de", uiMode = Configuration.UI_MODE_NIGHT_YES, fontScale = 1.3f)
@Composable
private fun BackupSettingsPreview() {
    QuickTileSettingsTheme { Surface { AppSettingsScreen(null, {}, {}, {}, {}, {}) } }
}

@Preview(locale = "en")
@Preview(locale = "de", uiMode = Configuration.UI_MODE_NIGHT_YES, fontScale = 1.3f)
@Composable
private fun RestoreConfirmationPreview() {
    QuickTileSettingsTheme { AppSettingsScreen("content://preview/backup", {}, {}, {}, {}, {}) }
}

@Preview(locale = "en")
@Preview(locale = "de", uiMode = Configuration.UI_MODE_NIGHT_YES, fontScale = 1.3f)
@Composable
private fun PermissionHelpPreview() {
    QuickTileSettingsTheme { PermissionSettingsScreen(PermissionSnapshot(), true, PermissionOperation(), null, {}, {}, {}, {}, {}, {}, {}) }
}

@Preview(locale = "de", widthDp = 640, heightDp = 360, fontScale = 1.3f)
@Composable
private fun AvailablePermissionMethodsPreview() {
    QuickTileSettingsTheme { PermissionSettingsScreen(PermissionSnapshot(shizukuAvailable = true, shizukuPermissionGranted = true, rooted = true), true, PermissionOperation(), null, {}, {}, {}, {}, {}, {}, {}) }
}

@Preview(locale = "en")
@Preview(locale = "de", uiMode = Configuration.UI_MODE_NIGHT_YES, fontScale = 1.3f)
@Composable
private fun AdbInstructionsPreview() {
    QuickTileSettingsTheme { AdbInstructionsScreen(adbPermissionGrantCommand("com.rbn.qtsettings"), {}) }
}

@Preview(locale = "en")
@Preview(locale = "de", uiMode = Configuration.UI_MODE_NIGHT_YES, fontScale = 1.3f)
@Composable
private fun AboutPreview() {
    QuickTileSettingsTheme { Surface { AboutScreen(
        com.rbn.qtsettings.platform.app.AppInfo("1.5.0", 150, "com.rbn.qtsettings",
            "15", 35, "Samsung", "Galaxy", "arm64-v8a"), null, {}, {}, {}, {}, {}
    ) } }
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
