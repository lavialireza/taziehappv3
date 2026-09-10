package com.example.bookapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// سطح ۱: زمینه‌ها (اصفهان، تهران، میرعزا و ...)
@Entity(tableName = "fields")
data class FieldEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String
)

// سطح ۲: تعزیه‌ها (عاشورا، بازار شام و ...) - متعلق به یک زمینه
@Entity(
    tableName = "taziehs",
    foreignKeys = [ForeignKey(
        entity = FieldEntity::class,
        parentColumns = ["id"],
        childColumns = ["fieldId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("fieldId")]
)
data class TaziehEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fieldId: Long,
    val title: String,
    // مشخصات نویسنده/گردآورنده این تعزیه (اختیاری)؛ با هر بروزرسانی محتوا قابل به‌روزرسانی است
    val author: String? = null,
    val authorEmail: String? = null
)

// سطح ۳: نقش‌ها (شمر، امام، یزید و ...) - متعلق به یک تعزیه
@Entity(
    tableName = "roles",
    foreignKeys = [ForeignKey(
        entity = TaziehEntity::class,
        parentColumns = ["id"],
        childColumns = ["taziehId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("taziehId")]
)
data class RoleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taziehId: Long,
    val title: String,
    // ترتیب نمایش نقش در فهرست/نمایش‌نامه؛ پیش‌فرض بر اساس ترتیب ورود از متن اصلی
    // (همان ترتیبی که در فایل Word/JSON آمده)، ولی از داخل برنامه هم قابل تغییر است.
    val orderIndex: Int = 0
)

// سطح ۴: بخش‌ها (ورود، ساقی‌نامه، شهادت و ...) - متعلق به یک نقش، شامل متن اشعار
@Entity(
    tableName = "sections",
    foreignKeys = [ForeignKey(
        entity = RoleEntity::class,
        parentColumns = ["id"],
        childColumns = ["roleId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("roleId")]
)
data class SectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roleId: Long,
    val orderIndex: Int,
    val title: String,
    val content: String, // متن اشعار همان بخش
    // آدرس فایل صوتی واقعی (ضبط‌شده) برای این بخش، در صورت وجود؛
    // می‌تواند یک URL کامل باشد یا مسیر نسبی داخل assets/audio (مثلاً "audio/karbala_shahadat.mp3").
    // اگر خالی/نال باشد، پخش با صدای مصنوعی (TTS) انجام می‌شود.
    val audioUrl: String? = null
)

// پاورقی: توضیح یک واژه/عبارت خاص در یک بخش (معنی لغت، توضیح مختصر، منبع و ...)
// کاربر خودش این‌ها را از داخل برنامه اضافه/ویرایش/حذف می‌کند.
@Entity(
    tableName = "footnotes",
    foreignKeys = [ForeignKey(
        entity = SectionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sectionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sectionId")]
)
data class FootnoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sectionId: Long,
    val term: String,       // واژه یا عبارتی که توضیح داده می‌شود
    val explanation: String // متن توضیح (معنی، منبع، نکته و ...)
)

// گفتگو: مجموعه‌ای نام‌دار از نوبت‌های پشت‌سرهم که هرکدام فقط اشاره (لینک) به یک
// بخش موجود است (بدون کپی متن) - برای مکالمه‌های چندنقشی مثل شمر و عباس، یا
// امام حسین و علی‌اکبر، که هر نوبتش در «نقش» جداگانه‌ای در دیتابیس ذخیره شده است.
@Entity(
    tableName = "dialogues",
    foreignKeys = [ForeignKey(
        entity = TaziehEntity::class,
        parentColumns = ["id"],
        childColumns = ["taziehId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("taziehId")]
)
data class DialogueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taziehId: Long,
    val title: String
)

// هر نوبت از یک گفتگو: اشاره به یک بخش موجود + ترتیبش در گفتگو
@Entity(
    tableName = "dialogue_turns",
    foreignKeys = [
        ForeignKey(
            entity = DialogueEntity::class,
            parentColumns = ["id"],
            childColumns = ["dialogueId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("dialogueId"), Index("sectionId")]
)
data class DialogueTurnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dialogueId: Long,
    val sectionId: Long,
    val orderIndex: Int
)

// عکس‌های قدیمی/تاریخی مربوط به یک تعزیه (نسخه‌های خطی، تعزیه‌خوانان معروف و ...)
// خود فایل عکس در حافظه داخلی برنامه کپی می‌شود و فقط مسیرش اینجا ذخیره می‌شود.
@Entity(
    tableName = "tazieh_images",
    foreignKeys = [ForeignKey(
        entity = TaziehEntity::class,
        parentColumns = ["id"],
        childColumns = ["taziehId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("taziehId")]
)
data class TaziehImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taziehId: Long,
    val filePath: String,   // مسیر فایل در حافظه داخلی برنامه
    val caption: String = "" // توضیح اختیاری (مثلاً «نسخه خطی قرن سیزدهم» یا اسم تعزیه‌خوان)
)
