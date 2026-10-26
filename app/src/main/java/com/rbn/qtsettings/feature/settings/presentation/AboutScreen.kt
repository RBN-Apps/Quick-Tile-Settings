package com.rbn.qtsettings.feature.settings.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.platform.app.AppInfo

@Composable
fun AboutScreen(
    info: AppInfo?,
    icon: ImageBitmap?,
    onPermissionHelp: () -> Unit,
    onShortcuts: () -> Unit,
    onSettings: () -> Unit,
    onLink: (String) -> Unit,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
    onReport: () -> Unit = {},
    onLicenses: () -> Unit = {}
) {
    LazyColumn(
        modifier.fillMaxSize().padding(horizontal = 16.dp),
        state = rememberLazyListState(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item("identity") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (icon != null) Image(icon, contentDescription = null, modifier = Modifier.size(72.dp))
                else Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(72.dp))
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() })
                Text(stringResource(R.string.about_version, info?.versionName
                    ?: stringResource(if (info == null) R.string.about_loading else R.string.about_unknown)),
                    color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.about_description), style = MaterialTheme.typography.bodyLarge)
            }
        }
        item("features") { AboutFeatures(onShortcuts, onSettings) }
        item("technical") { AboutTechnicalInfo(info, onCopy) }
        item("project") { AboutProjectLinks(onPermissionHelp, onLink, onReport, onLicenses) }
    }
}
