package com.cv.leafavoapp.ui.screen.detection // Sesuaikan package Anda

import android.content.Context
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.cv.leafavoapp.data.DetectionResult
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.ObjectDetector

class ObjectDetectorHelper(context: Context) {
    private var detector: ObjectDetector? = null // Ubah menjadi nullable untuk penanganan kesalahan

    init {
        try {
            val baseOptionsBuilder = BaseOptions.builder()

// Coba gunakan NNAPI
// Anda masih bisa menyertakan pemeriksaan CompatibilityList jika mau,
// meskipun NNAPI umumnya lebih luas didukung daripada GPU delegate spesifik.
            try {
                baseOptionsBuilder.useNnapi()
                // Log atau tandai bahwa NNAPI sedang dicoba
            } catch (e: Exception) {
                // Fallback ke CPU jika NNAPI gagal atau tidak didukung
                // Log bahwa NNAPI gagal, fallback ke CPU
                baseOptionsBuilder.setNumThreads(4) // Contoh fallback ke CPU
            }


            val options = ObjectDetector.ObjectDetectorOptions.builder()
                .setBaseOptions(baseOptionsBuilder.build())
                .setMaxResults(5)
                .setScoreThreshold(0.7f)
                .build()

            detector = ObjectDetector.createFromFileAndOptions(
                context,
                "model_object_detection_avo.tflite", // Pastikan nama model ini benar
                options
            )
        } catch (e: Exception) {
            // Tangani kesalahan inisialisasi, misalnya model tidak ditemukan atau delegate gagal
            e.printStackTrace()
            // Anda mungkin ingin melempar pengecualian khusus atau mengatur detector menjadi null
            // dan menanganinya di fungsi detect
            detector = null
        }
    }


    @OptIn(ExperimentalGetImage::class)
    fun detect(imageProxy: ImageProxy): List<DetectionResult> {
        if (detector == null) {
            // Jika detektor gagal diinisialisasi, kembalikan daftar kosong atau tangani kesalahan
            return emptyList()
        }

        val mediaImage = imageProxy.image ?: return emptyList()
        // Pastikan untuk menutup ImageProxy setelah selesai menggunakannya, biasanya di luar kelas ini.
        // val bitmap = imageProxy.toBitmap() // Konversi ke Bitmap jika diperlukan untuk tfImage
        // Jika TensorImage.fromBitmap(imageProxy.toBitmap()) tidak bekerja,
        // Anda mungkin perlu mengelola rotasi gambar secara manual.
        // Untuk Task Library, seringkali lebih baik menggunakan MediaImage langsung jika didukung,
        // tapi konversi ke TensorImage dari Bitmap juga umum.

        val tfImage = TensorImage.fromBitmap(imageProxy.toBitmap()) // Anda mungkin perlu mengatur rotasi gambar di sini

        val results = detector?.detect(tfImage) // Gunakan safe call karena detector sekarang nullable

        return results?.map { detection ->
            DetectionResult(
                label = detection.categories.firstOrNull()?.label ?: "Unknown", // Tambahkan penanganan jika tidak ada kategori
                score = detection.categories.firstOrNull()?.score ?: 0.0f,
                boundingBox = detection.boundingBox
            )
        } ?: emptyList() // Kembalikan daftar kosong jika results null
    }

    // Pertimbangkan untuk menambahkan fungsi close jika Anda perlu melepaskan sumber daya detector secara eksplisit
    // fun close() {
    //     detector?.close()
    // }
}