package com.rbn.qtsettings.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rbn.qtsettings.R
import com.rbn.qtsettings.app.navigation.AppDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    destination: AppDestination,
    onBack: () -> Unit,
    onOpenDestination: (AppDestination) -> Unit,
    onShowPermissionHelp: () -> Unit,
    onShowAbout: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenNavigation: (() -> Unit)? = null
) {
    val inSubScreen = destination != AppDestination.TileSettings
    var showOverflowMenu by remember { mutableStateOf(false) }
    if (inSubScreen) {
        TopAppBar(
            modifier = modifier,
            title = {
                Text(
                    when (destination) {
                        AppDestination.ShortcutSettings ->
                            stringResource(R.string.shortcut_settings_title)

                        AppDestination.AppSettings ->
                            stringResource(R.string.app_settings_title)

                        AppDestination.UsbTile -> stringResource(R.string.usb_menu_tile)
                        AppDestination.UsbActions -> stringResource(R.string.usb_menu_actions)
                        AppDestination.UsbRevert -> stringResource(R.string.usb_menu_revert)
                        AppDestination.TileSettings ->
                            stringResource(R.string.app_name)
                    }
                )
            },
            actions = {
                if (onOpenNavigation != null) NavigationMenuButton(onOpenNavigation)
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        onBack()
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.navigate_back_desc)
                    )
                }
            }
        )
    } else {
        TopAppBar(
            modifier = modifier,
            title = { Text(stringResource(R.string.app_name)) },
            navigationIcon = {
                if (onOpenNavigation != null) NavigationMenuButton(onOpenNavigation)
            },
            actions = {
                IconButton(
                    onClick = {
                        onOpenDestination(AppDestination.ShortcutSettings)
                    }
                ) {
                    Icon(
                        Icons.Filled.AppShortcut,
                        contentDescription = stringResource(R.string.shortcut_settings_open_desc)
                    )
                }
                IconButton(onClick = { showOverflowMenu = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.app_menu_desc)
                    )
                }
                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_settings)) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            showOverflowMenu = false
                            onOpenDestination(AppDestination.AppSettings)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_permission_help)) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.AdminPanelSettings,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            showOverflowMenu = false
                            onShowPermissionHelp()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_about)) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            showOverflowMenu = false
                            onShowAbout()
                        }
                    )
                }
            }
        )
    }
}

@Composable
private fun NavigationMenuButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Default.Menu, stringResource(R.string.navigation_open))
    }
}
