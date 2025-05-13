package com.rbn.qtsettings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.ui.composables.MainScreen
import com.rbn.qtsettings.ui.theme.QuickTileSettingsTheme
import com.rbn.qtsettings.viewmodel.MainViewModel
import com.rbn.qtsettings.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        ViewModelFactory(PreferencesManager.getInstance(this.applicationContext))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val tileSource = intent.getStringExtra("android.intent.extra.COMPONENT_NAME")
        if (tileSource != null) {
            if (tileSource.contains("PrivateDnsTileService")) {
                viewModel.setInitialTab(0)
            } else if (tileSource.contains("UsbDebuggingTileService")) {
                viewModel.setInitialTab(1)
            }
        }


        setContent {
            QuickTileSettingsTheme {
                MainScreen(viewModel = viewModel, onOpenAdbSettings = {
                    openAdbWirelessDebuggingSettings(this)
                })
            }
        }
    }

    private fun openAdbWirelessDebuggingSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            context.startActivity(intent)
        } catch (e: Exception) {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
        }
    }
}