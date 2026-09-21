package com.rbn.qtsettings.utils

import android.content.ContentResolver
import android.provider.Settings

/** Reads the two debugging flags that Android 17 can redact for ordinary app UIDs. */
internal object DebuggingSettingsReader {
    fun isDeveloperOptionsEnabled(resolver: ContentResolver): Boolean =
        isEnabled(resolver, Constants.DEVELOPMENT_SETTINGS_ENABLED)

    fun isUsbDebuggingEnabled(resolver: ContentResolver): Boolean =
        isEnabled(resolver, Constants.ADB_ENABLED)

    private fun isEnabled(resolver: ContentResolver, key: String): Boolean {
        val enabled = try {
            Settings.Global.getInt(resolver, key, 0) == 1
        } catch (_: RuntimeException) {
            false
        }
        if (enabled) return true

        // Android 17 may report 0 even with WRITE_SECURE_SETTINGS. An untracked
        // GET_global call returns the actual value on affected builds. See:
        // https://github.com/RBN-Apps/Quick-Tile-Settings/issues/170
        // Keep extras null: requesting generation tracking also enables redaction.
        // This provider protocol is internal, so unsupported/failed reads stay false.
        return try {
            resolver.call(Settings.Global.CONTENT_URI, "GET_global", key, null)
                ?.getString("value") == "1"
        } catch (_: RuntimeException) {
            false
        }
    }
}
