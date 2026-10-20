package com.fourierart.app.export

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.opengl.EGL14
import android.opengl.EGLExt
import android.opengl.GLES20
import android.opengl.GLUtils
import android.view.Surface
import java.io.Closeable
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Bitmap -> GLES texture -> timestamped encoder Surface. Never records a display or waits in real time. */
internal class VideoEncoder(file: File, width: Int, height: Int, fps: Int) : Closeable {
    private val codec = MediaCodec.createEncoderByType("video/avc")
    private var codecStarted = false
    private var muxer: MediaMuxer? = null
    private var muxerStarted = false
    private var track = -1
    private var input: Surface? = null
    private var display = EGL14.EGL_NO_DISPLAY
    private var context = EGL14.EGL_NO_CONTEXT
    private var surface = EGL14.EGL_NO_SURFACE
    private var texture = 0
    private var program = 0
    private val info = MediaCodec.BufferInfo()
    private var uploaded = false
    private val vertices = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
        // Bitmap first scanline is the top; flip texture coordinates, not the exported drawing.
        put(floatArrayOf(-1f,-1f,0f,1f, 1f,-1f,1f,1f, -1f,1f,0f,0f, 1f,1f,1f,0f)); position(0)
    }
    init {
        try {
            val format = MediaFormat.createVideoFormat("video/avc", width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, if (height > width) 4_000_000 else 3_000_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            input = codec.createInputSurface()
            codec.start(); codecStarted = true
            muxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            check(display != EGL14.EGL_NO_DISPLAY)
            check(EGL14.eglInitialize(display, IntArray(2), 0, IntArray(2), 1))
            val attrs = intArrayOf(EGL14.EGL_RED_SIZE,8,EGL14.EGL_GREEN_SIZE,8,EGL14.EGL_BLUE_SIZE,8,EGL14.EGL_ALPHA_SIZE,8,
                EGL14.EGL_RENDERABLE_TYPE,EGL14.EGL_OPENGL_ES2_BIT,0x3142,1,EGL14.EGL_NONE) // EGL_RECORDABLE_ANDROID
            val configs = arrayOfNulls<android.opengl.EGLConfig>(1); val count = IntArray(1)
            check(EGL14.eglChooseConfig(display,attrs,0,configs,0,1,count,0) && count[0] > 0)
            context = EGL14.eglCreateContext(display,configs[0],EGL14.EGL_NO_CONTEXT,intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION,2,EGL14.EGL_NONE),0)
            check(context != EGL14.EGL_NO_CONTEXT)
            surface = EGL14.eglCreateWindowSurface(display,configs[0],input,intArrayOf(EGL14.EGL_NONE),0)
            check(surface != EGL14.EGL_NO_SURFACE && EGL14.eglMakeCurrent(display,surface,surface,context))
            fun shader(type: Int, source: String): Int {
                val id = GLES20.glCreateShader(type)
                try {
                    GLES20.glShaderSource(id,source); GLES20.glCompileShader(id)
                    val ok = IntArray(1); GLES20.glGetShaderiv(id,GLES20.GL_COMPILE_STATUS,ok,0)
                    check(ok[0] != 0) { GLES20.glGetShaderInfoLog(id) }; return id
                } catch (e: Throwable) { GLES20.glDeleteShader(id); throw e }
            }
            val vertex = shader(GLES20.GL_VERTEX_SHADER,"attribute vec2 aPosition; attribute vec2 aTexture; varying vec2 vTexture; void main(){gl_Position=vec4(aPosition,0.,1.);vTexture=aTexture;}")
            val fragment = shader(GLES20.GL_FRAGMENT_SHADER,"precision mediump float; varying vec2 vTexture; uniform sampler2D uTexture; void main(){gl_FragColor=texture2D(uTexture,vTexture);}")
            try {
                program = GLES20.glCreateProgram(); GLES20.glAttachShader(program,vertex); GLES20.glAttachShader(program,fragment); GLES20.glLinkProgram(program)
                val ok = IntArray(1); GLES20.glGetProgramiv(program,GLES20.GL_LINK_STATUS,ok,0)
                check(ok[0] != 0) { GLES20.glGetProgramInfoLog(program) }
            } finally { GLES20.glDeleteShader(vertex); GLES20.glDeleteShader(fragment) }
            GLES20.glUseProgram(program)
            val tex = IntArray(1); GLES20.glGenTextures(1,tex,0); texture = tex[0]
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0); GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glUniform1i(GLES20.glGetUniformLocation(program,"uTexture"),0)
            for ((name,offset) in listOf("aPosition" to 0,"aTexture" to 2)) {
                val loc = GLES20.glGetAttribLocation(program,name); vertices.position(offset)
                GLES20.glEnableVertexAttribArray(loc); GLES20.glVertexAttribPointer(loc,2,GLES20.GL_FLOAT,false,16,vertices)
            }
            GLES20.glViewport(0,0,width,height)
            check(GLES20.glGetError() == GLES20.GL_NO_ERROR)
        } catch (e: Throwable) { close(); throw e }
    }
    fun frame(bitmap: Bitmap, nanos: Long, checkCancelled: () -> Unit) {
        drain(false,checkCancelled)
        if (!uploaded) { GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,bitmap,0); uploaded = true }
        else GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D,0,0,0,bitmap)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4)
        check(GLES20.glGetError() == GLES20.GL_NO_ERROR)
        check(EGLExt.eglPresentationTimeANDROID(display,surface,nanos))
        check(EGL14.eglSwapBuffers(display,surface))
        drain(false,checkCancelled)
    }
    fun finish(checkCancelled: () -> Unit) {
        codec.signalEndOfInputStream(); drain(true,checkCancelled)
        check(muxerStarted)
        // stop() writes the MP4 index and must succeed before publishing the result.
        muxer!!.stop(); muxerStarted = false
    }
    private fun drain(end: Boolean, checkCancelled: () -> Unit) {
        val deadline = System.nanoTime() + 30_000_000_000L
        while (true) {
            checkCancelled()
            val index = codec.dequeueOutputBuffer(info, if (end) 10_000 else 0)
            when {
                index == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!end) return
                    check(System.nanoTime() < deadline) { "Video encoder timeout" }
                }
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    check(!muxerStarted); track = muxer!!.addTrack(codec.outputFormat); muxer!!.start(); muxerStarted = true
                }
                index >= 0 -> {
                    try {
                        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && info.size > 0) {
                            check(muxerStarted)
                            val data = checkNotNull(codec.getOutputBuffer(index)); data.position(info.offset); data.limit(info.offset + info.size)
                            muxer!!.writeSampleData(track,data,info)
                        }
                    } finally { codec.releaseOutputBuffer(index,false) }
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                }
            }
        }
    }
    override fun close() {
        if (display != EGL14.EGL_NO_DISPLAY) {
            if (context != EGL14.EGL_NO_CONTEXT) {
                if (texture != 0) GLES20.glDeleteTextures(1,intArrayOf(texture),0)
                if (program != 0) GLES20.glDeleteProgram(program)
            }
            EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT)
            if (surface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display,surface)
            if (context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display,context)
            EGL14.eglReleaseThread(); EGL14.eglTerminate(display)
            display = EGL14.EGL_NO_DISPLAY
        }
        runCatching { if (codecStarted) codec.stop() }; runCatching { codec.release() }
        runCatching { input?.release() }
        runCatching { if (muxerStarted) muxer?.stop() }; runCatching { muxer?.release() }; muxer = null
    }
}
