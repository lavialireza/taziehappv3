package com.example.bookapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

@Database(
    entities = [
        FieldEntity::class,
        TaziehEntity::class,
        RoleEntity::class,
        SectionEntity::class,
        NoteEntity::class,
        FootnoteEntity::class,
        DialogueEntity::class,
        DialogueTurnEntity::class,
        TaziehImageEntity::class,
        SectionFts::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fieldDao(): FieldDao
    abstract fun taziehDao(): TaziehDao
    abstract fun roleDao(): RoleDao
    abstract fun sectionDao(): SectionDao
    abstract fun searchDao(): SearchDao
    abstract fun sectionFtsDao(): SectionFtsDao
    abstract fun noteDao(): NoteDao
    abstract fun footnoteDao(): FootnoteDao
    abstract fun dialogueDao(): DialogueDao
    abstract fun dialogueTurnDao(): DialogueTurnDao
    abstract fun taziehImageDao(): TaziehImageDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /**
         * از نسخه ۵ به ۶: فقط دو جدول تازه (گفتگو و نوبت‌های گفتگو) اضافه می‌شود؛
         * هیچ جدول موجودی تغییر نمی‌کند. چون این تغییر را خودمان نوشتیم و ساختار
         * دقیقش را مطمئنیم، برخلاف گذارهای قبلی، اینجا از Migration واقعی استفاده
         * می‌کنیم تا داده‌ی کاربر (بوکمارک، یادداشت، پاورقی، نقش من) پاک نشود.
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dialogues` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `taziehId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        FOREIGN KEY(`taziehId`) REFERENCES `taziehs`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_dialogues_taziehId` ON `dialogues` (`taziehId`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dialogue_turns` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `dialogueId` INTEGER NOT NULL,
                        `sectionId` INTEGER NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        FOREIGN KEY(`dialogueId`) REFERENCES `dialogues`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`sectionId`) REFERENCES `sections`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_dialogue_turns_dialogueId` ON `dialogue_turns` (`dialogueId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_dialogue_turns_sectionId` ON `dialogue_turns` (`sectionId`)")
            }
        }

        /** از نسخه ۶ به ۷: فقط جدول تصاویر تعزیه اضافه می‌شود. */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tazieh_images` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `taziehId` INTEGER NOT NULL,
                        `filePath` TEXT NOT NULL,
                        `caption` TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(`taziehId`) REFERENCES `taziehs`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tazieh_images_taziehId` ON `tazieh_images` (`taziehId`)")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `taziehs` ADD COLUMN `author` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `taziehs` ADD COLUMN `authorEmail` TEXT DEFAULT NULL")
            }
        }


        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `fields` ADD COLUMN `stableKey` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `taziehs` ADD COLUMN `stableKey` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `roles` ADD COLUMN `stableKey` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `sections` ADD COLUMN `stableKey` TEXT NOT NULL DEFAULT ''")

                // برای داده‌های قدیمی یک کلید پایدار بر اساس مسیر متنی می‌سازیم؛
                // داده‌های جدید در صورت وجود id صریح، از همان id استفاده خواهند کرد.
                db.execSQL("UPDATE fields SET stableKey = 'field:' || title WHERE stableKey = ''")
                db.execSQL("""UPDATE taziehs SET stableKey = 'tazieh:' ||
                    'field:' || (SELECT title FROM fields WHERE fields.id = taziehs.fieldId) || ':' || title
                    WHERE stableKey = ''""")
                db.execSQL("""UPDATE roles SET stableKey = 'role:tazieh:' ||
                    (SELECT 'field:' || f.title || ':' || t.title FROM taziehs t JOIN fields f ON f.id = t.fieldId WHERE t.id = roles.taziehId) || ':' || title
                    WHERE stableKey = ''""")
                db.execSQL("""UPDATE sections SET stableKey = 'section:role:tazieh:' ||
                    (SELECT 'field:' || f.title || ':' || t.title || ':' || r.title || ':' || sections.title
                     FROM roles r JOIN taziehs t ON t.id = r.taziehId JOIN fields f ON f.id = t.fieldId
                     WHERE r.id = sections.roleId)
                    WHERE stableKey = ''""")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fields_stableKey` ON `fields` (`stableKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_taziehs_stableKey` ON `taziehs` (`stableKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_roles_stableKey` ON `roles` (`stableKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sections_stableKey` ON `sections` (`stableKey`)")

                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `sections_fts` USING fts4(`sectionId`, `title`, `content`, tokenize=unicode61)")
                db.execSQL("INSERT INTO sections_fts(sectionId, title, content) SELECT id, title, content FROM sections")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bookapp.db"
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    // برای گذارهای قبل از نسخه ۵ که مطمئن نیستیم schema دقیقشان چه بوده،
                    // همچنان بازسازی خودکار انجام می‌شود.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * فایل‌های JSON پوشه‌ی assets/content را که هنوز پردازش نشده‌اند (بر اساس نام فایل)
 * پیدا کرده و محتوای هرکدام را به دیتابیس اضافه می‌کند، بدون پاک‌کردن محتوای قبلی.
 * به این ترتیب برای افزودن یک مجلس تعزیه‌ی جدید، کافی است یک فایل JSON تازه در
 * assets/content قرار بگیرد؛ بقیه‌ی محتوا دست‌نخورده باقی می‌ماند و فقط فایل جدید
 * به ترتیب (بر اساس نام فایل) به انتهای نقش‌های موجود اضافه یا نقش/تعزیه/زمینه‌ی
 * تازه ساخته می‌شود.
 */
/**
 * فایل‌های JSON پوشه‌ی assets/content را که هنوز پردازش نشده‌اند (بر اساس نام فایل)
 * به دیتابیس اضافه می‌کند و شمار فایل‌های تازه‌ی اضافه‌شده را برمی‌گرداند (۰ یعنی
 * چیز تازه‌ای نبود). اگر این اولین اجرای برنامه نباشد (یعنی قبلاً محتوایی داشته)
 * و چیزی واقعاً تازه اضافه شده باشد، یک اعلان محلی هم نشان داده می‌شود.
 */
suspend fun syncLocalContentFiles(context: Context, db: AppDatabase): Int {
    val allFileNames = context.assets.list("content")?.filter { it.endsWith(".json") }?.sorted() ?: emptyList()
    // کلید ردیابی «اسم‌فایل:هش‌محتوا» است نه فقط اسم فایل، تا اگر محتوای همان فایل
    // (برای اصلاح یک جمله یا افزودن مشخصات نویسنده) تغییر کند، دوباره پردازش شود
    // نه اینکه چون اسمش را قبلاً دیده نادیده گرفته شود.
    val allFiles = allFileNames.map { name ->
        val text = context.assets.open("content/$name").bufferedReader(Charsets.UTF_8).use { it.readText() }
        FileWithKey(name, text, "$name:${contentKey(text)}")
    }

    var processed = Prefs.getProcessedContentFiles(context)
    val hadContentBefore = db.fieldDao().getAll().isNotEmpty()

    // اگر دیتابیس به هر دلیلی (مثلاً migration مخرب بین نسخه‌ها) خالی شده باشد
    // ولی Prefs هنوز فایل‌ها را «پردازش‌شده» می‌داند، باید همه چیز دوباره بارگذاری شود.
    if (!hadContentBefore && processed.isNotEmpty()) {
        processed = emptySet()
    }

    val newOrChangedFiles = allFiles.filter { it.key !in processed }
    if (newOrChangedFiles.isEmpty()) return 0

    for (file in newOrChangedFiles) {
        mergeContentFromJson(db, file.text)
    }
    // کل فهرست فعلی (با هشِ فعلیِ هرکدام) را به‌عنوان پردازش‌شده ثبت می‌کنیم؛ کلیدهای
    // قدیمیِ همان فایل‌ها (با هش قبلی) به‌طور طبیعی دیگر استفاده نمی‌شوند.
    Prefs.setProcessedContentFiles(context, allFiles.map { it.key }.toSet())

    // فقط وقتی اعلان نشان بده که این اولین نصب نبوده (کاربر قبلاً محتوا داشته)
    // تا کاربرِ تازه‌نصب‌کرده با یک اعلان اضافه و بی‌مورد مواجه نشود
    if (hadContentBefore) {
        showNewContentNotification(context, newOrChangedFiles.size)
    }

    return newOrChangedFiles.size
}

private data class FileWithKey(val name: String, val text: String, val key: String)

/**
 * محتوای فعلی (زمینه/تعزیه/نقش/بخش) را کامل پاک کرده و از یک متن JSON
 * که از اینترنت دریافت شده، دوباره می‌سازد. برای «بروزرسانی محتوا بدون
 * نیاز به ساخت نسخه جدید اپ» استفاده می‌شود.
 * آدرس پیش‌فرض: فایل sample_data.json روی گیت‌هاب (شاخه main).
 */
suspend fun syncRemoteContent(
    db: AppDatabase,
    url: String = "https://raw.githubusercontent.com/lavialireza/tazeahappv-1/main/app/src/main/assets/content/001_sample.json"
): Result<Unit> {
    return try {
        val jsonText = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            withHttpGet(url)
        }
        mergeContentFromJson(db, jsonText)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private fun withHttpGet(urlString: String): String {
    val connection = URL(urlString).openConnection() as HttpURLConnection
    connection.connectTimeout = 15000
    connection.readTimeout = 15000
    connection.requestMethod = "GET"
    return try {
        connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

internal suspend fun mergeContentFromJson(db: AppDatabase, jsonText: String) {
    val fields = JSONArray(jsonText)
    db.withTransaction {
        for (fi in 0 until fields.length()) {
            val fieldObj = fields.getJSONObject(fi)
            val fieldTitle = fieldObj.getString("title")
            val fieldKey = fieldObj.optString("id", "").ifBlank { contentKey("field:$fieldTitle") }
            val field = db.fieldDao().getByStableKey(fieldKey)
                ?: db.fieldDao().getByTitle(fieldTitle)
            val fieldId = if (field != null) {
                if (field.title != fieldTitle) db.fieldDao().updateTitle(field.id, fieldTitle)
                db.fieldDao().updateStableKey(field.id, fieldKey)
                field.id
            } else db.fieldDao().insert(FieldEntity(title = fieldTitle, stableKey = fieldKey))

            val taziehs = fieldObj.getJSONArray("taziehs")
            for (ti in 0 until taziehs.length()) {
                val taziehObj = taziehs.getJSONObject(ti)
                val taziehTitle = taziehObj.getString("title")
                val taziehKey = taziehObj.optString("id", "").ifBlank { contentKey("tazieh:$fieldKey:$taziehTitle") }
                val existingTazieh = db.taziehDao().getByStableKey(taziehKey)
                    ?: db.taziehDao().getByTitle(fieldId, taziehTitle)
                val taziehId = if (existingTazieh != null) existingTazieh.id
                else db.taziehDao().insert(TaziehEntity(fieldId = fieldId, title = taziehTitle, stableKey = taziehKey))
                val author = taziehObj.optString("author", "").ifBlank { existingTazieh?.author }
                val authorEmail = taziehObj.optString("authorEmail", "").ifBlank { existingTazieh?.authorEmail }
                db.taziehDao().updateMetadata(taziehId, taziehKey, taziehTitle, author, authorEmail)

                val complete = taziehObj.optBoolean("complete", false)
                val roles = taziehObj.getJSONArray("roles")
                val seenRoleKeys = mutableSetOf<String>()
                for (ri in 0 until roles.length()) {
                    val roleObj = roles.getJSONObject(ri)
                    val roleTitle = roleObj.getString("title")
                    val roleKey = roleObj.optString("id", "").ifBlank { contentKey("role:$taziehKey:$roleTitle") }
                    seenRoleKeys += roleKey
                    val existingRole = db.roleDao().getByStableKey(roleKey)
                        ?: db.roleDao().getByTitle(taziehId, roleTitle)
                    val roleId = if (existingRole != null) {
                        db.roleDao().updateTitle(existingRole.id, roleTitle)
                        db.roleDao().updateStableKey(existingRole.id, roleKey)
                        db.roleDao().updateOrderIndex(existingRole.id, ri)
                        existingRole.id
                    } else db.roleDao().insert(RoleEntity(taziehId = taziehId, title = roleTitle, orderIndex = ri, stableKey = roleKey))

                    val sections = roleObj.getJSONArray("sections")
                    val seenSectionKeys = mutableSetOf<String>()
                    for (si in 0 until sections.length()) {
                        val secObj = sections.getJSONObject(si)
                        val sectionTitle = secObj.getString("title")
                        val newContent = secObj.getString("content")
                        val hasAudioField = secObj.has("audio")
                        val newAudio = if (hasAudioField) secObj.optString("audio", "").ifBlank { null } else null
                        val sectionKey = secObj.optString("id", "").ifBlank { contentKey("section:$roleKey:$sectionTitle") }
                        seenSectionKeys += sectionKey
                        val existingSection = db.sectionDao().getByStableKey(sectionKey)
                            ?: db.sectionDao().getByTitle(roleId, sectionTitle)
                        if (existingSection != null) {
                            db.sectionDao().updateStableKey(existingSection.id, sectionKey)
                            db.sectionDao().updateFromContent(existingSection.id, sectionTitle, si, newContent, if (hasAudioField) newAudio else existingSection.audioUrl)
                            db.sectionFtsDao().deleteBySection(existingSection.id)
                            db.sectionFtsDao().insert(SectionFts(existingSection.id, normalizePersian(sectionTitle), normalizePersian(newContent)))
                        } else {
                            val newId = db.sectionDao().insert(SectionEntity(roleId, si, sectionTitle, newContent, newAudio, sectionKey))
                            db.sectionFtsDao().insert(SectionFts(newId, normalizePersian(sectionTitle), normalizePersian(newContent)))
                        }
                    }
                    if (complete) {
                        db.sectionDao().getByRole(roleId).filter { it.stableKey !in seenSectionKeys }.forEach {
                            db.sectionDao().delete(it.id)
                            db.sectionFtsDao().deleteBySection(it.id)
                        }
                    }
                }
                if (complete) {
                    db.roleDao().getByTazieh(taziehId).filter { it.stableKey !in seenRoleKeys }.forEach { roleToDelete ->
                        db.sectionDao().getByRole(roleToDelete.id).forEach { db.sectionFtsDao().deleteBySection(it.id) }
                        db.roleDao().delete(roleToDelete.id)
                    }
                }
            }
        }
    }
}
