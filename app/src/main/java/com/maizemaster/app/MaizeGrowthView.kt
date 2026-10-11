package com.maizemaster.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.View

class MaizeGrowthView(
    context: Context,
    stage: String
) : View(context) {

    private val paint = Paint(
        Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG
    )

    private val boundaries = intArrayOf(
        0, 230, 450, 775, 1105, 1420, 1765, 2048
    )

    private var index = stageIndex(stage)

    init {
        contentDescription =
            "Illustration of maize: ${stage.ifBlank { "stage not recorded" }}"
    }

    fun setStage(stage: String) {
        index = stageIndex(stage)
        contentDescription =
            "Illustration of maize: ${stage.ifBlank { "stage not recorded" }}"
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (index < 0) return

        val image = bitmap
            ?: BitmapFactory.decodeResource(
                resources,
                R.drawable.maize_growth_stages
            ).also {
                bitmap = it
            }
            ?: return

        val left = boundaries[index] * image.width / 2048
        val right = boundaries[index + 1] * image.width / 2048

        val source = Rect(
            left,
            0,
            right,
            image.height
        )

        val scale = minOf(
            height.toFloat() / image.height,
            width.toFloat() / source.width()
        )

        val drawWidth = source.width() * scale
        val drawHeight = image.height * scale
        val x = (width - drawWidth) / 2f

        canvas.drawBitmap(
            image,
            source,
            RectF(
                x,
                height - drawHeight,
                x + drawWidth,
                height.toFloat()
            ),
            paint
        )
    }

    companion object {
        private var bitmap: Bitmap? = null

        private fun stageIndex(stage: String): Int =
            when (stage) {
                "Planted" -> 0
                "Emergence" -> 1
                "Vegetative growth" -> 2
                "Tasseling" -> 3
                "Silking" -> 4
                "Grain filling" -> 5
                "Physiological maturity" -> 6
                else -> -1
            }
    }
}
