
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
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class FieldDesignerView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var cachedBitmap: Bitmap? = null
    private var needsRedraw = true

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
            canvas.drawBitmap(it, 0f, 0f, paint)
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

        val fieldPath = Path().apply {
            if (fieldShape == "Oval" ||
                fieldShape == "Circle"
            ) {
                addOval(bounds, Path.Direction.CW)
            } else {
                addRect(bounds, Path.Direction.CW)
            }
        }

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            left,
            top,
            left + drawWidth,
            top + drawHeight,
            Color.rgb(105, 68, 38),
            Color.rgb(154, 107, 64),
            Shader.TileMode.CLAMP
        )

        canvas.drawPath(fieldPath, paint)
        paint.shader = null

        canvas.save()
        canvas.clipPath(fieldPath)

        val actualRows = w / rowSpacing

        val displayRows = min(
            16,
            max(2, ceil(actualRows).toInt())
        )

        val rowGap = drawWidth / displayRows

        val plantCount = min(
            26,
            max(2, (drawHeight / 14f).toInt())
        )

        val plantGap = drawHeight / plantCount

        for (i in 0 until displayRows) {
            val x = left + (i + 0.5f) * rowGap

            paint.style = Paint.Style.STROKE
            paint.color = Color.rgb(75, 48, 28)
            paint.strokeWidth = max(
                1.5f,
                rowGap * 0.18f
            )

            canvas.drawLine(
                x,
                top,
                x,
                top + drawHeight,
                paint
            )

            for (j in 0 until plantCount) {
                val y = top + (j + 0.5f) * plantGap

                val plantSize = min(
                    12f,
                    min(
                        rowGap * 0.42f,
                        plantGap * 0.48f
                    )
                )

                if (plantSize > 0.5f) {
                    drawMaizePlant(
                        canvas,
                        x,
                        y,
                        plantSize
                    )
                }
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

    override fun onDetachedFromWindow() {
        cachedBitmap?.recycle()
        cachedBitmap = null
        needsRedraw = true
        super.onDetachedFromWindow()
    }
}
