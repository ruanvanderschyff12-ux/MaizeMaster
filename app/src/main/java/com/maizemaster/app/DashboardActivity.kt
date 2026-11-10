package com.maizemaster.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.*
import java.util.Locale
import com.google.firebase.auth.FirebaseAuth

class DashboardActivity : Activity() {
    private val dark = MaizeUi.forest
    private val green = Color.rgb(28, 137, 57)
    private val light = MaizeUi.page
    private val storage by lazy { FieldStorage(this) }
    private lateinit var fieldArea: LinearLayout
    private lateinit var summary: TextView
    private lateinit var accountStatus: TextView
    private val syncHandler = Handler(Looper.getMainLooper())
    private var homeVisible = false
    private val automaticSync = object : Runnable {
        override fun run() {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            if (!homeVisible || !FieldCloudSync.isAutomaticEnabled(this@DashboardActivity, uid)) return
            FieldCloudSync(applicationContext).sync {
                if (homeVisible && FirebaseAuth.getInstance().currentUser?.uid == uid) refresh()
            }
            syncHandler.postDelayed(this, 60_000L)
        }
    }


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
        background = MaizeUi.surface(this@DashboardActivity)
        elevation = dp(1).toFloat()
        setPadding(dp(16), dp(15), dp(16), dp(15))
    }
    private fun button(s: String, action: () -> Unit) = Button(this).apply {
        text = s
        isAllCaps = false
        textSize = 14f
        MaizeUi.primary(this)
        setOnClickListener { action() }
    }
    // Route each dashboard shortcut to its own field section.
    private fun openFields(section: Int = 4) = startActivity(Intent(this, MainActivity::class.java).putExtra("open_section", section))
    private fun openWeather() = startActivity(Intent(this, WeatherActivity::class.java))
    private fun openGrowth() = startActivity(Intent(this, GrowthActivity::class.java))
    private fun openHarvest() = startActivity(Intent(this, HarvestActivity::class.java))
    private fun openMaizeAi() = startActivity(Intent(this, MaizeAiActivity::class.java))
    private fun openKnowledge() = startActivity(Intent(this, KnowledgeActivity::class.java))
    private fun tile(icon: String, title: String, description: String, action: () -> Unit): LinearLayout =
        panel().apply {
            minimumHeight = dp(122)
            isFocusable = true
            addView(label(icon, 27f))
            addView(label(title, 15f, true).apply { setPadding(0, dp(6), 0, dp(3)) })
            addView(label(description, 13f, false, Color.DKGRAY))
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

        content.addView(MaizeUi.header(this, "FARM DASHBOARD", "MaizeMaster",
            "Manage your fields from planting through harvest."))

        val field = panel().apply { background = shape(Color.rgb(226, 242, 226)) }
        field.addView(label("🌱  My Field", 21f, true))
        summary = label("Loading saved fields...", 14f)
        summary.setPadding(0, dp(10), 0, dp(10))
        field.addView(summary)
        field.addView(button("View My Fields  ›") { openFields() })
        content.addView(field, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(17) })

        fun tools(title: String, items: List<Pair<Triple<String, String, String>, () -> Unit>>) {
            content.addView(label(title, 20f, true).apply { setPadding(dp(3), dp(4), 0, dp(12)) })
            for (pair in items.chunked(2)) {
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                pair.forEachIndexed { index, (item, action) ->
                    row.addView(tile(item.first, item.second, item.third, action),
                        LinearLayout.LayoutParams(0, -1, 1f).apply {
                            if (index == 0) rightMargin = dp(6) else leftMargin = dp(6)
                        })
                }
                content.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
            }
        }

        tools("Field management", listOf(
            Triple("🗺️", "Field Designer", "Plan field shape and rows") to { openFields(1) },
            Triple("🌱", "Planting Details", "Record planting and variety") to { openFields(2) },
            Triple("📊", "Harvest Estimates", "Plan expected tonnage") to { openFields(3) },
            Triple("🚜", "GPS Harvesting", "Track your harvest passes") to { openHarvest() },
            Triple("🌿", "Growth Stages", "Follow crop development") to { openGrowth() },
            Triple("🌤️", "Weather", "Check forecasts and risks") to { openWeather() },
            Triple("🌽", "Yield Estimator", "Calculate expected yield") to { startActivity(Intent(this, YieldEstimatorActivity::class.java)) },
            Triple("📍", "Measure a Field", "Walk or drive a GPS boundary") to { startActivity(Intent(this, FieldBoundaryActivity::class.java)) }
        ))
        tools("Grain and crop tools", listOf(
            Triple("🌽", "Grain Grading", "Estimate quality from a photo") to { startActivity(Intent(this, MaizeGradingActivity::class.java)) },
            Triple("📷", "AI Crop Scanner", "Check visible crop symptoms") to { startActivity(Intent(this, MaizeCameraActivity::class.java)) },
            Triple("🤖", "MaizeMate", "Ask maize farming questions") to { openMaizeAi() },
            Triple("🛡️", "Pests & Diseases", "Look up common crop risks") to { DiseaseGuide.show(this) }
        ))

        val accountPanel = panel()
        accountPanel.addView(label("Account & cloud sync", 19f, true))
        accountStatus = label("Checking account...", 13f).apply { setPadding(0, dp(6), 0, dp(10)) }
        accountPanel.addView(accountStatus)
        accountPanel.addView(button("Manage farmer account") {
            startActivity(Intent(this, FarmerAccountActivity::class.java))
        })
        content.addView(accountPanel, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })

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
            Triple("🚜", "Harvest", { openHarvest() }),
            Triple("🌽", "Grading", { startActivity(Intent(this, MaizeGradingActivity::class.java)) }),
            Triple("📖", "Knowledge", { openKnowledge() })
        )
        for ((symbol, name, action) in tabs) {
            val tab = column().apply {
                gravity = Gravity.CENTER
                background = if (name == "Home") shape(green, 12) else null
                setPadding(dp(3), dp(5), dp(3), dp(5))
                addView(label(symbol, 22f, false, Color.WHITE))
                addView(label(name, 11f, false, Color.WHITE))
                isFocusable = true
                contentDescription = name
                setOnClickListener { action() }
            }
            nav.addView(tab, LinearLayout.LayoutParams(0, dp(57), 1f))
        }
        root.addView(nav, LinearLayout.LayoutParams(-1, -2))
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        homeVisible = true
        if (::summary.isInitialized) refresh()
        if (::accountStatus.isInitialized) {
            val user = FirebaseAuth.getInstance().currentUser
            accountStatus.text = when {
                user == null -> "Sign in to sync saved fields across phones."
                FieldCloudSync.isAutomaticEnabled(this, user.uid) -> "Signed in: ${user.email ?: "Farmer"} · Automatic sync on Home"
                else -> "Signed in: ${user.email ?: "Farmer"} · Manual field sync"
            }
        }
        syncHandler.removeCallbacks(automaticSync)
        syncHandler.post(automaticSync)
        WeatherAutoSync.refresh(this)
    }

    override fun onPause() {
        homeVisible = false
        syncHandler.removeCallbacks(automaticSync)
        super.onPause()
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
