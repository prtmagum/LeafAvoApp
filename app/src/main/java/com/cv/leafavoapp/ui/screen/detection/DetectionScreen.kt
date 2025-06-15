package com.cv.leafavoapp.ui.screen.detection // Sesuaikan dengan package Anda

import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.max

@Composable
fun DetectionScreen() {
    val context = LocalContext.current
    // Asumsi ObjectDetectorHelper dan ViewModel sudah disiapkan dengan benar
    val detectorHelper = remember { ObjectDetectorHelper(context) }
    val viewModel: DetectionViewModel = viewModel(
        factory = DetectionViewModelFactory(detectorHelper)
    )

    val detectionsState = viewModel.detections.collectAsState()
    val imageSizeState = viewModel.imageSize.collectAsState()

    // --- BARU: State untuk mengelola instance kamera dan status flash ---
    var camera: Camera? by remember { mutableStateOf(null) }
    var isFlashOn by remember { mutableStateOf(false) }
    // ----------------------------------------------------------------

    Box(modifier = Modifier.fillMaxSize()) {
        // --- DIUBAH: Menggunakan CameraPreview baru yang bisa mengembalikan instance Camera ---
        CameraPreview(
            analyzer = viewModel.imageAnalyzer,
            onCameraBound = { camera = it }, // Mendapatkan instance kamera saat berhasil di-bind
            modifier = Modifier.fillMaxSize()
        )
        // ---------------------------------------------------------------------------------

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val modelInputWidth = imageSizeState.value.width
            val modelInputHeight = imageSizeState.value.height

            if (modelInputWidth > 0f && modelInputHeight > 0f) {
                val scaleFactor = max(canvasWidth / modelInputWidth, canvasHeight / modelInputHeight)
                val scaledImageWidth = modelInputWidth * scaleFactor
                val scaledImageHeight = modelInputHeight * scaleFactor
                val offsetX = (canvasWidth - scaledImageWidth) / 2f
                val offsetY = (canvasHeight - scaledImageHeight) / 2f

                detectionsState.value.forEach { detection ->
                    val boundingBox = detection.boundingBox
                    val drawLeft = boundingBox.left * scaleFactor + offsetX
                    val drawTop = boundingBox.top * scaleFactor + offsetY
                    val rectWidth = boundingBox.width() * scaleFactor
                    val rectHeight = boundingBox.height() * scaleFactor

                    drawRect(
                        color = Color.Red,
                        topLeft = Offset(drawLeft, drawTop),
                        size = androidx.compose.ui.geometry.Size(rectWidth, rectHeight),
                        style = Stroke(width = 7f)
                    )

                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            "${detection.label} ${(detection.score * 100).toInt()}%",
                            drawLeft,
                            drawTop - 10f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.RED
                                textSize = 70f
                            }
                        )
                    }
                }
            }
        }

        // --- BARU: IconButton untuk mengontrol flash/senter ---
        IconButton(
            onClick = {
                camera?.let { cam ->
                    // Cek apakah perangkat memiliki unit flash
                    if (cam.cameraInfo.hasFlashUnit()) {
                        isFlashOn = !isFlashOn
                        // Mengaktifkan atau menonaktifkan senter (torch)
                        cam.cameraControl.enableTorch(isFlashOn)
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd) // Posisi di pojok kanan atas
                .padding(16.dp)
        ) {
            Icon(
                // Ganti ikon berdasarkan status flash
                imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                contentDescription = "Toggle Flash",
                tint = Color.White
            )
        }
        // ---------------------------------------------------------
    }
}

/**
 * --- BARU: Composable untuk menampilkan pratinjau kamera menggunakan CameraX ---
 * Composable ini mengelola setup CameraX dan mengembalikan instance Camera
 * yang bisa digunakan untuk kontrol lebih lanjut seperti flash.
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    analyzer: ImageAnalysis.Analyzer,
    onCameraBound: (Camera) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                // Konfigurasi untuk pratinjau
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                // Konfigurasi untuk analisis gambar (yang sudah Anda miliki di ViewModel)
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .apply {
                        setAnalyzer(executor, analyzer)
                    }

                // Pilih kamera belakang
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

                // Unbind semua use case sebelumnya sebelum me-rebind
                cameraProvider.unbindAll()

                // Bind use case ke lifecycle
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis // Tambahkan use case analisis gambar di sini
                )

                // Kirim instance kamera ke luar untuk kontrol flash
                onCameraBound(camera)

            }, executor)
            previewView
        },
        modifier = modifier
    )
}