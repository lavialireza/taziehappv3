package com.example.bookapp.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bookapp.data.AudioPlayerHelper
import com.example.bookapp.data.FootnoteEntity
import com.example.bookapp.data.Prefs
import com.example.bookapp.data.SearchResult
import com.example.bookapp.data.SpeechHelper
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextScreen(
    title: String,
    content: String,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    sectionId: Long? = null,
    audioUrl: String? = null,
    relatedSections: List<SearchResult> = emptyList(),
    onRelatedClick: (SearchResult) -> Unit = {},
    footnotes: List<FootnoteEntity> = emptyList(),
    onAddFootnote: (term: String, explanation: String) -> Unit = { _, _ -> },
    onEditFootnote: (FootnoteEntity, term: String, explanation: String) -> Unit = { _, _, _ -> },
    onDeleteFootnote: (FootnoteEntity) -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    hasPrevSection: Boolean = false,
    hasNextSection: Boolean = false,
    onPrevSection: () -> Unit = {},
    onNextSection: () -> Unit = {},
    onAttachAudio: (android.net.Uri) -> Unit = {},
    onRemoveAudio: () -> Unit = {},
    fieldTitle: String? = null,
    taziehTitle: String? = null,
    roleTitle: String? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    var immersive by remember { mutableStateOf(false) }

    fun applyImmersive(on: Boolean) {
        val window = activity?.window ?: return
        val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        if (on) {
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }
    DisposableEffect(Unit) {
        onDispose { applyImmersive(false) } // با خروج از صفحه، نوارهای سیستم برمی‌گردند
    }

    val lineSpacing = remember { Prefs.getLineSpacing(context) }
    var isSpeaking by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val speechHelper = remember {
        SpeechHelper(context) { status ->
            when (status) {
                "no_engine" -> statusMessage = "موتور خواندن صوتی روی این گوشی در دسترس نیست"
                "no_persian_voice" -> statusMessage = "صدای فارسی روی این گوشی نصب نیست (به تنظیمات گوشی مراجعه کنید)"
                "error" -> statusMessage = "خطا در پخش صدا"
            }
            if (status == "done" || status == "error") isSpeaking = false
        }
    }
    val audioPlayerHelper = remember {
        AudioPlayerHelper(context) { status ->
            when (status) {
                "error" -> statusMessage = "خطا در پخش فایل صوتی"
                "done" -> isSpeaking = false
            }
        }
    }
    val audioPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) onAttachAudio(uri)
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            isSpeaking = false
            statusMessage = null
        }
    }
    var tag by remember(sectionId) { mutableStateOf(sectionId?.let { Prefs.getTag(context, it) }) }
    var showTagDialog by remember { mutableStateOf(false) }

    // حالت تمام‌صفحه: نوار بالا هنگام اسکرول به پایین جمع می‌شود
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    DisposableEffect(Unit) {
        onDispose {
            speechHelper.shutdown()
            audioPlayerHelper.stop()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!immersive) {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "جستجو")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "تنظیمات")
                    }
                    IconButton(onClick = {
                        if (isSpeaking) {
                            speechHelper.stop()
                            audioPlayerHelper.stop()
                            isSpeaking = false
                        } else {
                            if (!audioUrl.isNullOrBlank()) {
                                audioPlayerHelper.play(audioUrl)
                            } else {
                                speechHelper.speak(content)
                            }
                            isSpeaking = true
                        }
                    }) {
                        Icon(
                            if (isSpeaking) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = if (isSpeaking) "توقف خواندن" else if (!audioUrl.isNullOrBlank()) "پخش صدای واقعی" else "خواندن صوتی"
                        )
                    }
                    IconButton(onClick = onToggleBookmark) {
                        Icon(
                            if (isBookmarked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "نشان کردن"
                        )
                    }
                    var moreExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { moreExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "بیشتر")
                    }
                    DropdownMenu(expanded = moreExpanded, onDismissRequest = { moreExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(if (immersive) "خروج از حالت تمام‌صفحه" else "حالت مطالعه بدون مزاحمت (تمام‌صفحه)") },
                            onClick = {
                                moreExpanded = false
                                immersive = !immersive
                                applyImmersive(immersive)
                            }
                        )
                        if (sectionId != null) {
                            DropdownMenuItem(
                                text = { Text("برچسب شخصی") },
                                onClick = { moreExpanded = false; showTagDialog = true }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("کپی متن") },
                            onClick = { moreExpanded = false; copyToClipboard(context, title, content) }
                        )
                        DropdownMenuItem(
                            text = { Text("اشتراک‌گذاری") },
                            onClick = { moreExpanded = false; shareText(context, title, content) }
                        )
                        if (sectionId != null) {
                            DropdownMenuItem(
                                text = { Text("اشتراک‌گذاری لینک مستقیم این بخش") },
                                onClick = { moreExpanded = false; shareSectionLink(context, title, sectionId) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("گزارش اشکال در این متن") },
                            onClick = { moreExpanded = false; reportContentIssue(context, title, content, sectionId) }
                        )
                        if (sectionId != null) {
                            DropdownMenuItem(
                                text = { Text(if (audioUrl.isNullOrBlank()) "افزودن صدای واقعی" else "تعویض صدای واقعی") },
                                onClick = { moreExpanded = false; audioPickerLauncher.launch("audio/*") }
                            )
                            if (!audioUrl.isNullOrBlank()) {
                                DropdownMenuItem(
                                    text = { Text("حذف صدای واقعی") },
                                    onClick = { moreExpanded = false; onRemoveAudio() }
                                )
                            }
                        }
                    }
                }
            )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (immersive) Modifier.clickable { immersive = false; applyImmersive(false) } else Modifier)
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
        // روی صفحه‌های بزرگ (تبلت) عرض متن محدود می‌شود تا طول خط زیاد نشود و خواندن راحت بماند
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (!immersive && (fieldTitle != null || taziehTitle != null || roleTitle != null)) {
                val breadcrumb = listOfNotNull(fieldTitle, taziehTitle, roleTitle).joinToString(" ← ")
                val verseCount = content.lineSequence().count { it.isNotBlank() }
                Text(
                    breadcrumb,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "$verseCount بیت",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
            }
            if (!tag.isNullOrBlank()) {
                AssistChip(onClick = { showTagDialog = true }, label = { Text(tag!!) })
                Spacer(Modifier.height(8.dp))
            }
            Text(
                content,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = MaterialTheme.typography.bodyLarge.fontSize * lineSpacing
                )
            )

            if (hasPrevSection || hasNextSection) {
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = onPrevSection, enabled = hasPrevSection) {
                        Text("◀ بخش قبل")
                    }
                    OutlinedButton(onClick = onNextSection, enabled = hasNextSection) {
                        Text("بخش بعد ▶")
                    }
                }
            }

            if (sectionId != null) {
                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                FootnotesSection(
                    footnotes = footnotes,
                    onAdd = onAddFootnote,
                    onEdit = onEditFootnote,
                    onDelete = onDeleteFootnote
                )
            }

            if (relatedSections.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                Text("بخش‌های مرتبط (هم‌نام)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                relatedSections.forEach { related ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = { onRelatedClick(related) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(related.roleTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "${related.taziehTitle} · ${related.fieldTitle}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }

    if (showTagDialog && sectionId != null) {
        var input by remember { mutableStateOf(tag ?: "") }
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            title = { Text("برچسب شخصی") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("مثلاً: حفظ کنم، برای مجلس بعدی") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    Prefs.setTag(context, sectionId, input)
                    tag = input.ifBlank { null }
                    showTagDialog = false
                }) { Text("ذخیره") }
            },
            dismissButton = {
                TextButton(onClick = { showTagDialog = false }) { Text("انصراف") }
            }
        )
    }
}

