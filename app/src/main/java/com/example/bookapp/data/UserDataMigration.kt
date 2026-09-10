package com.example.bookapp.data

import android.content.Context
import androidx.room.withTransaction
import java.util.Calendar

/** Bridges the old SharedPreferences user data into Room while the UI migrates gradually. */
suspend fun migratePrefsUserDataToRoom(context: Context, db: AppDatabase) {
    db.withTransaction {
        val sections = db.sectionDao().getAllForUserMigration()
        val byId = sections.associateBy { it.id }

        val dao = db.userDataDao()
        dao.deleteAllBookmarks()
        Prefs.getBookmarks(context).forEach { id -> byId[id]?.let { dao.insertBookmark(BookmarkEntity(sectionUid = it.uid)) } }

        dao.deleteAllTags()
        Prefs.getAllTags(context).forEach { (id, tag) -> byId[id]?.let { dao.insertTag(SectionTagEntity(sectionUid = it.uid, tag = tag)) } }

        dao.deleteAllRecent()
        Prefs.getRecent(context).forEachIndexed { index, id -> byId[id]?.let { dao.insertRecent(RecentSectionEntity(sectionUid = it.uid, position = index)) } }

        dao.deleteAllReadingHistory()
        Prefs.getReadSectionIds(context).forEach { id -> byId[id]?.let { dao.insertReading(ReadingHistoryEntity(sectionUid = it.uid)) } }

        dao.deleteAllActiveDays()
        Prefs.getActiveDayValues(context).forEach { day -> dao.insertActiveDay(ActiveDayEntity(dayKey = day)) }

        dao.deleteAllMyRoles()
        val taziehs = db.taziehDao().getAll().associateBy { it.id }
        val roleIds = Prefs.getAllMyRoles(context).values.toSet()
        val roles = if (roleIds.isEmpty()) emptyMap() else db.roleDao().getByIds(roleIds).associateBy { it.id }
        Prefs.getAllMyRoles(context).forEach { (taziehId, roleId) ->
            val t = taziehs[taziehId]
            val r = roles[roleId]
            if (t != null && r != null) dao.insertMyRole(MyRoleEntity(taziehUid = t.uid, roleUid = r.uid))
        }
    }
}

/** Local-day helper used by future Room-based streak calculations. */
fun localDayKeyString(): String {
    val c = Calendar.getInstance()
    return "%04d-%02d-%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
}
