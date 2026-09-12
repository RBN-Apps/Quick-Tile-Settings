package com.rbn.qtsettings

import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.core.content.IntentCompat
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme
import com.rbn.qtsettings.feature.shortcuts.presentation.*
import com.rbn.qtsettings.services.PrivateDnsTileService
import com.rbn.qtsettings.services.UsbDebuggingTileService
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.PermissionUtils
import com.rbn.qtsettings.utils.SystemQuickActionResult
import com.rbn.qtsettings.utils.SystemQuickActions

class QuickActionDialogActivity : ComponentActivity() {

    private val container get() = (application as QuickTileSettingsApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)

        if (!PermissionUtils.hasWriteSecureSettingsPermission(this)) {
            Toast.makeText(this, R.string.toast_permission_not_granted_adb, Toast.LENGTH_LONG)
                .show()
            openApp(resolveTileType())
            return
        }


        setContent {
            QuickTileSettingsTheme {
                val tileType = remember { resolveTileType() }
                val actions = remember(tileType) { buildQuickActions(tileType) }
                QuickActionsDialog(
                    tileType = tileType,
                    actions = actions,
                    onDismiss = { finish() },
                    onOpenApp = { openApp(tileType) },
                    onActionClick = { action -> runQuickAction(action) }
                )
            }
        }
    }

    private fun resolveTileType(): TileType {
        return QuickActionResolver.resolveTileType(
            componentName = intent.resolveTileComponentName(),
            componentNameText = intent.getStringExtra(Intent.EXTRA_COMPONENT_NAME)
        )
    }

    private fun Intent.resolveTileComponentName(): ComponentName? {
        val component = IntentCompat.getParcelableExtra(
            this,
            Intent.EXTRA_COMPONENT_NAME,
            ComponentName::class.java
        )
        if (component != null) {
            return component
        }

        val flattenedComponent = getStringExtra(Intent.EXTRA_COMPONENT_NAME) ?: return null
        return ComponentName.unflattenFromString(flattenedComponent)
            ?: when {
                flattenedComponent.contains(UsbDebuggingTileService::class.java.name) ->
                    ComponentName(this@QuickActionDialogActivity, UsbDebuggingTileService::class.java)

                flattenedComponent.contains(PrivateDnsTileService::class.java.name) ->
                    ComponentName(this@QuickActionDialogActivity, PrivateDnsTileService::class.java)

                else -> null
            }
    }

    private fun buildQuickActions(tileType: TileType): List<QuickAction> {
        return QuickActionResolver.buildQuickActions(this, container.dnsSettingsStore.read(), container.usbSettingsStore.read(), tileType)
    }

    private fun runQuickAction(action: QuickAction) {
        val requiresUnlock = when (action.kind) {
            QuickActionKind.DnsAuto,
            is QuickActionKind.DnsHostname,
            QuickActionKind.DnsOff -> container.dnsSettingsStore.read().requireUnlock

            is QuickActionKind.UsbDebugging -> container.usbSettingsStore.read().requireUnlock
        }

        val keyguardManager = getSystemService(KeyguardManager::class.java)
        if (requiresUnlock && keyguardManager?.isKeyguardLocked == true) {
            keyguardManager.requestDismissKeyguard(
                this,
                object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() {
                        executeQuickAction(action)
                    }

                    override fun onDismissCancelled() {
                        finish()
                    }

                    override fun onDismissError() {
                        finish()
                    }
                }
            )
            return
        }

        executeQuickAction(action)
    }

    private fun executeQuickAction(action: QuickAction) {
        try {
            when (val kind = action.kind) {
                QuickActionKind.DnsOff -> {
                    handleDnsActionResult(
                        result = SystemQuickActions.setDnsMode(this, DNS_MODE_OFF),
                        successMessage = getString(R.string.shortcut_toast_dns_off)
                    )
                }

                QuickActionKind.DnsAuto -> {
                    handleDnsActionResult(
                        result = SystemQuickActions.setDnsMode(this, DNS_MODE_AUTO),
                        successMessage = getString(R.string.shortcut_toast_dns_auto)
                    )
                }

                is QuickActionKind.DnsHostname -> {
                    handleDnsActionResult(
                        result = SystemQuickActions.setActiveDns(this, kind.entry.target.value),
                        successMessage = getString(
                            R.string.shortcut_toast_dns_hostname,
                            kind.entry.name
                        )
                    )
                }

                is QuickActionKind.UsbDebugging -> {
                    val result = SystemQuickActions.setUsbDebuggingEnabled(
                        context = this,
                        enabled = kind.enable,
                        toggleDeveloperOptions = container.usbSettingsStore.read().alsoHideDevOptions,
                        toggleWirelessDebugging = container.usbSettingsStore.read().alsoDisableWirelessDebugging
                    )
                    when (result) {
                        SystemQuickActionResult.SUCCESS -> {
                            Toast.makeText(
                                this,
                                if (kind.enable) {
                                    R.string.shortcut_toast_usb_on
                                } else {
                                    R.string.shortcut_toast_usb_off
                                },
                                Toast.LENGTH_SHORT
                            ).show()
                            requestTileRefresh(UsbDebuggingTileService::class.java)
                        }

                        SystemQuickActionResult.PERMISSION_MISSING ->
                            Toast.makeText(
                                this,
                                R.string.toast_permission_not_granted_adb,
                                Toast.LENGTH_LONG
                            ).show()

                        SystemQuickActionResult.DEVELOPER_OPTIONS_DISABLED ->
                            Toast.makeText(
                                this,
                                R.string.toast_developer_options_disabled,
                                Toast.LENGTH_LONG
                            ).show()

                        SystemQuickActionResult.INVALID_DNS_HOSTNAME,
                        SystemQuickActionResult.FAILED ->
                            Toast.makeText(
                                this,
                                R.string.toast_error_saving_settings,
                                Toast.LENGTH_SHORT
                            ).show()
                    }
                }
            }
        } catch (_: Exception) {
            Toast.makeText(this, R.string.toast_error_saving_settings, Toast.LENGTH_SHORT).show()
        } finally {
            finish()
        }
    }

    private fun handleDnsActionResult(
        result: SystemQuickActionResult,
        successMessage: String
    ) {
        when (result) {
            SystemQuickActionResult.SUCCESS -> {
                Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
                requestTileRefresh(PrivateDnsTileService::class.java)
            }

            SystemQuickActionResult.PERMISSION_MISSING ->
                Toast.makeText(
                    this,
                    R.string.toast_permission_not_granted_adb,
                    Toast.LENGTH_LONG
                ).show()

            SystemQuickActionResult.INVALID_DNS_HOSTNAME,
            SystemQuickActionResult.DEVELOPER_OPTIONS_DISABLED,
            SystemQuickActionResult.FAILED ->
                Toast.makeText(
                    this,
                    R.string.toast_error_saving_settings,
                    Toast.LENGTH_SHORT
                ).show()
        }
    }

    private fun requestTileRefresh(serviceClass: Class<out TileService>) {
        TileService.requestListeningState(this, ComponentName(this, serviceClass))
    }

    private fun openApp(tileType: TileType) {
        val tileSource = when (tileType) {
            TileType.DNS -> PrivateDnsTileService::class.java.name
            TileType.USB -> UsbDebuggingTileService::class.java.name
            TileType.ALL -> null
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            tileSource?.let { putExtra(Intent.EXTRA_COMPONENT_NAME, it) }
        }

        val keyguardManager = getSystemService(KeyguardManager::class.java)
        if (keyguardManager?.isKeyguardLocked == true) {
            keyguardManager.requestDismissKeyguard(
                this,
                object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() {
                        startActivity(intent)
                        finish()
                    }

                    override fun onDismissCancelled() {
                        finish()
                    }

                    override fun onDismissError() {
                        finish()
                    }
                }
            )
        } else {
            startActivity(intent)
            finish()
        }
    }

}
