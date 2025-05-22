package com.rbn.qtsettings.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.ui.theme.QuickTileSettingsTheme

@Composable
fun HelpDialog(
    onDismissRequest: () -> Unit,
    onOpenAdbSettings: () -> Unit,
    onCopyToClipboard: (String) -> Unit,
    onGrantWithShizuku: () -> Unit,
    onGrantWithRoot: () -> Unit,
    onRequestShizukuPermission: () -> Unit,
    isShizukuAvailable: Boolean,
    appHasShizukuPermission: Boolean,
    isDeviceRooted: Boolean
) {
    val context = LocalContext.current
    val adbGrantCommand =
        "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
    val adbInstallCommand = stringResource(R.string.adb_command_install_g)
    val githubReleasesUrl = stringResource(id = R.string.github_releases_url)
    val shizukuPlayStoreUrl = stringResource(id = R.string.shizuku_play_store_url)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(R.string.help_dialog_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(R.string.help_dialog_intro),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Method 1: Grant
                Text(
                    text = stringResource(R.string.help_dialog_method_1_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.help_dialog_how_to_grant_via_adb),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = adbGrantCommand,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
                ElevatedButton(
                    onClick = { onCopyToClipboard(adbGrantCommand) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.button_copy_grant_command))
                }
                Spacer(modifier = Modifier.height(24.dp))

                // Method 2: Install with -g
                Text(
                    text = stringResource(R.string.help_dialog_method_2_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.help_dialog_how_to_install_via_adb),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = adbInstallCommand,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
                ElevatedButton(
                    onClick = { onCopyToClipboard(adbInstallCommand) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.button_copy_install_command))
                }

                Spacer(modifier = Modifier.height(24.dp))

                val apkNoteFullText = stringResource(R.string.help_dialog_apk_path_note)
                val linkText = stringResource(R.string.github_releases_link_text)

                val annotatedString = buildAnnotatedString {
                    append(apkNoteFullText)
                    append("\n")
                    withLink(
                        LinkAnnotation.Url(
                            url = githubReleasesUrl,
                            TextLinkStyles(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                        )
                    ) {
                        append(linkText)
                    }
                }

                Text(
                    text = annotatedString,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(top = 6.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Method 3: Shizuku
                Text(
                    text = stringResource(R.string.help_dialog_method_3_shizuku_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                val shizukuDesc = stringResource(R.string.help_dialog_shizuku_desc)
                val shizukuLinkText = stringResource(R.string.shizuku_link_text)
                val shizukuAnnotatedString = buildAnnotatedString {
                    append(shizukuDesc)
                    append(" ")
                    withLink(
                        LinkAnnotation.Url(
                            url = shizukuPlayStoreUrl,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                        )
                    ) {
                        append(shizukuLinkText)
                    }
                    append(".")
                }
                Text(
                    text = shizukuAnnotatedString,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (!isShizukuAvailable) {
                    Text(
                        text = stringResource(R.string.shizuku_not_available_detailed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (!appHasShizukuPermission) {
                    Text(
                        text = stringResource(R.string.shizuku_permission_not_granted_to_app_detailed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Method 4: Root
                Text(
                    text = stringResource(R.string.help_dialog_method_4_root_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.help_dialog_root_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (!isDeviceRooted) {
                    Text(
                        text = stringResource(R.string.device_not_rooted_detailed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    ElevatedButton(onClick = onGrantWithRoot, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.button_grant_with_root))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.help_dialog_after_grant),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.dialog_close))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onOpenAdbSettings()
                onDismissRequest()
            }) {
                Text(stringResource(R.string.help_button_open_dev_settings_adb))
            }
        }
    )
}

@Preview(showBackground = true, widthDp = 380)
@Composable
fun HelpDialogPreviewWithLink() {
    QuickTileSettingsTheme {
        HelpDialog(
            onDismissRequest = {},
            onOpenAdbSettings = {},
            onCopyToClipboard = {},
            onGrantWithShizuku = {},
            onGrantWithRoot = {},
            onRequestShizukuPermission = {},
            isShizukuAvailable = true,
            appHasShizukuPermission = false,
            isDeviceRooted = true
        )
    }
}