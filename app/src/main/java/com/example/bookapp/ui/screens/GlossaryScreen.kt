package com.example.bookapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class GlossaryTerm(val term: String, val explanation: String)

/**
 * فهرست ثابت اصطلاحات رایج تعزیه، برای کاربرانی که با این واژه‌ها آشنا نیستند.
 * این لیست مستقل از محتوای دیتابیس است؛ اگر خواستید واژه‌ی تازه‌ای اضافه کنید،
 * کافی است یک خط به GLOSSARY_TERMS در پایین همین فایل اضافه کنید.
 */
val GLOSSARY_TERMS = listOf(
    GlossaryTerm("تعزیه", "نوعی نمایش سنتی و آیینی ایرانی که وقایع کربلا و رویدادهای مذهبی مشابه را روایت می‌کند."),
    GlossaryTerm("مجلس", "هر یک از داستان‌های مستقل تعزیه (مثل «مجلس شهادت حضرت علی‌اکبر»)؛ همان چیزی که در این برنامه «تعزیه» نامیده می‌شود."),
    GlossaryTerm("تعزیه‌خوان", "بازیگری که نقشی را در تعزیه اجرا می‌کند؛ معمولاً متن نقش خود را با آواز می‌خواند."),
    GlossaryTerm("ذوالجناح", "نام اسب امام حسین (ع) در واقعه‌ی عاشورا."),
    GlossaryTerm("علمدار", "لقب حضرت عباس (ع)، به‌خاطر بر دوش‌کشیدن پرچم (علم) سپاه امام حسین (ع)."),
    GlossaryTerm("ساقی‌نامه", "بخشی از تعزیه که درباره‌ی تشنگی و آب‌رسانی (معمولاً به‌وسیله‌ی حضرت عباس) است."),
    GlossaryTerm("شبیه‌خوانی", "نام دیگر تعزیه؛ چون بازیگران «شبیه» شخصیت‌های واقعه را اجرا می‌کنند."),
    GlossaryTerm("مرثیه", "شعر یا سرودی در سوگ یک شخصیت یا واقعه، که در بسیاری از مجالس تعزیه خوانده می‌شود."),
    GlossaryTerm("نوحه", "نوعی سرود سوگواری با وزن مشخص، معمولاً همراه با حرکات دسته‌جمعی."),
    GlossaryTerm("وعده‌گاه", "محل برگزاری اجرای تعزیه، که معمولاً به‌طور موقت یا دائم (مثل تکیه) آماده می‌شود."),
    GlossaryTerm("تکیه", "بنایی که به‌طور خاص برای برگزاری مراسم عزاداری و تعزیه ساخته می‌شود."),
    GlossaryTerm("خیمه‌گاه", "محل استقرار خیمه‌های سپاه امام حسین (ع) در کربلا؛ صحنه‌ی بسیاری از مجالس تعزیه."),
    GlossaryTerm("اصغر", "اشاره به حضرت علی‌اصغر (ع)، کوچک‌ترین فرزند امام حسین (ع) که در کربلا به شهادت رسید.")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlossaryScreen(onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) GLOSSARY_TERMS
        else GLOSSARY_TERMS.filter { it.term.contains(query, ignoreCase = true) || it.explanation.contains(query, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("دیکشنری اصطلاحات تعزیه") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("جستجوی اصطلاح") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered) { entry ->
                    Card(shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(entry.term, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(entry.explanation, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
