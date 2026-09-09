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
    val bodyFamily = FontChoices[fontChoice] ?: FontFamily.Serif
    // تیترها با B Titr و متن اصلی با فونت خواناتر نمایش داده می‌شوند.
    // B Nazanin در این بسته وجود ندارد؛ بنابراین Serif به‌عنوان fallback فارسی
    // انتخاب شده تا متن طولانی خوانایی بیشتری داشته باشد.
    return Typography(
        displayLarge = TextStyle(fontFamily = TaziehFontFamily, fontWeight = FontWeight.Bold),
        displayMedium = TextStyle(fontFamily = TaziehFontFamily, fontWeight = FontWeight.Bold),
        displaySmall = TextStyle(fontFamily = TaziehFontFamily, fontWeight = FontWeight.Bold),
        headlineLarge = TextStyle(fontFamily = TaziehFontFamily, fontWeight = FontWeight.Bold),
        headlineMedium = TextStyle(fontFamily = TaziehFontFamily, fontWeight = FontWeight.Bold),
        headlineSmall = TextStyle(fontFamily = TaziehFontFamily, fontWeight = FontWeight.Bold),
        titleLarge = TextStyle(fontFamily = TaziehFontFamily, fontWeight = FontWeight.Bold),
        titleMedium = TextStyle(fontFamily = bodyFamily),
        titleSmall = TextStyle(fontFamily = bodyFamily),
        bodyLarge = TextStyle(fontFamily = bodyFamily, fontSize = 18.sp, lineHeight = 32.sp),
        bodyMedium = TextStyle(fontFamily = bodyFamily, fontSize = 16.sp, lineHeight = 28.sp),
        bodySmall = TextStyle(fontFamily = bodyFamily, fontSize = 14.sp, lineHeight = 24.sp),
        labelLarge = TextStyle(fontFamily = bodyFamily),
        labelMedium = TextStyle(fontFamily = bodyFamily),
        labelSmall = TextStyle(fontFamily = bodyFamily)
    )
}
