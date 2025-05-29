package com.cv.leafavoapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.cv.leafavoapp.LeafAvoApp
import com.cv.leafavoapp.ui.theme.LeafAvoAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dataStoreHelper = DataStoreHelper(applicationContext)

        enableEdgeToEdge()

        setContent {
           LeafAvoAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LeafAvoApp(dataStoreHelper = dataStoreHelper)
                }
            }
        }
    }
}
