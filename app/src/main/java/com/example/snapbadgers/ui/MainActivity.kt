package com.example.snapbadgers.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.example.snapbadgers.ui.theme.SnapBadgersTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SnapBadgersTheme {
                Surface {
                    SnapBadgersDemoScreen()
                }
            }
        }
    }
}
