package com.maizemaster.app

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import org.json.JSONArray
import org.json.JSONObject

/** GPS records distance, not crop weight. Foreground notification remains while tracking. */
class HarvestGpsService : Service(), LocationListener {
    private val preferences by lazy { getSharedPreferences("maizemaster_harvest_trips", MODE_PRIVATE) }
    private var fieldId: String? = null
    private var previous: Location? = null
    private var manager: LocationManager? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopTracking()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val id = intent.getStringExtra(EXTRA_FIELD) ?: return START_NOT_STICKY
                if (preferences.getString("${id}_active", null) == null) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                // A new trip must not inherit the previous trip’s GPS position or distance baseline.
                try { manager?.removeUpdates(this) } catch (_: Exception) {}
                manager = null
                previous = null
                fieldId = id
                startTracking()
            }
            else -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_NOT_STICKY
    }

    private fun startTracking() {
        val id = fieldId ?: return
        val channel = "harvest_gps"
        val notifications = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            notifications.createNotificationChannel(NotificationChannel(channel, "Harvest GPS tracking", NotificationManager.IMPORTANCE_LOW))
        }
        val open = Intent(this, HarvestActivity::class.java)
        val pending = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(this, channel)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("MaizeMaster harvest GPS active")
            .setContentText("Recording harvester movement. Tap to return.")
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(5501, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else startForeground(5501, notification)
        val fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine) {
            preferences.edit().putString("${id}_gps_error", "Precise location permission is required.").apply()
            stopTracking()
            return
        }
        try {
            manager = getSystemService(LOCATION_SERVICE) as LocationManager
            manager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this)
            // Seed the marker immediately when a recent accurate fix exists, even before movement.
            @Suppress("DEPRECATION")
            val recent = manager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (recent != null && System.currentTimeMillis() - recent.time in 0..30000 &&
                recent.hasAccuracy() && recent.accuracy <= 40f) {
                onLocationChanged(recent)
            }
            if (manager?.isProviderEnabled(LocationManager.GPS_PROVIDER) != true) {
                preferences.edit().putString("${id}_gps_error", "Enable GPS location on your phone.").apply()
            }
        } catch (e: Exception) {
            preferences.edit().putString("${id}_gps_error", "GPS tracking unavailable: ${e.message}").apply()
            stopTracking()
        }
    }

    override fun onLocationChanged(location: Location) {
        val id = fieldId ?: return
        if (preferences.getString("${id}_active", null) == null) { stopTracking(); return }
        if (!location.hasAccuracy() || location.accuracy > 40f) {
            preferences.edit().putString("${id}_gps_error", "Waiting for a stronger GPS signal. Try outdoors.").apply()
            return
        }
        preferences.edit().remove("${id}_gps_error").putString("${id}_live_position", JSONArray().put(location.latitude).put(location.longitude).toString()).apply()
        if (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos > 15_000_000_000L) return
        val last = previous
        previous = location
        if (last == null) {
            saveRoutePoint(id, location)
            return
        }
        val seconds = (location.elapsedRealtimeNanos - last.elapsedRealtimeNanos) / 1e9
        if (seconds <= 0 || seconds > 15) return
        val meters = last.distanceTo(location).toDouble()
        if (meters < 3.0 || meters > 8.0 * seconds) return
        saveRoutePoint(id, location)
        val current = preferences.getFloat("${id}_gps_meters", 0f)
        preferences.edit()
            .putFloat("${id}_gps_meters", (current + meters).toFloat())
            .putInt("${id}_gps_points", preferences.getInt("${id}_gps_points", 0) + 1)
            .apply()
    }

    private fun saveRoutePoint(id: String, location: Location) {
        try {
            val key = "${id}_gps_route"
            val route = JSONArray(preferences.getString(key, "[]"))
            if (route.length() >= 3000) return
            route.put(JSONArray().put(location.latitude).put(location.longitude))
            preferences.edit().putString(key, route.toString()).apply()
        } catch (_: Exception) {
            preferences.edit().putString("${id}_gps_error", "Unable to save GPS route").apply()
        }
    }

    private fun stopTracking() {
        try { manager?.removeUpdates(this) } catch (_: Exception) {}
        manager = null
        previous = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        try { manager?.removeUpdates(this) } catch (_: Exception) {}
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.maizemaster.app.START_HARVEST_GPS"
        const val ACTION_STOP = "com.maizemaster.app.STOP_HARVEST_GPS"
        const val EXTRA_FIELD = "field_id"
    }
}