private fun copyToClipboard(context: Context, title: String, content: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(title, content)
    clipboard.setPrimaryClip(clip)
}

private fun shareText(context: Context, title: String, content: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, "$title\n\n$content")
    }
    context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری"))
}

private fun shareSectionLink(context: Context, title: String, sectionId: Long) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, "«$title» را در اپلیکیشن تعزیه ببینید:\ntaziehapp://section/$sectionId")
    }
    context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری لینک"))
}

/**
 * یک ایمیل با متن بخش و شناسه‌اش آماده می‌کند تا کاربر بتواند غلط تایپی یا
 * اشکال احتمالی حاصل از تبدیل Word→JSON را به سازنده‌ی برنامه گزارش دهد.
 * جای‌گذارنده‌ی [ایمیل خودتان را اینجا بنویسید] باید با ایمیل واقعی جایگزین شود.
 */
private fun reportContentIssue(context: Context, title: String, content: String, sectionId: Long?) {
    val body = buildString {
        appendLine("توضیح اشکال (لطفاً اینجا بنویسید):")
        appendLine()
        appendLine("——————————")
        appendLine("عنوان بخش: $title")
        if (sectionId != null) appendLine("شناسه بخش: $sectionId")
        appendLine("متن فعلی:")
        appendLine(content)
    }
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = android.net.Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf("your-email@example.com"))
        putExtra(Intent.EXTRA_SUBJECT, "گزارش اشکال محتوا: $title")
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // اگر هیچ اپ ایمیلی نصب نبود، به‌جایش اشتراک‌گذاری عمومی نشان می‌دهیم
        shareText(context, "گزارش اشکال: $title", body)
    }
}

