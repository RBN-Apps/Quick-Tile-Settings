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
    @Serializable @SerialName("dns") data object Dns : AppRoute
    @Serializable @SerialName("usb") data object Usb : AppRoute
    @Serializable @SerialName("shortcuts") data class Shortcuts(val origin: AppArea) : AppRoute
    @Serializable @SerialName("settings") data class Settings(val origin: AppArea) : AppRoute
}

internal val AppNavigationSavedState = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
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
}
