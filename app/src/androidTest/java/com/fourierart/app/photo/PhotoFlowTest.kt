package com.fourierart.app.photo

import android.graphics.*
import android.net.Uri
import android.os.SystemClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.fourierart.app.MainActivity
import com.fourierart.app.core.PhotoContours
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.regex.Pattern

@RunWith(AndroidJUnit4::class)
class PhotoFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun ready() {compose.waitUntil(60000) {!compose.activity.photoSession.loading}}
    @Test fun prepareRecreatePickerCancellationPermissionDenialAndRealCameraCapture() {
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        ready();compose.runOnIdle {compose.activity.photoSession.sample(false)};ready()
        val context=compose.activity.applicationContext
        val input=File(context.cacheDir,"flow-input.png")
        val b=Bitmap.createBitmap(120,80,Bitmap.Config.ARGB_8888)
        Canvas(b).apply {drawColor(Color.WHITE);drawCircle(60f,40f,25f,Paint().apply {color=Color.BLACK})}
        input.outputStream().use {b.compress(Bitmap.CompressFormat.PNG,100,it)}
        compose.runOnIdle {compose.activity.photoSession.apply {mode=PhotoContours.Mode.DRAWING;importPhoto(Uri.fromFile(input))}}
        ready();compose.onNodeWithTag("photo_editor").assertExists()
        compose.onNodeWithTag("crop_canvas").performScrollTo().performTouchInput {
            val factor=kotlin.math.min(width/120f,height/80f)
            val left=(width-120*factor)/2;val top=(height-80*factor)/2
            swipe(Offset(left+2,top+2),Offset(left+20*factor,top+12*factor),500)
        }
        compose.runOnIdle {assertTrue(compose.activity.photoSession.draft.crop.left>0.1f)}
        compose.onNodeWithTag("rotate_right").performScrollTo().performClick();ready()
        val model=compose.activity.photoSession;val crop=model.draft
        compose.activityRule.scenario.recreate();ready()
        compose.runOnIdle {assertSame(model,compose.activity.photoSession);assertEquals(crop,compose.activity.photoSession.draft)}
        compose.onNodeWithTag("apply_photo").performScrollTo().performClick();ready()
        compose.runOnIdle {assertFalse(model.editing);assertNotNull(model.image);assertNotNull(model.drawing)}
        val prepared=model.image
        compose.onNodeWithTag("choose_photo").performScrollTo().performClick()
        assertTrue("Photo Picker did not open",device.wait(Until.hasObject(By.pkg(Pattern.compile(".*(documentsui|providers.media|photopicker).*"))),10000));device.pressBack()
        compose.waitUntil(10000) {compose.onAllNodesWithTag("choose_photo").fetchSemanticsNodes().isNotEmpty()}
        compose.runOnIdle {assertSame(prepared,model.image)}
        compose.onNodeWithTag("take_photo").performScrollTo().performClick()
        val deny=device.wait(Until.findObject(By.res(Pattern.compile(".*:id/permission_deny_button"))),10000)
        assertNotNull("Camera permission was not requested on first use",deny);deny.click()
        compose.onNodeWithTag("photo_notice").assertExists()
        compose.runOnIdle {assertSame(prepared,model.image)}
        compose.onNodeWithTag("take_photo").performScrollTo().performClick()
        val allow=device.wait(Until.findObject(By.res(Pattern.compile(".*:id/permission_allow(_foreground_only)?_button"))),10000)
        assertNotNull("Camera permission dialog did not return",allow);allow.click()
        compose.waitUntil(30000) {compose.onAllNodesWithTag("camera_shutter").fetchSemanticsNodes().firstOrNull()?.config?.contains(SemanticsProperties.Disabled)==false}
        compose.onNodeWithTag("camera_shutter").assertIsEnabled()
        val folder=File(context.getExternalFilesDir(null),"quality").apply {mkdirs()}
        device.takeScreenshot(File(folder,"stage2-camera.png"))
        compose.onNodeWithTag("camera_cancel").performClick()
        compose.runOnIdle {assertSame(prepared,model.image)}
        compose.onNodeWithTag("take_photo").performScrollTo().performClick()
        compose.waitUntil(30000) {compose.onAllNodesWithTag("camera_shutter").fetchSemanticsNodes().firstOrNull()?.config?.contains(SemanticsProperties.Disabled)==false}
        compose.onNodeWithTag("camera_shutter").assertIsEnabled().performClick()
        compose.waitUntil(30000) {model.editing && !model.loading}
        assertNotNull(model.source);assertTrue(model.captures.listFiles().orEmpty().isEmpty())
        compose.onNodeWithTag("crop_canvas").performScrollTo()
        device.takeScreenshot(File(folder,"stage2-editor.png"))
        compose.onNodeWithTag("editor_cancel").performScrollTo().performClick();ready()
        compose.runOnIdle {assertNull(model.source);assertNull(model.image)}
        input.delete()
    }
}
