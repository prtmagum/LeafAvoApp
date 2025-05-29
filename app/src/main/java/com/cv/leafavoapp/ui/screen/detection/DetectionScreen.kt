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

@Composable
fun DetectionScreen() {
    val context = LocalContext.current
    val detectorHelper = remember { ObjectDetectorHelper(context) }
    val viewModel: DetectionViewModel = viewModel(
        factory = DetectionViewModelFactory(detectorHelper)
    )

    val detections = viewModel.detections.collectAsState()
    val imageSize = viewModel.imageSize.collectAsState() // Ambil ukuran frame kamera

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            analyzer = viewModel.imageAnalyzer,
            modifier = Modifier.fillMaxSize()
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Dapatkan ukuran Canvas (layar)
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Hitung scaling factor
            val scaleX = if (imageSize.value.width != 0f) canvasWidth / imageSize.value.width else 1f
            val scaleY = if (imageSize.value.height != 0f) canvasHeight / imageSize.value.height else 1f

            detections.value.forEach { detection ->
                val left = detection.boundingBox.left * scaleX
                val top = detection.boundingBox.top * scaleY
                val width = detection.boundingBox.width() * scaleX
                val height = detection.boundingBox.height() * scaleY

                drawRect(
                    color = Color.Red,
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(width, height),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 7f)
                )

                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        "${detection.label} ${(detection.score * 100).toInt()}%",
                        left,
                        top - 10f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.RED
                            textSize = 70f
                        }
                    )
                }
            }
        }
    }
}
