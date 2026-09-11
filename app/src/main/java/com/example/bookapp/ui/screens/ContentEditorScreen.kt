package com.example.bookapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bookapp.data.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentEditorScreen(
    db: AppDatabase,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var fields by remember { mutableStateOf(emptyList<FieldEntity>()) }
    var taziehs by remember { mutableStateOf(emptyList<TaziehEntity>()) }
    var roles by remember { mutableStateOf(emptyList<RoleEntity>()) }
    var sections by remember { mutableStateOf(emptyList<SectionEntity>()) }
    var footnotes by remember { mutableStateOf(emptyList<FootnoteEntity>()) }
    var selectedFieldId by remember { mutableLongStateOf(-1L) }
    var selectedTaziehId by remember { mutableLongStateOf(-1L) }
    var selectedRoleId by remember { mutableLongStateOf(-1L) }
    var selectedSectionId by remember { mutableLongStateOf(-1L) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var dialog by remember { mutableStateOf<EditorDialog?>(null) }
    var deleteTarget by remember { mutableStateOf<DeleteTarget?>(null) }

    suspend fun reload() {
        fields = db.fieldDao().getAll()
        taziehs = if (selectedFieldId > 0) db.taziehDao().getByField(selectedFieldId) else emptyList()
        roles = if (selectedTaziehId > 0) db.roleDao().getByTazieh(selectedTaziehId) else emptyList()
        sections = if (selectedRoleId > 0) db.sectionDao().getByRole(selectedRoleId) else emptyList()
    }

    LaunchedEffect(Unit) { reload() }
    LaunchedEffect(selectedFieldId) {
        if (selectedFieldId > 0) {
            taziehs = db.taziehDao().getByField(selectedFieldId)
            if (taziehs.none { it.id == selectedTaziehId }) selectedTaziehId = -1
        } else { taziehs = emptyList(); selectedTaziehId = -1 }
    }
    LaunchedEffect(selectedTaziehId) {
        if (selectedTaziehId > 0) {
            roles = db.roleDao().getByTazieh(selectedTaziehId)
            if (roles.none { it.id == selectedRoleId }) selectedRoleId = -1
        } else { roles = emptyList(); selectedRoleId = -1 }
    }
    LaunchedEffect(selectedRoleId) {
        sections = if (selectedRoleId > 0) db.sectionDao().getByRole(selectedRoleId) else emptyList()
        footnotes = emptyList()
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("ویرایش محتوای تعزیه") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت") } }
        )
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("مدیریت ساختاری محتوا", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text("زمینه، تعزیه، نقش و بخش را مستقیماً ویرایش کنید. حذف هر مورد، روابط وابسته آن را نیز طبق ساختار پایگاه‌داده حذف می‌کند.")
                    }
                }
            }
            item {
                LevelHeader("۱. زمینه‌های تعزیه", Icons.Filled.Folder, "افزودن زمینه") { dialog = EditorDialog.Field(null) }
            }
            items(fields, key = { "f${it.id}" }) { field ->
                EntityRow(field.title, if (field.id == selectedFieldId) "انتخاب‌شده" else null,
                    onClick = { selectedFieldId = field.id },
                    onEdit = { dialog = EditorDialog.Field(field) },
                    onDelete = { deleteTarget = DeleteTarget.Field(field) })
            }
            if (selectedFieldId > 0) {
                item {
                    LevelHeader("۲. تعزیه‌های زمینه «${fields.firstOrNull { it.id == selectedFieldId }?.title ?: ""}»", Icons.Filled.MenuBook, "افزودن تعزیه") {
                        dialog = EditorDialog.Tazieh(null, selectedFieldId)
                    }
                }
                items(taziehs, key = { "t${it.id}" }) { t ->
                    EntityRow(t.title, t.author?.takeIf { it.isNotBlank() },
                        onClick = { selectedTaziehId = t.id },
                        onEdit = { dialog = EditorDialog.Tazieh(t, t.fieldId) },
                        onDelete = { deleteTarget = DeleteTarget.Tazieh(t) })
                }
            }
            if (selectedTaziehId > 0) {
                item {
                    val title = taziehs.firstOrNull { it.id == selectedTaziehId }?.title ?: ""
                    LevelHeader("۳. نقش‌های «$title»", Icons.Filled.Person, "افزودن نقش") {
                        dialog = EditorDialog.Role(null, selectedTaziehId)
                    }
                }
                items(roles, key = { "r${it.id}" }) { r ->
                    EntityRow(r.title, "ترتیب ${r.orderIndex + 1}",
                        onClick = { selectedRoleId = r.id },
                        onEdit = { dialog = EditorDialog.Role(r, r.taziehId) },
                        onDelete = { deleteTarget = DeleteTarget.Role(r) })
                }
            }
            if (selectedRoleId > 0) {
                item {
                    val title = roles.firstOrNull { it.id == selectedRoleId }?.title ?: ""
                    LevelHeader("۴. بخش‌های نقش «$title»", Icons.Filled.MenuBook, "افزودن بخش") {
                        dialog = EditorDialog.Section(null, selectedRoleId)
                    }
                }
                items(sections, key = { "s${it.id}" }) { s ->
                    EntityRow(s.title.ifBlank { "بخش ${s.orderIndex + 1}" }, "ترتیب ${s.orderIndex + 1}",
                        onClick = { selectedSectionId = s.id; scope.launch { footnotes = db.footnoteDao().getBySection(s.id) } },
                        onEdit = { dialog = EditorDialog.Section(s, s.roleId) },
                        onDelete = { deleteTarget = DeleteTarget.Section(s) })
                }
                item {
                    LevelHeader("۵. پانویس‌های بخش انتخاب‌شده", Icons.Filled.MenuBook, "افزودن پانویس") {
                        if (selectedSectionId > 0) dialog = EditorDialog.Footnote(null, selectedSectionId)
                    }
                }
                items(footnotes, key = { "fn${it.id}" }) { f ->
                    EntityRow(f.term, f.explanation, onClick = {}, onEdit = { dialog = EditorDialog.Footnote(f, f.sectionId) }, onDelete = { deleteTarget = DeleteTarget.Footnote(f) })
                }
            }
            if (message != null) item { Text(message!!, color = MaterialTheme.colorScheme.primary) }
        }
    }

    dialog?.let { d ->
        when (d) {
            is EditorDialog.Field -> FieldDialog(d.value, onDismiss = { dialog = null }) { title ->
                scope.launch { busy = true; try { if (d.value == null) db.fieldDao().insert(FieldEntity(title = title)) else db.fieldDao().updateIdentity(d.value.id, title, d.value.uid); reload(); message = "زمینه ذخیره شد." } finally { busy = false; dialog = null } }
            }
            is EditorDialog.Tazieh -> TaziehDialog(d.value, fields, d.parentFieldId, { dialog = null }) { title, author ->
                scope.launch { busy = true; try { if (d.value == null) db.taziehDao().insert(TaziehEntity(fieldId = d.parentFieldId, title = title, author = author.ifBlank { null })) else { db.taziehDao().updateIdentity(d.value.id, d.parentFieldId, title, d.value.uid); db.taziehDao().updateAuthor(d.value.id, author.ifBlank { null }, d.value.authorEmail) }; reload(); message = "تعزیه ذخیره شد." } finally { busy = false; dialog = null } }
            }
            is EditorDialog.Role -> RoleDialog(d.value, d.parentTaziehId, { dialog = null }) { title, order ->
                scope.launch { busy = true; try { if (d.value == null) db.roleDao().insert(RoleEntity(taziehId = d.parentTaziehId, title = title, orderIndex = order)) else db.roleDao().updateFromContent(d.value.id, d.parentTaziehId, title, order, d.value.uid); reload(); message = "نقش ذخیره شد." } finally { busy = false; dialog = null } }
            }
            is EditorDialog.Section -> SectionDialog(d.value, d.parentRoleId, { dialog = null }) { title, content, audio, order ->
                scope.launch { busy = true; try { if (d.value == null) db.sectionDao().insert(SectionEntity(roleId = d.parentRoleId, orderIndex = order, title = title, content = content, audioUrl = audio.ifBlank { null })) else db.sectionDao().updateFromContent(d.value.id, d.parentRoleId, title, content, audio.ifBlank { null }, order, d.value.uid, d.value.sourceUid); reload(); message = "بخش ذخیره شد." } finally { busy = false; dialog = null } }
            }
            is EditorDialog.Footnote -> FootnoteDialog(d.value, d.sectionId, { dialog = null }) { term, explanation ->
                scope.launch { busy = true; try { if (d.value == null) db.footnoteDao().insert(FootnoteEntity(sectionId = d.sectionId, term = term, explanation = explanation)) else db.footnoteDao().update(d.value.copy(sectionId = d.sectionId, term = term, explanation = explanation)); footnotes = db.footnoteDao().getBySection(d.sectionId); message = "پانویس ذخیره شد." } finally { busy = false; dialog = null } }
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("حذف محتوا") },
            text = { Text("آیا از حذف «${target.title}» مطمئن هستید؟ موارد وابسته نیز ممکن است حذف شوند.") },
            confirmButton = { Button(onClick = {
                scope.launch { busy = true; try { when (target) { is DeleteTarget.Field -> db.fieldDao().delete(target.value.id); is DeleteTarget.Tazieh -> db.taziehDao().delete(target.value.id); is DeleteTarget.Role -> db.roleDao().delete(target.value.id); is DeleteTarget.Section -> db.sectionDao().delete(target.value.id); is DeleteTarget.Footnote -> db.footnoteDao().delete(target.value.id) }; selectedFieldId = if (target is DeleteTarget.Field) -1L else selectedFieldId; selectedTaziehId = -1; selectedRoleId = -1; selectedSectionId = -1; footnotes = emptyList(); reload(); message = "مورد حذف شد." } finally { busy = false; deleteTarget = null } }
            }) { Text("حذف") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("انصراف") } }
        )
    }
    if (busy) AlertDialog(onDismissRequest = {}, confirmButton = {}, title = { Text("در حال ذخیره…") }, text = { LinearProgressIndicator(Modifier.fillMaxWidth()) })
}

