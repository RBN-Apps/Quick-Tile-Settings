package com.rbn.qtsettings.feature.permissions.domain

data class PermissionSnapshot(
    val hasWriteSecureSettings: Boolean = false,
    val developerOptionsEnabled: Boolean = false,
    val shizukuAvailable: Boolean = false,
    val shizukuPermissionGranted: Boolean = false,
    val rooted: Boolean = false
)
enum class GrantMethod { Shizuku, Root }
sealed interface GrantResult {
    data object Granted : GrantResult
    data object VerificationFailed : GrantResult
    data object ShizukuUnavailable : GrantResult
    data object ShizukuPermissionRequired : GrantResult
    data object NotRooted : GrantResult
    data class CommandFailed(val exitCode: Int) : GrantResult
    data class Error(val message: String?) : GrantResult
}
interface PermissionGateway {
    fun snapshot(): PermissionSnapshot
    suspend fun runGrant(method: GrantMethod): Int
}

/** Re-check capabilities at the time of the action; never trust a stale UI snapshot. */
class PermissionGrants(private val gateway: PermissionGateway) {
    suspend fun grant(method: GrantMethod): GrantResult {
        val current = gateway.snapshot()
        if (method == GrantMethod.Shizuku) {
            if (!current.shizukuAvailable) return GrantResult.ShizukuUnavailable
            if (!current.shizukuPermissionGranted) return GrantResult.ShizukuPermissionRequired
        } else if (!current.rooted) return GrantResult.NotRooted
        val exit = gateway.runGrant(method)
        return when {
            exit != 0 -> GrantResult.CommandFailed(exit)
            gateway.snapshot().hasWriteSecureSettings -> GrantResult.Granted
            else -> GrantResult.VerificationFailed
        }
    }
}
