package com.maizemaster.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import java.util.Locale

class DashboardActivity : Activity() {
    private val dark = Color.rgb(8, 65, 40)
    private val green = Color.rgb(28, 137, 57)
    private val light = Color.rgb(246, 249, 246)
    private val storage by lazy { FieldStorage(this) }
    private lateinit var fieldArea: LinearLayout
    private lateinit var summary: TextView

    private fun dp(v: Int) = (v * resources.displayMetrics.density + .5f).toInt()
    private fun shape(color: Int, radius: Int = 15) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }
    private fun label(s: String, size: Float = 15f, bold: Boolean = false, color: Int = dark) =
        TextView(this).apply {
            text = s
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
        }
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun panel() = column().apply {
        background = shape(Color.WHITE)
        elevation = dp(3).toFloat()
        setPadding(dp(16), dp(15), dp(16), dp(15))
    }
    private fun button(s: String, action: () -> Unit) = Button(this).apply {
        text = s
        isAllCaps = false
        textSize = 14f
        setTextColor(Color.WHITE)
        background = shape(green, 13)
        setOnClickListener { action() }
    }
    private fun openFields() = startActivity(Intent(this, MainActivity::class.java))
    private fun openWeather() = startActivity(Intent(this, WeatherActivity::class.java))
    private fun openGrowth() = startActivity(Intent(this, GrowthActivity::class.java))
    private fun openHarvest() = startActivity(Intent(this, HarvestActivity::class.java))
    private fun openKnowledge() = startActivity(Intent(this, KnowledgeActivity::class.java))
    private fun unavailable(title: String) {
        AlertDialog.Builder(this).setTitle(title)
            .setMessage("This feature is being developed. Your current field management and weather tools are available now.")
            .setPositiveButton("OK", null).show()
    }
    private fun showFeaturePage(title: String, content: String) {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            setBackgroundColor(light)
        }
        page.addView(label(title, 23f, true))
        val scroller = ScrollView(this)
        scroller.addView(label(content, 16f).apply {
            setPadding(0, dp(18), 0, dp(18))
        })
        page.addView(scroller, LinearLayout.LayoutParams(-1, 0, 1f))
        val dialog = AlertDialog.Builder(this).setView(page).create()
        page.addView(button("Back to dashboard") { dialog.dismiss() })
        dialog.show()
        dialog.window?.setLayout(-1, -1)
    }

    private fun showGrowth() {
        val fields = storage.loadFields()
        val message = if (fields.isEmpty()) "Save a field first to track maize growth." else fields.joinToString("\n\n") { field ->
            val growth = if (field.plantingDate.isNotBlank() && field.maturityDays > 0)
                MaizeGrowthCalculator.calculate(field.plantingDate, field.maturityDays).stage
            else "Set planting date and maturity days in My Fields"
            "🌽 ${field.name}\nGrowth: $growth\nPlanted: ${field.plantingDate.ifBlank { "Not set" }}"
        }
        showFeaturePage("🌱 Maize Growth Stages", message)
    }

    private fun showKnowledge() {
        showFeaturePage("📖 Maize Farming Knowledge", "PLANTING\nPlant in suitable soil moisture and temperature. Check seed depth, row spacing and plant population.\n\nGROWTH\nScout for uneven emergence, weeds, nutrient deficiencies and leaf damage.\n\nFLOWERING\nHeat and water stress during tasseling and silking can reduce kernel set.\n\nHARVEST\nCheck grain moisture and storage conditions before harvesting.\n\nThese are general guidelines; use local agronomic advice for your field.")
    }

    private fun tile(icon: String, title: String, description: String, action: () -> Unit): LinearLayout =
        panel().apply {
            minimumHeight = dp(116)
            addView(label(icon, 27f))
            addView(label(title, 15f, true).apply { setPadding(0, dp(6), 0, dp(3)) })
            addView(label(description, 11f, false, Color.DKGRAY))
            setOnClickListener { action() }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = dark
        window.navigationBarColor = dark

        val root = column().apply { setBackgroundColor(light) }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
        }
        val body = column()
        scroll.addView(body)

        body.addView(ImageView(this).apply {
            setImageResource(R.drawable.maizemaster_realistic_banner)
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "MaizeMaster maize field banner"
            layoutParams = LinearLayout.LayoutParams(-1, dp(160))
        })

        val content = column().apply {
            setPadding(dp(14), dp(14), dp(14), dp(18))
        }
        body.addView(content)

        val weather = panel()
        weather.addView(label("☀️  Weather & maize risks", 19f, true))
        weather.addView(label("See your local forecast and how conditions can affect maize.", 13f).apply {
            setPadding(0, dp(7), 0, dp(9))
        })
        weather.addView(button("View live weather  ›") { openWeather() })
        content.addView(weather, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })

        val field = panel().apply { background = shape(Color.rgb(226, 242, 226)) }
        field.addView(label("🌱  My Field", 21f, true))
        summary = label("Loading saved fields...", 14f)
        summary.setPadding(0, dp(10), 0, dp(10))
        field.addView(summary)
        field.addView(button("View My Fields  ›") { openFields() })
        content.addView(field, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(17) })

        content.addView(label("Explore MaizeMaster", 20f, true).apply {
            setPadding(dp(3), 0, 0, dp(12))
        })

        val items = listOf(
            Triple("🗺️", "Field Designer", "Design your field") to { openFields() },
            Triple("🌤️", "Weather", "Forecast & risks") to { openWeather() },
            Triple("🌱", "Growth Stages", "Follow crop growth") to { openGrowth() },
            Triple("🌽", "Yield Estimator", "Estimate your harvest") to { openFields() },
            Triple("🛡️", "Pests & Diseases", "Learn about crop risks") to { DiseaseGuide.show(this) },
            Triple("🚜", "Harvest Tracker", "Record harvest progress") to { openHarvest() }
        )
        for (i in 0 until 3) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (j in 0 until 2) {
                val item = items[i * 2 + j]
                row.addView(tile(item.first.first, item.first.second, item.first.third, item.second),
                    LinearLayout.LayoutParams(0, -2, 1f).apply {
                        if (j == 0) rightMargin = dp(6) else leftMargin = dp(6)
                    })
            }
            content.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        }

        fieldArea = column()
        content.addView(label("Saved Fields", 19f, true).apply { setPadding(0, dp(6), 0, dp(8)) })
        content.addView(fieldArea)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = shape(dark, 18)
            setPadding(dp(4), dp(5), dp(4), dp(5))
        }
        val tabs = listOf(
            Triple("⌂", "Home", { scroll.smoothScrollTo(0, 0) }),
            Triple("🌱", "My Fields", { openFields() }),
            Triple("☁", "Weather", { openWeather() }),
            Triple("📖", "Knowledge", { showKnowledge() })
        )
        for ((symbol, name, action) in tabs) {
            val tab = column().apply {
                gravity = Gravity.CENTER
                background = if (name == "Home") shape(green, 12) else null
                setPadding(dp(3), dp(5), dp(3), dp(5))
                addView(label(symbol, 22f, false, Color.WHITE))
                addView(label(name, 11f, false, Color.WHITE))
                setOnClickListener { action() }
            }
            nav.addView(tab, LinearLayout.LayoutParams(0, dp(57), 1f))
        }
        root.addView(nav, LinearLayout.LayoutParams(-1, -2))
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        if (::summary.isInitialized) refresh()
        WeatherAutoSync.refresh(this)
    }

    private fun refresh() {
        val fields = storage.loadFields()
        val hectares = fields.sumOf { it.hectares() }
        val expected = fields.sumOf { it.estimatedTons() }
        val harvested = fields.sumOf { it.harvestedTons }
        summary.text = String.format(Locale.getDefault(),
            "Saved fields: %d    •    Area: %.2f ha\nEstimated harvest: %.2f tons\nHarvested: %.2f tons",
            fields.size, hectares, expected, harvested)
        fieldArea.removeAllViews()
        if (fields.isEmpty()) {
            fieldArea.addView(label("No fields saved yet. Tap Field Designer to add your first field.", 13f))
        } else {
            for (f in fields.take(5)) {
                val item = panel()
                item.addView(label("🌽  " + f.name, 16f, true))
                item.addView(label(String.format(Locale.getDefault(),
                    "%.2f ha  •  %.2f tons expected  •  %.2f tons harvested",
                    f.hectares(), f.estimatedTons(), f.harvestedTons), 12f))
                item.setOnClickListener { openFields() }
                fieldArea.addView(item, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
            }
        }
    }
}
