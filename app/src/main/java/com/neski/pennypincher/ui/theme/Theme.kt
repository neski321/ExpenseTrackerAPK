package com.neski.pennypincher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Primary,
    background = LightBackground,
    surface = LightCard,
    onPrimary = LightCard,
    onBackground = LightForeground,
    onSurface = LightForeground
)

private val DarkColors = darkColorScheme(
    primary = Primary,
    background = DarkBackground,
    surface = DarkCard,
    onPrimary = DarkBackground,
    onBackground = DarkForeground,
    onSurface = DarkForeground
)

@Composable
fun PennyPincherTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (useDarkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
