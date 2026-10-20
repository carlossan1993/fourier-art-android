package com.fourierart.app.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlin.math.max

object PhotoIO {
    fun decode(context: Context, uri: Uri): Bitmap {
        val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        (context.contentResolver.openInputStream(uri) ?: error("Image unavailable")).use { BitmapFactory.decodeStream(it,null,bounds) }
        require(bounds.outWidth>0 && bounds.outHeight>0) { "Invalid image" }
        var sample=1
        while(max(bounds.outWidth,bounds.outHeight)/sample>1800) sample*=2
        val bitmap=context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply { inSampleSize=sample })
        } ?: error("Image decoding failed")
        val orientation=try { context.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL } catch (_:Exception) { ExifInterface.ORIENTATION_NORMAL }
        val oriented=orient(bitmap,orientation)
        if(oriented!==bitmap) bitmap.recycle()
        return oriented
    }
    fun orient(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix=Matrix()
        when(orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f,1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f,-1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {matrix.setRotate(90f);matrix.postScale(-1f,1f)}
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {matrix.setRotate(-90f);matrix.postScale(-1f,1f)}
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,matrix,true)
    }
    fun rotate(source: Bitmap, turns: Int): Bitmap = if(turns==0) source else Bitmap.createBitmap(source,0,0,
        source.width,source.height,Matrix().apply { setRotate(turns*90f) },true)
    fun prepare(source: Bitmap, preparation: PhotoPreparation): Bitmap {
        val rotated=rotate(source,preparation.turns)
        val r=preparation.crop.pixels(rotated.width,rotated.height)
        return Bitmap.createBitmap(rotated,r[0],r[1],r[2],r[3])
    }
}
