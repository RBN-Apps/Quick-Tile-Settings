package com.rbn.qtsettings.core.designsystem

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.rbn.qtsettings.core.designsystem.theme.AppShapes
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing

enum class AppButtonStyle { Primary, Secondary, Text, Destructive }

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppButtonStyle = AppButtonStyle.Primary,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val touchModifier = modifier.heightIn(min = AppSpacing.minimumTouchTarget)
    val content: @Composable RowScope.() -> Unit = {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        }
        Text(text, modifier = Modifier.weight(1f, fill = false))
    }
    when (style) {
        AppButtonStyle.Primary -> Button(onClick, touchModifier, enabled,
            shape = AppShapes.button, content = content)
        AppButtonStyle.Secondary -> OutlinedButton(onClick, touchModifier, enabled,
            shape = AppShapes.button, content = content)
        AppButtonStyle.Text -> TextButton(onClick, touchModifier, enabled,
            shape = AppShapes.button, content = content)
        AppButtonStyle.Destructive -> Button(onClick, touchModifier, enabled,
            shape = AppShapes.button,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError), content = content)
    }
}
