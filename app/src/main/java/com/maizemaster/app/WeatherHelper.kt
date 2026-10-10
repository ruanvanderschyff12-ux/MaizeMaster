package com.maizemaster.app

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlin.math.PI

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 30)
        }

        fun heading(value: String): TextView =
            TextView(this).apply {
                text = value
                textSize = 22f
                setPadding(0, 18, 0, 12)
            }

        fun numberInput(hintText: String): EditText =
            EditText(this).apply {
                hint = hintText
                inputType = 8194
            }

        val title = heading("🌽 MaizeMaster")

        val shapes = arrayOf("Rectangle", "Oval", "Circle")

        val shapeSelector = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                shapes
            )
        }

        val fieldView = FieldDesignerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                500
            )
        }

        val widthInput = numberInput("Field width (metres)")
        val lengthInput = numberInput("Field length (metres)")
        val rowInput = numberInput("Row spacing (metres)")
        val plantInput = numberInput("Plant spacing (metres)")
        val yieldInput = numberInput("Expected tons per hectare")

        val maizeTypes = arrayOf("White maize", "Yellow maize")

        val maizeSelector = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                maizeTypes
            )
        }

        val result = TextView(this).apply {
            textSize = 19f
            setPadding(0, 20, 0, 20)
            text = "Enter your measurements to calculate."
        }

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
                        if (shape == "Circle") View.GONE
                        else View.VISIBLE
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {}
            }

        val calculateButton = Button(this).apply {
            text = "Calculate My Harvest"

            setOnClickListener {
                val shape = shapeSelector.selectedItem.toString()

                val w = widthInput.text.toString().toDoubleOrNull()
                val l = if (shape == "Circle") w
                    else lengthInput.text.toString().toDoubleOrNull()
                val row = rowInput.text.toString().toDoubleOrNull()
                val plant = plantInput.text.toString().toDoubleOrNull()
                val yield = yieldInput.text.toString().toDoubleOrNull()

                if (w == null || l == null || row == null ||
                    plant == null || yield == null ||
                    !w.isFinite() || !l.isFinite() ||
                    !row.isFinite() || !plant.isFinite() ||
                    !yield.isFinite() ||
                    w <= 0 || l <= 0 || row <= 0 ||
                    plant <= 0 || yield < 0
                ) {
                    result.text = "Please enter valid measurements."
                } else {
                    val hectares =
                        HarvestCalculator.calculateHectares(w, l, shape)

                    val tons =
                        HarvestCalculator.estimateTons(hectares, yield)

                    val plants =
                        HarvestCalculator.estimatePlants(
                            hectares, row, plant
                        )

                    fieldView.fieldWidth = w.toFloat()
                    fieldView.fieldLength = l.toFloat()
                    fieldView.rowSpacing = row.toFloat()
                    fieldView.fieldShape = shape

                    val maize = maizeSelector.selectedItem.toString()

                    result.text = """
                        Maize type: $maize

                        Field area: %.2f hectares

                        Estimated plant positions: %,d

                        Estimated harvest: %.2f tons
                    """.trimIndent().format(hectares, plants, tons)
                }
            }
        }

        layout.addView(title)
        layout.addView(heading("Field Shape"))
        layout.addView(shapeSelector)
        layout.addView(fieldView)
        layout.addView(widthInput)
        layout.addView(lengthInput)
        layout.addView(heading("Planting Details"))
        layout.addView(rowInput)
        layout.addView(plantInput)
        layout.addView(heading("Maize Type"))
        layout.addView(maizeSelector)
        layout.addView(yieldInput)
        layout.addView(calculateButton)
        layout.addView(result)

        val scroll = ScrollView(this)
        scroll.addView(layout)
        setContentView(scroll)
    }
}
