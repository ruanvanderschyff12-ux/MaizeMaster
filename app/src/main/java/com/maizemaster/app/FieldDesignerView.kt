package com.maizemaster.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val margin = 30f
        val availableWidth = width - margin * 2
        val availableHeight = height - margin * 2

        if (availableWidth <= 0 || availableHeight <= 0) return

        val scale = minOf(
            availableWidth / fieldWidth,
            availableHeight / fieldLength
        )

        val drawWidth = fieldWidth * scale
        val drawHeight = fieldLength * scale

        val left = (width - drawWidth) / 2f
        val top = (height - drawHeight) / 2f

        paint.color = Color.rgb(115, 77, 38)
        paint.style = Paint.Style.FILL

        canvas.drawRect(
            left, top,
            left + drawWidth,
            top + drawHeight,
            paint
        )

        paint.color = Color.rgb(55, 145, 55)
        paint.strokeWidth = 2f

        val rowCount = (fieldWidth / rowSpacing)
            .toInt().coerceAtMost(500)

        for (i in 1 until rowCount) {
            val x = left + i * rowSpacing * scale

            canvas.drawLine(
                x, top,
                x, top + drawHeight,
                paint
            )
        }

        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f

        canvas.drawRect(
            left, top,
            left + drawWidth,
            top + drawHeight,
            paint
        )

        paint.style = Paint.Style.FILL
    }
}
