package com.rbn.qtsettings.platform.permissions

/** The same grant is displayed for ADB and executed by the existing root/Shizuku gateway. */
fun permissionGrantCommand(packageName: String) =
    "pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"

fun adbPermissionGrantCommand(packageName: String) = "adb shell ${permissionGrantCommand(packageName)}"
