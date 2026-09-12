package com.rbn.qtsettings.platform.permissions

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.rbn.qtsettings.feature.permissions.domain.GrantMethod
import com.rbn.qtsettings.feature.permissions.domain.PermissionGateway
import com.rbn.qtsettings.feature.permissions.domain.PermissionSnapshot
import com.rbn.qtsettings.utils.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import rikka.shizuku.Shizuku

/** Blocking Android/root/Shizuku access; callers run this gateway off the main thread. */
class AndroidPermissionGateway(context: Context) : PermissionGateway {
    private val context = context.applicationContext
    override fun snapshot() = PermissionSnapshot(
        hasWriteSecureSettings = PermissionUtils.hasWriteSecureSettingsPermission(context),
        developerOptionsEnabled = PermissionUtils.isDeveloperOptionsEnabled(context),
        shizukuAvailable = PermissionUtils.isShizukuAvailableAndReady(),
        shizukuPermissionGranted = PermissionUtils.checkShizukuPermission(context) == PackageManager.PERMISSION_GRANTED,
        rooted = PermissionUtils.isDeviceRooted()
    )

    override suspend fun runGrant(method: GrantMethod): Int = coroutineScope {
        val command = "pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
        val process = when (method) {
            GrantMethod.Shizuku -> Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            GrantMethod.Root -> Runtime.getRuntime().exec(arrayOf("su", "-c", command))
        }
        try {
            // Drain both pipes concurrently so either pipe filling cannot block waitFor.
            val stdout = async(Dispatchers.IO) { process.inputStream.bufferedReader().use { it.readText() } }
            val stderr = async(Dispatchers.IO) { process.errorStream.bufferedReader().use { it.readText() } }
            val exit = process.waitFor()
            stdout.await()
            val error = stderr.await()
            if (exit != 0) Log.e("PermissionGrant", "$method failed ($exit): $error")
            exit
        } finally { process.destroy() }
    }
}
