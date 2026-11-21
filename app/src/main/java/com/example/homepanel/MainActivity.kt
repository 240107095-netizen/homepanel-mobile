package com.example.homepanel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.homepanel.ui.library.LibraryScreen
import com.example.homepanel.ui.theme.HomepanelTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HomepanelTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    LibraryScreen(onTitleClick = {}, modifier = Modifier.padding(padding))
                }
            }
        }
    }
}
