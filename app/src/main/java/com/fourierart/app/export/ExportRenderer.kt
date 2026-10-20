package com.fourierart.app.export

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.fourierart.app.core.*
import kotlin.math.PI
import kotlin.math.min

/** Same deterministic Fourier sampling and timeline as the player. No screen capture or photo processing. */
class ExportRenderer(private val snapshot: ExportSnapshot) {
    private data class Stroke(val points: List<Point2>, val path: Path, val rotations: List<Harmonic>)
    private val timeline = DrawingTimeline(snapshot.drawing)
    private val strokes = snapshot.drawing.strokes.map { s ->
        val points = s.trace(snapshot.components)
        Stroke(points, Path().apply { points.forEachIndexed { i, p ->
            if (i == 0) moveTo(p.x.toFloat(), p.y.toFloat()) else lineTo(p.x.toFloat(), p.y.toFloat())
        } }, s.curve.harmonics.take(s.curve.effectiveComponents(snapshot.components)).flatMap { it.rotations() })
    }
    val background = if (snapshot.style.lightBackground) 0xFFFFFBF2.toInt() else 0xFF0D1929.toInt()
    val ink = snapshot.style.color.argb(snapshot.style.lightBackground)
    val guide = if (snapshot.style.lightBackground) 0xFF005F57.toInt() else 0xFF65DCD0.toInt()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val partial = Path()
    fun draw(canvas: Canvas, width: Int, height: Int, progress: Double) {
        canvas.drawColor(background)
        val viewport = snapshot.viewport
        val short = min(width, height).toFloat()
        val scale = short * .43f * viewport.zoom.toFloat()
        canvas.save()
        try {
            canvas.clipRect(0, 0, width, height)
            canvas.translate(width * (.5 + viewport.panX).toFloat(), height * (.5 + viewport.panY).toFloat())
            canvas.scale(scale, scale)
            paint.style = Paint.Style.STROKE; paint.color = ink; paint.strokeWidth = short * (if (strokes.size > 200) .003f else .00375f) / scale
            val frame = timeline.frame(progress)
            for (i in 0 until frame.completed) canvas.drawPath(strokes[i].path, paint)
            val active = frame.active ?: return
            val s = snapshot.drawing.strokes[active]; val part = frame.localProgress
            if (part > 0) {
                val points = strokes[active].points
                val count = (part * points.lastIndex).toInt().coerceIn(0, points.lastIndex)
                partial.rewind(); partial.moveTo(points[0].x.toFloat(), points[0].y.toFloat())
                for (i in 1..count) partial.lineTo(points[i].x.toFloat(), points[i].y.toFloat())
                val tip = s.position(part, snapshot.components); partial.lineTo(tip.x.toFloat(), tip.y.toFloat())
                canvas.drawPath(partial, paint)
            }
            if (snapshot.style.epicircles) {
                var pos = Point2(0.0, 0.0)
                val t = 2 * PI * part * s.visibleFraction
                for (h in strokes[active].rotations) {
                    val p = h.at(t); val dest = Point2(pos.x + p.x, pos.y + p.y)
                    if (h.frequency != 0) {
                        paint.color = guide; paint.alpha = 89; paint.strokeWidth = short * .00175f / scale
                        if (h.radius * scale > 1.2) canvas.drawCircle(pos.x.toFloat(), pos.y.toFloat(), h.radius.toFloat(), paint)
                        paint.alpha = 166; paint.strokeWidth = short * .002f / scale
                        canvas.drawLine(pos.x.toFloat(), pos.y.toFloat(), dest.x.toFloat(), dest.y.toFloat(), paint)
                    }
                    pos = dest
                }
                paint.style = Paint.Style.FILL; paint.color = ink
                canvas.drawCircle(pos.x.toFloat(), pos.y.toFloat(), short * .0075f / scale, paint)
            }
        } finally { canvas.restore() }
    }
}
