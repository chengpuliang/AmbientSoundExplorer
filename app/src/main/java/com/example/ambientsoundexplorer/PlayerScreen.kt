package com.example.ambientsoundexplorer

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ambientsoundexplorer.Player.player
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(pageViewModel: PageViewModel, apiService: ApiService, music: Music) {
    val scope = rememberCoroutineScope()
    val bitmap = remember { mutableStateOf<Bitmap?>(null) }
    val reminderData = remember { mutableStateListOf<Reminder>() }
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var playerProgress by remember { mutableFloatStateOf(player.currentPosition.toFloat()) }
    LaunchedEffect(Unit) {
        bitmap.value = apiService.getMusicPicture(music.music_id)
        reminderData.clear()
        reminderData.addAll(apiService.getReminderList(music_id = music.music_id))
        while (true) {
            if (player.isPlaying) {
                playerProgress = player.currentPosition.toFloat()
            }
            delay(1000)
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(0.dp, 12.dp)
        ) {
            IconButton(
                onClick = { pageViewModel.pop() }
            ) {
                Icon(
                    painter = painterResource(R.drawable.outline_arrow_back_ios_24), ""
                )
            }
            Text(
                text = music.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        if (bitmap.value != null) {
            Image(
                bitmap = bitmap.value!!.asImageBitmap(), "",
                modifier = Modifier
                    .size(256.dp)
                    .clip(CircleShape)
                    .border(12.dp, Color.Blue, CircleShape)
                    .shadow(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = music.title,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
        Text(
            text = music.author,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.padding(8.dp, 0.dp)
        ) {
            Text(
                text = "${(playerProgress / 1000 / 60).toInt()}:${if (playerProgress / 1000 % 60 < 10) "0" else ""}${(playerProgress / 1000 % 60).toInt()}",
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(player.duration / 1000 / 60)}:${player.duration / 1000 % 60}"
            )
        }
        Slider(
            value = playerProgress,
            valueRange = 0f..player.duration.toFloat(),
            onValueChange = {
                player.seekTo(it.toInt())
                playerProgress = it
            }
        )
        Row(
            modifier = Modifier.padding(18.dp)
        ) {
            FilledIconButton(
                onClick = {
                    if (player.isPlaying) {
                        player.pause()
                        isPlaying = false
                    } else {
                        player.start()
                        isPlaying = true
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White),
                shape = CircleShape,
                modifier = Modifier
                    .shadow(6.dp, CircleShape)
                    .size(64.dp)
            ) {
                Icon(
                    painter = if (isPlaying) painterResource(R.drawable.baseline_pause_24) else painterResource(
                        R.drawable.outline_play_arrow_24
                    ),
                    "",
                    modifier = Modifier
                )
            }
        }
        Text(
            "提醒通知",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )
        reminderData.forEach { reminder ->
            var checked by remember { mutableStateOf(reminder.enabled) }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    }
                    Switch(
                        checked = checked,
                        onCheckedChange = {
                            checked = it
                            scope.launch {
                                reminder.enabled = it
                                checked = apiService.patchReminder(reminder).enabled
                            }
                        }
                    )
                }
            }
        }
    }
}