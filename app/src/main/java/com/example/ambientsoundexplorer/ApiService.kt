package com.example.ambientsoundexplorer

import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class ApiService(val endpoint: String, val apiKey: String) {
    enum class sortOrder {ascending,descending}
    fun getMusicList(sort: sortOrder, filter_term: String = "", action: (result: MutableList<Music>) -> Unit) {
        println("fuck")
        val connection =
            URL("$endpoint/music/list?sort_order=${sort.name}&filter_term=${filter_term}").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("X-API-KEY", apiKey)
        try {
            val respCode = connection.responseCode
            if (respCode == HttpURLConnection.HTTP_OK) {
                val data = connection.getInputStream().bufferedReader().readText()
                var result = mutableListOf<Music>()
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
                action(result)
            } else {

            }
        } catch (e: Exception) {
            println(e)
        } finally {
            connection.disconnect()
        }
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
    val enabled: Boolean
)