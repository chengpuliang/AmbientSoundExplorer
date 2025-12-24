package com.example.ambientsoundexplorer.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.widget.RemoteViews
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import com.example.ambientsoundexplorer.MainActivity
import com.example.ambientsoundexplorer.PlayBackWidget
import com.example.ambientsoundexplorer.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@SuppressLint("StaticFieldLeak")
object PlayerService {

    /* ---------- 狀態定義 ---------- */

    enum class PlayerState {
        IDLE,
        PREPARING,
        PREPARED,
        PLAYING,
        PAUSED
    }

    var state = MutableStateFlow(PlayerState.IDLE)

    /* ---------- 對外狀態 ---------- */

    val playing = mutableStateOf(false)
    val playingMusic: MutableStateFlow<Music?> = MutableStateFlow(null)
    var playingBitmap: Bitmap? = null
        private set
    private var playlist: List<Music>? = null
    var playIndex = 0
    /* ---------- Android 物件 ---------- */

    private lateinit var context: Context
    private lateinit var mediaSession: MediaSession
    private lateinit var notificationManager: NotificationManager
    private lateinit var widgetViews: RemoteViews
    private lateinit var appWidgetManager: AppWidgetManager

    private val headers = hashMapOf(
        "X-API-KEY" to ApiService.apiKey
    )

    /* ---------- MediaPlayer ---------- */

    val player: MediaPlayer = MediaPlayer().apply {

        setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )

        setOnCompletionListener {
            CoroutineScope(Dispatchers.IO).launch {
                playNext()
            }
        }

        setOnErrorListener { _, _, _ ->
            resetInternal()
            true
        }
    }

    /* ---------- 初始化 ---------- */

    fun init(context: Context) {
        this.context = context.applicationContext

        mediaSession = MediaSession(context, "PlayerService").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() = start()
                override fun onPause() = pause()
                override fun onSeekTo(pos: Long) = seekTo(pos.toInt())
            })
        }

        notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(
            NotificationChannel(
                "MediaPlayer",
                "音樂播放",
                NotificationManager.IMPORTANCE_LOW
            )
        )

        widgetViews = RemoteViews(context.packageName, R.layout.play_back_widget)
        appWidgetManager = AppWidgetManager.getInstance(context)
    }

    /* ---------- 播放 ---------- */

    suspend fun playPrevious() = withContext(Dispatchers.IO) {
        if (playIndex == 0) playIndex = playlist!!.size - 1 else playIndex--
        play(playIndex)
    }

    suspend fun playNext() = withContext(Dispatchers.IO) {
        if (playIndex == playlist!!.size - 1) playIndex = 0 else playIndex++
        play(playIndex)
    }


    suspend fun play(newIndex: Int, newPlaylist: List<Music>? = null) =
        withContext(Dispatchers.IO) {
            println(newIndex)
            playIndex = newIndex
            if (newPlaylist != null) playlist = newPlaylist
            playingMusic.value = playlist?.get(playIndex)
            state.value = PlayerState.PREPARING
            resetInternal()
            playingBitmap = ApiService.getMusicPicture(playingMusic.value!!.music_id)

            player.setDataSource(
                context,
                (ApiService.endpoint + "/music/audio?music_id=${playingMusic.value!!.music_id}").toUri(),
                headers
            )
            player.prepare()
            state.value = PlayerState.PREPARED
            player.start()
            state.value = PlayerState.PLAYING
            playing.value = true

            updateMediaSession()
            updatePlaybackState(PlaybackState.STATE_PLAYING)
            updateWidget()
            showNotification()
        }

    /* ---------- 控制 ---------- */

    fun start() {
        if (state.value == PlayerState.PREPARED || state.value == PlayerState.PAUSED) {
            player.start()
            state.value = PlayerState.PLAYING
            updatePlaybackState(PlaybackState.STATE_PLAYING)
            updateWidget()
        }
    }

    fun pause() {
        if (state.value == PlayerState.PLAYING) {
            player.pause()
            state.value = PlayerState.PAUSED
            playing.value = false
            updatePlaybackState(PlaybackState.STATE_PAUSED)
            updateWidget()
        }
    }

    fun seekTo(pos: Int) {
        if (state.value == PlayerState.PLAYING || state.value == PlayerState.PAUSED) {
            player.seekTo(pos)
            updatePlaybackState(if (state.value == PlayerState.PLAYING) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED)
        }
    }

    /* ---------- 內部工具 ---------- */

    private fun resetInternal() {
        try {
            player.reset()
        } catch (_: Exception) {
        }
        state.value = PlayerState.IDLE
        playing.value = false
    }

    private fun updateMediaSession() {
        mediaSession.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, playingMusic.value?.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, playingMusic.value?.author)
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, playingBitmap)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, player.duration.toLong())
                .build()
        )
        mediaSession.isActive = true
    }

    private fun updatePlaybackState(state: Int) {
        mediaSession.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or
                            PlaybackState.ACTION_PAUSE or
                            PlaybackState.ACTION_SEEK_TO
                )
                .setState(
                    state,
                    player.currentPosition.toLong(),
                    1f
                )
                .build()
        )
    }

    private fun showNotification() {
        val notification: Notification =
            Notification.Builder(context, "MediaPlayer")
                .setStyle(
                    Notification.MediaStyle()
                        .setMediaSession(mediaSession.sessionToken)
                )
                .setSmallIcon(R.drawable.outline_music_note_24)
                .setContentTitle(playingMusic.value?.title)
                .setContentText(playingMusic.value?.author)
                .setLargeIcon(playingBitmap)
                .setOngoing(true)
                .build()

        notificationManager.notify(1, notification)
    }

    private fun updateWidget() {
        val intent = Intent(context, MainActivity::class.java)
        widgetViews.setTextViewText(R.id.appwidget_text, playingMusic.value!!.title)
        widgetViews.setTextViewText(R.id.appwidget_artist, playingMusic.value!!.author)
        widgetViews.setImageViewBitmap(R.id.appwidget_artwork, playingBitmap!!)
        widgetViews.setOnClickPendingIntent(
            R.id.appwidget, PendingIntent.getActivity(
                context, 0,
                intent, PendingIntent.FLAG_IMMUTABLE
            )
        )
        widgetViews.setImageViewResource(
            R.id.appwidget_playBtn,
            if (isPlaying()) R.drawable.baseline_pause_24 else R.drawable.outline_play_arrow_24
        )
        widgetViews.setOnClickPendingIntent(
            R.id.appwidget_playBtn, PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, PlayBackWidget::class.java).apply {
                    action = "com.example.ambientsoundexplorer.action.WidgetPlayPause"
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        widgetViews.setOnClickPendingIntent(
            R.id.appwidget_skipPreviousBtn, PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, PlayBackWidget::class.java).apply {
                    action = "com.example.ambientsoundexplorer.action.WidgetSkipPrevious"
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        widgetViews.setOnClickPendingIntent(
            R.id.appwidget_skipNextBtn, PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, PlayBackWidget::class.java).apply {
                    action = "com.example.ambientsoundexplorer.action.WidgetSkipNext"
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        val appWidgetIds =
            appWidgetManager.getAppWidgetIds(ComponentName(context, PlayBackWidget::class.java))
        for (i in appWidgetIds.indices) {
            appWidgetManager.updateAppWidget(appWidgetIds[i], widgetViews)
        }
    }

    /* ---------- 給 UI 用 ---------- */

    fun isPlaying(): Boolean = state.value == PlayerState.PLAYING
}
