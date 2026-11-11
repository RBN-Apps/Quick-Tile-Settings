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
import com.rbn.qtsettings.app.navigation.entryId
import kotlinx.serialization.PolymorphicSerializer

/** Executes inside the shrunk APK; serializers and route classes are not kept for this test. */
fun verifyMinifiedNavigationRestore() {
    val serializer = NavBackStackSerializer(PolymorphicSerializer(NavKey::class))
    val original = NavBackStack<NavKey>(
        AppRoute.Dns, AppRoute.DnsWifiTarget, AppRoute.DnsMobileTarget,
        AppRoute.WifiRules, AppRoute.WifiRule(null), AppRoute.WifiRule("saved-rule"),
        AppRoute.WifiRule(null, targetStep = true), AppRoute.WifiRule("saved-rule", targetStep = true),
        AppRoute.DnsProviders, AppRoute.DnsAutomation, AppRoute.DnsRevert,
        AppRoute.Usb, AppRoute.UsbTile, AppRoute.UsbActions, AppRoute.UsbRevert,
        AppRoute.Shortcuts(AppArea.Dns), AppRoute.Shortcuts(AppArea.Usb),
        AppRoute.Shortcuts(AppArea.Dns, true), AppRoute.Shortcuts(AppArea.Usb, true), AppRoute.ShortcutHelp,
        AppRoute.Settings(AppArea.Dns), AppRoute.Settings(AppArea.Usb), AppRoute.About, AppRoute.Permissions, AppRoute.AdbInstructions
    )
    // Global navigation is independently saveable, including its empty state and caller links.
    val paths = listOf(original, NavBackStack<NavKey>(),
        NavBackStack<NavKey>(AppRoute.About, AppRoute.Permissions, AppRoute.AdbInstructions))
    paths.forEach { path ->
        val saved = encodeToSavedState(serializer, path, AppNavigationSavedState)
        val parcel = Parcel.obtain()
        try {
            parcel.writeBundle(saved)
            parcel.setDataPosition(0)
            val restored = decodeFromSavedState(serializer, checkNotNull(parcel.readBundle()), AppNavigationSavedState)
            check(path.toList() == restored.toList())
            check(path.map { (it as AppRoute).entryId } == restored.map { (it as AppRoute).entryId })
        } finally {
            parcel.recycle()
        }
    }
}
