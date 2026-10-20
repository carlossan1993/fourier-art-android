package com.fourierart.app.core

import kotlin.math.abs

/** Playback is a pure time mapping; no wall clock, Android, or accumulated background time. */
enum class PlaybackSpeed(val multiplier: Double) { SLOW(.5), NORMAL(1.0), FAST(2.0), VERY_FAST(4.0) }
enum class TraceColor(private val dark: Long, private val light: Long) {
    INK(0xFFFFFFFF,0xFF101B2B), TEAL(0xFF65DCD0,0xFF00665E), GOLD(0xFFFFD166,0xFF805300), ROSE(0xFFFF8FAE,0xFFA3224C);
    fun argb(lightBackground: Boolean): Int=(if(lightBackground) light else dark).toInt()
}
data class AnimationStyle(val speed: PlaybackSpeed=PlaybackSpeed.NORMAL,val epicircles: Boolean=true,
    val color: TraceColor=TraceColor.INK,val lightBackground: Boolean=false)
object PlaybackClock {
    const val DRAW_SECONDS=12.0
    const val CYCLE=1.2 // Hold the completed drawing for 20% of the drawing duration, then repeat.
    fun advance(position: Double,seconds: Double,speed: PlaybackSpeed,playing: Boolean=true): Double {
        require(position.isFinite() && position in 0.0..CYCLE && seconds.isFinite() && seconds>=0.0)
        if(!playing || seconds==0.0) return position
        val period=DRAW_SECONDS*CYCLE/speed.multiplier
        return (position+(seconds%period)*speed.multiplier/DRAW_SECONDS)%CYCLE
    }
    fun progress(position: Double): Double {require(position.isFinite());return position.coerceIn(0.0,1.0)}
}
data class PreviewViewport(val zoom: Double=1.0,val panX: Double=0.0,val panY: Double=0.0) {
    init {require(zoom.isFinite() && zoom in 1.0..4.0 && panX.isFinite() && panY.isFinite())
        require(abs(panX)<=.4*(zoom-1)+1e-9 && abs(panY)<=.4*(zoom-1)+1e-9)}
    fun transform(factor: Double,dx: Double,dy: Double,anchorX: Double=.5,anchorY: Double=.5): PreviewViewport {
        require(listOf(factor,dx,dy,anchorX,anchorY).all {it.isFinite()} && factor>0)
        val next=(zoom*factor).coerceIn(1.0,4.0)
        if(next==1.0) return PreviewViewport()
        val ratio=next/zoom;val limit=.4*(next-1)
        return PreviewViewport(next,((anchorX-.5)*(1-ratio)+panX*ratio+dx).coerceIn(-limit,limit),
            ((anchorY-.5)*(1-ratio)+panY*ratio+dy).coerceIn(-limit,limit))
    }
}
