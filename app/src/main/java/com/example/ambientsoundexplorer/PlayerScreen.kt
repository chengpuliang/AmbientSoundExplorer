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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PlayerScreen(pageViewModel: PageViewModel, apiService: ApiService, music: Music) {
    val bitmap = remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(Unit) {
        bitmap.value = apiService.getMusicPicture(music.music_id)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
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
                fontWeight = FontWeight.Bold
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
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}