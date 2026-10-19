package com.fourierart.app.core

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.PI

/** Platform-free raster processing, bounded to <=640px by the caller. */
object RasterContours {
    fun largestComponent(mask: BooleanArray, width: Int, height: Int): BooleanArray {
        require(mask.size == width * height && width > 0 && height > 0)
        val seen = BooleanArray(mask.size)
        val queue = IntArray(mask.size)
        var largest = IntArray(0)
        for (start in mask.indices) {
            if (!mask[start] || seen[start]) continue
            var read = 0; var end = 1
            queue[0] = start; seen[start] = true
            while (read < end) {
                val i = queue[read++]; val x = i % width; val y = i / width
                for (j in intArrayOf(if (x > 0) i - 1 else -1, if (x < width - 1) i + 1 else -1,
                    if (y > 0) i - width else -1, if (y < height - 1) i + width else -1)) {
                    if (j >= 0 && mask[j] && !seen[j]) { seen[j] = true; queue[end++] = j }
                }
            }
            if (end > largest.size) largest = queue.copyOf(end)
        }
        return BooleanArray(mask.size).also { output -> largest.forEach { output[it] = true } }
    }

    /** Oriented grid edges preserve concavities and holes. Right-turn routing resolves diagonal contacts. */
    fun outlines(mask: BooleanArray, width: Int, height: Int): List<ContourStroke> {
        require(mask.size == width * height)
        fun has(x: Int, y: Int) = x in 0 until width && y in 0 until height && mask[y * width + x]
        fun vertex(x: Int, y: Int) = y * (width + 1) + x
        val next = HashMap<Int, MutableList<Int>>()
        fun edge(a: Int, b: Int) { next.getOrPut(a) { ArrayList(2) }.add(b) }
        for (i in mask.indices) if (mask[i]) {
            val x = i % width; val y = i / width
            if (!has(x, y - 1)) edge(vertex(x, y), vertex(x + 1, y))
            if (!has(x + 1, y)) edge(vertex(x + 1, y), vertex(x + 1, y + 1))
            if (!has(x, y + 1)) edge(vertex(x + 1, y + 1), vertex(x, y + 1))
            if (!has(x - 1, y)) edge(vertex(x, y + 1), vertex(x, y))
        }
        fun direction(a: Int, b: Int): Int = when (b - a) { 1 -> 0; width + 1 -> 1; -1 -> 2; else -> 3 }
        val paths = ArrayList<ContourStroke>()
        for (start in next.keys.sorted()) {
            while (!next[start].isNullOrEmpty()) {
                val vertices = ArrayList<Int>(); var current = start; var incoming = -1
                do {
                    vertices.add(current)
                    val options = next[current] ?: break
                    if (options.isEmpty()) break
                    val dest = if (incoming < 0) options.first() else options.minBy { b ->
                        when ((direction(current, b) - incoming + 4) % 4) { 1 -> 0; 0 -> 1; 3 -> 2; else -> 3 }
                    }
                    options.remove(dest)
                    incoming = direction(current, dest); current = dest
                } while (current != start)
                if (current == start && vertices.size >= 8) {
                    val points = vertices.map { Point2((it % (width + 1)).toDouble(), (it / (width + 1)).toDouble()) }
                    val border = points.count { it.x <= 1 || it.y <= 1 || it.x >= width - 1 || it.y >= height - 1 }
                    val sides = listOf(points.any { it.x <= 1 }, points.any { it.y <= 1 },
                        points.any { it.x >= width - 1 }, points.any { it.y >= height - 1 }).count { it }
                    if (sides < 3 || border.toDouble() / points.size <= 0.12) paths.add(ContourStroke(points, true))
                }
            }
        }
        return paths.sortedByDescending { abs(area(it.points)) }
    }

    fun area(points: List<Point2>): Double = points.indices.sumOf { i ->
        val a = points[i]; val b = points[(i + 1) % points.size]; a.x * b.y - b.x * a.y
    } / 2

