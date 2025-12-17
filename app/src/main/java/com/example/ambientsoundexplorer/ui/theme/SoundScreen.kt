package com.example.ambientsoundexplorer.ui.theme

import android.media.AudioAttributes
import android.media.MediaController2
import android.media.MediaPlayer
import android.media.session.MediaController
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ambientsoundexplorer.ApiService
import com.example.ambientsoundexplorer.Music
import com.example.ambientsoundexplorer.R;
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


@Composable
fun SoundScreen(apiService: ApiService) {
    var searchText by remember { mutableStateOf("") }
    val data = remember { mutableStateListOf<Music>() }
    var sortOrder by remember { mutableStateOf(ApiService.sortOrder.ascending) }
    var loading by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val headers = HashMap<String,String>().apply { put("X-API-KEY",apiService.apiKey) }
    val player: MediaPlayer = remember { MediaPlayer() }.apply {
        setOnPreparedListener {
            it.start()
        }
        setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
    }
    //val mediaController = android.widget.MediaController(context).apply { setMediaPlayer(playe) }
    var playingId by remember { mutableStateOf(-1) }
    LaunchedEffect(Unit) {
        GlobalScope.launch (Dispatchers.IO){
            apiService.getMusicList(sortOrder,action = { music ->
                data.clear()
                data.addAll(music)
                loading = false
            })
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            player.stop()
        }
    }
    Column(
        modifier = Modifier.padding(20.dp,0.dp)
    ) {
        Row (
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(0.dp,10.dp).height(40.dp)
        ) {
            Text (
                text = "環境音效",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    loading = true
                    sortOrder = if (sortOrder == ApiService.sortOrder.ascending) ApiService.sortOrder.descending else ApiService.sortOrder.ascending
                    GlobalScope.launch (Dispatchers.IO){
                        apiService.getMusicList(sortOrder,action = { music ->
                            data.clear()
                            data.addAll(music)
                            loading = false
                        })
                    }
                },
            ) {
                Icon(painter = painterResource(R.drawable.outline_filter_list_24),"", Modifier.scale(1.2f))
            }
        }

        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
                loading = true
                GlobalScope.launch (Dispatchers.IO){
                    apiService.getMusicList(sortOrder,searchText,action = { music ->
                        data.clear()
                        data.addAll(music)
                        loading = false
                    })
                }
            },
            placeholder = {Text("搜尋")},
            trailingIcon = {Icon(painter = painterResource(R.drawable.outline_search_24),"")},
            modifier = Modifier.fillMaxWidth()
        )
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.padding(24.dp).align(Alignment.CenterHorizontally))
        }
        if (data.isEmpty()) {
            Text("無符合的結果", color = Color.Gray, textAlign = TextAlign.Center , modifier = Modifier.fillMaxWidth().padding(12.dp))
        }
        data.forEach { music ->
            Card (
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.padding(0.dp,6.dp)
            ) {
                Row (
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {
                    IconButton(
                        onClick = {
                            println(playingId)
                            if (playingId == music.music_id) {
                                player.stop()
                                playingId = -1
                            } else {
                                player.reset()
                                player.setDataSource(context, Uri.parse(apiService.endpoint+"/music/audio?music_id=${music.music_id}"),headers)
                                player.prepareAsync()
                                playingId = music.music_id
                            }
                        }
                    ) {
                        Icon(painter = if (playingId == music.music_id) painterResource(R.drawable.baseline_pause_24) else painterResource(R.drawable.outline_play_arrow_24) ,"")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text (
                            text = music.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.End
                        )
                        Row (
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                painter = painterResource(R.drawable.baseline_calendar_month_24),"", modifier = Modifier.scale(0.65f), tint = Color.Gray
                            )
                            Text (
                                text = music.date,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}
