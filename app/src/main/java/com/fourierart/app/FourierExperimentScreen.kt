package com.fourierart.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.fourierart.app.contours.*
import com.fourierart.app.core.*
import com.fourierart.app.export.*
import com.fourierart.app.photo.*
import com.fourierart.app.player.*
import com.fourierart.app.ui.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private enum class Page { HOME, PREPARE, REVIEW, PARAMETERS, PLAYER, GUIDE, SETTINGS }
private val accent=Aqua

@Composable
fun FourierExperimentScreen(model: PhotoSessionModel, exports: ExportModel) {
    val context=LocalContext.current
    var page by rememberSaveable {mutableStateOf(Page.HOME)}
    var guideReturn by rememberSaveable {mutableStateOf(Page.HOME)}
    var cameraOpen by rememberSaveable {mutableStateOf(false)}
    var exportOpen by rememberSaveable {mutableStateOf(false)}
    var playerExpanded by rememberSaveable {mutableStateOf(false)}
    var pendingSample by rememberSaveable {mutableIntStateOf(-1)}
    var confirmDiscard by rememberSaveable {mutableStateOf(false)}
    var observedWork by rememberSaveable {mutableStateOf("")}
    val workKey=if(model.source==null) "none" else "${model.workingPhotoId}:${model.editing}"
    LaunchedEffect(workKey,model.loading) {
        // A new photo/preparation changes the route; recreation preserves the user's current screen.
        if(!model.loading && observedWork!=workKey) {
            observedWork=workKey
            page=when {
                page==Page.SETTINGS || page==Page.GUIDE -> page
                model.editing && model.source!=null -> Page.PREPARE
                model.image!=null -> Page.REVIEW
                page==Page.REVIEW || page==Page.PREPARE -> Page.HOME
                else -> page
            }
        }
    }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {uri ->if(uri!=null)model.importPhoto(uri)}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {granted ->
        if(granted) {model.notice=0;cameraOpen=true} else model.notice=R.string.camera_denied
    }
    fun choose() {model.notice=0;picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))}
    fun camera() {
        model.notice=0
        if(!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY))model.notice=R.string.camera_error
        else if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)cameraOpen=true
        else permission.launch(Manifest.permission.CAMERA)
    }
    fun home() {model.checkpointAnimation();page=Page.HOME}
    fun demo(circle: Boolean) {
        if(model.source!=null)pendingSample=if(circle)1 else 0
        else {model.sample(circle);page=Page.PLAYER}
    }
    fun discard() {model.sample(false);page=Page.HOME;confirmDiscard=false}
    if(cameraOpen) {
        CameraCapture(model.captures,onCaptured={cameraOpen=false;model.importPhoto(android.net.Uri.fromFile(it),it)},
            onClose={cameraOpen=false},onError={cameraOpen=false;model.notice=R.string.camera_error})
        return
    }
    if(model.correcting) {ContourEditor(model);return}
    if(exportOpen && exports.snapshot!=null) {ExportScreen(exports,onClose={exportOpen=false});return}
    if(playerExpanded && model.drawing!=null) {
        AnimationPlayer(model,expanded=true,onExpand={playerExpanded=false});return
    }
    BackHandler(enabled=page!=Page.HOME) {
        model.checkpointAnimation()
        page=when(page) {Page.PARAMETERS -> Page.PLAYER;Page.PLAYER -> if(model.image!=null)Page.REVIEW else Page.HOME;Page.GUIDE -> guideReturn;else -> Page.HOME}
    }
    when(page) {
        Page.HOME -> HomeScreen(model,onChoose={choose()},onCamera={camera()},onResume={page=if(model.editing)Page.PREPARE else Page.REVIEW},
            onDemo={page=Page.PLAYER},onSample={demo(it)},onGuide={guideReturn=Page.HOME;page=Page.GUIDE},onSettings={page=Page.SETTINGS})
        Page.PREPARE -> WorkflowPage(stringResource(R.string.prepare_title),onBack={home()},step=1,tag="screen_prepare",bottom={
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(model::cancelEdit,Modifier.weight(1f).heightIn(min=52.dp).testTag("editor_cancel"),enabled=!model.loading) {Text(stringResource(if(model.applied==null)R.string.discard_photo else R.string.cancel))}
                ActionButton(stringResource(R.string.prepare_next),model::applyPhoto,Modifier.weight(1f).testTag("apply_photo"),!model.loading)
            }
        }) {
            SessionStatus(model)
            if(model.editorPreview!=null) PhotoEditor(model,showActions=false)
            Text(stringResource(R.string.prepare_reminder),color=Muted,style=MaterialTheme.typography.bodySmall)
        }
        Page.REVIEW -> ReviewScreen(model,onBack={home()},onAnimate={page=Page.PLAYER},onChoose={choose()},onCamera={camera()},
            onDiscard={confirmDiscard=true},onGuide={guideReturn=Page.REVIEW;page=Page.GUIDE})
        Page.PLAYER -> WorkflowPage(stringResource(R.string.animation_title),onBack={model.checkpointAnimation();page=if(model.image!=null)Page.REVIEW else Page.HOME},step=3,tag="screen_player",
            bottom={ActionButton(stringResource(R.string.export_title),onClick={
                model.drawing?.let {drawing ->
                    model.checkpointAnimation();exports.open(ExportSnapshot(drawing,model.components,model.animationStyle,model.animationViewport));exportOpen=true
                }
            },enabled=!model.loading && model.drawing!=null && !exports.busy && !exports.saving,modifier=Modifier.fillMaxWidth().testTag("open_export"))}) {
            SessionStatus(model)
            if(model.drawing!=null) {
                AnimationPlayer(model,onExpand={playerExpanded=true},showParameters=false)
                ActionButton(stringResource(R.string.animation_parameters),onClick={model.checkpointAnimation();page=Page.PARAMETERS},icon=Glyph.SETTINGS,
                    modifier=Modifier.fillMaxWidth().testTag("open_parameters"))
                Text(stringResource(R.string.components_summary,model.components,model.drawing!!.strokes.size),color=Muted,style=MaterialTheme.typography.bodySmall)
                if(model.image!=null) OutlinedButton(onClick={page=Page.REVIEW},modifier=Modifier.fillMaxWidth().testTag("review_photo")) {Text(stringResource(R.string.review_title))}
            } else {
                StatusMessage(stringResource(R.string.no_drawing))
                ActionButton(stringResource(R.string.review_title),onClick={page=Page.REVIEW},modifier=Modifier.fillMaxWidth())
            }
        }
        Page.PARAMETERS -> WorkflowPage(stringResource(R.string.animation_parameters),onBack={page=Page.PLAYER},step=3,tag="screen_parameters",
            bottom={ActionButton(stringResource(R.string.view_animation),onClick={page=Page.PLAYER},icon=Glyph.PLAY,modifier=Modifier.fillMaxWidth().testTag("parameters_done"))}) {
            if(model.drawing!=null) {
                AnimationStill(model,Modifier.fillMaxWidth().height(210.dp))
                AnimationParameters(model)
                Text(stringResource(R.string.proof_explanation),color=Muted,style=MaterialTheme.typography.bodySmall)
            }
        }
        Page.GUIDE -> GuideScreen(onBack={page=guideReturn})
        Page.SETTINGS -> SettingsScreen(model,onBack={home()},onDiscard={confirmDiscard=true})
    }
    if(pendingSample>=0) AlertDialog(onDismissRequest={pendingSample=-1},title={Text(stringResource(R.string.replace_title))},
        text={Text(stringResource(R.string.replace_sample_help))},confirmButton={TextButton(onClick={val circle=pendingSample==1;pendingSample=-1;model.sample(circle);page=Page.PLAYER}) {Text(stringResource(R.string.replace_confirm))}},
        dismissButton={TextButton(onClick={pendingSample=-1}) {Text(stringResource(R.string.cancel))}})
    if(confirmDiscard) AlertDialog(onDismissRequest={confirmDiscard=false},title={Text(stringResource(R.string.discard_title))},text={Text(stringResource(R.string.discard_help))},
        confirmButton={TextButton(onClick={discard()},modifier=Modifier.testTag("confirm_discard")) {Text(stringResource(R.string.discard_photo))}},
        dismissButton={TextButton(onClick={confirmDiscard=false}) {Text(stringResource(R.string.cancel))}})
}

