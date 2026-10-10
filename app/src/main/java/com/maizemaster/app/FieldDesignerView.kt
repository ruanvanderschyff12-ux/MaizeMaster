package com.maizemaster.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View

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

        val margin = 30f
        val availableWidth = width - margin * 2
        val availableHeight = height - margin * 2

        if (availableWidth <= 0 || availableHeight <= 0) return

        val shapeWidth = if (fieldShape == "Circle") {
            minOf(fieldWidth, fieldLength)
        } else fieldWidth

        val shapeLength = if (fieldShape == "Circle") {
            shapeWidth
        } else fieldLength

        val scale = minOf(
            availableWidth / shapeWidth,
            availableHeight / shapeLength
        )

        val drawWidth = shapeWidth * scale
        val drawHeight = shapeLength * scale

        val left = (width - drawWidth) / 2f
        val top = (height - drawHeight) / 2f

        val path = Path()

        when (fieldShape) {
            "Oval", "Circle" -> path.addOval(
                left, top,
                left + drawWidth,
                top + drawHeight,
                Path.Direction.CW
            )
            else -> path.addRect(
                left, top,
                left + drawWidth,
                top + drawHeight,
                Path.Direction.CW
            )
        }

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(115, 77, 38)
        canvas.drawPath(path, paint)

        canvas.save()
        canvas.clipPath(path)

        paint.color = Color.rgb(55, 145, 55)
        paint.strokeWidth = 3f

        val rowCount = (shapeWidth / rowSpacing)
            .toInt().coerceAtMost(500)

        for (i in 1 until rowCount) {
            val x = left + i * rowSpacing * scale

            canvas.drawLine(
                x, top,
                x, top + drawHeight,
                paint
            )
        }

        canvas.restore()

        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
    }
}
