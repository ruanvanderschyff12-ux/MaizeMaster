
package com.maizemaster.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class FieldDesignerView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    var fieldWidth = 200f
        set(value) {
            field = value.coerceAtLeast(1f)
            invalidate()
        }

    var fieldLength = 300f
        set(value) {
            field = value.coerceAtLeast(1f)
            invalidate()
        }

    var rowSpacing = 0.75f
        set(value) {
            field = value.coerceAtLeast(0.1f)
            invalidate()
        }

    var fieldShape = "Rectangle"
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val margin = 24f
        val availableWidth = width - margin * 2
        val availableHeight = height - margin * 2

        if (availableWidth <= 0 || availableHeight <= 0) return

        val w = fieldWidth.coerceAtLeast(1f)
        val h = if (fieldShape == "Circle") w
                else fieldLength.coerceAtLeast(1f)

        val scale = min(availableWidth / w, availableHeight / h)

        val drawWidth = w * scale
        val drawHeight = h * scale

        val left = (width - drawWidth) / 2f
        val top = (height - drawHeight) / 2f

        val bounds = RectF(
            left, top,
            left + drawWidth,
            top + drawHeight
        )

        val fieldPath = Path().apply {
            if (fieldShape == "Oval" || fieldShape == "Circle") {
                addOval(bounds, Path.Direction.CW)
            } else {
                addRect(bounds, Path.Direction.CW)
            }
        }

        // Draw soil background
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(126, 91, 57)
        canvas.drawPath(fieldPath, paint)

        canvas.save()
        canvas.clipPath(fieldPath)

        // Draw visible representative maize rows
        val actualRows = w / rowSpacing
        val displayRows = min(18, max(2, ceil(actualRows).toInt()))

        val rowGap = drawWidth / displayRows

        for (i in 0 until displayRows) {
            val x = left + (i + 0.5f) * rowGap

            // Planting row
            paint.color = Color.rgb(65, 115, 42)
            paint.strokeWidth = max(2f, rowGap * 0.22f)
            paint.style = Paint.Style.STROKE

            canvas.drawLine(x, top, x, top + drawHeight, paint)

            // Individual maize plants
            paint.style = Paint.Style.FILL

            val plantCount = min(
                35,
                max(2, (drawHeight / 12f).toInt())
            )

            for (j in 0 until plantCount) {
                val y = top + (j + 0.5f) *
                    drawHeight / plantCount

                val radius = min(5f, rowGap * 0.32f)

                paint.color = Color.rgb(105, 205, 65)
                canvas.drawCircle(x, y, radius, paint)

                paint.color = Color.rgb(38, 125, 35)
                canvas.drawCircle(
                    x - radius * 0.3f,
                    y - radius * 0.3f,
                    radius * 0.45f,
                    paint
                )
            }
        }

        canvas.restore()

        // Field border
        paint.color = Color.rgb(35, 75, 30)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawPath(fieldPath, paint)

        paint.style = Paint.Style.FILL
    }
}
