package com.fourierart.app.core

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fourierart.app.export.ExportRenderer
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit

/** Evidence collection, not an automatic recognizability assertion. No production tuning. */
@RunWith(AndroidJUnit4::class)
class VisualBankTest {
    @Test fun collectFrozenBank() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val assets = instrumentation.context.assets
        assumeTrue("Run the dedicated frozen bank workflow", assets.list("visual-bank")?.contains("manifest.json") == true)
        val folder = File(instrumentation.targetContext.getExternalFilesDir(null), "visual-bank").apply { mkdirs() }
        val manifest = JSONObject(assets.open("visual-bank/manifest.json").bufferedReader().use { it.readText() })
        val cases = manifest.getJSONArray("cases")
        assertEquals(30, cases.length())
        val environment = JSONObject().put("api", Build.VERSION.SDK_INT).put("device", Build.FINGERPRINT)
        environment.put("googlePlayServices", instrumentation.targetContext.packageManager.getPackageInfo("com.google.android.gms", 0).versionName)
        // The accepted app uses the unbundled Google model. Prepare it, not a substitute mask.
        val segmenter = SubjectSegmentation.getClient(SubjectSegmenterOptions.Builder().enableForegroundConfidenceMask().build())
        try {
            val installer = ModuleInstall.getClient(instrumentation.targetContext)
            val request = ModuleInstallRequest.newBuilder().addApi(segmenter).build()
            val install = Tasks.await(installer.installModules(request), 30, TimeUnit.SECONDS)
            environment.put("installSession", install.sessionId)
            val deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(5)
            var available = false
            do {
                available = Tasks.await(installer.areModulesAvailable(segmenter), 15, TimeUnit.SECONDS).areModulesAvailable()
                if (!available) Thread.sleep(2000)
            } while (!available && System.nanoTime() < deadline)
            environment.put("subjectModuleAvailable", available)
        } catch (e: Exception) {
            environment.put("subjectModuleAvailable", false).put("moduleError", e.toString())
        } finally { segmenter.close() }
        File(folder, "environment.json").writeText(environment.toString(2))
        val results = JSONArray()
        for (index in 0 until cases.length()) {
            val case = cases.getJSONObject(index)
            val id = case.getString("id")
            val mode = PhotoContours.Mode.valueOf(case.getString("mode"))
            val record = JSONObject().put("id", id).put("mode", mode.name)
            val start = System.nanoTime()
            val bitmap = assets.open("visual-bank/" + case.getString("file")).use { BitmapFactory.decodeStream(it) }!!
            try {
                val analysis = PhotoContours.analyze(bitmap, mode)
                record.put("analyzeMs", (System.nanoTime() - start) / 1_000_000)
                    .put("faces", analysis.faces.size).put("detectedFaces", analysis.detectedFaceCount)
                    .put("completeFaces", analysis.faces.count { it.complete }).put("plainBackground", analysis.plainBackground)
                    .put("modelUnavailable", analysis.modelUnavailable).put("hasSubjectMask", analysis.confidence != null)
                val extracted = PhotoContours.extract(analysis, mode, 50, DetailLevel.BALANCED, LineStyle.STRUCTURE)
                record.put("kind", extracted.kind.name)
                val drawing = extracted.drawing
                if (drawing != null) {
                    record.put("strokes", drawing.strokes.size)
                    val output = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888)
                    try {
                        val snapshot = ExportSnapshot(drawing, 400, AnimationStyle(epicircles = false, lightBackground = true, color = TraceColor.INK))
                        ExportRenderer(snapshot).draw(Canvas(output), output.width, output.height, 1.0)
                        File(folder, "$id.png").outputStream().use { output.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    } finally { output.recycle() }
                } else record.put("strokes", 0)
            } catch (e: Exception) {
                record.put("error", e.toString())
            } finally { bitmap.recycle() }
            record.put("totalMs", (System.nanoTime() - start) / 1_000_000)
            File(folder, "$id.json").writeText(record.toString(2))
            results.put(record)
            android.util.Log.i("VisualBank", record.toString())
        }
        File(folder, "results.json").writeText(results.toString(2))
        assertEquals(30, results.length())
    }
}
