package com.rbn.qtsettings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.utils.Constants.ADB_ENABLED
import com.rbn.qtsettings.utils.Constants.ACTION_DNS_ADGUARD
import com.rbn.qtsettings.utils.Constants.ACTION_DNS_AUTO
import com.rbn.qtsettings.utils.Constants.ACTION_DNS_CLOUDFLARE
import com.rbn.qtsettings.utils.Constants.ACTION_DNS_CUSTOM
import com.rbn.qtsettings.utils.Constants.ACTION_DNS_OFF
import com.rbn.qtsettings.utils.Constants.ACTION_DNS_QUAD9
import com.rbn.qtsettings.utils.Constants.ACTION_USB_OFF
import com.rbn.qtsettings.utils.Constants.ACTION_USB_ON
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.EXTRA_DNS_ENTRY_ID
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON
import com.rbn.qtsettings.utils.Constants.PRIVATE_DNS_MODE
import com.rbn.qtsettings.utils.Constants.PRIVATE_DNS_SPECIFIER
import com.rbn.qtsettings.utils.PermissionUtils
import com.rbn.qtsettings.utils.ShortcutUtils
import com.rbn.qtsettings.utils.VpnDetectionUtils

class ShortcutActionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleAction(intent?.action)
        finish()
    }

    private fun handleAction(action: String?) {
        if (action == null) return
        val prefsManager = PreferencesManager.getInstance(this)

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
                if (!isShortcutAllowed(prefsManager, ShortcutUtils.SHORTCUT_ID_DNS_OFF)) return
                VpnDetectionUtils.setPrivateDnsOff(this)
                Toast.makeText(this, R.string.shortcut_toast_dns_off, Toast.LENGTH_SHORT).show()
            }

            ACTION_DNS_AUTO -> {
                if (!isShortcutAllowed(prefsManager, ShortcutUtils.SHORTCUT_ID_DNS_AUTO)) return
                VpnDetectionUtils.restorePrivateDns(this, DNS_MODE_AUTO)
                Toast.makeText(this, R.string.shortcut_toast_dns_auto, Toast.LENGTH_SHORT).show()
            }

            ACTION_DNS_ADGUARD -> {
                if (!isShortcutAllowed(prefsManager, ShortcutUtils.SHORTCUT_ID_DNS_ADGUARD)) return
                setDnsHostname(
                    hostname = "dns.adguard.com",
                    displayName = getString(R.string.shortcut_dns_adguard_short)
                )
            }

            ACTION_DNS_CLOUDFLARE -> {
                if (!isShortcutAllowed(
                        prefsManager,
                        ShortcutUtils.SHORTCUT_ID_DNS_CLOUDFLARE
                    )
                ) return
                setDnsHostname(
                    hostname = "one.one.one.one",
                    displayName = getString(R.string.shortcut_dns_cloudflare_short)
                )
            }

            ACTION_DNS_QUAD9 -> {
                if (!isShortcutAllowed(prefsManager, ShortcutUtils.SHORTCUT_ID_DNS_QUAD9)) return
                setDnsHostname(
                    hostname = "dns.quad9.net",
                    displayName = getString(R.string.shortcut_dns_quad9_short)
                )
            }

            ACTION_DNS_CUSTOM -> {
                val entry = prefsManager.resolveCustomDnsShortcutEntry(
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
                if (!isShortcutAllowed(prefsManager, ShortcutUtils.SHORTCUT_ID_USB_ON)) return
                handleUsbDebuggingAction(enable = true)
            }

            ACTION_USB_OFF -> {
                if (!isShortcutAllowed(prefsManager, ShortcutUtils.SHORTCUT_ID_USB_OFF)) return
                handleUsbDebuggingAction(enable = false)
            }
        }
    }

    private fun isShortcutAllowed(
        prefsManager: PreferencesManager,
        shortcutId: String
    ): Boolean {
        if (prefsManager.canExecuteShortcutId(shortcutId)) {
            return true
        }

        Toast.makeText(this, R.string.shortcut_toast_action_disabled, Toast.LENGTH_LONG).show()
        return false
    }

    private fun setDnsHostname(hostname: String, displayName: String) {
        Settings.Global.putString(contentResolver, PRIVATE_DNS_MODE, DNS_MODE_ON)
        Settings.Global.putString(contentResolver, PRIVATE_DNS_SPECIFIER, hostname)
        Toast.makeText(
            this,
            getString(R.string.shortcut_toast_dns_hostname, displayName),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun handleUsbDebuggingAction(enable: Boolean) {
        if (!PermissionUtils.isDeveloperOptionsEnabled(this)) {
            Toast.makeText(
                this,
                R.string.toast_developer_options_disabled,
                Toast.LENGTH_LONG
            ).show()
            return
        }
        Settings.Global.putInt(contentResolver, ADB_ENABLED, if (enable) 1 else 0)
        Toast.makeText(
            this,
            if (enable) R.string.shortcut_toast_usb_on else R.string.shortcut_toast_usb_off,
            Toast.LENGTH_SHORT
        ).show()
    }
}
