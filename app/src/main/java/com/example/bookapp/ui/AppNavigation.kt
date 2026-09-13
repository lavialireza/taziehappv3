package com.example.bookapp.ui

import android.content.Intent
import com.example.bookapp.BuildConfig

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bookapp.data.AppDatabase
import com.example.bookapp.data.ContentImportPreview
import com.example.bookapp.data.ContentHealthReport
import com.example.bookapp.data.buildDetailedContentHealthReport
import com.example.bookapp.data.toPersianText
import com.example.bookapp.data.exportContentJson
import com.example.bookapp.data.importContentJson
import com.example.bookapp.data.previewContentImport
import com.example.bookapp.data.readJsonFromUri
import com.example.bookapp.data.WordImportPreview
import com.example.bookapp.data.importWordContent
import com.example.bookapp.data.previewWordImport
import com.example.bookapp.data.readWordFromUri
import com.example.bookapp.data.NoteEntity
import com.example.bookapp.data.Prefs
import com.example.bookapp.data.SearchResult
import com.example.bookapp.data.SectionEntity
import com.example.bookapp.data.syncLocalContentFiles
import com.example.bookapp.data.syncRemoteContent
import com.example.bookapp.ui.screens.*
import kotlinx.coroutines.launch

private const val ROUTE_SPLASH = "splash"
private const val ROUTE_ONBOARDING = "onboarding"
private const val ROUTE_LOGIN = "login"
private const val ROUTE_MAIN_MENU = "main_menu"
private const val ROUTE_VIEWER_CONTENT = "viewer_content"
private const val ROUTE_SEARCH = "search"
private const val ROUTE_BOOKMARKS = "bookmarks"
private const val ROUTE_NOTES = "notes"
private const val ROUTE_MY_ROLE = "my_role"
private const val ROUTE_ALL_IMAGES = "all_images"
private const val ROUTE_REHEARSAL = "rehearsal/{roleId}/{roleTitle}"
private const val ROUTE_ABOUT = "about"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_VERSION = "version"
private const val ROUTE_CHANGELOG = "changelog"
private const val ROUTE_GLOSSARY = "glossary"
private const val ROUTE_MUHARRAM_CALENDAR = "muharram_calendar"
private const val ROUTE_CONTENT_MANAGEMENT = "content_management"
private const val ROUTE_CONTENT_EDITOR = "content_editor"
private const val ROUTE_FIELDS = "fields"
private const val ROUTE_TAZIEHS = "taziehs/{fieldId}/{fieldTitle}"
private const val ROUTE_ROLES = "roles/{taziehId}/{taziehTitle}"
private const val ROUTE_SECTIONS = "sections/{roleId}/{roleTitle}"
private const val ROUTE_TAZIEH_INDEX = "tazieh_index/{taziehId}/{taziehTitle}"
private const val ROUTE_DIALOGUES = "dialogues/{taziehId}/{taziehTitle}"
private const val ROUTE_DIALOGUE_BUILDER = "dialogue_builder/{taziehId}/{taziehTitle}"
private const val ROUTE_DIALOGUE_READER = "dialogue_reader/{dialogueId}"
private const val ROUTE_TAZIEH_GALLERY = "tazieh_gallery/{taziehId}/{taziehTitle}"
private const val ROUTE_TEXT = "text/{sectionId}"
private const val ROUTE_TEXT_PAGER = "text_pager/{roleId}/{startIndex}"
private const val ROUTE_COMPARE = "compare/{taziehId}"

