package com.rbn.qtsettings

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.feature.dns.presentation.DnsSettingsAction
import com.rbn.qtsettings.feature.dns.presentation.DnsSettingsViewModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DnsDeleteDialogInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun deletionSurvivesRecreationAndRotation_cancelPreservesEntry_confirmDeletesIt() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = PreferencesManager.getInstance(context)
        compose.dismissInitialDialogsIfPresent(context)
        lateinit var entryId: String
        compose.runOnIdle {
            preferences.addCustomDnsHostname("Dialog rotation test", "dialog-test.example.com")
            val entry = preferences.dnsHostnames.value.single { it.hostname == "dialog-test.example.com" }
            entryId = entry.id
            ViewModelProvider(compose.activity)[DnsSettingsViewModel::class.java].onAction(DnsSettingsAction.Delete(entry.id))
        }
        try {
            compose.activityRule.scenario.recreate()
            compose.onNodeWithText(context.getString(R.string.confirm_delete_hostname_title)).assertIsDisplayed()
            compose.activityRule.scenario.onActivity {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            compose.waitUntil(10_000) {
                compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            }
            compose.onNodeWithText(context.getString(R.string.dialog_cancel)).assertIsDisplayed().performClick()
            compose.runOnIdle {
                assertTrue(preferences.dnsHostnames.value.any { it.id == entryId })
                val entry = preferences.dnsHostnames.value.single { it.id == entryId }
                ViewModelProvider(compose.activity)[DnsSettingsViewModel::class.java].onAction(DnsSettingsAction.Delete(entry.id))
            }
            compose.onNodeWithText(context.getString(R.string.button_delete)).assertIsDisplayed().performClick()
            compose.runOnIdle { assertFalse(preferences.dnsHostnames.value.any { it.id == entryId }) }
        } finally {
            compose.runOnIdle { preferences.deleteCustomDnsHostname(entryId) }
            compose.activityRule.scenario.onActivity {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }
}
