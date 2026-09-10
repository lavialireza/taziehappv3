package com.example.bookapp.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

data class TaziehImageItem(
    val id: Long,
    val filePath: String,
    val caption: String
)

/**
 * گالری تصاویر یک تعزیه: عکس‌های قدیمی نسخه‌های خطی، تعزیه‌خوانان معروف و ...
 * کاربر خودش از گالری گوشی عکس اضافه می‌کند؛ توضیح (caption) اختیاری است.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaziehGalleryScreen(
    taziehTitle: String,
    images: List<TaziehImageItem>,
    onAddImage: (android.net.Uri) -> Unit,
    onDeleteImage: (TaziehImageItem) -> Unit,
    onUpdateCaption: (TaziehImageItem, String) -> Unit,
    onBack: () -> Unit
) {
    var editingImage by remember { mutableStateOf<TaziehImageItem?>(null) }
    var captionText by remember { mutableStateOf("") }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let(onAddImage) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تصاویر: $taziehTitle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("افزودن عکس") },
                icon = { Icon(Icons.Filled.AddAPhoto, contentDescription = null) },
                onClick = { pickImageLauncher.launch("image/*") }
            )
        }
    ) { padding ->
        if (images.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(
                    "هنوز عکسی اضافه نشده.\nمثلاً عکس نسخه‌ی خطی قدیمی یا یک تعزیه‌خوان معروف این تعزیه را اضافه کنید.",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(images, key = { it.id }) { image ->
                    Card(shape = RoundedCornerShape(12.dp)) {
                        Column {
                            AsyncImage(
                                model = image.filePath,
                                contentDescription = image.caption,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text(
                                    image.caption.ifBlank { "بدون توضیح" },
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            editingImage = image
                                            captionText = image.caption
                                        }
                                )
                                IconButton(onClick = { onDeleteImage(image) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Filled.Delete, contentDescription = "حذف عکس")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val current = editingImage
    if (current != null) {
        AlertDialog(
            onDismissRequest = { editingImage = null },
            title = { Text("توضیح عکس") },
            text = {
                OutlinedTextField(
                    value = captionText,
                    onValueChange = { captionText = it },
                    placeholder = { Text("مثلاً «نسخه خطی قرن ۱۳» یا «مرحوم فلانی در نقش شمر»") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateCaption(current, captionText.trim())
                    editingImage = null
                }) { Text("ذخیره") }
            },
            dismissButton = {
                TextButton(onClick = { editingImage = null }) { Text("انصراف") }
            }
        )
    }
}