@Composable
private fun SessionStatus(model: PhotoSessionModel) {
    if(model.notice!=0) StatusMessage(stringResource(model.notice),modifier=Modifier.testTag("photo_notice"))
    if(model.loading) StatusMessage(stringResource(if(model.editing)R.string.preparing_photo else R.string.processing_photo),loading=true,modifier=Modifier.testTag("photo_loading"))
}

@Composable
private fun ReviewScreen(model: PhotoSessionModel,onBack: ()->Unit,onAnimate: ()->Unit,onChoose: ()->Unit,onCamera: ()->Unit,onDiscard: ()->Unit,onGuide: ()->Unit) {
    WorkflowPage(stringResource(R.string.review_title),onBack,step=2,tag="screen_review",bottom={
        ActionButton(stringResource(R.string.view_animation),onAnimate,enabled=!model.loading && model.drawing!=null,icon=Glyph.PLAY,modifier=Modifier.fillMaxWidth().testTag("start_animation"))
    }) {
        SessionStatus(model)
        model.image?.let {image ->ContourPreview(image,model.drawing,Modifier.fillMaxWidth().height(280.dp))}
        if(!model.loading) {
            val message=when(model.result) {
                1 -> stringResource(if(model.correction!=ContourRecipe())R.string.contour_applied else when(model.outcome) {
                    PhotoContours.Kind.FACE -> R.string.outcome_face;PhotoContours.Kind.FACE_PARTIAL -> R.string.outcome_face_partial
                    PhotoContours.Kind.SUBJECT -> R.string.outcome_subject;else -> R.string.outcome_drawing
                },model.drawing?.strokes?.size ?: 0)
                2 -> stringResource(if(model.outcome==PhotoContours.Kind.MODEL_UNAVAILABLE)R.string.model_unavailable else R.string.outcome_missing)
                5 -> stringResource(R.string.profile_changed)
                6 -> stringResource(R.string.contour_empty)
                else -> stringResource(R.string.outcome_contour_error)
            }
            StatusMessage(message,modifier=Modifier.testTag("contour_status"))
        }
        ActionButton(stringResource(R.string.correct_contours),model::openCorrection,enabled=!model.loading && model.analysis!=null && model.result!=5,
            modifier=Modifier.fillMaxWidth().testTag("correct_contours"))
        Text(stringResource(R.string.correction_intro),color=Muted,style=MaterialTheme.typography.bodySmall)
        SectionCard(stringResource(R.string.processing_profile)) {
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                for(profile in PhotoContours.Mode.entries)ChoiceButton(stringResource(when(profile){PhotoContours.Mode.AUTO->R.string.profile_auto;PhotoContours.Mode.SUBJECT->R.string.profile_subject;else->R.string.profile_drawing}),
                    model.mode==profile,{model.mode=profile;model.settingsChanged()},Modifier.weight(1f).testTag("profile_${profile.name.lowercase()}"),!model.loading)
            }
            Text(stringResource(R.string.photo_warning),style=MaterialTheme.typography.bodySmall,color=Muted)
            Text(stringResource(R.string.line_style))
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                for(style in LineStyle.entries)ChoiceButton(stringResource(if(style==LineStyle.STRUCTURE)R.string.style_structure else R.string.style_detail),model.lineStyle==style,
                    {model.lineStyle=style;model.settingsChanged()},Modifier.weight(1f),!model.loading)
            }
            Text(stringResource(R.string.detail_label))
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                for(detail in DetailLevel.entries)ChoiceButton(stringResource(when(detail){DetailLevel.CLEAN->R.string.detail_clean;DetailLevel.BALANCED->R.string.detail_balanced;else->R.string.detail_fine}),model.detail==detail,
                    {model.detail=detail;model.settingsChanged()},Modifier.weight(1f),!model.loading)
            }
            Text(stringResource(R.string.detail_help),color=Muted,style=MaterialTheme.typography.bodySmall)
            val sensitivityLabel=stringResource(R.string.sensitivity,model.sensitivity)
            Text(sensitivityLabel)
            Slider(value=model.sensitivity.toFloat(),onValueChange={model.sensitivity=it.roundToInt();model.settingsChanged()},enabled=!model.loading,valueRange=0f..100f,steps=9,
                modifier=Modifier.semantics{contentDescription=sensitivityLabel})
            ActionButton(stringResource(R.string.reprocess),model::reprocess,enabled=!model.loading,modifier=Modifier.fillMaxWidth().testTag("reprocess_photo"))
        }
        SectionCard(stringResource(R.string.photo_options)) {
            OutlinedButton(model::editPhoto,Modifier.fillMaxWidth().testTag("edit_photo"),enabled=!model.loading) {Text(stringResource(R.string.edit_photo))}
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onChoose,Modifier.weight(1f).testTag("choose_photo"),enabled=!model.loading) {Text(stringResource(R.string.choose_photo))}
                OutlinedButton(onCamera,Modifier.weight(1f).testTag("take_photo"),enabled=!model.loading) {Text(stringResource(R.string.take_photo))}
            }
            Text(stringResource(R.string.working_photo_notice),color=Muted,style=MaterialTheme.typography.bodySmall)
            TextButton(onDiscard,enabled=!model.loading,modifier=Modifier.fillMaxWidth().testTag("discard_photo")) {Text(stringResource(R.string.discard_photo))}
        }
        TextButton(onGuide,Modifier.fillMaxWidth().testTag("review_help")) {Text(stringResource(R.string.photo_tips))}
    }
}
@Composable
private fun ContourPreview(bitmap: Bitmap, drawing: ContourDrawing?, modifier: Modifier) {
    val preview = remember(bitmap) { bitmap.asImageBitmap() }
    val description=stringResource(R.string.selected_image)
    Canvas(modifier.semantics{contentDescription=description}) {
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
