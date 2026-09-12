package com.rbn.qtsettings.feature.permissions

import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.presentation.UiMessage
import com.rbn.qtsettings.feature.permissions.domain.*
import com.rbn.qtsettings.feature.permissions.presentation.AppPermissionViewModel
import com.rbn.qtsettings.feature.settings.domain.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import com.rbn.qtsettings.QueuedIoDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.*
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
class AppPermissionViewModelTest {
    private val backgroundScope = CoroutineScope(Dispatchers.Unconfined)
    @After fun closeCollectors() { backgroundScope.cancel() }

    private class Gateway : PermissionGateway {
        var current = PermissionSnapshot(shizukuAvailable = true, shizukuPermissionGranted = true, rooted = true)
        var calls = 0
        var exit = 0
        var verifies = true
        var failure: Exception? = null
        override fun snapshot() = current
        override suspend fun runGrant(method: GrantMethod): Int {
            calls++
            failure?.let { throw it }
            if (exit == 0 && verifies) current = current.copy(hasWriteSecureSettings = true)
            return exit
        }
    }
    private class Settings : AppSettings {
        override val helpShown = MutableStateFlow(false)
        override fun setHelpShown(shown: Boolean) { helpShown.value = shown }
    }

    @Test fun unavailableCapabilities_neverExecutePrivilegedCommand() = runBlocking {
        val gateway = Gateway()
        val grants = PermissionGrants(gateway)
        gateway.current = PermissionSnapshot()
        assertEquals(GrantResult.ShizukuUnavailable, grants.grant(GrantMethod.Shizuku))
        assertEquals(GrantResult.NotRooted, grants.grant(GrantMethod.Root))
        gateway.current = gateway.current.copy(shizukuAvailable = true)
        assertEquals(GrantResult.ShizukuPermissionRequired, grants.grant(GrantMethod.Shizuku))
        assertEquals(0, gateway.calls)
    }

    @Test fun commandExitAndActualPermission_bothMatterForEachMethod() = runBlocking {
        GrantMethod.entries.forEach { method ->
            val gateway = Gateway()
            val grants = PermissionGrants(gateway)
            gateway.exit = 1
            assertEquals(GrantResult.CommandFailed(1), grants.grant(method))
            gateway.exit = 0; gateway.verifies = false
            assertEquals(GrantResult.VerificationFailed, grants.grant(method))
            gateway.verifies = true
            assertEquals(GrantResult.Granted, grants.grant(method))
        }
    }

    @Test fun grantIsSingleFlight_andMessageDoesNotReplayAfterCollectorRestart() = runBlocking {
        val dispatcher = QueuedIoDispatcher()
        try {
            val gateway = Gateway()
            val vm = AppPermissionViewModel(Settings(), gateway, dispatcher)
            val messages = mutableListOf<UiMessage>()
            vm.grant(GrantMethod.Root); vm.grant(GrantMethod.Root)
            dispatcher.drain()
            val first = backgroundScope.launch(Dispatchers.Unconfined) { vm.events.collect { messages += it } }
            assertEquals(1, gateway.calls)
            assertEquals(listOf(UiMessage(R.string.permission_granted_root_success)), messages)
            first.cancel()
            backgroundScope.launch(Dispatchers.Unconfined) { vm.events.collect { messages += it } }
            vm.refresh(); dispatcher.drain()
            assertEquals(1, messages.size)
            assertTrue(vm.state.value.hasWriteSecureSettings)
        } finally { backgroundScope.cancel() }
    }

    @Test fun shizukuDisconnectAndRootFailure_reportErrorAndAllowRetry() = runBlocking {
        val dispatcher = QueuedIoDispatcher()
        try {
            val gateway = Gateway()
            val vm = AppPermissionViewModel(Settings(), gateway, dispatcher)
            val messages = mutableListOf<UiMessage>()
            backgroundScope.launch(Dispatchers.Unconfined) { vm.events.collect { messages += it } }
            for (method in GrantMethod.entries) {
                gateway.failure = IllegalStateException("service unavailable")
                vm.grant(method); dispatcher.drain()
                assertEquals("service unavailable", messages.last().argument)
                gateway.failure = null
                vm.grant(method); dispatcher.drain()
                assertTrue(vm.state.value.hasWriteSecureSettings)
            }
            assertEquals(4, messages.size)
        } finally { backgroundScope.cancel() }
    }

    @Test fun permissionResultAndSettingsReturn_refreshWithoutDuplicateFeedback() = runBlocking {
        val dispatcher = QueuedIoDispatcher()
        try {
            val gateway = Gateway()
            val settings = Settings()
            val vm = AppPermissionViewModel(settings, gateway, dispatcher)
            val messages = mutableListOf<UiMessage>()
            backgroundScope.launch(Dispatchers.Unconfined) { vm.events.collect { messages += it } }
            gateway.current = gateway.current.copy(shizukuPermissionGranted = false)
            vm.onShizukuPermissionResult(false); dispatcher.drain()
            assertFalse(vm.state.value.shizukuPermissionGranted)
            gateway.current = gateway.current.copy(shizukuPermissionGranted = true, hasWriteSecureSettings = true)
            vm.refresh(); vm.refresh(); dispatcher.drain()
            assertTrue(vm.state.value.shizukuPermissionGranted)
            assertTrue(vm.state.value.hasWriteSecureSettings)
            assertEquals(listOf(UiMessage(R.string.shizuku_permission_denied_to_app)), messages)
            vm.setHelpShown(true); assertTrue(settings.helpShown.value)
        } finally { backgroundScope.cancel() }
    }
}
