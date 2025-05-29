package com.cv.leafavoapp.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cv.leafavoapp.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var dataStoreHelper: DataStoreHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        dataStoreHelper = DataStoreHelper(applicationContext)

        val imageView = findViewById<ImageView>(R.id.imageView2)

        // Mendecode bitmap dengan opsi skala
        val options = BitmapFactory.Options().apply {
            inSampleSize = 4 // Sesuaikan inSampleSize untuk mengecilkan ukuran bitmap
        }
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.leafavotf, options)
        imageView.setImageBitmap(bitmap)

        // Menjalankan coroutine untuk mengecek status login dan menambahkan jeda waktu
        lifecycleScope.launch {
            delay(2000) // Jeda waktu 2 detik

            val isLoggedIn = dataStoreHelper.isLoggedIn.first() // Ambil nilai pertama dari Flow
            if (isLoggedIn) {
                // Jika sudah login, pindah ke MainActivity
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            } else {
                // Jika belum login, pindah ke LoginActivity
                startActivity(Intent(this@SplashActivity, OnBoardingActivity::class.java))
            }
            finish() // Tutup SplashActivity
        }
    }
}
