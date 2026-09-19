package com.rbn.qtsettings.app

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rbn.qtsettings.feature.permissions.domain.PermissionSnapshot
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** A foreground grant does not cause onResume or a DNS/USB settings change. */
class AppControlPermissionObserver(
    private val permissions: StateFlow<PermissionSnapshot>,
    private val refreshFeatures: () -> Unit
) : DefaultLifecycleObserver {
    override fun onCreate(owner: LifecycleOwner) {
        owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                permissions.map { it.hasWriteSecureSettings }
                    .distinctUntilChanged()
                    .collect { refreshFeatures() }
            }
        }
    }
}
