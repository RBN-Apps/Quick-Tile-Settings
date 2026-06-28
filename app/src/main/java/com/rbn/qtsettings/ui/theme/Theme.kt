package com.rbn.qtsettings.ui.theme

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.util.TypedValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun QuickTileSettingsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val dynamicScheme =
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            context.systemAccentColorOrNull(darkTheme)?.let { systemAccentColor ->
                dynamicScheme.copy(
                    primary = systemAccentColor,
                    onPrimary = systemAccentColor.contrastColor(),
                    secondary = systemAccentColor,
                    onSecondary = systemAccentColor.contrastColor(),
                    tertiary = systemAccentColor,
                    onTertiary = systemAccentColor.contrastColor()
                )
            } ?: dynamicScheme
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

private fun Context.systemAccentColorOrNull(darkTheme: Boolean): Color? =
    systemDynamicAccentColorOrNull(darkTheme) ?: themeAccentColorOrNull()

private fun Context.systemDynamicAccentColorOrNull(darkTheme: Boolean): Color? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null

    val resourceId = if (darkTheme) {
        android.R.color.system_accent1_200
    } else {
        android.R.color.system_accent1_600
    }
    return try {
        Color(getColor(resourceId))
    } catch (_: Resources.NotFoundException) {
        null
    }
}

private fun Context.themeAccentColorOrNull(): Color? {
    val typedValue = TypedValue()
    if (!theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true)) return null

    val colorInt = when {
        typedValue.resourceId != 0 -> try {
            getColor(typedValue.resourceId)
        } catch (_: Resources.NotFoundException) {
            return null
        }

        typedValue.type in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT -> {
            typedValue.data
        }

        else -> return null
    }

    return Color(colorInt)
}

private fun Color.contrastColor(): Color =
    if (luminance() > 0.5f) Color.Black else Color.White
