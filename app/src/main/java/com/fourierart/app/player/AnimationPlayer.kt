package com.fourierart.app.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.fourierart.app.ui.ChoiceButton
import com.fourierart.app.R
import com.fourierart.app.core.*
import com.fourierart.app.photo.PhotoSessionModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.roundToInt

@Composable
fun AnimationPlayer(model: PhotoSessionModel,expanded: Boolean=false,onExpand: ()->Unit={},showParameters: Boolean=true) {
    val drawing=model.drawing ?: return
    val owner=LocalLifecycleOwner.current
    DisposableEffect(owner,model) {
        val observer=LifecycleEventObserver {_,event ->if(event==Lifecycle.Event.ON_PAUSE) model.checkpointAnimation()}
        owner.lifecycle.addObserver(observer)
        onDispose {owner.lifecycle.removeObserver(observer)}
    }
    if(expanded) BackHandler {model.checkpointAnimation();onExpand()}
    Column(if(expanded) Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp) else Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        if(expanded) Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.proof_animation),modifier=Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick={model.checkpointAnimation();onExpand()},modifier=Modifier.testTag("player_close")) {Text(stringResource(R.string.close_player))}
        }
        PlayerCanvas(model,drawing,expanded,if(expanded) Modifier.fillMaxWidth().weight(1f) else Modifier.fillMaxWidth().height(300.dp))
        if(expanded) {
            val height=(LocalConfiguration.current.screenHeightDp*.36f).dp
            Column(Modifier.heightIn(max=height).verticalScroll(rememberScrollState())) {PlayerControls(model,drawing,expanded,onExpand,showParameters)}
        } else PlayerControls(model,drawing,expanded,onExpand,showParameters)
    }
}

private data class PreparedStroke(val points: List<Point2>,val path: Path,val rotations: List<Harmonic>)

@Composable
private fun PlayerCanvas(model: PhotoSessionModel,drawing: ContourDrawing,expanded: Boolean,modifier: Modifier,fixedProgress: Double?=null) {
    val components=model.components
    var prepared by remember(drawing,components) {mutableStateOf<List<PreparedStroke>>(emptyList())}
    LaunchedEffect(drawing,components) {
        prepared=withContext(Dispatchers.Default) {
            drawing.strokes.map {s ->
                val points=s.trace(components)
                val path=Path().apply {points.forEachIndexed {i,p ->if(i==0) moveTo(p.x.toFloat(),p.y.toFloat()) else lineTo(p.x.toFloat(),p.y.toFloat())}}
                PreparedStroke(points,path,s.curve.harmonics.take(s.curve.effectiveComponents(components)).flatMap {it.rotations()})
            }
        }
    }
    val timeline=remember(drawing) {DrawingTimeline(drawing)}
    val ready=prepared.size==drawing.strokes.size
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val view=LocalView.current
    var visible by remember {mutableStateOf(false)}
    val running=fixedProgress==null && model.animationPlaying && !model.showComplete && !model.loading && ready && visible
    LaunchedEffect(lifecycle,drawing,running) {
        if(running) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var previous: Long?=null
            while(true) withInfiniteAnimationFrameNanos {now ->
                previous?.let {last ->model.tickAnimation(((now-last).coerceAtLeast(0))/1_000_000_000.0)}
                previous=now
            }
        }
    }
    val description=stringResource(R.string.animation_canvas_description)
    Canvas(modifier.clipToBounds().testTag(if(ready) "fourier_ready" else "fourier_preparing").semantics {contentDescription=description}
        .onGloballyPositioned {coordinates ->
            val b=coordinates.boundsInWindow();visible=b.width>1 && b.height>1 && b.bottom>0 && b.top<view.height
        }.pointerInput(expanded) {
            if(expanded) detectTransformGestures {centroid,pan,zoom,_ ->
                model.updateAnimationViewport(model.animationViewport.transform(zoom.toDouble(),pan.x.toDouble()/size.width,pan.y.toDouble()/size.height,
                    centroid.x.toDouble()/size.width,centroid.y.toDouble()/size.height))
            }
        }) {
        val style=model.animationStyle
        val light=style.lightBackground
        drawRect(if(light) Color(0xFFFFFBF2) else Color(0xFF0D1929))
        if(!ready) return@Canvas
        val viewport=model.animationViewport
        val scale=size.minDimension*.43f*viewport.zoom.toFloat()
        val origin=Offset(size.width*(.5+viewport.panX).toFloat(),size.height*(.5+viewport.panY).toFloat())
        val ink=Color(style.color.argb(light))
        val guide=if(light) Color(0xFF005F57) else Color(0xFF65DCD0)
        val progress=fixedProgress ?: if(model.showComplete) 1.0 else PlaybackClock.progress(model.animationPosition)
        val frame=timeline.frame(progress)
        withTransform({translate(origin.x,origin.y);scale(scale,scale,pivot=Offset.Zero)}) {
            val line=Stroke((if(drawing.strokes.size>200) 1.2.dp else 1.5.dp).toPx()/scale,cap=StrokeCap.Round)
            for(i in 0 until frame.completed) drawPath(prepared[i].path,ink,style=line)
            val active=frame.active ?: return@withTransform
            val stroke=drawing.strokes[active];val part=frame.localProgress
            if(part>0) {
                val points=prepared[active].points;val count=(part*points.lastIndex).toInt().coerceIn(0,points.lastIndex)
                val path=Path().apply {
                    moveTo(points[0].x.toFloat(),points[0].y.toFloat())
                    for(i in 1..count) lineTo(points[i].x.toFloat(),points[i].y.toFloat())
                    val tip=stroke.position(part,components);lineTo(tip.x.toFloat(),tip.y.toFloat())
                }
                drawPath(path,ink,style=line)
            }
            if(style.epicircles) {
                val t=2*PI*part*stroke.visibleFraction
                var pos=Point2(0.0,0.0)
                for(h in prepared[active].rotations) {
                    val point=h.at(t);val dest=Point2(pos.x+point.x,pos.y+point.y)
                    if(h.frequency!=0) {
                        if(h.radius*scale>1.2) drawCircle(guide.copy(alpha=.35f),h.radius.toFloat(),Offset(pos.x.toFloat(),pos.y.toFloat()),style=Stroke(.7.dp.toPx()/scale))
                        drawLine(guide.copy(alpha=.65f),Offset(pos.x.toFloat(),pos.y.toFloat()),Offset(dest.x.toFloat(),dest.y.toFloat()),.8.dp.toPx()/scale)
                    }
                    pos=dest
                }
                drawCircle(ink,3.dp.toPx()/scale,Offset(pos.x.toFloat(),pos.y.toFloat()))
            }
        }
    }
}

