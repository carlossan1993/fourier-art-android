package com.fourierart.app.photo

import android.content.pm.ActivityInfo
import android.graphics.*
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.os.LocaleListCompat
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

@RunWith(AndroidJUnit4::class)
class ExperienceFlowAndroidTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val model get()=compose.activity.photoSession
    private val device get()=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private fun ready() {compose.waitForIdle();compose.waitUntil(60000){!model.loading};compose.waitForIdle()}
    private fun click(tag: String, fixed: Boolean=false) {
        val node=compose.onNodeWithTag(tag);if(!fixed)node.performScrollTo();node.performClick();compose.waitForIdle()
    }
    private fun screenshot(name: String) {
        val folder=File(compose.activity.getExternalFilesDir(null),"quality/experience").apply{mkdirs()}
        device.takeScreenshot(File(folder,"stage7-$name.png"))
    }
    private fun home() {
        ready();compose.runOnIdle {model.sample(false)};ready()
        // A fresh Activity starts home; returning from a previous test is handled by system Back.
        if(compose.onAllNodesWithTag("screen_home").fetchSemanticsNodes().isEmpty()) {device.pressBack();ready()}
        compose.onNodeWithTag("screen_home").assertExists()
    }
    private fun fixture() : File {
        val file=File(compose.activity.cacheDir,"stage7-input.png")
        val b=Bitmap.createBitmap(160,120,Bitmap.Config.ARGB_8888)
        Canvas(b).apply {drawColor(Color.WHITE);drawCircle(80f,60f,40f,Paint().apply{color=Color.BLACK;style=Paint.Style.STROKE;strokeWidth=3f})}
        file.outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)};b.recycle();return file
    }
    @Test fun homeGuideSettingsLanguageAndDemoAreDiscoverable() {
        home();screenshot("home")
        click("open_guide",true);compose.onNodeWithTag("screen_guide").assertExists();screenshot("guide")
        click("nav_back",true);click("open_settings",true)
        click("language_es");compose.waitUntil(15000){compose.activity.resources.configuration.locales[0].language=="es"};ready()
        compose.onNodeWithTag("screen_settings").assertExists();compose.onNodeWithText("Ajustes").assertExists()
        screenshot("settings-es")
        click("language_en");compose.waitUntil(15000){compose.activity.resources.configuration.locales[0].language=="en"};ready()
        compose.onNodeWithTag("screen_settings").assertExists()
        click("nav_back",true);click("resume_animation")
        compose.onNodeWithTag("screen_player").assertExists()
        click("open_parameters");compose.onNodeWithTag("screen_parameters").assertExists()
        click("color_teal");compose.onNodeWithTag("color_teal").assertIsSelected()
        val drawing=model.drawing
        compose.activityRule.scenario.recreate();ready()
        compose.onNodeWithTag("screen_parameters").assertExists();assertSame(drawing,model.drawing)
        screenshot("parameters")
        click("parameters_done",true);screenshot("player")
        click("open_export",true);click("format_png");compose.onNodeWithTag("format_png").assertIsSelected()
        click("export_start");compose.waitUntil(60000){!compose.activity.exportSession.busy};compose.waitForIdle()
        compose.onNodeWithTag("export_result").performScrollTo().assertExists();assertNotNull(compose.activity.exportSession.completed)
        screenshot("export-result")
        compose.onNodeWithTag("export_close").performClick();ready()
        click("nav_back",true)
        compose.runOnIdle{model.updateAnimationStyle(AnimationStyle());AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())}
    }
    @Test fun photoPreparationCanResumeAndEmptyContoursOfferCorrection() {
        home();val file=fixture()
        compose.runOnIdle{model.mode=PhotoContours.Mode.DRAWING;model.importPhoto(Uri.fromFile(file))};ready()
        compose.onNodeWithTag("screen_prepare").assertExists();val draft=model.draft
        screenshot("prepare")
        click("nav_back",true);compose.onNodeWithTag("screen_home").assertExists()
        click("resume_work");compose.onNodeWithTag("screen_prepare").assertExists();assertEquals(draft,model.draft)
        click("apply_photo",true);ready()
        compose.onNodeWithTag("screen_review").assertExists();screenshot("review")
        compose.onNodeWithTag("start_animation").assertIsEnabled()
        click("start_animation",true);compose.onNodeWithTag("screen_player").assertExists()
        compose.activityRule.scenario.recreate();ready();compose.onNodeWithTag("screen_player").assertExists()
        val photoDrawing=model.drawing
        click("open_export",true);click("format_png");click("export_start")
        compose.waitUntil(60000){!compose.activity.exportSession.busy};compose.waitForIdle()
        assertSame(photoDrawing,compose.activity.exportSession.snapshot!!.drawing)
        assertNotNull(compose.activity.exportSession.completed)
        screenshot("photo-export")
        compose.onNodeWithTag("export_close").performClick();ready()
        click("nav_back",true);click("correct_contours")
        compose.runOnIdle{model.addContourEdit(com.fourierart.app.contours.ContourEdit.Erase((0..10).flatMap{row ->
            val y=row/10.0;if(row%2==0)listOf(Point2(0.0,y),Point2(1.0,y))else listOf(Point2(1.0,y),Point2(0.0,y))
        },.15))};ready()
        compose.onNodeWithTag("contour_apply").performClick();ready()
        assertNull(model.drawing)
        compose.onNodeWithTag("start_animation").assertIsNotEnabled()
        compose.onNodeWithTag("correct_contours").assertIsEnabled()
        screenshot("empty-contours")
        click("nav_back",true);click("open_settings",true);click("settings_discard")
        compose.onNodeWithTag("confirm_discard").performClick();ready()
        assertNull(model.source);compose.onNodeWithTag("screen_home").assertExists();file.delete()
    }
    @Test fun largeTextAndLandscapeKeepNavigationAndExportAccessible() {
        home()
        try {
            device.executeShellCommand("settings put system font_scale 1.5")
            compose.activityRule.scenario.recreate();ready()
            screenshot("large-text-home")
            click("resume_animation");click("open_parameters")
            compose.onNodeWithTag("fourier_components").performScrollTo().assertExists()
            screenshot("large-text-parameters")
            click("parameters_done",true)
            compose.runOnIdle{compose.activity.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}
            device.waitForIdle();ready();compose.onNodeWithTag("screen_player").assertExists()
            click("open_export",true)
            compose.onNodeWithTag("format_png").performScrollTo().performClick()
            compose.onNodeWithTag("export_start").performScrollTo().assertIsEnabled()
            screenshot("landscape-export")
            compose.onNodeWithTag("export_close").performClick()
        } finally {
            device.executeShellCommand("settings put system font_scale 1.0")
            compose.runOnIdle{compose.activity.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}
            compose.activityRule.scenario.recreate();ready()
        }
    }
}
