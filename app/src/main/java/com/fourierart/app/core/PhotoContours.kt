package com.fourierart.app.core

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Inference is cached; sensitivity affects only bounded local raster work. */
object PhotoContours {
    const val RASTER_SIDE = 1024
    enum class Mode { AUTO, SUBJECT, DRAWING }
    enum class Kind { FACE, FACE_PARTIAL, SUBJECT, DRAWING, MODEL_UNAVAILABLE, NO_SUBJECT }
    data class Analysis(
        val width: Int, val height: Int, val pixels: IntArray,
        val faces: List<FacialGeometry>, val detectedFaceCount: Int, val confidence: FloatArray?,
        val plainBackground: Boolean, val modelUnavailable: Boolean
    ) { val face: FacialGeometry? get() = faces.firstOrNull() }
    data class Result(val drawing: ContourDrawing?, val kind: Kind, val silhouette: List<ContourStroke> = emptyList())

    // Worker dispatcher only. Facial inference uses the original oriented bitmap, not the small raster.
    fun analyze(input: Bitmap, mode: Mode): Analysis {
        val factor = minOf(1.0, RASTER_SIDE.toDouble() / max(input.width, input.height))
        val width = max(1, (input.width * factor).toInt())
        val height = max(1, (input.height * factor).toInt())
        val bitmap = Bitmap.createScaledBitmap(input, width, height, true)
        try {
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            // Protect facial structure in Drawing too: selecting this profile must not delete an eye.
            val (faces, faceCount) = if (mode != Mode.SUBJECT) faceGeometries(input, width, height) else emptyList<FacialGeometry>() to 0
            val simple = RasterContours.plainBackgroundMask(pixels, width, height, 50)
            var confidence: FloatArray? = null
            var unavailable = false
            if (mode != Mode.DRAWING && simple == null) {
                val client = SubjectSegmentation.getClient(SubjectSegmenterOptions.Builder()
                    .enableForegroundConfidenceMask().build())
                try {
                    val result = Tasks.await(client.process(InputImage.fromBitmap(bitmap, 0)), 15, TimeUnit.SECONDS)
                    val mask = result.foregroundConfidenceMask
                    if (mask != null && mask.limit() == pixels.size) confidence = FloatArray(pixels.size) { mask[it] }
                    else unavailable = true
                } catch (e: Exception) {
                    unavailable = true
                    if (e is InterruptedException) { Thread.currentThread().interrupt(); throw e }
                } finally { client.close() }
            }
            return Analysis(width, height, pixels, faces, faceCount, confidence, simple != null, unavailable)
        } finally { if (bitmap !== input) bitmap.recycle() }
    }

