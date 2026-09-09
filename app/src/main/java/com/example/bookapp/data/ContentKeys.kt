package com.example.bookapp.data

import java.security.MessageDigest

/** شناسه پایدار محتوا؛ مستقل از ID داخلی Room و مناسب برای Update/Backup/DeepLink. */
fun contentKey(raw: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(raw.trim().toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }.take(24)
}

fun normalizePersian(text: String): String = text
    .replace('\u064A', '\u06CC') // ي -> ی
    .replace('\u0649', '\u06CC') // ى -> ی
    .replace('\u0643', '\u06A9') // ك -> ک
    .replace('\u0629', '\u0647') // ة -> ه
    .replace('\u0624', '\u0648') // ؤ -> و
    .replace('\u0626', '\u06CC') // ئ -> ی
    .replace('\u0623', '\u0627') // أ -> ا
    .replace('\u0625', '\u0627') // إ -> ا
    .replace('\u0622', '\u0627') // آ -> ا
    .replace(Regex("[\\u064B-\\u065F\\u0670]"), "")
    .replace(Regex("[\\u200C\\u200D]"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()
