package com.cv.leafavoapp.ui.screen.detection // Sesuaikan dengan package Anda

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.max // Import 'max'

@Composable
fun DetectionScreen() {
    val context = LocalContext.current
    val detectorHelper = remember { ObjectDetectorHelper(context) }
    val viewModel: DetectionViewModel = viewModel(
        factory = DetectionViewModelFactory(detectorHelper)
    )

    val detectionsState = viewModel.detections.collectAsState()
    val imageSizeState = viewModel.imageSize.collectAsState() // Ukuran frame kamera (setelah rotasi)

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            analyzer = viewModel.imageAnalyzer,
            modifier = Modifier.fillMaxSize()
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val modelInputWidth = imageSizeState.value.width
            val modelInputHeight = imageSizeState.value.height

            if (modelInputWidth > 0f && modelInputHeight > 0f) {
                // Hitung scaleFactor untuk menyesuaikan gambar input model ke ukuran Canvas
                // sambil mempertahankan aspek rasio. Ini meniru perilaku FILL_CENTER
                // dari PreviewView, di mana gambar mungkin di-crop agar pas.
                val scaleFactor = max(canvasWidth / modelInputWidth, canvasHeight / modelInputHeight)

                // Lebar dan tinggi gambar model setelah diskalakan
                val scaledImageWidth = modelInputWidth * scaleFactor
                val scaledImageHeight = modelInputHeight * scaleFactor

                // Hitung offset untuk memusatkan gambar yang diskalakan di dalam Canvas.
                // Jika gambar di-crop, salah satu offset ini bisa negatif atau nol.
                val offsetX = (canvasWidth - scaledImageWidth) / 2f
                val offsetY = (canvasHeight - scaledImageHeight) / 2f

                detectionsState.value.forEach { detection ->
                    val boundingBox = detection.boundingBox

                    // Transformasikan koordinat bounding box dari ruang gambar model
                    // ke ruang Canvas.
                    val drawLeft = boundingBox.left * scaleFactor + offsetX
                    val drawTop = boundingBox.top * scaleFactor + offsetY
                    // val drawRight = boundingBox.right * scaleFactor + offsetX // Alternatif jika butuh right & bottom
                    // val drawBottom = boundingBox.bottom * scaleFactor + offsetY

                    val rectWidth = boundingBox.width() * scaleFactor
                    val rectHeight = boundingBox.height() * scaleFactor

                    // Gambar kotak pembatas
                    drawRect(
                        color = Color.Red,
                        topLeft = Offset(drawLeft, drawTop),
                        size = androidx.compose.ui.geometry.Size(rectWidth, rectHeight),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 7f) // Anda bisa sesuaikan ketebalan garis
                    )

                    // Gambar label dan skor
                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            "${detection.label} ${(detection.score * 100).toInt()}%",
                            drawLeft, // Posisi teks dekat dengan kiri-atas kotak
                            drawTop - 10f, // Sedikit di atas kotak
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.RED
                                textSize = 70f // Anda bisa sesuaikan ukuran teks
                                // Pertimbangkan menambahkan latar belakang pada teks agar lebih mudah terbaca
                            }
                        )
                    }
                }
            }
        }
    }
}