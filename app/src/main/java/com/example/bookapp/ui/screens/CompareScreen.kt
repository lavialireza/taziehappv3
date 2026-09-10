package com.example.bookapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bookapp.data.SectionEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    roleATitle: String,
    roleASections: List<SectionEntity>,
    roleBTitle: String,
    roleBSections: List<SectionEntity>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مقایسه: $roleATitle ↔ $roleBTitle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // نیمه بالا: نقش اول
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(roleATitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                roleASections.forEach { section ->
                    Text(section.title, style = MaterialTheme.typography.titleSmall)
                    Text(section.content, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                }
            }

            HorizontalDivider(thickness = 2.dp)

            // نیمه پایین: نقش دوم (پس‌زمینه متفاوت تا از نقش اول قابل تشخیص باشد)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(roleBTitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                roleBSections.forEach { section ->
                    Text(section.title, style = MaterialTheme.typography.titleSmall)
                    Text(section.content, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
