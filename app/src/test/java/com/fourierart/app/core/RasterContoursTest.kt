package com.fourierart.app.core

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

class RasterContoursTest {
    @Test fun maskPreservesConcavityAndHole() {
        val w = 40; val h = 50
        val mask = BooleanArray(w * h) { i ->
            val x = i % w; val y = i / w
            x in 5..30 && y in 5..40 && !(x in 15..30 && y in 20..40) && !(x in 8..11 && y in 8..12)
        }
        val paths = RasterContours.outlines(mask, w, h)
        assertEquals(2, paths.size)
        assertEquals(mask.count { it }.toDouble(), abs(paths.sumOf { RasterContours.area(it.points) }), 1e-8)
        assertTrue(paths.all { it.closed })
        assertTrue(paths.all { s -> s.points.indices.all { i ->
            val a = s.points[i]; val b = s.points[(i + 1) % s.points.size]
            abs(a.x - b.x) + abs(a.y - b.y) == 1.0
        } })
    }

    @Test fun borderFrameIsRejected() {
        val w = 40; val h = 40
        val paths = RasterContours.outlines(BooleanArray(w * h) { true }, w, h)
        assertTrue(paths.isEmpty())
    }

    @Test fun diagonalContactsDoNotOverwriteBoundaryLinks() {
        val w = 30; val h = 30
        val mask = BooleanArray(w * h) { i ->
            val x = i % w; val y = i / w
            (x in 5..9 && y in 5..9) || (x in 10..14 && y in 10..14)
        }
        val paths = RasterContours.outlines(mask, w, h)
        assertEquals(50.0, paths.sumOf { abs(RasterContours.area(it.points)) }, 1e-8)
    }

    @Test fun largestComponentDropsNoise() {
        val w = 20; val h = 20
        val mask = BooleanArray(w * h) { i -> i == 0 || (i % w in 5..10 && i / w in 4..14) }
        val largest = RasterContours.largestComponent(mask, w, h)
        assertFalse(largest[0])
        assertEquals(66, largest.count { it })
    }

    @Test fun disconnectedFeaturesAndBranchesLiftPen() {
        val w = 60; val h = 60
        val pixels = BooleanArray(w * h) { i ->
            val x = i % w; val y = i / w
            (y == 10 && x in 5..25) || (x == 15 && y in 10..25) || (x == 45 && y in 35..50)
        }
        val paths = RasterContours.lineStrokes(pixels, w, h, 5.0)
        assertEquals(4, paths.size)
        assertTrue(paths.none { it.closed })
        assertTrue(paths.all { s -> s.points.zipWithNext().all { (a, b) -> hypot(a.x - b.x, a.y - b.y) <= 1.42 } })
    }

    @Test fun edgeDetectionKeepsDetailInsideMaskAndRejectsFlatImage() {
        val w = 60; val h = 60
        val flat = IntArray(w * h) { -1 }
        assertFalse(RasterContours.edges(flat, w, h, 50).any { it })
        val pixels = IntArray(w * h) { i -> if (i % w in 20..40 && i / w in 20..40) 0xff000000.toInt() else -1 }
        val mask = BooleanArray(w * h) { i -> i % w in 10..50 && i / w in 10..50 }
        val edges = RasterContours.edges(pixels, w, h, 50, mask)
        assertTrue(edges.any { it })
        assertTrue(edges.indices.filter { edges[it] }.all { it % w in 12..48 && it / w in 12..48 })
        assertTrue(RasterContours.lineStrokes(edges, w, h, 8.0).isNotEmpty())
    }
}
