package com.maizemaster.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import org.json.JSONArray
import kotlin.math.cos
import kotlin.math.max

class HarvestRouteView(context: Context) : View(context) {
    var route = JSONArray()
        set(value) { field = value; invalidate() }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun drawMaizeField(canvas: Canvas) {
        val pad = 16f * resources.displayMetrics.density
        val bounds = RectF(pad, pad, width - pad, height - pad)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(128, 90, 52)
        canvas.drawRoundRect(bounds, 16f, 16f, paint)
        canvas.save()
        canvas.clipRect(bounds)
        val spacing = 19f * resources.displayMetrics.density
        var x = bounds.left + spacing / 2
        while (x < bounds.right) {
            paint.color = Color.rgb(40, 125, 52)
            paint.strokeWidth = spacing * 0.65f
            canvas.drawLine(x, bounds.top, x, bounds.bottom, paint)
            var y = bounds.top + spacing / 2
            while (y < bounds.bottom) {
                paint.color = Color.rgb(105, 184, 70)
                paint.style = Paint.Style.FILL
                canvas.drawOval(RectF(x - spacing * 0.4f, y - spacing * 0.18f,
                    x + spacing * 0.4f, y + spacing * 0.18f), paint)
                canvas.drawOval(RectF(x - spacing * 0.18f, y - spacing * 0.4f,
                    x + spacing * 0.18f, y + spacing * 0.4f), paint)
                y += spacing
            }
            x += spacing
        }
        canvas.restore()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(231, 243, 231))
        val points = mutableListOf<Pair<Double, Double>>()
        for (i in 0 until route.length()) {
            val item = route.optJSONArray(i) ?: continue
            val lat = item.optDouble(0, Double.NaN)
            val lon = item.optDouble(1, Double.NaN)
            if (lat.isFinite() && lon.isFinite()) points.add(lat to lon)
        }
        if (points.isEmpty()) {
            drawMaizeField(canvas)
            return
        }
        val latCenter = points.map { it.first }.average()
        val lonCenter = points.map { it.second }.average()
        val factor = cos(Math.toRadians(latCenter)).coerceAtLeast(0.01)
        val east = points.map { (it.second - lonCenter) * 111320.0 * factor }
        val north = points.map { (it.first - latCenter) * 111320.0 }
        val extent = max(20.0, max((east.maxOrNull() ?: 0.0) - (east.minOrNull() ?: 0.0),
            (north.maxOrNull() ?: 0.0) - (north.minOrNull() ?: 0.0)))
        val scale = (minOf(width, height) * 0.8) / extent
        fun x(i: Int) = (width / 2.0 + east[i] * scale).toFloat()
        fun y(i: Int) = (height / 2.0 - north[i] * scale).toFloat()
        drawMaizeField(canvas)
        val path = Path()
        path.moveTo(x(0), y(0))
        for (i in 1 until points.size) path.lineTo(x(i), y(i))
        paint.color = Color.rgb(143, 99, 57)
        paint.strokeWidth = 18f * resources.displayMetrics.density
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawPath(path, paint)
        paint.color = Color.rgb(23, 144, 71)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 7f * resources.displayMetrics.density
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(10, 76, 46)
        canvas.drawCircle(x(points.lastIndex), y(points.lastIndex), 8f * resources.displayMetrics.density, paint)
    }
}
