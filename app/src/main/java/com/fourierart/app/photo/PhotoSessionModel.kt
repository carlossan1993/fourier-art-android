package com.fourierart.app.photo

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fourierart.app.R
import com.fourierart.app.core.*
import com.fourierart.app.contours.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.File

class PhotoSessionModel(app: Application): AndroidViewModel(app) {
    var drawing by mutableStateOf<ContourDrawing?>(ContourDrawing.sample(FourierCurve.star()))
    var showComplete by mutableStateOf(false)
    var mode by mutableStateOf(PhotoContours.Mode.AUTO)
    var outcome by mutableStateOf(PhotoContours.Kind.NO_SUBJECT)
    var image by mutableStateOf<Bitmap?>(null); private set
    var source by mutableStateOf<Bitmap?>(null); private set
    var editorPreview by mutableStateOf<Bitmap?>(null); private set
    var draft by mutableStateOf(PhotoPreparation()); private set
    var applied by mutableStateOf<PhotoPreparation?>(null); private set
    var editing by mutableStateOf(false); private set
    var components by mutableIntStateOf(400)
    var lineStyle by mutableStateOf(LineStyle.STRUCTURE)
    var detail by mutableStateOf(DetailLevel.BALANCED)
    var sensitivity by mutableIntStateOf(50)
    var loading by mutableStateOf(true); private set
    var result by mutableIntStateOf(0)
    var notice by mutableIntStateOf(0)
    var analysis by mutableStateOf<PhotoContours.Analysis?>(null); private set
    var correcting by mutableStateOf(false); private set
    var correction by mutableStateOf(ContourRecipe()); private set
    var correctionHistory by mutableStateOf(CorrectionHistory()); private set
    var correctionPreview by mutableStateOf<List<ContourStroke>>(emptyList()); private set
    private var detectedStrokes: List<ContourStroke> = emptyList()
    private var silhouetteStrokes: List<ContourStroke> = emptyList()
    private var detectedKind=PhotoContours.Kind.NO_SUBJECT
    private var analyzedMode: PhotoContours.Mode?=null
    private var sourceFile: String?=null
    private val store=PhotoSessionStore(app)
    private val disk=Mutex()
    private var operation: Job?=null
    private var saveJob: Job?=null
    private var generation=0
    val captures=File(app.cacheDir,"captures").apply {mkdirs()}

    init {
        captures.listFiles()?.forEach {it.delete()}
        operate {
            val saved=withContext(Dispatchers.IO) {disk.withLock {store.read()}}
            if(saved!=null) {
                source=saved.source;sourceFile=saved.file;draft=saved.draft;applied=saved.applied;editing=saved.editing
                readOptions(saved.options)
                editorPreview=withContext(Dispatchers.Default) {PhotoIO.rotate(saved.source,draft.turns)}
                image=saved.applied?.let {p -> withContext(Dispatchers.Default) {PhotoIO.prepare(saved.source,p)}}
                drawing=null;result=5
                if(!editing && image!=null) process(image!!)
            } else withContext(Dispatchers.IO) {disk.withLock {store.clear()}}
        }
    }

