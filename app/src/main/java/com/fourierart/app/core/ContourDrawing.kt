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
    fun trace(components: Int, frames: Int = 320): List<Point2> =
        (0..frames).map { curve.sample(2 * kotlin.math.PI * visibleFraction * it / frames, components) }
}

class ContourDrawing private constructor(
    val sourceStrokes: List<ContourStroke>,
    val strokes: List<DrawingStroke>
) {
    companion object {
        fun fromStrokes(input: List<ContourStroke>): ContourDrawing {
            val usable = input.filter { it.points.size >= (if (it.closed) 3 else 2) && it.length > 1e-6 }
            require(usable.isNotEmpty()) { "No usable strokes" }
            require(usable.all { s -> s.points.all { it.x.isFinite() && it.y.isFinite() } })
            val points = usable.flatMap { it.points }
            val minX = points.minOf { it.x }; val maxX = points.maxOf { it.x }
            val minY = points.minOf { it.y }; val maxY = points.maxOf { it.y }
            val side = max(maxX - minX, maxY - minY)
            require(side > 1e-6)
            val cx = (minX + maxX) / 2; val cy = (minY + maxY) / 2
            return ContourDrawing(usable, usable.map { stroke ->
                val normalized = stroke.points.map { Point2(1.7 * (it.x - cx) / side, 1.7 * (it.y - cy) / side) }
                DrawingStroke(if (stroke.closed) FourierCurve.fromClosedPath(normalized)
                    else FourierCurve.fromOpenPath(normalized), stroke.closed)
            })
        }

        fun sample(curve: FourierCurve): ContourDrawing =
            ContourDrawing(emptyList(), listOf(DrawingStroke(curve, true)))
    }
}
