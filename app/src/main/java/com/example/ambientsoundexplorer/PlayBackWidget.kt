package com.example.ambientsoundexplorer

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.ambientsoundexplorer.services.PlayerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


/**
 * Implementation of App Widget functionality.
 */
class PlayBackWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        println(intent?.action)
        if (intent?.action.equals("com.example.ambientsoundexplorer.action.WidgetPlayPause")) {
            if (PlayerService.isPlaying()) PlayerService.pause() else PlayerService.start()
        } else if (intent?.action.equals("com.example.ambientsoundexplorer.action.WidgetSkipPrevious")) {
            CoroutineScope(Dispatchers.IO).launch {
                PlayerService.playPrevious()
            }
        } else if (intent?.action.equals("com.example.ambientsoundexplorer.action.WidgetSkipNext")) {
            CoroutineScope(Dispatchers.IO).launch {
                PlayerService.playNext()
            }
        }
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val views = RemoteViews(context.packageName, R.layout.play_back_widget)

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}