
package com.maizemaster.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import org.json.JSONArray
import kotlin.math.cos
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class FieldDesignerView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var editingBoundary = false
        set(value) { field = value; invalidate() }
    private var activeVertex = -1
    private var gpsCenterLat = 0.0
    private var gpsCenterLon = 0.0
    private var gpsFactor = 1.0
    private var gpsScale = 1.0
    private var gpsCenterE = 0.0
    private var gpsCenterN = 0.0
    private var zoom = 1f
    private var panX = 0f
    private var panY = 0f
    private var touchX = 0f
    private var touchY = 0f
    private val pinch = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                zoom = (zoom * detector.scaleFactor).coerceIn(1f, 12f)
                invalidate()
                return true
            }
        })
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!editingBoundary) pinch.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activeVertex = if (editingBoundary) findVertex(event.x, event.y) else -1
                touchX = event.x; touchY = event.y
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                if (editingBoundary && activeVertex >= 0) {
                    moveVertex(activeVertex, event.x, event.y)
                } else if (!editingBoundary && !pinch.isInProgress && event.pointerCount == 1) {
                    panX += event.x - touchX
                    panY += event.y - touchY
                    invalidate()
                }
                touchX = event.x; touchY = event.y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activeVertex = -1
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }
    private var cachedBitmap: Bitmap? = null
    private var needsRedraw = true
    // Limit preview rendering work; the saved row count remains exact.
    private val maxPreviewRows = 420

    var gpsBoundary = JSONArray()
        set(value) { field = value; refreshField() }

    var growthDays = 45
        set(value) { field = value.coerceAtLeast(0); refreshField() }

    var fieldWidth = 200f
        set(value) {
            val newValue = value.coerceAtLeast(1f)
            if (field != newValue) {
                field = newValue
                refreshField()
            }
        }

    var fieldLength = 300f
        set(value) {
            val newValue = value.coerceAtLeast(1f)
            if (field != newValue) {
                field = newValue
                refreshField()
            }
        }

    var rowCount = 0
        set(value) { field = value.coerceAtLeast(0); refreshField() }
    var rowsHorizontal = false
        set(value) { field = value; refreshField() }
    fun zoomIn() { zoom = (zoom * 1.5f).coerceAtMost(12f); invalidate() }
    fun zoomOut() { zoom = (zoom / 1.5f).coerceAtLeast(1f); if (zoom == 1f) { panX = 0f; panY = 0f }; invalidate() }
    fun resetZoom() { zoom = 1f; panX = 0f; panY = 0f; invalidate() }

    var rowSpacing = 0.75f
        set(value) {
            val newValue = value.coerceAtLeast(0.1f)
            if (field != newValue) {
                field = newValue
                refreshField()
            }
        }

    var fieldShape = "Rectangle"
        set(value) {
            if (field != value) {
                field = value
                refreshField()
            }
        }

    private fun refreshField() {
        needsRedraw = true
        invalidate()
    }

    override fun onSizeChanged(
        w: Int,
        h: Int,
        oldw: Int,
        oldh: Int
    ) {
        super.onSizeChanged(w, h, oldw, oldh)
        needsRedraw = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (width <= 0 || height <= 0) return

        if (needsRedraw ||
            cachedBitmap?.width != width ||
            cachedBitmap?.height != height
        ) {
            cachedBitmap?.recycle()

            cachedBitmap = Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
            )

            cachedBitmap?.let {
                drawField(Canvas(it))
            }

            needsRedraw = false
        }

        cachedBitmap?.let {
            paint.style = Paint.Style.FILL
            paint.shader = null
            paint.color = Color.WHITE
            canvas.save()
            canvas.translate(width / 2f + panX, height / 2f + panY)
            canvas.scale(zoom, zoom)
            canvas.translate(-width / 2f, -height / 2f)
            canvas.drawBitmap(it, 0f, 0f, paint)
            if (editingBoundary) drawVertexHandles(canvas)
            canvas.restore()
        }
    }

    private fun drawMaizePlant(
        canvas: Canvas,
        x: Float,
        y: Float,
        size: Float
    ) {
        val darkGreen = Color.rgb(23, 103, 35)
        val mediumGreen = Color.rgb(45, 151, 50)
        val lightGreen = Color.rgb(133, 205, 68)

        for (i in 0 until 8) {
            canvas.save()
            canvas.rotate(i * 45f, x, y)

            val leaf = Path().apply {
                moveTo(x, y)
                cubicTo(
                    x - size * 0.35f,
                    y - size * 0.25f,
                    x - size * 0.28f,
                    y - size * 0.75f,
                    x,
                    y - size
                )
                cubicTo(
                    x + size * 0.28f,
                    y - size * 0.75f,
                    x + size * 0.35f,
                    y - size * 0.25f,
                    x,
                    y
                )
                close()
            }

            paint.style = Paint.Style.FILL
            paint.color =
                if (i % 2 == 0) mediumGreen else darkGreen

            canvas.drawPath(leaf, paint)

            paint.style = Paint.Style.STROKE
            paint.color = lightGreen
            paint.strokeWidth = max(
                0.5f,
                size * 0.07f
            )

            canvas.drawLine(
                x,
                y,
                x,
                y - size * 0.85f,
                paint
            )

            canvas.restore()
        }

        paint.style = Paint.Style.FILL
        paint.color = lightGreen
        canvas.drawCircle(
            x,
            y,
            max(0.7f, size * 0.15f),
            paint
        )
    }

    private fun drawField(canvas: Canvas) {
        val margin = 24f
        val availableWidth = width - margin * 2f
        val availableHeight = height - margin * 2f

        if (availableWidth <= 0f ||
            availableHeight <= 0f
        ) return

        val w = fieldWidth.coerceAtLeast(1f)
        val h = if (fieldShape == "Circle") {
            w
        } else {
            fieldLength.coerceAtLeast(1f)
        }

        val scale = min(
            availableWidth / w,
            availableHeight / h
        )

        val drawWidth = w * scale
        val drawHeight = h * scale

        val left = (width - drawWidth) / 2f
        val top = (height - drawHeight) / 2f

        val bounds = RectF(
            left,
            top,
            left + drawWidth,
            top + drawHeight
        )

        val gpsPoints = mutableListOf<Pair<Double, Double>>()
        for (i in 0 until gpsBoundary.length()) {
            val p = gpsBoundary.optJSONArray(i) ?: continue
            val lat = p.optDouble(0, Double.NaN)
            val lon = p.optDouble(1, Double.NaN)
            if (lat.isFinite() && lon.isFinite()) gpsPoints.add(lat to lon)
        }
        val trackedOutline = if (gpsPoints.size >= 3) {
            val lat0 = gpsPoints.map { it.first }.average()
            val lon0 = gpsPoints.map { it.second }.average()
            val factor = cos(Math.toRadians(lat0)).coerceAtLeast(0.01)
            gpsCenterLat = lat0
            gpsCenterLon = lon0
            gpsFactor = factor
            val east = gpsPoints.map { (it.second - lon0) * 111320.0 * factor }
            val north = gpsPoints.map { (it.first - lat0) * 111320.0 }
            val minE = east.minOrNull() ?: 0.0
            val maxE = east.maxOrNull() ?: 1.0
            val minN = north.minOrNull() ?: 0.0
            val maxN = north.maxOrNull() ?: 1.0
            val gpsScale = min(availableWidth.toDouble() / (maxE - minE).coerceAtLeast(1.0),
                availableHeight.toDouble() / (maxN - minN).coerceAtLeast(1.0))
            val centerE = (maxE + minE) / 2.0
            val centerN = (maxN + minN) / 2.0
            this.gpsScale = gpsScale
            gpsCenterE = centerE
            gpsCenterN = centerN
            Path().apply {
                for (i in gpsPoints.indices) {
                    val px = (width / 2.0 + (east[i] - centerE) * gpsScale).toFloat()
                    val py = (height / 2.0 - (north[i] - centerN) * gpsScale).toFloat()
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            }
        } else null
        val fieldPath = Path().apply {
            if (trackedOutline != null) addPath(trackedOutline)
            else if (fieldShape == "Oval" ||
                fieldShape == "Circle"
            ) {
                addOval(bounds, Path.Direction.CW)
            } else {
                addRect(bounds, Path.Direction.CW)
            }
        }

        val cropBounds = if (trackedOutline != null) RectF().also { trackedOutline.computeBounds(it, true) } else bounds
        val cropLeft = cropBounds.left
        val cropTop = cropBounds.top
        val cropWidth = cropBounds.width()
        val cropHeight = cropBounds.height()
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            cropLeft,
            cropTop,
            cropLeft + cropWidth,
            cropTop + cropHeight,
            Color.rgb(105, 68, 38),
            Color.rgb(154, 107, 64),
            Shader.TileMode.CLAMP
        )

        canvas.drawPath(fieldPath, paint)
        paint.shader = null

        canvas.save()
        canvas.clipPath(fieldPath)

        // Render the real number of rows, preserving a top-down maize canopy.
        val acrossMetres = if (rowsHorizontal) h else w
        val actualRows = if (rowCount > 0) rowCount else
            ceil(acrossMetres / rowSpacing).toInt().coerceAtLeast(1)
        val safeRows = actualRows.coerceIn(1, 100000)
        val previewRows = min(safeRows, maxPreviewRows)
        val acrossPixels = if (rowsHorizontal) cropHeight else cropWidth
        val alongPixels = if (rowsHorizontal) cropWidth else cropHeight
        val rowGap = acrossPixels / safeRows
        val plantCount = (alongPixels / 13f).toInt().coerceIn(2, 35)
        val plantGap = alongPixels / plantCount
        for (i in 0 until previewRows) {
            val rowIndex = ((i + 0.5f) * safeRows / previewRows).toInt().coerceIn(0, safeRows - 1)
            val cross = (rowIndex + 0.5f) * rowGap
            val x1 = if (rowsHorizontal) cropLeft else cropLeft + cross
            val y1 = if (rowsHorizontal) cropTop + cross else cropTop
            val x2 = if (rowsHorizontal) cropLeft + cropWidth else x1
            val y2 = if (rowsHorizontal) y1 else cropTop + cropHeight
            paint.style = Paint.Style.STROKE
            paint.color = Color.rgb(75, 48, 28)
            paint.strokeWidth = max(0.25f, min(3f, rowGap * 0.18f))
            canvas.drawLine(x1, y1, x2, y2, paint)
            // Individual maize leaves appear when zooming in.
            if (safeRows <= maxPreviewRows && rowGap * zoom >= 2f && growthDays >= 7) {
                for (j in 0 until plantCount) {
                    val along = (j + 0.5f) * plantGap
                    val x = if (rowsHorizontal) cropLeft + along else x1
                    val y = if (rowsHorizontal) y1 else cropTop + along
                    val size = min(12f, min(rowGap * 0.42f, plantGap * 0.48f))
                    if (size > 0.3f) drawMaizePlant(canvas, x, y,
                        size * (growthDays / 100f).coerceIn(0.12f, 1f))
                }
            } else {
                paint.style = Paint.Style.STROKE
                paint.color = Color.rgb(35, 123, 43)
                paint.strokeWidth = max(0.6f, min(acrossPixels / previewRows * 0.75f, 3f))
                canvas.drawLine(x1, y1, x2, y2, paint)
            }
        }

        canvas.restore()

        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(26, 89, 43)
        paint.strokeWidth = 4f
        canvas.drawPath(fieldPath, paint)

        paint.style = Paint.Style.FILL
        paint.shader = null
    }

    private fun pointOnScreen(lat: Double, lon: Double): Pair<Float, Float> {
        val e = (lon - gpsCenterLon) * 111320.0 * gpsFactor
        val n = (lat - gpsCenterLat) * 111320.0
        val x = (width / 2.0 + (e - gpsCenterE) * gpsScale).toFloat()
        val y = (height / 2.0 - (n - gpsCenterN) * gpsScale).toFloat()
        return (width / 2f + panX + (x - width / 2f) * zoom) to
            (height / 2f + panY + (y - height / 2f) * zoom)
    }

    private fun findVertex(x: Float, y: Float): Int {
        if (gpsBoundary.length() < 3) return -1
        var best = -1
        var distance = 52f * resources.displayMetrics.density
        for (i in 0 until gpsBoundary.length()) {
            val p = gpsBoundary.optJSONArray(i) ?: continue
            val (px, py) = pointOnScreen(p.optDouble(0), p.optDouble(1))
            val d = kotlin.math.hypot(x - px, y - py)
            if (d < distance) { distance = d; best = i }
        }
        return best
    }

    private fun moveVertex(index: Int, screenX: Float, screenY: Float) {
        if (gpsScale <= 0.0 || zoom <= 0f) return
        val x = width / 2.0 + (screenX - width / 2f - panX) / zoom
        val y = height / 2.0 + (screenY - height / 2f - panY) / zoom
        val east = gpsCenterE + (x - width / 2.0) / gpsScale
        val north = gpsCenterN - (y - height / 2.0) / gpsScale
        val lat = gpsCenterLat + north / 111320.0
        val lon = gpsCenterLon + east / (111320.0 * gpsFactor)
        if (!lat.isFinite() || !lon.isFinite()) return
        val edited = JSONArray(gpsBoundary.toString())
        edited.put(index, JSONArray().put(lat).put(lon))
        gpsBoundary = edited
    }

    private fun drawVertexHandles(canvas: Canvas) {
        paint.shader = null
        paint.style = Paint.Style.FILL
        for (i in 0 until gpsBoundary.length()) {
            val p = gpsBoundary.optJSONArray(i) ?: continue
            val (sx, sy) = pointOnScreen(p.optDouble(0), p.optDouble(1))
            val x = width / 2f + (sx - width / 2f - panX) / zoom
            val y = height / 2f + (sy - height / 2f - panY) / zoom
            paint.color = Color.WHITE
            canvas.drawCircle(x, y, 9f / zoom, paint)
            paint.color = Color.rgb(24, 116, 228)
            canvas.drawCircle(x, y, 6f / zoom, paint)
        }
    }

    override fun onDetachedFromWindow() {
        cachedBitmap?.recycle()
        cachedBitmap = null
        needsRedraw = true
        super.onDetachedFromWindow()
    }
}
