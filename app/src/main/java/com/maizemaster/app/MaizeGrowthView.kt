
package com.maizemaster.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.view.View
import kotlin.math.abs
import kotlin.math.min

class MaizeGrowthView(
    context: Context,
    initialStage: String = ""
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    var progressPercent: Int = 0
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }

    init {
        setStage(initialStage)
    }

    fun setStage(stage: String) {
        progressPercent = when {
            stage.contains("Harvested", true) -> 100
            stage.contains("Not planted", true) -> 0
            stage.contains("Planted", true) -> 0
            stage.contains("Germination", true) -> 3
            stage.contains("Emergence", true) -> 5
            stage.contains("Seedling", true) -> 10
            stage.contains("Late vegetative", true) -> 43
            stage.contains("Vegetative", true) -> 30
            stage.contains("Tasseling", true) -> 52
            stage.contains("Silking", true) -> 58
            stage.contains("Blister", true) -> 65
            stage.contains("Milk", true) -> 73
            stage.contains("Dough", true) -> 82
            stage.contains("Dent", true) -> 92
            stage.contains("Grain filling", true) -> 75
            stage.contains("Physiological maturity", true) -> 100
            else -> 0
        }
    }

    private fun color(hex: String): Int =
        Color.parseColor(hex)

    private fun line(
        canvas: Canvas,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        shade: Int,
        thickness: Float
    ) {
        paint.reset()
        paint.isAntiAlias = true
        paint.color = shade
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = thickness
        paint.strokeCap = Paint.Cap.ROUND

        canvas.drawLine(
            x1, y1, x2, y2, paint
        )
    }

    private fun oval(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        shade: Int
    ) {
        paint.reset()
        paint.isAntiAlias = true
        paint.color = shade
        paint.style = Paint.Style.FILL

        canvas.drawOval(
            left, top, right, bottom, paint
        )
    }

    private fun leaf(
        canvas: Canvas,
        x: Float,
        y: Float,
        length: Float,
        rise: Float,
        side: Float,
        shade: Int
    ) {
        val tipX = x + length * side
        val tipY = y - rise

        val path = Path().apply {
            moveTo(x, y)

            cubicTo(
                x + length * side * 0.20f,
                y - rise * 0.85f,
                tipX - length * side * 0.20f,
                tipY - 12f,
                tipX,
                tipY
            )

            cubicTo(
                tipX - length * side * 0.35f,
                tipY + 24f,
                x + length * side * 0.25f,
                y + 12f,
                x,
                y
            )

            close()
        }

        paint.reset()
        paint.isAntiAlias = true
        paint.color = shade
        paint.style = Paint.Style.FILL

        canvas.drawPath(path, paint)

        line(
            canvas,
            x,
            y,
            tipX,
            tipY,
            color("#A7D46A"),
            1.6f
        )
    }

    private fun root(
        canvas: Canvas,
        startX: Float,
        startY: Float,
        length: Float,
        spread: Float,
        depth: Int
    ) {
        if (depth <= 0) return

        val endX = startX + spread
        val endY = startY + length

        line(
            canvas,
            startX,
            startY,
            endX,
            endY,
            color("#D6B88A"),
            1.2f + depth * 0.45f
        )

        root(
            canvas,
            endX,
            endY,
            length * 0.54f,
            spread * 0.65f + 7f,
            depth - 1
        )

        root(
            canvas,
            endX,
            endY,
            length * 0.48f,
            spread * 0.65f - 9f,
            depth - 1
        )
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) {
        val desiredHeight =
            (300f * resources.displayMetrics.density).toInt()

        setMeasuredDimension(
            View.MeasureSpec.getSize(widthMeasureSpec),
            resolveSize(
                desiredHeight,
                heightMeasureSpec
            )
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        if (
            viewWidth <= 0f ||
            viewHeight <= 0f
        ) return

        val scale = min(
            viewWidth / 360f,
            viewHeight / 300f
        )

        canvas.save()

        canvas.translate(
            (viewWidth - 360f * scale) / 2f,
            (viewHeight - 300f * scale) / 2f
        )

        canvas.scale(scale, scale)

        // SKY BACKGROUND

        paint.reset()
        paint.isAntiAlias = true

        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            300f,
            color("#E6F5ED"),
            color("#F9FCF3"),
            Shader.TileMode.CLAMP
        )

        canvas.drawRoundRect(
            0f, 0f,
            360f, 300f,
            20f, 20f,
            paint
        )

        paint.shader = null

        // SUN

        oval(
            canvas,
            275f, 24f,
            316f, 65f,
            color("#FFE6A0")
        )

        // SOIL

        paint.color = color("#79553C")

        canvas.drawRoundRect(
            0f, 212f,
            360f, 300f,
            18f, 18f,
            paint
        )

        paint.color = color("#A47B53")

        canvas.drawRect(
            0f, 212f,
            360f, 223f,
            paint
        )

        // SOIL TEXTURE

        for (i in 0 until 24) {
            val x =
                ((i * 83 + 17) % 345).toFloat()

            val y =
                (231 + (i * 29 % 61)).toFloat()

            oval(
                canvas,
                x, y,
                x + 3f, y + 2f,
                color("#C29A70")
            )
        }

        val progress = progressPercent

        val centerX = 180f
        val soilY = 214f

        // MAIZE SEED

        oval(
            canvas,
            centerX - 8f,
            soilY + 3f,
            centerX + 8f,
            soilY + 17f,
            color("#E6B94D")
        )

        if (progress <= 1) {
            canvas.restore()
            return
        }

        // ROOT DEVELOPMENT

        val rootDepth = when {
            progress < 5 -> 1
            progress < 15 -> 2
            else -> 3
        }

        val rootLength = when {
            progress < 5 -> 9f
            progress < 15 -> 13f
            else -> 18f
        }

        for (i in -2..2) {
            root(
                canvas,
                centerX,
                soilY + 12f,
                rootLength,
                i * 5f,
                rootDepth
            )
        }

        // PLANT HEIGHT

        val plantHeight = when {
            progress < 5 -> 12f
            progress < 16 -> 35f
            progress < 38 -> 70f
            progress < 50 -> 115f
            progress < 62 -> 155f
            progress < 80 -> 173f
            else -> 184f
        }

        val topY = soilY - plantHeight

        // STEM SHADOW

        line(
            canvas,
            centerX + 3f,
            soilY,
            centerX + 3f,
            topY,
            color("#205A2F"),
            7f
        )

        // MAIN STEM

        line(
            canvas,
            centerX,
            soilY,
            centerX,
            topY,
            color("#61A63A"),
            6f
        )

        // MAIZE LEAVES

        val leafCount = when {
            progress < 5 -> 0
            progress < 16 -> 2
            progress < 38 -> 4
            progress < 50 -> 6
            progress < 62 -> 8
            else -> 10
        }

        for (i in 0 until leafCount) {
            val fraction =
                (i + 1f) / (leafCount + 1f)

            val y =
                soilY - plantHeight * fraction

            val side =
                if (i % 2 == 0) -1f else 1f

            val length = when {
                progress < 16 -> 20f
                progress < 38 -> 37f
                else -> 55f
            } + i * 2f

            leaf(
                canvas,
                centerX,
                y,
                length,
                16f + i * 2f,
                side,
                if (i % 2 == 0) {
                    color("#398A39")
                } else {
                    color("#63B447")
                }
            )
        }

        // TASSEL DEVELOPMENT

        if (progress >= 50) {
            for (i in -3..3) {
                line(
                    canvas,
                    centerX,
                    topY + 10f,
                    centerX + i * 9f,
                    topY - 21f + abs(i) * 4f,
                    color("#B49B4E"),
                    2f
                )
            }
        }

        // DEVELOPING MAIZE COB

        if (progress >= 55) {
            val cobSize = when {
                progress < 65 -> 0.55f
                progress < 80 -> 0.75f
                else -> 1f
            }

            canvas.save()

            canvas.translate(
                centerX + 18f,
                soilY - plantHeight * 0.52f
            )

            canvas.rotate(-24f)
            canvas.scale(cobSize, cobSize)

            // GREEN HUSK

            oval(
                canvas,
                -13f, -24f,
                13f, 29f,
                color("#4E9D45")
            )

            // MAIZE COB

            oval(
                canvas,
                -9f, -20f,
                9f, 17f,
                if (progress < 80) {
                    color("#F2D977")
                } else {
                    color("#EFC34E")
                }
            )

            // MAIZE KERNELS

            for (row in 0 until 6) {
                for (column in -1..1) {
                    oval(
                        canvas,
                        column * 5f - 2f,
                        -15f + row * 5f,
                        column * 5f + 2f,
                        -11f + row * 5f,
                        color("#F9E68A")
                    )
                }
            }

            // MAIZE SILKS

            if (progress < 80) {
                for (i in -2..2) {
                    line(
                        canvas,
                        i * 2f,
                        -18f,
                        i * 5f + 4f,
                        -31f,
                        color("#D5A95A"),
                        1.2f
                    )
                }
            }

            canvas.restore()
        }

        // MATURE PLANT COLOURS

        if (progress >= 90) {
            line(
                canvas,
                centerX,
                soilY,
                centerX,
                topY,
                color("#BDAA56"),
                3f
            )

            leaf(
                canvas,
                centerX,
                soilY - 25f,
                55f,
                17f,
                -1f,
                color("#B6A354")
            )
        }

        // GROUND GRASS

        for (
            x in listOf(
                34f,
                64f,
                291f,
                324f
            )
        ) {
            line(
                canvas,
                x,
                soilY,
                x - 5f,
                soilY - 9f,
                color("#73A85C"),
                2f
            )

            line(
                canvas,
                x,
                soilY,
                x + 5f,
                soilY - 7f,
                color("#73A85C"),
                2f
            )
        }

        canvas.restore()
    }
}
