package com.aiko.shogi.ui.theme

import androidx.compose.material3.MaterialTheme
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
)

@Composable
fun AikoShogiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
