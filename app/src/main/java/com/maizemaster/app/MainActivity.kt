
package com.maizemaster.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {

    private val green = Color.rgb(27, 94, 50)
    private val lightGreen = Color.rgb(232, 245, 233)
    private val backgroundColor = Color.rgb(246, 249, 245)

    private fun rounded(color: Int, radius: Float = 22f): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = green
        window.navigationBarColor = Color.BLACK

        val scroll = ScrollView(this).apply {
            setBackgroundColor(backgroundColor)
            fillViewport = true
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 40)
        }

        fun title(textValue: String, size: Float = 20f): TextView {
            return TextView(this).apply {
                text = textValue
                textSize = size
                setTextColor(green)
                setTypeface(null, Typeface.BOLD)
                setPadding(8, 14, 8, 14)
            }
        }

        fun input(hintValue: String): EditText {
            return EditText(this).apply {
                hint = hintValue
                inputType = 8194
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

        val fieldSection = section()
        fieldSection.addView(title("🌱 My Maize Field"))

        val shapes = arrayOf("Rectangle", "Oval", "Circle")
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

        val plantingSection = section()
        plantingSection.addView(title("🚜 Planting Details"))

        val rowInput = input("Row spacing (metres)")
        val plantInput = input("Plant spacing (metres)")

        plantingSection.addView(rowInput)
        plantingSection.addView(plantInput)

        plantingSection.addView(title("🌽 Maize Variety"))

        val maizeSelector = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                arrayOf("White maize", "Yellow maize")
            )
        }
        plantingSection.addView(maizeSelector)

        val yieldInput = input("Expected tons per hectare")
        plantingSection.addView(yieldInput)

        layout.addView(plantingSection)

        val resultSection = section()
        resultSection.addView(title("📊 Harvest Estimate"))

        val result = TextView(this).apply {
            text = "Enter your field measurements to calculate."
            textSize = 17f
            setTextColor(green)
            setPadding(12, 18, 12, 18)
        }

        val calculateButton = Button(this).apply {
            text = "CALCULATE MY HARVEST"
            textSize = 16f
            setTextColor(Color.WHITE)
            background = rounded(green, 22f)
            setPadding(12, 18, 12, 18)
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
                    fieldView.invalidate()
                    lengthInput.visibility =
                        if (shape == "Circle") View.GONE
                        else View.VISIBLE
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        calculateButton.setOnClickListener {
            val shape = shapeSelector.selectedItem.toString()
            val w = widthInput.text.toString().toDoubleOrNull()
            val l = if (shape == "Circle") w
                else lengthInput.text.toString().toDoubleOrNull()
            val row = rowInput.text.toString().toDoubleOrNull()
            val plant = plantInput.text.toString().toDoubleOrNull()
            val yieldRate = yieldInput.text.toString().toDoubleOrNull()

            if (w == null || l == null || row == null ||
                plant == null || yieldRate == null ||
                !w.isFinite() || !l.isFinite() ||
                !row.isFinite() || !plant.isFinite() ||
                !yieldRate.isFinite() ||
                w <= 0 || l <= 0 || row <= 0 ||
                plant <= 0 || yieldRate < 0
            ) {
                result.text = "Please enter valid measurements."
            } else {
                val hectares =
                    HarvestCalculator.calculateHectares(w, l, shape)
                val tons =
                    HarvestCalculator.estimateTons(hectares, yieldRate)
                val plants =
                    HarvestCalculator.estimatePlants(
                        hectares, row, plant
                    )

                fieldView.fieldWidth = w.toFloat()
                fieldView.fieldLength = l.toFloat()
                fieldView.rowSpacing = row.toFloat()
                fieldView.fieldShape = shape
                fieldView.invalidate()

                result.text = """
                    🌽 ${maizeSelector.selectedItem}

                    📏 Field: %.2f hectares

                    🌱 Plant positions: %,d

                    🚜 Estimated harvest: %.2f tons
                """.trimIndent().format(hectares, plants, tons)
            }
        }

        resultSection.addView(calculateButton)
        resultSection.addView(result)
        layout.addView(resultSection)

        scroll.addView(layout)
        setContentView(scroll)
    }
}
