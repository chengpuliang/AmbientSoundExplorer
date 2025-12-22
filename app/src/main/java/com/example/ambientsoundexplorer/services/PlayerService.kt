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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("StaticFieldLeak")
object PlayerService {

    /* ---------- 狀態定義 ---------- */

    enum class PlayerState {
        IDLE,
        PREPARING,
        PREPARED,
        PLAYING,
        PAUSED,
        STOPPED
    }

    private var state: PlayerState = PlayerState.IDLE
    private val preparing = AtomicBoolean(false)

    /* ---------- 對外狀態 ---------- */

    val playing = mutableStateOf(false)
    var playingMusic: Music? = null
        private set
    var playingBitmap: Bitmap? = null
        private set

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

        setOnPreparedListener {
            state = PlayerState.PREPARED
            preparing.set(false)

            it.start()
            state = PlayerState.PLAYING
            playing.value = true

            updateMediaSession()
            updatePlaybackState(PlaybackState.STATE_PLAYING)
            updateWidget()
            showNotification()
        }

        setOnCompletionListener {
            state = PlayerState.STOPPED
            playing.value = false
            playingMusic = null
            playingBitmap = null
            mediaSession.isActive = false
            updatePlaybackState(PlaybackState.STATE_STOPPED)
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

    suspend fun play(music: Music) = withContext(Dispatchers.IO) {
        if (playingMusic?.music_id == music.music_id &&
            (state == PlayerState.PLAYING || state == PlayerState.PAUSED)
        ) return@withContext

        if (!preparing.compareAndSet(false, true)) return@withContext
        resetInternal()

        try {
            playingMusic = music
            playingBitmap = ApiService.getMusicPicture(music.music_id)

            player.setDataSource(
                context,
                (ApiService.endpoint + "/music/audio?music_id=${music.music_id}").toUri(),
                headers
            )

            state = PlayerState.PREPARING
            player.prepareAsync()

        } catch (e: Exception) {
            resetInternal()
        }
    }

    /* ---------- 控制 ---------- */

    fun start() {
        if (state == PlayerState.PREPARED || state == PlayerState.PAUSED) {
            player.start()
            state = PlayerState.PLAYING
            playing.value = true
            updatePlaybackState(PlaybackState.STATE_PLAYING)
            updateWidget()
        }
    }

    fun pause() {
        if (state == PlayerState.PLAYING) {
            player.pause()
            state = PlayerState.PAUSED
            playing.value = false
            updatePlaybackState(PlaybackState.STATE_PAUSED)
            updateWidget()
        }
    }

    fun seekTo(pos: Int) {
        if (state == PlayerState.PLAYING || state == PlayerState.PAUSED) {
            player.seekTo(pos)
            updatePlaybackState(PlaybackState.STATE_PLAYING)
        }
    }

    /* ---------- 內部工具 ---------- */

    private fun resetInternal() {
        try {
            player.reset()
        } catch (_: Exception) {
        }
        preparing.set(false)
        state = PlayerState.IDLE
        playing.value = false
    }

    private fun updateMediaSession() {
        mediaSession.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, playingMusic?.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, playingMusic?.author)
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
                .setContentTitle(playingMusic?.title)
                .setContentText(playingMusic?.author)
                .setLargeIcon(playingBitmap)
                .setOngoing(true)
                .build()

        notificationManager.notify(1, notification)
    }

    private fun updateWidget() {
        val intent = Intent(context, MainActivity::class.java)
        widgetViews.setTextViewText(R.id.appwidget_text, playingMusic!!.title)
        widgetViews.setTextViewText(R.id.appwidget_artist, playingMusic!!.author)
        widgetViews.setImageViewBitmap(R.id.appwidget_artwork, playingBitmap!!)
        widgetViews.setOnClickPendingIntent(
            R.id.appwidget, PendingIntent.getActivity(
                context, 0,
                intent, PendingIntent.FLAG_IMMUTABLE
            )
        )
        val appWidgetIds =
            appWidgetManager.getAppWidgetIds(ComponentName(context, PlayBackWidget::class.java))
        for (i in appWidgetIds.indices) {
            appWidgetManager.updateAppWidget(appWidgetIds[i], widgetViews)
        }
    }

    /* ---------- 給 UI 用 ---------- */

    fun isPlaying(): Boolean = state == PlayerState.PLAYING
    fun duration(): Int = if (state >= PlayerState.PREPARED) player.duration else 0
    fun position(): Int = if (state >= PlayerState.PREPARED) player.currentPosition else 0
}
