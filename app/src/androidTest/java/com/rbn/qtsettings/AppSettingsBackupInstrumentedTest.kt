package com.rbn.qtsettings

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.rbn.qtsettings.app.AppStores
import com.rbn.qtsettings.feature.settings.presentation.AppSettingsViewModel
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AppSettingsBackupInstrumentedTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private fun text(id: Int) = compose.onNodeWithText(context.getString(id))

    @Test fun restoreConfirmation_survivesRecreation_andCancelPreservesSettings() {
        val stores = AppStores.getInstance(context)
        val previousHelp = stores.app.helpShown.value
        val before = stores.usb.state.value
        stores.app.setHelpShown(true)
        try {
            ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java)).use { scenario ->
                openSettings()
                scenario.onActivity { ViewModelProvider(it)[AppSettingsViewModel::class.java].selectRestore("content://test/not-imported") }
                text(R.string.backup_restore_confirm_title).assertIsDisplayed()
                if (InstrumentationRegistry.getArguments().getString("settingsScreenshots") == "true") {
                    val image = instrumentation.uiAutomation.takeScreenshot()
                    File(context.getExternalFilesDir(null), "restore-confirmation.png").outputStream().use {
                        image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                    }
                    image.recycle()
                }
                scenario.recreate()
                text(R.string.backup_restore_confirm_title).assertIsDisplayed()
                text(R.string.dialog_cancel).performClick()
                text(R.string.backup_restore_confirm_title).assertDoesNotExist()
                assertEquals(before, stores.usb.state.value)
                scenario.onActivity { assertNull(ViewModelProvider(it)[AppSettingsViewModel::class.java].pendingRestore.value) }
            }
        } finally { stores.app.setHelpShown(previousHelp) }
    }

    @Test fun confirmedRestore_readsDocumentAndRefreshesExistingFeatureStore() {
        val stores = AppStores.getInstance(context)
        val previousHelp = stores.app.helpShown.value
        val original = stores.backup.exportSettingsBackupJson()
        val file = File(context.cacheDir, "settings-restore-test.json")
        stores.app.setHelpShown(true)
        try {
            stores.usb.setRequireUnlock(true)
            file.writeText(stores.backup.exportSettingsBackupJson())
            stores.usb.setRequireUnlock(false)
            ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java)).use { scenario ->
                openSettings()
                scenario.onActivity { ViewModelProvider(it)[AppSettingsViewModel::class.java].selectRestore(Uri.fromFile(file).toString()) }
                text(R.string.backup_restore_confirm_button).performClick()
                compose.waitUntil(5_000) { stores.usb.state.value.requireUnlock }
                text(R.string.backup_restore_confirm_title).assertDoesNotExist()
            }
        } finally {
            stores.backup.restoreSettingsBackupJson(original)
            stores.app.setHelpShown(previousHelp)
            file.delete()
        }
    }

    private fun openSettings() {
        instrumentation.setInTouchMode(true)
        compose.onNodeWithContentDescription(context.getString(R.string.app_menu_desc)).performClick()
        text(R.string.menu_settings).performClick()
        text(R.string.settings_backup_title).assertIsDisplayed()
    }
}
