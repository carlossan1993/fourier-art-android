package com.fourierart.app.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class FacePart {
    OVAL, LEFT_EYE, RIGHT_EYE, LEFT_BROW_TOP, LEFT_BROW_BOTTOM, RIGHT_BROW_TOP, RIGHT_BROW_BOTTOM,
    NOSE_BRIDGE, NOSE_BOTTOM, UPPER_LIP_TOP, UPPER_LIP_BOTTOM, LOWER_LIP_TOP, LOWER_LIP_BOTTOM
}

data class FacialGeometry(val parts: Map<FacePart, ContourStroke>) {
    val oval: List<Point2> get() = parts[FacePart.OVAL]?.points.orEmpty()
    val complete: Boolean get() = listOf(FacePart.LEFT_EYE, FacePart.RIGHT_EYE, FacePart.NOSE_BOTTOM,
        FacePart.UPPER_LIP_TOP, FacePart.LOWER_LIP_BOTTOM).all { (parts[it]?.points?.size ?: 0) >= 3 } && oval.size >= 20
}

/** Semantic strokes are never subjected to a raster length filter or detail budget. */
object PortraitComposer {
    fun compose(face: FacialGeometry, pixels: IntArray, width: Int, height: Int,
        outline: List<ContourStroke>, details: List<ContourStroke>): List<ContourStroke> {
        val parts = face.parts
        val semantic = ArrayList<ContourStroke>()
        fun add(part: FacePart) { parts[part]?.let { semantic.add(it) } }
        // Head silhouette contains hair/ears, jaw remains an independent internal line under the chin.
        val eyesY = listOfNotNull(parts[FacePart.LEFT_EYE], parts[FacePart.RIGHT_EYE])
            .flatMap { it.points }.map { it.y }.average()
        val oval = face.oval
        if (oval.isNotEmpty()) {
            val top = oval.minOf { it.y }; val span = oval.maxOf { it.y } - top
            val cutoff = if (eyesY.isFinite()) eyesY - span * 0.13 else top + span * 0.25
            val start = oval.indexOfFirst { it.y < cutoff }.coerceAtLeast(0)
            val rotated = oval.drop(start) + oval.take(start)
            val runs = split(rotated, { it.y >= cutoff })
            runs.maxByOrNull { it.size }?.takeIf { it.size >= 3 }?.let { semantic.add(ContourStroke(it, false)) }
        }
        add(FacePart.LEFT_EYE); add(FacePart.RIGHT_EYE)
        fun joined(a: FacePart, b: FacePart) {
            val p = parts[a]?.points; val q = parts[b]?.points
            if (p == null || q == null) { add(a); add(b); return }
            val oriented = if (StrokeRefinement.distance(p.last(), q.first()) < StrokeRefinement.distance(p.last(), q.last())) q else q.reversed()
            semantic.add(ContourStroke(p + oriented, true))
        }
        joined(FacePart.LEFT_BROW_TOP, FacePart.LEFT_BROW_BOTTOM)
        joined(FacePart.RIGHT_BROW_TOP, FacePart.RIGHT_BROW_BOTTOM)
        // Landmarks locate a nose; they are not themselves visible edges. A straight bridge gives a T-shaped glyph.
        val observedNose = nose(pixels, width, height, face)
        if (observedNose.isNotEmpty()) semantic.addAll(observedNose) else parts[FacePart.NOSE_BOTTOM]?.let { s ->
            semantic.add(if (s.points.size == 3) ContourStroke(listOf(s.points[1], s.points[0], s.points[2]), false) else s)
        }
        joined(FacePart.UPPER_LIP_TOP, FacePart.LOWER_LIP_BOTTOM)
        joined(FacePart.UPPER_LIP_BOTTOM, FacePart.LOWER_LIP_TOP)
        for (part in listOf(FacePart.LEFT_EYE, FacePart.RIGHT_EYE)) {
            parts[part]?.let { eye -> iris(pixels, width, height, eye.points)?.let { semantic.add(it) } }
        }
        // Raw edges inside the facial oval create shadow/noise duplicates. Keep semantic features there.
        val context = details.flatMap { stroke ->
            if (oval.size < 3 || stroke.points.none { StrokeRefinement.contains(oval, it) }) listOf(stroke)
            else split(stroke.points, { !StrokeRefinement.contains(oval, it) })
                .filter { it.size >= 3 }.map { ContourStroke(it, false) }
        }
        val minContext = max(5.0, max(width, height) * 0.012)
        val joined = StrokeRefinement.join(context.filter { it.length >= minContext }, max(1.5, max(width, height) * 0.004))
        val selected = StrokeRefinement.select(joined, width, height, 64)
        return outline.take(3).map { StrokeRefinement.smooth(it) } + semantic + selected.map { StrokeRefinement.smooth(it) }
    }

