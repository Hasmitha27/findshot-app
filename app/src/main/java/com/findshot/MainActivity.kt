package com.findshot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import com.findshot.screens.ImageSearchScreen
import com.findshot.ui.theme.FindshotTheme




class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FindshotTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ImageSearchScreen()
                }
            }
        }
    }
}