@Composable
private fun PlaybackProgress(model: PhotoSessionModel) {
    val progress=if(model.showComplete) 1.0 else PlaybackClock.progress(model.animationPosition)
    val progressLabel=stringResource(R.string.animation_progress,(progress*100).roundToInt())
    Text(progressLabel,style=MaterialTheme.typography.bodySmall)
    Slider(value=progress.toFloat(),onValueChange={model.seekAnimation(it.toDouble())},valueRange=0f..1f,modifier=Modifier.testTag("player_progress").semantics{contentDescription=progressLabel})
}

@Composable
private fun PlayerControls(model: PhotoSessionModel,drawing: ContourDrawing,expanded: Boolean,onExpand: ()->Unit,showParameters: Boolean) {
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        PlaybackProgress(model)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick=model::toggleAnimation,modifier=Modifier.weight(1f).testTag("player_toggle")) {
                Text(stringResource(if(model.animationPlaying && !model.showComplete) R.string.pause_animation else R.string.resume_animation))
            }
            OutlinedButton(onClick=model::restartAnimation,modifier=Modifier.weight(1f).testTag("player_restart")) {Text(stringResource(R.string.restart_animation))}
        }
        OutlinedButton(onClick={model.showComplete=!model.showComplete;model.settingsChanged(false)},modifier=Modifier.fillMaxWidth().testTag("fourier_complete")) {
            Text(stringResource(if(model.showComplete) R.string.show_animation else R.string.show_complete))
        }
        if(showParameters) AnimationParameters(model)
        if(!expanded) OutlinedButton(onClick=onExpand,modifier=Modifier.fillMaxWidth().testTag("player_expand")) {Text(stringResource(R.string.expand_player))}
        Text(stringResource(if(expanded) R.string.player_gesture_help else R.string.player_help),style=MaterialTheme.typography.bodySmall)
    }
}