@Composable
fun AppNavigation(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    autoDarkMode: Boolean,
    onAutoDarkModeChange: (Boolean) -> Unit,
    fontScale: Float,
    onFontScaleChange: (Float) -> Unit,
    themeChoice: String,
    onThemeChoiceChange: (String) -> Unit,
    fontChoice: String,
    onFontChoiceChange: (String) -> Unit,
    keepScreenOn: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
    shortcutTarget: String?,
    deepLinkSectionId: Long? = null
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val publicViewer = BuildConfig.PUBLIC_VIEWER
    val navController: NavHostController = rememberNavController()

    LaunchedEffect(Unit) {
        // محتوای همراه APK برای هر دو build بارگذاری می‌شود؛ این مسیر فقط assets
        // داخلی برنامه را می‌خواند و در نسخه عمومی هیچ ابزار ورود/ویرایش در اختیار کاربر نیست.
        val isReturningUser = Prefs.getProcessedContentFiles(context).isNotEmpty()
        val syncResult = runCatching { syncLocalContentFiles(context, db) }
        val newFilesCount = syncResult.getOrElse { error ->
            android.util.Log.e("TaziehContent", "خطا در بارگذاری محتوای همراه برنامه", error)
            0
        }
        if (!publicViewer && isReturningUser && newFilesCount > 0) {
            com.example.bookapp.data.showNewContentNotification(context, newFilesCount)
        }
    }

    // مقصد بعد از ورود: اگر از طریق لینک اشتراک‌گذاری یک بخش خاص باز شده باشد
    // اولویت با آن است؛ وگرنه اگر از میان‌بر آیکون باز شده باشد، به همان مقصد می‌رویم
    fun postLoginRoute(): String = when {
        deepLinkSectionId != null -> "text/$deepLinkSectionId"
        shortcutTarget == "search" -> ROUTE_SEARCH
        shortcutTarget == "notes" -> ROUTE_NOTES
        shortcutTarget == "bookmarks" -> ROUTE_BOOKMARKS
        else -> ROUTE_MAIN_MENU
    }

    // انیمیشن سریع و سبک بین صفحات (نه کند/سنگین)، تا هم نرم باشد و هم سرعت استفاده از برنامه افت نکند
    val navAnimDuration = 220
    NavHost(
        navController = navController,
        startDestination = ROUTE_SPLASH,
        enterTransition = {
            fadeIn(tween(navAnimDuration)) + slideInHorizontally(tween(navAnimDuration)) { it / 6 }
        },
        exitTransition = {
            fadeOut(tween(navAnimDuration)) + slideOutHorizontally(tween(navAnimDuration)) { -it / 6 }
        },
        popEnterTransition = {
            fadeIn(tween(navAnimDuration)) + slideInHorizontally(tween(navAnimDuration)) { -it / 6 }
        },
        popExitTransition = {
            fadeOut(tween(navAnimDuration)) + slideOutHorizontally(tween(navAnimDuration)) { it / 6 }
        }
    ) {

        composable(ROUTE_SPLASH) {
            SplashScreen(onFinished = {
                val next = if (Prefs.isOnboardingShown(context)) ROUTE_LOGIN else ROUTE_ONBOARDING
                navController.navigate(next) {
                    popUpTo(ROUTE_SPLASH) { inclusive = true }
                }
            })
        }

        composable(ROUTE_ONBOARDING) {
            OnboardingScreen(onFinished = {
                Prefs.setOnboardingShown(context)
                navController.navigate(ROUTE_LOGIN) {
                    popUpTo(ROUTE_ONBOARDING) { inclusive = true }
                }
            })
        }

        composable(ROUTE_LOGIN) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(postLoginRoute()) {
                    popUpTo(ROUTE_LOGIN) { inclusive = true }
                }
            })
        }

        composable(ROUTE_MAIN_MENU) {
            var randomVerse by remember { mutableStateOf<SearchResult?>(null) }
            var recentItems by remember { mutableStateOf(listOf<SearchResult>()) }

            LaunchedEffect(Unit) {
                randomVerse = db.searchDao().getRandomSection()
                val ids = Prefs.getRecent(context)
                if (ids.isNotEmpty()) {
                    val fetched = db.searchDao().getByIds(ids)
                    val byId = fetched.associateBy { it.sectionId }
                    recentItems = ids.mapNotNull { byId[it] }
                }
            }

            MainMenuScreen(
                randomVerse = randomVerse,
                recentItems = recentItems,
                onOpenTaziehList = { navController.navigate(ROUTE_FIELDS) },
                onOpenSearch = { navController.navigate(ROUTE_SEARCH) },
                onOpenBookmarks = { navController.navigate(ROUTE_BOOKMARKS) },
                onOpenNotes = { navController.navigate(ROUTE_NOTES) },
                onOpenGallery = { navController.navigate(ROUTE_ALL_IMAGES) },
                onOpenMyRole = { navController.navigate(ROUTE_MY_ROLE) },
                onOpenAbout = { navController.navigate(ROUTE_ABOUT) },
                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                onOpenVersion = { navController.navigate(ROUTE_VERSION) },
                onOpenChangelog = { navController.navigate(ROUTE_CHANGELOG) },
                onOpenGlossary = { navController.navigate(ROUTE_GLOSSARY) },
                onOpenMuharramCalendar = { navController.navigate(ROUTE_MUHARRAM_CALENDAR) },
                showContentManagement = !publicViewer,
                onOpenContentManagement = { if (!publicViewer) navController.navigate(ROUTE_CONTENT_MANAGEMENT) },
                onItemClick = { result -> navController.navigate("text/${result.sectionId}") }
            )
        }

        if (publicViewer) composable(ROUTE_VIEWER_CONTENT) {
            ViewerContentScreen(
                db = db,
                onOpenSection = { sectionId -> navController.navigate("text/$sectionId") },
                onBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_SEARCH) {
            var fields by remember { mutableStateOf(listOf<com.example.bookapp.data.FieldEntity>()) }
            var allTaziehs by remember { mutableStateOf(listOf<com.example.bookapp.data.TaziehEntity>()) }
            var allRoles by remember { mutableStateOf(listOf<com.example.bookapp.data.RoleEntity>()) }
            var allSections by remember { mutableStateOf(listOf<SectionEntity>()) }
            var searchCorpus by remember { mutableStateOf(emptyList<com.example.bookapp.data.SearchCorpusRow>()) }
            var bookmarkedIds by remember { mutableStateOf(Prefs.getBookmarks(context)) }
            LaunchedEffect(Unit) {
                fields = db.fieldDao().getAll()
                allTaziehs = db.taziehDao().getAll()
                allRoles = allTaziehs.flatMap { db.roleDao().getByTazieh(it.id) }
                allSections = db.sectionDao().getAll()
                searchCorpus = db.searchDao().getSearchCorpus()
            }
            SearchScreen(
                fields = fields,
                allTaziehs = allTaziehs,
                allRoles = allRoles,
                allSections = allSections,
                onSearch = { query, options ->
                    com.example.bookapp.data.AdvancedSearchEngine.search(searchCorpus, query, options)
                },
                onResultClick = { result -> navController.navigate("text/${result.sectionId}") },
                onSearchDialogues = { query -> db.searchDao().searchDialogues(query) },
                onDialogueResultClick = { d -> navController.navigate("dialogue_reader/${d.dialogueId}") },
                isBookmarked = { id -> id in bookmarkedIds },
                onToggleBookmark = { id ->
                    Prefs.toggleBookmark(context, id)
                    bookmarkedIds = Prefs.getBookmarks(context)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_BOOKMARKS) {
            var items by remember { mutableStateOf(listOf<SearchResult>()) }
            LaunchedEffect(Unit) {
                val ids = Prefs.getBookmarks(context).toList()
                items = if (ids.isEmpty()) emptyList() else db.searchDao().getByIds(ids)
            }
            BookmarksScreen(
                items = items,
                onItemClick = { result -> navController.navigate("text/${result.sectionId}") },
                onBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_NOTES) {
            var notes by remember { mutableStateOf(listOf<NoteEntity>()) }
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            suspend fun reload() { notes = db.noteDao().getAll() }
            LaunchedEffect(Unit) { reload() }

            NotesScreen(
                notes = notes,
                onAddNote = { title, content ->
                    val note = NoteEntity(title = title, content = content)
                    scope.launch {
                        db.noteDao().insert(note)
                        reload()
                    }
                },
                onUpdateNote = { note, title, content ->
                    scope.launch {
                        db.noteDao().update(note.copy(title = title, content = content))
                        reload()
                    }
                },
                onDeleteNote = { id ->
                    scope.launch {
                        db.noteDao().delete(id)
                        reload()
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_MY_ROLE) {
            var items by remember { mutableStateOf(listOf<MyRoleItem>()) }
            val scope = androidx.compose.runtime.rememberCoroutineScope()

            suspend fun reloadMyRoles() {
                val saved = Prefs.getAllMyRoles(context)
                items = saved.mapNotNull { (taziehId, roleId) ->
                    val tazieh = db.taziehDao().getById(taziehId) ?: return@mapNotNull null
                    val role = try { db.roleDao().getById(roleId) } catch (e: Exception) { null } ?: return@mapNotNull null
                    MyRoleItem(
                        taziehId = taziehId,
                        taziehTitle = tazieh.title,
                        roleId = roleId,
                        roleTitle = role.title
                    )
                }
            }
            LaunchedEffect(Unit) { reloadMyRoles() }

            MyRoleScreen(
                items = items,
                onRead = { item ->
                    navController.navigate("sections/${item.roleId}/${item.roleTitle}")
                },
                onRehearse = { item ->
                    navController.navigate("rehearsal/${item.roleId}/${item.roleTitle}")
                },
                onExportPdf = { item ->
                    scope.launch {
                        val sections = db.sectionDao().getByRole(item.roleId)
                        com.example.bookapp.data.exportRoleToPdf(context, item.roleTitle, sections)
                    }
                },
                onRemove = { item ->
                    Prefs.clearMyRole(context, item.taziehId)
                    scope.launch { reloadMyRoles() }
                },
                onBack = { navController.popBackStack() },
                readOnly = publicViewer
            )
        }

        composable(ROUTE_ALL_IMAGES) {
            var images by remember { mutableStateOf(listOf<GalleryImageItem>()) }
            LaunchedEffect(Unit) {
                val taziehs = db.taziehDao().getAll()
                images = taziehs.flatMap { tazieh ->
                    db.taziehImageDao().getByTazieh(tazieh.id).map { img ->
                        GalleryImageItem(img.id, img.filePath, img.caption, tazieh.title)
                    }
                }
            }
            AllImagesGalleryScreen(
                images = images,
                onBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_REHEARSAL) { backStackEntry ->
            val roleId = backStackEntry.arguments?.getString("roleId")?.toLongOrNull() ?: 0L
            val roleTitle = backStackEntry.arguments?.getString("roleTitle") ?: ""
            var sections by remember { mutableStateOf(listOf<SectionEntity>()) }
            LaunchedEffect(roleId) {
                sections = db.sectionDao().getByRole(roleId)
            }
            RehearsalScreen(
                roleTitle = roleTitle,
                sections = sections,
                onBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_ABOUT) {
            var fieldsCount by remember { mutableIntStateOf(0) }
            var taziehsCount by remember { mutableIntStateOf(0) }
            var rolesCount by remember { mutableIntStateOf(0) }
            var sectionsCount by remember { mutableIntStateOf(0) }
            LaunchedEffect(Unit) {
                fieldsCount = db.searchDao().countFields()
                taziehsCount = db.searchDao().countTaziehs()
                rolesCount = db.searchDao().countRoles()
                sectionsCount = db.searchDao().countSections()
            }
            AboutScreen(
                fieldsCount = fieldsCount,
                taziehsCount = taziehsCount,
                rolesCount = rolesCount,
                sectionsCount = sectionsCount,
                readCount = Prefs.getReadSectionsCount(context),
                streakDays = Prefs.getStreakDays(context),
                activeDaysLast14 = Prefs.getActiveDaysLast(context, 14),
                onBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                darkMode = darkMode,
                onDarkModeChange = onDarkModeChange,
                autoDarkMode = autoDarkMode,
                onAutoDarkModeChange = onAutoDarkModeChange,
                fontScale = fontScale,
                onFontScaleChange = onFontScaleChange,
                fontChoice = fontChoice,
                onFontChoiceChange = onFontChoiceChange,
                themeChoice = themeChoice,
                onThemeChoiceChange = onThemeChoiceChange,
                keepScreenOn = keepScreenOn,
                onKeepScreenOnChange = onKeepScreenOnChange,
                showContentSync = !publicViewer,
                onSyncContent = { syncRemoteContent(db) },
                onCheckAppUpdate = {
                    val installed = com.example.bookapp.data.UpdateHelper.getInstalledVersion(context)
                    com.example.bookapp.data.UpdateHelper.checkForUpdate(installed.buildNumber)
                },
                db = db,
                onBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_VERSION) { VersionScreen(onBack = { navController.popBackStack() }) }

        composable(ROUTE_CHANGELOG) {
            ChangelogScreen(
                entries = listOf(
                    ChangelogEntry("جدید", listOf(
                        "پشتیبان‌گیری کامل (یادداشت، بوکمارک، پاورقی، نقش من، گفتگو) با امکان ذخیره در فضای ابری یا حافظه گوشی و بازیابی",
                        "گزارش اشکال محتوا از داخل هر متن",
                        "تصحیح رنگ تم طلایی در حالت روشن",
                        "امکان روشن/خاموش‌کردن قفل صفحه از تنظیمات",
                        "پس‌زمینه متفاوت برای نقش دوم در مقایسه و گفتگو"
                    )),
                    ChangelogEntry("نسخه‌های قبلی", listOf(
                        "گالری تصاویر (اختصاصی هر تعزیه + گالری عمومی در منوی اصلی)",
                        "صدای واقعی برای بخش‌ها",
                        "گفتگوهای چندنقشی (مثل شمر و عباس)",
                        "فهرست تعزیه با ترتیب قابل ویرایش",
                        "حالت تمرین و نقش من",
                        "پاورقی برای واژه‌ها و توضیحات"
                    ))
                ),
                onBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_GLOSSARY) {
            GlossaryScreen(onBack = { navController.popBackStack() })
        }

        composable(ROUTE_MUHARRAM_CALENDAR) {
            var suggestions by remember { mutableStateOf(listOf<MuharramTaziehSuggestion>()) }
            val countdowns = remember { com.example.bookapp.data.computeMuharramCountdowns() }
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            LaunchedEffect(Unit) {
                if (countdowns != null) {
                    val allTaziehs = db.taziehDao().getAll()
                    val results = mutableListOf<MuharramTaziehSuggestion>()
                    com.example.bookapp.data.MUHARRAM_EVENTS.forEach { event ->
                        allTaziehs.filter { it.title.contains(event.matchKeyword) }.forEach { t ->
                            results.add(MuharramTaziehSuggestion(event.title, t.id, t.title))
                        }
                    }
                    suggestions = results
                }
            }
            MuharramCalendarScreen(
                countdowns = countdowns,
                suggestions = suggestions,
                onOpenTazieh = { taziehId ->
                    scope.launch {
                        val tazieh = db.taziehDao().getById(taziehId)
                        if (tazieh != null) navController.navigate("roles/$taziehId/${tazieh.title}")
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        if (!publicViewer) composable(ROUTE_CONTENT_MANAGEMENT) {
            var fieldsCount by remember { mutableIntStateOf(0) }
            var taziehsCount by remember { mutableIntStateOf(0) }
            var rolesCount by remember { mutableIntStateOf(0) }
            var sectionsCount by remember { mutableIntStateOf(0) }
            var imagesCount by remember { mutableIntStateOf(0) }
            var dialoguesCount by remember { mutableIntStateOf(0) }
            var processedFilesCount by remember { mutableIntStateOf(0) }
            var warnings by remember { mutableStateOf(emptyList<String>()) }
            var busy by remember { mutableStateOf(false) }
            var message by remember { mutableStateOf<String?>(null) }
            var importPreview by remember { mutableStateOf<ContentImportPreview?>(null) }
            var pendingImportJson by remember { mutableStateOf<String?>(null) }
            var wordPreview by remember { mutableStateOf<WordImportPreview?>(null) }
            var pendingWordBytes by remember { mutableStateOf<ByteArray?>(null) }
            var healthReport by remember { mutableStateOf<ContentHealthReport?>(null) }
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            val exportLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                if (uri != null) scope.launch {
                    busy = true
                    try {
                        val json = exportContentJson(db)
                        context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(json) }
                        message = "خروجی JSON سازگار با برنامه جانبی با موفقیت ذخیره شد."
                    } catch (e: Exception) {
                        message = "خطا در خروجی JSON: ${e.message ?: "خطای نامشخص"}"
                    } finally { busy = false }
                }
            }
            val importLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) scope.launch {
                    busy = true
                    try {
                        val json = readJsonFromUri(context, uri)
                        val preview = previewContentImport(db, json)
                        pendingImportJson = if (preview.valid) json else null
                        importPreview = preview
                    } catch (e: Exception) {
                        pendingImportJson = null
                        importPreview = ContentImportPreview(false, listOf("خطا در خواندن فایل: ${e.message ?: "خطای نامشخص"}"))
                    } finally { busy = false }
                }
            }
            val wordImportLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) scope.launch {
                    busy = true
                    try {
                        val bytes = readWordFromUri(context, uri)
                        val (preview, _) = previewWordImport(db, bytes)
                        pendingWordBytes = if (preview.valid) bytes else null
                        wordPreview = preview
                    } catch (e: Exception) {
                        pendingWordBytes = null
                        wordPreview = WordImportPreview(false, listOf("خطا در خواندن فایل Word: ${e.message ?: "خطای نامشخص"}"))
                    } finally { busy = false }
                }
            }

            suspend fun reload() {
                val fields = db.fieldDao().getAll()
                val taziehs = db.taziehDao().getAll()
                val roles = taziehs.flatMap { db.roleDao().getByTazieh(it.id) }
                val sections = db.sectionDao().getAll()
                val images = taziehs.sumOf { db.taziehImageDao().getByTazieh(it.id).size }
                val dialogues = taziehs.sumOf { db.dialogueDao().getByTazieh(it.id).size }
                fieldsCount = fields.size
                taziehsCount = taziehs.size
                rolesCount = roles.size
                sectionsCount = sections.size
                imagesCount = images
                dialoguesCount = dialogues
                processedFilesCount = Prefs.getProcessedContentFiles(context).size
                warnings = buildList {
                    if (fields.any { it.title.isBlank() }) add("یک یا چند زمینه بدون عنوان است")
                    if (taziehs.any { it.title.isBlank() }) add("یک یا چند تعزیه بدون عنوان است")
                    if (roles.any { it.title.isBlank() }) add("یک یا چند نقش بدون عنوان است")
                    if (sections.any { it.content.isBlank() }) add("یک یا چند بخش بدون متن است")
                    if (fields.any { it.uid.isBlank() } || taziehs.any { it.uid.isBlank() } || roles.any { it.uid.isBlank() } || sections.any { it.uid.isBlank() }) add("یک یا چند رکورد شناسه پایدار ندارد")
                }
            }
            LaunchedEffect(Unit) { reload() }

            ContentManagementScreen(
                fieldsCount = fieldsCount,
                taziehsCount = taziehsCount,
                rolesCount = rolesCount,
                sectionsCount = sectionsCount,
                imagesCount = imagesCount,
                dialoguesCount = dialoguesCount,
                healthWarnings = warnings,
                processedFilesCount = processedFilesCount,
                busy = busy,
                message = message,
                onRefresh = { scope.launch { reload() } },
                onSyncLocal = {
                    scope.launch {
                        busy = true
                        message = null
                        try {
                            val count = syncLocalContentFiles(context, db)
                            reload()
                            message = if (count == 0) "محتوای جدیدی برای اضافه‌کردن وجود ندارد." else "$count فایل محتوایی بررسی و به‌روزرسانی شد."
                        } catch (e: Exception) {
                            message = "خطا در به‌روزرسانی محتوا: ${e.message ?: "خطای نامشخص"}"
                        } finally { busy = false }
                    }
                },
                onSyncRemote = {
                    scope.launch {
                        busy = true
                        message = null
                        try {
                            val result = syncRemoteContent(db)
                            reload()
                            message = result.fold({ "محتوای آنلاین با موفقیت همگام شد." }, { "خطا در همگام‌سازی آنلاین: ${it.message ?: "خطای نامشخص"}" })
                        } finally { busy = false }
                    }
                },
                onOpenEditor = { navController.navigate(ROUTE_CONTENT_EDITOR) },
                onImportJson = { importLauncher.launch(arrayOf("application/json", "text/json", "text/plain")) },
                onExportJson = { exportLauncher.launch("tazieh-content-compatible.json") },
                onImportWord = { wordImportLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/octet-stream")) },
                onDetailedHealthCheck = { scope.launch { busy = true; try { healthReport = buildDetailedContentHealthReport(db) } finally { busy = false } } },
                onExportHealthReport = {
                    scope.launch {
                        busy = true
                        try {
                            val report = healthReport ?: buildDetailedContentHealthReport(db).also { healthReport = it }
                            val text = report.toPersianText()
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                                putExtra(Intent.EXTRA_SUBJECT, "گزارش سلامت محتوا")
                            }, "اشتراک‌گذاری گزارش"))
                        } finally {
                            busy = false
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )

            val preview = importPreview
            if (preview != null) {
                AlertDialog(
                    onDismissRequest = { importPreview = null; pendingImportJson = null },
                    title = { Text(if (preview.valid) "پیش‌نمایش ورود JSON" else "فایل JSON نامعتبر") },
                    text = {
                        if (!preview.valid) {
                            Column { preview.errors.take(10).forEach { Text("• $it") } }
                        } else {
                            Column {
                                Text("قالب دقیق برنامه جانبی تأیید شد.")
                                Spacer(Modifier.height(8.dp))
                                Text("زمینه: ${preview.fields}")
                                Text("تعزیه: ${preview.taziehs}")
                                Text("نقش: ${preview.roles}")
                                Text("بخش: ${preview.sections}")
                                Spacer(Modifier.height(8.dp))
                                Text("موجود/قابل‌به‌روزرسانی: ${preview.existingItems}")
                                Text("جدید: ${preview.newItems}")
                                Spacer(Modifier.height(8.dp))
                                Text("فقط ساختار محتوا و متن بخش‌ها وارد می‌شود؛ اطلاعات شخصی، یادداشت‌ها، بوکمارک‌ها، تصاویر و گفتگوهای محلی دست‌کاری نمی‌شوند.")
                            }
                        }
                    },
                    confirmButton = {
                        if (preview.valid && pendingImportJson != null) {
                            TextButton(onClick = {
                                val json = pendingImportJson
                                importPreview = null
                                pendingImportJson = null
                                if (json != null) scope.launch {
                                    busy = true
                                    try {
                                        val result = importContentJson(db, json)
                                        reload()
                                        message = "ورود JSON با موفقیت انجام شد: ${result.newItems} مورد جدید و ${result.existingItems} مورد موجود/قابل‌به‌روزرسانی."
                                    } catch (e: Exception) {
                                        message = "خطا در ورود JSON: ${e.message ?: "خطای نامشخص"}"
                                    } finally { busy = false }
                                }
                            }) { Text("تأیید و ورود") }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { importPreview = null; pendingImportJson = null }) { Text("انصراف") }
                    }
                )
            }

            val wp = wordPreview
            if (wp != null) {
                AlertDialog(
                    onDismissRequest = { wordPreview = null; pendingWordBytes = null },
                    title = { Text(if (wp.valid) "پیش‌نمایش ورود Word" else "فایل Word نامعتبر") },
                    text = {
                        if (!wp.valid) {
                            Column { wp.errors.take(10).forEach { Text("• $it") } }
                        } else {
                            Column {
                                Text("ساختار Word مطابق قرارداد ورود محتوای برنامه شناسایی شد.")
                                Spacer(Modifier.height(8.dp))
                                Text("Heading 1 → زمینه")
                                Text("Heading 2 → تعزیه")
                                Text("Heading 3 → نقش")
                                Text("Heading 4 → بخش")
                                Text("پاراگراف‌های معمولی زیر Heading 4 → متن بخش")
                                Spacer(Modifier.height(8.dp))
                                Text("زمینه: ${wp.fields} | تعزیه: ${wp.taziehs}")
                                Text("نقش: ${wp.roles} | بخش: ${wp.sections}")
                                Text("پاراگراف‌های متن: ${wp.paragraphs}")
                                Text("موجود/قابل‌به‌روزرسانی: ${wp.existingItems}")
                                Text("جدید: ${wp.newItems}")
                                Spacer(Modifier.height(8.dp))
                                Text("فقط ساختار محتوا و متن وارد می‌شود؛ یادداشت‌ها، نشانک‌ها، تصاویر، گفتگوها و اطلاعات شخصی تغییر نمی‌کنند.")
                            }
                        }
                    },
                    confirmButton = {
                        if (wp.valid && pendingWordBytes != null) {
                            TextButton(onClick = {
                                val bytes = pendingWordBytes
                                wordPreview = null
                                pendingWordBytes = null
                                if (bytes != null) scope.launch {
                                    busy = true
                                    try {
                                        val result = importWordContent(db, bytes)
                                        reload()
                                        message = "ورود Word با موفقیت انجام شد: ${result.newItems} مورد جدید و ${result.existingItems} مورد موجود/قابل‌به‌روزرسانی."
                                    } catch (e: Exception) {
                                        message = "خطا در ورود Word: ${e.message ?: "خطای نامشخص"}"
                                    } finally { busy = false }
                                }
                            }) { Text("تأیید و ورود") }
                        }
                    },
                    dismissButton = { TextButton(onClick = { wordPreview = null; pendingWordBytes = null }) { Text("انصراف") } }
                )
            }

            val hr = healthReport
            if (hr != null) {
                AlertDialog(
                    onDismissRequest = { healthReport = null },
                    title = { Text("گزارش عمیق سلامت محتوا") },
                    text = {
                        Column(Modifier.heightIn(max = 520.dp)) {
                            Text("بررسی‌شده: ${hr.checkedFields} زمینه، ${hr.checkedTaziehs} تعزیه، ${hr.checkedRoles} نقش، ${hr.checkedSections} بخش")
                            Spacer(Modifier.height(8.dp))
                            Text("بدون عنوان: ${hr.emptyTitles} | بدون متن: ${hr.emptyTexts}")
                            Text("عنوان تکراری: ${hr.duplicateTitles} | مشکل ترتیب: ${hr.orderIssues}")
                            Text("آدرس صوت مشکوک: ${hr.invalidAudio}")
                            Spacer(Modifier.height(10.dp))
                            if (hr.errors.isEmpty() && hr.warnings.isEmpty()) {
                                Text("✓ هیچ مورد قابل‌توجهی پیدا نشد.", color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                            } else {
                                if (hr.errors.isNotEmpty()) {
                                    Text("خطاها", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    hr.errors.take(12).forEach { Text("• $it", style = androidx.compose.material3.MaterialTheme.typography.bodySmall) }
                                }
                                if (hr.warnings.isNotEmpty()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text("هشدارها", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    hr.warnings.take(16).forEach { Text("• $it", style = androidx.compose.material3.MaterialTheme.typography.bodySmall) }
                                }
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { healthReport = null }) { Text("بستن") } }
                )
            }
        }

        if (!publicViewer) composable(ROUTE_CONTENT_EDITOR) {
            ContentEditorScreen(
                db = db,
                onBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_FIELDS) {
            var fields by remember { mutableStateOf(emptyList<FieldCatalogItem>()) }
            LaunchedEffect(Unit) {
                val allFields = db.fieldDao().getAll()
                fields = allFields.map { field ->
                    FieldCatalogItem(
                        id = field.id,
                        title = field.title,
                        taziehCount = db.taziehDao().getByField(field.id).size
                    )
                }
            }
            FieldsScreen(
                items = fields,
                onOpen = { field -> navController.navigate("taziehs/${field.id}/${field.title}") },
                onBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_TAZIEHS) { backStackEntry ->
            val fieldId = backStackEntry.arguments?.getString("fieldId")?.toLongOrNull() ?: 0L
            var catalog by remember { mutableStateOf(emptyList<TaziehCatalogItem>()) }
            LaunchedEffect(fieldId) {
                val taziehs = db.taziehDao().getByField(fieldId)
                val fieldTitle = db.fieldDao().getAll().firstOrNull { it.id == fieldId }?.title ?: "زمینه"
                catalog = taziehs.map { t ->
                    val roles = db.roleDao().getByTazieh(t.id)
                    val hasAudio = roles.any { r -> db.sectionDao().getByRole(r.id).any { !it.audioUrl.isNullOrBlank() } }
                    TaziehCatalogItem(t.id, fieldId, fieldTitle, t.title, t.author, roles.size, hasAudio)
                }
            }
            TaziehCatalogScreen(
                items = catalog,
                initialFieldId = fieldId,
                onOpen = { item -> navController.navigate("roles/${item.id}/${item.title}") },
                onBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_ROLES) { backStackEntry ->
            val taziehId = backStackEntry.arguments?.getString("taziehId")?.toLongOrNull() ?: 0L
            val taziehTitle = backStackEntry.arguments?.getString("taziehTitle") ?: ""
            var roles by remember { mutableStateOf(listOf<com.example.bookapp.data.RoleEntity>()) }
            var myRoleId by remember { mutableStateOf<Long?>(null) }
            var roleItems by remember { mutableStateOf(listOf<ProfessionalRoleItem>()) }
            val scope = androidx.compose.runtime.rememberCoroutineScope()

            suspend fun reloadRoles() {
                roles = db.roleDao().getByTazieh(taziehId)
                myRoleId = Prefs.getMyRole(context, taziehId)
                roleItems = roles.map { role ->
                    val sections = db.sectionDao().getByRole(role.id)
                    ProfessionalRoleItem(
                        id = role.id,
                        title = role.title,
                        sectionCount = sections.size,
                        firstVerse = sections.firstOrNull()?.content?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim() ?: "",
                        isMine = role.id == myRoleId
                    )
                }
            }
            LaunchedEffect(taziehId) { reloadRoles() }

            ProfessionalRoleScreen(
                taziehTitle = taziehTitle,
                items = roleItems,
                onOpen = { item -> navController.navigate("sections/${item.id}/${item.title}") },
                onSetMine = { item ->
                    Prefs.setMyRole(context, taziehId, item.id)
                    myRoleId = item.id
                    scope.launch { reloadRoles() }
                },
                onCompare = { navController.navigate("compare/$taziehId") },
                onBack = { navController.popBackStack() },
                readOnly = publicViewer
            )
        }

        composable(ROUTE_TAZIEH_INDEX) { backStackEntry ->
            val taziehId = backStackEntry.arguments?.getString("taziehId")?.toLongOrNull() ?: 0L
            val taziehTitle = backStackEntry.arguments?.getString("taziehTitle") ?: ""
            var indexItems by remember { mutableStateOf(listOf<TaziehIndexItem>()) }
            var taziehAuthor by remember { mutableStateOf<String?>(null) }
            var taziehAuthorEmail by remember { mutableStateOf<String?>(null) }
            val scope = androidx.compose.runtime.rememberCoroutineScope()

            suspend fun reloadIndex() {
                val roles = db.roleDao().getByTazieh(taziehId)
                indexItems = roles.map { role ->
                    val firstSection = db.sectionDao().getByRole(role.id).firstOrNull()
                    val firstVerse = firstSection?.content
                        ?.lineSequence()
                        ?.firstOrNull { it.isNotBlank() }
                        ?.trim() ?: ""
                    TaziehIndexItem(roleId = role.id, roleTitle = role.title, firstVerse = firstVerse)
                }
                val tazieh = db.taziehDao().getById(taziehId)
                taziehAuthor = tazieh?.author
                taziehAuthorEmail = tazieh?.authorEmail
            }
            LaunchedEffect(taziehId) { reloadIndex() }

            TaziehIndexScreen(
                taziehTitle = taziehTitle,
                author = taziehAuthor,
                authorEmail = taziehAuthorEmail,
                items = indexItems,
                onItemClick = { item -> navController.navigate("text_pager/${item.roleId}/0") },
                onExportPdf = {
                    scope.launch {
                        val roles = db.roleDao().getByTazieh(taziehId)
                        val rolesWithSections = roles.map { role ->
                            role.title to db.sectionDao().getByRole(role.id)
                        }
                        com.example.bookapp.data.exportTaziehToPdf(context, taziehTitle, rolesWithSections)
                    }
                },
                onRename = { item, newTitle ->
                    scope.launch {
                        db.roleDao().updateTitle(item.roleId, newTitle)
                        reloadIndex()
                    }
                },
                onMove = { index, direction ->
                    scope.launch {
                        val sorted = sortTaziehIndexItems(indexItems)
                        val targetIndex = index + direction
                        if (targetIndex in sorted.indices) {
                            val roleA = db.roleDao().getById(sorted[index].roleId)
                            val roleB = db.roleDao().getById(sorted[targetIndex].roleId)
                            db.roleDao().updateOrderIndex(roleA.id, roleB.orderIndex)
                            db.roleDao().updateOrderIndex(roleB.id, roleA.orderIndex)
                            reloadIndex()
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_DIALOGUES) { backStackEntry ->
            val taziehId = backStackEntry.arguments?.getString("taziehId")?.toLongOrNull() ?: 0L
            val taziehTitle = backStackEntry.arguments?.getString("taziehTitle") ?: ""
            var dialogues by remember { mutableStateOf(listOf<DialogueSummary>()) }
            val scope = androidx.compose.runtime.rememberCoroutineScope()

            suspend fun reloadDialogues() {
                dialogues = db.dialogueDao().getByTazieh(taziehId).map { d ->
                    DialogueSummary(d.id, d.title, db.dialogueTurnDao().getByDialogue(d.id).size)
                }
            }
            LaunchedEffect(taziehId) { reloadDialogues() }

            DialoguesScreen(
                taziehTitle = taziehTitle,
                dialogues = dialogues,
                onOpenDialogue = { d -> navController.navigate("dialogue_reader/${d.id}") },
                onEditDialogue = { d, title ->
                    scope.launch { db.dialogueDao().updateTitle(d.id, title); reloadDialogues() }
                },
                onDeleteDialogue = { d ->
                    scope.launch {
                        db.dialogueDao().delete(d.id)
                        reloadDialogues()
                    }
                },
                onCreateNew = { if (!publicViewer) navController.navigate("dialogue_builder/$taziehId/$taziehTitle") },
                readOnly = publicViewer,
                onBack = { navController.popBackStack() }
            )
        }

        if (!publicViewer) composable(ROUTE_DIALOGUE_BUILDER) { backStackEntry ->
            val taziehId = backStackEntry.arguments?.getString("taziehId")?.toLongOrNull() ?: 0L
            val taziehTitle = backStackEntry.arguments?.getString("taziehTitle") ?: ""
            var allSections by remember { mutableStateOf(listOf<SectionPickerItem>()) }
            val scope = androidx.compose.runtime.rememberCoroutineScope()

            LaunchedEffect(taziehId) {
                val roles = db.roleDao().getByTazieh(taziehId)
                allSections = roles.flatMap { role ->
                    db.sectionDao().getByRole(role.id).map { section ->
                        SectionPickerItem(section.id, role.title, section.title)
                    }
                }
            }

            DialogueBuilderScreen(
                allSections = allSections,
                onSave = { title, orderedSectionIds ->
                    scope.launch {
                        val dialogueId = db.dialogueDao().insert(com.example.bookapp.data.DialogueEntity(taziehId = taziehId, title = title))
                        orderedSectionIds.forEachIndexed { index, sectionId ->
                            db.dialogueTurnDao().insert(
                                com.example.bookapp.data.DialogueTurnEntity(dialogueId = dialogueId, sectionId = sectionId, orderIndex = index)
                            )
                        }
                        navController.popBackStack()
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_DIALOGUE_READER) { backStackEntry ->
            val dialogueId = backStackEntry.arguments?.getString("dialogueId")?.toLongOrNull() ?: 0L
            var dialogueTitle by remember { mutableStateOf("") }
            var turns by remember { mutableStateOf(listOf<DialogueTurnDisplay>()) }
            var allSections by remember { mutableStateOf(listOf<SectionPickerItem>()) }
            var showAddTurn by remember { mutableStateOf(false) }
            val scope = androidx.compose.runtime.rememberCoroutineScope()

            suspend fun reloadTurns() {
                val dialogue = db.dialogueDao().getById(dialogueId)
                dialogueTitle = dialogue.title
                val turnEntities = db.dialogueTurnDao().getByDialogue(dialogueId)
                turns = turnEntities.map { turn ->
                    val section = db.sectionDao().getById(turn.sectionId)
                    val role = db.roleDao().getById(section.roleId)
                    DialogueTurnDisplay(
                        turnId = turn.id,
                        sectionId = section.id,
                        roleTitle = role.title,
                        sectionTitle = section.title,
                        content = section.content
                    )
                }
            }
            LaunchedEffect(dialogueId) {
                reloadTurns()
                val dialogue = db.dialogueDao().getById(dialogueId)
                val roles = db.roleDao().getByTazieh(dialogue.taziehId)
                allSections = roles.flatMap { role -> db.sectionDao().getByRole(role.id).map { section -> SectionPickerItem(section.id, role.title, section.title) } }
            }

            DialogueReaderScreen(
                dialogueTitle = dialogueTitle,
                turns = turns,
                onMoveTurn = { index, direction ->
                    scope.launch {
                        val turnEntities = db.dialogueTurnDao().getByDialogue(dialogueId)
                        val targetIndex = index + direction
                        if (targetIndex in turnEntities.indices) {
                            val a = turnEntities[index]
                            val b = turnEntities[targetIndex]
                            db.dialogueTurnDao().updateOrderIndex(a.id, b.orderIndex)
                            db.dialogueTurnDao().updateOrderIndex(b.id, a.orderIndex)
                            reloadTurns()
                        }
                    }
                },
                onDeleteTurn = { turn ->
                    scope.launch {
                        db.dialogueTurnDao().deleteTurn(turn.turnId)
                        reloadTurns()
                    }
                },
                onAddTurn = { showAddTurn = true },
                onExportPdf = {
                    scope.launch {
                        val triples = turns.map { Triple(it.roleTitle, it.sectionTitle, it.content) }
                        com.example.bookapp.data.exportDialogueToPdf(context, dialogueTitle, triples)
                    }
                },
                onBack = { navController.popBackStack() },
                readOnly = publicViewer
            )
            if (showAddTurn && !publicViewer) {
                var chosen by remember { mutableStateOf<SectionPickerItem?>(null) }
                AlertDialog(
                    onDismissRequest = { showAddTurn = false },
                    title = { Text("افزودن نوبت به گفتگو") },
                    text = {
                        androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max = 420.dp)) {
                            items(allSections, key = { it.sectionId }) { item ->
                                ListItem(
                                    headlineContent = { Text(item.sectionTitle) },
                                    supportingContent = { Text(item.roleTitle) },
                                    modifier = Modifier.fillMaxWidth().clickable { chosen = item }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(enabled = chosen != null, onClick = {
                            val sectionId = chosen?.sectionId
                            if (sectionId != null) scope.launch {
                                val current = db.dialogueTurnDao().getByDialogue(dialogueId)
                                db.dialogueTurnDao().insert(com.example.bookapp.data.DialogueTurnEntity(dialogueId = dialogueId, sectionId = sectionId, orderIndex = current.size))
                                reloadTurns(); showAddTurn = false
                            }
                        }) { Text("افزودن") }
                    },
                    dismissButton = { TextButton(onClick = { showAddTurn = false }) { Text("انصراف") } }
                )
            }
        }

        composable(ROUTE_TAZIEH_GALLERY) { backStackEntry ->
            val taziehId = backStackEntry.arguments?.getString("taziehId")?.toLongOrNull() ?: 0L
            val taziehTitle = backStackEntry.arguments?.getString("taziehTitle") ?: ""
            var images by remember { mutableStateOf(listOf<TaziehImageItem>()) }
            val scope = androidx.compose.runtime.rememberCoroutineScope()

            suspend fun reloadImages() {
                images = db.taziehImageDao().getByTazieh(taziehId).map {
                    TaziehImageItem(it.id, it.filePath, it.caption)
                }
            }
            LaunchedEffect(taziehId) { reloadImages() }

            TaziehGalleryScreen(
                taziehTitle = taziehTitle,
                images = images,
                onAddImage = { uri ->
                    scope.launch {
                        val path = com.example.bookapp.data.copyImageToAppStorage(context, uri)
                        if (path != null) {
                            db.taziehImageDao().insert(com.example.bookapp.data.TaziehImageEntity(taziehId = taziehId, filePath = path))
                            reloadImages()
                        }
                    }
                },
                onDeleteImage = { image ->
                    scope.launch {
                        db.taziehImageDao().delete(image.id)
                        com.example.bookapp.data.deleteImageFromAppStorage(image.filePath)
                        reloadImages()
                    }
                },
                onUpdateCaption = { image, caption ->
                    scope.launch {
                        db.taziehImageDao().updateCaption(image.id, caption)
                        reloadImages()
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_COMPARE) { backStackEntry ->
            val taziehId = backStackEntry.arguments?.getString("taziehId")?.toLongOrNull() ?: 0L
            var taziehTitle by remember { mutableStateOf("") }
            var roles by remember { mutableStateOf(listOf<com.example.bookapp.data.RoleEntity>()) }
            var compareSections by remember { mutableStateOf(listOf<CompareSectionItem>()) }
            var roleSections by remember { mutableStateOf(emptyMap<Long, List<SectionEntity>>()) }
            LaunchedEffect(taziehId) {
                val tazieh = db.taziehDao().getById(taziehId)
                taziehTitle = tazieh?.title.orEmpty()
                roles = db.roleDao().getByTazieh(taziehId)
                val loadedSections = roles.associate { role -> role.id to db.sectionDao().getByRole(role.id) }
                roleSections = loadedSections
                compareSections = roles.flatMap { role ->
                    loadedSections[role.id].orEmpty().map { section -> CompareSectionItem(section, role.title) }
                }
            }
            CompareScreen(
                taziehTitle = taziehTitle.ifBlank { "تعزیه" },
                roles = roles,
                sections = compareSections,
                roleSections = roleSections,
                onBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_SECTIONS) { backStackEntry ->
            val roleId = backStackEntry.arguments?.getString("roleId")?.toLongOrNull() ?: 0L
            val roleTitle = backStackEntry.arguments?.getString("roleTitle") ?: ""
            var items by remember { mutableStateOf(listOf<ListItemData>()) }
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            LaunchedEffect(roleId) {
                items = db.sectionDao().getByRole(roleId).map { ListItemData(it.id, it.title) }
            }
            var taziehIdForCompare by remember { mutableStateOf<Long?>(null) }
            LaunchedEffect(roleId) {
                taziehIdForCompare = db.roleDao().getById(roleId).taziehId
            }
            GenericListScreen(
                screenTitle = roleTitle,
                items = items,
                onItemClick = { clicked ->
                    val index = items.indexOfFirst { it.id == clicked.id }.coerceAtLeast(0)
                    navController.navigate("text_pager/$roleId/$index")
                },
                onBack = { navController.popBackStack() },
                topBarAction = {
                    TextButton(
                        onClick = { taziehIdForCompare?.let { navController.navigate("compare/$it") } },
                        enabled = taziehIdForCompare != null
                    ) { Text("مقایسه") }
                },
                floatingAction = {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                        androidx.compose.material3.ExtendedFloatingActionButton(
                            text = { androidx.compose.material3.Text("حالت تمرین") },
                            icon = { androidx.compose.material3.Icon(Icons.Filled.School, contentDescription = null) },
                            onClick = { navController.navigate("rehearsal/$roleId/$roleTitle") }
                        )
                        if (!publicViewer) {
                            Spacer(Modifier.height(10.dp))
                            androidx.compose.material3.ExtendedFloatingActionButton(
                                text = { androidx.compose.material3.Text("خروجی PDF") },
                                icon = { androidx.compose.material3.Icon(Icons.Filled.Share, contentDescription = null) },
                                onClick = {
                                    scope.launch {
                                        val fullSections = db.sectionDao().getByRole(roleId)
                                        com.example.bookapp.data.exportRoleToPdf(context, roleTitle, fullSections)
                                    }
                                }
                            )
                        }
                    }
                }
            )
        }

        composable(ROUTE_TEXT_PAGER) { backStackEntry ->
            val roleId = backStackEntry.arguments?.getString("roleId")?.toLongOrNull() ?: 0L
            val startIndex = backStackEntry.arguments?.getString("startIndex")?.toIntOrNull() ?: 0
            var sections by remember { mutableStateOf(listOf<SectionEntity>()) }
            var bookmarkVersion by remember { mutableIntStateOf(0) }
            var breadcrumb by remember { mutableStateOf(Triple<String?, String?, String?>(null, null, null)) }
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            LaunchedEffect(roleId) {
                sections = db.sectionDao().getByRole(roleId)
                val role = db.roleDao().getById(roleId)
                val tazieh = db.taziehDao().getById(role.taziehId)
                val fieldEntity = tazieh?.let { t -> db.fieldDao().getAll().find { it.id == t.fieldId } }
                breadcrumb = Triple(fieldEntity?.title, tazieh?.title, role.title)
            }
            if (sections.isNotEmpty()) {
                TextPagerScreen(
                    sections = sections,
                    startIndex = startIndex,
                    isBookmarked = { id -> bookmarkVersion.let { Prefs.isBookmarked(context, id) } },
                    onToggleBookmark = { id ->
                        Prefs.toggleBookmark(context, id)
                        bookmarkVersion++
                    },
                    onPageShown = { id ->
                        Prefs.addRecent(context, id)
                        Prefs.markSectionRead(context, id)
                    },
                    onOpenSearch = { navController.navigate(ROUTE_SEARCH) },
                    onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                    onAttachAudio = { sectionId, uri ->
                        scope.launch {
                            val path = com.example.bookapp.data.copyAudioToAppStorage(context, uri)
                            if (path != null) {
                                db.sectionDao().updateAudioUrl(sectionId, path)
                                sections = db.sectionDao().getByRole(roleId)
                            }
                        }
                    },
                    onRemoveAudio = { sectionId ->
                        scope.launch {
                            sections.find { it.id == sectionId }?.audioUrl?.let {
                                com.example.bookapp.data.deleteAudioFromAppStorage(it)
                            }
                            db.sectionDao().updateAudioUrl(sectionId, null)
                            sections = db.sectionDao().getByRole(roleId)
                        }
                    },
                    fieldTitle = breadcrumb.first,
                    taziehTitle = breadcrumb.second,
                    roleTitle = breadcrumb.third,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(ROUTE_TEXT) { backStackEntry ->
            val sectionId = backStackEntry.arguments?.getString("sectionId")?.toLongOrNull() ?: 0L
            var title by remember { mutableStateOf("") }
            var content by remember { mutableStateOf("") }
            var bookmarked by remember { mutableStateOf(Prefs.isBookmarked(context, sectionId)) }
            var relatedSections by remember { mutableStateOf(listOf<com.example.bookapp.data.SearchResult>()) }
            var sectionAudioUrl by remember { mutableStateOf<String?>(null) }
            var footnotes by remember { mutableStateOf(listOf<com.example.bookapp.data.FootnoteEntity>()) }
            var siblingSections by remember { mutableStateOf(listOf<com.example.bookapp.data.SectionEntity>()) }
            var siblingIndex by remember { mutableStateOf(-1) }
            var breadcrumb by remember { mutableStateOf(Triple<String?, String?, String?>(null, null, null)) }
            val scope = androidx.compose.runtime.rememberCoroutineScope()

            suspend fun reloadFootnotes() {
                footnotes = db.footnoteDao().getBySection(sectionId)
            }

            LaunchedEffect(sectionId) {
                val section = db.sectionDao().getById(sectionId)
                title = section.title
                content = section.content
                bookmarked = Prefs.isBookmarked(context, sectionId)
                sectionAudioUrl = section.audioUrl
                Prefs.addRecent(context, sectionId)
                Prefs.markSectionRead(context, sectionId)
                relatedSections = db.searchDao().getRelatedByTitle(section.title, sectionId)
                reloadFootnotes()
                siblingSections = db.sectionDao().getByRole(section.roleId)
                siblingIndex = siblingSections.indexOfFirst { it.id == sectionId }
                val role = db.roleDao().getById(section.roleId)
                val tazieh = db.taziehDao().getById(role.taziehId)
                val fieldEntity = tazieh?.let { t -> db.fieldDao().getAll().find { it.id == t.fieldId } }
                breadcrumb = Triple(fieldEntity?.title, tazieh?.title, role.title)
            }
            TextScreen(
                title = title,
                content = content,
                isBookmarked = bookmarked,
                onToggleBookmark = {
                    bookmarked = Prefs.toggleBookmark(context, sectionId)
                },
                sectionId = sectionId,
                audioUrl = sectionAudioUrl,
                relatedSections = relatedSections,
                onRelatedClick = { related -> navController.navigate("text/${related.sectionId}") },
                footnotes = footnotes,
                onAddFootnote = { term, explanation ->
                    scope.launch {
                        db.footnoteDao().insert(com.example.bookapp.data.FootnoteEntity(sectionId = sectionId, term = term, explanation = explanation))
                        reloadFootnotes()
                    }
                },
                onEditFootnote = { fn, term, explanation ->
                    scope.launch {
                        db.footnoteDao().update(fn.copy(term = term, explanation = explanation))
                        reloadFootnotes()
                    }
                },
                onDeleteFootnote = { fn ->
                    scope.launch {
                        db.footnoteDao().delete(fn.id)
                        reloadFootnotes()
                    }
                },
                onOpenSearch = { navController.navigate(ROUTE_SEARCH) },
                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                fieldTitle = breadcrumb.first,
                taziehTitle = breadcrumb.second,
                roleTitle = breadcrumb.third,
                darkMode = darkMode,
                onDarkModeChange = onDarkModeChange,
                fontScale = fontScale,
                onFontScaleChange = onFontScaleChange,
                fontChoice = fontChoice,
                onFontChoiceChange = onFontChoiceChange,
                hasPrevSection = siblingIndex > 0,
                hasNextSection = siblingIndex in 0 until siblingSections.size - 1,
                onPrevSection = {
                    if (siblingIndex > 0) navController.navigate("text/${siblingSections[siblingIndex - 1].id}")
                },
                onNextSection = {
                    if (siblingIndex in 0 until siblingSections.size - 1) navController.navigate("text/${siblingSections[siblingIndex + 1].id}")
                },
                onAttachAudio = { uri ->
                    scope.launch {
                        val path = com.example.bookapp.data.copyAudioToAppStorage(context, uri)
                        if (path != null) {
                            db.sectionDao().updateAudioUrl(sectionId, path)
                            sectionAudioUrl = path
                        }
                    }
                },
                onRemoveAudio = {
                    scope.launch {
                        sectionAudioUrl?.let { com.example.bookapp.data.deleteAudioFromAppStorage(it) }
                        db.sectionDao().updateAudioUrl(sectionId, null)
                        sectionAudioUrl = null
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
