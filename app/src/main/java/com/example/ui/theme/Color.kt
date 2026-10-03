package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Warm Sanctuary Design System Tokens (Global Color Consistency Pass)
val CanvasSurface = Color(0xFFFEF8F6)
val CanvasSurfaceDim = Color(0xFFEEDCD7)
val CanvasSurfaceBright = Color(0xFFFEF8F6)
val SurfaceContainerLowest = Color(0xFFFFFFFF)
val SurfaceContainerLow = Color(0xFFF9F1ED)
val SurfaceContainer = Color(0xFFF3E7E3)
val SurfaceContainerHigh = Color(0xFFEDE0DC)
val SurfaceContainerHighest = Color(0xFFE5D8D4)

val OnSurface = Color(0xFF1D1B1A)
val OnSurfaceVariant = Color(0xFF4A3B3A)
val InverseSurface = Color(0xFF32302F)
val InverseOnSurface = Color(0xFFF6EFED)
val OutlineColor = Color(0xFF755D5B)
val OutlineVariantColor = Color(0xFFC9A8A5)

val PrimaryDustyRose = Color(0xFF894A4A)
val PrimaryDustyRoseAccent = Color(0xFFC47B7B)
val PrimaryDustyRoseDeep = Color(0xFFB86B6B)
val PrimaryContainer = Color(0xFFA66262)
val OnPrimary = Color(0xFFFFFFFF)
val OnPrimaryContainer = Color(0xFFFFFBFF)
val PrimaryFixed = Color(0xFFFFDAD9)
val PrimaryFixedDim = Color(0xFFFFB3B2)
val OnPrimaryFixed = Color(0xFF390B0E)
val OnPrimaryFixedVariant = Color(0xFF6F3536)

val SecondaryMutedCoral = Color(0xFF92493A)
val SecondaryCoralAccent = Color(0xFFD98270)
val SecondaryCoralLight = Color(0xFFE8927C)
val SecondaryContainer = Color(0xFFFEA08D)
val OnSecondaryContainer = Color(0xFF783527)
val SecondaryFixed = Color(0xFFFFDAD3)
val SecondaryFixedDim = Color(0xFFFFB4A5)
val OnSecondaryFixed = Color(0xFF3C0802)
val OnSecondaryFixedVariant = Color(0xFF753325)

// Tertiary redirected to warm coral palette (strictly eliminating green)
val TertiaryForest = Color(0xFF92493A)
val TertiaryContainer = Color(0xFFFEA08D)
val TertiaryFixed = Color(0xFFFFDAD3)
val TertiaryFixedDim = Color(0xFFFFB4A5)

val ErrorRed = Color(0xFFBA1A1A)
val ErrorContainer = Color(0xFFFFDAD6)
val OnError = Color(0xFFFFFFFF)
val OnErrorContainer = Color(0xFF93000A)

// Additional warm semantic tokens to eliminate visual grey fallbacks
// Use these in place of Material3 defaults that produce cool grey tones on device
val WarmMutedText         = Color(0xFF7A5E5B)  // warmer than OnSurfaceVariant; for footnotes, metadata
val SubtleWarmDivider     = Color(0xFFEDE0DC)  // same as SurfaceContainerHigh — intentional warm divider
val WarmTrackBackground   = Color(0xFFF0E3DF)  // warm beige for slider/progress inactive track
val DisabledWarmContent   = Color(0xFFBCA7A5)  // warm-tinted disabled icon/text, replaces Material grey
val WarmIconBackground    = Color(0xFFF5EBE8)  // warm ivory tint for icon container backgrounds
