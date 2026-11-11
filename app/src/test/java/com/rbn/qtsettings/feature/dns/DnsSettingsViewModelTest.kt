package com.rbn.qtsettings.feature.dns

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.rbn.qtsettings.R
import com.rbn.qtsettings.feature.dns.data.DnsSettingsStore
import com.rbn.qtsettings.feature.dns.domain.*
import com.rbn.qtsettings.feature.dns.presentation.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DnsSettingsViewModelTest {
    private lateinit var store: DnsSettingsStore
    private lateinit var gateway: DnsActionsTest.FakeGateway
    private lateinit var handle: SavedStateHandle
    private lateinit var vm: DnsSettingsViewModel

    /** The countdown never runs here; these assertions drive the view model directly. */
    private val revertScope = CoroutineScope(Dispatchers.Unconfined)

    private fun viewModel(savedState: SavedStateHandle): DnsSettingsViewModel {
        val actions = DnsActions(gateway)
        return DnsSettingsViewModel(store, actions, DnsAutoRevert(actions, revertScope), savedState)
    }

    @Before fun setup() {
        val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("dns-owner-test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        store = DnsSettingsStore(prefs)
        gateway = DnsActionsTest.FakeGateway()
        handle = SavedStateHandle()
        vm = viewModel(handle)
    }
    @Test fun cycleFromOverview_advancesTheSameCycleAsTheTile_andNamesTheAppliedState() = runBlocking {
        store.setDnsToggleOff(true)
        store.setDnsToggleAuto(true)
        store.read().providers.forEach { store.setSelected(it.id, false) }
        vm.onAction(DnsSettingsAction.Cycle)
        val first = gateway.read()
        assertEquals(R.string.shortcut_toast_dns_auto, vm.events.first().resource)
        assertEquals(DnsTarget.Automatic, first)
        vm.onAction(DnsSettingsAction.Cycle)
        assertEquals(DnsTarget.Off, gateway.read())
        assertEquals(DnsTarget.Off, vm.state.value.active)
    }

    @Test fun cycleWithoutAnyEnabledState_reportsItAndTouchesNothing() = runBlocking {
        store.setDnsToggleOff(false)
        store.setDnsToggleAuto(false)
        store.read().providers.forEach { store.setSelected(it.id, false) }
        val before = gateway.writes.size
        vm.onAction(DnsSettingsAction.Cycle)
        assertEquals(R.string.toast_no_states_enabled_dns, vm.events.first().resource)
        assertEquals(before, gateway.writes.size)
    }

    @Test fun addingAfterEditingClearsOldDraft_andCancelWritesNothing() {
        store.add("Existing", "old.example")
        val old = store.read().providers.single { !it.predefined }
        vm.onAction(DnsSettingsAction.Edit(old.id))
        assertEquals("Existing", vm.editor.value.name)
        vm.onAction(DnsSettingsAction.Edit(null))
        assertTrue(vm.editor.value.visible)
        assertNull(vm.editor.value.id)
        assertEquals("", vm.editor.value.name)
        vm.editName("Unsaved")
        vm.dismissEditor()
        assertFalse(vm.editor.value.visible)
        assertEquals(listOf(old), store.read().providers.filter { !it.predefined })
    }
    @Test fun editorRestoresRawDraftAndErrors_thenSavesNormalizedFields() {
        vm.onAction(DnsSettingsAction.Edit(null))
        vm.editName("  Home  "); vm.editHostname("bad host")
        vm.saveEditor()
        assertEquals(DnsEditorError.HostnameInvalid, vm.editor.value.hostnameError)
        val restoredHandle = SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) })
        val restored = viewModel(restoredHandle)
        assertEquals(vm.editor.value, restored.editor.value)
        restored.editHostname(" dns.example ")
        assertNull(restored.editor.value.hostnameError)
        restored.saveEditor()
        val added = store.read().providers.single { !it.predefined }
        assertEquals("Home", added.name)
        assertEquals(DnsTarget.Hostname("dns.example"), added.target)
        assertFalse(restored.editor.value.visible)
    }
    @Test fun blankFieldsHaveIndependentErrors_andNeverSave() {
        vm.onAction(DnsSettingsAction.Edit(null)); vm.saveEditor()
        assertEquals(DnsEditorError.NameEmpty, vm.editor.value.nameError)
        assertEquals(DnsEditorError.HostnameEmpty, vm.editor.value.hostnameError)
        vm.editName("New")
        assertNull(vm.editor.value.nameError)
        assertEquals(DnsEditorError.HostnameEmpty, vm.editor.value.hostnameError)
        assertTrue(store.read().providers.all { it.predefined })
    }
    @Test fun editingPreservesIdAndCycleSelection_andPredefinedEntriesAreProtected() {
        store.add("Old", "old.example")
        val old = store.read().providers.single { !it.predefined }
        vm.onAction(DnsSettingsAction.Selected(old.id, false))
        vm.onAction(DnsSettingsAction.Edit(old.id))
        vm.editName("New"); vm.editHostname("new.example"); vm.saveEditor()
        val updated = store.read().providers.single { it.id == old.id }
        assertEquals("New", updated.name)
        assertEquals(DnsTarget.Hostname("new.example"), updated.target)
        assertFalse(updated.selectedForCycle)
        val predefined = store.read().providers.first { it.predefined }
        vm.onAction(DnsSettingsAction.Edit(predefined.id))
        assertFalse(vm.editor.value.visible)
        vm.onAction(DnsSettingsAction.Delete(predefined.id)); vm.confirmDeletion()
        assertTrue(store.read().providers.any { it.id == predefined.id })
    }
    @Test fun pendingDeletionRestores_cancelPreservesEntry_confirmRemovesIt() {
        store.add("Delete", "delete.example")
        val id = store.read().providers.single { !it.predefined }.id
        vm.onAction(DnsSettingsAction.Delete(id))
        val restored = viewModel(SavedStateHandle(mapOf("dns.delete" to id)))
        assertEquals(id, restored.pendingDeletion.value)
        restored.onAction(DnsSettingsAction.Delete(null))
        assertTrue(store.read().providers.any { it.id == id })
        restored.onAction(DnsSettingsAction.Delete(id)); restored.confirmDeletion()
        assertNull(restored.pendingDeletion.value)
        assertFalse(store.read().providers.any { it.id == id })
    }
    @Test fun selectionDoesNotSetActive_andActionsRefreshStateAndEmitMessages() = runBlocking {
        vm.onAction(DnsSettingsAction.ToggleOff(false))
        assertFalse(vm.state.value.settings.toggleOff)
        assertTrue(gateway.writes.isEmpty())
        vm.onAction(DnsSettingsAction.Activate(DnsTarget.Automatic))
        assertEquals(DnsTarget.Automatic, vm.state.value.active)
        assertEquals(DnsMessage(R.string.shortcut_toast_dns_auto), vm.events.first())
        vm.onAction(DnsSettingsAction.Activate(DnsTarget.Off))
        assertEquals(DnsTarget.Off, vm.state.value.active)
        assertEquals(DnsMessage(R.string.shortcut_toast_dns_off), vm.events.first())
        gateway.current = DnsTarget.Hostname("external.example"); vm.refresh()
        assertEquals(gateway.current, vm.state.value.active)
    }
    @Test fun manualOrderRejectsPartialAndDuplicateIds_andSurvivesSortRoundTrip() {
        vm.onAction(DnsSettingsAction.ManualOrder(true))
        val ids = store.read().providers.map { it.id }.reversed()
        assertFalse(vm.reorder(ids.dropLast(1)))
        assertFalse(vm.reorder(List(ids.size) { ids.first() }))
        assertTrue(vm.reorder(ids))
        vm.onAction(DnsSettingsAction.ManualOrder(false))
        assertFalse(vm.reorder(ids))
        vm.onAction(DnsSettingsAction.ManualOrder(true))
        assertEquals(ids, vm.state.value.settings.providers.map { it.id })
    }
}
