package com.fourierart.app.contours

import com.fourierart.app.core.ContourStroke
import com.fourierart.app.core.Point2
import kotlin.math.*

enum class ContourPreset { DETAILS, SILHOUETTE }
sealed interface ContourEdit {
    data class Erase(val path: List<Point2>, val radius: Double): ContourEdit
    data class Draw(val path: List<Point2>): ContourEdit
    data class Keep(val left: Double, val top: Double, val right: Double, val bottom: Double): ContourEdit
}
/** Commands use photo-normalized coordinates; eraser radius is relative to its shorter side. */
data class ContourRecipe(val preset: ContourPreset=ContourPreset.DETAILS, val cleanup: Int=0,
    val edits: List<ContourEdit> = emptyList()) {
    fun append(edit: ContourEdit)=copy(edits=edits+edit).also {require(it.edits.size<=100)}
}
data class CorrectionHistory(val current: ContourRecipe=ContourRecipe(), val past: List<ContourRecipe> = emptyList(),
    val future: List<ContourRecipe> = emptyList()) {
    fun change(next: ContourRecipe)=if(next==current) this else CorrectionHistory(next,(past+current).takeLast(20))
    fun undo()=if(past.isEmpty()) this else CorrectionHistory(past.last(),past.dropLast(1),listOf(current)+future)
    fun redo()=if(future.isEmpty()) this else CorrectionHistory(future.first(),(past+current).takeLast(20),future.drop(1))
}

