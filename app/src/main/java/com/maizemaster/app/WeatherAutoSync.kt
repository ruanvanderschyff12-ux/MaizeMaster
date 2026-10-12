package com.maizemaster.app

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Refreshes weather for all fields with saved coordinates when dashboard opens. */
object WeatherAutoSync {
    fun refresh(context: Context) {
        val app = context.applicationContext
        val fields = FieldStorage(app).loadFields()
        val locations = app.getSharedPreferences("maizemaster_locations", Context.MODE_PRIVATE)
        val cache = app.getSharedPreferences("maizemaster_weather_cache", Context.MODE_PRIVATE)
        Thread {
            for (field in fields) {
                val lat = locations.getString("${field.id}_lat", null)?.toDoubleOrNull()
                val lon = locations.getString("${field.id}_lon", null)?.toDoubleOrNull()
                if (lat == null || lon == null || !lat.isFinite() || !lon.isFinite() ||
                    lat !in -90.0..90.0 || lon !in -180.0..180.0) continue
                val now = System.currentTimeMillis()
                if (now - cache.getLong("${field.id}_updated", 0L) < 3600000L) continue
                var connection: HttpURLConnection? = null
                try {
                    val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                        "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m" +
                        "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum&timezone=auto&forecast_days=7"
                    connection = URL(url).openConnection() as HttpURLConnection
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    if (connection.responseCode != 200) continue
                    val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                    val current = json.getJSONObject("current")
                    val daily = json.getJSONObject("daily")
                    val historyKey = "${field.id}_history"
                    val old = try { JSONArray(cache.getString(historyKey, "[]")) } catch (_: Exception) { JSONArray() }
                    old.put(JSONObject().apply {
                        put("time", now)
                        put("temperature", current.optDouble("temperature_2m"))
                        put("humidity", current.optDouble("relative_humidity_2m"))
                        put("wind", current.optDouble("wind_speed_10m"))
                        put("weatherCode", current.optInt("weather_code"))
                    })
                    val history = JSONArray()
                    for (i in maxOf(0, old.length() - 90) until old.length()) history.put(old.get(i))
                    val forecast = StringBuilder("🌽 ${field.name}\nUpdated automatically\n\n")
                    val dates = daily.getJSONArray("time")
                    for (i in 0 until dates.length()) {
                        forecast.append("${dates.getString(i)}: ")
                        forecast.append("${daily.getJSONArray("temperature_2m_min").optDouble(i)}–")
                        forecast.append("${daily.getJSONArray("temperature_2m_max").optDouble(i)}°C, ")
                        forecast.append("rain ${daily.getJSONArray("precipitation_sum").optDouble(i)} mm\n")
                    }
                    cache.edit()
                        .putString(historyKey, history.toString())
                        .putString("${field.id}_forecast", forecast.toString())
                        .putLong("${field.id}_updated", now)
                        .apply()
                } catch (e: Exception) {
                    Log.w("MaizeMaster", "Weather sync failed for field ${field.id}", e)
                } finally {
                    connection?.disconnect()
                }
            }
        }.start()
    }
}
