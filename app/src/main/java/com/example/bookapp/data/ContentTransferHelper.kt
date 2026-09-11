package com.example.bookapp.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Stage 3: safe JSON export/import preview for the content hierarchy. */
data class ContentImportPreview(
    val valid: Boolean,
    val errors: List<String>,
    val fields: Int = 0,
    val taziehs: Int = 0,
    val roles: Int = 0,
    val sections: Int = 0,
    val existingItems: Int = 0,
    val newItems: Int = 0
)

suspend fun exportContentJson(db: AppDatabase): String = withContext(Dispatchers.IO) {
    val root = JSONArray()
    db.fieldDao().getAll().forEach { field ->
        val fieldObj = JSONObject().apply {
            put("title", field.title)
            put("uid", field.uid)
            put("taziehs", JSONArray())
        }
        val taziehArray = fieldObj.getJSONArray("taziehs")
        db.taziehDao().getByField(field.id).forEach { tazieh ->
            val taziehObj = JSONObject().apply {
                put("title", tazieh.title)
                put("uid", tazieh.uid)
                tazieh.author?.let { put("author", it) }
                tazieh.authorEmail?.let { put("authorEmail", it) }
                put("roles", JSONArray())
            }
            val roleArray = taziehObj.getJSONArray("roles")
            db.roleDao().getByTazieh(tazieh.id).forEach { role ->
                val roleObj = JSONObject().apply {
                    put("title", role.title)
                    put("uid", role.uid)
                    put("sections", JSONArray())
                }
                val sectionArray = roleObj.getJSONArray("sections")
                db.sectionDao().getByRole(role.id).forEach { section ->
                    sectionArray.put(JSONObject().apply {
                        put("title", section.title)
                        put("content", section.content)
                        section.audioUrl?.let { put("audio", it) }
                        put("uid", section.uid)
                        put("sourceUid", section.sourceUid)
                    })
                }
                roleArray.put(roleObj)
            }
            taziehArray.put(taziehObj)
        }
        root.put(fieldObj)
    }
    root.toString(2)
}

suspend fun previewContentImport(db: AppDatabase, jsonText: String): ContentImportPreview = withContext(Dispatchers.IO) {
    val errors = ContentValidator.validate(jsonText)
    if (errors.isNotEmpty()) return@withContext ContentImportPreview(false, errors.take(20))

    val fields = JSONArray(jsonText)
    var fieldCount = 0
    var taziehCount = 0
    var roleCount = 0
    var sectionCount = 0
    var existing = 0

    for (fi in 0 until fields.length()) {
        val f = fields.getJSONObject(fi)
        fieldCount++
        if (db.fieldDao().getByUid(f.getString("uid")) != null) existing++
        val taziehs = f.getJSONArray("taziehs")
        for (ti in 0 until taziehs.length()) {
            val t = taziehs.getJSONObject(ti)
            taziehCount++
            if (db.taziehDao().getByUid(t.getString("uid")) != null) existing++
            val roles = t.getJSONArray("roles")
            for (ri in 0 until roles.length()) {
                val r = roles.getJSONObject(ri)
                roleCount++
                if (db.roleDao().getByUid(r.getString("uid")) != null) existing++
                val sections = r.getJSONArray("sections")
                for (si in 0 until sections.length()) {
                    val s = sections.getJSONObject(si)
                    sectionCount++
                    if (db.sectionDao().getByUid(s.getString("uid")) != null) existing++
                }
            }
        }
    }
    ContentImportPreview(
        valid = true,
        errors = emptyList(),
        fields = fieldCount,
        taziehs = taziehCount,
        roles = roleCount,
        sections = sectionCount,
        existingItems = existing,
        newItems = fieldCount + taziehCount + roleCount + sectionCount - existing
    )
}

suspend fun importContentJson(db: AppDatabase, jsonText: String): ContentImportPreview {
    val preview = previewContentImport(db, jsonText)
    if (!preview.valid) return preview
    mergeContentFromJson(db, jsonText, ContentUid.source("manual-import"))
    return preview
}

suspend fun readJsonFromUri(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8).use { reader ->
        reader?.readText() ?: throw IllegalArgumentException("امکان خواندن فایل وجود ندارد")
    }
}
