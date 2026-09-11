package com.rbn.qtsettings.feature.usb.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme
import com.rbn.qtsettings.feature.usb.domain.UsbSystemState

private val enabledState = UsbSettingsState(system = UsbSystemState(true, true, false), hasPermission = true)

@Composable
private fun UsbPreview(content: @Composable () -> Unit) {
    QuickTileSettingsTheme { Surface(Modifier.fillMaxSize(), content = content) }
}

@Preview(name = "Overview light", showBackground = true)
@Preview(name = "Overview dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Overview large German", locale = "de", fontScale = 1.5f, widthDp = 360, heightDp = 640)
@Composable
private fun OverviewPreview() {
    UsbPreview { UsbSettingsScreen(enabledState, {}) }
}

@Preview(name = "Tile light", showBackground = true)
@Preview(name = "Tile dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tile large German", locale = "de", fontScale = 1.5f, widthDp = 360, heightDp = 640)
@Composable
private fun TilePreview() {
    UsbPreview { UsbTileScreen(enabledState, {}) }
}

@Preview(name = "Tile without cycle state", showBackground = true)
@Composable
private fun EmptyCycleTilePreview() {
    UsbPreview {
        UsbTileScreen(
            enabledState.copy(settings = enabledState.settings.copy(toggleEnable = false, toggleDisable = false)),
            {}
        )
    }
}

@Preview(name = "Actions light", showBackground = true)
@Preview(name = "Actions dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ActionsPreview() {
    UsbPreview { UsbAdditionalActionsScreen(enabledState, {}) }
}

@Preview(name = "Revert light", showBackground = true)
@Preview(name = "Revert dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Revert large German", locale = "de", fontScale = 1.5f, widthDp = 360, heightDp = 640)
@Composable
private fun RevertPreview() {
    UsbPreview {
        UsbAutoRevertScreen(enabledState.copy(settings = enabledState.settings.copy(enableAutoRevert = true)), {})
    }
}

@Preview(name = "Revert disabled", showBackground = true)
@Composable
private fun DisabledRevertPreview() {
    UsbPreview { UsbAutoRevertScreen(UsbSettingsState(), {}) }
}

@Preview(name = "Revert details German", locale = "de", widthDp = 360, heightDp = 640)
@Composable
private fun RevertDetailsPreview() {
    UsbPreview { UsbRevertDetailsDialog {} }
}

@Preview(name = "Tile disabled", showBackground = true)
@Composable
private fun DisabledTilePreview() {
    UsbPreview { UsbTileScreen(UsbSettingsState(), {}) }
}

/** Comparison only: the production flow keeps cycle selection and companion actions separate. */
@Preview(name = "Alternative combined", widthDp = 360, heightDp = 800, locale = "de")
@Composable
internal fun UsbCombinedPilotPreview() {
    UsbPreview {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            UsbTileContent(enabledState, {})
            UsbAdditionalActionsContent(enabledState, {})
        }
    }
}
