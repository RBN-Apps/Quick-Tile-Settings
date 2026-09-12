package com.rbn.qtsettings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rbn.qtsettings.app.AppPermissionBinding
import com.rbn.qtsettings.app.AppRoot
import com.rbn.qtsettings.app.AppSystemStateObserver
import com.rbn.qtsettings.app.navigation.AppArea
import com.rbn.qtsettings.app.navigation.resolveTileArea
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme
import com.rbn.qtsettings.feature.automation.presentation.AutomationViewModel
import com.rbn.qtsettings.feature.dns.presentation.DnsSettingsViewModel
import com.rbn.qtsettings.feature.usb.presentation.UsbSettingsViewModel
import com.rbn.qtsettings.platform.dns.DnsSystemStateObserver
import com.rbn.qtsettings.platform.usb.UsbSystemStateObserver
import com.rbn.qtsettings.viewmodel.MainViewModel

/** Stable Android entry point. App setup belongs here; feature behavior does not. */
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        (application as QuickTileSettingsApplication).container.mainViewModelFactory
    }
    private val usbViewModel: UsbSettingsViewModel by viewModels {
        (application as QuickTileSettingsApplication).container.usbViewModelFactory
    }
    private val dnsViewModel: DnsSettingsViewModel by viewModels {
        (application as QuickTileSettingsApplication).container.dnsViewModelFactory
    }
    private val automationViewModel: AutomationViewModel by viewModels {
        (application as QuickTileSettingsApplication).container.automationViewModelFactory
    }
    private var externalArea by mutableStateOf<AppArea?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Recreation restores Navigation 3's stacks, not the original tile intent.
        if (savedInstanceState == null) externalArea = intent.resolveTileArea()
        val permissions = AppPermissionBinding(this, viewModel, automationViewModel)
        lifecycle.addObserver(AppSystemStateObserver(applicationContext, viewModel, automationViewModel))
        lifecycle.addObserver(permissions)
        lifecycle.addObserver(UsbSystemStateObserver(applicationContext, usbViewModel))
        lifecycle.addObserver(DnsSystemStateObserver(applicationContext, dnsViewModel))
        viewModel.onAutomationBackupRestored = automationViewModel::onBackupRestored
        setContent {
            QuickTileSettingsTheme {
                AppRoot(
                    viewModel = viewModel,
                    usbViewModel = usbViewModel,
                    dnsViewModel = dnsViewModel,
                    automationViewModel = automationViewModel,
                    externalArea = externalArea,
                    onExternalAreaConsumed = { externalArea = null },
                    onOpenAdbSettings = { startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) },
                    onRequestShizukuPermission = permissions::requestShizukuPermission
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        externalArea = intent.resolveTileArea()
    }
}
