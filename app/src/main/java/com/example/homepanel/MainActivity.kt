package com.example.homepanel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.homepanel.ui.navigation.HomepanelApp
import com.example.homepanel.ui.theme.HomepanelTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HomepanelTheme {
                HomepanelApp()
            }
        }
    }
}
