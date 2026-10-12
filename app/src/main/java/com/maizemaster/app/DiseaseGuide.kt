
package com.maizemaster.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

object DiseaseGuide {

    private data class Problem(
        val name: String,
        val category: String,
        val weather: String,
        val symptoms: String,
        val damage: String,
        val advice: String,
        val photoUrl: String
    )

    private val earRotPhotos =
        "https://ag.purdue.edu/news/department/agry/" +
        "kernel-news/2024/09/corn-harvest-and-the-" +
        "importance-of-watching-for-ear-rots.html"

    private val toxinPhotos =
        "https://ag.purdue.edu/news/department/agry/" +
        "kernel-news/2026/09/risk-and-mycotoxin-" +
        "management-in-indiana-corn.html"

    private val leafPhotos =
        "https://extension.purdue.edu/news/" +
        "fieldcroppathology/2022/06/" +
        "field-crop-disease-resources-for-indiana.html"

    private val stalkPhotos =
        "https://ag.purdue.edu/news/department/agry/" +
        "kernel-news/2023/09/2023-corn-" +
        "stalkrot-earrot.html"

    private val seedlingPhotos =
        "https://corn.agronomy.wisc.edu/" +
        "Management/pdfs/IPMManual2010.pdf"

    private val problems = listOf(
        Problem(
            "Gibberella ear rot",
            "Ear and kernel diseases",
            "Cool, wet conditions around silking " +
                "can favor infection.",
            "Pink or reddish mold, often starting " +
                "at the cob tip and moving downward.",
            "Rotten kernels, reduced grain quality, " +
                "possible DON and zearalenone toxins.",
            "Inspect cobs in several parts of the " +
                "field. Arrange grain testing if " +
                "mycotoxin contamination is suspected.",
            earRotPhotos
        ),
        Problem(
            "Fusarium ear rot",
            "Ear and kernel diseases",
            "Warm conditions, plant stress and " +
                "insect injury can favor infection.",
            "White or pink mold on scattered " +
                "kernels, sometimes near insect damage.",
            "Poor grain quality and possible " +
                "fumonisin contamination.",
            "Inspect insect-damaged cobs and " +
                "test suspect grain.",
            toxinPhotos
        ),
        Problem(
            "Diplodia ear rot",
            "Ear and kernel diseases",
            "Wet conditions around and after " +
                "silking can favor infection.",
            "White fungal growth, often beginning " +
                "near the base of the cob. Kernels " +
                "may become lightweight and dull.",
            "Yield loss, poor grain quality " +
                "and possible livestock health risks.",
            "Inspect cobs and seek local advice " +
                "before feeding severely affected grain.",
            earRotPhotos
        ),
        Problem(
            "Aspergillus ear rot",
            "Ear and kernel diseases",
            "Hot, dry stress can increase risk, " +
                "especially during grain development.",
            "Olive-green or yellow-green powdery " +
                "mold on affected kernels.",
            "Possible aflatoxin contamination, " +
                "which can make grain unsafe.",
            "Do not rely on appearance to judge " +
                "grain safety. Arrange aflatoxin testing.",
            toxinPhotos
        ),
        Problem(
            "Penicillium ear and storage mold",
            "Ear and kernel diseases",
            "Damaged kernels and unsuitable " +
                "storage moisture favor fungal growth.",
            "Blue-green fungal growth on " +
                "damaged kernels.",
            "Spoilage and possible mycotoxin risks, " +
                "depending on the fungal species.",
            "Inspect storage conditions and " +
                "test suspect grain.",
            toxinPhotos
        ),
        Problem(
            "Gray leaf spot",
            "Leaf diseases",
            "Warm, humid weather and extended " +
                "leaf wetness favor infection.",
            "Rectangular gray or tan lesions " +
                "limited by the leaf veins.",
            "Loss of healthy leaf area and " +
                "reduced grain-filling potential.",
            "Scout leaves across the field " +
                "and record disease severity.",
            leafPhotos
        ),
        Problem(
            "Northern corn leaf blight",
            "Leaf diseases",
            "Humid conditions and prolonged " +
                "leaf wetness favor infection.",
            "Long, cigar-shaped tan lesions " +
                "on maize leaves.",
            "Reduced photosynthesis and " +
                "possible yield loss.",
            "Inspect lower and upper leaves " +
                "and monitor spread.",
            leafPhotos
        ),
        Problem(
            "Common rust",
            "Leaf diseases",
            "Suitable temperatures and moisture " +
                "can encourage rust development.",
            "Scattered reddish-brown raised " +
                "pustules on maize leaves.",
            "Leaf damage and possible yield loss " +
                "if infection becomes severe.",
            "Inspect both sides of leaves and " +
                "confirm which rust is present.",
            leafPhotos
        ),
        Problem(
            "Southern rust",
            "Leaf diseases",
            "Warm, humid conditions can " +
                "favor infection.",
            "Small orange pustules, usually " +
                "more concentrated on upper " +
                "leaf surfaces.",
            "Rapid loss of green leaf area " +
                "during severe outbreaks.",
            "Scout frequently in favorable " +
                "weather and seek local agronomic advice.",
            "https://cropprotectionnetwork.org/" +
                "publications/an-overview-of-southern-rust"
        ),
        Problem(
            "Seed rot and damping-off",
            "Roots and seedlings",
            "Cold, wet and poorly drained soil " +
                "can favor seedling pathogens.",
            "Rotten seeds, missing plants, weak " +
                "seedlings or poor emergence.",
            "Reduced plant population and " +
                "uneven crop establishment.",
            "Inspect affected seedbeds and " +
                "check drainage and planting depth.",
            seedlingPhotos
        ),
        Problem(
            "Root and stalk rot",
            "Roots and stalks",
            "Different pathogens and stresses, " +
                "including drought or excess water, " +
                "can weaken stalks.",
            "Discolored internal stalk tissue, " +
                "weak stalk bases, lodging or " +
                "premature plant death.",
            "Harvest losses and difficulty " +
                "combining lodged maize.",
            "Check stalk strength in multiple " +
                "areas and prioritize at-risk fields.",
            stalkPhotos
        ),
        Problem(
            "Waterlogging damage",
            "Weather and physical damage",
            "Heavy rainfall, flooding and " +
                "poor soil drainage.",
            "Yellow leaves, slow growth, " +
                "stressed roots and standing water.",
            "Reduced oxygen to roots, nutrient " +
                "losses and possible plant death.",
            "Record flooded areas and duration. " +
                "Inspect roots and drainage when safe.",
            seedlingPhotos
        ),
        Problem(
            "Hail and storm damage",
            "Weather and physical damage",
            "Thunderstorms, hail and strong winds.",
            "Shredded leaves, bruised cobs, " +
                "broken stalks or lodged plants.",
            "Lost leaf area, exposed kernels " +
                "and possible harvest losses.",
            "Inspect damage after the storm. " +
                "Watch damaged ears for later rot.",
            seedlingPhotos
        ),
        Problem(
            "Storage mold and grain spoilage",
            "Harvest and storage",
            "Grain stored too wet or without " +
                "suitable aeration and temperature control.",
            "Musty smell, visible mold, " +
                "discolored kernels or heating grain.",
            "Spoilage, quality rejection and " +
                "potential mycotoxin contamination.",
            "Measure grain moisture, manage drying " +
                "and aeration, and test suspect grain.",
            toxinPhotos
        )
    )

