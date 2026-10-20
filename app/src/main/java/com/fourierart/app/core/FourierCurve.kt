package com.fourierart.app.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sqrt

data class Point2(val x: Double, val y: Double)
data class Harmonic(val frequency: Int, val real: Double, val imaginary: Double, val cosineOnly: Boolean = false) {
    val radius: Double get() = hypot(real, imaginary)

    fun at(t: Double): Point2 {
        val angle = frequency * t
        val c = cos(angle)
        val s = sin(angle)
        return if (cosineOnly) Point2(real * c, imaginary * c)
        else Point2(real * c - imaginary * s, real * s + imaginary * c)
    }
    // A reflected path uses symmetric Nyquist interpolation between sample points.
    // It is rendered as two counter-rotating half-vectors, so the tip stays exact.
    fun rotations(): List<Harmonic> = if (cosineOnly) listOf(
        Harmonic(frequency, real / 2, imaginary / 2), Harmonic(-frequency, real / 2, imaginary / 2)) else listOf(this)
}

/** Pure Kotlin: arc-length samples -> normalized radix-2 FFT -> deterministic epicircle chain. */
class FourierCurve private constructor(val points: List<Point2>, private val reflected: Boolean = false) {
    private var groupEnds = IntArray(0)
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
    }.let { coefficients ->
        // Keep reflected open strokes even: select +/-k together, never half a return pair.
        val groups = if (reflected) {
            val byFrequency = coefficients.associateBy { it.frequency }
            (1 until points.size / 2).map { k -> listOf(byFrequency.getValue(k), byFrequency.getValue(-k)) } +
                listOf(listOf(byFrequency.getValue(points.size / 2).copy(cosineOnly = true)))
        } else coefficients.drop(1).map { listOf(it) }
        val ordered = groups.sortedWith(compareByDescending<List<Harmonic>> { group ->
            group.sumOf { it.real * it.real + it.imaginary * it.imaginary } / group.size
        }.thenBy { group -> group.minOf { abs(it.frequency) } }.thenByDescending { it.first().frequency })
        var count = 1
        groupEnds = (listOf(1) + ordered.map { group -> count += group.size; count }).toIntArray()
        listOf(coefficients.first()) + ordered.flatten()
    }
    private val residualPower = DoubleArray(harmonics.size + 1).apply {
        for (i in harmonics.indices.reversed()) {
            val h = harmonics[i]
            this[i] = this[i + 1] + h.real * h.real + h.imaginary * h.imaginary
        }
    }

    fun effectiveComponents(requested: Int): Int {
        val capped = requested.coerceIn(1, harmonics.size)
        val index = groupEnds.binarySearch(capped)
        return groupEnds[if (index >= 0) index else -index - 2]
    }

    /** Parseval RMS against the uniformly resampled input, in the same coordinate units. */
    fun reconstructionRms(components: Int): Double = sqrt(residualPower[effectiveComponents(components)])

    /** At least four render intervals per fastest retained cycle; independent of display FPS. */
    fun recommendedFrames(components: Int, fraction: Double = 1.0): Int {
        require(fraction.isFinite() && fraction > 0.0 && fraction <= 1.0)
        val count = effectiveComponents(components)
        var frequency = 0
        for (i in 0 until count) frequency = maxOf(frequency, abs(harmonics[i].frequency))
        return maxOf(128, ceil(4 * frequency * fraction).toInt())
    }

    fun sample(time: Double, components: Int): Point2 {
        require(time.isFinite())
        val phase = time % (2 * PI)
        var x = 0.0
        var y = 0.0
        for (i in 0 until effectiveComponents(components)) {
            val p = harmonics[i].at(phase)
            x += p.x
            y += p.y
        }
        return Point2(x, y)
    }

    fun trace(components: Int, frames: Int = 320, fraction: Double = 1.0): List<Point2> {
        require(frames in 1..16384)
        require(fraction.isFinite() && fraction > 0.0 && fraction <= 1.0)
        val x = DoubleArray(frames + 1); val y = DoubleArray(frames + 1)
        for (index in 0 until effectiveComponents(components)) {
            val h = harmonics[index]
            val angle = h.frequency * 2 * PI * fraction / frames
            val c = cos(angle); val s = sin(angle)
            if (h.cosineOnly) {
                for (i in 0..frames) { val value = cos(angle * i); x[i] += h.real * value; y[i] += h.imaginary * value }
                continue
            }
            var r = h.real; var im = h.imaginary
            for (i in 0..frames) {
                x[i] += r; y[i] += im
                val next = r * c - im * s
                im = r * s + im * c; r = next
            }
        }
        // Anchor both endpoints to direct evaluation, avoiding accumulated recurrence drift.
        val first = sample(0.0, components); val last = sample(2 * PI * fraction, components)
        x[0] = first.x; y[0] = first.y; x[frames] = last.x; y[frames] = last.y
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
            return FourierCurve(resample(path + path.drop(1).dropLast(1).reversed(), samples), reflected = true)
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
            require(path.all { it.x.isFinite() && it.y.isFinite() }) { "Non-finite contour" }
            val segmentLengths = path.indices.map { i ->
                val a = path[i]
                val b = path[(i + 1) % path.size]
                hypot(b.x - a.x, b.y - a.y)
            }
            val perimeter = segmentLengths.sum()
            require(perimeter.isFinite() && perimeter > 1e-9) { "Degenerate contour" }
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
