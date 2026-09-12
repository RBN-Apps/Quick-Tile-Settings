package com.rbn.qtsettings.app.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/** Stable wire names and explicit serializers keep saved routes independent of R8 class names. */
@Serializable
sealed interface AppRoute : NavKey {
    @Serializable @SerialName("dns_revert") data object DnsRevert : AppRoute
    @Serializable @SerialName("dns_automation") data object DnsAutomation : AppRoute
    @Serializable @SerialName("dns_providers") data object DnsProviders : AppRoute
    @Serializable @SerialName("usb_tile") data object UsbTile : AppRoute
    @Serializable @SerialName("usb_actions") data object UsbActions : AppRoute
    @Serializable @SerialName("usb_revert") data object UsbRevert : AppRoute
    @Serializable @SerialName("dns") data object Dns : AppRoute
    @Serializable @SerialName("usb") data object Usb : AppRoute
    @Serializable @SerialName("shortcuts") data class Shortcuts(val origin: AppArea) : AppRoute
    @Serializable @SerialName("settings") data class Settings(val origin: AppArea) : AppRoute
}

internal val AppNavigationSavedState = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(AppRoute.DnsRevert.serializer())
            subclass(AppRoute.DnsAutomation.serializer())
            subclass(AppRoute.DnsProviders.serializer())
            subclass(AppRoute.UsbTile.serializer())
            subclass(AppRoute.UsbActions.serializer())
            subclass(AppRoute.UsbRevert.serializer())
            subclass(AppRoute.Dns.serializer())
            subclass(AppRoute.Usb.serializer())
            subclass(AppRoute.Shortcuts.serializer())
            subclass(AppRoute.Settings.serializer())
        }
    }
}

internal val AppRoute.destination: AppDestination get() = when (this) {
    AppRoute.Dns, AppRoute.Usb -> AppDestination.TileSettings
    is AppRoute.Shortcuts -> AppDestination.ShortcutSettings
    is AppRoute.Settings -> AppDestination.AppSettings
    AppRoute.DnsProviders -> AppDestination.DnsProviders
    AppRoute.DnsAutomation -> AppDestination.DnsAutomation
    AppRoute.DnsRevert -> AppDestination.DnsRevert
    AppRoute.UsbTile -> AppDestination.UsbTile
    AppRoute.UsbActions -> AppDestination.UsbActions
    AppRoute.UsbRevert -> AppDestination.UsbRevert
}
