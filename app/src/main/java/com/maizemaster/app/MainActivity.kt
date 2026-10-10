package com.maizemaster.app

import android.app.Activity
import android.os.Bundle
import android.widget.*
import android.view.ViewGroup

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 30)
        }

        val title = TextView(this).apply {
            text = "🌽 MaizeMaster"
            textSize = 28f
        }

        val fieldView = FieldDesignerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                500
            )
        }

        val widthInput = EditText(this).apply {
            hint = "Field width (meters)"
            inputType = 8194
        }

        val lengthInput = EditText(this).apply {
            hint = "Field length (meters)"
            inputType = 8194
        }

        val spacingInput = EditText(this).apply {
            hint = "Row spacing (meters)"
            inputType = 8194
        }

        val result = TextView(this).apply {
            textSize = 18f
            text = "Enter your field measurements"
        }

        val button = Button(this).apply {
            text = "Update Field"

            setOnClickListener {
                val w = widthInput.text.toString()
                    .toFloatOrNull()

                val l = lengthInput.text.toString()
                    .toFloatOrNull()

                val s = spacingInput.text.toString()
                    .toFloatOrNull()

                if (w == null || l == null || s == null ||
                    !w.isFinite() || !l.isFinite() ||
                    !s.isFinite() || w <= 0f || l <= 0f ||
                    s <= 0f
                ) {
                    result.text = "Enter valid positive measurements"
                } else {
                    fieldView.fieldWidth = w
                    fieldView.fieldLength = l
                    fieldView.rowSpacing = s

                    val hectares = w * l / 10000.0

                    result.text =
                        "Field size: %.2f hectares"
                            .format(hectares)
                }
            }
        }

        layout.addView(title)
        layout.addView(fieldView)
        layout.addView(widthInput)
        layout.addView(lengthInput)
        layout.addView(spacingInput)
        layout.addView(button)
        layout.addView(result)

        val scroll = ScrollView(this)
        scroll.addView(layout)
        setContentView(scroll)
    }
}
