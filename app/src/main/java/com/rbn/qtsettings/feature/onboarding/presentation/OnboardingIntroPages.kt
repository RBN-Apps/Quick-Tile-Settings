package com.rbn.qtsettings.feature.onboarding.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Rule
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.AppShortcut
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material3.Card
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.AppButton
import com.rbn.qtsettings.core.designsystem.SettingsGroup
import com.rbn.qtsettings.core.designsystem.SettingsRowContent
import com.rbn.qtsettings.core.designsystem.theme.AppShapes
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing
import com.rbn.qtsettings.feature.onboarding.domain.OnboardingTile

/** The start button sits with the artwork rather than at the screen edge, and points the way. */
@Composable
internal fun OnboardingWelcomePage(
    motion: Boolean,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionGap)) {
        OnboardingHero(icon = null, title = stringResource(R.string.app_name),
            subtitle = stringResource(R.string.onboarding_welcome_subtitle),
            // The launcher glyph carries its own padding, so it is drawn at container size.
            painter = painterResource(R.drawable.ic_launcher_foreground), glyphSize = 96.dp)
        val nudge by rememberNudge(motion)
        // Its own tag: the pager keeps neighbouring pages composed, so the bar's primary button
        // and this one must stay distinguishable.
        AppButton(stringResource(R.string.onboarding_start), onStart,
            Modifier.fillMaxWidth().testTag(ONBOARDING_START_TAG)) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null,
                Modifier.size(ButtonDefaults.IconSize)
                    .graphicsLayer { translationX = nudge * 6.dp.toPx() })
        }
    }
}

@Composable
internal fun OnboardingTilesPage(modifier: Modifier = Modifier) {
    val highlights = listOf(
        Triple(Icons.Outlined.Dns, R.string.tab_title_dns, R.string.onboarding_highlight_dns),
        Triple(Icons.Outlined.Usb, R.string.tab_title_usb, R.string.onboarding_highlight_usb),
        Triple(Icons.AutoMirrored.Outlined.Rule, R.string.onboarding_highlight_revert_title, R.string.onboarding_highlight_revert),
        Triple(Icons.Outlined.AppShortcut, R.string.onboarding_highlight_shortcuts_title, R.string.onboarding_highlight_shortcuts)
    )
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionGap)) {
        OnboardingHero(Icons.Outlined.GridView, stringResource(R.string.onboarding_tiles_title),
            stringResource(R.string.onboarding_tiles_subtitle), compact = true)
        Card(Modifier.fillMaxWidth(), shape = AppShapes.groupItem(0, 1)) {
            Column(Modifier.padding(vertical = AppSpacing.contentGap)) {
                highlights.forEachIndexed { index, (icon, title, summary) ->
                    // Each line arrives a beat after the one above it, so the list reads downwards.
                    StaggeredEntrance(delayMillis = index * 70) {
                        OnboardingHighlight(icon, stringResource(title), stringResource(summary))
                    }
                }
            }
        }
    }
}

@Composable
internal fun OnboardingDonePage(
    state: OnboardingUiState,
    onAddTile: (OnboardingTile) -> Unit,
    modifier: Modifier = Modifier
) {
    val granted = state.granted
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionGap)) {
        OnboardingHero(
            if (granted) Icons.Outlined.CheckCircle else Icons.Outlined.Schedule,
            stringResource(if (granted) R.string.onboarding_done_title else R.string.onboarding_pending_title),
            stringResource(if (granted) R.string.onboarding_done_subtitle else R.string.onboarding_pending_subtitle)
        )
        if (state.canAddTiles) SettingsGroup(OnboardingTile.entries, itemKey = { it },
            title = stringResource(R.string.onboarding_add_tile_title)) { tile ->
            TileRow(tile, added = tile in state.tilesInPanel, onAdd = { onAddTile(tile) })
        } else Card(Modifier.fillMaxWidth(), shape = AppShapes.groupItem(0, 1)) {
            Column(Modifier.padding(AppSpacing.rowHorizontal),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.contentGap)) {
                Text(stringResource(R.string.onboarding_add_tile_title),
                    style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.onboarding_add_tile_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (!granted) Text(stringResource(R.string.onboarding_pending_hint),
            Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Android owns the dialog and the placement, so the row only reports what came back. */
@Composable
private fun TileRow(tile: OnboardingTile, added: Boolean, onAdd: () -> Unit) {
    SettingsRowContent(
        title = stringResource(if (tile == OnboardingTile.Dns) R.string.tab_title_dns else R.string.tab_title_usb),
        summary = stringResource(if (added) R.string.onboarding_tile_added else R.string.onboarding_tile_add_summary),
        icon = if (tile == OnboardingTile.Dns) Icons.Outlined.Dns else Icons.Outlined.Usb,
        enabled = !added,
        modifier = Modifier.clickable(enabled = !added, role = Role.Button, onClick = onAdd)
    ) {
        OnboardingCrossfade(added) { done ->
            Icon(if (done) Icons.Outlined.CheckCircle else Icons.Outlined.AddCircleOutline, null,
                tint = if (done) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StaggeredEntrance(delayMillis: Int, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    AnimatedVisibility(shown,
        enter = fadeIn(tween(260, delayMillis)) + slideInVertically(tween(300, delayMillis)) { it / 3 }
    ) { content() }
}
