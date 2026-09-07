package com.rbn.qtsettings.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object AppShapes {
    // Material defaults stay explicit; grouped settings use the existing Shortcut geometry.
    val material = Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(28.dp)
    )
    val button = CircleShape
    val textField = RoundedCornerShape(4.dp)
    private val groupOuter = 20.dp
    private val groupInner = 4.dp

    internal fun groupItem(index: Int, count: Int) = RoundedCornerShape(
        topStart = if (index == 0) groupOuter else groupInner,
        topEnd = if (index == 0) groupOuter else groupInner,
        bottomStart = if (index == count - 1) groupOuter else groupInner,
        bottomEnd = if (index == count - 1) groupOuter else groupInner
    )
}
