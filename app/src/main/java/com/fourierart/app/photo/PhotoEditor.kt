package com.fourierart.app.photo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import kotlin.math.*

@Composable
fun PhotoEditor(model: PhotoSessionModel,showActions: Boolean=true) {
    val bitmap=model.editorPreview ?: return
    val preview=remember(bitmap) {bitmap.asImageBitmap()}
    val currentCrop by rememberUpdatedState(model.draft.crop)
    val label=stringResource(R.string.crop_description)
    Column(verticalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.fillMaxWidth().testTag("photo_editor")) {
        Text(stringResource(R.string.crop_help),style=MaterialTheme.typography.bodySmall)
        Canvas(Modifier.fillMaxWidth().height(280.dp).testTag("crop_canvas").semantics {contentDescription=label}
            .pointerInput(bitmap,model.loading) {
                var handle=-1
                detectDragGestures(onDragStart={ p ->
                    val scale=min(size.width.toFloat()/bitmap.width,size.height.toFloat()/bitmap.height)
                    val w=bitmap.width*scale;val h=bitmap.height*scale
                    val left=(size.width-w)/2;val top=(size.height-h)/2
                    val c=currentCrop
                    val corners=listOf(Offset(left+c.left*w,top+c.top*h),Offset(left+c.right*w,top+c.top*h),
                        Offset(left+c.right*w,top+c.bottom*h),Offset(left+c.left*w,top+c.bottom*h))
                    val nearest=corners.indices.minBy { (corners[it]-p).getDistance() }
                    handle=if((corners[nearest]-p).getDistance()<44.dp.toPx() ||
                        p.x !in left+c.left*w..left+c.right*w || p.y !in top+c.top*h..top+c.bottom*h) nearest else -1
                }) { change, delta ->
                    if(!model.loading) {
                        val scale=min(size.width.toFloat()/bitmap.width,size.height.toFloat()/bitmap.height)
                        val w=bitmap.width*scale;val h=bitmap.height*scale
                        val left=(size.width-w)/2;val top=(size.height-h)/2
                        val c=currentCrop
                        model.setCrop(if(handle<0) c.moved(delta.x/w,delta.y/h) else
                            c.corner(handle,(change.position.x-left)/w,(change.position.y-top)/h))
                        change.consume()
                    }
                }
            }) {
            val scale=min(size.width/bitmap.width,size.height/bitmap.height)
            val w=bitmap.width*scale;val h=bitmap.height*scale
            val left=(size.width-w)/2;val top=(size.height-h)/2
            drawImage(preview,dstOffset=IntOffset(left.roundToInt(),top.roundToInt()),dstSize=IntSize(w.roundToInt(),h.roundToInt()))
            val c=currentCrop;val x0=left+c.left*w;val x1=left+c.right*w;val y0=top+c.top*h;val y1=top+c.bottom*h
            val shade=Color.Black.copy(alpha=0.5f)
            drawRect(shade,Offset(left,top),Size(w,(y0-top).coerceAtLeast(0f)))
            drawRect(shade,Offset(left,y1),Size(w,(top+h-y1).coerceAtLeast(0f)))
            drawRect(shade,Offset(left,y0),Size((x0-left).coerceAtLeast(0f),y1-y0))
            drawRect(shade,Offset(x1,y0),Size((left+w-x1).coerceAtLeast(0f),y1-y0))
            drawRect(Color(0xFF65DCD0),Offset(x0,y0),Size(x1-x0,y1-y0),style=Stroke(2.dp.toPx()))
            for(i in 1..2) {
                val x=x0+(x1-x0)*i/3;val y=y0+(y1-y0)*i/3
                drawLine(Color.White.copy(alpha=0.55f),Offset(x,y0),Offset(x,y1),1.dp.toPx())
                drawLine(Color.White.copy(alpha=0.55f),Offset(x0,y),Offset(x1,y),1.dp.toPx())
            }
            for(p in listOf(Offset(x0,y0),Offset(x1,y0),Offset(x1,y1),Offset(x0,y1))) drawCircle(Color(0xFF65DCD0),6.dp.toPx(),p)
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick={model.rotate(false)},enabled=!model.loading,modifier=Modifier.weight(1f).testTag("rotate_left")) {Text(stringResource(R.string.rotate_left))}
            OutlinedButton(onClick={model.rotate(true)},enabled=!model.loading,modifier=Modifier.weight(1f).testTag("rotate_right")) {Text(stringResource(R.string.rotate_right))}
        }
        val crop=model.draft.crop
        Text(stringResource(R.string.crop_horizontal,(crop.left*100).roundToInt(),(crop.right*100).roundToInt()),style=MaterialTheme.typography.bodySmall)
        RangeSlider(value=crop.left..crop.right,onValueChange={v -> if(v.endInclusive-v.start>=0.05f) model.setCrop(currentCrop.copy(left=v.start,right=v.endInclusive))},
            enabled=!model.loading,modifier=Modifier.testTag("crop_horizontal"))
        Text(stringResource(R.string.crop_vertical,(crop.top*100).roundToInt(),(crop.bottom*100).roundToInt()),style=MaterialTheme.typography.bodySmall)
        RangeSlider(value=crop.top..crop.bottom,onValueChange={v -> if(v.endInclusive-v.start>=0.05f) model.setCrop(currentCrop.copy(top=v.start,bottom=v.endInclusive))},
            enabled=!model.loading,modifier=Modifier.testTag("crop_vertical"))
        OutlinedButton(onClick=model::resetOriginal,enabled=!model.loading,modifier=Modifier.fillMaxWidth().testTag("reset_original")) {Text(stringResource(R.string.reset_original))}
        if(showActions) Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick=model::cancelEdit,enabled=!model.loading,modifier=Modifier.weight(1f).testTag("editor_cancel")) {
                Text(stringResource(if(model.applied==null) R.string.discard_photo else R.string.cancel))
            }
            Button(onClick=model::applyPhoto,enabled=!model.loading,modifier=Modifier.weight(1f).testTag("apply_photo")) {Text(stringResource(R.string.apply_photo))}
        }
    }
}
