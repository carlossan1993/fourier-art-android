package com.fourierart.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fourierart.app.R
import com.fourierart.app.core.FourierCurve
import kotlin.math.PI

val Night = Color(0xFF06131E)
val Aqua = Color(0xFF35D4DE)
val Muted = Color(0xFFAFBFCD)

enum class Glyph { CAMERA, PHOTO, BACK, GUIDE, SETTINGS, PLAY, HOME }

/** Small repo-native line icons; their enclosing labelled controls supply accessibility semantics. */
@Composable
fun LineIcon(kind: Glyph, modifier: Modifier = Modifier, tint: Color = Aqua) {
    Canvas(modifier.size(24.dp)) {
        val s = size.minDimension / 24f
        fun line(x: Float,y: Float,a: Float,b: Float) = drawLine(tint,Offset(x*s,y*s),Offset(a*s,b*s),1.7.dp.toPx(),StrokeCap.Round)
        fun box(x: Float,y: Float,w: Float,h: Float) = drawRoundRect(tint,Offset(x*s,y*s),androidx.compose.ui.geometry.Size(w*s,h*s),androidx.compose.ui.geometry.CornerRadius(3*s),style=Stroke(1.7.dp.toPx()))
        when(kind) {
            Glyph.CAMERA -> { box(2f,6f,20f,15f);line(7f,6f,9f,3f);line(9f,3f,15f,3f);line(15f,3f,17f,6f);drawCircle(tint,4*s,Offset(12*s,13*s),style=Stroke(1.7.dp.toPx())) }
            Glyph.PHOTO -> {box(3f,3f,18f,18f);line(4f,18f,9f,12f);line(9f,12f,14f,17f);line(14f,17f,17f,13f);line(17f,13f,21f,17f);drawCircle(tint,1.5f*s,Offset(16*s,8*s))}
            Glyph.BACK -> {line(18f,12f,5f,12f);line(5f,12f,11f,6f);line(5f,12f,11f,18f)}
            Glyph.GUIDE -> {box(4f,3f,16f,18f);line(8f,8f,16f,8f);line(8f,12f,16f,12f);line(8f,16f,13f,16f)}
            Glyph.SETTINGS -> {for((y,x) in listOf(6f to 8f,12f to 16f,18f to 10f)){line(3f,y,21f,y);drawCircle(Night,3*s,Offset(x*s,y*s));drawCircle(tint,2.5f*s,Offset(x*s,y*s),style=Stroke(1.7.dp.toPx()))}}
            Glyph.PLAY -> {val p=Path().apply{moveTo(8*s,4*s);lineTo(20*s,12*s);lineTo(8*s,20*s);close()};drawPath(p,tint,style=Stroke(1.7.dp.toPx()))}
            Glyph.HOME -> {line(3f,11f,12f,3f);line(12f,3f,21f,11f);line(5f,10f,5f,21f);line(5f,21f,19f,21f);line(19f,21f,19f,10f)}
        }
    }
}

@Composable
fun ChoiceButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(onClick,modifier.heightIn(min=48.dp).semantics {this.selected=selected},enabled=enabled,
        shape=RoundedCornerShape(12.dp),border=BorderStroke(1.dp,if(selected) Aqua else MaterialTheme.colorScheme.outline),
        colors=ButtonDefaults.outlinedButtonColors(containerColor=if(selected) Aqua.copy(alpha=.15f) else Color.Transparent,
            contentColor=if(selected) Aqua else MaterialTheme.colorScheme.onSurface)) {Text(label)}
}

@Composable
fun ActionButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: Glyph? = null) {
    Button(onClick,modifier.heightIn(min=52.dp),enabled=enabled,shape=RoundedCornerShape(14.dp),contentPadding=PaddingValues(16.dp)) {
        if(icon!=null) {LineIcon(icon,tint=MaterialTheme.colorScheme.onPrimary);Spacer(Modifier.width(10.dp))}
        Text(label,fontWeight=FontWeight.SemiBold)
    }
}

@Composable
fun SectionCard(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface,
        border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline.copy(alpha=.65f))) {
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(title,style=MaterialTheme.typography.titleMedium,modifier=Modifier.semantics{heading()})
            content()
        }
    }
}

@Composable
fun WorkflowPage(title: String, onBack: () -> Unit, step: Int? = null, tag: String = "", bottom: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal=16.dp).testTag(tag)) {
        Row(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onBack,Modifier.heightIn(min=48.dp).testTag("nav_back"),shape=RoundedCornerShape(12.dp),contentPadding=PaddingValues(12.dp)) {
                LineIcon(Glyph.BACK);Spacer(Modifier.width(6.dp));Text(stringResource(R.string.nav_back))
            }
            Column(Modifier.weight(1f)) {
                if(step!=null) Text(stringResource(R.string.workflow_step,step),color=Aqua,style=MaterialTheme.typography.labelSmall)
                Text(title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold,modifier=Modifier.semantics{heading()})
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical=10.dp),verticalArrangement=Arrangement.spacedBy(16.dp),content=content)
        if(bottom!=null) Box(Modifier.fillMaxWidth().padding(top=8.dp,bottom=12.dp)) {bottom()}
    }
}

@Composable
fun StatusMessage(text: String, loading: Boolean = false, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth().semantics{liveRegion=LiveRegionMode.Polite},shape=RoundedCornerShape(14.dp),color=Aqua.copy(alpha=.09f)) {
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(text,style=MaterialTheme.typography.bodyMedium)
            if(loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun FourierEmblem(modifier: Modifier = Modifier) {
    val curve=remember {FourierCurve.star()}
    val points=remember {curve.trace(50)}
    val rotations=remember {curve.harmonics.take(7).flatMap{it.rotations()}}
    Canvas(modifier.background(Brush.radialGradient(listOf(Color(0xFF123D4E),Night)))) {
        val scale=size.minDimension*.42f
        val centre=Offset(size.width*.5f,size.height*.5f)
        fun xy(x: Double,y: Double)=centre+Offset(x.toFloat()*scale,y.toFloat()*scale)
        val path=Path()
        points.forEachIndexed{i,p->val q=xy(p.x,p.y);if(i==0)path.moveTo(q.x,q.y)else path.lineTo(q.x,q.y)}
        drawPath(path,Color(0xFFE9FCFF),style=Stroke(1.5.dp.toPx(),cap=StrokeCap.Round))
        var x=0.0;var y=0.0
        for(h in rotations) {
            val p=h.at(PI*.7)
            if(h.frequency!=0) {
                drawCircle(Aqua.copy(alpha=.25f),(h.radius*scale).toFloat(),xy(x,y),style=Stroke(1.dp.toPx()))
                drawLine(Aqua.copy(alpha=.7f),xy(x,y),xy(x+p.x,y+p.y),1.dp.toPx())
            }
            x+=p.x;y+=p.y
        }
        drawCircle(Aqua,3.dp.toPx(),xy(x,y))
    }
}
