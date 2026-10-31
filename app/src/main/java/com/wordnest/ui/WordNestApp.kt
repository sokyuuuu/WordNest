package com.wordnest.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.wordnest.WordNestApplication
import com.wordnest.ui.nav.AppNavHost

@Composable
fun WordNestApp() {
    val context = LocalContext.current.applicationContext as WordNestApplication
    AppNavHost(container = context.container)
}
