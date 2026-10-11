
package com.maizemaster.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import java.util.Locale

class DashboardActivity : Activity() {

    private val darkGreen = Color.rgb(24, 86, 48)
    private val green = Color.rgb(43, 133, 72)
    private val paleGreen = Color.rgb(231, 244, 232)
    private val backgroundColor = Color.rgb(246, 249, 245)

    private lateinit var storage: FieldStorage
    private lateinit var summary: TextView
    private lateinit var fieldContainer: LinearLayout

    private fun rounded(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = 28f
        }
    }

    private fun text(
        value: String,
        size: Float = 16f,
        bold: Boolean = false,
        color: Int = darkGreen
    ): TextView {
        return TextView(this).apply {
            this.text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
            setPadding(12, 12, 12, 12)
        }
    }

    private fun card(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 20, 22, 20)
            background = rounded(Color.WHITE)
            elevation = 4f

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 12, 0, 12)
            }
        }
    }

    private fun button(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 15f
            setTextColor(Color.WHITE)
            background = rounded(green)
            setOnClickListener { action() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        storage = FieldStorage(this)
        window.statusBarColor = darkGreen

        val scroll = ScrollView(this).apply {
            setBackgroundColor(backgroundColor)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 40)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 28)
            background = rounded(darkGreen)
        }

        header.addView(
            text("🌽 MaizeMaster", 29f, true, Color.WHITE)
        )

        header.addView(
            text(
                "Your Smart Farming Dashboard",
                16f,
                false,
                Color.WHITE
            )
        )

        header.addView(
            text(
                "Manage your maize fields and harvests",
                14f,
                false,
                Color.WHITE
            )
        )

        layout.addView(header)

        layout.addView(
            text("🌾 Farm Overview", 23f, true)
        )

        val overviewCard = card().apply {
            background = rounded(paleGreen)
        }

        summary = text("Loading your fields...", 17f, true)
        overviewCard.addView(summary)
        layout.addView(overviewCard)

        layout.addView(
            text("🚜 Quick Actions", 23f, true)
        )

        val actions = card()

        actions.addView(
            button("🌱 MANAGE MY FIELDS") {
                startActivity(
                    Intent(this, MainActivity::class.java)
                )
            }
        )

        actions.addView(
            button("📏 FIELD & YIELD CALCULATOR") {
                startActivity(
                    Intent(this, MainActivity::class.java)
                )
            }
        )

        actions.addView(
            button("🌦️ WEATHER & RISKS") {
                startActivity(
                    Intent(this, WeatherActivity::class.java)
                )
            }
        )

        actions.addView(
            button("🚜 RECORD HARVEST") {
                startActivity(
                    Intent(this, MainActivity::class.java)
                )
            }
        )

        layout.addView(actions)

        layout.addView(
            text("🌽 My Maize Fields", 23f, true)
        )

        fieldContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        layout.addView(fieldContainer)

        scroll.addView(layout)
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        if (::storage.isInitialized) {
            refreshDashboard()
        }
    }

    private fun refreshDashboard() {
        val fields = storage.loadFields()

        val totalHectares = fields.sumOf { it.hectares() }
        val estimatedTons = fields.sumOf { it.estimatedTons() }
        val harvestedTons = fields.sumOf { it.harvestedTons }
        val remainingTons = fields.sumOf { it.remainingTons() }

        summary.text = String.format(
            Locale.getDefault(),
            "🌾 Saved Fields: %d\n\n" +
            "📏 Total Area: %.2f ha\n\n" +
            "🌽 Estimated Harvest: %.2f tons\n\n" +
            "🚜 Harvested: %.2f tons\n\n" +
            "📦 Remaining Estimate: %.2f tons",
            fields.size,
            totalHectares,
            estimatedTons,
            harvestedTons,
            remainingTons
        )

        fieldContainer.removeAllViews()

        if (fields.isEmpty()) {
            val emptyCard = card()
            emptyCard.addView(
                text("No saved fields yet. Add your first maize field.")
            )
            emptyCard.addView(
                button("ADD FIRST FIELD") {
                    startActivity(
                        Intent(this, MainActivity::class.java)
                    )
                }
            )
            fieldContainer.addView(emptyCard)
            return
        }

        for (field in fields) {
            val fieldCard = card()

            fieldCard.addView(
                text("🌽 ${field.name}", 20f, true)
            )

            fieldCard.addView(
                text(
                    String.format(
                        Locale.getDefault(),
                        "%s • %s\n" +
                        "Area: %.2f ha\n" +
                        "Expected: %.2f tons\n" +
                        "Harvested: %.2f tons\n" +
                        "Remaining: %.2f tons",
                        field.maizeVariety,
                        field.shape,
                        field.hectares(),
                        field.estimatedTons(),
                        field.harvestedTons,
                        field.remainingTons()
                    )
                )
            )

            val progress = ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
            ).apply {
                max = 100
                this.progress = field.harvestProgress().toInt()
                progressTintList =
                    android.content.res.ColorStateList.valueOf(green)
            }

            fieldCard.addView(progress)

            fieldCard.addView(
                text(
                    String.format(
                        Locale.getDefault(),
                        "%.1f%% of field area harvested",
                        field.harvestProgress()
                    ),
                    14f
                )
            )

            fieldCard.addView(
                button("OPEN FIELD MANAGER") {
                    startActivity(
                        Intent(this, MainActivity::class.java)
                    )
                }
            )

            fieldCard.addView(
                button("🌦️ FIELD WEATHER") {
                    startActivity(
                        Intent(this, WeatherActivity::class.java)
                            .putExtra("field_id", field.id)
                    )
                }
            )

            fieldContainer.addView(fieldCard)
        }
    }
}
