package com.fourierart.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class FourierCurveTest {
    @Test
    fun circleHasOneDominantPositiveFrequency() {
        val circle = FourierCurve.circle()
        assertEquals(FourierCurve.SAMPLE_COUNT, circle.points.size)
        val dominant = circle.harmonics.maxByOrNull { it.radius }
        assertEquals(1, dominant?.frequency)
        assertTrue((dominant?.radius ?: 0.0) > 0.7)
    }

    @Test
    fun moreHarmonicsCannotIncreaseDiscreteMeanSquaredError() {
        val star = FourierCurve.star()
        fun mse(n: Int): Double = star.points.indices.sumOf { index ->
            val p = star.points[index]
            val a = star.sample(2.0 * PI * index / star.points.size, n)
            val dx = p.x - a.x
            val dy = p.y - a.y
            dx * dx + dy * dy
        } / star.points.size
        val rough = mse(10)
        val refined = mse(200)
        assertTrue("Expected DFT improvement: $rough -> $refined", refined <= rough + 1e-9)
    }

    @Test
    fun traceContainsNoNonFiniteValues() {
        for (curve in listOf(FourierCurve.circle(), FourierCurve.star())) {
            val path = curve.trace(50)
            assertEquals(321, path.size)
            assertTrue(path.all { it.x.isFinite() && it.y.isFinite() })
            val start = path.first()
            val end = path.last()
            assertEquals(start.x, end.x, 1e-8)
            assertEquals(start.y, end.y, 1e-8)
        }
    }
    @Test fun fftMatchesIndependentDirectTransformAtPositiveAndNegativeFrequencies() {
        val curve = FourierCurve.star()
        for (h in curve.harmonics) {
            var r = 0.0; var im = 0.0
            for ((i, p) in curve.points.withIndex()) {
                val a = -2 * PI * h.frequency * i / curve.points.size
                r += p.x * kotlin.math.cos(a) - p.y * kotlin.math.sin(a)
                im += p.x * kotlin.math.sin(a) + p.y * kotlin.math.cos(a)
            }
            assertEquals(r / curve.points.size, h.real, 1e-10)
            assertEquals(im / curve.points.size, h.imaginary, 1e-10)
        }
    }

    @Test fun recurrentTraceMatchesDirectSamplingForOpenAndClosedPaths() {
        for (fraction in listOf(0.5, 1.0)) {
            val curve = FourierCurve.star()
            val trace = curve.trace(200, 640, fraction)
            for (i in trace.indices step 17) {
                val expected = curve.sample(2 * PI * fraction * i / 640, 200)
                assertEquals(expected.x, trace[i].x, 1e-10)
                assertEquals(expected.y, trace[i].y, 1e-10)
            }
        }
    }

    @Test fun denseContourReconstructionRetainsSmallFeaturesWithMoreSamples() {
        val path = (0 until 1200).map { i ->
            val a = 2 * PI * i / 1200
            val r = 1 + 0.04 * kotlin.math.cos(170 * a)
            Point2(r * kotlin.math.cos(a), r * kotlin.math.sin(a))
        }
        val curve = FourierCurve.fromClosedPath(path, 1024)
        val error = curve.points.indices.sumOf { i ->
            val p = curve.points[i]; val q = curve.sample(2 * PI * i / 1024, 800)
            kotlin.math.hypot(p.x - q.x, p.y - q.y)
        } / 1024
        assertTrue("Dense detail error: $error", error < 0.02)
    }

}
