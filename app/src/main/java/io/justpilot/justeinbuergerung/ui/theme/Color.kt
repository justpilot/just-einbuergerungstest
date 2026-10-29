package io.justpilot.justeinbuergerung.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand palette from the launcher icon: a bordeaux passport, gold and a cream background.
// Gold is the tertiary role, so a chosen exam answer never looks like the red of a wrong one.
internal val LightColors = lightColorScheme(
    primary = Color(0xFF8B1E33),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDADC),
    onPrimaryContainer = Color(0xFF40000F),
    secondary = Color(0xFF6B5D4F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF4E0CC),
    onSecondaryContainer = Color(0xFF251A0F),
    tertiary = Color(0xFF705D00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE170),
    onTertiaryContainer = Color(0xFF221B00),
    background = Color(0xFFFFF8F2),
    onBackground = Color(0xFF201A17),
    surface = Color(0xFFFFF8F2),
    onSurface = Color(0xFF201A17),
    surfaceVariant = Color(0xFFF0DFDC),
    onSurfaceVariant = Color(0xFF534341),
    outline = Color(0xFF857371),
    outlineVariant = Color(0xFFD8C2BF),
    surfaceDim = Color(0xFFE3D8D0),
    surfaceBright = Color(0xFFFFF8F2),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFCF2EA),
    surfaceContainer = Color(0xFFF6ECE4),
    surfaceContainerHigh = Color(0xFFF0E6DF),
    surfaceContainerHighest = Color(0xFFEBE0D9),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB2B9),
    onPrimary = Color(0xFF67001C),
    primaryContainer = Color(0xFF7A1426),
    onPrimaryContainer = Color(0xFFFFDADC),
    secondary = Color(0xFFD8C4B0),
    onSecondary = Color(0xFF3B2F23),
    secondaryContainer = Color(0xFF534538),
    onSecondaryContainer = Color(0xFFF4E0CC),
    tertiary = Color(0xFFE4C64A),
    onTertiary = Color(0xFF3A3000),
    tertiaryContainer = Color(0xFF544600),
    onTertiaryContainer = Color(0xFFFFE170),
    background = Color(0xFF1A1110),
    onBackground = Color(0xFFF0DEDC),
    surface = Color(0xFF1A1110),
    onSurface = Color(0xFFF0DEDC),
    surfaceVariant = Color(0xFF534341),
    onSurfaceVariant = Color(0xFFD8C2BF),
    outline = Color(0xFFA08C8A),
    outlineVariant = Color(0xFF534341),
    surfaceDim = Color(0xFF1A1110),
    surfaceBright = Color(0xFF423735),
    surfaceContainerLowest = Color(0xFF140C0B),
    surfaceContainerLow = Color(0xFF221A18),
    surfaceContainer = Color(0xFF271E1C),
    surfaceContainerHigh = Color(0xFF322826),
    surfaceContainerHighest = Color(0xFF3D3230),
)

/** Colours for answer feedback; Material 3 has an error role but no success role. */
data class FeedbackColors(
    val correct: Color,
    val correctContainer: Color,
    val onCorrectContainer: Color,
)

internal val LightFeedbackColors = FeedbackColors(
    correct = Color(0xFF2E6B30),
    correctContainer = Color(0xFFB1F2AE),
    onCorrectContainer = Color(0xFF002106),
)

internal val DarkFeedbackColors = FeedbackColors(
    correct = Color(0xFF95D693),
    correctContainer = Color(0xFF13521C),
    onCorrectContainer = Color(0xFFB1F2AE),
)
