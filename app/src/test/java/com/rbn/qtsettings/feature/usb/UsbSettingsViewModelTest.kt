package com.rbn.qtsettings.feature.usb

import android.content.Context
import com.rbn.qtsettings.feature.usb.data.UsbSettingsStore
import com.rbn.qtsettings.feature.usb.domain.UsbActions
import com.rbn.qtsettings.feature.usb.presentation.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class UsbSettingsViewModelTest {

    @Test fun existingPreferenceKeysAndDefaults_areReadWithoutMigration() {
        val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("qt_settings_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear()
            .putBoolean("usb_toggle_enable", false)
            .putBoolean("usb_toggle_disable", false)
            .putBoolean("usb_also_hide_dev_options", true)
            .putBoolean("usb_also_disable_wireless_debugging", true)
            .putBoolean("usb_enable_auto_revert", true)
            .putInt("usb_auto_revert_delay_seconds", 27)
            .putBoolean("usb_require_unlock", true).commit()
        val store = UsbSettingsStore(prefs)
        assertEquals(com.rbn.qtsettings.feature.usb.domain.UsbSettings(false, false, true, true, true, 27, true), store.read())
        store.setToggleEnable(true)
        assertTrue(prefs.getBoolean("usb_toggle_enable", false))
        prefs.edit().clear().commit()
        store.refresh()
        assertEquals(com.rbn.qtsettings.feature.usb.domain.UsbSettings(), store.state.value)
    }

    @Test fun cycleSelectionPersists_withoutChangingSystem_andDirectActionRefreshesStateAndEmitsOnce() = runBlocking {
        val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("usb-vm-test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val store = UsbSettingsStore(prefs)
        val gateway = UsbActionsTest.FakeGateway()
        val vm = UsbSettingsViewModel(store, UsbActions(gateway))
        vm.onAction(UsbSettingsAction.ToggleEnable(false))
        assertFalse(vm.state.value.settings.toggleEnable)
        assertFalse(UsbSettingsStore(prefs).read().toggleEnable)
        assertTrue(gateway.writes.isEmpty())
        vm.onAction(UsbSettingsAction.SetActive(true))
        assertTrue(vm.state.value.system.active)
        assertEquals(com.rbn.qtsettings.R.string.shortcut_toast_usb_on, vm.events.first())
        gateway.snapshot = gateway.snapshot.copy(usb = false)
        vm.refresh()
        assertFalse(vm.state.value.system.active)
        prefs.edit().putBoolean(UsbSettingsStore.KEY_USB_REQUIRE_UNLOCK, true).commit()
        store.refresh()
        assertTrue(vm.state.value.settings.requireUnlock)
    }
}
