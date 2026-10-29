package io.justpilot.justeinbuergerung

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.justpilot.justeinbuergerung.ui.JustApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // JustApp applies the theme itself, since the colour setting is read there.
        setContent { JustApp() }
    }
}
