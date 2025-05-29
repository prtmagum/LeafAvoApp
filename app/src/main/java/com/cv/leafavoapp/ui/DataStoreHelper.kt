package com.cv.leafavoapp.ui

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Ekstensi preferencesDataStore pada Context
private val Context.dataStore by preferencesDataStore(name = "user_preferences")
private val HISTORY_KEY = stringPreferencesKey("scan_history")

class DataStoreHelper(private val context: Context) {

    private val dataStore = context.dataStore

    companion object {
        private val PROFILE_IMAGE_URI_KEY = stringPreferencesKey("profile_image_uri")
        val LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
    }
    suspend fun addScanHistory(result: String) {
        val currentHistory = getScanHistory().first() ?: ""
        val updatedHistory = if (currentHistory.isEmpty()) result else "$currentHistory|$result"
        dataStore.edit { preferences ->
            preferences[HISTORY_KEY] = updatedHistory
        }
    }
    suspend fun saveScanHistory(data: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey("scan_history")] = data
        }
    }



    fun getScanHistory(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[HISTORY_KEY]
    }

    // Menyimpan URI gambar profil
    suspend fun setProfileImageUri(uri: String) {
        dataStore.edit { preferences ->
            preferences[PROFILE_IMAGE_URI_KEY] = uri
        }
    }


    // Mendapatkan URI gambar profil
    val profileImageUri: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[PROFILE_IMAGE_URI_KEY]
        }

    // Menyimpan status login
    suspend fun setLoginStatus(isLoggedIn: Boolean) {
        dataStore.edit { preferences ->
            preferences[LOGGED_IN_KEY] = isLoggedIn
        }
    }

    // Mendapatkan status login
    val isLoggedIn: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[LOGGED_IN_KEY] ?: false
        }

    // Menghapus foto profil
    suspend fun clearProfileImageUri() {
        dataStore.edit { preferences ->
            preferences.remove(PROFILE_IMAGE_URI_KEY)
        }
        Log.d("DataStoreHelper", "Profile image URI removed")
    }
}
