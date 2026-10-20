package com.fourierart.app.photo

import android.graphics.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.fourierart.app.MainActivity
import com.fourierart.app.core.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.*

@RunWith(AndroidJUnit4::class)
class FourierEngineAndroidTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun folder()=File(compose.activity.getExternalFilesDir(null),"quality").apply {mkdirs()}
    private fun ready() {
        compose.waitForIdle()
        compose.waitUntil(60000) {compose.onAllNodesWithTag("fourier_ready").fetchSemanticsNodes().isNotEmpty()}
        compose.waitForIdle()
    }
    @Test fun componentChangesReuseDrawingAndProduceCompleteMultistrokePreview() {
        compose.waitUntil(60000) {!compose.activity.photoSession.loading}
        compose.runOnIdle {compose.activity.photoSession.sample(false)}
        compose.waitUntil(60000) {!compose.activity.photoSession.loading}
        val drawing=ContourDrawing.fromStrokes(listOf(
            ContourStroke((0 until 240).map {i ->val t=2*PI*i/240;val r=50+8*cos(9*t);Point2(60+r*cos(t),60+r*sin(t))},true),
            ContourStroke(listOf(Point2(150.0,25.0),Point2(175.0,55.0),Point2(150.0,90.0)),false)))
        compose.runOnIdle {compose.activity.photoSession.apply {this.drawing=drawing;components=10;showComplete=true}}
        compose.onNodeWithTag("resume_animation").performScrollTo().performClick()
        ready()
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        compose.onNodeWithTag("fourier_ready").performScrollTo()
        device.takeScreenshot(File(folder(),"stage4-components-10.png"))
        compose.onNodeWithTag("open_parameters").performScrollTo().performClick()
        compose.onNodeWithTag("fourier_components").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) {it(6f)}
        ready()
        compose.runOnIdle {assertSame(drawing,compose.activity.photoSession.drawing);assertEquals(800,compose.activity.photoSession.components)}
        compose.onNodeWithTag("effective_components").assertExists()
        compose.onNodeWithTag("fourier_ready").performScrollTo()
        device.takeScreenshot(File(folder(),"stage4-components-800.png"))
        compose.onNodeWithTag("nav_back").performClick()
        compose.onNodeWithTag("fourier_complete").performScrollTo().performClick();ready()
        assertFalse(compose.activity.photoSession.showComplete)
        compose.runOnIdle {compose.activity.photoSession.sample(false)}
    }
    @Test fun measuredPrecisionProducesReferencePanelsAndErrorEvidence() {
        val points=(0 until 2048).map {i ->val t=2*PI*i/2048;val r=.7+.07*cos(27*t)+.1*cos(5*t);Point2(r*cos(t),r*sin(t))}
        val curve=FourierCurve.fromClosedPath(points,1024)
        val bitmap=Bitmap.createBitmap(1000,800,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap);canvas.drawColor(Color.rgb(13,25,41))
        val ink=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=Color.WHITE;style=Paint.Style.STROKE;strokeWidth=2f}
        val label=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=Color.rgb(101,220,208);textSize=22f}
        var previous=Double.POSITIVE_INFINITY
        val metrics=StringBuilder("RMS versus 1024 arc-length input samples, not photo recognition\n")
        for((index,n) in listOf(10,50,200,800).withIndex()) {
            val measured=sqrt(curve.points.indices.sumOf {j ->val p=curve.points[j];val q=curve.sample(2*PI*j/1024,n);(p.x-q.x).pow(2)+(p.y-q.y).pow(2)}/1024)
            assertTrue(measured<=previous+1e-12);previous=measured
            assertEquals(measured,curve.reconstructionRms(n),1e-10)
            val frames=curve.recommendedFrames(n);val trail=curve.trace(n,frames)
            val duplicate=FourierCurve.fromClosedPath(points,1024).trace(n,frames)
            assertEquals(trail,duplicate)
            val x=(index%2)*500+250;val y=(index/2)*400+220
            val path=Path()
            trail.forEachIndexed {i,p ->val px=x+p.x.toFloat()*190;val py=y+p.y.toFloat()*190;if(i==0) path.moveTo(px,py) else path.lineTo(px,py)}
            canvas.drawPath(path,ink)
            canvas.drawText("N=$n  RMS=${"%.6f".format(java.util.Locale.US,measured)}",(index%2)*500+24f,(index/2)*400+40f,label)
            metrics.append("N=$n samples=1024 frames=$frames rms=$measured\n")
        }
        File(folder(),"stage4-fourier-metrics.txt").writeText(metrics.toString())
        File(folder(),"stage4-fourier-precision.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        bitmap.recycle()
    }
}
