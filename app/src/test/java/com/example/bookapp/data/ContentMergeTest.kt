package com.example.bookapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * تست‌های واحد برای منطق ادغام تدریجی محتوا (mergeContentFromJson):
 *  - وقتی زمینه/تعزیه/نقشی با همان عنوان از قبل هست، رکورد تکراری ساخته نمی‌شود.
 *  - بخش‌های (sections) هر فایل جدید همیشه با ادامه‌ی شماره‌ترتیب به انتهای همان نقش اضافه می‌شوند.
 */
@RunWith(RobolectricTestRunner::class)
class ContentMergeTest {

    private lateinit var db: AppDatabase

    private val sampleJson = """
        [
          {
            "title": "اصفهان",
            "taziehs": [
              {
                "title": "عاشورا",
                "roles": [
                  {
                    "title": "شمر",
                    "sections": [
                      {"title": "ورود", "content": "بیت اول"},
                      {"title": "شهادت", "content": "بیت دوم"}
                    ]
                  }
                ]
              }
            ]
          }
        ]
    """.trimIndent()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `merging same file twice does not duplicate field, tazieh or role`() = runTest {
        mergeContentFromJson(db, sampleJson)
        mergeContentFromJson(db, sampleJson)

        assertEquals(1, db.fieldDao().getAll().size)
        assertEquals(1, db.taziehDao().getAll().size)

        val fieldId = db.fieldDao().getAll().first().id
        val taziehId = db.taziehDao().getByField(fieldId).first().id
        val roles = db.roleDao().getByTazieh(taziehId)
        assertEquals(1, roles.size)
    }

    @Test
    fun `merging same file twice appends sections in order instead of skipping`() = runTest {
        mergeContentFromJson(db, sampleJson)
        mergeContentFromJson(db, sampleJson)

        val fieldId = db.fieldDao().getAll().first().id
        val taziehId = db.taziehDao().getByField(fieldId).first().id
        val roleId = db.roleDao().getByTazieh(taziehId).first().id
        val sections = db.sectionDao().getByRole(roleId)

        // هر بار ۲ بخش داشتیم؛ بعد از دو بار ادغام باید ۴ بخش با orderIndex پیوسته ۰..۳ داشته باشیم
        assertEquals(4, sections.size)
        assertEquals(listOf(0, 1, 2, 3), sections.map { it.orderIndex })
    }

    @Test
    fun `new role added to an existing tazieh does not duplicate the tazieh`() = runTest {
        mergeContentFromJson(db, sampleJson)

        val secondRoleJson = """
            [
              {
                "title": "اصفهان",
                "taziehs": [
                  {
                    "title": "عاشورا",
                    "roles": [
                      {
                        "title": "امام حسین",
                        "sections": [
                          {"title": "ورود", "content": "بیت تازه"}
                        ]
                      }
                    ]
                  }
                ]
              }
            ]
        """.trimIndent()
        mergeContentFromJson(db, secondRoleJson)

        assertEquals(1, db.fieldDao().getAll().size)
        assertEquals(1, db.taziehDao().getAll().size)

        val fieldId = db.fieldDao().getAll().first().id
        val taziehId = db.taziehDao().getByField(fieldId).first().id
        val roles = db.roleDao().getByTazieh(taziehId)
        assertEquals(2, roles.size) // شمر (قبلی) + امام حسین (جدید)
    }
}