    /** Smoothed Sobel + nonmaximum suppression + hysteresis; no thresholded filled blobs as details. */
    fun edges(pixels: IntArray, width: Int, height: Int, sensitivity: Int, subject: BooleanArray? = null): BooleanArray {
        require(pixels.size == width * height && (subject == null || subject.size == pixels.size))
        val gray = DoubleArray(pixels.size) { i ->
            val c = pixels[i]
            (0.299 * ((c shr 16) and 255) + 0.587 * ((c shr 8) and 255) + 0.114 * (c and 255)) * ((c ushr 24) / 255.0) +
                255 * (1 - (c ushr 24) / 255.0)
        }
        val blur = gray.copyOf()
        for (y in 1 until height - 1) for (x in 1 until width - 1) {
            var sum = 0.0
            for (dy in -1..1) for (dx in -1..1) sum += gray[(y + dy) * width + x + dx] *
                (if (dx == 0) 2 else 1) * (if (dy == 0) 2 else 1)
            blur[y * width + x] = sum / 16
        }
        val mag = DoubleArray(pixels.size); val angle = IntArray(pixels.size)
        for (y in 1 until height - 1) for (x in 1 until width - 1) {
            val i = y * width + x
            val gx = -blur[i - width - 1] + blur[i - width + 1] - 2 * blur[i - 1] + 2 * blur[i + 1] - blur[i + width - 1] + blur[i + width + 1]
            val gy = -blur[i - width - 1] - 2 * blur[i - width] - blur[i - width + 1] + blur[i + width - 1] + 2 * blur[i + width] + blur[i + width + 1]
            mag[i] = hypot(gx, gy)
            angle[i] = (((atan2(gy, gx) * 180 / PI + 180) % 180 + 22.5).toInt() / 45) % 4
        }
        val thin = DoubleArray(pixels.size)
        for (y in 2 until height - 2) for (x in 2 until width - 2) {
            val i = y * width + x
            // Erode the allowed area to keep the silhouette boundary out of internal details.
            if (subject != null && (-2..2).any { dy -> (-2..2).any { dx -> !subject[i + dy * width + dx] } }) continue
            val step = when (angle[i]) { 0 -> 1; 1 -> width + 1; 2 -> width; else -> width - 1 }
            if (mag[i] >= mag[i - step] && mag[i] > mag[i + step]) thin[i] = mag[i]
        }
        val high = (145 - sensitivity.coerceIn(0, 100)).toDouble()
        val queue = IntArray(pixels.size); var count = 0; var read = 0
        val output = BooleanArray(pixels.size)
        for (i in thin.indices) if (thin[i] >= high) { output[i] = true; queue[count++] = i }
        while (read < count) {
            val i = queue[read++]; val x = i % width; val y = i / width
            for (dy in -1..1) for (dx in -1..1) {
                if (x + dx !in 1 until width - 1 || y + dy !in 1 until height - 1) continue
                val j = i + dy * width + dx
                if (!output[j] && thin[j] >= high * 0.4) { output[j] = true; queue[count++] = j }
            }
        }
        return output
    }

    /** Every graph edge is used once; branches lift the pen instead of joining unrelated features. */
    fun lineStrokes(edges: BooleanArray, width: Int, height: Int, minLength: Double): List<ContourStroke> {
        require(edges.size == width * height)
        fun neighbors(i: Int): List<Int> {
            val x = i % width; val y = i / width
            return buildList {
                for (dy in -1..1) for (dx in -1..1) {
                    if (dx == 0 && dy == 0 || x + dx !in 0 until width || y + dy !in 0 until height) continue
                    val j = i + dy * width + dx
                    if (!edges[j]) continue
                    // Avoid diagonal shortcuts when an orthogonal connection already exists.
                    if (dx != 0 && dy != 0 && (edges[i + dx] || edges[i + dy * width])) continue
                    add(j)
                }
            }
        }
        val adjacency = edges.indices.filter { edges[it] }.associateWith { neighbors(it) }
        val used = HashSet<Long>()
        fun key(a: Int, b: Int) = (minOf(a, b).toLong() shl 32) or maxOf(a, b).toLong()
        val strokes = ArrayList<ContourStroke>()
        fun walk(start: Int, first: Int) {
            val path = arrayListOf(start); var previous = start; var current = first
            used.add(key(start, first))
            while (true) {
                path.add(current)
                if (current == start) break
                val ns = adjacency[current].orEmpty()
                if (ns.size != 2) break
                val dest = ns.firstOrNull { it != previous && key(current, it) !in used } ?: break
                used.add(key(current, dest)); previous = current; current = dest
            }
            val closed = path.first() == path.last()
            val points = (if (closed) path.dropLast(1) else path).map { Point2((it % width).toDouble(), (it / width).toDouble()) }
            val stroke = ContourStroke(points, closed)
            if (stroke.length >= minLength && points.size >= 3) strokes.add(stroke)
        }
        for ((i, ns) in adjacency) if (ns.size != 2) for (j in ns) if (key(i, j) !in used) walk(i, j)
        for ((i, ns) in adjacency) for (j in ns) if (key(i, j) !in used) walk(i, j)
        return strokes.sortedByDescending { it.length }.take(40)
    }
}
