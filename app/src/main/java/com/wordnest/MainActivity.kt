package com.wordnest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordnest.data.repository.AppSettings
import com.wordnest.ui.nav.AppNavHost
import com.wordnest.ui.theme.WordNestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as WordNestApplication).container
        setContent {
            val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = AppSettings()
            )
            WordNestTheme(darkTheme = settings.darkMode) {
                AppNavHost(container = container)
            }
        }
    }
}
