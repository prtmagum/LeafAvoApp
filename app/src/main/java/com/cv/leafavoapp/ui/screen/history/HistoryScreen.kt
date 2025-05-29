package com.cv.leafavoapp.ui.screen.history

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cv.leafavoapp.R
import com.cv.leafavoapp.ui.DataStoreHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dataStoreHelper = remember { DataStoreHelper(context) }
    val historyFlow = dataStoreHelper.getScanHistory().collectAsState(initial = "")
    val coroutineScope = rememberCoroutineScope()

    val primaryColor = Color(0xFF128750)
    val backgroundColor = Color(0xFFF5F5F5)

    var deleteConfirmItem by remember { mutableStateOf<String?>(null) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }

    // Parsing and sorting items with unique entries
    val items = remember(historyFlow.value) {
        historyFlow.value?.split("|")
            ?.filter { it.isNotBlank() }
            // Ensure each item is unique - filter out duplicates
            ?.distinctBy { item ->
                val parts = item.split("~")
                if (parts.size >= 3) {
                    "${parts[0]}_${parts[2]}" // Unique key based on image path and timestamp
                } else {
                    item
                }
            }
            ?.sortedByDescending {
                // Extract timestamp and sort by it
                val parts = it.split("~")
                if (parts.size >= 3) {
                    try {
                        val timeStr = parts[2]
                        val parser = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
                        parser.parse(timeStr)?.time ?: 0L
                    } catch (e: Exception) {
                        0L
                    }
                } else 0L
            } ?: emptyList()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "History Icon",
                    tint = primaryColor,
                    modifier = Modifier.size(36.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    "Riwayat Pemindaian",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                )
            }

            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                thickness = 1.dp,
                color = Color.LightGray
            )

            // Actions Row
            if (items.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${items.size} hasil pemindaian",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )

                    TextButton(
                        onClick = { showDeleteAllConfirm = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.Red
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear All"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hapus Semua")
                    }
                }
            }

            // Empty State
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_place_holder),
                            contentDescription = "No History",
                            modifier = Modifier
                                .size(120.dp)
                                .padding(bottom = 16.dp)
                        )
                        Text(
                            "Belum ada riwayat pemindaian",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Hasil pemindaian daun akan muncul di sini",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            } else {
                // History List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = items,
                        // Generate a unique key using the combination of parts
                        key = { item ->
                            val parts = item.split("~")
                            if (parts.size >= 3) {
                                "${parts[0]}_${parts[2]}" // Kombinasi path file dan timestamp sebagai key unik
                            } else {
                                item.hashCode().toString() // Fallback ke hashCode jika format tidak sesuai
                            }
                        }
                    ) { item ->
                        val parts = item.split("~")
                        if (parts.size >= 3) {
                            val imagePath = parts[0]
                            val label = parts[1]
                            val time = parts[2]

                            HistoryItem(
                                imagePath = imagePath,
                                label = label,
                                time = time,
                                context = context,
                                onDeleteClick = { deleteConfirmItem = item }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }
        }

        // Delete confirmation dialog
        deleteConfirmItem?.let { itemToDelete ->
            val parts = itemToDelete.split("~")
            val label = if (parts.size >= 2) parts[1] else "item"

            AlertDialog(
                onDismissRequest = { deleteConfirmItem = null },
                title = { Text("Konfirmasi Hapus") },
                text = { Text("Apakah Anda yakin ingin menghapus hasil pemindaian \"$label\"?") },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val currentItems = historyFlow.value?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
                                val updatedItems = currentItems.filter { it != itemToDelete }
                                val updatedData = updatedItems.joinToString("|")
                                dataStoreHelper.saveScanHistory(updatedData)
                                deleteConfirmItem = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        )
                    ) {
                        Text("Hapus")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteConfirmItem = null }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Delete all confirmation dialog
        if (showDeleteAllConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteAllConfirm = false },
                title = { Text("Konfirmasi Hapus Semua") },
                text = { Text("Apakah Anda yakin ingin menghapus semua riwayat pemindaian? Tindakan ini tidak dapat dibatalkan.") },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                dataStoreHelper.saveScanHistory("")
                                showDeleteAllConfirm = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        )
                    ) {
                        Text("Hapus Semua")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllConfirm = false }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@Composable
fun HistoryItem(
    imagePath: String,
    label: String,
    time: String,
    context: Context,
    onDeleteClick: () -> Unit
) {
    var itemVisible by remember { mutableStateOf(false) }

    LaunchedEffect(imagePath) {
        itemVisible = true
    }

    val bitmap = remember(imagePath) {
        loadImageFromInternalStorage(context, imagePath)
    }

    AnimatedVisibility(
        visible = itemVisible,
        enter = fadeIn(initialAlpha = 0.3f) + slideInVertically(
            initialOffsetY = { 100 },
            animationSpec = tween(durationMillis = 300)
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Image
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } ?: run {
                        Image(
                            painter = painterResource(id = R.drawable.ic_place_holder),
                            contentDescription = "Image not found",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = parseLabel(label),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "Probabilitas: ${extractProbability(label)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Text(
                        text = formatDateTime(time),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                // Delete button
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

fun parseLabel(label: String): String {
    // Extract just the name part from something like "mentega (98.25%)"
    val namePart = label.split(" (").firstOrNull() ?: label
    // Return "-" for "Tidak dikenali" to prevent duplicate keys
    return namePart.trim()
}

fun extractProbability(label: String): String {
    // Extract the probability part from something like "mentega (98.25%)"
    val regex = Regex("\\((.*?)\\)")
    val matchResult = regex.find(label)
    return matchResult?.groupValues?.getOrNull(1) ?: "-"
}

fun formatDateTime(dateTime: String): String {
    // Convert "dd-MM-yyyy HH:mm" to a more readable format if needed
    return try {
        val inputFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
        val outputFormat = SimpleDateFormat("d MMM yyyy, HH:mm", Locale("id"))
        val date = inputFormat.parse(dateTime)
        date?.let { outputFormat.format(it) } ?: dateTime
    } catch (e: Exception) {
        dateTime
    }
}

fun loadImageFromInternalStorage(context: Context, filename: String): Bitmap? {
    return try {
        val fileInputStream = context.openFileInput(filename)
        val bitmap = android.graphics.BitmapFactory.decodeStream(fileInputStream)
        fileInputStream.close()
        bitmap
    } catch (e: Exception) {
        // Log error instead of just printStackTrace
        android.util.Log.e("HistoryScreen", "Error loading image: ${e.message}", e)
        null
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    MaterialTheme {
        Surface {
            HistoryScreen()
        }
    }
}