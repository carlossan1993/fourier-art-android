package com.fourierart.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.fourierart.app.core.FourierCurve
import com.fourierart.app.core.ImageSilhouette
import com.fourierart.app.core.Point2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val accent = Color(0xFF65DCD0)
private val background = Color(0xFF0D1929)
private val harmonicOptions = listOf(10, 20, 50, 100, 200)

@Composable
fun FourierExperimentScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var curve by remember { mutableStateOf(FourierCurve.star()) }
    var image by remember { mutableStateOf<Bitmap?>(null) }
    var components by remember { mutableIntStateOf(50) }
    var sensitivity by remember { mutableIntStateOf(50) }
    var loading by remember { mutableStateOf(false) }
    // 0 = sample; 1 = silhouette; 2 = no contour; 3 = image read failure; 4 = contour error
    var result by remember { mutableIntStateOf(0) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) scope.launch {
            loading = true
            var errorPhase = 3
            try {
                val bitmap = withContext(Dispatchers.IO) { decodePhoto(context, uri) }
                image = bitmap
                errorPhase = 4
                val detected = withContext(Dispatchers.Default) {
                    ImageSilhouette.fromBitmap(bitmap, sensitivity)
                }
                if (detected != null) {
                    curve = detected
                    result = 1
                } else {
                    result = 2
                }
            } catch (_: Exception) {
                result = errorPhase
            } finally {
                loading = false
            }
        }
    }

    val currentLanguage = context.resources.configuration.locales[0].language
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.app_name).uppercase(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.proof_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    stringResource(R.string.proof_choose),
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { curve = FourierCurve.circle(); image = null; result = 0 },
                        enabled = !loading,
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.circle)) }
                    OutlinedButton(
                        onClick = { curve = FourierCurve.star(); image = null; result = 0 },
                        enabled = !loading,
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.star)) }
                }
                Button(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.choose_photo)) }
                Text(
                    stringResource(R.string.photo_warning),
                    style = MaterialTheme.typography.bodySmall
                )
                if (image != null) {
                    Image(
                        bitmap = image!!.asImageBitmap(),
                        contentDescription = stringResource(R.string.selected_image),
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text(stringResource(R.string.sensitivity, sensitivity))
                    Slider(
                        value = sensitivity.toFloat(),
                        onValueChange = { sensitivity = it.roundToInt() },
                        valueRange = 25f..115f,
                        steps = 8
                    )
                    OutlinedButton(
                        onClick = {
                            val bitmap = image ?: return@OutlinedButton
                            scope.launch {
                                loading = true
                                try {
                                    val detected = withContext(Dispatchers.Default) {
                                        ImageSilhouette.fromBitmap(bitmap, sensitivity)
                                    }
                                    if (detected != null) {
                                        curve = detected
                                        result = 1
                                    } else {
                                        result = 2
                                    }
                                } catch (_: Exception) {
                                    result = 4
                                } finally {
                                    loading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading
                    ) { Text(stringResource(R.string.reprocess)) }
                }
                if (loading) CircularProgressIndicator()
                when (result) {
                    1 -> Text(stringResource(R.string.outcome_found), color = accent)
                    2 -> Text(stringResource(R.string.outcome_missing), color = Color(0xFFFFD18A))
                    3 -> Text(stringResource(R.string.outcome_error), color = Color(0xFFFFB3B3))
                    4 -> Text(stringResource(R.string.outcome_contour_error), color = Color(0xFFFFB3B3))
                    else -> Text(stringResource(R.string.outcome_sample), color = accent)
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.proof_animation),
                    style = MaterialTheme.typography.titleMedium
                )
                FourierDisplay(curve, components, Modifier.fillMaxWidth().height(270.dp))
                Text(stringResource(R.string.components_label, components))
                Slider(
                    value = harmonicOptions.indexOf(components).coerceAtLeast(0).toFloat(),
                    onValueChange = {
                        components = harmonicOptions[it.roundToInt().coerceIn(0, 4)]
                    },
                    valueRange = 0f..4f,
                    steps = 3
                )
                Text(
                    stringResource(R.string.proof_explanation),
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
            stringResource(R.string.proof_version),
            style = MaterialTheme.typography.labelSmall,
            color = Color.LightGray
        )
        Spacer(Modifier.height(8.dp))
    }
}

private fun changeLanguage(language: String) {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
}

@Composable
private fun FourierDisplay(curve: FourierCurve, components: Int, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "fourier-animation")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "trace-reveal"
    )
    val trail = remember(curve, components) { curve.trace(components, 320) }
    Canvas(modifier = modifier) {
        drawRect(background)
        val scale = min(size.width, size.height) * 0.39f
        val origin = Offset(size.width / 2f, size.height / 2f)
        fun xy(p: Point2): Offset = Offset(
            origin.x + p.x.toFloat() * scale,
            origin.y + p.y.toFloat() * scale
        )

        val length = (trail.size * progress).roundToInt().coerceIn(1, trail.size)
        if (length > 1) {
            val drawn = Path().apply {
                moveTo(xy(trail[0]).x, xy(trail[0]).y)
                for (i in 1 until length) {
                    val pos = xy(trail[i])
                    lineTo(pos.x, pos.y)
                }
            }
            drawPath(drawn, Color.White, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        }

        val t = 2 * PI * progress
        var pos = Point2(0.0, 0.0)
        for (h in curve.harmonics.take(components)) {
            val end = h.at(t)
            val destination = Point2(pos.x + end.x, pos.y + end.y)
            if (h.radius * scale > 1.2) {
                drawCircle(
                    accent.copy(alpha = 0.37f),
                    radius = (h.radius * scale).toFloat(),
                    center = xy(pos),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            drawLine(accent.copy(alpha = 0.7f), xy(pos), xy(destination), 1.dp.toPx())
            pos = destination
        }
        drawCircle(accent, 4.dp.toPx(), xy(pos))
    }
}

private fun decodePhoto(context: Context, uri: Uri): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    // BitmapFactory returns null deliberately with inJustDecodeBounds=true.
    // Check the stream itself and then use outWidth/outHeight instead.
    val boundsStream = context.contentResolver.openInputStream(uri)
        ?: throw IllegalArgumentException("Image stream unavailable")
    boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth < 1 || bounds.outHeight < 1) throw IllegalArgumentException("Invalid image")
    var downsample = 1
    while (max(bounds.outWidth, bounds.outHeight) / downsample > 900) downsample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = downsample }
    val bitmap = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    } ?: throw IllegalArgumentException("Image decoding failed")

    // BitmapFactory does not honor JPEG EXIF orientation. The gallery often does,
    // which previously made a portrait appear rotated after Fourier processing.
    val orientation = try {
        context.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (_: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.setRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.setRotate(-90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
        else -> return bitmap
    }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (rotated !== bitmap) bitmap.recycle()
    return rotated
}
