package com.wordnest.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.wordNestDataStore by preferencesDataStore(name = "wordnest_prefs")
