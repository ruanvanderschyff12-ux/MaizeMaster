package com.maizemaster.app

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
    private val forest = Color.rgb(12, 78, 49)
    private val green = Color.rgb(24, 142, 72)
    private fun label(value: String, size: Float = 15f, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(forest)
        if (bold) setTypeface(null, Typeface.BOLD)
    }
    private fun rounded(color: Int, radius: Int = 18) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }
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
        window.statusBarColor = forest
        window.navigationBarColor = forest
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(244, 249, 245)) }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(20), dp(16), dp(28))
        }
        scroll.addView(body)
        setContentView(scroll)
        body.addView(label("🚜 Harvest command centre", 25f, true))
        body.addView(label("Track every load, monitor tonnes and see what's left to harvest.", 14f).apply {
            setPadding(0, dp(6), 0, dp(18))
        })
        val fields = storage.loadFields()
        if (fields.isEmpty()) body.addView(label("Save a field in My Fields to start tracking harvest trips."))
        val totalHarvested = fields.sumOf { it.harvestedTons }
        val totalRemaining = fields.sumOf { it.remainingTons() }
        val overview = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = rounded(forest)
        }
        body.addView(overview, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(18) })
        overview.addView(label("🌽 FARM HARVEST OVERVIEW", 13f, true).apply { setTextColor(Color.WHITE) })
        overview.addView(label(String.format(Locale.US, "%.2f t harvested", totalHarvested), 27f, true).apply {
            setTextColor(Color.WHITE)
            setPadding(0, dp(8), 0, dp(4))
        })
        overview.addView(label(String.format(Locale.US, "%.2f t estimated remaining  •  %d fields", totalRemaining, fields.size), 14f).apply {
            setTextColor(Color.WHITE)
        })
        for (field in fields) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(18), dp(18), dp(18))
                background = rounded(Color.WHITE)
                elevation = dp(2).toFloat()
            }
            body.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) })
            val active = trips.getString("${field.id}_active", null)
            val entries = history(field.id)
            card.addView(label("🌽 ${field.name}", 21f, true))
            card.addView(label(if (active == null) "○ READY FOR HARVEST" else "● HARVEST TRIP IN PROGRESS", 12f, true).apply {
                setTextColor(if (active == null) forest else green)
                setPadding(0, dp(6), 0, dp(12))
            })
            card.addView(label(String.format(Locale.US, "%.2f t", field.harvestedTons), 29f, true))
            card.addView(label("Total tonnes harvested", 13f))
            card.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 1000
                progress = (field.harvestProgress() * 10).toInt().coerceIn(0, 1000)
                progressTintList = android.content.res.ColorStateList.valueOf(green)
            }, LinearLayout.LayoutParams(-1, dp(10)).apply { topMargin = dp(14); bottomMargin = dp(8) })
            card.addView(label(String.format(Locale.US, "%.1f%% of field area harvested", field.harvestProgress()), 14f, true))
            card.addView(label(String.format(Locale.US,
                "📦 Estimated remaining: %.2f t\n🌾 Estimated total: %.2f t\n📐 Area harvested: %.2f / %.2f ha",
                field.remainingTons(), field.estimatedTons(), field.harvestedHectares, field.hectares()), 15f).apply {
                setPadding(0, dp(12), 0, dp(14))
            })
            card.addView(label(if (active != null) "🚜 Trip started: $active" else "No active harvest trip", 14f))
            card.addView(Button(this).apply {
                isAllCaps = false
                text = if (active == null) "▶ Start harvest trip" else "■ End trip and record load"
                setTextColor(Color.WHITE)
                backgroundTintList = android.content.res.ColorStateList.valueOf(green)
                setOnClickListener {
                    if (active == null) {
                        trips.edit().putString("${field.id}_active", stamp()).apply()
                        render()
                    } else finishTrip(field, active)
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
            card.addView(label("RECENT HARVEST LOADS", 13f, true).apply {
                setPadding(0, dp(16), 0, dp(8))
            })
            card.addView(label(if (entries.length() == 0) "No completed trips yet." else buildString {
                append("Completed trips: ${entries.length()}\n")
                for (i in entries.length() - 1 downTo maxOf(0, entries.length() - 5)) {
                    val entry = entries.getJSONObject(i)
                    append("${entry.optString("end")}  •  ${entry.optDouble("tons")} t  •  ${entry.optDouble("hectares")} ha\n")
                }
            }, 14f))
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
