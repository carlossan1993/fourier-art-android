package com.fourierart.app.core

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max

/** Model inference is cached per imported photo. Sensitivity only reprocesses bounded local rasters. */
object PhotoContours {
    enum class Mode { AUTO, SUBJECT, DRAWING }
    enum class Kind { FACE, SUBJECT, DRAWING, MODEL_UNAVAILABLE, NO_SUBJECT }
    data class Analysis(
        val width: Int,
        val height: Int,
        val pixels: IntArray,
        val facialStrokes: List<ContourStroke>,
        val confidence: FloatArray?,
        val modelUnavailable: Boolean
    )
    data class Result(val drawing: ContourDrawing?, val kind: Kind)

    // Must run on a worker dispatcher, never the UI thread.
    fun analyze(input: Bitmap, mode: Mode): Analysis {
        val factor = minOf(1.0, 640.0 / max(input.width, input.height))
        val width = max(1, (input.width * factor).toInt())
        val height = max(1, (input.height * factor).toInt())
        val bitmap = Bitmap.createScaledBitmap(input, width, height, true)
        try {
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val image = InputImage.fromBitmap(bitmap, 0) // EXIF already applied at import.
            val facial = if (mode == Mode.AUTO) faceStrokes(image) else emptyList()
            var confidence: FloatArray? = null
            var unavailable = false
            if (mode != Mode.DRAWING && facial.isEmpty()) {
                val client = SubjectSegmentation.getClient(SubjectSegmenterOptions.Builder()
                    .enableForegroundConfidenceMask().build())
                try {
                    val result = Tasks.await(client.process(image), 25, TimeUnit.SECONDS)
                    val mask = result.foregroundConfidenceMask
                    if (mask != null && mask.limit() == pixels.size) {
                        confidence = FloatArray(pixels.size) { mask[it] }
                    } else unavailable = true
                } catch (e: Exception) {
                    // Includes a first-use model download, missing Play services or timeout.
                    unavailable = true
                    if (e is InterruptedException) { Thread.currentThread().interrupt(); throw e }
                } finally { client.close() }
            }
            return Analysis(width, height, pixels, facial, confidence, unavailable)
        } finally { if (bitmap !== input) bitmap.recycle() }
    }

    fun extract(analysis: Analysis, mode: Mode, sensitivity: Int): Result {
        val (width, height, pixels, facial, confidence, unavailable) = analysis
        if (mode == Mode.AUTO && facial.isNotEmpty()) return Result(ContourDrawing.fromStrokes(facial), Kind.FACE)
        if (mode != Mode.DRAWING && confidence == null) {
            return Result(null, if (unavailable) Kind.MODEL_UNAVAILABLE else Kind.NO_SUBJECT)
        }
        val strokes: List<ContourStroke>
        val kind: Kind
        if (mode == Mode.DRAWING) {
            // Explicit non-ML path for logos/drawings and devices without Play services.
            val lines = RasterContours.edges(pixels, width, height, sensitivity)
            strokes = RasterContours.lineStrokes(lines, width, height, max(10.0, max(width, height) * 0.025))
            kind = Kind.DRAWING
        } else {
            val threshold = 0.7f - sensitivity.coerceIn(0, 100) * 0.004f
            val mask = RasterContours.largestComponent(BooleanArray(pixels.size) { confidence!![it] >= threshold }, width, height)
            val area = mask.count { it }
            if (area < pixels.size * 0.005 || area > pixels.size * 0.94) return Result(null, Kind.NO_SUBJECT)
            val outlines = RasterContours.outlines(mask, width, height)
                .filter { abs(RasterContours.area(it.points)) >= pixels.size * 0.0005 }.take(5)
            if (outlines.isEmpty()) return Result(null, Kind.NO_SUBJECT)
            val lines = RasterContours.edges(pixels, width, height, sensitivity, mask)
            val details = RasterContours.lineStrokes(lines, width, height, max(14.0, max(width, height) * 0.035)).take(24)
            strokes = outlines + details
            kind = Kind.SUBJECT
        }
        return Result(if (strokes.isEmpty()) null else ContourDrawing.fromStrokes(strokes), if (strokes.isEmpty()) Kind.NO_SUBJECT else kind)
    }

    private fun faceStrokes(image: InputImage): List<ContourStroke> {
        val client = FaceDetection.getClient(FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setMinFaceSize(0.15f).build())
        try {
            val face = Tasks.await(client.process(image), 20, TimeUnit.SECONDS)
                .maxByOrNull { it.boundingBox.width() * it.boundingBox.height() } ?: return emptyList()
            val closed = setOf(FaceContour.FACE, FaceContour.LEFT_EYE, FaceContour.RIGHT_EYE)
            val types = listOf(FaceContour.FACE, FaceContour.LEFT_EYEBROW_TOP, FaceContour.LEFT_EYEBROW_BOTTOM,
                FaceContour.RIGHT_EYEBROW_TOP, FaceContour.RIGHT_EYEBROW_BOTTOM, FaceContour.LEFT_EYE,
                FaceContour.RIGHT_EYE, FaceContour.NOSE_BRIDGE, FaceContour.NOSE_BOTTOM,
                FaceContour.UPPER_LIP_TOP, FaceContour.UPPER_LIP_BOTTOM, FaceContour.LOWER_LIP_TOP, FaceContour.LOWER_LIP_BOTTOM)
            val strokes = types.mapNotNull { type ->
                face.getContour(type)?.points?.takeIf { it.size >= 3 }?.let { points ->
                    ContourStroke(points.map { Point2(it.x.toDouble(), it.y.toDouble()) }, type in closed)
                }
            }
            // Reject an incomplete face instead of returning an isolated oval as a portrait.
            return if (strokes.size >= 5 && strokes.any { it.closed && it.points.size >= 30 }) strokes else emptyList()
        } catch (e: Exception) {
            if (e is InterruptedException) { Thread.currentThread().interrupt(); throw e }
            return emptyList()
        } finally { client.close() }
    }
}
