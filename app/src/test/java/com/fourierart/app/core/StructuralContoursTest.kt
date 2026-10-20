package com.fourierart.app.core

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class StructuralContoursTest {
    @Test fun equalLuminanceColorsStillHaveAnObservedBoundary() {
        val w=100;val h=80
        val p=IntArray(w*h) { i -> if(i%w<50) 0xffc83232.toInt() else 0xff327f32.toInt() }
        val grayEdges=RasterContours.edges(p,w,h,80)
        val colorEdges=StructuralContours.field(StructuralContours.smooth(p,w,h),w,h,80).edges
        assertEquals(0,grayEdges.count { it })
        assertTrue(colorEdges.indices.count { colorEdges[it] && it%w in 48..51 }>60)
    }

    @Test fun bilateralRemovesSmallTextureWithoutBlurringStrongBoundary() {
        val w=60;val h=40
        val p=IntArray(w*h) { i -> val x=i%w;val y=i/w
            val v=(if(x<30) 40 else 210)+(if((x+y)%2==0) 8 else -8)
            0xff000000.toInt() or (v shl 16) or (v shl 8) or v }
        val filtered=StructuralContours.smooth(p,w,h)
        fun v(i:Int)=filtered[i] and 255
        assertTrue(abs(v(20*w+10)-v(20*w+11))<5)
        assertTrue(v(20*w+30)-v(20*w+29)>140)
    }

    @Test fun constantAndTransparentPicturesNeverInventContours() {
        for(p in listOf(IntArray(80*80){0xffeeeeee.toInt()},IntArray(80*80))) {
            assertTrue(StructuralContours.extract(p,80,80,100,null,DetailLevel.FINE).isEmpty())
        }
    }

    @Test fun maskPreventsOutsideEdgesAndDarkRegionFrames() {
        val w=100;val h=80
        val p=IntArray(w*h) {i -> if(i%w in 30..70 && i/w in 20..60) -1 else 0xff101010.toInt() }
        val allowed=BooleanArray(w*h) {i -> i%w in 25..75 && i/w in 15..65}
        val lines=StructuralContours.extract(p,w,h,80,allowed,DetailLevel.FINE)
        assertTrue(lines.isNotEmpty())
        assertTrue(lines.flatMap { it.points }.all {it.x in 25.0..75.0 && it.y in 15.0..65.0})
        assertTrue(StructuralContours.darkRegions(p,w,h,null).isEmpty())
    }

    @Test fun compactDarkPatchGetsItsOwnClosedOutlineBesideTexture() {
        val w=200;val h=160
        val p=IntArray(w*h) {i -> val x=i%w;val y=i/w
            val value=if(((x-100)*(x-100)/225.0+(y-65)*(y-65)/81.0)<1) 15 else 180+(x+y)%14
            0xff000000.toInt() or (value shl 16) or (value shl 8) or value }
        val dark=StructuralContours.darkRegions(p,w,h,null)
        assertTrue(dark.any { s -> s.closed && s.points.all { it.x in 84.0..116.0 && it.y in 55.0..75.0 } })
        val lines=StructuralContours.extract(p,w,h,70,null,DetailLevel.CLEAN)
        assertTrue(lines.any { it.closed && it.points.any { p -> p.x in 85.0..115.0 && p.y in 55.0..75.0 } })
        assertTrue(lines.size<=100)
    }

    @Test fun junctionContinuationRetainsOneLongBoundaryWithoutDrawingGaps() {
        val w=70;val h=70;val edges=BooleanArray(w*h)
        for(x in 5..60) edges[35*w+x]=true
        for(y in 10..34) edges[y*w+30]=true
        val strokes=RasterContours.lineStrokes(edges,w,h,1.0,true)
        assertTrue(strokes.any { it.length>=55 && it.points.all { p -> p.y==35.0 } })
        assertTrue(strokes.flatMap { it.points }.all { edges[it.y.toInt()*w+it.x.toInt()] })
    }

    @Test fun simplificationKeepsAClosedHoleAndBoundsLineError() {
        val p=(0 until 200).map { i -> val a=i*2*PI/200;Point2(50+20*cos(a),50+20*sin(a)) }
        val s=StrokeRefinement.simplify(ContourStroke(p,true),0.8)
        assertTrue(s.closed);assertTrue(s.points.size in 8..40)
        assertTrue(abs(RasterContours.area(p)-RasterContours.area(s.points))/abs(RasterContours.area(p))<0.06)
    }
}
