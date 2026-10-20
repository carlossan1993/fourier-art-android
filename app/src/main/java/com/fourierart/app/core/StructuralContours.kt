package com.fourierart.app.core

import kotlin.math.*

enum class LineStyle { DETAIL, STRUCTURE }

/** Color-aware boundaries at two scales. No pet landmarks or invented anatomical geometry. */
object StructuralContours {
    data class Field(val edges: BooleanArray, val strength: FloatArray)

    /** Bounded separable bilateral approximation with joint RGB range weights. */
    fun smooth(pixels: IntArray, width: Int, height: Int): IntArray {
        require(pixels.size == width * height && width > 0 && height > 0)
        var rgb = Array(3) { channel -> FloatArray(pixels.size) { i ->
            val c = pixels[i]; val a = (c ushr 24) / 255f
            ((c ushr (16 - channel * 8)) and 255) * a + 255 * (1 - a)
        } }
        val spatial = DoubleArray(4) { exp(-it * it / 8.0) }
        val range = DoubleArray(195076) { exp(-it / (2.0 * 32 * 32 * 3)) }
        repeat(2) {
            for (vertical in listOf(false, true)) {
                val source = rgb
                rgb = Array(3) { FloatArray(pixels.size) }
                for (y in 0 until height) for (x in 0 until width) {
                    val i = y * width + x
                    var total = 0.0; var r = 0.0; var g = 0.0; var b = 0.0
                    for (d in -3..3) {
                        val xx = if (vertical) x else (x + d).coerceIn(0, width - 1)
                        val yy = if (vertical) (y + d).coerceIn(0, height - 1) else y
                        val j = yy * width + xx
                        val dr = source[0][i] - source[0][j]; val dg = source[1][i] - source[1][j]; val db = source[2][i] - source[2][j]
                        val weight = spatial[abs(d)] * range[(dr*dr + dg*dg + db*db).toInt().coerceIn(0, range.lastIndex)]
                        total += weight; r += source[0][j]*weight; g += source[1][j]*weight; b += source[2][j]*weight
                    }
                    rgb[0][i] = (r/total).toFloat(); rgb[1][i] = (g/total).toFloat(); rgb[2][i] = (b/total).toFloat()
                }
            }
        }
        return IntArray(pixels.size) { i -> 0xff000000.toInt() or
            (rgb[0][i].roundToInt().coerceIn(0,255) shl 16) or
            (rgb[1][i].roundToInt().coerceIn(0,255) shl 8) or rgb[2][i].roundToInt().coerceIn(0,255) }
    }

    /** Dominant eigenvector of the RGB gradient tensor; catches equal-luminance color boundaries. */
    fun field(pixels: IntArray, width: Int, height: Int, sensitivity: Int, allowed: BooleanArray? = null): Field {
        require(pixels.size == width*height && (allowed == null || allowed.size == pixels.size))
        val channels = Array(3) { ch -> DoubleArray(pixels.size) { i ->
            val c = pixels[i]; val alpha = (c ushr 24)/255.0
            ((c ushr (16 - ch*8)) and 255)*alpha + 255*(1-alpha)
        } }
        val magnitude = FloatArray(pixels.size); val direction = IntArray(pixels.size)
        for (y in 1 until height-1) for (x in 1 until width-1) {
            val i = y*width+x
            var xx = 0.0; var xy = 0.0; var yy = 0.0
            for (c in channels) {
                val gx = -c[i-width-1]+c[i-width+1]-2*c[i-1]+2*c[i+1]-c[i+width-1]+c[i+width+1]
                val gy = -c[i-width-1]-2*c[i-width]-c[i-width+1]+c[i+width-1]+2*c[i+width]+c[i+width+1]
                xx += gx*gx; xy += gx*gy; yy += gy*gy
            }
            magnitude[i] = sqrt(max(0.0,(xx+yy+sqrt((xx-yy)*(xx-yy)+4*xy*xy))/6)).toFloat()
            val angle = 0.5*atan2(2*xy,xx-yy)
            direction[i] = (((angle*180/PI+180)%180+22.5).toInt()/45)%4
        }
        val thin = FloatArray(pixels.size)
        for (y in 2 until height-2) for (x in 2 until width-2) {
            val i = y*width+x
            if (allowed != null && (-2..2).any { dy -> (-2..2).any { dx -> !allowed[i+dy*width+dx] } }) continue
            val step = when(direction[i]) {0 -> 1;1 -> width+1;2 -> width;else -> width-1}
            if(magnitude[i]>=magnitude[i-step] && magnitude[i]>magnitude[i+step]) thin[i]=magnitude[i]
        }
        val high = (150-sensitivity.coerceIn(0,100)*0.95).toFloat()
        val output = BooleanArray(pixels.size); val queue = IntArray(pixels.size)
        var end = 0; var read = 0
        for(i in thin.indices) if(thin[i]>=high) {output[i]=true;queue[end++]=i}
        while(read<end) {
            val i=queue[read++];val x=i%width;val y=i/width
            for(dy in -1..1) for(dx in -1..1) {
                if(x+dx !in 1 until width-1 || y+dy !in 1 until height-1) continue
                val j=i+dy*width+dx
                if(!output[j] && thin[j]>=high*0.35) {output[j]=true;queue[end++]=j}
            }
        }
        return Field(output,thin)
    }

