package com.example.ambientsoundexplorer.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ambientsoundexplorer.Page
import com.example.ambientsoundexplorer.R;
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Composable
fun SoundScreen(apiService: ApiService) {
    var searchText by remember { mutableStateOf("") }
    var data = remember { mutableStateListOf<Music>() }
    var sortOrder by remember { mutableStateOf(ApiService.sortOrder.ascending) }
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
                    sortOrder = if (sortOrder == ApiService.sortOrder.ascending) ApiService.sortOrder.descending else ApiService.sortOrder.ascending
                    GlobalScope.launch (Dispatchers.IO){
                        apiService.getMusicList(sortOrder,action = { music ->
                            data.clear()
                            data.addAll(music)
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
                GlobalScope.launch (Dispatchers.IO){
                    apiService.getMusicList(sortOrder,searchText,action = { music ->
                        data.clear()
                        data.addAll(music)
                    })
                }
            },
            placeholder = {Text("搜尋")},
            trailingIcon = {Icon(painter = painterResource(R.drawable.outline_search_24),"")},
            modifier = Modifier.fillMaxWidth()
        )

        data.forEach { music ->
            Card (
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.padding(0.dp,6.dp)
            ) {
                Row (
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text (
                            text = music.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.End
                        )
                        Text (
                            text = music.date,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
