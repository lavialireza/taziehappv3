package com.example.bookapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.bookapp.R

val TaziehFontFamily = FontFamily(
    Font(R.font.b_titr_bold, FontWeight.Normal),
    Font(R.font.b_titr_bold, FontWeight.Bold)
)

/**
 * فونت‌های قابل انتخاب در تنظیمات. در حال حاضر فقط یک فایل فونت اختصاصی
 * (B Titr Bold) در پروژه موجود است؛ بقیه گزینه‌ها از فونت‌های عمومی
 * خود اندروید هستند. اگر فایل فونت (.ttf) دیگری ارسال شود، به همین لیست
 * اضافه می‌شود.
 */
val FontChoices = mapOf(
    "titr" to TaziehFontFamily,
    "serif" to FontFamily.Serif,
    "sans" to FontFamily.SansSerif,
    "cursive" to FontFamily.Cursive,
)

val FontChoiceLabels = mapOf(
    "titr" to "B Titr (اختصاصی)",
    "serif" to "سریف",
    "sans" to "بدون سریف",
    "cursive" to "شکسته",
)

fun typographyFor(fontChoice: String): Typography {
    val family = FontChoices[fontChoice] ?: TaziehFontFamily
    return Typography(
        displayLarge = TextStyle(fontFamily = family),
        displayMedium = TextStyle(fontFamily = family),
        displaySmall = TextStyle(fontFamily = family),
        headlineLarge = TextStyle(fontFamily = family),
        headlineMedium = TextStyle(fontFamily = family),
        headlineSmall = TextStyle(fontFamily = family),
        titleLarge = TextStyle(fontFamily = family),
        titleMedium = TextStyle(fontFamily = family),
        titleSmall = TextStyle(fontFamily = family),
        bodyLarge = TextStyle(fontFamily = family, fontSize = 18.sp, lineHeight = 32.sp),
        bodyMedium = TextStyle(fontFamily = family),
        bodySmall = TextStyle(fontFamily = family),
        labelLarge = TextStyle(fontFamily = family),
        labelMedium = TextStyle(fontFamily = family),
        labelSmall = TextStyle(fontFamily = family)
    )
}
