package com.fourierart.app.core

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.max

/**
 * Experimental high-contrast silhouette detector ONLY.
 * This is deliberately not advertised as person/pet segmentation.
 * Local computation, no network, no libraries or image uploads.
 */
object ImageSilhouette {
    fun fromBitmap(input: Bitmap, sensitivity: Int = 50): FourierCurve? {
        val maxSide = max(input.width, input.height)
        if (maxSide <= 0) return null
        val factor = minOf(1.0, 192.0 / maxSide)
        val width = max(1, (input.width * factor).toInt())
        val height = max(1, (input.height * factor).toInt())
        val bitmap = Bitmap.createScaledBitmap(input, width, height, true)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Approximate the background from a band of border pixels.
        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        var borderCount = 0
        for (y in 0 until height) for (x in 0 until width) {
            if (x < 3 || y < 3 || x >= width - 3 || y >= height - 3) {
                val color = pixels[y * width + x]
                sumR += Color.red(color)
                sumG += Color.green(color)
                sumB += Color.blue(color)
                borderCount++
            }
        }
        val r = sumR / borderCount
        val g = sumG / borderCount
        val b = sumB / borderCount
        val mask = BooleanArray(pixels.size) { index ->
            val c = pixels[index]
            val difference = maxOf(
                abs(Color.red(c) - r),
                abs(Color.green(c) - g),
                abs(Color.blue(c) - b)
            )
            difference >= sensitivity
        }

        // Keep largest FOUR-connected foreground component (discard speckle).
        val seen = BooleanArray(mask.size)
        val queue = IntArray(mask.size)
        var largest: IntArray = intArrayOf()
        for (start in mask.indices) {
            if (!mask[start] || seen[start]) continue
            var front = 0
            var end = 1
            queue[0] = start
            seen[start] = true
            while (front < end) {
                val index = queue[front++]
                val x = index % width
                val y = index / width
                val neighbors = intArrayOf(
                    if (x > 0) index - 1 else -1,
                    if (x + 1 < width) index + 1 else -1,
                    if (y > 0) index - width else -1,
                    if (y + 1 < height) index + width else -1
                )
                for (next in neighbors) {
                    if (next >= 0 && mask[next] && !seen[next]) {
                        seen[next] = true
                        queue[end++] = next
                    }
                }
            }
            if (end > largest.size) largest = queue.copyOf(end)
        }
        if (largest.size < 35 || largest.size > pixels.size * 0.87) return null
        val subject = BooleanArray(mask.size)
        for (index in largest) subject[index] = true

        // Boundary of each foreground pixel as clockwise oriented edges.
        // Walking edge links avoids unordered radial sorting (which breaks concavities).
        fun vertex(x: Int, y: Int) = y * (width + 1) + x
        fun has(x: Int, y: Int) =
            x in 0 until width && y in 0 until height && subject[y * width + x]

        val next = HashMap<Int, Int>()
        for (index in largest) {
            val x = index % width
            val y = index / width
            if (!has(x, y - 1)) next[vertex(x, y)] = vertex(x + 1, y)
            if (!has(x + 1, y)) next[vertex(x + 1, y)] = vertex(x + 1, y + 1)
            if (!has(x, y + 1)) next[vertex(x + 1, y + 1)] = vertex(x, y + 1)
            if (!has(x - 1, y)) next[vertex(x, y + 1)] = vertex(x, y)
        }
        val visited = HashSet<Int>()
        var best = emptyList<Int>()
        var bestArea = 0.0
        for (start in next.keys) {
            if (start in visited) continue
            val contour = ArrayList<Int>()
            var vertex = start
            do {
                if (!visited.add(vertex)) break
                contour.add(vertex)
                vertex = next[vertex] ?: break
            } while (vertex != start && contour.size <= next.size)
            if (vertex != start || contour.size < 12) continue
            var signedArea = 0.0
            for (i in contour.indices) {
                val a = contour[i]
                val bVertex = contour[(i + 1) % contour.size]
                signedArea += (a % (width + 1)).toDouble() * (bVertex / (width + 1)) -
                    (bVertex % (width + 1)).toDouble() * (a / (width + 1))
            }
            val area = abs(signedArea)
            if (area > bestArea) {
                bestArea = area
                best = contour
            }
        }
        if (best.size < 12) return null

        // A long rectangular perimeter from the photo border is not a subject.
        // Reject an almost-full-frame contour instead of claiming recognition.
        val nearBorder = best.count { vertex ->
            val x = vertex % (width + 1)
            val y = vertex / (width + 1)
            x <= 1 || y <= 1 || x >= width - 1 || y >= height - 1
        }
        val touchesTop = best.any { it / (width + 1) <= 1 }
        val touchesBottom = best.any { it / (width + 1) >= height - 1 }
        val touchesLeft = best.any { it % (width + 1) <= 1 }
        val touchesRight = best.any { it % (width + 1) >= width - 1 }
        val touchedSides = listOf(touchesTop, touchesBottom, touchesLeft, touchesRight).count { it }
        if (touchedSides >= 3 && nearBorder.toDouble() / best.size > 0.12) return null

        val xs = best.map { it % (width + 1).toDouble() }
        val ys = best.map { (it / (width + 1)).toDouble() }
        val centerX = (xs.min() + xs.max()) / 2
        val centerY = (ys.min() + ys.max()) / 2
        val longest = maxOf(xs.max() - xs.min(), ys.max() - ys.min())
        if (longest < 4) return null
        val path = xs.indices.map { Point2(1.7 * (xs[it] - centerX) / longest, 1.7 * (ys[it] - centerY) / longest) }
        return FourierCurve.fromClosedPath(path)
    }
}
