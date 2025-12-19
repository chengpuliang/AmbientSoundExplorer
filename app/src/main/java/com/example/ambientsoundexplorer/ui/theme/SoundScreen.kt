package com.example.ambientsoundexplorer.ui.theme

import android.media.MediaMetadata
import android.media.session.PlaybackState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.ambientsoundexplorer.PageViewModel
import com.example.ambientsoundexplorer.Player.mediaSession
import com.example.ambientsoundexplorer.Player.player
import com.example.ambientsoundexplorer.PlayerScreen
import com.example.ambientsoundexplorer.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Composable
fun SoundScreen(apiService: ApiService, pageViewModel: PageViewModel) {
    val scope = rememberCoroutineScope()
    var searchText by remember { mutableStateOf("") }
    val data = remember { mutableStateListOf<Music>() }
    var sortOrder by remember { mutableStateOf(ApiService.sortOrder.ascending) }
    var loading by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val headers = HashMap<String, String>().apply { put("X-API-KEY", apiService.apiKey) }
    var playingId by remember { mutableStateOf(-1) }
    val mediaController = android.widget.MediaController(context)
    LaunchedEffect(searchText) {
        loading = true
        data.clear()
        data.addAll(apiService.getMusicList(sortOrder, searchText))
        loading = false
    }
    LaunchedEffect(Unit) {
        player.setOnCompletionListener {
            playingId = -1
        }
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
                text = "環境音效",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    loading = true
                    sortOrder =
                        if (sortOrder == ApiService.sortOrder.ascending) ApiService.sortOrder.descending else ApiService.sortOrder.ascending
                    scope.launch(Dispatchers.IO) {
                        data.clear()
                        data.addAll(apiService.getMusicList(sortOrder))
                        loading = false
                    }
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.outline_filter_list_24),
                    "",
                    Modifier.scale(1.2f)
                )
            }
        }

        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
            },
            placeholder = { Text("搜尋") },
            trailingIcon = { Icon(painter = painterResource(R.drawable.outline_search_24), "") },
            modifier = Modifier.fillMaxWidth()
        )
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(24.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
        if (data.isEmpty()) {
            Text(
                "無符合的結果",
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )
        }
        data.forEach { music ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                onClick = {
                    pageViewModel.push { PlayerScreen(pageViewModel, apiService, music) }
                },
                modifier = Modifier.padding(0.dp, 6.dp)
            ) {
                Row(
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
                                player.setDataSource(
                                    context,
                                    Uri.parse(apiService.endpoint + "/music/audio?music_id=${music.music_id}"),
                                    headers
                                )
                                player.prepareAsync()

                                scope.launch {
                                    val stateBuilder = PlaybackState.Builder()
                                        .setActions(
                                            PlaybackState.ACTION_PLAY or
                                                    PlaybackState.ACTION_PAUSE or
                                                    PlaybackState.ACTION_SKIP_TO_NEXT or
                                                    PlaybackState.ACTION_SKIP_TO_PREVIOUS
                                        )
                                        .setState(
                                            PlaybackState.STATE_PLAYING,
                                            player.currentPosition.toLong(),
                                            1.0f
                                        )
                                    mediaSession.setPlaybackState(stateBuilder.build())
                                    mediaSession.setMetadata(
                                        MediaMetadata.Builder()
                                            .putString(
                                                MediaMetadata.METADATA_KEY_TITLE,
                                                music.title
                                            )
                                            .putString(
                                                MediaMetadata.METADATA_KEY_ARTIST,
                                                music.author
                                            )
                                            .putBitmap(
                                                MediaMetadata.METADATA_KEY_ALBUM_ART,
                                                apiService.getMusicPicture(music.music_id)
                                            )
                                            .build()
                                    )
                                    mediaSession.isActive = true
                                }
                                playingId = music.music_id
                            }
                        }
                    ) {
                        Icon(
                            painter = if (playingId == music.music_id) painterResource(R.drawable.baseline_pause_24) else painterResource(
                                R.drawable.outline_play_arrow_24
                            ), ""
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = music.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.End
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                painter = painterResource(R.drawable.baseline_calendar_month_24),
                                "",
                                modifier = Modifier.scale(0.65f),
                                tint = Color.Gray
                            )
                            Text(
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
