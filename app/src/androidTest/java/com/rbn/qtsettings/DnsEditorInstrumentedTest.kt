package com.rbn.qtsettings

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.feature.dns.presentation.DnsSettingsAction
import com.rbn.qtsettings.feature.dns.presentation.DnsSettingsViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DnsEditorInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun draftSurvivesActivityRecreation_cancelLeavesStoredProvidersUntouched() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.dismissInitialDialogsIfPresent(context)
        val store = PreferencesManager.getInstance(context).dnsStore
        val before = store.read().providers
        compose.runOnIdle {
            ViewModelProvider(compose.activity)[DnsSettingsViewModel::class.java]
                .onAction(DnsSettingsAction.Edit(null))
        }
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("Unsaved DNS")
        compose.onAllNodes(hasSetTextAction())[1].performTextReplacement("draft.example.com")
        compose.activityRule.scenario.recreate()
        compose.onAllNodes(hasSetTextAction())[0].assertTextContains("Unsaved DNS")
        compose.onAllNodes(hasSetTextAction())[1].assertTextContains("draft.example.com")
        compose.onNodeWithText(context.getString(R.string.dialog_cancel)).performClick()
        compose.runOnIdle { assertEquals(before, store.read().providers) }
    }
}
