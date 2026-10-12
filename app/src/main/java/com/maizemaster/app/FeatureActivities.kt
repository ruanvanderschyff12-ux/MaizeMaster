package com.maizemaster.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.widget.*
import android.view.View
import java.util.Locale

private fun Activity.featureLayout(title: String): LinearLayout {
    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(22, 22, 22, 22)
        setBackgroundColor(Color.rgb(246, 249, 246))
    }
    val back = Button(this).apply {
        text = "← Back to dashboard"
        setOnClickListener { finish() }
    }
    root.addView(back)
    root.addView(TextView(this).apply {
        text = title
        textSize = 25f
        setTextColor(Color.rgb(8, 65, 40))
        setTypeface(null, Typeface.BOLD)
        setPadding(0, 20, 0, 18)
    })
    return root
}
private fun Activity.info(text: String, size: Float = 17f) = TextView(this).apply {
    this.text = text
    textSize = size
    setTextColor(Color.rgb(25, 75, 45))
    setPadding(10, 14, 10, 14)
}

class LegacyKnowledgeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = featureLayout("📖 Maize Farming Knowledge")
        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        body.addView(info("🌱 PLANTING\nChoose appropriate seed, planting depth and spacing for local conditions."))
        body.addView(info("🌿 GROWTH\nMonitor emergence, soil moisture, weeds and nutrient deficiencies."))
        body.addView(info("🌽 FLOWERING\nHeat and drought stress during tasseling and silking can reduce kernel set."))
        body.addView(info("🚜 HARVEST\nMeasure grain moisture and inspect storage conditions before harvest."))
        body.addView(info("General educational guidance only; consult local agronomic advice.", 13f))
    }
}
