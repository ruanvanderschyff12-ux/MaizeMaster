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
import kotlin.math.*

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
        status.text = "Recording ${points.length()} GPS points | Approx. %.2f ha".format(calculateAreaHectares())
    }

    private fun calculateAreaHectares(): Double {
        if (points.length() < 3) return 0.0
        val lat0 = (0 until points.length()).map { points.getJSONArray(it).getDouble(0) }.average()
        val lon0 = (0 until points.length()).map { points.getJSONArray(it).getDouble(1) }.average()
        val factor = 111320.0 * cos(Math.toRadians(lat0))
        var twiceArea = 0.0
        for (i in 0 until points.length()) {
            val a = points.getJSONArray(i)
            val b = points.getJSONArray((i + 1) % points.length())
            val ax = (a.getDouble(1) - lon0) * factor
            val ay = (a.getDouble(0) - lat0) * 111320.0
            val bx = (b.getDouble(1) - lon0) * factor
            val by = (b.getDouble(0) - lat0) * 111320.0
            twiceArea += ax * by - bx * ay
        }
        return abs(twiceArea) / 20000.0
    }

    private fun saveBoundary() {
        if (points.length() < 3) {
            status.text = "At least 3 accurate GPS points are needed. Continue around the field."
            return
        }
        val hectares = calculateAreaHectares()
        if (!hectares.isFinite() || hectares < 0.001) {
            status.text = "GPS boundary too small or inaccurate. Continue around the field."
            return
        }
        if (prefs.edit().putString("${fieldId}_boundary", points.toString()).putFloat("${fieldId}_hectares", hectares.toFloat()).commit()) {
            stopRecording()
            Toast.makeText(this, "GPS boundary saved: %.2f hectares".format(hectares), Toast.LENGTH_LONG).show()
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
