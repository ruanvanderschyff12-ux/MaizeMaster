package com.maizemaster.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.*
import org.json.JSONArray

class FieldBoundaryActivity : Activity(), LocationListener {
    private val prefs by lazy { getSharedPreferences("maizemaster_boundaries", MODE_PRIVATE) }
    private var manager: LocationManager? = null
    private var recording = false
    private var fieldId = ""
    private var points = JSONArray()
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fieldId = intent.getStringExtra("field_id") ?: ""
        if (fieldId.isBlank()) { finish(); return }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 40, 32, 32)
        }
        status = TextView(this).apply { textSize = 17f }
        layout.addView(TextView(this).apply {
            text = "Record GPS Field Boundary"
            textSize = 24f
        })
        layout.addView(status)
        layout.addView(Button(this).apply {
            text = "Start walking field boundary"
            setOnClickListener { startRecording() }
        })
        layout.addView(Button(this).apply {
            text = "Finish and save boundary"
            setOnClickListener { saveBoundary() }
        })
        layout.addView(Button(this).apply {
            text = "Cancel recording"
            setOnClickListener { stopRecording(); finish() }
        })
        setContentView(layout)
        status.text = "Walk or drive around the outside edge of your field, then return to the starting point. GPS should be enabled outdoors."
    }

    private fun startRecording() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 801)
            return
        }
        points = JSONArray()
        recording = true
        manager = getSystemService(LOCATION_SERVICE) as LocationManager
        try {
            manager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000L, 4f, this)
            status.text = "Recording boundary... 0 GPS points"
        } catch (e: Exception) {
            recording = false
            status.text = "GPS unavailable: ${e.message}"
        }
    }

    override fun onLocationChanged(location: Location) {
        if (!recording || !location.hasAccuracy() || location.accuracy > 25f) return
        if (points.length() >= 4000) return
        val last = points.optJSONArray(points.length() - 1)
        if (last != null) {
            val previous = Location("boundary").apply {
                latitude = last.optDouble(0)
                longitude = last.optDouble(1)
            }
            if (previous.distanceTo(location) < 4f) return
        }
        points.put(JSONArray().put(location.latitude).put(location.longitude))
        status.text = "Recording boundary... ${points.length()} GPS points"
    }

    private fun saveBoundary() {
        if (points.length() < 3) {
            status.text = "At least 3 accurate GPS points are needed. Continue around the field."
            return
        }
        if (prefs.edit().putString("${fieldId}_boundary", points.toString()).commit()) {
            stopRecording()
            Toast.makeText(this, "Field GPS boundary saved", Toast.LENGTH_LONG).show()
            finish()
        } else status.text = "Unable to save boundary. Try again."
    }

    private fun stopRecording() {
        recording = false
        try { manager?.removeUpdates(this) } catch (_: Exception) {}
        manager = null
    }

    override fun onDestroy() {
        stopRecording()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == 801 && results.firstOrNull() == PackageManager.PERMISSION_GRANTED) startRecording()
        else status.text = "Precise location permission is needed."
    }
}
