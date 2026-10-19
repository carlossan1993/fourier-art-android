package com.fourierart.app.core

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.hypot

class ContourDrawingTest {
    @Test fun openStrokeDoesNotDrawClosingChord() {
        val curve = FourierCurve.fromOpenPath(listOf(Point2(0.0, 0.0), Point2(1.0, 0.0), Point2(1.0, 1.0)))
        val trace = DrawingStroke(curve, false).trace(256, 256)
        assertEquals(0.0, trace.first().x, 1e-8)
        assertEquals(0.0, trace.first().y, 1e-8)
        assertEquals(1.0, trace.last().x, 1e-8)
        assertEquals(1.0, trace.last().y, 1e-8)
        assertTrue(trace.all { p -> p.y < 0.015 || p.x > 0.985 })
    }

    @Test fun twoPointOpenStrokeIsSupported() {
        val curve = FourierCurve.fromOpenPath(listOf(Point2(2.0, 4.0), Point2(2.0, 14.0)))
        assertEquals(2.0, curve.sample(PI, 256).x, 1e-8)
        assertEquals(14.0, curve.sample(PI, 256).y, 1e-8)
    }

    @Test fun independentFeaturesKeepRelativePositionAndAspectRatio() {
        val drawing = ContourDrawing.fromStrokes(listOf(
            ContourStroke(listOf(Point2(10.0, 20.0), Point2(10.0, 40.0)), false),
            ContourStroke(listOf(Point2(20.0, 20.0), Point2(20.0, 40.0)), false)))
        assertEquals(2, drawing.strokes.size)
        val left = drawing.strokes[0].curve.points
        val right = drawing.strokes[1].curve.points
        assertEquals(0.85, right[0].x - left[0].x, 1e-8)
        assertEquals(1.7, left.maxOf { it.y } - left.minOf { it.y }, 1e-8)
        assertTrue(left.all { it.x < 0 } && right.all { it.x > 0 })
    }

    @Test fun fullFourierReconstructsEveryResampledPoint() {
        val curve = FourierCurve.fromOpenPath(listOf(Point2(0.0, 0.0), Point2(1.0, 2.0), Point2(-2.0, 4.0)))
        for ((i, p) in curve.points.withIndex()) {
            val q = curve.sample(2 * PI * i / curve.points.size, 256)
            assertTrue(hypot(p.x - q.x, p.y - q.y) < 1e-8)
        }
    }
}
