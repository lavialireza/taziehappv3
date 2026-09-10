package com.example.bookapp.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.bookapp.BuildConfig
import com.example.bookapp.data.AppDatabase
import com.example.bookapp.data.Prefs
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    fieldsCount: Int,
    taziehsCount: Int,
    rolesCount: Int,
    sectionsCount: Int,
    readCount: Int,
    streakDays: Int,
    activeDaysLast14: List<Boolean> = emptyList(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("درباره برنامه") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "این اپلیکیشن یک کتابخانه دیجیتال از متون تعزیه است که بر اساس " +
                        "زمینه، تعزیه، نقش و بخش دسته‌بندی شده است."
            )
            Spacer(Modifier.height(20.dp))
            Text("آمار مجموعه:", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Text("$fieldsCount زمینه")
            Text("$taziehsCount تعزیه")
            Text("$rolesCount نقش")
            Text("$sectionsCount بخش")

            Spacer(Modifier.height(20.dp))
            Text("آمار مطالعه شما:", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Text("$readCount بخش را تا الان خوانده‌اید")
            if (streakDays > 1) {
                Text("$streakDays روز متوالی سر زده‌اید 🔥")
            }
            if (activeDaysLast14.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("۱۴ روز اخیر:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    activeDaysLast14.forEach { active ->
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text("مشخصات نویسنده برنامه و گردآوری", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Text("مهندس علیرضا لاوی")
            Text("نسخه: ${BuildConfig.VERSION_NAME}")
            Text("راه ارتباطی: [09132383677]")

            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        android.content.Intent.EXTRA_TEXT,
                        "این اپ رو ببین: «تعزیه و شبیه‌خوانی» — کتابخانه‌ای کامل و آفلاین از نسخه‌های تعزیه.\nhttps://github.com/lavialireza/taziehapp"
                    )
                }
                context.startActivity(android.content.Intent.createChooser(intent, "معرفی اپ به دیگران"))
            }) {
                Text("معرفی این اپ به دیگران")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    autoDarkMode: Boolean,
    onAutoDarkModeChange: (Boolean) -> Unit,
    fontScale: Float,
    onFontScaleChange: (Float) -> Unit,
    fontChoice: String,
    onFontChoiceChange: (String) -> Unit,
    themeChoice: String,
    onThemeChoiceChange: (String) -> Unit,
    keepScreenOn: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onSyncContent: suspend () -> Result<Unit>,
    db: AppDatabase,
    onBack: () -> Unit
) {
    var syncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تنظیمات") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("حالت شب (تیره)", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = darkMode, onCheckedChange = onDarkModeChange, enabled = !autoDarkMode)
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("تاریک خودکار بر اساس ساعت", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "بین ساعت ۱۸ تا ۶ صبح خودکار به حالت تیره می‌رود",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = autoDarkMode, onCheckedChange = onAutoDarkModeChange)
            }

            Spacer(Modifier.height(24.dp))

            Text("سایز متن", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FontSizeOption("کوچک", 0.85f, fontScale, onFontScaleChange)
                FontSizeOption("متوسط", 1.0f, fontScale, onFontScaleChange)
                FontSizeOption("بزرگ", 1.3f, fontScale, onFontScaleChange)
                FontSizeOption("خیلی بزرگ", 1.6f, fontScale, onFontScaleChange)
            }

            Spacer(Modifier.height(24.dp))
            Text("فاصله خطوط متن", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            var lineSpacing by remember { mutableStateOf(Prefs.getLineSpacing(context)) }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LineSpacingOption("فشرده", 1.1f, lineSpacing) { lineSpacing = it; Prefs.setLineSpacing(context, it) }
                LineSpacingOption("معمولی", 1.4f, lineSpacing) { lineSpacing = it; Prefs.setLineSpacing(context, it) }
                LineSpacingOption("بازتر", 1.8f, lineSpacing) { lineSpacing = it; Prefs.setLineSpacing(context, it) }
            }
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text(
                    "این یک متن نمونه است تا فاصله‌ی خطوط را همین‌جا ببینید.\nخط دوم نمونه برای مقایسه فاصله با خط بالا.",
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = MaterialTheme.typography.bodyLarge.fontSize * lineSpacing),
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
            Text("تم رنگی", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeOption("طلایی", "default", themeChoice, onThemeChoiceChange, androidx.compose.ui.graphics.Color(0xFFD4A94A))
                ThemeOption("سبز", "green", themeChoice, onThemeChoiceChange, androidx.compose.ui.graphics.Color(0xFF3E8E5A))
                ThemeOption("قرمز", "red", themeChoice, onThemeChoiceChange, androidx.compose.ui.graphics.Color(0xFFA33B3B))
            }

            Spacer(Modifier.height(24.dp))
            Text("فونت متن", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                com.example.bookapp.ui.theme.FontChoiceLabels.forEach { (key, label) ->
                    ThemeOption(label, key, fontChoice, onFontChoiceChange)
                }
            }

            Spacer(Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text("بروزرسانی محتوا", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "اگر محتوای جدیدی به گیت‌هاب اضافه شده، با این دکمه بدون نیاز به نصب دوباره اپ، محتوا به‌روز می‌شود (نیاز به اینترنت دارد).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    syncing = true
                    syncMessage = null
                    scope.launch {
                        val result = onSyncContent()
                        syncing = false
                        syncMessage = if (result.isSuccess) {
                            com.example.bookapp.data.showNewContentNotification(context, 1)
                            "محتوا با موفقیت به‌روزرسانی شد ✅"
                        } else {
                            "خطا در بروزرسانی: ${result.exceptionOrNull()?.message ?: "اتصال اینترنت را بررسی کنید"} ❌"
                        }
                    }
                },
                enabled = !syncing
            ) {
                Text(if (syncing) "در حال بروزرسانی..." else "بروزرسانی محتوا از اینترنت")
            }
            syncMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("روشن نگه‌داشتن صفحه", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "صفحه گوشی حین استفاده از برنامه خاموش/قفل نشود",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = keepScreenOn, onCheckedChange = onKeepScreenOnChange)
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text("تغییر رمز عبور", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "اگر رمزی تنظیم نکنید، ورود به برنامه بدون رمز آزاد خواهد بود.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            ChangePasswordSection()

            Spacer(Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text("پشتیبان‌گیری کامل", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "یادداشت‌ها، علاقه‌مندی‌ها، پاورقی‌ها، نقش‌های «من» و گفتگوهای شما در یک فایل ذخیره می‌شود. " +
                    "از پنجره‌ای که باز می‌شود می‌توانید محل ذخیره را انتخاب کنید: حافظه‌ی داخلی/کارت حافظه‌ی گوشی، " +
                    "یا اگر روی گوشی نصب باشد یک سرویس ابری مثل گوگل‌درایو. می‌توانید یک رمز هم روی فایل بگذارید " +
                    "(چون شامل یادداشت‌های شخصی شماست)؛ در این صورت فقط با همان رمز قابل بازیابی است — اگر رمز را " +
                    "فراموش کنید، هیچ راهی برای بازیابی آن فایل وجود ندارد.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            var backupMessage by remember { mutableStateOf<String?>(null) }
            var pendingAction by remember { mutableStateOf<String?>(null) } // "backup" | "restore"
            var passwordInput by remember { mutableStateOf("") }
            var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

            val createBackupLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/octet-stream")
            ) { uri ->
                if (uri != null) {
                    scope.launch {
                        com.example.bookapp.data.writeBackupToUri(context, db, uri, passwordInput.ifBlank { null })
                        backupMessage = "پشتیبان با موفقیت ذخیره شد ✅"
                        passwordInput = ""
                    }
                }
            }
            val restoreBackupLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    pendingRestoreUri = uri
                    pendingAction = "restore"
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { pendingAction = "backup" }) {
                    Text("گرفتن پشتیبان")
                }
                OutlinedButton(onClick = {
                    restoreBackupLauncher.launch(arrayOf("application/octet-stream", "application/json", "*/*"))
                }) {
                    Text("بازیابی از پشتیبان")
                }
            }
            backupMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall)
            }

            if (pendingAction == "backup") {
                AlertDialog(
                    onDismissRequest = { pendingAction = null; passwordInput = "" },
                    title = { Text("رمز عبور پشتیبان (اختیاری)") },
                    text = {
                        Column {
                            Text("اگر می‌خواهید فایل رمزگذاری شود، رمزی وارد کنید؛ برای بدون‌رمز خالی بگذارید.", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("رمز (اختیاری)") },
                                singleLine = true,
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            pendingAction = null
                            createBackupLauncher.launch("پشتیبان-تعزیه.tazbackup")
                        }) { Text("ادامه") }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingAction = null; passwordInput = "" }) { Text("انصراف") }
                    }
                )
            }

            if (pendingAction == "restore" && pendingRestoreUri != null) {
                AlertDialog(
                    onDismissRequest = { pendingAction = null; passwordInput = ""; pendingRestoreUri = null },
                    title = { Text("رمز عبور پشتیبان") },
                    text = {
                        Column {
                            Text("اگر این فایل هنگام ذخیره رمزگذاری شده، همان رمز را وارد کنید؛ اگر رمزی نداشت، خالی بگذارید.", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("رمز (در صورت وجود)") },
                                singleLine = true,
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val uri = pendingRestoreUri!!
                            val pwd = passwordInput.ifBlank { null }
                            pendingAction = null; passwordInput = ""; pendingRestoreUri = null
                            scope.launch {
                                val result = com.example.bookapp.data.restoreBackupFromUri(context, db, uri, pwd)
                                backupMessage = if (result.isSuccess) {
                                    "بازیابی با موفقیت انجام شد ✅"
                                } else {
                                    "خطا در بازیابی: ${result.exceptionOrNull()?.message ?: "رمز اشتباه است یا فایل نامعتبر است"} ❌"
                                }
                            }
                        }) { Text("بازیابی") }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingAction = null; passwordInput = ""; pendingRestoreUri = null }) { Text("انصراف") }
                    }
                )
            }

            Spacer(Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text("گزارش خطا", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "اگر برنامه اخیراً برایتان کرش کرد (بسته شد)، جزئیاتش اینجا ذخیره شده؛ می‌توانید برای سازنده ارسال کنید.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = {
                val logFile = java.io.File(context.filesDir, "last_crash.txt")
                if (logFile.exists()) {
                    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", logFile)
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "ارسال گزارش خطا"))
                } else {
                    syncMessage = "گزارش خطایی یافت نشد"
                }
            }) {
                Text("ارسال آخرین گزارش خطا")
            }
        }
    }
}

