package com.maizemaster.app

import org.json.JSONArray
import kotlin.math.*

/** Offline grid approximation of uniquely harvested area; excludes prior swaths. */
object HarvestOverlapEstimator {
    private const val CELL = 2.0
    fun estimate(route: JSONArray, history: JSONArray, width: Double, boundary: JSONArray, remaining: Double): Double? {
        if (route.length() < 2 || width !in 0.1..30.0) return null
        val origin = route.optJSONArray(0) ?: return null
        val lat0 = origin.optDouble(0, Double.NaN)
        val lon0 = origin.optDouble(1, Double.NaN)
        if (!lat0.isFinite() || !lon0.isFinite()) return null
        val factor = cos(Math.toRadians(lat0)).coerceAtLeast(0.01)
        fun points(data: JSONArray): List<Pair<Double, Double>> = (0 until data.length()).mapNotNull { i ->
            val p = data.optJSONArray(i) ?: return@mapNotNull null
            val lat = p.optDouble(0, Double.NaN)
            val lon = p.optDouble(1, Double.NaN)
            if (!lat.isFinite() || !lon.isFinite()) null
            else Pair((lon - lon0) * 111320.0 * factor, (lat - lat0) * 111320.0)
        }
        val outline = points(boundary)
        fun inside(x: Double, y: Double): Boolean {
            if (outline.size < 3) return true
            var result = false
            for (i in outline.indices) {
                val a = outline[i]
                val b = outline[(i + 1) % outline.size]
                if ((a.second > y) != (b.second > y) &&
                    x < (b.first - a.first) * (y - a.second) / (b.second - a.second) + a.first) result = !result
            }
            return result
        }
        fun key(x: Int, y: Int) = (x.toLong() shl 32) xor (y.toLong() and 0xffffffffL)
        val used = HashSet<Long>()
        fun cover(data: JSONArray, cuttingWidth: Double) {
            val pts = points(data)
            val radius = cuttingWidth / 2.0
            val reach = ceil(radius / CELL).toInt() + 1
            for (i in 1 until pts.size) {
                val a = pts[i - 1]
                val b = pts[i]
                val distance = hypot(b.first - a.first, b.second - a.second)
                if (distance < 0.01 || distance > 35.0) continue
                val steps = ceil(distance / CELL).toInt().coerceAtLeast(1)
                for (step in 0..steps) {
                    val t = step.toDouble() / steps
                    val x = a.first + (b.first - a.first) * t
                    val y = a.second + (b.second - a.second) * t
                    val cx = floor(x / CELL).toInt()
                    val cy = floor(y / CELL).toInt()
                    for (dx in -reach..reach) for (dy in -reach..reach) {
                        val px = (cx + dx + 0.5) * CELL
                        val py = (cy + dy + 0.5) * CELL
                        if (hypot(px - x, py - y) <= radius && inside(px, py)) used.add(key(cx + dx, cy + dy))
                    }
                }
            }
        }
        for (i in 0 until history.length()) {
            val trip = history.optJSONObject(i) ?: continue
            cover(trip.optJSONArray("route") ?: continue, trip.optDouble("cuttingWidth", 6.0).coerceIn(0.1, 30.0))
        }
        val old = used.size
        cover(route, width)
        return ((used.size - old) * CELL * CELL / 10000.0).coerceIn(0.0, remaining)
    }
}
