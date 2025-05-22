package com.rbn.qtsettings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.ui.composables.MainScreen
import com.rbn.qtsettings.ui.theme.QuickTileSettingsTheme
import com.rbn.qtsettings.utils.PermissionUtils
import com.rbn.qtsettings.viewmodel.MainViewModel
import com.rbn.qtsettings.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        ViewModelFactory(PreferencesManager.getInstance(this.applicationContext))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
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

        Shizuku.addRequestPermissionResultListener(shizukuPermissionResultListener)
        viewModel.checkSystemStates(this)

        setContent {
            QuickTileSettingsTheme {
                MainScreen(
                    viewModel = viewModel,
                    onOpenAdbSettings = { openUsbDebuggingSettings(this) },
                    onRequestShizukuPermission = { requestShizukuPermissionForApp() }
                )
            }
        }

        lifecycleScope.launch {
            viewModel.permissionGrantStatus.collect { message ->
                message?.let {
                    Toast.makeText(applicationContext, it, Toast.LENGTH_LONG).show()
                    viewModel.clearPermissionGrantStatus()
                    viewModel.checkSystemStates(applicationContext)
                }
            }
        }
    }

    private fun openUsbDebuggingSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            context.startActivity(intent)
        } catch (e: Exception) {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkSystemStates(this)
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionResultListener)
        super.onDestroy()
    }

    private val shizukuPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == PermissionUtils.SHIZUKU_PERMISSION_REQUEST_CODE) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        this,
                        getString(R.string.shizuku_permission_granted_to_app),
                        Toast.LENGTH_SHORT
                    ).show()
                    viewModel.checkSystemStates(this)
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.shizuku_permission_denied_to_app),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    private fun requestShizukuPermissionForApp() {
        if (PermissionUtils.isShizukuAvailableAndReady()) {
            PermissionUtils.requestShizukuPermission(this)
        } else {
            Toast.makeText(
                this,
                getString(R.string.shizuku_not_available_or_not_ready),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}