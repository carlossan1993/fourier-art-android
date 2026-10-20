package com.fourierart.app.core

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.random.Random

class ExportTest {
    @Test fun formatsDimensionsAndTimeline() {
        for (format in ExportFormat.entries) for (frame in ExportFrame.entries) for (seconds in listOf(6,10)) {
            val spec = ExportSpec(format,frame,seconds)
            assertEquals(0,spec.width % 2); assertEquals(0,spec.height % 2)
            assertTrue(spec.width * spec.height <= 1080 * 1920)
            assertEquals(if(frame==ExportFrame.SQUARE) spec.width else spec.width*16/9,spec.height)
            assertEquals(1.0,spec.progress(spec.frames-1),0.0)
            if(format!=ExportFormat.PNG) {
                assertEquals(0.0,spec.progress(0),0.0)
                assertEquals(seconds*100,(0 until spec.frames).sumOf { spec.gifDelay(it) })
                for(i in 1 until spec.frames) assertTrue(spec.presentationNanos(i)>spec.presentationNanos(i-1))
                assertTrue(spec.progress((spec.frames*.9).toInt())==1.0)
            }
        }
    }
    @Test fun gifLzwDictionaryGrowthResetAndAllFramesDecode() {
        val w=311; val h=177
        val random=Random(412)
        val frames=listOf(ByteArray(w*h){0},ByteArray(w*h){random.nextInt(256).toByte()},ByteArray(w*h){(it%256).toByte()})
        val palette=IntArray(256){(0xff000000L or (it.toLong() shl 16) or (it.toLong() shl 8) or it.toLong()).toInt()}
        val output=ByteArrayOutputStream()
        StreamingGif(output,w,h,palette).apply {frames.forEachIndexed {i,f ->frame(f,8+i)};finish()}
        // Android's compile bootclasspath omits java.desktop; this JVM-only test uses the JDK
        // decoder reflectively so production never depends on desktop classes.
        val imageIO=Class.forName("javax.imageio.ImageIO")
        val readers=imageIO.getMethod("getImageReadersByFormatName",String::class.java).invoke(null,"gif") as Iterator<*>
        val reader=readers.next()!!
        val readerClass=Class.forName("javax.imageio.ImageReader")
        val stream=Class.forName("javax.imageio.stream.MemoryCacheImageInputStream")
            .getConstructor(java.io.InputStream::class.java).newInstance(ByteArrayInputStream(output.toByteArray()))
        val imageClass=Class.forName("java.awt.image.BufferedImage")
        try {
            readerClass.getMethod("setInput",Any::class.java).invoke(reader,stream)
            assertEquals(3,readerClass.getMethod("getNumImages",Boolean::class.javaPrimitiveType).invoke(reader,true))
            frames.forEachIndexed {index,expected ->
                val image=readerClass.getMethod("read",Int::class.javaPrimitiveType).invoke(reader,index)
                assertEquals(w,imageClass.getMethod("getWidth").invoke(image));assertEquals(h,imageClass.getMethod("getHeight").invoke(image))
                val rgb=imageClass.getMethod("getRGB",Int::class.javaPrimitiveType,Int::class.javaPrimitiveType)
                for(y in 0 until h) for(x in 0 until w) assertEquals("frame=$index x=$x y=$y",palette[expected[y*w+x].toInt() and 255],rgb.invoke(image,x,y))
            }
        } finally {
            readerClass.getMethod("dispose").invoke(reader)
            Class.forName("javax.imageio.stream.ImageInputStream").getMethod("close").invoke(stream)
        }
    }
    @Test fun palettePreservesEndpointsAndCanCancelWhileWriting() {
        val palette=ExportPalette(0xff0d1929.toInt(),0xffff8fae.toInt(),0xff65dcd0.toInt())
        for(index in listOf(0,127,255)) assertEquals(palette.colors[index],palette.colors[palette.index(palette.colors[index]).toInt() and 255])
        val output=ByteArrayOutputStream();var calls=0
        val gif=StreamingGif(output,128,128,palette.colors)
        try {gif.frame(ByteArray(128*128){Random(it).nextInt(256).toByte()},8){if(++calls>2) throw InterruptedException()};fail("Expected cancellation")}
        catch(expected:InterruptedException){assertTrue(calls>2)}
    }
}
