package com.fourierart.app.core

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Pinned public fixtures only. Never feed a user's photograph into public CI. */
@RunWith(AndroidJUnit4::class)
class PortraitQualityTest {
    @Test fun realPortraitKeepsBothEyesNoseMouthAndProducesReviewableOutput() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.context.assets.open("portrait.png").use { BitmapFactory.decodeStream(it) }!!
        try {
            val analysis = PhotoContours.analyze(bitmap, PhotoContours.Mode.DRAWING)
            val face = analysis.face
            assertNotNull("Bundled face model did not detect the public portrait", face)
            assertTrue("Incomplete facial geometry: ${face!!.parts.keys}", face.complete)
            val result = PhotoContours.extract(analysis, PhotoContours.Mode.DRAWING, 65)
            assertEquals(PhotoContours.Kind.FACE, result.kind)
            val drawing = result.drawing!!
            for (part in listOf(FacePart.LEFT_EYE, FacePart.RIGHT_EYE)) {
                val expected = face.parts[part]!!
                assertTrue("Lost facial feature: $part", drawing.sourceStrokes.any { it.points == expected.points })
            }
            assertEquals(2,face.parts[FacePart.NOSE_BRIDGE]!!.points.size)
            val nose = face.parts[FacePart.NOSE_BOTTOM]!!.points
            assertTrue("Nose disappeared after composition", drawing.sourceStrokes.any { s ->
                s.points.size >= 2 && s.points.count { p ->
                    p.x in (nose.minOf { it.x }-10)..(nose.maxOf { it.x }+10) &&
                    p.y in (nose.minOf { it.y }-20)..(nose.maxOf { it.y }+10)
                } > s.points.size/2
            })
            assertTrue("No context beyond the protected facial features", drawing.sourceStrokes.size > 12)
            assertTrue(drawing.strokes.flatMap { it.trace(200) }.all { it.x.isFinite() && it.y.isFinite() })
            val folder = File(instrumentation.targetContext.getExternalFilesDir(null), "quality").apply { mkdirs() }
            val variants = listOf("Texture 0.4" to PhotoContours.extract(analysis,PhotoContours.Mode.DRAWING,65,DetailLevel.FINE,LineStyle.DETAIL).drawing!!) + DetailLevel.entries.map { level ->
                val start = System.nanoTime()
                val variant = PhotoContours.extract(analysis, PhotoContours.Mode.DRAWING, 65, level).drawing!!
                File(folder,"metrics.txt").appendText("portrait-$level strokes=${variant.strokes.size} extractMs=${(System.nanoTime()-start)/1_000_000}\n")
                level.name to variant
            }
            comparison(folder,"portrait-detail",bitmap,analysis,variants)
        } finally { bitmap.recycle() }
    }
    @Test fun threeIndependentlyCroppedFacesKeepTheirOwnFeatures() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val source = instrumentation.context.assets.open("portrait.png").use { BitmapFactory.decodeStream(it) }!!
        val group = Bitmap.createBitmap(1500,650,Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(group);canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(source,null,android.graphics.Rect(0,0,600,600),null)
            canvas.drawBitmap(source,null,android.graphics.Rect(600,80,1100,580),null)
            canvas.drawBitmap(source,null,android.graphics.Rect(1100,220,1480,600),null)
            val start=System.nanoTime()
            val a=PhotoContours.analyze(group,PhotoContours.Mode.DRAWING)
            assertEquals("Box detector lost one of the public test faces",3,a.detectedFaceCount)
            assertEquals("Per-face crops did not all return contours",3,a.faces.size)
            assertTrue("Some test faces have incomplete geometry",a.faces.all { it.complete })
            val d=PhotoContours.extract(a,PhotoContours.Mode.DRAWING,50).drawing!!
            for(f in a.faces) for(part in listOf(FacePart.LEFT_EYE,FacePart.RIGHT_EYE))
                assertTrue(d.sourceStrokes.any { it.points==f.parts[part]!!.points })
            val folder=File(instrumentation.targetContext.getExternalFilesDir(null),"quality").apply { mkdirs() }
            File(folder,"metrics.txt").appendText("group faces=${a.faces.size}/${a.detectedFaceCount} strokes=${d.strokes.size} totalMs=${(System.nanoTime()-start)/1_000_000}\n")
            comparison(folder,"group",group,a,listOf("All 3 faces" to d))
        } finally {source.recycle();group.recycle()}
    }

    @Test fun publicCatComparesThreeDetailBudgets() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap=instrumentation.context.assets.open("cat.png").use { BitmapFactory.decodeStream(it) }!!
        try {
            val a=PhotoContours.analyze(bitmap,PhotoContours.Mode.DRAWING)
            val variants=listOf("Texture 0.4" to PhotoContours.extract(a,PhotoContours.Mode.DRAWING,65,DetailLevel.FINE,LineStyle.DETAIL).drawing!!) + DetailLevel.entries.map { level ->
                val start=System.nanoTime()
                val d=PhotoContours.extract(a,PhotoContours.Mode.DRAWING,65,level).drawing!!
                val folder=File(instrumentation.targetContext.getExternalFilesDir(null),"quality").apply { mkdirs() }
                File(folder,"metrics.txt").appendText("cat-$level strokes=${d.strokes.size} extractMs=${(System.nanoTime()-start)/1_000_000}\n")
                level.name to d
            }
            assertEquals("Accepted texture path changed its budget",400,variants.first().second.strokes.size)
            assertTrue("Structural path invented extra traces",variants.last().second.strokes.size<=400)
            assertTrue("Structural fine level lost all detail",variants.last().second.strokes.size>=variants[1].second.strokes.size)
            val folder=File(instrumentation.targetContext.getExternalFilesDir(null),"quality")
            comparison(folder,"cat-detail",bitmap,a,variants)
        } finally {bitmap.recycle()}
    }

    @Test fun plainBackgroundKeepsTwoSeparatedSubjects() {
        val bitmap=Bitmap.createBitmap(400,300,Bitmap.Config.ARGB_8888)
        try {
            val canvas=Canvas(bitmap);canvas.drawColor(Color.WHITE)
            val paint=Paint().apply { color=Color.rgb(30,30,30) }
            canvas.drawCircle(100f,150f,65f,paint);canvas.drawCircle(300f,150f,40f,paint)
            val a=PhotoContours.analyze(bitmap,PhotoContours.Mode.SUBJECT)
            assertTrue(a.plainBackground)
            val d=PhotoContours.extract(a,PhotoContours.Mode.SUBJECT,50).drawing!!
            assertTrue(d.sourceStrokes.any { it.closed && it.points.all { p -> p.x<200 } })
            assertTrue(d.sourceStrokes.any { it.closed && it.points.all { p -> p.x>200 } })
        } finally {bitmap.recycle()}
    }

    @Test fun dogReferenceProducesFiniteColorContoursAndStyleComparison() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val bitmap=instrumentation.context.assets.open("dog.jpg").use { BitmapFactory.decodeStream(it) }!!
        try {
            val a=PhotoContours.analyze(bitmap,PhotoContours.Mode.DRAWING)
            val folder=File(instrumentation.targetContext.getExternalFilesDir(null),"quality").apply { mkdirs() }
            val variants=LineStyle.entries.map { style ->
                val start=System.nanoTime()
                val result=PhotoContours.extract(a,PhotoContours.Mode.DRAWING,80,DetailLevel.BALANCED,style)
                val d=result.drawing!!
                assertTrue(d.strokes.isNotEmpty())
                assertTrue(d.strokes.flatMap { it.trace(400) }.all { it.x.isFinite() && it.y.isFinite() })
                File(folder,"metrics.txt").appendText("dog-$style strokes=${d.strokes.size} extractMs=${(System.nanoTime()-start)/1_000_000}\n")
                style.name to d
            }
            comparison(folder,"dog-styles",bitmap,a,variants)
        } finally {bitmap.recycle()}
    }

    private fun comparison(folder: File, name: String, bitmap: Bitmap, a: PhotoContours.Analysis,
        variants: List<Pair<String,ContourDrawing>>) {
        val cell=512;val header=58
        val output=Bitmap.createBitmap(cell*(variants.size+1),cell+header,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(output);canvas.drawColor(Color.rgb(13,25,41))
        val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.WHITE;textSize=22f }
        canvas.drawText("Public reference",15f,32f,paint)
        val scale=minOf(480.0/a.width,480.0/a.height)
        val left=(cell-a.width*scale)/2;val top=header+(cell-a.height*scale)/2
        canvas.drawBitmap(bitmap,null,android.graphics.RectF(left.toFloat(),top.toFloat(),
            (left+a.width*scale).toFloat(),(top+a.height*scale).toFloat()),null)
        for((index,variant) in variants.withIndex()) {
            val (label,d)=variant
            paint.style=Paint.Style.FILL
            canvas.drawText("$label: ${d.strokes.size} strokes",(index+1)*cell+15f,32f,paint)
            val points=d.sourceStrokes.flatMap { it.points }
            val x0=points.minOf { it.x };val x1=points.maxOf { it.x }
            val y0=points.minOf { it.y };val y1=points.maxOf { it.y }
            val side=maxOf(x1-x0,y1-y0);val cx=(x1+x0)/2;val cy=(y1+y0)/2
            paint.style=Paint.Style.STROKE;paint.strokeWidth=1.35f
            for(s in d.strokes) {
                val path=Path()
                s.trace(400,640).forEachIndexed { i,p ->
                    val x=(index+1)*cell+left+(p.x*side/1.7+cx)*scale
                    val y=top+(p.y*side/1.7+cy)*scale
                    if(i==0) path.moveTo(x.toFloat(),y.toFloat()) else path.lineTo(x.toFloat(),y.toFloat())
                }
                canvas.drawPath(path,paint)
            }
        }
        File(folder,"$name.png").outputStream().use {output.compress(Bitmap.CompressFormat.PNG,100,it)}
        output.recycle()
    }

}
