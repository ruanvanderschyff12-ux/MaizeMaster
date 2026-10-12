package com.maizemaster.app

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.widget.*
import android.graphics.Color

class HarvestActivity : Activity() {
    private val storage by lazy { FieldStorage(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = ScrollView(this)
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.rgb(246, 249, 246))
        }
        root.addView(list)
        setContentView(root)
        list.addView(TextView(this).apply { text = "🚜 Harvest tracker"; textSize = 24f })
        val fields = storage.loadFields()
        if (fields.isEmpty()) list.addView(TextView(this).apply { text = "Save a field first to track harvesting." })
        for (field in fields) {
            val summary = TextView(this).apply {
                textSize = 16f
                setPadding(0, 20, 0, 12)
            }
            fun refresh() {
                summary.text = "${field.name}\nHarvested: ${field.harvestedTons} t\nRemaining estimate: ${"%.2f".format(field.remainingTons())} t\nArea harvested: ${"%.1f".format(field.harvestProgress())}%"
            }
            refresh()
            list.addView(summary)
            list.addView(Button(this).apply {
                text = "Update ${field.name}"
                isAllCaps = false
                setOnClickListener {
                    val form = LinearLayout(this@HarvestActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(28, 10, 28, 0)
                    }
                    val hectares = EditText(this@HarvestActivity).apply {
                        hint = "Total hectares harvested"
                        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                        setText(field.harvestedHectares.toString())
                    }
                    val tons = EditText(this@HarvestActivity).apply {
                        hint = "Total tonnes harvested"
                        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                        setText(field.harvestedTons.toString())
                    }
                    form.addView(hectares)
                    form.addView(tons)
                    AlertDialog.Builder(this@HarvestActivity)
                        .setTitle("Record harvest")
                        .setView(form)
                        .setPositiveButton("Save") { _, _ ->
                            val h = hectares.text.toString().toDoubleOrNull()
                            val t = tons.text.toString().toDoubleOrNull()
                            if (h == null || t == null || !h.isFinite() || !t.isFinite() ||
                                h < 0 || h > field.hectares() || t < 0) {
                                Toast.makeText(this@HarvestActivity, "Enter valid totals within the field area.", Toast.LENGTH_LONG).show()
                            } else {
                                field.harvestedHectares = h
                                field.harvestedTons = t
                                if (storage.updateField(field)) refresh()
                                else Toast.makeText(this@HarvestActivity, "Could not save harvest.", Toast.LENGTH_LONG).show()
                            }
                        }.setNegativeButton("Cancel", null).show()
                }
            })
        }
    }
}