private sealed interface EditorDialog {
    data class Field(val value: FieldEntity?) : EditorDialog
    data class Tazieh(val value: TaziehEntity?, val parentFieldId: Long) : EditorDialog
    data class Role(val value: RoleEntity?, val parentTaziehId: Long) : EditorDialog
    data class Section(val value: SectionEntity?, val parentRoleId: Long) : EditorDialog
    data class Footnote(val value: FootnoteEntity?, val sectionId: Long) : EditorDialog
}
private sealed interface DeleteTarget { val title: String; data class Field(val value: FieldEntity): DeleteTarget { override val title get()=value.title }; data class Tazieh(val value:TaziehEntity):DeleteTarget { override val title get()=value.title }; data class Role(val value:RoleEntity):DeleteTarget { override val title get()=value.title }; data class Section(val value:SectionEntity):DeleteTarget { override val title get()=value.title }; data class Footnote(val value:FootnoteEntity):DeleteTarget { override val title get()=value.term } }

@Composable private fun LevelHeader(title:String, icon: androidx.compose.ui.graphics.vector.ImageVector, action:String, onAdd:()->Unit) { Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) { Row(Modifier.weight(1f)) { Icon(icon, null); Spacer(Modifier.width(8.dp)); Text(title, fontWeight=FontWeight.Bold) }; TextButton(onClick=onAdd){ Icon(Icons.Filled.Add,null); Spacer(Modifier.width(4.dp)); Text(action) } } }
@Composable private fun EntityRow(title:String, subtitle:String?, onClick:()->Unit, onEdit:()->Unit, onDelete:()->Unit) { OutlinedCard(onClick=onClick, modifier=Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement=Arrangement.spacedBy(6.dp)) { Column(Modifier.weight(1f)) { Text(title, fontWeight=FontWeight.SemiBold); subtitle?.let { Text(it, style=MaterialTheme.typography.bodySmall) } }; IconButton(onClick=onEdit){ Icon(Icons.Filled.Edit,"ویرایش") }; IconButton(onClick=onDelete){ Icon(Icons.Filled.Delete,"حذف") } } } }

