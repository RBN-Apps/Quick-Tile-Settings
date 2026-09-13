package com.rbn.qtsettings

import android.os.Parcel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import com.rbn.qtsettings.app.navigation.AppArea
import com.rbn.qtsettings.app.navigation.AppNavigationSavedState
import com.rbn.qtsettings.app.navigation.AppRoute
import kotlinx.serialization.PolymorphicSerializer

/** Executes inside the shrunk APK; serializers and route classes are not kept for this test. */
fun verifyMinifiedNavigationRestore() {
    val serializer = NavBackStackSerializer(PolymorphicSerializer(NavKey::class))
    val original = NavBackStack<NavKey>(
        AppRoute.Dns, AppRoute.DnsWifiTarget, AppRoute.DnsMobileTarget,
        AppRoute.WifiRules, AppRoute.WifiRule(null), AppRoute.WifiRule("saved-rule"),
        AppRoute.DnsProviders, AppRoute.DnsAutomation, AppRoute.DnsRevert,
        AppRoute.Usb, AppRoute.UsbTile, AppRoute.UsbActions, AppRoute.UsbRevert,
        AppRoute.Shortcuts(AppArea.Dns), AppRoute.Shortcuts(AppArea.Usb),
        AppRoute.Shortcuts(AppArea.Dns, true), AppRoute.Shortcuts(AppArea.Usb, true),
        AppRoute.Settings(AppArea.Dns), AppRoute.Settings(AppArea.Usb), AppRoute.About, AppRoute.Permissions, AppRoute.AdbInstructions
    )
    val saved = encodeToSavedState(serializer, original, AppNavigationSavedState)
    val parcel = Parcel.obtain()
    try {
        parcel.writeBundle(saved)
        parcel.setDataPosition(0)
        val restored = decodeFromSavedState(serializer, checkNotNull(parcel.readBundle()), AppNavigationSavedState)
        check(original.toList() == restored.toList())
    } finally {
        parcel.recycle()
    }
}
