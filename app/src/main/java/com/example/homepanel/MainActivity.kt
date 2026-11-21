package com.example.homepanel

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.homepanel.ui.navigation.HomepanelApp
import com.example.homepanel.ui.theme.HomepanelTheme

// AppCompatActivity нужна для смены языка на лету (AppCompatDelegate.setApplicationLocales)
class MainActivity : AppCompatActivity() {
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
