package com.example.ambientsoundexplorer.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Composable
fun SoundScreen(pageViewModel: PageViewModel) {
    val scope = rememberCoroutineScope()
    var searchText by remember { mutableStateOf("") }
    val data = remember { mutableStateListOf<Music>() }
    var sortOrder by remember { mutableStateOf(ApiService.sortOrder.ascending) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(searchText) {
        loading = true
        data.clear()
        data.addAll(ApiService.getMusicList(sortOrder, searchText))
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
                        data.addAll(ApiService.getMusicList(sortOrder))
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
        if (data.isEmpty() && !loading) {
            Text(
                "無符合的結果",
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )
        }
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            data.forEachIndexed { index, music ->
                Card(
                    onClick = {
                        scope.launch {
                            PlayerService.play(index, data)
                            pageViewModel.push {
                                PlayerScreen(
                                    pageViewModel
                                )
                            }
                        }

                    },
                    modifier = Modifier.padding(0.dp, 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Icon(
                            painter = if (PlayerService.playingMusic.value == music) painterResource(
                                R.drawable.baseline_pause_24
                            ) else painterResource(
                                R.drawable.outline_play_arrow_24
                            ), "", modifier = Modifier.padding(6.dp)
                        )
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
}
