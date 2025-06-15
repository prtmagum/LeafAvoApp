package com.cv.leafavoapp.ui.screen.scan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.cv.leafavoapp.R
import com.cv.leafavoapp.ui.DataStoreHelper
import com.cv.leafavoapp.ui.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector

@Composable
fun ScanScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var resultText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val dataStoreHelper = remember { DataStoreHelper(context) }

    val primaryColor = Color(0xFF128750)
    val secondaryColor = Color(0xFF2E7D32)
    val tertiaryColor = Color(0xFF004D40)
    val backgroundColor = Color(0xFFF5F5F5)

    // --- Bagian Logika Izin ---
    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+ (Android 13+)
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES // Untuk gambar
                // Jika perlu video: Manifest.permission.READ_MEDIA_VIDEO
            )
        } else { // API di bawah 33
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE
                // Jika Anda memerlukan WRITE_EXTERNAL_STORAGE untuk API < 29 (Android 10),
                // dan belum ditangani oleh Scoped Storage atau MediaStore, tambahkan di sini.
                // Namun, untuk fungsi pilih gambar, READ sudah cukup.
            )
        }
    }

    var allPermissionsGranted by remember { mutableStateOf(false) }

    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            allPermissionsGranted = permissions.values.all { it } // True jika semua izin diberikan
            if (allPermissionsGranted) {
                Toast.makeText(context, "Izin kamera & penyimpanan diberikan!", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(
                    context,
                    "Beberapa izin ditolak. Fitur mungkin terbatas.",
                    Toast.LENGTH_LONG
                ).show()
                // Anda bisa menambahkan logika untuk mengarahkan pengguna ke pengaturan aplikasi
                // atau menampilkan dialog penjelasan yang lebih detail.
            }
        }
    )

    // Fungsi untuk memeriksa apakah semua izin yang dibutuhkan sudah diberikan
    fun arePermissionsGranted(): Boolean {
        return permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Fungsi untuk meminta izin yang belum diberikan
    fun requestPermissions() {
        val permissionsNotGranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsNotGranted.isNotEmpty()) {
            multiplePermissionsLauncher.launch(permissionsNotGranted)
        }
    }

    // Periksa dan minta izin saat layar pertama kali dimuat atau saat state berubah (jika perlu)
    LaunchedEffect(Unit) { // Hanya dijalankan sekali saat komposisi awal
        if (!arePermissionsGranted()) {
            requestPermissions()
        } else {
            allPermissionsGranted = true
        }
    }
    // Setiap kali hasil dari launcher berubah (setelah pengguna merespon dialog izin),
    // periksa kembali apakah semua izin sudah diberikan.
    // Ini berguna jika pengguna memberikan izin melalui pengaturan setelah awalnya menolak.
    // Namun, `onResult` dari `multiplePermissionsLauncher` sudah menangani update `allPermissionsGranted`.
    // Jika Anda ingin UI reaktif terhadap perubahan izin dari luar (misal via settings),
    // Anda mungkin perlu mekanisme yang lebih kompleks atau mengandalkan `onResume` di Activity.
    // Untuk kasus umum, LaunchedEffect(Unit) dan onResult sudah cukup.

    // --- Akhir Bagian Logika Izin ---


    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (!allPermissionsGranted && !arePermissionsGranted()) { // Cek izin sebelum meluncurkan
            Toast.makeText(
                context,
                "Izin penyimpanan diperlukan untuk memilih gambar.",
                Toast.LENGTH_LONG
            ).show()
            requestPermissions() // Minta izin jika belum ada
            return@rememberLauncherForActivityResult
        }
        uri?.let {
            showResult = false
            resultText = ""
            descriptionText = ""
            try {
                val originalBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                bitmap = scaleBitmapDown(originalBitmap, 320)
            } catch (e: Exception) {
                Log.e("ScanScreen", "Error loading image from URI: $it", e)
                Toast.makeText(context, "Gagal memuat gambar.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { capturedBitmap ->
        if (!allPermissionsGranted && !arePermissionsGranted()) { // Cek izin sebelum meluncurkan
            Toast.makeText(
                context,
                "Izin kamera diperlukan untuk mengambil foto.",
                Toast.LENGTH_LONG
            ).show()
            requestPermissions() // Minta izin jika belum ada
            return@rememberLauncherForActivityResult
        }
        showResult = false
        resultText = ""
        descriptionText = ""
        bitmap = capturedBitmap?.let { scaleBitmapDown(it, 320) }
    }

    // Refresh status izin saat layar kembali aktif (opsional, tapi bisa berguna)
    // Ini bisa dilakukan jika Anda menggunakan LifecycleEventObserver,
    // namun untuk Composable, state dari launcher biasanya sudah cukup.

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Header with logos (kode Anda tetap sama)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.fkom),
                contentDescription = "Logo FKOM",
                modifier = Modifier.size(70.dp)
            )
            Text(
                text = "LeafAvo Scanner",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                ),
                textAlign = TextAlign.Center
            )
            Image(
                painter = painterResource(id = R.drawable.logo_ti),
                contentDescription = "Logo TI",
                modifier = Modifier.size(50.dp)
            )
        }

        // UI utama akan ditampilkan di sini, tergantung status izin
        // Jika izin sudah diberikan, tampilkan konten utama ScanScreen
        if (allPermissionsGranted || arePermissionsGranted()) { // Cek lagi untuk memastikan
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 100.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title (kode Anda tetap sama)
                Text(
                    text = "Klasifikasi dan Deteksi",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Image Display Card (kode Anda tetap sama)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap!!.asImageBitmap(),
                                contentDescription = "Gambar yang dipilih",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_place_holder),
                                    contentDescription = "Placeholder Galeri",
                                    modifier = Modifier.size(120.dp)
                                )
                                Text(
                                    text = "Pilih atau ambil gambar",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }

                        if (isScanning) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xAA000000)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = "Menganalisis...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        modifier = Modifier.padding(top = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }


                Spacer(modifier = Modifier.height(16.dp))

                // Buttons Row (Pilih Gambar & Ambil Foto) (kode Anda tetap sama)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, secondaryColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = secondaryColor)
                    ) {
                        Icon(Icons.Filled.Image, "Gallery icon", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pilih Gambar")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    OutlinedButton(
                        onClick = { cameraLauncher.launch() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, secondaryColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = secondaryColor)
                    ) {
                        Icon(Icons.Filled.Camera, "Camera icon", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ambil Foto")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scan Button (Klasifikasi) (kode Anda tetap sama)
                Button(
                    onClick = {
                        if (!allPermissionsGranted && !arePermissionsGranted()) {
                            Toast.makeText(
                                context,
                                "Izin diperlukan untuk memindai.",
                                Toast.LENGTH_LONG
                            ).show()
                            requestPermissions()
                            return@Button
                        }
                        bitmap?.let { bmp ->
                            isScanning = true
                            showResult = false
                            scope.launch {
                                delay(1000)
                                val result = predictImage(context, bmp)
                                resultText = result.first
                                descriptionText = result.second
                                val waktu = java.text.SimpleDateFormat(
                                    "dd-MM-yyyy HH:mm",
                                    java.util.Locale.getDefault()
                                ).format(java.util.Date())
                                val uriString =
                                    saveBitmapToInternalStorage(context, bmp, "classify")
                                dataStoreHelper.addScanHistory("$uriString~${result.first}~$waktu")
                                isScanning = false
                                showResult = true
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.White
                    ),
                    enabled = bitmap != null && !isScanning
                ) {
                    Icon(Icons.Filled.Search, "Scan icon")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pindai (Klasifikasi Daun)", fontSize = 15.sp)
                }


                Spacer(modifier = Modifier.height(10.dp))

                // Tombol Beralih ke Deteksi Objek (kode Anda tetap sama)
                Button(
                    onClick = {
                        navController.navigate(Screen.Detection.route)
                        Log.d(
                            "ScanScreen",
                            "Beralih ke Deteksi Objek diklik. Nama model: leafavo_model_v5_final_converted.tflite"
                        )
                        Toast.makeText(context, "Membuka Deteksi Objek...", Toast.LENGTH_SHORT)
                            .show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tertiaryColor,
                        contentColor = Color.White
                    ),
                    enabled = true
                ) {
                    Icon(Icons.Filled.TravelExplore, "Object detection icon")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Deteksi Objek Daun", fontSize = 15.sp)
                }


                Spacer(modifier = Modifier.height(20.dp))

                // Result Card (Untuk hasil klasifikasi) (kode Anda tetap sama)
                if (showResult && resultText.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .animateContentSize(
                                animationSpec = tween(
                                    durationMillis = 300,
                                    easing = LinearOutSlowInEasing
                                )
                            ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                                2.dp
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Hasil Analisis Klasifikasi",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
                                ),
                                textAlign = TextAlign.Center
                            )
                            Divider(
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .fillMaxWidth(0.8f),
                                thickness = 1.dp,
                                color = Color(0xFFE0E0E0)
                            )
                            Text(
                                text = resultText,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            if (descriptionText.isNotEmpty() && descriptionText != "-") {
                                Text(
                                    text = descriptionText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        } else {
            // Jika izin belum/tidak diberikan, tampilkan pesan dan tombol untuk meminta izin
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .align(Alignment.Center), // Pusatkan konten ini
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Anda bisa menggunakan ikon yang lebih relevan di sini
                Image(
                    painter = painterResource(id = R.drawable.ic_place_holder), // Ganti dengan ikon yang sesuai
                    contentDescription = "Izin Diperlukan",
                    modifier = Modifier
                        .size(100.dp)
                        .padding(bottom = 24.dp)
                )
                Text(
                    text = "Izin Diperlukan",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Aplikasi ini memerlukan izin Kamera untuk mengambil foto dan izin Penyimpanan untuk memilih gambar dari galeri agar fitur pemindaian dapat berfungsi.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Button(
                    onClick = { requestPermissions() },
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Berikan Izin")
                }
            }
        }
    }
}

// Fungsi-fungsi lainnya (saveBitmapToInternalStorage, scaleBitmapDown, predictImage, getDescription, ScanScreenPreview)
// Anda tidak perlu mengubahnya kecuali ada logika terkait izin di dalamnya.
// ... (kode Anda sebelumnya untuk fungsi-fungsi ini) ...

fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap, type: String): String {
    val filename = "${type}_scan_${System.currentTimeMillis()}.png"
    context.openFileOutput(filename, Context.MODE_PRIVATE).use { fos ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
    }
    return filename
}

fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val originalWidth = bitmap.width
    val originalHeight = bitmap.height
    var newWidth = originalWidth
    var newHeight = originalHeight
    val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()

    if (originalWidth > originalHeight) {
        if (originalWidth > maxDimension) {
            newWidth = maxDimension
            newHeight = (newWidth / aspectRatio).toInt()
        }
    } else {
        if (originalHeight > maxDimension) {
            newHeight = maxDimension
            newWidth = (newHeight * aspectRatio).toInt()
        }
    }
    if (newWidth <= 0) newWidth = 1 // Pastikan tidak nol
    if (newHeight <= 0) newHeight = 1 // Pastikan tidak nol

    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}


fun predictImage(context: Context, bitmap: Bitmap): Pair<String, String> {
    val modelName = "model_object_detection_avo.tflite" // Pastikan nama file ini benar

    // 1. Konfigurasi options untuk Object Detector
    val options = ObjectDetector.ObjectDetectorOptions.builder()
        .setBaseOptions(BaseOptions.builder().build())
        .setScoreThreshold(0.8f) // Ambang batas kepercayaan, bisa disesuaikan (misal: 0.5f = 50%)
        .setMaxResults(3) // Maksimal jumlah objek yang dideteksi
        .build()

    try {
        // 2. Buat instance ObjectDetector dari file model dan options
        // Library ini akan otomatis membaca label dari metadata model jika ada
        val objectDetector = ObjectDetector.createFromFileAndOptions(context, modelName, options)

        // 3. Konversi Bitmap ke TensorImage
        val tensorImage = TensorImage.fromBitmap(bitmap)

        // 4. Jalankan deteksi
        val results: List<Detection> = objectDetector.detect(tensorImage)

        // 5. Proses hasil deteksi untuk meniru klasifikasi
        if (results.isEmpty()) {
            return Pair("Tidak Dikenali", "Tidak ada objek daun yang terdeteksi pada gambar.")
        } else {
            // Ambil deteksi dengan skor tertinggi
            val bestDetection = results.maxByOrNull { it.categories.first().score }

            if (bestDetection != null) {
                val category = bestDetection.categories.first()
                val rawLabel = category.label // Ambil label mentah dari model

                // BERSIHKAN LABEL DI SINI
                val cleanLabel = rawLabel.trim() // Hapus spasi di awal dan akhir

                val score = category.score

                // Gunakan label yang sudah bersih untuk semuanya
                val description = getDescription(cleanLabel)
                val resultString = "$cleanLabel (%.2f%%)".format(score * 100)

                return Pair(resultString, description)
            } else {
                return Pair("Tidak Dikenali", "Gagal memproses hasil deteksi.")
            }
        }

    } catch (e: Exception) {
        // Tangani error, misal model tidak ditemukan atau tidak kompatibel
        Log.e("ScanScreen-ObjectDetection", "Error during object detection", e)
        return Pair("Error", "Gagal melakukan deteksi: ${e.message}")
    }
}


fun getDescription(label: String): String {
    val cleanLabel = label.replace(Regex("^[0-9 ]+"), "").trim().lowercase()

    return when (cleanLabel) {
        "mentega" -> "Ciri daunnya panjang, tekstur daunnya halus dan bagian sampingnya tidak bergelombang."
        "aligator" -> "Ciri daunnya, panjang, ujungnya lancip, dan bagian sampingnya bergelombang."
        "kendil" -> "Ciri daunnya berbentuk oval dan sedikit melingkar dan bagian sampingnya tidak bergelombang."
        else -> "Informasi untuk jenis daun '$cleanLabel' tidak ditemukan."
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_3A)
@Composable
fun ScanScreenPreview() {
    ScanScreen(navController = NavHostController(LocalContext.current))
}