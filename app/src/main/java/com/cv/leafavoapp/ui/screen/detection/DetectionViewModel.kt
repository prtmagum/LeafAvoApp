package com.cv.leafavoapp.ui.screen.detection

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cv.leafavoapp.data.DetectionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DetectionViewModel(
    private val detectorHelper: ObjectDetectorHelper
) : ViewModel() {

    private val _detections = MutableStateFlow<List<DetectionResult>>(emptyList())
    val detections: StateFlow<List<DetectionResult>> = _detections

    // _imageSize sekarang akan menyimpan dimensi gambar *setelah* memperhitungkan rotasi kamera,
    // yaitu dimensi gambar yang sebenarnya dilihat oleh model deteksi.
    private val _imageSize = MutableStateFlow(Size(0f, 0f))
    val imageSize: StateFlow<Size> = _imageSize

    val imageAnalyzer = ImageAnalysis.Analyzer { imageProxy ->
        processImage(imageProxy)
    }

    private fun processImage(imageProxy: ImageProxy) {
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val sourceWidth = imageProxy.width.toFloat()
        val sourceHeight = imageProxy.height.toFloat()

        // Sesuaikan imageSize berdasarkan rotasi. Bounding box dari model
        // akan relatif terhadap dimensi gambar yang sudah dirotasi.
        if (rotationDegrees == 90 || rotationDegrees == 270) {
            _imageSize.value = Size(sourceHeight, sourceWidth) // Lebar dan tinggi ditukar
        } else {
            _imageSize.value = Size(sourceWidth, sourceHeight)
        }

        val results = detectorHelper.detect(imageProxy)
        viewModelScope.launch {
            _detections.value = results
        }
        imageProxy.close()
    }
}