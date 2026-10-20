package com.fourierart.app.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import com.fourierart.app.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

object ExportEngine {
    fun directory(context: Context) = File(context.cacheDir,"exports").apply { check(isDirectory || mkdirs()) }
    fun clearStale(context: Context) {
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        directory(context).listFiles()?.filter { it.lastModified() < cutoff }?.forEach { it.delete() }
    }
    /** Owns one bitmap and bounded codec/index buffers. No list of rendered frames is retained. */
    suspend fun render(context: Context, snapshot: ExportSnapshot, spec: ExportSpec, progress: (Float) -> Unit = {}): File {
        var published: File? = null
        return try { withContext(Dispatchers.Default) {
        val coroutine = currentCoroutineContext()
        val checkCancelled = { coroutine.ensureActive() }
        val dir = directory(context)
        val id = UUID.randomUUID().toString()
        val partial = File(dir,"$id.part"); val result = File(dir,"fourier-$id.${spec.format.extension}")
        var bitmap: Bitmap? = null
        try {
            checkCancelled()
            val renderer = ExportRenderer(snapshot)
            bitmap = Bitmap.createBitmap(spec.width,spec.height,Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            when (spec.format) {
                ExportFormat.PNG -> {
                    renderer.draw(canvas,spec.width,spec.height,1.0); checkCancelled()
                    partial.outputStream().buffered().use { check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) }
                    progress(.95f)
                }
                ExportFormat.GIF -> {
                    val palette = ExportPalette(renderer.background,renderer.ink,renderer.guide)
                    val pixels = IntArray(spec.width * spec.height); val indices = ByteArray(pixels.size)
                    partial.outputStream().buffered().use { out ->
                        val gif = StreamingGif(out,spec.width,spec.height,palette.colors)
                        for (i in 0 until spec.frames) {
                            checkCancelled(); renderer.draw(canvas,spec.width,spec.height,spec.progress(i))
                            bitmap.getPixels(pixels,0,spec.width,0,0,spec.width,spec.height)
                            for (j in pixels.indices) { if (j % 8192 == 0) checkCancelled(); indices[j] = palette.index(pixels[j]) }
                            gif.frame(indices,spec.gifDelay(i),checkCancelled)
                            progress((i + 1f) / spec.frames * .97f)
                        }
                        gif.finish()
                    }
                }
                ExportFormat.MP4 -> VideoEncoder(partial,spec.width,spec.height,spec.fps).use { video ->
                    for (i in 0 until spec.frames) {
                        checkCancelled(); renderer.draw(canvas,spec.width,spec.height,spec.progress(i))
                        video.frame(bitmap,spec.presentationNanos(i),checkCancelled)
                        progress((i + 1f) / spec.frames * .97f)
                    }
                    video.finish(checkCancelled)
                }
            }
            checkCancelled()
            check(partial.length() > 0 && partial.renameTo(result))
            // Publishing and return must be atomic with respect to coroutine cancellation.
            progress(1f)
            published = result
            result
        } catch (e: Throwable) { partial.delete(); result.delete(); throw e }
        finally { bitmap?.recycle() }
        } } catch (e: Throwable) { published?.delete(); throw e }
    }
}