    fun extract(a: Analysis, mode: Mode, sensitivity: Int, detail: DetailLevel = DetailLevel.BALANCED, style: LineStyle = LineStyle.STRUCTURE): Result {
        val width = a.width; val height = a.height; val pixels = a.pixels
        var mask = if (a.plainBackground) RasterContours.plainBackgroundMask(pixels, width, height, sensitivity) else null
        if (mask == null && a.confidence != null) {
            val threshold = 0.7f - sensitivity.coerceIn(0, 100) * 0.004f
            mask = RasterContours.subjectComponents(BooleanArray(pixels.size) { a.confidence[it] >= threshold }, width, height)
        }
        mask = mask?.takeIf { m -> val area = m.count { it }; area > pixels.size * 0.005 && area < pixels.size * 0.94 }
        val outline = mask?.let { m -> RasterContours.outlines(m, width, height, rejectFrame = false)
            .filter { abs(RasterContours.area(it.points)) >= pixels.size * 0.0004 }.take(32)
            .flatMap { StrokeRefinement.clipFrame(it, width, height) } }.orEmpty()
        if (mode != Mode.DRAWING && a.face == null && (mask == null || outline.isEmpty())) {
            return Result(null, if (a.modelUnavailable) Kind.MODEL_UNAVAILABLE else Kind.NO_SUBJECT)
        }
        val regions = a.faces.mapNotNull { f -> f.oval.takeIf { it.size >= 3 }?.let { oval ->
            val x0 = oval.minOf { it.x }; val x1 = oval.maxOf { it.x }
            val y0 = oval.minOf { it.y }; val y1 = oval.maxOf { it.y }
            val dx = (x1 - x0) * 0.55; val dy = y1 - y0
            doubleArrayOf(x0 - dx, y0 - dy * 0.55, x1 + dx, y1 + dy * 0.3)
        } }
        val allowed = mask ?: if (mode == Mode.AUTO && regions.isNotEmpty()) {
            BooleanArray(pixels.size) { i ->
                val x = (i % width).toDouble(); val y = (i / width).toDouble()
                regions.any { x in it[0]..it[2] && y in it[1]..it[3] }
            }
        } else null
        val candidates = if (style == LineStyle.STRUCTURE) {
            StructuralContours.extract(pixels, width, height, sensitivity, allowed, detail)
        } else {
            val edges = RasterContours.edges(pixels, width, height, sensitivity, allowed)
            val raw = RasterContours.lineStrokes(edges, width, height, max(3.0, max(width, height) * 0.004))
            StrokeRefinement.select(raw, width, height, 1600)
        }
        val structural = RasterContours.edges(pixels, width, height, sensitivity, allowed, blurPasses = 3)
        val strokes: List<ContourStroke>
        val kind: Kind
        if (a.faces.isNotEmpty()) {
            strokes = PortraitComposer.composeFaces(a.faces, pixels, width, height, outline, candidates,
                structural, detail.budget)
            kind = if (a.faces.size == a.detectedFaceCount && a.faces.all { it.complete }) Kind.FACE else Kind.FACE_PARTIAL
        } else {
            val selected = if (style == LineStyle.STRUCTURE) candidates else {
                val joined = StrokeRefinement.join(candidates, max(1.5, max(width, height) * 0.004))
                DetailSelector.select(joined, pixels, width, height, structural, detail.budget).map { StrokeRefinement.smooth(it) }
            }
            strokes = outline.map { StrokeRefinement.smooth(it) } + selected
            kind = if (mode == Mode.DRAWING) Kind.DRAWING else Kind.SUBJECT
        }
        return if (strokes.isEmpty()) Result(null, Kind.NO_SUBJECT) else Result(ContourDrawing.fromStrokes(strokes), kind,
            outline.ifEmpty { a.faces.mapNotNull { it.parts[FacePart.OVAL] } })
    }

