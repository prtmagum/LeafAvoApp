package com.cv.leafavoapp.ui.onboarding

import androidx.annotation.DrawableRes
import com.cv.leafavoapp.R

sealed class TutorialModel(
    @DrawableRes val image: Int,
    val title: String,
    val description: String,
) {

    data object FirstPage : TutorialModel(
        image = R.drawable.asset1,
        title = "1. Unggah daun alpukat",
        description = "Pertama, unggah foto atau gambar daun alpukat aligator/kendil/mentega dengan klik tombol 'Pilih Gambar' untuk unggah dari galeri atau 'Ambil Foto' untuk unggah foto dari kamera."
    )

    data object SecondPage : TutorialModel(
        image = R.drawable.asset2,
        title = "2. Pindai Gambar",
        description = "Kedua, setelah foto atau gambar berhasil di unggah, klik tombol 'Pindai Gambar' untuk mengetahui jenis daun alpukat."
    )

    data object ThirdPages : TutorialModel(
        image = R.drawable.asset3,
        title = "3. Realtime Deteksi Daun",
        description = "Pengguna aplikasi bisa menggunakan fitur realtime deteksi untuk mengetahui langsung jenis daun alpukat dengan kamera tanpa diunggah terlebih dahulu."
    )
    data object FourthPages : TutorialModel(
        image = R.drawable.asset4,
        title = "4. Riwayat Pindai",
        description = "Hasil yang telah dipindai akan tersimpan di halaman riwayat. Anda bisa menghapus salah satu riwayat atau menghapus riwayat keseluruhan."
    )


}