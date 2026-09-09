package com.example.bookapp.data

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

/** ایندکس مستقل FTS4؛ متن نرمال‌شده فارسی در آن ذخیره می‌شود. */
@Entity(tableName = "sections_fts")
@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61)
data class SectionFts(
    val sectionId: Long,
    val title: String,
    val content: String
)
