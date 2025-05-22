package com.rbn.qtsettings.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.ui.theme.QuickTileSettingsTheme

@Stable
private enum class PermissionMethodType {
    ADB, SHIZUKU, ROOT
}

@Composable
fun PermissionGrantDialog(
    onDismissRequest: () -> Unit,
    onOpenDeveloperOptions: () -> Unit,
    onCopyToClipboard: (String) -> Unit,
    onGrantWithShizuku: () -> Unit,
    onGrantWithRoot: () -> Unit,
    onRequestShizukuPermission: () -> Unit,
    isShizukuAvailable: Boolean,
    appHasShizukuPermission: Boolean,
    isDeviceRooted: Boolean
) {
    var showAdbInstructionsDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val shizukuPlayStoreUrl = stringResource(id = R.string.shizuku_play_store_url)
    val shizukuGitHubUrl = stringResource(id = R.string.shizuku_github_releases_url)

    val permissionMethods = remember(isDeviceRooted, isShizukuAvailable, appHasShizukuPermission) {
        val methods = mutableListOf<PermissionMethodType>()
        if (isDeviceRooted && isShizukuAvailable) {
            methods.add(PermissionMethodType.ROOT)
            methods.add(PermissionMethodType.SHIZUKU)
            methods.add(PermissionMethodType.ADB)
        } else if (isDeviceRooted) {
            methods.add(PermissionMethodType.ROOT)
            methods.add(PermissionMethodType.ADB)
            methods.add(PermissionMethodType.SHIZUKU)
        } else if (isShizukuAvailable) {
            methods.add(PermissionMethodType.SHIZUKU)
            methods.add(PermissionMethodType.ADB)
            methods.add(PermissionMethodType.ROOT)
        } else {
            methods.add(PermissionMethodType.ADB)
            methods.add(PermissionMethodType.SHIZUKU)
            methods.add(PermissionMethodType.ROOT)
        }
        methods.distinct()
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(R.string.permission_grant_dialog_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(R.string.permission_grant_dialog_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                permissionMethods.forEachIndexed { index, methodType ->
                    when (methodType) {
                        PermissionMethodType.ADB -> {
                            PermissionMethodCard(
                                title = stringResource(
                                    R.string.permission_method_adb_title,
                                    index + 1,
                                    if (index == 0) stringResource(R.string.recommended_for_you) else ""
                                ),
                                description = stringResource(R.string.permission_method_adb_desc)
                            ) {
                                ElevatedButton(
                                    onClick = { showAdbInstructionsDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.button_show_adb_instructions))
                                }
                            }
                        }

                        PermissionMethodType.SHIZUKU -> {
                            PermissionMethodCard(
                                title = stringResource(
                                    R.string.permission_method_shizuku_title,
                                    index + 1,
                                    if (index == 0) stringResource(R.string.recommended_for_you) else ""
                                ),
                                description = if (!isShizukuAvailable) {
                                    stringResource(R.string.shizuku_not_available_detailed)
                                } else if (!appHasShizukuPermission) {
                                    stringResource(R.string.shizuku_permission_not_granted_to_app_detailed)
                                } else {
                                    stringResource(R.string.shizuku_ready_to_grant_desc)
                                }
                            ) {
                                if (!isShizukuAvailable) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                try {
                                                    uriHandler.openUri(shizukuPlayStoreUrl)
                                                } catch (_: Exception) {
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(stringResource(R.string.button_open_shizuku_play_store_short))
                                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                            Icon(
                                                Icons.AutoMirrored.Filled.OpenInNew,
                                                contentDescription = null,
                                                modifier = Modifier.size(ButtonDefaults.IconSize)
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                try {
                                                    uriHandler.openUri(shizukuGitHubUrl)
                                                } catch (_: Exception) {
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(stringResource(R.string.button_open_shizuku_github_short))
                                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                            Icon(
                                                Icons.AutoMirrored.Filled.OpenInNew,
                                                contentDescription = null,
                                                modifier = Modifier.size(ButtonDefaults.IconSize)
                                            )
                                        }
                                    }
                                } else if (!appHasShizukuPermission) {
                                    ElevatedButton(
                                        onClick = onRequestShizukuPermission,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(stringResource(R.string.button_request_shizuku_permission))
                                    }
                                } else {
                                    ElevatedButton(
                                        onClick = onGrantWithShizuku,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(stringResource(R.string.button_grant_with_shizuku))
                                    }
                                }
                            }
                        }

                        PermissionMethodType.ROOT -> {
                            PermissionMethodCard(
                                title = stringResource(
                                    R.string.permission_method_root_title,
                                    index + 1,
                                    if (index == 0) stringResource(R.string.recommended_for_you) else ""
                                ),
                                description = if (!isDeviceRooted) {
                                    stringResource(R.string.device_not_rooted_detailed)
                                } else {
                                    stringResource(R.string.root_ready_to_grant_desc)
                                }
                            ) {
                                if (isDeviceRooted) {
                                    ElevatedButton(
                                        onClick = onGrantWithRoot,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(stringResource(R.string.button_grant_with_root))
                                    }
                                }
                            }
                        }
                    }
                    if (index < permissionMethods.size - 1) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onOpenDeveloperOptions,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.DeveloperMode,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.button_open_developer_options))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.dialog_close))
            }
        }
    )

    if (showAdbInstructionsDialog) {
        AdbInstructionDialog(
            onDismissRequest = { showAdbInstructionsDialog = false },
            onCopyToClipboard = onCopyToClipboard
        )
    }
}

@Composable
fun PermissionMethodCard(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 800)
@Composable
fun PermissionGrantDialogPreview_RootShizuku() {
    QuickTileSettingsTheme {
        PermissionGrantDialog(
            onDismissRequest = {},
            onOpenDeveloperOptions = {},
            onCopyToClipboard = {},
            onGrantWithShizuku = {},
            onGrantWithRoot = {},
            onRequestShizukuPermission = {},
            isShizukuAvailable = true,
            appHasShizukuPermission = true,
            isDeviceRooted = true
        )
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 800)
@Composable
fun PermissionGrantDialogPreview_ShizukuNeedsPerm() {
    QuickTileSettingsTheme {
        PermissionGrantDialog(
            onDismissRequest = {},
            onOpenDeveloperOptions = {},
            onCopyToClipboard = {},
            onGrantWithShizuku = {},
            onGrantWithRoot = {},
            onRequestShizukuPermission = {},
            isShizukuAvailable = true,
            appHasShizukuPermission = false,
            isDeviceRooted = false
        )
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 800)
@Composable
fun PermissionGrantDialogPreview_AdbOnly() {
    QuickTileSettingsTheme {
        PermissionGrantDialog(
            onDismissRequest = {},
            onOpenDeveloperOptions = {},
            onCopyToClipboard = {},
            onGrantWithShizuku = {},
            onGrantWithRoot = {},
            onRequestShizukuPermission = {},
            isShizukuAvailable = false,
            appHasShizukuPermission = false,
            isDeviceRooted = false
        )
    }
}