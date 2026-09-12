package com.rbn.qtsettings.feature.automation

import androidx.lifecycle.SavedStateHandle
import com.rbn.qtsettings.data.WifiDnsRule
import com.rbn.qtsettings.data.WifiNetworkIdentity
import com.rbn.qtsettings.feature.automation.presentation.AutomationDialogViewModel
import com.rbn.qtsettings.feature.automation.presentation.WifiRuleEditorViewModel
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON
import com.rbn.qtsettings.utils.Constants.WIFI_DNS_ACTION_DEFAULT
import org.junit.Assert.*
import org.junit.Test

class WifiRuleEditorViewModelTest {
    private fun restore(saved: SavedStateHandle) =
        WifiRuleEditorViewModel(SavedStateHandle(saved.keys().associateWith { saved.get<Any?>(it) }))

    @Test fun bothStepQueriesAndDraftSurviveRestorationAndBack() {
        val saved = SavedStateHandle()
        val owner = WifiRuleEditorViewModel(saved).apply { start(null) }
        owner.ssid("Campus")
        owner.searchNetworks("camp")
        owner.next()
        owner.searchTargets("dns.example")
        owner.target(DNS_MODE_ON, "dns.example.com")
        val restored = restore(saved)
        assertTrue(restored.targetStep.value)
        assertEquals("camp", restored.networkQuery.value)
        assertEquals("dns.example", restored.targetQuery.value)
        restored.previous()
        assertFalse(restored.targetStep.value)
        assertEquals("Campus", restored.draft.value!!.ssid)
        assertEquals("dns.example.com", restored.draft.value!!.hostname)
    }

    @Test fun invalidIdentityCannotAdvanceAndStorageRejectionReturnsToNetwork() {
        val owner = WifiRuleEditorViewModel(SavedStateHandle()).apply { start(null) }
        owner.next()
        assertFalse(owner.targetStep.value)
        assertTrue(owner.draft.value!!.error)
        owner.ssid("Campus")
        owner.bssid("invalid")
        owner.next()
        assertFalse(owner.targetStep.value)
        owner.bssid("")
        owner.next()
        assertTrue(owner.targetStep.value)
        assertFalse(owner.save { _, _, _, _, _ -> false })
        assertFalse(owner.targetStep.value)
        assertTrue(owner.draft.value!!.error)
    }

    @Test fun restoredEditorKeepsUncommittedEditsOverStoredRule() {
        val saved = SavedStateHandle()
        val stored = WifiDnsRule("id", "Original", actionMode = DNS_MODE_ON, dnsHostname = "dns.example.com")
        val owner = WifiRuleEditorViewModel(saved).apply { start(stored) }
        owner.ssid(" Edited SSID ")
        owner.bssid("aa:bb:cc:dd:ee:ff")
        owner.requestDelete()
        val restored = restore(saved).apply { start(stored) }
        assertEquals(" Edited SSID ", restored.draft.value!!.ssid)
        assertEquals("aa:bb:cc:dd:ee:ff", restored.draft.value!!.bssid)
        assertEquals("dns.example.com", restored.draft.value!!.hostname)
        assertEquals("id", restored.draft.value!!.id)
        assertTrue(restored.confirmDelete.value)
        restored.cancelDelete()
        assertFalse(restored.confirmDelete.value)
    }

    @Test fun newRuleStartsWithOffAndInvalidSaveKeepsDraftUntilRetry() {
        val owner = WifiRuleEditorViewModel(SavedStateHandle()).apply { start(null) }
        assertEquals(DNS_MODE_OFF, owner.draft.value!!.action)
        owner.ssid(" Campus ")
        assertFalse(owner.save { _, _, _, _, _ -> false })
        assertTrue(owner.draft.value!!.error)
        owner.bssid("AA:BB:CC:DD:EE:FF")
        assertFalse(owner.draft.value!!.error)
        owner.target(WIFI_DNS_ACTION_DEFAULT, null)
        var calls = 0
        assertTrue(owner.save { id, ssid, bssid, mode, host ->
            calls++
            assertNull(id); assertEquals(" Campus ", ssid)
            assertEquals("AA:BB:CC:DD:EE:FF", bssid)
            assertEquals(WIFI_DNS_ACTION_DEFAULT, mode); assertNull(host)
            true
        })
        assertEquals(1, calls)
    }

    @Test fun blankFieldsAreSavedAsAbsentValues() {
        val owner = WifiRuleEditorViewModel(SavedStateHandle()).apply { start(null) }
        owner.ssid("Campus")
        owner.bssid("   ")
        owner.save { _, ssid, bssid, _, _ -> assertEquals("Campus", ssid); assertNull(bssid); true }
    }

    @Test fun pickedNetworkCopiesSsidOnlyUntilAccessPointIsChosenExplicitly() {
        val owner = WifiRuleEditorViewModel(SavedStateHandle()).apply { start(null) }
        owner.bssid("11:22:33:44:55:66")
        owner.target(DNS_MODE_ON, "dns.example.com")
        owner.useNetwork(WifiNetworkIdentity("Campus", "AA:BB:CC:DD:EE:FF"))
        val draft = owner.draft.value!!
        assertEquals("Campus", draft.ssid)
        assertEquals("", draft.bssid)
        assertEquals("AA:BB:CC:DD:EE:FF", draft.detectedBssid)
        assertEquals(DNS_MODE_ON, draft.action)
        assertEquals("dns.example.com", draft.hostname)
        owner.useDetectedBssid()
        assertEquals("AA:BB:CC:DD:EE:FF", owner.draft.value!!.bssid)
    }

    @Test fun hiddenNetworkIsIdentifiedByItsAccessPoint() {
        val owner = WifiRuleEditorViewModel(SavedStateHandle()).apply { start(null) }
        owner.useNetwork(WifiNetworkIdentity(null, "AA:BB:CC:DD:EE:FF"))
        assertEquals("", owner.draft.value!!.ssid)
        assertEquals("AA:BB:CC:DD:EE:FF", owner.draft.value!!.bssid)
    }

    @Test fun startDoesNotReplaceAnExistingDraft() {
        val owner = WifiRuleEditorViewModel(SavedStateHandle()).apply { start(WifiDnsRule("id", "Stored")) }
        owner.ssid("Edited")
        owner.start(WifiDnsRule("id", "Stored again"))
        assertEquals("Edited", owner.draft.value!!.ssid)
    }

    @Test fun dialogOwnerRestoresAndDismissesItsDestination() {
        val saved = SavedStateHandle()
        AutomationDialogViewModel(saved).open(AutomationDialogViewModel.NETWORK_MODE)
        val restored = AutomationDialogViewModel(SavedStateHandle(saved.keys().associateWith { saved.get<Any?>(it) }))
        assertEquals(AutomationDialogViewModel.NETWORK_MODE, restored.dialog.value)
        restored.dismiss()
        assertEquals("", restored.dialog.value)
    }
}
