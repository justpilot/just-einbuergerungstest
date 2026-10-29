package io.justpilot.justeinbuergerung.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

private val LocalFeedbackColors = staticCompositionLocalOf { LightFeedbackColors }

@Composable
fun JustEinbuergerungstestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Material You is available on every supported version (minSdk 31).
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalFeedbackColors provides if (darkTheme) DarkFeedbackColors else LightFeedbackColors) {
        MaterialTheme(colorScheme = colorScheme(darkTheme, dynamicColor), content = content)
    }
}

/** The colours of the theme; the settings also use them to preview each palette. */
@Composable
fun colorScheme(darkTheme: Boolean, dynamicColor: Boolean): ColorScheme = when {
    dynamicColor && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
    dynamicColor -> dynamicLightColorScheme(LocalContext.current)
    darkTheme -> DarkColors
    else -> LightColors
}

/** Extra colour roles of the app, next to [MaterialTheme.colorScheme]. */
object AppTheme {
    val feedbackColors: FeedbackColors
        @Composable
        @ReadOnlyComposable
        get() = LocalFeedbackColors.current
}
