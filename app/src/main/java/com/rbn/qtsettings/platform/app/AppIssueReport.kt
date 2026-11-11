package com.rbn.qtsettings.platform.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.core.net.toUri
import android.os.Build
import androidx.core.app.NotificationManagerCompat

/** Explicit allowlist: no network names, DNS targets, identifiers, settings dumps or logs. */
fun issueTechnicalDetails(context: Context, info: AppInfo): String {
    val config = context.resources.configuration
    fun granted(permission: String) = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    return buildString {
        appendLine("App: ${info.versionName ?: "Unknown"} (${info.versionCode ?: "Unknown"})")
        appendLine("Package: ${info.packageName}")
        appendLine("Android: ${info.androidVersion} / API ${info.apiLevel}")
        appendLine("Security patch: ${Build.VERSION.SECURITY_PATCH}")
        appendLine("Device: ${info.manufacturer} ${info.model}")
        appendLine("ABIs: ${info.abis}")
        appendLine("Language: ${config.locales.toLanguageTags()}")
        appendLine("App window: ${config.screenWidthDp} x ${config.screenHeightDp} dp")
        appendLine("Density: ${config.densityDpi} dpi; font scale: ${config.fontScale}")
        appendLine("Dark mode: ${config.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES}")
        appendLine("WRITE_SECURE_SETTINGS: ${granted(Manifest.permission.WRITE_SECURE_SETTINGS)}")
        appendLine("Fine location: ${granted(Manifest.permission.ACCESS_FINE_LOCATION)}")
        appendLine("Background location: ${granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)}")
        append("Notifications enabled: ${NotificationManagerCompat.from(context).areNotificationsEnabled()}")
    }
}

/** Field IDs match the public issue forms. This creates an editable draft URL, never an issue. */
fun bugReportUrl(projectUrl: String, info: AppInfo, technicalDetails: String?): String =
    "$projectUrl/issues/new".toUri().buildUpon()
        .appendQueryParameter("template", "bug-report.yml")
        // The public form requires these inputs. Declining metadata must still allow submission.
        .appendQueryParameter("app-version", if (technicalDetails == null) "Not provided" else "${info.versionName ?: "Unknown"} (${info.versionCode ?: "Unknown"})")
        .appendQueryParameter("android-version", if (technicalDetails == null) "Not provided" else "${info.androidVersion} / API ${info.apiLevel}")
        .appendQueryParameter("device-model", if (technicalDetails == null) "Not provided" else "${info.manufacturer} ${info.model}")
        .apply {
            if (technicalDetails != null) appendQueryParameter("additional-context", "### Technical information\n\n$technicalDetails")
        }.build().toString()

fun featureRequestUrl(projectUrl: String): String = "$projectUrl/issues/new".toUri().buildUpon()
    .appendQueryParameter("template", "feature-request.yml").build().toString()
