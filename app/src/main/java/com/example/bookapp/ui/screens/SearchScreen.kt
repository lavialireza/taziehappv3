package com.example.bookapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bookapp.data.AdvancedSearchOptions
import com.example.bookapp.data.AdvancedSearchResult
import com.example.bookapp.data.DialogueSearchResult
import com.example.bookapp.data.FieldEntity
import com.example.bookapp.data.RoleEntity
import com.example.bookapp.data.SearchMatchMode
import com.example.bookapp.data.SearchSortMode
import com.example.bookapp.data.TaziehEntity
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    fields: List<FieldEntity>,
    allTaziehs: List<TaziehEntity>,
    allRoles: List<RoleEntity>,
    onSearch: suspend (query: String, options: AdvancedSearchOptions) -> List<AdvancedSearchResult>,
    onSearchDialogues: suspend (query: String) -> List<DialogueSearchResult> = { emptyList() },
    onResultClick: (AdvancedSearchResult) -> Unit,
    onDialogueResultClick: (DialogueSearchResult) -> Unit = {},
    isBookmarked: (Long) -> Boolean = { false },
    onToggleBookmark: (Long) -> Unit = {},
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<AdvancedSearchResult>()) }
    var dialogueResults by remember { mutableStateOf(emptyList<DialogueSearchResult>()) }
    var searched by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var selectedFieldId by remember { mutableStateOf<Long?>(null) }
    var selectedTaziehId by remember { mutableStateOf<Long?>(null) }
    var selectedRoleId by remember { mutableStateOf<Long?>(null) }
    var matchMode by remember { mutableStateOf(SearchMatchMode.ALL_WORDS) }
    var sortMode by remember { mutableStateOf(SearchSortMode.RELEVANCE) }
    var showAdvanced by remember { mutableStateOf(false) }

    val taziehsForField = remember(selectedFieldId, allTaziehs) {
        if (selectedFieldId == null) emptyList() else allTaziehs.filter { it.fieldId == selectedFieldId }
    }
    val rolesForTazieh = remember(selectedTaziehId, allRoles) {
        if (selectedTaziehId == null) emptyList() else allRoles.filter { it.taziehId == selectedTaziehId }
    }

    val options = AdvancedSearchOptions(matchMode, selectedFieldId, selectedTaziehId, selectedRoleId, sortMode)

    LaunchedEffect(query, options) {
        val q = query.trim()
        if (q.length < 2) {
            results = emptyList()
            dialogueResults = emptyList()
            searched = false
            searching = false
            return@LaunchedEffect
        }
        delay(350)
        searching = true
        results = onSearch(q, options)
        dialogueResults = if (selectedFieldId == null && selectedTaziehId == null && selectedRoleId == null) onSearchDialogues(q) else emptyList()
        searched = true
        searching = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("جستجوی پیشرفته") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("عبارت، نام تعزیه، نقش یا بخشی از شعر") },
                placeholder = { Text("مثلاً: علی اکبر یا یا حسین") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                trailingIcon = { if (searching) CircularProgressIndicator(Modifier.size(20.dp)) }
            )

            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!showAdvanced, { showAdvanced = false }, label = { Text("جستجوی سریع") })
                FilterChip(showAdvanced, { showAdvanced = true }, label = { Text("فیلترهای پیشرفته") })
            }

            if (showAdvanced) {
                Text("نوع تطبیق", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp))
                LazyRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(matchMode == SearchMatchMode.ALL_WORDS, { matchMode = SearchMatchMode.ALL_WORDS }, label = { Text("همه کلمات") }) }
                    item { FilterChip(matchMode == SearchMatchMode.EXACT_PHRASE, { matchMode = SearchMatchMode.EXACT_PHRASE }, label = { Text("عبارت دقیق") }) }
                    item { FilterChip(matchMode == SearchMatchMode.ANY_WORD, { matchMode = SearchMatchMode.ANY_WORD }, label = { Text("هر کلمه") }) }
                }

                Text("زمینه", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 6.dp))
                LazyRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selectedFieldId == null, { selectedFieldId = null; selectedTaziehId = null; selectedRoleId = null }, label = { Text("همه") }) }
                    items(fields) { field -> FilterChip(selectedFieldId == field.id, { selectedFieldId = field.id; selectedTaziehId = null; selectedRoleId = null }, label = { Text(field.title) }) }
                }

                if (taziehsForField.isNotEmpty()) {
                    Text("تعزیه", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 6.dp))
                    LazyRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { FilterChip(selectedTaziehId == null, { selectedTaziehId = null; selectedRoleId = null }, label = { Text("همه تعزیه‌ها") }) }
                        items(taziehsForField) { tazieh -> FilterChip(selectedTaziehId == tazieh.id, { selectedTaziehId = tazieh.id; selectedRoleId = null }, label = { Text(tazieh.title) }) }
                    }
                }

                if (rolesForTazieh.isNotEmpty()) {
                    Text("نقش", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 6.dp))
                    LazyRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { FilterChip(selectedRoleId == null, { selectedRoleId = null }, label = { Text("همه نقش‌ها") }) }
                        items(rolesForTazieh) { role -> FilterChip(selectedRoleId == role.id, { selectedRoleId = role.id }, label = { Text(role.title) }) }
                    }
                }

                Text("مرتب‌سازی", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 6.dp))
                LazyRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(sortMode == SearchSortMode.RELEVANCE, { sortMode = SearchSortMode.RELEVANCE }, label = { Text("مرتبط‌ترین") }) }
                    item { FilterChip(sortMode == SearchSortMode.HIERARCHY, { sortMode = SearchSortMode.HIERARCHY }, label = { Text("ساختار") }) }
                    item { FilterChip(sortMode == SearchSortMode.TITLE, { sortMode = SearchSortMode.TITLE }, label = { Text("عنوان") }) }
                }
            }

            Spacer(Modifier.height(8.dp))
            if (searched && results.isEmpty() && dialogueResults.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("نتیجه‌ای یافت نشد") }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    if (dialogueResults.isNotEmpty()) {
                        item { Text("گفتگوها", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                        items(dialogueResults) { d ->
                            ListItem(headlineContent = { Text(d.dialogueTitle) }, supportingContent = { Text(d.taziehTitle) }, modifier = Modifier.clickable { onDialogueResultClick(d) })
                            HorizontalDivider()
                        }
                    }
                    if (results.isNotEmpty()) item { Text("بخش‌ها (${results.size})", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                    items(results) { r ->
                        ListItem(
                            headlineContent = { Text(r.sectionTitle) },
                            supportingContent = {
                                Column {
                                    Text("${r.fieldTitle} ← ${r.taziehTitle} ← ${r.roleTitle}", style = MaterialTheme.typography.bodySmall)
                                    if (r.snippet.isNotBlank()) Text(r.snippet, maxLines = 2, style = MaterialTheme.typography.bodySmall)
                                    Text("محل تطبیق: ${r.matchSource}", style = MaterialTheme.typography.labelSmall)
                                }
                            },
                            trailingContent = {
                                IconButton(onClick = { onToggleBookmark(r.sectionId) }) {
                                    Icon(if (isBookmarked(r.sectionId)) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, contentDescription = "نشان کردن")
                                }
                            },
                            modifier = Modifier.clickable { onResultClick(r) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
