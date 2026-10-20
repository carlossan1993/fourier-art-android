package com.fourierart.app.photo

import android.app.Application
import android.graphics.*
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.fourierart.app.MainActivity
import com.fourierart.app.contours.*
import com.fourierart.app.core.PhotoContours
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ContourCorrectionFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val model get()=compose.activity.photoSession
    private fun ready() {compose.waitForIdle();compose.waitUntil(60000) {!model.loading};compose.waitForIdle()}
    private fun tool(name: String) {compose.onNodeWithTag("tool_$name").performScrollTo().performClick()}
    private fun gesture(x0: Float,y0: Float,x1: Float,y1: Float) {
        val photo=model.image!!
        compose.onNodeWithTag("contour_canvas").performTouchInput {
            val scale=kotlin.math.min(width.toFloat()/photo.width,height.toFloat()/photo.height)
            val w=photo.width*scale;val h=photo.height*scale
            val left=(width-w)/2;val top=(height-h)/2
            swipe(Offset(left+x0*w,top+y0*h),Offset(left+x1*w,top+y1*h),500)
        }
        ready()
    }
    private fun draw() {tool("draw");gesture(.2f,.4f,.8f,.6f)}
    @Test fun drawEraseSelectUndoApplyRecoverCancelAndRestore() {
        ready();compose.runOnIdle {model.sample(false)};ready()
        val context=compose.activity.applicationContext
        val file=File(context.cacheDir,"correction-fixture.png")
        val bitmap=Bitmap.createBitmap(160,160,Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {drawColor(Color.WHITE);drawRect(30f,30f,130f,130f,Paint().apply {color=Color.BLACK;style=Paint.Style.STROKE;strokeWidth=3f})}
        file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        compose.runOnIdle {model.mode=PhotoContours.Mode.DRAWING;model.importPhoto(Uri.fromFile(file))};ready()
        compose.onNodeWithTag("apply_photo").performClick();ready()
        val baseline=model.drawing!!.sourceStrokes
        compose.onNodeWithTag("correct_contours").performScrollTo().performClick();ready()
        draw()
        assertEquals(1,model.correctionHistory.current.edits.size)
        assertTrue(model.correctionHistory.current.edits[0] is ContourEdit.Draw)
        compose.onNodeWithTag("contour_undo").performScrollTo().performClick();ready();assertTrue(model.correctionHistory.current.edits.isEmpty())
        compose.onNodeWithTag("contour_redo").performScrollTo().performClick();ready();assertEquals(1,model.correctionHistory.current.edits.size)
        tool("erase")
        gesture(.5f,.3f,.5f,.7f)
        assertTrue(model.correctionHistory.current.edits.last() is ContourEdit.Erase)
        tool("keep")
        gesture(.15f,.15f,.85f,.85f)
        assertTrue(model.correctionHistory.current.edits.last() is ContourEdit.Keep)
        val original=model.image
        val recipe=model.correctionHistory.current
        val expected=model.correctionPreview
        assertTrue(expected.isNotEmpty());assertNotEquals(baseline,expected)
        val folder=File(context.getExternalFilesDir(null),"quality").apply {mkdirs()}
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(File(folder,"stage3-correction.png"))
        compose.onNodeWithTag("contour_apply").performClick();ready()
        assertEquals(recipe,model.correction);assertEquals(expected,model.drawing!!.sourceStrokes);assertSame(original,model.image)
        compose.activityRule.scenario.recreate();ready();assertEquals(recipe,model.correction)
        // New ViewModel reads private files without inheriting any in-memory source or analysis.
        lateinit var recovered: PhotoSessionModel
        val store=ViewModelStore()
        compose.runOnIdle {recovered=PhotoSessionModel(context as Application);store.put("recovery",recovered)}
        compose.waitUntil(60000) {!recovered.loading}
        assertEquals(recipe,recovered.correction);assertNotNull(recovered.image);assertNotNull(recovered.drawing)
        assertEquals(expected,recovered.drawing!!.sourceStrokes)
        compose.runOnIdle {store.clear()}
        compose.onNodeWithTag("correct_contours").performScrollTo().performClick();ready();draw()
        compose.onNodeWithTag("contour_cancel").performClick();ready();assertEquals(recipe,model.correction)
        assertEquals(expected,model.drawing!!.sourceStrokes)
        compose.onNodeWithTag("correct_contours").performScrollTo().performClick();ready()
        compose.onNodeWithTag("contour_reset").performScrollTo().performClick();ready()
        compose.onNodeWithTag("contour_apply").performClick();ready()
        assertEquals(ContourRecipe(),model.correction);assertEquals(baseline,model.drawing!!.sourceStrokes)
        compose.runOnIdle {model.sample(false)};ready();file.delete()
    }
    @Test fun manualDrawingWorksWhenDetectionFindsNoContours() {
        ready();compose.runOnIdle {model.sample(false)};ready()
        val context=compose.activity.applicationContext
        val file=File(context.cacheDir,"empty-correction.png")
        val bitmap=Bitmap.createBitmap(80,80,Bitmap.Config.ARGB_8888).apply {eraseColor(Color.WHITE)}
        file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        compose.runOnIdle {model.mode=PhotoContours.Mode.DRAWING;model.importPhoto(Uri.fromFile(file))};ready()
        compose.onNodeWithTag("apply_photo").performClick();ready()
        assertNull(model.drawing)
        compose.onNodeWithTag("correct_contours").performScrollTo().performClick();ready();draw()
        compose.onNodeWithTag("contour_apply").performClick();ready()
        assertEquals(1,model.drawing!!.sourceStrokes.size);assertFalse(model.drawing!!.sourceStrokes[0].closed)
        compose.onNodeWithTag("correct_contours").performScrollTo().performClick();ready()
        // Erase every line: an empty contour is valid and must not revive stale Fourier strokes.
        compose.runOnIdle {model.addContourEdit(ContourEdit.Erase(listOf(com.fourierart.app.core.Point2(.1,.5),com.fourierart.app.core.Point2(.9,.5)),.15))};ready()
        compose.onNodeWithTag("contour_apply").performClick();ready()
        assertNull(model.drawing);assertEquals(6,model.result)
        compose.runOnIdle {model.sample(false)};ready();file.delete()
    }
    @Test fun jsonPreservesAllCommandsAndRejectsInvalidMetadata() {
        val recipe=ContourRecipe(ContourPreset.SILHOUETTE,42,listOf(
            ContourEdit.Draw(listOf(com.fourierart.app.core.Point2(.1,.2),com.fourierart.app.core.Point2(.8,.9))),
            ContourEdit.Erase(listOf(com.fourierart.app.core.Point2(.5,.4)),.03),ContourEdit.Keep(.1,.1,.9,.9)))
        assertEquals(recipe,ContourRecipeJson.decode(ContourRecipeJson.encode(recipe)))
        assertEquals(ContourRecipe(),ContourRecipeJson.decode(org.json.JSONObject("{\"version\":99}")))
        val corrupt=ContourRecipeJson.encode(recipe).put("cleanup",-1)
        assertEquals(ContourRecipe(),ContourRecipeJson.decode(corrupt))
    }
}
