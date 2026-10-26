package com.rbn.qtsettings.feature.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.AppDialog
import com.rbn.qtsettings.core.designsystem.SettingsRowContent
import com.rbn.qtsettings.core.designsystem.theme.AppShapes
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme

/** Metadata and full license texts are generated per build variant and bundled for offline use. */
@Composable
fun LicensesScreen(modifier: Modifier = Modifier) {
    // Explicit resource reference also keeps the data reachable when release resources are shrunk.
    val libs by produceLibraries(R.raw.aboutlibraries)
    val libraries = libs?.libraries
    // The id, not the entity, is saved so the open license survives recreation.
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    if (libraries == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    LazyColumn(
        modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.groupGap)
    ) {
        item("intro") {
            Text(stringResource(R.string.licenses_intro), style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = AppSpacing.sectionGap))
        }
        itemsIndexed(libraries, key = { _, library -> library.uniqueId }) { index, library ->
            Card(Modifier.fillMaxWidth(), shape = AppShapes.groupItem(index, libraries.size)) {
                LibraryRow(library) { selectedId = library.uniqueId }
            }
        }
    }
    libraries.firstOrNull { it.uniqueId == selectedId }?.let { library ->
        LicenseDialog(library) { selectedId = null }
    }
}

@Composable
private fun LibraryRow(library: Library, onClick: () -> Unit) {
    val author = library.developers.mapNotNull { it.name }.joinToString().ifBlank { library.organization?.name }
    SettingsRowContent(
        title = library.name,
        summary = listOfNotNull(library.artifactVersion, author).joinToString(" · ").ifBlank { null },
        status = library.licenses.joinToString { it.name }.ifBlank { null },
        icon = null,
        enabled = true,
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick)
    ) {}
}

@Composable
private fun LicenseDialog(library: Library, onDismiss: () -> Unit) {
    AppDialog(
        title = library.name,
        onDismissRequest = onDismiss,
        confirmLabel = stringResource(R.string.dialog_close),
        onConfirm = onDismiss
    ) {
        val licenses = library.licenses.toList()
        licenses.forEach { license ->
            // A single license is already named in the list row; only dual licensing needs labels.
            if (licenses.size > 1) {
                Text(license.name, style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.semantics { heading() })
            }
            Text(license.licenseContent.orEmpty().trim(), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(locale = "de")
@Composable
private fun LicensesPreview() {
    QuickTileSettingsTheme { LicensesScreen() }
}
