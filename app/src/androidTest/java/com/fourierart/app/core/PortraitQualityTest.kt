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

/** Public-domain NASA fixture only. Never feed a user's photograph into public CI. */
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
            for (part in listOf(FacePart.LEFT_EYE, FacePart.RIGHT_EYE, FacePart.NOSE_BRIDGE)) {
                val expected = face.parts[part]!!
                assertTrue("Lost facial feature: $part", drawing.sourceStrokes.any { it.points == expected.points })
            }
            assertTrue("No context beyond the protected facial features", drawing.sourceStrokes.size > 12)
            assertTrue(drawing.strokes.flatMap { it.trace(200) }.all { it.x.isFinite() && it.y.isFinite() })
            val folder = File(instrumentation.targetContext.getExternalFilesDir(null), "quality").apply { mkdirs() }
            val oldEdges = RasterContours.edges(analysis.pixels, analysis.width, analysis.height, 65)
            val old = RasterContours.lineStrokes(oldEdges, analysis.width, analysis.height,
                maxOf(10.0, maxOf(analysis.width, analysis.height) * 0.025)).take(40)
            val comparison = Bitmap.createBitmap(2048, 570, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(comparison)
            canvas.drawColor(Color.rgb(13,25,41))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 23f }
            for ((i, title) in listOf("Public reference", "Old: longest 40", "New: protected features", "Fourier: 200").withIndex()) {
                canvas.drawText(title, i*512f+15, 32f, paint)
            }
            canvas.drawBitmap(bitmap, null, android.graphics.Rect(0,50,512,562), null)
            fun source(paths: List<ContourStroke>, offset: Float) {
                paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.7f
                for (stroke in paths) {
                    val path = Path()
                    stroke.points.forEachIndexed { i,p ->
                        val x = offset + p.x.toFloat()*512/analysis.width
                        val y = 50+p.y.toFloat()*512/analysis.height
                        if(i==0) path.moveTo(x,y) else path.lineTo(x,y)
                    }
                    if(stroke.closed) path.close()
                    canvas.drawPath(path,paint)
                }
            }
            source(old,512f); source(drawing.sourceStrokes,1024f)
            paint.strokeWidth = 1.7f
            for(stroke in drawing.strokes) {
                val path=Path()
                stroke.trace(200).forEachIndexed { i,p ->
                    val x=1792f+p.x.toFloat()*220f; val y=306f+p.y.toFloat()*220f
                    if(i==0) path.moveTo(x,y) else path.lineTo(x,y)
                }
                canvas.drawPath(path,paint)
            }
            File(folder,"portrait-comparison.png").outputStream().use { comparison.compress(Bitmap.CompressFormat.PNG,100,it) }
            val final = Bitmap.createBitmap(comparison,1536,50,512,512)
            File(folder,"portrait-final.png").outputStream().use { final.compress(Bitmap.CompressFormat.PNG,100,it) }
            final.recycle()
            File(folder,"metrics.txt").writeText("kind=${result.kind}\nfaceParts=${face.parts.keys}\noldStrokes=${old.size}\nnewStrokes=${drawing.strokes.size}\nleftEye=true\nrightEye=true\nnoseBridgePoints=${face.parts[FacePart.NOSE_BRIDGE]?.points?.size}\n")
            comparison.recycle()
        } finally { bitmap.recycle() }
    }
}
