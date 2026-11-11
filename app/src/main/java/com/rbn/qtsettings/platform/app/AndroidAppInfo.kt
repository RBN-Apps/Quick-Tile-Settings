package com.rbn.qtsettings.platform.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri

/** Public installation/device metadata only; contains no identifiers or user configuration. */
data class AppInfo(
    val versionName: String?,
    val versionCode: Long?,
    val packageName: String,
    val androidVersion: String,
    val apiLevel: Int,
    val manufacturer: String,
    val model: String,
    val abis: String
)

data class InstalledAppInfo(val details: AppInfo, val icon: Bitmap?)

fun readInstalledAppInfo(context: Context): InstalledAppInfo {
    val info = try {
        context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (_: android.content.pm.PackageManager.NameNotFoundException) { null }
    return InstalledAppInfo(
        AppInfo(info?.versionName, info?.longVersionCode, context.packageName,
            Build.VERSION.RELEASE, Build.VERSION.SDK_INT, Build.MANUFACTURER, Build.MODEL,
            Build.SUPPORTED_ABIS.joinToString(", ")),
        // loadIcon preserves the adaptive foreground/background and its platform mask.
        context.applicationInfo.loadIcon(context.packageManager).toBitmap(192, 192)
    )
}

/** Attempt the public URL directly, without package visibility queries or network permission. */
fun openAppProjectLink(context: Context, url: String): Boolean = try {
    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: SecurityException) {
    false
}
