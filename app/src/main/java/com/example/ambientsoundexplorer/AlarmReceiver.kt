package com.example.ambientsoundexplorer

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.equals("com.example.ambientsoundexplorer.alarm")) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(
                0, Notification.Builder(context, "Reminder")
                    .setContentTitle("該聽 " + intent.getStringExtra("musicTitle") + " 囉！")
                    .setSmallIcon(R.drawable.outline_music_note_24)
                    .setContentIntent(
                        PendingIntent.getActivity(
                            context,
                            0,
                            Intent(
                                context.applicationContext,
                                MainActivity::class.java
                            ).apply {
                                putExtra("musicId", intent.getIntExtra("musicId", 0))
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            },
                            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    )
                    .build()
            )
        }
    }
}