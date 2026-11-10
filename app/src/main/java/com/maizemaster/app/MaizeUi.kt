package com.maizemaster.app

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

internal object MaizeUi {
    val forest = Color.rgb(17, 61, 43)
    val page = Color.rgb(243, 249, 244)
    private val green = Color.rgb(25, 120, 76)
    private val border = Color.rgb(216, 232, 220)

    fun dp(context: Context, size: Int): Int =
        (size * context.resources.displayMetrics.density + 0.5f).toInt()

    fun surface(context: Context, colour: Int = Color.WHITE) = GradientDrawable().apply {
        setColor(colour)
        cornerRadius = dp(context, 18).toFloat()
        setStroke(dp(context, 1), border)
    }

    fun header(context: Context, section: String, title: String, description: String): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padding = dp(context, 18)
            setPadding(padding, padding, padding, padding)
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(forest, green)).apply {
                cornerRadius = dp(context, 20).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 14) }
            addView(TextView(context).apply {
                text = section
                setTextColor(Color.rgb(220, 246, 208))
                textSize = 12f
                letterSpacing = 0.09f
                setTypeface(null, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = title
                setTextColor(Color.WHITE)
                textSize = 26f
                setTypeface(null, Typeface.BOLD)
                setPadding(0, dp(context, 4), 0, dp(context, 5))
            })
            addView(TextView(context).apply {
                text = description
                setTextColor(Color.WHITE)
                textSize = 14f
            })
        }

    fun primary(button: Button) {
        val context = button.context
        val state = ColorStateList(
            arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
            intArrayOf(Color.rgb(202, 219, 207), green)
        )
        val shape = GradientDrawable().apply {
            color = state
            cornerRadius = dp(context, 14).toFloat()
        }
        button.background = RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), shape, null)
        button.setTextColor(ColorStateList(
            arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
            intArrayOf(forest, Color.WHITE)
        ))
        button.isAllCaps = false
    }
}