@Composable private fun FieldDialog(value:FieldEntity?, onDismiss:()->Unit, onSave:(String)->Unit){ SimpleTextDialog(if(value==null)"افزودن زمینه" else "ویرایش زمینه", value?.title.orEmpty(), "نام زمینه", onDismiss,onSave) }
@Composable private fun TaziehDialog(value:TaziehEntity?, fields:List<FieldEntity>, parent:Long, onDismiss:()->Unit, onSave:(String,String)->Unit){ var title by remember{mutableStateOf(value?.title.orEmpty())}; var author by remember{mutableStateOf(value?.author.orEmpty())}; AlertDialog(onDismissRequest=onDismiss,title={Text(if(value==null)"افزودن تعزیه" else "ویرایش تعزیه")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){ OutlinedTextField(title,{title=it},label={Text("نام تعزیه")},singleLine=true); OutlinedTextField(author,{author=it},label={Text("نویسنده")},singleLine=true); Text("زمینه: ${fields.firstOrNull{it.id==parent}?.title.orEmpty()}") }},confirmButton={Button(enabled=title.isNotBlank(),onClick={onSave(title.trim(),author.trim())}){Icon(Icons.Filled.Save,null); Text("ذخیره")}},dismissButton={TextButton(onClick=onDismiss){Text("انصراف")}}) }
@Composable private fun RoleDialog(value:RoleEntity?, parent:Long, onDismiss:()->Unit, onSave:(String,Int)->Unit){ var title by remember{mutableStateOf(value?.title.orEmpty())}; var order by remember{mutableIntStateOf(value?.orderIndex ?: 0)}; AlertDialog(onDismissRequest=onDismiss,title={Text(if(value==null)"افزودن نقش" else "ویرایش نقش")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(title,{title=it},label={Text("نام نقش")},singleLine=true);OutlinedTextField(order.toString(),{it.toIntOrNull()?.let{n->order=n}},label={Text("شماره ترتیب")},singleLine=true)}},confirmButton={Button(enabled=title.isNotBlank(),onClick={onSave(title.trim(),order)}){Text("ذخیره")}},dismissButton={TextButton(onClick=onDismiss){Text("انصراف")}}) }
@Composable private fun SectionDialog(value:SectionEntity?, parent:Long, onDismiss:()->Unit, onSave:(String,String,String,Int)->Unit){ var title by remember{mutableStateOf(value?.title.orEmpty())}; var content by remember{mutableStateOf(value?.content.orEmpty())}; var audio by remember{mutableStateOf(value?.audioUrl.orEmpty())}; var order by remember{mutableIntStateOf(value?.orderIndex ?: 0)}; AlertDialog(onDismissRequest=onDismiss,title={Text(if(value==null)"افزودن بخش" else "ویرایش بخش")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(title,{title=it},label={Text("عنوان بخش")},singleLine=true);OutlinedTextField(content,{content=it},label={Text("متن")},minLines=5);OutlinedTextField(audio,{audio=it},label={Text("آدرس صوت (اختیاری)")},singleLine=true);OutlinedTextField(order.toString(),{it.toIntOrNull()?.let{n->order=n}},label={Text("شماره ترتیب")},singleLine=true)}},confirmButton={Button(enabled=content.isNotBlank(),onClick={onSave(title.trim(),content,audio.trim(),order)}){Text("ذخیره")}},dismissButton={TextButton(onClick=onDismiss){Text("انصراف")}}) }
@Composable private fun SimpleTextDialog(title:String, initial:String, label:String,onDismiss:()->Unit,onSave:(String)->Unit){var text by remember{mutableStateOf(initial)};AlertDialog(onDismissRequest=onDismiss,title={Text(title)},text={OutlinedTextField(text,{text=it},label={Text(label)},singleLine=true)},confirmButton={Button(enabled=text.isNotBlank(),onClick={onSave(text.trim())}){Text("ذخیره")}},dismissButton={TextButton(onClick=onDismiss){Text("انصراف")}})}

@Composable private fun FootnoteDialog(value:FootnoteEntity?, sectionId:Long, onDismiss:()->Unit, onSave:(String,String)->Unit){ var term by remember{mutableStateOf(value?.term.orEmpty())}; var explanation by remember{mutableStateOf(value?.explanation.orEmpty())}; AlertDialog(onDismissRequest=onDismiss,title={Text(if(value==null)"افزودن پانویس" else "ویرایش پانویس")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(term,{term=it},label={Text("عبارت")},singleLine=true);OutlinedTextField(explanation,{explanation=it},label={Text("توضیح")},minLines=4)}},confirmButton={Button(enabled=term.isNotBlank()&&explanation.isNotBlank(),onClick={onSave(term.trim(),explanation.trim())}){Text("ذخیره")}},dismissButton={TextButton(onClick=onDismiss){Text("انصراف")}})}
