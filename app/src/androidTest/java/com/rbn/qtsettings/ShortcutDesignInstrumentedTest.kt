package com.rbn.qtsettings

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.res.painterResource
import androidx.test.platform.app.InstrumentationRegistry
import com.rbn.qtsettings.core.designsystem.SettingsFavoriteSwitchRow
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme
import com.rbn.qtsettings.feature.dns.domain.DnsProvider
import com.rbn.qtsettings.feature.dns.domain.DnsTarget
import com.rbn.qtsettings.feature.shortcuts.domain.ShortcutConfiguration
import com.rbn.qtsettings.feature.shortcuts.presentation.*
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ShortcutDesignInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun label(id: Int) = instrumentation.targetContext.getString(id)

    @Test fun favoriteAndSwitch_haveIndependentSingleActions() {
        var checked by mutableStateOf(true)
        var favorites = 0
        var toggles = 0
        compose.setContent { QuickTileSettingsTheme {
            SettingsFavoriteSwitchRow("DNS", "dns.example", painterResource(R.drawable.ic_dns_on),
                checked, false, true, true, "Favorite", { checked = it; toggles++ },
                { favorites++ }, {}, {})
        } }
        compose.onNodeWithContentDescription("Favorite").performClick()
        assertEquals(1, favorites)
        assertEquals(0, toggles)
        compose.onNodeWithText("DNS").assertIsOn().performClick().assertIsOff()
        assertEquals(1, toggles)
        assertEquals(1, favorites)
    }

    @Test fun longQuickActions_scrollWithoutHidingFooter_andExecuteOnce() {
        var executions = 0
        var selected: QuickAction? = null
        val actions = (1..20).map { index -> QuickAction("Provider $index", "dns-$index.example",
            R.drawable.ic_dns_on, QuickActionKind.DnsHostname(
                DnsProvider("$index", "Provider $index", DnsTarget.Hostname("dns-$index.example"), false, true))) }
        compose.setContent { QuickTileSettingsTheme {
            QuickActionsDialog(TileType.DNS, actions, {}, {}, { selected = it; executions++ })
        } }
        compose.onNodeWithText(label(R.string.quick_action_open_app)).assertIsDisplayed()
        compose.onNodeWithText(label(R.string.dialog_cancel)).assertIsDisplayed()
        compose.onNodeWithText("Provider 20").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(actions.last(), selected)
        assertEquals(1, executions)
    }

    @Test fun emptyQuickActions_cancelAndOpenAppDoNotExecuteAnAction() {
        var opened = 0
        var dismissed = 0
        var executed = 0
        compose.setContent { QuickTileSettingsTheme {
            QuickActionsDialog(TileType.ALL, emptyList(), { dismissed++ }, { opened++ }, { executed++ })
        } }
        compose.onNodeWithText(label(R.string.quick_action_no_actions)).assertIsDisplayed()
        compose.onNodeWithText(label(R.string.dialog_cancel)).performClick()
        compose.onNodeWithText(label(R.string.quick_action_open_app)).performClick()
        assertEquals(1, dismissed)
        assertEquals(1, opened)
        assertEquals(0, executed)
    }

    @Test fun limitDialog_closesWithoutChangingConfiguration() {
        var dialog by mutableStateOf(ShortcutDialog.Limit)
        var changes = 0
        compose.setContent { QuickTileSettingsTheme {
            ShortcutSettingsDialogs(ShortcutSettingsState(ShortcutConfiguration(5), emptyList(), dialog = dialog)) {
                if (it == ShortcutSettingsAction.DismissDialog) dialog = ShortcutDialog.None else changes++
            }
        } }
        compose.onNodeWithText(label(R.string.shortcut_settings_limit_reached_title)).assertIsDisplayed()
        if (InstrumentationRegistry.getArguments().getString("captureShortcutUi") == "true") {
            val file = File(instrumentation.targetContext.getExternalFilesDir(null), "shortcut-limit.png")
            val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        compose.onNodeWithText(label(R.string.dialog_close)).performClick()
        compose.onNodeWithText(label(R.string.shortcut_settings_limit_reached_title)).assertDoesNotExist()
        assertEquals(0, changes)
    }
}
