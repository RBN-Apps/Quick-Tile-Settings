package com.rbn.qtsettings.feature.shortcuts.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Entry hint only; both sections always share the same global configuration. */
enum class ShortcutSection { Dns, Usb }

@Composable
internal fun ShortcutSectionHighlight(targeted: Boolean, content: @Composable (Modifier) -> Unit) {
    // Navigation's saveable entry retains consumption when switching areas or recreating the page.
    var consumed by rememberSaveable { mutableStateOf(false) }
    val alpha = remember { Animatable(if (targeted && !consumed) 1f else 0f) }
    val color = MaterialTheme.colorScheme.primary
    LaunchedEffect(Unit) {
        if (targeted && !consumed) {
            consumed = true
            alpha.animateTo(0f, tween(durationMillis = 1200, delayMillis = 400))
        }
    }
    content(Modifier.drawBehind {
        // Draw into the existing screen gutter instead of being covered by the opaque cards.
        // Drawing only (rather than animating padding) keeps text, rows and scroll position still.
        val outset = 6.dp.toPx()
        val topLeft = Offset(-outset, -outset)
        val outlineSize = Size(size.width + outset * 2, size.height + outset * 2)
        val radius = CornerRadius(22.dp.toPx())
        drawRoundRect(color.copy(alpha = alpha.value * 0.16f), topLeft, outlineSize, radius)
        drawRoundRect(color.copy(alpha = alpha.value * 0.65f), topLeft, outlineSize, radius,
            style = Stroke(width = 1.5.dp.toPx()))
    })
}
