package com.fourierart.app.contours

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.fourierart.app.R
import com.fourierart.app.core.Point2
import com.fourierart.app.photo.PhotoSessionModel
import kotlin.math.*

private enum class Tool { KEEP, ERASE, DRAW, MOVE }
private val ink=Color(0xFF65DCD0)

/** A full-screen workspace keeps drawing gestures independent of page scrolling. */
@Composable
fun ContourEditor(model: PhotoSessionModel) {
    val bitmap=model.image ?: return
    val raster=model.analysis ?: return
    val photo=remember(bitmap) {bitmap.asImageBitmap()}
    var tool by rememberSaveable {mutableStateOf(Tool.ERASE)}
    var zoom by rememberSaveable {mutableFloatStateOf(1f)}
    var panX by rememberSaveable {mutableFloatStateOf(0f)}
    var panY by rememberSaveable {mutableFloatStateOf(0f)}
    var radius by rememberSaveable {mutableFloatStateOf(.025f)}
    var showPhoto by rememberSaveable {mutableStateOf(true)}
    var gesture by remember {mutableStateOf<List<Point2>>(emptyList())}
    val history=model.correctionHistory
    val description=stringResource(R.string.contour_canvas_description)
    fun finish() {
        val points=gesture;gesture=emptyList()
        if(points.isEmpty()) return
        when(tool) {
            Tool.ERASE -> model.addContourEdit(ContourEdit.Erase(points,radius.toDouble()))
            Tool.DRAW -> if(points.size>=2) model.addContourEdit(ContourEdit.Draw(points))
            Tool.KEEP -> {
                val first=points.first();val last=points.last()
                val l=min(first.x,last.x);val r=max(first.x,last.x);val t=min(first.y,last.y);val b=max(first.y,last.y)
                if(r-l>.01 && b-t>.01) model.addContourEdit(ContourEdit.Keep(l,t,r,b))
            }
            Tool.MOVE -> Unit
        }
    }
    BackHandler {if(!model.loading) model.cancelCorrection()}
    Column(Modifier.fillMaxSize().background(Color(0xFF0D1929)).safeDrawingPadding().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.correct_contours),style=MaterialTheme.typography.titleLarge)
        Text(stringResource(when(tool) {
            Tool.KEEP -> R.string.keep_help;Tool.ERASE -> R.string.erase_help
            Tool.DRAW -> R.string.draw_help;Tool.MOVE -> R.string.move_help
        }),style=MaterialTheme.typography.bodySmall)
        Canvas(Modifier.fillMaxWidth().weight(1f).clipToBounds().testTag("contour_canvas").semantics {contentDescription=description}
            .pointerInput(bitmap,tool,zoom,model.loading) {
                if(model.loading) return@pointerInput
                awaitEachGesture {
                    val down=awaitFirstDown(requireUnconsumed=false)
                    val scale=min(size.width.toFloat()/bitmap.width,size.height.toFloat()/bitmap.height)*zoom
                    val w=bitmap.width*scale;val h=bitmap.height*scale
                    val left=(size.width-w)/2+panX;val top=(size.height-h)/2+panY
                    fun mapped(p: Offset)=Point2(((p.x-left)/w).toDouble().coerceIn(0.0,1.0),((p.y-top)/h).toDouble().coerceIn(0.0,1.0))
                    val valid=down.position.x in left..left+w && down.position.y in top..top+h
                    var previous=down.position
                    if(valid) {gesture=listOf(mapped(down.position));down.consume()}
                    var cancelled=false
                    try {
                        do {
                            val event=awaitPointerEvent();val change=event.changes.firstOrNull {it.id==down.id} ?: break
                            if(event.changes.count {it.pressed}>1) {cancelled=true;gesture=emptyList()}
                            if(valid && !cancelled) {
                                if(tool==Tool.MOVE) {
                                    val delta=change.position-previous
                                    panX=(panX+delta.x).coerceIn(-max(0f,(w-size.width)/2),max(0f,(w-size.width)/2))
                                    panY=(panY+delta.y).coerceIn(-max(0f,(h-size.height)/2),max(0f,(h-size.height)/2))
                                } else {
                                    val point=mapped(change.position)
                                    val last=gesture.lastOrNull()
                                    if(last==null || hypot((point.x-last.x)*w,(point.y-last.y)*h)>=1.5 || !change.pressed) {
                                        var list=gesture
                                        if(list.size>=510) list=list.filterIndexed {i,_->i%2==0}
                                        gesture=list+point
                                    }
                                }
                                previous=change.position;change.consume()
                            }
                        } while(change.pressed)
                        if(valid && !cancelled) finish() else gesture=emptyList()
                    } finally {gesture=emptyList()}
                }
            }) {
            drawRect(Color(0xFF111F30))
            val scale=min(size.width/bitmap.width,size.height/bitmap.height)*zoom
            val w=bitmap.width*scale;val h=bitmap.height*scale
            val left=(size.width-w)/2+panX;val top=(size.height-h)/2+panY
            if(showPhoto) drawImage(photo,dstOffset=IntOffset(left.roundToInt(),top.roundToInt()),dstSize=IntSize(w.roundToInt(),h.roundToInt()),alpha=.65f)
            fun xy(p: Point2)=Offset(left+p.x.toFloat()*w,top+p.y.toFloat()*h)
            for(s in model.correctionPreview) {
                val path=Path()
                s.points.forEachIndexed {i,p ->val q=xy(Point2(p.x/raster.width,p.y/raster.height));if(i==0) path.moveTo(q.x,q.y) else path.lineTo(q.x,q.y)}
                if(s.closed) path.close()
                drawPath(path,ink,style=Stroke(1.7.dp.toPx(),cap=StrokeCap.Round))
            }
            if(gesture.isNotEmpty() && tool!=Tool.MOVE) {
                if(tool==Tool.KEEP) {
                    val a=xy(gesture.first());val b=xy(gesture.last())
                    drawRect(ink.copy(alpha=.15f),Offset(min(a.x,b.x),min(a.y,b.y)),Size(abs(b.x-a.x),abs(b.y-a.y)))
                    drawRect(ink,Offset(min(a.x,b.x),min(a.y,b.y)),Size(abs(b.x-a.x),abs(b.y-a.y)),style=Stroke(2.dp.toPx()))
                } else {
                    val path=Path().apply {gesture.forEachIndexed {i,p ->val q=xy(p);if(i==0) moveTo(q.x,q.y) else lineTo(q.x,q.y)}}
                    val erasing=tool==Tool.ERASE
                    drawPath(path,if(erasing) Color.Red.copy(alpha=.45f) else Color.White,
                        style=Stroke(if(erasing) 2*radius*min(w,h) else 2.dp.toPx(),cap=StrokeCap.Round))
                    if(erasing) drawCircle(Color.Red.copy(alpha=.5f),radius*min(w,h),xy(gesture.last()),style=Stroke(1.dp.toPx()))
                }
            }
        }
        if(model.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        Column(Modifier.fillMaxWidth().heightIn(max=230.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                for(preset in ContourPreset.entries) OutlinedButton(onClick={model.setContourPreset(preset)},enabled=!model.loading && history.current.preset!=preset,
                    modifier=Modifier.weight(1f).testTag("preset_${preset.name.lowercase()}")) {
                    Text(stringResource(if(preset==ContourPreset.DETAILS) R.string.contour_details else R.string.contour_silhouette))
                }
            }
            Text(stringResource(if(history.current.edits.isEmpty()) R.string.contour_detected else R.string.contour_custom,model.correctionPreview.size),style=MaterialTheme.typography.bodySmall,modifier=Modifier.testTag("contour_count"))
            if(history.current.preset==ContourPreset.SILHOUETTE && model.correctionPreview.isEmpty()) Text(stringResource(R.string.no_silhouette),style=MaterialTheme.typography.bodySmall)
            for(pair in Tool.entries.chunked(2)) Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                for(value in pair) OutlinedButton(onClick={tool=value},enabled=!model.loading && tool!=value,
                    modifier=Modifier.weight(1f).testTag("tool_${value.name.lowercase()}")) {Text(stringResource(when(value) {
                        Tool.KEEP -> R.string.keep_subject;Tool.ERASE -> R.string.erase_segments;Tool.DRAW -> R.string.draw_lines;Tool.MOVE -> R.string.move_canvas
                    }))}
            }
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick=model::undoContour,enabled=!model.loading && history.past.isNotEmpty(),modifier=Modifier.weight(1f).testTag("contour_undo")) {Text(stringResource(R.string.undo))}
                OutlinedButton(onClick=model::redoContour,enabled=!model.loading && history.future.isNotEmpty(),modifier=Modifier.weight(1f).testTag("contour_redo")) {Text(stringResource(R.string.redo))}
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.show_photo),modifier=Modifier.weight(1f).padding(top=12.dp))
                Switch(checked=showPhoto,onCheckedChange={showPhoto=it},modifier=Modifier.testTag("show_photo"))
            }
            Text(stringResource(R.string.contour_zoom,(zoom*100).roundToInt()),style=MaterialTheme.typography.bodySmall)
            Slider(value=zoom,onValueChange={zoom=it;panX=0f;panY=0f},valueRange=1f..4f,modifier=Modifier.testTag("contour_zoom"),enabled=!model.loading)
            if(tool==Tool.ERASE) {
                Text(stringResource(R.string.eraser_size),style=MaterialTheme.typography.bodySmall)
                Slider(value=radius,onValueChange={radius=it},valueRange=0.005f..0.08f,modifier=Modifier.testTag("eraser_size"),enabled=!model.loading)
            }
            var cleanup by remember(history.current.cleanup) {mutableFloatStateOf(history.current.cleanup.toFloat())}
            Text(stringResource(R.string.small_strokes,cleanup.roundToInt()),style=MaterialTheme.typography.bodySmall)
            Slider(value=cleanup,onValueChange={cleanup=it},onValueChangeFinished={model.setContourCleanup(cleanup.roundToInt())},valueRange=0f..100f,enabled=!model.loading,modifier=Modifier.testTag("small_strokes"))
            OutlinedButton(onClick=model::resetContour,enabled=!model.loading && history.current!=ContourRecipe(),modifier=Modifier.fillMaxWidth().testTag("contour_reset")) {Text(stringResource(R.string.restore_detection))}
            Text(stringResource(R.string.correction_persistence),style=MaterialTheme.typography.bodySmall)
        }
        if(model.notice!=0) Text(stringResource(model.notice),color=Color(0xFFFFD18A))
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick=model::cancelCorrection,enabled=!model.loading,modifier=Modifier.weight(1f).testTag("contour_cancel")) {Text(stringResource(R.string.cancel))}
            Button(onClick=model::applyCorrection,enabled=!model.loading,modifier=Modifier.weight(1f).testTag("contour_apply")) {Text(stringResource(R.string.apply_contours))}
        }
    }
}
