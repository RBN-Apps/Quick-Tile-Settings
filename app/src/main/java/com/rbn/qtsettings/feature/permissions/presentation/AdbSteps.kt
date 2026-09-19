package com.rbn.qtsettings.feature.permissions.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.SettingsGroup
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing

/**
 * One step of the ADB route. Whatever the step needs to be carried out — the download link, the
 * command to copy, a shortcut into the system settings — belongs to the step and is rendered
 * inside its card, so nothing that matters floats between the numbered cards.
 */
internal class AdbStep(
    val title: String,
    val summary: String,
    val content: (@Composable ColumnScope.() -> Unit)? = null
)

/**
 * The steps as a numbered group. The numbers are drawn as badges instead of being written into
 * the titles: the setup page and the first run show different parts of the same route, and a
 * badge renumbers itself where a hard-coded "1." in a translated string cannot.
 */
@Composable
internal fun AdbStepGroup(
    steps: List<AdbStep>,
    modifier: Modifier = Modifier,
    title: String? = null
) {
    SettingsGroup(steps.indices.toList(), itemKey = { it }, modifier = modifier, title = title) { index ->
        val step = steps[index]
        Column {
            Row(
                Modifier.fillMaxWidth()
                    .heightIn(min = AppSpacing.rowMinHeight)
                    .padding(horizontal = AppSpacing.rowHorizontal, vertical = AppSpacing.rowVertical),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.rowGap),
                // A step summary runs over several lines; the badge stays on the title's line.
                verticalAlignment = Alignment.Top
            ) {
                AdbStepNumber(index + 1)
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.textGap)) {
                    Text(step.title, style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text(step.summary, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            step.content?.let { content ->
                Column(
                    Modifier.padding(horizontal = AppSpacing.rowHorizontal)
                        .padding(bottom = AppSpacing.rowVertical),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.contentGap),
                    content = content
                )
            }
        }
    }
}

/** Same footprint as a row icon, so a numbered step and an icon row line their text up. */
@Composable
private fun AdbStepNumber(number: Int) {
    val description = stringResource(R.string.permission_adb_step_number, number)
    Box(
        Modifier.size(AppSpacing.iconSize)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            // Spoken as "Step 3", not as the bare digit the badge shows.
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Text("$number", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary)
    }
}
