package com.example.ambientsoundexplorer.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ambientsoundexplorer.services.ApiService
import com.example.ambientsoundexplorer.services.Music
import com.example.ambientsoundexplorer.services.Reminder
import kotlinx.coroutines.launch

@Composable
fun ReminderScreen() {
    val data = remember { mutableStateListOf<Reminder>() }
    val musicData = remember { mutableStateListOf<Music>() }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        musicData.clear()
        musicData.addAll(ApiService.getMusicList(ApiService.sortOrder.ascending))
        data.clear()
        data.addAll(ApiService.getReminderList())
        loading = false
    }
    Column(
        modifier = Modifier.padding(20.dp, 0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(0.dp, 10.dp)
                .height(40.dp)
        ) {
            Text(
                text = "提醒",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                modifier = Modifier.weight(1f)
            )
        }
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(24.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            data.forEach { reminder ->
                var checked by remember { mutableStateOf(reminder.enabled) }
                Card(
                    modifier = Modifier.padding(0.dp, 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${reminder.hour}:${reminder.minute}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = musicData.find { it.music_id == reminder.music_id }!!.title,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = checked,
                            onCheckedChange = {
                                checked = it
                                scope.launch {
                                    reminder.enabled = it
                                    checked = ApiService.patchReminder(reminder).enabled
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
