package com.fourierart.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.testTag
import com.fourierart.app.photo.PhotoSessionModel
import com.fourierart.app.photo.CameraCapture
import com.fourierart.app.photo.PhotoEditor
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
import androidx.compose.runtime.LaunchedEffect
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
import com.fourierart.app.core.LineStyle
import com.fourierart.app.core.DetailLevel
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
private val harmonicOptions = listOf(10, 20, 50, 100, 200, 400, 800)

@Composable
fun FourierExperimentScreen(model: PhotoSessionModel) {
    val context = LocalContext.current
    var cameraOpen by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) cameraOpen = true else model.notice = R.string.camera_denied
    }
    with(model) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) importPhoto(uri)
    }
    if (cameraOpen) {
        CameraCapture(captures,
            onCaptured = { file -> cameraOpen = false; importPhoto(android.net.Uri.fromFile(file), file) },
            onClose = { cameraOpen = false },
            onError = { cameraOpen = false; notice = R.string.camera_error })
        return@with
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
                        onClick = { sample(true) },
                        enabled = !loading,
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.circle)) }
                    OutlinedButton(
                        onClick = { sample(false) },
                        enabled = !loading,
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.star)) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        enabled = !loading,
                        modifier = Modifier.weight(1f).testTag("choose_photo")
                    ) { Text(stringResource(R.string.choose_photo)) }
                    Button(
                        onClick = {
                            if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) notice = R.string.camera_error
                            else if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) cameraOpen = true
                            else permission.launch(Manifest.permission.CAMERA)
                        },
                        enabled = !loading,
                        modifier = Modifier.weight(1f).testTag("take_photo")
                    ) { Text(stringResource(R.string.take_photo)) }
                }
                if (editing) PhotoEditor(this@with)
                Text(
                    stringResource(R.string.photo_warning),
                    style = MaterialTheme.typography.bodySmall
                )
                if (!editing && image != null) {
                    ContourPreview(image!!, drawing, Modifier.fillMaxWidth().height(220.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { editPhoto() }, enabled = !loading, modifier = Modifier.weight(1f).testTag("edit_photo")) { Text(stringResource(R.string.edit_photo)) }
                        OutlinedButton(onClick = { sample(false) }, enabled = !loading, modifier = Modifier.weight(1f).testTag("discard_photo")) { Text(stringResource(R.string.discard_photo)) }
                    }
                    Text(stringResource(R.string.working_photo_notice), style = MaterialTheme.typography.bodySmall)

                    Text(stringResource(R.string.processing_profile))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (profile in PhotoContours.Mode.entries) {
                            OutlinedButton(
                                onClick = { mode = profile; settingsChanged() },
                                enabled = !loading && mode != profile,
                                modifier = Modifier.weight(1f)
                            ) { Text(stringResource(when (profile) {
                                PhotoContours.Mode.AUTO -> R.string.profile_auto
                                PhotoContours.Mode.SUBJECT -> R.string.profile_subject
                                PhotoContours.Mode.DRAWING -> R.string.profile_drawing
                            })) }
                        }
                    }
                    Text(stringResource(R.string.line_style))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (style in LineStyle.entries) {
                            OutlinedButton(
                                onClick = { lineStyle = style; settingsChanged() },
                                enabled = !loading && lineStyle != style,
                                modifier = Modifier.weight(1f)
                            ) { Text(stringResource(if (style == LineStyle.STRUCTURE) R.string.style_structure else R.string.style_detail)) }
                        }
                    }
                    Text(stringResource(R.string.style_help), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.detail_label))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (level in DetailLevel.entries) {
                            OutlinedButton(
                                onClick = { detail = level; settingsChanged() },
                                enabled = !loading && detail != level,
                                modifier = Modifier.weight(1f)
                            ) { Text(stringResource(when (level) {
                                DetailLevel.CLEAN -> R.string.detail_clean
                                DetailLevel.BALANCED -> R.string.detail_balanced
                                DetailLevel.FINE -> R.string.detail_fine
                            })) }
                        }
                    }
                    Text(stringResource(R.string.detail_help), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.sensitivity, sensitivity))
                    Slider(
                        value = sensitivity.toFloat(),
                        onValueChange = { sensitivity = it.roundToInt(); settingsChanged() },
                        enabled = !loading,
                        valueRange = 0f..100f,
                        steps = 9
                    )
                    if (result == 1 && (outcome == PhotoContours.Kind.FACE || outcome == PhotoContours.Kind.FACE_PARTIAL)) {
                        Text(stringResource(R.string.face_sensitivity), style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(
                        onClick = { reprocess() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading
                    ) { Text(stringResource(R.string.reprocess)) }
                }
                if (loading) CircularProgressIndicator(Modifier.testTag("photo_loading"))
                if (notice != 0) Text(stringResource(notice), color = Color(0xFFFFD18A), modifier = Modifier.testTag("photo_notice"))
                if (result == 1 && (analysis?.detectedFaceCount ?: 0) > 0) {
                    Text(stringResource(R.string.face_count, analysis!!.faces.size, analysis!!.detectedFaceCount),
                        style = MaterialTheme.typography.bodySmall)
                }
                if (!editing) when (result) {
                    1 -> Text(stringResource(when (outcome) {
                        PhotoContours.Kind.FACE -> R.string.outcome_face
                        PhotoContours.Kind.FACE_PARTIAL -> R.string.outcome_face_partial
                        PhotoContours.Kind.SUBJECT -> R.string.outcome_subject
                        else -> R.string.outcome_drawing
                    }, drawing?.strokes?.size ?: 0), color = if (outcome == PhotoContours.Kind.FACE_PARTIAL) Color(0xFFFFD18A) else accent)
                    2 -> Text(stringResource(if (outcome == PhotoContours.Kind.MODEL_UNAVAILABLE)
                        R.string.model_unavailable else R.string.outcome_missing), color = Color(0xFFFFD18A))
                    5 -> Text(stringResource(R.string.profile_changed), color = Color(0xFFFFD18A))
                    3 -> Text(stringResource(R.string.outcome_error), color = Color(0xFFFFB3B3))
                    4 -> Text(stringResource(R.string.outcome_contour_error), color = Color(0xFFFFB3B3))
                    else -> Text(stringResource(R.string.outcome_sample), color = accent)
                }
            }
        }

        if (!editing) Card(Modifier.fillMaxWidth()) {
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
                    OutlinedButton(onClick = { showComplete = !showComplete; settingsChanged(false) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(if (showComplete) R.string.show_animation else R.string.show_complete))
                    }
                } else {
                    Text(stringResource(R.string.no_drawing), Modifier.padding(16.dp))
                }
                Text(stringResource(R.string.components_label, components))
                Slider(
                    value = harmonicOptions.indexOf(components).coerceAtLeast(0).toFloat(),
                    onValueChange = {
                        components = harmonicOptions[it.roundToInt().coerceIn(0, harmonicOptions.lastIndex)]; settingsChanged(false)
                    },
                    valueRange = 0f..harmonicOptions.lastIndex.toFloat(),
                    steps = harmonicOptions.size - 2
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
        // Raster and overlay must use the same dimensions; face points are mapped into that raster.
        val detectorFactor = minOf(1.0, PhotoContours.RASTER_SIDE.toDouble() / max(bitmap.width, bitmap.height))
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
        var trails by remember(drawing, components) { mutableStateOf<List<List<Point2>>>(emptyList()) }
        LaunchedEffect(drawing, components) {
            trails = withContext(Dispatchers.Default) { drawing.strokes.map { it.trace(components, 640) } }
        }
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
                drawPath(path, Color.White, style = Stroke((if (drawing.strokes.size > 200) 1.2.dp else 1.5.dp).toPx(), cap = StrokeCap.Round))
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
