package com.maizemaster.app

import android.app.Activity
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView

class KnowledgeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this)
        val text = TextView(this).apply {
            textSize = 17f
            setPadding(28, 28, 28, 28)
            text = "📖 Maize farming knowledge\n\nPLANTING\nCheck soil moisture, temperature, seed depth and spacing.\n\nVEGETATIVE GROWTH\nScout for weeds, pests, nutrient deficiency and leaf damage.\n\nFLOWERING\nHeat and water stress at tasseling and silking can reduce kernel set.\n\nGRAIN FILLING\nMonitor moisture, crop health and lodging.\n\nHARVEST\nCheck grain moisture before harvest and storage.\n\nThese are general guidelines; consult local agronomic advice."
        }
        scroll.addView(text)
        setContentView(scroll)
    }
}
