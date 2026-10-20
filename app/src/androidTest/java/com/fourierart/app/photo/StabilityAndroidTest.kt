package com.fourierart.app.photo

import android.graphics.*
import android.net.Uri
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fourierart.app.MainActivity
import com.fourierart.app.core.*
import com.fourierart.app.export.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@RunWith(AndroidJUnit4::class)
class StabilityAndroidTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val model get()=compose.activity.photoSession
    private val context get()=compose.activity.applicationContext
    private fun ready() {compose.waitForIdle();compose.waitUntil(60000){!model.loading};compose.waitForIdle()}
    private fun fresh() {ready();compose.runOnIdle{model.sample(false)};ready()}
    private fun click(tag: String,fixed: Boolean=false) {
        val node=compose.onNodeWithTag(tag);if(!fixed)node.performScrollTo();node.performClick();compose.waitForIdle()
    }
    private fun record(name: String,text: String) {
        val folder=File(context.getExternalFilesDir(null),"quality/stability").apply{mkdirs()}
        File(folder,name+".txt").writeText(text)
    }
    private fun memory(): String {
        val info=Debug.MemoryInfo();Debug.getMemoryInfo(info)
        val runtime=Runtime.getRuntime()
        return "pssKb=${info.totalPss} javaUsedBytes=${runtime.totalMemory()-runtime.freeMemory()} nativeAllocatedBytes=${Debug.getNativeHeapAllocatedSize()} maxJavaHeapBytes=${runtime.maxMemory()}"
    }
    private fun fixture(name: String,width: Int=256,height: Int=192): File {
        val bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
        try {
            Canvas(bitmap).apply {
                drawColor(Color.WHITE)
                drawCircle(width*.5f,height*.5f,height*.3f,Paint().apply{color=Color.BLACK})
            }
            return File(context.cacheDir,name).also {file ->
                file.outputStream().use{check(bitmap.compress(Bitmap.CompressFormat.JPEG,95,it))}
            }
        } finally {bitmap.recycle()}
    }
    private fun dense(): ContourDrawing=ContourDrawing.fromStrokes((0 until 220).map {index ->
        val cx=(index%20)*60.0;val cy=(index/20)*60.0
        ContourStroke((0 until 48).map {i ->
            val angle=2*PI*i/48;val radius=20.0+7*cos(5*angle)
            Point2(cx+radius*cos(angle),cy+radius*sin(angle))
        },true)
    })

    @Test fun largeInputIsBoundedAndInvalidReplacementKeepsWorkingPhoto() {
        fresh()
        val large=fixture("stage8-large.jpg",3601,2401)
        val small=fixture("stage8-small.jpg")
        val invalid=File(context.cacheDir,"stage8-invalid.jpg").apply{writeText("not an image")}
        val metrics=StringBuilder("input=3601x2401\n")
        try {
            val start=SystemClock.elapsedRealtimeNanos()
            val decoded=PhotoIO.decode(context,Uri.fromFile(large))
            try {
                assertTrue(decoded.width<=1800 && decoded.height<=1800)
                metrics.append("decoded=${decoded.width}x${decoded.height} bytes=${decoded.allocationByteCount} decodeMs=${(SystemClock.elapsedRealtimeNanos()-start)/1e6}\n")
                repeat(8) {i ->
                    val prepared=PhotoIO.prepare(decoded,PhotoPreparation(i%4,CropBox(.1f,.1f,.9f,.9f)))
                    assertTrue(prepared.width>0 && prepared.height>0)
                    assertFalse(decoded.isRecycled);prepared.getPixel(prepared.width/2,prepared.height/2)
                    if(prepared!==decoded)prepared.recycle()
                }
            } finally {decoded.recycle()}
            compose.runOnIdle{model.mode=PhotoContours.Mode.SUBJECT;model.importPhoto(Uri.fromFile(small))}
            ready();compose.runOnIdle{model.applyPhoto()};ready()
            val image=model.image;val drawing=model.drawing;val id=model.workingPhotoId
            assertNotNull(image);assertNotNull(drawing)
            for(file in listOf(invalid,File(context.cacheDir,"stage8-missing.jpg"))) {
                compose.runOnIdle{model.importPhoto(Uri.fromFile(file))};ready()
                assertSame(image,model.image);assertSame(drawing,model.drawing)
                assertEquals(id,model.workingPhotoId);assertTrue(model.notice!=0)
            }
            val restored=PhotoSessionStore(context).read()!!
            assertEquals(id,restored.file);restored.source.recycle()
            metrics.append("invalidAndMissingPreservedWork=true\n${memory()}\n")
            record("input",metrics.toString())
        } finally {
            compose.runOnIdle{model.sample(false)};ready()
            large.delete();small.delete();invalid.delete()
        }
    }

    @Test fun repeatedNavigationReusesDrawingAndCloseReleasesExport() {
        fresh()
        compose.runOnIdle{model.components=400;model.seekAnimation(.3)}
        click("resume_animation")
        val drawing=model.drawing!!
        val points=drawing.strokes.map{it.trace(400)}
        val start=SystemClock.elapsedRealtimeNanos()
        repeat(4) {
            click("open_parameters");click("parameters_done",true)
            click("player_expand");click("player_close",true)
            click("open_export",true);click("format_png");click("export_start")
            compose.waitUntil(60000){!compose.activity.exportSession.busy};compose.waitForIdle()
            assertNotNull(compose.activity.exportSession.completed)
            click("export_close",true)
            assertNull(compose.activity.exportSession.snapshot)
            assertNull(compose.activity.exportSession.completed)
            assertSame(drawing,model.drawing)
            drawing.strokes.forEachIndexed {index,s ->assertSame(points[index],s.trace(400))}
        }
        compose.activityRule.scenario.recreate();ready()
        assertSame(drawing,model.drawing)
        record("navigation","cycles=4 elapsedMs=${(SystemClock.elapsedRealtimeNanos()-start)/1e6}\nreusedDrawingAndPoints=true releasedExportSnapshot=true\n${memory()}\n")
    }

    @Test fun denseTraceCacheAndSoftwareRenderHaveReproducibleMetrics() = runBlocking {
        fresh()
        val drawing=dense()
        val baselineStart=SystemClock.elapsedRealtimeNanos()
        val direct=drawing.strokes.map {it.curve.trace(400,it.curve.recommendedFrames(400,it.visibleFraction),it.visibleFraction)}
        val directNanos=SystemClock.elapsedRealtimeNanos()-baselineStart
        val firstStart=SystemClock.elapsedRealtimeNanos()
        val cached=drawing.strokes.map{it.trace(400)}
        val firstNanos=SystemClock.elapsedRealtimeNanos()-firstStart
        assertEquals(direct,cached)
        val reuseStart=SystemClock.elapsedRealtimeNanos()
        repeat(20){drawing.strokes.forEachIndexed{index,s ->assertSame(cached[index],s.trace(400))}}
        val reuseNanos=SystemClock.elapsedRealtimeNanos()-reuseStart
        val snapshot=ExportSnapshot(drawing,400,AnimationStyle(epicircles=false))
        val renderer=ExportRenderer(snapshot)
        val bitmap=Bitmap.createBitmap(360,360,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap)
        val frames=LongArray(60)
        try {
            repeat(60){i ->
                val start=SystemClock.elapsedRealtimeNanos()
                renderer.draw(canvas,360,360,i/59.0)
                frames[i]=SystemClock.elapsedRealtimeNanos()-start
            }
        } finally {bitmap.recycle()}
        val active=AtomicBoolean(true);val beats=AtomicInteger();val gap=AtomicLong();var previous=SystemClock.uptimeMillis()
        val handler=Handler(Looper.getMainLooper())
        val pulse=object:Runnable {override fun run() {
            if(!active.get())return
            val now=SystemClock.uptimeMillis();gap.updateAndGet{maxOf(it,now-previous)}
            previous=now;beats.incrementAndGet();if(active.get())handler.postDelayed(this,16)
        }}
        val before=memory();handler.post(pulse)
        var file: File?=null
        val exportStart=SystemClock.elapsedRealtimeNanos()
        try {
            file=ExportEngine.render(context,snapshot,ExportSpec(ExportFormat.GIF,seconds=6))
            val movie=Movie.decodeFile(file.absolutePath)
            assertNotNull(movie);assertEquals(6000,movie.duration())
            frames.sort()
            record("performance","syntheticStrokes=220 componentsMaximum=400\ndirectTraceMs=${directNanos/1e6} firstCachedTraceMs=${firstNanos/1e6} cached20PassesMs=${reuseNanos/1e6}\nsoftware360FrameMedianMs=${frames[30]/1e6} software360FrameP95Ms=${frames[57]/1e6}\ngifMs=${(SystemClock.elapsedRealtimeNanos()-exportStart)/1e6} gifBytes=${file.length()} mainHeartbeatCount=${beats.get()} mainHeartbeatMaxGapMs=${gap.get()}\nbefore $before\nafter ${memory()}\nThese are debug-emulator observations, not screen FPS or universal device limits.\n")
        } finally {active.set(false);handler.removeCallbacks(pulse);file?.delete()}
    }

    @Test fun closingDuringExportCancelsAndAllowsASecondExport() {
        fresh()
        val export=compose.activity.exportSession
        val before=ExportEngine.directory(context).listFiles().orEmpty().map{it.name}.toSet()
        val snapshot=ExportSnapshot(dense(),400,AnimationStyle())
        val start=SystemClock.elapsedRealtimeNanos()
        compose.runOnIdle {
            export.open(snapshot);export.configure(ExportSpec(ExportFormat.GIF,seconds=6));export.start()
            assertTrue(export.busy);export.close();assertNull(export.snapshot)
        }
        compose.waitUntil(10000){!export.busy};compose.waitForIdle()
        assertNull(export.completed)
        assertEquals(before,ExportEngine.directory(context).listFiles().orEmpty().map{it.name}.toSet())
        val cancelledMs=(SystemClock.elapsedRealtimeNanos()-start)/1e6
        compose.runOnIdle {
            export.open(ExportSnapshot(model.drawing!!,400,AnimationStyle()))
            export.configure(ExportSpec(ExportFormat.PNG,pngSize=720));export.start()
        }
        compose.waitUntil(60000){!export.busy};compose.waitForIdle()
        val result=export.completed!!
        val uri=export.shareIntent(context)!!.clipData!!.getItemAt(0).uri
        compose.runOnIdle{export.close()}
        assertNull(export.snapshot);assertNull(export.completed)
        // A URI already offered for sharing remains readable after closing the editor.
        val decoded=context.contentResolver.openInputStream(uri).use{BitmapFactory.decodeStream(it)}
        assertNotNull(decoded);assertEquals(720,decoded.width);decoded.recycle();result.delete()
        record("cancellation","closeAndCancelMs=$cancelledMs noPartialFiles=true secondExportReadableAfterClose=true\n${memory()}\n")
    }
}
