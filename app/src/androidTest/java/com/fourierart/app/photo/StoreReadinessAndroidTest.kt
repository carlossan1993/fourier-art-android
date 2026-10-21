package com.fourierart.app.photo

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.os.LocaleListCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.fourierart.app.MainActivity
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class StoreReadinessAndroidTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private fun ready() {
        compose.waitForIdle()
        compose.waitUntil(60000) { !compose.activity.photoSession.loading }
        compose.waitForIdle()
    }
    private fun click(tag: String, fixed: Boolean = false) {
        val node = compose.onNodeWithTag(tag)
        if (!fixed) node.performScrollTo()
        node.performClick(); ready()
    }
    private fun capture(locale: String, name: String) {
        ready(); InstrumentationRegistry.getInstrumentation().waitForIdleSync(); device.waitForIdle()
        val root = File(compose.activity.getExternalFilesDir(null), "quality/store/$locale").apply { mkdirs() }
        assertTrue(device.takeScreenshot(File(root, "$name.png")))
    }

    @Test fun policyAvailableOfflineInBothLanguagesAndSurvivesRecreation() {
        try {
            compose.runOnIdle { compose.activity.photoSession.sample(false) }; ready()
            for (locale in listOf("es", "en")) {
                click("open_settings", true)
                click("language_$locale")
                compose.waitUntil(15000) { compose.activity.resources.configuration.locales[0].language == locale }; ready()
                click("nav_back", true)
                compose.onNodeWithTag("screen_home").assertExists(); capture(locale, "01-home")
                click("sample_star")
                compose.runOnIdle { compose.activity.photoSession.seekAnimation(.78) }; ready()
                compose.onNodeWithTag("screen_player").assertExists(); capture(locale, "02-animation")
                click("open_parameters")
                compose.onNodeWithTag("screen_parameters").assertExists(); capture(locale, "03-controls")
                click("parameters_done", true); click("open_export", true)
                click("format_png")
                compose.onNodeWithTag("export_start").performScrollTo().assertIsEnabled()
                capture(locale, "04-export")
                compose.onNodeWithTag("export_close").performClick(); ready()
                click("nav_back", true); click("open_settings", true); click("open_privacy")
                compose.onNodeWithTag("screen_privacy").assertExists(); capture(locale, "05-privacy")
                compose.activityRule.scenario.recreate(); ready()
                compose.onNodeWithTag("screen_privacy").assertExists()
                val bodies = compose.activity.resources.getStringArray(com.fourierart.app.R.array.privacy_section_bodies)
                assertEquals(6, bodies.size)
                compose.onNodeWithText(bodies[3], useUnmergedTree = true).performScrollTo().assertIsDisplayed()
                assertTrue(bodies[3].contains("HTTPS")); capture(locale, "06-google-disclosure")
                compose.onNodeWithText(bodies.last(), useUnmergedTree = true).performScrollTo().assertIsDisplayed()
                device.pressBack(); ready(); compose.onNodeWithTag("screen_settings").assertExists()
                click("nav_back", true)
            }
        } finally {
            compose.runOnIdle { AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList()) }
        }
    }
}
