package com.example.bookapp.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/**
 * پشتیبان‌گیری کامل (JSON، قابل بازیابی) از همه‌ی چیزهایی که کاربر خودش در برنامه
 * ساخته: یادداشت‌ها، علاقه‌مندی‌ها (بوکمارک)، پاورقی‌ها، نقش‌های «من»، و گفتگوها.
 *
 * چون از Storage Access Framework اندروید (ACTION_CREATE_DOCUMENT / ACTION_OPEN_DOCUMENT)
 * استفاده می‌شود، کاربر از همان پنجره‌ی انتخاب مسیر سیستم می‌تواند هم یک سرویس ابری
 * (گوگل‌درایو و مشابه، اگر روی گوشی نصب باشد) و هم حافظه‌ی داخلی/خارجی گوشی (کارت
 * حافظه، پوشه‌ی Downloads) را انتخاب کند - نیازی به اتصال به یک سرویس ابری خاص نیست.
 *
 * نکته: خود فایل‌های تصویر (عکس‌های گالری هر تعزیه) در این نسخه از پشتیبان شامل
 * نمی‌شوند (فقط توضیح/مسیرشان)، چون حجم آن‌ها می‌تواند بزرگ باشد؛ اگر لازم شد
 * می‌توان بعداً پشتیبان‌گیری از خود فایل‌های تصویر را هم اضافه کرد.
 */
suspend fun buildBackupJson(context: Context, db: AppDatabase): String {
    val root = JSONObject()
    root.put("app", "taziehapp")
    root.put("backupVersion", 1)

    val notesArr = JSONArray()
    db.noteDao().getAll().forEach { note ->
        notesArr.put(JSONObject().apply {
            put("title", note.title)
            put("content", note.content)
        })
    }
    root.put("notes", notesArr)

    val bookmarksArr = JSONArray()
    Prefs.getBookmarks(context).forEach { bookmarksArr.put(it) }
    root.put("bookmarks", bookmarksArr)

    val footnotesArr = JSONArray()
    // برای همه‌ی بخش‌ها پاورقی‌ها را جمع می‌کنیم (فقط بخش‌هایی که واقعاً پاورقی دارند)
    db.fieldDao().getAll().forEach { field ->
        db.taziehDao().getByField(field.id).forEach { tazieh ->
            db.roleDao().getByTazieh(tazieh.id).forEach { role ->
                db.sectionDao().getByRole(role.id).forEach { section ->
                    db.footnoteDao().getBySection(section.id).forEach { fn ->
                        footnotesArr.put(JSONObject().apply {
                            put("sectionId", fn.sectionId)
                            put("term", fn.term)
                            put("explanation", fn.explanation)
                        })
                    }
                }
            }
        }
    }
    root.put("footnotes", footnotesArr)

    val myRolesArr = JSONArray()
    Prefs.getAllMyRoles(context).forEach { (taziehId, roleId) ->
        myRolesArr.put(JSONObject().apply {
            put("taziehId", taziehId)
            put("roleId", roleId)
        })
    }
    root.put("myRoles", myRolesArr)

    val dialoguesArr = JSONArray()
    db.fieldDao().getAll().forEach { field ->
        db.taziehDao().getByField(field.id).forEach { tazieh ->
            db.dialogueDao().getByTazieh(tazieh.id).forEach { dialogue ->
                val turnsArr = JSONArray()
                db.dialogueTurnDao().getByDialogue(dialogue.id).forEach { turn ->
                    turnsArr.put(JSONObject().apply {
                        put("sectionId", turn.sectionId)
                        put("orderIndex", turn.orderIndex)
                    })
                }
                dialoguesArr.put(JSONObject().apply {
                    put("taziehId", dialogue.taziehId)
                    put("title", dialogue.title)
                    put("turns", turnsArr)
                })
            }
        }
    }
    root.put("dialogues", dialoguesArr)

    return root.toString(2)
}

/**
 * فایل پشتیبان JSON را در مسیری که کاربر انتخاب کرده (ابری یا حافظه گوشی) می‌نویسد.
 * اگر رمز عبور داده شود، محتوا قبل از نوشتن با AES-GCM رمزگذاری می‌شود (چون
 * فایل پشتیبان شامل یادداشت‌های شخصی کاربر است).
 */
suspend fun writeBackupToUri(context: Context, db: AppDatabase, uri: Uri, password: String? = null) {
    val json = buildBackupJson(context, db)
    val bytes = if (password.isNullOrBlank()) {
        json.toByteArray(Charsets.UTF_8)
    } else {
        encryptBackupText(json, password)
    }
    context.contentResolver.openOutputStream(uri)?.use { out -> out.write(bytes) }
}

/**
 * پشتیبان را از مسیر انتخابی کاربر می‌خواند و همه‌چیز را بازیابی می‌کند.
 * این عملیات افزودنی است (مثل بقیه‌ی برنامه) نه جایگزینی؛ چیزی که همین الان
 * روی گوشی هست پاک نمی‌شود، فقط موارد داخل فایل پشتیبان اضافه/به‌روزرسانی می‌شوند.
 * اگر فایل با رمز ذخیره شده باشد، باید همان رمز را برای بازیابی وارد کنید.
 */
suspend fun restoreBackupFromUri(context: Context, db: AppDatabase, uri: Uri, password: String? = null): Result<Unit> {
    return try {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return Result.failure(IllegalStateException("فایل خوانده نشد"))

        val text = if (password.isNullOrBlank()) {
            String(bytes, Charsets.UTF_8)
        } else {
            decryptBackupBytes(bytes, password)
        }
        val root = JSONObject(text)

        val notesArr = root.optJSONArray("notes") ?: JSONArray()
        for (i in 0 until notesArr.length()) {
            val o = notesArr.getJSONObject(i)
            db.noteDao().insert(
                NoteEntity(title = o.getString("title"), content = o.getString("content"))
            )
        }

        val bookmarksArr = root.optJSONArray("bookmarks") ?: JSONArray()
        for (i in 0 until bookmarksArr.length()) {
            val sectionId = bookmarksArr.getLong(i)
            if (!Prefs.isBookmarked(context, sectionId)) Prefs.toggleBookmark(context, sectionId)
        }

        val footnotesArr = root.optJSONArray("footnotes") ?: JSONArray()
        for (i in 0 until footnotesArr.length()) {
            val o = footnotesArr.getJSONObject(i)
            db.footnoteDao().insert(
                FootnoteEntity(sectionId = o.getLong("sectionId"), term = o.getString("term"), explanation = o.getString("explanation"))
            )
        }

        val myRolesArr = root.optJSONArray("myRoles") ?: JSONArray()
        for (i in 0 until myRolesArr.length()) {
            val o = myRolesArr.getJSONObject(i)
            Prefs.setMyRole(context, o.getLong("taziehId"), o.getLong("roleId"))
        }

        val dialoguesArr = root.optJSONArray("dialogues") ?: JSONArray()
        for (i in 0 until dialoguesArr.length()) {
            val o = dialoguesArr.getJSONObject(i)
            val dialogueId = db.dialogueDao().insert(DialogueEntity(taziehId = o.getLong("taziehId"), title = o.getString("title")))
            val turnsArr = o.getJSONArray("turns")
            for (j in 0 until turnsArr.length()) {
                val t = turnsArr.getJSONObject(j)
                db.dialogueTurnDao().insert(
                    DialogueTurnEntity(dialogueId = dialogueId, sectionId = t.getLong("sectionId"), orderIndex = t.getInt("orderIndex"))
                )
            }
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
