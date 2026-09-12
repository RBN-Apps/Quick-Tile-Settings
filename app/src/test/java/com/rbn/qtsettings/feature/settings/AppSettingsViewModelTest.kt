package com.rbn.qtsettings.feature.settings

import androidx.lifecycle.SavedStateHandle
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.presentation.UiMessage
import com.rbn.qtsettings.feature.settings.domain.BackupDocuments
import com.rbn.qtsettings.feature.settings.presentation.AppSettingsViewModel
import kotlinx.coroutines.Dispatchers
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
class AppSettingsViewModelTest {
    private val backgroundScope = CoroutineScope(Dispatchers.Unconfined)
    @After fun closeCollectors() { backgroundScope.cancel() }

    private class Documents : BackupDocuments {
        val imports = mutableListOf<String>()
        val exports = mutableListOf<String>()
        var failure: Exception? = null
        override fun export(destination: String) { failure?.let { throw it }; exports += destination }
        override fun restore(source: String) { failure?.let { throw it }; imports += source }
    }

    @Test fun selectionSurvivesRecreation_cancelDoesNotImport_confirmRunsOnce() = runBlocking {
        val dispatcher = QueuedIoDispatcher()
        try {
            val documents = Documents()
            var reconciliations = 0
            val handle = SavedStateHandle()
            val vm = AppSettingsViewModel(documents, handle, { reconciliations++ }, dispatcher)
            vm.selectRestore("content://backup/old")
            val recreated = AppSettingsViewModel(documents, SavedStateHandle(mapOf(
                "settings.pendingRestore" to handle.get<String>("settings.pendingRestore"))), { reconciliations++ }, dispatcher)
            assertEquals("content://backup/old", recreated.pendingRestore.value)
            recreated.cancelRestore(); recreated.confirmRestore(); dispatcher.drain()
            assertTrue(documents.imports.isEmpty())
            recreated.selectRestore("content://backup/new")
            recreated.confirmRestore(); recreated.confirmRestore(); dispatcher.drain()
            assertEquals(listOf("content://backup/new"), documents.imports)
            assertEquals(1, reconciliations)
            assertNull(recreated.pendingRestore.value)
        } finally { backgroundScope.cancel() }
    }

    @Test fun invalidImport_doesNotReconcileMonitoring_andSuccessMessageIsConsumedOnce() = runBlocking {
        val dispatcher = QueuedIoDispatcher()
        try {
            val documents = Documents()
            var reconciliations = 0
            val vm = AppSettingsViewModel(documents, SavedStateHandle(), { reconciliations++ }, dispatcher)
            val messages = mutableListOf<UiMessage>()
            val collector = backgroundScope.launch(Dispatchers.Unconfined) { vm.events.collect { messages += it } }
            documents.failure = IllegalArgumentException("Invalid backup file")
            vm.selectRestore("content://backup/invalid"); vm.confirmRestore(); dispatcher.drain()
            assertEquals(0, reconciliations)
            assertEquals(R.string.backup_restore_error, messages.single().resource)
            documents.failure = null
            vm.selectRestore("content://backup/valid"); vm.confirmRestore(); dispatcher.drain()
            assertEquals(1, reconciliations)
            assertEquals(R.string.backup_restore_success, messages.last().resource)
            collector.cancel()
            backgroundScope.launch(Dispatchers.Unconfined) { vm.events.collect { messages += it } }
            dispatcher.drain()
            assertEquals(2, messages.size)
        } finally { backgroundScope.cancel() }
    }

    @Test fun exportFailureAndRetry_neverTriggerRestoreSideEffects() = runBlocking {
        val dispatcher = QueuedIoDispatcher()
        try {
            val documents = Documents()
            val vm = AppSettingsViewModel(documents, SavedStateHandle(), { fail("Export must not reconcile") }, dispatcher)
            val messages = mutableListOf<UiMessage>()
            backgroundScope.launch(Dispatchers.Unconfined) { vm.events.collect { messages += it } }
            documents.failure = IllegalStateException("Cannot open destination")
            vm.export("content://backup/export"); dispatcher.drain()
            assertEquals(R.string.backup_export_error, messages.single().resource)
            documents.failure = null
            vm.export("content://backup/export"); vm.export("content://backup/export"); dispatcher.drain()
            assertEquals(listOf("content://backup/export"), documents.exports)
            assertEquals(R.string.backup_export_success, messages.last().resource)
        } finally { backgroundScope.cancel() }
    }
}
