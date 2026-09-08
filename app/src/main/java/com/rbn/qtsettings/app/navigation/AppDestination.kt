package com.rbn.qtsettings.app.navigation

import kotlinx.serialization.Serializable

/** Chrome labels/callbacks only; navigation is owned by AppNavigation's typed stacks. */
enum class AppDestination { TileSettings, ShortcutSettings, AppSettings }

@Serializable
enum class AppArea { Dns, Usb }