@Composable
private fun FontSizeOption(label: String, value: Float, current: Float, onSelect: (Float) -> Unit) {
    val selected = kotlin.math.abs(current - value) < 0.01f
    FilterChip(
        selected = selected,
        onClick = { onSelect(value) },
        label = { Text(label) }
    )
}

@Composable
private fun LineSpacingOption(label: String, value: Float, current: Float, onSelect: (Float) -> Unit) {
    val selected = kotlin.math.abs(current - value) < 0.01f
    FilterChip(
        selected = selected,
        onClick = { onSelect(value) },
        label = { Text(label) }
    )
}

@Composable
private fun ThemeOption(
    label: String,
    value: String,
    current: String,
    onSelect: (String) -> Unit,
    swatchColor: androidx.compose.ui.graphics.Color? = null
) {
    FilterChip(
        selected = current == value,
        onClick = { onSelect(value) },
        label = { Text(label) },
        leadingIcon = swatchColor?.let {
            {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(it, shape = androidx.compose.foundation.shape.CircleShape)
                )
            }
        }
    )
}

@Composable
private fun ChangePasswordSection() {
    val context = LocalContext.current
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    val hasPasswordSet = Prefs.getAppPassword(context).isNotBlank()

    Column {
        if (hasPasswordSet) {
            OutlinedTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = { Text("رمز فعلی") },
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("رمز جدید (برای غیرفعال‌کردن رمز، خالی بگذارید)") },
            singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("تکرار رمز جدید") },
            singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        message?.let {
            Text(
                it,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
        }
        Button(onClick = {
            val savedPassword = Prefs.getAppPassword(context)
            when {
                savedPassword.isNotBlank() && currentPassword != savedPassword -> {
                    isError = true
                    message = "رمز فعلی درست نیست"
                }
                newPassword != confirmPassword -> {
                    isError = true
                    message = "رمز جدید و تکرار آن یکسان نیستند"
                }
                else -> {
                    Prefs.setAppPassword(context, newPassword)
                    isError = false
                    message = if (newPassword.isBlank()) "رمز عبور غیرفعال شد" else "رمز عبور با موفقیت تغییر کرد ✅"
                    currentPassword = ""
                    newPassword = ""
                    confirmPassword = ""
                }
            }
        }) {
            Text("ذخیره رمز")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ورژن برنامه") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("نسخه برنامه: ${BuildConfig.VERSION_NAME}")
        }
    }
}
