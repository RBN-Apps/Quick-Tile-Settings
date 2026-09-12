package com.rbn.qtsettings.feature.shortcuts.presentation

import android.content.res.Configuration
import androidx.compose.material3.Surface
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme
import com.rbn.qtsettings.data.DnsHostnamePolicy
import com.rbn.qtsettings.feature.shortcuts.domain.ShortcutCatalog
import com.rbn.qtsettings.feature.shortcuts.domain.ShortcutConfiguration

@Preview(showBackground = true, locale = "en")
@Preview(showBackground = true, locale = "de", uiMode = Configuration.UI_MODE_NIGHT_YES, fontScale = 1.3f)
@Composable
private fun ShortcutSettingsPreview() {
    QuickTileSettingsTheme {
        Surface {
            ShortcutSettingsScreen(
                ShortcutSettingsState(
                    configuration = ShortcutConfiguration(5, setOf(ShortcutCatalog.SHORTCUT_ID_DNS_OFF),
                        setOf(ShortcutCatalog.SHORTCUT_ID_DNS_OFF)),
                    dnsItems = buildPrivateDnsShortcutItems(DnsHostnamePolicy.defaultHostnames())
                ), onAction = {}
            )
        }
    }
}

@Preview(locale = "en")
@Preview(locale = "de", uiMode = Configuration.UI_MODE_NIGHT_YES, fontScale = 1.3f)
@Composable
private fun QuickActionsPreview() {
    QuickTileSettingsTheme {
        QuickActionsDialog(TileType.DNS,
            listOf(QuickAction(stringResource(R.string.shortcut_dns_off_short), stringResource(R.string.shortcut_dns_off_long),
                R.drawable.ic_dns_off, QuickActionKind.DnsOff)), {}, {}, {})
    }
}
