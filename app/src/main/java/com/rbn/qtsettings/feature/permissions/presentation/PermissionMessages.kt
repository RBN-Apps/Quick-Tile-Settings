package com.rbn.qtsettings.feature.permissions.presentation

import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.presentation.UiMessage
import com.rbn.qtsettings.feature.permissions.domain.GrantMethod
import com.rbn.qtsettings.feature.permissions.domain.GrantResult

internal fun GrantResult.message(method: GrantMethod): UiMessage {
    val shizuku = method == GrantMethod.Shizuku
    return when (this) {
        GrantResult.Granted -> UiMessage(if (shizuku) R.string.permission_granted_shizuku_success else R.string.permission_granted_root_success)
        GrantResult.VerificationFailed -> UiMessage(if (shizuku) R.string.permission_granted_shizuku_check_failed else R.string.permission_granted_root_check_failed)
        GrantResult.ShizukuUnavailable -> UiMessage(R.string.shizuku_not_available)
        GrantResult.ShizukuPermissionRequired -> UiMessage(R.string.shizuku_permission_not_granted_to_app_prompt)
        GrantResult.NotRooted -> UiMessage(R.string.device_not_rooted)
        is GrantResult.CommandFailed -> UiMessage(if (shizuku) R.string.permission_granted_shizuku_fail else R.string.permission_granted_root_fail, exitCode)
        is GrantResult.Error -> UiMessage(if (shizuku) R.string.permission_granted_shizuku_error else R.string.permission_granted_root_error, message ?: "Unknown error")
    }
}
