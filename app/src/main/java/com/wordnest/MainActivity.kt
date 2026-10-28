package com.wordnest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordnest.data.AppSettings
import com.wordnest.data.SettingsRepository
import com.wordnest.ui.WordNestApp
import com.wordnest.ui.theme.WordNestTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val repository = remember { SettingsRepository(applicationContext) }
            val settings by repository.settings.collectAsStateWithLifecycle(initialValue = AppSettings())

            WordNestTheme(darkTheme = settings.darkMode) {
                WordNestApp()
            }
        }
    }
}
