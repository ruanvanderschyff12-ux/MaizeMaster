package com.maizemaster.app

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import java.util.Locale

/** Estimates yield using an existing saved field; no field dimensions need re-entry. */
class YieldEstimatorActivity : Activity() {
    private val storage by lazy { FieldStorage(this) }
    private fun fmt(n: Double) = String.format(Locale.US, "%.2f", n)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 30, 24, 24)
            setBackgroundColor(Color.rgb(246, 249, 246))
        }
        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
        root.addView(TextView(this).apply { text = "🌽 Yield Estimator"; textSize = 25f; setTextColor(Color.rgb(21, 83, 48)) })
        root.addView(TextView(this).apply { text = "Select a saved field and enter expected tons per hectare. Estimates are predictions, not guaranteed yields."; textSize = 15f })
        val fields = storage.loadFields()
        if (fields.isEmpty()) {
            root.addView(TextView(this).apply { text = "No saved fields yet. Create a field in Field Designer first."; textSize = 17f })
            root.addView(Button(this).apply { text = "Back"; setOnClickListener { finish() } })
            return
        }
        val selector = Spinner(this)
        selector.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, fields.map { it.name })
        root.addView(selector)
        val rate = EditText(this).apply {
            hint = "Expected tons per hectare"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        root.addView(rate)
        val output = TextView(this).apply { textSize = 17f; setPadding(0, 18, 0, 18) }
        root.addView(output)
        fun display(field: SavedField) {
            val hectares = field.hectares()
            output.text = "Field: ${field.name}\nArea: ${fmt(hectares)} ha\nEstimated harvest: ${fmt(field.estimatedTons())} tons\nActually harvested: ${fmt(field.harvestedTons)} tons\nEstimated remaining: ${fmt(field.remainingTons())} tons"
        }
        selector.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val field = fields[position]
                rate.setText(if (field.expectedYield > 0) field.expectedYield.toString() else "")
                display(field)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        root.addView(Button(this).apply {
            text = "Calculate & Save Estimate"
            setOnClickListener {
                val value = rate.text.toString().trim().toDoubleOrNull()
                if (value == null || !value.isFinite() || value <= 0.0 || value > 100.0) {
                    rate.error = "Enter a realistic positive tons-per-hectare estimate (up to 100)"
                    return@setOnClickListener
                }
                val selected = fields[selector.selectedItemPosition]
                val updated = selected.copy(expectedYield = value)
                if (storage.updateField(updated)) {
                    fields[selector.selectedItemPosition] = updated
                    display(updated)
                    Toast.makeText(this@YieldEstimatorActivity, "Estimate saved for ${updated.name}", Toast.LENGTH_SHORT).show()
                } else Toast.makeText(this@YieldEstimatorActivity, "Could not save estimate", Toast.LENGTH_LONG).show()
            }
        })
        root.addView(Button(this).apply { text = "Back"; setOnClickListener { finish() } })
    }
}
