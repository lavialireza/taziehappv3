package com.example.bookapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface FieldDao {
    @Query("SELECT * FROM fields ORDER BY id")
    suspend fun getAll(): List<FieldEntity>

    @Query("SELECT * FROM fields WHERE title = :title LIMIT 1")
    suspend fun getByTitle(title: String): FieldEntity?

    @Insert
    suspend fun insert(field: FieldEntity): Long

    @Query("DELETE FROM fields")
    suspend fun deleteAll()
}

@Dao
interface TaziehDao {
    @Query("SELECT * FROM taziehs WHERE fieldId = :fieldId ORDER BY id")
    suspend fun getByField(fieldId: Long): List<TaziehEntity>

    @Query("SELECT * FROM taziehs ORDER BY fieldId, id")
    suspend fun getAll(): List<TaziehEntity>

    @Query("SELECT * FROM taziehs WHERE id = :taziehId")
    suspend fun getById(taziehId: Long): TaziehEntity?

    @Query("SELECT * FROM taziehs WHERE fieldId = :fieldId AND title = :title LIMIT 1")
    suspend fun getByTitle(fieldId: Long, title: String): TaziehEntity?

    @Insert
    suspend fun insert(tazieh: TaziehEntity): Long

    @Query("UPDATE taziehs SET author = :author, authorEmail = :authorEmail WHERE id = :taziehId")
    suspend fun updateAuthor(taziehId: Long, author: String?, authorEmail: String?)

    @Query("DELETE FROM taziehs")
    suspend fun deleteAll()
}

@Dao
interface RoleDao {
    @Query("SELECT * FROM roles WHERE taziehId = :taziehId ORDER BY orderIndex, id")
    suspend fun getByTazieh(taziehId: Long): List<RoleEntity>

    @Query("SELECT * FROM roles WHERE id = :roleId")
    suspend fun getById(roleId: Long): RoleEntity

    @Query("SELECT * FROM roles WHERE taziehId = :taziehId AND title = :title LIMIT 1")
    suspend fun getByTitle(taziehId: Long, title: String): RoleEntity?

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM roles WHERE taziehId = :taziehId")
    suspend fun getMaxOrderIndex(taziehId: Long): Int

    @Insert
    suspend fun insert(role: RoleEntity): Long

    @Query("UPDATE roles SET title = :title WHERE id = :roleId")
    suspend fun updateTitle(roleId: Long, title: String)

    @Query("UPDATE roles SET orderIndex = :orderIndex WHERE id = :roleId")
    suspend fun updateOrderIndex(roleId: Long, orderIndex: Int)

    @Query("DELETE FROM roles")
    suspend fun deleteAll()
}

@Dao
interface SectionDao {
    @Query("SELECT * FROM sections WHERE roleId = :roleId ORDER BY orderIndex")
    suspend fun getByRole(roleId: Long): List<SectionEntity>

    @Query("SELECT * FROM sections WHERE id = :sectionId")
    suspend fun getById(sectionId: Long): SectionEntity

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM sections WHERE roleId = :roleId")
    suspend fun getMaxOrderIndex(roleId: Long): Int

    @Query("SELECT * FROM sections WHERE roleId = :roleId AND title = :title LIMIT 1")
    suspend fun getByTitle(roleId: Long, title: String): SectionEntity?

    @Insert
    suspend fun insert(section: SectionEntity): Long

    @Query("UPDATE sections SET audioUrl = :audioUrl WHERE id = :sectionId")
    suspend fun updateAudioUrl(sectionId: Long, audioUrl: String?)

    @Query("UPDATE sections SET content = :content WHERE id = :sectionId")
    suspend fun updateContent(sectionId: Long, content: String)

    @Query("DELETE FROM sections")
    suspend fun deleteAll()
}

@Dao
interface FootnoteDao {
    @Query("SELECT * FROM footnotes WHERE sectionId = :sectionId ORDER BY id")
    suspend fun getBySection(sectionId: Long): List<FootnoteEntity>

    @Insert
    suspend fun insert(footnote: FootnoteEntity): Long

    @Update
    suspend fun update(footnote: FootnoteEntity)

    @Query("DELETE FROM footnotes WHERE id = :footnoteId")
    suspend fun delete(footnoteId: Long)
}

@Dao
interface DialogueDao {
    @Query("SELECT * FROM dialogues WHERE taziehId = :taziehId ORDER BY id")
    suspend fun getByTazieh(taziehId: Long): List<DialogueEntity>

    @Query("SELECT * FROM dialogues WHERE id = :dialogueId")
    suspend fun getById(dialogueId: Long): DialogueEntity

    @Insert
    suspend fun insert(dialogue: DialogueEntity): Long

    @Query("UPDATE dialogues SET title = :title WHERE id = :dialogueId")
    suspend fun updateTitle(dialogueId: Long, title: String)

    @Query("DELETE FROM dialogues WHERE id = :dialogueId")
    suspend fun delete(dialogueId: Long)
}

@Dao
interface DialogueTurnDao {
    @Query("SELECT * FROM dialogue_turns WHERE dialogueId = :dialogueId ORDER BY orderIndex")
    suspend fun getByDialogue(dialogueId: Long): List<DialogueTurnEntity>

    @Insert
    suspend fun insert(turn: DialogueTurnEntity): Long

    @Query("UPDATE dialogue_turns SET orderIndex = :orderIndex WHERE id = :turnId")
    suspend fun updateOrderIndex(turnId: Long, orderIndex: Int)

    @Query("DELETE FROM dialogue_turns WHERE id = :turnId")
    suspend fun deleteTurn(turnId: Long)

    @Query("DELETE FROM dialogue_turns WHERE dialogueId = :dialogueId")
    suspend fun deleteAllForDialogue(dialogueId: Long)
}

@Dao
interface TaziehImageDao {
    @Query("SELECT * FROM tazieh_images WHERE taziehId = :taziehId ORDER BY id")
    suspend fun getByTazieh(taziehId: Long): List<TaziehImageEntity>

    @Insert
    suspend fun insert(image: TaziehImageEntity): Long

    @Query("UPDATE tazieh_images SET caption = :caption WHERE id = :imageId")
    suspend fun updateCaption(imageId: Long, caption: String)

    @Query("SELECT * FROM tazieh_images WHERE id = :imageId")
    suspend fun getById(imageId: Long): TaziehImageEntity

    @Query("DELETE FROM tazieh_images WHERE id = :imageId")
    suspend fun delete(imageId: Long)
}
