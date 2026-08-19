package com.rbn.qtsettings

import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.IntentCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.services.PrivateDnsTileService
import com.rbn.qtsettings.services.UsbDebuggingTileService
import com.rbn.qtsettings.ui.theme.QuickTileSettingsTheme
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.PermissionUtils
import com.rbn.qtsettings.utils.SystemQuickActionResult
import com.rbn.qtsettings.utils.SystemQuickActions

class QuickActionDialogActivity : ComponentActivity() {

    private lateinit var prefsManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)

        if (!PermissionUtils.hasWriteSecureSettingsPermission(this)) {
            Toast.makeText(this, R.string.toast_permission_not_granted_adb, Toast.LENGTH_LONG)
                .show()
            openApp(resolveTileType())
            return
        }

        prefsManager = PreferencesManager.getInstance(applicationContext)

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
        return QuickActionResolver.buildQuickActions(this, prefsManager, tileType)
    }

    private fun runQuickAction(action: QuickAction) {
        val requiresUnlock = when (action.kind) {
            QuickActionKind.DnsAuto,
            is QuickActionKind.DnsHostname,
            QuickActionKind.DnsOff -> prefsManager.isDnsRequireUnlockEnabled()

            is QuickActionKind.UsbDebugging -> prefsManager.isUsbRequireUnlockEnabled()
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
                        result = SystemQuickActions.setActiveDns(this, kind.entry.hostname),
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
                        toggleDeveloperOptions = prefsManager.isUsbAlsoHideDevOptionsEnabled(),
                        toggleWirelessDebugging = prefsManager
                            .isUsbAlsoDisableWirelessDebuggingEnabled()
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

    @Composable
    private fun QuickActionsDialog(
        tileType: TileType,
        actions: List<QuickAction>,
        onDismiss: () -> Unit,
        onOpenApp: () -> Unit,
        onActionClick: (QuickAction) -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = {
                Text(
                    text = when (tileType) {
                        TileType.DNS -> stringResource(R.string.quick_action_dns_title)
                        TileType.USB -> stringResource(R.string.quick_action_usb_title)
                        TileType.ALL -> stringResource(R.string.quick_action_title)
                    }
                )
            },
            text = {
                if (actions.isEmpty()) {
                    Text(stringResource(R.string.quick_action_no_actions))
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(actions) { action ->
                            TextButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onActionClick(action) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(action.iconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = action.label,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = action.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
            dismissButton = {
                TextButton(onClick = onOpenApp) {
                    Text(stringResource(R.string.quick_action_open_app))
                }
            }
        )
    }
}

internal enum class TileType {
    DNS,
    USB,
    ALL
}

internal data class QuickAction(
    val label: String,
    val description: String,
    val iconRes: Int,
    val kind: QuickActionKind
)

internal sealed interface QuickActionKind {
    data object DnsOff : QuickActionKind
    data object DnsAuto : QuickActionKind
    data class DnsHostname(val entry: DnsHostnameEntry) : QuickActionKind
    data class UsbDebugging(val enable: Boolean) : QuickActionKind
}

internal object QuickActionResolver {

    fun resolveTileType(
        componentName: ComponentName?,
        componentNameText: String?
    ): TileType {
        return when {
            componentName?.className == UsbDebuggingTileService::class.java.name ||
                    componentNameText?.contains(UsbDebuggingTileService::class.java.name) == true ->
                TileType.USB

            componentName?.className == PrivateDnsTileService::class.java.name ||
                    componentNameText?.contains(PrivateDnsTileService::class.java.name) == true ->
                TileType.DNS

            else -> TileType.ALL
        }
    }

    fun buildQuickActions(
        context: Context,
        prefsManager: PreferencesManager,
        tileType: TileType
    ): List<QuickAction> {
        val actions = mutableListOf<QuickAction>()
        if (tileType == TileType.DNS || tileType == TileType.ALL) {
            actions += buildDnsActions(context, prefsManager)
        }
        if (tileType == TileType.USB || tileType == TileType.ALL) {
            actions += buildUsbActions(context, prefsManager)
        }
        return actions
    }

    private fun buildDnsActions(
        context: Context,
        prefsManager: PreferencesManager
    ): List<QuickAction> {
        val actions = mutableListOf<QuickAction>()
        if (prefsManager.isDnsToggleOffEnabled()) {
            actions += QuickAction(
                label = context.getString(R.string.shortcut_dns_off_short),
                description = context.getString(R.string.shortcut_dns_off_long),
                iconRes = R.drawable.ic_dns_off,
                kind = QuickActionKind.DnsOff
            )
        }
        if (prefsManager.isDnsToggleAutoEnabled()) {
            actions += QuickAction(
                label = context.getString(R.string.shortcut_dns_auto_short),
                description = context.getString(R.string.shortcut_dns_auto_long),
                iconRes = R.drawable.ic_dns_auto,
                kind = QuickActionKind.DnsAuto
            )
        }
        prefsManager.getDnsHostnamesSelectedForCycle().forEach { entry ->
            actions += QuickAction(
                label = entry.name,
                description = entry.hostname,
                iconRes = getDnsIcon(entry),
                kind = QuickActionKind.DnsHostname(entry)
            )
        }
        return actions
    }

    private fun buildUsbActions(
        context: Context,
        prefsManager: PreferencesManager
    ): List<QuickAction> {
        val actions = mutableListOf<QuickAction>()
        if (prefsManager.isUsbToggleEnableEnabled()) {
            actions += QuickAction(
                label = context.getString(R.string.shortcut_usb_on_short),
                description = context.getString(R.string.shortcut_usb_on_long),
                iconRes = R.drawable.ic_usb_on,
                kind = QuickActionKind.UsbDebugging(enable = true)
            )
        }
        if (prefsManager.isUsbToggleDisableEnabled()) {
            actions += QuickAction(
                label = context.getString(R.string.shortcut_usb_off_short),
                description = context.getString(R.string.shortcut_usb_off_long),
                iconRes = R.drawable.ic_usb_off,
                kind = QuickActionKind.UsbDebugging(enable = false)
            )
        }
        return actions
    }

    private fun getDnsIcon(entry: DnsHostnameEntry): Int =
        when (entry.hostname) {
            "dns.adguard.com" -> R.drawable.ic_dns_on_adguard
            "one.one.one.one" -> R.drawable.ic_dns_on_cloudflare
            "dns.quad9.net" -> R.drawable.ic_dns_on_quad9_security
            else -> R.drawable.ic_dns_on
        }
}
