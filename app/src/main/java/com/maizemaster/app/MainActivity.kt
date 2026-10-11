
package com.maizemaster.app

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.util.UUID

class MainActivity : Activity() {

    private val green = Color.rgb(27, 94, 50)
    private val lightGreen = Color.rgb(232, 245, 233)
    private val backgroundColor = Color.rgb(246, 249, 245)

    private lateinit var storage: FieldStorage
    private var editingId: String? = null

    private fun rounded(
        color: Int,
        radius: Float = 22f
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        storage = FieldStorage(this)

        window.statusBarColor = green
        window.navigationBarColor = Color.BLACK

        val scroll = ScrollView(this).apply {
            setBackgroundColor(backgroundColor)
            isFillViewport = true
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 40)
        }

        fun title(value: String): TextView {
            return TextView(this).apply {
                text = value
                textSize = 21f
                setTextColor(green)
                setTypeface(null, Typeface.BOLD)
                setPadding(8, 16, 8, 14)
            }
        }

        fun input(
            hintValue: String,
            numeric: Boolean = true
        ): EditText {
            return EditText(this).apply {
                hint = hintValue
                inputType = if (numeric) {
                    InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL or
                    InputType.TYPE_NUMBER_FLAG_SIGNED
                } else {
                    InputType.TYPE_CLASS_TEXT
                }
                textSize = 16f
                setSingleLine(true)
                setPadding(22, 18, 22, 18)
                background = rounded(Color.WHITE, 18f)

                val params = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 6, 0, 12)
                layoutParams = params
            }
        }

        fun section(): LinearLayout {
            return LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 20, 20, 20)
                background = rounded(lightGreen)

                val params = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 8, 0, 18)
                layoutParams = params
            }
        }

        fun button(label: String): Button {
            return Button(this).apply {
                text = label
                textSize = 15f
                setTextColor(Color.WHITE)
                background = rounded(green, 20f)
                setPadding(12, 16, 12, 16)

                val params = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 5, 0, 7)
                layoutParams = params
            }
        }

        fun message(value: String): TextView {
            return TextView(this).apply {
                text = value
                textSize = 16f
                setTextColor(green)
                setPadding(10, 12, 10, 12)
            }
        }

        // HEADER

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
            background = rounded(green, 30f)
        }

        header.addView(TextView(this).apply {
            text = "🌽 MaizeMaster"
            textSize = 29f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
        })

        header.addView(TextView(this).apply {
            text = "Your Smart Maize Farming Companion"
            textSize = 15f
            setTextColor(Color.WHITE)
        })

        layout.addView(header)

        // FIELD DESIGNER

        val fieldSection = section()
        fieldSection.addView(title("🌽 My Maize Field"))

        val nameInput = input("Field name", false)
        fieldSection.addView(nameInput)

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

        fieldSection.addView(shapeSelector)

        val fieldView = FieldDesignerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                500
            )
        }

        fieldSection.addView(fieldView)

        val widthInput = input("Field width (metres)")
        val lengthInput = input("Field length (metres)")

        fieldSection.addView(widthInput)
        fieldSection.addView(lengthInput)

        layout.addView(fieldSection)

        // PLANTING DETAILS

        val plantingSection = section()
        plantingSection.addView(title("🚜 Planting Details"))

        val rowInput = input("Row spacing (metres)")
        val plantInput = input("Plant spacing (metres)")

        plantingSection.addView(rowInput)
        plantingSection.addView(plantInput)

        plantingSection.addView(title("🌽 Maize Variety"))

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

        plantingSection.addView(maizeSelector)

        val yieldInput = input("Expected tons per hectare")
        plantingSection.addView(yieldInput)

        layout.addView(plantingSection)

        // RESULTS

        val resultSection = section()
        resultSection.addView(title("📊 Harvest Estimate"))

        val result = message(
            "Enter field measurements to calculate."
        )

        val calculateButton = button(
            "CALCULATE MY HARVEST"
        )

        resultSection.addView(calculateButton)
        resultSection.addView(result)

        layout.addView(resultSection)

        // SAVE AND EDIT

        val saveSection = section()
        saveSection.addView(title("💾 Field Management"))

        val editingLabel = message(
            "Creating a new field"
        )

        saveSection.addView(editingLabel)

        val saveButton = button("SAVE AS NEW FIELD")
        val updateButton = button("UPDATE SELECTED FIELD")
        val newButton = button("CLEAR / NEW FIELD")

        saveSection.addView(saveButton)
        saveSection.addView(updateButton)
        saveSection.addView(newButton)

        layout.addView(saveSection)

        // SAVED FIELDS

        val listSection = section()
        listSection.addView(title("🌾 My Saved Fields"))

        val fieldList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        listSection.addView(fieldList)
        layout.addView(listSection)

        // FIELD SHAPE SELECTION

        shapeSelector.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val shape = shapes[position]

                    fieldView.fieldShape = shape

                    lengthInput.visibility =
                        if (shape == "Circle") {
                            View.GONE
                        } else {
                            View.VISIBLE
                        }
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {}
            }

        // VALIDATE AND READ INPUTS

        fun readField(id: String): SavedField? {
            val name = nameInput.text.toString().trim()
            val shape = shapeSelector.selectedItem.toString()

            val w = widthInput.text.toString()
                .toDoubleOrNull()

            val l = if (shape == "Circle") {
                w
            } else {
                lengthInput.text.toString()
                    .toDoubleOrNull()
            }

            val row = rowInput.text.toString()
                .toDoubleOrNull()

            val plant = plantInput.text.toString()
                .toDoubleOrNull()

            val yieldRate = yieldInput.text.toString()
                .toDoubleOrNull()

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
                w <= 0.0 ||
                l <= 0.0 ||
                row <= 0.0 ||
                plant <= 0.0 ||
                yieldRate < 0.0 ||
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
                maizeVariety =
                    maizeSelector.selectedItem.toString(),
                expectedYield = yieldRate
            )
        }

        // CALCULATE AND UPDATE DRAWING

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
            fieldView.rowSpacing =
                field.rowSpacing.toFloat()
            fieldView.fieldShape = field.shape

            result.text = """
                🌽 ${field.maizeVariety}

                📏 Field: %.2f hectares

                🌱 Plant positions: %,d

                🚜 Estimated harvest: %.2f tons
            """.trimIndent().format(
                hectares,
                plants,
                tons
            )
        }

        // OPEN A SAVED FIELD FOR EDITING

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

            editingLabel.text =
                "Editing: ${field.name}"

            showCalculation(field)
            scroll.smoothScrollTo(0, 0)
        }

        // HARVEST PROGRESS DIALOG

        fun recordHarvest(field: SavedField) {
            val dialogLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(35, 20, 35, 10)
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
                message(
                    "Enter cumulative harvest totals for ${field.name}."
                )
            )

            dialogLayout.addView(hectaresInput)
            dialogLayout.addView(tonsInput)

            val dialog = AlertDialog.Builder(this)
                .setTitle("🚜 Record Harvest")
                .setView(dialogLayout)
                .setPositiveButton("SAVE", null)
                .setNegativeButton("CANCEL", null)
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
                        hectares < 0.0 ||
                        tons < 0.0 ||
                        hectares > field.hectares() + 0.000001
                    ) {
                        Toast.makeText(
                            this,
                            "Enter valid totals. Harvested hectares cannot exceed field size.",
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

        // SAVED FIELD LIST

        // Refresh function is assigned below so it can
        // also be called after saving harvest data.
        refreshList = {
            fieldList.removeAllViews()

            val fields = storage.loadFields()

            if (fields.isEmpty()) {
                fieldList.addView(
                    message("No fields saved yet.")
                )
            }

            for (field in fields) {
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16, 18, 16, 18)
                    background = rounded(Color.WHITE, 18f)

                    val params = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(0, 8, 0, 14)
                    layoutParams = params
                }

                card.addView(title("🌽 ${field.name}"))

                card.addView(
                    message(
                        """
                        Shape: ${field.shape}
                        Variety: ${field.maizeVariety}
                        Area: %.2f hectares
                        Estimated harvest: %.2f tons
                        Harvested: %.2f tons
                        Remaining estimate: %.2f tons
                        Area harvested: %.1f%%
                        """.trimIndent().format(
                            field.hectares(),
                            field.estimatedTons(),
                            field.harvestedTons,
                            field.remainingTons(),
                            field.harvestProgress()
                        )
                    )
                )

                val progress = ProgressBar(
                    this,
                    null,
                    android.R.attr.progressBarStyleHorizontal
                ).apply {
                    max = 100
                    this.progress =
                        field.harvestProgress().toInt()
                    progressTintList =
                        android.content.res.ColorStateList
                            .valueOf(green)
                }

                card.addView(progress)

                val editButton = button("EDIT FIELD")
                editButton.setOnClickListener {
                    openField(field)
                }
                card.addView(editButton)

                val harvestButton = button(
                    "RECORD HARVEST"
                )
                harvestButton.setOnClickListener {
                    recordHarvest(field)
                }
                card.addView(harvestButton)

                val duplicateButton = button(
                    "DUPLICATE FIELD"
                )
                duplicateButton.setOnClickListener {
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
                            this,
                            "Could not duplicate field.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                card.addView(duplicateButton)

                val deleteButton = button(
                    "DELETE FIELD"
                )
                deleteButton.setOnClickListener {
                    AlertDialog.Builder(this)
                        .setTitle("Delete field?")
                        .setMessage(
                            "Delete ${field.name} and its harvest totals?"
                        )
                        .setPositiveButton("DELETE") { _, _ ->
                            if (storage.deleteField(field.id)) {
                                if (editingId == field.id) {
                                    editingId = null
                                    editingLabel.text =
                                        "Creating a new field"
                                }
                                refreshList()
                            } else {
                                Toast.makeText(
                                    this,
                                    "Could not delete field.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        .setNegativeButton("CANCEL", null)
                        .show()
                }
                card.addView(deleteButton)

                fieldList.addView(card)
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
                            "New field size is smaller than harvested area. Adjust harvest totals first.",
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

        // NEW FIELD / CLEAR

        newButton.setOnClickListener {
            editingId = null

            nameInput.text.clear()
            widthInput.text.clear()
            lengthInput.text.clear()
            rowInput.text.clear()
            plantInput.text.clear()
            yieldInput.text.clear()

            shapeSelector.setSelection(0)
            maizeSelector.setSelection(0)

            fieldView.fieldWidth = 200f
            fieldView.fieldLength = 300f
            fieldView.rowSpacing = 0.75f
            fieldView.fieldShape = "Rectangle"

            editingLabel.text =
                "Creating a new field"

            result.text =
                "Enter field measurements to calculate."

            scroll.smoothScrollTo(0, 0)
        }

        scroll.addView(layout)
        setContentView(scroll)

        refreshList()
    }

    private lateinit var refreshList: () -> Unit
}
