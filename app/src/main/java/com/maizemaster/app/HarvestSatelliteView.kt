package com.maizemaster.app

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import org.json.JSONArray

/** Satellite map for an existing GPS field and recorded harvest routes. */
class HarvestSatelliteView(context: Context) : FrameLayout(context) {
    private val mapView = MapView(context)
    private var map: GoogleMap? = null
    private var boundary = JSONArray()
    private var route = JSONArray()
    private var live = JSONArray()
    private var history = JSONArray()
    private var cuttingWidth = 6.0
    private var centered = false
    init {
        addView(mapView, LayoutParams(-1, -1))
        mapView.onCreate(null)
        mapView.getMapAsync { googleMap ->
            map = googleMap
            googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            googleMap.uiSettings.isZoomControlsEnabled = true
            googleMap.uiSettings.isCompassEnabled = true
            redraw()
        }
    }
    fun update(boundaryPoints: JSONArray, routePoints: JSONArray, position: JSONArray, oldTrips: JSONArray, width: Double) {
        boundary = boundaryPoints; route = routePoints; live = position
        history = oldTrips; cuttingWidth = width
        redraw()
    }
    private fun coordinates(array: JSONArray): List<LatLng> = (0 until array.length()).mapNotNull {
        val p = array.optJSONArray(it) ?: return@mapNotNull null
        val lat = p.optDouble(0, Double.NaN); val lon = p.optDouble(1, Double.NaN)
        if (lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0) LatLng(lat, lon) else null
    }
    private fun redraw() {
        val m = map ?: return
        m.clear()
        val outline = coordinates(boundary)
        if (outline.size >= 3) m.addPolygon(PolygonOptions().addAll(outline)
            .strokeColor(Color.rgb(80, 230, 110)).strokeWidth(5f)
            .fillColor(Color.argb(30, 40, 160, 60)))
        for (i in 0 until history.length()) {
            val trip = history.optJSONObject(i) ?: continue
            val points = coordinates(trip.optJSONArray("route") ?: JSONArray())
            if (points.size >= 2) m.addPolyline(PolylineOptions().addAll(points)
                .color(Color.argb(175, 133, 80, 35)).width(14f))
        }
        val active = coordinates(route)
        if (active.size >= 2) m.addPolyline(PolylineOptions().addAll(active)
            .color(Color.rgb(155, 94, 42)).width(14f))
        val current = coordinates(live).firstOrNull()
        if (current != null) m.addCircle(CircleOptions().center(current).radius(4.0)
            .fillColor(Color.rgb(0, 115, 245)).strokeColor(Color.WHITE).strokeWidth(3f))
        if (!centered) {
            val targets = if (outline.isNotEmpty()) outline else if (active.isNotEmpty()) active else listOfNotNull(current)
            if (targets.isNotEmpty()) {
                val bounds = LatLngBounds.builder().apply { targets.forEach { include(it) } }.build()
                m.moveCamera(if (targets.size == 1) CameraUpdateFactory.newLatLngZoom(targets[0], 17f)
                    else CameraUpdateFactory.newLatLngBounds(bounds, 60))
                centered = true
            }
        }
    }
    fun resumeMap() = mapView.onResume()
    fun pauseMap() = mapView.onPause()
    fun destroyMap() = mapView.onDestroy()
    fun lowMemory() = mapView.onLowMemory()
}
