package com.fourierart.app.photo

import android.app.Application
import android.graphics.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.fourierart.app.MainActivity
import com.fourierart.app.core.*
import com.fourierart.app.player.AnimationJson
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PlaybackControlsAndroidTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val model get()=compose.activity.photoSession
    private fun ready() {compose.waitForIdle();compose.waitUntil(60000) {!model.loading};compose.waitForIdle()}
    private fun setup() {
        ready();compose.runOnIdle {model.sample(false)};ready()
        compose.runOnIdle {model.setAnimationStyle(AnimationStyle());model.seekAnimation(.25)}
        compose.waitUntil(60000) {compose.onAllNodesWithTag("fourier_ready").fetchSemanticsNodes().isNotEmpty()}
    }
    private fun click(tag: String) {compose.onNodeWithTag(tag).performScrollTo().performClick();compose.waitForIdle()}
    @Test fun controlsStyleFramingAndRecreationKeepTheSameDrawing() {
        setup();val original=model.drawing
        click("player_toggle");assertTrue(model.animationPlaying)
        click("player_toggle");assertFalse(model.animationPlaying)
        compose.onNodeWithTag("player_progress").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) {it(.4f)}
        compose.waitForIdle();assertEquals(.4,model.animationPosition,1e-6);assertFalse(model.animationPlaying)
        click("speed_fast");assertEquals(PlaybackSpeed.FAST,model.animationStyle.speed)
        click("player_epicircles");assertFalse(model.animationStyle.epicircles)
        click("player_background");assertTrue(model.animationStyle.lightBackground)
        click("color_rose");assertEquals(TraceColor.ROSE,model.animationStyle.color)
        compose.onNodeWithTag("player_zoom").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) {it(2f)}
        compose.waitForIdle();assertEquals(2.0,model.animationViewport.zoom,1e-6)
        click("player_expand")
        compose.onNodeWithTag("fourier_ready").performTouchInput {swipe(center,center+Offset(width*.15f,height*.06f),500)}
        compose.waitForIdle();assertTrue(model.animationViewport.panX>0)
        val folder=File(compose.activity.getExternalFilesDir(null),"quality").apply {mkdirs()}
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.takeScreenshot(File(folder,"stage5-player-light.png"))
        click("player_background");click("color_teal");click("player_epicircles")
        device.takeScreenshot(File(folder,"stage5-player-dark.png"))
        val style=model.animationStyle;val viewport=model.animationViewport;val position=model.animationPosition
        click("player_close");compose.activityRule.scenario.recreate();ready()
        assertEquals(style,model.animationStyle);assertEquals(viewport,model.animationViewport);assertEquals(position,model.animationPosition,0.0)
        assertSame(original,model.drawing)
        click("player_fit");assertEquals(PreviewViewport(),model.animationViewport)
        click("player_restart");assertTrue(model.animationPlaying);assertFalse(model.showComplete)
        // UI clock is an infinite animation; automatic test-clock advancement leaves it stopped.
        assertEquals(0.0,model.animationPosition,1e-6)
        compose.runOnIdle {model.seekAnimation(0.0);model.setAnimationStyle(AnimationStyle());model.checkpointAnimation()}
        ready()
    }
    @Test fun frameClockPausesAndDoesNotAccumulateBackgroundTime() {
        setup();compose.onNodeWithTag("fourier_ready").performScrollTo();compose.waitForIdle()
        compose.mainClock.autoAdvance=false
        compose.runOnIdle {model.toggleAnimation()}
        compose.mainClock.advanceTimeBy(64);compose.waitForIdle()
        val start=model.animationPosition
        compose.mainClock.advanceTimeBy(1000);compose.waitForIdle()
        assertTrue("Playback clock did not advance",model.animationPosition>start+.04)
        compose.runOnIdle {model.toggleAnimation()}
        compose.mainClock.advanceTimeBy(32);compose.waitForIdle()
        val paused=model.animationPosition
        compose.mainClock.advanceTimeBy(2000);compose.waitForIdle();assertEquals(paused,model.animationPosition,0.0)
        compose.runOnIdle {model.toggleAnimation()}
        compose.mainClock.advanceTimeBy(32);compose.waitForIdle()
        compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        val background=model.animationPosition
        compose.mainClock.advanceTimeBy(4000);assertEquals(background,model.animationPosition,0.0)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.mainClock.advanceTimeBy(32);compose.waitForIdle()
        assertTrue("Resuming jumped over background time",model.animationPosition-background<.02)
        compose.runOnIdle {model.seekAnimation(0.0)}
        compose.mainClock.autoAdvance=true;ready()
    }
    @Test fun savedPhotoRecoversStylePositionAndFramingAndRejectsInvalidValues() {
        setup()
        val app=compose.activity.applicationContext as Application
        val store=PhotoSessionStore(app)
        val bitmap=Bitmap.createBitmap(80,80,Bitmap.Config.ARGB_8888).apply {eraseColor(Color.WHITE)}
        val snapshot=AnimationJson.Snapshot(AnimationStyle(PlaybackSpeed.VERY_FAST,false,TraceColor.GOLD,true),.42,false,PreviewViewport(2.0,.2,-.1))
        val encoded=AnimationJson.encode(snapshot)
        assertEquals(snapshot,AnimationJson.decode(encoded))
        val invalid=JSONObject().put("zoom",99).put("position",-1).put("style",JSONObject().put("speed","unknown").put("color","unknown"))
        val fallback=AnimationJson.decode(invalid)
        assertEquals(0.0,fallback.position,0.0);assertEquals(PreviewViewport(),fallback.viewport);assertEquals(AnimationStyle(),fallback.style)
        val name=store.saveSource(bitmap)
        store.write(name,PhotoPreparation(),PhotoPreparation(),false,JSONObject().put("mode","DRAWING").put("animation",encoded))
        lateinit var recovered: PhotoSessionModel
        val owners=ViewModelStore()
        compose.runOnIdle {recovered=PhotoSessionModel(app);owners.put("recovered",recovered)}
        compose.waitUntil(60000) {!recovered.loading}
        assertNotNull(recovered.image);assertEquals(snapshot.style,recovered.animationStyle)
        assertEquals(snapshot.position,recovered.animationPosition,0.0);assertEquals(snapshot.playing,recovered.animationPlaying);assertEquals(snapshot.viewport,recovered.animationViewport)
        compose.runOnIdle {owners.clear()};store.clear()
    }
}
