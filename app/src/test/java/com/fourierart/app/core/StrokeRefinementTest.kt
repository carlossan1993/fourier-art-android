package com.fourierart.app.core

import org.junit.Assert.*
import org.junit.Test

class StrokeRefinementTest {
    private fun line(x: Double, y: Double, xx: Double, yy: Double) = ContourStroke(listOf(Point2(x, y), Point2((x+xx)/2, (y+yy)/2), Point2(xx, yy)), false)
    @Test fun joinsOnlyNearbyAlignedFragments() {
        val a = line(0.0, 10.0, 10.0, 10.0)
        assertEquals(1, StrokeRefinement.join(listOf(a, line(12.0, 10.0, 20.0, 10.0)), 3.0).size)
        assertEquals(2, StrokeRefinement.join(listOf(a, line(10.0, 12.0, 20.0, 12.0)), 3.0).size)
        assertEquals(2, StrokeRefinement.join(listOf(a, line(12.0, 10.0, 12.0, 20.0)), 3.0).size)
        assertEquals(2, StrokeRefinement.join(listOf(a, line(20.0, 10.0, 30.0, 10.0)), 3.0).size)
    }
    @Test fun spatialBudgetKeepsShortFeatureInAnotherRegion() {
        val longSeams = (0 until 100).map { line(110.0, 110.0+it/10.0, 195.0, 110.0+it/10.0) }
        val eye = line(20.0, 20.0, 25.0, 20.0)
        val result = StrokeRefinement.select(longSeams + eye, 200, 200, 20)
        assertTrue(result.contains(eye))
        assertEquals(20, result.size)
    }
    @Test fun lineTracingDoesNotSilentlyTruncateAtForty() {
        val w=120; val h=120
        val edges = BooleanArray(w*h)
        for (i in 0 until 60) { val x=5+(i%10)*11; val y=5+(i/10)*18
            for (dx in 0..5) edges[y*w+x+dx]=true }
        assertEquals(60, RasterContours.lineStrokes(edges,w,h,3.0).size)
    }
    @Test fun plainWhiteBackgroundIsFloodedButEnclosedWhiteStaysInSubject() {
        val w=80; val h=100
        val p=IntArray(w*h) { i -> val x=i%w; val y=i/w
            if(x in 20..60 && y in 20..99 && !(x in 30..40 && y in 40..50)) 0xff303030.toInt() else -1 }
        val mask=RasterContours.plainBackgroundMask(p,w,h,50)
        assertNotNull(mask)
        assertFalse(mask!![0]); assertTrue(mask[45*w+35]); assertTrue(mask[99*w+40])
    }
    @Test fun complexBorderDoesNotPretendToBePlainBackground() {
        val w=80; val h=80
        val p=IntArray(w*h) { i -> 0xff000000.toInt() or ((i*73 and 255) shl 16) or ((i*37 and 255) shl 8) or (i*91 and 255) }
        assertNull(RasterContours.plainBackgroundMask(p,w,h,50))
    }
    @Test fun croppedShouldersAreKeptWithoutClosingAlongPhotoFrame() {
        val w=80; val h=100
        val mask=BooleanArray(w*h) { i -> val x=i%w; val y=i/w
            y>=65 || (x in 25..55 && y in 15..70) }
        val raw=RasterContours.outlines(mask,w,h,rejectFrame=false)
        val clipped=raw.flatMap { StrokeRefinement.clipFrame(it,w,h) }
        assertTrue(clipped.isNotEmpty())
        assertTrue(clipped.none { it.closed })
        assertTrue(clipped.flatMap { it.points }.all { it.x>1 && it.x<w-1 && it.y>1 && it.y<h-1 })
        assertTrue(clipped.flatMap { it.points }.any { it.y==15.0 })
    }

}
