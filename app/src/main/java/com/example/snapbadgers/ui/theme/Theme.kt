package com.example.snapbadgers.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    secondary = Zinc400,
    onSecondary = Color.White,
    tertiary = Zinc700,
    background = Color.Black,
    surface = Zinc950,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Zinc900,
    onSurfaceVariant = Zinc400,
    outline = Zinc700
)

private val LightColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    secondary = Zinc700,
    onSecondary = Color.White,
    tertiary = Zinc500,
    background = Color(0xFFF4F4F5),
    surface = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE4E4E7),
    onSurfaceVariant = Zinc700,
    outline = Color(0xFFD4D4D8)
)

@Composable
fun SnapBadgersTheme(
    themeMode: String = "System",
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        "Dark" -> true
        "Light" -> false
        else -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
