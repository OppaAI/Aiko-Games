package com.aiko.games.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = ShoujoAccent,
    onPrimary = Color.White,
    secondary = BoardWood,
    background = ShoujoSoftPink,
    onBackground = ShoujoText,
    surface = Color.White,
    onSurface = ShoujoText,
    surfaceVariant = ShoujoPalePink,
)

private val DarkColors = darkColorScheme(
    primary = ShoujoAccent,
    onPrimary = Color.White,
    secondary = BoardWood,
    background = ShoujoDarkBg,
    onBackground = ShoujoDarkText,
    surface = ShoujoDarkSurface,
    onSurface = ShoujoDarkText,
    surfaceVariant = ShoujoDarkPalePink,
)

@Composable
fun AikoGamesTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
