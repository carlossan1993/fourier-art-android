package com.fourierart.app.core

import org.junit.Assert.*
import org.junit.Test

class DetailSelectorTest {
    @Test fun realStructuralEdgeWinsOverLongerFaintTexture() {
        val w = 160; val h = 100
        val pixels = IntArray(w*h) { i ->
            val x = i%w; val y = i/w
            val v = if (y<50 && x>60) 20 else if (y>=50 && x>60) 230 else 255
            0xff000000.toInt() or (v shl 16) or (v shl 8) or v
        }
        val meaningful = ContourStroke((10..40).map { Point2(60.0,it.toDouble()) },false)
        val texture = ContourStroke((65..95).map { Point2(60.0,it.toDouble()) },false)
        val structural = BooleanArray(w*h) { i -> i%w==60 && i/w in 10..40 }
        assertEquals(meaningful,DetailSelector.select(listOf(texture,meaningful),pixels,w,h,structural,1).single())
    }

    @Test fun detailLevelsKeepStablePrefixAndDoNotForceInventedStrokes() {
        val w=1000;val h=1000
        val input=(0 until 450).map { i ->
            val x=10.0+(i%25)*38;val y=10.0+(i/25)*40
            ContourStroke(listOf(Point2(x,y),Point2(x+5,y),Point2(x+10,y)),false)
        }
        val pixels=IntArray(w*h){-1};val structural=BooleanArray(w*h){true}
        val simple=DetailSelector.select(input,pixels,w,h,structural,100)
        val fine=DetailSelector.select(input,pixels,w,h,structural,400)
        assertEquals(100,simple.size);assertEquals(400,fine.size)
        assertEquals(simple,fine.take(100))
        assertEquals(2,DetailSelector.select(input.take(2),pixels,w,h,structural,400).size)
    }
}