    /** First detect ALL boxes without contour mode, then run contours on each individual crop. */
    private fun faceGeometries(input: Bitmap, width: Int, height: Int): Pair<List<FacialGeometry>, Int> {
        val boxesClient = FaceDetection.getClient(FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setMinFaceSize(0.04f).build())
        val contourClient = FaceDetection.getClient(FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setMinFaceSize(0.08f).build())
        val faces = ArrayList<FacialGeometry>()
        var detected = 0
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        fun remaining() = max(1, TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime()))
        try {
            val boxes = Tasks.await(boxesClient.process(InputImage.fromBitmap(input, 0)), remaining(), TimeUnit.MILLISECONDS)
                .sortedByDescending { it.boundingBox.width().toLong() * it.boundingBox.height() }
            detected = boxes.size
            for (first in boxes.take(8)) {
                if (System.nanoTime() >= deadline) break
                val box = first.boundingBox
                val marginX = box.width() * 0.25; val marginY = box.height() * 0.25
                val left = max(0, (box.left - marginX).toInt()); val top = max(0, (box.top - marginY).toInt())
                val right = min(input.width, (box.right + marginX).toInt()); val bottom = min(input.height, (box.bottom + marginY).toInt())
                if (right <= left || bottom <= top) continue
                val crop = Bitmap.createBitmap(input, left, top, right - left, bottom - top)
                var focused: Bitmap? = null
                try {
                    val scale = 800.0 / max(crop.width, crop.height)
                    val patch = Bitmap.createScaledBitmap(crop, max(1, (crop.width * scale).toInt()), max(1, (crop.height * scale).toInt()), true)
                    focused = patch
                    val expectedX = (box.exactCenterX() - left) * patch.width / crop.width
                    val expectedY = (box.exactCenterY() - top) * patch.height / crop.height
                    val refined = Tasks.await(contourClient.process(InputImage.fromBitmap(patch, 0)), remaining(), TimeUnit.MILLISECONDS)
                        .minByOrNull { kotlin.math.hypot((it.boundingBox.exactCenterX() - expectedX).toDouble(),
                            (it.boundingBox.exactCenterY() - expectedY).toDouble()) } ?: continue
                    if (abs(refined.boundingBox.exactCenterX() - expectedX) > patch.width * 0.25 ||
                        abs(refined.boundingBox.exactCenterY() - expectedY) > patch.height * 0.25) continue
                    val sx = crop.width.toDouble() / patch.width * width / input.width
                    val sy = crop.height.toDouble() / patch.height * height / input.height
                    val result = geometry(refined, sx, sy, left.toDouble() * width / input.width, top.toDouble() * height / input.height)
                    if (result.parts.isNotEmpty()) faces.add(result)
                } catch (e: Exception) {
                    if (e is InterruptedException) { Thread.currentThread().interrupt(); throw e }
                    // One failed crop must not discard other people's detected features.
                } finally {
                    if (focused != null && focused !== crop && focused !== input) focused.recycle()
                    if (crop !== input) crop.recycle()
                }
            }
        } catch (e: Exception) {
            if (e is InterruptedException) { Thread.currentThread().interrupt(); throw e }
        } finally { boxesClient.close(); contourClient.close() }
        return faces to detected
    }

    private fun geometry(face: Face, sx: Double, sy: Double, ox: Number, oy: Number): FacialGeometry {
        val mapping = mapOf(FacePart.OVAL to FaceContour.FACE, FacePart.LEFT_EYE to FaceContour.LEFT_EYE,
            FacePart.RIGHT_EYE to FaceContour.RIGHT_EYE, FacePart.LEFT_BROW_TOP to FaceContour.LEFT_EYEBROW_TOP,
            FacePart.LEFT_BROW_BOTTOM to FaceContour.LEFT_EYEBROW_BOTTOM, FacePart.RIGHT_BROW_TOP to FaceContour.RIGHT_EYEBROW_TOP,
            FacePart.RIGHT_BROW_BOTTOM to FaceContour.RIGHT_EYEBROW_BOTTOM, FacePart.NOSE_BRIDGE to FaceContour.NOSE_BRIDGE,
            FacePart.NOSE_BOTTOM to FaceContour.NOSE_BOTTOM, FacePart.UPPER_LIP_TOP to FaceContour.UPPER_LIP_TOP,
            FacePart.UPPER_LIP_BOTTOM to FaceContour.UPPER_LIP_BOTTOM, FacePart.LOWER_LIP_TOP to FaceContour.LOWER_LIP_TOP,
            FacePart.LOWER_LIP_BOTTOM to FaceContour.LOWER_LIP_BOTTOM)
        val closed = setOf(FacePart.OVAL, FacePart.LEFT_EYE, FacePart.RIGHT_EYE)
        return FacialGeometry(mapping.mapNotNull { (part, type) ->
            face.getContour(type)?.points?.takeIf { it.size >= (if (part in closed) 3 else 2) }?.let { points ->
                part to ContourStroke(points.map { Point2(it.x * sx + ox.toDouble(), it.y * sy + oy.toDouble()) }, part in closed)
            }
        }.toMap())
    }
}
