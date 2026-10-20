package com.fourierart.app.core

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class DrawingTraceCacheTest {
    private fun stroke()=DrawingStroke(FourierCurve.fromOpenPath(listOf(
        Point2(0.0,0.0),Point2(1.0,2.0),Point2(3.0,-1.0),Point2(4.0,1.0))),false)
    @Test fun cachedPointsMatchDirectFourierAndOnlyKeepOneConfiguration() {
        val s=stroke()
        val first=s.trace(200)
        assertEquals(s.curve.trace(200,s.curve.recommendedFrames(200,.5),.5),first)
        assertSame(first,s.trace(200))
        val changed=s.trace(10)
        assertNotSame(first,changed)
        assertEquals(s.curve.trace(10,s.curve.recommendedFrames(10,.5),.5),changed)
        val restored=s.trace(200)
        assertEquals(first,restored);assertNotSame(first,restored)
        val differentFrames=s.trace(200,256)
        assertEquals(257,differentFrames.size)
        assertEquals(s.position(1.0,200),differentFrames.last())
    }
    @Test fun equivalentEffectiveCountsReuseTheSameTrace() {
        val s=stroke();val a=s.trace(199)
        assertSame(a,s.trace(200))
    }
    @Test fun concurrentRequestsCannotMixConfigurations() {
        val s=stroke();val workers=Executors.newFixedThreadPool(4)
        try {
            val results=workers.invokeAll((0 until 24).map {i ->Callable {
                val n=if(i%2==0)20 else 200
                val points=s.trace(n,256)
                assertEquals(s.curve.trace(n,256,.5),points)
                assertEquals(s.position(1.0,n),points.last())
                points
            }})
            results.forEach {it.get()}
        } finally {workers.shutdownNow()}
    }
}
