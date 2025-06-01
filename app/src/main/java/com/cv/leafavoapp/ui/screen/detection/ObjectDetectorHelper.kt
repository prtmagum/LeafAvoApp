package com.cv.leafavoapp.ui.screen.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.cv.leafavoapp.data.DetectionResult
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.ObjectDetector

class ObjectDetectorHelper(context: Context) {
    private var detector: ObjectDetector? = null

    init {
        try {
            val baseOptionsBuilder = BaseOptions.builder()

            // Coba gunakan NNAPI
            try {
                baseOptionsBuilder.useNnapi()
                Log.d("ObjectDetectorHelper", "Menggunakan NNAPI delegate")
            } catch (e: Exception) {
                Log.e("ObjectDetectorHelper", "NNAPI tidak tersedia, fallback ke CPU")
                baseOptionsBuilder.setNumThreads(4)
            }

            val options = ObjectDetector.ObjectDetectorOptions.builder()
                .setBaseOptions(baseOptionsBuilder.build())
                .setMaxResults(5)
                .setScoreThreshold(0.7f)
                .build()

            detector = ObjectDetector.createFromFileAndOptions(
                context,
                "model_object_detection_avo.tflite", // Pastikan nama file model benar
                options
            )
            Log.d("ObjectDetectorHelper", "ObjectDetector berhasil diinisialisasi")
        } catch (e: Exception) {
            Log.e("ObjectDetectorHelper", "Gagal inisialisasi ObjectDetector: ${e.message}")
            e.printStackTrace()
            detector = null
        }
    }

    @OptIn(ExperimentalGetImage::class)
    fun detect(imageProxy: ImageProxy): List<DetectionResult> {
        if (detector == null) {
            Log.e("ObjectDetectorHelper", "Detector belum diinisialisasi")
            return emptyList()
        }

        val mediaImage = imageProxy.image ?: return emptyList()

        // Konversi ImageProxy ke Bitmap dan putar sesuai orientasi
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val bitmap = imageProxy.toBitmap().rotate(rotationDegrees)

        val tfImage = TensorImage.fromBitmap(bitmap)

        val results = detector?.detect(tfImage)

        return results?.map { detection ->
            DetectionResult(
                label = detection.categories.firstOrNull()?.label ?: "Unknown",
                score = detection.categories.firstOrNull()?.score ?: 0.0f,
                boundingBox = detection.boundingBox
            )
        } ?: emptyList()
    }

    fun close() {
        detector?.close()
    }
}

// Fungsi ekstensi untuk rotasi Bitmap
fun Bitmap.rotate(degrees: Int): Bitmap {
    if (degrees == 0) return this
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

// Fungsi ekstensi untuk konversi ImageProxy ke Bitmap (YUV ke RGB)
fun ImageProxy.toBitmap(): Bitmap {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = android.graphics.YuvImage(nv21, android.graphics.ImageFormat.NV21, width, height, null)
    val out = java.io.ByteArrayOutputStream()
    yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 100, out)
    val imageBytes = out.toByteArray()
    return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}
