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
        TaziehImageEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fieldDao(): FieldDao
    abstract fun taziehDao(): TaziehDao
    abstract fun roleDao(): RoleDao
    abstract fun sectionDao(): SectionDao
    abstract fun searchDao(): SearchDao
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

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bookapp.db"
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
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
        FileWithKey(name, text, "$name:${text.hashCode()}")
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
            val fieldId = db.fieldDao().getByTitle(fieldTitle)?.id
                ?: db.fieldDao().insert(FieldEntity(title = fieldTitle))

            val taziehs = fieldObj.getJSONArray("taziehs")
            for (ti in 0 until taziehs.length()) {
                val taziehObj = taziehs.getJSONObject(ti)
                val taziehTitle = taziehObj.getString("title")
                val existingTazieh = db.taziehDao().getByTitle(fieldId, taziehTitle)
                val taziehId = existingTazieh?.id
                    ?: db.taziehDao().insert(TaziehEntity(fieldId = fieldId, title = taziehTitle))

                // اگر مشخصات نویسنده/ایمیل در فایل JSON آمده باشد، همیشه به‌روزرسانی می‌شود
                // (چه تعزیه تازه ساخته شده باشد چه از قبل موجود بوده)
                val author = taziehObj.optString("author", "").ifBlank { null }
                val authorEmail = taziehObj.optString("authorEmail", "").ifBlank { null }
                if (author != null || authorEmail != null) {
                    db.taziehDao().updateAuthor(
                        taziehId,
                        author ?: existingTazieh?.author,
                        authorEmail ?: existingTazieh?.authorEmail
                    )
                }

                val roles = taziehObj.getJSONArray("roles")
                for (ri in 0 until roles.length()) {
                    val roleObj = roles.getJSONObject(ri)
                    val roleTitle = roleObj.getString("title")
                    val roleId = db.roleDao().getByTitle(taziehId, roleTitle)?.id
                        ?: run {
                            val nextRoleOrder = db.roleDao().getMaxOrderIndex(taziehId) + 1
                            db.roleDao().insert(RoleEntity(taziehId = taziehId, title = roleTitle, orderIndex = nextRoleOrder))
                        }

                    var nextOrderIndex = db.sectionDao().getMaxOrderIndex(roleId) + 1
                    val sections = roleObj.getJSONArray("sections")
                    for (si in 0 until sections.length()) {
                        val secObj = sections.getJSONObject(si)
                        val sectionTitle = secObj.getString("title")
                        val newContent = secObj.getString("content")
                        val newAudio = secObj.optString("audio", "").ifBlank { null }

                        // بروزرسانی: اگر بخشی با همین عنوان زیر همین نقش از قبل وجود دارد،
                        // به‌جای اضافه‌کردن یک نسخه‌ی تکراری، متنش (و صدایش در صورت وجود) جایگزین می‌شود
                        // — دقیقاً برای همین که اگر یک جمله را در Word اصلاح کردید و دوباره بروزرسانی
                        // زدید، همان بخش در اپ هم اصلاح شود، نه اینکه یک بخش تکراری اضافه شود.
                        val existingSection = db.sectionDao().getByTitle(roleId, sectionTitle)
                        if (existingSection != null) {
                            if (existingSection.content != newContent) {
                                db.sectionDao().updateContent(existingSection.id, newContent)
                            }
                            if (newAudio != null && newAudio != existingSection.audioUrl) {
                                db.sectionDao().updateAudioUrl(existingSection.id, newAudio)
                            }
                        } else {
                            db.sectionDao().insert(
                                SectionEntity(
                                    roleId = roleId,
                                    orderIndex = nextOrderIndex,
                                    title = sectionTitle,
                                    content = newContent,
                                    audioUrl = newAudio
                                )
                            )
                            nextOrderIndex++
                        }
                    }
                }
            }
        }
    }
}
