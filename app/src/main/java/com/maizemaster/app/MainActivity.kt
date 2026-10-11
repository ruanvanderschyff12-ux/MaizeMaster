
package com.maizemaster.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.util.Locale
import java.util.UUID

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

    private fun bg(color: Int, radius: Int = 18): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun text(
        value: String,
        size: Float = 15f,
        color: Int = ink,
        bold: Boolean = false
    ): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(null, Typeface.BOLD)
    }

    private fun vertical(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

    private fun card(): LinearLayout = vertical().apply {
        setPadding(dp(18), dp(18), dp(18), dp(18))
        background = bg(Color.WHITE)
        elevation = dp(2).toFloat()
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(14)
        }
    }

    private fun heading(label: String): TextView =
        text(label, 19f, forest, true).apply {
            setPadding(0, 0, 0, dp(13))
        }

    private fun input(
        hintValue: String,
        numeric: Boolean = true
    ): EditText = EditText(this).apply {
        hint = hintValue
        inputType = if (numeric)
            InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL
        else InputType.TYPE_CLASS_TEXT

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
        label: String,
        secondary: Boolean = false
    ): Button = Button(this).apply {
        text = label
        textSize = 13f
        isAllCaps = false
        setTextColor(if (secondary) forest else Color.WHITE)
        background = bg(if (secondary) pale else green, 12)
        layoutParams = LinearLayout.LayoutParams(-1, dp(49)).apply {
            bottomMargin = dp(8)
        }
    }

    private fun metric(
        label: String,
        value: String
    ): LinearLayout = vertical().apply {
        setPadding(dp(13), dp(12), dp(13), dp(12))
        background = bg(pale, 14)
        addView(text(label, 12f, muted))
        addView(text(value, 21f, forest, true))
    }

    private fun fmt(value: Double): String =
        String.format(Locale.US, "%.2f", value)

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
            setPadding(dp(16), dp(16), dp(16), dp(28))
        }

        scroll.addView(layout)
        setContentView(scroll)

        // GREEN HEADER

        val header = vertical().apply {
            setPadding(dp(22), dp(23), dp(22), dp(23))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(forest, green)
            ).apply {
                cornerRadius = dp(22).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(15)
            }
        }

        header.addView(
            text("🌽  MaizeMaster", 27f, Color.WHITE, true)
        )

        header.addView(
            text(
                "Your maize farm at a glance",
                14f,
                Color.WHITE
            ).apply {
                setPadding(0, dp(7), 0, 0)
            }
        )

        layout.addView(header)

        // FARM OVERVIEW

        val overview = card()
        overview.addView(heading("Farm overview"))

        val overviewRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val areaMetric = metric("Total area", "0.00 ha")
        val yieldMetric = metric("Expected yield", "0.00 t")
        val harvestMetric = metric("Harvested", "0.00 t")

        listOf(
            areaMetric,
            yieldMetric,
            harvestMetric
        ).forEachIndexed { index, item ->
            overviewRow.addView(
                item,
                LinearLayout.LayoutParams(0, -2, 1f).apply {
                    if (index < 2) rightMargin = dp(5)
                }
            )
        }

        overview.addView(overviewRow)

        val fieldCount = text("0 saved fields", 13f, muted).apply {
            setPadding(0, dp(12), 0, 0)
        }

        overview.addView(fieldCount)
        layout.addView(overview)

        // WEATHER

        val weatherCard = card()
        weatherCard.addView(
            heading("🌦️ Weather & field risks")
        )

        weatherCard.addView(
            text(
                "Check forecasts and possible maize-growing risks for your fields.",
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

        layout.addView(weatherCard)

        // FIELD DESIGNER

        val fieldCard = card()
        fieldCard.addView(heading("🌱 Field designer"))

        fieldCard.addView(
            text(
                "Shape and maize rows preview",
                13f,
                muted
            ).apply {
                setPadding(0, 0, 0, dp(10))
            }
        )

        val fieldView = FieldDesignerView(this).apply {
            background = bg(pale, 14)
            layoutParams = LinearLayout.LayoutParams(
                -1,
                dp(250)
            ).apply {
                bottomMargin = dp(12)
            }
        }

        fieldCard.addView(fieldView)

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
        layout.addView(fieldCard)

        // PLANTING DETAILS

        val plantingCard = card()
        plantingCard.addView(
            heading("🚜 Planting details")
        )

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

        val yieldInput = input(
            "Expected tons per hectare"
        )

        plantingCard.addView(yieldInput)
        layout.addView(plantingCard)

        // HARVEST ESTIMATE

        val estimateCard = card()
        estimateCard.addView(
            heading("📊 Harvest estimate")
        )

        val result = text(
            "Enter your field measurements to calculate.",
            15f,
            ink
        ).apply {
            setPadding(0, dp(10), 0, dp(12))
        }

        val calculateButton = button(
            "Calculate harvest"
        )

        estimateCard.addView(calculateButton)
        estimateCard.addView(result)
        layout.addView(estimateCard)

        // FIELD MANAGEMENT

        val management = card()
        management.addView(
            heading("💾 Field management")
        )

        val editingLabel = text(
            "Creating a new field",
            14f,
            muted
        ).apply {
            setPadding(0, 0, 0, dp(12))
        }

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
        layout.addView(management)

        // SAVED FIELDS

        val savedCard = card()
        savedCard.addView(
            heading("🌾 My saved fields")
        )

        val fieldList = vertical()
        savedCard.addView(fieldList)
        layout.addView(savedCard)

        // FIELD SHAPE SELECTION

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
                        if (shapes[position] == "Circle")
                            View.GONE
                        else View.VISIBLE
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {}
            }

        // READ FIELD INPUTS

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
            val yieldRate = yieldInput.text.toString().toDoubleOrNull()

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
                Toast.makeText(
                    this,
                    "Enter a field name and valid measurements.",
                    Toast.LENGTH_LONG
                ).show()
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
                maizeVariety = maizeSelector.selectedItem.toString(),
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

            result.text =
                "🌽 ${field.maizeVariety}\n\n" +
                "📏 Area: ${fmt(hectares)} hectares\n" +
                "🌱 Plant positions: ${
                    String.format(Locale.US, "%,d", plants)
                }\n" +
                "🚜 Estimated harvest: ${fmt(tons)} tons"
        }

        // OPEN SAVED FIELD

        fun openField(field: SavedField) {
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
                varieties.indexOf(
                    field.maizeVariety
                ).coerceAtLeast(0)
            )

            editingLabel.text = "Editing: ${field.name}"

            showCalculation(field)
            scroll.smoothScrollTo(0, 0)
        }

        // RECORD HARVEST

        fun recordHarvest(field: SavedField) {
            val dialogLayout = vertical().apply {
                setPadding(
                    dp(22),
                    dp(12),
                    dp(22),
                    dp(8)
                )
            }

            val hectaresInput = input(
                "Total hectares harvested"
            )

            val tonsInput = input(
                "Total tons harvested"
            )

            hectaresInput.setText(
                field.harvestedHectares.toString()
            )

            tonsInput.setText(
                field.harvestedTons.toString()
            )

            dialogLayout.addView(
                text(
                    "Enter cumulative harvest totals for ${field.name}.",
                    14f,
                    ink
                )
            )

            dialogLayout.addView(hectaresInput)
            dialogLayout.addView(tonsInput)

            val dialog = AlertDialog.Builder(this)
                .setTitle("Record harvest")
                .setView(dialogLayout)
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
                        Toast.makeText(
                            this,
                            "Enter valid totals within the field area.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        field.harvestedHectares = hectares
                        field.harvestedTons = tons

                        if (storage.updateField(field)) {
                            dialog.dismiss()
                            refreshList()
                        } else {
                            Toast.makeText(
                                this,
                                "Could not save harvest.",
                                Toast.LENGTH_SHORT
                            ).show()
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

            (areaMetric.getChildAt(1) as TextView).text =
                "${fmt(fields.sumOf { it.hectares() })} ha"

            (yieldMetric.getChildAt(1) as TextView).text =
                "${fmt(fields.sumOf { it.estimatedTons() })} t"

            (harvestMetric.getChildAt(1) as TextView).text =
                "${fmt(fields.sumOf { it.harvestedTons })} t"

            fieldCount.text =
                "${fields.size} saved field${
                    if (fields.size == 1) "" else "s"
                }"

            if (fields.isEmpty()) {
                fieldList.addView(
                    text(
                        "No saved fields yet. Create your first field above.",
                        14f,
                        muted
                    )
                )
            }

            for (field in fields) {
                val item = vertical().apply {
                    setPadding(
                        dp(14),
                        dp(15),
                        dp(14),
                        dp(14)
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
                        "${field.maizeVariety}  •  ${field.shape}\n" +
                        "Area: ${fmt(field.hectares())} ha  •  " +
                        "Expected: ${fmt(field.estimatedTons())} t\n" +
                        "Harvested: ${fmt(field.harvestedTons)} t  •  " +
                        "Remaining: ${fmt(field.remainingTons())} t\n" +
                        "Area harvested: ${fmt(field.harvestProgress())}%",
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
                                -1,
                                dp(9)
                            ).apply {
                                bottomMargin = dp(12)
                            }
                    }
                )

                // EDIT FIELD

                item.addView(
                    button("Edit field").apply {
                        setOnClickListener {
                            openField(field)
                        }
                    }
                )

                // FIELD WEATHER

                item.addView(
                    button(
                        "Check field weather",
                        true
                    ).apply {
                        setOnClickListener {
                            startActivity(
                                Intent(
                                    this@MainActivity,
                                    WeatherActivity::class.java
                                ).putExtra(
                                    "field_id",
                                    field.id
                                )
                            )
                        }
                    }
                )

                // HARVEST

                item.addView(
                    button("Record harvest").apply {
                        setOnClickListener {
                            recordHarvest(field)
                        }
                    }
                )

                // DUPLICATE FIELD

                item.addView(
                    button(
                        "Duplicate field",
                        true
                    ).apply {
                        setOnClickListener {
                            val copy = field.copy(
                                id = UUID.randomUUID().toString(),
                                name = "${field.name} Copy",
                                harvestedHectares = 0.0,
                                harvestedTons = 0.0
                            )

                            if (storage.addField(copy)) {
                                refreshList()
                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Could not duplicate field.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )

                // DELETE FIELD

                item.addView(
                    button(
                        "Delete field",
                        true
                    ).apply {
                        setOnClickListener {
                            AlertDialog.Builder(this@MainActivity)
                                .setTitle("Delete field?")
                                .setMessage(
                                    "Delete ${field.name} and its harvest totals?"
                                )
                                .setPositiveButton("Delete") { _, _ ->
                                    if (storage.deleteField(field.id)) {
                                        if (editingId == field.id) {
                                            editingId = null
                                            editingLabel.text =
                                                "Creating a new field"
                                        }
                                        refreshList()
                                    } else {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Could not delete field.",
                                            Toast.LENGTH_SHORT
                                        ).show()
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
            readField(
                editingId ?: UUID.randomUUID().toString()
            )?.let {
                showCalculation(it)
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

                    Toast.makeText(
                        this,
                        "Field saved successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Could not save field.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // UPDATE EXISTING FIELD

        updateButton.setOnClickListener {
            val id = editingId

            if (id == null) {
                Toast.makeText(
                    this,
                    "Open a saved field first.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val updated = readField(id)
                val previous = storage.loadFields()
                    .firstOrNull { it.id == id }

                if (updated != null && previous != null) {
                    updated.harvestedHectares =
                        previous.harvestedHectares

                    updated.harvestedTons =
                        previous.harvestedTons

                    if (
                        updated.harvestedHectares >
                        updated.hectares() + 0.000001
                    ) {
                        Toast.makeText(
                            this,
                            "Field size is smaller than harvested area.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else if (storage.updateField(updated)) {
                        editingLabel.text =
                            "Editing: ${updated.name}"

                        showCalculation(updated)
                        refreshList()

                        Toast.makeText(
                            this,
                            "Field updated!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Could not update field.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        // CLEAR / NEW FIELD

        newButton.setOnClickListener {
            editingId = null

            listOf(
                nameInput,
                widthInput,
                lengthInput,
                rowInput,
                plantInput,
                yieldInput
            ).forEach {
                it.text.clear()
            }

            shapeSelector.setSelection(0)
            maizeSelector.setSelection(0)

            fieldView.fieldWidth = 200f
            fieldView.fieldLength = 300f
            fieldView.rowSpacing = 0.75f
            fieldView.fieldShape = "Rectangle"

            editingLabel.text = "Creating a new field"
            result.text =
                "Enter your field measurements to calculate."

            scroll.smoothScrollTo(0, 0)
        }

        refreshList()
    }
}
