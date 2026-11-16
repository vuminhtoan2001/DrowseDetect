package com.awakedrive.drowsedetect

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.awakedrive.drowsedetect.ui.home.HomeScreen
import com.awakedrive.drowsedetect.ui.settings.SettingsScreen
import com.awakedrive.drowsedetect.ui.theme.AwakeDriveTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AwakeDriveTheme {
                var showSettings by rememberSaveable { mutableStateOf(false) }
                if (showSettings) {
                    BackHandler { showSettings = false }
                    SettingsScreen(onBack = { showSettings = false })
                } else {
                    HomeScreen(onOpenSettings = { showSettings = true })
                }
            }
        }
    }
}
