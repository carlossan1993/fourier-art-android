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
        if (maxGap <= 0 || input.size < 2) return input
        data class End(val stroke: Int, val first: Boolean, val point: Point2, val dx: Double, val dy: Double)
        val ends = ArrayList<End>()
        input.forEachIndexed { i, s ->
            if (!s.closed && s.points.size >= 3) {
                for (first in listOf(true, false)) {
                    val p = if (first) s.points.first() else s.points.last()
                    val q = if (first) s.points[minOf(3, s.points.lastIndex)] else s.points[max(0, s.points.size - 4)]
                    val d = distance(p, q)
                    if (d > 1e-6) ends.add(End(i, first, p, (p.x - q.x) / d, (p.y - q.y) / d))
                }
            }
        }
        fun cell(x: Int, y: Int) = (x.toLong() shl 32) xor (y.toLong() and 0xffffffffL)
        val grid = HashMap<Long, MutableList<Int>>()
        data class Link(val a: Int, val b: Int, val cost: Double)
        val links = ArrayList<Link>()
        ends.forEachIndexed { i, a ->
            val x = kotlin.math.floor(a.point.x / maxGap).toInt()
            val y = kotlin.math.floor(a.point.y / maxGap).toInt()
            for (dy in -1..1) for (dx in -1..1) for (j in grid[cell(x + dx, y + dy)].orEmpty()) {
                val b = ends[j]
                if (a.stroke == b.stroke) continue
                val gap = distance(a.point, b.point)
                if (gap > maxGap) continue
                val alignment = -(a.dx * b.dx + a.dy * b.dy)
                if (alignment < 0.88) continue
                if (gap > 0.1) {
                    val gx = (b.point.x - a.point.x) / gap; val gy = (b.point.y - a.point.y) / gap
                    if (gx * a.dx + gy * a.dy < 0.72 || -(gx * b.dx + gy * b.dy) < 0.72) continue
                }
                links.add(Link(i, j, gap + (1 - alignment) * maxGap))
            }
            grid.getOrPut(cell(x, y)) { ArrayList() }.add(i)
        }
        val parent = IntArray(input.size) { it }
        fun root(i: Int): Int { var r = i; while (parent[r] != r) r = parent[r]; return r }
        val partner = IntArray(ends.size) { -1 }
        for (link in links.sortedBy { it.cost }) {
            val a = ends[link.a]; val b = ends[link.b]
            if (partner[link.a] >= 0 || partner[link.b] >= 0 || root(a.stroke) == root(b.stroke)) continue
            partner[link.a] = link.b; partner[link.b] = link.a
            parent[root(a.stroke)] = root(b.stroke)
        }
        val endIndex = HashMap<Pair<Int, Boolean>, Int>()
        ends.forEachIndexed { i, e -> endIndex[e.stroke to e.first] = i }
        val used = BooleanArray(input.size)
        val result = ArrayList<ContourStroke>()
        // Walk whole chains from a free endpoint; independent of input order or number of fragments.
        for ((index, end) in ends.withIndex()) {
            if (partner[index] >= 0 || used[end.stroke]) continue
            val points = ArrayList<Point2>()
            var current = end
            while (!used[current.stroke]) {
                used[current.stroke] = true
                val p = input[current.stroke].points
                points.addAll(if (current.first) p else p.reversed())
                val exit = endIndex[current.stroke to !current.first] ?: break
                val next = partner[exit]
                if (next < 0) break
                current = ends[next]
            }
            result.add(ContourStroke(points, false))
        }
        input.forEachIndexed { i, s -> if (!used[i]) result.add(s) }
        return result
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

    /** Remove pixel stair steps within a bounded geometric error, including closed contours. */
    fun simplify(stroke: ContourStroke, tolerance: Double): ContourStroke {
        fun reduce(points: List<Point2>): List<Point2> {
            if (points.size < 3) return points
            val keep = BooleanArray(points.size); keep[0] = true; keep[points.lastIndex] = true
            val stack = java.util.ArrayDeque<Pair<Int, Int>>()
            stack.add(0 to points.lastIndex)
            while (stack.isNotEmpty()) {
                val (start, end) = stack.removeLast()
                val a = points[start]; val b = points[end]
                val dx = b.x-a.x; val dy = b.y-a.y; val length = dx*dx+dy*dy
                var farthest = -1; var error = tolerance*tolerance
                for (i in start+1 until end) {
                    val p = points[i]
                    val t = if (length > 0) (((p.x-a.x)*dx+(p.y-a.y)*dy)/length).coerceIn(0.0,1.0) else 0.0
                    val ex = p.x-a.x-t*dx; val ey = p.y-a.y-t*dy; val d = ex*ex+ey*ey
                    if (d > error) { error = d; farthest = i }
                }
                if (farthest >= 0) { keep[farthest] = true; stack.add(start to farthest); stack.add(farthest to end) }
            }
            return points.filterIndexed { i, _ -> keep[i] }
        }
        if (stroke.points.size < 5 || tolerance <= 0) return stroke
        val p = stroke.points
        val result = if (stroke.closed) {
            val middle = p.indices.maxBy { distance(p.first(), p[it]) }
            reduce(p.take(middle+1)).dropLast(1) + reduce(p.drop(middle)+p.first()).dropLast(1)
        } else reduce(p)
        return if (result.size >= (if (stroke.closed) 3 else 2)) ContourStroke(result, stroke.closed) else stroke
    }
}
