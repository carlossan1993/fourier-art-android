package com.fourierart.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF65DCD0),
                    onPrimary = Color(0xFF0D1929),
                    background = Color(0xFF0D1929),
                    surface = Color(0xFF17293B),
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                Surface(Modifier.fillMaxSize()) {
                    FourierExperimentScreen()
                }
            }
        }
    }
}
