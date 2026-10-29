package io.justpilot.justeinbuergerung

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.justpilot.justeinbuergerung.ui.JustApp
import io.justpilot.justeinbuergerung.ui.theme.JustEinbürgerungstestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JustEinbürgerungstestTheme {
                JustApp()
            }
        }
    }
}
