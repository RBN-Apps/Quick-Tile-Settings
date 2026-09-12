package com.rbn.qtsettings

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.rbn.qtsettings.feature.shortcuts.data.ShortcutSettingsStore
import com.rbn.qtsettings.feature.shortcuts.domain.ShortcutCatalog
import com.rbn.qtsettings.utils.Constants.ACTION_DNS_ADGUARD
import com.rbn.qtsettings.utils.Constants.ACTION_DNS_AUTO
import com.rbn.qtsettings.utils.Constants.ACTION_DNS_CLOUDFLARE
import com.rbn.qtsettings.utils.Constants.ACTION_DNS_CUSTOM
import com.rbn.qtsettings.utils.Constants.ACTION_DNS_OFF
import com.rbn.qtsettings.utils.Constants.ACTION_DNS_QUAD9
import com.rbn.qtsettings.utils.Constants.ACTION_USB_OFF
import com.rbn.qtsettings.utils.Constants.ACTION_USB_ON
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.EXTRA_DNS_ENTRY_ID
import com.rbn.qtsettings.utils.PermissionUtils
import com.rbn.qtsettings.utils.SystemQuickActionResult
import com.rbn.qtsettings.utils.SystemQuickActions

class ShortcutActionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleAction(intent?.action)
        finish()
    }

    private fun handleAction(action: String?) {
        if (action == null) return
        val shortcutSettings = (application as QuickTileSettingsApplication).container.shortcutSettingsStore

        if (!PermissionUtils.hasWriteSecureSettingsPermission(this)) {
            Toast.makeText(this, R.string.shortcut_toast_permission_missing, Toast.LENGTH_LONG)
                .show()
            startActivity(Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            return
        }

        when (action) {
            ACTION_DNS_OFF -> {
                if (!isShortcutAllowed(shortcutSettings, ShortcutCatalog.SHORTCUT_ID_DNS_OFF)) return
                val message = if (SystemQuickActions.setDnsMode(this, DNS_MODE_OFF) == SystemQuickActionResult.SUCCESS) {
                    R.string.shortcut_toast_dns_off
                } else {
                    R.string.shortcut_toast_action_failed
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }

            ACTION_DNS_AUTO -> {
                if (!isShortcutAllowed(shortcutSettings, ShortcutCatalog.SHORTCUT_ID_DNS_AUTO)) return
                val result = SystemQuickActions.setDnsMode(this, DNS_MODE_AUTO)
                Toast.makeText(this, if (result == SystemQuickActionResult.SUCCESS)
                    R.string.shortcut_toast_dns_auto else R.string.shortcut_toast_action_failed, Toast.LENGTH_SHORT).show()
            }

            ACTION_DNS_ADGUARD -> {
                if (!isShortcutAllowed(shortcutSettings, ShortcutCatalog.SHORTCUT_ID_DNS_ADGUARD)) return
                setDnsHostname(
                    hostname = "dns.adguard.com",
                    displayName = getString(R.string.shortcut_dns_adguard_short)
                )
            }

            ACTION_DNS_CLOUDFLARE -> {
                if (!isShortcutAllowed(
                        shortcutSettings,
                        ShortcutCatalog.SHORTCUT_ID_DNS_CLOUDFLARE
                    )
                ) return
                setDnsHostname(
                    hostname = "one.one.one.one",
                    displayName = getString(R.string.shortcut_dns_cloudflare_short)
                )
            }

            ACTION_DNS_QUAD9 -> {
                if (!isShortcutAllowed(shortcutSettings, ShortcutCatalog.SHORTCUT_ID_DNS_QUAD9)) return
                setDnsHostname(
                    hostname = "dns.quad9.net",
                    displayName = getString(R.string.shortcut_dns_quad9_short)
                )
            }

            ACTION_DNS_CUSTOM -> {
                val entry = shortcutSettings.resolveCustomDnsShortcutEntry(
                    intent?.getStringExtra(EXTRA_DNS_ENTRY_ID)
                )
                if (entry == null) {
                    Toast.makeText(
                        this,
                        R.string.shortcut_toast_action_unavailable,
                        Toast.LENGTH_LONG
                    )
                        .show()
                    return
                }
                setDnsHostname(entry.hostname, entry.name)
            }

            ACTION_USB_ON -> {
                if (!isShortcutAllowed(shortcutSettings, ShortcutCatalog.SHORTCUT_ID_USB_ON)) return
                handleUsbDebuggingAction(enable = true)
            }

            ACTION_USB_OFF -> {
                if (!isShortcutAllowed(shortcutSettings, ShortcutCatalog.SHORTCUT_ID_USB_OFF)) return
                handleUsbDebuggingAction(enable = false)
            }
        }
    }

    private fun isShortcutAllowed(
        shortcutSettings: ShortcutSettingsStore,
        shortcutId: String
    ): Boolean {
        if (shortcutSettings.canExecuteShortcutId(shortcutId)) {
            return true
        }

        Toast.makeText(this, R.string.shortcut_toast_action_disabled, Toast.LENGTH_LONG).show()
        return false
    }

    private fun setDnsHostname(hostname: String, displayName: String) {
        val result = SystemQuickActions.setActiveDns(this, hostname)
        Toast.makeText(
            this,
            if (result == SystemQuickActionResult.SUCCESS) getString(R.string.shortcut_toast_dns_hostname, displayName)
            else getString(R.string.shortcut_toast_action_failed),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun handleUsbDebuggingAction(enable: Boolean) {
        val result = SystemQuickActions.setUsbDebuggingEnabled(
            this, enable, toggleDeveloperOptions = false, toggleWirelessDebugging = false
        )
        val message = when (result) {
            SystemQuickActionResult.SUCCESS ->
                if (enable) R.string.shortcut_toast_usb_on else R.string.shortcut_toast_usb_off
            SystemQuickActionResult.DEVELOPER_OPTIONS_DISABLED -> R.string.toast_developer_options_disabled
            SystemQuickActionResult.PERMISSION_MISSING -> R.string.toast_permission_not_granted_adb
            else -> R.string.toast_error_saving_settings
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
