package com.maizemaster.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView

/** Dedicated growth screen. Recalculates from planting dates whenever opened. */
class GrowthActivity : Activity() {
    private val dark = Color.rgb(8, 65, 40)
    private val green = Color.rgb(28, 137, 57)
    private fun dp(n: Int) = (n * resources.displayMetrics.density + .5f).toInt()
    private fun text(value: String, size: Float = 15f, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(dark)
            if (bold) setTypeface(null, Typeface.BOLD)
        }
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun background() = GradientDrawable().apply {
        setColor(Color.WHITE)
        cornerRadius = dp(16).toFloat()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = dark
        window.navigationBarColor = dark
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(246, 249, 246)) }
        val body = column().apply { setPadding(dp(16), dp(20), dp(16), dp(24)) }
        scroll.addView(body)
        setContentView(scroll)
        body.addView(text("🌱 Maize growth stages", 24f, true))
        body.addView(text("Automatic estimates refresh whenever this screen opens. Actual stages depend on temperature, moisture and maize variety.", 13f).apply {
            setPadding(0, dp(8), 0, dp(16))
        })
        val fields = FieldStorage(this).loadFields()
        if (fields.isEmpty()) {
            body.addView(text("No saved fields yet. Add a field and set its planting date to start tracking."))
        }
        for (field in fields) {
            val card = column().apply {
                background = background()
                setPadding(dp(16), dp(16), dp(16), dp(16))
            }
            body.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })
            card.addView(text("🌽 " + field.name, 19f, true))
            if (field.plantingDate.isBlank() || field.maturityDays <= 0) {
                card.addView(text("Set the planting date and expected maturity days in My Fields."))
            } else {
                val calendar = MaizeGrowthCalculator.calculate(field.plantingDate, field.maturityDays)
                val weatherEstimate = WeatherGrowthEstimator.calculate(this, field)
                val growth = weatherEstimate.result
                val picture = MaizeGrowthView(this).apply {
                    progressPercent = growth.progressPercent
                }
                card.addView(picture, LinearLayout.LayoutParams(-1, dp(260)))
                card.addView(text(growth.stage, 17f, true))
                card.addView(text("Calendar estimate: ${calendar.stage}", 13f))
                card.addView(text(weatherEstimate.details, 13f).apply { setPadding(0, dp(8), 0, dp(8)) })
                card.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                    max = 100
                    progress = growth.progressPercent
                }, LinearLayout.LayoutParams(-1, dp(12)).apply {
                    topMargin = dp(12)
                    bottomMargin = dp(12)
                })
                card.addView(text("Estimated progress: ${growth.progressPercent}%"))
                card.addView(text("Days since planting: ${growth.daysSincePlanting}"))
                card.addView(text("Estimated days to maturity: ${growth.daysUntilMaturity}"))
                card.addView(text(growth.advice, 14f).apply { setPadding(0, dp(10), 0, 0) })
                if (field.growthStage.isNotBlank()) {
                    card.addView(text("Observed stage: " + field.growthStage, 13f))
                }
                if (field.growthNotes.isNotBlank()) {
                    card.addView(text("Field notes: " + field.growthNotes, 13f))
                }
            }
        }
        body.addView(Button(this).apply {
            text = "Manage fields and planting dates"
            isAllCaps = false
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(green)
            setOnClickListener { startActivity(Intent(this@GrowthActivity, MainActivity::class.java)) }
        })
    }
}
