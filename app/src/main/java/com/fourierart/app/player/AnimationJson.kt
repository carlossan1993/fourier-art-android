package com.fourierart.app.player

import com.fourierart.app.core.*
import org.json.JSONObject

object AnimationJson {
    data class Snapshot(val style: AnimationStyle=AnimationStyle(),val position: Double=0.0,val playing: Boolean=true,
        val viewport: PreviewViewport=PreviewViewport())
    fun style(s: AnimationStyle)=JSONObject().put("speed",s.speed.name).put("epicircles",s.epicircles)
        .put("color",s.color.name).put("light",s.lightBackground)
    fun readStyle(o: JSONObject?): AnimationStyle {
        if(o==null) return AnimationStyle()
        return AnimationStyle(PlaybackSpeed.entries.firstOrNull {it.name==o.optString("speed")} ?: PlaybackSpeed.NORMAL,
            o.optBoolean("epicircles",true),TraceColor.entries.firstOrNull {it.name==o.optString("color")} ?: TraceColor.INK,o.optBoolean("light",false))
    }
    fun encode(s: Snapshot)=JSONObject().put("style",style(s.style)).put("position",s.position).put("playing",s.playing)
        .put("zoom",s.viewport.zoom).put("panX",s.viewport.panX).put("panY",s.viewport.panY)
    fun decode(o: JSONObject?): Snapshot {
        if(o==null) return Snapshot()
        val position=o.optDouble("position",0.0).takeIf {it.isFinite() && it in 0.0..PlaybackClock.CYCLE} ?: 0.0
        val view=try {PreviewViewport(o.optDouble("zoom",1.0),o.optDouble("panX",0.0),o.optDouble("panY",0.0))} catch(_:IllegalArgumentException) {PreviewViewport()}
        return Snapshot(readStyle(o.optJSONObject("style")),position,o.optBoolean("playing",true),view)
    }
}