    fun extract(pixels: IntArray, width: Int, height: Int, sensitivity: Int,
        allowed: BooleanArray?, level: DetailLevel): List<ContourStroke> {
        val filtered = smooth(pixels,width,height)
        val fine = field(filtered,width,height,sensitivity,allowed)
        val cw = (width+1)/2;val ch = (height+1)/2
        val coarsePixels = IntArray(cw*ch) { i ->
            val x=i%cw*2;val y=i/cw*2
            var r=0;var g=0;var b=0;var n=0
            for(dy in 0..1) for(dx in 0..1) if(x+dx<width && y+dy<height) {
                val c=filtered[(y+dy)*width+x+dx];r+=(c ushr 16) and 255;g+=(c ushr 8) and 255;b+=c and 255;n++
            }
            0xff000000.toInt() or ((r/n) shl 16) or ((g/n) shl 8) or (b/n)
        }
        val coarseMask = allowed?.let { m -> BooleanArray(cw*ch) { i ->
            val x=i%cw*2;val y=i/cw*2
            (0..1).all { dy -> (0..1).all { dx -> x+dx>=width || y+dy>=height || m[(y+dy)*width+x+dx] } }
        } }
        val coarseField = field(coarsePixels,cw,ch,sensitivity,coarseMask)
        val coarse = RasterContours.lineStrokes(coarseField.edges,cw,ch,2.5,true).map { s ->
            ContourStroke(s.points.map { Point2(min(width-1.0,it.x*2+0.5),min(height-1.0,it.y*2+0.5)) },s.closed)
        }
        val structural = BooleanArray(pixels.size)
        for(s in coarse) for(p in s.points) structural[p.y.toInt()*width+p.x.toInt()] = true
        val joined = StrokeRefinement.join(StrokeRefinement.select(coarse,width,height,1400),max(2.0,max(width,height)*0.004))
        val anchors = darkRegions(filtered,width,height,allowed)
        val anchorMap = BooleanArray(pixels.size)
        for(s in anchors) {
            mark(s,anchorMap,width,height,2)

        }
        val primary = anchors + DetailSelector.select(joined.flatMap { subtract(it,anchorMap,width,height) },pixels,width,height,structural,
            max(0,level.budget*3/4-anchors.size))
        val occupied = BooleanArray(pixels.size)
        for(i in occupied.indices) occupied[i]=anchorMap[i]
        for(s in primary) mark(s,occupied,width,height,2)
        val extra = RasterContours.lineStrokes(fine.edges,width,height,2.5,true)
            .flatMap { subtract(it,occupied,width,height) }
        val secondary = DetailSelector.select(StrokeRefinement.join(StrokeRefinement.select(extra,width,height,1200),2.0),
            pixels,width,height,fine.edges,level.budget-primary.size)
        return (primary+secondary).map { StrokeRefinement.smooth(StrokeRefinement.simplify(it,0.9)) }
    }

