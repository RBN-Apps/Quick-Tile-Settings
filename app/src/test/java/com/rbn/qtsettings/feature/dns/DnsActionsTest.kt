package com.rbn.qtsettings.feature.dns

import com.rbn.qtsettings.feature.dns.domain.*
import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class DnsActionsTest {
    /** These assertions drive the ticket directly; no countdown ever runs on it. */
    private val revertScope = CoroutineScope(Dispatchers.Unconfined)

    class FakeGateway : DnsSystemGateway {
        var current: DnsTarget? = DnsTarget.Off
        var permitted = true
        var succeeds = true
        var version = 0L
        val writes = mutableListOf<DnsTarget>()
        override fun hasPermission() = permitted
        override fun read() = current
        override fun apply(target: DnsTarget): Boolean {
            writes += target
            if (succeeds) current = target
            return succeeds
        }
        override fun generation() = version
        override fun invalidateRevert() { version++ }
    }
    @Test fun cycleUsesOnlySelectedProvidersInOrder_andKeepsActiveSeparate() {
        val gateway = FakeGateway()
        val actions = DnsActions(gateway)
        val a = DnsTarget.Hostname("a.example")
        val b = DnsTarget.Hostname("b.example")
        val settings = DnsSettings(providers = listOf(
            DnsProvider("b", "B", b, false, true),
            DnsProvider("a", "A", a, false, false)
        ))
        assertEquals(DnsActionResult.Success, actions.cycle(settings).result)
        assertEquals(DnsTarget.Automatic, actions.read())
        assertEquals(DnsTarget.Automatic, actions.cycle(settings).previous)
        assertEquals(b, actions.read())
        actions.cycle(settings)
        assertEquals(DnsTarget.Off, actions.read())
        gateway.current = a
        actions.cycle(settings)
        assertEquals(DnsTarget.Off, actions.read())
    }
    @Test fun noChoices_invalidHostname_andMissingPermission_doNotWrite() {
        val gateway = FakeGateway(); val actions = DnsActions(gateway)
        assertEquals(DnsActionResult.NoCycleStates, actions.cycle(DnsSettings(toggleOff = false, toggleAuto = false)).result)
        assertEquals(DnsActionResult.InvalidHostname, actions.set(DnsTarget.Hostname("bad host")))
        gateway.permitted = false
        assertEquals(DnsActionResult.PermissionMissing, actions.set(DnsTarget.Automatic))
        assertTrue(gateway.writes.isEmpty())
        assertEquals(0L, gateway.version)
    }
    @Test fun laterDirectActionWinsOverPendingRevert_evenFromAnotherOwner() {
        val gateway = FakeGateway(); val tile = DnsActions(gateway)
        val timer = DnsAutoRevert(tile, revertScope)
        val cycle = tile.cycle(DnsSettings())
        timer.schedule(cycle.previous!!, cycle.generation)
        DnsActions(gateway).set(DnsTarget.Hostname("direct.example"))
        assertFalse(timer.isCurrent())
        assertNull(timer.finish())
        assertEquals(DnsTarget.Hostname("direct.example"), tile.read())
    }
    @Test fun unchangedTicketRestoresExactlyOnce_andCancelledTicketDoesNothing() {
        val gateway = FakeGateway(); val actions = DnsActions(gateway); val timer = DnsAutoRevert(actions, revertScope)
        val cycle = actions.cycle(DnsSettings())
        timer.schedule(cycle.previous!!, cycle.generation)
        assertEquals(DnsActionResult.Success, timer.finish())
        assertEquals(DnsTarget.Off, actions.read())
        assertNull(timer.finish())
        timer.schedule(DnsTarget.Automatic, actions.generation())
        timer.cancel()
        assertNull(timer.finish())
    }
    @Test fun failedDirectActionDoesNotInvalidateRestorableState() {
        val gateway = FakeGateway(); val actions = DnsActions(gateway)
        actions.set(DnsTarget.Automatic)
        val version = actions.generation()
        gateway.succeeds = false
        assertEquals(DnsActionResult.Failed, actions.set(DnsTarget.Hostname("dns.example")))
        assertEquals(DnsTarget.Automatic, actions.read())
        assertEquals(version, actions.generation())
    }
}
