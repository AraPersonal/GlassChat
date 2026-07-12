package com.glasschat.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

private val DarkScheme = darkColorScheme(
    primary = AccentBlue,
    secondary = AccentPurple,
    background = BgTop,
    surface = BgBottom,
    onPrimary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun GlassChatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        typography = MaterialTheme.typography.copy(
            bodyLarge = TextStyle(fontSize = 16.sp, color = TextPrimary),
            titleLarge = TextStyle(fontSize = 20.sp, color = TextPrimary)
        ),
        content = content
    )
}
