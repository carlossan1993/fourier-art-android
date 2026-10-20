package com.fourierart.app.photo

import kotlin.math.*

/** Coordinates refer to the oriented image after manual quarter-turns. */
data class CropBox(val left: Float = 0f, val top: Float = 0f, val right: Float = 1f, val bottom: Float = 1f) {
    init { require(listOf(left,top,right,bottom).all { it.isFinite() } && left>=0 && top>=0 && right<=1 && bottom<=1 && right-left>=0.049f && bottom-top>=0.049f) }
    fun clockwise() = CropBox(1-bottom,left,1-top,right)
    fun moved(dx: Float, dy: Float): CropBox {
        val x=dx.coerceIn(-left,1-right);val y=dy.coerceIn(-top,1-bottom)
        return CropBox(left+x,top+y,right+x,bottom+y)
    }
    fun corner(index: Int, x: Float, y: Float): CropBox = when(index) {
        0 -> copy(left=x.coerceIn(0f,max(0f,right-0.05f)),top=y.coerceIn(0f,max(0f,bottom-0.05f)))
        1 -> copy(right=x.coerceIn(min(1f,left+0.05f),1f),top=y.coerceIn(0f,max(0f,bottom-0.05f)))
        2 -> copy(right=x.coerceIn(min(1f,left+0.05f),1f),bottom=y.coerceIn(min(1f,top+0.05f),1f))
        else -> copy(left=x.coerceIn(0f,max(0f,right-0.05f)),bottom=y.coerceIn(min(1f,top+0.05f),1f))
    }
    fun pixels(width: Int, height: Int): IntArray {
        require(width>0 && height>0)
        val x=floor(left*width).toInt().coerceIn(0,width-1);val y=floor(top*height).toInt().coerceIn(0,height-1)
        val r=ceil(right*width).toInt().coerceIn(x+1,width);val b=ceil(bottom*height).toInt().coerceIn(y+1,height)
        return intArrayOf(x,y,r-x,b-y)
    }
}
data class PhotoPreparation(val turns: Int = 0, val crop: CropBox = CropBox()) {
    init { require(turns in 0..3) }
    fun clockwise() = PhotoPreparation((turns+1)%4,crop.clockwise())
    fun counterclockwise() = clockwise().clockwise().clockwise()
}
