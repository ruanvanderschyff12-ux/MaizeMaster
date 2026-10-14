package com.maizemaster.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View

/** Original maize-and-robot mascot drawn locally, with no network or image assets. */
class MaizeMateMascot(context: Context) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private fun oval(c: Canvas, color: Int, l: Float, t: Float, r: Float, b: Float) {
        p.color = color; p.style = Paint.Style.FILL
        c.drawOval(RectF(l,t,r,b),p)
    }
    private fun round(c: Canvas, color: Int, l: Float, t: Float, r: Float, b: Float, rad: Float) {
        p.color = color; p.style = Paint.Style.FILL
        c.drawRoundRect(RectF(l,t,r,b),rad,rad,p)
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val save = canvas.save()
        canvas.scale(width / 120f, height / 120f)
        val gold = Color.rgb(248,193,54)
        val green = Color.rgb(42,145,73)
        val navy = Color.rgb(17,64,59)
        oval(canvas,Color.rgb(232,247,226),5f,5f,115f,115f)
        // Leaf husks and robot antenna.
        val leaf = Path().apply { moveTo(59f,99f); cubicTo(12f,89f,15f,50f,33f,45f); cubicTo(30f,73f,45f,81f,59f,99f); close() }
        p.color=green;canvas.drawPath(leaf,p)
        val leaf2 = Path().apply { moveTo(59f,99f); cubicTo(105f,86f,103f,57f,88f,45f); cubicTo(91f,72f,75f,87f,59f,99f);close() }
        p.color=Color.rgb(21,107,62);canvas.drawPath(leaf2,p)
        round(canvas,gold,37f,22f,82f,93f,21f)
        // Kernel texture on left half.
        for (y in 0..5) for (x in 0..1) {
            oval(canvas,if((x+y)%2==0) Color.rgb(255,223,102) else Color.rgb(234,165,38),
                40f+x*10f,29f+y*10f,49f+x*10f,38f+y*10f)
        }
        // Metallic robot half.
        round(canvas,Color.rgb(111,153,162),60f,22f,83f,91f,10f)
        round(canvas,navy,62f,38f,82f,62f,7f)
        oval(canvas,Color.rgb(96,239,222),66f,44f,78f,55f)
        p.color=Color.WHITE;p.strokeWidth=3f
        canvas.drawLine(72f,20f,72f,11f,p)
        oval(canvas,Color.rgb(255,207,74),67f,5f,77f,15f)
        round(canvas,navy,48f,68f,73f,73f,3f)
        round(canvas,Color.WHITE,51f,69f,69f,71f,2f)
        canvas.restoreToCount(save)
    }
}
