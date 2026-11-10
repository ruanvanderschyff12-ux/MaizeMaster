package com.maizemaster.app

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class KnowledgeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val padding = MaizeUi.dp(this, 16)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }
        content.addView(MaizeUi.header(this, "FIELD GUIDE", "Maize Knowledge",
            "Practical reminders from planting to storage."))
        val topics = listOf(
            "Planting" to "Check soil moisture, temperature, seed depth and spacing.",
            "Vegetative growth" to "Scout for weeds, pests, nutrient deficiency and leaf damage.",
            "Flowering" to "Heat and water stress at tasseling and silking can reduce kernel set.",
            "Grain filling" to "Monitor moisture, crop health and lodging.",
            "Harvest and storage" to "Check grain moisture before harvest and storage."
        )
        for ((title, description) in topics) {
            content.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = MaizeUi.surface(this@KnowledgeActivity)
                setPadding(padding, padding, padding, padding)
                addView(TextView(this@KnowledgeActivity).apply {
                    text = title
                    textSize = 17f
                    setTextColor(MaizeUi.forest)
                    setTypeface(null, android.graphics.Typeface.BOLD)
                })
                addView(TextView(this@KnowledgeActivity).apply {
                    text = description
                    textSize = 15f
                    setPadding(0, MaizeUi.dp(this@KnowledgeActivity, 4), 0, 0)
                })
            }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = MaizeUi.dp(this@KnowledgeActivity, 10) })
        }
        setContentView(ScrollView(this).apply {
            setBackgroundColor(MaizeUi.page)
            addView(content)
        })
    }
}
