package com.fourierart.app.core

import java.io.OutputStream

/** GIF89a, global palette, looping, full opaque frames and streaming LZW (maximum 4096 entries). */
class StreamingGif(private val out: OutputStream, private val width: Int, private val height: Int, palette: IntArray) {
    init {
        require(width in 1..65535 && height in 1..65535 && palette.size == 256)
        ascii("GIF89a"); word(width); word(height); byte(0xf7); byte(0); byte(0)
        palette.forEach { byte(it ushr 16); byte(it ushr 8); byte(it) }
        byte(0x21); byte(0xff); byte(11); ascii("NETSCAPE2.0"); byte(3); byte(1); word(0); byte(0)
    }
    fun frame(indices: ByteArray, delayCentiseconds: Int, check: () -> Unit = {}) {
        require(indices.size == width * height && delayCentiseconds in 1..65535)
        check()
        byte(0x21); byte(0xf9); byte(4); byte(4); word(delayCentiseconds); byte(0); byte(0)
        byte(0x2c); word(0); word(0); word(width); word(height); byte(0); byte(8)
        val block = ByteArray(255); var blockSize = 0
        var bits = 0; var accumulator = 0
        fun packed(value: Int) {
            block[blockSize++] = value.toByte()
            if (blockSize == 255) { byte(blockSize); out.write(block, 0, blockSize); blockSize = 0; check() }
        }
        var size = 9; var next = 258
        fun code(value: Int) {
            accumulator = accumulator or (value shl bits); bits += size
            while (bits >= 8) { packed(accumulator and 255); accumulator = accumulator ushr 8; bits -= 8 }
        }
        // Int-key dictionary avoids strings/large per-frame allocations.
        val keys = IntArray(8192) { -1 }; val values = IntArray(8192)
        fun slot(key: Int): Int {
            var i = ((key * -1640531527) ushr 19) and 8191
            while (keys[i] != -1 && keys[i] != key) i = (i + 1) and 8191
            return i
        }
        code(256)
        var prefix = indices[0].toInt() and 255
        for (i in 1 until indices.size) {
            val symbol = indices[i].toInt() and 255
            val key = (prefix shl 8) or symbol; val at = slot(key)
            if (keys[at] == key) prefix = values[at] else {
                code(prefix)
                // Decoder creates this entry after receiving the NEXT code: grow after emission.
                if (next == (1 shl size) && size < 12) size++
                if (next < 4096) { keys[at] = key; values[at] = next++ }
                else { code(256); keys.fill(-1); size = 9; next = 258 }
                prefix = symbol
            }
        }
        code(prefix)
        if (next == (1 shl size) && size < 12) size++
        code(257)
        if (bits > 0) packed(accumulator and 255)
        if (blockSize > 0) { byte(blockSize); out.write(block, 0, blockSize) }
        byte(0)
    }
    fun finish() { byte(0x3b); out.flush() }
    private fun byte(v: Int) = out.write(v and 255)
    private fun word(v: Int) { byte(v); byte(v ushr 8) }
    private fun ascii(v: String) = out.write(v.toByteArray(Charsets.US_ASCII))
}

/** Two colour ramps preserve the chosen ink, background and antialiased epicycle guides. */
class ExportPalette(background: Int, ink: Int, guide: Int) {
    val colors = IntArray(256) { i ->
        val color = if (i < 128) ink else guide
        val t = (i % 128) / 127.0
        fun channel(shift: Int) = (((background ushr shift) and 255) * (1 - t) + ((color ushr shift) and 255) * t + .5).toInt()
        (255 shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
    // Bounded 32K colour lookup; exact common colours get their exact ramp endpoint.
    private val lookup = ByteArray(32768) { key ->
        val r = ((key ushr 10) and 31) * 255 / 31
        val g = ((key ushr 5) and 31) * 255 / 31
        val b = (key and 31) * 255 / 31
        var best = 0; var distance = Int.MAX_VALUE
        for (i in colors.indices) {
            val c = colors[i]; val dr = r - ((c ushr 16) and 255); val dg = g - ((c ushr 8) and 255); val db = b - (c and 255)
            val d = dr * dr + dg * dg + db * db
            if (d < distance) { distance = d; best = i }
        }
        best.toByte()
    }
    fun index(argb: Int): Byte = when (argb) {
        colors[0] -> 0; colors[127] -> 127; colors[255] -> 255.toByte()
        else -> lookup[(((argb ushr 19) and 31) shl 10) or (((argb ushr 11) and 31) shl 5) or ((argb ushr 3) and 31)]
    }
}
