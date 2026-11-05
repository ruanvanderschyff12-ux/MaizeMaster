package com.maizemaster.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import org.json.JSONArray
import kotlin.math.cos
import kotlin.math.max

class HarvestRouteView(context: Context) : View(context) {
    var gpsBoundary = JSONArray()
        set(value) { field = value; invalidate() }
    var rowSpacing = 0.75
        set(value) { field = value.coerceAtLeast(0.1); invalidate() }
    var fieldShape = "Rectangle"
        set(value) { field = value; invalidate() }
    var fieldWidth = 200.0
        set(value) { field = value.coerceAtLeast(1.0); invalidate() }
    var fieldLength = 300.0
        set(value) { field = value.coerceAtLeast(1.0); invalidate() }
    var cuttingWidth = 6.0
        set(value) { field = value.coerceIn(0.5, 30.0); invalidate() }
    var previousRoutes = JSONArray()
        set(value) { field = value; invalidate() }
    var livePosition = JSONArray()
        set(value) { field = value; invalidate() }
    var route = JSONArray()
        set(value) { field = value; invalidate() }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var followLocation = false
        set(value) { field = value; zoom = 1f; panX = 0f; panY = 0f; invalidate() }
    private var zoom = 1f
    private var panX = 0f
    private var panY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private val zoomDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                zoom = (zoom * detector.scaleFactor).coerceIn(1f, 8f)
                invalidate()
                return true
            }
        })

    // This preview is read-only so vertical swipes always reach the parent ScrollView.
    // GPS recording and live route drawing continue independently.
    override fun onTouchEvent(event: MotionEvent): Boolean {
        zoomDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 1 && !zoomDetector.isInProgress) {
                    panX += event.x - lastX
                    panY += event.y - lastY
                    invalidate()
                }
                lastX = event.x
                lastY = event.y
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return true
    }

    private fun drawMaizeField(canvas: Canvas, outline: Path? = null) {
        val density = resources.displayMetrics.density
        val pad = 12f * density
        val displayW = (width - 2f * pad).coerceAtLeast(1f)
        val displayH = (height - 2f * pad).coerceAtLeast(1f)
        val actualW = fieldWidth.coerceAtLeast(1.0)
        val actualH = if (fieldShape == "Circle") actualW else fieldLength.coerceAtLeast(1.0)
        val worldW = actualW
        val worldH = actualH
        val fit = minOf(displayW / worldW, displayH / worldH).toFloat()
        val drawW = worldW.toFloat() * fit
        val drawH = worldH.toFloat() * fit
        val bounds = if (outline != null) {
            val result = RectF()
            outline.computeBounds(result, true)
            result
        } else RectF((width - drawW) / 2f, (height - drawH) / 2f,
            (width + drawW) / 2f, (height + drawH) / 2f)
        val shapePath = Path().apply {
            if (outline != null) addPath(outline)
            else if (fieldShape == "Circle" || fieldShape == "Oval") addOval(bounds, Path.Direction.CW)
            else addRoundRect(bounds, 5f * density, 5f * density, Path.Direction.CW)
        }
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(117, 78, 42)
        canvas.drawPath(shapePath, paint)
        canvas.save()
        canvas.clipPath(shapePath)
        val displayRows = kotlin.math.min(16, kotlin.math.max(2, kotlin.math.ceil(actualW / rowSpacing).toInt()))
        val spacing = bounds.width() / displayRows.coerceAtLeast(1)
        // Alternating soil tracks make rows readable at different zoom levels.
        var row = bounds.left + spacing * 0.5f
        while (row < bounds.right) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = spacing * 0.56f
            paint.color = Color.rgb(45, 113, 43)
            canvas.drawLine(row, bounds.top, row, bounds.bottom, paint)
            val plantGap = bounds.height() / kotlin.math.min(26, kotlin.math.max(2, (bounds.height() / 14f).toInt()))
            var plantX = bounds.top + plantGap * 0.5f
            while (plantX < bounds.bottom) {
                val r = spacing * 0.44f
                paint.style = Paint.Style.FILL
                paint.color = Color.rgb(24, 102, 37)
                canvas.drawOval(RectF(row-r, plantX-r*0.25f, row+r, plantX+r*0.25f), paint)
                canvas.drawOval(RectF(row-r*0.25f, plantX-r, row+r*0.25f, plantX+r), paint)
                paint.color = Color.rgb(105, 187, 55)
                canvas.drawOval(RectF(row-r*0.83f, plantX-r*0.20f, row+r*0.83f, plantX+r*0.20f), paint)
                canvas.drawOval(RectF(row-r*0.20f, plantX-r*0.83f, row+r*0.20f, plantX+r*0.83f), paint)
                paint.color = Color.rgb(224, 186, 65)
                canvas.drawCircle(row, plantX, r*0.14f, paint)
                plantX += plantGap
            }
            row += spacing
        }
        canvas.restore()
        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(42, 82, 43)
        paint.strokeWidth = 2f * density
        canvas.drawPath(shapePath, paint)
        paint.style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(231, 243, 231))
        canvas.save()
        canvas.translate(width / 2f + panX, height / 2f + panY)
        canvas.scale(zoom, zoom)
        canvas.translate(-width / 2f, -height / 2f)
        // Completed GPS passes are drawn in brown, even when no trip is active.
        // Keep the live route green until the operator confirms the load.
        val points = mutableListOf<Pair<Double, Double>>()
        for (i in 0 until route.length()) {
            val item = route.optJSONArray(i) ?: continue
            val lat = item.optDouble(0, Double.NaN)
            val lon = item.optDouble(1, Double.NaN)
            if (lat.isFinite() && lon.isFinite()) points.add(lat to lon)
        }
        val boundaryPoints = mutableListOf<Pair<Double, Double>>()
        for (i in 0 until gpsBoundary.length()) {
            val item = gpsBoundary.optJSONArray(i) ?: continue
            val lat = item.optDouble(0, Double.NaN)
            val lon = item.optDouble(1, Double.NaN)
            if (lat.isFinite() && lon.isFinite()) boundaryPoints.add(lat to lon)
        }
        if (boundaryPoints.size < 3) {
            drawMaizeField(canvas)
            val liveLat = livePosition.optDouble(0, Double.NaN)
            val liveLon = livePosition.optDouble(1, Double.NaN)
            val hasLive = liveLat.isFinite() && liveLon.isFinite()
            val anchor = if (hasLive) Pair(liveLat, liveLon) else points.lastOrNull()
            if (anchor != null) {
                val factor = cos(Math.toRadians(anchor.first)).coerceAtLeast(0.01)
                fun local(p: Pair<Double, Double>) = Pair(
                    (p.second - anchor.second) * 111320.0 * factor,
                    (p.first - anchor.first) * 111320.0)
                // Only current route determines zoom; distant old routes cannot hide the blue dot.
                val current = points.map(::local)
                val extent = max(40.0, max(
                    (current.maxOfOrNull { it.first } ?: 0.0) - (current.minOfOrNull { it.first } ?: 0.0),
                    (current.maxOfOrNull { it.second } ?: 0.0) - (current.minOfOrNull { it.second } ?: 0.0)))
                val scale = minOf(width, height) * 0.65 / extent
                fun px(p: Pair<Double, Double>) = (width / 2.0 + p.first * scale).toFloat()
                fun py(p: Pair<Double, Double>) = (height / 2.0 - p.second * scale).toFloat()
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND
                paint.color = Color.rgb(143, 99, 57)
                for (j in 0 until previousRoutes.length()) {
                    val trip = previousRoutes.optJSONObject(j) ?: continue
                    val saved = trip.optJSONArray("route") ?: continue
                    val historyPoints = mutableListOf<Pair<Double, Double>>()
                    for (k in 0 until saved.length()) {
                        val item = saved.optJSONArray(k) ?: continue
                        val lat = item.optDouble(0, Double.NaN)
                        val lon = item.optDouble(1, Double.NaN)
                        if (lat.isFinite() && lon.isFinite()) historyPoints.add(lat to lon)
                    }
                    val oldPath = Path()
                    var begun = false
                    for (point in historyPoints) {
                        val p = local(point)
                        if (kotlin.math.abs(p.first) > extent || kotlin.math.abs(p.second) > extent) {
                            begun = false
                            continue
                        }
                        if (!begun) { oldPath.moveTo(px(p), py(p)); begun = true }
                        else oldPath.lineTo(px(p), py(p))
                    }
                    paint.strokeWidth = (trip.optDouble("cuttingWidth", 6.0).coerceIn(0.5, 30.0) * scale).toFloat().coerceAtLeast(2f)
                    canvas.drawPath(oldPath, paint)
                }
                if (current.size >= 2) {
                    val path = Path().apply {
                        moveTo(px(current.first()), py(current.first()))
                        for (i in 1 until current.size) lineTo(px(current[i]), py(current[i]))
                    }
                    paint.color = Color.rgb(23, 144, 71)
                    paint.strokeWidth = 5f * resources.displayMetrics.density
                    canvas.drawPath(path, paint)
                }
                if (hasLive) {
                    val density = resources.displayMetrics.density
                    paint.style = Paint.Style.FILL
                    paint.color = Color.argb(70, 0, 108, 226)
                    canvas.drawCircle(width / 2f, height / 2f, 16f * density, paint)
                    paint.color = Color.WHITE
                    canvas.drawCircle(width / 2f, height / 2f, 10f * density, paint)
                    paint.color = Color.rgb(0, 108, 226)
                    canvas.drawCircle(width / 2f, height / 2f, 7f * density, paint)
                }
            }
            paint.style = Paint.Style.FILL
            paint.textSize = 11f * resources.displayMetrics.density
            paint.color = Color.rgb(8, 65, 40)
            canvas.drawText("GPS route • field outline not aligned", 10f * resources.displayMetrics.density, 18f * resources.displayMetrics.density, paint)
            canvas.restore()
            return
        }
        val latCenter = boundaryPoints.map { it.first }.average()
        val lonCenter = boundaryPoints.map { it.second }.average()
        val factor = cos(Math.toRadians(latCenter)).coerceAtLeast(0.01)
        val east = points.map { (it.second - lonCenter) * 111320.0 * factor }
        val north = points.map { (it.first - latCenter) * 111320.0 }
        val boundaryEast = boundaryPoints.map { (it.second - lonCenter) * 111320.0 * factor }
        val boundaryNorth = boundaryPoints.map { (it.first - latCenter) * 111320.0 }
        val liveLat = livePosition.optDouble(0, Double.NaN)
        val liveLon = livePosition.optDouble(1, Double.NaN)
        val hasLive = liveLat.isFinite() && liveLon.isFinite()
        val liveEast = if (hasLive) (liveLon - lonCenter) * 111320.0 * factor else 0.0
        val liveNorth = if (hasLive) (liveLat - latCenter) * 111320.0 else 0.0
        val west = boundaryEast.minOrNull() ?: 0.0
        val eastLimit = boundaryEast.maxOrNull() ?: 0.0
        val south = boundaryNorth.minOrNull() ?: 0.0
        val northLimit = boundaryNorth.maxOrNull() ?: 0.0
        val centerEast = if (followLocation && hasLive) liveEast else (west + eastLimit) / 2.0
        val centerNorth = if (followLocation && hasLive) liveNorth else (south + northLimit) / 2.0
        val extent = max(20.0, max(eastLimit - west, northLimit - south))
        val scale = (minOf(width, height) * 0.8) / extent
        fun x(i: Int) = (width / 2.0 + (east[i] - centerEast) * scale).toFloat()
        fun y(i: Int) = (height / 2.0 - (north[i] - centerNorth) * scale).toFloat()
        val gpsOutline = Path().apply {
            for (i in boundaryPoints.indices) {
                val px = (width / 2.0 + (boundaryEast[i] - centerEast) * scale).toFloat()
                val py = (height / 2.0 - (boundaryNorth[i] - centerNorth) * scale).toFloat()
                if (i == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }
        drawMaizeField(canvas, gpsOutline)
        if (points.isEmpty()) {
            if (livePosition.length() >= 2) {
                val lat = livePosition.optDouble(0, Double.NaN)
                val lon = livePosition.optDouble(1, Double.NaN)
                if (lat.isFinite() && lon.isFinite()) {
                    val px = (width / 2.0 + ((lon - lonCenter) * 111320.0 * factor - centerEast) * scale).toFloat()
                    val py = (height / 2.0 - ((lat - latCenter) * 111320.0 - centerNorth) * scale).toFloat()
                    paint.style = Paint.Style.FILL
                    paint.color = Color.rgb(0, 108, 226)
                    canvas.drawCircle(px, py, 9f * resources.displayMetrics.density, paint)
                }
            }
            canvas.restore()
            return
        }
        val pad = 16f * resources.displayMetrics.density
        val fit = minOf((width - 2f * pad) / fieldWidth.toFloat(),
            (height - 2f * pad) / (if (fieldShape == "Circle") fieldWidth else fieldLength).toFloat())
        val fieldW = fieldWidth.toFloat() * fit
        val fieldH = (if (fieldShape == "Circle") fieldWidth else fieldLength).toFloat() * fit
        val bounds = RectF((width - fieldW) / 2f, (height - fieldH) / 2f,
            (width + fieldW) / 2f, (height + fieldH) / 2f)
        val boundary = Path().apply {
            if (fieldShape == "Oval" || fieldShape == "Circle") addOval(bounds, Path.Direction.CW)
            else addRect(bounds, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(gpsOutline)
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.rgb(143, 99, 57)
        for (j in 0 until previousRoutes.length()) {
            val trip = previousRoutes.optJSONObject(j) ?: continue
            val savedRoute = trip.optJSONArray("route") ?: continue
            val oldPath = Path()
            var count = 0
            for (k in 0 until savedRoute.length()) {
                val coord = savedRoute.optJSONArray(k) ?: continue
                val lat = coord.optDouble(0, Double.NaN)
                val lon = coord.optDouble(1, Double.NaN)
                if (!lat.isFinite() || !lon.isFinite()) continue
                val px = (width / 2.0 + ((lon - lonCenter) * 111320.0 * factor - centerEast) * scale).toFloat()
                val py = (height / 2.0 - ((lat - latCenter) * 111320.0 - centerNorth) * scale).toFloat()
                if (count++ == 0) oldPath.moveTo(px, py) else oldPath.lineTo(px, py)
            }
            if (count > 1) {
                paint.strokeWidth = (trip.optDouble("cuttingWidth", 6.0).coerceIn(0.5, 30.0) * scale).toFloat().coerceAtLeast(1f)
                canvas.drawPath(oldPath, paint)
            }
        }
        val path = Path()
        if (points.isNotEmpty()) path.moveTo(x(0), y(0))
        for (i in 1 until points.size) path.lineTo(x(i), y(i))
        paint.color = Color.rgb(143, 99, 57)
        paint.strokeWidth = (cuttingWidth * scale).toFloat().coerceAtLeast(1f)
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(143, 99, 57)
        val radius = (cuttingWidth * scale / 2.0).toFloat().coerceAtLeast(0.5f)
        for (i in points.indices) canvas.drawCircle(x(i), y(i), radius, paint)
        canvas.restore()
        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(20, 90, 45)
        paint.strokeWidth = 3f * resources.displayMetrics.density
        canvas.drawPath(gpsOutline, paint)
        paint.color = Color.rgb(23, 144, 71)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 7f * resources.displayMetrics.density
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(10, 76, 46)
        if (points.isNotEmpty()) canvas.drawCircle(x(points.lastIndex), y(points.lastIndex), 4f * resources.displayMetrics.density, paint)
        if (livePosition.length() >= 2) {
            val lat = livePosition.optDouble(0, Double.NaN)
            val lon = livePosition.optDouble(1, Double.NaN)
            if (lat.isFinite() && lon.isFinite()) {
                paint.color = Color.rgb(0, 108, 226)
                canvas.drawCircle((width / 2.0 + ((lon - lonCenter) * 111320.0 * factor - centerEast) * scale).toFloat(),
                    (height / 2.0 - ((lat - latCenter) * 111320.0 - centerNorth) * scale).toFloat(),
                    9f * resources.displayMetrics.density, paint)
            }
        }
        canvas.restore()
    }
}
