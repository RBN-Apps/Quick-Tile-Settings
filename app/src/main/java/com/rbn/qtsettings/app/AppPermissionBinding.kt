package com.rbn.qtsettings.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.WifiIdentityAccessState
import com.rbn.qtsettings.utils.PermissionUtils
import com.rbn.qtsettings.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

/** Activity-owned launchers and permission feedback, not a permission policy owner.
 * Construct before STARTED. The lifecycle releases listeners and collection jobs.
 */
class AppPermissionBinding(
    private val activity: ComponentActivity,
    private val viewModel: MainViewModel
) : DefaultLifecycleObserver {
    private val notificationPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Check if permission is permanently denied
            val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            )

            if (!shouldShowRationale) {
                // Permission is permanently denied, show settings dialog
                viewModel.onNotificationPermissionPermanentlyDenied()
            } else {
                // Permission denied but not permanently
                viewModel.onNotificationPermissionResult(false)
            }
        } else {
            viewModel.onNotificationPermissionResult(isGranted)
        }
    }

    private val wifiSsidPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val preciseLocationGranted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
        PermissionUtils.setWifiLocationPermissionBlocked(
            activity,
            !preciseLocationGranted && !ActivityCompat.shouldShowRequestPermissionRationale(
                activity, Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
        viewModel.onWifiSsidPermissionResult(preciseLocationGranted)
    }

    override fun onCreate(owner: LifecycleOwner) {
        Shizuku.addRequestPermissionResultListener(shizukuPermissionResultListener)
        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.permissionGrantStatus.collect { message ->
                        message?.let {
                            Toast.makeText(activity.applicationContext, it, Toast.LENGTH_LONG).show()
                            viewModel.clearPermissionGrantStatus()
                            viewModel.checkSystemStates(activity.applicationContext)
                        }
                    }
                }

                launch {
                    viewModel.requestNotificationPermission.collect { requestCounter ->
                        if (requestCounter > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                activity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED

                            if (!hasPermission) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.onNotificationPermissionResult(true)
                            }
                        }
                    }
                }

                launch {
                    viewModel.requestWifiSsidPermission.collect { requestCounter ->
                        if (requestCounter > 0) {
                            viewModel.clearWifiSsidPermissionRequest()
                            if (PermissionUtils.getWifiIdentityAccessState(activity) ==
                                WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_BLOCKED
                            ) {
                                PermissionUtils.openAppPermissionSettings(activity)
                            } else {
                                wifiSsidPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionResultListener)
    }

    private val shizukuPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == PermissionUtils.SHIZUKU_PERMISSION_REQUEST_CODE) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.shizuku_permission_granted_to_app),
                        Toast.LENGTH_SHORT
                    ).show()
                    viewModel.checkSystemStates(activity)
                } else {
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.shizuku_permission_denied_to_app),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    fun requestShizukuPermission() {
        if (PermissionUtils.isShizukuAvailableAndReady()) {
            PermissionUtils.requestShizukuPermission(activity)
        } else {
            Toast.makeText(
                activity,
                activity.getString(R.string.shizuku_not_available_or_not_ready),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
