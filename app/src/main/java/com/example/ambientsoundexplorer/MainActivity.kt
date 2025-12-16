package com.example.ambientsoundexplorer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.ambientsoundexplorer.ui.theme.AmbientSoundExplorerTheme
import com.example.ambientsoundexplorer.ui.theme.ApiService
import com.example.ambientsoundexplorer.ui.theme.ReminderScreen
import com.example.ambientsoundexplorer.ui.theme.SoundScreen

enum class Page {sounds,reminders}
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var currentPage by remember { mutableStateOf(Page.sounds) }
            val apiService = ApiService("http://57.180.75.22:8000","YZ5TNCN55K")
            AmbientSoundExplorerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
                            NavigationBarItem(
                                selected = currentPage == Page.sounds,
                                onClick = {
                                    currentPage = Page.sounds
                                },
                                icon = {
                                    Icon(painter = painterResource(R.drawable.outline_music_note_24),"")
                                },
                                label = {Text("環境音效")}
                            )
                            NavigationBarItem(
                                selected = currentPage == Page.reminders,
                                onClick = {
                                    currentPage = Page.reminders
                                },
                                icon = {
                                    Icon(painter = painterResource(R.drawable.outline_timer_24),"")
                                },
                                label = {Text("提醒")}
                            )
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        when (currentPage) {
                            Page.sounds -> SoundScreen(apiService)
                            Page.reminders -> ReminderScreen()
                        }
                    }
                }
            }
        }
    }
}
