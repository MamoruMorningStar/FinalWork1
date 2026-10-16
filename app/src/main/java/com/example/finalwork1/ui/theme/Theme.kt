package com.example.finalwork1.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable

private val ColorOutlineDark = Color(0xFF50645F)
private val ColorContainerDark = Color(0xFF344629)
private val ColorSecondaryContainerDark = Color(0xFF204B49)

private val DarkColorScheme = darkColorScheme(
    primary = PortalGreen,
    onPrimary = Space,
    secondary = PortalTurquoise,
    onSecondary = Space,
    tertiary = PortalPurple,
    background = Space,
    onBackground = SpaceText,
    surface = SpaceSurface,
    onSurface = SpaceText,
    surfaceVariant = SpaceRaised,
    onSurfaceVariant = SpaceText,
    outline = MutedText,
    outlineVariant = ColorOutlineDark,
    primaryContainer = ColorContainerDark,
    onPrimaryContainer = SpaceText,
    secondaryContainer = ColorSecondaryContainerDark,
    onSecondaryContainer = SpaceText,
)

private val LightColorScheme = lightColorScheme(
    primary = PortalGreenDark,
    onPrimary = LightSurface,
    secondary = Color(0xFF168D83),
    onSecondary = LightSurface,
    tertiary = Color(0xFF7358B8),
    background = LightBackground,
    onBackground = Ink,
    surface = LightSurface,
    onSurface = Ink,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Ink,
    outline = Color(0xFF6E857B),
    outlineVariant = Color(0xFFC8DCCB),
    primaryContainer = Color(0xFFDDF7A8),
    onPrimaryContainer = Ink,
    secondaryContainer = Color(0xFFC9F3EC),
    onSecondaryContainer = Ink,
)

@Composable
fun FINALWORK1Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Keep the app's own palette instead of replacing it with Android's dynamic colors.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
