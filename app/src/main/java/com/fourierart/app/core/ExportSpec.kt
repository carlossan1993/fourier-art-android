package com.fourierart.app.core

enum class ExportFormat(val extension: String, val mime: String) {
    PNG("png", "image/png"), GIF("gif", "image/gif"), MP4("mp4", "video/mp4")
}
enum class ExportFrame { SQUARE, VERTICAL }

/** Explicit output duration is independent of the interactive player's playback speed. */
data class ExportSpec(
    val format: ExportFormat = ExportFormat.MP4,
    val frame: ExportFrame = ExportFrame.SQUARE,
    val seconds: Int = 10,
    val pngSize: Int = 1080
) {
    init { require(seconds == 6 || seconds == 10); require(pngSize == 720 || pngSize == 1080) }
    val width: Int get() = when (format) {
        ExportFormat.PNG -> pngSize
        ExportFormat.GIF -> if (frame == ExportFrame.SQUARE) 480 else 360
        ExportFormat.MP4 -> 720
    }
    val height: Int get() = if (frame == ExportFrame.SQUARE) width else width * 16 / 9
    val fps: Int get() = if (format == ExportFormat.GIF) 12 else 24
    val frames: Int get() = if (format == ExportFormat.PNG) 1 else seconds * fps
    fun progress(index: Int): Double {
        require(index in 0 until frames)
        return if (frames == 1) 1.0 else (index.toDouble() / (frames - 1) / .85).coerceAtMost(1.0)
    }
    fun presentationNanos(index: Int): Long { require(index in 0 until frames); return index * 1_000_000_000L / fps }
    fun gifDelay(index: Int): Int {
        require(index in 0 until frames)
        return ((index + 1) * 100.0 / fps).toInt() - (index * 100.0 / fps).toInt()
    }
}

data class ExportSnapshot(val drawing: ContourDrawing, val components: Int, val style: AnimationStyle,
    val viewport: PreviewViewport = PreviewViewport())