object ContourCorrection {
    private const val EPS=1e-8
    private fun mix(a: Point2,b: Point2,t: Double)=Point2(a.x+(b.x-a.x)*t,a.y+(b.y-a.y)*t)
    private fun near(a: Point2,b: Point2)=hypot(a.x-b.x,a.y-b.y)<1e-6
    private fun dot(a: Point2,b: Point2)=a.x*b.x+a.y*b.y
    private fun minus(a: Point2,b: Point2)=Point2(a.x-b.x,a.y-b.y)
    private fun linear(value: Double,delta: Double,lo: Double,hi: Double): Pair<Double,Double>? {
        if(abs(delta)<EPS) return if(value in lo-EPS..hi+EPS) 0.0 to 1.0 else null
        val a=(lo-value)/delta;val b=(hi-value)/delta
        val start=max(0.0,min(a,b));val end=min(1.0,max(a,b))
        return if(end-start>EPS) start to end else null
    }
    private fun circle(a: Point2,b: Point2,c: Point2,r: Double): Pair<Double,Double>? {
        val d=minus(b,a);val q=minus(a,c);val aa=dot(d,d)
        if(aa<EPS) return if(dot(q,q)<=r*r) 0.0 to 1.0 else null
        val bb=2*dot(q,d);val cc=dot(q,q)-r*r;val det=bb*bb-4*aa*cc
        if(det<=0) return null
        val l=max(0.0,(-bb-sqrt(det))/(2*aa));val h=min(1.0,(-bb+sqrt(det))/(2*aa))
        return if(h-l>EPS) l to h else null
    }
    private fun merge(input: List<Pair<Double,Double>>): List<Pair<Double,Double>> {
        val out=ArrayList<Pair<Double,Double>>()
        for(i in input.sortedBy {it.first}) {
            val last=out.lastOrNull()
            if(last!=null && i.first<=last.second+EPS) out[out.lastIndex]=last.first to max(last.second,i.second)
            else out.add(i)
        }
        return out
    }
    private fun capsule(a: Point2,b: Point2,c: Point2,d: Point2,r: Double): List<Pair<Double,Double>> {
        val result=mutableListOf<Pair<Double,Double>>()
        circle(a,b,c,r)?.let {result.add(it)};circle(a,b,d,r)?.let {result.add(it)}
        val v=minus(d,c);val length=hypot(v.x,v.y)
        if(length>EPS) {
            val u=Point2(v.x/length,v.y/length);val n=Point2(-u.y,u.x)
            val q=minus(a,c);val direction=minus(b,a)
            val along=linear(dot(q,u),dot(direction,u),0.0,length)
            val across=linear(dot(q,n),dot(direction,n),-r,r)
            if(along!=null && across!=null) {
                val l=max(along.first,across.first);val h=min(along.second,across.second)
                if(h-l>EPS) result.add(l to h)
            }
        }
        return merge(result)
    }
    private fun pieces(stroke: ContourStroke, intervals: (Point2,Point2)->List<Pair<Double,Double>>): List<ContourStroke> {
        if(stroke.points.size<2) return emptyList()
        val edges=stroke.points.zipWithNext()+if(stroke.closed) listOf(stroke.points.last() to stroke.points.first()) else emptyList()
        val fragments=mutableListOf<MutableList<Point2>>()
        var current: MutableList<Point2>?=null
        var complete=true
        for((a,b) in edges) {
            val kept=intervals(a,b)
            if(kept.size!=1 || abs(kept[0].first)>EPS || abs(kept[0].second-1)>EPS) complete=false
            for((l,h) in kept) {
                val start=mix(a,b,l);val end=mix(a,b,h)
                if(near(start,end)) continue
                val previous=current
                if(previous!=null && near(previous.last(),start)) previous.add(end)
                else {current=mutableListOf(start,end);fragments.add(current)}
            }
            if(kept.isEmpty() || kept.last().second<1-EPS) current=null
        }
        if(complete) return listOf(stroke)
        if(stroke.closed && fragments.size>1 && near(fragments.last().last(),fragments.first().first())) {
            val joined=fragments.removeAt(fragments.lastIndex);joined.addAll(fragments.first().drop(1));fragments[0]=joined
        }
        return fragments.map {ContourStroke(it,false)}.filter {it.length>EPS}
    }
    fun keep(strokes: List<ContourStroke>,left: Double,top: Double,right: Double,bottom: Double)=strokes.flatMap {stroke ->
        pieces(stroke) {a,b ->
            val x=linear(a.x,b.x-a.x,left,right);val y=linear(a.y,b.y-a.y,top,bottom)
            if(x==null || y==null) emptyList() else {
                val l=max(x.first,y.first);val h=min(x.second,y.second)
                if(h-l>EPS) listOf(l to h) else emptyList()
            }
        }
    }
    fun erase(strokes: List<ContourStroke>,path: List<Point2>,radius: Double): List<ContourStroke> {
        require(radius.isFinite() && radius>0 && path.all {it.x.isFinite() && it.y.isFinite()})
        if(path.isEmpty()) return strokes
        val sweep=if(path.size==1) listOf(path[0] to path[0]) else path.zipWithNext()
        return strokes.flatMap {s -> pieces(s) {a,b ->
            val removed=merge(sweep.flatMap {(c,d)->capsule(a,b,c,d,radius)})
            val kept=mutableListOf<Pair<Double,Double>>();var start=0.0
            for((l,h) in removed) {if(l-start>EPS) kept.add(start to l);start=max(start,h)}
            if(1-start>EPS) kept.add(start to 1.0)
            kept
        }}
    }
    fun apply(details: List<ContourStroke>,silhouette: List<ContourStroke>,width: Int,height: Int,recipe: ContourRecipe): List<ContourStroke> {
        require(width>0 && height>0 && recipe.cleanup in 0..100 && recipe.edits.size<=100)
        fun pixel(p: Point2): Point2 {require(p.x.isFinite() && p.y.isFinite());return Point2(p.x.coerceIn(0.0,1.0)*width,p.y.coerceIn(0.0,1.0)*height)}
        var strokes=if(recipe.preset==ContourPreset.SILHOUETTE) silhouette else details
        // Cleanup affects detected lines only; a deliberate corrective mark is never silently removed.
        val minimum=hypot(width.toDouble(),height.toDouble())*recipe.cleanup/2500.0
        strokes=strokes.filter {it.length>=minimum}
        for(edit in recipe.edits) {
            strokes=when(edit) {
                is ContourEdit.Draw -> {
                    require(edit.path.size<=512)
                    val points=edit.path.map(::pixel)
                    if(points.size<2) strokes else strokes+ContourStroke(points,false).takeIf {it.length>1e-6}.let {listOfNotNull(it)}
                }
                is ContourEdit.Erase -> {
                    require(edit.path.size<=512 && edit.radius in 0.001..0.15)
                    erase(strokes,edit.path.map(::pixel),edit.radius*min(width,height))
                }
                is ContourEdit.Keep -> {
                    require(listOf(edit.left,edit.top,edit.right,edit.bottom).all {it.isFinite() && it in 0.0..1.0})
                    require(edit.right>edit.left && edit.bottom>edit.top)
                    keep(strokes,edit.left*width,edit.top*height,edit.right*width,edit.bottom*height)
                }
            }
        }
        return strokes
    }
}
