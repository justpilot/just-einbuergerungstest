package io.justpilot.justeinbuergerung.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
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
fun JustEinbürgerungstestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic colour is available on every supported version (minSdk 31).
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
        dynamicColor -> dynamicLightColorScheme(LocalContext.current)
        darkTheme -> DarkColors
        else -> LightColors
    }
    CompositionLocalProvider(LocalFeedbackColors provides if (darkTheme) DarkFeedbackColors else LightFeedbackColors) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}

/** Extra colour roles of the app, next to [MaterialTheme.colorScheme]. */
object AppTheme {
    val feedbackColors: FeedbackColors
        @Composable
        @ReadOnlyComposable
        get() = LocalFeedbackColors.current
}
