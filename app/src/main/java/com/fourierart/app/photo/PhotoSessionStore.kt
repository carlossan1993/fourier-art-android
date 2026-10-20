package com.fourierart.app.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** One private working photo. No gallery writes, backup or retained collection of photographs. */
class PhotoSessionStore(context: Context, directory: File = File(context.noBackupFilesDir,"working-photo")) {
    private val folder=directory.apply { mkdirs() }
    private val record=AtomicFile(File(folder,"session.json"))
    data class Snapshot(val source: Bitmap, val file: String, val applied: PhotoPreparation?, val draft: PhotoPreparation,
        val editing: Boolean, val options: JSONObject)
    @Synchronized fun read(): Snapshot? = try {
        val json=record.openRead().bufferedReader().use { JSONObject(it.readText()) }
        val name=json.getString("file")
        require(name.matches(Regex("[a-f0-9-]+\\.png")))
        val bitmap=BitmapFactory.decodeFile(File(folder,name).path) ?: error("Missing work photo")
        folder.listFiles()?.filter { it.extension=="png" && it.name!=name }?.forEach { it.delete() }
        Snapshot(bitmap,name,json.optJSONObject("applied")?.let { decode(it) },decode(json.getJSONObject("draft")),
            json.optBoolean("editing",true),json.optJSONObject("options") ?: JSONObject())
    } catch (_:Exception) { null }
    @Synchronized fun saveSource(bitmap: Bitmap): String {
        val name=UUID.randomUUID().toString()+".png"
        val file=File(folder,name)
        file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) }
        return name
    }
    @Synchronized fun write(file: String, applied: PhotoPreparation?, draft: PhotoPreparation, editing: Boolean, options: JSONObject) {
        val json=JSONObject().put("file",file).put("applied",applied?.let { encode(it) })
            .put("draft",encode(draft)).put("editing",editing).put("options",options)
        val stream=record.startWrite()
        try {stream.write(json.toString().toByteArray());record.finishWrite(stream)}
        catch(e:Exception) {record.failWrite(stream);throw e}
        folder.listFiles()?.filter { it.extension=="png" && it.name!=file }?.forEach { it.delete() }
    }
    @Synchronized fun clear() {record.delete();folder.listFiles()?.forEach { it.delete() }}
    private fun encode(p: PhotoPreparation)=JSONObject().put("turns",p.turns).put("left",p.crop.left)
        .put("top",p.crop.top).put("right",p.crop.right).put("bottom",p.crop.bottom)
    private fun decode(j: JSONObject)=PhotoPreparation(j.getInt("turns"),CropBox(j.getDouble("left").toFloat(),
        j.getDouble("top").toFloat(),j.getDouble("right").toFloat(),j.getDouble("bottom").toFloat()))
}
