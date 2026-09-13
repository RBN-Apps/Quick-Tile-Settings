package com.rbn.qtsettings.feature.permissions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.presentation.UiMessage
import com.rbn.qtsettings.feature.permissions.domain.*
import com.rbn.qtsettings.feature.settings.domain.AppSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AppPermissionViewModel(
    private val settings: AppSettings,
    private val gateway: PermissionGateway,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    val helpShown = settings.helpShown
    private val snapshot = MutableStateFlow(PermissionSnapshot())
    val state = snapshot.asStateFlow()
    private val ready = MutableStateFlow(false)
    val initialized = ready.asStateFlow()
    private val messages = Channel<UiMessage>(Channel.BUFFERED)
    val events = messages.receiveAsFlow()
    private val access = Mutex()
    private val operation = MutableStateFlow(PermissionOperation())
    val progress = operation.asStateFlow()

    fun setHelpShown(shown: Boolean) = settings.setHelpShown(shown)
    fun refresh() {
        viewModelScope.launch {
            access.withLock {
                operation.update { it.copy(checking = true) }
                try {
                    val latest = withContext(io) { gateway.snapshot() }
                    val permissionChanged = snapshot.value.hasWriteSecureSettings != latest.hasWriteSecureSettings
                    snapshot.value = latest
                    ready.value = true
                    operation.update { it.copy(checkFailed = false,
                        feedback = if (permissionChanged) null else it.feedback) }
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { operation.update { it.copy(checkFailed = true) } }
                finally { operation.update { it.copy(checking = false) } }
            }
        }
    }

    fun grant(method: GrantMethod) {
        if (operation.value.granting) return
        operation.update { it.copy(granting = true, feedback = null) }
        viewModelScope.launch {
            try {
                access.withLock {
                    val result = try { withContext(io) { PermissionGrants(gateway).grant(method) } }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { GrantResult.Error(e.message) }
                    try {
                        snapshot.value = withContext(io) { gateway.snapshot() }
                        ready.value = true
                        operation.update { it.copy(checkFailed = false, feedback = result.message(method)) }
                        messages.send(result.message(method))
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) {
                        operation.update { it.copy(checkFailed = true,
                            feedback = UiMessage(R.string.permission_check_failed)) }
                    }
                }
            } finally { operation.update { it.copy(granting = false) } }
        }
    }

    fun onShizukuPermissionResult(granted: Boolean) {
        val message = UiMessage(if (granted) R.string.shizuku_permission_granted_to_app
            else R.string.shizuku_permission_denied_to_app)
        operation.update { it.copy(feedback = message) }
        messages.trySend(message)
        refresh()
    }
}

/** Transient status only; grants and permission truth stay in the existing owner/gateway. */
data class PermissionOperation(
    val checking: Boolean = false,
    val granting: Boolean = false,
    val checkFailed: Boolean = false,
    val feedback: UiMessage? = null
)
