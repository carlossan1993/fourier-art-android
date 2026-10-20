package com.fourierart.app.core

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class FourierEngineTest {
    private val path=listOf(Point2(3.0,-2.0),Point2(5.0,1.0),Point2(-1.0,3.0),Point2(-2.0,-1.0))
    private fun mse(curve: FourierCurve,n: Int)=curve.points.indices.sumOf {i ->
        val p=curve.points[i];val q=curve.sample(2*PI*i/curve.points.size,n)
        (p.x-q.x).pow(2)+(p.y-q.y).pow(2)
    }/curve.points.size
    @Test fun fftMatchesIndependentDftAcrossSampleSizesAndSignedFrequencies() {
        for(size in listOf(64,128,256,512,1024)) {
            val curve=FourierCurve.fromClosedPath(path,size)
            assertEquals(size,curve.harmonics.map {it.frequency}.distinct().size)
            for(h in curve.harmonics.filterIndexed {i,_->size<=128 || i%13==0}) {
                var real=0.0;var imaginary=0.0
                curve.points.forEachIndexed {i,p ->
                    val a=-2*PI*h.frequency*i/size
                    real+=p.x*cos(a)-p.y*sin(a);imaginary+=p.x*sin(a)+p.y*cos(a)
                }
                assertEquals(real/size,h.real,1e-10);assertEquals(imaginary/size,h.imaginary,1e-10)
            }
        }
    }
    @Test fun parsevalMatchesMeasuredErrorAndEveryPrefixImproves() {
        for(curve in listOf(FourierCurve.fromClosedPath(path,64),FourierCurve.fromOpenPath(path,64))) {
            var previous=Double.POSITIVE_INFINITY
            for(n in 1..64) {
                val measured=mse(curve,n)
                assertTrue(measured<=previous+1e-12);previous=measured
                assertEquals(measured,curve.reconstructionRms(n).pow(2),1e-10)
            }
            assertTrue(sqrt(previous)<1e-10)
        }
    }
    @Test fun amplitudeOrderingKeepsDcFirstAndUsesBothFrequencySigns() {
        val curve=FourierCurve.fromClosedPath(path)
        assertEquals(0,curve.harmonics.first().frequency)
        assertTrue(curve.harmonics.any {it.frequency<0})
        assertTrue(curve.harmonics.drop(1).zipWithNext().all {(a,b)->a.radius>=b.radius})
        val clockwise=FourierCurve.fromClosedPath((0 until 256).map {val t=-2*PI*it/256;Point2(4+cos(t),-3+sin(t))})
        assertEquals(-1,clockwise.harmonics[1].frequency)
        assertTrue(clockwise.reconstructionRms(2)<1e-10)
    }
    @Test fun bestComponentsBeatOldFrequencyOrderAtSameBudget() {
        val curve=FourierCurve.fromClosedPath((0 until 2048).map {val t=2*PI*it/2048;Point2(cos(t)+.2*cos(17*t),sin(t)+.2*sin(17*t))},1024)
        val oldOrder=curve.harmonics.sortedWith(compareBy<Harmonic> {abs(it.frequency)}.thenByDescending {it.frequency}).take(10).toSet()
        val oldPower=curve.harmonics.filter {it !in oldOrder}.sumOf {it.real*it.real+it.imaginary*it.imaginary}
        assertTrue(curve.reconstructionRms(10).pow(2)<oldPower*.9)
    }
    @Test fun repeatedBuildsAndProgressRequestsAreExactlyDeterministic() {
        val a=FourierCurve.fromOpenPath(path,512);val b=FourierCurve.fromOpenPath(path.toList(),512)
        assertEquals(a.points,b.points);assertEquals(a.harmonics,b.harmonics)
        assertEquals(a.trace(200,1024,.5),b.trace(200,1024,.5))
    }
    @Test fun adaptiveTraceSamplesFastCyclesAndAnchorsOpenEndpoint() {
        val curve=FourierCurve.fromOpenPath(path,1024);val stroke=DrawingStroke(curve,false)
        for(n in listOf(10,50,200,800,1024)) {
            val frames=curve.recommendedFrames(n,.5)
            val frequency=curve.harmonics.take(curve.effectiveComponents(n)).maxOf {abs(it.frequency)}
            assertTrue(frames>=ceil(2.0*frequency).toInt())
            val trace=stroke.trace(n)
            assertEquals(frames+1,trace.size)
            assertEquals(stroke.position(0.0,n),trace.first());assertEquals(stroke.position(1.0,n),trace.last())
            for(i in trace.indices step 37) {
                val p=stroke.position(i.toDouble()/frames,n)
                assertEquals(p.x,trace[i].x,1e-10);assertEquals(p.y,trace[i].y,1e-10)
            }
        }
    }
    @Test fun openStrokeKeepsItsReturnSymmetryAtEveryBudget() {
        val curve=FourierCurve.fromOpenPath(path,64)
        for(n in 1..64) {
            assertTrue(curve.effectiveComponents(n)<=n)
            for(j in 1..19) {
                val t=PI*j/20;val forward=curve.sample(t,n);val backward=curve.sample(2*PI-t,n)
                assertEquals(forward.x,backward.x,1e-10);assertEquals(forward.y,backward.y,1e-10)
            }
        }
    }
    @Test fun reflectedNyquistEpicyclesEndAtTheSameSamplePosition() {
        val curve=FourierCurve.fromOpenPath(path,64)
        for(n in listOf(10,50,64)) for(j in 0..30) {
            val t=PI*j/30
            val endpoint=curve.harmonics.take(curve.effectiveComponents(n)).flatMap {it.rotations()}.map {it.at(t)}
            val expected=curve.sample(t,n)
            assertEquals(expected.x,endpoint.sumOf {it.x},1e-10);assertEquals(expected.y,endpoint.sumOf {it.y},1e-10)
        }
    }
    @Test fun duplicateVerticesAndUnevenSpacingDoNotChangeArcLengthSamples() {
        val a=FourierCurve.fromClosedPath(listOf(Point2(0.0,0.0),Point2(4.0,0.0),Point2(4.0,4.0),Point2(0.0,4.0)))
        val b=FourierCurve.fromClosedPath(listOf(Point2(0.0,0.0),Point2(0.0,0.0),Point2(1.0,0.0),Point2(4.0,0.0),Point2(4.0,4.0),Point2(0.0,4.0),Point2(0.0,0.0)))
        assertEquals(a.points,b.points);assertEquals(a.harmonics,b.harmonics)
    }
    @Test fun componentLimitsAreExplicitAndPeriodIsStable() {
        val curve=FourierCurve.fromClosedPath(path,64)
        assertEquals(1,curve.effectiveComponents(0));assertEquals(64,curve.effectiveComponents(800))
        val a=curve.sample(.37,50);val b=curve.sample(.37+200*PI,50)
        assertEquals(a.x,b.x,1e-9);assertEquals(a.y,b.y,1e-9)
    }
    @Test fun invalidContoursAndRenderParametersAreRejected() {
        fun rejects(block: ()->Unit) {try {block();fail("Expected invalid input rejection")} catch(_:IllegalArgumentException) {}}
        rejects {FourierCurve.fromClosedPath(listOf(Point2(Double.NaN,0.0),Point2(1.0,1.0),Point2(2.0,3.0)))}
        rejects {FourierCurve.fromOpenPath(listOf(Point2(0.0,0.0),Point2(0.0,0.0)))}
        rejects {FourierCurve.fromClosedPath(path,100)}
        rejects {FourierCurve.circle().sample(Double.NaN,20)}
        rejects {FourierCurve.circle().trace(20,0)}
        rejects {FourierCurve.circle().trace(20,128,Double.POSITIVE_INFINITY)}
        rejects {ContourDrawing.fromStrokes(listOf(ContourStroke(listOf(Point2(Double.NaN,0.0),Point2(1.0,1.0)),false),ContourStroke(path,true)))}
    }
}
