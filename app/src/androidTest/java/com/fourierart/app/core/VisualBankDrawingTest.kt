package com.fourierart.app.core

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fourierart.app.export.ExportRenderer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Separate diagnostic for the same frozen inputs. Never counted as the primary SUBJECT test. */
@RunWith(AndroidJUnit4::class)
class VisualBankDrawingTest {
    @Test fun collectDrawingDiagnostic() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val assets = instrumentation.context.assets
        assumeTrue("Run the dedicated frozen bank workflow", assets.list("visual-bank")?.contains("manifest.json") == true)
        val folder = File(instrumentation.targetContext.getExternalFilesDir(null), "drawing-bank").apply { mkdirs() }
        val manifest = JSONObject(assets.open("visual-bank/manifest.json").bufferedReader().use { it.readText() })
        val cases = manifest.getJSONArray("cases")
        assertEquals(30, cases.length())
        val environment = JSONObject().put("api", Build.VERSION.SDK_INT).put("device", Build.FINGERPRINT)
        environment.put("diagnostic", "DRAWING profile for every case; does not replace fixed primary profiles")
        File(folder, "environment.json").writeText(environment.toString(2))
        val results = JSONArray()
        for (index in 0 until cases.length()) {
            val case = cases.getJSONObject(index)
            val id = case.getString("id")
            val mode = PhotoContours.Mode.DRAWING
            val record = JSONObject().put("id", id).put("mode", mode.name).put("primaryMode", case.getString("mode"))
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
