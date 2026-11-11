package com.rbn.qtsettings.feature.permissions.domain

/** Preserve the established setup recommendation, independently of Shizuku app authorization. */
enum class PermissionMethod { Adb, Shizuku, Root }

fun PermissionSnapshot.orderedMethods(): List<PermissionMethod> = when {
    rooted && shizukuAvailable -> listOf(PermissionMethod.Root, PermissionMethod.Shizuku, PermissionMethod.Adb)
    rooted -> listOf(PermissionMethod.Root, PermissionMethod.Adb, PermissionMethod.Shizuku)
    shizukuAvailable -> listOf(PermissionMethod.Shizuku, PermissionMethod.Adb, PermissionMethod.Root)
    else -> listOf(PermissionMethod.Adb, PermissionMethod.Shizuku, PermissionMethod.Root)
}
