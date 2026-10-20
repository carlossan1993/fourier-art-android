package com.fourierart.app.core

import kotlin.math.hypot
import kotlin.math.max

/** A pen lift separates strokes. All strokes share ONE coordinate transform. */
data class ContourStroke(val points: List<Point2>, val closed: Boolean) {
    val length: Double get() = points.zipWithNext().sumOf { (a, b) -> hypot(b.x - a.x, b.y - a.y) } +
        if (closed && points.size > 1) hypot(points.first().x - points.last().x, points.first().y - points.last().y) else 0.0
}

data class DrawingStroke(val curve: FourierCurve, val closed: Boolean) {
    // An open stroke is reflected for Fourier periodicity, but only the outward half is drawn.
    val visibleFraction: Double get() = if (closed) 1.0 else 0.5
    fun trace(components: Int, frames: Int = curve.recommendedFrames(components, visibleFraction)): List<Point2> =
        curve.trace(components, frames, visibleFraction)
    fun position(progress: Double, components: Int): Point2 {
        require(progress.isFinite())
        return curve.sample(2 * kotlin.math.PI * progress.coerceIn(0.0, 1.0) * visibleFraction, components)
    }
    val visibleLength: Double get() {
        val points = if (closed) curve.points + curve.points.first() else curve.points.take(curve.points.size / 2 + 1)
        return points.zipWithNext().sumOf { (a,b) -> hypot(b.x-a.x,b.y-a.y) }
    }
}

class ContourDrawing private constructor(
    val sourceStrokes: List<ContourStroke>,
    val strokes: List<DrawingStroke>
) {
    companion object {
        fun fromStrokes(input: List<ContourStroke>): ContourDrawing {
            require(input.all { s -> s.points.all { it.x.isFinite() && it.y.isFinite() } }) { "Non-finite contour" }
            val usable = input.filter { it.points.size >= (if (it.closed) 3 else 2) && it.length > 1e-6 }
            require(usable.isNotEmpty()) { "No usable strokes" }
            require(usable.all { s -> s.points.all { it.x.isFinite() && it.y.isFinite() } })
            val points = usable.flatMap { it.points }
            val minX = points.minOf { it.x }; val maxX = points.maxOf { it.x }
            val minY = points.minOf { it.y }; val maxY = points.maxOf { it.y }
            val side = max(maxX - minX, maxY - minY)
            require(side.isFinite() && side > 1e-6)
            val cx = minX + (maxX - minX) / 2; val cy = minY + (maxY - minY) / 2
            return ContourDrawing(usable, usable.map { stroke ->
                val normalized = stroke.points.map { Point2(1.7 * (it.x - cx) / side, 1.7 * (it.y - cy) / side) }
                // About one sample per 1.5 raster pixels; open strokes require the return journey too.
                val target = stroke.length * (if (stroke.closed) 1 else 2) / 1.5
                var samples = 64
                while (samples < target && samples < 1024) samples *= 2
                DrawingStroke(if (stroke.closed) FourierCurve.fromClosedPath(normalized, samples)
                    else FourierCurve.fromOpenPath(normalized, samples), stroke.closed)
            })
        }

        fun sample(curve: FourierCurve): ContourDrawing =
            ContourDrawing(emptyList(), listOf(DrawingStroke(curve, true)))
    }
}
