package com.fourierart.app.photo

import org.junit.Assert.*
import org.junit.Test

class PhotoPreparationTest {
    @Test fun fourTurnsRestoreCropAndOppositeTurnsCancel() {
        val p=PhotoPreparation(0,CropBox(.12f,.2f,.8f,.91f))
        val q=p.clockwise().clockwise().clockwise().clockwise()
        assertEquals(p.turns,q.turns)
        assertEquals(p.crop.left,q.crop.left,0.00001f);assertEquals(p.crop.bottom,q.crop.bottom,0.00001f)
        val back=p.clockwise().counterclockwise()
        assertEquals(p.crop.top,back.crop.top,0.00001f);assertEquals(0,back.turns)
    }
    @Test fun movingSelectionClampsToImageAndKeepsItsSize() {
        val c=CropBox(.2f,.3f,.7f,.8f).moved(4f,-4f)
        assertEquals(1f,c.right,0.00001f);assertEquals(0f,c.top,0.00001f)
        assertEquals(.5f,c.right-c.left,0.00001f);assertEquals(.5f,c.bottom-c.top,0.00001f)
    }
    @Test fun handlesCannotInvertOrEscapeAndPixelCropIsNonEmpty() {
        val c=CropBox().corner(0,2f,2f).corner(2,-2f,-2f)
        assertTrue(c.right>c.left && c.bottom>c.top)
        for(w in listOf(1,13,1800)) for(h in listOf(1,17,900)) {
            val p=c.pixels(w,h)
            assertTrue(p[0]>=0 && p[1]>=0 && p[2]>0 && p[3]>0 && p[0]+p[2]<=w && p[1]+p[3]<=h)
        }
    }
    @Test(expected=IllegalArgumentException::class) fun invalidCropCannotReachBitmapCode() {CropBox(Float.NaN,0f,1f,1f)}
}
