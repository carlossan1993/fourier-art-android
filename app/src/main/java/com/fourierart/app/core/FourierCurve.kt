package com.fourierart.app.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class Point2(val x: Double, val y: Double)
data class Harmonic(val frequency: Int, val real: Double, val imaginary: Double) {
    val radius: Double get() = hypot(real, imaginary)

    fun at(t: Double): Point2 {
        val angle = frequency * t
        val c = cos(angle)
        val s = sin(angle)
        return Point2(real * c - imaginary * s, real * s + imaginary * c)
    }
}

/** Pure Kotlin: ordered, CLOSED path -> DFT coefficients -> rotating epicircle chain. */
class FourierCurve private constructor(val points: List<Point2>) {
    val harmonics: List<Harmonic> = buildList {
        val count = points.size
        val frequencies = buildList {
            add(0)
            for (k in 1 until count / 2) {
                add(k)
                add(-k)
            }
            add(count / 2)
        }
        // Radix-2 FFT: more contour samples need not mean quadratic DFT work per stroke.
        val real = DoubleArray(count) { points[it].x }
        val imaginary = DoubleArray(count) { points[it].y }
        var j = 0
        for (i in 1 until count) {
            var bit = count shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) {
                val r = real[i]; real[i] = real[j]; real[j] = r
                val v = imaginary[i]; imaginary[i] = imaginary[j]; imaginary[j] = v
            }
        }
        var block = 2
        while (block <= count) {
            val angle = -2 * PI / block
            val stepR = cos(angle); val stepI = sin(angle)
            for (start in 0 until count step block) {
                var wr = 1.0; var wi = 0.0
                for (offset in 0 until block / 2) {
                    val a = start + offset; val b = a + block / 2
                    val tr = wr * real[b] - wi * imaginary[b]
                    val ti = wr * imaginary[b] + wi * real[b]
                    real[b] = real[a] - tr; imaginary[b] = imaginary[a] - ti
                    real[a] += tr; imaginary[a] += ti
                    val nextR = wr * stepR - wi * stepI
                    wi = wr * stepI + wi * stepR; wr = nextR
                }
            }
            block *= 2
        }
        for (k in frequencies) {
            val index = (k + count) % count
            add(Harmonic(k, real[index] / count, imaginary[index] / count))
        }
    }

    fun sample(time: Double, components: Int): Point2 {
        var x = 0.0
        var y = 0.0
        for (h in harmonics.take(components.coerceIn(1, harmonics.size))) {
            val p = h.at(time)
            x += p.x
            y += p.y
        }
        return Point2(x, y)
    }

    fun trace(components: Int, frames: Int = 320, fraction: Double = 1.0): List<Point2> {
        require(frames > 0)
        val x = DoubleArray(frames + 1); val y = DoubleArray(frames + 1)
        for (h in harmonics.take(components.coerceIn(1, harmonics.size))) {
            val angle = h.frequency * 2 * PI * fraction / frames
            val c = cos(angle); val s = sin(angle)
            var r = h.real; var im = h.imaginary
            for (i in 0..frames) {
                x[i] += r; y[i] += im
                val next = r * c - im * s
                im = r * s + im * c; r = next
            }
        }
        return (0..frames).map { Point2(x[it], y[it]) }
    }

    companion object {
        const val SAMPLE_COUNT = 256

        fun fromClosedPath(path: List<Point2>, samples: Int = SAMPLE_COUNT): FourierCurve {
            require(path.size >= 3) { "A closed curve needs at least 3 points" }
            require(samples in 64..1024 && samples.countOneBits() == 1)
            return FourierCurve(resample(path, samples))
        }

        fun fromOpenPath(path: List<Point2>, samples: Int = SAMPLE_COUNT): FourierCurve {
            require(path.size >= 2) { "An open stroke needs at least 2 points" }
            // Periodic extension returns along the SAME stroke, never closes with a chord.
            require(samples in 64..1024 && samples.countOneBits() == 1)
            return FourierCurve(resample(path + path.drop(1).dropLast(1).reversed(), samples))
        }

        fun circle(): FourierCurve = fromClosedPath(
            (0 until SAMPLE_COUNT).map {
                val t = 2.0 * PI * it / SAMPLE_COUNT
                Point2(0.76 * cos(t), 0.76 * sin(t))
            }
        )

        fun star(): FourierCurve = fromClosedPath(
            (0 until 10).map {
                val t = -PI / 2 + it * PI / 5
                val r = if (it % 2 == 0) 0.88 else 0.38
                Point2(r * cos(t), r * sin(t))
            }
        )

        private fun resample(path: List<Point2>, samples: Int): List<Point2> {
            val segmentLengths = path.indices.map { i ->
                val a = path[i]
                val b = path[(i + 1) % path.size]
                hypot(b.x - a.x, b.y - a.y)
            }
            val perimeter = segmentLengths.sum()
            require(perimeter > 1e-9) { "Degenerate contour" }
            val result = ArrayList<Point2>(samples)
            var segment = 0
            var currentDistance = 0.0
            for (j in 0 until samples) {
                val target = perimeter * j / samples
                while (segment < path.lastIndex && currentDistance + segmentLengths[segment] < target) {
                    currentDistance += segmentLengths[segment]
                    segment++
                }
                val length = segmentLengths[segment]
                val f = if (length > 0) ((target - currentDistance) / length).coerceIn(0.0, 1.0) else 0.0
                val a = path[segment]
                val b = path[(segment + 1) % path.size]
                result.add(Point2(a.x + (b.x - a.x) * f, a.y + (b.y - a.y) * f))
            }
            return result
        }
    }
}
