package com.maizemaster.app

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Full-screen growth detail for one saved field, with the existing shaded maize illustration. */
class GrowthFieldDetailActivity : Activity() {
    private val storage by lazy { FieldStorage(this) }
    private val fieldId by lazy { intent.getStringExtra("field_id").orEmpty() }
    private fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showField()
    }
    private fun showField() {
        val field = storage.loadFields().firstOrNull { it.id == fieldId }
        if (field == null) { finish(); return }
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(246, 249, 246)) }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(24))
        }
        scroll.addView(content)
        setContentView(scroll)
        fun label(value: String, size: Float = 16f) {
            content.addView(TextView(this).apply {
                text = value
                textSize = size
                setTextColor(Color.rgb(8, 65, 40))
                setPadding(0, dp(7), 0, dp(7))
            })
        }
        label("🌽 " + field.name, 26f)
        val valid = field.plantingDate.isNotBlank() && field.maturityDays > 0
        val estimate = if (valid) WeatherGrowthEstimator.calculate(this, field) else null
        val progress = estimate?.result?.progressPercent ?: 0
        label(if (valid) "Estimated growth stage: " + estimate!!.result.stage else "Set planting date and maturity days to calculate growth.")
        content.addView(MaizeGrowthView(this).apply {
            progressPercent = progress
        }, LinearLayout.LayoutParams(-1, dp(320)))
        if (valid) {
            label("Estimated progress: " + progress + "%")
            label("Estimated days to maturity: " + estimate!!.result.daysUntilMaturity)
            label(estimate.details, 14f)
            label(estimate.result.advice, 14f)
        }
        label("Observed stage: " + field.growthStage.ifBlank { "Not recorded" })
        if (field.growthNotes.isNotBlank()) label("Notes: " + field.growthNotes, 14f)
        content.addView(Button(this).apply {
            text = "Edit growth stage, planting date & notes"
            isAllCaps = false
            setOnClickListener {
                MaizeGrowthTracker.show(this@GrowthFieldDetailActivity, storage, fieldId) {
                    showField()
                }
            }
        })
        content.addView(Button(this).apply {
            text = "Back to growth stages"
            isAllCaps = false
            setOnClickListener { finish() }
        })
    }
}
