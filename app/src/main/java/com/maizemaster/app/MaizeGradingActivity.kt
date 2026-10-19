package com.maizemaster.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale

class MaizeGradingActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
            setBackgroundColor(Color.rgb(245, 250, 246))
        }
        fun label(t: String, size: Float = 16f): TextView = TextView(this).apply {
            text = t
            textSize = size
            setTextColor(Color.rgb(25, 75, 40))
            setPadding(0, 12, 0, 10)
        }
        fun weightField(title: String): EditText {
            root.addView(label(title))
            return EditText(this).apply {
                hint = "Weight in grams"
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                root.addView(this)
            }
        }
        root.addView(label("🌽 Maize Grain Grading", 25f))
        root.addView(label("Enter measured sample weights. This is a defect calculator, not an official grade certificate.", 14f))
        val sample = weightField("Total sample weight (g)")
        val retained = weightField("Defective kernels retained above sieve (g)")
        val below = weightField("Defective material passing through sieve (g)")
        root.addView(label("Record the sieve specification used and follow the applicable grading standard.", 13f))
        val sieve = EditText(this).apply { hint = "Sieve size / specification (optional)" }
        root.addView(sieve)
        val result = label("", 17f)
        root.addView(Button(this).apply {
            text = "Calculate measured defects"
            setOnClickListener {
                val total = sample.text.toString().toDoubleOrNull()
                val upper = retained.text.toString().toDoubleOrNull()
                val lower = below.text.toString().toDoubleOrNull()
                if (total == null || upper == null || lower == null || !total.isFinite() || !upper.isFinite() || !lower.isFinite() ||
                    total <= 0 || upper < 0 || lower < 0 || upper + lower > total) {
                    result.text = "Enter valid non-negative weights. Defects must not exceed the total sample weight."
                } else {
                    val pctUpper = upper / total * 100
                    val pctLower = lower / total * 100
                    result.text = String.format(Locale.US,
"Sample: %.1f g\nDefects above sieve: %.2f%%\nDefects below sieve: %.2f%%\nCombined recorded defects: %.2f%%\n\nSieve: %s\n\nOfficial grade: NOT DETERMINED. Additional tests, defect categories and the applicable South African grading rules are needed.",
                        total, pctUpper, pctLower, pctUpper + pctLower,
                        sieve.text.toString().ifBlank { "Not specified" })
                }
            }
        })
        root.addView(result)
        root.addView(Button(this).apply {
            text = "📷 Open AI photo scanner (supporting evidence)"
            setOnClickListener { startActivity(Intent(this@MaizeGradingActivity, MaizeCameraActivity::class.java)) }
        })
        root.addView(Button(this).apply {
            text = "← Back"
            setOnClickListener { finish() }
        })
        val scroll = ScrollView(this).apply { isFillViewport = true }
        scroll.addView(root)
        setContentView(scroll)
    }
}
