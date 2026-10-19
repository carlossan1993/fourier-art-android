package com.fourierart.app.core

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class PortraitComposerTest {
    private fun line(vararg xy: Double) = ContourStroke(xy.toList().chunked(2).map { Point2(it[0], it[1]) }, false)
    private fun loop(cx: Double, cy: Double, rx: Double, ry: Double) = ContourStroke((0 until 36).map {
        val a = it * 2 * PI / 36; Point2(cx + rx * cos(a), cy + ry * sin(a)) }, true)
    private fun face(): FacialGeometry = FacialGeometry(mapOf(
        FacePart.OVAL to loop(100.0, 80.0, 40.0, 55.0),
        FacePart.LEFT_EYE to loop(85.0, 65.0, 8.0, 3.0),
        FacePart.RIGHT_EYE to loop(115.0, 65.0, 8.0, 3.0),
        FacePart.NOSE_BRIDGE to line(100.0, 65.0, 100.0, 88.0),
        FacePart.NOSE_BOTTOM to line(100.0, 92.0, 94.0, 90.0, 106.0, 90.0),
        FacePart.UPPER_LIP_TOP to line(90.0, 106.0, 100.0, 102.0, 110.0, 106.0),
        FacePart.LOWER_LIP_BOTTOM to line(90.0, 106.0, 100.0, 111.0, 110.0, 106.0)))

    @Test fun clothingCannotConsumeEitherEyeOrNose() {
        val face = face()
        val clothes = (0 until 180).map { i -> line(5.0, 150.0 + i / 5.0, 195.0, 150.0 + i / 5.0) }
        val result = PortraitComposer.compose(face, IntArray(200 * 200) { -1 }, 200, 200, emptyList(), clothes)
        assertTrue(face.complete)
        for (part in listOf(FacePart.LEFT_EYE, FacePart.RIGHT_EYE)) {
            assertTrue("Lost semantic feature $part", result.any { it.points == face.parts[part]!!.points })
        }
        assertEquals(2,face.parts[FacePart.NOSE_BRIDGE]!!.points.size)
        assertTrue(result.any { it.points.contains(face.parts[FacePart.NOSE_BOTTOM]!!.points.first()) })
        assertTrue(result.size <= 80)
    }

    @Test fun missingEyeDoesNotPassPortraitCompleteness() {
        val face = face()
        assertFalse(FacialGeometry(face.parts - FacePart.RIGHT_EYE).complete)
    }

    @Test fun noseBaseIsOrderedThroughCenter() {
        val result = PortraitComposer.compose(face(), IntArray(40000) { -1 }, 200, 200, emptyList(), emptyList())
        assertTrue(result.any { it.points == listOf(Point2(94.0, 90.0), Point2(100.0, 92.0), Point2(106.0, 90.0)) })
    }

    @Test fun shadowEdgesInsideFaceAreNotDrawnOverProtectedFeatures() {
        val noise = line(80.0, 85.0, 100.0, 85.0, 120.0, 85.0)
        val result = PortraitComposer.compose(face(), IntArray(40000) { -1 }, 200, 200, emptyList(), listOf(noise))
        assertFalse(result.any { it.points == noise.points })
    }

    @Test fun irisUsesObservedDarkPixelsAndDoesNotInventOnFlatPatch() {
        val pixels = IntArray(80 * 40) { -1 }
        for (y in 14..24) for (x in 33..42) pixels[y * 80 + x] = 0xff202020.toInt()
        val eye = listOf(Point2(20.0, 12.0), Point2(60.0, 12.0), Point2(60.0, 27.0), Point2(20.0, 27.0))
        val iris = PortraitComposer.iris(pixels, 80, 40, eye)
        assertNotNull(iris)
        assertTrue(iris!!.points.all { it.x in 33.0..43.0 && it.y in 14.0..25.0 })
        assertNull(PortraitComposer.iris(IntArray(3200) { -1 }, 80, 40, eye))
    }
    @Test fun flatNoseDoesNotInventStraightBridgeAndObservedEdgesStayNearNose() {
        val face=face()
        val white=IntArray(40000){-1}
        assertTrue(PortraitComposer.nose(white,200,200,face).isEmpty())
        val composed=PortraitComposer.compose(face,white,200,200,emptyList(),emptyList())
        assertFalse(composed.any { it.points == face.parts[FacePart.NOSE_BRIDGE]!!.points })
        val pixels=IntArray(40000) { i ->
            val x=i%200;val y=i/200
            val v=if((x in 94..98 || x in 103..107) && y in 85..92) 40 else 150+(x%12)*5
            0xff000000.toInt() or (v shl 16) or (v shl 8) or v
        }
        val observed=PortraitComposer.nose(pixels,200,200,face)
        assertTrue(observed.isNotEmpty())
        assertTrue(observed.flatMap{it.points}.all{it.x in 89.0..111.0 && it.y in 72.0..96.0})
    }

}
