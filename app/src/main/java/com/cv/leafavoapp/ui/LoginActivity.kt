package com.cv.leafavoapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cv.leafavoapp.BuildConfig
import com.cv.leafavoapp.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    lateinit var binding: ActivityLoginBinding
    lateinit var auth: FirebaseAuth
    private lateinit var dataStoreHelper: DataStoreHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityLoginBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)

        binding.tvAppVersion.text = "Version ${BuildConfig.VERSION_NAME}" // Set versi aplikasi

        // Inisialisasi DataStoreHelper
        dataStoreHelper = DataStoreHelper(applicationContext)
        
        setContentView(binding.root)


        auth = FirebaseAuth.getInstance()

        binding.forgotPassword.setOnClickListener {
            val intent = Intent(this, ResetPasswordActivity::class.java)
            startActivity(intent)
        }

        binding.tvToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Jika sudah login, langsung masuk ke MainActivity
        lifecycleScope.launchWhenStarted {
            dataStoreHelper.isLoggedIn.collect { loggedIn ->
                if (loggedIn) {
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }
            }
        }

        // Lanjutkan kode login...
        auth = FirebaseAuth.getInstance()
        binding.btnLogin.setOnClickListener {
            val email = binding.edtEmailLogin.text.toString()
            val password = binding.edtPasswordLogin.text.toString()
            LoginFirebase(email, password)
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.edtEmailLogin.text.toString().trim()
            val password = binding.edtPasswordLogin.text.toString().trim()

            when {
                email.isEmpty() -> {
                    binding.edtEmailLogin.error = "Email tidak boleh kosong"
                    binding.edtEmailLogin.requestFocus()
                }
                !email.contains("@") -> {
                    binding.edtEmailLogin.error = "Email harus mengandung '@'"
                    binding.edtEmailLogin.requestFocus()
                }
                password.isEmpty() -> {
                    binding.edtPasswordLogin.error = "Kata sandi tidak boleh kosong"
                    binding.edtPasswordLogin.requestFocus()
                }
                password.length < 8 -> {
                    binding.edtPasswordLogin.error = "Kata sandi harus minimal 8 karakter"
                    binding.edtPasswordLogin.requestFocus()
                }
                else -> {
                    LoginFirebase(email, password)
                }
            }
        }
    }


    private fun LoginFirebase(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) {
                if (it.isSuccessful) {
                    lifecycleScope.launch {
                        dataStoreHelper.setLoginStatus(true) // Simpan status login
                    }
                    Toast.makeText(this, "Selamat datang $email", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "${it.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}