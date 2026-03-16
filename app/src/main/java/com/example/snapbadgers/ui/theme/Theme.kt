package com.example.snapbadgers.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
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

@Composable
fun SnapBadgersTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
