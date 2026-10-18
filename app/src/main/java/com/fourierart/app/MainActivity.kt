package com.fourierart.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FourierArtApp()
        }
    }
}

private val background = Color(0xFF0D1929)
private val teal = Color(0xFF64DDD0)

@Composable
private fun FourierArtApp() {
    val currentLanguage = LocalContext.current.resources.configuration.locales[0].language

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = teal,
            onPrimary = background,
            background = background,
            surface = Color(0xFF17293B),
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.app_name).uppercase(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.tagline),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                EpicycleIllustration()
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            stringResource(R.string.welcome_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            stringResource(R.string.welcome_description),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.choose_photo))
                        }
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.take_photo))
                        }
                        Text(
                            stringResource(R.string.stage_notice),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Text(
                    stringResource(R.string.language_label),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { changeLanguage("es") },
                        enabled = currentLanguage != "es"
                    ) { Text("Español") }
                    OutlinedButton(
                        onClick = { changeLanguage("en") },
                        enabled = currentLanguage != "en"
                    ) { Text("English") }
                }
                Text(
                    stringResource(R.string.version_label),
                    color = Color.LightGray,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

private fun changeLanguage(language: String) {
    AppCompatDelegate.setApplicationLocales(
        LocaleListCompat.forLanguageTags(language)
    )
}

@Composable
private fun EpicycleIllustration() {
    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        val center = Offset(size.width * 0.38f, size.height * 0.54f)
        val bigRadius = size.height * 0.34f
        val secondCenter = Offset(center.x + bigRadius, center.y)
        val smallRadius = bigRadius * 0.43f
        val tip = Offset(
            secondCenter.x + smallRadius * 0.48f,
            secondCenter.y - smallRadius * 0.87f
        )
        drawCircle(
            color = teal.copy(alpha = 0.65f),
            radius = bigRadius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = teal.copy(alpha = 0.8f),
            radius = smallRadius,
            center = secondCenter,
            style = Stroke(width = 2.dp.toPx())
        )
        drawLine(teal, center, secondCenter, 2.dp.toPx())
        drawLine(teal, secondCenter, tip, 2.dp.toPx())
        drawCircle(color = Color.White, radius = 5.dp.toPx(), center = tip)
    }
}
