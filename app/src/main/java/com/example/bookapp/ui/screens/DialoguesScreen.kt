package com.example.bookapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class DialogueSummary(
    val id: Long,
    val title: String,
    val turnsCount: Int
)

/**
 * فهرست گفتگوهای ساخته‌شده در یک تعزیه (مثل «گفتگوی شمر و عباس»)، هرکدام
 * مجموعه‌ای از نوبت‌هاست که فقط به بخش‌های موجود اشاره می‌کنند (بدون کپی متن).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialoguesScreen(
    taziehTitle: String,
    dialogues: List<DialogueSummary>,
    onOpenDialogue: (DialogueSummary) -> Unit,
    onDeleteDialogue: (DialogueSummary) -> Unit,
    onCreateNew: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("گفتگوها: $taziehTitle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("گفتگوی جدید") },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = onCreateNew
            )
        }
    ) { padding ->
        if (dialogues.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(
                    "هنوز گفتگویی نساخته‌اید.\nمثلاً می‌توانید مکالمه‌ی شمر و عباس یا امام حسین و علی‌اکبر را از بخش‌های موجود بسازید.",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dialogues, key = { it.id }) { dialogue ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        onClick = { onOpenDialogue(dialogue) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Forum, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(dialogue.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "${dialogue.turnsCount} نوبت",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDeleteDialogue(dialogue) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "حذف گفتگو")
                            }
                        }
                    }
                }
            }
        }
    }
}
