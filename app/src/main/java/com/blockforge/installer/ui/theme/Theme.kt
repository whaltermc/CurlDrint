package com.blockforge.installer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val BlockForgeColorScheme = darkColorScheme(
    primary = Amethyst,
    onPrimary = PureBlack,
    primaryContainer = AmethystDark,
    onPrimaryContainer = OnBlack,
    secondary = AmethystLight,
    background = PureBlack,
    onBackground = OnBlack,
    surface = NearBlack,
    onSurface = OnBlack,
    surfaceVariant = PanelGrey,
    onSurfaceVariant = MutedText,
    error = ErrorRed
)

@Composable
fun BlockForgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BlockForgeColorScheme,
        typography = blockForgeTypography(),
        content = content
    )
}
