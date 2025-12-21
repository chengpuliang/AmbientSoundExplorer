package com.example.ambientsoundexplorer.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ambientsoundexplorer.PageViewModel
import com.example.ambientsoundexplorer.R
import com.example.ambientsoundexplorer.services.ApiService
import com.example.ambientsoundexplorer.services.Music
import com.example.ambientsoundexplorer.services.PlayerService
import com.example.ambientsoundexplorer.services.PlayerService.player
import com.example.ambientsoundexplorer.services.Reminder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    pageViewModel: PageViewModel,
    apiService: ApiService,
    music_list: List<Music>,
    index: Int
) {
    val scope = rememberCoroutineScope()
    var currentIndex by remember { mutableStateOf(index) }
    var music by remember { mutableStateOf(music_list[index]) }
    val bitmap = remember { mutableStateOf<Bitmap?>(null) }
    val reminderData = remember { mutableStateListOf<Reminder>() }
    var playerProgress by remember { mutableFloatStateOf(player.currentPosition.toFloat()) }
    LaunchedEffect(music.music_id) {
        bitmap.value = apiService.getMusicPicture(music.music_id)
        reminderData.clear()
        reminderData.addAll(apiService.getReminderList(music_id = music.music_id))
        if (PlayerService.playingMusic != music) {
            PlayerService.play(music)
        }
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
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                    painter = painterResource(R.drawable.outline_arrow_back_ios_24),
                    "",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = music.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        if (bitmap.value != null) {
            Image(
                bitmap = bitmap.value!!.asImageBitmap(), "",
                modifier = Modifier
                    .size(256.dp)
                    .shadow(32.dp, CircleShape, spotColor = MaterialTheme.colorScheme.onSurface)
                    .clip(CircleShape)
                    .border(6.dp, Color.DarkGray, CircleShape)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = music.title,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onSurface
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
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(player.duration / 1000 / 60)}:${player.duration / 1000 % 60}",
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Slider(
            value = playerProgress,
            valueRange = 0f..player.duration.toFloat(),
            onValueChange = {
                PlayerService.seekTo(it.toInt())
                playerProgress = it
            }
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(18.dp)
        ) {
            IconButton(
                onClick = {
                    scope.launch {
                        currentIndex--
                        if (currentIndex < 0) currentIndex = music_list.size - 1
                        music = music_list[currentIndex]
                        bitmap.value = ApiService.getMusicPicture(music.music_id)
                        //PlayerService.play(music)
                    }
                },
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_skip_previous_24),
                    "",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            FilledIconButton(
                onClick = {
                    if (player.isPlaying) {
                        PlayerService.pause()
                    } else if (PlayerService.playingMusic == null) {
                        scope.launch {
                            PlayerService.play(music)
                        }
                    } else {
                        PlayerService.start()
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = CircleShape,
                modifier = Modifier
                    .shadow(6.dp, CircleShape)
                    .size(64.dp)
            ) {
                Icon(
                    painter = if (PlayerService.playing.value) painterResource(R.drawable.baseline_pause_24) else painterResource(
                        R.drawable.outline_play_arrow_24
                    ),
                    "",
                    modifier = Modifier
                )
            }
            IconButton(
                onClick = {
                    scope.launch {
                        currentIndex++
                        if (currentIndex >= music_list.size) currentIndex = 0;
                        music = music_list[currentIndex]
                        println(music)
                        bitmap.value = ApiService.getMusicPicture(music.music_id)
                        //PlayerService.play(music)
                    }
                },
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_skip_next_24),
                    "",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Text(
            "提醒通知",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        )
        reminderData.forEach { reminder ->
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