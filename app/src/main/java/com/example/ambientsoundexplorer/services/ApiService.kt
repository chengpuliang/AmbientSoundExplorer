package com.example.ambientsoundexplorer.services

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ApiService {
    lateinit var endpoint: String
    lateinit var apiKey: String

    enum class sortOrder { ascending, descending }

    fun init(endpoint: String, apiKey: String) {
        this.endpoint = endpoint
        this.apiKey = apiKey
    }

    suspend fun getMusicList(sort: sortOrder, filter_term: String = ""): MutableList<Music> =
        withContext(Dispatchers.IO) {
            val connection =
                URL("$endpoint/music/list?sort_order=${sort.name}&filter_term=${filter_term}").openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("X-API-KEY", apiKey)
            connection.responseCode
            val data = connection.inputStream.bufferedReader().readText()
            val result = mutableListOf<Music>()
            val jsonArray = JSONArray(data)
            for (i in 0..<jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                result.add(
                    Music(
                        music_id = obj.getInt("music_id"),
                        title = obj.getString("title"),
                        date = obj.getString("date"),
                        author = obj.getString("author")
                    )
                )
            }
            connection.disconnect()
            result
        }

    suspend fun getReminderList(music_id: Int = -1): MutableList<Reminder> =
        withContext(Dispatchers.IO) {
            val result = mutableListOf<Reminder>()
            val connection =
                URL("$endpoint/reminders/list" + if (music_id != -1) "?music_id=${music_id}" else "").openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("X-API-KEY", apiKey)
            val data = connection.inputStream.bufferedReader().readText()
            val jsonArray = JSONArray(data)
            for (i in 0..<jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                result.add(
                    Reminder(
                        reminder_id = obj.getInt("reminder_id"),
                        hour = obj.getInt("hour"),
                        minute = obj.getInt("minute"),
                        music_id = obj.getInt("music_id"),
                        enabled = obj.getBoolean("enabled")
                    )
                )
            }
            connection.disconnect()
            result
        }

    suspend fun patchReminder(reminder: Reminder): Reminder =
        withContext(Dispatchers.IO) {
            val connection =
                URL("$endpoint/reminders/${reminder.reminder_id}").openConnection() as HttpURLConnection
            connection.requestMethod = "PATCH"
            connection.setRequestProperty("X-API-KEY", apiKey)
            connection.setRequestProperty("Content-Type", "application/json")


            OutputStreamWriter(connection.outputStream).apply {
                write(
                    JSONObject().apply {
                        put("hour", reminder.hour)
                        put("minute", reminder.minute)
                        put("enabled", reminder.enabled)
                    }.toString()
                )
                close()
            }


            val data = connection.inputStream.bufferedReader().readText()
            val obj = JSONObject(data)
            connection.disconnect()
            Reminder(
                reminder_id = obj.getInt("reminder_id"),
                hour = obj.getInt("hour"),
                minute = obj.getInt("minute"),
                music_id = obj.getInt("music_id"),
                enabled = obj.getBoolean("enabled")
            )
        }

    suspend fun getMusicPicture(music_id: Int): Bitmap =
        withContext(Dispatchers.IO) {
            val connection =
                URL("$endpoint/music/picture?music_id=$music_id").openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("X-API-KEY", apiKey)
            connection.setRequestProperty("accept", "image/jpeg")
            val result = BitmapFactory.decodeStream(connection.inputStream)
            connection.disconnect()
            result
        }
}

data class Music(
    val music_id: Int,
    val title: String,
    val date: String,
    val author: String
)

data class Reminder(
    val reminder_id: Int,
    val hour: Int,
    val minute: Int,
    val music_id: Int,
    var enabled: Boolean
)