    fun show(activity: Activity) {
        val density =
            activity.resources.displayMetrics.density

        fun dp(value: Int): Int =
            (value * density).toInt()

        val layout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(18), dp(16), dp(18), dp(20)
            )
            setBackgroundColor(
                Color.rgb(242, 248, 239)
            )
        }

        fun addText(
            value: String,
            size: Float,
            heading: Boolean = false
        ) {
            layout.addView(
                TextView(activity).apply {
                    text = value
                    textSize = size
                    setTextColor(
                        if (heading) {
                            Color.rgb(24, 96, 47)
                        } else {
                            Color.rgb(40, 54, 43)
                        }
                    )
                    setPadding(
                        0, dp(10), 0, dp(8)
                    )
                }
            )
        }

        addText(
            "🌽 Maize Disease Identification",
            22f,
            true
        )

        addText(
            "Tap a problem to view symptoms, " +
                "weather conditions, possible " +
                "damage and real reference photos.",
            15f
        )

        var previousCategory = ""

        for (problem in problems) {
            if (problem.category != previousCategory) {
                previousCategory = problem.category
                addText(
                    "🌿 ${problem.category}",
                    19f,
                    true
                )
            }

            layout.addView(
                Button(activity).apply {
                    text = "🔍 ${problem.name}"
                    isAllCaps = false

                    setOnClickListener {
                        showProblem(
                            activity,
                            problem
                        )
                    }
                }
            )
        }

        addText(
            "⚠️ Important: These descriptions " +
                "and photos are for education, " +
                "not a confirmed diagnosis. " +
                "Weather cannot prove that a " +
                "disease is present. Laboratory " +
                "testing may be necessary for " +
                "grain safety.",
            13f
        )

        val scroll = ScrollView(activity)
        scroll.addView(layout)

        AlertDialog.Builder(activity)
            .setView(scroll)
            .setPositiveButton(
                "Close",
                null
            )
            .show()
    }

    private fun showProblem(
        activity: Activity,
        problem: Problem
    ) {
        val details = buildString {
            append("🌦️ WEATHER CONDITIONS\n")
            append(problem.weather)

            append("\n\n🔍 SYMPTOMS\n")
            append(problem.symptoms)

            append("\n\n⚠️ POSSIBLE DAMAGE\n")
            append(problem.damage)

            append("\n\n👨‍🌾 FARMER ADVICE\n")
            append(problem.advice)

            append(
                "\n\n📸 Tap VIEW PHOTOS to open " +
                    "an agricultural reference " +
                    "page in your browser."
            )

            append(
                "\n\nPhotos may show multiple " +
                    "diseases for comparison. " +
                    "They do not confirm the " +
                    "cause of symptoms in your field."
            )
        }

        AlertDialog.Builder(activity)
            .setTitle(problem.name)
            .setMessage(details)
            .setPositiveButton(
                "📸 VIEW PHOTOS"
            ) { _, _ ->
                try {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(problem.photoUrl)
                    )
                    activity.startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(
                        activity,
                        "Could not open photo page.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNegativeButton(
                "Close",
                null
            )
            .show()
    }
}
