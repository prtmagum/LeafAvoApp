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

    private val _imageSize = MutableStateFlow(Size(0f, 0f))
    val imageSize: StateFlow<Size> = _imageSize

    val imageAnalyzer = ImageAnalysis.Analyzer { imageProxy ->
        processImage(imageProxy)
    }

    private fun processImage(imageProxy: ImageProxy) {
        // Update ukuran image di setiap frame
        _imageSize.value = Size(imageProxy.width.toFloat(), imageProxy.height.toFloat())

        val results = detectorHelper.detect(imageProxy)
        viewModelScope.launch {
            _detections.value = results
        }
        imageProxy.close()
    }
}




