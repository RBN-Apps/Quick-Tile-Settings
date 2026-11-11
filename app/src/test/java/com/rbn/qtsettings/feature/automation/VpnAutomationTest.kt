package com.rbn.qtsettings.feature.automation

import com.rbn.qtsettings.feature.automation.domain.*
import org.junit.Assert.*
import org.junit.Test

class VpnAutomationTest {
    private class Gateway : VpnAutomationGateway {
        var current = VpnDnsState("hostname", "before.example")
        var saved: VpnDnsState? = null
        var succeeds = true
        var writes = 0
        override fun currentDns() = current
        override fun savedDns() = saved
        override fun save(state: VpnDnsState) { saved = state }
        override fun clear() { saved = null }
        override fun disableDns(): Boolean { writes++; if (succeeds) current = VpnDnsState("off", current.hostname); return succeeds }
        override fun restoreDns(state: VpnDnsState): Boolean { writes++; if (succeeds) current = state; return succeeds }
    }

    @Test fun duplicateConnectAndRestartKeepOriginalSnapshot() {
        val gateway = Gateway(); val owner = VpnAutomation(gateway)
        val original = gateway.current
        owner.initialize(true) { false }
        owner.connected()
        VpnAutomation(gateway).initialize(true) { false }
        assertEquals(original, gateway.saved)
        assertEquals(1, gateway.writes)
        owner.disconnected({ false })
        assertEquals(original, gateway.current)
        assertNull(gateway.saved)
    }

    @Test fun successfulNetworkPolicyOnDisconnectWinsOverSavedDns() {
        val gateway = Gateway(); val owner = VpnAutomation(gateway)
        owner.connected()
        assertTrue(owner.disconnected({ gateway.current = VpnDnsState("opportunistic", null); true }))
        assertEquals("opportunistic", gateway.current.mode)
        assertEquals(1, gateway.writes)
        assertNull(gateway.saved)
    }

    @Test fun failedRestoreKeepsSnapshotForServiceRestart() {
        val gateway = Gateway(); val owner = VpnAutomation(gateway)
        val original = gateway.current
        owner.connected(); gateway.succeeds = false
        assertFalse(owner.disconnected({ false }))
        assertEquals(original, gateway.saved)
        gateway.succeeds = true
        VpnAutomation(gateway).initialize(false) { false }
        assertEquals(original, gateway.current)
        assertNull(gateway.saved)
    }

    @Test fun failedDisableAndAlreadyOffNeverInventSavedState() {
        val gateway = Gateway(); val owner = VpnAutomation(gateway)
        gateway.succeeds = false
        owner.connected()
        assertNull(gateway.saved)
        gateway.current = VpnDnsState("off", null); gateway.succeeds = true
        owner.connected()
        assertNull(gateway.saved)
        assertEquals(1, gateway.writes)
    }

    @Test fun tileDisconnectWithoutSnapshotMayApplyPolicy_backgroundDoesNot() {
        val owner = VpnAutomation(Gateway()); var calls = 0
        assertFalse(owner.disconnected({ calls++; true }))
        assertEquals(0, calls)
        assertTrue(owner.disconnected({ calls++; true }, applyWithoutSavedState = true))
        assertEquals(1, calls)
    }
}
