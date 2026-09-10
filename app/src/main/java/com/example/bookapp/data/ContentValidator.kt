package com.example.bookapp.data

import org.json.JSONArray
import org.json.JSONObject

/** Strict validation for content files before they are imported. */
object ContentValidator {
    fun validate(text: String): List<String> {
        val errors = mutableListOf<String>()
        val root = try { JSONObject(text) } catch (_: Exception) { null }
        val fields = if (root != null) root.optJSONArray("fields") else try { JSONArray(text) } catch (_: Exception) { null }
        if (fields == null) {
            return listOf("ریشه فایل باید آرایه JSON یا شیء دارای fields باشد")
        }
        val fieldUids = mutableSetOf<String>()
        val taziehUids = mutableSetOf<String>()
        val roleUids = mutableSetOf<String>()
        val sectionUids = mutableSetOf<String>()
        for (fi in 0 until fields.length()) {
            val f = fields.optJSONObject(fi) ?: run { errors += "Field شماره $fi معتبر نیست"; continue }
            requireUid(f, "Field", fi, fieldUids, errors)
            if (f.optString("title").isBlank()) errors += "Field شماره $fi عنوان ندارد"
            val taziehs = f.optJSONArray("taziehs") ?: run { errors += "Field شماره $fi آرایه taziehs ندارد"; continue }
            for (ti in 0 until taziehs.length()) {
                val t = taziehs.optJSONObject(ti) ?: run { errors += "Tazieh $fi/$ti معتبر نیست"; continue }
                requireUid(t, "Tazieh", ti, taziehUids, errors)
                val roles = t.optJSONArray("roles") ?: run { errors += "Tazieh $fi/$ti آرایه roles ندارد"; continue }
                for (ri in 0 until roles.length()) {
                    val r = roles.optJSONObject(ri) ?: run { errors += "Role $fi/$ti/$ri معتبر نیست"; continue }
                    requireUid(r, "Role", ri, roleUids, errors)
                    val sections = r.optJSONArray("sections") ?: run { errors += "Role $fi/$ti/$ri آرایه sections ندارد"; continue }
                    for (si in 0 until sections.length()) {
                        val s = sections.optJSONObject(si) ?: run { errors += "Section $fi/$ti/$ri/$si معتبر نیست"; continue }
                        requireUid(s, "Section", si, sectionUids, errors)
                        if (!s.has("content")) errors += "Section ${s.optString("uid")} فیلد content ندارد"
                    }
                }
            }
        }
        return errors
    }

    private fun requireUid(obj: JSONObject, kind: String, index: Int, seen: MutableSet<String>, errors: MutableList<String>) {
        val uid = obj.optString("uid").trim()
        if (uid.isBlank()) errors += "$kind شماره $index فاقد uid است"
        else if (!seen.add(uid)) errors += "$kind دارای uid تکراری است: $uid"
    }
}
