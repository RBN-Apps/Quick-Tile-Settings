package com.rbn.qtsettings.feature.settings.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.presentation.UiMessage
import com.rbn.qtsettings.feature.settings.domain.BackupDocuments
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppSettingsViewModel(
    private val documents: BackupDocuments,
    private val savedState: SavedStateHandle,
    private val onBackupRestored: () -> Unit,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    val pendingRestore = savedState.getStateFlow<String?>(PENDING_RESTORE, null)
    private val messages = Channel<UiMessage>(Channel.BUFFERED)
    val events = messages.receiveAsFlow()
    private var working = false

    fun selectRestore(source: String?) { savedState[PENDING_RESTORE] = source }
    fun cancelRestore() { savedState[PENDING_RESTORE] = null }
    fun confirmRestore() {
        val source = pendingRestore.value ?: return
        if (working) return
        cancelRestore()
        transfer(restore = true, source)
    }
    fun export(destination: String) = transfer(restore = false, destination)

    private fun transfer(restore: Boolean, reference: String) {
        if (working) return
        working = true
        viewModelScope.launch {
            try {
                withContext(io) {
                    if (restore) documents.restore(reference) else documents.export(reference)
                }
                // Reconcile the existing automation owner only after a successful import.
                if (restore) onBackupRestored()
                messages.send(UiMessage(if (restore) R.string.backup_restore_success else R.string.backup_export_success))
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                messages.send(UiMessage(if (restore) R.string.backup_restore_error else R.string.backup_export_error,
                    e.message ?: "Unknown error"))
            } finally { working = false }
        }
    }
    private companion object { const val PENDING_RESTORE = "settings.pendingRestore" }
}
