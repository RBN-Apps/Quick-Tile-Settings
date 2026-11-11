package com.rbn.qtsettings.feature.usb

import com.rbn.qtsettings.feature.usb.domain.*
import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class UsbActionsTest {
    /** These assertions drive the ticket directly; no countdown ever runs on it. */
    private val revertScope = CoroutineScope(Dispatchers.Unconfined)

    internal class FakeGateway : UsbSystemGateway {
        var snapshot = UsbSystemState(false, true, false)
        var permission = true
        var epoch = 0L
        var failWrites: Set<Int> = emptySet()
        var throwWrites: Set<Int> = emptySet()
        val writes = mutableListOf<Pair<UsbSetting, Boolean>>()
        override fun hasPermission() = permission
        override fun read() = snapshot
        override fun generation() = epoch
        override fun invalidateRevert() { epoch++ }
        override fun write(setting: UsbSetting, enabled: Boolean): Boolean {
            writes += setting to enabled
            if (writes.size in throwWrites) throw SecurityException("permission lost")
            if (writes.size in failWrites) return false
            snapshot = when (setting) {
                UsbSetting.Usb -> snapshot.copy(usb = enabled)
                UsbSetting.DeveloperOptions -> snapshot.copy(developerOptions = enabled)
                UsbSetting.Wireless -> snapshot.copy(wireless = enabled)
            }
            return true
        }
    }

    @Test fun failedWirelessWrite_rollsBackAllCompanions_withoutInvalidatingPendingRevert() {
        val gateway = FakeGateway().apply { snapshot = UsbSystemState(false, false, false); failWrites = setOf(3) }
        assertEquals(UsbActionResult.Failed, UsbActions(gateway).set(true, true, true))
        assertEquals(UsbSystemState(false, false, false), gateway.snapshot)
        assertEquals(0L, gateway.epoch)
    }

    @Test fun exceptionDuringWrite_rollsBack() {
        val gateway = FakeGateway().apply { throwWrites = setOf(2) }
        assertEquals(UsbActionResult.Failed, UsbActions(gateway).set(true, false, true))
        assertEquals(UsbSystemState(false, true, false), gateway.snapshot)
    }

    @Test fun rollbackFailure_isExplicitAndInvalidatesOldRevert() {
        val gateway = FakeGateway().apply { failWrites = setOf(2, 3) }
        assertEquals(UsbActionResult.PartialFailure, UsbActions(gateway).set(true, false, true))
        assertEquals(1L, gateway.epoch)
    }

    @Test fun permissionAndDeveloperOptionsFailures_doNotWrite() {
        val gateway = FakeGateway().apply { permission = false }
        val actions = UsbActions(gateway)
        assertEquals(UsbActionResult.PermissionMissing, actions.set(true, true, true))
        gateway.permission = true
        gateway.snapshot = UsbSystemState(false, false, false)
        assertEquals(UsbActionResult.DeveloperOptionsDisabled, actions.set(true, false, true))
        assertTrue(gateway.writes.isEmpty())
    }

    @Test fun companionOrder_enablesDeveloperOptionsFirst_andDisablesThemLast() {
        val gateway = FakeGateway()
        val actions = UsbActions(gateway)
        actions.set(true, true, true)
        assertEquals(UsbSetting.DeveloperOptions to true, gateway.writes.first())
        gateway.writes.clear()
        actions.set(false, true, true)
        assertEquals(UsbSetting.DeveloperOptions to false, gateway.writes.last())
    }

    @Test fun emptyCycleAndSingleStateAndStaleAdbFlag() {
        val gateway = FakeGateway()
        val actions = UsbActions(gateway)
        assertEquals(UsbActionResult.NoCycleStates, actions.cycle(UsbSettings(toggleEnable = false, toggleDisable = false)).result)
        assertTrue(gateway.writes.isEmpty())
        repeat(2) { actions.cycle(UsbSettings(toggleDisable = false)); assertTrue(gateway.snapshot.usb) }
        gateway.snapshot = UsbSystemState(true, false, false)
        actions.cycle(UsbSettings(alsoHideDevOptions = true))
        assertTrue(gateway.snapshot.active)
    }

    @Test fun actionBetweenCycleAndScheduling_cannotBeOverwrittenByTimer() {
        val gateway = FakeGateway()
        val actions = UsbActions(gateway)
        val cycle = actions.cycle(UsbSettings())
        actions.set(true, false, false)
        val revert = UsbAutoRevert(actions, revertScope)
        revert.schedule(cycle.previous!!, cycle.generation)
        assertNull(revert.finish(UsbSettings()))
        assertTrue(gateway.snapshot.usb)
    }

    @Test fun cancelAndRepeatedFinish_neverApplyTwice() {
        val gateway = FakeGateway()
        val actions = UsbActions(gateway)
        val revert = UsbAutoRevert(actions, revertScope)
        val cycle = actions.cycle(UsbSettings())
        revert.schedule(cycle.previous!!, cycle.generation)
        revert.cancel()
        assertNull(revert.finish(UsbSettings()))
        revert.schedule(cycle.previous, cycle.generation)
        assertEquals(UsbActionResult.Success, revert.finish(UsbSettings()))
        assertNull(revert.finish(UsbSettings()))
        assertFalse(gateway.snapshot.usb)
    }

    @Test fun developerOptionsDisabledDuringDelay_blocksRestoration() {
        val gateway = FakeGateway()
        val actions = UsbActions(gateway)
        val cycle = actions.cycle(UsbSettings())
        val revert = UsbAutoRevert(actions, revertScope)
        revert.schedule(cycle.previous!!, cycle.generation)
        gateway.snapshot = gateway.snapshot.copy(developerOptions = false)
        val count = gateway.writes.size
        assertEquals(UsbActionResult.DeveloperOptionsDisabled, revert.finish(UsbSettings()))
        assertEquals(count, gateway.writes.size)
    }
}
