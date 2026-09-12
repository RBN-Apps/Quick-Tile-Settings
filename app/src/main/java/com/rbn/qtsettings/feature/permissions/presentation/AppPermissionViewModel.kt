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
    private var granting = false

    fun setHelpShown(shown: Boolean) = settings.setHelpShown(shown)
    fun refresh() {
        viewModelScope.launch {
            access.withLock {
                snapshot.value = withContext(io) { gateway.snapshot() }
                ready.value = true
            }
        }
    }

    fun grant(method: GrantMethod) {
        if (granting) return
        granting = true
        viewModelScope.launch {
            try {
                access.withLock {
                    val result = try { withContext(io) { PermissionGrants(gateway).grant(method) } }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { GrantResult.Error(e.message) }
                    snapshot.value = withContext(io) { gateway.snapshot() }
                    ready.value = true
                    messages.send(result.message(method))
                }
            } finally { granting = false }
        }
    }

    fun onShizukuPermissionResult(granted: Boolean) {
        messages.trySend(UiMessage(if (granted) R.string.shizuku_permission_granted_to_app
            else R.string.shizuku_permission_denied_to_app))
        refresh()
    }
}
