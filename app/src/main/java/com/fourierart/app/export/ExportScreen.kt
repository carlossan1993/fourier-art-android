package com.fourierart.app.export

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fourierart.app.R
import com.fourierart.app.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ExportScreen(model: ExportModel, onClose: () -> Unit) {
    val snapshot = model.snapshot ?: return
    val spec = model.spec
    val context = LocalContext.current
    val working = model.busy || model.saving
    var previewProgress by remember { mutableFloatStateOf(.5f) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var previewError by remember { mutableStateOf(false) }
    // A single preview bitmap, at display resolution. Final output is rendered separately at its selected resolution.
    LaunchedEffect(snapshot,spec,previewProgress) {
        previewError = false
        try {
            preview = withContext(Dispatchers.Default) {
                val width = 360; val height = if (spec.frame == ExportFrame.SQUARE) 360 else 640
                Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888).also {
                    ExportRenderer(snapshot).draw(Canvas(it),width,height,if (spec.format == ExportFormat.PNG) 1.0 else previewProgress.toDouble())
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { previewError = true }
        catch (e: OutOfMemoryError) { previewError = true }
    }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) result.data?.data?.let(model::save)
    }
    fun close() { model.cancel(); onClose() }
    BackHandler { close() }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text(stringResource(R.string.export_title),style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f))
            OutlinedButton(onClick={close()},modifier=Modifier.testTag("export_close")) { Text(stringResource(R.string.close_player)) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            val maxHeight = (LocalConfiguration.current.screenHeightDp * .40f).dp
            Box(Modifier.fillMaxWidth().height(maxHeight),contentAlignment=Alignment.Center) {
                preview?.let { Image(it.asImageBitmap(),stringResource(R.string.export_preview),Modifier.fillMaxHeight().aspectRatio(spec.width.toFloat()/spec.height).testTag("export_preview")) }
                if (preview == null && !previewError) CircularProgressIndicator()
            }
            if (previewError) Text(stringResource(R.string.export_memory))
            Text(stringResource(R.string.export_dimensions,spec.width,spec.height),style=MaterialTheme.typography.bodySmall)
            if (spec.format != ExportFormat.PNG) {
                Text(stringResource(R.string.export_preview_position),style=MaterialTheme.typography.bodySmall)
                Slider(previewProgress,{previewProgress=it},enabled=!working,modifier=Modifier.testTag("export_preview_progress"))
            }
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                for (format in ExportFormat.entries) OutlinedButton({model.configure(spec.copy(format=format))},enabled=!working && format!=spec.format,
                    modifier=Modifier.weight(1f).testTag("format_${format.name.lowercase()}")) { Text(format.name) }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                for (frame in ExportFrame.entries) OutlinedButton({model.configure(spec.copy(frame=frame))},enabled=!working && frame!=spec.frame,
                    modifier=Modifier.weight(1f).testTag("frame_${frame.name.lowercase()}")) { Text(if(frame==ExportFrame.SQUARE) "1:1" else "9:16") }
            }
            if (spec.format == ExportFormat.PNG) {
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    for (size in listOf(720,1080)) OutlinedButton({model.configure(spec.copy(pngSize=size))},enabled=!working && spec.pngSize!=size,
                        modifier=Modifier.weight(1f)) { Text("${size} px") }
                }
            } else {
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    for (seconds in listOf(6,10)) OutlinedButton({model.configure(spec.copy(seconds=seconds))},enabled=!working && spec.seconds!=seconds,
                        modifier=Modifier.weight(1f).testTag("duration_$seconds")) { Text(stringResource(R.string.export_seconds,seconds)) }
                }
                Text(stringResource(R.string.export_timing),style=MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick=model::fit,enabled=!working && snapshot.viewport!=PreviewViewport(),modifier=Modifier.fillMaxWidth()) { Text(stringResource(R.string.fit_drawing)) }
            Text(stringResource(R.string.export_style),style=MaterialTheme.typography.bodySmall)
            Button(onClick=model::start,enabled=!working,modifier=Modifier.fillMaxWidth().testTag("export_start")) { Text(stringResource(R.string.export_create)) }
            if (working) {
                LinearProgressIndicator(progress={model.progress},modifier=Modifier.fillMaxWidth().testTag("export_progress"))
                Text(stringResource(if(model.saving) R.string.export_saving else R.string.export_rendering,(model.progress*100).roundToInt()))
                OutlinedButton(onClick=model::cancel,modifier=Modifier.fillMaxWidth().testTag("export_cancel")) { Text(stringResource(R.string.export_cancel)) }
            }
            if (model.notice!=0) Text(stringResource(model.notice),modifier=Modifier.testTag("export_notice"))
            val completed = model.completed
            val output = model.completedSpec
            if (completed!=null && output!=null) {
                Text(stringResource(R.string.export_result,output.format.name,output.width,output.height,
                    String.format(Locale.getDefault(),"%.2f",completed.length()/1_048_576.0)),modifier=Modifier.testTag("export_result"))
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Button(onClick={ save.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE); type=output.format.mime
                        putExtra(Intent.EXTRA_TITLE,"Fourier-Art-${System.currentTimeMillis()}.${output.format.extension}")
                    }) },enabled=!working,modifier=Modifier.weight(1f).testTag("export_save")) { Text(stringResource(R.string.export_save)) }
                    OutlinedButton(onClick={
                        try { model.shareIntent(context)?.let { context.startActivity(Intent.createChooser(it,context.getString(R.string.export_share))) } }
                        catch (e: Exception) { model.shareFailed() }
                    },enabled=!working,modifier=Modifier.weight(1f).testTag("export_share")) { Text(stringResource(R.string.export_share)) }
                }
                Text(stringResource(R.string.export_cache_notice),style=MaterialTheme.typography.bodySmall)
            }
        }
    }
}
