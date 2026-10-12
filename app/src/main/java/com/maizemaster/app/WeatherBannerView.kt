
package com.maizemaster.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class WeatherBannerView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    var growthStage: String = "Vegetative growth"
        set(value) {
            field = value
            invalidate()
        }

    var condition: String = "Partly cloudy"
        set(value) {
            field = value
            invalidate()
        }

    private fun color(hex: String) = Color.parseColor(hex)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val save = canvas.save()
        canvas.scale(width / 400f, height / 220f)

        val bounds = RectF(0f, 0f, 400f, 220f)
        val clip = Path().apply {
            addRoundRect(bounds, 18f, 18f, Path.Direction.CW)
        }
        canvas.clipPath(clip)

        drawSky(canvas)
        drawSunAndClouds(canvas)
        drawLandscape(canvas)
        drawMaizeField(canvas)

        canvas.restoreToCount(save)
    }

    private fun drawSky(canvas: Canvas) {
        val rainy = condition.contains("rain", true) ||
            condition.contains("storm", true)

        paint.shader = LinearGradient(
            0f, 0f, 0f, 170f,
            if (rainy) color("#344F61") else color("#11628D"),
            if (rainy) color("#A4B2B7") else color("#9FD8EF"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, 400f, 220f, paint)
        paint.shader = null
    }

    private fun drawSunAndClouds(canvas: Canvas) {
        val storm = condition.contains("storm", true)
        val rainy = condition.contains("rain", true)
        val cloudy = condition.contains("cloud", true)

        if (!storm && !rainy) {
            paint.color = color("#FFD34D")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f

            for (i in 0 until 8) {
                val angle = i * Math.PI / 4
                val x1 = 325 + cos(angle).toFloat() * 30
                val y1 = 55 + sin(angle).toFloat() * 30
                val x2 = 325 + cos(angle).toFloat() * 39
                val y2 = 55 + sin(angle).toFloat() * 39
                canvas.drawLine(x1, y1, x2, y2, paint)
            }

            paint.style = Paint.Style.FILL
            canvas.drawCircle(325f, 55f, 23f, paint)
        }

        if (cloudy || rainy || storm) {
            paint.color = if (storm) {
                color("#687B86")
            } else {
                Color.WHITE
            }

            canvas.drawOval(RectF(300f, 70f, 385f, 92f), paint)
            canvas.drawCircle(325f, 70f, 18f, paint)
            canvas.drawCircle(350f, 65f, 23f, paint)
            canvas.drawCircle(372f, 76f, 15f, paint)
        }

        if (rainy || storm) {
            paint.color = color("#6BC9F3")
            paint.strokeWidth = 2.5f
            for (i in 0 until 7) {
                val x = 310f + i * 11f
                canvas.drawLine(x, 98f, x - 5f, 112f, paint)
            }
        }
    }

    private fun drawLandscape(canvas: Canvas) {
        paint.color = color("#598B65")
        val hills = Path().apply {
            moveTo(0f, 148f)
            quadTo(100f, 105f, 205f, 145f)
            quadTo(305f, 115f, 400f, 140f)
            lineTo(400f, 220f)
            lineTo(0f, 220f)
            close()
        }
        canvas.drawPath(hills, paint)

        paint.color = color("#75502C")
        canvas.drawRect(0f, 169f, 400f, 220f, paint)
    }

    private fun drawMaizeField(canvas: Canvas) {
        val stage = growthStage.lowercase()

        val plantHeight = when {
            "plant" in stage || "germ" in stage -> 8f
            "seed" in stage || "emerg" in stage -> 23f
            "vegetative" in stage -> 53f
            "tassel" in stage || "silk" in stage -> 100f
            "grain" in stage || "milk" in stage ||
                "dent" in stage -> 110f
            "matur" in stage || "harvest" in stage -> 105f
            else -> 45f
        }

        val mature = "matur" in stage ||
            "harvest" in stage

        for (row in 0..3) {
            val scale = 0.45f + row * 0.2f
            val baseline = 175f + row * 14f
            val spacing = 25f + row * 8f

            var x = -15f
            while (x < 430f) {
                drawPlant(
                    canvas,
                    x,
                    baseline,
                    plantHeight * scale,
                    mature
                )
                x += spacing
            }
        }
    }

    private fun drawPlant(
        canvas: Canvas,
        x: Float,
        bottom: Float,
        height: Float,
        mature: Boolean
    ) {
        if (height < 12f) {
            paint.color = color("#5BBD52")
            canvas.drawOval(
                RectF(x - 3f, bottom - 7f, x + 5f, bottom),
                paint
            )
            return
        }

        val top = bottom - height
        paint.color = if (mature) {
            color("#B6A052")
        } else {
            color("#245D32")
        }

        paint.strokeWidth = 2.5f
        canvas.drawLine(x, bottom, x, top, paint)

        val leafCount = if (height < 35f) 2 else 5

        for (i in 0 until leafCount) {
            val y = bottom - height * (0.23f + i * 0.13f)
            val direction = if (i % 2 == 0) -1 else 1
            val reach = height * 0.35f

            val leaf = Path().apply {
                moveTo(x, y + 5f)
                quadTo(
                    x + direction * reach * 0.45f,
                    y - height * 0.18f,
                    x + direction * reach,
                    y - height * 0.08f
                )
                quadTo(
                    x + direction * reach * 0.5f,
                    y + height * 0.05f,
                    x,
                    y + 5f
                )
                close()
            }

            paint.color = if (mature) {
                color("#C3AC59")
            } else if (i % 2 == 0) {
                color("#347F3A")
            } else {
                color("#63AD43")
            }

            canvas.drawPath(leaf, paint)
        }

        if (height >= 65f) {
            paint.color = color("#D9B65B")
            paint.strokeWidth = 1.5f

            canvas.drawLine(x, top, x, top - 10f, paint)
            canvas.drawLine(x, top - 4f, x - 6f, top - 11f, paint)
            canvas.drawLine(x, top - 4f, x + 6f, top - 11f, paint)
        }
    }
}
