package com.example.snapbadgers.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.snapbadgers.data.SettingsRepository
import com.example.snapbadgers.ui.theme.SnapBadgersTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsRepository = remember { SettingsRepository(this) }
            val themeMode by settingsRepository.themeMode

            SnapBadgersTheme(themeMode = themeMode) {
                Surface {
                    SnapBadgersDemoScreen(settingsRepository = settingsRepository)
                }
            }
        }
    }
}
