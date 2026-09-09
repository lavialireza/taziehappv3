package com.example.bookapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContentMergeTest {
    private lateinit var db: AppDatabase

    private val sampleJson = """
        [{
          "id":"field-1","title":"اصفهان","taziehs":[{
            "id":"tazieh-1","title":"عاشورا","complete":true,"roles":[{
              "id":"role-1","title":"شمر","sections":[
                {"id":"sec-1","title":"ورود","content":"بیت اول"},
                {"id":"sec-2","title":"شهادت","content":"بیت دوم"}
              ]
            }]
          }]
        }]
    """.trimIndent()

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }
    @After fun tearDown() { db.close() }

    @Test fun `same complete file twice does not duplicate and preserves order`() = runTest {
        mergeContentFromJson(db, sampleJson)
        mergeContentFromJson(db, sampleJson)
        val role = db.roleDao().getByTazieh(db.taziehDao().getAll().first().id).first()
        val sections = db.sectionDao().getByRole(role.id)
        assertEquals(2, sections.size)
        assertEquals(listOf(0,1), sections.map { it.orderIndex })
    }

    @Test fun `changed content is updated without creating duplicate`() = runTest {
        mergeContentFromJson(db, sampleJson)
        val updated = sampleJson.replace("بیت اول", "بیت اصلاح‌شده")
        mergeContentFromJson(db, updated)
        val role = db.roleDao().getByTazieh(db.taziehDao().getAll().first().id).first()
        val sections = db.sectionDao().getByRole(role.id)
        assertEquals(2, sections.size)
        assertEquals("بیت اصلاح‌شده", sections.first { it.stableKey == "sec-1" }.content)
    }

    @Test fun `complete update removes sections missing from source`() = runTest {
        mergeContentFromJson(db, sampleJson)
        val reduced = sampleJson.replace(",\n                {\"id\":\"sec-2\",\"title\":\"شهادت\",\"content\":\"بیت دوم\"}", "")
        mergeContentFromJson(db, reduced)
        val role = db.roleDao().getByTazieh(db.taziehDao().getAll().first().id).first()
        assertEquals(listOf("sec-1"), db.sectionDao().getByRole(role.id).map { it.stableKey })
    }

    @Test fun `reordering keeps stable section identities`() = runTest {
        mergeContentFromJson(db, sampleJson)
        val reordered = sampleJson.replace(
            "{\"id\":\"sec-1\",\"title\":\"ورود\",\"content\":\"بیت اول\"},\n                {\"id\":\"sec-2\",\"title\":\"شهادت\",\"content\":\"بیت دوم\"}",
            "{\"id\":\"sec-2\",\"title\":\"شهادت\",\"content\":\"بیت دوم\"},\n                {\"id\":\"sec-1\",\"title\":\"ورود\",\"content\":\"بیت اول\"}"
        )
        mergeContentFromJson(db, reordered)
        val role = db.roleDao().getByTazieh(db.taziehDao().getAll().first().id).first()
        assertEquals(listOf("sec-2", "sec-1"), db.sectionDao().getByRole(role.id).map { it.stableKey })
        assertNotEquals(db.sectionDao().getByStableKey("sec-1")?.id, db.sectionDao().getByStableKey("sec-2")?.id)
    }
}
