package com.rbn.qtsettings.core.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler

/** Common external-link action; preserves the existing no-handler fallback. */
@Composable
fun AppLinkButton(text: String, url: String, modifier: Modifier = Modifier) {
    val handler = LocalUriHandler.current
    AppButton(text, onClick = { try { handler.openUri(url) } catch (_: Exception) {} },
        modifier = modifier, style = AppButtonStyle.Secondary, icon = Icons.AutoMirrored.Outlined.OpenInNew)
}
