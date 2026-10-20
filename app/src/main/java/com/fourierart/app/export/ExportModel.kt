package com.fourierart.app.export

import android.app.Application
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.runtime.*
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fourierart.app.R
import com.fourierart.app.core.*
import kotlinx.coroutines.*
import java.io.File

class ExportModel(app: Application) : AndroidViewModel(app) {
    var snapshot by mutableStateOf<ExportSnapshot?>(null); private set
    var spec by mutableStateOf(ExportSpec()); private set
    var completed by mutableStateOf<File?>(null); private set
    var completedSpec by mutableStateOf<ExportSpec?>(null); private set
    var busy by mutableStateOf(false); private set
    var saving by mutableStateOf(false); private set
    var progress by mutableFloatStateOf(0f); private set
    var notice by mutableIntStateOf(0); private set
    private var job: Job? = null
    init { viewModelScope.launch(Dispatchers.IO) { runCatching { ExportEngine.clearStale(app) } } }
    fun open(value: ExportSnapshot) { if (busy || saving) return; snapshot = value; completed = null; completedSpec = null; notice = 0 }
    fun configure(value: ExportSpec) { if (!busy && !saving) { spec = value; notice = 0 } }
    fun fit() { if (!busy && !saving) snapshot = snapshot?.copy(viewport = PreviewViewport()) }
    fun start() {
        val source = snapshot ?: return
        if (busy || saving) return
        val options = spec
        notice = 0; progress = 0f; busy = true
        job = viewModelScope.launch {
            try {
                val result = ExportEngine.render(getApplication(),source,options) { value ->
                    // A small number of frame updates, delivered on the UI dispatcher.
                    viewModelScope.launch(Dispatchers.Main.immediate) { progress = value }
                }
                completed = result; completedSpec = options; notice = R.string.export_ready
            } catch (e: CancellationException) { notice = R.string.export_cancelled; throw e }
            catch (e: Exception) { notice = R.string.export_failed }
            catch (e: OutOfMemoryError) { notice = R.string.export_memory }
            finally { busy = false }
        }
    }
    fun cancel() { job?.cancel() }
    fun save(uri: Uri) {
        val file = completed ?: return
        if (busy || saving) return
        notice = 0; progress = 0f; saving = true
        job = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    var success = false
                    try {
                        resolver.openOutputStream(uri,"w").use { output ->
                            checkNotNull(output)
                            file.inputStream().use { input ->
                                val buffer = ByteArray(64 * 1024); var copied = 0L
                                while (true) {
                                    ensureActive(); val count = input.read(buffer); if (count < 0) break
                                    output.write(buffer,0,count); copied += count
                                    val value = copied.toFloat() / file.length().coerceAtLeast(1)
                                    withContext(Dispatchers.Main) { progress = value }
                                }
                            }
                            output.flush()
                        }
                        ensureActive(); success = true
                    } finally {
                        if (!success) runCatching { DocumentsContract.deleteDocument(resolver,uri) }
                    }
                }
                notice = R.string.export_saved
            } catch (e: CancellationException) { notice = R.string.export_cancelled; throw e }
            catch (e: Exception) { notice = R.string.export_save_failed }
            finally { saving = false }
        }
    }
    fun shareIntent(context: Context): Intent? {
        val file = completed ?: return null
        val options = completedSpec ?: return null
        val uri = FileProvider.getUriForFile(context,"${context.packageName}.exports",file)
        return Intent(Intent.ACTION_SEND).apply {
            type = options.format.mime; putExtra(Intent.EXTRA_STREAM,uri)
            clipData = ClipData.newUri(context.contentResolver,file.name,uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    fun shareFailed() { notice = R.string.export_share_failed }
}
