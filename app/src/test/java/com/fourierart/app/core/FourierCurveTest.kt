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
}
