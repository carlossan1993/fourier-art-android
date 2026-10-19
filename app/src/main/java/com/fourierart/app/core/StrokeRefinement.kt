package com.fourierart.app.core

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

/** Geometry-only cleanup. A join must have nearby endpoints AND a consistent tangent. */
object StrokeRefinement {
    fun distance(a: Point2, b: Point2) = hypot(a.x - b.x, a.y - b.y)

    fun contains(polygon: List<Point2>, p: Point2): Boolean {
        var inside = false
        var j = polygon.lastIndex
        for (i in polygon.indices) {
            val a = polygon[i]; val b = polygon[j]
            if ((a.y > p.y) != (b.y > p.y) && p.x < (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x) inside = !inside
            j = i
        }
        return inside
    }

    fun join(input: List<ContourStroke>, maxGap: Double): List<ContourStroke> {
        val paths = input.toMutableList()
        fun compatible(a: List<Point2>, b: List<Point2>): Boolean {
            if (distance(a.last(), b.first()) > maxGap) return false
            val a0 = a[max(0, a.size - 4)]; val a1 = a.last()
            val b0 = b.first(); val b1 = b[minOf(3, b.lastIndex)]
            val ux = a1.x - a0.x; val uy = a1.y - a0.y
            val vx = b1.x - b0.x; val vy = b1.y - b0.y
            val len = hypot(ux, uy) * hypot(vx, vy)
            if (len < 1e-6 || (ux * vx + uy * vy) / len < 0.88) return false
            val gap = distance(a1, b0)
            // Prevent a sideways jump between parallel edges (double eyelids / clothing seams).
            return gap < 0.1 || ((b0.x - a1.x) * ux + (b0.y - a1.y) * uy) / (gap * hypot(ux, uy)) > 0.72
        }
        // Two passes are sufficient to reconnect short raster breaks without quadratic unbounded merging.
        repeat(2) {
            for (i in paths.indices) {
                if (paths[i].closed || paths[i].points.isEmpty()) continue
                for (j in i + 1 until paths.size) {
                    if (paths[j].closed || paths[j].points.isEmpty()) continue
                    val a = paths[i].points; val b = paths[j].points
                    if (minOf(distance(a.first(), b.first()), distance(a.first(), b.last()),
                        distance(a.last(), b.first()), distance(a.last(), b.last())) > maxGap) continue
                    val pair = listOf(a to b, a to b.reversed(), a.reversed() to b, a.reversed() to b.reversed())
                        .filter { (x, y) -> compatible(x, y) }.minByOrNull { (x, y) -> distance(x.last(), y.first()) } ?: continue
                    paths[i] = ContourStroke(pair.first + pair.second, false)
                    paths[j] = ContourStroke(emptyList(), false)
                }
            }
        }
        return paths.filter { it.points.isNotEmpty() }
    }

    /** Spatial coverage: reserve room in each cell instead of spending the whole budget on long seams. */
    fun select(input: List<ContourStroke>, width: Int, height: Int, budget: Int = 100): List<ContourStroke> {
        if (budget <= 0 || input.isEmpty()) return emptyList()
        val ordered = input.sortedByDescending { it.length }
        val cells = Array(16) { ArrayList<Int>() }
        for ((i, stroke) in ordered.withIndex()) {
            val p = stroke.points[stroke.points.size / 2]
            val x = (p.x * 4 / width).toInt().coerceIn(0, 3)
            val y = (p.y * 4 / height).toInt().coerceIn(0, 3)
            cells[y * 4 + x].add(i)
        }
        val picked = linkedSetOf<Int>()
        for (round in 0 until budget) {
            var added = false
            for (cell in cells) {
                if (picked.size == budget) break
                if (round < cell.size) { picked.add(cell[round]); added = true }
            }
            if (!added || picked.size == budget) break
        }
        return picked.map { ordered[it] }
    }

    /** A cropped body may touch three borders. Keep its silhouette but never draw the photo frame. */
    fun clipFrame(stroke: ContourStroke, width: Int, height: Int): List<ContourStroke> {
        fun border(p: Point2) = p.x <= 1 || p.y <= 1 || p.x >= width - 1 || p.y >= height - 1
        if (stroke.points.none { border(it) }) return listOf(stroke)
        val first = stroke.points.indexOfFirst { border(it) }
        val ordered = if (stroke.closed) stroke.points.drop(first) + stroke.points.take(first) else stroke.points
        val result = ArrayList<ContourStroke>(); var run = ArrayList<Point2>()
        for (p in ordered) {
            if (!border(p)) run.add(p) else { if (run.size >= 3) result.add(ContourStroke(run, false)); run = ArrayList() }
        }
        if (run.size >= 3) result.add(ContourStroke(run, false))
        return result
    }

    fun smooth(stroke: ContourStroke): ContourStroke {
        val p = stroke.points
        if (p.size < 4) return stroke
        val result = ArrayList<Point2>()
        if (!stroke.closed) result.add(p.first())
        val count = if (stroke.closed) p.size else p.size - 1
        for (i in 0 until count) {
            val a = p[i]; val b = p[(i + 1) % p.size]
            result.add(Point2(0.75 * a.x + 0.25 * b.x, 0.75 * a.y + 0.25 * b.y))
            result.add(Point2(0.25 * a.x + 0.75 * b.x, 0.25 * a.y + 0.75 * b.y))
        }
        if (!stroke.closed) result.add(p.last())
        return ContourStroke(result, stroke.closed)
    }
}
