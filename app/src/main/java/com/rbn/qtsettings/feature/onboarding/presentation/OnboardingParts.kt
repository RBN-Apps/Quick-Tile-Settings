package com.rbn.qtsettings.feature.onboarding.presentation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing

/** Set once per page from the available height; the heroes read it instead of measuring again. */
internal val LocalOnboardingShortPage = staticCompositionLocalOf { false }

/** The dots read as one label, so TalkBack announces the position instead of five shapes. */
@Composable
internal fun OnboardingProgress(position: Int, count: Int, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.onboarding_progress, position, count)
    Row(modifier.clearAndSetSemantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.textGap),
        verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { index ->
            val active = index == position - 1
            val width by animateDpAsState(if (active) 22.dp else 8.dp, label = "onboarding_step_width")
            Box(Modifier.height(8.dp).width(width).clip(CircleShape).background(
                if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)))
        }
    }
}

@Composable
internal fun OnboardingHero(
    icon: ImageVector?,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    painter: Painter? = null,
    glyphSize: Dp? = null
) {
    val box = if (compact) 64.dp else 96.dp
    val glyph = glyphSize ?: if (compact) 32.dp else 48.dp
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.rowGap)) {
        if (!LocalOnboardingShortPage.current) {
            Box(Modifier.size(box).clip(RoundedCornerShape(percent = 30))
                .background(MaterialTheme.colorScheme.primaryContainer), Alignment.Center) {
                val tint = MaterialTheme.colorScheme.onPrimaryContainer
                if (icon != null) Icon(icon, null, Modifier.size(glyph), tint = tint)
                else if (painter != null) Icon(painter, null, Modifier.size(glyph), tint = tint)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.contentGap)) {
            Text(title, Modifier.semantics { heading() }, textAlign = TextAlign.Center,
                style = if (compact) MaterialTheme.typography.headlineSmall
                    else MaterialTheme.typography.headlineMedium)
            if (subtitle != null) Text(subtitle, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** One capability of the app: an icon, a name and a single line about it. */
@Composable
internal fun OnboardingHighlight(icon: ImageVector, title: String, summary: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(AppSpacing.rowHorizontal),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.rowGap)) {
        Box(Modifier.size(40.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer), Alignment.Center) {
            Icon(icon, null, Modifier.size(AppSpacing.iconSize),
                tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.textGap)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(summary, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
