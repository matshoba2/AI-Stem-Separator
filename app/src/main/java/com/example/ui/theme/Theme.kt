package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkStudioColorScheme = darkColorScheme(
    primary = StemBass,           // Cyber Turquoise as the main accent
    secondary = StemMelody,       // Hyper Purple
    tertiary = StemVocal,         // Glowing Rose Pink
    background = BackgroundDark,  // Deep Anthracite
    surface = SurfaceDark,        // Graphite Card Surfaces
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextPrimary,   // Sharp, light-grey text
    onSurface = TextPrimary,
    outline = OutlineGrey         // Defined borders
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // We enforce our distinct Dark Studio theme for that premium hardware vibe!
    MaterialTheme(
        colorScheme = DarkStudioColorScheme,
        typography = Typography,
        content = content
    )
}
