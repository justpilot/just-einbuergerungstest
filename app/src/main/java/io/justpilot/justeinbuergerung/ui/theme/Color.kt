package io.justpilot.justeinbuergerung.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Calm blue-teal palette, used when dynamic colour is off (and in previews).
internal val LightColors = lightColorScheme(
    primary = Color(0xFF2B6577),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB8EAFF),
    onPrimaryContainer = Color(0xFF001F28),
    secondary = Color(0xFF4C626A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFE6F0),
    onSecondaryContainer = Color(0xFF071E25),
    tertiary = Color(0xFF5A5B7E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE0E0FF),
    onTertiaryContainer = Color(0xFF161837),
    background = Color(0xFFF6FAFD),
    onBackground = Color(0xFF171C1F),
    surface = Color(0xFFF6FAFD),
    onSurface = Color(0xFF171C1F),
    surfaceVariant = Color(0xFFDBE4E8),
    onSurfaceVariant = Color(0xFF40484C),
    outline = Color(0xFF70787C),
    outlineVariant = Color(0xFFBFC8CC),
    surfaceDim = Color(0xFFD6DBDE),
    surfaceBright = Color(0xFFF6FAFD),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F4F7),
    surfaceContainer = Color(0xFFEAEEF1),
    surfaceContainerHigh = Color(0xFFE4E9EC),
    surfaceContainerHighest = Color(0xFFDFE3E6),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFF95CEE3),
    onPrimary = Color(0xFF003544),
    primaryContainer = Color(0xFF074D5E),
    onPrimaryContainer = Color(0xFFB8EAFF),
    secondary = Color(0xFFB3CAD4),
    onSecondary = Color(0xFF1E333B),
    secondaryContainer = Color(0xFF354A52),
    onSecondaryContainer = Color(0xFFCFE6F0),
    tertiary = Color(0xFFC3C3EB),
    onTertiary = Color(0xFF2C2D4D),
    tertiaryContainer = Color(0xFF424465),
    onTertiaryContainer = Color(0xFFE0E0FF),
    background = Color(0xFF0F1417),
    onBackground = Color(0xFFDEE3E6),
    surface = Color(0xFF0F1417),
    onSurface = Color(0xFFDEE3E6),
    surfaceVariant = Color(0xFF40484C),
    onSurfaceVariant = Color(0xFFBFC8CC),
    outline = Color(0xFF899296),
    outlineVariant = Color(0xFF40484C),
    surfaceDim = Color(0xFF0F1417),
    surfaceBright = Color(0xFF353A3D),
    surfaceContainerLowest = Color(0xFF0A0F11),
    surfaceContainerLow = Color(0xFF171C1F),
    surfaceContainer = Color(0xFF1B2023),
    surfaceContainerHigh = Color(0xFF252B2D),
    surfaceContainerHighest = Color(0xFF303638),
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
