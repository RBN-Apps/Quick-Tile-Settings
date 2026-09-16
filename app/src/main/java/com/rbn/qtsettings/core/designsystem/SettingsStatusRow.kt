package com.rbn.qtsettings.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription

/** Read-only sibling of the navigation rows: reports live system state, offers no control. */
@Composable
fun SettingsStatusRow(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    summary: String? = null,
    status: String? = null,
    stateLabel: String? = null
) = SettingsRowContent(
    title = title, summary = summary, icon = icon, enabled = true, status = status,
    modifier = modifier.statusSemantics(stateLabel, title, status, summary), trailing = {}
)

@Composable
fun SettingsStatusRow(
    title: String,
    icon: Painter,
    modifier: Modifier = Modifier,
    summary: String? = null,
    status: String? = null,
    stateLabel: String? = null
) = SettingsRowContent(
    title = title, summary = summary, icon = null, iconPainter = icon, enabled = true, status = status,
    modifier = modifier.statusSemantics(stateLabel, title, status, summary), trailing = {}
)

/** Without a visible section heading the row needs to name what it is reporting. */
private fun Modifier.statusSemantics(stateLabel: String?, vararg lines: String?): Modifier {
    if (stateLabel == null) return this
    val spoken = "$stateLabel: " + lines.filterNotNull().joinToString(", ")
    return clearAndSetSemantics { contentDescription = spoken }
}
