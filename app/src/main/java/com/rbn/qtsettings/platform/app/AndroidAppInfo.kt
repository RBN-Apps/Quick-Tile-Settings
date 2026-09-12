package com.rbn.qtsettings.platform.app

import android.content.Context

fun appVersionName(context: Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
} catch (_: Exception) { "Unknown" }