    private fun operate(block: suspend ()->Unit) {
        operation?.cancel();val ticket=++generation
        loading=true;notice=0
        operation=viewModelScope.launch {
            try {block()} catch(e:CancellationException) {throw e}
            catch(_:OutOfMemoryError) {notice=R.string.photo_memory_error}
            catch(_:Exception) {notice=R.string.outcome_error}
            finally {if(ticket==generation) loading=false}
        }
    }
    private fun options()=JSONObject().put("mode",mode.name).put("style",lineStyle.name).put("detail",detail.name)
        .put("components",components).put("sensitivity",sensitivity).put("complete",showComplete)
        .put("correction",ContourRecipeJson.encode(correction))
    private fun readOptions(o: JSONObject) {
        correction=ContourRecipeJson.decode(o.optJSONObject("correction"))
        mode=PhotoContours.Mode.entries.firstOrNull {it.name==o.optString("mode")} ?: PhotoContours.Mode.AUTO
        lineStyle=LineStyle.entries.firstOrNull {it.name==o.optString("style")} ?: LineStyle.STRUCTURE
        detail=DetailLevel.entries.firstOrNull {it.name==o.optString("detail")} ?: DetailLevel.BALANCED
        components=o.optInt("components",400).takeIf {it in listOf(10,20,50,100,200,400,800)} ?: 400
        sensitivity=o.optInt("sensitivity",50).coerceIn(0,100);showComplete=o.optBoolean("complete",false)
    }
    private suspend fun persist(file: String?=sourceFile, prep: PhotoPreparation?=applied,
        work: PhotoPreparation=draft, edit: Boolean=editing) {
        val name=file ?: return
        val settings=options()
        withContext(Dispatchers.IO) {disk.withLock {ensureActive();store.write(name,prep,work,edit,settings)}}
    }
    fun settingsChanged(clear: Boolean=true) {
        if(clear) {drawing=null;result=5}
        saveJob?.cancel();saveJob=viewModelScope.launch {delay(180);try {persist()} catch(e:CancellationException){throw e} catch(_:Exception){notice=R.string.photo_save_error}}
    }
    fun importPhoto(uri: Uri, temporary: File?=null) {
        saveJob?.cancel()
        operate {
            try {
                val bitmap=withContext(Dispatchers.IO) {PhotoIO.decode(getApplication(),uri)}
                val settings=options().put("correction",ContourRecipeJson.encode(ContourRecipe()))
                val file=withContext(Dispatchers.IO) {disk.withLock {
                    ensureActive();val name=store.saveSource(bitmap)
                    store.write(name,null,PhotoPreparation(),true,settings);name
                }}
                clearCorrection();source=bitmap;sourceFile=file;editorPreview=bitmap;draft=PhotoPreparation();applied=null;editing=true
                image=null;analysis=null;analyzedMode=null;drawing=null;result=5
            } finally {temporary?.delete()}
        }
    }
    fun setCrop(crop: CropBox) {draft=draft.copy(crop=crop);settingsChanged(false)}
    fun rotate(right: Boolean) {
        val original=source ?: return
        val next=if(right) draft.clockwise() else draft.counterclockwise()
        saveJob?.cancel()
        operate {val preview=withContext(Dispatchers.Default) {PhotoIO.rotate(original,next.turns)}
            draft=next;editorPreview=preview;persist()}
    }
    fun resetOriginal() {draft=PhotoPreparation();editorPreview=source;settingsChanged(false)}
    fun editPhoto() {
        val original=source ?: return;val prep=applied ?: draft
        operate {editorPreview=withContext(Dispatchers.Default) {PhotoIO.rotate(original,prep.turns)}
            draft=prep;editing=true;persist()}
    }
    fun cancelEdit() {
        if(applied==null) {sample(false);return}
        draft=applied!!;editing=false
        val bitmap=image
        if(bitmap!=null && analysis==null) operate {process(bitmap);persist()} else settingsChanged(false)
    }
    fun applyPhoto() {
        val original=source ?: return;val prep=draft
        saveJob?.cancel()
        operate {
            val prepared=withContext(Dispatchers.Default) {PhotoIO.prepare(original,prep)}
            clearCorrection()
            persist(prep=prep,work=prep,edit=false)
            image=prepared;applied=prep;editing=false;analysis=null;analyzedMode=null;drawing=null;result=5
            process(prepared)
        }
    }
    fun reprocess() {val bitmap=image ?: return;operate {process(bitmap);persist()}}
    private suspend fun process(bitmap: Bitmap) {
        val old=analysis?.takeIf {analyzedMode==mode && !it.modelUnavailable}
        val selectedMode=mode;val selectedSensitivity=sensitivity;val selectedDetail=detail;val selectedStyle=lineStyle
        try {
            val processed=withContext(Dispatchers.Default) {
                val cached=old ?: PhotoContours.analyze(bitmap,selectedMode)
                cached to PhotoContours.extract(cached,selectedMode,selectedSensitivity,selectedDetail,selectedStyle)
            }
            analysis=processed.first;analyzedMode=selectedMode;outcome=processed.second.kind;detectedKind=outcome
            detectedStrokes=processed.second.drawing?.sourceStrokes.orEmpty();silhouetteStrokes=processed.second.silhouette
            val rendered=if(correction==ContourRecipe()) detectedStrokes to processed.second.drawing
                else withContext(Dispatchers.Default) {renderCorrection(correction)}
            drawing=rendered.second;correctionPreview=rendered.first
            result=if(drawing!=null) 1 else if(correction!=ContourRecipe()) 6 else 2
        } catch(e:CancellationException) {throw e} catch(_:Exception) {drawing=null;result=4}
    }
    private fun clearCorrection() {
        correcting=false;correction=ContourRecipe();correctionHistory=CorrectionHistory();correctionPreview=emptyList()
        detectedStrokes=emptyList();silhouetteStrokes=emptyList()
    }
    private fun previewCorrection(recipe: ContourRecipe): List<ContourStroke> {
        val a=analysis ?: return emptyList()
        return ContourCorrection.apply(detectedStrokes,silhouetteStrokes,a.width,a.height,recipe)
    }
    private fun renderCorrection(recipe: ContourRecipe): Pair<List<ContourStroke>,ContourDrawing?> {
        val strokes=previewCorrection(recipe)
        return strokes to if(strokes.isEmpty()) null else ContourDrawing.fromStrokes(strokes)
    }
    fun openCorrection() {
        if(image==null || analysis==null || loading) return
        correctionHistory=CorrectionHistory(correction);correcting=true
        // Geometry already matches the applied correction, including an intentionally empty result.
    }
    private fun changeCorrection(history: CorrectionHistory) {
        if(!correcting) return
        operate {
            val strokes=withContext(Dispatchers.Default) {previewCorrection(history.current)}
            correctionHistory=history;correctionPreview=strokes
        }
    }
    fun setContourPreset(preset: ContourPreset)=changeCorrection(correctionHistory.change(correctionHistory.current.copy(preset=preset)))
    fun setContourCleanup(value: Int)=changeCorrection(correctionHistory.change(correctionHistory.current.copy(cleanup=value.coerceIn(0,100))))
    fun addContourEdit(edit: ContourEdit) {
        if(correctionHistory.current.edits.size>=100) {notice=R.string.correction_limit;return}
        changeCorrection(correctionHistory.change(correctionHistory.current.append(edit)))
    }
    fun undoContour()=changeCorrection(correctionHistory.undo())
    fun redoContour()=changeCorrection(correctionHistory.redo())
    fun resetContour()=changeCorrection(correctionHistory.change(ContourRecipe()))
    fun cancelCorrection() {
        if(loading) return
        correcting=false
        operate {correctionPreview=withContext(Dispatchers.Default) {previewCorrection(correction)}}
    }
    fun applyCorrection() {
        val recipe=correctionHistory.current
        operate {
            val rendered=withContext(Dispatchers.Default) {renderCorrection(recipe)}
            // Commit only after durable metadata succeeds; a failed save leaves the editor open.
            val previous=correction;correction=recipe
            try {persist()} catch(e:Exception) {correction=previous;throw e}
            correctionPreview=rendered.first;drawing=rendered.second;outcome=detectedKind
            result=if(drawing!=null) 1 else if(recipe==ContourRecipe()) 2 else 6
            correcting=false
        }
    }
    fun sample(circle: Boolean) {
        saveJob?.cancel()
        operate {
            withContext(Dispatchers.IO) {disk.withLock {ensureActive();store.clear();captures.listFiles()?.forEach {it.delete()}}}
            clearCorrection();sourceFile=null;source=null;image=null;editorPreview=null;applied=null;draft=PhotoPreparation();editing=false
            analysis=null;analyzedMode=null;result=0;showComplete=false
            drawing=ContourDrawing.sample(if(circle) FourierCurve.circle() else FourierCurve.star())
        }
    }
}
