package com.fourierart.app.photo

import android.graphics.*
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PhotoPreparationAndroidTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private fun quadrants():Bitmap = Bitmap.createBitmap(80,60,Bitmap.Config.ARGB_8888).also { b ->
        val c=Canvas(b);val p=Paint()
        for((i,color) in listOf(Color.RED,Color.GREEN,Color.BLUE,Color.YELLOW).withIndex()) {
            p.color=color;val x=(i%2)*40f;val y=(i/2)*30f;c.drawRect(x,y,x+40,y+30,p)
        }
    }
    private fun corners(b: Bitmap)=listOf(b.getPixel(b.width/4,b.height/4),b.getPixel(b.width*3/4,b.height/4),
        b.getPixel(b.width/4,b.height*3/4),b.getPixel(b.width*3/4,b.height*3/4))
    @Test fun allEightExifOrientationsDecodeExactlyOnce() {
        val source=quadrants();val colors=listOf(Color.RED,Color.GREEN,Color.BLUE,Color.YELLOW)
        val expected=listOf(listOf(0,1,2,3),listOf(1,0,3,2),listOf(3,2,1,0),listOf(2,3,0,1),
            listOf(0,2,1,3),listOf(2,0,3,1),listOf(3,1,2,0),listOf(1,3,0,2))
        val folder=File(context.cacheDir,"exif-tests").apply {mkdirs()}
        try {
            for(o in 1..8) {
                val file=File(folder,"$o.jpg");file.outputStream().use {source.compress(Bitmap.CompressFormat.JPEG,100,it)}
                ExifInterface(file).apply {setAttribute(ExifInterface.TAG_ORIENTATION,o.toString());saveAttributes()}
                val decoded=PhotoIO.decode(context,Uri.fromFile(file))
                assertEquals(if(o>=5) 60 else 80,decoded.width)
                for((i,c) in corners(decoded).withIndex()) {
                    val e=colors[expected[o-1][i]]
                    assertTrue("EXIF $o quadrant $i",kotlin.math.abs(Color.red(e)-Color.red(c))<12 && kotlin.math.abs(Color.green(e)-Color.green(c))<12 && kotlin.math.abs(Color.blue(e)-Color.blue(c))<12)
                }
            }
        } finally {folder.deleteRecursively();source.recycle()}
    }
    @Test fun manualRotationAndCropShareTheSameImageCoordinates() {
        val original=quadrants()
        val prepared=PhotoIO.prepare(original,PhotoPreparation(1,CropBox(.5f,0f,1f,.5f)))
        assertEquals(30,prepared.width);assertEquals(40,prepared.height)
        assertTrue(corners(prepared).all {it==Color.RED})
        assertEquals(listOf(Color.RED,Color.GREEN,Color.BLUE,Color.YELLOW),corners(PhotoIO.prepare(original,PhotoPreparation())))
    }
    @Test fun privateSessionRestoresSourceDraftAndAppliedStateWithoutAccumulatingPhotos() {
        val folder=File(context.cacheDir,"session-store-test");val store=PhotoSessionStore(context,folder)
        try {
            val source=PhotoIO.orient(quadrants(),ExifInterface.ORIENTATION_ROTATE_90)
            val file=store.saveSource(source);val prep=PhotoPreparation(1,CropBox(.1f,.2f,.8f,.9f))
            store.write(file,prep,prep,true,JSONObject().put("mode","DRAWING"))
            val restored=PhotoSessionStore(context,folder).read()!!
            assertEquals(60,restored.source.width);assertEquals(80,restored.source.height)
            assertEquals(prep,restored.applied);assertEquals(prep,restored.draft);assertTrue(restored.editing)
            assertEquals("DRAWING",restored.options.getString("mode"))
            assertEquals(corners(source),corners(restored.source))
            val second=store.saveSource(quadrants());store.write(second,null,PhotoPreparation(),true,JSONObject())
            assertEquals(1,folder.listFiles()!!.count {it.extension=="png"})
            store.clear();assertNull(store.read());assertTrue(folder.listFiles().orEmpty().isEmpty())
        } finally {store.clear();folder.deleteRecursively()}
    }
}
