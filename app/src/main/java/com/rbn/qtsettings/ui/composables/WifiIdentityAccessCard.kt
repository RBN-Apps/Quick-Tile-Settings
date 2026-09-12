package com.rbn.qtsettings.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.WifiIdentityAccessState

@Composable
internal fun WifiIdentityAccessCard(
    accessState: WifiIdentityAccessState,
    onRequestPermission: () -> Unit,
    onOpenLocationSettings: () -> Unit
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val explanation = buildAnnotatedString {
        append(
            stringResource(
                when (accessState) {
                    WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_REQUIRED ->
                        R.string.wifi_ssid_permission_description

                    WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_BLOCKED ->
                        R.string.wifi_permission_blocked_description

                    WifiIdentityAccessState.LOCATION_SERVICES_DISABLED ->
                        R.string.wifi_location_services_description

                    WifiIdentityAccessState.AVAILABLE ->
                        R.string.wifi_ssid_permission_description
                }
            )
        )
        append("\n\n")
        append(stringResource(R.string.wifi_ssid_manual_entry_notice))
        append("\n")
        withLink(
            LinkAnnotation.Url(
                stringResource(R.string.wifi_ssid_permission_docs_url),
                TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
            )
        ) { append(stringResource(R.string.wifi_ssid_permission_docs_link)) }
    }
    val title = when (accessState) {
        WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_REQUIRED ->
            R.string.wifi_ssid_permission_title

        WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_BLOCKED ->
            R.string.wifi_ssid_permission_title

        WifiIdentityAccessState.LOCATION_SERVICES_DISABLED ->
            R.string.wifi_location_services_title

        WifiIdentityAccessState.AVAILABLE -> R.string.wifi_ssid_permission_title
    }
    val actionLabel = when (accessState) {
        WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_REQUIRED ->
            R.string.wifi_ssid_permission_grant

        WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_BLOCKED ->
            R.string.wifi_permission_open_settings

        WifiIdentityAccessState.LOCATION_SERVICES_DISABLED ->
            R.string.wifi_location_services_open

        WifiIdentityAccessState.AVAILABLE -> R.string.wifi_ssid_permission_grant
    }
    val onAction = when (accessState) {
        WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_REQUIRED -> onRequestPermission
        WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_BLOCKED -> onRequestPermission
        WifiIdentityAccessState.LOCATION_SERVICES_DISABLED -> onOpenLocationSettings
        WifiIdentityAccessState.AVAILABLE -> onRequestPermission
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            Text(explanation, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            OutlinedButton(
                onClick = onAction,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .testTag("wifi_identity_access_action")
            ) {
                Text(stringResource(actionLabel))
            }
        }
    }
}
