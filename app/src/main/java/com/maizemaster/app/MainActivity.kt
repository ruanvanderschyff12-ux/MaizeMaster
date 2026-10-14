
package com.maizemaster.app

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class MainActivity : Activity() {

    private val forest = Color.rgb(21, 83, 48)
    private val green = Color.rgb(38, 125, 69)
    private val pale = Color.rgb(234, 245, 235)
    private val page = Color.rgb(247, 249, 246)
    private val ink = Color.rgb(35, 56, 43)
    private val muted = Color.rgb(103, 121, 108)

    private lateinit var storage: FieldStorage
    private lateinit var refreshList: () -> Unit
    private var editingId: String? = null

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()

    private fun bg(color: Int, radius: Int = 18) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun text(
        value: String,
        size: Float = 15f,
        color: Int = ink,
        bold: Boolean = false
    ) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(null, Typeface.BOLD)
    }

    private fun vertical() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
    }

    private fun card() = vertical().apply {
        setPadding(dp(18), dp(18), dp(18), dp(18))
        background = bg(Color.WHITE)
        elevation = dp(2).toFloat()
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(14)
        }
    }

    private fun heading(value: String) =
        text(value, 19f, forest, true).apply {
            setPadding(0, 0, 0, dp(13))
        }

    private fun input(
        hintValue: String,
        numeric: Boolean = true
    ) = EditText(this).apply {
        hint = hintValue
        inputType = if (numeric) {
            InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL
        } else {
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        textSize = 15f
        setTextColor(ink)
        setSingleLine(true)
        setPadding(dp(14), dp(13), dp(14), dp(13))
        background = bg(page, 12)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(10)
        }
    }

    private fun button(
        value: String,
        secondary: Boolean = false
    ) = Button(this).apply {
        text = value
        textSize = 13f
        isAllCaps = false
        setTextColor(if (secondary) forest else Color.WHITE)
        background = bg(if (secondary) pale else green, 12)
        layoutParams = LinearLayout.LayoutParams(-1, dp(49)).apply {
            bottomMargin = dp(8)
        }
    }

    private fun fmt(value: Double): String =
        String.format(Locale.US, "%.2f", value)

    private fun message(value: String) {
        Toast.makeText(this, value, Toast.LENGTH_LONG).show()
    }

    private fun dateFormat() =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
        }

    private fun daysSince(date: String): Long? {
        return try {
            val planted = dateFormat().parse(date) ?: return null
            val today = dateFormat().parse(
                dateFormat().format(System.currentTimeMillis())
            ) ?: return null

            TimeUnit.MILLISECONDS.toDays(
                today.time - planted.time
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun growthStages() = arrayOf(
        "Not planted",
        "Germination (VE)",
        "Seedling (V1-V3)",
        "Vegetative growth (V4-V12)",
        "Late vegetative growth (V13-VT)",
        "Tasseling (VT)",
        "Silking (R1)",
        "Blister (R2)",
        "Milk (R3)",
        "Dough (R4)",
        "Dent (R5)",
        "Physiological maturity (R6)",
        "Harvested"
    )

    private fun growthProgress(stage: String): Int {
        val stages = growthStages()
        val index = stages.indexOf(stage)
        return if (index < 0) 0 else {
            (index * 100 / (stages.size - 1)).coerceIn(0, 100)
        }
    }

    private fun pickDate(
        current: String,
        onSelected: (String) -> Unit
    ) {
        val calendar = Calendar.getInstance()

        try {
            if (current.isNotBlank()) {
                dateFormat().parse(current)?.let {
                    calendar.time = it
                }
            }
        } catch (_: Exception) {}

        DatePickerDialog(
            this,
            { _, year, month, day ->
                onSelected(
                    String.format(
                        Locale.US,
                        "%04d-%02d-%02d",
                        year,
                        month + 1,
                        day
                    )
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    override fun onResume() {
        super.onResume()
        if (::refreshList.isInitialized) refreshList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        storage = FieldStorage(this)
        window.statusBarColor = forest
        window.navigationBarColor = forest

        val scroll = ScrollView(this).apply {
            setBackgroundColor(page)
            isFillViewport = true
        }

        val layout = vertical().apply {
            setPadding(0, 0, 0, dp(28))
        }

        scroll.addView(layout)
        setContentView(scroll)

        // Full-width, square-cornered banner.
        val header = ImageView(this).apply {
            setImageResource(R.drawable.maizemaster_realistic_banner)
            scaleType = ImageView.ScaleType.CENTER_CROP
            adjustViewBounds = false
            contentDescription = "MaizeMaster - Your maize farm at a glance"
            layoutParams = LinearLayout.LayoutParams(-1, dp(175))
        }
        layout.addView(header)

        // Content cards retain comfortable side margins.
        val content = vertical().apply {
            setPadding(dp(16), dp(16), dp(16), 0)
        }
        layout.addView(content)

                // Dedicated pages: only the selected section is visible.
        val homePage = vertical()
        val designerPage = vertical()
        val plantingPage = vertical()
        val estimatePage = vertical()
        val fieldsPage = vertical()
        val pages = listOf(homePage, designerPage, plantingPage, estimatePage, fieldsPage)
        val pageNames = listOf("Home", "Field Designer", "Planting Details", "Harvest Estimate", "My Fields")
        val navigation = card()
        navigation.addView(heading("🌽 MaizeMaster sections"))
        val pageSelector = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item, pageNames)
        }
        navigation.addView(pageSelector)
        content.addView(navigation)
        pages.forEach { content.addView(it) }

        // FARM OVERVIEW

        val overview = card()
        overview.addView(heading("Farm overview"))

        val overviewText = text("", 15f, forest, true)
        overview.addView(overviewText)
        homePage.addView(overview)

        // WEATHER

        val weatherCard = card()
        weatherCard.addView(
            heading("🌦️ Weather & field risks")
        )
        weatherCard.addView(
            text(
                "Check weather forecasts and possible maize-growing risks.",
                14f,
                muted
            )
        )
        weatherCard.addView(
            button("Open weather forecast").apply {
                setOnClickListener {
                    startActivity(
                        Intent(
                            this@MainActivity,
                            WeatherActivity::class.java
                        )
                    )
                }
            }
        )
        homePage.addView(weatherCard)

        fun showPage(index: Int) {
            pages.forEachIndexed { i, section ->
                section.visibility = if (i == index) View.VISIBLE else View.GONE
            }
            scroll.post { scroll.smoothScrollTo(0, 0) }
        }
        pageSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                showPage(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        fun navigateTo(index: Int) {
            if (pageSelector.selectedItemPosition == index) showPage(index)
            else pageSelector.setSelection(index)
        }
        homePage.addView(card().apply {
            addView(heading("Manage your maize farm"))
            addView(text("Choose a section below to work on it without the other forms in the way.", 14f, muted))
            listOf("🌱 Field Designer" to 1, "🚜 Planting Details" to 2,
                "📊 Harvest Estimate" to 3, "🌾 My Saved Fields" to 4).forEach { (label, index) ->
                addView(button(label).apply { setOnClickListener { navigateTo(index) } })
            }
        })

        // FIELD DESIGNER

        val fieldCard = card()
        fieldCard.addView(heading("🌱 Field designer"))

        val fieldView = FieldDesignerView(this).apply {
            background = bg(pale, 14)
            layoutParams =
                LinearLayout.LayoutParams(-1, dp(250)).apply {
                    bottomMargin = dp(12)
                }
        }

        fieldCard.addView(fieldView)
        val editOutlineButton = button("✏️ Adjust GPS field outline", true)
        val saveOutlineButton = button("💾 Save adjusted outline")
        val cancelOutlineButton = button("Cancel outline changes", true)
        saveOutlineButton.visibility = View.GONE
        cancelOutlineButton.visibility = View.GONE
        fieldCard.addView(editOutlineButton)
        fieldCard.addView(saveOutlineButton)
        fieldCard.addView(cancelOutlineButton)

        fun stopOutlineEditing() {
            fieldView.editingBoundary = false
            editOutlineButton.visibility = View.VISIBLE
            saveOutlineButton.visibility = View.GONE
            cancelOutlineButton.visibility = View.GONE
        }
        fun restoreOutline() {
            fieldView.gpsBoundary = try {
                JSONArray(getSharedPreferences("maizemaster_boundaries", MODE_PRIVATE)
                    .getString("${editingId}_boundary", "[]"))
            } catch (_: Exception) { JSONArray() }
            stopOutlineEditing()
        }
        editOutlineButton.setOnClickListener {
            if (editingId == null) {
                message("Select a saved field first.")
            } else if (fieldView.gpsBoundary.length() < 3) {
                message("Record the GPS field boundary before adjusting it.")
            } else {
                fieldView.editingBoundary = true
                editOutlineButton.visibility = View.GONE
                saveOutlineButton.visibility = View.VISIBLE
                cancelOutlineButton.visibility = View.VISIBLE
                message("Drag blue points to adjust the boundary. Save when finished.")
            }
        }
        saveOutlineButton.setOnClickListener {
            val id = editingId
            if (id != null && fieldView.gpsBoundary.length() >= 3) {
                val saved = getSharedPreferences("maizemaster_boundaries", MODE_PRIVATE)
                    .edit().putString("${id}_boundary", fieldView.gpsBoundary.toString()).commit()
                if (saved) {
                    stopOutlineEditing()
                    message("GPS field outline saved.")
                } else message("Could not save boundary.")
            }
        }
        cancelOutlineButton.setOnClickListener { restoreOutline() }

        val nameInput = input("Field name", false)
        fieldCard.addView(nameInput)

        val shapes = arrayOf(
            "Rectangle",
            "Oval",
            "Circle"
        )

        val shapeSelector = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                shapes
            )
        }

        fieldCard.addView(text("Field shape", 13f, muted))
        fieldCard.addView(shapeSelector)

        val widthInput = input("Width / diameter (metres)")
        val lengthInput = input("Length (metres)")

        fieldCard.addView(widthInput)
        fieldCard.addView(lengthInput)
        designerPage.addView(fieldCard)

        // PLANTING DETAILS

        val plantingCard = card()
        plantingCard.addView(heading("🚜 Planting details"))

        val rowInput = input("Row spacing (metres)")
        val plantInput = input("Plant spacing (metres)")

        plantingCard.addView(rowInput)
        plantingCard.addView(plantInput)

        plantingCard.addView(
            text("Maize variety", 13f, muted)
        )

        val varieties = arrayOf(
            "White maize",
            "Yellow maize"
        )

        val maizeSelector = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                varieties
            )
        }

        plantingCard.addView(maizeSelector)

        val yieldInput = input("Expected tons per hectare")
        plantingCard.addView(yieldInput)
        plantingPage.addView(plantingCard)

        // HARVEST ESTIMATE

        val estimateCard = card()
        estimateCard.addView(
            heading("📊 Harvest estimate")
        )

        val result = text(
            "Enter field measurements to calculate.",
            15f,
            ink
        )

        val calculateButton = button("Calculate harvest")
        estimateCard.addView(calculateButton)
        estimateCard.addView(result)
        estimatePage.addView(estimateCard)

        // FIELD MANAGEMENT

        val management = card()
        management.addView(
            heading("💾 Field management")
        )

        val editingLabel = text(
            "Creating a new field",
            14f,
            muted
        )

        management.addView(editingLabel)

        val saveButton = button("Save as new field")
        val updateButton = button(
            "Update selected field",
            true
        )
        val newButton = button(
            "Clear / new field",
            true
        )

        management.addView(saveButton)
        management.addView(updateButton)
        management.addView(newButton)
        designerPage.addView(management)

        // SAVED FIELDS

        val savedCard = card()
        savedCard.addView(
            heading("🌾 My saved fields")
        )

        val fieldList = vertical()
        savedCard.addView(fieldList)
        fieldsPage.addView(savedCard)

        // FIELD SHAPE

        shapeSelector.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    fieldView.fieldShape = shapes[position]
                    lengthInput.visibility =
                        if (shapes[position] == "Circle") {
                            View.GONE
                        } else {
                            View.VISIBLE
                        }
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {}
            }

        // READ FIELD

        fun readField(id: String): SavedField? {
            val name = nameInput.text.toString().trim()
            val shape = shapeSelector.selectedItem.toString()
            val w = widthInput.text.toString().toDoubleOrNull()

            val l = if (shape == "Circle") {
                w
            } else {
                lengthInput.text.toString().toDoubleOrNull()
            }

            val row = rowInput.text.toString().toDoubleOrNull()
            val plant = plantInput.text.toString().toDoubleOrNull()
            val yieldRate =
                yieldInput.text.toString().toDoubleOrNull()

            if (
                name.isBlank() ||
                w == null ||
                l == null ||
                row == null ||
                plant == null ||
                yieldRate == null ||
                !w.isFinite() ||
                !l.isFinite() ||
                !row.isFinite() ||
                !plant.isFinite() ||
                !yieldRate.isFinite() ||
                w <= 0 ||
                l <= 0 ||
                row <= 0 ||
                plant <= 0 ||
                yieldRate < 0 ||
                w > Float.MAX_VALUE ||
                l > Float.MAX_VALUE ||
                row > Float.MAX_VALUE
            ) {
                message(
                    "Enter a field name and valid measurements."
                )
                return null
            }

            return SavedField(
                id = id,
                name = name,
                shape = shape,
                width = w,
                length = l,
                rowSpacing = row,
                plantSpacing = plant,
                maizeVariety =
                    maizeSelector.selectedItem.toString(),
                expectedYield = yieldRate
            )
        }

        // CALCULATE HARVEST

        fun showCalculation(field: SavedField) {
            val hectares = field.hectares()
            val tons = field.estimatedTons()

            val plants = HarvestCalculator.estimatePlants(
                hectares,
                field.rowSpacing,
                field.plantSpacing
            )

            fieldView.fieldWidth = field.width.toFloat()
            fieldView.fieldLength = field.length.toFloat()
            fieldView.rowSpacing = field.rowSpacing.toFloat()
            fieldView.fieldShape = field.shape
            fieldView.gpsBoundary = try {
                JSONArray(getSharedPreferences("maizemaster_boundaries", MODE_PRIVATE)
                    .getString("${field.id}_boundary", "[]"))
            } catch (_: Exception) { JSONArray() }
            fieldView.growthDays = daysSince(field.plantingDate)?.toInt()?.coerceAtLeast(0) ?: 0

            result.text =
                "🌽 ${field.maizeVariety}\n\n" +
                "📏 Area: ${fmt(hectares)} hectares\n" +
                "🌱 Plant positions: ${
                    String.format(Locale.US, "%,d", plants)
                }\n" +
                "🚜 Estimated harvest: ${fmt(tons)} tons"
        }

        // EDIT FIELD

        fun openField(field: SavedField) {
            stopOutlineEditing()
            editingId = field.id

            nameInput.setText(field.name)
            widthInput.setText(field.width.toString())
            lengthInput.setText(field.length.toString())
            rowInput.setText(field.rowSpacing.toString())
            plantInput.setText(field.plantSpacing.toString())
            yieldInput.setText(field.expectedYield.toString())

            shapeSelector.setSelection(
                shapes.indexOf(field.shape).coerceAtLeast(0)
            )

            maizeSelector.setSelection(
                varieties.indexOf(field.maizeVariety)
                    .coerceAtLeast(0)
            )

            editingLabel.text = "Editing: ${field.name}"
            showCalculation(field)
            navigateTo(1)
        }

        // RECORD HARVEST

        fun recordHarvest(field: SavedField) {
            val form = vertical().apply {
                setPadding(dp(22), dp(12), dp(22), dp(8))
            }

            val hectaresInput =
                input("Total hectares harvested")

            val tonsInput =
                input("Total tons harvested")

            hectaresInput.setText(
                field.harvestedHectares.toString()
            )

            tonsInput.setText(
                field.harvestedTons.toString()
            )

            form.addView(
                text(
                    "Enter cumulative harvest totals.",
                    14f,
                    ink
                )
            )

            form.addView(hectaresInput)
            form.addView(tonsInput)

            val dialog = AlertDialog.Builder(this)
                .setTitle("Record harvest: ${field.name}")
                .setView(form)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create()

            dialog.setOnShowListener {
                dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
                ).setOnClickListener {
                    val hectares = hectaresInput.text
                        .toString().toDoubleOrNull()

                    val tons = tonsInput.text
                        .toString().toDoubleOrNull()

                    if (
                        hectares == null ||
                        tons == null ||
                        !hectares.isFinite() ||
                        !tons.isFinite() ||
                        hectares < 0 ||
                        tons < 0 ||
                        hectares > field.hectares() + 0.000001
                    ) {
                        message(
                            "Enter valid harvest totals."
                        )
                    } else {
                        field.harvestedHectares = hectares
                        field.harvestedTons = tons

                        if (storage.updateField(field)) {
                            dialog.dismiss()
                            refreshList()
                        } else {
                            message(
                                "Could not save harvest."
                            )
                        }
                    }
                }
            }

            dialog.show()
        }

        // GROWTH TRACKER

        fun editGrowth(field: SavedField) {
            val form = vertical().apply {
                setPadding(dp(22), dp(12), dp(22), dp(8))
            }

            form.addView(
                text("Planting date", 14f, forest, true)
            )

            var selectedDate = field.plantingDate

            val dateButton = button(
                if (selectedDate.isBlank()) {
                    "Select planting date"
                } else {
                    "Planted: $selectedDate"
                },
                true
            )

            dateButton.setOnClickListener {
                pickDate(selectedDate) { date ->
                    selectedDate = date
                    dateButton.text = "Planted: $date"
                }
            }

            form.addView(dateButton)

            val maturityInput =
                input("Expected days to maturity")

            if (field.maturityDays > 0) {
                maturityInput.setText(
                    field.maturityDays.toString()
                )
            }

            form.addView(maturityInput)

            form.addView(
                text(
                    "Observed maize growth stage",
                    14f,
                    forest,
                    true
                )
            )

            val stages = growthStages()

            val stageSelector = Spinner(this).apply {
                adapter = ArrayAdapter(
                    this@MainActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    stages
                )

                setSelection(
                    stages.indexOf(field.growthStage)
                        .coerceAtLeast(0)
                )
            }

            form.addView(stageSelector)

            form.addView(
                text(
                    "The app estimates growth automatically. " +
                    "You can record the observed stage separately.",
                    12f,
                    muted
                )
            )

            val notesInput = input(
                "Growth observations / notes",
                false
            ).apply {
                setSingleLine(false)
                minLines = 3
                inputType =
                    InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE
                setText(field.growthNotes)
            }

            form.addView(notesInput)

            val dialogScroll = ScrollView(this)
            dialogScroll.addView(form)

            val dialog = AlertDialog.Builder(this)
                .setTitle("🌽 Growth tracker: ${field.name}")
                .setView(dialogScroll)
                .setPositiveButton("Save growth", null)
                .setNegativeButton("Cancel", null)
                .create()

            dialog.setOnShowListener {
                dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
                ).setOnClickListener {
                    val maturityText =
                        maturityInput.text.toString().trim()

                    val maturity =
                        if (maturityText.isBlank()) {
                            0
                        } else {
                            maturityText.toIntOrNull()
                        }

                    val age =
                        if (selectedDate.isBlank()) {
                            null
                        } else {
                            daysSince(selectedDate)
                        }

                    if (
                        maturity == null ||
                        maturity < 0 ||
                        maturity > 1000
                    ) {
                        message(
                            "Enter valid maturity days (0-1000)."
                        )
                    } else if (
                        selectedDate.isNotBlank() &&
                        (age == null || age < 0)
                    ) {
                        message(
                            "Choose a valid planting date."
                        )
                    } else {
                        field.plantingDate = selectedDate
                        field.maturityDays = maturity
                        field.growthStage =
                            stageSelector.selectedItem.toString()
                        field.growthNotes =
                            notesInput.text.toString().trim()
                        field.growthObservedDate =
                            dateFormat().format(
                                System.currentTimeMillis()
                            )

                        if (storage.updateField(field)) {
                            dialog.dismiss()
                            refreshList()
                            message(
                                "Growth information saved."
                            )
                        } else {
                            message(
                                "Could not save growth information."
                            )
                        }
                    }
                }
            }

            dialog.show()
        }

        // REFRESH SAVED FIELDS

        refreshList = {
            fieldList.removeAllViews()

            val fields = storage.loadFields()

            overviewText.text =
                "🌱 Fields: ${fields.size}\n" +
                "📏 Total area: ${
                    fmt(fields.sumOf { it.hectares() })
                } ha\n" +
                "🌽 Expected yield: ${
                    fmt(fields.sumOf { it.estimatedTons() })
                } t\n" +
                "🚜 Harvested: ${
                    fmt(fields.sumOf { it.harvestedTons })
                } t"

            if (fields.isEmpty()) {
                fieldList.addView(
                    text(
                        "No saved fields yet. Create one above.",
                        14f,
                        muted
                    )
                )
            }

            for (field in fields) {

                val item = vertical().apply {
                    setPadding(
                        dp(14), dp(15), dp(14), dp(14)
                    )
                    background = bg(pale, 14)
                    layoutParams =
                        LinearLayout.LayoutParams(-1, -2).apply {
                            bottomMargin = dp(12)
                        }
                }

                item.addView(
                    text(
                        "🌽 ${field.name}",
                        18f,
                        forest,
                        true
                    )
                )

                item.addView(
                    text(
                        "${field.maizeVariety} • ${field.shape}\n" +
                        "Area: ${fmt(field.hectares())} ha\n" +
                        "Expected: ${fmt(field.estimatedTons())} t\n" +
                        "Harvested: ${fmt(field.harvestedTons)} t\n" +
                        "Remaining: ${fmt(field.remainingTons())} t\n" +
                        "Area harvested: ${
                            fmt(field.harvestProgress())
                        }%",
                        14f,
                        ink
                    ).apply {
                        setPadding(0, dp(8), 0, dp(12))
                    }
                )

                item.addView(
                    ProgressBar(
                        this,
                        null,
                        android.R.attr.progressBarStyleHorizontal
                    ).apply {
                        max = 100
                        progress = field.harvestProgress()
                            .toInt().coerceIn(0, 100)
                        progressTintList =
                            ColorStateList.valueOf(green)
                        layoutParams =
                            LinearLayout.LayoutParams(
                                -1, dp(9)
                            ).apply {
                                bottomMargin = dp(12)
                            }
                    }
                )

                // AUTOMATIC GROWTH CALCULATION

                val automaticGrowth =
                    if (
                        field.plantingDate.isNotBlank() &&
                        field.maturityDays > 0
                    ) {
                        MaizeGrowthCalculator.calculate(
                            field.plantingDate,
                            field.maturityDays
                        )
                    } else {
                        null
                    }

                val stage = automaticGrowth?.stage
                    ?: field.growthStage

                val progress =
                    automaticGrowth?.progressPercent
                        ?: growthProgress(stage)

                // GROWTH CARD

                val growthCard = vertical().apply {
                    setPadding(
                        dp(12), dp(12), dp(12), dp(12)
                    )
                    background = bg(Color.WHITE, 12)
                    layoutParams =
                        LinearLayout.LayoutParams(-1, -2).apply {
                            bottomMargin = dp(12)
                        }
                }

                growthCard.addView(
                    text(
                        "🌱 Maize growth",
                        16f,
                        forest,
                        true
                    )
                )

                // NEW MAIZE GROWTH ILLUSTRATION

                val maizeIllustration =
                    MaizeGrowthView(this).apply {
                        progressPercent = progress

                        layoutParams =
                            LinearLayout.LayoutParams(
                                -1,
                                dp(300)
                            ).apply {
                                topMargin = dp(12)
                                bottomMargin = dp(12)
                            }
                    }

                growthCard.addView(maizeIllustration)

                // GROWTH STAGE

                if (
                    stage.isNotBlank() &&
                    stage != "Not planted"
                ) {
                    growthCard.addView(
                        text(
                            "🌽 $stage",
                            17f,
                            green,
                            true
                        )
                    )

                    growthCard.addView(
                        ProgressBar(
                            this,
                            null,
                            android.R.attr.progressBarStyleHorizontal
                        ).apply {
                            max = 100
                            this.progress = progress
                            progressTintList =
                                ColorStateList.valueOf(green)
                            layoutParams =
                                LinearLayout.LayoutParams(
                                    -1,
                                    dp(10)
                                ).apply {
                                    topMargin = dp(10)
                                    bottomMargin = dp(10)
                                }
                        }
                    )

                    growthCard.addView(
                        text(
                            "Estimated growth: $progress%",
                            14f,
                            forest,
                            true
                        )
                    )

                    if (automaticGrowth != null) {
                        growthCard.addView(
                            text(
                                automaticGrowth.advice,
                                13f,
                                ink
                            ).apply {
                                setPadding(
                                    0, dp(10), 0, dp(8)
                                )
                            }
                        )
                    }
                } else {
                    growthCard.addView(
                        text(
                            "Enter a planting date and maturity " +
                            "duration to start automatic tracking.",
                            13f,
                            muted
                        )
                    )
                }

                // PLANTING INFORMATION

                if (field.plantingDate.isNotBlank()) {
                    val age = daysSince(
                        field.plantingDate
                    )

                    growthCard.addView(
                        text(
                            "Planted: ${field.plantingDate}" +
                            if (age != null && age >= 0) {
                                "\nDays since planting: $age"
                            } else {
                                ""
                            },
                            13f,
                            ink
                        )
                    )

                    if (field.maturityDays > 0) {
                        val remaining =
                            automaticGrowth
                                ?.daysUntilMaturity
                                ?.toLong()
                                ?: if (age != null) {
                                    (
                                        field.maturityDays.toLong() -
                                        age
                                    ).coerceAtLeast(0)
                                } else {
                                    null
                                }

                        growthCard.addView(
                            text(
                                "Expected maturity: ${
                                    field.maturityDays
                                } days" +
                                if (remaining != null) {
                                    "\nApproximate days remaining: $remaining"
                                } else {
                                    ""
                                },
                                13f,
                                ink
                            )
                        )
                    }
                }

                // OBSERVED STAGE

                if (
                    automaticGrowth != null &&
                    field.growthStage.isNotBlank() &&
                    field.growthStage != "Not planted"
                ) {
                    growthCard.addView(
                        text(
                            "Observed stage: ${field.growthStage}",
                            12f,
                            muted
                        ).apply {
                            setPadding(0, dp(8), 0, 0)
                        }
                    )
                }

                if (field.growthNotes.isNotBlank()) {
                    growthCard.addView(
                        text(
                            "Notes: ${field.growthNotes}",
                            13f,
                            ink
                        ).apply {
                            setPadding(0, dp(8), 0, 0)
                        }
                    )
                }

                if (field.growthObservedDate.isNotBlank()) {
                    growthCard.addView(
                        text(
                            "Last observation: ${
                                field.growthObservedDate
                            }",
                            12f,
                            muted
                        ).apply {
                            setPadding(0, dp(8), 0, 0)
                        }
                    )
                }

                item.addView(growthCard)

                // FIELD ACTIONS

                item.addView(
                    button("🌱 Update maize growth").apply {
                        setOnClickListener {
                            editGrowth(field)
                        }
                    }
                )

                item.addView(
                    button("🚜 Record harvest").apply {
                        setOnClickListener {
                            recordHarvest(field)
                        }
                    }
                )

                item.addView(
                    button("✏️ Edit field", true).apply {
                        setOnClickListener {
                            openField(field)
                        }
                    }
                )

                item.addView(
                    button("🗑️ Delete field", true).apply {
                        setOnClickListener {
                            AlertDialog.Builder(this@MainActivity)
                                .setTitle("Delete field?")
                                .setMessage(
                                    "Delete ${field.name} and its saved records?"
                                )
                                .setPositiveButton(
                                    "Delete"
                                ) { _, _ ->
                                    if (
                                        storage.deleteField(
                                            field.id
                                        )
                                    ) {
                                        if (
                                            editingId == field.id
                                        ) {
                                            editingId = null
                                            editingLabel.text =
                                                "Creating a new field"
                                        }
                                        refreshList()
                                    } else {
                                        message(
                                            "Could not delete field."
                                        )
                                    }
                                }
                                .setNegativeButton(
                                    "Cancel",
                                    null
                                )
                                .show()
                        }
                    }
                )

                fieldList.addView(item)
            }
        }

        // CALCULATE BUTTON

        calculateButton.setOnClickListener {
            val field = readField(
                editingId ?: UUID.randomUUID().toString()
            )

            if (field != null) {
                showCalculation(field)
            }
        }

        // SAVE NEW FIELD

        saveButton.setOnClickListener {
            val field = readField(
                UUID.randomUUID().toString()
            )

            if (field != null) {
                if (storage.addField(field)) {
                    editingId = field.id
                    editingLabel.text =
                        "Editing: ${field.name}"
                    showCalculation(field)
                    refreshList()
                    message(
                        "Field saved successfully."
                    )
                } else {
                    message(
                        "Could not save field."
                    )
                }
            }
        }

        // UPDATE FIELD

        updateButton.setOnClickListener {
            val id = editingId

            if (id == null) {
                message(
                    "Select a saved field to update."
                )
            } else {
                val original = storage.loadFields()
                    .firstOrNull { it.id == id }

                val updated = readField(id)

                if (original == null) {
                    message("Saved field not found.")
                } else if (updated != null) {

                    // PRESERVE ALL SAVED RECORDS

                    updated.harvestedHectares =
                        original.harvestedHectares
                    updated.harvestedTons =
                        original.harvestedTons
                    updated.plantingDate =
                        original.plantingDate
                    updated.maturityDays =
                        original.maturityDays
                    updated.growthStage =
                        original.growthStage
                    updated.growthNotes =
                        original.growthNotes
                    updated.growthObservedDate =
                        original.growthObservedDate

                    if (
                        updated.harvestedHectares >
                        updated.hectares() + 0.000001
                    ) {
                        message(
                            "New field area is smaller than " +
                            "the recorded harvested area."
                        )
                    } else if (
                        storage.updateField(updated)
                    ) {
                        showCalculation(updated)
                        refreshList()
                        message(
                            "Field updated successfully."
                        )
                    } else {
                        message(
                            "Could not update field."
                        )
                    }
                }
            }
        }

        // CLEAR / NEW FIELD

        newButton.setOnClickListener {
            stopOutlineEditing()
            fieldView.gpsBoundary = JSONArray()
            editingId = null

            nameInput.setText("")
            widthInput.setText("")
            lengthInput.setText("")
            rowInput.setText("")
            plantInput.setText("")
            yieldInput.setText("")

            shapeSelector.setSelection(0)
            maizeSelector.setSelection(0)

            editingLabel.text =
                "Creating a new field"

            result.text =
                "Enter field measurements to calculate."

            fieldView.fieldShape = "Rectangle"
            fieldView.fieldWidth = 200f
            fieldView.fieldLength = 300f
            fieldView.rowSpacing = 0.75f

            message(
                "Ready to create a new field."
            )

            navigateTo(1)
        }

        // INITIAL DASHBOARD

        refreshList()
        showPage(0)
    }
}
