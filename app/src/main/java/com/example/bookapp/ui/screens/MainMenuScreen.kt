package com.example.bookapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.example.bookapp.data.SearchResult

// رنگ سبز اختصاصی برای گزینه «لیست تعزیه‌ها» (مستقل از تم انتخابی)
private val TaziehGreen = Color(0xFF2E7D4F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    randomVerse: SearchResult?,
    recentItems: List<SearchResult>,
    onOpenTaziehList: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenMyRole: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenVersion: () -> Unit,
    onOpenChangelog: () -> Unit,
    onOpenGlossary: () -> Unit,
    onOpenMuharramCalendar: () -> Unit,
    onItemClick: (SearchResult) -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("تعزیه") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp)
        ) {

            if (randomVerse != null) {
                Spacer(Modifier.height(14.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(randomVerse) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "بیت امروز",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            randomVerse.sectionTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${randomVerse.taziehTitle} ← ${randomVerse.roleTitle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            MenuCard("لیست تعزیه‌ها", Icons.Filled.List, onOpenTaziehList, accentColor = TaziehGreen)
            Spacer(Modifier.height(10.dp))
            MenuCard("جستجو", Icons.Filled.Search, onOpenSearch)
            Spacer(Modifier.height(10.dp))
            MenuCard("علاقه‌مندی‌ها", Icons.Filled.Favorite, onOpenBookmarks)
            Spacer(Modifier.height(10.dp))
            MenuCard("دفتر یادداشت", Icons.Filled.Edit, onOpenNotes)
            Spacer(Modifier.height(10.dp))
            MenuCard("گالری تصاویر", Icons.Filled.PhotoLibrary, onOpenGallery)
            Spacer(Modifier.height(10.dp))
            MenuCard("نقش من", Icons.Filled.School, onOpenMyRole, accentColor = TaziehGreen)
            Spacer(Modifier.height(10.dp))
            MenuCard("درباره برنامه", Icons.Filled.Info, onOpenAbout)
            Spacer(Modifier.height(10.dp))
            MenuCard("تنظیمات", Icons.Filled.Settings, onOpenSettings)
            Spacer(Modifier.height(10.dp))
            MenuCard("ورژن برنامه", null, onOpenVersion)
            Spacer(Modifier.height(10.dp))
            MenuCard("چه چیزی جدید است؟", null, onOpenChangelog)
            Spacer(Modifier.height(10.dp))
            MenuCard("دیکشنری اصطلاحات تعزیه", Icons.AutoMirrored.Filled.MenuBook, onOpenGlossary)
            Spacer(Modifier.height(10.dp))
            MenuCard("تقویم محرم", null, onOpenMuharramCalendar)

            if (recentItems.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    "اخیراً مشاهده‌شده",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(8.dp))
                recentItems.forEach { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable { onItemClick(item) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.History, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(item.sectionTitle, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${item.taziehTitle} ← ${item.roleTitle}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun MenuCard(
    title: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    accentColor: Color? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = accentColor?.copy(alpha = 0.14f) ?: MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = accentColor ?: MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = accentColor ?: MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