    private fun split(points: List<Point2>, allowed: (Point2) -> Boolean): List<List<Point2>> {
        val result = ArrayList<List<Point2>>(); var run = ArrayList<Point2>()
        for (p in points) {
            if (allowed(p)) run.add(p) else if (run.isNotEmpty()) { result.add(run); run = ArrayList() }
        }
        if (run.isNotEmpty()) result.add(run)
        return result
    }

    /** Trace contrast inside the landmark-guided nose region, with no forced line down the bridge. */
    fun nose(pixels: IntArray, width: Int, height: Int, face: FacialGeometry): List<ContourStroke> {
        val base = face.parts[FacePart.NOSE_BOTTOM]?.points ?: return emptyList()
        val bridge = face.parts[FacePart.NOSE_BRIDGE]?.points ?: return emptyList()
        val span = base.maxOf { it.x } - base.minOf { it.x }
        val topY = bridge.minOf { it.y }; val bottomY = base.maxOf { it.y }
        val noseHeight = bottomY - topY
        if (span < 3 || noseHeight < 4) return emptyList()
        val x0 = max(0, (base.minOf { it.x } - span * 0.4).toInt())
        val x1 = min(width - 1, (base.maxOf { it.x } + span * 0.4).toInt())
        val y0 = max(0, (topY + noseHeight * 0.28).toInt())
        val y1 = min(height - 1, (bottomY + noseHeight * 0.18).toInt())
        val w = x1 - x0 + 1; val h = y1 - y0 + 1
        if (w < 6 || h < 6) return emptyList()
        val gray = IntArray(w * h) { i ->
            val c = pixels[(y0 + i / w) * width + x0 + i % w]
            (((c shr 16) and 255) * 299 + ((c shr 8) and 255) * 587 + (c and 255) * 114) / 1000
        }
        val sorted = gray.sorted(); val low = sorted[sorted.size / 10]; val high = sorted[sorted.size * 9 / 10]
        if (high - low < 18) return emptyList()
        val contrast = IntArray(gray.size) { i ->
            val v = ((gray[i] - low) * 235 / (high - low) + 10).coerceIn(0,255)
            0xff000000.toInt() or (v shl 16) or (v shl 8) or v
        }
        val raw = RasterContours.lineStrokes(RasterContours.edges(contrast,w,h,75),w,h,2.5)
        return StrokeRefinement.join(raw.take(40),1.5).sortedByDescending { it.length }.take(6).map { stroke ->
            StrokeRefinement.smooth(ContourStroke(stroke.points.map { Point2(it.x+x0,it.y+y0) },stroke.closed))
        }
    }

    /** Trace observed dark iris pixels inside the eyelids; do not invent an eye or pupil. */
    fun iris(pixels: IntArray, width: Int, height: Int, eye: List<Point2>): ContourStroke? {
        if (eye.size < 4) return null
        val left = max(0, eye.minOf { it.x }.toInt()); val top = max(0, eye.minOf { it.y }.toInt())
        val right = min(width - 1, eye.maxOf { it.x }.toInt() + 1); val bottom = min(height - 1, eye.maxOf { it.y }.toInt() + 1)
        val w = right - left + 1; val h = bottom - top + 1
        if (w < 6 || h < 4) return null
        val values = ArrayList<Int>()
        fun luminance(c: Int) = (((c shr 16) and 255) * 299 + ((c shr 8) and 255) * 587 + (c and 255) * 114) / 1000
        for (y in top..bottom) for (x in left..right) if (StrokeRefinement.contains(eye, Point2(x.toDouble(), y.toDouble()))) values.add(luminance(pixels[y * width + x]))
        if (values.size < 8) return null
        values.sort()
        if (values.last() - values.first() < 35) return null
        val threshold = min(100, values[values.size / 4] + 12)
        val mask = BooleanArray(w * h) { i ->
            val x = left + i % w; val y = top + i / w
            x > left + w * 0.15 && x < left + w * 0.85 && y > top && y < bottom &&
                StrokeRefinement.contains(eye, Point2(x.toDouble(), y.toDouble())) && luminance(pixels[y * width + x]) < threshold
        }
        val component = RasterContours.largestComponent(mask, w, h)
        val area = component.count { it }
        if (area < 3 || area > values.size * 0.6) return null
        val indices = component.indices.filter { component[it] }
        val minX = indices.minOf { it % w }.toDouble(); val maxX = indices.maxOf { it % w } + 1.0
        val minY = indices.minOf { it / w }.toDouble(); val maxY = indices.maxOf { it / w } + 1.0
        val cx = (minX + maxX) / 2; val cy = (minY + maxY) / 2
        if (cx / w !in 0.2..0.8) return null
        // Fit a smooth iris to the observed dark component's bounds; no cross-shaped pixel outline.
        return ContourStroke((0 until 32).map { i ->
            val t = i * 2 * PI / 32
            Point2(left + cx + (maxX-minX)/2*cos(t), top + cy + (maxY-minY)/2*sin(t))
        },true)
    }
}
