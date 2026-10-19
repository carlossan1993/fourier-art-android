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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.fourierart.app.core.FourierCurve
import com.fourierart.app.core.ContourDrawing
import com.fourierart.app.core.PhotoContours
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
    var drawing by remember { mutableStateOf<ContourDrawing?>(ContourDrawing.sample(FourierCurve.star())) }
    var showComplete by remember(drawing) { mutableStateOf(false) }
    var mode by remember { mutableStateOf(PhotoContours.Mode.AUTO) }
    var analysis by remember { mutableStateOf<PhotoContours.Analysis?>(null) }
    var analyzedMode by remember { mutableStateOf<PhotoContours.Mode?>(null) }
    var outcome by remember { mutableStateOf(PhotoContours.Kind.NO_SUBJECT) }
    var image by remember { mutableStateOf<Bitmap?>(null) }
    var components by remember { mutableIntStateOf(50) }
    var sensitivity by remember { mutableIntStateOf(50) }
    var loading by remember { mutableStateOf(false) }
    // 0 = sample; 1 = detected strokes; 2 = no contour; 3 = read failure; 4 = contour error; 5 = profile changed
    var result by remember { mutableIntStateOf(0) }

    suspend fun process(bitmap: Bitmap) {
        val processed = withContext(Dispatchers.Default) {
            val cached = analysis?.takeIf { analyzedMode == mode && !it.modelUnavailable } ?: PhotoContours.analyze(bitmap, mode)
            Pair(cached, PhotoContours.extract(cached, mode, sensitivity))
        }
        analysis = processed.first
        analyzedMode = mode
        drawing = processed.second.drawing
        outcome = processed.second.kind
        result = if (drawing != null) 1 else 2
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) scope.launch {
            loading = true
            var errorPhase = 3
            try {
                val bitmap = withContext(Dispatchers.IO) { decodePhoto(context, uri) }
                image = bitmap
                analysis = null
                analyzedMode = null
                drawing = null
                errorPhase = 4
                process(bitmap)
            } catch (_: Exception) {
                drawing = null
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
                        onClick = { drawing = ContourDrawing.sample(FourierCurve.circle()); image = null; analysis = null; result = 0 },
                        enabled = !loading,
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.circle)) }
                    OutlinedButton(
                        onClick = { drawing = ContourDrawing.sample(FourierCurve.star()); image = null; analysis = null; result = 0 },
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
                    ContourPreview(image!!, drawing, Modifier.fillMaxWidth().height(220.dp))
                    Text(stringResource(R.string.processing_profile))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (profile in PhotoContours.Mode.entries) {
                            OutlinedButton(
                                onClick = { mode = profile; drawing = null; result = 5 },
                                enabled = !loading && mode != profile,
                                modifier = Modifier.weight(1f)
                            ) { Text(stringResource(when (profile) {
                                PhotoContours.Mode.AUTO -> R.string.profile_auto
                                PhotoContours.Mode.SUBJECT -> R.string.profile_subject
                                PhotoContours.Mode.DRAWING -> R.string.profile_drawing
                            })) }
                        }
                    }
                    Text(stringResource(R.string.sensitivity, sensitivity))
                    Slider(
                        value = sensitivity.toFloat(),
                        onValueChange = { sensitivity = it.roundToInt() },
                        enabled = !loading && !(result == 1 && outcome == PhotoContours.Kind.FACE),
                        valueRange = 0f..100f,
                        steps = 9
                    )
                    if (result == 1 && outcome == PhotoContours.Kind.FACE) {
                        Text(stringResource(R.string.face_sensitivity), style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(
                        onClick = {
                            val bitmap = image ?: return@OutlinedButton
                            scope.launch {
                                loading = true
                                try {
                                    process(bitmap)
                                } catch (_: Exception) {
                                    drawing = null
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
                    1 -> Text(stringResource(when (outcome) {
                        PhotoContours.Kind.FACE -> R.string.outcome_face
                        PhotoContours.Kind.SUBJECT -> R.string.outcome_subject
                        else -> R.string.outcome_drawing
                    }, drawing?.strokes?.size ?: 0), color = accent)
                    2 -> Text(stringResource(if (outcome == PhotoContours.Kind.MODEL_UNAVAILABLE)
                        R.string.model_unavailable else R.string.outcome_missing), color = Color(0xFFFFD18A))
                    5 -> Text(stringResource(R.string.profile_changed), color = Color(0xFFFFD18A))
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
                if (drawing != null) {
                    FourierDisplay(drawing!!, components, showComplete, Modifier.fillMaxWidth().height(300.dp))
                    OutlinedButton(onClick = { showComplete = !showComplete }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(if (showComplete) R.string.show_animation else R.string.show_complete))
                    }
                } else {
                    Text(stringResource(R.string.no_drawing), Modifier.padding(16.dp))
                }
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
private fun ContourPreview(bitmap: Bitmap, drawing: ContourDrawing?, modifier: Modifier) {
    val preview = remember(bitmap) { bitmap.asImageBitmap() }
    Canvas(modifier) {
        val factor = min(size.width / bitmap.width, size.height / bitmap.height)
        val left = (size.width - bitmap.width * factor) / 2f
        val top = (size.height - bitmap.height * factor) / 2f
        drawImage(preview,
            dstOffset = androidx.compose.ui.unit.IntOffset(left.roundToInt(), top.roundToInt()),
            dstSize = androidx.compose.ui.unit.IntSize((bitmap.width * factor).roundToInt(), (bitmap.height * factor).roundToInt()))
        // Detector uses <=640px while the import may have up to 900px.
        val detectorFactor = minOf(1.0, 640.0 / max(bitmap.width, bitmap.height))
        val detectorWidth = max(1, (bitmap.width * detectorFactor).toInt())
        val detectorHeight = max(1, (bitmap.height * detectorFactor).toInt())
        for (stroke in drawing?.sourceStrokes.orEmpty()) {
            val path = Path()
            stroke.points.forEachIndexed { i, p ->
                val x = left + p.x.toFloat() / detectorWidth * bitmap.width * factor
                val y = top + p.y.toFloat() / detectorHeight * bitmap.height * factor
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            if (stroke.closed) path.close()
            drawPath(path, accent, style = Stroke(1.5.dp.toPx()))
        }
    }
}

@Composable
private fun FourierDisplay(drawing: ContourDrawing, components: Int, showComplete: Boolean, modifier: Modifier = Modifier) {
    androidx.compose.runtime.key(drawing) {
        val transition = rememberInfiniteTransition(label = "fourier-animation")
        val animatedProgress by transition.animateFloat(
            initialValue = 0f, targetValue = 1.25f,
            animationSpec = infiniteRepeatable(animation = tween(15000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
            label = "trace-reveal"
        )
        val progress = if (showComplete) 1f else animatedProgress.coerceAtMost(1f)
        val trails = remember(drawing, components) { drawing.strokes.map { it.trace(components) } }
        val weights = remember(drawing) {
            drawing.strokes.map { stroke ->
                stroke.curve.points.zipWithNext().sumOf { (a, b) -> kotlin.math.hypot(b.x - a.x, b.y - a.y) }
                    .times(stroke.visibleFraction).coerceAtLeast(0.01)
            }
        }
        val total = weights.sum()
        Canvas(modifier = modifier.clipToBounds()) {
            drawRect(background)
            val scale = min(size.width, size.height) * 0.43f
            val origin = Offset(size.width / 2f, size.height / 2f)
            fun xy(p: Point2) = Offset(origin.x + p.x.toFloat() * scale, origin.y + p.y.toFloat() * scale)
            var elapsed = 0.0
            var active = drawing.strokes.lastIndex
            var local = 1.0
            for (i in trails.indices) {
                val part = ((progress * total - elapsed) / weights[i]).coerceIn(0.0, 1.0)
                if (part < 1 && progress * total >= elapsed) { active = i; local = part }
                elapsed += weights[i]
                if (part <= 0) continue
                val trail = trails[i]
                val count = (part * (trail.size - 1)).toInt().coerceIn(0, trail.lastIndex)
                val path = Path().apply {
                    moveTo(xy(trail[0]).x, xy(trail[0]).y)
                    for (j in 1..count) { val p = xy(trail[j]); lineTo(p.x, p.y) }
                }
                drawPath(path, Color.White, style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round))
            }
            if (showComplete) return@Canvas
            val stroke = drawing.strokes[active]
            val t = 2 * PI * local * stroke.visibleFraction
            var pos = Point2(0.0, 0.0)
            for (h in stroke.curve.harmonics.take(components)) {
                val end = h.at(t)
                val dest = Point2(pos.x + end.x, pos.y + end.y)
                // DC positions the chain; it is an offset, not a rotating circle.
                if (h.frequency != 0) {
                    if (h.radius * scale > 1.2) drawCircle(accent.copy(alpha = 0.3f),
                        radius = (h.radius * scale).toFloat(), center = xy(pos), style = Stroke(0.7.dp.toPx()))
                    drawLine(accent.copy(alpha = 0.6f), xy(pos), xy(dest), 0.8.dp.toPx())
                }
                pos = dest
            }
            drawCircle(accent, 3.dp.toPx(), xy(pos))
        }
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
