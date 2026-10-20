package com.fourierart.app.photo

import android.content.Intent
import android.graphics.*
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.UiDevice
import com.fourierart.app.MainActivity
import com.fourierart.app.core.*
import com.fourierart.app.export.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class ExportAndroidTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val context get() = compose.activity.applicationContext
    private fun snapshot() = ExportSnapshot(ContourDrawing.fromStrokes(listOf(
        ContourStroke(listOf(Point2(0.0,0.0),Point2(90.0,0.0),Point2(20.0,45.0)),true),
        ContourStroke(listOf(Point2(80.0,65.0),Point2(90.0,100.0),Point2(60.0,100.0)),false)
    )),200,AnimationStyle(epicircles=true,color=TraceColor.ROSE))
    private fun folder() = File(compose.activity.getExternalFilesDir(null),"quality/exports").apply { mkdirs() }
    private fun expected(source: ExportSnapshot, spec: ExportSpec) = Bitmap.createBitmap(spec.width,spec.height,Bitmap.Config.ARGB_8888).also {
        ExportRenderer(source).draw(Canvas(it),spec.width,spec.height,1.0)
    }
    private fun verifyImage(actual: Bitmap, reference: Bitmap) {
        assertEquals(reference.width,actual.width);assertEquals(reference.height,actual.height)
        var distance = 0L;var count = 0;var ax=0.0;var ay=0.0;var an=0;var ex=0.0;var ey=0.0;var en=0
        val bg=reference.getPixel(0,0)
        fun difference(a: Int,b: Int) = abs(Color.red(a)-Color.red(b))+abs(Color.green(a)-Color.green(b))+abs(Color.blue(a)-Color.blue(b))
        for(y in 0 until actual.height) for(x in 0 until actual.width) {
            val a=actual.getPixel(x,y);val e=reference.getPixel(x,y)
            distance+=difference(a,e);count++
            if(difference(a,bg)>180) {ax+=x;ay+=y;an++}
            if(difference(e,bg)>180) {ex+=x;ey+=y;en++}
        }
        assertTrue("Blank decoded result",an>100 && en>100)
        assertTrue("Wrong colours or frame",distance.toDouble()/count<8)
        assertEquals("Horizontal position",ex/en,ax/an,4.0)
        assertEquals("Vertical position / upside-down video",ey/en,ay/an,4.0)
        assertTrue("Missing strokes",an.toDouble()/en in .65..1.45)
    }
    @Test fun realPngGifAndH264RoundTripBothFrames() = runBlocking {
        val source=snapshot();val metrics=StringBuilder("Synthetic asymmetric multistroke drawing; no user images.\n")
        for(frame in ExportFrame.entries) for(format in ExportFormat.entries) {
            val spec=ExportSpec(format,frame,6)
            val start=System.nanoTime();val file=ExportEngine.render(context,source,spec)
            val ref=expected(source,spec)
            try {
                when(format) {
                    ExportFormat.PNG -> {
                        val decoded=BitmapFactory.decodeFile(file.absolutePath);assertNotNull(decoded)
                        try {verifyImage(decoded,ref)} finally {decoded.recycle()}
                    }
                    ExportFormat.GIF -> {
                        val movie=file.inputStream().use {Movie.decodeStream(it)};assertNotNull(movie)
                        assertEquals(spec.width,movie.width());assertEquals(spec.height,movie.height());assertEquals(6000,movie.duration())
                        val decoded=Bitmap.createBitmap(spec.width,spec.height,Bitmap.Config.ARGB_8888)
                        try {movie.setTime(5700);movie.draw(Canvas(decoded),0f,0f);verifyImage(decoded,ref)} finally {decoded.recycle()}
                    }
                    ExportFormat.MP4 -> {
                        val extractor=MediaExtractor()
                        try {
                            extractor.setDataSource(file.absolutePath);assertEquals(1,extractor.trackCount)
                            val track=extractor.getTrackFormat(0)
                            assertEquals("video/avc",track.getString(MediaFormat.KEY_MIME))
                            assertEquals(spec.width,track.getInteger(MediaFormat.KEY_WIDTH));assertEquals(spec.height,track.getInteger(MediaFormat.KEY_HEIGHT))
                            extractor.selectTrack(0);var frames=0;var previous=-1L
                            while(extractor.sampleTime>=0) {
                                assertTrue(extractor.sampleTime>previous);previous=extractor.sampleTime;frames++
                                assertTrue(abs(spec.presentationNanos(frames-1)/1000-previous)<=2L)
                                extractor.advance()
                            }
                            assertEquals(spec.frames,frames)
                        } finally {extractor.release()}
                        val retriever=MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(file.absolutePath)
                            val duration=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()
                            assertTrue("duration=$duration",duration in 5900..6100)
                            val decoded=checkNotNull(retriever.getFrameAtTime(5_700_000,MediaMetadataRetriever.OPTION_CLOSEST))
                            try {verifyImage(decoded,ref)} finally {decoded.recycle()}
                        } finally {retriever.release()}
                    }
                }
                metrics.append("$format $frame ${spec.width}x${spec.height} ${spec.frames} frames bytes=${file.length()} elapsedMs=${(System.nanoTime()-start)/1_000_000}\n")
                file.copyTo(File(folder(),"stage6-${frame.name.lowercase()}.${format.extension}"),overwrite=true)
            } finally {ref.recycle();file.delete()}
        }
        File(folder(),"stage6-export-metrics.txt").writeText(metrics.toString())
    }
    @Test fun cancellationDeletesPartialFilesAndKeepsPreviousResult() = runBlocking {
        val source=snapshot();val previous=ExportEngine.render(context,source,ExportSpec(ExportFormat.PNG,pngSize=720))
        try {
            for(format in ExportFormat.entries) {
                val before=ExportEngine.directory(context).listFiles()!!.map {it.name}.toSet()
                val job=launch {
                    val self=coroutineContext.job
                    ExportEngine.render(context,source,ExportSpec(format,seconds=6)) {if(it>.02f) self.cancel()}
                    fail("Expected cancellation")
                }
                job.join();assertTrue(job.isCancelled)
                assertEquals(before,ExportEngine.directory(context).listFiles()!!.map {it.name}.toSet())
                assertTrue(previous.length()>0)
            }
        } finally {previous.delete()}
    }
    private fun click(tag: String) {
        val node=compose.onNodeWithTag(tag)
        if(tag!="open_export") node.performScrollTo()
        node.performClick();compose.waitForIdle()
    }
    @Test fun exportUiPreviewRecreationSaveAndNativeShare() {
        compose.waitForIdle();compose.waitUntil(60000) {!compose.activity.photoSession.loading}
        compose.runOnIdle {compose.activity.photoSession.sample(false)}
        compose.waitUntil(60000) {!compose.activity.photoSession.loading}
        click("resume_animation");click("open_export");click("format_png");click("frame_vertical")
        compose.waitUntil(60000) {compose.onAllNodesWithTag("export_preview").fetchSemanticsNodes().isNotEmpty()}
        val source=compose.activity.exportSession.snapshot
        compose.activityRule.scenario.recreate();compose.waitForIdle()
        assertSame(source,compose.activity.exportSession.snapshot)
        assertEquals(ExportFrame.VERTICAL,compose.activity.exportSession.spec.frame)
        click("export_start")
        compose.waitUntil(60000) {!compose.activity.exportSession.busy}
        compose.waitForIdle();assertNotNull(compose.activity.exportSession.completed)
        compose.onNodeWithTag("export_result").performScrollTo().assertExists()
        val intent=checkNotNull(compose.activity.exportSession.shareIntent(context))
        assertEquals("image/png",intent.type);assertEquals(Intent.ACTION_SEND,intent.action)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION!=0)
        val uri=checkNotNull(intent.clipData).getItemAt(0).uri
        assertEquals("content",uri.scheme)
        context.contentResolver.openInputStream(uri).use {assertNotNull(BitmapFactory.decodeStream(it))}
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        compose.onNodeWithTag("export_preview").performScrollTo()
        device.takeScreenshot(File(folder(),"stage6-export-preview.png"))
        click("export_save");device.waitForIdle();assertFalse(device.currentPackageName==context.packageName)
        device.pressBack();device.waitForIdle();compose.waitForIdle()
        assertNotNull(compose.activity.exportSession.completed)
        click("export_save");device.waitForIdle()
        val saveButton = device.wait(Until.findObject(By.text(java.util.regex.Pattern.compile("(?i)save|guardar"))),15000)
        assertNotNull("Native save button",saveButton);saveButton.click()
        device.waitForIdle()
        compose.waitUntil(60000) {compose.activity.exportSession.notice==com.fourierart.app.R.string.export_saved}
        assertFalse(compose.activity.exportSession.saving)
        click("export_share");device.waitForIdle()
        // Native chooser is launched, but no recipient is selected and no message is sent.
        assertNotNull(compose.activity.exportSession.completed)
        device.pressBack();device.waitForIdle();compose.waitForIdle()
        compose.onNodeWithTag("export_close").performClick();compose.waitForIdle()
        compose.runOnIdle {compose.activity.photoSession.sample(false)}
    }
}
