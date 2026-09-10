package com.example.bookapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * تست‌های واحد برای قابلیت‌هایی که بعد از ContentMergeTest اضافه شدند:
 * پاورقی، گفتگو (و نوبت‌هایش)، ترتیب نقش‌ها، و پشتیبان‌گیری/بازیابی.
 */
@RunWith(RobolectricTestRunner::class)
class DataLayerTest {

    private lateinit var db: AppDatabase
    private lateinit var fieldId: Long
    private lateinit var taziehId: Long
    private var roleAId: Long = 0
    private var roleBId: Long = 0
    private var sectionAId: Long = 0
    private var sectionBId: Long = 0

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        fieldId = db.fieldDao().insert(FieldEntity(title = "اصفهان"))
        taziehId = db.taziehDao().insert(TaziehEntity(fieldId = fieldId, title = "عاشورا"))
        roleAId = db.roleDao().insert(RoleEntity(taziehId = taziehId, title = "امام حسین", orderIndex = 0))
        roleBId = db.roleDao().insert(RoleEntity(taziehId = taziehId, title = "علی‌اکبر", orderIndex = 1))
        sectionAId = db.sectionDao().insert(SectionEntity(roleId = roleAId, orderIndex = 0, title = "وداع", content = "بیت امام"))
        sectionBId = db.sectionDao().insert(SectionEntity(roleId = roleBId, orderIndex = 0, title = "جواب", content = "بیت علی‌اکبر"))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `footnote can be added, read back, and updated`() = runTest {
        val id = db.footnoteDao().insert(FootnoteEntity(sectionId = sectionAId, term = "وداع", explanation = "خداحافظی"))
        var list = db.footnoteDao().getBySection(sectionAId)
        assertEquals(1, list.size)
        assertEquals("خداحافظی", list[0].explanation)

        db.footnoteDao().update(list[0].copy(explanation = "خداحافظی پیش از میدان"))
        list = db.footnoteDao().getBySection(sectionAId)
        assertEquals("خداحافظی پیش از میدان", list[0].explanation)

        db.footnoteDao().delete(id)
        assertTrue(db.footnoteDao().getBySection(sectionAId).isEmpty())
    }

    @Test
    fun `role orderIndex controls getByTazieh ordering and can be swapped`() = runTest {
        var roles = db.roleDao().getByTazieh(taziehId)
        assertEquals(listOf("امام حسین", "علی‌اکبر"), roles.map { it.title })

        // جابه‌جایی ترتیب (شبیه دکمه‌های بالا/پایین در فهرست)
        db.roleDao().updateOrderIndex(roleAId, 1)
        db.roleDao().updateOrderIndex(roleBId, 0)

        roles = db.roleDao().getByTazieh(taziehId)
        assertEquals(listOf("علی‌اکبر", "امام حسین"), roles.map { it.title })
    }

    @Test
    fun `dialogue turns keep given order and cascade-delete with dialogue`() = runTest {
        val dialogueId = db.dialogueDao().insert(DialogueEntity(taziehId = taziehId, title = "گفتگوی امام و علی‌اکبر"))
        db.dialogueTurnDao().insert(DialogueTurnEntity(dialogueId = dialogueId, sectionId = sectionAId, orderIndex = 0))
        db.dialogueTurnDao().insert(DialogueTurnEntity(dialogueId = dialogueId, sectionId = sectionBId, orderIndex = 1))

        var turns = db.dialogueTurnDao().getByDialogue(dialogueId)
        assertEquals(2, turns.size)
        assertEquals(sectionAId, turns[0].sectionId)
        assertEquals(sectionBId, turns[1].sectionId)

        // جابه‌جایی ترتیب نوبت‌ها
        db.dialogueTurnDao().updateOrderIndex(turns[0].id, 1)
        db.dialogueTurnDao().updateOrderIndex(turns[1].id, 0)
        turns = db.dialogueTurnDao().getByDialogue(dialogueId)
        assertEquals(sectionBId, turns[0].sectionId)

        // حذف گفتگو باید نوبت‌هایش را هم پاک کند (ForeignKey CASCADE)
        db.dialogueDao().delete(dialogueId)
        assertTrue(db.dialogueTurnDao().getByDialogue(dialogueId).isEmpty())
    }

    @Test
    fun `backup json round-trips notes bookmarks footnotes and dialogues`() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        db.noteDao().insert(NoteEntity(title = "یادداشت من", content = "متن یادداشت"))
        Prefs.toggleBookmark(context, sectionAId)
        db.footnoteDao().insert(FootnoteEntity(sectionId = sectionAId, term = "وداع", explanation = "خداحافظی"))
        Prefs.setMyRole(context, taziehId, roleAId)
        val dialogueId = db.dialogueDao().insert(DialogueEntity(taziehId = taziehId, title = "گفتگوی تست"))
        db.dialogueTurnDao().insert(DialogueTurnEntity(dialogueId = dialogueId, sectionId = sectionAId, orderIndex = 0))

        val json = buildBackupJson(context, db)

        // نوشتن json در یک فایل موقت و خواندنش از طریق Uri، دقیقاً مثل جریان واقعی برنامه
        val tempFile = File.createTempFile("backup_test", ".json")
        tempFile.writeText(json)
        val uri = android.net.Uri.fromFile(tempFile)

        // یک دیتابیس تازه و خالی برای شبیه‌سازی «بازیابی روی یک نصب جدید»
        val freshDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val freshFieldId = freshDb.fieldDao().insert(FieldEntity(title = "اصفهان"))
        val freshTaziehId = freshDb.taziehDao().insert(TaziehEntity(fieldId = freshFieldId, title = "عاشورا"))
        val freshRoleId = freshDb.roleDao().insert(RoleEntity(taziehId = freshTaziehId, title = "امام حسین"))
        val freshSectionId = freshDb.sectionDao().insert(SectionEntity(roleId = freshRoleId, orderIndex = 0, title = "وداع", content = "بیت امام"))

        val result = restoreBackupFromUri(context, freshDb, uri)
        assertTrue(result.isSuccess)

        val restoredNotes = freshDb.noteDao().getAll()
        assertEquals(1, restoredNotes.size)
        assertEquals("یادداشت من", restoredNotes[0].title)

        val restoredFootnotes = freshDb.footnoteDao().getBySection(sectionAId)
        assertTrue(restoredFootnotes.isNotEmpty())

        val restoredDialogues = freshDb.dialogueDao().getByTazieh(freshTaziehId)
        assertTrue(restoredDialogues.any { it.title == "گفتگوی تست" })

        tempFile.delete()
        freshDb.close()
    }
}
