package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisColorScheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = Color(0xFF001F29),
    primaryContainer = Color(0xFF004D60),
    onPrimaryContainer = Color(0xFFA5EEFF),

    secondary = JarvisElectricBlue,
    onSecondary = Color(0xFF00224D),
    secondaryContainer = Color(0xFF0D3268),
    onSecondaryContainer = Color(0xFFC7DEFF),

    tertiary = JarvisAmber,
    onTertiary = Color(0xFF3B2900),
    tertiaryContainer = Color(0xFF6B4C00),
    onTertiaryContainer = Color(0xFFFFDF9E),

    error = JarvisCrimson,
    onError = Color.White,
    errorContainer = Color(0xFF93001D),
    onErrorContainer = Color(0xFFFFDAD7),

    background = JarvisBackground,
    onBackground = JarvisTextPrimary,
    surface = JarvisSurface,
    onSurface = JarvisTextPrimary,
    surfaceVariant = JarvisSurfaceVariant,
    onSurfaceVariant = JarvisTextSecondary,
    outline = JarvisCardBorder,
    outlineVariant = Color(0xFF16253D)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = Typography,
        content = content
    )
}
