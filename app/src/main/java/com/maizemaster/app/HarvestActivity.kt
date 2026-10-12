package com.maizemaster.app

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Harvest trips are recorded per field; totals persist between app launches. */
class HarvestActivity : Activity() {
    private val storage by lazy { FieldStorage(this) }
    private val trips by lazy { getSharedPreferences("maizemaster_harvest_trips", MODE_PRIVATE) }
    private fun stamp() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    private fun number(hint: String): EditText = EditText(this).apply {
        this.hint = hint
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    }
    private fun history(fieldId: String): JSONArray = try {
        JSONArray(trips.getString("${fieldId}_history", "[]"))
    } catch (_: Exception) { JSONArray() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun render() {
        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 32)
            setBackgroundColor(Color.rgb(246, 249, 246))
        }
        scroll.addView(body)
        setContentView(scroll)
        body.addView(TextView(this).apply {
            text = "🚜 Harvest Trip Tracker"
            textSize = 24f
            setTextColor(Color.rgb(15, 92, 47))
        })
        body.addView(TextView(this).apply {
            text = "Start a trip, then enter the measured tonnes and harvested area when you finish. Each completed trip adds to the field counter."
            textSize = 14f
        })
        val fields = storage.loadFields()
        if (fields.isEmpty()) body.addView(TextView(this).apply { text = "Save a field first in My Fields." })
        for (field in fields) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(18, 22, 18, 22)
                setBackgroundColor(Color.WHITE)
            }
            body.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 18 })
            val active = trips.getString("${field.id}_active", null)
            card.addView(TextView(this).apply {
                textSize = 19f
                text = "🌽 ${field.name}"
                setTextColor(Color.rgb(15, 92, 47))
            })
            card.addView(TextView(this).apply {
                textSize = 16f
                text = String.format(Locale.US,
                    "Estimated total: %.2f t\nHarvested: %.2f t\nEstimated remaining: %.2f t\nArea harvested: %.2f / %.2f ha (%.1f%%)",
                    field.estimatedTons(), field.harvestedTons, field.remainingTons(),
                    field.harvestedHectares, field.hectares(), field.harvestProgress())
            })
            val entries = history(field.id)
            card.addView(TextView(this).apply {
                text = if (active != null) "🟢 Trip started: $active" else "No trip currently running"
                textSize = 14f
            })
            card.addView(Button(this).apply {
                isAllCaps = false
                text = if (active == null) "▶ Start harvest trip" else "■ End trip and add harvest"
                setOnClickListener {
                    if (active == null) {
                        trips.edit().putString("${field.id}_active", stamp()).apply()
                        render()
                    } else {
                        finishTrip(field, active)
                    }
                }
            })
            if (active != null) card.addView(Button(this).apply {
                text = "Cancel trip (no harvest added)"
                isAllCaps = false
                setOnClickListener {
                    AlertDialog.Builder(this@HarvestActivity)
                        .setMessage("Cancel this trip without adding any harvest?")
                        .setPositiveButton("Cancel trip") { _, _ ->
                            trips.edit().remove("${field.id}_active").apply()
                            render()
                        }.setNegativeButton("Keep trip", null).show()
                }
            })
            card.addView(TextView(this).apply {
                textSize = 14f
                text = if (entries.length() == 0) "No completed trips yet." else buildString {
                    append("Completed trips: ${entries.length()}\n")
                    for (i in entries.length() - 1 downTo maxOf(0, entries.length() - 10)) {
                        val entry = entries.getJSONObject(i)
                        append("${entry.optString("end")} — ${entry.optDouble("tons")} t, ${entry.optDouble("hectares")} ha\n")
                    }
                }
            })
        }
    }

    private fun finishTrip(field: SavedField, started: String) {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 8, 28, 0)
        }
        val tons = number("Tonnes collected on this trip")
        val hectares = number("Hectares harvested on this trip")
        form.addView(tons)
        form.addView(hectares)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Finish trip — ${field.name}")
            .setMessage("Enter this trip's amounts, not the running total.")
            .setView(form)
            .setPositiveButton("Add trip", null)
            .setNegativeButton("Keep trip running", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val t = tons.text.toString().toDoubleOrNull()
                val h = hectares.text.toString().toDoubleOrNull()
                if (t == null || h == null || !t.isFinite() || !h.isFinite() ||
                    t < 0 || h < 0 || t + h <= 0 ||
                    field.harvestedHectares + h > field.hectares() + 0.000001) {
                    Toast.makeText(this, "Enter valid trip amounts within the remaining field area.", Toast.LENGTH_LONG).show()
                } else {
                    field.harvestedTons += t
                    field.harvestedHectares += h
                    if (!storage.updateField(field)) {
                        Toast.makeText(this, "Could not save harvest; trip remains active.", Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }
                    val entries = history(field.id)
                    entries.put(JSONObject().apply {
                        put("start", started)
                        put("end", stamp())
                        put("tons", t)
                        put("hectares", h)
                    })
                    trips.edit()
                        .putString("${field.id}_history", entries.toString())
                        .remove("${field.id}_active")
                        .apply()
                    dialog.dismiss()
                    render()
                }
            }
        }
        dialog.show()
    }
}
