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

class GrowthActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fields = FieldStorage(this).loadFields()
        val root = featureLayout("🌱 Maize Growth Stages")
        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        if (fields.isEmpty()) {
            body.addView(info("Save a maize field first in My Fields."))
            return
        }
        val selector = Spinner(this)
        selector.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, fields.map { it.name })
        body.addView(selector)
        val picture = FieldDesignerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 340)
        }
        body.addView(picture)
        body.addView(info("Illustrated field preview: plant size changes with days since planting.", 13f))
        val details = info("")
        body.addView(details)
        selector.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val f = fields[position]
                picture.fieldWidth = f.width.toFloat()
                picture.fieldLength = f.length.toFloat()
                picture.fieldShape = f.shape
                picture.rowSpacing = f.rowSpacing.toFloat()
                if (f.plantingDate.isBlank() || f.maturityDays <= 0) {
                    picture.growthDays = 0
                    details.text = "Set planting date and maturity days for ${f.name} in My Fields."
                } else {
                    val g = MaizeGrowthCalculator.calculate(f.plantingDate, f.maturityDays)
                    picture.growthDays = g.daysSincePlanting
                    details.text = "Field: ${f.name}\nPlanted: ${f.plantingDate}\nAge: ${g.daysSincePlanting} days\nEstimated stage: ${g.stage}\nProgress: ${g.progressPercent}%\nDays to maturity: ${g.daysUntilMaturity}\n\n${g.advice}\n\nGrowth is an estimate; inspect the actual crop."
                }
            }
        }
    }
}
class HarvestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fields = FieldStorage(this).loadFields()
        val root = featureLayout("🚜 Harvest Tracker")
        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        if (fields.isEmpty()) {
            body.addView(info("Save a field first in My Fields."))
            return
        }
        val selector = Spinner(this)
        selector.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, fields.map { it.name })
        body.addView(selector)
        val stats = info("")
        body.addView(stats)
        val tons = EditText(this).apply {
            hint = "Total harvested tonnes"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val hectares = EditText(this).apply {
            hint = "Total harvested hectares"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        body.addView(tons)
        body.addView(hectares)
        fun display(f: SavedField) {
            stats.text = String.format(Locale.US,
                "Field: %s\nEstimated: %.2f tonnes\nHarvested: %.2f tonnes\nRemaining estimate: %.2f tonnes\nArea harvested: %.1f%%",
                f.name, f.estimatedTons(), f.harvestedTons, f.remainingTons(), f.harvestProgress())
            tons.setText(f.harvestedTons.toString())
            hectares.setText(f.harvestedHectares.toString())
        }
        selector.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                display(fields[position])
            }
        }
        body.addView(Button(this).apply {
            text = "Save harvest progress"
            setOnClickListener {
                val f = fields[selector.selectedItemPosition]
                val t = tons.text.toString().toDoubleOrNull()
                val h = hectares.text.toString().toDoubleOrNull()
                if (t == null || h == null || t < 0 || h < 0 || h > f.hectares()) {
                    Toast.makeText(this@HarvestActivity, "Enter valid harvested totals and hectares.", Toast.LENGTH_LONG).show()
                } else {
                    f.harvestedTons = t
                    f.harvestedHectares = h
                    if (FieldStorage(this@HarvestActivity).updateField(f)) {
                        display(f)
                        Toast.makeText(this@HarvestActivity, "Harvest saved", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@HarvestActivity, "Could not save harvest", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}
class KnowledgeActivity : Activity() {
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
