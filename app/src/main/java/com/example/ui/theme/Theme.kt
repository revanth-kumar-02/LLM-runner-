package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val WarmSanctuaryColorScheme = lightColorScheme(
    primary = PrimaryDustyRose,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    inversePrimary = PrimaryFixedDim,
    secondary = SecondaryMutedCoral,
    onSecondary = OnPrimary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = TertiaryForest,
    onTertiary = OnPrimary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnSecondaryContainer,
    error = ErrorRed,
    errorContainer = ErrorContainer,
    onError = OnError,
    onErrorContainer = OnErrorContainer,
    background = CanvasSurface,
    onBackground = OnSurface,
    surface = CanvasSurface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceContainerHighest,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceTint = PrimaryDustyRose,
    surfaceDim = CanvasSurfaceDim,
    surfaceBright = CanvasSurfaceBright,
    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,
    outline = OutlineColor,
    outlineVariant = OutlineVariantColor,
    scrim = SurfaceContainerHighest,      // warm beige scrim instead of Material grey
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // Strictly Light Theme Warm Sanctuary palette (No dynamic colors override)
    MaterialTheme(
        colorScheme = WarmSanctuaryColorScheme,
        typography = AppTypography,
        content = content
    )
}