/** Static, complete artwork for checking parameters without advancing playback. */
@Composable
fun AnimationStill(model: PhotoSessionModel,modifier: Modifier) {
    val drawing=model.drawing ?: return
    PlayerCanvas(model,drawing,false,modifier,1.0)
}

@Composable
fun AnimationParameters(model: PhotoSessionModel) {
    val drawing=model.drawing ?: return
    val circleLabel=stringResource(R.string.show_epicircles)
    val backgroundLabel=stringResource(R.string.light_background)
    val componentsLabel=stringResource(R.string.fourier_components_description)
    val zoomLabel=stringResource(R.string.player_zoom,(model.animationViewport.zoom*100).roundToInt())
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.playback_speed),style=MaterialTheme.typography.labelLarge)
        for(row in PlaybackSpeed.entries.chunked(2)) Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            for(speed in row) ChoiceButton(label=stringResource(when(speed) {
                    PlaybackSpeed.SLOW -> R.string.speed_slow;PlaybackSpeed.NORMAL -> R.string.speed_normal;PlaybackSpeed.FAST -> R.string.speed_fast;PlaybackSpeed.VERY_FAST -> R.string.speed_very_fast
                }),selected=model.animationStyle.speed==speed,onClick={model.updateAnimationStyle(model.animationStyle.copy(speed=speed))},
                modifier=Modifier.weight(1f).testTag("speed_${speed.name.lowercase()}"))
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.show_epicircles),modifier=Modifier.weight(1f).padding(top=12.dp))
            Switch(checked=model.animationStyle.epicircles,onCheckedChange={model.updateAnimationStyle(model.animationStyle.copy(epicircles=it))},modifier=Modifier.testTag("player_epicircles").semantics{contentDescription=circleLabel})
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.light_background),modifier=Modifier.weight(1f).padding(top=12.dp))
            Switch(checked=model.animationStyle.lightBackground,onCheckedChange={model.updateAnimationStyle(model.animationStyle.copy(lightBackground=it))},modifier=Modifier.testTag("player_background").semantics{contentDescription=backgroundLabel})
        }
        Text(stringResource(R.string.trace_color),style=MaterialTheme.typography.labelLarge)
        for(row in TraceColor.entries.chunked(2)) Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            for(color in row) ChoiceButton(label=stringResource(when(color) {
                    TraceColor.INK -> R.string.color_ink;TraceColor.TEAL -> R.string.color_teal;TraceColor.GOLD -> R.string.color_gold;TraceColor.ROSE -> R.string.color_rose
                }),selected=model.animationStyle.color==color,onClick={model.updateAnimationStyle(model.animationStyle.copy(color=color))},
                modifier=Modifier.weight(1f).testTag("color_${color.name.lowercase()}"))
        }
        Text(stringResource(R.string.components_label,model.components))
        val options=listOf(10,20,50,100,200,400,800)
        val counts=drawing.strokes.map {it.curve.effectiveComponents(model.components)}
        Text(stringResource(R.string.effective_components,drawing.strokes.size,counts.min(),counts.max()),style=MaterialTheme.typography.bodySmall,modifier=Modifier.testTag("effective_components"))
        Slider(value=options.indexOf(model.components).coerceAtLeast(0).toFloat(),onValueChange={model.components=options[it.roundToInt().coerceIn(0,options.lastIndex)];model.settingsChanged(false)},
            valueRange=0f..options.lastIndex.toFloat(),steps=options.size-2,modifier=Modifier.testTag("fourier_components").semantics{contentDescription=componentsLabel;stateDescription=model.components.toString()})
        Text(stringResource(R.string.player_zoom,(model.animationViewport.zoom*100).roundToInt()))
        Slider(value=model.animationViewport.zoom.toFloat(),onValueChange={model.updateAnimationViewport(model.animationViewport.transform(it/model.animationViewport.zoom,0.0,0.0))},valueRange=1f..4f,modifier=Modifier.testTag("player_zoom").semantics{contentDescription=zoomLabel})
        OutlinedButton(onClick={model.updateAnimationViewport(PreviewViewport())},enabled=model.animationViewport!=PreviewViewport(),modifier=Modifier.fillMaxWidth().testTag("player_fit")) {Text(stringResource(R.string.fit_drawing))}
    }
}
