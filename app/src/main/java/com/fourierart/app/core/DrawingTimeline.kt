package com.fourierart.app.core

/** Pure progress mapping, shared by preview and future offline rendering. No wall clock or UI state. */
class DrawingTimeline(drawing: ContourDrawing) {
    data class Frame(val completed: Int, val active: Int?, val localProgress: Double)
    private val cumulative = DoubleArray(drawing.strokes.size + 1).apply {
        require(drawing.strokes.isNotEmpty())
        for (i in drawing.strokes.indices) this[i + 1] = this[i] + drawing.strokes[i].visibleLength.coerceAtLeast(1e-9)
        require(last().isFinite() && last() > 0.0)
        val total = last()
        for (i in indices) this[i] /= total
        this[lastIndex] = 1.0
    }
    val strokeCount: Int get() = cumulative.size - 1
    fun startOf(index: Int): Double { require(index in 0 until strokeCount); return cumulative[index] }
    fun endOf(index: Int): Double { require(index in 0 until strokeCount); return cumulative[index + 1] }
    fun frame(progress: Double): Frame {
        require(progress.isFinite())
        val value = progress.coerceIn(0.0,1.0)
        if (value == 1.0) return Frame(strokeCount,null,1.0)
        // The exact boundary belongs to the next stroke, with the pen lifted between them.
        var low = 0; var high = strokeCount
        while (low < high) {
            val mid = (low + high) ushr 1
            if (cumulative[mid + 1] <= value) low = mid + 1 else high = mid
        }
        val fraction = ((value - cumulative[low]) / (cumulative[low + 1] - cumulative[low])).coerceIn(0.0,1.0)
        return Frame(low,low,fraction)
    }
}
