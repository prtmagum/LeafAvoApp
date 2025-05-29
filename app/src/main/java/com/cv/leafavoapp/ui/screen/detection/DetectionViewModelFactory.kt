package com.cv.leafavoapp.ui.screen.detection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class DetectionViewModelFactory(
    private val detectorHelper: ObjectDetectorHelper
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetectionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DetectionViewModel(detectorHelper) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
