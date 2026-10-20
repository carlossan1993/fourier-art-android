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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.fourierart.app.ui.*
import com.fourierart.app.R
import com.fourierart.app.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.withLock
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
    val previewMutex = remember { kotlinx.coroutines.sync.Mutex() }
    var renderer by remember(snapshot) { mutableStateOf<ExportRenderer?>(null) }
    LaunchedEffect(snapshot) {
        try { renderer = withContext(Dispatchers.Default) { ExportRenderer(snapshot) } }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { previewError = true }
        catch (e: OutOfMemoryError) { previewError = true }
    }
    // Prepare curves once; position changes only render one bitmap at display resolution.
    LaunchedEffect(renderer,spec,previewProgress) {
        val prepared = renderer ?: return@LaunchedEffect
        previewError = false
        var rendered: Bitmap? = null
        try {
            val result = withContext(Dispatchers.Default) {
                previewMutex.withLock {
                    kotlinx.coroutines.currentCoroutineContext().ensureActive()
                    val width = 360; val height = if (spec.frame == ExportFrame.SQUARE) 360 else 640
                    Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888).also {
                        rendered = it
                        prepared.draw(Canvas(it),width,height,if (spec.format == ExportFormat.PNG) 1.0 else previewProgress.toDouble())
                    }
                }
            }
            preview = result
        } catch (e: kotlinx.coroutines.CancellationException) { rendered?.recycle(); throw e }
        catch (e: Exception) { rendered?.recycle(); previewError = true }
        catch (e: OutOfMemoryError) { rendered?.recycle(); previewError = true }
    }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) result.data?.data?.let(model::save)
    }
    fun close() { model.cancel(); onClose() }
    BackHandler { close() }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            OutlinedButton(onClick={close()},modifier=Modifier.heightIn(min=48.dp).testTag("export_close")) {LineIcon(Glyph.BACK);Spacer(Modifier.width(6.dp));Text(stringResource(R.string.nav_back))}
            Column(Modifier.weight(1f).padding(start=12.dp)) {
                Text(stringResource(R.string.workflow_step,4),color=Aqua,style=MaterialTheme.typography.labelSmall)
                Text(stringResource(R.string.export_title),style=MaterialTheme.typography.titleLarge)
            }
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
                val previewLabel=stringResource(R.string.export_preview_position)
                Slider(previewProgress,{previewProgress=it},enabled=!working,modifier=Modifier.testTag("export_preview_progress").semantics{contentDescription=previewLabel})
            }
            Text(stringResource(R.string.export_format_label),style=MaterialTheme.typography.titleSmall,modifier=Modifier.fillMaxWidth())
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                for (format in ExportFormat.entries) ChoiceButton(format.name,format==spec.format,{model.configure(spec.copy(format=format))},
                    Modifier.weight(1f).testTag("format_${format.name.lowercase()}"),!working)
            }
            Text(stringResource(R.string.export_format_help),color=Muted,style=MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.export_frame_label),style=MaterialTheme.typography.titleSmall,modifier=Modifier.fillMaxWidth())
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                for (frame in ExportFrame.entries) ChoiceButton(if(frame==ExportFrame.SQUARE) "1:1" else "9:16",frame==spec.frame,{model.configure(spec.copy(frame=frame))},
                    Modifier.weight(1f).testTag("frame_${frame.name.lowercase()}"),!working)
            }
            if (spec.format == ExportFormat.PNG) {
                Text(stringResource(R.string.export_png_label),style=MaterialTheme.typography.titleSmall,modifier=Modifier.fillMaxWidth())
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    for (size in listOf(720,1080)) ChoiceButton("${size} px",spec.pngSize==size,{model.configure(spec.copy(pngSize=size))},Modifier.weight(1f),!working)
                }
            } else {
                Text(stringResource(R.string.export_duration_label),style=MaterialTheme.typography.titleSmall,modifier=Modifier.fillMaxWidth())
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    for (seconds in listOf(6,10)) ChoiceButton(stringResource(R.string.export_seconds,seconds),spec.seconds==seconds,{model.configure(spec.copy(seconds=seconds))},
                        Modifier.weight(1f).testTag("duration_$seconds"),!working)
                }
                Text(stringResource(R.string.export_timing),style=MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick=model::fit,enabled=!working && snapshot.viewport!=PreviewViewport(),modifier=Modifier.fillMaxWidth()) { Text(stringResource(R.string.fit_drawing)) }
            Text(stringResource(R.string.export_style),style=MaterialTheme.typography.bodySmall)
            Button(onClick=model::start,enabled=!working,modifier=Modifier.fillMaxWidth().testTag("export_start")) { Text(stringResource(R.string.export_create)) }
            if (model.notice!=0) StatusMessage(stringResource(model.notice),modifier=Modifier.testTag("export_notice"))
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
        if (working) {
            LinearProgressIndicator(progress={model.progress},modifier=Modifier.fillMaxWidth().testTag("export_progress"))
            Text(stringResource(if(model.saving) R.string.export_saving else R.string.export_rendering,(model.progress*100).roundToInt()))
            OutlinedButton(onClick=model::cancel,modifier=Modifier.fillMaxWidth().testTag("export_cancel")) { Text(stringResource(R.string.export_cancel)) }
        }
    }
}
