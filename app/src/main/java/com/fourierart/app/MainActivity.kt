package com.fourierart.app

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.fourierart.app.photo.PhotoSessionModel
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

class MainActivity : AppCompatActivity() {
    val photoSession: PhotoSessionModel by lazy { ViewModelProvider(this)[PhotoSessionModel::class.java] }

    val exportSession: com.fourierart.app.export.ExportModel by lazy { ViewModelProvider(this)[com.fourierart.app.export.ExportModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(0xFF06131E.toInt()),
            navigationBarStyle = SystemBarStyle.dark(0xFF06131E.toInt())
        )
        setContent { FourierArtContent(photoSession, exportSession) }
    }
}

@androidx.compose.runtime.Composable
fun FourierArtContent(photoSession: PhotoSessionModel, exportSession: com.fourierart.app.export.ExportModel) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF35D4DE),
            onPrimary = Color(0xFF06131E),
            background = Color(0xFF06131E),
            surface = Color(0xFF102532),
            outline = Color(0xFF35515F),
            onSurfaceVariant = Color(0xFFAFBFCD),
            onBackground = Color.White,
            onSurface = Color.White
        ),
        shapes = androidx.compose.material3.Shapes(
            small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
        )
    ) {
        Surface(Modifier.fillMaxSize()) {
            FourierExperimentScreen(photoSession, exportSession)
        }
    }
}
