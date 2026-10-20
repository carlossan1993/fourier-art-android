package com.fourierart.app.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

enum class DetailLevel(val budget: Int) { CLEAN(100), BALANCED(220), FINE(400) }

/** Rank visible contrast and edges that survive smoothing before spending the detail budget. */
object DetailSelector {
    fun select(input: List<ContourStroke>, pixels: IntArray, width: Int, height: Int,
        structural: BooleanArray, budget: Int): List<ContourStroke> {
        require(pixels.size == width * height && structural.size == pixels.size)
        if (budget <= 0 || input.isEmpty()) return emptyList()
        val nearStructure = BooleanArray(pixels.size)
        for (i in structural.indices) if (structural[i]) {
            val x = i % width; val y = i / width
            for (dy in -2..2) for (dx in -2..2) if (x + dx in 0 until width && y + dy in 0 until height)
                nearStructure[i + dy * width + dx] = true
        }
        fun gray(x: Int, y: Int): Double {
            val c = pixels[y.coerceIn(0, height - 1) * width + x.coerceIn(0, width - 1)]
            val a = (c ushr 24) / 255.0
            return (0.299 * ((c shr 16) and 255) + 0.587 * ((c shr 8) and 255) + 0.114 * (c and 255)) * a + 255 * (1 - a)
        }
        data class Candidate(val stroke: ContourStroke, val score: Double, val cell: Int)
        val ranked = input.filter { it.points.size >= 2 && it.length >= max(4.0, max(width, height) * 0.004) }.map { s ->
            var support = 0; var contrast = 0.0; var count = 0
            val step = max(1, s.points.size / 40)
            for (j in s.points.indices step step) {
                val p = s.points[j]; val x = p.x.toInt().coerceIn(0, width - 1); val y = p.y.toInt().coerceIn(0, height - 1)
                if (nearStructure[y * width + x]) support++
                contrast += max(abs(gray(x - 2, y) - gray(x + 2, y)), abs(gray(x, y - 2) - gray(x, y + 2)))
                count++
            }
            val p = s.points[s.points.size / 2]
            val cell = (p.y * 4 / height).toInt().coerceIn(0, 3) * 4 + (p.x * 4 / width).toInt().coerceIn(0, 3)
            Candidate(s, s.length.pow(0.65) * (0.25 + 0.75 * support / count.toDouble()) *
                (0.5 + (contrast / count / 120).coerceAtMost(1.5)), cell)
        }.sortedByDescending { it.score }.toMutableList()
        val occupancy = IntArray(16)
        val result = ArrayList<ContourStroke>()
        // Soft coverage: a busy texture region cannot consume the whole budget, but empty cells get no quota.
        while (ranked.isNotEmpty() && result.size < budget) {
            val index = ranked.indices.maxBy { ranked[it].score / sqrt(1 + occupancy[ranked[it].cell] * 0.18) }
            val next = ranked.removeAt(index)
            result.add(next.stroke); occupancy[next.cell]++
        }
        return result
    }
}
