package com.fourierart.app.contours

import com.fourierart.app.core.Point2
import org.json.JSONArray
import org.json.JSONObject

object ContourRecipeJson {
    fun encode(recipe: ContourRecipe): JSONObject {
        val edits=JSONArray()
        for(edit in recipe.edits) {
            val o=JSONObject()
            fun path(points: List<Point2>): JSONArray=JSONArray().apply {for(p in points) put(JSONArray().put(p.x).put(p.y))}
            when(edit) {
                is ContourEdit.Draw -> o.put("type","draw").put("path",path(edit.path))
                is ContourEdit.Erase -> o.put("type","erase").put("path",path(edit.path)).put("radius",edit.radius)
                is ContourEdit.Keep -> o.put("type","keep").put("left",edit.left).put("top",edit.top).put("right",edit.right).put("bottom",edit.bottom)
            }
            edits.put(o)
        }
        return JSONObject().put("version",1).put("preset",recipe.preset.name).put("cleanup",recipe.cleanup).put("edits",edits)
    }
    fun decode(o: JSONObject?): ContourRecipe = try {
        if(o==null) ContourRecipe() else {
            require(o.getInt("version")==1)
            val a=o.getJSONArray("edits");require(a.length()<=100)
            val edits=(0 until a.length()).map {i ->
                val e=a.getJSONObject(i)
                fun path(): List<Point2> {
                    val p=e.getJSONArray("path");require(p.length() in 1..512)
                    return (0 until p.length()).map {j ->p.getJSONArray(j).let {Point2(it.getDouble(0),it.getDouble(1))}}.also {list ->
                        require(list.all {it.x.isFinite() && it.y.isFinite() && it.x in 0.0..1.0 && it.y in 0.0..1.0})
                    }
                }
                when(e.getString("type")) {
                    "draw" -> ContourEdit.Draw(path())
                    "erase" -> ContourEdit.Erase(path(),e.getDouble("radius").also {require(it in 0.001..0.15)})
                    "keep" -> ContourEdit.Keep(e.getDouble("left"),e.getDouble("top"),e.getDouble("right"),e.getDouble("bottom")).also {
                        require(listOf(it.left,it.top,it.right,it.bottom).all {v ->v.isFinite() && v in 0.0..1.0} && it.right>it.left && it.bottom>it.top)
                    }
                    else -> error("Unknown correction")
                }
            }
            ContourRecipe(ContourPreset.valueOf(o.getString("preset")),o.getInt("cleanup").also {require(it in 0..100)},edits)
        }
    } catch(_:Exception) {ContourRecipe()}
}
