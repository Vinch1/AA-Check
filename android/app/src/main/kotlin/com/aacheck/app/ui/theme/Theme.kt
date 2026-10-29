package com.aacheck.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFE5734A),
    onPrimary = Color.White,
    secondary = Color(0xFF5C9E8C),
    background = Color(0xFFF8F2E8),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1EAE0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE5734A),
    secondary = Color(0xFF5C9E8C),
)

@Composable
fun AaCheckTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