/**
 * پاورقی: توضیح واژه‌ها/عبارت‌های یک بخش (معنی لغت، توضیح مختصر، منبع و ...).
 * کاملاً توسط خود کاربر نوشته، ذخیره و ویرایش می‌شود؛ چیزی از پیش تولید نمی‌شود.
 */
@Composable
private fun FootnotesSection(
    footnotes: List<FootnoteEntity>,
    onAdd: (term: String, explanation: String) -> Unit,
    onEdit: (FootnoteEntity, term: String, explanation: String) -> Unit,
    onDelete: (FootnoteEntity) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<FootnoteEntity?>(null) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text("پاورقی", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        TextButton(onClick = { editing = null; showDialog = true }) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("افزودن پاورقی")
        }
    }

    if (footnotes.isEmpty()) {
        Text(
            "هنوز پاورقی‌ای برای این بخش ثبت نشده (مثلاً معنی یک واژه، توضیح مختصر یا منبع).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        footnotes.forEach { fn ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.Top
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(fn.term, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Text(fn.explanation, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { editing = fn; showDialog = true }) {
                        Icon(Icons.Filled.Label, contentDescription = "ویرایش پاورقی")
                    }
                    IconButton(onClick = { onDelete(fn) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "حذف پاورقی")
                    }
                }
            }
        }
    }

    if (showDialog) {
        var term by remember { mutableStateOf(editing?.term ?: "") }
        var explanation by remember { mutableStateOf(editing?.explanation ?: "") }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editing == null) "افزودن پاورقی" else "ویرایش پاورقی") },
            text = {
                Column {
                    OutlinedTextField(
                        value = term,
                        onValueChange = { term = it },
                        label = { Text("واژه یا عبارت") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = explanation,
                        onValueChange = { explanation = it },
                        label = { Text("توضیح (معنی، منبع، نکته و ...)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (term.isNotBlank() && explanation.isNotBlank()) {
                        val current = editing
                        if (current == null) onAdd(term.trim(), explanation.trim())
                        else onEdit(current, term.trim(), explanation.trim())
                    }
                    showDialog = false
                }) { Text("ذخیره") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("انصراف") }
            }
        )
    }
}

/**
 * حالت مطالعه حرفه‌ای: امکان سوایپ (کشیدن انگشت) بین بخش‌های یک نقش،
 * بدون نیاز به برگشتن به فهرست بعد از هر بخش.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TextPagerScreen(
    sections: List<com.example.bookapp.data.SectionEntity>,
    startIndex: Int,
    isBookmarked: (Long) -> Boolean,
    onToggleBookmark: (Long) -> Unit,
    onPageShown: (Long) -> Unit,
    onOpenSearch: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onAttachAudio: (sectionId: Long, uri: android.net.Uri) -> Unit = { _, _ -> },
    onRemoveAudio: (sectionId: Long) -> Unit = {},
    fieldTitle: String? = null,
    taziehTitle: String? = null,
    roleTitle: String? = null,
    onBack: () -> Unit
) {
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = startIndex.coerceIn(0, (sections.size - 1).coerceAtLeast(0))
    ) { sections.size }
    val pagerScope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        if (sections.isNotEmpty()) {
            onPageShown(sections[pagerState.currentPage].id)
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (sections.isNotEmpty()) {
            LinearProgressIndicator(
                progress = { (pagerState.currentPage + 1).toFloat() / sections.size.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
        }
        androidx.compose.foundation.pager.HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
        val section = sections[page]
        Column(Modifier.fillMaxSize()) {
            TextScreen(
                title = "${section.title}  (${page + 1}/${sections.size})",
                content = section.content,
                isBookmarked = isBookmarked(section.id),
                onToggleBookmark = { onToggleBookmark(section.id) },
                sectionId = section.id,
                audioUrl = section.audioUrl,
                onOpenSearch = onOpenSearch,
                onOpenSettings = onOpenSettings,
                hasPrevSection = page > 0,
                hasNextSection = page < sections.size - 1,
                onPrevSection = {
                    pagerScope.launch { pagerState.animateScrollToPage(page - 1) }
                },
                onNextSection = {
                    pagerScope.launch { pagerState.animateScrollToPage(page + 1) }
                },
                onAttachAudio = { uri -> onAttachAudio(section.id, uri) },
                onRemoveAudio = { onRemoveAudio(section.id) },
                fieldTitle = fieldTitle,
                taziehTitle = taziehTitle,
                roleTitle = roleTitle,
                onBack = onBack
            )
        }
        }
    }
}