    /** Observed compact dark regions provide closed boundaries even where edge chains fragment. */
    fun darkRegions(pixels: IntArray, width: Int, height: Int, allowed: BooleanArray?): List<ContourStroke> {
        val gray=IntArray(pixels.size) { i -> val c=pixels[i]
            (((c ushr 16) and 255)*299+((c ushr 8) and 255)*587+(c and 255)*114)/1000 }
        val histogram=IntArray(256);var total=0
        for(i in gray.indices) if(allowed==null || allowed[i]) {histogram[gray[i]]++;total++}
        if(total==0) return emptyList()
        fun quantile(fraction: Double): Int {var n=0;for(i in histogram.indices) {n+=histogram[i];if(n>=max(1,(total*fraction).toInt())) return i};return 255}
        val low=quantile(0.01);val median=quantile(0.5)
        if(median-low<22) return emptyList()
        val threshold=(quantile(0.01)+8.0).coerceIn(22.0,55.0)
        val dark=BooleanArray(pixels.size) { i -> gray[i]<threshold && (allowed==null || allowed[i]) }
        val radius=max(1,(max(width,height)*0.0025).roundToInt())
        val core=BooleanArray(pixels.size)
        for(y in radius until height-radius) for(x in radius until width-radius) {
            val i=y*width+x
            if(!dark[i]) continue
            core[i]=(-radius..radius).all { dy -> (-radius..radius).all { dx ->
                dx*dx+dy*dy>radius*radius || dark[i+dy*width+dx] } }
        }
        val opened=BooleanArray(pixels.size)
        for(i in core.indices) if(core[i]) {
            val x=i%width;val y=i/width
            for(dy in -radius..radius) for(dx in -radius..radius) if(dx*dx+dy*dy<=radius*radius &&
                x+dx in 0 until width && y+dy in 0 until height) {
                val j=i+dy*width+dx;if(dark[j]) opened[j]=true
            }
        }
        return RasterContours.outlines(opened,width,height).filter { s ->
            val area=abs(RasterContours.area(s.points))
            val w=s.points.maxOf { it.x }-s.points.minOf { it.x };val h=s.points.maxOf { it.y }-s.points.minOf { it.y }
            area in max(12.0,pixels.size*0.00006)..(pixels.size*0.025) &&
                min(w,h)>=3 && max(w,h)/min(w,h)<=4 && area/max(1.0,s.length*s.length)>0.025 &&
                s.points.none { it.x<=2 || it.y<=2 || it.x>=width-2 || it.y>=height-2 }
        }.take(16)
    }

    private fun mark(s: ContourStroke, map: BooleanArray, width: Int, height: Int, radius: Int) {
        fun dot(x: Int,y: Int) { for(dy in -radius..radius) for(dx in -radius..radius)
            if(x+dx in 0 until width && y+dy in 0 until height) map[(y+dy)*width+x+dx]=true }
        val p = if(s.closed) s.points+s.points.first() else s.points
        for((a,b) in p.zipWithNext()) {
            val n=max(1,ceil(StrokeRefinement.distance(a,b)).toInt())
            for(i in 0..n) dot((a.x+(b.x-a.x)*i/n).roundToInt(),(a.y+(b.y-a.y)*i/n).roundToInt())
        }
    }

    private fun subtract(s: ContourStroke, occupied: BooleanArray, width: Int, height: Int): List<ContourStroke> {
        fun hit(p: Point2) = occupied[p.y.roundToInt().coerceIn(0,height-1)*width+p.x.roundToInt().coerceIn(0,width-1)]
        val cut=s.points.indexOfFirst { hit(it) }
        if(cut<0) return listOf(s)
        val p=if(s.closed) s.points.drop(cut)+s.points.take(cut) else s.points
        val result=ArrayList<ContourStroke>();var run=ArrayList<Point2>()
        for(q in p) {if(!hit(q)) run.add(q) else {if(run.size>=3) result.add(ContourStroke(run,false));run=ArrayList()} }
        if(run.size>=3) result.add(ContourStroke(run,false))
        return result
    }
}
