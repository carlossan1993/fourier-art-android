package com.fourierart.app.core

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.pow

class AnimationPlaybackTest {
    @Test fun speedMultipliersMapToTheSameDrawingDuration() {
        for(speed in PlaybackSpeed.entries) assertEquals(.5,PlaybackClock.advance(0.0,6/speed.multiplier,speed),1e-12)
    }
    @Test fun pausingPreservesTheExactPositionRegardlessOfElapsedTime() {
        assertEquals(.371,PlaybackClock.advance(.371,100000.0,PlaybackSpeed.VERY_FAST,false),0.0)
        assertEquals(.371,PlaybackClock.advance(.371,0.0,PlaybackSpeed.NORMAL),0.0)
    }
    @Test fun changingSpeedContinuesWithoutJumpingBackToStart() {
        val first=PlaybackClock.advance(0.0,3.0,PlaybackSpeed.NORMAL)
        assertEquals(.25,first,1e-12)
        assertEquals(.75,PlaybackClock.advance(first,3.0,PlaybackSpeed.FAST),1e-12)
    }
    @Test fun frameChunkingDoesNotChangeThePlaybackPosition() {
        for(speed in PlaybackSpeed.entries) {
            var a=0.0;repeat(600) {a=PlaybackClock.advance(a,1.0/60,speed)}
            var b=0.0;repeat(300) {b=PlaybackClock.advance(b,1.0/30,speed)}
            assertEquals(PlaybackClock.advance(0.0,10.0,speed),a,1e-12);assertEquals(a,b,1e-12)
        }
    }
    @Test fun completedDrawingHoldsBeforeTheNextCycle() {
        val hold=PlaybackClock.advance(0.0,13.0,PlaybackSpeed.NORMAL)
        assertEquals(1.0,PlaybackClock.progress(hold),0.0)
        assertEquals(.05,PlaybackClock.advance(hold,2.0,PlaybackSpeed.NORMAL),1e-12)
    }
    @Test fun longDurationsStayFiniteAndBounded() {
        for(speed in PlaybackSpeed.entries) {val position=PlaybackClock.advance(.2,Double.MAX_VALUE,speed)
            assertTrue(position.isFinite() && position in 0.0..PlaybackClock.CYCLE)}
    }
    @Test(expected=IllegalArgumentException::class) fun invalidTimeCannotChangePlayback() {PlaybackClock.advance(.5,Double.NaN,PlaybackSpeed.NORMAL)}
    @Test(expected=IllegalArgumentException::class) fun negativeDeltaIsRejected() {PlaybackClock.advance(.5,-1.0,PlaybackSpeed.NORMAL)}
    @Test fun zoomKeepsTheAnchorUnderTheFingerBeforeClamping() {
        val view=PreviewViewport().transform(2.0,0.0,0.0,.7,.4)
        assertEquals(2.0,view.zoom,0.0);assertEquals(-.2,view.panX,1e-12);assertEquals(.1,view.panY,1e-12)
        val reset=view.transform(.5,0.0,0.0,.7,.4)
        assertEquals(PreviewViewport(),reset)
    }
    @Test fun viewportClampsPanAndZoomAndCentersAtOneHundredPercent() {
        val view=PreviewViewport().transform(100.0,20.0,-20.0)
        assertEquals(4.0,view.zoom,0.0);assertEquals(1.2,view.panX,1e-12);assertEquals(-1.2,view.panY,1e-12)
        assertEquals(PreviewViewport(),view.transform(.001,0.0,0.0))
    }
    @Test(expected=IllegalArgumentException::class) fun nonFiniteViewportIsRejected() {PreviewViewport(Double.NaN)}
    @Test fun allTracePalettesHaveReadableContrastOnBothBackgrounds() {
        fun luminance(color: Int): Double {
            val c=listOf((color ushr 16)and 255,(color ushr 8)and 255,color and 255).map {v ->val s=v/255.0;if(s<=.04045) s/12.92 else ((s+.055)/1.055).pow(2.4)}
            return .2126*c[0]+.7152*c[1]+.0722*c[2]
        }
        for(light in listOf(false,true)) for(color in TraceColor.entries) {
            val a=luminance(color.argb(light));val b=luminance((if(light) 0xFFFFFBF2 else 0xFF0D1929).toInt())
            assertTrue("Insufficient contrast for $color light=$light",(maxOf(a,b)+.05)/(minOf(a,b)+.05)>=4.5)
        }
    }
}
