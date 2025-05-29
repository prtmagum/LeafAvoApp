package com.cv.leafavoapp.ui.screen.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.cv.leafavoapp.R
import com.cv.leafavoapp.ui.DataStoreHelper
import com.cv.leafavoapp.ui.OnBoardingActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ProfileScreen(
    dataStoreHelper: DataStoreHelper? = null,
    onNavigateToTutorial: () -> Unit = {},
            onNavigateToDeveloper: () -> Unit = {}
) {
    val context = LocalContext.current
    val name = remember { mutableStateOf("User") }
    val email = remember { mutableStateOf("user@gmail.com") }
    val profileImageUri = remember { mutableStateOf<Uri?>(null) }

    // Tambahkan ActivityResultLauncher untuk mengambil gambar
    val pickImageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                profileImageUri.value = it
                // Simpan URI gambar profil ke DataStore jika ada
                saveImageToInternalStorage(context, it)?.let { savedUri ->
                    profileImageUri.value = savedUri
                    dataStoreHelper?.let { helper ->
                        CoroutineScope(Dispatchers.IO).launch {
                            helper.setProfileImageUri(savedUri.toString())
                        }
                    }
                }
            }
        }
    if (dataStoreHelper != null) {
        // Ambil data dari FirebaseAuth atau DataStore
        LaunchedEffect(Unit) {
            FirebaseAuth.getInstance().currentUser?.let { user ->
                name.value = user.displayName ?: "User"
                email.value = user.email ?: "No email found"
            }
        }

        LaunchedEffect(Unit) {
            dataStoreHelper.profileImageUri.collect { uriString ->
                profileImageUri.value = uriString?.let { Uri.parse(it) } ?: Uri.EMPTY
            }
        }
    }

    // Rancangan profil UI tetap berjalan meskipun data dari DataStore tidak ada dalam preview.
    ProfileScreenContent(
        name = name.value,
        email = email.value,
        profileImageUri = profileImageUri.value,
        onPickImage = { pickImageLauncher.launch("image/*") },
        onLogout = { logout(context, dataStoreHelper) },
        onNavigateToTutorial = onNavigateToTutorial,
        onNavigateToDeveloper = onNavigateToDeveloper
    )
}

// Fungsi untuk menyimpan gambar ke penyimpanan internal
private fun saveImageToInternalStorage(context: Context, uri: Uri): Uri? {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.filesDir, "profile_images")
        if (!file.exists()) file.mkdir()

        val savedFile = File(file, "profile_image.jpg")
        val outputStream = savedFile.outputStream()

        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }

        return Uri.fromFile(savedFile)
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

// Fungsi untuk melakukan logout dan mengatur ulang DataStore
fun logout(context: Context, dataStoreHelper: DataStoreHelper?) {
    FirebaseAuth.getInstance().signOut()

    CoroutineScope(Dispatchers.IO).launch {
        dataStoreHelper?.setLoginStatus(false)
        dataStoreHelper?.clearProfileImageUri()
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()

            // Navigasi kembali ke layar login
            val intent = Intent(context, OnBoardingActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
    }
}

@Composable
private fun ProfileScreenContent(
    name: String,
    email: String,
    profileImageUri: Uri?,
    onPickImage: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToTutorial: () -> Unit,
    onNavigateToDeveloper:() -> Unit
) {
    val primaryGreen = Color(0xFF128750)
    val lightGreen = Color(0xFF8BC34A)
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(primaryGreen.copy(alpha = 0.7f), lightGreen.copy(alpha = 0.3f))
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header dengan gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(gradientBrush)
            ) {
                // Menampilkan foto profil di tengah header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color.White, CircleShape)
                            .background(Color.White)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = profileImageUri ?: R.drawable.profile_default,
                                placeholder = painterResource(R.drawable.profile_default)
                            ),
                            contentDescription = "profile_image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Edit icon over the image
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(primaryGreen)
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    }
                }
            }

            // Profile info card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = email,
                        fontSize = 16.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = onPickImage,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, primaryGreen),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = primaryGreen
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = primaryGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Ganti Gambar Profil")
                    }
                }
            }

            // Buttons section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Tutorial button
                    Button(
                        onClick = onNavigateToTutorial,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = "Tutorial",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Tutorial Aplikasi",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onNavigateToDeveloper,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "developer",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Informasi Developer",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Logout button
                    Button(
                        onClick = onLogout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red.copy(alpha = 0.8f),
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Logout",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Keluar",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // App version at bottom
//            Text(
//                text = "Version ${BuildConfig.VERSION_NAME}",
//                fontSize = 14.sp,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(bottom = 16.dp),
//                textAlign = TextAlign.Center,
//                color = Color.Gray,
//                style = MaterialTheme.typography.bodyMedium,
//            )
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_3A)
@Composable
fun ProfileScreenPreview() {
    ProfileScreenContent(
        name = "Preview User",
        email = "preview@gmail.com",
        profileImageUri = null,
        onPickImage = {},
        onLogout = {},
        onNavigateToTutorial = {},
        onNavigateToDeveloper = {}
    )
}