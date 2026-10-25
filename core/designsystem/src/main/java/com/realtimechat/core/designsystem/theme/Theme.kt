package com.realtimechat.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightGreenColorScheme = lightColorScheme(
    primary = LightGreenPrimary,
    primaryContainer = LightGreenPrimaryContainer,
    onPrimaryContainer = LightGreenOnPrimaryContainer,
    secondary = LightGreenSecondary,
    secondaryContainer = LightGreenSecondaryContainer,
    surface = LightGreenSurface,
    surfaceVariant = LightGreenSurfaceVariant
)

private val DarkGreenColorScheme = darkColorScheme(
    primary = LightGreenOnlineDot,
    primaryContainer = LightGreenOnPrimaryContainer,
    onPrimaryContainer = LightGreenPrimaryContainer,
    secondary = LightGreenSecondary,
    secondaryContainer = LightGreenPrimary
)

@Composable
fun RealtimeChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkGreenColorScheme else LightGreenColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
