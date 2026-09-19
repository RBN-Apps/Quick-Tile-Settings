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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rbn.qtsettings.app.AppPermissionBinding
import com.rbn.qtsettings.app.AppControlPermissionObserver
import com.rbn.qtsettings.app.AppRoot
import com.rbn.qtsettings.app.AppSystemStateObserver
import com.rbn.qtsettings.app.navigation.AppArea
import com.rbn.qtsettings.app.navigation.resolveTileArea
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme
import com.rbn.qtsettings.feature.automation.presentation.AutomationViewModel
import com.rbn.qtsettings.feature.dns.presentation.DnsSettingsViewModel
import com.rbn.qtsettings.feature.onboarding.presentation.OnboardingRoot
import com.rbn.qtsettings.feature.onboarding.presentation.OnboardingViewModel
import com.rbn.qtsettings.feature.usb.presentation.UsbSettingsViewModel
import com.rbn.qtsettings.platform.dns.DnsSystemStateObserver
import com.rbn.qtsettings.platform.tiles.TilePanelRequests
import com.rbn.qtsettings.platform.usb.UsbSystemStateObserver
import com.rbn.qtsettings.feature.permissions.presentation.AppPermissionViewModel
import com.rbn.qtsettings.feature.settings.presentation.AppSettingsViewModel

/** Stable Android entry point. App setup belongs here; feature behavior does not. */
class MainActivity : ComponentActivity() {
    private val viewModel: AppPermissionViewModel by viewModels {
        (application as QuickTileSettingsApplication).container.permissionViewModelFactory
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
    private val settingsViewModel: AppSettingsViewModel by viewModels {
        val context = applicationContext
        val automation = automationViewModel
        (application as QuickTileSettingsApplication).container.settingsViewModelFactory {
            automation.onBackupRestored(context)
        }
    }
    private val onboardingViewModel: OnboardingViewModel by viewModels {
        (application as QuickTileSettingsApplication).container.onboardingViewModelFactory
    }
    private var externalArea by mutableStateOf<AppArea?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Recreation restores Navigation 3's stacks, not the original tile intent.
        if (savedInstanceState == null) externalArea = intent.resolveTileArea()
        (application as QuickTileSettingsApplication).container.shortcutSettingsStore.refreshShortcutConfiguration()
        val permissions = AppPermissionBinding(this, viewModel, automationViewModel)
        lifecycle.addObserver(AppSystemStateObserver(applicationContext, viewModel, automationViewModel))
        lifecycle.addObserver(permissions)
        lifecycle.addObserver(UsbSystemStateObserver(applicationContext, usbViewModel))
        lifecycle.addObserver(DnsSystemStateObserver(applicationContext, dnsViewModel))
        lifecycle.addObserver(AppControlPermissionObserver(viewModel.state) {
            dnsViewModel.refresh()
            usbViewModel.refresh()
        })
        val openDeveloperOptions = { startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
        val tilePanel = TilePanelRequests(this)
        setContent {
            QuickTileSettingsTheme {
                // The first run owns the whole window; the shell appears once it is finished.
                val onboarded by onboardingViewModel.completed.collectAsStateWithLifecycle()
                if (!onboarded) OnboardingRoot(onboardingViewModel, viewModel,
                    onOpenDeveloperOptions = openDeveloperOptions,
                    onRequestShizukuPermission = permissions::requestShizukuPermission,
                    onAddTile = { tile ->
                        tilePanel.request(tile) { onboardingViewModel.onTileResult(tile, it) }
                    },
                    canAddTiles = TilePanelRequests.AVAILABLE)
                else AppRoot(
                    viewModel = viewModel,
                    settingsViewModel = settingsViewModel,
                    usbViewModel = usbViewModel,
                    dnsViewModel = dnsViewModel,
                    automationViewModel = automationViewModel,
                    shortcutViewModelFactory = (application as QuickTileSettingsApplication).container.shortcutViewModelFactory,
                    externalArea = externalArea,
                    onExternalAreaConsumed = { externalArea = null },
                    onOpenAdbSettings = openDeveloperOptions,
                    onRequestShizukuPermission = permissions::requestShizukuPermission,
                    onRestartOnboarding = onboardingViewModel::restart
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
