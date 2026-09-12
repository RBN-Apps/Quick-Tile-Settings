package com.rbn.qtsettings.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.rbn.qtsettings.core.designsystem.theme.AppShapes
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing

/** Error roles are intentional for a missing capability; geometry/actions use the common tokens. */
@Composable
fun SettingsWarningCard(title: String, message: String, actionLabel: String, onAction: () -> Unit,
    modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth().padding(vertical = AppSpacing.contentGap), shape = AppShapes.groupItem(0, 1),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer)) {
        Column(Modifier.padding(AppSpacing.rowHorizontal), verticalArrangement = Arrangement.spacedBy(AppSpacing.contentGap)) {
            Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodyMedium)
            AppButton(actionLabel, onAction)
        }
    }
}
