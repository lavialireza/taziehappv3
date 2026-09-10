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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bookapp.data.FieldEntity
import com.example.bookapp.data.DialogueSearchResult
import com.example.bookapp.data.SearchResult
import com.example.bookapp.data.TaziehEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    fields: List<FieldEntity>,
    allTaziehs: List<TaziehEntity>,
    onSearch: suspend (query: String, fieldId: Long?, taziehId: Long?) -> List<SearchResult>,
    onSearchDialogues: suspend (query: String) -> List<DialogueSearchResult> = { emptyList() },
    onResultClick: (SearchResult) -> Unit,
    onDialogueResultClick: (DialogueSearchResult) -> Unit = {},
    isBookmarked: (Long) -> Boolean = { false },
    onToggleBookmark: (Long) -> Unit = {},
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(listOf<SearchResult>()) }
    var dialogueResults by remember { mutableStateOf(listOf<DialogueSearchResult>()) }
    var searched by remember { mutableStateOf(false) }
    var selectedFieldId by remember { mutableStateOf<Long?>(null) }
    var selectedTaziehId by remember { mutableStateOf<Long?>(null) }

    val taziehsForField = remember(selectedFieldId, allTaziehs) {
        if (selectedFieldId == null) emptyList() else allTaziehs.filter { it.fieldId == selectedFieldId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("جستجو") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("جستجو در عناوین و متن اشعار...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFieldId == null,
                        onClick = {
                            selectedFieldId = null
                            selectedTaziehId = null
                        },
                        label = { Text("همه زمینه‌ها") }
                    )
                }
                items(fields) { field ->
                    FilterChip(
                        selected = selectedFieldId == field.id,
                        onClick = {
                            selectedFieldId = field.id
                            selectedTaziehId = null
                        },
                        label = { Text(field.title) }
                    )
                }
            }

            if (taziehsForField.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedTaziehId == null,
                            onClick = { selectedTaziehId = null },
                            label = { Text("همه تعزیه‌ها") }
                        )
                    }
                    items(taziehsForField) { tazieh ->
                        FilterChip(
                            selected = selectedTaziehId == tazieh.id,
                            onClick = { selectedTaziehId = tazieh.id },
                            label = { Text(tazieh.title) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            LaunchedEffect(query, selectedFieldId, selectedTaziehId) {
                if (query.trim().length >= 2) {
                    results = onSearch(query.trim(), selectedFieldId, selectedTaziehId)
                    dialogueResults = if (selectedFieldId == null && selectedTaziehId == null) onSearchDialogues(query.trim()) else emptyList()
                    searched = true
                } else {
                    results = emptyList()
                    dialogueResults = emptyList()
                    searched = false
                }
            }

            if (searched && results.isEmpty() && dialogueResults.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("نتیجه‌ای یافت نشد")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (dialogueResults.isNotEmpty()) {
                        item {
                            Text(
                                "گفتگوها",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(dialogueResults) { d ->
                            ListItem(
                                headlineContent = { Text(d.dialogueTitle) },
                                supportingContent = { Text(d.taziehTitle) },
                                modifier = Modifier.clickable { onDialogueResultClick(d) }
                            )
                            HorizontalDivider()
                        }
                        if (results.isNotEmpty()) {
                            item {
                                Text(
                                    "بخش‌ها",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    items(results) { r ->
                        ListItem(
                            headlineContent = { Text(r.sectionTitle) },
                            supportingContent = { Text("${r.fieldTitle} ← ${r.taziehTitle} ← ${r.roleTitle}") },
                            trailingContent = {
                                IconButton(onClick = { onToggleBookmark(r.sectionId) }) {
                                    Icon(
                                        if (isBookmarked(r.sectionId)) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = "نشان کردن"
                                    )
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
